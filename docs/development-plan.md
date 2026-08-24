# Development Plan — Asset Tagging System (JSF track)

_Last updated: 2026-08-24. This is a personal working doc (gitignored — see `.gitignore`), not a project deliverable. Update it as steps complete or plans change._

**2026-08-24 note.** The 2026-08-20 architecture audit produced a fine-grained, verified task list — the **Refactoring Roadmap** artifact (Stage 0 through Stage 9, ~40 tasks, each naming the exact file/class/method to touch) — that is now more precise than this doc's remaining Steps and should be treated as authoritative for *how* to do the JSF-view and service-layer work. This doc still tracks the overall step list and the parts of the app the roadmap doesn't cover (approval workflow design, etc.), but where the two disagree on a task already covered by the roadmap, follow the roadmap. It isn't checked into the repo — link: `https://claude.ai/code/artifact/232b922d-6b71-49f1-9f72-8f14979bf094`.

**Rewritten 2026-08-17.** The previous version of this document predated a full schema redesign and no longer matched the codebase — its old Step 2 (`audit_log`) and Step 7 approval design (`first_approver_id`/`final_approver_id`) were built on tables and columns that were removed. Rather than keep flagging drift, this rewrite removes what's superseded and replaces it with the current task list. The old version, if ever needed, is in git history for this file locally, and its still-useful debugging lessons are preserved in Appendix A below.

## How to use this doc

Each step below has **What** (the concrete deliverable), **Why** (the reasoning — so you can judge edge cases yourself instead of following blindly), **How** (concrete approach, method signatures, SQL sketches), and **Done when** (a checkable finish line). Steps are meant to be done roughly in order — later steps assume earlier ones work — but see "Where things stand" for what's actually next.

This doc doesn't include full Java implementations. It gives you the shape and the SQL/logic sketch; you write the actual classes. Design rationale (*why* the schema looks the way it does) lives in [docs/DESIGN.md](DESIGN.md), not here — this doc references it rather than re-deriving it, so the two don't drift out of sync with each other the way this file drifted from the code once before.

**Once a step or phase is verified done, compress it.** Replace its full What/Why/How instructions with a short dated note — what shipped, and a pointer to the file(s) if a future reader needs the detail. This is why Steps 0 and 1 below are two sentences each instead of a page: they were once as detailed as Step 3.5 currently is, and got compressed the moment they were confirmed working. The point is that this file should always be readable top-to-bottom as "what's actually left," not an ever-growing archive of instructions for things that already happened — that archive already exists, in git history and in this file's own 2026-08-17 rewrite note above. Verify before compressing (a clean build, not just "looks right") — a compressed step is a claim that it's really done, not a hope.

---

## Where things stand right now

- **2026-08-17: full two-axis schema redesign + Flyway adoption.** `assets` no longer conflates condition and custody; `approvals`' fixed approver columns became a proper `approval_actions` table; `audit_log` + `asset_history` merged into one `activity_log` table with correlation ids and recordable refusals. Full rationale: [docs/DESIGN.md](DESIGN.md). Schema is Flyway-managed (`src/main/resources/db/migration/`), not hand-run SQL scripts.
- **2026-08-19: enum pass.** Every enumerated value now has a Java enum, four constants were renamed to match the redesigned schema, and `EntityType` became `ActivityEntityType`. Full list, rationale, and the rules for adding values: [docs/ENUM_REFERENCE.md](ENUM_REFERENCE.md). **This edits `V1__baseline_schema.sql` in place, so the local database must be recreated** ([docs/SETUP.md](SETUP.md) §2) before the app will start.
- **2026-08-18: the application runs again.** Step 3.5 (schema realignment) is done — see its entry below. Verified for real: clean startup under the `local` profile, no `ddl-auto=validate` error, `/login` returns `200`, `/assets`/`/user` correctly `302`-redirect when unauthenticated.
- **2026-08-24: templating refactor done (roadmap Stage 4, all of T4.1–T4.6).** `WEB-INF/templates/base.xhtml` and `main.xhtml` exist; `header.xhtml`/`sidebar.xhtml` moved to `WEB-INF/fragments/`; all 8 pages (`dashboard`, `asset-list`, `add-asset`, `asset-view`, `user-list`, `user-detail`, `audit-log`, `login`) converted to `ui:composition` against them, Bootstrap CDN links now live in `base.xhtml` only instead of pasted per-page. Filenames are unchanged (that's Stage 8, not this). Verified: clean `./mvnw -o compile`, and the app runs and renders through this chrome (see the `local`-profile launch note below). Known, deliberate loose end: the sidebar's Assets/Users/Settings links use `h:link outcome=` values that don't match any current filename (`/assets`, `/user`, `/settings` vs. the real `asset-list.xhtml`/`user-list.xhtml`/nonexistent), so they render without an `href` and log a "no matching navigation case" warning — this is fixed by Stage 8's file rename, not before.
- **Launching the app:** `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` — the `local` profile is what supplies `DB_USER`/`DB_PASS`/`DB_NAME` from `application-local.properties`; omitting it (or a typo in the flag name) leaves those as unresolved `${...}` placeholders and MySQL rejects the literal string as a username.
- **What's next, in the order the Refactoring Roadmap recommends:** Stage 0 (five small correctness fixes, all still outstanding — T0.1 fixes the blank `/audit-log` page's bean-name + field-name mismatch, T0.2 guards `LoginAuditListener`'s log write, T0.3 null-guards `correlationId` in `ActivityLogDao`, T0.4 removes a duplicated `sessionManagement(...)` block in `SecurityConfig`, T0.5 renames "Audit Log" to "Activity Log" in the UI), then T1.1 (prove `@ViewScoped` works in this Weld+Spring stack — blocking for Stage 7), then Stage 2 (the service layer, built on the asset slice — this is also where asset-registration's activity-log write belongs, *not* a standalone patch to `AssetFormBean`; see the note on old Step 2 item 4 below). This supersedes the previous "Step 2 → Step 4 → Step 7" ordering below, which predates the roadmap.
- **Compact status of everything built before the redesign:**
  - Steps 0, 1, 3.5 — done.
  - Step 2 (old) — the `audit_log`/`AuditLogDao` mechanism was fully working end-to-end as of 2026-08-06, then deleted in Step 3.5 Phase C as part of the redesign. Replaced below by a new Step 2 targeting `activity_log` — see its corrected checklist status below; largely done, two real gaps remain and both are now tracked as roadmap tasks.
  - Page chrome (header/sidebar) — done; rebuilt into `base.xhtml`/`main.xhtml`/`fragments/` 2026-08-24 (see above).
  - Step 4 — list/detail done for the User directory; create/update never started.
  - Step 7.1–7.2d (Asset category lookup, asset list, asset create, asset detail skeleton) — done, realigned to the new schema in Step 3.5.
  - Step 7's approval/transfer workflow itself — not started; sketch in Step 7 below, against the current `approval_actions` schema, not the old removed one. Out of scope for the roadmap until its Stage 2 (service layer) lands — see the roadmap's §4.
