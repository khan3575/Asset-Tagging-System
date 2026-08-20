# Setup Guide

This document describes how to configure and run the Asset Tagging System in a local development environment.

**Updated 2026-08-17** for Flyway-based schema management. If you set up this project before that date using the old `sql-schema/*.sql` scripts (now removed from the repository), your local database needs to be recreated from scratch — see Section 2.

## 1. Prerequisites

| Requirement | Notes |
|---|---|
| JDK 25 | The project targets `java.version=25` in `pom.xml`. An earlier JDK version will not build the project. |
| MySQL 8.0 or later | Must be running and reachable on the default port (3306) prior to starting the application. |
| Apache Maven | Not required as a separate installation. The Maven Wrapper (`./mvnw`, or `mvnw.cmd` on Windows) is included and will download the required Maven version automatically. |

No Node.js installation or frontend build process is required. Client-side assets are either CDN-hosted (Bootstrap, Bootstrap Icons) or served directly from `src/main/webapp` without a build step.

## 2. Database Setup

Hibernate schema generation is disabled (`spring.jpa.hibernate.ddl-auto=validate`); it confirms the schema matches the entities, it never creates or alters it. Flyway owns the schema now, applying migrations automatically at application startup — but it connects to a database that already exists; it cannot create the one it is connected to. Create it once, by hand:

