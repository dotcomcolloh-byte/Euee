package com.telefam.company.server.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class PostgresTelefamRepository(
    jdbcUrl: String,
    username: String,
    password: String
) : TelefamRepository {

    private val dataSource: HikariDataSource

    init {
        val cfg = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = username
            this.password = password
            maximumPoolSize = 16
            isAutoCommit = false
        }
        dataSource = HikariDataSource(cfg)
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate()
    }

    private fun <T> withRlsTx(role: String, userId: String?, block: (Connection) -> T): T {
        dataSource.connection.use { conn ->
            conn.autoCommit = false
            try {
                conn.prepareStatement("SELECT set_config('app.current_role', ?, true)").use { st ->
                    st.setString(1, role)
                    st.execute()
                }
                conn.prepareStatement("SELECT set_config('app.current_user_id', ?, true)").use { st ->
                    st.setString(1, userId ?: "")
                    st.execute()
                }
                val res = block(conn)
                conn.commit()
                return res
            } catch (e: Throwable) {
                runCatching { conn.rollback() }
                throw e
            }
        }
    }

    override fun findOtpChallenge(emailHmac: String, deviceIdHash: String): OtpChallengeRecord? =
        withRlsTx("telefam_auth_service", null) { conn ->
            conn.prepareStatement(
                """
                SELECT id, email_hmac, device_id_hash, otp_hmac, expires_at, resend_available_at,
                       failed_attempts, lock_cycles, locked_until, consumed
                FROM otp_challenges
                WHERE email_hmac = ? AND device_id_hash = ?
                """.trimIndent()
            ).use { st ->
                st.setString(1, emailHmac)
                st.setString(2, deviceIdHash)
                st.executeQuery().use { rs ->
                    if (!rs.next()) null else rs.toOtpChallenge()
                }
            }
        }

    override fun upsertOtpChallenge(
        emailHmac: String,
        deviceIdHash: String,
        otpHmac: String,
        expiresAt: Instant,
        resendAvailableAt: Instant,
        failedAttempts: Int,
        lockCycles: Int,
        lockedUntil: Instant?,
        consumed: Boolean
    ) {
        withRlsTx("telefam_auth_service", null) { conn ->
            conn.prepareStatement(
                """
                INSERT INTO otp_challenges (
                    email_hmac, device_id_hash, otp_hmac, expires_at, resend_available_at,
                    failed_attempts, lock_cycles, locked_until, consumed
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (email_hmac, device_id_hash) DO UPDATE SET
                    otp_hmac = EXCLUDED.otp_hmac,
                    expires_at = EXCLUDED.expires_at,
                    resend_available_at = EXCLUDED.resend_available_at,
                    failed_attempts = EXCLUDED.failed_attempts,
                    lock_cycles = EXCLUDED.lock_cycles,
                    locked_until = EXCLUDED.locked_until,
                    consumed = EXCLUDED.consumed
                """.trimIndent()
            ).use { st ->
                st.setString(1, emailHmac)
                st.setString(2, deviceIdHash)
                st.setString(3, otpHmac)
                st.setTimestamp(4, Timestamp.from(expiresAt))
                st.setTimestamp(5, Timestamp.from(resendAvailableAt))
                st.setInt(6, failedAttempts)
                st.setInt(7, lockCycles)
                st.setTimestamp(8, lockedUntil?.let { Timestamp.from(it) })
                st.setBoolean(9, consumed)
                st.executeUpdate()
            }
        }
    }

    override fun updateOtpFailureState(
        emailHmac: String,
        deviceIdHash: String,
        failedAttempts: Int,
        lockCycles: Int,
        lockedUntil: Instant?
    ) {
        withRlsTx("telefam_auth_service", null) { conn ->
            conn.prepareStatement(
                """
                UPDATE otp_challenges
                SET failed_attempts = ?, lock_cycles = ?, locked_until = ?
                WHERE email_hmac = ? AND device_id_hash = ?
                """.trimIndent()
            ).use { st ->
                st.setInt(1, failedAttempts)
                st.setInt(2, lockCycles)
                st.setTimestamp(3, lockedUntil?.let { Timestamp.from(it) })
                st.setString(4, emailHmac)
                st.setString(5, deviceIdHash)
                st.executeUpdate()
            }
        }
    }

    override fun markOtpConsumed(emailHmac: String, deviceIdHash: String) {
        withRlsTx("telefam_auth_service", null) { conn ->
            conn.prepareStatement(
                """
                UPDATE otp_challenges
                SET consumed = TRUE, failed_attempts = 0, lock_cycles = 0, locked_until = NULL
                WHERE email_hmac = ? AND device_id_hash = ?
                """.trimIndent()
            ).use { st ->
                st.setString(1, emailHmac)
                st.setString(2, deviceIdHash)
                st.executeUpdate()
            }
        }
    }

    override fun findUserByEmailHmac(emailHmac: String): UserRecord? =
        withRlsTx("telefam_auth_service", null) { conn ->
            conn.prepareStatement("SELECT * FROM users WHERE email_hmac = ?").use { st ->
                st.setString(1, emailHmac)
                st.executeQuery().use { rs -> if (!rs.next()) null else rs.toUserRecord() }
            }
        }

    override fun findUserByAppleSubHmac(appleSubHmac: String): UserRecord? =
        withRlsTx("telefam_auth_service", null) { conn ->
            conn.prepareStatement("SELECT * FROM users WHERE apple_sub_hmac = ?").use { st ->
                st.setString(1, appleSubHmac)
                st.executeQuery().use { rs -> if (!rs.next()) null else rs.toUserRecord() }
            }
        }

    override fun findUserById(userId: String): UserRecord? =
        withRlsTx("telefam_user", userId) { conn ->
            conn.prepareStatement("SELECT * FROM users WHERE id = ?").use { st ->
                st.setObject(1, UUID.fromString(userId))
                st.executeQuery().use { rs -> if (!rs.next()) null else rs.toUserRecord() }
            }
        }

    override fun createUser(
        emailEncrypted: String,
        emailHmac: String,
        googleSubHmac: String?,
        appleSubHmac: String?
    ): UserRecord =
        withRlsTx("telefam_auth_service", null) { conn ->
            val id = UUID.randomUUID()
            conn.prepareStatement(
                """
                INSERT INTO users (id, email_encrypted, email_hmac, google_sub_hmac, apple_sub_hmac)
                VALUES (?, ?, ?, ?, ?)
                RETURNING *
                """.trimIndent()
            ).use { st ->
                st.setObject(1, id)
                st.setString(2, emailEncrypted)
                st.setString(3, emailHmac)
                st.setString(4, googleSubHmac)
                st.setString(5, appleSubHmac)
                st.executeQuery().use { rs ->
                    rs.next()
                    rs.toUserRecord()
                }
            }
        }

    override fun linkAppleSub(userId: String, appleSubHmac: String) {
        withRlsTx("telefam_auth_service", userId) { conn ->
            conn.prepareStatement(
                "UPDATE users SET apple_sub_hmac = COALESCE(apple_sub_hmac, ?), updated_at = NOW() WHERE id = ?"
            ).use { st ->
                st.setString(1, appleSubHmac)
                st.setObject(2, UUID.fromString(userId))
                st.executeUpdate()
            }
        }
    }

    override fun isUsernameAvailable(usernameLower: String, excludeUserId: String): Boolean =
        withRlsTx("telefam_auth_service", null) { conn ->
            conn.prepareStatement(
                "SELECT 1 FROM users WHERE username_lower = ? AND id <> ?"
            ).use { st ->
                st.setString(1, usernameLower)
                st.setObject(2, UUID.fromString(excludeUserId))
                st.executeQuery().use { rs -> !rs.next() }
            }
        }

    override fun upsertUserDevice(
        userId: String,
        deviceIdHash: String,
        hardwareName: String,
        imeiAttestationHash: String,
        publicKeySpki: String,
        ipAddress: String
    ) {
        withRlsTx("telefam_auth_service", userId) { conn ->
            conn.prepareStatement(
                """
                INSERT INTO user_devices (
                    user_id, device_id_hash, device_hardware_name, imei_attestation_hash,
                    device_public_key_spki, first_ip, last_ip
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id, device_id_hash) DO UPDATE SET
                    device_hardware_name = EXCLUDED.device_hardware_name,
                    imei_attestation_hash = EXCLUDED.imei_attestation_hash,
                    device_public_key_spki = EXCLUDED.device_public_key_spki,
                    last_ip = EXCLUDED.last_ip,
                    last_seen_at = NOW()
                """.trimIndent()
            ).use { st ->
                st.setObject(1, UUID.fromString(userId))
                st.setString(2, deviceIdHash)
                st.setString(3, hardwareName)
                st.setString(4, imeiAttestationHash)
                st.setString(5, publicKeySpki)
                st.setString(6, ipAddress)
                st.setString(7, ipAddress)
                st.executeUpdate()
            }
        }
    }

    override fun createDeviceSession(
        sessionId: String,
        userId: String,
        deviceIdHash: String,
        accessTokenHmac: String,
        refreshTokenEncrypted: String,
        refreshTokenHmac: String,
        expiresAt: Instant
    ) {
        withRlsTx("telefam_auth_service", userId) { conn ->
            conn.prepareStatement(
                """
                INSERT INTO device_sessions (
                    id, user_id, device_id_hash, access_token_hmac,
                    refresh_token_encrypted, refresh_token_hmac, expires_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { st ->
                st.setObject(1, UUID.fromString(sessionId))
                st.setObject(2, UUID.fromString(userId))
                st.setString(3, deviceIdHash)
                st.setString(4, accessTokenHmac)
                st.setString(5, refreshTokenEncrypted)
                st.setString(6, refreshTokenHmac)
                st.setTimestamp(7, Timestamp.from(expiresAt))
                st.executeUpdate()
            }
        }
    }

    override fun findActiveSession(sessionId: String, userId: String, deviceIdHash: String): DeviceSessionRecord? =
        withRlsTx("telefam_user", userId) { conn ->
            conn.prepareStatement(
                """
                SELECT id, user_id, device_id_hash, access_token_hmac, expires_at, revoked
                FROM device_sessions
                WHERE id = ? AND user_id = ? AND device_id_hash = ? AND revoked = FALSE AND expires_at > NOW()
                """.trimIndent()
            ).use { st ->
                st.setObject(1, UUID.fromString(sessionId))
                st.setObject(2, UUID.fromString(userId))
                st.setString(3, deviceIdHash)
                st.executeQuery().use { rs ->
                    if (!rs.next()) null else DeviceSessionRecord(
                        id = rs.getObject("id", UUID::class.java).toString(),
                        userId = rs.getObject("user_id", UUID::class.java).toString(),
                        deviceIdHash = rs.getString("device_id_hash"),
                        accessTokenHmac = rs.getString("access_token_hmac"),
                        expiresAt = rs.getTimestamp("expires_at").toInstant(),
                        revoked = rs.getBoolean("revoked")
                    )
                }
            }
        }

    override fun findSessionByRefreshHmac(refreshTokenHmac: String, deviceIdHash: String): DeviceSessionRecord? =
        withRlsTx("telefam_auth_service", null) { conn ->
            conn.prepareStatement(
                """
                SELECT id, user_id, device_id_hash, access_token_hmac, expires_at, revoked
                FROM device_sessions
                WHERE refresh_token_hmac = ? AND device_id_hash = ? AND revoked = FALSE AND expires_at > NOW()
                """.trimIndent()
            ).use { st ->
                st.setString(1, refreshTokenHmac)
                st.setString(2, deviceIdHash)
                st.executeQuery().use { rs ->
                    if (!rs.next()) null else DeviceSessionRecord(
                        id = rs.getObject("id", UUID::class.java).toString(),
                        userId = rs.getObject("user_id", UUID::class.java).toString(),
                        deviceIdHash = rs.getString("device_id_hash"),
                        accessTokenHmac = rs.getString("access_token_hmac"),
                        expiresAt = rs.getTimestamp("expires_at").toInstant(),
                        revoked = rs.getBoolean("revoked")
                    )
                }
            }
        }

    override fun rotateSessionTokens(
        sessionId: String,
        userId: String,
        newAccessTokenHmac: String,
        newRefreshTokenEncrypted: String,
        newRefreshTokenHmac: String,
        newExpiresAt: Instant
    ) {
        withRlsTx("telefam_auth_service", userId) { conn ->
            conn.prepareStatement(
                """
                UPDATE device_sessions
                SET access_token_hmac = ?,
                    refresh_token_encrypted = ?,
                    refresh_token_hmac = ?,
                    expires_at = ?
                WHERE id = ? AND user_id = ? AND revoked = FALSE
                """.trimIndent()
            ).use { st ->
                st.setString(1, newAccessTokenHmac)
                st.setString(2, newRefreshTokenEncrypted)
                st.setString(3, newRefreshTokenHmac)
                st.setTimestamp(4, Timestamp.from(newExpiresAt))
                st.setObject(5, UUID.fromString(sessionId))
                st.setObject(6, UUID.fromString(userId))
                st.executeUpdate()
            }
        }
    }

    override fun revokeSession(sessionId: String, userId: String) {
        withRlsTx("telefam_user", userId) { conn ->
            conn.prepareStatement(
                "UPDATE device_sessions SET revoked = TRUE WHERE id = ? AND user_id = ?"
            ).use { st ->
                st.setObject(1, UUID.fromString(sessionId))
                st.setObject(2, UUID.fromString(userId))
                st.executeUpdate()
            }
        }
    }

    override fun updateUserAvatar(
        userId: String,
        objectKey: String,
        sha256Hex: String,
        dhashHex: String,
        mimeType: String,
        byteSize: Int,
        width: Int,
        height: Int
    ) {
        withRlsTx("telefam_user", userId) { conn ->
            conn.prepareStatement(
                """
                INSERT INTO photo_fingerprints (user_id, object_key, sha256_hex, dhash_hex, mime_type, byte_size, width, height)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { st ->
                st.setObject(1, UUID.fromString(userId))
                st.setString(2, objectKey)
                st.setString(3, sha256Hex)
                st.setString(4, dhashHex)
                st.setString(5, mimeType)
                st.setInt(6, byteSize)
                st.setInt(7, width)
                st.setInt(8, height)
                st.executeUpdate()
            }
            conn.prepareStatement(
                """
                UPDATE users
                SET avatar_object_key = ?, avatar_fingerprint_sha256 = ?, avatar_fingerprint_dhash = ?, updated_at = NOW()
                WHERE id = ?
                """.trimIndent()
            ).use { st ->
                st.setString(1, objectKey)
                st.setString(2, sha256Hex)
                st.setString(3, dhashHex)
                st.setObject(4, UUID.fromString(userId))
                st.executeUpdate()
            }
        }
    }

    override fun updateUserProfile(
        userId: String,
        fullName: String,
        username: String,
        usernameLower: String,
        phoneEncrypted: String,
        phoneHmac: String,
        phoneCountryIso: String,
        gender: String,
        dobEncrypted: String,
        locationEncrypted: String,
        bioEncrypted: String,
        usernameChangedAt: Instant?
    ): UserRecord = withRlsTx("telefam_user", userId) { conn ->
        conn.prepareStatement(
            """
            UPDATE users SET
                full_name = ?, username = ?, username_lower = ?,
                phone_encrypted = ?, phone_hmac = ?, phone_country_iso = ?,
                gender = ?, dob_encrypted = ?, location_encrypted = ?, bio_encrypted = ?,
                username_changed_at = COALESCE(?, username_changed_at),
                profile_completed = TRUE, updated_at = NOW()
            WHERE id = ?
            RETURNING *
            """.trimIndent()
        ).use { st ->
            st.setString(1, fullName)
            st.setString(2, username)
            st.setString(3, usernameLower)
            st.setString(4, phoneEncrypted)
            st.setString(5, phoneHmac)
            st.setString(6, phoneCountryIso)
            st.setString(7, gender)
            st.setString(8, dobEncrypted)
            st.setString(9, locationEncrypted)
            st.setString(10, bioEncrypted)
            st.setTimestamp(11, usernameChangedAt?.let { Timestamp.from(it) })
            st.setObject(12, UUID.fromString(userId))
            st.executeQuery().use { rs ->
                rs.next()
                rs.toUserRecord()
            }
        }
    }

    override fun getProfileStats(userId: String): ProfileStatsRecord =
        withRlsTx("telefam_user", userId) { conn ->
            val uid = UUID.fromString(userId)
            val followers = conn.prepareStatement(
                "SELECT COUNT(*) FROM user_follows WHERE followee_id = ?"
            ).use { st ->
                st.setObject(1, uid)
                st.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
            }
            val following = conn.prepareStatement(
                "SELECT COUNT(*) FROM user_follows WHERE follower_id = ?"
            ).use { st ->
                st.setObject(1, uid)
                st.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
            }
            ProfileStatsRecord(
                followersCount = followers,
                followingCount = following,
                likesCount = 0,
                postsCount = 0
            )
        }

    override fun listUserBackgroundPhotos(userId: String): List<BackgroundPhotoRecord> =
        withRlsTx("telefam_user", userId) { conn ->
            conn.prepareStatement(
                """
                SELECT id, user_id, object_key, sha256_hex, dhash_hex, mime_type, byte_size, width, height, display_order
                FROM user_background_photos
                WHERE user_id = ?
                ORDER BY display_order ASC, created_at ASC
                LIMIT 5
                """.trimIndent()
            ).use { st ->
                st.setObject(1, UUID.fromString(userId))
                st.executeQuery().use { rs ->
                    val list = mutableListOf<BackgroundPhotoRecord>()
                    while (rs.next()) {
                        list.add(rs.toBackgroundPhotoRecord())
                    }
                    list
                }
            }
        }

    override fun countUserBackgroundPhotos(userId: String): Int =
        withRlsTx("telefam_user", userId) { conn ->
            conn.prepareStatement(
                "SELECT COUNT(*) FROM user_background_photos WHERE user_id = ?"
            ).use { st ->
                st.setObject(1, UUID.fromString(userId))
                st.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
            }
        }

    override fun addUserBackgroundPhoto(
        userId: String,
        objectKey: String,
        sha256Hex: String,
        dhashHex: String,
        mimeType: String,
        byteSize: Int,
        width: Int,
        height: Int
    ): BackgroundPhotoRecord = withRlsTx("telefam_user", userId) { conn ->
        val uid = UUID.fromString(userId)
        conn.prepareStatement(
            """
            INSERT INTO photo_fingerprints (user_id, object_key, sha256_hex, dhash_hex, mime_type, byte_size, width, height)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { st ->
            st.setObject(1, uid)
            st.setString(2, objectKey)
            st.setString(3, sha256Hex)
            st.setString(4, dhashHex)
            st.setString(5, mimeType)
            st.setInt(6, byteSize)
            st.setInt(7, width)
            st.setInt(8, height)
            st.executeUpdate()
        }

        val nextOrder = conn.prepareStatement(
            "SELECT COALESCE(MAX(display_order), -1) + 1 FROM user_background_photos WHERE user_id = ?"
        ).use { st ->
            st.setObject(1, uid)
            st.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
        }

        conn.prepareStatement(
            """
            INSERT INTO user_background_photos (
                user_id, object_key, sha256_hex, dhash_hex, mime_type, byte_size, width, height, display_order
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id, user_id, object_key, sha256_hex, dhash_hex, mime_type, byte_size, width, height, display_order
            """.trimIndent()
        ).use { st ->
            st.setObject(1, uid)
            st.setString(2, objectKey)
            st.setString(3, sha256Hex)
            st.setString(4, dhashHex)
            st.setString(5, mimeType)
            st.setInt(6, byteSize)
            st.setInt(7, width)
            st.setInt(8, height)
            st.setInt(9, nextOrder)
            st.executeQuery().use { rs ->
                rs.next()
                rs.toBackgroundPhotoRecord()
            }
        }
    }

    override fun deleteUserBackgroundPhoto(userId: String, photoId: String): String? =
        withRlsTx("telefam_user", userId) { conn ->
            conn.prepareStatement(
                "DELETE FROM user_background_photos WHERE id = ? AND user_id = ? RETURNING object_key"
            ).use { st ->
                st.setObject(1, UUID.fromString(photoId))
                st.setObject(2, UUID.fromString(userId))
                st.executeQuery().use { rs -> if (rs.next()) rs.getString("object_key") else null }
            }
        }

    override fun appendSecurityAuditLog(entry: AuditLogEntry) {
        val role = if (entry.userId != null) "telefam_user" else "telefam_auth_service"
        withRlsTx(role, entry.userId) { conn ->
            conn.prepareStatement(
                """
                INSERT INTO security_audit_logs (
                    user_id, event_type, severity, device_hardware_name,
                    imei_or_device_hash, ip_address, device_id_hash, success, details_json
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
                """.trimIndent()
            ).use { st ->
                st.setObject(1, entry.userId?.let { UUID.fromString(it) })
                st.setString(2, entry.eventType)
                st.setString(3, entry.severity)
                st.setString(4, entry.deviceHardwareName)
                st.setString(5, entry.imeiOrDeviceHash)
                st.setString(6, entry.ipAddress)
                st.setString(7, entry.deviceIdHash)
                st.setBoolean(8, entry.success)
                st.setString(9, entry.detailsJson)
                st.executeUpdate()
            }
        }
    }

    override fun listDirectoryContacts(viewerUserId: String): List<ContactDirectoryRecord> =
        withRlsTx("telefam_user", viewerUserId) { conn ->
            val viewerUuid = UUID.fromString(viewerUserId)
            conn.prepareStatement(
                """
                SELECT
                    u.id,
                    u.full_name,
                    u.username,
                    u.phone_country_iso,
                    u.avatar_object_key,
                    EXISTS (
                        SELECT 1 FROM user_follows f_out
                        WHERE f_out.follower_id = ? AND f_out.followee_id = u.id
                    ) AS i_follow,
                    EXISTS (
                        SELECT 1 FROM user_follows f_in
                        WHERE f_in.follower_id = u.id AND f_in.followee_id = ?
                    ) AS follows_me
                FROM users u
                WHERE u.profile_completed = TRUE
                  AND u.username IS NOT NULL
                  AND u.id <> ?
                ORDER BY LOWER(u.full_name) ASC, LOWER(u.username) ASC
                LIMIT 500
                """.trimIndent()
            ).use { st ->
                st.setObject(1, viewerUuid)
                st.setObject(2, viewerUuid)
                st.setObject(3, viewerUuid)
                st.executeQuery().use { rs ->
                    val list = mutableListOf<ContactDirectoryRecord>()
                    while (rs.next()) {
                        list.add(
                            ContactDirectoryRecord(
                                userId = rs.getObject("id", UUID::class.java).toString(),
                                fullName = rs.getString("full_name") ?: "",
                                username = rs.getString("username") ?: "",
                                phoneCountryIso = rs.getString("phone_country_iso") ?: "KE",
                                avatarObjectKey = rs.getString("avatar_object_key"),
                                iFollow = rs.getBoolean("i_follow"),
                                followsMe = rs.getBoolean("follows_me")
                            )
                        )
                    }
                    list
                }
            }
        }

    override fun countRecentFollows(userId: String, since: Instant): Int =
        withRlsTx("telefam_user", userId) { conn ->
            conn.prepareStatement(
                "SELECT COUNT(*) FROM follow_rate_events WHERE user_id = ? AND created_at >= ?"
            ).use { st ->
                st.setObject(1, UUID.fromString(userId))
                st.setTimestamp(2, Timestamp.from(since))
                st.executeQuery().use { rs ->
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        }

    override fun recordFollowRateEvent(userId: String, at: Instant) {
        withRlsTx("telefam_user", userId) { conn ->
            conn.prepareStatement(
                "INSERT INTO follow_rate_events (user_id, created_at) VALUES (?, ?)"
            ).use { st ->
                st.setObject(1, UUID.fromString(userId))
                st.setTimestamp(2, Timestamp.from(at))
                st.executeUpdate()
            }
        }
    }

    override fun followUser(followerId: String, targetUserId: String): FollowEdgeResult? =
        withRlsTx("telefam_user", followerId) { conn ->
            val followerUuid = UUID.fromString(followerId)
            val targetUuid = UUID.fromString(targetUserId)
            val targetExists = conn.prepareStatement(
                "SELECT 1 FROM users WHERE id = ? AND profile_completed = TRUE"
            ).use { st ->
                st.setObject(1, targetUuid)
                st.executeQuery().use { rs -> rs.next() }
            }
            if (!targetExists) return@withRlsTx null

            conn.prepareStatement(
                """
                INSERT INTO user_follows (follower_id, followee_id)
                VALUES (?, ?)
                ON CONFLICT (follower_id, followee_id) DO NOTHING
                """.trimIndent()
            ).use { st ->
                st.setObject(1, followerUuid)
                st.setObject(2, targetUuid)
                st.executeUpdate()
            }

            val followsMe = conn.prepareStatement(
                "SELECT 1 FROM user_follows WHERE follower_id = ? AND followee_id = ?"
            ).use { st ->
                st.setObject(1, targetUuid)
                st.setObject(2, followerUuid)
                st.executeQuery().use { rs -> rs.next() }
            }
            FollowEdgeResult(targetUserId = targetUserId, iFollow = true, followsMe = followsMe)
        }

    override fun unfollowUser(followerId: String, targetUserId: String): FollowEdgeResult? =
        withRlsTx("telefam_user", followerId) { conn ->
            val followerUuid = UUID.fromString(followerId)
            val targetUuid = UUID.fromString(targetUserId)
            val targetExists = conn.prepareStatement(
                "SELECT 1 FROM users WHERE id = ? AND profile_completed = TRUE"
            ).use { st ->
                st.setObject(1, targetUuid)
                st.executeQuery().use { rs -> rs.next() }
            }
            if (!targetExists) return@withRlsTx null

            conn.prepareStatement(
                "DELETE FROM user_follows WHERE follower_id = ? AND followee_id = ?"
            ).use { st ->
                st.setObject(1, followerUuid)
                st.setObject(2, targetUuid)
                st.executeUpdate()
            }

            val followsMe = conn.prepareStatement(
                "SELECT 1 FROM user_follows WHERE follower_id = ? AND followee_id = ?"
            ).use { st ->
                st.setObject(1, targetUuid)
                st.setObject(2, followerUuid)
                st.executeQuery().use { rs -> rs.next() }
            }
            FollowEdgeResult(targetUserId = targetUserId, iFollow = false, followsMe = followsMe)
        }

    private fun ResultSet.toOtpChallenge() = OtpChallengeRecord(
        id = getObject("id", UUID::class.java).toString(),
        emailHmac = getString("email_hmac"),
        deviceIdHash = getString("device_id_hash"),
        otpHmac = getString("otp_hmac"),
        expiresAt = getTimestamp("expires_at").toInstant(),
        resendAvailableAt = getTimestamp("resend_available_at").toInstant(),
        failedAttempts = getInt("failed_attempts"),
        lockCycles = getInt("lock_cycles"),
        lockedUntil = getTimestamp("locked_until")?.toInstant(),
        consumed = getBoolean("consumed")
    )

    private fun ResultSet.toUserRecord() = UserRecord(
        id = getObject("id", UUID::class.java).toString(),
        emailEncrypted = getString("email_encrypted"),
        emailHmac = getString("email_hmac"),
        googleSubHmac = getString("google_sub_hmac"),
        fullName = getString("full_name") ?: "",
        username = getString("username"),
        usernameLower = getString("username_lower"),
        phoneEncrypted = getString("phone_encrypted"),
        phoneCountryIso = getString("phone_country_iso") ?: "KE",
        gender = getString("gender"),
        dobEncrypted = getString("dob_encrypted"),
        locationEncrypted = getString("location_encrypted"),
        bioEncrypted = getString("bio_encrypted"),
        avatarObjectKey = getString("avatar_object_key"),
        avatarFingerprintSha256 = getString("avatar_fingerprint_sha256"),
        avatarFingerprintDhash = getString("avatar_fingerprint_dhash"),
        profileCompleted = getBoolean("profile_completed"),
        usernameChangedAt = getTimestamp("username_changed_at")?.toInstant(),
        appleSubHmac = runCatching { getString("apple_sub_hmac") }.getOrNull()
    )

    private fun ResultSet.toBackgroundPhotoRecord() = BackgroundPhotoRecord(
        id = getObject("id", UUID::class.java).toString(),
        userId = getObject("user_id", UUID::class.java).toString(),
        objectKey = getString("object_key"),
        sha256Hex = getString("sha256_hex"),
        dhashHex = getString("dhash_hex"),
        mimeType = getString("mime_type"),
        byteSize = getInt("byte_size"),
        width = getInt("width"),
        height = getInt("height"),
        displayOrder = getInt("display_order")
    )
}
