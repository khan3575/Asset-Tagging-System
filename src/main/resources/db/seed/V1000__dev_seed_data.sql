-- =============================================================================
-- V1000: development seed data.
--
-- Lives in db/seed, NOT db/migration -- a separate Flyway location that is
-- not part of the base application.properties, and only added to
-- spring.flyway.locations locally (the `local` profile) or via an explicit
-- SPRING_FLYWAY_LOCATIONS override (the public demo deployment -- see
-- compose.yaml). It must never run against a real company's real asset
-- inventory: the passwords below are public knowledge in this file, and no
-- real deployment should contain a made-up user called Mehedi with a known
-- password. The public demo is a deliberate, narrow exception to that rule,
-- not a contradiction of it -- everything in the demo database is fictional
-- to begin with.
--
-- Versioned as V1000 (not V2) so it always sorts after every real schema
-- migration, however many exist. It is still an ordinary versioned migration
-- as far as Flyway is concerned -- edit it after it has run locally and your
-- local database's checksum will mismatch, same as any V-file. If you change
-- it, drop and recreate your local database rather than editing in place.
--
-- password_hash for every user below is the BCrypt hash (cost 10) of the
-- plaintext "DemoOnly2026!", verified with bcrypt.checkpw before being written
-- here. This file is also applied to the public demo deployment (see
-- compose.yaml), so this plaintext is intentionally public -- it is not, and
-- must never become, a real account's password.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Departments, roles, categories
-- -----------------------------------------------------------------------------
INSERT INTO departments (id, name) VALUES
    (1, 'Engineering'),
    (2, 'Human Resources'),
    (3, 'Finance'),
    (4, 'Operations');

INSERT INTO roles (id, name) VALUES
    (1, 'ROLE_EMPLOYEE'),
    (2, 'ROLE_ADMIN');

INSERT INTO asset_categories (id, name, depreciation_rate_percentage) VALUES
    (1, 'Laptops',          15.00),
    (2, 'Monitors',         10.00),
    (3, 'Network Devices',  12.50),
    (4, 'Furniture',         5.00);

-- -----------------------------------------------------------------------------
-- Users -- Sakib (employee), Mehedi and Fahim (admins)
-- -----------------------------------------------------------------------------
INSERT INTO users (id, first_name, last_name, email, password_hash, dept_id, enabled) VALUES
    (1, 'Sakib',  'Khan',   'sakib@gmail.com',  '$2a$10$FXPscHxt5dSwAMnOpr2Sie24ZZ8/1/PqxN1if9PaIBExuZx6gIMvO', 1, 1),
    (2, 'Mehedi', 'Hasan',  'mehedi@gmail.com', '$2a$10$FXPscHxt5dSwAMnOpr2Sie24ZZ8/1/PqxN1if9PaIBExuZx6gIMvO', 4, 1),
    (3, 'Fahim',  'Ahmad',  'fahim@gmail.com',  '$2a$10$FXPscHxt5dSwAMnOpr2Sie24ZZ8/1/PqxN1if9PaIBExuZx6gIMvO', 3, 1);

INSERT INTO user_role (user_id, role_id) VALUES
    (1, 1),  -- Sakib  -> ROLE_EMPLOYEE
    (2, 2),  -- Mehedi -> ROLE_ADMIN
    (3, 2);  -- Fahim  -> ROLE_ADMIN

-- -----------------------------------------------------------------------------
-- Assets. Note condition_status only -- no status/custody mixing.
-- Mehedi (2) registered all four into inventory.
-- -----------------------------------------------------------------------------
INSERT INTO assets (id, asset_tag, name, category_id, purchase_date, purchase_value, condition_status, created_by_user_id) VALUES
    (1, 'AST-1001', 'MacBook Pro M3 16"',   1, '2026-01-15', 2500.00, 'IN_SERVICE', 2),
    (2, 'AST-1002', 'Dell UltraSharp 27"',  2, '2026-02-10',  450.00, 'IN_SERVICE', 2),
    (3, 'AST-1003', 'ThinkPad T14 Gen 4',   1, '2025-11-01', 1400.00, 'IN_SERVICE', 2),
    (4, 'AST-1004', 'Cisco Network Switch', 3, '2024-05-20', 3200.00, 'DAMAGED',    2);

