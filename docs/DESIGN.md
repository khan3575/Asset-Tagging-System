# Data Model Design

This document explains *why* the schema in `src/main/resources/db/migration/V1__baseline_schema.sql` looks the way it does. [docs/ARCHITECTURE.md](ARCHITECTURE.md) describes how the application is built; this document explains the reasoning behind the data it's built on. Written 2026-08-17, superseding the schema described in `sql-schema/`.

## 1. What was wrong with the original schema

Nine specific defects motivated this redesign, each traceable to a concrete problem in the pre-redesign schema and code:

| # | Defect | Consequence |
|---|---|---|
| D1 | `assets.status` encoded two independent facts — physical condition and custody — in one column | An asset that is both damaged and held by someone could not be represented; one fact had to be discarded |
| D2 | `assets.enabled` duplicated `status = 'RETIRED'` | Two columns, one fact, no constraint keeping them in agreement |
| D3 | Custody was stored twice — in `assets.status = 'ASSIGNED'` and in `asset_custody` | No constraint kept the two in agreement |
| D4 | Every asset event was written to both `asset_history` and `audit_log` | One event, two rows, two tables, two schemas to maintain in lockstep |
| D5 | `approvals` had fixed `first_approver_id` / `final_approver_id` columns | The approver count was welded to exactly two, contradicting the `required_approval_count` column that claimed otherwise |
| D6 | No column recorded *who* rejected a request | A rejecting administrator was unrecoverable — a real accountability gap in an audit-focused system |
| D7 | Nothing prevented one administrator from filling both approver slots | Two-person sign-off could silently degrade to one person |
| D8 | `audit_log.created_at`, the sort column for every page of the log, had no index | Every page load sorted the entire table |
| D9 | `value` is a SQL reserved word | The entity had to escape it dialect-specifically (`"\"value\""`), which is fragile |

## 2. Four principles

Every decision below follows from one of these four rules.

**Principle 1 — One fact, one place.** No fact is stored in two columns. Where a value is derivable from another table, it is derived in a view, never copied. *(Resolves D2, D3.)*

**Principle 2 — Independent facts get independent columns.** Condition and custody vary independently, so they occupy separate axes. Two facts that can both be true at once must never share a column. *(Resolves D1.)*

**Principle 3 — State tables enforce; logs record.** If the database must *prevent* something, it needs a column and a constraint. A log is append-only and accepts every write by definition, so no invariant can be derived from one. This is why `asset_custody` and `approvals` remain ordinary constrained tables rather than being folded into the event log.

**Principle 4 — One event, one row, one table.** All activity — logins, asset events, approvals, user and department changes — lands in a single `activity_log`. Columns that are queried or joined stay typed and foreign-keyed; only the genuinely variable remainder goes in a `JSON` column. *(Resolves D4.)*

The test for "does this belong in a constrained table or in the log" is not importance — it is: **does the database need to reject a bad write, or just answer a question quickly?** Custody is checked on every asset page and must never permit two simultaneous holders; it gets a table. A department closing is written once and read rarely; it gets a log entry only, not a dedicated history table.

## 3. Assets — the two-axis split

The single most consequential change. `assets.condition_status` describes **the physical state of the object only** — `IN_SERVICE`, `DAMAGED`, `MAINTENANCE`, `UNUSABLE`, `RETIRED`. Whether anyone is holding it is not stored on this table at all; it is derived from `asset_custody`.

`AVAILABLE` and `ASSIGNED` — the two values that described custody in the old schema — are deliberately absent from `condition_status`. Neither was ever a condition. They return as a *computed* value from the `asset_overview` view (Section 6), so existing UI expectations of a single status string are unaffected once the Java layer is updated to read from it.

```
An asset can now be simultaneously:
  condition_status = 'DAMAGED'
  held by Sakib (a row in asset_custody with status = 'ACTIVE')

Under the old single-column model, writing 'DAMAGED' into assets.status
would silently overwrite 'ASSIGNED' — the system would lose track of who
was holding a broken asset. This is now structurally impossible.
```

