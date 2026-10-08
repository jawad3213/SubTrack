-- =====================================================================================
-- V4: Massive demo seed data — simulates a fully active SubTrack platform.
-- 20 users, 40+ SaaS services, 120+ subscriptions, 600+ payments, alerts, notifications,
-- exchange rates, invoices, and system configuration.
-- All passwords are BCrypt-hashed "Password1" (work factor 12).
-- =====================================================================================

-- ─────────────────────────────────────────────────────────────────────
-- 1. EXCHANGE RATES  (base currency MAD)
-- ─────────────────────────────────────────────────────────────────────
-- exchange_rate has no unique constraint on (from_currency, to_currency) in V1,
-- so ON CONFLICT cannot be used: update existing pairs, then insert missing ones.
CREATE TEMP TABLE seed_rate (from_currency VARCHAR(3), to_currency VARCHAR(3), rate NUMERIC(15,6));
INSERT INTO seed_rate VALUES
('USD', 'MAD', 10.050000),
('EUR', 'MAD', 10.950000),
('GBP', 'MAD', 12.700000),
('CAD', 'MAD',  7.420000),
('CHF', 'MAD', 11.200000),
('JPY', 'MAD',  0.067000),
('AUD', 'MAD',  6.550000),
('MAD', 'USD',  0.099502),
('MAD', 'EUR',  0.091324),
('MAD', 'GBP',  0.078740),
('USD', 'EUR',  0.918000),
('EUR', 'USD',  1.089000),
('GBP', 'USD',  1.264000),
('USD', 'GBP',  0.791000);

UPDATE exchange_rate er SET rate = s.rate, updated_at = NOW()
FROM seed_rate s
WHERE er.from_currency = s.from_currency AND er.to_currency = s.to_currency;

INSERT INTO exchange_rate (id, from_currency, to_currency, rate, updated_at)
SELECT gen_random_uuid(), s.from_currency, s.to_currency, s.rate, NOW()
FROM seed_rate s
WHERE NOT EXISTS (
    SELECT 1 FROM exchange_rate er
    WHERE er.from_currency = s.from_currency AND er.to_currency = s.to_currency
);
DROP TABLE seed_rate;

-- Categories are normally created by DatabaseInitializer after migrations run,
-- but the subscriptions below look them up by name, so make sure they exist.
INSERT INTO category (id, name, description, icon, color, created_at) VALUES
(gen_random_uuid(), 'Work',          'Professional tools and software',      'briefcase',   '#007bff', NOW()),
(gen_random_uuid(), 'Cloud',         'Cloud storage and services',           'cloud',       '#17a2b8', NOW()),
(gen_random_uuid(), 'Entertainment', 'Streaming and media services',         'play-circle', '#dc3545', NOW()),
(gen_random_uuid(), 'Productivity',  'Productivity and collaboration tools', 'tasks',       '#28a745', NOW()),
(gen_random_uuid(), 'Utilities',     'Utility and helper services',          'wrench',      '#6c757d', NOW()),
(gen_random_uuid(), 'Other',         'Miscellaneous subscriptions',          'folder',      '#ffc107', NOW())
ON CONFLICT (name) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────
-- 2. SAAS SERVICES CATALOG  (40+ services)
-- ─────────────────────────────────────────────────────────────────────
INSERT INTO saas_service (id, name, description, category, logo_url, website_url, default_price, default_currency, default_frequency, is_popular, created_at, updated_at) VALUES
-- Entertainment
(gen_random_uuid(), 'Netflix',          'Streaming de films et séries',                         'Entertainment', 'https://logo.clearbit.com/netflix.com',       'https://netflix.com',       15.49,  'USD', 'MONTHLY', true,  NOW(), NOW()),
(gen_random_uuid(), 'Spotify',          'Musique en streaming',                                 'Entertainment', 'https://logo.clearbit.com/spotify.com',       'https://spotify.com',        9.99,  'USD', 'MONTHLY', true,  NOW(), NOW()),
(gen_random_uuid(), 'Disney+',          'Films et séries Disney, Marvel, Star Wars',            'Entertainment', 'https://logo.clearbit.com/disneyplus.com',    'https://disneyplus.com',    13.99,  'USD', 'MONTHLY', true,  NOW(), NOW()),
(gen_random_uuid(), 'YouTube Premium',  'YouTube sans publicités avec YouTube Music',           'Entertainment', 'https://logo.clearbit.com/youtube.com',       'https://youtube.com',       13.99,  'USD', 'MONTHLY', false, NOW(), NOW()),
(gen_random_uuid(), 'Apple TV+',        'Séries et films originaux Apple',                      'Entertainment', 'https://logo.clearbit.com/apple.com',         'https://tv.apple.com',       9.99,  'USD', 'MONTHLY', false, NOW(), NOW()),
(gen_random_uuid(), 'HBO Max',          'Streaming HBO, Warner Bros, DC',                       'Entertainment', 'https://logo.clearbit.com/hbomax.com',        'https://hbomax.com',        15.99,  'USD', 'MONTHLY', false, NOW(), NOW()),
(gen_random_uuid(), 'Amazon Prime',     'Livraison rapide + Prime Video',                       'Entertainment', 'https://logo.clearbit.com/amazon.com',        'https://amazon.com',        14.99,  'USD', 'MONTHLY', true,  NOW(), NOW()),
(gen_random_uuid(), 'Crunchyroll',      'Anime en streaming',                                   'Entertainment', 'https://logo.clearbit.com/crunchyroll.com',   'https://crunchyroll.com',    7.99,  'USD', 'MONTHLY', false, NOW(), NOW()),
-- Productivity
(gen_random_uuid(), 'Notion',           'Workspace tout-en-un : notes, wikis, bases de données', 'Productivity', 'https://logo.clearbit.com/notion.so',        'https://notion.so',          10.00, 'USD', 'MONTHLY', true,  NOW(), NOW()),
(gen_random_uuid(), 'Slack',            'Messagerie d''équipe et collaboration',                 'Productivity', 'https://logo.clearbit.com/slack.com',        'https://slack.com',           8.75, 'USD', 'MONTHLY', true,  NOW(), NOW()),
(gen_random_uuid(), 'Todoist',          'Gestion de tâches et productivité',                     'Productivity', 'https://logo.clearbit.com/todoist.com',      'https://todoist.com',         4.00, 'USD', 'MONTHLY', false, NOW(), NOW()),
(gen_random_uuid(), 'Trello',           'Tableaux Kanban pour la gestion de projets',            'Productivity', 'https://logo.clearbit.com/trello.com',       'https://trello.com',          5.00, 'USD', 'MONTHLY', false, NOW(), NOW()),
(gen_random_uuid(), 'Asana',            'Gestion de projets pour les équipes',                   'Productivity', 'https://logo.clearbit.com/asana.com',        'https://asana.com',          10.99, 'USD', 'MONTHLY', false, NOW(), NOW()),
(gen_random_uuid(), 'Monday.com',       'Plateforme de gestion du travail',                      'Productivity', 'https://logo.clearbit.com/monday.com',       'https://monday.com',          9.00, 'USD', 'MONTHLY', false, NOW(), NOW()),
(gen_random_uuid(), 'Linear',           'Suivi de projets pour les équipes logicielles',         'Productivity', 'https://logo.clearbit.com/linear.app',       'https://linear.app',          8.00, 'USD', 'MONTHLY', false, NOW(), NOW()),
-- Cloud
(gen_random_uuid(), 'iCloud+',          'Stockage cloud Apple 200 Go',                          'Cloud',        'https://logo.clearbit.com/icloud.com',       'https://icloud.com',          2.99, 'USD', 'MONTHLY', true,  NOW(), NOW()),
(gen_random_uuid(), 'Google One',       'Stockage Google étendu 2 To',                          'Cloud',        'https://logo.clearbit.com/google.com',       'https://one.google.com',      9.99, 'USD', 'MONTHLY', true,  NOW(), NOW()),
(gen_random_uuid(), 'Dropbox Plus',     'Stockage cloud 2 To avec synchronisation',             'Cloud',        'https://logo.clearbit.com/dropbox.com',      'https://dropbox.com',        11.99, 'USD', 'MONTHLY', false, NOW(), NOW()),
(gen_random_uuid(), 'OneDrive',         'Stockage cloud Microsoft 1 To',                        'Cloud',        'https://logo.clearbit.com/microsoft.com',    'https://onedrive.live.com',   1.99, 'USD', 'MONTHLY', false, NOW(), NOW()),
-- Work / Dev Tools
(gen_random_uuid(), 'GitHub Pro',       'Fonctionnalités avancées pour les développeurs',        'Work',         'https://logo.clearbit.com/github.com',      'https://github.com',          4.00, 'USD', 'MONTHLY', true,  NOW(), NOW()),
(gen_random_uuid(), 'JetBrains',        'IDE tout-en-un pour développeurs',                      'Work',         'https://logo.clearbit.com/jetbrains.com',   'https://jetbrains.com',      24.90, 'USD', 'MONTHLY', false, NOW(), NOW()),
(gen_random_uuid(), 'Figma',            'Design collaboratif d''interfaces',                     'Work',         'https://logo.clearbit.com/figma.com',       'https://figma.com',          15.00, 'USD', 'MONTHLY', true,  NOW(), NOW()),
(gen_random_uuid(), 'Adobe Creative Cloud', 'Suite complète Adobe : Photoshop, Illustrator, etc.', 'Work',       'https://logo.clearbit.com/adobe.com',       'https://adobe.com',          54.99, 'USD', 'MONTHLY', true,  NOW(), NOW()),
(gen_random_uuid(), 'Canva Pro',        'Design graphique simplifié',                            'Work',         'https://logo.clearbit.com/canva.com',       'https://canva.com',          12.99, 'USD', 'MONTHLY', false, NOW(), NOW()),
(gen_random_uuid(), 'Vercel Pro',       'Déploiement et hébergement frontend',                   'Work',         'https://logo.clearbit.com/vercel.com',      'https://vercel.com',         20.00, 'USD', 'MONTHLY', false, NOW(), NOW()),
(gen_random_uuid(), 'Heroku',           'Plateforme cloud pour déployer des applications',       'Work',         'https://logo.clearbit.com/heroku.com',      'https://heroku.com',          7.00, 'USD', 'MONTHLY', false, NOW(), NOW()),
(gen_random_uuid(), 'DigitalOcean',     'Infrastructure cloud pour développeurs',                'Work',         'https://logo.clearbit.com/digitalocean.com','https://digitalocean.com',   12.00, 'USD', 'MONTHLY', false, NOW(), NOW()),
-- AI
(gen_random_uuid(), 'ChatGPT Plus',     'Accès à GPT-4 et fonctionnalités avancées',            'Work',         'https://logo.clearbit.com/openai.com',      'https://chat.openai.com',    20.00, 'USD', 'MONTHLY', true,  NOW(), NOW()),
(gen_random_uuid(), 'Claude Pro',       'Assistant IA Anthropic avancé',                         'Work',         'https://logo.clearbit.com/anthropic.com',   'https://claude.ai',          20.00, 'USD', 'MONTHLY', false, NOW(), NOW()),
(gen_random_uuid(), 'Midjourney',       'Génération d''images par IA',                           'Work',         'https://logo.clearbit.com/midjourney.com',  'https://midjourney.com',     10.00, 'USD', 'MONTHLY', false, NOW(), NOW()),
(gen_random_uuid(), 'Copilot',          'Assistant de code IA GitHub',                           'Work',         'https://logo.clearbit.com/github.com',      'https://github.com/features/copilot', 10.00, 'USD', 'MONTHLY', true, NOW(), NOW()),
-- Utilities / VPN / Security
(gen_random_uuid(), 'NordVPN',          'VPN rapide et sécurisé',                                'Utilities',    'https://logo.clearbit.com/nordvpn.com',     'https://nordvpn.com',         4.49, 'USD', 'MONTHLY', true,  NOW(), NOW()),
(gen_random_uuid(), '1Password',        'Gestionnaire de mots de passe',                         'Utilities',    'https://logo.clearbit.com/1password.com',   'https://1password.com',       2.99, 'USD', 'MONTHLY', true,  NOW(), NOW()),
(gen_random_uuid(), 'Bitwarden',        'Gestionnaire de mots de passe open source',             'Utilities',    'https://logo.clearbit.com/bitwarden.com',   'https://bitwarden.com',      10.00, 'USD', 'ANNUAL',  false, NOW(), NOW()),
(gen_random_uuid(), 'ExpressVPN',       'VPN premium haute vitesse',                             'Utilities',    'https://logo.clearbit.com/expressvpn.com',  'https://expressvpn.com',     12.95, 'USD', 'MONTHLY', false, NOW(), NOW()),
(gen_random_uuid(), 'Grammarly',        'Correction et amélioration de l''écriture',             'Utilities',    'https://logo.clearbit.com/grammarly.com',   'https://grammarly.com',      12.00, 'USD', 'MONTHLY', false, NOW(), NOW()),
-- Communication
(gen_random_uuid(), 'Zoom Pro',         'Visioconférences professionnelles',                     'Work',         'https://logo.clearbit.com/zoom.us',         'https://zoom.us',            13.33, 'USD', 'MONTHLY', false, NOW(), NOW()),
(gen_random_uuid(), 'Microsoft 365',    'Suite Office complète + 1 To OneDrive',                 'Work',         'https://logo.clearbit.com/microsoft.com',   'https://microsoft.com',      69.99, 'USD', 'ANNUAL',  true,  NOW(), NOW()),
(gen_random_uuid(), 'Google Workspace', 'Gmail, Drive, Docs pour les entreprises',               'Work',         'https://logo.clearbit.com/google.com',      'https://workspace.google.com', 7.20,'USD', 'MONTHLY', false, NOW(), NOW()),
-- Fitness / Health
(gen_random_uuid(), 'Strava',           'Suivi d''activités sportives',                          'Other',        'https://logo.clearbit.com/strava.com',      'https://strava.com',          5.00, 'USD', 'MONTHLY', false, NOW(), NOW()),
(gen_random_uuid(), 'Headspace',        'Méditation et bien-être mental',                        'Other',        'https://logo.clearbit.com/headspace.com',   'https://headspace.com',      12.99, 'USD', 'MONTHLY', false, NOW(), NOW()),
(gen_random_uuid(), 'Duolingo Plus',    'Apprentissage de langues sans publicités',              'Other',        'https://logo.clearbit.com/duolingo.com',    'https://duolingo.com',        7.99, 'USD', 'MONTHLY', false, NOW(), NOW());

