CREATE TABLE clinic_notification_settings (
    id UUID PRIMARY KEY,
    clinic_id UUID NOT NULL,
    whatsapp_enabled BOOLEAN NOT NULL,
    waha_session_name VARCHAR(255) NOT NULL,
    sms_fallback_enabled BOOLEAN NOT NULL,
    default_preference VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX ux_clinic_notification_settings_clinic_id
    ON clinic_notification_settings (clinic_id);

CREATE TABLE notification_outbox (
    id UUID PRIMARY KEY,
    clinic_id UUID NOT NULL,
    doctor_id UUID,
    patient_id VARCHAR(255),
    patient_name VARCHAR(255) NOT NULL,
    phone_e164 VARCHAR(32) NOT NULL,
    appointment_external_id VARCHAR(255),
    appointment_date_time TIMESTAMPTZ,
    message_text TEXT NOT NULL,
    preference VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    channel_used VARCHAR(32),
    fallback_used BOOLEAN NOT NULL DEFAULT FALSE,
    provider_message_id VARCHAR(255),
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_notification_outbox_appointment_external_id
    ON notification_outbox (appointment_external_id);

CREATE INDEX ix_notification_outbox_patient_id
    ON notification_outbox (patient_id);

CREATE INDEX ix_notification_outbox_status
    ON notification_outbox (status);

CREATE INDEX ix_notification_outbox_created_at
    ON notification_outbox (created_at);

CREATE TABLE notification_attempt (
    id UUID PRIMARY KEY,
    notification_outbox_id UUID NOT NULL REFERENCES notification_outbox (id) ON DELETE CASCADE,
    channel VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    provider_message_id VARCHAR(255),
    error_message TEXT,
    attempted_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_notification_attempt_notification_outbox_id
    ON notification_attempt (notification_outbox_id);
