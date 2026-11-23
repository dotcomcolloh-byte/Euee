CREATE TABLE IF NOT EXISTS user_follows (
    follower_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    followee_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (follower_id, followee_id),
    CHECK (follower_id <> followee_id)
);

CREATE TABLE IF NOT EXISTS follow_rate_events (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_follows_follower ON user_follows(follower_id);
CREATE INDEX IF NOT EXISTS idx_user_follows_followee ON user_follows(followee_id);
CREATE INDEX IF NOT EXISTS idx_follow_rate_user_time ON follow_rate_events(user_id, created_at DESC);

ALTER TABLE user_follows ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_follows FORCE ROW LEVEL SECURITY;

ALTER TABLE follow_rate_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE follow_rate_events FORCE ROW LEVEL SECURITY;

-- Allow authenticated users to discover completed user profiles in Contacts directory
CREATE POLICY users_directory_select ON users FOR SELECT
    USING (
        current_setting('app.current_role', true) = 'telefam_user'
        AND profile_completed = TRUE
    );

-- Users can see follow edges involving themselves
CREATE POLICY user_follows_select ON user_follows FOR SELECT
    USING (
        follower_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid
        OR followee_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid
    );

-- Users can only follow as themselves
CREATE POLICY user_follows_insert ON user_follows FOR INSERT
    WITH CHECK (
        follower_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid
        AND follower_id <> followee_id
    );

-- Users can only unfollow their own outgoing edges
CREATE POLICY user_follows_delete ON user_follows FOR DELETE
    USING (
        follower_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid
    );

-- Follow rate events scoped strictly to owner
CREATE POLICY follow_rate_owner_select ON follow_rate_events FOR SELECT
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);

CREATE POLICY follow_rate_owner_insert ON follow_rate_events FOR INSERT
    WITH CHECK (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);
