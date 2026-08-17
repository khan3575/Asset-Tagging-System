# DAO Reference

Every method on every class in `src/main/java/com/sil/asset_tagging_system/dao/`, its signature, its task, and its status against the current schema (`V1__baseline_schema.sql`). Written 2026-08-17. Status definitions:

- **Clean** — verified column-by-column against the current schema; works as written.
- **Broken** — references a column that was renamed or removed.
- **Dead table** — references a table that no longer exists at all.
- **Dead code** — compiles and would work, but nothing in the application currently calls it.

All data access uses `EntityManager.createNativeQuery(...)` — no `JpaRepository`, no JPQL. See [docs/ARCHITECTURE.md](ARCHITECTURE.md) §2.1 for why.

## ApprovalDao — **Clean**

Verified field-by-field: every column it reads or writes (`asset_id`, `request_type`, `status`, `initiated_by_user_id`, `requester_id`, `previous_holder_id`) exists unchanged in the current `approvals` table.

| Method | Task |
|---|---|
| `boolean existsOpenTransferRequest(Long assetId)` | True if a `TRANSFER_REQUEST` for this asset is currently `PENDING` or `FIRST_APPROVED`. Called by `AssetDetailBean` to gate whether a new transfer can be started. |
| `@Transactional Long createTransferRequest(Long assetId, Long initiatedByUserId, Long requesterId, Long previousHolderId)` | Inserts a new `TRANSFER_REQUEST` row (`status = PENDING`) and returns its generated id. `previousHolderId` may be `null` if the asset has no current custodian. **Not called from anywhere in the application** — dead code, but ready to use. |

## AssetCategoryDao — **Clean**

| Method | Task |
|---|---|
| `Optional<AssetCategory> findByNameIgnoreCase(String name)` | Case-insensitive category lookup by name |
| `boolean existsByNameIgnoreCase(String name)` | Duplicate-name check |
| `List<AssetCategory> findAll()` | Full category list, feeds the `<h:selectOneMenu>` on `add-asset.xhtml` via `LookupBean` |

## AssetCustodyDao — **Clean**

| Method | Task |
|---|---|
| `Optional<Long> findActiveCustodianId(Long assetId)` | The current holder's user id, or empty if unheld. Reads `WHERE status = 'ACTIVE'` — could be rewritten to join on `active_asset_id` for consistency with `asset_overview`, but is correct as written. |

No write method exists on this DAO at all — assigning or releasing custody has no code path anywhere in the application yet, only schema support.

## AssetDao — **Broken**

Every method selects or writes `value` and/or `status`, neither of which exists in the current `assets` table (renamed `purchase_value` and `condition_status` respectively — see [DESIGN.md](DESIGN.md) §3). `updateAsset` additionally takes an `AssetStatus` enum whose values (`AVAILABLE`, `ASSIGNED`, ...) no longer match the column's allowed values (`IN_SERVICE`, `DAMAGED`, ...).

| Method | Task | Fix needed |
|---|---|---|
| `Optional<Asset> findByAssetTagIgnoreCase(String assetTag)` | Case-insensitive asset-tag lookup | Rename `value`→`purchase_value`, `status`→`condition_status` in the `SELECT` list |
| `Optional<Asset> findById(Long id)` | Lookup by id, used by `AssetDetailBean` | Same |
| `List<Asset> findAll()` | Full asset list, filtered/paginated in Java by `AssetBean` | Same |
| `@Transactional Long createAsset(String assetTag, String name, Long categoryId, LocalDate purchaseDate, BigDecimal value, Long createdByUserId)` | Insert a new asset; called by `AssetFormBean.save()` | `INSERT` list needs `purchase_value` in place of `value`; hardcoded `status = AssetStatus.AVAILABLE.name()` must become `condition_status = AssetCondition.IN_SERVICE.name()` (a new enum, since `AVAILABLE` describes custody, not condition — see DESIGN.md §3) |
| `Boolean existsByAssetTagIgnoreCase(String assetTag)` | Duplicate-tag check on create | No column reference — actually clean, listed here for completeness |
| `Boolean existsByAssetTagIgnoreCaseAndIdNot(String assetTag, Long id)` | Duplicate-tag check on edit (edit flow does not exist yet) | Clean, same as above |
| `@Transactional void updateAsset(Long id, AssetStatus status, BigDecimal value)` | Update condition and value; **not called from anywhere** | Signature needs to change to an `AssetCondition` parameter; `SET status = ..., value = ...` needs to become `SET condition_status = ..., purchase_value = ...` |

## AssetDocumentDao — **Clean**

| Method | Task |
|---|---|
| `boolean existsByAssetId(Long assetId)` | Whether an asset has an associated document row. The only method on this DAO — no upload or retrieval path exists anywhere in the application. |

## AssetHistoryDao — **Dead table**

The `asset_history` table this DAO targets does not exist in the current schema at all — it was replaced by `activity_log` ([DESIGN.md](DESIGN.md) §7). Both methods will throw a SQL error on the first call, independent of the entity-mismatch problem elsewhere.

| Method | Task | Replacement |
|---|---|---|
| `List<AssetHistory> findByAssetIdOrderByActionDateDesc(Long assetId)` | Per-asset event history, newest first — currently unreachable from any view | A method reading `activity_log WHERE asset_id = :assetId ORDER BY occurred_at DESC` on a new DAO |
| `@Transactional void insert(Long assetId, HistoryAction action, Long performedByUserId, Long previousHolderId, Long newHolderId, AssetStatus previousStatus, AssetStatus newStatus, Long approvalId, String notes)` | Records one history row; called by `AssetEventRecorder.record()`, which nothing else calls | Superseded by an `activity_log` insert with the wider column set (`correlation_id`, `outcome`, `actor_roles`) — see DESIGN.md §7.1 |

