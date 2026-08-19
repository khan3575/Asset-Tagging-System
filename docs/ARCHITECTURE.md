# System Architecture

## 1. Introduction

This document describes the architecture of the Asset Tagging System: the technology stack, the structure of the application, the request-processing model, the security architecture, and the current data model. It is intended for developers who are contributing to or maintaining this project.

**This document was rewritten on 2026-08-17** following a full redesign of the data model (the "two-axis" schema, described in [docs/DESIGN.md](DESIGN.md)) and the adoption of Flyway for schema management. Section 0 below states plainly what does and does not work today — read it before making changes.

## 0. Current State — Read This First

The database schema and the Java data-access layer moved out of sync during the redesign. The schema (Section 9) is current and Flyway-managed. **The `model` and `dao` packages are not yet updated to match it.** This is not a hypothetical risk — it has been verified directly against the running schema.

### 0.1 The application does not start

`spring.jpa.hibernate.ddl-auto=validate` runs at application context startup, before any request can be served. It compares every `@Entity` class against the live schema and refuses to boot if any of them disagree. **Five** entities currently disagree — four on individual columns, one because its entire table is gone:

| Entity | Field | Entity expects column | Schema (`V1__baseline_schema.sql`) has |
|---|---|---|---|
| `User` | `password` | `password` | `password_hash` |
| `Department` | `enabled` (`Boolean`) | `enabled` | *(removed — replaced by `closed_at DATETIME`)* |
| `Asset` | `value` | `"value"` | `purchase_value`, and precision changed `(10,2)` → `(12,2)` |
| `Asset` | `status` | `status` | `condition_status`, with a different set of allowed values |
| `Asset` | `enabled` | `enabled` | *(removed entirely — see [DESIGN.md](DESIGN.md))* |
| `Approval` | `firstApprover`, `finalApprover` | `first_approver_id`, `final_approver_id` | *(removed — see `approval_actions` in DESIGN.md)* |
| `Approval` | `firstApproverNotes`, `finalApproverNotes`, `rejectionReason` | matching columns | *(removed)* |
| `Approval` | `requestDate` | `request_date` | `requested_at` |
| `Approval` | `firstActionDate`, `finalActionDate`, `cancelledAt` | matching columns | *(removed — consolidated into `closed_at`)* |
| `AssetHistory` | *(the whole entity)* | table `asset_history` | *(table removed entirely — replaced by `activity_log`)* |

**`AssetHistory` was missed in the first pass of this audit and confirmed afterward.** Hibernate validates every registered `@Entity` at startup, not just the ones a request happens to query — so this entity alone would crash the application even if the other four were perfectly fixed. It isn't a column mismatch to patch; the entity itself needs deleting, alongside `AssetHistoryDao` (0.2) and everything that depends on either. The full deletion list — `AssetHistory`, `HistoryAction`, `AssetStatus`, `AssetHistoryDao`, `AuditLogDao`, `AuditLogEntry`, `AssetEventRecorder`, `AuditLogBean` — is in [docs/development-plan.md](development-plan.md) Step 3.5, Phase C, verified via `grep` to have no other callers.

`Role`, `AssetCategory`, `AssetCustody`, and `AssetDocument` were checked and are **not** affected — their entities match the current schema exactly.

### 0.2 Two DAOs target tables that no longer exist

`AuditLogDao` and `AssetHistoryDao` both issue native SQL against `audit_log` and `asset_history`. Neither table is created by `V1__baseline_schema.sql` — both were replaced by the single `activity_log` table (Section 8). Every method on both DAOs will fail with a SQL error the moment it runs, independent of the entity-validation problem above.

### 0.3 What is *not* broken

Verified clean against the current schema: `RoleDao`, `AssetCategoryDao`, `AssetCustodyDao`, `AssetDocumentDao`, `ApprovalDao`, and `DaoUtils`. `ApprovalDao` in particular is worth noting explicitly — it was checked column-by-column and every field it reads or writes (`asset_id`, `request_type`, `status`, `initiated_by_user_id`, `requester_id`, `previous_holder_id`) still exists unchanged in the new `approvals` table.