- **`docs/SITE_MAP.md` and `docs/DAO_REFERENCE.md`, cited below and in Step 7, were deliberately deleted in the 2026-08-20 docs consolidation** (they were per-item status tables that went stale faster than they could be maintained — the exact failure this doc's own compression rule exists to avoid). Don't recreate them; the Refactoring Roadmap artifact linked above is their replacement for task-level status. [docs/ARCHITECTURE.md](ARCHITECTURE.md) §9's Implementation Status table is the current source for component-level status.

---

## Guiding principles

- **Raw SQL via `EntityManager.createNativeQuery`, not JPQL/derived queries.** Hard project requirement (matches the legacy JSF system this is internship practice for), not a style preference. Unaffected by the redesign.
- **`@Entity` classes are for ORM mapping only, never queried through `JpaRepository`.** This still holds even though Step 3.5 below requires editing four entities' fields — renaming a column mapping is mapping work, not a switch to Spring Data queries.
- **Every mutating action logs itself, from now on** — via the unified `activity_log` (not the old `audit_log`). See the new Step 2.
- **Employee first, in full, then replicate.** Already exercised once (User directory before Asset); the same instinct applies again for the approval workflow — get one request type working end-to-end before generalizing to all three (`ASSIGNMENT`/`TRANSFER`/`RETURN`).

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

- [x] 1a. `ActivityLogDao.log(...)` — done and verified (2026-08-18): correct column/parameter count, `UUID_TO_BIN(:correlationId)`, no swallowed exception.
- [x] 1b. `ActivityLogDao.findRecent(limit, offset)` and `countAll()` — **done** (this item was stale — both exist and are used by `ActivityLogBean`). Remaining gap isn't here, it's `correlationId` having no null guard before `.toString()` — roadmap T0.3.
- [x] 2. **Don't swallow the write's exception** — done, as part of 1a. The old `AuditLogDao.log()` caught its own exception to protect login from an unrelated audit-write failure — reasonable at the time, but the decision this session (see [docs/ARCHITECTURE.md](ARCHITECTURE.md) §8.3) is that every *other* mutation's log write should join the same transaction as the business action and fail it if the write fails. Keep the swallow-and-log-only behavior for the two `AUTH` events specifically (`LoginAuditListener` has no business transaction to join).
- [x] 3. `CorrelationFilter` — **done** (this item was stale). `security/CorrelationFilter.java` exists: a `@Component Filter` at `Ordered.HIGHEST_PRECEDENCE`, minting one UUID per request into a `ThreadLocal`, auto-registered by Spring Boot. Not yet done: the swallow-and-log-only behavior item 2 above promises isn't actually implemented — `LoginAuditListener`'s two `.log(...)` calls are unguarded, so a log-write failure currently *would* abort sign-in. That's roadmap T0.2.
- [x] 4a. `LoginAuditListener` — **done**. Both `onSuccess`/`onFailure` inject `ActivityLogDao`, build a real `ActivityLog` via its builder, and call `.log(act)`. (Needs T0.2's try/catch per above, and T0.3's null guard on `correlationId` — both small, both outstanding.)
- [ ] 4b. `AssetFormBean` — **not done, and don't just uncomment the old call.** The dead comment is still sitting in `AssetFormBean.save()`. Per [ARCHITECTURE.md](ARCHITECTURE.md) §9, the service layer doesn't exist yet — `@Transactional` isn't on anything, transactions currently span one DAO call, not one user action. Wiring `ActivityLogDao` directly into `AssetFormBean` now would violate the "log write joins the business transaction" rule (item 2) since there's no transaction to join. **This is roadmap Stage 2 (T2.1–T2.4)'s job**, not a standalone fix: create `AssetService`, move the transaction boundary onto it, give it a `register(...)` method that does the duplicate-tag check, the insert, and the log write in one `@Transactional` method, then point `AssetFormBean` at the service instead of `AssetDao` directly.
- [x] 5a. `ActivityLogBean` — **done** (this item was stale). `bean/ActivityLogBean.java` exists: `@Named @RequestScoped`, real `@PostConstruct` using `PageParams`, and an `entityIdOf(entry)` helper that picks whichever of `assetId`/`approvalId`/`subjectUserId` is set.
- [ ] 5b. `audit-log.xhtml` — **not done; this is the one actually blank-rendering bug.** The page was converted to the new `ui:composition` template (2026-08-24) but its bindings were never updated to match `ActivityLogBean`: still reads `#{auditLogBean...}` (should be `#{activityLogBean...}`) and three stale field names — `entry.createdAt`→`entry.occurredAt`, `entry.description`→`entry.summary`, `##{entry.entityId}`→`##{activityLogBean.entityIdOf(entry)}`. This is roadmap T0.1, a find-and-replace in one file. Verify by signing in and confirming the 19 existing rows appear.
- [ ] 6. **Optional, skip for now.** `AssetEventRecorder` was deleted in Phase C too (it depended on the two dead DAOs and was already dead code — nothing called it). If you still want one shared write path for every future asset mutation, that's a fresh class, not a rewrite of the old one.

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

## Step 3.5 — Schema realignment — **DONE, verified 2026-08-18**

`model`/`dao` brought back in sync with the two-axis schema (Phases A–F: new `AssetCondition` enum, renamed/removed fields on `User`/`Department`/`Asset`/`Approval`, eight dead files deleted, `AssetDao`/`UserDao`/`DepartmentDao` SQL fixed, the two remaining `AuditLogDao` callers stubbed, `asset-list.xhtml`/`AssetDetailBean` updated). One extra bug found and fixed along the way: a MySQL comment-syntax error (`--approvals` needs a space after `--`) in `V1__baseline_schema.sql` that was failing the Flyway migration outright.

Verified for real, not just compiled: app starts under `local` with no `ddl-auto=validate` error, `GET /login` returns `200`, `GET /assets`/`GET /user` correctly return `302` (security redirect, not a crash) when unauthenticated. Full detail if ever needed: [docs/ARCHITECTURE.md](ARCHITECTURE.md) §0, [docs/DAO_REFERENCE.md](DAO_REFERENCE.md), [docs/DESIGN.md](DESIGN.md) §3–§4, §10.

`/audit-log` is the one known, acceptable exception — its bean was deleted in Phase C and nothing replaces it yet. That's Step 2's job, not a regression.

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
- `AssetDao.updateCondition(id, AssetCondition)` — separate from a value edit, same reasoning as the old design (different side effects: moving into `DAMAGED`/`UNDER_MAINTENANCE` should force-release active custody).

### 7.4 — Disable/retire an asset

Set `condition_status = RETIRED` (no separate `enabled` flag anymore — see DESIGN.md §3), force-release any active custody. Whether retired assets stay visible (greyed out) in `/assets` or get filtered out is still an open call — no existing precedent to copy.

### 7.5 — Server-side validation beyond duplicate-tag

`add-asset.xhtml` currently only has client-side `required="true"` (bypassable via a direct POST) and the duplicate-tag check. Decide and enforce real business rules (`purchase_value` positive, `purchase_date` not in the future) with a `FacesMessage`.

### 7.6 — Document upload (`AssetDocument`)

**Not started, unaffected by the redesign** — `asset_documents` is unchanged by the schema work. Its own small spike: needs `multipart/form-data` + `h:inputFile`/`Part`, plus a plain Spring MVC streaming endpoint for download, since JSF can't render binary through EL directly.

### Explicitly deferred

`RETURN` — the schema now supports it cleanly (`requester_id` is nullable specifically for this case, `previous_holder_id` names the person returning the asset — see DESIGN.md §4), but it's not part of the confirmed near-term scope. Build it once `TRANSFER`/`ASSIGNMENT` are working end-to-end, reusing the same `approval_actions` machinery.

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
