--liquibase formatted sql

--changeset system:001-create-mailbox
CREATE TABLE IF NOT EXISTS email_service.mailbox
(
    id                   UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    email_address        VARCHAR(255) NOT NULL UNIQUE,
    domain               VARCHAR(255) NOT NULL,
    provider             VARCHAR(50)  NOT NULL DEFAULT 'RESEND',
    status               VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    is_inbound_enabled   BOOLEAN      NOT NULL DEFAULT TRUE,
    is_outbound_enabled  BOOLEAN      NOT NULL DEFAULT TRUE,
    default_queue_key    VARCHAR(100),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_mailbox_email ON email_service.mailbox (email_address);
CREATE INDEX idx_mailbox_status ON email_service.mailbox (status);

--rollback DROP TABLE IF EXISTS email_service.mailbox;