### 0.4 What fixing this involves

Not a schema problem — the schema is finished and correct. The remaining work is entirely in `model/` and `dao/`:

1. Rename/retype the fields listed in 0.1 on `User`, `Department`, `Asset`, and `Approval`, replace `Asset.status` (`AssetStatus` enum) with a new `AssetCondition` enum matching `IN_SERVICE / DAMAGED / UNDER_MAINTENANCE / BEYOND_REPAIR / RETIRED`, and remove `Approval`'s deleted fields.
2. Rewrite `AssetDao`'s and `UserDao`'s native SQL to use the renamed columns.
3. Rewrite `DepartmentDao`'s native SQL (`SELECT id, name, enabled FROM departments` → must read `closed_at`).
4. Replace `AuditLogDao` and `AssetHistoryDao` with a single DAO against `activity_log` (see Section 8.3).
5. Update the beans and views that read the affected fields: `AssetBean`, `AssetDetailBean`, `AssetFormBean`, `asset-list.xhtml`, `asset-view.xhtml`, `add-asset.xhtml`.

This is tracked as the top item in Section 12.

## 2. Technology Stack

| Layer | Technology |
|---|---|
| Language / Build | Java 25, Apache Maven |
| Application Framework | Spring Boot 4.1, packaged as a WAR and deployable to an embedded or external servlet container |
| Presentation Layer | Jakarta Server Faces (JSF) 4.1, Mojarra implementation, Facelets (`.xhtml`) views |
| Framework Integration | JoinFaces (`mojarra4-spring-boot-starter`), which configures Mojarra and Weld (CDI) within the Spring Boot application context |
| Security | Spring Security, form-based authentication, BCrypt password hashing, session-based authentication |
| Data Access | Spring Data JPA for entity mapping; query execution via `EntityManager` native SQL, not JPQL or repository-derived queries |
| Schema Management | Flyway (`spring-boot-starter-flyway` + `flyway-mysql`), versioned SQL migrations under `src/main/resources/db/migration` |
| Database | MySQL 8 (production and development); an in-memory H2 test profile exists but cannot host this schema — see Section 2.2 |
| Client-Side Technology | Bootstrap 5 and Bootstrap Icons (CDN-hosted), plain JavaScript |
| Boilerplate Reduction | Lombok |
| Testing | JUnit 5, Spring Boot Test, Spring Security Test |

### 2.1 Technology Rationale

The application is intentionally built using a server-rendered architecture rather than a REST API with a client-side single-page application. This design decision reflects the technology stack of the production system this project is intended to prepare developers for, which is built on an earlier version of JSF. Accordingly, two architectural constraints apply throughout the codebase:

- **Data access is implemented using native SQL, not Spring Data JPA repositories or JPQL.** Every data access object executes parameterized SQL via `EntityManager.createNativeQuery(...)`. This is a project requirement, not a stylistic preference, and applies to all new data access code.
- **JPA `@Entity` classes are retained for object-relational mapping only.** Entity classes continue to use JPA annotations for mapping and relationships, but are not queried through `JpaRepository` interfaces. Schema management (`spring.jpa.hibernate.ddl-auto=validate`) confirms entities match the schema; it never generates or alters the schema itself. The migrations under `db/migration` are the authoritative source (Section 2.2).

### 2.2 Flyway and the H2 problem

Schema changes are versioned SQL files, applied automatically at application startup, before Hibernate's `ddl-auto=validate` check runs. `V` (versioned) migrations apply once, in order, and are checksummed — never edit one after it has run anywhere. `R` (repeatable) migrations, used only for the `asset_overview` view, re-run whenever their file content changes. Full convention detail lives in the header comments of the migration files themselves (`src/main/resources/db/migration/`).

Two Flyway locations are configured:

| Location | Contains | Active under |
|---|---|---|
| `classpath:db/migration` | The real schema — `V1__baseline_schema.sql`, `R__asset_overview_view.sql` | Every environment |
| `classpath:db/seed` | Development fixture data (`V1000__dev_seed_data.sql`) — known accounts, sample assets and requests | `local` profile only |

