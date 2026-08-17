# Development Plan — Asset Tagging System (JSF track)

_Last updated: 2026-08-17. This is a personal working doc (gitignored — see `.gitignore`), not a project deliverable. Update it as steps complete or plans change._

**Rewritten 2026-08-17.** The previous version of this document predated a full schema redesign and no longer matched the codebase — its old Step 2 (`audit_log`) and Step 7 approval design (`first_approver_id`/`final_approver_id`) were built on tables and columns that were removed. Rather than keep flagging drift, this rewrite removes what's superseded and replaces it with the current task list. The old version, if ever needed, is in git history for this file locally, and its still-useful debugging lessons are preserved in Appendix A below.

## How to use this doc

Each step below has **What** (the concrete deliverable), **Why** (the reasoning — so you can judge edge cases yourself instead of following blindly), **How** (concrete approach, method signatures, SQL sketches), and **Done when** (a checkable finish line). Steps are meant to be done roughly in order — later steps assume earlier ones work — but see "Where things stand" for what's actually next.

This doc doesn't include full Java implementations. It gives you the shape and the SQL/logic sketch; you write the actual classes. Design rationale (*why* the schema looks the way it does) lives in [docs/DESIGN.md](DESIGN.md), not here — this doc references it rather than re-deriving it, so the two don't drift out of sync with each other the way this file drifted from the code once before.

---

## Where things stand right now

- **2026-08-17: full two-axis schema redesign + Flyway adoption**, done in one session. `assets` no longer conflates condition and custody; `approvals`' fixed approver columns became a proper `approval_actions` table; `audit_log` + `asset_history` merged into one `activity_log` table with correlation ids and recordable refusals. Full rationale: [docs/DESIGN.md](DESIGN.md). Schema is now Flyway-managed (`src/main/resources/db/migration/`), not hand-run SQL scripts.
- **The application does not currently start.** `ddl-auto=validate` rejects four entities (`User`, `Department`, `Asset`, `Approval`) whose fields no longer match the schema, verified column-by-column. This is the single most urgent item — see Step 3.5 below. Not a hypothetical risk; confirmed by reading both the entities and the migration side by side.
- **Compact status of everything built before the redesign:**
  - Steps 0 and 1 — done, fully unaffected by the redesign (routing and DI concerns, no schema involvement).
  - Step 2 (old) — the `audit_log`/`AuditLogDao` mechanism it built was fully working end-to-end as of 2026-08-06. It is now dead code — `audit_log` doesn't exist anymore. Replaced below by a new Step 2 targeting `activity_log`.
  - Step 3 — `RoleDao`/`DepartmentDao`/`UserDao` done; `CustomUserDetailsService` swapped over; login was confirmed working at the time. Two of these DAOs now need a column-rename fix (Step 3.5) — the *approach* wasn't wrong, the column names underneath it changed.
  - Page chrome (header/sidebar) — done, unaffected.
  - Step 4 — list/detail done for the User directory; create/update never started.
  - Step 7.1–7.2d (Asset category lookup, asset list, asset create, asset detail skeleton) — done, needs the same column-rename fix as Step 3.
  - Step 7's approval/transfer design (the old `first_approver_id`/`final_approver_id` state machine and its five-page UI spec) — removed from this document entirely, replaced below with a sketch against the current schema.
- **Full current status, kept accurate going forward, lives in the repo's committed docs — treat these as more current than this file if they ever disagree:** [docs/ARCHITECTURE.md](ARCHITECTURE.md) §0/§12, [docs/SITE_MAP.md](SITE_MAP.md), [docs/DAO_REFERENCE.md](DAO_REFERENCE.md).

---

## Guiding principles

- **Raw SQL via `EntityManager.createNativeQuery`, not JPQL/derived queries.** Hard project requirement (matches the legacy JSF system this is internship practice for), not a style preference. Unaffected by the redesign.
- **`@Entity` classes are for ORM mapping only, never queried through `JpaRepository`.** This still holds even though Step 3.5 below requires editing four entities' fields — renaming a column mapping is mapping work, not a switch to Spring Data queries.
- **Every mutating action logs itself, from now on** — via the unified `activity_log` (not the old `audit_log`). See the new Step 2.
- **Employee first, in full, then replicate.** Already exercised once (User directory before Asset); the same instinct applies again for the approval workflow — get one request type working end-to-end before generalizing to all three (`ASSET_REQUEST`/`TRANSFER_REQUEST`/`RETURN_REQUEST`).

---

## Step 0 — Clean URL for the login page

