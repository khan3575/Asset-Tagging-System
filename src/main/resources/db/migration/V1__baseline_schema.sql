-- departments
CREATE TABLE departments (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    closed_at   DATETIME     NULL,        -- NULL = open; closed departments are kept, never deleted
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- roles

CREATE TABLE roles (
    id    BIGINT PRIMARY KEY AUTO_INCREMENT,
    name  VARCHAR(50) NOT NULL UNIQUE,
    CONSTRAINT chk_role_name CHECK (name IN ('ROLE_ADMIN', 'ROLE_EMPLOYEE'))
);



-- asset categories 
CREATE TABLE asset_categories (
    id                            BIGINT PRIMARY KEY AUTO_INCREMENT,
    name                          VARCHAR(50)   NOT NULL UNIQUE,
    depreciation_rate_percentage  DECIMAL(5,2)  NOT NULL DEFAULT 0.00,
    retired_at                    DATETIME      NULL      -- NULL = still selectable for new assets
);




CREATE TABLE users (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    first_name     VARCHAR(60)  NOT NULL,
    last_name      VARCHAR(60)  NOT NULL,
    email          VARCHAR(100) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,   -- BCrypt. Renamed from password
    dept_id        BIGINT       NOT NULL,
    enabled        TINYINT(1)   NOT NULL DEFAULT 1,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_dept FOREIGN KEY (dept_id) REFERENCES departments(id),

    INDEX idx_user_dept    (dept_id),
    INDEX idx_user_enabled (enabled)
);

-- user role

CREATE TABLE user_role (
    user_id  BIGINT NOT NULL,
    role_id  BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),

    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);


-- assets

CREATE TABLE assets (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    asset_tag           VARCHAR(50)  NOT NULL UNIQUE,
    name                VARCHAR(100) NOT NULL,
    category_id         BIGINT       NOT NULL,
    purchase_date       DATE         NOT NULL,
    purchase_value      DECIMAL(12,2) NOT NULL,   -- renamed from value
    condition_status    VARCHAR(20)  NOT NULL DEFAULT 'IN_SERVICE',
    created_by_user_id  BIGINT       NOT NULL,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_asset_category   FOREIGN KEY (category_id)        REFERENCES asset_categories(id),
    CONSTRAINT fk_asset_created_by FOREIGN KEY (created_by_user_id) REFERENCES users(id),
    CONSTRAINT chk_asset_condition CHECK (condition_status IN
        ('IN_SERVICE', 'DAMAGED', 'UNDER_MAINTENANCE', 'BEYOND_REPAIR', 'RETIRED')),

    INDEX idx_asset_condition (condition_status),
    INDEX idx_asset_category  (category_id)
);


-- asset documents

CREATE TABLE asset_documents (
    asset_id           BIGINT PRIMARY KEY,
    asset_image        LONGBLOB    NULL,
    invoice_pdf        LONGBLOB    NULL,
    image_mime_type    VARCHAR(50) NULL,
    invoice_mime_type  VARCHAR(50) NULL,
    updated_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_doc_asset FOREIGN KEY (asset_id) REFERENCES assets(id) ON DELETE CASCADE
);

-- approvals