**The `asset_custody` table depends on a MySQL-specific generated column** (`active_asset_id`, with a `UNIQUE` constraint on it) to guarantee at most one active custodian per asset at the database level. H2, used by the `test` Spring profile, does not support this construct. Tests exercising custody behavior must run against a real MySQL instance via Testcontainers (`org.testcontainers:mysql` is already a project dependency); the H2 test profile cannot verify this guarantee and should not be relied on for it.

## 3. Architectural Overview

The application follows a layered architecture:

```
                     ┌─────────────────────────┐
                     │        Web Browser        │
                     └────────────┬──────────────┘
                                  │ HTTP
                     ┌────────────▼──────────────┐
                     │  Spring Security Filter    │
                     │  Chain (Authentication,    │
                     │  Session Management)       │
                     └────────────┬──────────────┘
                                  │
                     ┌────────────▼──────────────┐
                     │  Spring MVC Controllers    │
                     │  (URL routing/forwarding)  │
                     └────────────┬──────────────┘
                                  │ forward
                     ┌────────────▼──────────────┐
                     │  JSF / Facelets View       │
                     │  Layer (.xhtml)            │
                     └────────────┬──────────────┘
                                  │ EL binding
                     ┌────────────▼──────────────┐
                     │  JSF Managed Beans (CDI)   │
                     └────────────┬──────────────┘
                                  │ dependency injection
                     ┌────────────▼──────────────┐
                     │  Data Access Objects (DAO) │
                     └────────────┬──────────────┘
                                  │ EntityManager (native SQL)
                     ┌────────────▼──────────────┐
                     │   MySQL (Flyway-migrated)   │
                     └─────────────────────────────┘
```

Flyway sits outside this request-time diagram entirely — it runs once, at application startup, before the filter chain (or anything else) begins accepting requests.

## 4. Dependency Injection Model

The application operates two dependency-injection containers concurrently: the Spring `ApplicationContext` and Weld, the CDI implementation used by JSF. These containers are independent by default; however, in this specific configuration (Spring Boot 4.1, JoinFaces 6.1, Weld), a CDI-managed bean is able to inject a Spring-managed bean directly, without an explicit integration layer such as `SpringBeanFacesELResolver`.

Two categories of managed components exist in the codebase:

| Category | Annotations | Container | Location |
|---|---|---|---|
| Spring beans | `@Service`, `@Repository`, `@Controller`, `@Component` | Spring `ApplicationContext` | `controller`, `dao`, `security`, `config`, `service` |
| JSF managed beans | `@Named` with a CDI scope (`@RequestScoped`, `@ApplicationScoped`) | Weld (CDI) | `bean` |

JSF managed beans are resolved by Expression Language (EL) references (for example, `#{userListBean.users}`) within Facelets views, and inject the Spring-managed data access objects they require.

## 5. Component Structure

| Package | Responsibility |
|---|---|
| `bean` | JSF managed beans backing individual views |
| `config` | Spring configuration classes, including `SecurityConfig` |
| `controller` | Spring MVC controllers that forward clean URLs to their corresponding Facelets views |
| `dao` | Data access objects implementing native SQL queries via `EntityManager` |
| `exception` | Application-specific exception hierarchy, rooted at `AssetTaggingSystemException` |
| `model` | JPA entity classes and enumerations |
| `security` | Spring Security integration: `CustomUserDetailsService`, `CustomUserDetails`, `LoginAuditListener`, `RestAccessDeniedHandler` |
| `service` | Cross-cutting business components, currently `AssetEventRecorder` |
| `util` | Shared helpers: `FacesUtil` (request-parameter access), `PageParams` (pagination parsing), `OptionalUtils` (consistent not-found handling) |
| `validation` | Shared validation constants |

There is no `repository` package. Spring Data JPA repository interfaces were used briefly early in the project and were fully removed once the native-SQL DAO layer replaced them — no trace of them should be reintroduced.

### 5.1 Controller Layer

Controllers in this application do not implement business logic. Their sole responsibility is to expose clean, extension-less URLs (for example, `/user` rather than `/user-list.xhtml`) by forwarding the incoming request to the corresponding Facelets view via `HttpServletRequest.getRequestDispatcher(...).forward(...)`. Once the forward is complete, the JSF lifecycle and the associated managed bean are responsible for populating and rendering the view.

