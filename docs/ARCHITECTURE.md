# System Architecture

This document describes how the Asset Tagging System is built: its layers, request
processing, dependency-injection model, security architecture, and audit mechanism.
[docs/DESIGN.md](DESIGN.md) explains the reasoning behind the data model the application
is built on; this document describes the application itself.

Rewritten 2026-08-20 following a full architecture audit. All statements about current
behaviour in Section 9 were verified by building and running the application, not
inferred.

## 1. Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| Build | Apache Maven (wrapper included), WAR packaging |
| Application framework | Spring Boot 4.1 |
| Presentation | Jakarta Server Faces 4.1 (Mojarra), Facelets |
| JSF integration | JoinFaces 6.1 |
| Security | Spring Security |
| Persistence | Hibernate via `EntityManager`, native SQL exclusively |
| Schema management | Flyway |
| Database | MySQL 8 |
| Servlet container | Tomcat, embedded for development and external for deployment |
| Styling | Bootstrap 5.3 |

## 2. Layers

```
Browser
   │
   ▼
Facelets view (*.xhtml)      Markup and binding only. No logic.
   │  #{assetListView.assets}
   ▼
Backing bean (@Named)        View state. Translates user gestures into
   │                         one service call. No SQL, no transactions.
   ▼
Service (@Service)           Transaction boundary. Business rules.
   │  @Transactional         Orchestrates DAOs. Returns DTOs.
   ▼
DAO (@Repository)            Native SQL. One statement per method.
   │
   ▼
MySQL
```

### 2.1 Layer contracts

| Layer | May | May not |
|---|---|---|
| View | Bind values, render conditionally | Contain logic beyond a boolean test |
| Bean | Hold view state, call one service, resolve the current `Actor`, return navigation outcomes | Call a DAO, open a transaction, contain business rules |
| Service | Enforce rules, call several DAOs, open transactions | Import anything from `jakarta.faces` |
| DAO | Execute one SQL statement, map results | Contain business rules, open transactions |

The prohibition on `jakarta.faces` imports in the service layer is deliberate. A service
that reads `FacesContext` cannot be called from a scheduled job, a test, or a Spring event
listener. This project has encountered that failure before, when a Spring Security event
listener called a helper that assumed `FacesContext` was available and threw a
`NullPointerException` that aborted authentication.

### 2.2 Transaction boundaries

`@Transactional` belongs on service methods and nowhere else. A transaction should span
one user action, not one SQL statement.

Registering an asset is one action that writes two rows: the asset, and its activity-log
entry. With the transaction on the service method, both commit together or neither does.
With the transaction on the DAO method, they are independent, and a failure between them
leaves an asset that no audit trail records.

## 3. Request Processing

**Current as of 2026-08-27 (Refactoring Roadmap Stage 8, complete).** There is no
controller layer. `FacesServlet` answers every view URL directly:

- `joinfaces.faces.automatic-extensionless-mapping=true` makes `FacesServlet` register an
  exact servlet-path mapping for every physical `.xhtml` file it discovers, under the
  view's extensionless name (`webapp/asset/list.xhtml` → `/asset/list`). The webapp
  directory tree *is* the URL scheme; nothing bridges it.
- `joinfaces.faces.disable-facesservlet-to-xhtml=true` removes `FacesServlet`'s old
  `*.xhtml` wildcard mapping, so the extension is no longer a working URL at all.
- Every URL is now a flat, parameterless page or a query-string one — `/asset/detail?id=1`,
  not `/assets/1`. `f:viewParam` reads the id directly; no translation layer exists or is
  needed, since JSF's lack of a path-variable mechanism is no longer a constraint the
  routing has to work around.
- The five Spring MVC `@Controller` classes that previously bridged path-variable URLs into
  query-string ones (`AssetController`, `UserController`, `DashboardController`,
  `AuditLogViewController`, `LoginViewController`) are **deleted**. The one piece of
  behavior still needed — redirecting `GET /` to `/dashboard` — moved to
  `config/WebConfig.java`'s `addViewControllers(...)`.
- §7.1 covers the security rule this depends on: `.xhtml` and `/resources/**` are sealed at
  the `SecurityConfig` layer, first in the filter chain, in the same change that enabled
  the two properties above (Constraint C1 in the roadmap — splitting them across two
  changes means `.xhtml` templates serve as raw static source in between).

