# Setup Guide

Environment setup, configuration, and the commands for running, testing and building the
Asset Tagging System.

## 1. Prerequisites

| Requirement | Notes |
|---|---|
| JDK 25 | The project targets Java 25 |
| MySQL 8 | Running and reachable on `localhost:3306` |
| Git | — |

Maven is not required separately. The repository includes the Maven wrapper (`./mvnw` on
macOS and Linux, `mvnw.cmd` on Windows).

## 2. Database

Create an empty database. Do not create any tables — Flyway builds the entire schema on
first start.

```sql
CREATE DATABASE asset_tagging_system
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

The MySQL account used by the application requires `SELECT`, `INSERT`, `UPDATE`, `DELETE`
and DDL privileges on this database, since Flyway issues `CREATE TABLE` statements.

## 3. Local Configuration

Credentials are supplied by a profile-specific file that is excluded from version control.
Copy the provided template:

```bash
cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties
```

Then edit it:

```properties
DB_NAME=asset_tagging_system
DB_USER=your_mysql_user
DB_PASS=your_mysql_password

# Adds the development fixtures on top of the base schema migrations.
# Never point a shared or production environment at db/seed.
spring.flyway.locations=classpath:db/migration,classpath:db/seed
spring.flyway.clean-disabled=false
```

`application-local.properties` must never be committed. The `.example` file is the
committed template and contains no real credentials.

## 4. Running

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The application starts on `http://localhost:8080`.

On first start Flyway applies `V1__baseline_schema.sql`, the repeatable view migration,
and — under the `local` profile only — the development seed data. Hibernate then validates
every entity against the migrated schema and refuses to start if any disagree.

Successful startup is confirmed by:

```
Started AssetTaggingSystemApplication in N seconds
```

The application writes to `logs/app.log` in addition to the console.

### 4.1 Live reload of Facelets views

`spring-boot-devtools` is on the classpath, so a recompile triggers an automatic restart.
Editing a `.xhtml` file under `src/main/webapp/` and refreshing the browser is sufficient
during development; `joinfaces.faces.project-stage=Development` disables Facelets caching.

## 5. Development Accounts

Provided by `src/main/resources/db/seed/V1000__dev_seed_data.sql`, applied locally under the
`local` profile and also to the public demo deployment (see `compose.yaml`). All three
accounts share the password `DemoOnly2026!`, which is valid solely within this seeded
dataset and must never be reused elsewhere.

| Email | Role | Department |
|---|---|---|
| `sakib@gmail.com` | `ROLE_EMPLOYEE` | Engineering |
| `mehedi@gmail.com` | `ROLE_ADMIN` | Operations |
| `fahim@gmail.com` | `ROLE_ADMIN` | Finance |

Seed data is applied only when `db/seed` is included in `spring.flyway.locations`, which is
configured in `application-local.properties` and deliberately absent from the shared
`application.properties`.

## 6. Tests

```bash
./mvnw test
```

## 7. Building for Deployment

```bash
./mvnw clean package
```

This produces a WAR in `target/`. `spring-boot-starter-tomcat` is declared at `provided`
scope so the container supplies its own servlet runtime, and
`AssetTaggingSystemApplication` extends `SpringBootServletInitializer` so the WAR
bootstraps correctly when deployed to an external Tomcat.

The deployed environment must supply `DB_NAME`, `DB_USER` and `DB_PASS`, and should
activate the `prod` profile, which sets `joinfaces.faces.project-stage=Production`.
Leaving the project stage at `Development` in a deployed environment renders full stack
traces to the browser.

## 8. Changing the Schema

The schema is Flyway-managed and applied migrations are immutable.

1. Add a new file `src/main/resources/db/migration/V<n>__<description>.sql`.
2. Update the corresponding JPA entity in `model/`.
3. Restart. Flyway applies the migration, then `ddl-auto=validate` confirms the entities
   still agree with the schema.

Never edit a migration that has already run against any database. Never run
`flyway:clean` outside a throwaway environment; `spring.flyway.clean-disabled=true` in the
shared configuration guards against this, and is deliberately overridden only in the local
profile.

MySQL DDL statements auto-commit individually — a migration that fails partway through
leaves everything before the failure permanently applied, with no transactional rollback the
way a JPQL/Hibernate operation would get. Keep each migration to one logical change so a
failure is easy to reason about and undo by hand.

## 9. Troubleshooting

| Symptom | Cause |
|---|---|
| `Schema-validation: missing column` at startup | An entity and the schema disagree. Add a migration, or correct the entity — do not change `ddl-auto` |
| `Access denied for user` | `DB_USER` / `DB_PASS` in `application-local.properties` are wrong, or the account lacks privileges |
| `Unknown database` | The database in Section 2 has not been created |
| No seed accounts exist | `db/seed` is missing from `spring.flyway.locations`, or the `local` profile is not active |
| Seed data looks wrong or out of date | The local database can hold different rows than `db/seed/V1000__dev_seed_data.sql` currently describes — Flyway does not re-run an already-applied versioned migration just because the file changed. Recreate the local database (Section 2) after editing the seed file |
| Flyway reports a checksum mismatch | An already-applied migration was edited. Restore it; add a new migration instead |
| Views render without styling | A view is using plain `<head>` instead of `h:head`, so declared resources were dropped |