**DONE (2026-07-28), unaffected by the redesign.** `LoginViewController` forwards `/login` → `/login.xhtml`; `SecurityConfig` and `login.xhtml`'s form both point at `/login`, not `/login.xhtml`. Verified: `/login`, a wrong password (`/login?error`), and logout (`/login?logout`) all use clean URLs.

---

## Step 1 — Prove a JSF bean can reach a Spring service

**DONE (2026-07-28), unaffected by the redesign.** Confirmed: a `@Named @RequestScoped` bean can `@Inject` a Spring `@Service` directly — no `SpringBeanFacesELResolver` or `WebApplicationContextUtils` bridge needed in this stack (Spring Boot 4.1 + JoinFaces 6.1 + Weld). Use plain `@Inject` for every managed bean going forward. The throwaway spike files used to confirm this were deleted once the answer was known.

---

## Step 2 — Unified activity log

### What

A DAO and write path against `activity_log` (schema already exists, Flyway-managed), replacing the now-dead `AuditLogDao`/`AssetHistoryDao` pair. Plus the login `@EventListener` and the `/audit-log` viewing panel, both rewired from the old table to the new one.

### Why

This remains the actual priority of the project — see the standing instruction that logging is the key deliverable, not a byproduct of CRUD pages. What changed is the shape: one table instead of two, with three columns (`correlation_id`, `outcome`, `actor_roles`) that make the log answerable in ways the old design couldn't be — full reasoning in [docs/DESIGN.md](DESIGN.md) §7. Those three columns specifically cannot be added after the fact without losing history on every row written before they existed, which is why they're worth getting into the very first version of the new DAO rather than bolted on later.

### How

Read [docs/DESIGN.md](DESIGN.md) §7 for the table shape and the full column list — not repeated here to avoid the two documents drifting apart again. Concretely:

1. A new `ActivityLogDao` (or similar name) — `log(...)` taking at minimum actor, action, entity type, and the relevant subject id(s); `findRecent(limit, offset)`; `countAll()`. Shape mirrors the old `AuditLogDao` closely enough to reuse its `findRecent`/`countAll` pattern — the main difference is the extra columns and that `correlation_id` needs to come from somewhere (see point 3).
2. **Do not swallow the write's exception.** The old `AuditLogDao.log()` caught its own exception to protect login from an unrelated audit-write failure — reasonable at the time, but the decision this session (see [docs/ARCHITECTURE.md](ARCHITECTURE.md) §8.3) is that every *other* mutation's log write should join the same transaction as the business action and fail it if the write fails. Keep the swallow-and-log-only behavior for the two `AUTH` events specifically (`LoginAuditListener` has no business transaction to join); remove it everywhere else once other mutations start calling this DAO.
3. `correlation_id` needs a value from somewhere per request — a servlet filter minting one UUID per request (sketch in DESIGN.md §7.1) is the intended mechanism. Worth building this filter as part of this step rather than deferring it, since retrofitting it once several call sites already exist means updating all of them at once instead of one filter.
4. Rewire `LoginAuditListener` to call the new DAO instead of the old one.
5. Rewire `AuditLogBean`/`audit-log.xhtml` to read from the new DAO. Column set on the page will need to grow slightly (`outcome` at minimum is worth surfacing — a `DENIED` row is exactly the kind of thing this panel exists to show).
6. `AssetEventRecorder` (in `service/`) is the intended single write path for every future asset mutation — needs a full rewrite against `activity_log`, not a port from its current `asset_history`+`audit_log` version. Nothing calls it yet either way.

### Done when

- A real mutation (once one exists past asset-create — see Step 3.5/Step 7) produces a row in `activity_log` with a `correlation_id` shared by every row that same action produced.
- Login success/failure still work and still produce rows, now in `activity_log`.
- `/audit-log` renders real entries from the new table.
- The write-fails-the-transaction behavior is confirmed for at least one non-auth mutation (deliberately break a write and confirm the whole operation rolls back, not just the log row).

---

## Step 3 — Raw-SQL DAOs for User, Department, Role

**DONE (2026-07-29 through 2026-08-02)**, subject to Step 3.5's fix below. `RoleDao`, `DepartmentDao`, `UserDao` all exist using `EntityManager.createNativeQuery`; `CustomUserDetailsService` uses `UserDao`; the old `UserRepository`/`DepartmentRepository`/`RoleRepository` Spring Data interfaces are fully deleted (confirmed, no trace remains).

