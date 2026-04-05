--liquibase formatted sql

--changeset system:008-seed-queue-mailboxes
--comment: Ящики для очередей ARM (id совпадает с assignment_groups / skill_groups). Домен system-alerts.ru.
--  Группы назначения (assignment):
--    contact-center-first-line     — Первая линия, контакт-центр (телефон, чат, email, L1)
--    qualification-routing         — Квалификация и маршрутизация между линиями
--    digital-banking               — ДБО, интернет- и мобильный банк
--    cards-and-payments            — Карты, СБП, переводы, лимиты
--    loans-deposits                — Кредиты, ипотека, вклады и накопления
--    second-line-systems           — Вторая линия, прикладные системы (L2, ИТ)
--    premium-clients               — Премиум и ключевые клиенты
--    corporate-sme                 — МСБ и корпоративные клиенты (РКО, эквайринг)
--  Скилл-группы (skill):
--    skill-client-communication    — Базовая клиентская коммуникация, L1, тикетинг
--    skill-digital-banking         — Дистанционное банковское обслуживание
--    skill-card-payments           — Платёжные инструменты и карты
--    skill-lending-products        — Кредитование и долговые продукты
--    skill-incident-management     — Инциденты, мониторинг, эскалация в ИТ
--    skill-fraud-compliance        — Финбез, AML, комплаенс, 115-ФЗ
--    skill-corporate-banking       — Корпоративное обслуживание, РКО, зарплатные проекты
INSERT INTO email_service.mailbox (id, email_address, domain, provider, status, is_inbound_enabled, is_outbound_enabled,
                                    default_queue_key, created_at, updated_at)
VALUES
    ('a1b2c3d4-1001-4000-8000-000000000001'::uuid, 'contact-center-first-line@system-alerts.ru', 'system-alerts.ru',
     'RESEND', 'ACTIVE', TRUE, TRUE, 'assignment:a1b2c3d4-1001-4000-8000-000000000001', NOW(), NOW()),
    ('a1b2c3d4-1001-4000-8000-000000000002'::uuid, 'qualification-routing@system-alerts.ru', 'system-alerts.ru',
     'RESEND', 'ACTIVE', TRUE, TRUE, 'assignment:a1b2c3d4-1001-4000-8000-000000000002', NOW(), NOW()),
    ('a1b2c3d4-1001-4000-8000-000000000003'::uuid, 'digital-banking@system-alerts.ru', 'system-alerts.ru', 'RESEND',
     'ACTIVE', TRUE, TRUE, 'assignment:a1b2c3d4-1001-4000-8000-000000000003', NOW(), NOW()),
    ('a1b2c3d4-1001-4000-8000-000000000004'::uuid, 'cards-and-payments@system-alerts.ru', 'system-alerts.ru', 'RESEND',
     'ACTIVE', TRUE, TRUE, 'assignment:a1b2c3d4-1001-4000-8000-000000000004', NOW(), NOW()),
    ('a1b2c3d4-1001-4000-8000-000000000005'::uuid, 'loans-deposits@system-alerts.ru', 'system-alerts.ru', 'RESEND',
     'ACTIVE', TRUE, TRUE, 'assignment:a1b2c3d4-1001-4000-8000-000000000005', NOW(), NOW()),
    ('a1b2c3d4-1001-4000-8000-000000000006'::uuid, 'second-line-systems@system-alerts.ru', 'system-alerts.ru', 'RESEND',
     'ACTIVE', TRUE, TRUE, 'assignment:a1b2c3d4-1001-4000-8000-000000000006', NOW(), NOW()),
    ('a1b2c3d4-1001-4000-8000-000000000007'::uuid, 'premium-clients@system-alerts.ru', 'system-alerts.ru', 'RESEND',
     'ACTIVE', TRUE, TRUE, 'assignment:a1b2c3d4-1001-4000-8000-000000000007', NOW(), NOW()),
    ('a1b2c3d4-1001-4000-8000-000000000008'::uuid, 'corporate-sme@system-alerts.ru', 'system-alerts.ru', 'RESEND',
     'ACTIVE', TRUE, TRUE, 'assignment:a1b2c3d4-1001-4000-8000-000000000008', NOW(), NOW()),
    ('b1b2c3d4-2001-4000-8000-000000000001'::uuid, 'skill-client-communication@system-alerts.ru', 'system-alerts.ru',
     'RESEND', 'ACTIVE', TRUE, TRUE, 'skill:b1b2c3d4-2001-4000-8000-000000000001', NOW(), NOW()),
    ('b1b2c3d4-2001-4000-8000-000000000002'::uuid, 'skill-digital-banking@system-alerts.ru', 'system-alerts.ru', 'RESEND',
     'ACTIVE', TRUE, TRUE, 'skill:b1b2c3d4-2001-4000-8000-000000000002', NOW(), NOW()),
    ('b1b2c3d4-2001-4000-8000-000000000003'::uuid, 'skill-card-payments@system-alerts.ru', 'system-alerts.ru', 'RESEND',
     'ACTIVE', TRUE, TRUE, 'skill:b1b2c3d4-2001-4000-8000-000000000003', NOW(), NOW()),
    ('b1b2c3d4-2001-4000-8000-000000000004'::uuid, 'skill-lending-products@system-alerts.ru', 'system-alerts.ru',
     'RESEND', 'ACTIVE', TRUE, TRUE, 'skill:b1b2c3d4-2001-4000-8000-000000000004', NOW(), NOW()),
    ('b1b2c3d4-2001-4000-8000-000000000005'::uuid, 'skill-incident-management@system-alerts.ru', 'system-alerts.ru',
     'RESEND', 'ACTIVE', TRUE, TRUE, 'skill:b1b2c3d4-2001-4000-8000-000000000005', NOW(), NOW()),
    ('b1b2c3d4-2001-4000-8000-000000000006'::uuid, 'skill-fraud-compliance@system-alerts.ru', 'system-alerts.ru', 'RESEND',
     'ACTIVE', TRUE, TRUE, 'skill:b1b2c3d4-2001-4000-8000-000000000006', NOW(), NOW()),
    ('b1b2c3d4-2001-4000-8000-000000000007'::uuid, 'skill-corporate-banking@system-alerts.ru', 'system-alerts.ru',
     'RESEND', 'ACTIVE', TRUE, TRUE, 'skill:b1b2c3d4-2001-4000-8000-000000000007', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

--rollback DELETE FROM email_service.mailbox WHERE id IN ('a1b2c3d4-1001-4000-8000-000000000001'::uuid, 'a1b2c3d4-1001-4000-8000-000000000002'::uuid, 'a1b2c3d4-1001-4000-8000-000000000003'::uuid, 'a1b2c3d4-1001-4000-8000-000000000004'::uuid, 'a1b2c3d4-1001-4000-8000-000000000005'::uuid, 'a1b2c3d4-1001-4000-8000-000000000006'::uuid, 'a1b2c3d4-1001-4000-8000-000000000007'::uuid, 'a1b2c3d4-1001-4000-8000-000000000008'::uuid, 'b1b2c3d4-2001-4000-8000-000000000001'::uuid, 'b1b2c3d4-2001-4000-8000-000000000002'::uuid, 'b1b2c3d4-2001-4000-8000-000000000003'::uuid, 'b1b2c3d4-2001-4000-8000-000000000004'::uuid, 'b1b2c3d4-2001-4000-8000-000000000005'::uuid, 'b1b2c3d4-2001-4000-8000-000000000006'::uuid, 'b1b2c3d4-2001-4000-8000-000000000007'::uuid);
