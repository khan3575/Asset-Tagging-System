# System Architecture

## 1. Introduction

This document describes the architecture of the Asset Tagging System: the technology stack, the structure of the application, the request-processing model, the security architecture, and the current data model. It is intended for developers who are contributing to or maintaining this project.

## 2. Technology Stack

| Layer | Technology |
|---|---|
| Language / Build | Java 25, Apache Maven |
| Application Framework | Spring Boot 4.1, packaged as a WAR and deployable to an embedded or external servlet container |
| Presentation Layer | Jakarta Server Faces (JSF) 4.1, Mojarra implementation, Facelets (`.xhtml`) views |
| Framework Integration | JoinFaces (`mojarra4-spring-boot-starter`), which configures Mojarra and Weld (CDI) within the Spring Boot application context |
| Security | Spring Security, form-based authentication, BCrypt password hashing, session-based authentication |
| Data Access | Spring Data JPA for entity mapping; query execution via `EntityManager` native SQL, not JPQL or repository-derived queries |
| Database | MySQL (production and development); H2 in-memory database for the automated test profile |
| Client-Side Technology | Bootstrap 5 and Bootstrap Icons (CDN-hosted), plain JavaScript |
| Boilerplate Reduction | Lombok |
| Testing | JUnit 5, Spring Boot Test, Spring Security Test |

### 2.1 Technology Rationale

The application is intentionally built using a server-rendered architecture rather than a REST API with a client-side single-page application. This design decision reflects the technology stack of the production system this project is intended to prepare developers for, which is built on an earlier version of JSF. Accordingly, two architectural constraints apply throughout the codebase:

- **Data access is implemented using native SQL, not Spring Data JPA repositories or JPQL.** All data access objects (`UserDao`, `DepartmentDao`, `RoleDao`, `AuditLogDao`) execute parameterized SQL via `EntityManager.createNativeQuery(...)`. This is a project requirement, not a stylistic preference, and applies to all new data access code.
- **JPA `@Entity` classes are retained for object-relational mapping only.** Entity classes (`User`, `Department`, `Role`, and the Asset-domain entities) continue to use JPA annotations for mapping and relationships, but are not queried through `JpaRepository` interfaces. Schema management (`spring.jpa.hibernate.ddl-auto=validate`) is disabled; the SQL scripts under `sql-schema/` are the authoritative source for database schema.

## 3. Architectural Overview

The application follows a layered architecture, illustrated below.

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
                     │          MySQL              │
                     └─────────────────────────────┘
