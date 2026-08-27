# Development Plan — Asset Tagging System (JSF track)

_Last updated: 2026-08-27. This is a personal working doc (gitignored — see `.gitignore`), not a project deliverable. Update it as steps complete or plans change._

**2026-08-24 note.** The 2026-08-20 architecture audit produced a fine-grained, verified task list — the **Refactoring Roadmap** artifact (Stage 0 through Stage 9, ~40 tasks, each naming the exact file/class/method to touch) — that is now more precise than this doc's remaining Steps and should be treated as authoritative for *how* to do the JSF-view and service-layer work. This doc still tracks the overall step list and the parts of the app the roadmap doesn't cover (approval workflow design, etc.), but where the two disagree on a task already covered by the roadmap, follow the roadmap. It isn't checked into the repo — link: `https://claude.ai/code/artifact/232b922d-6b71-49f1-9f72-8f14979bf094`.

**Rewritten 2026-08-17.** The previous version of this document predated a full schema redesign and no longer matched the codebase — its old Step 2 (`audit_log`) and Step 7 approval design (`first_approver_id`/`final_approver_id`) were built on tables and columns that were removed. Rather than keep flagging drift, this rewrite removes what's superseded and replaces it with the current task list. The old version, if ever needed, is in git history for this file locally, and its still-useful debugging lessons are preserved in Appendix A below.

## How to use this doc

Each step below has **What** (the concrete deliverable), **Why** (the reasoning — so you can judge edge cases yourself instead of following blindly), **How** (concrete approach, method signatures, SQL sketches), and **Done when** (a checkable finish line). Steps are meant to be done roughly in order — later steps assume earlier ones work — but see "Where things stand" for what's actually next.

This doc doesn't include full Java implementations. It gives you the shape and the SQL/logic sketch; you write the actual classes. Design rationale (*why* the schema looks the way it does) lives in [docs/DESIGN.md](DESIGN.md), not here — this doc references it rather than re-deriving it, so the two don't drift out of sync with each other the way this file drifted from the code once before.

**Once a step or phase is verified done, compress it.** Replace its full What/Why/How instructions with a short dated note — what shipped, and a pointer to the file(s) if a future reader needs the detail. This is why Steps 0 and 1 below are two sentences each instead of a page: they were once as detailed as Step 3.5 currently is, and got compressed the moment they were confirmed working. The point is that this file should always be readable top-to-bottom as "what's actually left," not an ever-growing archive of instructions for things that already happened — that archive already exists, in git history and in this file's own 2026-08-17 rewrite note above. Verify before compressing (a clean build, not just "looks right") — a compressed step is a claim that it's really done, not a hope.

---

## Where things stand right now