`assets.enabled` was removed entirely (D2) — a retired asset is simply `condition_status = 'RETIRED'`.

## 4. Approvals — one row per decision

`approvals` no longer has `first_approver_id` / `final_approver_id`. A new table, `approval_actions`, holds one row per decision:

```sql
approval_actions (
    approval_id, actor_user_id, action,  -- APPROVED | REJECTED | CANCELLED
    sequence_no, notes, created_at
)
CONSTRAINT uq_aa_one_action_per_actor UNIQUE (approval_id, actor_user_id)
CONSTRAINT uq_aa_sequence             UNIQUE (approval_id, sequence_no)
```

This resolves D5, D6, and D7 together: the approver count is no longer welded to two (adding a third signature costs a row, not a column), a rejection is an ordinary row that happens to say `REJECTED` and carries a name and a reason, and the `UNIQUE (approval_id, actor_user_id)` constraint makes it a database error — not an application bug — for one administrator to sign the same request twice.

`approvals.status` is still stored as a column, even though it is technically derivable by counting `approval_actions` rows against `required_approval_count`. This is a deliberate, narrow exception to Principle 1: the approval queue is the most frequently loaded page in the workflow, and computing status per row on every load would need an aggregate subquery each time. The condition attached to this exception: **exactly one code path may write `approvals.status`**, immediately after inserting the corresponding `approval_actions` row, inside the same transaction.

`open_asset_id`, a generated column, gives "at most one open request per asset" the same database-enforced guarantee `asset_custody` already had for holders — see Section 5.

`requester_id` was made nullable: a `RETURN_REQUEST` has no incoming holder (the asset returns to inventory, held by nobody), and the old schema's `NOT NULL` forced the returning employee to be recorded as if they were receiving the asset.

## 5. Custody — unchanged, because it was already right

`asset_custody` keeps its original design almost exactly, because it already embodies Principle 3 better than anything else in the original schema:

```sql
active_asset_id BIGINT AS (CASE WHEN status = 'ACTIVE' THEN asset_id END) STORED,
CONSTRAINT uq_one_active_custody_per_asset UNIQUE (active_asset_id)
```

Since MySQL permits unlimited `NULL`s in a `UNIQUE` column but at most one of any real value, an asset may accumulate any number of historical (`RELEASED`) custody rows while **at most one live holder is physically representable**. A second concurrent assignment is not bad data — it is a rejected `INSERT`. `custodian_id` was made `NOT NULL` (a custody row without a custodian has no meaning), and `approvals.open_asset_id` reuses this exact technique for the same reason.

## 6. The read-model view

`assets` and `asset_custody` together answer "is this asset available," but neither answers it alone. `asset_overview` (Section 6 of ARCHITECTURE.md) joins them and recomputes the single-status reading the old schema had, via `active_asset_id` — never `asset_id + status = 'ACTIVE'` — specifically so the join inherits the same uniqueness guarantee rather than needing to reconstruct it.

It is a Flyway *repeatable* migration (`R__asset_overview_view.sql`), not a versioned one: a view holds no data, so `CREATE OR REPLACE VIEW` is naturally idempotent, and Flyway re-runs it automatically whenever the file's content changes. Editing it never requires a new migration file the way editing a table would.

## 7. The unified log, and why not a delimited string

The original proposal considered was a single packed column — `"asset_id|action_type|..."` — instead of separate `audit_log`/`asset_history` tables. The instinct (one log, not several) was adopted; the packed encoding was not, for concrete reasons:

| Packed string | Typed columns (what was built) |
|---|---|
| `WHERE data LIKE '%\|3\|%'` — full table scan, and also matches assets 13, 30, 130 | `WHERE asset_id = 3` — index seek |
| No foreign key possible; a bad holder id is never caught | Six foreign keys; a bad id is rejected at insert |
| Holder names need parsing in Java, then a query per row | One `JOIN users` resolves every name |
| A note containing `\|` corrupts the row | Free text lives in its own column |
| A new field means old rows have fewer segments than new ones, forever | A new nullable column leaves old rows valid |

