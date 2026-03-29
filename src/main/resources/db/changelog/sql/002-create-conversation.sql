--liquibase formatted sql

--changeset system:002-create-conversation
CREATE TABLE IF NOT EXISTS email_service.conversation
(
    id                 UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    mailbox_id         UUID        NOT NULL REFERENCES email_service.mailbox (id),
    subject_normalized VARCHAR(500),
    case_id            UUID,
    status             VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    started_at         TIMESTAMPTZ,
    last_message_at    TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_conversation_mailbox ON email_service.conversation (mailbox_id);
CREATE INDEX idx_conversation_case ON email_service.conversation (case_id);
CREATE INDEX idx_conversation_status ON email_service.conversation (status);
CREATE INDEX idx_conversation_last_message ON email_service.conversation (last_message_at DESC);

--rollback DROP TABLE IF EXISTS email_service.conversation;