- **2026-08-27: Stages 0, 2, 3, 4, 5, 6, 7, and 8 of the Refactoring Roadmap are all done. Stage 9 (cleanup) is next.** Detail:
  - **Stage 2 — service layer (asset slice), roadmap T2.1–T2.4.** `AssetService.register(...)` exists, `@Transactional`, and is the one place the duplicate-tag check, the `assets` insert, and the `activity_log` write for `ASSET_REGISTERED` happen together. `@Transactional` was removed from `AssetDao.createAsset`/`updateAsset` (the boundary now lives on the service, per Spring's default `REQUIRED` propagation joining the caller's transaction). `AssetFormBean` calls the service, not `AssetDao`, directly. **Verified by fault injection**, not just by reading the code: temporarily breaking `INSERT_LOG_SQL`'s column list and confirming no `assets` row is created either — same technique as T0.2. `ActivityLogDao.log(...)` needed its `@Transactional` put *back* after this — removing it broke the login-audit write path, since `LoginAuditListener` calls `log(...)` with no surrounding transaction to join at all (the two AUTH events are the one case that needs the DAO method's own transaction, per the governing rule in [docs/DESIGN.md](DESIGN.md) §7).
  - **Stage 3 — DTOs and the N+1, roadmap T3.1–T3.3.** `AssetDao.findAll()`'s per-row `category` lazy-load is gone. New `dto/AssetRow.java` record; `AssetDao.findPage(search, limit, offset)`/`countAssets(search)` do a real joined, `LIMIT`/`OFFSET` query, hand-mapping `Object[]` rows the same way `UserDao` already did. `AssetBean` no longer loads the whole table into memory and filters in Java. **Verified**: one `/assets` load now fires exactly 2 `SELECT`s (page + count) regardless of row count, confirmed via `spring.jpa.show-sql=true`.
  - **Stage 5 — composite components, roadmap T5.1–T5.4, all four built.** `webapp/resources/ats/`: `pagination.xhtml`, `badge.xhtml`, `field.xhtml`, `filterSelect.xhtml`. Rolled out to all list/detail/form pages. Each one had a real bug caught only by testing live, not by reading the markup — worth internalizing the pattern, not just the fixes:
    - `ui:repeat` is a real `UIComponent` (`UIRepeat`), so `f:param`s generated inside it land as children of *that* component, not of the surrounding `h:outputLink` — and `h:outputLink` only scans its own direct children when building a link's query string. Fixed by using `c:forEach` (`xmlns:c="jakarta.tags.core"`) instead — Mojarra bundles a build-time-only JSTL subset directly in its Facelets implementation, so this needed **no new Maven dependency**, and doesn't reopen the JSP+JSTL deferral in Appendix B (see that entry — this is a narrow, specific exception, not general JSTL adoption).
    - HTML5 boolean attributes (`selected`, `disabled`) are presence-based, not value-based — `selected="#{someBoolean}"` on a plain (non-`h:`) element renders the literal text `selected="false"`, which the browser still treats as present. Fix: make the EL return the string `'selected'` or `null` (Facelets omits `null`-valued literal attributes on plain elements) instead of a boolean.
    - Facelets *also* omits a plain element's attribute when the EL result is an **empty string**, not just `null` — confirmed empirically with a debug probe. A `value=""` sentinel option (for "no filter selected") silently lost its `value` attribute, which would have made the browser submit the option's *text* instead on a real form post. Fixed by pulling that one option out of the generic map and rendering it with a literal (non-EL) `value=""`, via `ats:filterSelect`'s `emptyLabel` attribute.
    - `for` is a reserved word in Java/EL's tokenizer — `<cc:attribute name="for">` plus `#{cc.attrs.for}` fails to parse. Renamed to `fieldId` in `ats:field`. Avoid `for`/`class`/other Java keywords as composite-component attribute names generally.
    - A composite's root `<ui:component>` tag needs `xmlns:ui="jakarta.faces.facelets"` declared on itself, even though nothing *inside* the component uses another `ui:` tag — the prefix on the root element itself needs binding. Missed this twice (`badge.xhtml`, `filterSelect.xhtml`) before it stuck.
  - **Stage 6 — view parameters, roadmap T6.1–T6.2, done, plus one page (`audit-log.xhtml`) the roadmap didn't mention but had the identical pattern.** `f:viewParam`/`f:viewAction` now on `asset-view.xhtml`, `user-detail.xhtml`, `asset-list.xhtml`, `user-list.xhtml`, and `audit-log.xhtml`. `FacesUtil.java` is deleted — `grep -rn "getRequestParams" src/` returns nothing. `PageParams` was kept but its API changed from `parse(Map<String,String>, int)` to two small static methods, `clamp(Integer)`/`offset(int, int)`, since `f:viewParam` now does the binding `PageParams` used to do by hand.
    - **A real bug found that the roadmap's T6.1 spec didn't anticipate**: `AssetController`/`UserController`'s `/assets/{id}` and `/user/{id}` mappings declared `@PathVariable Long id`. Spring MVC converts that path segment to `Long` *at the controller layer*, before the request ever reaches JSF — so a bad id (`/assets/abc`) threw a raw `MethodArgumentTypeMismatchException` (visible as an ugly Spring 400 stack trace), and `f:viewParam`'s validation was never actually reachable through the real URL a user would type. Fixed by changing both to `@PathVariable String id`, so Spring passes the raw string through unconverted and JSF's own converter does the real validation. **Verified**: `/assets/abc` now returns a clean `'abc' must be a number consisting of one or more digits.` JSF message, zero exceptions logged.
    - **Superseded 2026-08-27 by Stage 8, T8.4**: the paragraph above (JSF having no path-variable mechanism of its own, so `AssetController`/`UserController` were load-bearing) was correct at the time but is no longer the architecture. Stage 8 replaced every path-variable URL (`/assets/{id}`) with a query-param one (`/asset/detail?id={id}`) precisely so no bridging controller is needed — see the Stage 8 entry below. `f:viewParam` reads the query param directly now.
  - **Stage 7 (T7.1 + T7.2, user edit flow and bookmarkable search) — DONE, 2026-08-27.** Both bugs noted in the previous version of this entry were fixed by the user directly (`UserService.updateUser` now correctly uses `org.springframework.transaction.annotation.Transactional`; `getUser` now uses `OptionalUtils.orThrowDbFetch`). The `editing` state machine (`edit()`/`save()`/`cancel()`, `immediate="true"` on Cancel) is built on `UserDetailBean`; `user-detail.xhtml` (now `user/detail.xhtml`, see Stage 8) is a real `h:form` with dual-mode fields — `h:selectOneMenu` for department/status, `h:selectBooleanCheckbox` per role, exactly as planned. `user-detail.js` is deleted. T7.2: both search forms (`asset/list.xhtml`, `user/list.xhtml`) are `h:form` with a `search()` action returning `faces-redirect=true&includeViewParams=true`, so filtered/paginated URLs are bookmarkable.
    - **Closed 2026-08-27, same day**: `UserService.updateUser(...)` now writes a `USER_UPDATED` `activity_log` row (`entityType=USER`, `subjectUserId` = the user being edited, `actorUserId`/`actorRoles` from the admin performing the edit), inside the same `@Transactional` method as the DAO calls. `UserDetailBean.update(...)` now resolves the actor via `SecurityUtil.currentUserId()`/`primaryRole()` and passes them through, mirroring `AssetFormBean`/`AssetService.register`'s exact pattern (including the same accepted simplification of passing `null` for `ipAddress`). **Verified atomic by fault injection**, same technique as `AssetService.register`: temporarily broke `INSERT_LOG_SQL`'s column list, confirmed the `users` row, `user_role` rows, and `activity_log` row all stayed unchanged together (a `500`, not a partial write).
  - **Stage 8 (T8.1–T8.5, URL migration) — DONE, 2026-08-27, all five tasks.**
    - **T8.1**: all `.xhtml` views moved into domain folders (`asset-list.xhtml`→`asset/list.xhtml`, `add-asset.xhtml`→`asset/form.xhtml`, `asset-view.xhtml`→`asset/detail.xhtml`, `user-list.xhtml`→`user/list.xhtml`, `user-detail.xhtml`→`user/detail.xhtml`, `audit-log.xhtml`→`activity/log.xhtml`; `dashboard.xhtml`/`login.xhtml` unchanged).
    - **T8.2**: every internal link converted to `h:link outcome=`/`h:form`-relative navigation; the sidebar's three outcome/filename mismatches fixed.
    - **T8.3**: `joinfaces.faces.automatic-extensionless-mapping=true` and `joinfaces.faces.disable-facesservlet-to-xhtml=true` are now set, alongside the two `denyAll` matchers in `SecurityConfig` (`.xhtml` and `/resources/**`), landed in the same change per Constraint C1. **Two things found live, not anticipated by the roadmap or the earlier JSF-URL-architecture spike**, detailed in `[[project_asset_tagging_jsf_url_architecture]]`: (1) Spring Security re-filters `FORWARD`-dispatched requests using the forward's *target* path, not the original request URI — so the `denyAll` rule catches a controller's internal `forward()` to a `.xhtml` file just as well as a direct external hit, which is stronger protection than the earlier spike's "raw source leak" finding implied. (2) A root-level view file whose extensionless name collides with an existing `@GetMapping` (`dashboard.xhtml`↔`/dashboard`, `login.xhtml`↔`/login`) gets routed straight to `FacesServlet` by the servlet container's exact-match precedence, bypassing the controller's Java code entirely — real but invisible, since the response is identical either way.
    - **T8.4**: all five controllers (`AssetController`, `UserController`, `AuditLogViewController`, `DashboardController`, `LoginViewController`) deleted. New `config/WebConfig.java` replaces the one piece of controller behavior still needed — `addRedirectViewController("/", "/dashboard")`. This forced two real fixes T8.2 had deliberately deferred: `asset/list.xhtml`'s and `user/list.xhtml`'s per-row detail links (previously `/assets/#{row.id}`-style, relying on the now-deleted path-variable controllers) converted to `h:link outcome=` with a nested `<f:param name="id">`; all three `ats:pagination` `url=` attributes updated from the old dead paths (`/assets`, `/user`, `/audit-log`) to the real routes.
    - **T8.5, done as part of T8.4 rather than separately** — a live authorization bug, not a cleanup item: `SecurityConfig`'s `hasRole("ADMIN")` rule still matched the literal string `/audit-log`, a path nothing has served since T8.1 renamed the file. That rule provided **zero protection** for the entire window between T8.1 and today — any authenticated employee (not just an admin) could view the activity log by visiting `/activity/log`. Verified live with `sakib@gmail.com` (`ROLE_EMPLOYEE`) before and after the fix. Now `.requestMatchers("/activity/**").hasRole("ADMIN")`.
    - Old bookmark-style URLs (`/assets`, `/user`, `/audit-log`, `/assets/{id}`, `/user/{id}`) now correctly `404` — nothing in the app links to them any more.
