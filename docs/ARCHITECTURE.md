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
| Bean | Hold view state, call one service, return navigation outcomes | Call a DAO, open a transaction, contain business rules |
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

**Correction, 2026-08-27**: the paragraph and properties below describe an aspirational
extensionless-only design that does **not** match the current codebase, and the two named
properties aren't set anywhere in `application.properties` — they were probably confused
with OmniFaces's `joinfaces.omnifaces.faces-views-scan-paths`/`faces-views-extension-action`
(present in `application.properties`, but commented out). What's actually true, verified
empirically this session:

- Spring MVC `@Controller` classes (`AssetController`, `UserController`, `DashboardController`,
  `AuditLogViewController`, `LoginViewController`) exist and are load-bearing — they're what
  makes `/assets`, `/assets/{id}`, `/assets/new`, `/user`, `/user/{id}` etc. resolve at all.
  Each does a plain `RequestDispatcher.forward(...)` to the real `.xhtml` file, converting a
  path segment into a query parameter along the way (`/assets/{id}` → forward to
  `/asset-view.xhtml?id={id}`).
- This bridge is not incidental scaffolding waiting to be deleted — **JSF has no
  path-variable mechanism of its own**. `f:viewParam` only reads query-string parameters.
  Any URL with an `{id}`-style path segment (as opposed to a flat, parameterless page like
  `/dashboard` or `/audit-log`) structurally needs something to translate that segment into
  a query parameter before JSF can read it via `f:viewParam` — in this codebase, that's
  these controllers. Removing them would break every parameterized detail page.
- **A real bug in this bridge, found and fixed 2026-08-27**: both controllers originally
  declared `@PathVariable Long id`, which made Spring MVC convert the path segment to `Long`
  *before* forwarding — so a bad id (`/assets/abc`) threw Spring's own
  `MethodArgumentTypeMismatchException` (a raw 400 stack trace) instead of ever reaching
  `f:viewParam`'s validation. Fixed to `@PathVariable String id`, passing the raw string
  through unconverted so JSF's own converter does the real validation. See
  [development-plan.md](development-plan.md)'s 2026-08-27 entry for the verification.
- Stage 8 of the Refactoring Roadmap ("URL migration") is a narrower, different problem —
  the sidebar's `h:link outcome=` values not matching real filenames — not about removing
  this controller layer.

The one part of the original paragraph that *is* accurate and current:

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
`LoginAuditListener`; business events are recorded by the service performing them.

**The governing rule:** an activity-log write joins the transaction of the action it
records. If the log write fails, the action fails. Authentication events are the sole
exception, because they have no business transaction to join and a logging fault must not
prevent users from signing in.

## 9. Implementation Status

| Component | Status |
|---|---|
| Database schema | Implemented, Flyway-managed |
| Authentication and session management | Implemented |
| Activity log — authentication events | Implemented |
| Activity log — viewing screen | Implemented, `f:viewParam`-driven |
| Asset directory — list, search, pagination | Implemented, DB-side pagination (no N+1) |
| Asset detail view | Implemented, read-only |
| Asset registration | Implemented, transactional, logs `ASSET_REGISTERED` |
| User directory — list, search, filter, pagination | Implemented, `f:viewParam`-driven |
| User detail view | Implemented, read-only |
| User editing | In progress (roadmap T7.1) — service/DAO layer exists, `@ViewScoped` edit-state machine and form not yet built; see [development-plan.md](development-plan.md) |
| Activity log — business actions | Implemented for asset registration; not yet for user edits (pending T7.1) |
| Service layer | `AssetService`, `UserService` implemented; `@Transactional` boundary on both |
| Approval workflow | Schema implemented; no read or write path |
| Custody assignment and release | Schema implemented; read path only |
| Dashboard | Placeholder view |
| Role-based authorisation | Partial; no method-level rules |
| Document upload | Out of scope |
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