`activity_log` therefore has typed, foreign-keyed columns for everything that is filtered or joined on (`asset_id`, `approval_id`, `subject_user_id`, both holder-id pairs, both condition-status pairs), and a `details JSON` column for the genuinely variable remainder. `details` is display and diagnostic data only — no business logic or application branch may ever read from it; anything a query needs to filter on has earned a real column instead.

### 7.1 Three columns added after the first draft

The first version of `activity_log` (documented as a proposal before implementation) recorded only successful, unrelated events. Three columns were added before the baseline was written, because none of the three can be added retrospectively — an event already written without them cannot be linked or classified after the fact:

- **`correlation_id` (`BINARY(16)`) + `sequence_in_action`** — one UUID minted per HTTP request (see `CorrelationFilter` in ARCHITECTURE.md Section 8), shared by every row that request produces, ordered by `sequence_in_action`. Without it, the two rows one approval click produces — a decision, then a custody transfer — are unrelated events with no way to reconstruct that they came from the same action.
- **`outcome` (`SUCCEEDED` / `DENIED` / `FAILED`) + `failure_reason`** — a refused action is now evidence, not silently absent. An employee repeatedly attempting to approve their own request previously left no trace at all; it now produces a `DENIED` row with a reason.
- **`actor_roles`** — a *snapshot* of the actor's roles at the moment of the action, deliberately denormalized rather than joined live from `user_role`. An audit record must describe what was observed, not a value that can drift underneath it — the same reasoning that makes an invoice copy the price at time of sale rather than joining to the current product price. Without this, an approval from an administrator later demoted would look unauthorized after the fact, and one from someone later promoted would look authorized when it was not.

`BINARY(16)` rather than a readable `CHAR(36)` UUID halves the index size at the cost of needing `HEX()`/`UNHEX(REPLACE(..., '-', ''))` at the query boundary — wrapped once in the DAO layer so no caller sees the encoding.

## 8. What was considered and deliberately not adopted

A separate, more ambitious design — a three-layer architecture (current state / temporal history / append-only ledger, with `user_role_period` and `user_department_period` tables, archive partitioning, and cryptographic seal-chaining for tamper evidence) — was explored for a scenario requiring full point-in-time reconstruction of the entire system and compliance-grade tamper evidence. **None of that was adopted.** What was adopted from that exploration is exactly the three columns in Section 7.1 — the two decisions were treated as separable, and only the narrow, retrofit-now-or-never one was taken. If a future requirement genuinely needs full point-in-time reconstruction beyond what `asset_custody`'s date range already provides, or provable log immutability, that exploration is the starting point, not this document.

## 9. Known limitations of this design

Stated plainly, so they are a decision rather than a surprise later:

- **No enforced immutability.** `activity_log` can still be `UPDATE`d or `DELETE`d — nothing prevents it at the database level yet. Triggers plus a restricted grant (application user has `INSERT`/`SELECT` only) would close this and can be added at any time without losing history.
- **Point-in-time reconstruction is uneven across dimensions.** `asset_custody` genuinely supports "who held this on date X" via a direct range query. Condition and department history do not have an equivalent table — reconstructing them means replaying `activity_log` events, which is fine for one asset and too slow for a report across the whole register.
- **`details` JSON is unindexed and unenforced.** By convention only, it must never carry anything a query filters on or any value application logic branches on.
- **Nothing prevents a native-SQL DAO from bypassing the log entirely.** Unlike JPA-managed writes (which Hibernate Envers could intercept automatically), a native `UPDATE` statement can be issued from anywhere. The convention that must be followed — and is not yet enforced by tooling — is that no DAO exposes a public mutating method outside `AssetEventRecorder`.
