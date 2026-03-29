--liquibase formatted sql

--changeset system:007-create-outbox-event
CREATE TABLE IF NOT EXISTS email_service.outbox_event
(
    id             UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id   VARCHAR(255) NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload_json   JSONB        NOT NULL,
    status         VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    retry_count    INT          NOT NULL DEFAULT 0,
    next_retry_at  TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_outbox_status_retry ON email_service.outbox_event (status, next_retry_at);
CREATE INDEX idx_outbox_aggregate ON email_service.outbox_event (aggregate_type, aggregate_id);

--rollback DROP TABLE IF EXISTS email_service.outbox_event;
