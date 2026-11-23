ALTER TABLE users ADD COLUMN IF NOT EXISTS username_changed_at TIMESTAMPTZ;

UPDATE users
SET username_changed_at = updated_at
WHERE profile_completed = TRUE AND username_changed_at IS NULL;

CREATE TABLE IF NOT EXISTS user_background_photos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    object_key VARCHAR(128) NOT NULL UNIQUE,
    sha256_hex VARCHAR(64) NOT NULL,
    dhash_hex VARCHAR(16) NOT NULL,
    mime_type VARCHAR(32) NOT NULL,
    byte_size INT NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_bg_photos_user ON user_background_photos(user_id, display_order ASC, created_at ASC);

ALTER TABLE user_background_photos ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_background_photos FORCE ROW LEVEL SECURITY;

CREATE POLICY user_bg_photos_owner_select ON user_background_photos FOR SELECT
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);

CREATE POLICY user_bg_photos_owner_insert ON user_background_photos FOR INSERT
    WITH CHECK (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);

CREATE POLICY user_bg_photos_owner_delete ON user_background_photos FOR DELETE
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);
