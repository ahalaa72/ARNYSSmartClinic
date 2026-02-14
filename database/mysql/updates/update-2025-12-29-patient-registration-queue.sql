-- ============================================================================
-- Patient Self-Registration Module - Database Schema
-- Version: 1.0.0
-- Date: 2025-12-29
--
-- Description: Creates tables for the patient self-registration feature.
--              This is a standalone commercial module that can be enabled/disabled.
--
-- Tables Created:
--   - patient_registration_queue: Stores pending patient registrations
--   - registration_tokens: Manages session tokens for QR code scans
--   - registration_module_config: Module configuration per facility
--
-- Installation: Run this script once per database installation.
-- Rollback: See bottom of file for DROP statements.
-- ============================================================================

-- ============================================================================
-- Table: registration_module_config
-- Purpose: Stores module configuration per facility (multi-clinic support)
-- ============================================================================
CREATE TABLE IF NOT EXISTS registration_module_config (
    id INT AUTO_INCREMENT PRIMARY KEY,
    facility_id INT DEFAULT 1,

    -- Module Status
    module_enabled TINYINT(1) DEFAULT 1,

    -- Token Configuration
    token_expiry_minutes INT DEFAULT 30,
    max_sessions_per_ip_per_hour INT DEFAULT 5,
    max_submissions_per_ip_per_hour INT DEFAULT 5,

    -- QR Code Configuration
    qr_code_url VARCHAR(500),
    clinic_name VARCHAR(100),
    clinic_phone VARCHAR(20),
    clinic_address VARCHAR(255),

    -- Default Values
    default_province VARCHAR(20) DEFAULT 'ON',
    default_country VARCHAR(50) DEFAULT 'Canada',
    default_language VARCHAR(10) DEFAULT 'en',

    -- Mandatory Fields (JSON array of field names)
    mandatory_fields TEXT,

    -- Email Configuration
    email_notifications_enabled TINYINT(1) DEFAULT 1,
    staff_notification_email VARCHAR(100),
    supported_languages VARCHAR(100) DEFAULT 'en,fr,ar,hi,zh',

    -- Branding
    logo_url VARCHAR(500),
    primary_color VARCHAR(7) DEFAULT '#007bff',

    -- Audit
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_update_user VARCHAR(6),

    UNIQUE KEY uk_facility (facility_id),
    INDEX idx_enabled (module_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- Table: registration_tokens
-- Purpose: Manages session tokens generated when patients scan QR code
-- ============================================================================
CREATE TABLE IF NOT EXISTS registration_tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,

    -- Token Data
    token VARCHAR(64) NOT NULL,
    facility_id INT DEFAULT 1,

    -- Timestamps
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    used_at DATETIME,

    -- Status
    used TINYINT(1) DEFAULT 0,

    -- Security/Rate Limiting
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),

    -- Audit
    last_update_user VARCHAR(6),
    last_update_date DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_token (token),
    INDEX idx_expires (expires_at),
    INDEX idx_facility (facility_id),
    INDEX idx_ip_created (ip_address, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- Table: patient_registration_queue
-- Purpose: Stores pending patient registrations awaiting staff review
-- ============================================================================
CREATE TABLE IF NOT EXISTS patient_registration_queue (
    id INT AUTO_INCREMENT PRIMARY KEY,

    -- Registration Metadata
    registration_token VARCHAR(64) NOT NULL,
    facility_id INT DEFAULT 1,
    status ENUM('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED') DEFAULT 'PENDING',

    -- Timestamps
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME,
    submitted_at DATETIME,
    reviewed_at DATETIME,

    -- Review Info
    reviewed_by VARCHAR(6),
    rejection_reason TEXT,

    -- Patient Demographics (mirrors demographic table)
    first_name VARCHAR(30),
    last_name VARCHAR(30),
    middle_names VARCHAR(100),
    pref_name VARCHAR(30),
    title VARCHAR(10),

    -- Date of Birth (stored as separate fields like demographic table)
    year_of_birth VARCHAR(4),
    month_of_birth VARCHAR(2),
    date_of_birth VARCHAR(2),

    -- Gender/Sex
    sex CHAR(1),
    gender VARCHAR(25),
    pronoun VARCHAR(25),

    -- Contact Information
    address VARCHAR(60),
    city VARCHAR(50),
    province VARCHAR(20),
    postal VARCHAR(9),
    phone VARCHAR(20),
    phone2 VARCHAR(20),
    cell_phone VARCHAR(20),
    email VARCHAR(100),
    consent_email TINYINT(1) DEFAULT 0,

    -- Health Card Information
    hin VARCHAR(20),
    ver CHAR(3),
    hc_type VARCHAR(20),
    hc_renew_date DATE,

    -- Additional Information
    official_lang VARCHAR(60),
    spoken_lang VARCHAR(60),
    country_of_origin CHAR(4),

    -- Emergency Contact (new field not in demographic)
    emergency_contact_name VARCHAR(60),
    emergency_contact_phone VARCHAR(20),
    emergency_contact_relationship VARCHAR(30),

    -- Validation Results
    hin_validation_status ENUM('VALID', 'INVALID', 'NOT_VALIDATED') DEFAULT 'NOT_VALIDATED',
    hin_validation_error VARCHAR(255),
    duplicate_check_status ENUM('CLEAR', 'POTENTIAL_DUPLICATE', 'CONFIRMED_DUPLICATE') DEFAULT 'CLEAR',
    duplicate_warning TEXT,
    potential_duplicate_ids VARCHAR(255),

    -- Final Result
    demographic_no INT,

    -- Audit Fields
    last_update_user VARCHAR(6),
    last_update_date DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Indexes
    UNIQUE KEY uk_token (registration_token),
    INDEX idx_status (status),
    INDEX idx_facility_status (facility_id, status),
    INDEX idx_created (created_at),
    INDEX idx_submitted (submitted_at),
    INDEX idx_hin (hin),
    INDEX idx_name_dob (last_name, first_name, year_of_birth, month_of_birth, date_of_birth),
    INDEX idx_demographic_no (demographic_no),

    -- Foreign Keys
    CONSTRAINT fk_prq_facility FOREIGN KEY (facility_id)
        REFERENCES registration_module_config(facility_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- Table: registration_rate_limit
-- Purpose: Tracks API usage for rate limiting (per IP address)
-- ============================================================================
CREATE TABLE IF NOT EXISTS registration_rate_limit (
    id INT AUTO_INCREMENT PRIMARY KEY,

    ip_address VARCHAR(45) NOT NULL,
    endpoint VARCHAR(50) NOT NULL,
    request_count INT DEFAULT 1,
    window_start DATETIME NOT NULL,
    window_end DATETIME NOT NULL,

    INDEX idx_ip_endpoint_window (ip_address, endpoint, window_start),
    INDEX idx_window_end (window_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- Default Configuration Insert
-- ============================================================================
INSERT INTO registration_module_config (
    facility_id,
    module_enabled,
    token_expiry_minutes,
    max_sessions_per_ip_per_hour,
    max_submissions_per_ip_per_hour,
    clinic_name,
    default_province,
    default_country,
    default_language,
    mandatory_fields,
    email_notifications_enabled,
    supported_languages
) VALUES (
    1,
    1,
    30,
    5,
    5,
    'Medical Clinic',
    'ON',
    'Canada',
    'en',
    '["first_name","last_name","year_of_birth","month_of_birth","date_of_birth","sex","address","city","province","postal","cell_phone","email"]',
    1,
    'en,fr,ar,hi,zh'
) ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- ============================================================================
-- ROLLBACK SCRIPT (Run to remove module completely)
-- ============================================================================
-- DROP TABLE IF EXISTS registration_rate_limit;
-- DROP TABLE IF EXISTS patient_registration_queue;
-- DROP TABLE IF EXISTS registration_tokens;
-- DROP TABLE IF EXISTS registration_module_config;

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================
-- SELECT 'registration_module_config' AS table_name, COUNT(*) AS row_count FROM registration_module_config
-- UNION ALL
-- SELECT 'registration_tokens', COUNT(*) FROM registration_tokens
-- UNION ALL
-- SELECT 'patient_registration_queue', COUNT(*) FROM patient_registration_queue
-- UNION ALL
-- SELECT 'registration_rate_limit', COUNT(*) FROM registration_rate_limit;