Values arriving in the URL are declared with `f:viewParam` inside `f:metadata`, never
parsed from the request parameter map by hand. `f:viewParam` performs type conversion and
validation; `f:viewAction` then runs after binding is complete, unlike `@PostConstruct`,
which runs before it. (`FacesUtil.getRequestParams()`, the old hand-parsing helper, has
been deleted — every list/detail page uses `f:viewParam`/`f:viewAction` now.)

## 4. Dependency Injection

Two containers run concurrently. This is inherent to running Jakarta Faces on Spring Boot
and is not a defect.

- **Spring** owns `@Service`, `@Repository`, `@Component` and `@Configuration` beans.
- **CDI (Weld)** owns `@Named` backing beans. Weld is required by Mojarra 4 and arrives
  transitively through JoinFaces.

JoinFaces bridges the two, which is why a `@Named` backing bean can inject a Spring
`@Service` directly.

### 4.1 Bean scopes

| Scope | Import | Use for |
|---|---|---|
| `@RequestScoped` | `jakarta.enterprise.context.RequestScoped` | Read-only views: lists, detail pages |
| `@ViewScoped` | `jakarta.faces.view.ViewScoped` | Any view containing a form |
| `@ApplicationScoped` | `jakarta.enterprise.context.ApplicationScoped` | Reference data that does not change at runtime |

A postback is a new HTTP request, so a `@RequestScoped` bean is a new instance with every
field reset to its default. State cannot survive a form submission. Any view with a form
therefore uses `@ViewScoped`, must implement `java.io.Serializable`, must declare a
`serialVersionUID`, and must mark injected services `transient`.

Note that `jakarta.faces.bean.ViewScoped` was removed in Jakarta Faces 4.0. Only
`jakarta.faces.view.ViewScoped` works with `@Named`.

## 5. Data Access

All data access uses `EntityManager.createNativeQuery(...)`. There are no Spring Data JPA
repositories, no JPQL, and no derived query methods. The `repository/` package was removed
and is not to be reintroduced. This mirrors the legacy system the project is modelled on
and is a project requirement rather than a style preference.

Services return DTO records shaped for the view. JPA entities do not cross the service
boundary. Rendering an entity resolves lazy associations during the render phase, which
produces one query per row; a flat record containing the joined values already resolved
makes that impossible by construction. `spring.jpa.open-in-view=false` turns any remaining
lazy access into an immediate, located error rather than a silent extra query.

Shared helpers live in `DaoUtils`: `exists(...)` for count-based existence checks and
`getLastInsertId(...)` for retrieving a generated key.

## 6. The Read-Model View

`R__asset_overview_view.sql` is a repeatable Flyway migration defining `asset_overview`,
which joins `assets` and `asset_custody` to answer "is this asset available" — a question
neither table answers alone. The join uses the generated column `active_asset_id` rather
than `asset_id AND status = 'ACTIVE'`, so it inherits the uniqueness guarantee described in
[docs/DESIGN.md](DESIGN.md) Section 5 instead of reconstructing it.

## 7. Security Architecture

Authentication is form-based against the `users` table, with BCrypt password hashing.
`CustomUserDetailsService` loads a user by email address; `CustomUserDetails` exposes the
user id and given name alongside the standard `UserDetails` contract.

Authorisation operates at two levels:

- **URL rules** in `SecurityConfig` govern navigation.
- **Method rules** (`@PreAuthorize`) govern decisions that depend on data, such as
  preventing an administrator from approving a request they raised themselves.

### 7.1 Denying direct template access

Two matchers are security-critical and must appear **first** in the filter chain, since
Spring Security evaluates rules in order:

```java
.requestMatchers("/**/*.xhtml", "/*.xhtml").denyAll()
.requestMatchers("/resources/**").denyAll()
```

Because `FacesServlet` no longer answers `*.xhtml`, the servlet container's default
servlet would otherwise serve Facelets templates as static files, disclosing their source.
This was verified directly: without the rule, requesting a `.xhtml` URL returns the
unprocessed template with its component tags intact.

Postbacks are unaffected, because a JSF form posts to the extensionless view id. The
`/resources/**` rule is included because that directory is directly readable as static
content, which would otherwise expose composite components as source; denying it costs
nothing, since Jakarta Faces serves those files through `/jakarta.faces.resource/**`
instead.

## 8. Logging and Audit

Two independent mechanisms.

**Application logging** — SLF4J and Logback writing to `logs/app.log`, for diagnostics.

**The activity log** — a business record in the `activity_log` table. Beyond actor, action
and timestamp, each row carries:

| Column | Purpose |
|---|---|
| `correlation_id` with `sequence_in_action` | Groups and orders every row produced by a single user action |
| `outcome` | `SUCCEEDED`, `DENIED` or `FAILED`, so refusals are recorded rather than invisible |
| `actor_roles` | A snapshot of the actor's authority at the time, not a live join |
| `details` | JSON for the genuinely variable remainder. Display only; no business logic may read from this column |

`CorrelationFilter` assigns a UUID per HTTP request. Authentication events are recorded by
`LoginAuditListener` and `BrowserAccessDeniedHandler`; business events are recorded by the
service performing them.

Every row is built through `AuditTrail`, which is the only component that calls
`ActivityLogDao` to write. Callers supply what is specific to their action and nothing
else; the correlation id, the default sequence, the outcome, the role parse, and the
choice of write path are decided in one place:

```java
auditTrail.record(ASSET_CONDITION_CHANGED, ASSET)
        .by(actor)
        .asset(assetId)
        .condition(previousCondition, newCondition)
        .summary("...")
        .save();
```

The actor arrives as an `Actor` record — `(userId, role, ipAddress)` — resolved in the web
layer by `Actor.current()` and passed into the service. Services never resolve it
themselves, which is what keeps them callable outside a request; the constraint is
self-enforcing, since `Actor.current()` reads `FacesContext` and throws outside one.

**The governing rule:** an activity-log write joins the transaction of the action it
records. If the log write fails, the action fails.

Two categories are excluded, each for a distinct reason.

**Authentication events** have no business transaction to join, and a logging fault must
not prevent users from signing in. `LoginAuditListener` and `BrowserAccessDeniedHandler`
therefore wrap their writes in `try`/`catch` and proceed regardless of the outcome.

**Refusals** — any row whose `outcome` is `DENIED` or `FAILED` — must not join the
transaction, because that transaction is about to be rolled back deliberately. A service
that refuses an action writes the audit row and then throws a `BusinessRuleException`,
which extends `RuntimeException` and so triggers Spring's default rollback. A row written
through the ordinary path would be discarded along with the action it was recording,
leaving the refusal invisible — the precise outcome the `DENIED` value exists to prevent.
Refusals are therefore written through `ActivityLogDao.logRefusal(...)`, annotated
`@Transactional(propagation = Propagation.REQUIRES_NEW)`, so the evidence commits
independently of the rollback that follows it.

Call sites do not choose between the two paths. Marking an entry `.refused(reason)` or
`.failed(reason)` sets the outcome *and* selects `logRefusal`, so the two cannot be set
inconsistently; `.bestEffort()` additionally swallows a logging failure, and is used only
on the authentication paths described above.

The distinction is not stylistic. A successful action and its log entry must succeed or
fail together; a refused action and its log entry must not.

## 9. Implementation Status

