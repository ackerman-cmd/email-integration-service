--liquibase formatted sql

--changeset system:003-create-message
CREATE TABLE IF NOT EXISTS email_service.message
(
    id                  UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    conversation_id     UUID         NOT NULL REFERENCES email_service.conversation (id),
    direction           VARCHAR(20)  NOT NULL,
    status              VARCHAR(50)  NOT NULL,
    provider            VARCHAR(50)  NOT NULL DEFAULT 'RESEND',
    provider_message_id VARCHAR(255),
    internet_message_id VARCHAR(500),
    in_reply_to         VARCHAR(500),
    references_raw      TEXT,
    subject             VARCHAR(1000),
    from_email          VARCHAR(255) NOT NULL,
    from_name           VARCHAR(255),
    reply_to_email      VARCHAR(255),
    text_body           TEXT,
    html_body           TEXT,
    raw_headers_json    JSONB,
    sent_at             TIMESTAMPTZ,
    received_at         TIMESTAMPTZ,
    created_by_user_id  UUID,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_message_conversation ON email_service.message (conversation_id);
CREATE INDEX idx_message_provider_id ON email_service.message (provider_message_id);
CREATE INDEX idx_message_internet_message_id ON email_service.message (internet_message_id);
CREATE INDEX idx_message_in_reply_to ON email_service.message (in_reply_to);
CREATE INDEX idx_message_direction_status ON email_service.message (direction, status);

--rollback DROP TABLE IF EXISTS email_service.message;