- **2026-08-17: full two-axis schema redesign + Flyway adoption.** `assets` no longer conflates condition and custody; `approvals`' fixed approver columns became a proper `approval_actions` table; `audit_log` + `asset_history` merged into one `activity_log` table with correlation ids and recordable refusals. Full rationale: [docs/DESIGN.md](DESIGN.md). Schema is Flyway-managed (`src/main/resources/db/migration/`), not hand-run SQL scripts.
- **2026-08-19: enum pass.** Every enumerated value now has a Java enum, four constants were renamed to match the redesigned schema, and `EntityType` became `ActivityEntityType`. Full list, rationale, and the rules for adding values: [docs/ENUM_REFERENCE.md](ENUM_REFERENCE.md). **This edits `V1__baseline_schema.sql` in place, so the local database must be recreated** ([docs/SETUP.md](SETUP.md) §2) before the app will start.
- **2026-08-18: the application runs again.** Step 3.5 (schema realignment) is done — see its entry below. Verified for real: clean startup under the `local` profile, no `ddl-auto=validate` error, `/login` returns `200`, `/assets`/`/user` correctly `302`-redirect when unauthenticated.
- **2026-08-24: templating refactor done (roadmap Stage 4, all of T4.1–T4.6).** `WEB-INF/templates/base.xhtml` and `main.xhtml` exist; `header.xhtml`/`sidebar.xhtml` moved to `WEB-INF/fragments/`; all 8 pages (`dashboard`, `asset-list`, `add-asset`, `asset-view`, `user-list`, `user-detail`, `audit-log`, `login`) converted to `ui:composition` against them, Bootstrap CDN links now live in `base.xhtml` only instead of pasted per-page. Filenames are unchanged (that's Stage 8, not this). Verified: clean `./mvnw -o compile`, and the app runs and renders through this chrome (see the `local`-profile launch note below). Known, deliberate loose end at the time: the sidebar's Assets/Users/Settings links used `h:link outcome=` values that didn't match any current filename — fixed 2026-08-27 by Stage 8's file rename and outcome updates (see the Stage 8 entry above). `/settings` still has no backing page and is left as a deliberate warning; everything else is clean.
- **2026-08-24: Stage 0 complete (T0.1–T0.5).** `audit-log.xhtml` binds `#{activityLogBean...}` against the correct field names and renders real rows, verified in the browser under an admin account; the same pass added presentation improvements requiring no Java (`<f:convertDateTime type="localDateTime">` on the timestamp, a Bootstrap badge on a new `Outcome` column driven by a ternary on `entry.outcome`, and removal of the `Ip Address` column). `SecurityConfig.filterChain()`'s dead first `.sessionManagement(...)` block is deleted. Both `LoginAuditListener` log writes are wrapped in try/catch, and `ActivityLogDao.log(...)` substitutes a fresh `UUID.randomUUID()` when `correlationId` is null. The sidebar and page titles read "Activity Log"; the *filename* remains `audit-log.xhtml` until Stage 8.
  - **T0.2 was verified by fault injection, not by inspection.** Misspelling a column inside `INSERT_LOG_SQL` (never in a migration — MySQL DDL auto-commits) makes every log write fail; sign-in then had to still succeed. Both paths must be tested separately, as they are separate catch blocks: `onSuccess` via a valid login, `onFailure` via a wrong password. Confirmed before the `onFailure` guard existed, an audit-write failure on that path produced an error page instead of `/login?error` — the listener's exception replaces the `AuthenticationException` inside `ProviderManager`, so Spring Security's failure handler never runs. Repeat this test whenever the write path changes.
  - **Note on T0.3's guard:** it is unreachable from an HTTP request, because `CorrelationFilter` always populates the `ThreadLocal` first. It exists for writes originating off the request thread — scheduled work, `@Async`, tests, and the Stage 2 service layer. A null id is replaced rather than rejected because `activity_log.correlation_id` is `NOT NULL`, so passing null through would trade a `NullPointerException` for a constraint violation.
  - **Access gotcha, cost real debugging time:** `/audit-log` is restricted to `hasRole("ADMIN")` in `SecurityConfig`, and the seed's `sakib@gmail.com` (user 1) is `ROLE_EMPLOYEE`. A denied request is not surfaced — `RestAccessDeniedHandler` issues a silent `sendRedirect` to `/dashboard?error`, and nothing on the dashboard renders that parameter, so the page appears to simply not navigate. Use `mehedi@gmail.com` or `fahim@gmail.com` (both `ROLE_ADMIN`, all three seed users share one password hash). Making that refusal visible is worth doing eventually; it is not currently a roadmap task.
  - **Known ceiling on the current page:** `h:dataTable` emits one flat `<tr>` per row, which precludes day separators, multi-line event cells, and expandable correlation traces. Any further presentation work requires replacing it with `ui:repeat` plus hand-written markup. Target design and a build-order breakdown: `https://claude.ai/code/artifact/f6afd58f-cda5-4f4d-a72d-6650dbffda41`.
- **2026-08-24: T1.1 answered — `@ViewScoped` works in this stack.** Verified empirically on a second instance (port 8099) by driving three real HTTP postbacks with curl against a throwaway `scope-test.xhtml` + `ScopeTestBean`: the counter accumulated 1→2→3 and `@PostConstruct` fired exactly once across all three clicks (both signals agreeing = genuinely one surviving instance, not a request-scoped bean that looks right). Use `jakarta.faces.view.ViewScoped` (the `jakarta.faces.bean` one was removed in Faces 4). **Two findings to carry into Stage 7:** (1) the scope is `@NormalScope(passivating = true)`, so beans MUST `implement Serializable` + declare `serialVersionUID`, and injected Spring services MUST be marked `transient` — the canonical pattern is in [docs/MODULE_GUIDE.md](MODULE_GUIDE.md) §(ApprovalDecisionView). (2) **This stack does NOT enforce that at deploy time** — the non-`Serializable` spike bean deployed with no `WELD-000072` error, because JoinFaces's discovery + server-side state saving never actually serialized it. So the DAO-survives-passivation question is still *unproven*; the definitive test is flipping `jakarta.faces.STATE_SAVING_METHOD=client` and confirming a `transient`-service bean still works. Not blocking; note it when Stage 7 builds its first `@ViewScoped` bean. The spike files are throwaway (delete once noted, like the Step 1 spike).
- **Launching the app:** `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` — the `local` profile is what supplies `DB_USER`/`DB_PASS`/`DB_NAME` from `application-local.properties`; omitting it (or a typo in the flag name) leaves those as unresolved `${...}` placeholders and MySQL rejects the literal string as a username.
- **What's next: Stage 9 (Cleanup)** — 11 small, independent tasks (T9.1–T9.11): delete `DashboardBean` and `FacesUtil` (both now zero-caller), fix `LookupBean`'s scope, trim `pom.xml` (`tomcat-embed-jasper`, test-scope `spring-boot-starter-data-jpa-test`, the `junit-jupiter-api` version pin), pick one test database (Testcontainers MySQL, drop H2), rename `RestAccessDeniedHandler`→`BrowserAccessDeniedHandler`, make `CorrelationFilter.CURRENT` private with static accessors. None touch JSF views. Full list in the Refactoring Roadmap artifact.
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
- [x] 1b. `ActivityLogDao.findRecent(limit, offset)` and `countAll()` — **done** (this item was stale — both exist and are used by `ActivityLogBean`). The `correlationId` null guard (roadmap T0.3) landed 2026-08-24.
- [x] 2. **Don't swallow the write's exception** — done, as part of 1a. The old `AuditLogDao.log()` caught its own exception to protect login from an unrelated audit-write failure — reasonable at the time, but the decision this session (see [docs/ARCHITECTURE.md](ARCHITECTURE.md) §8.3) is that every *other* mutation's log write should join the same transaction as the business action and fail it if the write fails. Keep the swallow-and-log-only behavior for the two `AUTH` events specifically (`LoginAuditListener` has no business transaction to join).
- [x] 3. `CorrelationFilter` — **done** (this item was stale). `security/CorrelationFilter.java` exists: a `@Component Filter` at `Ordered.HIGHEST_PRECEDENCE`, minting one UUID per request into a `ThreadLocal`, auto-registered by Spring Boot. The swallow-and-log-only behaviour item 2 promises for the two `AUTH` events landed 2026-08-24 (roadmap T0.2), verified by fault injection.
- [x] 4a. `LoginAuditListener` — **done**. Both `onSuccess`/`onFailure` inject `ActivityLogDao`, build a real `ActivityLog` via its builder, and call `.log(act)` inside a try/catch (T0.2, 2026-08-24). T0.3's `correlationId` guard is in `ActivityLogDao`, not here.
- [x] 4b. `AssetFormBean` — **done (2026-08-25, roadmap Stage 2, T2.1–T2.4).** `AssetService.register(...)` does the duplicate-tag check, the `assets` insert, and the `activity_log` write for `ASSET_REGISTERED` inside one `@Transactional` method; `AssetFormBean` calls the service, not `AssetDao`. Verified atomic by fault injection (see the 2026-08-27 status note above). The write-fails-the-transaction behavior this "Done when" section asks for below is now satisfied.
- [x] 5a. `ActivityLogBean` — **done** (this item was stale). `bean/ActivityLogBean.java` exists: `@Named @RequestScoped`, real `@PostConstruct` using `PageParams`, and an `entityIdOf(entry)` helper that picks whichever of `assetId`/`approvalId`/`subjectUserId` is set.
- [x] 5b. `audit-log.xhtml` — **done (2026-08-24, roadmap T0.1).** Bindings corrected to `#{activityLogBean...}` and to the real field names (`occurredAt`, `summary`, `entityIdOf(entry)`); verified rendering real rows in the browser. See the 2026-08-24 Stage 0 entry under "Where things stand" for the presentation changes made in the same pass and for the `h:dataTable` ceiling on any further work here.
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

**Superseded by roadmap Stage 7 — DONE, 2026-08-27.** List, detail, and edit all exist and work. `UserDetailBean`'s `edit()`/`save()`/`cancel()` state machine (`@ViewScoped` + `h:form` + `f:viewAction`/command-button actions, no hand-written controller endpoint) persists real updates via `UserService.updateUser`. See the Stage 7 entry in "Where things stand right now" above.

### Remaining

- [ ] Create form (if genuinely needed — user creation may end up admin-only and low priority relative to the approval workflow; re-confirm before building)

### Done when

The edit form works end-to-end in the browser, a successful save produces an `activity_log` row in the same transaction as the update, and Cancel correctly discards an in-progress edit without a validation error blocking it.

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

**Superseded 2026-08-25 (roadmap Stage 3, T3.1–T3.3).** The line that used to be here said Asset search/pagination deliberately loaded everything via `findAll()` and filtered in Java, unlike the Employee (`UserDao`) pattern — that decision was reversed. `AssetDao.findAll()`'s per-row `category` access was a real, measured N+1 (confirmed via `spring.jpa.show-sql=true`: one extra `SELECT` per row). `AssetDao.findPage(search, limit, offset)`/`countAssets(search)` now do a real joined, DB-side `LIMIT`/`OFFSET` query returning `dto/AssetRow.java`, matching the Employee pattern this note used to say not to copy. If DB-side pagination is ever reverted for Assets specifically, that reversal needs its own reason too, the same as this one did.

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
- **Two different `@Transactional` annotations exist, and only one does anything here.** `org.springframework.transaction.annotation.Transactional` is the real one — this app has no JTA transaction manager (its own boot log says `WELD-000101: Transactional services not available`), so `jakarta.transaction.Transactional` on a `@Service` method is very likely a silent no-op. An IDE auto-import can pick the wrong one without any compile error. If a multi-statement `@Transactional` method's atomicity hasn't been proven by fault injection (break one of the statements, confirm the others roll back too), don't trust that it's actually atomic just because the annotation is present.
- **`@PathVariable Long id` in a Spring MVC controller converts *before* the request reaches JSF.** If that controller forwards to a Facelets view reading the same value via `f:viewParam`, a bad id never reaches JSF's converter at all — Spring throws its own `MethodArgumentTypeMismatchException` first (a raw 400 stack trace), and `f:viewParam`'s validation message is unreachable through the real URL. Use `@PathVariable String id` and let JSF do the actual conversion/validation.
- **A composite component's own root `<ui:component>` tag needs `xmlns:ui="jakarta.faces.facelets"` declared on itself** — even if nothing else inside the file uses another `ui:` tag. The prefix used on the root element itself still needs to be bound, or every page that uses the component fails to parse with "the prefix 'ui' ... is not bound."
- **`for` is a reserved word in Java/EL's tokenizer** — a composite-component attribute named `for` fails to parse as `#{cc.attrs.for}` ("Failed to parse the expression"). Avoid Java keywords as `cc:attribute` names generally (used `fieldId` instead).
- **HTML5's `selected`/`disabled`/`checked` are presence-based, not value-based.** `selected="#{someBoolean}"` on a plain (non-`h:`) element renders the literal text `selected="false"` when false — which the browser still treats as selected, since the attribute is present at all. Make the EL return `'selected'`/`null` instead of a boolean; Facelets omits `null`-valued attributes on plain elements.
- **Facelets omits a plain element's attribute when the EL result is an empty string, not just when it's `null`.** A `value=""` sentinel (e.g. the "no filter" option in a `<select>`) silently loses its `value` attribute entirely, which makes the browser fall back to submitting the option's *text* instead. If an attribute genuinely needs to render as `""`, write it as a literal (non-EL) attribute, not an EL expression that happens to evaluate to empty.

## Appendix B — Explicitly deferred or cut (and why)

- **JSP + JSTL pages**: deferred, not cancelled. Current decision (2026-07-27) is JSF-only for now. Don't add JSTL to a regular page for general looping/conditionals.
  - **Narrow exception, added 2026-08-25**: `xmlns:c="jakarta.tags.core"`'s `c:forEach` is now used *inside* `webapp/resources/ats/pagination.xhtml` and `filterSelect.xhtml` specifically, for one reason: `ui:repeat` is a real `UIComponent`, so `f:param`/`option` elements generated inside it don't land as direct children of the surrounding `h:outputLink`/`select`, which those specific renderers require. `c:forEach` is Facelets' own build-time-only tag (bundled in Mojarra's Facelets implementation — no JSTL/JSP dependency added). This doesn't reopen the general JSP+JSTL deferral above — don't reach for `c:forEach` outside this specific "must produce literal direct children" situation.
- **Phase 6 — hand-writing one legacy JSF-1.x XML-config managed bean**: cut from the active plan (2026-07-28) given the speed priority. Pure internship-prep practice value, no functional value to the app. Worth revisiting on slack time, or better, learning it directly on the real internship codebase.
- **Role-based HTTP authorization**: `SecurityConfig` currently only requires authentication, no role rules (`@EnableMethodSecurity` is on but nothing uses `@PreAuthorize` anywhere). Deliberately deferred. When it happens: `authorizeHttpRequests` is first-match-wins, so role-specific rules must be added *before* any catch-all `authenticated()`/`permitAll()`.
