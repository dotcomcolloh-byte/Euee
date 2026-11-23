CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Application database roles for strict PostgreSQL RLS enforcement
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'telefam_auth_service') THEN
        CREATE ROLE telefam_auth_service NOLOGIN;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'telefam_user') THEN
        CREATE ROLE telefam_user NOLOGIN;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email_encrypted TEXT NOT NULL,
    email_hmac VARCHAR(64) NOT NULL UNIQUE,
    google_sub_hmac VARCHAR(64) UNIQUE,
    full_name VARCHAR(64) NOT NULL DEFAULT '',
    username VARCHAR(24),
    username_lower VARCHAR(24) UNIQUE,
    phone_encrypted TEXT,
    phone_hmac VARCHAR(64),
    phone_country_iso VARCHAR(4) NOT NULL DEFAULT 'KE',
    gender VARCHAR(32),
    dob_encrypted TEXT,
    location_encrypted TEXT,
    bio_encrypted TEXT,
    avatar_object_key VARCHAR(128),
    avatar_fingerprint_sha256 VARCHAR(64),
    avatar_fingerprint_dhash VARCHAR(16),
    profile_completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS user_devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id_hash VARCHAR(64) NOT NULL,
    device_hardware_name VARCHAR(128) NOT NULL,
    imei_attestation_hash VARCHAR(64) NOT NULL,
    device_public_key_spki TEXT NOT NULL,
    first_ip VARCHAR(64) NOT NULL,
    last_ip VARCHAR(64) NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, device_id_hash)
);

CREATE TABLE IF NOT EXISTS device_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id_hash VARCHAR(64) NOT NULL,
    access_token_hmac VARCHAR(64) NOT NULL UNIQUE,
    refresh_token_encrypted TEXT NOT NULL,
    refresh_token_hmac VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS otp_challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email_hmac VARCHAR(64) NOT NULL,
    device_id_hash VARCHAR(64) NOT NULL,
    otp_hmac VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    resend_available_at TIMESTAMPTZ NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    lock_cycles INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (email_hmac, device_id_hash)
);

CREATE TABLE IF NOT EXISTS photo_fingerprints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    object_key VARCHAR(128) NOT NULL UNIQUE,
    sha256_hex VARCHAR(64) NOT NULL,
    dhash_hex VARCHAR(16) NOT NULL,
    mime_type VARCHAR(32) NOT NULL,
    byte_size INT NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS security_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    event_type VARCHAR(64) NOT NULL,
    severity VARCHAR(16) NOT NULL DEFAULT 'INFO',
    device_hardware_name VARCHAR(128) NOT NULL,
    imei_or_device_hash VARCHAR(64) NOT NULL,
    ip_address VARCHAR(64) NOT NULL,
    device_id_hash VARCHAR(64) NOT NULL,
    success BOOLEAN NOT NULL,
    details_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_username_lower ON users(username_lower);
CREATE INDEX IF NOT EXISTS idx_device_sessions_user_dev ON device_sessions(user_id, device_id_hash);
CREATE INDEX IF NOT EXISTS idx_photo_fp_sha256 ON photo_fingerprints(sha256_hex);
CREATE INDEX IF NOT EXISTS idx_photo_fp_dhash ON photo_fingerprints(dhash_hex);
CREATE INDEX IF NOT EXISTS idx_audit_user_created ON security_audit_logs(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_ip_created ON security_audit_logs(ip_address, created_at DESC);

-- Enable and force Row-Level Security on every table
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE users FORCE ROW LEVEL SECURITY;

ALTER TABLE user_devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_devices FORCE ROW LEVEL SECURITY;

ALTER TABLE device_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE device_sessions FORCE ROW LEVEL SECURITY;

ALTER TABLE otp_challenges ENABLE ROW LEVEL SECURITY;
ALTER TABLE otp_challenges FORCE ROW LEVEL SECURITY;

ALTER TABLE photo_fingerprints ENABLE ROW LEVEL SECURITY;
ALTER TABLE photo_fingerprints FORCE ROW LEVEL SECURITY;

ALTER TABLE security_audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE security_audit_logs FORCE ROW LEVEL SECURITY;

-- Users policies: users can only read/update their own row; auth service manages onboarding
CREATE POLICY users_self_select ON users FOR SELECT
    USING (
        current_setting('app.current_role', true) = 'telefam_auth_service'
        OR id = NULLIF(current_setting('app.current_user_id', true), '')::uuid
    );

CREATE POLICY users_self_update ON users FOR UPDATE
    USING (
        current_setting('app.current_role', true) = 'telefam_auth_service'
        OR id = NULLIF(current_setting('app.current_user_id', true), '')::uuid
    )
    WITH CHECK (
        current_setting('app.current_role', true) = 'telefam_auth_service'
        OR id = NULLIF(current_setting('app.current_user_id', true), '')::uuid
    );

CREATE POLICY users_auth_insert ON users FOR INSERT
    WITH CHECK (current_setting('app.current_role', true) = 'telefam_auth_service');

-- User devices & sessions policies: strictly scoped to owner user_id + device context
CREATE POLICY devices_owner_policy ON user_devices FOR ALL
    USING (
        current_setting('app.current_role', true) = 'telefam_auth_service'
        OR user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid
    )
    WITH CHECK (
        current_setting('app.current_role', true) = 'telefam_auth_service'
        OR user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid
    );

CREATE POLICY sessions_owner_policy ON device_sessions FOR ALL
    USING (
        current_setting('app.current_role', true) = 'telefam_auth_service'
        OR user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid
    )
    WITH CHECK (
        current_setting('app.current_role', true) = 'telefam_auth_service'
        OR user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid
    );

-- OTP challenges: strictly restricted to telefam_auth_service
CREATE POLICY otp_service_only ON otp_challenges FOR ALL
    USING (current_setting('app.current_role', true) = 'telefam_auth_service')
    WITH CHECK (current_setting('app.current_role', true) = 'telefam_auth_service');

-- Photo fingerprints: owner read/insert only
CREATE POLICY photo_fp_owner_select ON photo_fingerprints FOR SELECT
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);

CREATE POLICY photo_fp_owner_insert ON photo_fingerprints FOR INSERT
    WITH CHECK (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);

-- Security audit logs: append-only (INSERT allowed, UPDATE/DELETE denied by omission of update/delete policies)
CREATE POLICY audit_append_only_insert ON security_audit_logs FOR INSERT
    WITH CHECK (current_setting('app.current_role', true) IN ('telefam_auth_service', 'telefam_user'));

CREATE POLICY audit_owner_select ON security_audit_logs FOR SELECT
    USING (
        current_setting('app.current_role', true) = 'telefam_auth_service'
        OR user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid
    );
