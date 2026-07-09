DROP DATABASE IF EXISTS asset_tagging_system;

CREATE DATABASE asset_tagging_system
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE asset_tagging_system;

CREATE TABLE departments (
                             id BIGINT PRIMARY KEY AUTO_INCREMENT,
                             name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE roles (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE asset_categories (
                                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                  name VARCHAR(50) NOT NULL UNIQUE,
                                  depreciation_rate_percentage DECIMAL(5,2) DEFAULT 0.00
);

CREATE TABLE users (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       first_name VARCHAR(60) NOT NULL,
                       last_name VARCHAR(60) NOT NULL,
                       email VARCHAR(100) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       dept_id BIGINT NOT NULL,
                       enabled TINYINT(1) DEFAULT 1,
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

                       CONSTRAINT fk_user_dept FOREIGN KEY (dept_id) REFERENCES departments(id)
);

CREATE TABLE user_role (
                           user_id BIGINT NOT NULL,
                           role_id BIGINT NOT NULL,
                           PRIMARY KEY (user_id, role_id),

                           CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                           CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE TABLE assets (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        asset_tag VARCHAR(50) NOT NULL UNIQUE,
                        name VARCHAR(100) NOT NULL,
                        category_id BIGINT NOT NULL,
                        purchase_date DATE NOT NULL,
                        value DECIMAL(10, 2) NOT NULL,
                        status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
                        created_by_user_id BIGINT NOT NULL,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

                        CONSTRAINT chk_asset_status CHECK (status IN ('AVAILABLE', 'ASSIGNED', 'DAMAGED', 'RETIRED')),

                        CONSTRAINT fk_asset_category FOREIGN KEY (category_id) REFERENCES asset_categories(id),
                        CONSTRAINT fk_asset_created_by FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);

CREATE TABLE asset_documents (
                                 asset_id BIGINT PRIMARY KEY,
                                 asset_image LONGBLOB NULL,
                                 invoice_pdf LONGBLOB NULL,
                                 image_mime_type VARCHAR(50),
                                 invoice_mime_type VARCHAR(50),
                                 updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                 CONSTRAINT fk_doc_asset FOREIGN KEY (asset_id) REFERENCES assets(id) ON DELETE CASCADE
);

CREATE TABLE approvals (
                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                           asset_id BIGINT NOT NULL,
                           initiated_by_user_id BIGINT NOT NULL,
                           requester_id BIGINT NOT NULL,
                           previous_holder_id BIGINT NULL,

                           request_type VARCHAR(30) NOT NULL,
                           required_approval_count TINYINT NOT NULL DEFAULT 2,

                           first_approver_id BIGINT NULL,
                           final_approver_id BIGINT NULL,

                           status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                           request_reason TEXT NULL,
                           first_approver_notes TEXT NULL,
                           final_approver_notes TEXT NULL,
                           rejection_reason TEXT NULL,
                           request_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                           first_action_date DATETIME NULL,
                           final_action_date DATETIME NULL,
                           cancelled_at DATETIME NULL,

                           version INT NOT NULL DEFAULT 0,

                           CONSTRAINT fk_appr_asset FOREIGN KEY (asset_id) REFERENCES assets(id),
                           CONSTRAINT fk_appr_initiator FOREIGN KEY (initiated_by_user_id) REFERENCES users(id),
                           CONSTRAINT fk_appr_req FOREIGN KEY (requester_id) REFERENCES users(id),
                           CONSTRAINT fk_appr_prev FOREIGN KEY (previous_holder_id) REFERENCES users(id),
                           CONSTRAINT fk_appr_admin1 FOREIGN KEY (first_approver_id) REFERENCES users(id),
                           CONSTRAINT fk_appr_admin2 FOREIGN KEY (final_approver_id) REFERENCES users(id),

                           CONSTRAINT chk_request_type CHECK (request_type IN ('ASSET_REQUEST', 'TRANSFER_REQUEST', 'RETURN_REQUEST')),

                           CONSTRAINT chk_approval_status CHECK (status IN ('PENDING', 'FIRST_APPROVED', 'APPROVED', 'REJECTED', 'CANCELLED')),

                           CONSTRAINT chk_required_approval_count CHECK (required_approval_count IN (1, 2)),
                           CONSTRAINT chk_return_single_approval CHECK (request_type <> 'RETURN_REQUEST' OR required_approval_count = 1),

                           CONSTRAINT chk_appr_distinct_approvers CHECK (final_approver_id IS NULL OR first_approver_id IS NULL OR final_approver_id <> first_approver_id),
                           CONSTRAINT chk_appr_not_self_transfer CHECK (previous_holder_id IS NULL OR previous_holder_id <> requester_id)
);

CREATE TABLE asset_custody (
                               id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               asset_id BIGINT NOT NULL,
                               custodian_id BIGINT NOT NULL,
                               approval_id BIGINT NULL,
                               assigned_by_user_id BIGINT NULL,
                               custody_start DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               custody_end DATETIME NULL,

                               status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',

                               active_asset_id BIGINT AS (CASE WHEN status = 'ACTIVE' THEN asset_id END) STORED,

                               CONSTRAINT fk_custody_asset FOREIGN KEY (asset_id) REFERENCES assets(id),
                               CONSTRAINT fk_custody_user FOREIGN KEY (custodian_id) REFERENCES users(id),
                               CONSTRAINT fk_custody_approval FOREIGN KEY (approval_id) REFERENCES approvals(id),
                               CONSTRAINT fk_custody_assigned_by FOREIGN KEY (assigned_by_user_id) REFERENCES users(id),
                               CONSTRAINT chk_custody_status CHECK (status IN ('ACTIVE', 'RELEASED')),
                               CONSTRAINT chk_custody_dates CHECK (custody_end IS NULL OR custody_end >= custody_start),
                               CONSTRAINT uq_one_active_custody_per_asset UNIQUE (active_asset_id)
);

CREATE TABLE asset_history (
                               id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               asset_id BIGINT NOT NULL,
                               action VARCHAR(100) NOT NULL,
                               previous_status VARCHAR(50) NULL,
                               new_status VARCHAR(50) NULL,
                               previous_holder_id BIGINT NULL,
                               new_holder_id BIGINT NULL,
                               action_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                               performed_by_user_id BIGINT NOT NULL,
                               approval_id BIGINT NULL,
                               notes TEXT NULL,

                               CONSTRAINT fk_hist_asset FOREIGN KEY (asset_id) REFERENCES assets(id),
                               CONSTRAINT fk_hist_prev_holder FOREIGN KEY (previous_holder_id) REFERENCES users(id),
                               CONSTRAINT fk_hist_new_holder FOREIGN KEY (new_holder_id) REFERENCES users(id),
                               CONSTRAINT fk_hist_user FOREIGN KEY (performed_by_user_id) REFERENCES users(id),
                               CONSTRAINT fk_hist_approval FOREIGN KEY (approval_id) REFERENCES approvals(id)
);

CREATE INDEX idx_asset_status ON assets(status);
CREATE INDEX idx_custody_status ON asset_custody(status);
CREATE INDEX idx_custody_asset_status ON asset_custody(asset_id, status);
CREATE INDEX idx_approval_status ON approvals(status);
CREATE INDEX idx_approval_status_type ON approvals(status, request_type);
CREATE INDEX idx_history_action ON asset_history(action);
CREATE INDEX idx_history_date ON asset_history(action_date);

CREATE VIEW available_assets_view AS
SELECT
    a.id, a.asset_tag, a.name, ac.name AS category,
    a.purchase_date, a.value, a.status
FROM assets a
JOIN asset_categories ac ON ac.id = a.category_id
WHERE a.status = 'AVAILABLE';

CREATE VIEW assigned_assets_view AS
SELECT
    a.id AS asset_id, a.asset_tag, a.name AS asset_name,
    ac.name AS category,
    u.id AS employee_id,
    CONCAT(u.first_name, ' ', u.last_name) AS employee_name,
    u.email, d.name AS department,
    c.custody_start, c.status AS custody_status
FROM asset_custody c
JOIN assets a ON a.id = c.asset_id
JOIN asset_categories ac ON ac.id = a.category_id
JOIN users u ON u.id = c.custodian_id
LEFT JOIN departments d ON d.id = u.dept_id
WHERE c.status = 'ACTIVE';

CREATE VIEW asset_summary_view AS
SELECT
    COUNT(*) AS total_assets,
    SUM(CASE WHEN status = 'AVAILABLE' THEN 1 ELSE 0 END) AS available_assets,
    SUM(CASE WHEN status = 'ASSIGNED' THEN 1 ELSE 0 END) AS assigned_assets,
    SUM(CASE WHEN status = 'DAMAGED' THEN 1 ELSE 0 END) AS damaged_assets,
    SUM(CASE WHEN status = 'RETIRED' THEN 1 ELSE 0 END) AS retired_assets,
    SUM(value) AS total_purchase_value
FROM assets;

CREATE VIEW pending_approvals_view AS
SELECT
    ap.id AS approval_id, ap.request_type, ap.status, ap.required_approval_count,
    a.id AS asset_id, a.asset_tag, a.name AS asset_name,
    CONCAT(requester.first_name, ' ', requester.last_name) AS requester_name,
    requester.email AS requester_email,
    CONCAT(previous_holder.first_name, ' ', previous_holder.last_name) AS previous_holder_name,
    ap.request_reason, ap.request_date
FROM approvals ap
JOIN assets a ON a.id = ap.asset_id
JOIN users requester ON requester.id = ap.requester_id
LEFT JOIN users previous_holder ON previous_holder.id = ap.previous_holder_id
WHERE ap.status IN ('PENDING', 'FIRST_APPROVED');