## 6. Request Processing Flow

The following sequence describes the processing of a request to the user directory (`GET /user`) — the one flow in the application fully verified working end-to-end against the live schema:

1. The Spring Security filter chain evaluates the request against the configured authorization rules.
2. `UserController` forwards the request to `user-list.xhtml`.
3. The JSF runtime constructs the component tree for the view and resolves its EL expressions.
4. Weld instantiates `UserListBean` (request-scoped) and invokes its `@PostConstruct` initialization method.
5. The initialization method reads request parameters (search term, role, department, status, page) from `FacesContext` via `FacesUtil`, parses paging via `PageParams`, and invokes `UserDao.findUsers(...)` and `UserDao.countUsers(...)`.
6. `UserDao` executes native SQL against MySQL via `EntityManager` and maps the result set to `User`, `Department`, and `Role` objects. Because a native query cannot join-fetch a `@ManyToMany` association in a single result set, role assignments are retrieved in a second query and assembled in application code.
7. The view renders using `h:dataTable`, bound to the resulting collection.

Full method-level detail for every DAO is in [docs/DAO_REFERENCE.md](DAO_REFERENCE.md). The complete route table is in [docs/SITE_MAP.md](SITE_MAP.md). Every enumerated value and the column it backs is in [docs/ENUM_REFERENCE.md](ENUM_REFERENCE.md).

## 7. Security Architecture

Security is configured in `SecurityConfig` using Spring Security's `formLogin()` and `logout()` DSL.

| Aspect | Configuration |
|---|---|
| Authentication | Form-based, via `CustomUserDetailsService`, backed by `UserDao` |
| Password Storage | BCrypt (`PasswordEncoder` bean) |
| Session Management | Session-based; one active session per user; session-fixation protection enabled |
| Login Endpoint | `/login` |
| Logout Endpoint | `/logout` |
| CSRF Protection | Currently disabled |
| Authorization | Currently unrestricted beyond authentication (`anyRequest().authenticated()`); `@EnableMethodSecurity` is switched on but no `@PreAuthorize` annotation exists anywhere in the codebase, so no role-based rule is enforced anywhere |
| Access-Denied Handling | `RestAccessDeniedHandler` redirects to `/dashboard?error` |

The application data model includes role definitions (`ROLE_ADMIN`, `ROLE_EMPLOYEE`); enforcement of role-based authorization is planned but not yet implemented (Section 12).

## 8. Logging and Audit Architecture

**This section describes the target design.** The schema exists and is Flyway-managed; the Java layer that would populate and read it does not exist yet (Section 0.2). `AuditLogDao` and `AssetHistoryDao`, which implemented the previous two-table version of this design, are non-functional against the current schema.

### 8.1 One table, not two

Every event in the system — logins, asset registration, condition changes, custody moves, approval decisions, user and department changes — is recorded as a single row in `activity_log`. This replaces the previous `audit_log` (site-wide activity) and `asset_history` (per-asset event log) pair, which required every asset mutation to be written twice, once to each table, and kept no relationship between the two rows. Full table definition and rationale: [docs/DESIGN.md](DESIGN.md).

### 8.2 What makes a row useful, not just present

Three columns exist specifically to make the log answerable, not merely complete:

- **`correlation_id`** (`BINARY(16)`) — one UUID minted per HTTP request, shared by every row that request produces. A single approval click can write two rows (the approval decision, and the resulting custody transfer); without this column, nothing connects them.
- **`outcome`** (`SUCCEEDED` / `DENIED` / `FAILED`) — a refused action (an employee attempting to approve their own request) is recorded as evidence, not silently dropped. Previously only successful actions were recordable at all.
- **`actor_roles`** — a snapshot of the actor's roles *at the moment of the action*, not a live reference to their current roles. Recorded this way deliberately: an audit record must describe what was observed, not a value that can drift underneath it after the fact.

Both `correlation_id` and `outcome` were included in the baseline schema specifically because neither can be added retrospectively — an event already written without them can never be linked or classified after the fact.

