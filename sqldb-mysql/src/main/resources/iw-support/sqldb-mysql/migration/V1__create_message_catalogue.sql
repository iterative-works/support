-- PURPOSE: Create message_catalogue and message_catalogue_history tables for SQL-based message storage
-- PURPOSE: Includes audit trigger to track message_text changes automatically (MySQL/MariaDB version)

-- Create message_catalogue table
CREATE TABLE message_catalogue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_key VARCHAR(255) NOT NULL,
    language VARCHAR(10) NOT NULL,
    message_text TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT uq_message_catalogue_key_lang UNIQUE (message_key, language)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create indexes for message_catalogue
CREATE INDEX idx_message_catalogue_key_lang ON message_catalogue(message_key, language);
CREATE INDEX idx_message_catalogue_language ON message_catalogue(language);
CREATE INDEX idx_message_catalogue_updated_at ON message_catalogue(updated_at);

-- Create message_catalogue_history table for audit trail
CREATE TABLE message_catalogue_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_catalogue_id BIGINT NOT NULL,
    message_key VARCHAR(255) NOT NULL,
    language VARCHAR(10) NOT NULL,
    old_message_text TEXT,
    new_message_text TEXT NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by VARCHAR(255),
    change_reason TEXT,
    CONSTRAINT fk_message_catalogue
        FOREIGN KEY (message_catalogue_id)
        REFERENCES message_catalogue(id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create indexes for message_catalogue_history
CREATE INDEX idx_message_history_catalogue_id ON message_catalogue_history(message_catalogue_id);
CREATE INDEX idx_message_history_changed_at ON message_catalogue_history(changed_at);

-- Create trigger to log message_text changes
-- Flyway supports DELIMITER statement for MySQL stored procedures and triggers

DELIMITER //

CREATE TRIGGER message_catalogue_audit
AFTER UPDATE ON message_catalogue
FOR EACH ROW
BEGIN
    -- Only log if message_text actually changed
    -- MySQL doesn't have IS DISTINCT FROM, so we need explicit NULL handling
    IF (OLD.message_text IS NULL AND NEW.message_text IS NOT NULL) OR
       (OLD.message_text IS NOT NULL AND NEW.message_text IS NULL) OR
       (OLD.message_text <> NEW.message_text) THEN
        INSERT INTO message_catalogue_history (
            message_catalogue_id,
            message_key,
            language,
            old_message_text,
            new_message_text,
            changed_at,
            changed_by,
            change_reason
        ) VALUES (
            NEW.id,
            NEW.message_key,
            NEW.language,
            OLD.message_text,
            NEW.message_text,
            NEW.updated_at,
            NEW.updated_by,
            NULL
        );
    END IF;
END //

DELIMITER ;

-- Note: MySQL/MariaDB automatically updates updated_at with ON UPDATE CURRENT_TIMESTAMP
-- in the table definition (line 12), so we don't need a separate trigger for that
