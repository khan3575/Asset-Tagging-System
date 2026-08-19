# Asset Tagging System

## Overview

The Asset Tagging System is an internal web application for managing organizational assets. Its scope includes asset registration, custody assignment, multi-step approval workflows, and a complete audit trail of system activity. The application is implemented as a server-rendered Java web application using Jakarta Server Faces (JSF), consistent with the technology stack of the production system this project is designed to prepare for.

## Technology Stack

| Category | Technology |
|---|---|
| Language / Platform | Java 25 |
| Build Tool | Apache Maven (wrapper included) |
| Application Framework | Spring Boot 4.1 (WAR packaging) |
| Presentation Layer | Jakarta Server Faces 4.1 (Mojarra), Facelets |
| Security | Spring Security |
| Persistence | Spring Data JPA (entity mapping); data access via `EntityManager` native SQL queries |
| Schema Management | Flyway, versioned SQL migrations |
| Database | MySQL |
| Client-Side Technology | Bootstrap 5, plain JavaScript |
| Testing | JUnit 5, Spring Boot Test, H2 (in-memory, test profile — cannot host this schema; see docs/ARCHITECTURE.md §2.2) |

Full architectural detail, including the rationale for these choices, is provided in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## System Requirements

- JDK 25
- MySQL 8.0 or later
- No Node.js or frontend build tooling is required

## Getting Started

1. Create the database once, by hand — Flyway manages the schema from there. See [docs/SETUP.md](docs/SETUP.md) §2.
2. Create an `application-local.properties` file with the required database credentials.
3. Start the application using the Maven wrapper with the `local` profile active; Flyway migrations apply automatically.

Complete, step-by-step installation and configuration instructions are provided in [docs/SETUP.md](docs/SETUP.md).

> **Known issue:** the application does not currently start — several JPA entities do not match the redesigned schema. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#0-current-state--read-this-first) for the complete, verified list and what fixing it involves.

## Documentation

| Document | Description |
|---|---|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | System architecture, component structure, security model, current known issues |
| [docs/DESIGN.md](docs/DESIGN.md) | Data model design rationale — why the schema is shaped the way it is |
| [docs/SITE_MAP.md](docs/SITE_MAP.md) | Every route, its view/bean/DAO chain, and current status |
| [docs/DAO_REFERENCE.md](docs/DAO_REFERENCE.md) | Every DAO method's signature, task, and status against the current schema |
| [docs/ENUM_REFERENCE.md](docs/ENUM_REFERENCE.md) | Every enumerated value, the column it backs, and where it is enforced |
| [docs/SETUP.md](docs/SETUP.md) | Environment setup, configuration, running and testing the application |

## Project Status

| Module | Status |
|---|---|
| Database Schema (two-axis model, unified activity log) | Implemented, Flyway-managed |
| Authentication and Session Management | Implemented, blocked by schema/code misalignment |
| Unified Activity Log | Schema implemented; DAO/service layer not started |
| Application Logging (environment-aware) | Implemented |
| User Management — Directory and Search | Implemented, blocked by schema/code misalignment |
| User Management — Create / Update | Planned |
| Dashboard | Stub view only |
| Asset Management | Implemented against the old schema, needs updating |
| Approval Workflow | Schema implemented; no UI or write path |
| Asset Custody Tracking | Schema implemented; read path only |
| Role-Based Authorization | Planned |

A detailed breakdown of implementation status by component is provided in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#12-implementation-status).