INSERT INTO asset_documents (asset_id, asset_image, invoice_pdf, image_mime_type, invoice_mime_type) VALUES
    (1, UNHEX('89504E470D0A1A0A0000000D49484452'), UNHEX('255044462D312E340A25E2E3CFD3'), 'image/png', 'application/pdf'),
    (2, UNHEX('89504E470D0A1A0A0000000D49484452'), UNHEX('255044462D312E340A25E2E3CFD3'), 'image/png', 'application/pdf');

-- -----------------------------------------------------------------------------
-- Approval workflows, in the new shape: one row per decision in
-- approval_actions, no fixed first/final approver columns on approvals itself.
-- -----------------------------------------------------------------------------

-- Approval #1: admin-initiated transfer of the MacBook, Mehedi -> Sakib.
-- Mehedi both held the asset and initiated the transfer; Fahim gave the
-- second, distinct sign-off required to complete it.
INSERT INTO approvals (id, asset_id, request_type, initiated_by_user_id, requester_id, previous_holder_id,
                        required_approval_count, status, request_reason, requested_at, closed_at) VALUES
    (1, 1, 'TRANSFER', 2, 1, 2, 2, 'APPROVED',
     'Reassigning MacBook to Sakib for the new project.', '2026-07-08 10:00:00', '2026-07-09 11:00:00');

INSERT INTO approval_actions (approval_id, actor_user_id, action, sequence_no, notes, created_at) VALUES
    (1, 2, 'APPROVED', 1, 'Agreed, reassign to Sakib.', '2026-07-08 14:30:00'),
    (1, 3, 'APPROVED', 2, 'Final approval granted.',    '2026-07-09 11:00:00');

-- Approval #2: employee self-service request for the (unheld) Dell monitor.
-- One admin has signed off; still waiting on the second. Deliberately left
-- open so the "requests awaiting my approval" query in the v2 proposal has
-- something real to return.
INSERT INTO approvals (id, asset_id, request_type, initiated_by_user_id, requester_id, previous_holder_id,
                        required_approval_count, status, request_reason, requested_at) VALUES
    (2, 2, 'ASSIGNMENT', 1, 1, NULL, 2, 'PARTIALLY_APPROVED',
     'Need external monitor for dual-screen setup.', '2026-07-09 08:15:00');

INSERT INTO approval_actions (approval_id, actor_user_id, action, sequence_no, notes, created_at) VALUES
    (2, 2, 'APPROVED', 1, 'Approved by first admin, awaiting final.', '2026-07-09 10:45:00');

-- Approval #3: Sakib returns the ThinkPad to inventory. A return has no
-- incoming holder, so requester_id is NULL and previous_holder_id names the
-- person giving the asset back -- the corrected shape from the v2 proposal.
-- Only one admin sign-off required, and it is still pending.
INSERT INTO approvals (id, asset_id, request_type, initiated_by_user_id, requester_id, previous_holder_id,
                        required_approval_count, status, request_reason, requested_at) VALUES
    (3, 3, 'RETURN', 1, NULL, 1, 1, 'PENDING',
     'Switching to the MacBook full-time, returning the ThinkPad to inventory.', '2026-07-09 09:00:00');

-- -----------------------------------------------------------------------------
-- Custody. MacBook has two rows -- Mehedi's original (pre-workflow, no
-- approval_id) then Sakib's current one (tied to approval #1). ThinkPad
-- predates the workflow entirely. Dell monitor has no row: approval #2 is
-- still open, so nobody holds it yet.
-- -----------------------------------------------------------------------------
INSERT INTO asset_custody (asset_id, custodian_id, approval_id, assigned_by_user_id, custody_start, custody_end, status) VALUES
    (1, 2, NULL, 2, '2026-01-20 09:00:00', '2026-07-09 11:00:00', 'RELEASED'),
    (1, 1, 1,    3, '2026-07-09 11:00:00', NULL,                  'ACTIVE'),
    (3, 1, NULL, 2, '2025-11-05 10:30:00', NULL,                  'ACTIVE');

-- -----------------------------------------------------------------------------
-- activity_log. Session variables hold one UUID per user action so every row
-- that action produced shares a correlation_id -- the mechanism described in
-- the v2 proposal, §9. actor_roles is a snapshot of authority at the time,
-- not a join to user_role, and is exactly what makes it possible to answer
-- "was this administrator's role current when they acted" later.
-- -----------------------------------------------------------------------------

