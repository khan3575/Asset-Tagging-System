DROP DATABASE IF EXISTS asset_tagging_system;

CREATE DATABASE asset_tagging_system
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE asset_tagging_system;

CREATE TABLE departments (
                             id BIGINT PRIMARY KEY AUTO_INCREMENT,
                             name VARCHAR(50) NOT NULL UNIQUE,
                             enabled TINYINT(1) NOT NULL DEFAULT 1
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
                       enabled TINYINT(1) NOT NULL DEFAULT 1,
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
                        enabled TINYINT(1) NOT NULL DEFAULT 1,

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



                           CONSTRAINT fk_appr_asset FOREIGN KEY (asset_id) REFERENCES assets(id),
                           CONSTRAINT fk_appr_initiator FOREIGN KEY (initiated_by_user_id) REFERENCES users(id),
                           CONSTRAINT fk_appr_req FOREIGN KEY (requester_id) REFERENCES users(id),
                           CONSTRAINT fk_appr_prev FOREIGN KEY (previous_holder_id) REFERENCES users(id),
                           CONSTRAINT fk_appr_admin1 FOREIGN KEY (first_approver_id) REFERENCES users(id),
                           CONSTRAINT fk_appr_admin2 FOREIGN KEY (final_approver_id) REFERENCES users(id)
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


