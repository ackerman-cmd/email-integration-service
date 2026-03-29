--liquibase formatted sql

--changeset system:000-create-schema
CREATE SCHEMA IF NOT EXISTS email_service;
GRANT ALL PRIVILEGES ON SCHEMA email_service TO admin;

--rollback DROP SCHEMA IF EXISTS email_service CASCADE;
