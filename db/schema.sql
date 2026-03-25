-- SubTrack Database Schema for PostgreSQL
-- Version: 1.0

-- Enable UUID extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================
-- TABLES
-- ============================================

-- Users (Base User) Table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role VARCHAR(10) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Client Table
CREATE TABLE client (
    id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    account_type VARCHAR(20) NOT NULL DEFAULT 'B2C',
    timezone VARCHAR(50) DEFAULT 'UTC',
    email_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    telegram_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    telegram_chat_id VARCHAR(50),
    whatsapp_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    whatsapp_number VARCHAR(20)
);

-- Admin Table
CREATE TABLE admin (
    id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE
);

-- Category Table
CREATE TABLE category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    icon VARCHAR(100),
    color VARCHAR(7),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Subscription Table
CREATE TABLE subscription (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES client(id) ON DELETE CASCADE,
    category_id UUID REFERENCES category(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    original_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    frequency VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    start_date DATE NOT NULL,
    next_billing_date DATE,
    logo_url VARCHAR(500),
    cancel_link VARCHAR(500),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Payment History Table
CREATE TABLE payment_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL REFERENCES subscription(id) ON DELETE CASCADE,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_date DATE NOT NULL,
    invoice_url VARCHAR(500),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Alert Rule Table
CREATE TABLE alert_rule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL REFERENCES subscription(id) ON DELETE CASCADE,
    channel VARCHAR(20) NOT NULL,
    timing_days INTEGER NOT NULL DEFAULT 3,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Email Integration Table
CREATE TABLE email_integration (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES client(id) ON DELETE CASCADE,
    email_address VARCHAR(255) NOT NULL,
    access_token TEXT,
    refresh_token TEXT,
    token_expires_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(client_id)
);

-- Invoice Table
CREATE TABLE invoice (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES client(id) ON DELETE CASCADE,
    raw_content TEXT,
    service_name VARCHAR(255),
    amount DECIMAL(10,2),
    currency VARCHAR(3),
    invoice_date DATE,
    is_processed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Exchange Rate Table
CREATE TABLE exchange_rate (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    rate DECIMAL(15,6) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(from_currency, to_currency)
);

-- SaaS Service (Catalog) Table
CREATE TABLE saas_service (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    category VARCHAR(100),
    logo_url VARCHAR(500),
    website_url VARCHAR(500),
    default_price DECIMAL(10,2),
    default_currency VARCHAR(3),
    default_frequency VARCHAR(20),
    is_popular BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- INDEXES
-- ============================================

CREATE INDEX idx_subscription_client ON subscription(client_id);
CREATE INDEX idx_subscription_category ON subscription(category_id);
CREATE INDEX idx_subscription_status ON subscription(status);
CREATE INDEX idx_payment_history_subscription ON payment_history(subscription_id);
CREATE INDEX idx_payment_history_date ON payment_history(payment_date);
CREATE INDEX idx_alert_rule_subscription ON alert_rule(subscription_id);
CREATE INDEX idx_invoice_client ON invoice(client_id);
CREATE INDEX idx_exchange_rate_currencies ON exchange_rate(from_currency, to_currency);

-- ============================================
-- DEFAULT DATA
-- ============================================

-- Default Categories
INSERT INTO category (name, description, icon, color) VALUES
('Work', 'Professional tools and software', 'briefcase', '#007bff'),
('Cloud', 'Cloud storage and services', 'cloud', '#17a2b8'),
('Entertainment', 'Streaming and media services', 'play-circle', '#dc3545'),
('Productivity', 'Productivity and collaboration tools', 'tasks', '#28a745'),
('Utilities', 'Utility and helper services', 'wrench', '#6c757d'),
('Other', 'Miscellaneous subscriptions', 'folder', '#ffc107')
ON CONFLICT (name) DO NOTHING;

-- NOTE: Admin and test user passwords are set correctly by DatabaseInitializer at startup.
-- DatabaseInitializer always inserts correct BCrypt-hashed passwords via JDBC on first startup.

-- Sample SaaS Services (name is UNIQUE so ON CONFLICT (name) is safe)
INSERT INTO saas_service (name, category, default_price, default_currency, default_frequency, is_popular) VALUES
('Netflix', 'Entertainment', 15.99, 'USD', 'MONTHLY', true),
('Spotify', 'Entertainment', 9.99, 'USD', 'MONTHLY', true),
('Amazon Prime', 'Entertainment', 14.99, 'USD', 'MONTHLY', true),
('Microsoft 365', 'Work', 12.99, 'USD', 'MONTHLY', true),
('Google Workspace', 'Work', 12.00, 'USD', 'MONTHLY', true),
('Dropbox', 'Cloud', 11.99, 'USD', 'MONTHLY', false),
('Adobe Creative Cloud', 'Work', 54.99, 'USD', 'MONTHLY', true),
('Slack', 'Productivity', 8.75, 'USD', 'MONTHLY', true),
('GitHub', 'Work', 4.00, 'USD', 'MONTHLY', true),
('Zoom', 'Productivity', 15.99, 'USD', 'MONTHLY', true)
ON CONFLICT (name) DO NOTHING;

-- ============================================
-- COMMENTS
-- ============================================

COMMENT ON TABLE client IS 'Users of the SubTrack platform';
COMMENT ON TABLE subscription IS 'Recurring subscriptions managed by users';
COMMENT ON TABLE payment_history IS 'Historical payment records for price change detection';
COMMENT ON TABLE category IS 'User-defined categories for organizing subscriptions';
COMMENT ON TABLE alert_rule IS 'Notification rules for subscription billing alerts';
COMMENT ON TABLE email_integration IS 'OAuth tokens for connected email accounts';
COMMENT ON TABLE invoice IS 'AI-processed invoice data from emails';
COMMENT ON TABLE exchange_rate IS 'Currency conversion rates';
COMMENT ON TABLE saas_service IS 'Catalog of known SaaS services';
