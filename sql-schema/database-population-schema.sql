USE asset_tagging_system;

-- 1. POPULATE DEPARTMENTS
INSERT INTO departments (name) VALUES
                                   ('Engineering'),
                                   ('Human Resources'),
                                   ('Finance'),
                                   ('Operations');

-- 2. POPULATE ROLES
INSERT INTO roles (id, name) VALUES
                                 (1, 'ROLE_EMPLOYEE'),
                                 (2, 'ROLE_ADMIN');

-- 3. POPULATE ASSET CATEGORIES
INSERT INTO asset_categories (name, depreciation_rate_percentage) VALUES
                                                                      ('Laptops', 15.00),
                                                                      ('Monitors', 10.00),
                                                                      ('Network Devices', 12.50),
                                                                      ('Furniture', 5.00);

-- 4. POPULATE USERS
-- password column holds a BCrypt hash ($2a$, cost factor 10), never plaintext -- Spring
-- Security's BCryptPasswordEncoder verifies against this at login time.
-- Un-encrypted (plaintext) value for every seed user below is: Password123!
-- This is a real, verified hash of that string (checked with bcrypt.checkpw before commit) --
-- it is for local/dev seed data only and must never be reused for a real account.
INSERT INTO users (first_name, last_name, email, password, dept_id, enabled) VALUES
                                                                                 ('Sakib', 'Khan', 'sakib.khan@enterprise.com', '$2a$10$sU5t9uqsAEij84Wdmvu6S.q6CWvW2uw1o2zX2Vyf3KS1.OXoZOHFq', 1, 1), -- ID: 1 (Employee 1)
                                                                                 ('Mehedi', 'Hasan', 'mehedi.hasan@enterprise.com', '$2a$10$sU5t9uqsAEij84Wdmvu6S.q6CWvW2uw1o2zX2Vyf3KS1.OXoZOHFq', 4, 1), -- ID: 2 (Admin 1)
                                                                                 ('Fahim', 'Ahmad', 'fahim.ahmad@enterprise.com', '$2a$10$sU5t9uqsAEij84Wdmvu6S.q6CWvW2uw1o2zX2Vyf3KS1.OXoZOHFq', 3, 1);    -- ID: 3 (Admin 2)

-- 5. MAP USERS TO ROLES (user_role)
INSERT INTO user_role (user_id, role_id) VALUES
                                             (1, 1), -- Sakib -> ROLE_EMPLOYEE
                                             (2, 2), -- Mehedi -> ROLE_ADMIN
                                             (3, 2); -- Fahim -> ROLE_ADMIN

-- 6. POPULATE ASSETS
-- created_by_user_id = 2 (Mehedi, Admin) registered all of these into inventory.
INSERT INTO assets (asset_tag, name, category_id, purchase_date, value, status, created_by_user_id) VALUES
                                                                         ('AST-1001', 'MacBook Pro M3 16"', 1, '2026-01-15', 2500.00, 'ASSIGNED', 2),   -- ID: 1 (The Transfer Target)
                                                                         ('AST-1002', 'Dell UltraSharp 27"', 2, '2026-02-10', 450.00, 'AVAILABLE', 2),  -- ID: 2
                                                                         ('AST-1003', 'ThinkPad T14 Gen 4', 1, '2025-11-01', 1400.00, 'ASSIGNED', 2),   -- ID: 3
                                                                         ('AST-1004', 'Cisco Network Switch', 3, '2024-05-20', 3200.00, 'DAMAGED', 2);  -- ID: 4

-- 7. POPULATE ASSET DOCUMENTS (Vertical Partition Blob Simulation)
INSERT INTO asset_documents (asset_id, asset_image, invoice_pdf, image_mime_type, invoice_mime_type) VALUES
                                                                                                         (1, UNHEX('89504E470D0A1A0A0000000D49484452'), UNHEX('255044462D312E340A25E2E3CFD3'), 'image/png', 'application/pdf'),
                                                                                                         (2, UNHEX('89504E470D0A1A0A0000000D49484452'), UNHEX('255044462D312E340A25E2E3CFD3'), 'image/png', 'application/pdf');

