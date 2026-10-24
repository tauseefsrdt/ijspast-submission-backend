-- ========================================================================
-- IJSPAST Journal Management System - MySQL Database Schema
-- International Journal of Scientific Progress in Applied Science and Technology
-- Shri Ramswaroop Memorial University (SRMU)
-- ========================================================================

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    institution VARCHAR(255),
    department VARCHAR(255),
    domain VARCHAR(255),
    phone VARCHAR(64),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    account_setup_completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_email (email),
    INDEX idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS authors (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) UNIQUE,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    institution VARCHAR(255) NOT NULL,
    department VARCHAR(255),
    city_country VARCHAR(255) NOT NULL,
    orcid VARCHAR(64) NOT NULL,
    phone VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_author_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_authors_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS reviewers (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) UNIQUE,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    institution VARCHAR(255) NOT NULL,
    department VARCHAR(255),
    domain VARCHAR(255) NOT NULL,
    phone VARCHAR(64),
    active_assignments_count INT DEFAULT 0,
    completed_reviews_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_reviewer_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_reviewers_email (email),
    INDEX idx_reviewers_domain (domain)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS manuscripts (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    domain VARCHAR(255) NOT NULL,
    abstract_text TEXT NOT NULL,
    keywords VARCHAR(500) NOT NULL,
    coauthors TEXT,
    author_id VARCHAR(64) NOT NULL,
    author_ref_id VARCHAR(64),
    author_name VARCHAR(255) NOT NULL,
    author_email VARCHAR(255) NOT NULL,
    author_institution VARCHAR(255) NOT NULL,
    author_department VARCHAR(255),
    author_city_country VARCHAR(255) NOT NULL,
    author_orcid VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    submission_date VARCHAR(32) NOT NULL,
    similarity_score INT DEFAULT 6,
    assigned_reviewer_id VARCHAR(64),
    assigned_reviewer_name VARCHAR(255),
    assigned_reviewer2_id VARCHAR(64),
    assigned_reviewer2_name VARCHAR(255),
    assigned_reviewer1_ref_id VARCHAR(64),
    assigned_reviewer2_ref_id VARCHAR(64),
    assigned_date VARCHAR(32),
    review_deadline VARCHAR(32),
    revision_request_comments TEXT,
    author_revision_response TEXT,
    revision_forwarded_to_reviewer BOOLEAN DEFAULT FALSE,
    reviewer_feedback_for_author TEXT,
    reviewer_feedback_for_editor TEXT,
    rejection_reason TEXT,
    publication_doi VARCHAR(128),
    publication_volume VARCHAR(64),
    publication_issue VARCHAR(64),
    publication_date VARCHAR(32),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_manuscript_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_manuscript_author_ref FOREIGN KEY (author_ref_id) REFERENCES authors(id) ON DELETE SET NULL,
    CONSTRAINT fk_manuscript_reviewer FOREIGN KEY (assigned_reviewer_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_manuscript_reviewer2 FOREIGN KEY (assigned_reviewer2_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_manuscript_reviewer1_ref FOREIGN KEY (assigned_reviewer1_ref_id) REFERENCES reviewers(id) ON DELETE SET NULL,
    CONSTRAINT fk_manuscript_reviewer2_ref FOREIGN KEY (assigned_reviewer2_ref_id) REFERENCES reviewers(id) ON DELETE SET NULL,
    INDEX idx_manuscripts_author (author_id),
    INDEX idx_manuscripts_author_ref (author_ref_id),
    INDEX idx_manuscripts_status (status),
    INDEX idx_manuscripts_reviewer (assigned_reviewer_id),
    INDEX idx_manuscripts_reviewer2 (assigned_reviewer2_id),
    INDEX idx_manuscripts_reviewer1_ref (assigned_reviewer1_ref_id),
    INDEX idx_manuscripts_reviewer2_ref (assigned_reviewer2_ref_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS manuscript_files (
    id VARCHAR(64) PRIMARY KEY,
    manuscript_id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    size BIGINT NOT NULL,
    type VARCHAR(128) NOT NULL,
    file_key VARCHAR(255) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    is_revised BOOLEAN NOT NULL DEFAULT FALSE,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_files_manuscript FOREIGN KEY (manuscript_id) REFERENCES manuscripts(id) ON DELETE CASCADE,
    INDEX idx_files_manuscript (manuscript_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS status_history_items (
    id VARCHAR(64) PRIMARY KEY,
    manuscript_id VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    date VARCHAR(64) NOT NULL,
    actor VARCHAR(255) NOT NULL,
    role VARCHAR(64) NOT NULL,
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_manuscript FOREIGN KEY (manuscript_id) REFERENCES manuscripts(id) ON DELETE CASCADE,
    INDEX idx_history_manuscript (manuscript_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS review_reports (
    id VARCHAR(64) PRIMARY KEY,
    manuscript_id VARCHAR(64) NOT NULL,
    reviewer_id VARCHAR(64) NOT NULL,
    reviewer_name VARCHAR(255) NOT NULL,
    recommendation VARCHAR(64) NOT NULL,
    technical_quality INT NOT NULL,
    originality INT NOT NULL,
    clarity INT NOT NULL,
    comments_for_editor TEXT NOT NULL,
    comments_for_author TEXT NOT NULL,
    confidential_comments TEXT,
    stage INT NOT NULL DEFAULT 1,
    stage_name VARCHAR(128) DEFAULT 'Stage 1: Initial Assessment',
    rubric_scores TEXT,
    overall_score DOUBLE,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_manuscript FOREIGN KEY (manuscript_id) REFERENCES manuscripts(id) ON DELETE CASCADE,
    CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_review_manuscript (manuscript_id),
    INDEX idx_review_reviewer (reviewer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reset_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_reset_token_hash (token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS account_setup_tokens (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_setup_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_setup_token_hash (token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS email_notification_logs (
    id VARCHAR(64) PRIMARY KEY,
    recipient_email VARCHAR(255) NOT NULL,
    recipient_name VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    email_type VARCHAR(64) NOT NULL,
    manuscript_id VARCHAR(64),
    content TEXT NOT NULL,
    sent_at VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL DEFAULT 'Delivered',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email_logs_type (email_type),
    INDEX idx_email_logs_recipient (recipient_email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
