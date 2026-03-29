CREATE SCHEMA IF NOT EXISTS email_service;

GRANT ALL PRIVILEGES ON SCHEMA email_service TO admin;

ALTER USER admin SET search_path TO email_service;