-- 8. POPULATE APPROVALS (moved before asset_custody -- custody now references approvals.id)
INSERT INTO approvals (
    asset_id, initiated_by_user_id, requester_id, previous_holder_id,
    request_type, required_approval_count,
    first_approver_id, final_approver_id, status,
    request_reason, first_approver_notes, final_approver_notes,
    request_date, first_action_date, final_action_date
) VALUES
-- Flow #3: Admin-initiated transfer. Mehedi (2, Admin) personally held the MacBook and
-- initiated its transfer to Sakib (1) -- her own initiation counts as the first approval.
-- Fahim (3, Admin) completed the loop as the second, distinct approver.
(1, 2, 1, 2, 'TRANSFER_REQUEST', 2, 2, 3, 'APPROVED',
 'Reassigning MacBook to Sakib for the new project.', 'Agreed, reassign to Sakib.', 'Final approval granted.',
 '2026-07-08 10:00:00', '2026-07-08 14:30:00', '2026-07-09 11:00:00'),

-- Flow #2: Employee self-service request. Sakib (1) requests the available Dell Monitor (2)
-- from central inventory storage -- starts PENDING, needs two distinct admins.
-- Admin 1 (Mehedi) has greenlit it; still waiting on Admin 2's sign-off.
(2, 1, 1, NULL, 'ASSET_REQUEST', 2, 2, NULL, 'FIRST_APPROVED',
 'Need external monitor for dual-screen setup.', 'Approved by first admin, awaiting final.', NULL,
 '2026-07-09 08:15:00', '2026-07-09 10:45:00', NULL),

-- Flow #5: Return request. Sakib (1) is returning the ThinkPad to inventory -- self-service,
-- requires only ONE admin approval, still PENDING (nobody has verified yet).
(3, 1, 1, NULL, 'RETURN_REQUEST', 1, NULL, NULL, 'PENDING',
 'Switching to the MacBook full-time, returning the ThinkPad to inventory.', NULL, NULL,
 '2026-07-09 09:00:00', NULL, NULL);

-- 9. POPULATE ASSET CUSTODY LIVES
INSERT INTO asset_custody (asset_id, custodian_id, approval_id, assigned_by_user_id, custody_start, custody_end, status) VALUES
-- MacBook (ID 1): Mehedi's original custody. approval_id NULL -- this predates the approval
-- workflow (initial onboarding assignment), which is the documented backfill exception.
(1, 2, NULL, 2, '2026-01-20 09:00:00', '2026-07-09 11:00:02', 'RELEASED'),
-- MacBook (ID 1): now actively held by Sakib after the approved transfer (approval_id 1).
-- Fahim, the final approver, also executed the physical handoff.
(1, 1, 1, 3, '2026-07-09 11:00:02', NULL, 'ACTIVE'),
-- ThinkPad (ID 3) is held by Sakib (ID 1) -- also predates the workflow, same backfill exception.
(3, 1, NULL, 2, '2025-11-05 10:30:00', NULL, 'ACTIVE');

-- 10. POPULATE GLOBAL AUDIT LOGS (asset_history)
INSERT INTO asset_history (asset_id, action, previous_status, new_status, previous_holder_id, new_holder_id, performed_by_user_id, approval_id, notes, action_date) VALUES
(1, 'ASSET_INITIAL_REGISTRATION', NULL, 'AVAILABLE', NULL, NULL, 2, NULL,
 'MacBook initialized into inventory management registry.', '2026-01-15 09:30:00'),

(1, 'CUSTODY_ALLOCATED', 'AVAILABLE', 'ASSIGNED', NULL, 2, 2, NULL,
 'Deployed out to Mehedi Hasan for initial operations onboarding.', '2026-01-20 09:02:00'),

(4, 'STATUS_REPORT_DAMAGE', 'AVAILABLE', 'DAMAGED', NULL, NULL, 1, NULL,
 'Sakib logged system connection dropped; physical capacitor blown.', '2026-06-12 16:45:00'),

(1, 'PEER_TRANSFER_COMPLETED', 'ASSIGNED', 'ASSIGNED', 2, 1, 3, 1,
 'P2P handoff verified. Route authorization verified by Mehedi and finalized by Fahim.', '2026-07-09 11:00:02'),

(3, 'RETURN_REQUESTED', NULL, NULL, NULL, NULL, 1, 3,
 'Sakib requested to return the ThinkPad to inventory.', '2026-07-09 09:00:00');
