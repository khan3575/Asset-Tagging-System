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
| Database | MySQL |
| Client-Side Technology | Bootstrap 5, plain JavaScript |
| Testing | JUnit 5, Spring Boot Test, H2 (in-memory, test profile) |

Full architectural detail, including the rationale for these choices, is provided in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## System Requirements

- JDK 25
- MySQL 8.0 or later
- No Node.js or frontend build tooling is required

## Getting Started

1. Provision the database using the scripts in `sql-schema/`.
2. Create an `application-local.properties` file with the required database credentials.
3. Start the application using the Maven wrapper with the `local` profile active.

Complete, step-by-step installation and configuration instructions are provided in [docs/SETUP.md](docs/SETUP.md).

## Documentation

| Document | Description |
|---|---|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | System architecture, component structure, security model, data model |
| [docs/SETUP.md](docs/SETUP.md) | Environment setup, configuration, running and testing the application |

## Project Status

| Module | Status |
|---|---|
| Authentication and Session Management | Implemented |
| Audit Logging (login events) | Implemented |
| Application Logging (environment-aware) | Implemented |
| User Management — Directory and Search | Implemented |
| User Management — Create / Update | Planned |
| Dashboard | Planned |
| Asset Management | Planned |
| Approval Workflow | Planned |
| Asset Custody Tracking | Planned |
| Asset History | Planned |
| Role-Based Authorization | Planned |

A detailed breakdown of implementation status by component is provided in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#12-implementation-status).