CREATE TABLE approvals (
    id                       BIGINT PRIMARY KEY AUTO_INCREMENT,
    asset_id                 BIGINT      NOT NULL,
    request_type             VARCHAR(20) NOT NULL,
    initiated_by_user_id     BIGINT      NOT NULL,   -- who started it
    requester_id             BIGINT      NULL,       -- incoming holder; NULL for a return
    previous_holder_id       BIGINT      NULL,       -- outgoing holder; NULL if no one holds it
    required_approval_count  TINYINT     NOT NULL DEFAULT 2,
    status                   VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    request_reason           TEXT        NULL,
    requested_at             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at                DATETIME    NULL,

    open_asset_id BIGINT AS (CASE WHEN status IN ('PENDING', 'PARTIALLY_APPROVED')
                                   THEN asset_id END) STORED,

    CONSTRAINT fk_appr_asset     FOREIGN KEY (asset_id)             REFERENCES assets(id),
    CONSTRAINT fk_appr_initiator FOREIGN KEY (initiated_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_appr_requester FOREIGN KEY (requester_id)         REFERENCES users(id),
    CONSTRAINT fk_appr_prev      FOREIGN KEY (previous_holder_id)   REFERENCES users(id),

    CONSTRAINT uq_one_open_request_per_asset UNIQUE (open_asset_id),
    CONSTRAINT chk_appr_status CHECK (status IN
        ('PENDING', 'PARTIALLY_APPROVED', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_appr_type CHECK (request_type IN
        ('ASSIGNMENT', 'TRANSFER', 'RETURN')),
    CONSTRAINT chk_appr_count CHECK (required_approval_count BETWEEN 1 AND 5),

    INDEX idx_appr_queue (status, requested_at),
    INDEX idx_appr_asset (asset_id, status)
);


-- approval actions

CREATE TABLE approval_actions (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    approval_id    BIGINT      NOT NULL,
    actor_user_id  BIGINT      NOT NULL,
    action         VARCHAR(20) NOT NULL,   -- APPROVED | REJECTED | CANCELLED
    sequence_no    TINYINT     NOT NULL,   -- 1 = first sign-off, 2 = second, ...
    notes          TEXT        NULL,
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_aa_approval FOREIGN KEY (approval_id)   REFERENCES approvals(id) ON DELETE CASCADE,
    CONSTRAINT fk_aa_actor    FOREIGN KEY (actor_user_id) REFERENCES users(id),
    CONSTRAINT chk_aa_action  CHECK (action IN ('APPROVED', 'REJECTED', 'CANCELLED')),

    CONSTRAINT uq_aa_one_action_per_actor UNIQUE (approval_id, actor_user_id),
    CONSTRAINT uq_aa_sequence             UNIQUE (approval_id, sequence_no)
);

-- asset custody

CREATE TABLE asset_custody (
    id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
    asset_id             BIGINT      NOT NULL,
    custodian_id         BIGINT      NOT NULL,
    approval_id          BIGINT      NULL,
    assigned_by_user_id  BIGINT      NOT NULL,
    custody_start        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    custody_end          DATETIME    NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    active_asset_id BIGINT AS (CASE WHEN status = 'ACTIVE' THEN asset_id END) STORED,

    CONSTRAINT fk_custody_asset       FOREIGN KEY (asset_id)            REFERENCES assets(id),
    CONSTRAINT fk_custody_custodian   FOREIGN KEY (custodian_id)        REFERENCES users(id),
    CONSTRAINT fk_custody_approval    FOREIGN KEY (approval_id)         REFERENCES approvals(id),
    CONSTRAINT fk_custody_assigned_by FOREIGN KEY (assigned_by_user_id) REFERENCES users(id),

    CONSTRAINT uq_one_active_custody_per_asset UNIQUE (active_asset_id),
    CONSTRAINT chk_custody_status CHECK (status IN ('ACTIVE', 'RELEASED')),
    CONSTRAINT chk_custody_dates  CHECK (custody_end IS NULL OR custody_end >= custody_start),

    INDEX idx_custody_custodian  (custodian_id, status),
    INDEX idx_custody_asset_time (asset_id, custody_start)
);


CREATE TABLE activity_log (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    occurred_at         DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    correlation_id      BINARY(16)  NOT NULL,
    sequence_in_action  SMALLINT    NOT NULL DEFAULT 1,

    actor_user_id       BIGINT       NULL,   -- NULL only when authentication itself failed
    actor_roles         VARCHAR(100) NULL,   -- e.g. 'ROLE_ADMIN' -- snapshot, not a join
    ip_address           VARCHAR(45)  NULL,

    action              VARCHAR(40) NOT NULL,
    entity_type         VARCHAR(20) NOT NULL,   -- ASSET | USER | APPROVAL | AUTH | DEPARTMENT
    outcome             VARCHAR(12) NOT NULL DEFAULT 'SUCCEEDED',
    failure_reason      VARCHAR(255) NULL,

    asset_id            BIGINT      NULL,
    approval_id         BIGINT      NULL,
    subject_user_id     BIGINT      NULL,   -- the user acted upon, not the actor

    previous_holder_id  BIGINT      NULL,
    new_holder_id       BIGINT      NULL,
    previous_condition  VARCHAR(20) NULL,
    new_condition       VARCHAR(20) NULL,

    summary             VARCHAR(255) NULL,  -- human-readable, for display
    details             JSON         NULL,  -- the genuinely variable remainder -- display only,
                                             -- no business logic may ever read from this column

    CONSTRAINT fk_log_actor    FOREIGN KEY (actor_user_id)      REFERENCES users(id),
    CONSTRAINT fk_log_asset    FOREIGN KEY (asset_id)           REFERENCES assets(id),
    CONSTRAINT fk_log_approval FOREIGN KEY (approval_id)        REFERENCES approvals(id),
    CONSTRAINT fk_log_subject  FOREIGN KEY (subject_user_id)    REFERENCES users(id),
    CONSTRAINT fk_log_prev     FOREIGN KEY (previous_holder_id) REFERENCES users(id),
    CONSTRAINT fk_log_new      FOREIGN KEY (new_holder_id)      REFERENCES users(id),

    CONSTRAINT chk_log_entity  CHECK (entity_type IN ('ASSET', 'USER', 'APPROVAL', 'AUTH', 'DEPARTMENT')),
    CONSTRAINT chk_log_outcome CHECK (outcome IN ('SUCCEEDED', 'DENIED', 'FAILED')),

    INDEX idx_log_time   (occurred_at),
    INDEX idx_log_corr   (correlation_id, sequence_in_action),
    INDEX idx_log_asset  (asset_id,      occurred_at),
    INDEX idx_log_actor  (actor_user_id, occurred_at),
    INDEX idx_log_type   (entity_type,   occurred_at),
    INDEX idx_log_action (action,        occurred_at)
);
