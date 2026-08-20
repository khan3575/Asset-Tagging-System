# Asset Tagging System

A web application for tracking company-owned physical assets across their working life:
registration, physical condition, which employee holds each item, the approval process
governing a change of holder, and a permanent record of every action taken.

## Overview

The audit trail is the system's primary purpose rather than a supporting feature. Every
authentication event and business action is recorded with the actor, their authority at the
time, the originating IP address, the outcome, and a correlation identifier grouping all
records produced by a single user action.

### Domain concepts

| Concept | Definition |
|---|---|
| Asset | A tracked physical item, identified by a unique asset tag |
| Condition | The physical state of an asset: `IN_SERVICE`, `DAMAGED`, `UNDER_MAINTENANCE`, `BEYOND_REPAIR`, `RETIRED` |
| Custody | Which employee holds an asset, and for what period. An asset has at most one active custody record |
| Approval | A request to change custody, requiring a configurable number of sign-offs |
| Approval action | One individual sign-off. A given user may act on a given approval only once |
| Activity log | The immutable record of every action performed in the system |

### Roles

| Role | Capabilities |
|---|---|
| `ROLE_ADMIN` | Initiates transfers, approves and rejects requests, views current holders, changes asset condition |
| `ROLE_EMPLOYEE` | Submits requests for themselves. Cannot see which employee holds a given asset |

### The two-axis model

An asset's **condition** and its **custody** are independent facts, stored separately.
`assets.condition_status` describes physical state and nothing else; who holds an asset
lives exclusively in `asset_custody`. A damaged asset can still be held; an in-service
asset can be unassigned.

Three invariants are enforced by the database rather than application code: at most one
open approval per asset, at most one active custody record per asset, and at most one
sign-off per user per approval. The reasoning is in [docs/DESIGN.md](docs/DESIGN.md).

## Technology Stack

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
| Styling | Bootstrap 5.3 |

## System Requirements

- JDK 25
- MySQL 8
- Git

Maven is not required separately; the repository includes the Maven wrapper.

## Getting Started

```bash
# 1. Create an empty database (Flyway builds the schema)
mysql -u root -p -e "CREATE DATABASE asset_tagging_system
                     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. Supply local credentials
cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties
#    then edit DB_NAME, DB_USER and DB_PASS

# 3. Run
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The application starts on `http://localhost:8080`. Development accounts and full
configuration detail are in [docs/SETUP.md](docs/SETUP.md).

## Documentation

| Document | Contents |
|---|---|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Layers, request processing, dependency injection, security, audit mechanism, implementation status |
| [docs/MODULE_GUIDE.md](docs/MODULE_GUIDE.md) | How to add a feature, with conventions and anti-patterns |
| [docs/DESIGN.md](docs/DESIGN.md) | Data-model rationale — why the schema has the shape it has |
| [docs/ENUM_REFERENCE.md](docs/ENUM_REFERENCE.md) | Every enumerated value, the column it backs, and where it is enforced |
| [docs/SETUP.md](docs/SETUP.md) | Environment setup, configuration, running, testing, deployment |

`src/main/resources/db/migration/` is the authoritative definition of the schema. Where any
document disagrees with the code, the code is correct and the document is to be updated.

## Project Status

| Module | Status |
|---|---|
| Database schema | Implemented, Flyway-managed |
| Authentication and session management | Implemented |
| Activity log — authentication events | Implemented |
| Activity log — viewing screen | View exists; corrections outstanding |
| Asset directory — list, search, pagination | Implemented |
| Asset detail view | Implemented, read-only |
| Asset registration | Implemented |
| User directory — list, search, filter, pagination | Implemented |
| User detail view | Implemented, read-only |
| User editing | View exists; no working write path |
| Activity log — business actions | Not implemented |
| Service layer | Not implemented; transactions currently sit on DAO methods |
| Approval workflow | Schema implemented; no read or write path |
| Custody assignment and release | Schema implemented; read path only |
| Dashboard | Placeholder view |
| Role-based authorisation | Partial; no method-level rules |
| Document upload | Out of scope |

A component-level breakdown is in
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#9-implementation-status).
