# Site Map

Every route the application defines, what serves it, and its current status. Written 2026-08-17 against branch `dev`. Cross-reference [docs/ARCHITECTURE.md](ARCHITECTURE.md) Section 0 for why the "Broken" column exists at all — the schema redesign left several routes unable to function until the corresponding DAOs and entities are updated.

## 1. Routes

| Route | Controller | View | Bean(s) | DAOs reached | Status |
|---|---|---|---|---|---|
| `GET /` | `DashboardController` | *(redirects to `/dashboard`)* | — | — | Works |
| `GET /dashboard` | `DashboardController` | `dashboard.xhtml` | `DashboardBean` | *(none — empty bean)* | Renders, no content |
| `GET /login` | `LoginViewController` | `login.xhtml` | — | — | View renders; login itself blocked, see below |
| `POST /login` | *(Spring Security `formLogin`)* | → `/dashboard?login=success` | `CustomUserDetailsService` | `UserDao` | **Broken** — `User.password` vs. `users.password_hash` (ARCHITECTURE.md §0.1) prevents the app from starting at all |
| `POST /logout` | *(Spring Security)* | → `/login?logout` | — | — | Works once startup is fixed |
| `GET /assets` | `AssetController` | `asset-list.xhtml` | `AssetBean` | `AssetDao` | **Broken** — `AssetDao` selects `value`/`status`, which no longer exist |
| `GET /assets/new` | `AssetController` | `add-asset.xhtml` | `AssetFormBean`, `LookupBean` | `AssetDao`, `AuditLogDao`, `AssetCategoryDao` | **Broken** — `AssetDao` write path affected, and `AuditLogDao` targets a dropped table |
| `GET /assets/{id}` | `AssetController` | `asset-view.xhtml` | `AssetDetailBean` | `AssetDao`, `AssetCustodyDao`, `ApprovalDao`, `UserDao` | **Broken** — same `AssetDao` issue; `AssetCustodyDao` and `ApprovalDao` are individually fine |
| `GET /user` | `UserController` | `user-list.xhtml` | `UserListBean`, `LookupBean` | `UserDao`, `DepartmentDao` | **Broken** — blocked by the same startup failure as login; `DepartmentDao` also selects a dropped `enabled` column |
| `GET /user/{id}` | `UserController` | `user-detail.xhtml` | `UserDetailBean`, `LookupBean` | `UserDao` | **Broken** — same startup failure |
| `GET /audit-log` | `AuditLogViewController` | `audit-log.xhtml` | `AuditLogBean` | `AuditLogDao` | **Broken** — `AuditLogDao` targets the dropped `audit_log` table entirely |

## 2. Links rendered with no route behind them

Five hrefs exist in the UI today with no corresponding controller mapping. Each is an independent, well-scoped task once the schema-alignment work (ARCHITECTURE.md §0.4) is done.

| Link | Rendered by | Notes |
|---|---|---|
| `/settings` | `sidebar.xhtml` | No view, bean, or controller |
| `/me` | `header.xhtml` | Likely intended as a redirect to `/user/{currentUserId}` |
| `/forgot-password` | `login.xhtml` | No view, bean, or controller |
| `/approvals/{id}` | *(intended for an approval detail page)* | Approval detail view does not exist yet — needed once `approval_actions` is wired up |
| `/assets/{id}/transfer`, `/assets/{id}/request` | *(planned in earlier design notes, not built)* | The custody-move and self-request flows have no route yet — `ApprovalDao.createTransferRequest` exists and is correct against the current schema but is called from nowhere |

## 3. Write paths — the whole application, enumerated

Exactly two places currently attempt to mutate data, both currently broken by the schema misalignment:

| Action | Path | Status |
|---|---|---|
| Create an asset | `AssetFormBean.save()` → `AssetDao.createAsset(...)` | Broken — targets renamed columns |
| Record a login event | `LoginAuditListener` → `AuditLogDao.log(...)` | Broken — table no longer exists (fails silently; see ARCHITECTURE.md §8.3 on why this needs to change to fail loudly) |

Everything else in the application — custody assignment, approval decisions, condition changes, department or role changes — has schema support (`asset_custody`, `approval_actions`, `activity_log`) but no Java write path calling it at all.

## 4. Reading this table going forward

"Broken" here specifically means: fails once the schema-alignment work in ARCHITECTURE.md §0 is done for the *other* parts of the app, or fails today if isolated and tested directly. Update the Status column as each piece is fixed — this file, not memory, is the place to track it, since it is checked into the repository and travels with the code.
