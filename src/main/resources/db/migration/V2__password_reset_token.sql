-- Single-use password reset tokens (only the SHA-256 hash is stored).
-- IF NOT EXISTS: hbm2ddl may already have added these columns on databases created before migrations.
ALTER TABLE client ADD COLUMN IF NOT EXISTS password_reset_token_hash VARCHAR(64);
ALTER TABLE client ADD COLUMN IF NOT EXISTS password_reset_expires_at TIMESTAMP(6);