**Bugs caught and fixed during original review, worth remembering for any new DAO code (Step 7 in particular):**
- `entityManager.createNativeQuery(sql).getResultList()` never returns `null` — always check `.isEmpty()`.
- A join's `ON` clause needs a genuine equality comparison — a missing or malformed condition (e.g. `ON ur.user_id - u.id`, a typo for `=`) can silently produce wrong results rather than an error, since MySQL evaluates the arithmetic and treats non-zero as true.
- `...IgnoreCase` methods need `LOWER()` on **both** sides of the comparison explicitly — don't rely on the database's collation happening to be case-insensitive.
- Don't mix `createNativeQuery(sql, Entity.class)` (auto-mapping) with manual `Object[]` row mapping in the same method — pick one.
- Cast numeric native-query columns via `((Number) row[i]).longValue()`, not a direct `(Long)` cast — the JDBC driver's actual runtime type isn't guaranteed.
- Every nullable column needs an explicit null check before casting (`row[i] == null ? null : ...`).

---

## Step 3.5 — Schema realignment *(new, do this next — blocks everything else)*

### What

Bring `model/` and `dao/` back in sync with the redesigned schema. This is not new functionality — it is the reason the application currently cannot start at all.

### Why

The schema redesign (2026-08-17) renamed or removed columns that four entities and several DAOs still reference by their old names. `ddl-auto=validate` catches this at startup, before a single request can be served. Full verified list, checked column-by-column against `V1__baseline_schema.sql`: [docs/ARCHITECTURE.md](ARCHITECTURE.md) §0, [docs/DAO_REFERENCE.md](DAO_REFERENCE.md).

### How

1. **`User.java`** — `password` → `password_hash`.
2. **`Department.java`** — remove `enabled` (`Boolean`); add `closedAt` (`LocalDateTime`, nullable) mapped to `closed_at`.
3. **`Asset.java`** — `value` → `purchaseValue` (`"purchase_value"`, precision `(12,2)`); `status` → `conditionStatus`, backed by a **new enum** `AssetCondition` (`IN_SERVICE`, `DAMAGED`, `MAINTENANCE`, `UNUSABLE`, `RETIRED` — not the old `AssetStatus`, which mixed condition and custody); remove `enabled` entirely.
4. **`Approval.java`** — remove `firstApprover`, `finalApprover`, `firstApproverNotes`, `finalApproverNotes`, `rejectionReason`, `firstActionDate`, `finalActionDate`, `cancelledAt`; rename `requestDate` → `requestedAt`; add `closedAt`.
5. **New entity/DTO**: `ApprovalAction`, mapping `approval_actions` — needed once Step 7's approval workflow starts, not strictly required just to get the app booting, but do it here while everything else in `model/` is already open.
6. **`AssetDao`** — every `SELECT`/`INSERT`/`UPDATE` needs `value`→`purchase_value`, `status`→`condition_status`. `updateAsset`'s `AssetStatus` parameter becomes `AssetCondition`.
7. **`UserDao`** — `findByEmailIgnoreCase` and `findUsers` both select `u.password` — rename to `u.password_hash`. This is the highest-leverage single fix: nothing else in the app is reachable until login works.
8. **`DepartmentDao.findAllDepartments()`** — currently `SELECT id, name, enabled FROM departments` with no filter. Rename to read `closed_at`, and add `WHERE closed_at IS NULL` for the general-lookup case (feeding dropdowns) — a separate historical-lookup path (resolving a user's own department even if it's since closed) should not filter.
9. **Retire `AuditLogDao` and `AssetHistoryDao` entirely** — both target tables that no longer exist. Don't patch them; they're superseded by Step 2's new DAO.
10. Update the beans/views that touch the renamed fields: `AssetBean`, `AssetDetailBean`, `AssetFormBean`, `asset-list.xhtml`, `asset-view.xhtml`, `add-asset.xhtml` (all reference `asset.status`/`asset.value` or bind form fields to the old names).

### Done when

- The application starts cleanly under the `local` profile with no `ddl-auto=validate` error.
- Login works end-to-end in the browser.
- `/user`, `/assets`, `/assets/new` all render without a SQL error.
- `/audit-log` either works against the new Step 2 DAO, or is temporarily hidden if Step 2 isn't done yet — don't leave it pointing at the deleted `AuditLogDao`.

---

## Step 4 — Employee CRUD as Facelets pages

**Partially done.** List and detail pages exist and work (once Step 3.5 lands). Create/update do not exist — `user-detail.xhtml` has an edit-mode UI already but no `@PostMapping` behind it.

### Remaining

