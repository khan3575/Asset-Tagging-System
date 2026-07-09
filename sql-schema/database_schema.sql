-- database drop-create --
DROP DATABASE IF EXISTS asset_tagging_system;
CREATE DATABASE asset_tagging_system;
USE asset_tagging_system;

-- 1. Reference Tables (Lookup Tables)
CREATE TABLE departments (
                             id BIGINT PRIMARY KEY AUTO_INCREMENT,
                             name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE roles (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       name VARCHAR(50) NOT NULL UNIQUE DEFAULT 'ROLE_EMPLOYEE'
);

CREATE TABLE asset_categories (
                                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                  name VARCHAR(50) NOT NULL UNIQUE,
                                  depreciation_rate_percentage DECIMAL(5,2) DEFAULT 0.00
);

-- 2. Core Entities
CREATE TABLE users (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       first_name VARCHAR(60) NOT NULL,
                       last_name VARCHAR(60) NOT NULL,
                       email VARCHAR(100) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       dept_id BIGINT,
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
                        name VARCHAR(100) NOT NULL,
                        category_id BIGINT NOT NULL,
                        purchase_date DATE NOT NULL,
                        value DECIMAL(10, 2) NOT NULL,

    -- Store as VARCHAR for easy modifications, but validate values
                        status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    -- CHECK CONSTRAINT: Enforces data integrity without the rigidity of an ENUM type
                        CONSTRAINT chk_asset_status CHECK (status IN ('AVAILABLE', 'ASSIGNED', 'DAMAGED', 'RETIRED'))
);

-- Vertically Partitioned Document Table
CREATE TABLE asset_documents (
                                 asset_id BIGINT PRIMARY KEY,
                                 asset_image LONGBLOB NULL,
                                 invoice_pdf LONGBLOB NULL,
                                 image_mime_type VARCHAR(50),
                                 invoice_mime_type VARCHAR(50),
                                 updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                 CONSTRAINT fk_doc_asset FOREIGN KEY (asset_id) REFERENCES assets(id) ON DELETE CASCADE
);

-- 3. Transaction & Workflow Tables
CREATE TABLE asset_custody (
                               id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               asset_id BIGINT NOT NULL,
                               custodian_id BIGINT NOT NULL,
                               custody_start DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               custody_end DATETIME NULL,

    -- Fixed to match the rest of your modern architecture
                               status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',

                               CONSTRAINT fk_custody_asset FOREIGN KEY (asset_id) REFERENCES assets(id),
                               CONSTRAINT fk_custody_user FOREIGN KEY (custodian_id) REFERENCES users(id),
                               CONSTRAINT chk_custody_status CHECK (status IN ('ACTIVE', 'RELEASED'))
);
-- 🔥 FIXED: Replaced rigid ENUM with VARCHAR + CHECK constraint for modern Java/Spring alignment
CREATE TABLE approvals (
                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                           asset_id BIGINT NOT NULL,
                           requester_id BIGINT NOT NULL,          -- Employee 1 (Who wants it)
                           previous_holder_id BIGINT NULL,        -- Employee 2 (Who has it now - NULL if from inventory)

    -- Two-Tier Multi-Manager Approvals
                           first_approver_id BIGINT NULL,         -- Admin 1
                           final_approver_id BIGINT NULL,         -- Admin 2

    -- Modern State Tracker using VARCHAR
                           status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                           request_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                           first_action_date DATETIME NULL,       -- When Admin 1 signed off
                           final_action_date DATETIME NULL,       -- When Admin 2 signed off

    -- Relationships
                           CONSTRAINT fk_appr_asset FOREIGN KEY (asset_id) REFERENCES assets(id),
                           CONSTRAINT fk_appr_req FOREIGN KEY (requester_id) REFERENCES users(id),
                           CONSTRAINT fk_appr_prev FOREIGN KEY (previous_holder_id) REFERENCES users(id),
                           CONSTRAINT fk_appr_admin1 FOREIGN KEY (first_approver_id) REFERENCES users(id),
                           CONSTRAINT fk_appr_admin2 FOREIGN KEY (final_approver_id) REFERENCES users(id),

    -- 🔥 CHECK CONSTRAINT: Enforces the exact multi-tier lifecycle states safely
                           CONSTRAINT chk_approval_status CHECK (status IN ('PENDING', 'FIRST_APPROVED', 'APPROVED', 'REJECTED'))
);

CREATE TABLE asset_history (
                               id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               asset_id BIGINT NOT NULL,
                               action VARCHAR(100) NOT NULL,
                               action_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                               performed_by_user_id BIGINT NOT NULL,
                               notes TEXT NULL,

                               CONSTRAINT fk_hist_asset FOREIGN KEY (asset_id) REFERENCES assets(id),
                               CONSTRAINT fk_hist_user FOREIGN KEY (performed_by_user_id) REFERENCES users(id)
);