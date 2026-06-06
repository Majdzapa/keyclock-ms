-- ================================================================
-- Flyway Migration V1: Create certificates table
-- CertManager Platform
-- ================================================================

CREATE TABLE IF NOT EXISTS certificates
(
    id                  BIGINT          NOT NULL AUTO_INCREMENT,

    -- File metadata
    filename            VARCHAR(255)    NOT NULL,
    mime_type           VARCHAR(100),

    -- Parsed certificate fields
    issuer              TEXT,
    subject             TEXT,
    owner               VARCHAR(512),
    serial_number       VARCHAR(255),
    valid_from          DATETIME(6),
    valid_to            DATETIME(6),
    algorithm           VARCHAR(128),
    fingerprint         VARCHAR(512),
    sans                TEXT            COMMENT 'Subject Alternative Names, comma-separated',
    expiration_date     DATETIME(6)     COMMENT 'Alias of valid_to for scheduler queries',

    -- Origin metadata
    source_type         ENUM('MANUAL_UPLOAD', 'REMOTE_URL') NOT NULL,
    remote_url          VARCHAR(2048),
    uploaded_by         VARCHAR(255)    NOT NULL,
    uploaded_by_group   VARCHAR(255),

    -- Raw certificate storage
    certificate_content LONGBLOB        COMMENT 'Raw file bytes',

    -- Auditing
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_uploaded_by_group (uploaded_by_group),
    INDEX idx_expiration_date (expiration_date),
    INDEX idx_source_type (source_type),
    INDEX idx_valid_to (valid_to)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