### 8.3 The write path this implies

Every mutation should, within one transaction: write to the relevant state table, then write the corresponding `activity_log` row. If the log write fails, the transaction must fail with it — an operation that succeeds while leaving no trace defeats the purpose of an audit trail. The one exception is authentication: `LOGIN_SUCCEEDED` / `LOGIN_FAILED` are raised from a Spring Security event listener (`LoginAuditListener`) with no business transaction to join, and may fail independently of the login attempt itself.

No component currently implements this write path. `AssetEventRecorder` (in `service/`) is the intended home for it but still targets the old `asset_history` + `audit_log` pair and needs to be rewritten against `activity_log`; nothing currently calls it.

### 8.4 Application Logging

Separately from the audit trail above, operational and diagnostic logging is implemented using SLF4J and Logback, unaffected by the schema redesign. Log verbosity is environment-dependent: the `local` profile enables debug-level logging for application code, while the `prod` profile restricts output to warnings and errors. Log output is written to a rotating file (`logs/app.log`) using Spring Boot's default rotation policy. This is diagnostic noise for developers, not a permanent business record, and is not interchangeable with the `activity_log` audit trail above.

## 9. Data Model

The authoritative schema definition is `src/main/resources/db/migration/V1__baseline_schema.sql`, applied automatically by Flyway. Development fixture data is in `src/main/resources/db/seed/V1000__dev_seed_data.sql`, applied only under the `local` profile. The pre-redesign `sql-schema/` directory has been removed from the working tree — see the note at the end of this section.

Full rationale for every table, including the four design principles behind the two-axis split, is in [docs/DESIGN.md](DESIGN.md). Summary:

| Table | Description |
|---|---|
| `departments` | Organizational departments. `closed_at` (nullable) governs whether a department is still selectable; a closed department is never deleted, since users may still reference it. |
| `roles` | Application roles (`ROLE_ADMIN`, `ROLE_EMPLOYEE`) |
| `users` | Employee accounts, associated with a department. `password_hash` stores the BCrypt hash. |
| `user_role` | Many-to-many association between `users` and `roles` |
| `asset_categories` | Classification of assets, including depreciation rate. `retired_at` governs selectability, same pattern as `departments.closed_at`. |
| `assets` | Registered assets. Holds **condition only** (`condition_status`) — custody is never stored here; see the two-axis rationale in DESIGN.md. |
| `asset_documents` | Binary document storage associated with an asset (image, invoice), unchanged from the original design |
| `approvals` | The request state machine — status and the request's parties. Approver identities moved out to `approval_actions`. |
| `approval_actions` | One row per approval decision (approve/reject/cancel), replacing the old fixed `first_approver_id`/`final_approver_id` columns. A `UNIQUE (approval_id, actor_user_id)` constraint makes it impossible for one administrator to supply two signatures on the same request. |
| `asset_custody` | Current and historical custody assignments. A generated column (`active_asset_id`) with a `UNIQUE` constraint guarantees at most one active custodian per asset at the database level. |
| `activity_log` | The unified event log for the whole system (Section 8) |
| `asset_overview` (view) | Recomputes the pre-redesign single-status reading (`AVAILABLE`/`ASSIGNED`/condition) by joining `assets` and `asset_custody` — a repeatable Flyway migration, not a `V`-versioned one, since it holds no data of its own |

Implementation status for each table's corresponding application layer is in Section 12.

**On `sql-schema/`:** the directory has been removed from the working tree. It held the pre-redesign schema (`database_schema.sql`, `database-population-schema.sql`, and the DROP-based recreation approach they implied) and was no longer run by anything — Flyway migrations under `db/migration` are authoritative. The full pre-redesign schema and rationale, if ever needed for comparison, is recoverable from git history (`git log -- sql-schema/`).

## 10. Project Structure

