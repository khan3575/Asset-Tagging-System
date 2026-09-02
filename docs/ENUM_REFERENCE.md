# Enum Reference

This document is the authority on every enumerated value in the system: which Java enum backs which database column, which values are legal, and where each set is enforced. It exists because enum values are the one kind of data that is duplicated by necessity — the constant in Java and the string in the database must agree exactly, and nothing in the build checks that they do.

Related: [docs/DESIGN.md](DESIGN.md) for why the schema has the shape it has, [docs/ARCHITECTURE.md](ARCHITECTURE.md) for how these columns are read and written.

## 1. Rules

1. **The database stores the constant name, verbatim.** Every enum-typed column is a `VARCHAR` holding text such as `IN_SERVICE`, never a numeric code.
2. **Every enum-typed entity field requires `@Enumerated(EnumType.STRING)`.** JPA's default is `ORDINAL`, which stores the constant's declaration position as a number. Against a `VARCHAR` column that default produces a schema validation failure at startup, or silently meaningless data if validation is disabled. The annotation belongs on the *field*, not on the enum declaration — `@Enumerated` targets methods and fields only, and placing it on the type is a compile error.
3. **Native queries must bind `.name()`, not the enum object.** Queries are issued through `EntityManager.createNativeQuery`, which binds plain JDBC values and does not consult the entity's `@Enumerated` mapping. Passing an enum instance directly leaves the binding strategy to the driver; passing `value.name()` does not.
4. **Renaming a constant is a schema change.** The value appears in `CHECK` constraints, in generated-column expressions, and in already-stored rows. A rename must move the Java enum, the constraint, any generated-column expression, the dev seed data, and calling code together in one change — never partially.
5. **Values already written to `activity_log` are historical record and must never be renamed.** Every other table holds current state and can be migrated; the log holds what was true at the time.

## 2. The enums

| Enum | Column | DB constraint | Values |
|---|---|---|---|
| `RoleName` | `roles.name` | `chk_role_name` | `ROLE_ADMIN`, `ROLE_EMPLOYEE` |
| `AssetCondition` | `assets.condition_status` | `chk_asset_condition` | `IN_SERVICE`, `DAMAGED`, `UNDER_MAINTENANCE`, `BEYOND_REPAIR`, `RETIRED` |
| `CustodyStatus` | `asset_custody.status` | `chk_custody_status` | `ACTIVE`, `RELEASED` |
| `RequestType` | `approvals.request_type` | `chk_appr_type` | `ASSIGNMENT`, `TRANSFER`, `RETURN` |
| `ApprovalStatus` | `approvals.status` | `chk_appr_status` | `PENDING`, `PARTIALLY_APPROVED`, `APPROVED`, `REJECTED`, `CANCELLED` |
| `ApprovalActionType` | `approval_actions.action` | `chk_aa_action` | `APPROVED`, `REJECTED`, `CANCELLED` |
| `ActivityEntityType` | `activity_log.entity_type` | `chk_log_entity` | `ASSET`, `USER`, `APPROVAL`, `AUTH`, `DEPARTMENT` |
| `ActivityOutcome` | `activity_log.outcome` | `chk_log_outcome` | `SUCCEEDED`, `DENIED`, `FAILED` |
| `ActivityAction` | `activity_log.action` | none, by design | See Section 3 |

`AssetCondition` and `RoleName` each back a second, denormalized column in `activity_log` — `previous_condition`/`new_condition` and `actor_roles` respectively. Those columns are deliberate point-in-time snapshots rather than live joins, and reuse the same enum because the vocabulary is the same.

### 2.1 Notes on individual sets

**`RoleName`** — the `ROLE_` prefix is mandated by Spring Security's `hasRole()` convention, which prepends `ROLE_` when matching. It is stored in the database with the prefix intact so that `CustomUserDetails.getAuthorities()` can pass the name through unchanged.