-- Registration of all four assets by Mehedi (admin at the time).
SET @corr := UUID();
INSERT INTO activity_log (occurred_at, correlation_id, actor_user_id, actor_roles, action, entity_type,
                           asset_id, new_condition, summary) VALUES
    ('2026-01-15 09:30:00', UNHEX(REPLACE(@corr, '-', '')), 2, 'ROLE_ADMIN', 'ASSET_REGISTERED', 'ASSET',
     1, 'IN_SERVICE', 'Registered AST-1001 MacBook Pro M3 16"');
SET @corr := UUID();
INSERT INTO activity_log (occurred_at, correlation_id, actor_user_id, actor_roles, action, entity_type,
                           asset_id, new_condition, summary) VALUES
    ('2026-02-10 09:30:00', UNHEX(REPLACE(@corr, '-', '')), 2, 'ROLE_ADMIN', 'ASSET_REGISTERED', 'ASSET',
     2, 'IN_SERVICE', 'Registered AST-1002 Dell UltraSharp 27"');
SET @corr := UUID();
INSERT INTO activity_log (occurred_at, correlation_id, actor_user_id, actor_roles, action, entity_type,
                           asset_id, new_condition, summary) VALUES
    ('2025-11-01 09:30:00', UNHEX(REPLACE(@corr, '-', '')), 2, 'ROLE_ADMIN', 'ASSET_REGISTERED', 'ASSET',
     3, 'IN_SERVICE', 'Registered AST-1003 ThinkPad T14 Gen 4');
SET @corr := UUID();
INSERT INTO activity_log (occurred_at, correlation_id, actor_user_id, actor_roles, action, entity_type,
                           asset_id, new_condition, summary) VALUES
    ('2024-05-20 09:30:00', UNHEX(REPLACE(@corr, '-', '')), 2, 'ROLE_ADMIN', 'ASSET_REGISTERED', 'ASSET',
     4, 'IN_SERVICE', 'Registered AST-1004 Cisco Network Switch');

-- Mehedi's original custody of the MacBook (pre-workflow onboarding assignment).
SET @corr := UUID();
INSERT INTO activity_log (occurred_at, correlation_id, actor_user_id, actor_roles, action, entity_type,
                           asset_id, new_holder_id, summary) VALUES
    ('2026-01-20 09:00:00', UNHEX(REPLACE(@corr, '-', '')), 2, 'ROLE_ADMIN', 'CUSTODY_ASSIGNED', 'ASSET',
     1, 2, 'Deployed to Mehedi Hasan for initial onboarding.');

-- The Cisco switch is reported damaged by Sakib.
SET @corr := UUID();
INSERT INTO activity_log (occurred_at, correlation_id, actor_user_id, actor_roles, action, entity_type,
                           asset_id, previous_condition, new_condition, summary) VALUES
    ('2026-06-12 16:45:00', UNHEX(REPLACE(@corr, '-', '')), 1, 'ROLE_EMPLOYEE', 'ASSET_CONDITION_CHANGED', 'ASSET',
     4, 'IN_SERVICE', 'DAMAGED', 'Connection dropped; capacitor blown.');

-- Approval #1's full lifecycle: submitted, first approval, final approval +
-- custody transfer. The final click is one correlation_id across two rows --
-- see the v2 proposal, UC-4/UC-5.
SET @corr := UUID();
INSERT INTO activity_log (occurred_at, correlation_id, actor_user_id, actor_roles, action, entity_type,
                           asset_id, approval_id, summary) VALUES
    ('2026-07-08 10:00:00', UNHEX(REPLACE(@corr, '-', '')), 2, 'ROLE_ADMIN', 'REQUEST_SUBMITTED', 'APPROVAL',
     1, 1, 'Requested transfer of AST-1001 to Sakib.');
SET @corr := UUID();
INSERT INTO activity_log (occurred_at, correlation_id, actor_user_id, actor_roles, action, entity_type,
                           asset_id, approval_id, summary) VALUES
    ('2026-07-08 14:30:00', UNHEX(REPLACE(@corr, '-', '')), 2, 'ROLE_ADMIN', 'REQUEST_APPROVED', 'APPROVAL',
     1, 1, 'First sign-off: agreed, reassign to Sakib.');