```
src/main/java/com/sil/asset_tagging_system/
├── bean/          JSF managed beans
├── config/        Spring configuration
├── controller/    Spring MVC controllers (URL forwarding)
├── dao/           Data access objects (native SQL)
├── exception/     Application exception hierarchy
├── model/         JPA entities and enumerations
│   └── enums/     AssetStatus, ApprovalStatus, CustodyStatus, HistoryAction,
│                  RequestType, RoleName — AssetStatus and HistoryAction are
│                  stale relative to the schema; see Section 0.1 and 0.2
├── security/      Authentication and authorization integration
├── service/       AssetEventRecorder (cross-cutting, currently unwired — Section 8.3)
├── util/          FacesUtil, PageParams, OptionalUtils
└── validation/     Shared validation constants

src/main/webapp/
├── *.xhtml                 Top-level views
├── WEB-INF/templates/      Shared Facelets fragments (header, sidebar)
└── resources/js/           Client-side JavaScript, served via the JSF resource-library mechanism

src/main/resources/
├── db/
│   ├── migration/           Flyway schema migrations — authoritative, every environment
│   │   ├── V1__baseline_schema.sql
│   │   └── R__asset_overview_view.sql
│   └── seed/                Flyway dev-only fixture data — local profile only
│       └── V1000__dev_seed_data.sql
├── application.properties           Base config; sets spring.flyway.locations=db/migration
├── application-local.properties     Gitignored; adds db/seed to Flyway locations
└── application-*.properties         Environment-specific overrides

docs/              Project documentation (this directory)
```

## 11. Static Resource Handling

Static assets (JavaScript, CSS, images) must be located under `src/main/webapp/resources/<library-name>/` and referenced using the JSF resource-library tags `<h:outputScript>` and `<h:outputStylesheet>`. Assets placed under `WEB-INF/` are not servable, as the Servlet specification prohibits direct client access to any path under `WEB-INF/`, regardless of file presence.

## 12. Implementation Status

| Component | Status | Notes |
|---|---|---|
| **Schema/code alignment** | **Broken — blocks startup** | See Section 0. `User`, `Department`, `Asset`, `Approval` entities and `AssetDao`, `UserDao`, `DepartmentDao`, `AuditLogDao`, `AssetHistoryDao` all need updating against the current schema. This is the highest-priority outstanding item. |
| Authentication and Session Management | Implemented, currently non-functional | Logic is correct; blocked entirely by `User.password` vs. `users.password_hash` (Section 0.1) until the entity is fixed. |
| Unified Activity Log (`activity_log`) | Schema implemented; application layer not started | Table, indexes and constraints exist and are Flyway-managed. No DAO, no `AssetEventRecorder` rewrite, no view. |
| Application Logging | Implemented | Environment-aware SLF4J/Logback configuration, unaffected by the schema change |
| User Directory (list, search, filter, pagination) | Implemented, currently non-functional | Logic verified correct in isolation (Section 6); blocked by the same startup failure as authentication. |
| User Detail View | Implemented, currently non-functional | Same blocker. |
| User Create / Update | In Progress | View includes an edit mode; no corresponding controller endpoint exists yet |
| Dashboard | Stub view exists | `/dashboard` renders; `DashboardBean` is an empty class with no content |
| Asset Management (list, view, create) | Implemented against the old schema, now broken | See Section 0.1; needs `AssetDao`/`Asset`/`AssetBean`/`AssetDetailBean`/`AssetFormBean` updated to `condition_status`/`purchase_value` |
| Two-Axis Asset Model (condition vs. custody) | Schema implemented | See DESIGN.md. Not yet reflected anywhere in the Java layer or the UI. |
| Approval Workflow | Schema implemented; partially wired | `ApprovalDao.existsOpenTransferRequest` / `createTransferRequest` are correct against the current schema and unused by any UI flow. No approve/reject path, no queue view, no `approval_actions` writes anywhere. |
| Asset Custody Tracking | Schema implemented; read path only | `AssetCustodyDao.findActiveCustodianId` works; no write path exists anywhere in the application. |
| Flyway Schema Management | Implemented | `V1` baseline + `R` view migration + `V1000` dev seed, all verified to compile and resolve offline |
| Role-Based Authorization | Not implemented | Role data model exists and resolves correctly into `GrantedAuthority`; `@EnableMethodSecurity` is enabled but zero rules are defined anywhere |