**`ApprovalStatus.PARTIALLY_APPROVED`** — the approval workflow requires `required_approval_count` sign-offs, configurable from 1 to 5, each recorded as a row in `approval_actions`. This status covers every intermediate point: at least one sign-off recorded, fewer than the number required. Together with `PENDING` it forms the set of *open* states, which is what the `open_asset_id` generated column keys on to enforce one open request per asset.

**`ApprovalActionType` vs `ApprovalStatus`** — three values overlap by name and must not be conflated. `ApprovalActionType` records what one actor did, on one row of `approval_actions`. `ApprovalStatus` records where the request as a whole stands. A request with two `APPROVED` actions out of three required is itself still `PARTIALLY_APPROVED`.

**`USER_DISABLED` and `USER_ENABLED`** — an account is never deleted, since the log and custody history reference its id. These two record the withdrawal and restoration of access, and are distinguished from `USER_UPDATED` by comparing the `enabled` flag against its prior value inside the same transaction.

**`ActivityOutcome.DENIED`** — an authenticated actor refused an action they were not permitted to take. Distinct from `FAILED`, which is an action that did not complete. Recording refusals is a deliberate feature of the log design (see [docs/DESIGN.md](DESIGN.md) §7.1); a rejected self-approval attempt is exactly the kind of event the audit panel exists to surface.

## 3. `ActivityAction`, and why it has no CHECK constraint

`activity_log.action` is a plain `VARCHAR(40)` with no `CHECK`. This is deliberate and is not an oversight in the schema.

Every value written to this column originates in application code; no user input reaches it. The Java enum is therefore a sufficient authority, and it is the one that catches mistakes at the point they are made — at compile time, in the file writing the log entry. A `CHECK` constraint would add a second copy of the list whose only effect would be to require a migration each time a new action is introduced.

Current values, grouped by area:

| Area | Values |
|---|---|
| Authentication | `LOGIN_SUCCEEDED`, `LOGIN_FAILED`, `LOGOUT`, `ACCESS_DENIED`, `PASSWORD_CHANGED` |
| Assets | `ASSET_REGISTERED`, `ASSET_UPDATED`, `ASSET_CONDITION_CHANGED`, `ASSET_DOCUMENT_UPLOADED` |
| Custody | `CUSTODY_ASSIGNED`, `CUSTODY_TRANSFERRED`, `CUSTODY_RELEASED` |
| Approvals | `REQUEST_SUBMITTED`, `REQUEST_APPROVED`, `REQUEST_REJECTED`, `REQUEST_CANCELLED` |
| Users and departments | `USER_CREATED`, `USER_UPDATED`, `USER_DISABLED`, `USER_ENABLED`, `PASSWORD_CHANGED`, `DEPARTMENT_CREATED`, `DEPARTMENT_CLOSED` |

Adding a value requires only a new constant. Two constraints apply: the name must fit within 40 characters, and an existing name must never be changed, because rows already written carry the old spelling and the log is a historical record.

`CUSTODY_ASSIGNED`, `DEPARTMENT_CREATED` and `DEPARTMENT_CLOSED` are reserved for workflows that do not exist yet — department management is not implemented, and `CUSTODY_ASSIGNED` is covered in practice by `CUSTODY_TRANSFERRED`, which records the assignment half of a move. Every other value is written by application code. Values that appear in `db/seed/V1000__dev_seed_data.sql` are included so that seeded rows and application-written rows use one vocabulary.

`ACCESS_DENIED` and `PASSWORD_CHANGED` were both added after the initial set, for authorisation failures reaching `BrowserAccessDeniedHandler` and self-service password changes respectively — `PASSWORD_CHANGED` is recorded on failure as well as success, since a rejected attempt with a wrong current password is precisely the event worth keeping. Neither required a migration, which is the concrete payoff of the no-`CHECK` decision described above: adding a new `ActivityAction` value going forward is always this cheap.
