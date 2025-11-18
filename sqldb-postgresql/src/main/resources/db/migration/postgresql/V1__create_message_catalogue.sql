-- PURPOSE: Create message_catalogue and message_catalogue_history tables for SQL-based message storage
-- PURPOSE: Includes audit trigger to track message_text changes automatically

-- Create message_catalogue table
CREATE TABLE message_catalogue (
    id BIGSERIAL PRIMARY KEY,
    message_key VARCHAR(255) NOT NULL,
    language VARCHAR(10) NOT NULL,
    message_text TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT uq_message_catalogue_key_lang UNIQUE (message_key, language)
);

-- Create indexes for message_catalogue
CREATE INDEX idx_message_catalogue_key_lang ON message_catalogue(message_key, language);
CREATE INDEX idx_message_catalogue_language ON message_catalogue(language);
CREATE INDEX idx_message_catalogue_updated_at ON message_catalogue(updated_at);

-- Create message_catalogue_history table for audit trail
CREATE TABLE message_catalogue_history (
    id BIGSERIAL PRIMARY KEY,
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
);

-- Create indexes for message_catalogue_history
CREATE INDEX idx_message_history_catalogue_id ON message_catalogue_history(message_catalogue_id);
CREATE INDEX idx_message_history_changed_at ON message_catalogue_history(changed_at);

-- Create trigger function to log message_text changes
CREATE OR REPLACE FUNCTION message_catalogue_audit_trigger()
RETURNS TRIGGER AS $$
BEGIN
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
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger on message_catalogue table
CREATE TRIGGER message_catalogue_audit
AFTER UPDATE ON message_catalogue
FOR EACH ROW
WHEN (OLD.message_text IS DISTINCT FROM NEW.message_text)
EXECUTE FUNCTION message_catalogue_audit_trigger();

-- Create function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to update updated_at on every UPDATE
CREATE TRIGGER update_message_catalogue_updated_at
    BEFORE UPDATE ON message_catalogue
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
