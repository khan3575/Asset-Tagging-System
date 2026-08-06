# Setup Guide

This document describes how to configure and run the Asset Tagging System in a local development environment.

## 1. Prerequisites

| Requirement | Notes |
|---|---|
| JDK 25 | The project targets `java.version=25` in `pom.xml`. An earlier JDK version will not build the project. |
| MySQL 8.0 or later | Must be running and reachable on the default port (3306) prior to starting the application. |
| Apache Maven | Not required as a separate installation. The Maven Wrapper (`./mvnw`, or `mvnw.cmd` on Windows) is included and will download the required Maven version automatically. |

No Node.js installation or frontend build process is required. Client-side assets are either CDN-hosted (Bootstrap, Bootstrap Icons) or served directly from `src/main/webapp` without a build step.

## 2. Database Setup

Hibernate schema generation is disabled (`spring.jpa.hibernate.ddl-auto=validate`); the database schema must be created manually. Execute the following scripts, in order:

```bash
mysql -u root -p < sql-schema/database_schema.sql
mysql -u root -p < sql-schema/database-population-schema.sql
```

`database_schema.sql` begins with `DROP DATABASE IF EXISTS asset_tagging_system`. On a system with an existing database of that name, this operation is destructive. Both scripts may be re-executed at any time to restore the database to a known, clean state.

### 2.1 Sample Accounts

The population script provisions the following accounts for local development and testing. All accounts share the password `Password123!`, which is valid only within this seeded development dataset and must not be reused for any other purpose.

| Email | Role | Department |
|---|---|---|
| `sakib@gmail.com` | ROLE_EMPLOYEE | Engineering |
| `mehedi@gmail.com` | ROLE_ADMIN | Operations |
| `fahim@gmail.com` | ROLE_ADMIN | Finance |

## 3. Application Configuration

The `local` Spring profile requires a properties file that is not included in version control, as it is intended to hold environment-specific and potentially sensitive values. Create the following file prior to first run:

**File:** `src/main/resources/application-local.properties`

```properties
DB_NAME=asset_tagging_system
DB_USER=your_mysql_username
DB_PASS=your_mysql_password

logging.level.com.sil.asset_tagging_system=DEBUG
```

These values populate the `spring.datasource.*` properties defined in `application.properties`. If the application is started without an active profile supplying these values, it will attempt to connect to the database using the literal, unresolved placeholder text and the connection will fail.

## 4. Running the Application

The `local` profile must be active when the application is started. The method of activation depends on the execution environment.

**Maven Wrapper:**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The property must be specified as `spring-boot.run.profiles`, which is recognized by the Spring Boot Maven plugin. The standard system property `spring.profiles.active` is not honored by the `spring-boot:run` goal when set via `-D`, as it is applied to Maven's own JVM rather than the forked application process.

Alternatively, the profile may be activated using an environment variable, which is honored regardless of how the application is started:

```bash
export SPRING_PROFILES_ACTIVE=local      # macOS/Linux
set SPRING_PROFILES_ACTIVE=local         # Windows
```

**IDE (IntelliJ IDEA):** Set the **Active profiles** field of the run configuration for `AssetTaggingSystemApplication` to `local`.

Once started, the application is available at `http://localhost:8080`. The login page is located at `http://localhost:8080/login`.

### 4.1 Live Reload of Facelets Views

The property `joinfaces.faces.project-stage=Development`, defined in `application.properties`, enables automatic reloading of `.xhtml` view changes without requiring an application restart. If changes are not reflected:

- Confirm that `src/main/webapp` is being served directly from source rather than from a compiled output directory, depending on the execution method used.
- If a view renders correctly in Chrome but produces an XML parsing error in Firefox, inspect the view for malformed or unclosed markup. Firefox requests JSF views as strict XML and will reject any structurally invalid document; Chrome does not enforce this.

## 5. Running Tests

```bash
./mvnw test
```

Automated tests execute under the `test` Spring profile, defined in `src/test/resources/application-test.properties`, against an in-memory H2 database. No local MySQL instance or additional configuration is required to run the test suite.

## 6. Building for Deployment

The application is packaged as a WAR file and may be deployed to an external servlet container, in addition to running via the embedded Tomcat server used during development.

```bash
./mvnw clean package
```

The resulting artifact is produced at `target/asset-tagging-system-0.0.1-SNAPSHOT.war`.

## 7. Troubleshooting

| Symptom | Resolution |
|---|---|
| Application fails to start with a schema validation error | The local database schema does not match `sql-schema/database_schema.sql`. Re-execute the scripts described in Section 2. |
| Login fails using a documented sample account | Verify the contents of the `users` table directly. The population script may have been modified since it was last executed against the local database. Re-execute the scripts described in Section 2. |
| A CDN-hosted asset (Bootstrap, icon font) fails to load with no visible network error | Inspect the browser console rather than the network log. Subresource Integrity (`integrity`/`crossorigin`) attributes on CDN `<link>`/`<script>` tags block the resource silently if the computed hash does not match, producing only a console warning. |
| A static asset placed under `WEB-INF/` returns HTTP 404 | This is expected behavior. The Servlet specification prohibits direct client access to any path under `WEB-INF/`. Static assets must be placed under `src/main/webapp/resources/` and referenced using `<h:outputScript>` or `<h:outputStylesheet>` (see [docs/ARCHITECTURE.md](ARCHITECTURE.md#11-static-resource-handling)). |
