--liquibase formatted sql

--changeset system:005-create-message-attachment
CREATE TABLE IF NOT EXISTS email_service.message_attachment
(
    id                     UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    message_id             UUID         NOT NULL REFERENCES email_service.message (id),
    provider_attachment_id VARCHAR(255),
    file_name              VARCHAR(500) NOT NULL,
    content_type           VARCHAR(255) NOT NULL,
    size_bytes             BIGINT,
    storage_key            VARCHAR(1000),
    checksum               VARCHAR(255),
    is_inline              BOOLEAN      NOT NULL DEFAULT FALSE,
    content_id             VARCHAR(500)
);

CREATE INDEX idx_attachment_message ON email_service.message_attachment (message_id);

--rollback DROP TABLE IF EXISTS email_service.message_attachment;