```

## 4. Dependency Injection Model

The application operates two dependency-injection containers concurrently: the Spring `ApplicationContext` and Weld, the CDI implementation used by JSF. These containers are independent by default; however, in this specific configuration (Spring Boot 4.1, JoinFaces 6.1, Weld), a CDI-managed bean is able to inject a Spring-managed bean directly, without an explicit integration layer such as `SpringBeanFacesELResolver`.

Two categories of managed components exist in the codebase:

| Category | Annotations | Container | Location |
|---|---|---|---|
| Spring beans | `@Service`, `@Repository`, `@Controller`, `@Component` | Spring `ApplicationContext` | `controller`, `dao`, `security`, `config` |
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
| `repository` | Spring Data JPA repository interfaces (see Section 12) |
| `security` | Spring Security integration: `CustomUserDetailsService`, `CustomUserDetails`, `LoginAuditListener`, `RestAccessDeniedHandler` |
| `validation` | Shared validation constants |

### 5.1 Controller Layer

Controllers in this application do not implement business logic. Their sole responsibility is to expose clean, extension-less URLs (for example, `/user` rather than `/user-list.xhtml`) by forwarding the incoming request to the corresponding Facelets view via `HttpServletRequest.getRequestDispatcher(...).forward(...)`. Once the forward is complete, the JSF lifecycle and the associated managed bean are responsible for populating and rendering the view.

## 6. Request Processing Flow

The following sequence describes the processing of a request to the user directory (`GET /user`):

1. The Spring Security filter chain evaluates the request against the configured authorization rules.
2. `UserController` forwards the request to `user-list.xhtml`.
3. The JSF runtime constructs the component tree for the view and resolves its EL expressions.
4. Weld instantiates `UserListBean` (request-scoped) and invokes its `@PostConstruct` initialization method.
5. The initialization method reads request parameters (search term, role, department, status, page) from `FacesContext` and invokes `UserDao.findUsers(...)` and `UserDao.countUsers(...)`.
6. `UserDao` executes native SQL against MySQL via `EntityManager` and maps the result set to `User`, `Department`, and `Role` objects. Because a native query cannot join-fetch a `@ManyToMany` association in a single result set, role assignments are retrieved in a second query and assembled in application code.
7. The view renders using `h:dataTable`, bound to the resulting collection.

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
| Authorization | Currently unrestricted (`permitAll()` on all request patterns); see Section 12 |
| Access-Denied Handling | `RestAccessDeniedHandler` redirects to `/dashboard?error` |

The application data model includes role definitions (`ROLE_ADMIN`, `ROLE_EMPLOYEE`); enforcement of role-based authorization is planned but not yet implemented (Section 12).

## 8. Logging and Audit Architecture

The system distinguishes between two categories of logging, which serve different purposes and are not interchangeable.

### 8.1 Audit Trail

The `audit_log` table, accessed through `AuditLogDao`, is a permanent business record of system activity. It is not affected by logging-level configuration and is retained identically across all environments. Login success and failure events are recorded automatically via `LoginAuditListener`, a Spring event listener attached to Spring Security's authentication events. Audit records are viewable through the `/audit-log` endpoint. The established convention is that all future create, update, and delete operations record an entry via `AuditLogDao.log(...)`.

### 8.2 Application Logging

Operational and diagnostic logging is implemented using SLF4J and Logback. Log verbosity is environment-dependent: the `local` profile enables debug-level logging for application code, while the `prod` profile restricts output to warnings and errors. Log output is written to a rotating file (`logs/app.log`) using Spring Boot's default rotation policy.

## 9. Data Model

The authoritative schema definition is `sql-schema/database_schema.sql`. Sample data is provided in `sql-schema/database-population-schema.sql`. Hibernate schema generation is disabled; the database must be provisioned using these scripts (see [docs/SETUP.md](SETUP.md)).

| Table | Description |
|---|---|
| `departments` | Organizational departments |
| `roles` | Application roles (`ROLE_ADMIN`, `ROLE_EMPLOYEE`) |
| `users` | Employee accounts, associated with a department |
| `user_role` | Many-to-many association between `users` and `roles` |
| `audit_log` | System-wide audit trail (Section 8.1) |
| `asset_categories` | Classification of assets, including depreciation rate |
| `assets` | Registered assets, including tag, status, value, and category |
| `asset_documents` | Binary document storage associated with an asset (image, invoice) |
| `approvals` | Multi-step approval workflow records for asset requests, transfers, and returns |
| `asset_custody` | Current and historical custody assignments for each asset |
| `asset_history` | Append-only event history for each asset |

Implementation status for each table's corresponding application layer is provided in Section 12.

## 10. Project Structure

```
src/main/java/com/sil/asset_tagging_system/
├── bean/          JSF managed beans
├── config/        Spring configuration
├── controller/    Spring MVC controllers (URL forwarding)
├── dao/           Data access objects (native SQL)
├── exception/     Application exception hierarchy
├── model/         JPA entities and enumerations
├── repository/    Spring Data JPA repositories (Asset domain)
├── security/      Authentication and authorization integration
└── validation/    Shared validation constants

src/main/webapp/
├── *.xhtml                 Top-level views
├── WEB-INF/templates/      Shared Facelets fragments (header, sidebar)
└── resources/js/           Client-side JavaScript, served via the JSF resource-library mechanism

sql-schema/        Database schema and sample data definitions
docs/              Project documentation
```

## 11. Static Resource Handling

Static assets (JavaScript, CSS, images) must be located under `src/main/webapp/resources/<library-name>/` and referenced using the JSF resource-library tags `<h:outputScript>` and `<h:outputStylesheet>`. Assets placed under `WEB-INF/` are not servable, as the Servlet specification prohibits direct client access to any path under `WEB-INF/`, regardless of file presence.

## 12. Implementation Status

| Component | Status | Notes |
|---|---|---|
| Authentication and Session Management | Implemented | Spring Security `formLogin()`, BCrypt |
| Audit Logging (login events) | Implemented | `AuditLogDao`, `LoginAuditListener`, viewable at `/audit-log` |
| Application Logging | Implemented | Environment-aware SLF4J/Logback configuration |
| User Directory (list, search, filter, pagination) | Implemented | `UserController`, `UserListBean`, `UserDao` |
| User Detail View | Implemented | Read-only |
| User Create / Update | In Progress | View includes an edit mode; no corresponding controller endpoint exists yet |
| Dashboard | Planned | Referenced as the post-login destination; no controller mapping or view exists yet |
| Asset Management | Planned | Schema and JPA entity defined; no data access, controller, or view layer implemented |
| Approval Workflow | Planned | Schema and JPA entity defined; no data access, controller, or view layer implemented |
| Asset Custody Tracking | Planned | Schema and JPA entity defined; no data access, controller, or view layer implemented |
| Asset History | Planned | Schema and JPA entity defined; no data access, controller, or view layer implemented |
| Role-Based Authorization | Planned | Role data model exists; not currently enforced |
| Asset-Domain Data Access Migration | Planned | `repository/` interfaces for the Asset domain are to be replaced by native-SQL DAOs, consistent with Section 2.1 |