SET @corr := UUID();
INSERT INTO activity_log (occurred_at, correlation_id, sequence_in_action, actor_user_id, actor_roles,
                           action, entity_type, asset_id, approval_id, summary) VALUES
    ('2026-07-09 11:00:00', UNHEX(REPLACE(@corr, '-', '')), 1, 3, 'ROLE_ADMIN',
     'REQUEST_APPROVED', 'APPROVAL', 1, 1, 'Final approval granted.');
INSERT INTO activity_log (occurred_at, correlation_id, sequence_in_action, actor_user_id, actor_roles,
                           action, entity_type, asset_id, approval_id, previous_holder_id, new_holder_id, summary) VALUES
    ('2026-07-09 11:00:00', UNHEX(REPLACE(@corr, '-', '')), 2, 3, 'ROLE_ADMIN',
     'CUSTODY_TRANSFERRED', 'ASSET', 1, 1, 2, 1, 'Handed from Mehedi to Sakib.');

-- Approval #2 (Dell monitor): submitted, then one sign-off. Still open.
SET @corr := UUID();
INSERT INTO activity_log (occurred_at, correlation_id, actor_user_id, actor_roles, action, entity_type,
                           asset_id, approval_id, summary) VALUES
    ('2026-07-09 08:15:00', UNHEX(REPLACE(@corr, '-', '')), 1, 'ROLE_EMPLOYEE', 'REQUEST_SUBMITTED', 'APPROVAL',
     2, 2, 'Need external monitor for dual-screen setup.');
SET @corr := UUID();
INSERT INTO activity_log (occurred_at, correlation_id, actor_user_id, actor_roles, action, entity_type,
                           asset_id, approval_id, summary) VALUES
    ('2026-07-09 10:45:00', UNHEX(REPLACE(@corr, '-', '')), 2, 'ROLE_ADMIN', 'REQUEST_APPROVED', 'APPROVAL',
     2, 2, 'Approved by first admin, awaiting final.');

-- Sakib then attempts to give the SECOND sign-off on his own request, and is
-- refused. This is UC-11 from the v2 proposal: the one scenario the previous
-- schema could not record at all, because it had no outcome column. Nothing
-- else is written for this attempt -- no approval_actions row, no state change.
SET @corr := UUID();
INSERT INTO activity_log (occurred_at, correlation_id, actor_user_id, actor_roles, action, entity_type,
                           asset_id, approval_id, outcome, failure_reason) VALUES
    ('2026-07-09 10:50:00', UNHEX(REPLACE(@corr, '-', '')), 1, 'ROLE_EMPLOYEE', 'REQUEST_APPROVED', 'APPROVAL',
     2, 2, 'DENIED', 'Requester may not approve their own request.');

-- Approval #3 (ThinkPad return): submitted, still pending.
SET @corr := UUID();
INSERT INTO activity_log (occurred_at, correlation_id, actor_user_id, actor_roles, action, entity_type,
                           asset_id, approval_id, summary) VALUES
    ('2026-07-09 09:00:00', UNHEX(REPLACE(@corr, '-', '')), 1, 'ROLE_EMPLOYEE', 'REQUEST_SUBMITTED', 'APPROVAL',
     3, 3, 'Returning the ThinkPad to inventory.');

-- Two AUTH events, showing why actor_user_id is nullable: a failed login
-- never resolves to a real user, so the attempted email is data, not a
-- foreign key, and lives in details.
SET @corr := UUID();
INSERT INTO activity_log (occurred_at, correlation_id, actor_user_id, actor_roles, action, entity_type,
                           ip_address, outcome) VALUES
    ('2026-08-17 08:02:11', UNHEX(REPLACE(@corr, '-', '')), 1, 'ROLE_EMPLOYEE', 'LOGIN_SUCCEEDED', 'AUTH',
     '10.0.0.14', 'SUCCEEDED');
SET @corr := UUID();
INSERT INTO activity_log (occurred_at, correlation_id, actor_user_id, action, entity_type,
                           ip_address, outcome, details) VALUES
    ('2026-08-17 08:05:47', UNHEX(REPLACE(@corr, '-', '')), NULL, 'LOGIN_FAILED', 'AUTH',
     '10.0.0.99', 'FAILED', JSON_OBJECT('attempted_email', 'root@example.com'));
