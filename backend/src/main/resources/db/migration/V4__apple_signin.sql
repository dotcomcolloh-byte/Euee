ALTER TABLE users
    ADD COLUMN IF NOT EXISTS apple_sub_hmac VARCHAR(64) UNIQUE;

CREATE INDEX IF NOT EXISTS idx_users_apple_sub_hmac
    ON users(apple_sub_hmac)
    WHERE apple_sub_hmac IS NOT NULL;
