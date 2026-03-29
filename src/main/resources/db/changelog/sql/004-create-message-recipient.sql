--liquibase formatted sql

--changeset system:004-create-message-recipient
CREATE TABLE IF NOT EXISTS email_service.message_recipient
(
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    message_id UUID        NOT NULL REFERENCES email_service.message (id),
    type       VARCHAR(10) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    name       VARCHAR(255)
);

CREATE INDEX idx_recipient_message ON email_service.message_recipient (message_id);

--rollback DROP TABLE IF EXISTS email_service.message_recipient;