```bash
mysql -u root -p -e "DROP DATABASE IF EXISTS asset_tagging_system;
                     CREATE DATABASE asset_tagging_system
                       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

That is the only manual database step. From here on, starting the application (Section 4) applies every migration under `src/main/resources/db/migration/` automatically, in order, and records what ran in a `flyway_schema_history` table it creates for itself. Confirm it worked:

```sql
SELECT version, description, success FROM flyway_schema_history;
```

**The old `sql-schema/*.sql` scripts have been removed from the repository.** They described the pre-redesign schema and were no longer used by anything even before removal — see [docs/ARCHITECTURE.md](ARCHITECTURE.md) §9 and [docs/DESIGN.md](DESIGN.md) for what changed and why. Recoverable from git history (`git log -- sql-schema/`) if ever needed for comparison.

### 2.1 Changing the Schema

Flyway records a checksum of every migration file at the moment it applies it, in `flyway_schema_history`. At each subsequent startup it re-hashes the files on disk and compares. A mismatch means the file was edited after it was applied — that is, the database no longer contains what the migration claims it contains — and Flyway refuses to start.

**An applied migration file is immutable.** To change the schema, add a new versioned migration (`V2__…sql`, `V3__…sql`); Flyway applies what is new and never revisits what has already run. Update the corresponding JPA entity in the same commit, so that `ddl-auto=validate` confirms the two agree at the next startup.

Two exceptions apply:

- **Repeatable migrations** (`R__` prefix, such as `R__asset_overview_view.sql`) are designed to be edited. Flyway re-applies a repeatable migration whenever its checksum changes, after all versioned migrations. Objects that can be dropped and recreated safely — views in particular — belong in these files.
- **The pre-deployment baseline.** While the schema is not yet deployed anywhere and no data requires preserving, correcting `V1__baseline_schema.sql` in place and recreating the database (Section 2) is preferable to accumulating corrective migrations against a design that was never live. This ceases to be an option permanently once any database exists that cannot be discarded.

**`flyway repair` does not resolve a checksum mismatch caused by an edit.** It rewrites the recorded checksums to match the current files without re-running anything, leaving the database in its previous state while Flyway reports agreement. Its legitimate uses are clearing the history row left by a failed migration, and accepting a change to an applied file that does not alter the schema, such as a corrected comment.

### 2.2 Sample Accounts

Development fixture data — under the `local` profile only, never in a shared or production environment — is provided by `src/main/resources/db/seed/V1000__dev_seed_data.sql`. It provisions three accounts sharing the password `Password123!`, valid only within this seeded dataset and never to be reused elsewhere:

| Email | Role | Department |
|---|---|---|
| `sakib@gmail.com` | ROLE_EMPLOYEE | Engineering |
| `mehedi@gmail.com` | ROLE_ADMIN | Operations |
| `fahim@gmail.com` | ROLE_ADMIN | Finance |

This file only runs when `db/seed` is included in `spring.flyway.locations` — configured in `application-local.properties` (Section 3), not in the shared `application.properties`.

## 3. Application Configuration

The `local` Spring profile requires a properties file that is not included in version control, as it is intended to hold environment-specific and potentially sensitive values. Create the following file prior to first run:

**File:** `src/main/resources/application-local.properties`

```properties
DB_NAME=asset_tagging_system
DB_USER=your_mysql_username
DB_PASS=your_mysql_password

logging.level.com.sil.asset_tagging_system=DEBUG

# Adds the dev seed data on top of the base schema migrations. Never point a
# shared or production environment at this location.
spring.flyway.locations=classpath:db/migration,classpath:db/seed
spring.flyway.clean-disabled=false
```

These values populate the `spring.datasource.*` properties defined in `application.properties`. If the application is started without an active profile supplying these values, it will attempt to connect to the database using the literal, unresolved placeholder text and the connection will fail.

`spring.flyway.clean-disabled=true` is the default in `application.properties`, disabling the `flyway:clean` command everywhere — it drops every Flyway-managed object outright. It is only relaxed under the `local` profile, and even there, use it deliberately: it is equivalent to the old `DROP DATABASE` step, and re-running migrations from empty is the fastest way back to a known-clean local database if seed data or a migration gets into a bad state.

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

Flyway runs during Spring Boot's startup sequence, before the embedded Tomcat server begins accepting requests — migration failures prevent the application from starting at all, rather than surfacing later as a broken page.

**Startup currently fails regardless of Flyway.** Several JPA entities do not match the current schema (`User.password`, among others) and Hibernate's `ddl-auto=validate` check rejects them before the application context finishes initializing. This is a known, tracked issue — see [docs/ARCHITECTURE.md](ARCHITECTURE.md) §0 for the complete list and what fixing it involves. It is not a setup mistake; the schema and the Java layer are genuinely out of sync as of this writing.

Once that is fixed, the application will be available at `http://localhost:8080`, with the login page at `http://localhost:8080/login`.

### 4.1 Live Reload of Facelets Views

The property `joinfaces.faces.project-stage=Development`, defined in `application.properties`, enables automatic reloading of `.xhtml` view changes without requiring an application restart. If changes are not reflected:

- Confirm that `src/main/webapp` is being served directly from source rather than from a compiled output directory, depending on the execution method used.
- If a view renders correctly in Chrome but produces an XML parsing error in Firefox, inspect the view for malformed or unclosed markup. Firefox requests JSF views as strict XML and will reject any structurally invalid document; Chrome does not enforce this.

## 5. Running Tests

```bash
./mvnw test
```

Automated tests execute under the `test` Spring profile, defined in `src/test/resources/application-test.properties`, against an in-memory H2 database.

**H2 cannot host this schema.** `asset_custody.active_asset_id` is a MySQL-specific generated column, and the `UNIQUE` constraint on it is what guarantees at most one active custodian per asset. Any test exercising custody behavior must run against real MySQL via Testcontainers (`org.testcontainers:mysql` is already a project dependency) rather than the H2 profile — see [docs/ARCHITECTURE.md](ARCHITECTURE.md) §2.2.

## 6. Building for Deployment

The application is packaged as a WAR file and may be deployed to an external servlet container, in addition to running via the embedded Tomcat server used during development.

```bash
./mvnw clean package
```

The resulting artifact is produced at `target/asset-tagging-system-0.0.1-SNAPSHOT.war`.

## 7. Troubleshooting

| Symptom | Resolution |
|---|---|
| Application fails to start with a schema validation error mentioning `password`, `enabled`, `value`, `status`, or an `Approval` field | This is the known issue described in [docs/ARCHITECTURE.md](ARCHITECTURE.md) §0 — the Java entity/DAO layer has not yet been updated to match the redesigned schema. Not a local setup problem. |
| Application fails to start with a Flyway checksum mismatch on `V1__baseline_schema.sql` | An applied migration file was edited in place — expected while the baseline is still being revised (most recently 2026-08-19, see [docs/ENUM_REFERENCE.md](ENUM_REFERENCE.md) §4). Recreate the database as in Section 2; the seed data is reapplied on the next start under the `local` profile. |
| Application fails to start with a Flyway error | Check `flyway_schema_history` for a row with `success = 0` — a prior run left a migration partially applied (MySQL DDL auto-commits, so a failed migration can leave some of its statements in effect). Fix the underlying issue, then run `flyway repair` before restarting, or drop and recreate the database (Section 2) if nothing needs preserving. |
| Login fails using a documented sample account | Confirm the `local` profile's `spring.flyway.locations` includes `classpath:db/seed` (Section 3) — without it, `V1000__dev_seed_data.sql` never runs and no sample accounts exist. |
| A CDN-hosted asset (Bootstrap, icon font) fails to load with no visible network error | Inspect the browser console rather than the network log. Subresource Integrity (`integrity`/`crossorigin`) attributes on CDN `<link>`/`<script>` tags block the resource silently if the computed hash does not match, producing only a console warning. |
| A static asset placed under `WEB-INF/` returns HTTP 404 | This is expected behavior. The Servlet specification prohibits direct client access to any path under `WEB-INF/`. Static assets must be placed under `src/main/webapp/resources/` and referenced using `<h:outputScript>` or `<h:outputStylesheet>` (see [docs/ARCHITECTURE.md](ARCHITECTURE.md#11-static-resource-handling)). |