-- ─────────────────────────────────────────────────────────────────────
-- 3. CLIENT USERS  (20 users — password: "Password1" for all)
--    BCrypt hash of "Password1" (cost 12):
--    $2a$12$diGcFOclwy.nzrNWwcgNOeDJ4LoT/SDfNNeWMB4xy2MUI23BADvOe
-- ─────────────────────────────────────────────────────────────────────
DO $$
DECLARE
    bcrypt_hash TEXT := '$2a$12$diGcFOclwy.nzrNWwcgNOeDJ4LoT/SDfNNeWMB4xy2MUI23BADvOe';
    user_ids UUID[];
    new_user_id UUID;
    i INT;
BEGIN
    -- Create 20 users with staggered registration dates over last 6 months
    user_ids := ARRAY[]::UUID[];

    -- User 1: Youssef El Amrani
    INSERT INTO client (id, email, password, first_name, last_name, account_type, role, timezone, is_active, email_notifications, telegram_enabled, whatsapp_enabled, created_at, updated_at)
    VALUES (gen_random_uuid(), 'youssef.elamrani@gmail.com', bcrypt_hash, 'Youssef', 'El Amrani', 'B2C', 'CLIENT', 'Africa/Casablanca', true, true, false, false, NOW() - INTERVAL '185 days', NOW() - INTERVAL '2 days')
    RETURNING id INTO new_user_id;
    user_ids[1] := new_user_id;

    -- User 2: Fatima Zahra Bennis
    INSERT INTO client (id, email, password, first_name, last_name, account_type, role, timezone, is_active, email_notifications, telegram_enabled, whatsapp_enabled, created_at, updated_at)
    VALUES (gen_random_uuid(), 'fatima.bennis@outlook.com', bcrypt_hash, 'Fatima Zahra', 'Bennis', 'B2C', 'CLIENT', 'Africa/Casablanca', true, true, true, false, NOW() - INTERVAL '170 days', NOW() - INTERVAL '1 day')
    RETURNING id INTO new_user_id;
    user_ids[2] := new_user_id;

    -- User 3: Ahmed Tazi
    INSERT INTO client (id, email, password, first_name, last_name, account_type, role, timezone, is_active, email_notifications, telegram_enabled, whatsapp_enabled, whatsapp_number, created_at, updated_at)
    VALUES (gen_random_uuid(), 'ahmed.tazi@yahoo.fr', bcrypt_hash, 'Ahmed', 'Tazi', 'FREELANCE', 'CLIENT', 'Africa/Casablanca', true, true, false, true, '+212661234567', NOW() - INTERVAL '160 days', NOW() - INTERVAL '3 days')
    RETURNING id INTO new_user_id;
    user_ids[3] := new_user_id;

    -- User 4: Sara Idrissi
    INSERT INTO client (id, email, password, first_name, last_name, account_type, role, timezone, is_active, email_notifications, telegram_enabled, whatsapp_enabled, created_at, updated_at)
    VALUES (gen_random_uuid(), 'sara.idrissi@gmail.com', bcrypt_hash, 'Sara', 'Idrissi', 'B2C', 'CLIENT', 'Europe/Paris', true, true, false, false, NOW() - INTERVAL '150 days', NOW() - INTERVAL '5 days')
    RETURNING id INTO new_user_id;
    user_ids[4] := new_user_id;

    -- User 5: Karim Alaoui
    INSERT INTO client (id, email, password, first_name, last_name, account_type, role, timezone, is_active, email_notifications, telegram_enabled, whatsapp_enabled, created_at, updated_at)
    VALUES (gen_random_uuid(), 'karim.alaoui@protonmail.com', bcrypt_hash, 'Karim', 'Alaoui', 'B2B', 'CLIENT', 'Africa/Casablanca', true, true, true, true, NOW() - INTERVAL '140 days', NOW())
    RETURNING id INTO new_user_id;
    user_ids[5] := new_user_id;

    -- User 6: Nadia Chraibi
    INSERT INTO client (id, email, password, first_name, last_name, account_type, role, timezone, is_active, email_notifications, telegram_enabled, whatsapp_enabled, created_at, updated_at)
    VALUES (gen_random_uuid(), 'nadia.chraibi@gmail.com', bcrypt_hash, 'Nadia', 'Chraibi', 'B2C', 'CLIENT', 'Africa/Casablanca', true, true, false, false, NOW() - INTERVAL '130 days', NOW() - INTERVAL '10 days')
    RETURNING id INTO new_user_id;
    user_ids[6] := new_user_id;

    -- User 7: Omar Fassi
    INSERT INTO client (id, email, password, first_name, last_name, account_type, role, timezone, is_active, email_notifications, telegram_enabled, whatsapp_enabled, created_at, updated_at)
    VALUES (gen_random_uuid(), 'omar.fassi@hotmail.com', bcrypt_hash, 'Omar', 'Fassi', 'FREELANCE', 'CLIENT', 'Europe/Paris', true, true, false, false, NOW() - INTERVAL '120 days', NOW() - INTERVAL '7 days')
    RETURNING id INTO new_user_id;
    user_ids[7] := new_user_id;

    -- User 8: Layla Berrada
    INSERT INTO client (id, email, password, first_name, last_name, account_type, role, timezone, is_active, email_notifications, telegram_enabled, whatsapp_enabled, created_at, updated_at)
    VALUES (gen_random_uuid(), 'layla.berrada@gmail.com', bcrypt_hash, 'Layla', 'Berrada', 'B2C', 'CLIENT', 'Africa/Casablanca', true, true, false, false, NOW() - INTERVAL '110 days', NOW() - INTERVAL '4 days')
    RETURNING id INTO new_user_id;
    user_ids[8] := new_user_id;

    -- User 9: Mehdi Ouazzani
    INSERT INTO client (id, email, password, first_name, last_name, account_type, role, timezone, is_active, email_notifications, telegram_enabled, whatsapp_enabled, created_at, updated_at)
    VALUES (gen_random_uuid(), 'mehdi.ouazzani@gmail.com', bcrypt_hash, 'Mehdi', 'Ouazzani', 'B2B', 'CLIENT', 'Africa/Casablanca', true, true, true, false, NOW() - INTERVAL '100 days', NOW() - INTERVAL '1 day')
    RETURNING id INTO new_user_id;
    user_ids[9] := new_user_id;

    -- User 10: Zineb Hajji
    INSERT INTO client (id, email, password, first_name, last_name, account_type, role, timezone, is_active, email_notifications, telegram_enabled, whatsapp_enabled, created_at, updated_at)
    VALUES (gen_random_uuid(), 'zineb.hajji@outlook.com', bcrypt_hash, 'Zineb', 'Hajji', 'B2C', 'CLIENT', 'Africa/Casablanca', true, true, false, false, NOW() - INTERVAL '95 days', NOW() - INTERVAL '2 days')
    RETURNING id INTO new_user_id;
    user_ids[10] := new_user_id;

    -- User 11: Amine Ziani (suspended user)
    INSERT INTO client (id, email, password, first_name, last_name, account_type, role, timezone, is_active, email_notifications, telegram_enabled, whatsapp_enabled, created_at, updated_at)
    VALUES (gen_random_uuid(), 'amine.ziani@gmail.com', bcrypt_hash, 'Amine', 'Ziani', 'B2C', 'CLIENT', 'Africa/Casablanca', false, true, false, false, NOW() - INTERVAL '90 days', NOW() - INTERVAL '15 days')
    RETURNING id INTO new_user_id;
    user_ids[11] := new_user_id;

    -- User 12: Hiba Benali
    INSERT INTO client (id, email, password, first_name, last_name, account_type, role, timezone, is_active, email_notifications, telegram_enabled, whatsapp_enabled, created_at, updated_at)
    VALUES (gen_random_uuid(), 'hiba.benali@gmail.com', bcrypt_hash, 'Hiba', 'Benali', 'FREELANCE', 'CLIENT', 'Europe/London', true, true, false, false, NOW() - INTERVAL '80 days', NOW() - INTERVAL '3 days')
    RETURNING id INTO new_user_id;
    user_ids[12] := new_user_id;

    -- User 13: Rachid Mansouri
    INSERT INTO client (id, email, password, first_name, last_name, account_type, role, timezone, is_active, email_notifications, telegram_enabled, whatsapp_enabled, created_at, updated_at)
    VALUES (gen_random_uuid(), 'rachid.mansouri@yahoo.com', bcrypt_hash, 'Rachid', 'Mansouri', 'B2C', 'CLIENT', 'Africa/Casablanca', true, true, false, true, NOW() - INTERVAL '70 days', NOW() - INTERVAL '6 days')
    RETURNING id INTO new_user_id;
    user_ids[13] := new_user_id;

    -- User 14: Imane Filali
    INSERT INTO client (id, email, password, first_name, last_name, account_type, role, timezone, is_active, email_notifications, telegram_enabled, whatsapp_enabled, created_at, updated_at)
    VALUES (gen_random_uuid(), 'imane.filali@gmail.com', bcrypt_hash, 'Imane', 'Filali', 'B2C', 'CLIENT', 'Europe/Paris', true, true, false, false, NOW() - INTERVAL '60 days', NOW() - INTERVAL '2 days')
    RETURNING id INTO new_user_id;
    user_ids[14] := new_user_id;

    -- User 15: Hamza Kettani
    INSERT INTO client (id, email, password, first_name, last_name, account_type, role, timezone, is_active, email_notifications, telegram_enabled, whatsapp_enabled, created_at, updated_at)
    VALUES (gen_random_uuid(), 'hamza.kettani@protonmail.com', bcrypt_hash, 'Hamza', 'Kettani', 'B2B', 'CLIENT', 'Africa/Casablanca', true, true, true, true, NOW() - INTERVAL '50 days', NOW())
    RETURNING id INTO new_user_id;
    user_ids[15] := new_user_id;

    -- User 16: Meryem Sqalli
    INSERT INTO client (id, email, password, first_name, last_name, account_type, role, timezone, is_active, email_notifications, telegram_enabled, whatsapp_enabled, created_at, updated_at)
    VALUES (gen_random_uuid(), 'meryem.sqalli@gmail.com', bcrypt_hash, 'Meryem', 'Sqalli', 'B2C', 'CLIENT', 'Africa/Casablanca', true, true, false, false, NOW() - INTERVAL '40 days', NOW() - INTERVAL '1 day')
    RETURNING id INTO new_user_id;
    user_ids[16] := new_user_id;

    -- User 17: Adil Benkirane
    INSERT INTO client (id, email, password, first_name, last_name, account_type, role, timezone, is_active, email_notifications, telegram_enabled, whatsapp_enabled, created_at, updated_at)
    VALUES (gen_random_uuid(), 'adil.benkirane@outlook.fr', bcrypt_hash, 'Adil', 'Benkirane', 'FREELANCE', 'CLIENT', 'Europe/Paris', true, true, false, false, NOW() - INTERVAL '30 days', NOW() - INTERVAL '5 days')
    RETURNING id INTO new_user_id;
    user_ids[17] := new_user_id;

    -- User 18: Kenza Lahlou
    INSERT INTO client (id, email, password, first_name, last_name, account_type, role, timezone, is_active, email_notifications, telegram_enabled, whatsapp_enabled, created_at, updated_at)
    VALUES (gen_random_uuid(), 'kenza.lahlou@gmail.com', bcrypt_hash, 'Kenza', 'Lahlou', 'B2C', 'CLIENT', 'Africa/Casablanca', true, true, false, false, NOW() - INTERVAL '20 days', NOW() - INTERVAL '2 days')
    RETURNING id INTO new_user_id;
    user_ids[18] := new_user_id;

    -- User 19: Othmane Belhaj (suspended user)
    INSERT INTO client (id, email, password, first_name, last_name, account_type, role, timezone, is_active, email_notifications, telegram_enabled, whatsapp_enabled, created_at, updated_at)
    VALUES (gen_random_uuid(), 'othmane.belhaj@gmail.com', bcrypt_hash, 'Othmane', 'Belhaj', 'B2C', 'CLIENT', 'Africa/Casablanca', false, false, false, false, NOW() - INTERVAL '15 days', NOW() - INTERVAL '5 days')
    RETURNING id INTO new_user_id;
    user_ids[19] := new_user_id;

    -- User 20: Salma Naciri
    INSERT INTO client (id, email, password, first_name, last_name, account_type, role, timezone, is_active, email_notifications, telegram_enabled, whatsapp_enabled, created_at, updated_at)
    VALUES (gen_random_uuid(), 'salma.naciri@gmail.com', bcrypt_hash, 'Salma', 'Naciri', 'B2C', 'CLIENT', 'Africa/Casablanca', true, true, false, false, NOW() - INTERVAL '10 days', NOW())
    RETURNING id INTO new_user_id;
    user_ids[20] := new_user_id;

    -- ─────────────────────────────────────────────────────────────────────
    -- 4. SUBSCRIPTIONS  (~6-8 per user, mixed statuses/currencies/frequencies)
    --    Uses category IDs looked up by name.
    -- ─────────────────────────────────────────────────────────────────────
    -- Helper: fetch category IDs
    DECLARE
        cat_work UUID;
        cat_cloud UUID;
        cat_ent UUID;
        cat_prod UUID;
        cat_util UUID;
        cat_other UUID;
        sub_id UUID;
    BEGIN
        SELECT id INTO cat_work FROM category WHERE name = 'Work' LIMIT 1;
        SELECT id INTO cat_cloud FROM category WHERE name = 'Cloud' LIMIT 1;
        SELECT id INTO cat_ent FROM category WHERE name = 'Entertainment' LIMIT 1;
        SELECT id INTO cat_prod FROM category WHERE name = 'Productivity' LIMIT 1;
        SELECT id INTO cat_util FROM category WHERE name = 'Utilities' LIMIT 1;
        SELECT id INTO cat_other FROM category WHERE name = 'Other' LIMIT 1;

        -- ── USER 1: Youssef (power user — 8 subs) ──
        INSERT INTO subscription (id, client_id, category_id, name, description, price, original_currency, frequency, status, start_date, next_billing_date, logo_url, created_at, updated_at) VALUES
        (gen_random_uuid(), user_ids[1], cat_ent,  'Netflix',         'Forfait Standard',              15.49, 'USD', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '180 days')::date, (NOW() + INTERVAL '12 days')::date,  'https://logo.clearbit.com/netflix.com',     NOW() - INTERVAL '180 days', NOW()),
        (gen_random_uuid(), user_ids[1], cat_ent,  'Spotify',         'Premium individuel',             9.99, 'USD', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '180 days')::date, (NOW() + INTERVAL '5 days')::date,   'https://logo.clearbit.com/spotify.com',     NOW() - INTERVAL '180 days', NOW()),
        (gen_random_uuid(), user_ids[1], cat_work, 'ChatGPT Plus',    'Accès GPT-4',                   20.00, 'USD', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '150 days')::date, (NOW() + INTERVAL '8 days')::date,   'https://logo.clearbit.com/openai.com',      NOW() - INTERVAL '150 days', NOW()),
        (gen_random_uuid(), user_ids[1], cat_work, 'GitHub Pro',      'Repos privés illimités',          4.00, 'USD', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '180 days')::date, (NOW() + INTERVAL '15 days')::date,  'https://logo.clearbit.com/github.com',      NOW() - INTERVAL '180 days', NOW()),
        (gen_random_uuid(), user_ids[1], cat_cloud,'iCloud+',         '200 Go de stockage',              2.99, 'USD', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '170 days')::date, (NOW() + INTERVAL '20 days')::date,  'https://logo.clearbit.com/icloud.com',      NOW() - INTERVAL '170 days', NOW()),
        (gen_random_uuid(), user_ids[1], cat_util, 'NordVPN',         'Plan 2 ans',                     83.88, 'USD', 'ANNUAL',   'ACTIVE',    (NOW() - INTERVAL '365 days')::date, (NOW() + INTERVAL '180 days')::date, 'https://logo.clearbit.com/nordvpn.com',     NOW() - INTERVAL '180 days', NOW()),
        (gen_random_uuid(), user_ids[1], cat_prod, 'Notion',          'Plan Team',                      10.00, 'USD', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '120 days')::date, (NOW() + INTERVAL '3 days')::date,   'https://logo.clearbit.com/notion.so',       NOW() - INTERVAL '120 days', NOW()),
        (gen_random_uuid(), user_ids[1], cat_ent,  'Disney+',         'Forfait Standard',               13.99, 'USD', 'MONTHLY',   'CANCELLED', (NOW() - INTERVAL '160 days')::date, NULL, 'https://logo.clearbit.com/disneyplus.com', NOW() - INTERVAL '160 days', NOW() - INTERVAL '30 days');

        -- ── USER 2: Fatima Zahra (6 subs) ──
        INSERT INTO subscription (id, client_id, category_id, name, description, price, original_currency, frequency, status, start_date, next_billing_date, logo_url, created_at, updated_at) VALUES
        (gen_random_uuid(), user_ids[2], cat_ent,  'Netflix',         'Forfait Premium',                22.99, 'USD', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '165 days')::date, (NOW() + INTERVAL '7 days')::date,   'https://logo.clearbit.com/netflix.com',     NOW() - INTERVAL '165 days', NOW()),
        (gen_random_uuid(), user_ids[2], cat_ent,  'YouTube Premium', 'Famille',                        22.99, 'USD', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '140 days')::date, (NOW() + INTERVAL '18 days')::date,  'https://logo.clearbit.com/youtube.com',     NOW() - INTERVAL '140 days', NOW()),
        (gen_random_uuid(), user_ids[2], cat_work, 'Canva Pro',       'Design pour réseaux sociaux',    12.99, 'USD', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '150 days')::date, (NOW() + INTERVAL '10 days')::date,  'https://logo.clearbit.com/canva.com',       NOW() - INTERVAL '150 days', NOW()),
        (gen_random_uuid(), user_ids[2], cat_cloud,'Google One',      '2 To de stockage',                9.99, 'USD', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '160 days')::date, (NOW() + INTERVAL '22 days')::date,  'https://logo.clearbit.com/google.com',      NOW() - INTERVAL '160 days', NOW()),
        (gen_random_uuid(), user_ids[2], cat_prod, 'Todoist',         'Plan Pro',                        4.00, 'USD', 'MONTHLY',   'PAUSED',    (NOW() - INTERVAL '130 days')::date, NULL, 'https://logo.clearbit.com/todoist.com',  NOW() - INTERVAL '130 days', NOW() - INTERVAL '20 days'),
        (gen_random_uuid(), user_ids[2], cat_other,'Duolingo Plus',   'Apprentissage du coréen',         7.99, 'USD', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '100 days')::date, (NOW() + INTERVAL '14 days')::date,  'https://logo.clearbit.com/duolingo.com',    NOW() - INTERVAL '100 days', NOW());

        -- ── USER 3: Ahmed (freelancer — 7 subs, mixed currencies) ──
        INSERT INTO subscription (id, client_id, category_id, name, description, price, original_currency, frequency, status, start_date, next_billing_date, logo_url, created_at, updated_at) VALUES
        (gen_random_uuid(), user_ids[3], cat_work, 'Adobe Creative Cloud', 'Photographe',               54.99, 'EUR', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '155 days')::date, (NOW() + INTERVAL '9 days')::date,   'https://logo.clearbit.com/adobe.com',       NOW() - INTERVAL '155 days', NOW()),
        (gen_random_uuid(), user_ids[3], cat_work, 'Figma',            'Professional',                  15.00, 'USD', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '140 days')::date, (NOW() + INTERVAL '6 days')::date,   'https://logo.clearbit.com/figma.com',       NOW() - INTERVAL '140 days', NOW()),
        (gen_random_uuid(), user_ids[3], cat_work, 'Copilot',          'GitHub Copilot',                10.00, 'USD', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '130 days')::date, (NOW() + INTERVAL '11 days')::date,  'https://logo.clearbit.com/github.com',      NOW() - INTERVAL '130 days', NOW()),
        (gen_random_uuid(), user_ids[3], cat_work, 'Vercel Pro',       'Déploiement frontend',          20.00, 'USD', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '120 days')::date, (NOW() + INTERVAL '16 days')::date,  'https://logo.clearbit.com/vercel.com',      NOW() - INTERVAL '120 days', NOW()),
        (gen_random_uuid(), user_ids[3], cat_prod, 'Slack',            'Pro',                            8.75, 'USD', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '150 days')::date, (NOW() + INTERVAL '4 days')::date,   'https://logo.clearbit.com/slack.com',       NOW() - INTERVAL '150 days', NOW()),
        (gen_random_uuid(), user_ids[3], cat_ent,  'Spotify',          'Premium',                        9.99, 'EUR', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '155 days')::date, (NOW() + INTERVAL '13 days')::date,  'https://logo.clearbit.com/spotify.com',     NOW() - INTERVAL '155 days', NOW()),
        (gen_random_uuid(), user_ids[3], cat_util, '1Password',        'Individual',                     2.99, 'USD', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '160 days')::date, (NOW() + INTERVAL '25 days')::date,  'https://logo.clearbit.com/1password.com',   NOW() - INTERVAL '160 days', NOW());

        -- ── USER 4: Sara (6 subs) ──
        INSERT INTO subscription (id, client_id, category_id, name, description, price, original_currency, frequency, status, start_date, next_billing_date, logo_url, created_at, updated_at) VALUES
        (gen_random_uuid(), user_ids[4], cat_ent,  'Netflix',          'Standard',                      13.99, 'EUR', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '145 days')::date, (NOW() + INTERVAL '11 days')::date,  'https://logo.clearbit.com/netflix.com',     NOW() - INTERVAL '145 days', NOW()),
        (gen_random_uuid(), user_ids[4], cat_ent,  'Amazon Prime',     'Annual',                        69.90, 'EUR', 'ANNUAL',    'ACTIVE',    (NOW() - INTERVAL '300 days')::date, (NOW() + INTERVAL '65 days')::date,  'https://logo.clearbit.com/amazon.com',      NOW() - INTERVAL '145 days', NOW()),
        (gen_random_uuid(), user_ids[4], cat_work, 'Microsoft 365',    'Personnel',                     69.99, 'EUR', 'ANNUAL',    'ACTIVE',    (NOW() - INTERVAL '200 days')::date, (NOW() + INTERVAL '165 days')::date, 'https://logo.clearbit.com/microsoft.com',   NOW() - INTERVAL '145 days', NOW()),
        (gen_random_uuid(), user_ids[4], cat_cloud,'Dropbox Plus',     '2 To',                          11.99, 'EUR', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '130 days')::date, (NOW() + INTERVAL '2 days')::date,   'https://logo.clearbit.com/dropbox.com',     NOW() - INTERVAL '130 days', NOW()),
        (gen_random_uuid(), user_ids[4], cat_util, 'Grammarly',        'Premium',                       12.00, 'EUR', 'MONTHLY',   'PAUSED',    (NOW() - INTERVAL '100 days')::date, NULL, 'https://logo.clearbit.com/grammarly.com', NOW() - INTERVAL '100 days', NOW() - INTERVAL '15 days'),
        (gen_random_uuid(), user_ids[4], cat_other,'Headspace',        'Méditation',                    12.99, 'EUR', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '90 days')::date,  (NOW() + INTERVAL '19 days')::date,  'https://logo.clearbit.com/headspace.com',   NOW() - INTERVAL '90 days', NOW());

        -- ── USER 5: Karim (B2B power user — 8 subs) ──
        INSERT INTO subscription (id, client_id, category_id, name, description, price, original_currency, frequency, status, start_date, next_billing_date, logo_url, created_at, updated_at) VALUES
        (gen_random_uuid(), user_ids[5], cat_work, 'ChatGPT Plus',     'Équipe',                       25.00, 'USD', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '135 days')::date, (NOW() + INTERVAL '6 days')::date,   'https://logo.clearbit.com/openai.com',      NOW() - INTERVAL '135 days', NOW()),
        (gen_random_uuid(), user_ids[5], cat_work, 'Claude Pro',       'Anthropic',                    20.00, 'USD', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '100 days')::date, (NOW() + INTERVAL '14 days')::date,  'https://logo.clearbit.com/anthropic.com',   NOW() - INTERVAL '100 days', NOW()),
        (gen_random_uuid(), user_ids[5], cat_work, 'JetBrains',        'All Products Pack',           249.00, 'USD', 'ANNUAL',    'ACTIVE',    (NOW() - INTERVAL '130 days')::date, (NOW() + INTERVAL '235 days')::date, 'https://logo.clearbit.com/jetbrains.com',   NOW() - INTERVAL '130 days', NOW()),
        (gen_random_uuid(), user_ids[5], cat_prod, 'Slack',            'Business+',                    12.50, 'USD', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '140 days')::date, (NOW() + INTERVAL '8 days')::date,   'https://logo.clearbit.com/slack.com',       NOW() - INTERVAL '140 days', NOW()),
        (gen_random_uuid(), user_ids[5], cat_prod, 'Notion',           'Team',                         10.00, 'USD', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '140 days')::date, (NOW() + INTERVAL '3 days')::date,   'https://logo.clearbit.com/notion.so',       NOW() - INTERVAL '140 days', NOW()),
        (gen_random_uuid(), user_ids[5], cat_work, 'Zoom Pro',         'Mensuel',                      13.33, 'USD', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '120 days')::date, (NOW() + INTERVAL '17 days')::date,  'https://logo.clearbit.com/zoom.us',         NOW() - INTERVAL '120 days', NOW()),
        (gen_random_uuid(), user_ids[5], cat_work, 'DigitalOcean',     'Droplets + Spaces',            48.00, 'USD', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '130 days')::date, (NOW() + INTERVAL '1 day')::date,    'https://logo.clearbit.com/digitalocean.com',NOW() - INTERVAL '130 days', NOW()),
        (gen_random_uuid(), user_ids[5], cat_work, 'Google Workspace', 'Business Starter',              7.20, 'USD', 'MONTHLY',   'ACTIVE',    (NOW() - INTERVAL '140 days')::date, (NOW() + INTERVAL '21 days')::date,  'https://logo.clearbit.com/google.com',      NOW() - INTERVAL '140 days', NOW());

        -- ── USERS 6-10: medium usage (5 subs each) ──
        -- User 6: Nadia
        INSERT INTO subscription (id, client_id, category_id, name, description, price, original_currency, frequency, status, start_date, next_billing_date, logo_url, created_at, updated_at) VALUES
        (gen_random_uuid(), user_ids[6], cat_ent,  'Netflix',          'Standard',                     15.49, 'USD', 'MONTHLY', 'ACTIVE',    (NOW() - INTERVAL '125 days')::date, (NOW() + INTERVAL '9 days')::date,  'https://logo.clearbit.com/netflix.com',  NOW()-INTERVAL '125 days', NOW()),
        (gen_random_uuid(), user_ids[6], cat_ent,  'Spotify',          'Duo',                          14.99, 'USD', 'MONTHLY', 'ACTIVE',    (NOW() - INTERVAL '125 days')::date, (NOW() + INTERVAL '12 days')::date, 'https://logo.clearbit.com/spotify.com',  NOW()-INTERVAL '125 days', NOW()),
        (gen_random_uuid(), user_ids[6], cat_cloud,'iCloud+',          '50 Go',                         0.99, 'USD', 'MONTHLY', 'ACTIVE',    (NOW() - INTERVAL '130 days')::date, (NOW() + INTERVAL '23 days')::date, 'https://logo.clearbit.com/icloud.com',   NOW()-INTERVAL '130 days', NOW()),
        (gen_random_uuid(), user_ids[6], cat_prod, 'Trello',           'Standard',                      5.00, 'USD', 'MONTHLY', 'CANCELLED', (NOW() - INTERVAL '100 days')::date, NULL, 'https://logo.clearbit.com/trello.com', NOW()-INTERVAL '100 days', NOW()-INTERVAL '20 days'),
        (gen_random_uuid(), user_ids[6], cat_other,'Strava',           'Premium',                       5.00, 'USD', 'MONTHLY', 'ACTIVE',    (NOW() - INTERVAL '80 days')::date,  (NOW() + INTERVAL '5 days')::date,  'https://logo.clearbit.com/strava.com',   NOW()-INTERVAL '80 days', NOW());

        -- User 7: Omar (freelancer)
        INSERT INTO subscription (id, client_id, category_id, name, description, price, original_currency, frequency, status, start_date, next_billing_date, logo_url, created_at, updated_at) VALUES
        (gen_random_uuid(), user_ids[7], cat_work, 'Figma',            'Professional',                 15.00, 'EUR', 'MONTHLY', 'ACTIVE',    (NOW() - INTERVAL '115 days')::date, (NOW() + INTERVAL '7 days')::date,  'https://logo.clearbit.com/figma.com',    NOW()-INTERVAL '115 days', NOW()),
        (gen_random_uuid(), user_ids[7], cat_work, 'Adobe Creative Cloud','Photographe',               11.99, 'EUR', 'MONTHLY', 'ACTIVE',    (NOW() - INTERVAL '110 days')::date, (NOW() + INTERVAL '15 days')::date, 'https://logo.clearbit.com/adobe.com',    NOW()-INTERVAL '110 days', NOW()),
        (gen_random_uuid(), user_ids[7], cat_work, 'ChatGPT Plus',     'GPT-4',                        20.00, 'EUR', 'MONTHLY', 'ACTIVE',    (NOW() - INTERVAL '90 days')::date,  (NOW() + INTERVAL '4 days')::date,  'https://logo.clearbit.com/openai.com',   NOW()-INTERVAL '90 days', NOW()),
        (gen_random_uuid(), user_ids[7], cat_ent,  'Spotify',          'Premium',                       9.99, 'EUR', 'MONTHLY', 'ACTIVE',    (NOW() - INTERVAL '115 days')::date, (NOW() + INTERVAL '10 days')::date, 'https://logo.clearbit.com/spotify.com',  NOW()-INTERVAL '115 days', NOW()),
        (gen_random_uuid(), user_ids[7], cat_util, 'NordVPN',          'Mensuel',                      12.99, 'EUR', 'MONTHLY', 'PAUSED',    (NOW() - INTERVAL '100 days')::date, NULL, 'https://logo.clearbit.com/nordvpn.com', NOW()-INTERVAL '100 days', NOW()-INTERVAL '10 days');

        -- User 8: Layla
        INSERT INTO subscription (id, client_id, category_id, name, description, price, original_currency, frequency, status, start_date, next_billing_date, logo_url, created_at, updated_at) VALUES
        (gen_random_uuid(), user_ids[8], cat_ent,  'Netflix',          'Standard',                     15.49, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '105 days')::date, (NOW()+INTERVAL '13 days')::date, 'https://logo.clearbit.com/netflix.com', NOW()-INTERVAL '105 days', NOW()),
        (gen_random_uuid(), user_ids[8], cat_ent,  'Disney+',          'Standard',                     13.99, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '100 days')::date, (NOW()+INTERVAL '6 days')::date,  'https://logo.clearbit.com/disneyplus.com', NOW()-INTERVAL '100 days', NOW()),
        (gen_random_uuid(), user_ids[8], cat_ent,  'Apple TV+',        'Mensuel',                       9.99, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '80 days')::date,  (NOW()+INTERVAL '20 days')::date, 'https://logo.clearbit.com/apple.com', NOW()-INTERVAL '80 days', NOW()),
        (gen_random_uuid(), user_ids[8], cat_cloud,'Google One',       '100 Go',                        1.99, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '105 days')::date, (NOW()+INTERVAL '24 days')::date, 'https://logo.clearbit.com/google.com', NOW()-INTERVAL '105 days', NOW()),
        (gen_random_uuid(), user_ids[8], cat_other,'Duolingo Plus',    'Espagnol',                      7.99, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '60 days')::date,  (NOW()+INTERVAL '16 days')::date, 'https://logo.clearbit.com/duolingo.com', NOW()-INTERVAL '60 days', NOW());

        -- User 9: Mehdi (B2B)
        INSERT INTO subscription (id, client_id, category_id, name, description, price, original_currency, frequency, status, start_date, next_billing_date, logo_url, created_at, updated_at) VALUES
        (gen_random_uuid(), user_ids[9], cat_work, 'ChatGPT Plus',    'Team',                          25.00, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '95 days')::date,  (NOW()+INTERVAL '2 days')::date,  'https://logo.clearbit.com/openai.com',   NOW()-INTERVAL '95 days', NOW()),
        (gen_random_uuid(), user_ids[9], cat_prod, 'Asana',           'Premium',                       10.99, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '90 days')::date,  (NOW()+INTERVAL '8 days')::date,  'https://logo.clearbit.com/asana.com',    NOW()-INTERVAL '90 days', NOW()),
        (gen_random_uuid(), user_ids[9], cat_prod, 'Monday.com',      'Pro',                           16.00, 'USD', 'MONTHLY', 'CANCELLED',(NOW()-INTERVAL '85 days')::date, NULL, 'https://logo.clearbit.com/monday.com', NOW()-INTERVAL '85 days', NOW()-INTERVAL '10 days'),
        (gen_random_uuid(), user_ids[9], cat_work, 'Heroku',          'Hobby',                          7.00, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '95 days')::date,  (NOW()+INTERVAL '11 days')::date, 'https://logo.clearbit.com/heroku.com',   NOW()-INTERVAL '95 days', NOW()),
        (gen_random_uuid(), user_ids[9], cat_ent,  'Spotify',         'Premium',                        9.99, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '80 days')::date,  (NOW()+INTERVAL '18 days')::date, 'https://logo.clearbit.com/spotify.com',  NOW()-INTERVAL '80 days', NOW()),
        (gen_random_uuid(), user_ids[9], cat_util, '1Password',       'Teams',                          7.99, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '95 days')::date,  (NOW()+INTERVAL '5 days')::date,  'https://logo.clearbit.com/1password.com',NOW()-INTERVAL '95 days', NOW());

        -- User 10: Zineb
        INSERT INTO subscription (id, client_id, category_id, name, description, price, original_currency, frequency, status, start_date, next_billing_date, logo_url, created_at, updated_at) VALUES
        (gen_random_uuid(), user_ids[10], cat_ent,  'Netflix',        'Standard',                      15.49, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '90 days')::date,  (NOW()+INTERVAL '4 days')::date,  'https://logo.clearbit.com/netflix.com',  NOW()-INTERVAL '90 days', NOW()),
        (gen_random_uuid(), user_ids[10], cat_ent,  'Crunchyroll',    'Premium',                        7.99, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '85 days')::date,  (NOW()+INTERVAL '15 days')::date, 'https://logo.clearbit.com/crunchyroll.com', NOW()-INTERVAL '85 days', NOW()),
        (gen_random_uuid(), user_ids[10], cat_work, 'Canva Pro',      'Mensuel',                       12.99, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '70 days')::date,  (NOW()+INTERVAL '21 days')::date, 'https://logo.clearbit.com/canva.com',    NOW()-INTERVAL '70 days', NOW()),
        (gen_random_uuid(), user_ids[10], cat_cloud,'OneDrive',       '100 Go',                         1.99, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '90 days')::date,  (NOW()+INTERVAL '27 days')::date, 'https://logo.clearbit.com/microsoft.com',NOW()-INTERVAL '90 days', NOW());

        -- ── USERS 11-15: varied usage ──
        -- User 11: Amine (suspended, had 3 subs all cancelled)
        INSERT INTO subscription (id, client_id, category_id, name, description, price, original_currency, frequency, status, start_date, next_billing_date, logo_url, created_at, updated_at) VALUES
        (gen_random_uuid(), user_ids[11], cat_ent,  'Netflix',        'Basic',                          9.99, 'USD', 'MONTHLY', 'CANCELLED', (NOW()-INTERVAL '85 days')::date, NULL, 'https://logo.clearbit.com/netflix.com',  NOW()-INTERVAL '85 days', NOW()-INTERVAL '20 days'),
        (gen_random_uuid(), user_ids[11], cat_ent,  'Spotify',        'Free to Premium',                9.99, 'USD', 'MONTHLY', 'CANCELLED', (NOW()-INTERVAL '80 days')::date, NULL, 'https://logo.clearbit.com/spotify.com',  NOW()-INTERVAL '80 days', NOW()-INTERVAL '20 days'),
        (gen_random_uuid(), user_ids[11], cat_work, 'ChatGPT Plus',   'GPT-4',                         20.00, 'USD', 'MONTHLY', 'CANCELLED', (NOW()-INTERVAL '75 days')::date, NULL, 'https://logo.clearbit.com/openai.com',   NOW()-INTERVAL '75 days', NOW()-INTERVAL '20 days');

        -- User 12: Hiba (freelancer — 6 subs, GBP)
        INSERT INTO subscription (id, client_id, category_id, name, description, price, original_currency, frequency, status, start_date, next_billing_date, logo_url, created_at, updated_at) VALUES
        (gen_random_uuid(), user_ids[12], cat_work, 'Figma',           'Professional',                 12.00, 'GBP', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '75 days')::date, (NOW()+INTERVAL '9 days')::date,  'https://logo.clearbit.com/figma.com',    NOW()-INTERVAL '75 days', NOW()),
        (gen_random_uuid(), user_ids[12], cat_work, 'Copilot',         'Individual',                    8.00, 'GBP', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '70 days')::date, (NOW()+INTERVAL '14 days')::date, 'https://logo.clearbit.com/github.com',   NOW()-INTERVAL '70 days', NOW()),
        (gen_random_uuid(), user_ids[12], cat_work, 'Midjourney',      'Standard',                     24.00, 'GBP', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '60 days')::date, (NOW()+INTERVAL '6 days')::date,  'https://logo.clearbit.com/midjourney.com',NOW()-INTERVAL '60 days', NOW()),
        (gen_random_uuid(), user_ids[12], cat_ent,  'Spotify',         'Premium',                       9.99, 'GBP', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '75 days')::date, (NOW()+INTERVAL '11 days')::date, 'https://logo.clearbit.com/spotify.com',  NOW()-INTERVAL '75 days', NOW()),
        (gen_random_uuid(), user_ids[12], cat_ent,  'Netflix',         'Standard',                     12.99, 'GBP', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '75 days')::date, (NOW()+INTERVAL '18 days')::date, 'https://logo.clearbit.com/netflix.com',  NOW()-INTERVAL '75 days', NOW()),
        (gen_random_uuid(), user_ids[12], cat_util, 'Bitwarden',       'Premium',                      10.00, 'GBP', 'ANNUAL',  'ACTIVE', (NOW()-INTERVAL '75 days')::date, (NOW()+INTERVAL '290 days')::date,'https://logo.clearbit.com/bitwarden.com',NOW()-INTERVAL '75 days', NOW());

        -- User 13-16: light users (3-4 subs each)
        INSERT INTO subscription (id, client_id, category_id, name, description, price, original_currency, frequency, status, start_date, next_billing_date, logo_url, created_at, updated_at) VALUES
        (gen_random_uuid(), user_ids[13], cat_ent,  'Netflix',         'Standard',                     15.49, 'MAD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '65 days')::date, (NOW()+INTERVAL '3 days')::date,  'https://logo.clearbit.com/netflix.com',  NOW()-INTERVAL '65 days', NOW()),
        (gen_random_uuid(), user_ids[13], cat_ent,  'HBO Max',         'Standard',                     15.99, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '50 days')::date, (NOW()+INTERVAL '19 days')::date, 'https://logo.clearbit.com/hbomax.com',   NOW()-INTERVAL '50 days', NOW()),
        (gen_random_uuid(), user_ids[13], cat_work, 'ChatGPT Plus',    'Individual',                   20.00, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '60 days')::date, (NOW()+INTERVAL '7 days')::date,  'https://logo.clearbit.com/openai.com',   NOW()-INTERVAL '60 days', NOW());

        INSERT INTO subscription (id, client_id, category_id, name, description, price, original_currency, frequency, status, start_date, next_billing_date, logo_url, created_at, updated_at) VALUES
        (gen_random_uuid(), user_ids[14], cat_ent,  'Netflix',         'Premium',                      22.99, 'EUR', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '55 days')::date, (NOW()+INTERVAL '8 days')::date,  'https://logo.clearbit.com/netflix.com',  NOW()-INTERVAL '55 days', NOW()),
        (gen_random_uuid(), user_ids[14], cat_ent,  'Spotify',         'Famille',                      17.99, 'EUR', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '55 days')::date, (NOW()+INTERVAL '12 days')::date, 'https://logo.clearbit.com/spotify.com',  NOW()-INTERVAL '55 days', NOW()),
        (gen_random_uuid(), user_ids[14], cat_cloud,'iCloud+',         '2 To',                          9.99, 'EUR', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '50 days')::date, (NOW()+INTERVAL '22 days')::date, 'https://logo.clearbit.com/icloud.com',   NOW()-INTERVAL '50 days', NOW()),
        (gen_random_uuid(), user_ids[14], cat_other,'Headspace',       'Annuel',                       69.99, 'EUR', 'ANNUAL',  'ACTIVE', (NOW()-INTERVAL '45 days')::date, (NOW()+INTERVAL '320 days')::date,'https://logo.clearbit.com/headspace.com',NOW()-INTERVAL '45 days', NOW());

        INSERT INTO subscription (id, client_id, category_id, name, description, price, original_currency, frequency, status, start_date, next_billing_date, logo_url, created_at, updated_at) VALUES
        (gen_random_uuid(), user_ids[15], cat_work, 'JetBrains',       'IntelliJ IDEA',               199.00, 'USD', 'ANNUAL',  'ACTIVE', (NOW()-INTERVAL '45 days')::date, (NOW()+INTERVAL '320 days')::date,'https://logo.clearbit.com/jetbrains.com',NOW()-INTERVAL '45 days', NOW()),
        (gen_random_uuid(), user_ids[15], cat_work, 'ChatGPT Plus',    'Team',                         25.00, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '40 days')::date, (NOW()+INTERVAL '5 days')::date,  'https://logo.clearbit.com/openai.com',   NOW()-INTERVAL '40 days', NOW()),
        (gen_random_uuid(), user_ids[15], cat_prod, 'Linear',          'Standard',                      8.00, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '45 days')::date, (NOW()+INTERVAL '10 days')::date, 'https://logo.clearbit.com/linear.app',   NOW()-INTERVAL '45 days', NOW()),
        (gen_random_uuid(), user_ids[15], cat_ent,  'Netflix',         'Standard',                     15.49, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '50 days')::date, (NOW()+INTERVAL '15 days')::date, 'https://logo.clearbit.com/netflix.com',  NOW()-INTERVAL '50 days', NOW());

        INSERT INTO subscription (id, client_id, category_id, name, description, price, original_currency, frequency, status, start_date, next_billing_date, logo_url, created_at, updated_at) VALUES
        (gen_random_uuid(), user_ids[16], cat_ent,  'Netflix',         'Standard',                     15.49, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '35 days')::date, (NOW()+INTERVAL '7 days')::date,  'https://logo.clearbit.com/netflix.com',  NOW()-INTERVAL '35 days', NOW()),
        (gen_random_uuid(), user_ids[16], cat_ent,  'Spotify',         'Premium',                       9.99, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '35 days')::date, (NOW()+INTERVAL '13 days')::date, 'https://logo.clearbit.com/spotify.com',  NOW()-INTERVAL '35 days', NOW()),
        (gen_random_uuid(), user_ids[16], cat_work, 'Canva Pro',       'Pro',                          12.99, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '30 days')::date, (NOW()+INTERVAL '2 days')::date,  'https://logo.clearbit.com/canva.com',    NOW()-INTERVAL '30 days', NOW());

        -- User 17: Adil
        INSERT INTO subscription (id, client_id, category_id, name, description, price, original_currency, frequency, status, start_date, next_billing_date, logo_url, created_at, updated_at) VALUES
        (gen_random_uuid(), user_ids[17], cat_work, 'GitHub Pro',      'Individual',                    4.00, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '25 days')::date, (NOW()+INTERVAL '3 days')::date,  'https://logo.clearbit.com/github.com',   NOW()-INTERVAL '25 days', NOW()),
        (gen_random_uuid(), user_ids[17], cat_work, 'Copilot',         'Monthly',                      10.00, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '25 days')::date, (NOW()+INTERVAL '3 days')::date,  'https://logo.clearbit.com/github.com',   NOW()-INTERVAL '25 days', NOW()),
        (gen_random_uuid(), user_ids[17], cat_ent,  'Spotify',         'Premium',                       9.99, 'EUR', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '28 days')::date, (NOW()+INTERVAL '2 days')::date,  'https://logo.clearbit.com/spotify.com',  NOW()-INTERVAL '28 days', NOW()),
        (gen_random_uuid(), user_ids[17], cat_prod, 'Notion',          'Plus',                          10.00, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '20 days')::date, (NOW()+INTERVAL '10 days')::date, 'https://logo.clearbit.com/notion.so',    NOW()-INTERVAL '20 days', NOW());

        -- User 18: Kenza (new user, 2 subs)
        INSERT INTO subscription (id, client_id, category_id, name, description, price, original_currency, frequency, status, start_date, next_billing_date, logo_url, created_at, updated_at) VALUES
        (gen_random_uuid(), user_ids[18], cat_ent,  'Netflix',         'Standard',                     15.49, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '15 days')::date, (NOW()+INTERVAL '15 days')::date, 'https://logo.clearbit.com/netflix.com',  NOW()-INTERVAL '15 days', NOW()),
        (gen_random_uuid(), user_ids[18], cat_ent,  'Spotify',         'Premium',                       9.99, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '18 days')::date, (NOW()+INTERVAL '12 days')::date, 'https://logo.clearbit.com/spotify.com',  NOW()-INTERVAL '18 days', NOW());

        -- User 19: Othmane (suspended, 2 subs cancelled)
        INSERT INTO subscription (id, client_id, category_id, name, description, price, original_currency, frequency, status, start_date, next_billing_date, logo_url, created_at, updated_at) VALUES
        (gen_random_uuid(), user_ids[19], cat_ent,  'Netflix',         'Basic',                         9.99, 'USD', 'MONTHLY', 'CANCELLED', (NOW()-INTERVAL '12 days')::date, NULL, 'https://logo.clearbit.com/netflix.com', NOW()-INTERVAL '12 days', NOW()-INTERVAL '5 days'),
        (gen_random_uuid(), user_ids[19], cat_work, 'ChatGPT Plus',    'Individual',                   20.00, 'USD', 'MONTHLY', 'CANCELLED', (NOW()-INTERVAL '10 days')::date, NULL, 'https://logo.clearbit.com/openai.com',  NOW()-INTERVAL '10 days', NOW()-INTERVAL '5 days');

        -- User 20: Salma (newest user, 3 subs)
        INSERT INTO subscription (id, client_id, category_id, name, description, price, original_currency, frequency, status, start_date, next_billing_date, logo_url, created_at, updated_at) VALUES
        (gen_random_uuid(), user_ids[20], cat_ent,  'Netflix',         'Standard',                     15.49, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '8 days')::date,  (NOW()+INTERVAL '22 days')::date, 'https://logo.clearbit.com/netflix.com',  NOW()-INTERVAL '8 days', NOW()),
        (gen_random_uuid(), user_ids[20], cat_ent,  'Amazon Prime',    'Mensuel',                      14.99, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '7 days')::date,  (NOW()+INTERVAL '23 days')::date, 'https://logo.clearbit.com/amazon.com',   NOW()-INTERVAL '7 days', NOW()),
        (gen_random_uuid(), user_ids[20], cat_work, 'ChatGPT Plus',    'Plus',                         20.00, 'USD', 'MONTHLY', 'ACTIVE', (NOW()-INTERVAL '5 days')::date,  (NOW()+INTERVAL '25 days')::date, 'https://logo.clearbit.com/openai.com',   NOW()-INTERVAL '5 days', NOW());

        -- ─────────────────────────────────────────────────────────────────────
        -- 5. PAYMENT HISTORY  (3-6 months of payments per active subscription)
        -- ─────────────────────────────────────────────────────────────────────
        FOR sub_id IN (SELECT s.id FROM subscription s WHERE s.client_id = ANY(user_ids) AND s.status = 'ACTIVE' AND s.frequency = 'MONTHLY')
        LOOP
            FOR i IN 1..6 LOOP
                INSERT INTO payment_history (id, subscription_id, amount, currency, payment_date, created_at)
                SELECT gen_random_uuid(), sub_id, s.price, s.original_currency,
                       (s.start_date + (i * INTERVAL '1 month'))::date,
                       s.created_at + (i * INTERVAL '1 month')
                FROM subscription s WHERE s.id = sub_id
                AND (s.start_date + (i * INTERVAL '1 month'))::date <= CURRENT_DATE;
            END LOOP;
        END LOOP;

        -- Quarterly payments
        FOR sub_id IN (SELECT s.id FROM subscription s WHERE s.client_id = ANY(user_ids) AND s.status = 'ACTIVE' AND s.frequency = 'QUARTERLY')
        LOOP
            FOR i IN 1..2 LOOP
                INSERT INTO payment_history (id, subscription_id, amount, currency, payment_date, created_at)
                SELECT gen_random_uuid(), sub_id, s.price, s.original_currency,
                       (s.start_date + (i * INTERVAL '3 months'))::date,
                       s.created_at + (i * INTERVAL '3 months')
                FROM subscription s WHERE s.id = sub_id
                AND (s.start_date + (i * INTERVAL '3 months'))::date <= CURRENT_DATE;
            END LOOP;
        END LOOP;

        -- Annual payments (initial payment)
        INSERT INTO payment_history (id, subscription_id, amount, currency, payment_date, created_at)
        SELECT gen_random_uuid(), s.id, s.price, s.original_currency, s.start_date, s.created_at
        FROM subscription s WHERE s.client_id = ANY(user_ids) AND s.status = 'ACTIVE' AND s.frequency = 'ANNUAL';

        -- Cancelled/paused subs get 1-2 payments
        FOR sub_id IN (SELECT s.id FROM subscription s WHERE s.client_id = ANY(user_ids) AND s.status IN ('CANCELLED', 'PAUSED'))
        LOOP
            FOR i IN 0..1 LOOP
                INSERT INTO payment_history (id, subscription_id, amount, currency, payment_date, created_at)
                SELECT gen_random_uuid(), sub_id, s.price, s.original_currency,
                       (s.start_date + (i * INTERVAL '1 month'))::date,
                       s.created_at + (i * INTERVAL '1 month')
                FROM subscription s WHERE s.id = sub_id
                AND (s.start_date + (i * INTERVAL '1 month'))::date <= CURRENT_DATE;
            END LOOP;
        END LOOP;

        -- ─────────────────────────────────────────────────────────────────────
        -- 6. ALERT RULES  (on ~60% of active subscriptions)
        -- ─────────────────────────────────────────────────────────────────────
        INSERT INTO alert_rule (id, subscription_id, channel, timing_days, is_active, created_at, updated_at)
        SELECT gen_random_uuid(), s.id, 'EMAIL', 3, true, s.created_at, NOW()
        FROM subscription s WHERE s.client_id = ANY(user_ids) AND s.status = 'ACTIVE'
        AND random() < 0.6;

        INSERT INTO alert_rule (id, subscription_id, channel, timing_days, is_active, created_at, updated_at)
        SELECT gen_random_uuid(), s.id, 'EMAIL', 7, true, s.created_at, NOW()
        FROM subscription s WHERE s.client_id = ANY(user_ids) AND s.status = 'ACTIVE'
        AND random() < 0.3;

        -- Telegram alerts for users with telegram enabled
        INSERT INTO alert_rule (id, subscription_id, channel, timing_days, is_active, created_at, updated_at)
        SELECT gen_random_uuid(), s.id, 'TELEGRAM', 1, true, s.created_at, NOW()
        FROM subscription s
        JOIN client c ON s.client_id = c.id
        WHERE s.client_id = ANY(user_ids) AND s.status = 'ACTIVE' AND c.telegram_enabled = true
        AND random() < 0.5;

        -- ─────────────────────────────────────────────────────────────────────
        -- 7. NOTIFICATION LOG  (simulated past alerts)
        -- ─────────────────────────────────────────────────────────────────────
        -- Successful email notifications
        INSERT INTO notification_log (id, subscription_id, channel, client_email, recipient_identifier, delivery_status, sent_at)
        SELECT gen_random_uuid(), s.id, 'EMAIL', c.email, c.email, 'SENT',
               NOW() - (random() * INTERVAL '90 days')
        FROM subscription s
        JOIN client c ON s.client_id = c.id
        WHERE s.client_id = ANY(user_ids) AND s.status = 'ACTIVE'
        AND random() < 0.7;

        -- A few more historical notifications
        INSERT INTO notification_log (id, subscription_id, channel, client_email, recipient_identifier, delivery_status, sent_at)
        SELECT gen_random_uuid(), s.id, 'EMAIL', c.email, c.email, 'SENT',
               NOW() - (random() * INTERVAL '60 days')
        FROM subscription s
        JOIN client c ON s.client_id = c.id
        WHERE s.client_id = ANY(user_ids) AND s.status = 'ACTIVE'
        AND random() < 0.5;

        -- Some failed notifications
        INSERT INTO notification_log (id, subscription_id, channel, client_email, recipient_identifier, delivery_status, error_message, sent_at)
        SELECT gen_random_uuid(), s.id, 'EMAIL', c.email, c.email, 'FAILED', 'SMTP connection timeout',
               NOW() - (random() * INTERVAL '30 days')
        FROM subscription s
        JOIN client c ON s.client_id = c.id
        WHERE s.client_id = ANY(user_ids) AND s.status = 'ACTIVE'
        AND random() < 0.05;

        -- ─────────────────────────────────────────────────────────────────────
        -- 8. INVOICES  (imported invoices for a few users)
        -- ─────────────────────────────────────────────────────────────────────
        -- Processed invoices
        INSERT INTO invoice (id, client_id, service_name, amount, currency, invoice_date, raw_content, is_processed, parse_attempts, created_at) VALUES
        (gen_random_uuid(), user_ids[1], 'Netflix',      15.49, 'USD', (NOW()-INTERVAL '30 days')::date, 'From: netflix@email.com\nSubject: Your Netflix bill\nAmount: $15.49\nDate: ...',  true, 1, NOW()-INTERVAL '29 days'),
        (gen_random_uuid(), user_ids[1], 'Spotify',       9.99, 'USD', (NOW()-INTERVAL '28 days')::date, 'From: no-reply@spotify.com\nSubject: Your Spotify Premium receipt\nAmount: $9.99', true, 1, NOW()-INTERVAL '27 days'),
        (gen_random_uuid(), user_ids[1], 'ChatGPT',      20.00, 'USD', (NOW()-INTERVAL '25 days')::date, 'From: billing@openai.com\nSubject: OpenAI receipt\nAmount: $20.00',              true, 1, NOW()-INTERVAL '24 days'),
        (gen_random_uuid(), user_ids[3], 'Adobe',        54.99, 'EUR', (NOW()-INTERVAL '20 days')::date, 'From: mail@adobe.com\nSubject: Adobe Creative Cloud\nAmount: €54.99',            true, 1, NOW()-INTERVAL '19 days'),
        (gen_random_uuid(), user_ids[3], 'Figma',        15.00, 'USD', (NOW()-INTERVAL '18 days')::date, 'From: billing@figma.com\nSubject: Figma invoice\nAmount: $15.00',                true, 2, NOW()-INTERVAL '17 days'),
        (gen_random_uuid(), user_ids[5], 'DigitalOcean', 48.00, 'USD', (NOW()-INTERVAL '15 days')::date, 'From: billing@digitalocean.com\nSubject: DigitalOcean invoice\nAmount: $48.00',  true, 1, NOW()-INTERVAL '14 days'),
        (gen_random_uuid(), user_ids[5], 'Zoom',         13.33, 'USD', (NOW()-INTERVAL '12 days')::date, 'From: no-reply@zoom.us\nSubject: Zoom Pro billing\nAmount: $13.33',              true, 1, NOW()-INTERVAL '11 days'),
        (gen_random_uuid(), user_ids[2], 'YouTube',      22.99, 'USD', (NOW()-INTERVAL '10 days')::date, 'From: payments-noreply@google.com\nSubject: Google payment\nAmount: $22.99',     true, 1, NOW()-INTERVAL '9 days');

        -- Unprocessed invoices (pending AI parsing)
        INSERT INTO invoice (id, client_id, service_name, amount, currency, invoice_date, raw_content, is_processed, parse_attempts, created_at) VALUES
        (gen_random_uuid(), user_ids[1], NULL, NULL, NULL, NULL, 'From: unknown@billing.com\nSubject: Monthly subscription\nDear customer, your monthly payment of $12.99 has been processed...', false, 0, NOW()-INTERVAL '2 days'),
        (gen_random_uuid(), user_ids[5], NULL, NULL, NULL, NULL, 'From: receipts@heroku.com\nSubject: Heroku invoice\nHeroku Hobby $7.00 charged to your card ending in 4242...', false, 0, NOW()-INTERVAL '1 day'),
        (gen_random_uuid(), user_ids[3], NULL, NULL, NULL, NULL, 'From: billing@vercel.com\nSubject: Vercel Pro Plan\nYour Vercel Pro subscription: $20.00/month...', false, 3, NOW()-INTERVAL '5 days'),
        (gen_random_uuid(), user_ids[9], NULL, NULL, NULL, NULL, 'From: do-not-reply@asana.com\nSubject: Asana Premium receipt\nThank you for your Asana Premium subscription...', false, 1, NOW()-INTERVAL '3 days');

    END;
END $$;

-- ─────────────────────────────────────────────────────────────────────
-- 9. SYSTEM CONFIG  (API integrations placeholder)
-- ─────────────────────────────────────────────────────────────────────
INSERT INTO system_config (config_key, config_value, updated_at) VALUES
('EXCHANGE_RATE_API_KEY', NULL, NOW()),
('SMTP_HOST', 'smtp.gmail.com', NOW()),
('SMTP_PORT', '587', NOW()),
('SMTP_USERNAME', NULL, NOW()),
('SMTP_PASSWORD', NULL, NOW()),
('TELEGRAM_BOT_TOKEN', NULL, NOW()),
('WHATSAPP_API_URL', NULL, NOW()),
('WHATSAPP_API_TOKEN', NULL, NOW())
ON CONFLICT (config_key) DO NOTHING;