- [ ] Create form (if genuinely needed — user creation may end up admin-only and low priority relative to the approval workflow; re-confirm before building)
- [ ] Edit form: `h:form`/`h:inputText`/validation/`h:message`, wired to a real `POST /user/{id}` handler, calling the new Step 2 activity log on success (not the old `AuditLogDao.log(...)`)

### Done when

Both forms work end-to-end in the browser and each successful save produces an `activity_log` row.

---

## Step 5 — Dashboard (minimal)

**Not started, unaffected by the redesign.** `DashboardBean` is an empty class; `dashboard.xhtml` renders a heading and nothing else.

### Done when

Logging in redirects to a dashboard that renders without error. Minimal content is fine — expand only if the project calls for it later (e.g. a summary tile linking to `/audit-log`).

---

## Step 6 — Raw JS, as needed

**Ongoing, unaffected by the redesign.** Plain JavaScript woven into forms as a specific need comes up (confirm-before-delete, inline validation feedback), not built speculatively ahead of time. No fixed finish line.

---

## Step 7 — Asset domain: custody and approvals

### 7.1–7.2d — DONE, needs the Step 3.5 fix

`AssetCategoryDao`, `AssetDao` (list/create/find), `AssetBean`, `AssetController`, `asset-list.xhtml`, `AssetFormBean`, `add-asset.xhtml`, and the `AssetDetailBean`/`asset-view.xhtml` skeleton (three read-only fields) all exist and were verified working before the redesign. All need the column renames from Step 3.5; none need new design.

One decision already made and worth keeping: **Asset search/pagination loads everything via `findAll()` and filters in Java**, not DB-side `LIMIT`/`OFFSET` per filter like the Employee pattern — deliberate, given realistic data size (2–5k rows). Don't revert to DB-side pagination for Assets without a reason.

### 7.3 — Custody and approval workflow *(redesigned against the current schema — replaces everything previously here)*

The previous version of this document had a full page-by-page UI spec and state machine here, built on `approvals.first_approver_id`/`final_approver_id`. Those columns don't exist anymore — see [docs/DESIGN.md](DESIGN.md) §4 for why (`approval_actions`, one row per decision, database-enforced against double sign-off). The design below is a fresh sketch against the current schema, not a port of the old one — several specifics (self-approval guard, whether execution happens inside the approval click) need re-deciding, not assuming.

**Still valid from the old design (business rules, not schema):**
- `ROLE_ADMIN` initiates transfers, approves/rejects, sees current holders. `ROLE_EMPLOYEE` submits self-requests only and never sees who holds what. Gate on `HeaderBean.getRole()`, same mechanism as before.
- A holder change always goes through an approval request; a condition/value edit does not.
- `/approvals` (admin queue) and `/approvals/{id}` (act on one request) are both necessary — there's no other way to reach "approve a request."

