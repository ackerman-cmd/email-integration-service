--liquibase formatted sql

--changeset system:006-create-provider-event
CREATE TABLE IF NOT EXISTS email_service.provider_event
(
    id                  UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    provider            VARCHAR(50)  NOT NULL DEFAULT 'RESEND',
    event_type          VARCHAR(100) NOT NULL,
    provider_event_id   VARCHAR(255),
    provider_message_id VARCHAR(255),
    payload_json        JSONB        NOT NULL,
    deduplication_key   VARCHAR(500) NOT NULL UNIQUE,
    status              VARCHAR(50)  NOT NULL DEFAULT 'RECEIVED',
    error_text          TEXT,
    received_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    processed_at        TIMESTAMPTZ
);

CREATE INDEX idx_provider_event_dedup ON email_service.provider_event (deduplication_key);
CREATE INDEX idx_provider_event_status ON email_service.provider_event (status);
CREATE INDEX idx_provider_event_provider_message ON email_service.provider_event (provider_message_id);

--rollback DROP TABLE IF EXISTS email_service.provider_event;