| Component | Status |
|---|---|
| Database schema | Implemented, Flyway-managed |
| Authentication and session management | Implemented |
| Activity log — authentication events | Implemented — `LOGIN_SUCCEEDED`, `LOGIN_FAILED`, `LOGOUT`, `ACCESS_DENIED` |
| Activity log — viewing screen | Implemented — joined actor/subject/holder names, before-and-after values, refusal reasons, correlation grouping, and filters on entity, action, outcome, date range and free text |
| Asset directory — list, search, pagination | Implemented, DB-side pagination (no N+1) |
| Asset detail view | Implemented — read fields plus admin-only transfer-initiation and condition-change forms |
| Asset registration | Implemented, transactional, logs `ASSET_REGISTERED` |
| Asset condition change | Implemented, transactional, force-releases active custody for `DAMAGED`/`UNDER_MAINTENANCE`, logs `ASSET_CONDITION_CHANGED` |
| User directory — list, search, filter, pagination | Implemented, `f:viewParam`-driven |
| User detail view | Implemented, read-only |
| User editing | Implemented — `@ViewScoped` edit-state machine, `h:selectOneMenu`/`h:selectBooleanCheckbox` form |
| Activity log — business actions | Implemented across every mutation path: asset registration and condition change, user edit and disable, transfer request, each approval signature, rejection and cancellation, and custody transfer and release |
| Activity log — refusals | Implemented — duplicate asset tag, pending-transfer conflict, self-approval, closed-approval, and authorisation failure are all recorded as `DENIED` rows through the `REQUIRES_NEW` path described in §8 |
| Service layer | `AssetService`, `UserService`, `ApprovalService` implemented; `@Transactional` boundary on all three |
| Approval workflow | Implemented — admin-initiated transfer, employee self-request (`ASSIGNMENT`), holder-initiated `RETURN`, `/approval/list` queue, `/approval/detail` decide page, self-approval blocked structurally and in the UI, every step audited. A `RETURN` requires one signature; the other types require two |
| Custody assignment and release | Implemented — transfer, force-release on any out-of-service condition, and `RETURN` (release with no replacement holder). `custody_end` is set by the database rather than passed from Java, so it shares a clock with the `custody_start` column default |
| Holder visibility | Admin-only. An employee sees whether an asset is available, assigned, or held by them, never who else holds it |
| Asset registration validation | Server-side in `AssetService`: required tag, name and category, purchase value above zero, purchase date not in the future. Violations recorded as `DENIED` rows |
| Asset editing | Implemented — tag, name, category, purchase date and value, admin-only, with the same validation as registration plus a duplicate-tag check excluding itself. Changed fields recorded as `details` JSON on an `ASSET_UPDATED` row. Condition is excluded and keeps its own form |
| User creation | Implemented at `/user/form`, admin-only. Always `ROLE_EMPLOYEE`; promotion is a separate act on the user detail page |
| Account withdrawal | Soft only. Accounts are never deleted, because `activity_log` and custody history reference the id; `enabled` is cleared to withdraw access and set to restore it, audited as `USER_DISABLED`/`USER_ENABLED`. A disabled account is refused at sign-in by Spring Security's `isEnabled()` check and recorded as `LOGIN_FAILED` with reason `User is disabled` |
| Dashboard | Implemented, role-differentiated — administrators see estate counts, condition breakdown and recent activity; employees see only what they hold and their own requests |
| Account settings | Implemented at `/settings` — own profile plus a self-service password change, verified server-side and audited as `PASSWORD_CHANGED` |
| Role-based authorisation | Partial; no method-level rules |
| Document upload | Implemented — photograph and purchase invoice per asset, stored as `LONGBLOB` in `asset_documents`, admin-only upload with type allow-list and size caps, audited as `ASSET_DOCUMENT_UPLOADED`. Served by `AssetDocumentController`, the single Spring MVC controller, because expression language cannot render a binary stream |
| Composite components (`resources/ats/`) | Implemented — `pagination`, `badge`, `field`, `filterSelect` |
| Query-string parameter handling | `f:viewParam`/`f:viewAction` on all list/detail pages; `FacesUtil` deleted |

The codebase is being brought to the architecture described above incrementally. Sections
2 through 8 describe the standard that new work must meet;
[docs/MODULE_GUIDE.md](MODULE_GUIDE.md) is the procedure for meeting it.

## 10. Project Structure

```
src/main/java/com/sil/asset_tagging_system/
├── config/          Spring configuration
├── bean/            JSF backing beans, grouped by domain
├── service/         Business logic and transaction boundaries
├── dto/             Read models and command objects
├── dao/             Native-SQL data access
├── model/           JPA entities and enums
├── security/        Authentication, correlation, authorisation helpers
├── exception/       Application exception hierarchy
├── util/            Stateless helpers
└── validation/      Shared validation constants

src/main/webapp/
├── WEB-INF/
│   ├── templates/   base.xhtml, main.xhtml
│   └── fragments/   header.xhtml, sidebar.xhtml
├── resources/
│   ├── css/
│   ├── js/
│   └── ats/         Composite components
└── <domain>/        Pages; the directory name is the URL segment

src/main/resources/
├── application*.properties
└── db/
    ├── migration/   Flyway versioned and repeatable migrations
    └── seed/        Development fixtures, local profile only
```

Anything under `WEB-INF/` is unreachable by URL — a servlet-container guarantee rather
than a configuration setting. Templates and fragments are never a navigation destination
and therefore belong there. Pages must not, because view scanning would not find them.

## 11. Static Resources

Stylesheets and scripts are declared with `h:outputStylesheet` and `h:outputScript`, not
hand-written `<link>` and `<script>` tags. Jakarta Faces then serves them through its own
resource handler at `/jakarta.faces.resource/<name>.faces?ln=<library>`.

Views must use `h:head` and `h:body` rather than plain `<head>` and `<body>`. The resource
handler relocates declared resources into the head *component*; with a plain `<head>`
there is no component to relocate into and the resources are silently dropped.
