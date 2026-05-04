ALTER TABLE clinic_notification_settings
    ADD COLUMN IF NOT EXISTS whatsapp_reply_processing_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS reply_window_hours INTEGER NOT NULL DEFAULT 72,
    ADD COLUMN IF NOT EXISTS save_reply_preview BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE notification_outbox
    ADD COLUMN IF NOT EXISTS reminder_type_code VARCHAR(64),
    ADD COLUMN IF NOT EXISTS correlation_code VARCHAR(32),
    ADD COLUMN IF NOT EXISTS reply_status VARCHAR(32) NOT NULL DEFAULT 'NONE',
    ADD COLUMN IF NOT EXISTS reply_received_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS reply_text_preview VARCHAR(200),
    ADD COLUMN IF NOT EXISTS reply_processed_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS ix_notification_outbox_correlation_code
    ON notification_outbox (correlation_code);

CREATE INDEX IF NOT EXISTS ix_notification_outbox_phone_status_created
    ON notification_outbox (phone_e164, status, created_at);

CREATE INDEX IF NOT EXISTS ix_notification_outbox_patient_type_created
    ON notification_outbox (patient_id, reminder_type_code, created_at);
