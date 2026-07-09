USE asset_taggin_system;

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

-- 4. POPULATE USERS (Rifa Removed)
-- Password strings represent standard BCrypt hashes used by Spring Security
INSERT INTO users (first_name, last_name, email, password, dept_id, enabled) VALUES
                                                                                 ('Sakib', 'Khan', 'sakib.khan@enterprise.com', '$2a$10$vX8Vb/E8K7Rj7b2V0m8wO.eE6g1z9qH7iX2Kz9u1m6n5b4v3c2x1a', 1, 1), -- ID: 1 (Employee 1)
                                                                                 ('Mehedi', 'Hasan', 'mehedi.hasan@enterprise.com', '$2a$10$vX8Vb/E8K7Rj7b2V0m8wO.eE6g1z9qH7iX2Kz9u1m6n5b4v3c2x1a', 4, 1), -- ID: 2 (Admin 1)
                                                                                 ('Fahim', 'Ahmad', 'fahim.ahmad@enterprise.com', '$2a$10$vX8Vb/E8K7Rj7b2V0m8wO.eE6g1z9qH7iX2Kz9u1m6n5b4v3c2x1a', 3, 1);    -- ID: 3 (Admin 2)

-- 5. MAP USERS TO ROLES (user_role)
INSERT INTO user_role (user_id, role_id) VALUES
                                             (1, 1), -- Sakib -> ROLE_EMPLOYEE
                                             (2, 2), -- Mehedi -> ROLE_ADMIN
                                             (3, 2); -- Fahim -> ROLE_ADMIN

-- 6. POPULATE ASSETS
INSERT INTO assets (name, category_id, purchase_date, value, status) VALUES
                                                                         ('MacBook Pro M3 16"', 1, '2026-01-15', 2500.00, 'ASSIGNED'),   -- ID: 1 (The Transfer Target)
                                                                         ('Dell UltraSharp 27"', 2, '2026-02-10', 450.00, 'AVAILABLE'),  -- ID: 2
                                                                         ('ThinkPad T14 Gen 4', 1, '2025-11-01', 1400.00, 'ASSIGNED'),   -- ID: 3
                                                                         ('Cisco Network Switch', 3, '2024-05-20', 3200.00, 'DAMAGED');  -- ID: 4

-- 7. POPULATE ASSET DOCUMENTS (Vertical Partition Blob Simulation)
INSERT INTO asset_documents (asset_id, asset_image, invoice_pdf, image_mime_type, invoice_mime_type) VALUES
                                                                                                         (1, UNHEX('89504E470D0A1A0A0000000D49484452'), UNHEX('255044462D312E340A25E2E3CFD3'), 'image/png', 'application/pdf'),
                                                                                                         (2, UNHEX('89504E470D0A1A0A0000000D49484452'), UNHEX('255044462D312E340A25E2E3CFD3'), 'image/png', 'application/pdf');

-- 8. POPULATE ASSET CUSTODY LIVES
INSERT INTO asset_custody (asset_id, custodian_id, custody_start, custody_end, status) VALUES
-- MacBook (ID 1) was initially assigned to Mehedi (ID 2), but is being transferred to Sakib
(1, 2, '2026-01-20 09:00:00', NULL, 'ACTIVE'),
-- ThinkPad (ID 3) is securely held by Sakib (ID 1)
(3, 1, '2025-11-05 10:30:00', NULL, 'ACTIVE');

-- 9. POPULATE APPROVALS (Updated Scenario Workflow)
INSERT INTO approvals (
    asset_id, requester_id, previous_holder_id,
    first_approver_id, final_approver_id, status,
    request_date, first_action_date, final_action_date
) VALUES
-- Scenario 1: Sakib (1) requests the MacBook currently assigned to Mehedi (2).
-- Mehedi signed off as first approver (2). Fahim completed the loop as final approver (3).
(1, 1, 2, 2, 3, 'APPROVED', '2026-07-08 10:00:00', '2026-07-08 14:30:00', '2026-07-09 11:00:00'),

-- Scenario 2: Sakib (1) requests the available Dell Monitor (2) from central inventory storage.
-- It has been greenlit by Admin 1 (Mehedi), but remains frozen waiting for Admin 2's sign-off.
(2, 1, NULL, 2, NULL, 'FIRST_APPROVED', '2026-07-09 08:15:00', '2026-07-09 10:45:00', NULL);

-- 10. POPULATE GLOBAL AUDIT LOGS (asset_history)
INSERT INTO asset_history (asset_id, action, action_date, performed_by_user_id, notes) VALUES
                                                                                           (1, 'ASSET_INITIAL_REGISTRATION', '2026-01-15 09:30:00', 2, 'MacBook initialized into inventory management registry.'),
                                                                                           (1, 'CUSTODY_ALLOCATED', '2026-01-20 09:02:00', 2, 'Deployed out to Mehedi Hasan for initial operations onboarding.'),
                                                                                           (4, 'STATUS_REPORT_DAMAGE', '2026-06-12 16:45:00', 1, 'Sakib logged system connection dropped; physical capacitor blown.'),
                                                                                           (1, 'PEER_TRANSFER_COMPLETED', '2026-07-09 11:00:02', 3, 'P2P handoff verified. Route authorization verified by Mehedi and finalized by Fahim.');