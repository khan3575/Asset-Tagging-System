# Design

This document explains *why* the application is shaped the way it is. [docs/ARCHITECTURE.md](ARCHITECTURE.md) describes how it is built; this document explains the reasoning behind the decisions it is built on.

It is in two parts:

- **[Part I — Data Model Design](#part-i--data-model-design)** (Sections 1–10). Why the schema in `src/main/resources/db/migration/V1__baseline_schema.sql` looks the way it does. Written 2026-08-17, superseding the schema described in `sql-schema/`.
- **[Part II — View Layer Design](#part-ii--view-layer-design)** (Sections 11–23). How the Jakarta Faces view layer in `src/main/webapp/` is to be structured, and the specification to build it against. Written 2026-08-20.

---

# Part I — Data Model Design

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

The single most consequential change. `assets.condition_status` describes **the physical state of the object only** — `IN_SERVICE`, `DAMAGED`, `UNDER_MAINTENANCE`, `BEYOND_REPAIR`, `RETIRED`. Whether anyone is holding it is not stored on this table at all; it is derived from `asset_custody`.

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

`requester_id` was made nullable: a `RETURN` request has no incoming holder (the asset returns to inventory, held by nobody), and the old schema's `NOT NULL` forced the returning employee to be recorded as if they were receiving the asset.

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

## 10. Reference tables: `closed_at`/`retired_at` are permanent, by decision

`departments.name` and `asset_categories.name` are plain `UNIQUE` — closing a department (or retiring a category) does not free its name for reuse, since the constraint has no relationship to `closed_at`/`retired_at`. `asset_custody.active_asset_id` and `approvals.open_asset_id` both solve an equivalent problem elsewhere in this schema with a generated column that goes `NULL` when the row is inactive, so the uniqueness only binds the live one — the same trick would apply here (`active_name` generated as `CASE WHEN closed_at IS NULL THEN name END`, uniqued on that instead of `name` directly).

**Deliberately not applied, confirmed 2026-08-17.** A closed department or retired category is expected to be filterable out of any picklist at read time (`WHERE closed_at IS NULL`, per `DepartmentDao.findAllDepartments()` — [docs/development-plan.md](development-plan.md) Step 3.5) rather than have its name become reusable. The row itself is never deleted, specifically so that historical references — `users.dept_id` pointing at a closed department, `activity_log` entries naming a retired category — continue to resolve to a real, permanent record rather than a name that has since been reassigned to something else. Freeing the name for reuse would work against that goal, not support it: a later department that happened to reuse "Engineering" would make old history ambiguous about which "Engineering" it meant. Simple permanent uniqueness is therefore the correct choice here, not an oversight — Principle 3 (state tables enforce invariants) is satisfied by the plain `UNIQUE` constraint exactly as well as the generated-column version would be; the generated-column trick exists elsewhere in this schema to solve *reuse*, which is not a requirement here.

---

# Part II — View Layer Design

Part I explains the data the application is built on. Part II explains how the view layer that presents it is to be structured, and is the specification to build against. Written 2026-08-20, superseding the ad-hoc page structure currently in `src/main/webapp/`.

Scope: this document specifies *structure, conventions, and the reasoning behind them*. It does not teach Jakarta Faces tag semantics — [docs/jsf-basics-guide.md](jsf-basics-guide.md) covers those, and is the reference for how any individual tag behaves. Where the two disagree, this document governs, because it describes decisions specific to this application.

Platform: Mojarra 4 (Jakarta Faces 4.1) via `org.joinfaces:mojarra4-spring-boot-starter` 6.1.0, on Spring Boot with Spring Security. No component library (PrimeFaces, BootsFaces, OmniFaces) is used, and none is to be introduced without revisiting this document — every pattern below is achievable with the specification tag libraries alone.

## 11. What is wrong with the current view layer

Ten defects motivate this redesign. Each is present in the tree today and each is traceable to a specific file.

| # | Defect | Consequence |
|---|---|---|
| V1 | No template inheritance. Seven pages each carry their own doctype, namespace declarations, `<h:head>`, CDN links, container, and closing script | The Bootstrap CDN URL appears 23 times across 8 files. A version bump is a 23-line edit with no compiler assistance |
| V2 | `<ui:include>` is used for chrome where `<ui:composition>` + `<ui:define>` is the correct mechanism | Pages must supply the surrounding document themselves, which is the root cause of V1 |
| V3 | `h:outputLink value="/assets"` writes a root-relative URL that omits the context path | Every such link breaks the moment the application is deployed anywhere other than the root context. Nine occurrences; `user-list.xhtml:79` uses the correct `#{request.contextPath}` form, so the codebase contradicts itself |
| V4 | Request parameters are parsed by hand in `@PostConstruct` via `FacesUtil.getRequestParams()`, with manual null checks and `Long.valueOf` / `RoleName.valueOf` calls | Type conversion, validation, and required-ness are reimplemented per bean. Malformed input throws `NumberFormatException` out of a lifecycle method rather than producing a message |
| V5 | Select elements with a pre-selected option are built from raw `<option>` markup plus two mutually exclusive `<ui:fragment>` branches | `user-list.xhtml:45-52`, `user-detail.xhtml:63-70`, `:77-84`, `:91-104` — four copies of a construct that exists to work around not using `f:selectItems` |
| V6 | The pagination control is duplicated three times | `asset-list.xhtml`, `user-list.xhtml`, `audit-log.xhtml`, differing only in target URL, bean name, and carried filters |
| V7 | The page-header block (title, subtitle, record-count badge, primary action) is duplicated five times with drifting markup | Changing the badge style is a five-file edit |
| V8 | `asset-view.xhtml:31` reads `#{assetBean.totalCount}` on an asset *detail* page | Instantiates `AssetBean`, which executes `AssetDao.findAll()` — a full table load — to render one badge that reports the wrong number |
| V9 | Read-only fields are rendered as `h:inputText disabled="disabled"` inside an `h:form` that has no submit control | `asset-view.xhtml:35-53`. Disabled inputs submit nothing and convey "temporarily unavailable", not "not editable" |
| V10 | The user edit flow is a raw `<form method="POST">` toggled by hand-written JavaScript, posting to a route that does not exist | `user-detail.xhtml:37-120` plus `resources/js/user-detail.js`. No `POST /user/{id}` mapping exists, so the Save button silently 405s |

V1, V2, V6 and V7 are duplication. V3, V4, V8, V9 and V10 are defects that duplication concealed.

## 12. Five principles

**Principle 5 — One file knows the document shape.** Exactly one file declares the doctype, `<html>`, `<h:head>`, and the stylesheet links. Every other view contributes content and is structurally ignorant of HTML's document form. *(Resolves V1.)*

**Principle 6 — Slots for structure, includes for fragments.** A file that offers named extension points is a *template*, consumed with `<ui:composition template>` or `<ui:decorate>`. A file that offers a fixed block of markup is a *fragment*, consumed with `<ui:include>`. The two live in different directories and are never mixed. *(Resolves V2.)*

**Principle 7 — Anything parameterised is a component, not a copy.** A repeated block that varies by more than styling gets a composite component with a declared `cc:interface`. `<ui:include>` with `<ui:param>` is permitted only where the fragment takes no parameters at all, because `ui:param` has no declared contract and a misspelled name fails silently. *(Resolves V5, V6, V7.)*

**Principle 8 — The framework converts, validates, and reports.** No view-layer code performs string-to-type conversion, null-checking of request input, or error reporting that a specification tag already performs. Parameters arrive through `f:viewParam`; conversion through `f:convert*`; validation through Bean Validation or `f:validate*`; errors through `h:message`. *(Resolves V4.)*

**Principle 9 — A URL is built once, in one way.** Every internal link is written `#{request.contextPath}/route`. No exceptions, no second convention. *(Resolves V3.)*

The test for "component or fragment" is not size — it is: **does anything about it vary between uses?** The sidebar is identical on every page and takes nothing; it is a fragment. Pagination varies by target URL, page count, and carried parameters; it is a component.

## 13. Navigation, view ids, and why the URL scheme constrains everything

This section is placed before the file structure because it governs it.

The application serves pretty URLs (`/assets`, `/user/{id}`) from Spring MVC controllers that call `RequestDispatcher.forward()` to a `.xhtml` file. The consequence is that **the browser's URL and the JSF view id are different strings**. `GET /assets` renders the view whose id is `/asset-list.xhtml`.

For GET-only pages this is harmless. For pages containing an `h:form` it is not, and the reason must be understood before any form is written: Jakarta Faces computes a form's `action` attribute from the **view id**, not from the request URI. A form rendered on `/assets` therefore posts to `/asset-list.xhtml`, and the browser's address bar changes to that URL on submit.

Three consequences follow, and all three are design constraints rather than bugs to be fixed later:

1. **Any view containing a form must be reachable at its own `.xhtml` URL.** A blanket `.requestMatchers("/**/*.xhtml").denyAll()` security rule would break every postback in the application. Security rules for view files must therefore be written per route, mirroring whatever rule guards the pretty URL that forwards to them. `/audit-log` and `/audit-log.xhtml` carrying the same `hasRole("ADMIN")` rule is the pattern to follow, not a special case.
2. **Every action method ends in a redirect.** Post/Redirect/Get is mandatory here rather than merely advisable: without it the user is left on the `.xhtml` URL, and a refresh re-submits. `AssetFormBean.save()` already does this and is the model.
3. **JSF navigation outcomes cannot be used.** `<h:link outcome="/assets"/>` and `return "/assets?faces-redirect=true"` both resolve their argument as a *view id* and would look for `/assets.xhtml`, which does not exist. All navigation is therefore explicit: `#{request.contextPath}/route` in markup, `FacesUtil.redirect("/route")` in Java.

**Considered and not adopted.** Serving views directly at their own `.xhtml` URLs and deleting the controller layer would make view id and URL identical and remove all three constraints. It was rejected because the resulting URLs (`/asset-list.xhtml?id=3`) are worse for a system whose links are shared and bookmarked. A URL-rewriting filter or PrettyFaces would give both, at the cost of a dependency this project has chosen to do without. If the constraints above ever become expensive, that is the direction to take — not abandoning the pretty URLs.

## 14. File structure

```
src/main/webapp/
├── WEB-INF/
│   ├── templates/                 files with slots — consumed by ui:composition
│   │   ├── base.xhtml             the only HTML-aware file
│   │   └── master.xhtml           application chrome
│   └── fragments/                 files without slots — consumed by ui:include
│       ├── header.xhtml
│       └── sidebar.xhtml
├── resources/                     JSF resource library root
│   ├── ats/                       composite component library → xmlns:ats
│   │   ├── pageHeader.xhtml
│   │   ├── pagination.xhtml
│   │   ├── filterSelect.xhtml
│   │   └── field.xhtml
│   ├── css/
│   │   └── app.css
│   └── js/
│       └── user-detail.js
├── login.xhtml                    client of base.xhtml
├── dashboard.xhtml                client of master.xhtml
├── asset-list.xhtml               client of master.xhtml
├── add-asset.xhtml                client of master.xhtml
├── asset-view.xhtml               client of master.xhtml
├── user-list.xhtml                client of master.xhtml
├── user-detail.xhtml              client of master.xhtml
└── activity-log.xhtml             client of master.xhtml
```

Three deliberate placements:

**`WEB-INF/templates` and `WEB-INF/fragments` are separate directories.** The split enforces Principle 6 at the level of the filesystem: a reviewer can tell from a path alone which consumption mechanism is correct. The existing `header.xhtml` and `sidebar.xhtml` move to `fragments/`; they have no slots and never will.

**Everything under `WEB-INF` is unreachable by URL.** The servlet container refuses to serve it, so a template can never be requested directly and render as a broken half-page.

**Pages stay at the web root, not in `WEB-INF/views`.** This is forced by Section 13: form-bearing views must be reachable at their own URL. The empty `WEB-INF/views` directory currently in the tree is to be deleted.

**`audit-log.xhtml` is renamed `activity-log.xhtml`**, matching the `activity_log` table and `ActivityLogBean`. The route `/audit-log` may keep its name or change with it, but the file must not contradict the schema.

## 15. The template hierarchy

Three layers. Each page chooses its depth.

```
base.xhtml ─── doctype, <html>, <h:head> + CDN links + app.css, <h:body>, closing scripts
   │              slots: metadata, title, body
   │
   ├── master.xhtml ─── container, header fragment, sidebar fragment, <h:messages>,
   │        │           the col-9 <main> element
   │        │           consumes: body   ·   re-exposes: pageHeader, content
   │        │
   │        ├── dashboard.xhtml       defines: title, content
   │        ├── asset-list.xhtml      defines: metadata, title, pageHeader, content
   │        ├── add-asset.xhtml       defines: title, pageHeader, content
   │        ├── asset-view.xhtml      defines: metadata, title, pageHeader, content
   │        ├── user-list.xhtml       defines: metadata, title, pageHeader, content
   │        ├── user-detail.xhtml     defines: metadata, title, pageHeader, content
   │        └── activity-log.xhtml    defines: metadata, title, pageHeader, content
   │
   └── login.xhtml ─── defines: title, body — no chrome, no authenticated user to render it for
```

### 15.1 `base.xhtml`

A plain XHTML document — **not** itself a `<ui:composition>`. Wrapping a root template in `ui:composition` causes the tag's trimming behaviour to discard the entire document, producing a blank page with no error.

Required content, in order:

1. `<ui:insert name="metadata"/>` — placed before `<h:head>`. Pages supply a complete `<f:metadata>` element here (Section 17). This placement is finicky; verify it on the first page that uses `f:viewParam` before writing the rest.
2. `<h:head>` — the JSF component, never a plain `<head>`. JSF relocates `h:outputStylesheet`, `h:outputScript target="head"`, and every `@ResourceDependency` into the head *component*; with a plain `<head>` there is no component to relocate into and those resources are dropped silently.
3. `<title><ui:insert name="title">Asset Tagging System</ui:insert></title>` — a named insert with default content, so a page that forgets a title still renders a sensible one.
4. `<meta charset>`, `<meta name="viewport">`.
5. The two Bootstrap CDN `<link>` elements. These stay as literal `<link>` tags: `h:outputStylesheet` addresses the JSF resource library, which is for files inside `resources/`, not remote URLs.
6. `<h:outputStylesheet library="css" name="app.css"/>` — for project styles.
7. `<h:body>` containing `<ui:insert name="body"/>`.
8. The Bootstrap bundle `<script>` as the last child of `<h:body>`.

### 15.2 `master.xhtml`

Simultaneously a **client** of `base.xhtml` and a **template** for every page. This dual role is the mechanism by which the chain extends, and it is the one piece of Facelets templating that is not obvious:

```
<ui:composition template=".../base.xhtml">        ← client of base
    <ui:define name="body">                       ← fills base's slot
        ...chrome...
        <ui:insert name="content"/>               ← offers a slot of its own
    </ui:define>
</ui:composition>
```

`master.xhtml` owns: the `container` div, the `<ui:include>` of the header fragment, the row wrapper, the `<ui:include>` of the sidebar fragment, the `<main class="col-9">` element, a single `<h:messages>` (Section 19), a `<ui:insert name="pageHeader"/>`, and a `<ui:insert name="content"/>`.

It does not own: page titles, headings, or anything a specific page would want to change. Anything a page needs to vary is a slot.

`title` and `metadata` are **not** re-declared in `master.xhtml`. A `<ui:define>` in a page propagates to whichever ancestor template declares the matching `<ui:insert>`, so pages fill `base`'s title slot directly through `master`.

### 15.3 Pages

Every page is a `<ui:composition template="...">` and nothing else. Content outside the composition tag is discarded, so a page may retain an `<html>` wrapper for editor preview if desired, but the project convention is to omit it: a file whose first element is `ui:composition` is unambiguously a template client.

Pages declare only the namespaces they use. The current habit of copying all five namespace declarations into every page is to stop; an unused `xmlns:p` is noise that suggests passthrough attributes are in play when they are not.

## 16. Composite components

Composite components live at `resources/<library>/<name>.xhtml` and require no registration — the directory name becomes the tag library. The library is `ats`, giving pages `xmlns:ats="jakarta.faces.composite/ats"` and tags such as `<ats:pagination/>`.

A composite component is chosen over `<ui:include>` + `<ui:param>` wherever anything varies, because `cc:interface` declares the contract: attributes are named, typed, defaulted, and can be `required="true"`. A missing required attribute is an error at build time. A misspelled `ui:param` name is an empty string at render time, reported nowhere.

Four components are specified. Build them in this order; each removes a defect from Section 11.

### 16.1 `ats:pagination` — resolves V6

| Attribute | Type | Required | Purpose |
|---|---|---|---|
| `route` | `java.lang.String` | yes | Path without context, e.g. `/assets`. The component prepends `#{request.contextPath}` |
| `page` | `java.lang.Integer` | yes | Current page, 1-based |
| `totalPages` | `java.lang.Integer` | yes | Total page count |

Carried filter parameters are the difficult part: `asset-list` carries `search`, `user-list` carries four, `activity-log` carries none. Pass them as a **facet** rather than attributes, so the caller supplies a block of `<f:param>` elements the component renders inside both links. Use `<cc:renderFacet name="params"/>` in the implementation and `<f:facet name="params">` at the call site.

Render nothing at all when `totalPages` is less than 2. The current pages render an empty flex row in that case.

### 16.2 `ats:pageHeader` — resolves V7

| Attribute | Type | Required | Purpose |
|---|---|---|---|
| `title` | `java.lang.String` | yes | Heading text |
| `subtitle` | `java.lang.String` | no | Muted line beneath |
| `count` | `java.lang.Long` | no | Record-count badge; the badge is omitted entirely when unset |

The primary action button varies too much between pages to be an attribute — `asset-list` has "Add Asset", `user-list` has none, `user-detail` has a name badge. Expose it as `<cc:insertChildren/>` so the caller nests whatever control it needs.

Building this component is what surfaces V8: `asset-view.xhtml` passes a count it has no business displaying, and the component's `count` attribute makes that visible rather than incidental. Remove the badge from that page; do not add `totalCount` to `AssetDetailBean` to satisfy it.

### 16.3 `ats:filterSelect` — resolves V5

| Attribute | Type | Required | Purpose |
|---|---|---|---|
| `id` | — | yes | Also the request-parameter name; see below |
| `label` | `java.lang.String` | yes | Field label |
| `options` | `java.lang.Object` | yes | `List` or array of values |
| `selected` | `java.lang.Object` | no | Currently selected value |
| `emptyLabel` | `java.lang.String` | no | Label for the "no filter" option, default `All` |

This component renders a **plain `<select>` element**, not `h:selectOneMenu`, and the reason is Section 18: the filter forms are non-JSF GET forms, and the `name` attribute must be exactly the request parameter the bean reads. A JSF input renders its `name` from its client id, which is subject to naming-container prefixing and is not a string the component can guarantee.

Inside the implementation, use a single `<option>` element with `selected="#{option eq cc.attrs.selected ? 'selected' : ''}"` rather than two `<ui:fragment>` branches. Compare by identifier, not by object: `#{dept.id eq cc.attrs.selected}`. EL's `==`/`eq` delegates to `equals()`, and the Lombok-built entities do not all define one, so object comparison is unreliable here.

### 16.4 `ats:field` — supports Section 19

| Attribute | Type | Required | Purpose |
|---|---|---|---|
| `label` | `java.lang.String` | yes | Field label |
| `for` | `java.lang.String` | yes | Client id of the wrapped input |

Wraps a Bootstrap `col` div, an `h:outputLabel`, the caller's input via `<cc:insertChildren/>`, and an `h:message for=` beneath it. This is what makes per-field validation messages (Section 19) cheap enough to apply consistently rather than only on the one page that currently has any.

## 17. View parameters replace manual parsing

`FacesUtil.getRequestParams()` and every hand-written `Long.valueOf` / `RoleName.valueOf` / null-check in a `@PostConstruct` are to be removed. The specification mechanism is `<f:metadata>` in the page and `<f:viewParam>` per parameter:

- `<f:viewParam name="id" value="#{assetDetailBean.id}"/>` binds a request parameter to a bean property, applying the converter registered for the property's type. A non-numeric `id` produces a conversion message instead of a `NumberFormatException` escaping a lifecycle method.
- `<f:viewParam name="roleName" value="#{userListBean.roleName}"/>` converts to the enum automatically. The four-line ternary blocks in `UserListBean.init()` are deleted, not relocated.
- `<f:viewAction action="#{bean.load}"/>`, placed after the params inside `f:metadata`, runs once on a GET request after all parameters are bound and before rendering. This is where DAO calls move to.

Two consequences for the bean layer, both mandatory:

**Parameter-bound properties need setters.** `f:viewParam` writes through a value expression. Add `@Setter` to the specific fields it binds — field-level, never class-level. A display bean must not expose setters for derived state such as `entries`, `totalPageCount`, or `totalCount`; only genuine inbound parameters get one.

**`@PostConstruct` stops loading data.** With `f:viewAction`, `init()` either disappears or shrinks to defaults that do not depend on request input. This is the correct ordering: `@PostConstruct` runs before view parameters are applied, which is precisely why the current code has to read the parameter map by hand.

`PageParams` remains useful for deriving `offset` from `page`, but is called from the view action rather than from a parameter map.

## 18. Forms — three kinds, and which to use

The application legitimately needs all three. Choosing wrongly is the source of V9 and V10.

**Kind 1 — Non-JSF GET form, for search and filter.** `asset-list` and `user-list` use a plain `<form>` with no `method`, submitting to the same URL as a query string. **This is correct and is to be kept.** A JSF postback would produce a non-bookmarkable page whose back-button behaviour is a re-submission prompt, and the filtered result would have no shareable URL. Bookmarkability is worth more here than component state. The inputs stay plain HTML so their `name` attributes match the parameters `f:viewParam` declares.

**Kind 2 — JSF postback form, for creating and editing.** `add-asset` and the rebuilt `user-detail` use `<h:form>` with `h:inputText`, `h:selectOneMenu`, and `h:commandButton action="#{bean.method}"`. Use this wherever the submission changes data, because conversion, validation, message association, and re-display of rejected input are all handled by the framework. `add-asset.xhtml` is already correct and is the reference.

Note that `h:form` is a naming container: `<h:inputText id="assetTag">` inside `<h:form id="add-asset-form">` renders `name="add-asset-form:assetTag"`. `h:outputLabel for=` resolves this correctly; hand-written JavaScript selecting by `getElementById("assetTag")` does not. Any script touching a JSF input must use the full client id, which is the second reason V10's approach fails.

**Kind 3 — Not a form at all.** Read-only data is rendered with `h:outputText` or plain EL, never with a disabled input. `asset-view.xhtml` is to be rewritten as a definition list or read-only card; the `h:form` there wraps three disabled inputs and no submit control, so it exists only to look like a form. If a page later needs to edit those fields, it becomes Kind 2 with real inputs, not disabled ones re-enabled by script.

### 18.1 The user-detail edit flow — resolving V10

The current design toggles `disabled` attributes with JavaScript and posts to a route that does not exist. Replace it with the JSF mechanism:

- Promote `UserDetailBean` to `@ViewScoped` (`jakarta.faces.view.ViewScoped`), which requires the bean to implement `Serializable`. Request scope cannot hold an editing flag across a postback.
- Add a boolean `editing` property, an `edit()` action that sets it, and a `cancel()` action that clears it and reloads.
- Render the two states with `rendered="#{userDetailBean.editing}"` on real `h:inputText`/`h:selectOneMenu` controls and `rendered="#{not userDetailBean.editing}"` on `h:outputText`. The framework re-renders; nothing is hidden with CSS.
- The Save button is an `h:commandButton action="#{userDetailBean.save}"` whose method performs the write and then redirects (Section 13, consequence 2).

`resources/js/user-detail.js` is deleted by this change. A `POST /user/{id}` Spring MVC route is **not** to be added — the postback goes to the view id, which is what makes this work without one.

## 19. Messages and validation

One `<h:messages globalOnly="true"/>` lives in `master.xhtml`, above the content slot, so every page gets bean-level messages without asking. It carries `styleClass="alert alert-danger"`. The copy currently in `add-asset.xhtml` is removed when that page becomes a template client.

Field-level messages are `<h:message for="..."/>` adjacent to each input, supplied automatically by `ats:field` (Section 16.4). `globalOnly="true"` on the master-level component is what keeps field messages from appearing twice.

Validation belongs on the model, not in the page. Prefer Bean Validation annotations on the bean property — the project already has `validation/ValidationConstants` — over `required="true"` sprinkled across `h:inputText` tags, so that the same rule applies whether a value arrives from a form, a view parameter, or a future import. `required="true"` remains appropriate for presence-only checks on inputs with no bean-side counterpart.

## 20. Resources

Project CSS and JavaScript are served through the JSF resource library, not by hardcoded `<script src>` or `<link href>` paths:

- `<h:outputStylesheet library="css" name="app.css"/>` → `resources/css/app.css`
- `<h:outputScript library="js" name="foo.js" target="body"/>` → `resources/js/foo.js`

`user-detail.xhtml:125` already does this correctly and is the pattern. The benefit is not brevity: the resource handler adds cache-busting version tokens, resolves locale and library versions, and — critically — allows JSF to relocate the element into `h:head` or the end of `h:body` regardless of where the tag appears in the source.

Remote CDN URLs cannot be resource-library entries and stay as literal `<link>`/`<script>` elements in `base.xhtml`, appearing exactly once.

The sidebar currently mixes emoji glyphs (`📊`, `📜`, `👥`, `⚙️`) with Bootstrap Icons (`<i class="bi bi-briefcase">`). Standardise on Bootstrap Icons; the icon font is already loaded on every page, and emoji render inconsistently across platforms.

## 21. Bean contract by page type

The view structure implies a bean structure. Three shapes, and a page should match one of them exactly.

**List bean** — `@Named @RequestScoped`, `@Getter` only, field-level `@Setter` on filter and page parameters. Holds the current slice, the filter values, and the page counters. Loads in `f:viewAction`. Pagination is performed in SQL with `LIMIT`/`OFFSET`, never in memory: `AssetBean` currently calls `findAll()` and slices the result with `subList`, loading the entire asset register on every page view. It is to be brought in line with `UserListBean`, which does this correctly, once `AssetDao` grows a `findAssets(search, limit, offset)` / `countAssets(search)` pair.

**Detail bean** — `@Named` and `@RequestScoped`, unless it supports editing, in which case `@ViewScoped` and `Serializable`. Holds one entity and its directly related data. Binds exactly one view parameter, `id`.

**Form bean** — `@Named @RequestScoped`, `@Getter @Setter` at class level, because every field is genuinely written by the framework. Action methods return `null` and redirect explicitly.

`LookupBean` remains `@ApplicationScoped` and is the single home for static option lists. `UserListBean.getRoleOptions()` duplicates `LookupBean.getRoleOptions()` verbatim and is to be deleted, with the page reading `#{lookupBean.roleOptions}`.

## 22. Build order

Each step leaves the application in a working state. Do not begin a step before the previous one renders.

| Step | Work | Removes |
|---|---|---|
| 1 | `base.xhtml`; convert `login.xhtml` to a client of it | Part of V1 |
| 2 | `master.xhtml`; move `header.xhtml`/`sidebar.xhtml` to `fragments/`; convert `dashboard.xhtml` | V2, most of V1 |
| 3 | Convert the remaining five pages to `master.xhtml` clients | V1 |
| 4 | `ats:pageHeader`, applied to all six chrome pages | V7, V8 |
| 5 | `ats:pagination`, applied to the three list pages | V6 |
| 6 | `ats:filterSelect`, applied to `user-list` and `user-detail` | V5 |
| 7 | Replace every `h:outputLink value="/…"` with a context-path-prefixed URL | V3 |
| 8 | `f:metadata`/`f:viewParam`/`f:viewAction` on all six parameterised pages; delete `FacesUtil.getRequestParams()` and the manual parsing | V4 |
| 9 | Rewrite `asset-view.xhtml` as read-only output | V9 |
| 10 | Rebuild the `user-detail` edit flow as `@ViewScoped` + `rendered`; delete `user-detail.js` | V10 |
| 11 | `ats:field` + `h:message`, applied to `add-asset` and the rebuilt `user-detail` | — |

Steps 1–3 are prerequisites for everything else and remove the largest volume of duplication. Steps 8 and 10 are the two that change Java as well as markup and should not be attempted on a red build.

## 23. Known limitations of the view layer design

- **View id and URL remain different strings.** Section 13 explains why this is accepted rather than solved. It means form-bearing views need per-route security rules, and that a user who submits a form sees a `.xhtml` URL for the duration of one redirect.
- **No AJAX.** Every interaction is a full page load. `f:ajax` is available and specified, and the filter forms are the obvious first candidate, but partial rendering is deliberately out of scope until the full-page flows are correct.
- **No internationalisation.** Every label is a literal string in markup. The specification mechanism is a `<resource-bundle>` in `faces-config.xml` read via `#{msg.key}`. Retrofitting this later is mechanical but touches every page; it is deferred knowingly, not overlooked.
- **`ats:filterSelect` renders plain HTML.** It is a composite component wrapping non-JSF markup, which is unusual. It is correct here because the GET filter forms (Section 18, Kind 1) require exact control of the `name` attribute, but it means the component cannot participate in JSF validation or AJAX. If the filter forms ever become postbacks, this component is replaced by `h:selectOneMenu` + `f:selectItems` rather than extended.
- **Composite components are not unit-testable.** There is no test tooling in this project for the view layer at all, and the components are verified by rendering pages. This is a gap, not a decision.