This entire class should be retired once its replacement exists, not repaired in place.

## AuditLogDao — **Dead table**

The `audit_log` table this DAO targets does not exist in the current schema — replaced by `activity_log`. `log()` catches its own exception internally (originally a deliberate choice — see the decision recorded in ARCHITECTURE.md §8.3 to change this), so a failed write here does not currently surface as an error; `findRecent`/`countAll` have no such protection and throw directly.

| Method | Task | Replacement |
|---|---|---|
| `@Transactional void log(Long actorUserId, String action, String entityType, Long entityId, String description, String ipAddress)` | The single audit-write convention every mutation was meant to call. Called by `LoginAuditListener` (both success and failure) and `AssetFormBean.save()`. | An `activity_log` insert carrying `correlation_id` and `outcome` in addition to today's parameters |
| `List<AuditLogEntry> findRecent(int limit, int offset)` | Paginated recent-activity read for `/audit-log` | Equivalent read against `activity_log` |
| `long countAll()` | Total row count, for pagination | Equivalent read against `activity_log` |

Per the decision recorded in ARCHITECTURE.md §8.3, the replacement's `log()`-equivalent should **not** swallow its own exception — the write should join and be able to fail the caller's transaction, with the sole exception of the two `AUTH` events raised outside any transaction.

## DaoUtils — **Clean**

Package-private helper shared by the other DAOs, not part of the public data-access API.

| Method | Task |
|---|---|
| `static boolean exists(EntityManager entityManager, String sql, Map<String, Object> params)` | Runs a `SELECT COUNT(*)`-shaped query and returns whether the count is greater than zero |
| `static long getLastInsertId(EntityManager entityManager)` | `SELECT LAST_INSERT_ID()` — the standard way to retrieve an `AUTO_INCREMENT` id after a native-SQL `INSERT`, since native inserts don't populate the entity's id field the way `EntityManager.persist()` would |

## DepartmentDao — **Broken**

| Method | Task | Fix needed |
|---|---|---|
| `Boolean existsByNameIgnoreCase(String name)` | Duplicate-name check | Clean — no affected column |
| `List<Department> findAllDepartments()` | Full department list, feeds `LookupBean` and the department filter on `/user` | `SELECT id, name, enabled FROM departments` — `enabled` no longer exists (replaced by `closed_at`, a different type and meaning; see DESIGN.md §1 note on `closed_at`). Additionally has no `WHERE` filter today, so even once column names are fixed this method should add `WHERE closed_at IS NULL` for the general lookup case, while a separate historical-lookup path (resolving a user's own department, even if closed) should not filter. |

## RoleDao — **Clean**

| Method | Task |
|---|---|
| `Optional<Role> findByName(RoleName name)` | Single-role lookup |
| `Boolean existsByName(RoleName name)` | Existence check |
| `List<Role> findAllRoles()` | Full role list, feeds `LookupBean.getRoleOptions()` |

## UserDao — **Broken**

Every method that selects `password` is affected; the rest of the class (role resolution, filtering, pagination) is unaffected and can be left as-is.

| Method | Task | Fix needed |
|---|---|---|
| `Optional<User> findByEmailIgnoreCase(String email)` | The login lookup — used by `CustomUserDetailsService`. The single most important method in the class to fix first, since nothing else in the app works until login does. | `SELECT ... u.password ...` → `u.password_hash` |
| `List<Role> findRolesForUser(Long userId)` | Second-query role resolution (a native query can't join-fetch a `@ManyToMany` in one result set) | Clean, no affected column |
| `Boolean existsByEmailIgnoreCase(String email)` | Duplicate-email check | Clean |
| `Boolean existsByEmailIgnoreCaseAndIdNot(String email, Long userId)` | Duplicate-email check on edit | Clean |
| `Optional<User> findByIdAndRoleName(Long userId, RoleName roleName)` | Role-gated user lookup | Clean, no affected column |
| `List<User> findUsers(RoleName roleName, String search, Long deptId, Boolean enabled, int limit, int offset)` | The paginated, filtered directory search backing `/user` — the most complex query in the codebase, dynamically appends `WHERE` clauses via the private helpers `appendUserFilters`/`bindUserFilters` | `u.password` in the `SELECT` list → `u.password_hash` |
| `long countUsers(RoleName roleName, String search, Long departmentId, Boolean enabled)` | Matching count for the same filters, for pagination | Clean, no `password` reference |
| `Map<Long, Set<Role>> findRolesForUsers(List<Long> userList)` | Batch role resolution for a page of users — avoids one role query per row | Clean |
| `Optional<User> findById(Long id)` | Single-user lookup **deliberately without** the password column (used for display, not authentication) | Clean — this method's own `SELECT` never included `password`, unaffected |

## Summary

| DAO | Status | Blocking |
|---|---|---|
| `ApprovalDao` | Clean | — |
| `AssetCategoryDao` | Clean | — |
| `AssetCustodyDao` | Clean | — |
| `AssetDao` | Broken | 3 renamed/removed columns across every method |
| `AssetDocumentDao` | Clean | — |
| `AssetHistoryDao` | Dead table | Entire target table removed |
| `AuditLogDao` | Dead table | Entire target table removed |
| `DaoUtils` | Clean | — |
| `DepartmentDao` | Broken | 1 removed column |
| `RoleDao` | Clean | — |
| `UserDao` | Broken | 1 renamed column, affecting 2 of 9 methods |

Fixing `UserDao.findByEmailIgnoreCase` and the `User` entity together is the single highest-leverage fix — it is the only thing blocking the application from starting at all (ARCHITECTURE.md §0.1).