**What's genuinely new and needs deciding fresh, not assumed from the old design:**
- Whether an admin-initiated transfer still auto-counts as the first approval (the old design's `first_approver_id = initiator` prefill has no direct equivalent under `approval_actions` — it would mean inserting an `approval_actions` row for the initiator at creation time, which is a deliberate choice to make, not a given).
- Self-approval prevention no longer needs an application-level guard written by hand — `approval_actions`'s `UNIQUE (approval_id, actor_user_id)` constraint makes a second signature from the same person a database error. The UI should still hide the button for a clean experience, but the enforcement is now structural.
- Whether the final approval click executes the custody transfer in the same request (as the old design assumed, since there's no background worker) — still the simplest option, but worth a fresh look now that `approval_actions` makes partial-completion states easier to represent if ever needed.

**Concrete pieces needed, whatever the above decisions land on:**
- `AssetCustodyDao`: `releaseActiveCustody(assetId, endTime)`, `transferCustody(assetId, newCustodianId, approvalId, assignedByUserId)` (releases old, inserts new — order matters, see DESIGN.md §5's note on why).
- `ApprovalDao`: a `recordAction(approvalId, actorUserId, action, notes)` method writing to `approval_actions`, plus recomputing/writing `approvals.status` in the same call (the one place allowed to write it — see DESIGN.md §4's note on this denormalization). `createTransferRequest` already exists and is correct against the current schema.
- New pages: `/assets/{id}/transfer` (admin), `/assets/{id}/request` (employee), `/approvals` (queue), `/approvals/{id}` (act). None exist yet — see [docs/SITE_MAP.md](SITE_MAP.md) §2 for the current list of dead links this would resolve.
- `AssetDao.updateCondition(id, AssetCondition)` — separate from a value edit, same reasoning as the old design (different side effects: moving into `DAMAGED`/`MAINTENANCE` should force-release active custody).

### 7.4 — Disable/retire an asset

Set `condition_status = RETIRED` (no separate `enabled` flag anymore — see DESIGN.md §3), force-release any active custody. Whether retired assets stay visible (greyed out) in `/assets` or get filtered out is still an open call — no existing precedent to copy.

### 7.5 — Server-side validation beyond duplicate-tag

`add-asset.xhtml` currently only has client-side `required="true"` (bypassable via a direct POST) and the duplicate-tag check. Decide and enforce real business rules (`purchase_value` positive, `purchase_date` not in the future) with a `FacesMessage`.

### 7.6 — Document upload (`AssetDocument`)

**Not started, unaffected by the redesign** — `asset_documents` is unchanged by the schema work. Its own small spike: needs `multipart/form-data` + `h:inputFile`/`Part`, plus a plain Spring MVC streaming endpoint for download, since JSF can't render binary through EL directly.

### Explicitly deferred

`RETURN_REQUEST` — the schema now supports it cleanly (`requester_id` is nullable specifically for this case, `previous_holder_id` names the person returning the asset — see DESIGN.md §4), but it's not part of the confirmed near-term scope. Build it once `TRANSFER_REQUEST`/`ASSET_REQUEST` are working end-to-end, reusing the same `approval_actions` machinery.

---

## Appendix A — Gotchas already learned (don't rediscover these)

- **Facelets caches parsed views.** `.xhtml` edits won't show up without a restart unless `joinfaces.faces.project-stage=Development` is set (already in `application.properties`).
- **Firefox parses JSF pages as strict XML**; Chrome doesn't. A malformed tag anywhere on the page produces a raw XML parser error in Firefox and a normal degraded render in Chrome. If a page looks fine in Chrome but shows a parse-error dump in Firefox, that's why.
- **CDN `integrity`/`crossorigin` (SRI) attributes silently block the whole resource if the hash doesn't match** — no visible network error, just a Console warning, and the page renders unstyled.
- **Any Java-side redirect/URL pointing at a Facelets page needs the real `.xhtml` extension** — no JSP-style clean-URL forwarding for Facelets.
- **`h:` component tags use `styleClass`, not `class`**; plain HTML tags use normal `class`. Both exist on the same page since Bootstrap CDN tags are plain HTML.
- **Seed data drift**: the local database can hold different data than `src/main/resources/db/seed/V1000__dev_seed_data.sql` currently describes, if the seed file was edited after Flyway last ran it locally — Flyway won't silently re-run an already-applied `V`-versioned migration just because the file changed (that's what `R`-repeatable migrations are for, and seed data isn't one). If a known-good account "doesn't work," check what's actually in the `users` table before assuming the code is broken, and recreate the local database (drop + recreate, `docs/SETUP.md` §2) if the seed file itself changed.
- **The page-view URL and the form-submission URL are two separate things.** Making `/login` render the page does not automatically make the login form submit there — the `<form action="...">` attribute has to be updated independently.
- **Native-query SQL written as a `"""` text block must not have a trailing semicolon inside it** — a common cause of a SQL syntax error when Hibernate sends the string through JDBC as a single statement.
- **Every nullable column needs an explicit null check before casting**, when manually mapping a native query's `Object[]` row back into a Java object.
- **MySQL DDL auto-commits.** A Flyway migration that fails partway through leaves everything before the failure permanently applied — there's no transactional rollback the way a JPQL/Hibernate operation might have. Keep post-baseline migrations small (one logical change each) specifically because of this.

## Appendix B — Explicitly deferred or cut (and why)

- **JSP + JSTL pages**: deferred, not cancelled. Current decision (2026-07-27) is JSF-only for now. Don't add JSTL (`c:forEach` etc.) to any page until that changes.
- **Phase 6 — hand-writing one legacy JSF-1.x XML-config managed bean**: cut from the active plan (2026-07-28) given the speed priority. Pure internship-prep practice value, no functional value to the app. Worth revisiting on slack time, or better, learning it directly on the real internship codebase.
- **Role-based HTTP authorization**: `SecurityConfig` currently only requires authentication, no role rules (`@EnableMethodSecurity` is on but nothing uses `@PreAuthorize` anywhere). Deliberately deferred. When it happens: `authorizeHttpRequests` is first-match-wins, so role-specific rules must be added *before* any catch-all `authenticated()`/`permitAll()`.
