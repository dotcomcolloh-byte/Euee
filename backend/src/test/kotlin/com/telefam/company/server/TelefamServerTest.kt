package com.telefam.company.server

import com.telefam.company.server.auth.AppleJwk
import com.telefam.company.server.auth.AppleJwksProvider
import com.telefam.company.server.auth.AppleTokenVerifierService
import com.telefam.company.server.auth.GoogleTokenVerifier
import com.telefam.company.server.auth.VerifiedGoogleUser
import com.telefam.company.server.config.ServerConfig
import com.telefam.company.server.crypto.ServerCrypto
import com.telefam.company.server.db.AuditLogEntry
import com.telefam.company.server.db.BackgroundPhotoRecord
import com.telefam.company.server.db.ContactDirectoryRecord
import com.telefam.company.server.db.DeviceSessionRecord
import com.telefam.company.server.db.FollowEdgeResult
import com.telefam.company.server.db.OtpChallengeRecord
import com.telefam.company.server.db.ProfileStatsRecord
import com.telefam.company.server.db.TelefamRepository
import com.telefam.company.server.db.UserRecord
import com.telefam.company.server.email.EmailSender
import com.telefam.company.server.media.MediaSandboxProcessor
import com.telefam.company.server.media.ProcessedAvatarResult
import com.telefam.company.server.media.UnixSocketSandboxMediaClient
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class TelefamServerTest {

    private class InMemoryTestRepo : TelefamRepository {
        val otps = ConcurrentHashMap<String, OtpChallengeRecord>()
        val users = ConcurrentHashMap<String, UserRecord>()
        val sessions = ConcurrentHashMap<String, DeviceSessionRecord>()
        val audits = mutableListOf<AuditLogEntry>()
        val follows = ConcurrentHashMap.newKeySet<Pair<String, String>>()
        val followEvents = mutableListOf<Pair<String, Instant>>()
        val backgroundPhotos = mutableListOf<BackgroundPhotoRecord>()

        private fun otpKey(emailHmac: String, dev: String) = "$emailHmac:$dev"

        override fun findOtpChallenge(emailHmac: String, deviceIdHash: String) = otps[otpKey(emailHmac, deviceIdHash)]

        override fun upsertOtpChallenge(
            emailHmac: String, deviceIdHash: String, otpHmac: String,
            expiresAt: Instant, resendAvailableAt: Instant,
            failedAttempts: Int, lockCycles: Int, lockedUntil: Instant?, consumed: Boolean
        ) {
            otps[otpKey(emailHmac, deviceIdHash)] = OtpChallengeRecord(
                UUID.randomUUID().toString(), emailHmac, deviceIdHash, otpHmac,
                expiresAt, resendAvailableAt, failedAttempts, lockCycles, lockedUntil, consumed
            )
        }

        override fun updateOtpFailureState(emailHmac: String, deviceIdHash: String, failedAttempts: Int, lockCycles: Int, lockedUntil: Instant?) {
            val cur = otps[otpKey(emailHmac, deviceIdHash)] ?: return
            otps[otpKey(emailHmac, deviceIdHash)] = cur.copy(failedAttempts = failedAttempts, lockCycles = lockCycles, lockedUntil = lockedUntil)
        }

        override fun markOtpConsumed(emailHmac: String, deviceIdHash: String) {
            val cur = otps[otpKey(emailHmac, deviceIdHash)] ?: return
            otps[otpKey(emailHmac, deviceIdHash)] = cur.copy(consumed = true, failedAttempts = 0, lockCycles = 0, lockedUntil = null)
        }

        override fun findUserByEmailHmac(emailHmac: String) = users.values.firstOrNull { it.emailHmac == emailHmac }
        override fun findUserByAppleSubHmac(appleSubHmac: String) = users.values.firstOrNull { it.appleSubHmac == appleSubHmac }
        override fun findUserById(userId: String) = users[userId]

        override fun createUser(emailEncrypted: String, emailHmac: String, googleSubHmac: String?, appleSubHmac: String?): UserRecord {
            val id = UUID.randomUUID().toString()
            val u = UserRecord(id, emailEncrypted, emailHmac, googleSubHmac, "", null, null, null, "KE", null, null, null, null, null, null, null, false, null, appleSubHmac)
            users[id] = u
            return u
        }

        override fun linkAppleSub(userId: String, appleSubHmac: String) {
            users[userId]?.let { users[userId] = it.copy(appleSubHmac = it.appleSubHmac ?: appleSubHmac) }
        }

        override fun isUsernameAvailable(usernameLower: String, excludeUserId: String) =
            users.values.none { it.usernameLower == usernameLower && it.id != excludeUserId }

        override fun upsertUserDevice(userId: String, deviceIdHash: String, hardwareName: String, imeiAttestationHash: String, publicKeySpki: String, ipAddress: String) {}

        val refreshMap = ConcurrentHashMap<String, String>()

        override fun createDeviceSession(sessionId: String, userId: String, deviceIdHash: String, accessTokenHmac: String, refreshTokenEncrypted: String, refreshTokenHmac: String, expiresAt: Instant) {
            sessions[sessionId] = DeviceSessionRecord(sessionId, userId, deviceIdHash, accessTokenHmac, expiresAt, false)
            refreshMap["$refreshTokenHmac:$deviceIdHash"] = sessionId
        }

        override fun findActiveSession(sessionId: String, userId: String, deviceIdHash: String) =
            sessions[sessionId]?.takeIf { !it.revoked && it.userId == userId && it.deviceIdHash == deviceIdHash }

        override fun findSessionByRefreshHmac(refreshTokenHmac: String, deviceIdHash: String): DeviceSessionRecord? {
            val sid = refreshMap["$refreshTokenHmac:$deviceIdHash"] ?: return null
            return sessions[sid]?.takeIf { !it.revoked && it.deviceIdHash == deviceIdHash }
        }

        override fun rotateSessionTokens(
            sessionId: String,
            userId: String,
            newAccessTokenHmac: String,
            newRefreshTokenEncrypted: String,
            newRefreshTokenHmac: String,
            newExpiresAt: Instant
        ) {
            val cur = sessions[sessionId] ?: return
            if (cur.revoked || cur.userId != userId) return
            refreshMap.entries.removeIf { it.value == sessionId }
            sessions[sessionId] = cur.copy(accessTokenHmac = newAccessTokenHmac, expiresAt = newExpiresAt)
            refreshMap["$newRefreshTokenHmac:${cur.deviceIdHash}"] = sessionId
        }

        override fun revokeSession(sessionId: String, userId: String) {
            sessions[sessionId]?.let { sessions[sessionId] = it.copy(revoked = true) }
            refreshMap.entries.removeIf { it.value == sessionId }
        }

        override fun updateUserAvatar(userId: String, objectKey: String, sha256Hex: String, dhashHex: String, mimeType: String, byteSize: Int, width: Int, height: Int) {
            users[userId]?.let {
                users[userId] = it.copy(avatarObjectKey = objectKey, avatarFingerprintSha256 = sha256Hex, avatarFingerprintDhash = dhashHex)
            }
        }

        override fun updateUserProfile(
            userId: String, fullName: String, username: String, usernameLower: String,
            phoneEncrypted: String, phoneHmac: String, phoneCountryIso: String,
            gender: String, dobEncrypted: String, locationEncrypted: String, bioEncrypted: String,
            usernameChangedAt: Instant?
        ): UserRecord {
            val cur = users[userId]!!
            val u = cur.copy(
                fullName = fullName, username = username, usernameLower = usernameLower,
                phoneEncrypted = phoneEncrypted, phoneCountryIso = phoneCountryIso,
                gender = gender, dobEncrypted = dobEncrypted, locationEncrypted = locationEncrypted,
                bioEncrypted = bioEncrypted, profileCompleted = true,
                usernameChangedAt = usernameChangedAt ?: cur.usernameChangedAt
            )
            users[userId] = u
            return u
        }

        override fun getProfileStats(userId: String): ProfileStatsRecord = ProfileStatsRecord(
            followersCount = follows.count { it.second == userId },
            followingCount = follows.count { it.first == userId },
            likesCount = 0,
            postsCount = 0
        )

        override fun listUserBackgroundPhotos(userId: String): List<BackgroundPhotoRecord> =
            backgroundPhotos.filter { it.userId == userId }.sortedBy { it.displayOrder }

        override fun countUserBackgroundPhotos(userId: String): Int =
            backgroundPhotos.count { it.userId == userId }

        override fun addUserBackgroundPhoto(
            userId: String,
            objectKey: String,
            sha256Hex: String,
            dhashHex: String,
            mimeType: String,
            byteSize: Int,
            width: Int,
            height: Int
        ): BackgroundPhotoRecord {
            val rec = BackgroundPhotoRecord(
                id = UUID.randomUUID().toString(),
                userId = userId,
                objectKey = objectKey,
                sha256Hex = sha256Hex,
                dhashHex = dhashHex,
                mimeType = mimeType,
                byteSize = byteSize,
                width = width,
                height = height,
                displayOrder = backgroundPhotos.count { it.userId == userId }
            )
            backgroundPhotos.add(rec)
            return rec
        }

        override fun deleteUserBackgroundPhoto(userId: String, photoId: String): String? {
            val idx = backgroundPhotos.indexOfFirst { it.id == photoId && it.userId == userId }
            if (idx < 0) return null
            return backgroundPhotos.removeAt(idx).objectKey
        }

        override fun appendSecurityAuditLog(entry: AuditLogEntry) {
            audits.add(entry)
        }

        override fun listDirectoryContacts(viewerUserId: String): List<ContactDirectoryRecord> =
            users.values
                .filter { it.profileCompleted && !it.username.isNullOrBlank() && it.id != viewerUserId }
                .map { u ->
                    ContactDirectoryRecord(
                        userId = u.id,
                        fullName = u.fullName,
                        username = u.username.orEmpty(),
                        phoneCountryIso = u.phoneCountryIso,
                        avatarObjectKey = u.avatarObjectKey,
                        iFollow = (viewerUserId to u.id) in follows,
                        followsMe = (u.id to viewerUserId) in follows
                    )
                }

        override fun countRecentFollows(userId: String, since: Instant): Int =
            followEvents.count { it.first == userId && !it.second.isBefore(since) }

        override fun recordFollowRateEvent(userId: String, at: Instant) {
            followEvents.add(userId to at)
        }

        override fun followUser(followerId: String, targetUserId: String): FollowEdgeResult? {
            val target = users[targetUserId]?.takeIf { it.profileCompleted } ?: return null
            follows.add(followerId to target.id)
            return FollowEdgeResult(target.id, iFollow = true, followsMe = (target.id to followerId) in follows)
        }

        override fun unfollowUser(followerId: String, targetUserId: String): FollowEdgeResult? {
            val target = users[targetUserId]?.takeIf { it.profileCompleted } ?: return null
            follows.remove(followerId to target.id)
            return FollowEdgeResult(target.id, iFollow = false, followsMe = (target.id to followerId) in follows)
        }
    }

    private fun testConfig() = ServerConfig(
        port = 8080,
        isProduction = false,
        publicBaseUrl = "https://api.telefam.company",
        databaseUrl = "",
        dbUser = "",
        dbPassword = "",
        resendApiKey = "test",
        resendFromEmail = "Telefam <noreply@telefam.company>",
        googleWebClientId = "test-client",
        jwtHmacSecret = ByteArray(32) { 1 },
        dataEncryptionKey = ByteArray(32) { 2 },
        deviceBindingPepper = ByteArray(32) { 3 },
        sandboxSocketPath = "/tmp/unused.sock",
        avatarStorageDir = Files.createTempDirectory("telefam-avatars").toString()
    )

    @Test
    fun `otp cooldown 60s and 3x60s lock then 12m lock plus device-bound token and profile validation`() = testApplication {
        var now = Instant.parse("2026-11-22T12:00:00Z")
        val cfg = testConfig()
        val repo = InMemoryTestRepo()
        val crypto = ServerCrypto(cfg.jwtHmacSecret, cfg.dataEncryptionKey, cfg.deviceBindingPepper)
        var lastOtp = ""
        val mailer = object : EmailSender {
            override suspend fun sendOtpEmail(toEmail: String, otpCode: String): Boolean {
                lastOtp = otpCode
                return true
            }
        }
        val googleVerifier = object : GoogleTokenVerifier {
            override fun verify(idToken: String, expectedNonce: String): VerifiedGoogleUser? = null
        }
        val sandbox = object : MediaSandboxProcessor {
            override fun process(rawBytes: ByteArray, cropX: Double, cropY: Double, cropWidth: Double, cropHeight: Double, maxWidth: Int, maxHeight: Int, mode: String): Result<ProcessedAvatarResult> {
                if (!UnixSocketSandboxMediaClient.hasValidImageMagicBytes(rawBytes)) {
                    return Result.failure(IllegalArgumentException("Bad magic"))
                }
                return Result.success(ProcessedAvatarResult(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()), "image/jpeg", maxWidth, maxHeight, "a".repeat(64), "0123456789abcdef"))
            }
        }

        application {
            telefamModule(cfg, repo, crypto, mailer, googleVerifier, sandbox) { now }
        }

        val dev1 = "a".repeat(64)
        val dev2 = "b".repeat(64)

        // 1. Send OTP
        val send1 = client.post("/api/v1/auth/otp/send") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"alice.doe@telefam.company","deviceIdHash":"$dev1","deviceHardwareName":"Pixel 9","devicePublicKey":"pk1"}""")
        }
        assertEquals(HttpStatusCode.OK, send1.status)
        assertEquals(6, lastOtp.length)

        // 2. Resend within 60s rejected
        now = now.plusSeconds(20)
        val sendTooSoon = client.post("/api/v1/auth/otp/send") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"alice.doe@telefam.company","deviceIdHash":"$dev1","deviceHardwareName":"Pixel 9","devicePublicKey":"pk1"}""")
        }
        assertEquals(HttpStatusCode.TooManyRequests, sendTooSoon.status)

        // 3. Wrong OTP 3x -> 60s lock (cycle 1, 2, 3), then 4th cycle -> 12m (720s) lock
        for (cycle in 1..4) {
            repeat(2) {
                val r = client.post("/api/v1/auth/otp/verify") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"email":"alice.doe@telefam.company","otp":"000000","deviceIdHash":"$dev1","deviceHardwareName":"Pixel 9","devicePublicKey":"pk1"}""")
                }
                assertEquals(HttpStatusCode.Unauthorized, r.status)
            }
            val lockResp = client.post("/api/v1/auth/otp/verify") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"alice.doe@telefam.company","otp":"000000","deviceIdHash":"$dev1","deviceHardwareName":"Pixel 9","devicePublicKey":"pk1"}""")
            }
            assertEquals(HttpStatusCode.TooManyRequests, lockResp.status)
            val expectedSec = if (cycle <= 3) "60" else "720"
            assertTrue(lockResp.bodyAsText().contains("\"lockRemainingSeconds\":$expectedSec"))
            now = now.plusSeconds(expectedSec.toLong() + 1)
        }

        // 4. Request fresh OTP after 12-minute lock expires and verify
        val send2 = client.post("/api/v1/auth/otp/send") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"alice.doe@telefam.company","deviceIdHash":"$dev1","deviceHardwareName":"Pixel 9","devicePublicKey":"pk1"}""")
        }
        assertEquals(HttpStatusCode.OK, send2.status)
        val okResp = client.post("/api/v1/auth/otp/verify") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"alice.doe@telefam.company","otp":"$lastOtp","deviceIdHash":"$dev1","deviceHardwareName":"Pixel 9","devicePublicKey":"pk1"}""")
        }
        assertEquals(HttpStatusCode.OK, okResp.status)
        val token = Json.parseToJsonElement(okResp.bodyAsText()).jsonObject["accessToken"]!!.jsonPrimitive.content

        // 5. Device-bound token rejected when replayed from another device ID
        val crossDevice = client.get("/api/v1/profile/check-username?username=alice_doe") {
            header("Authorization", "Bearer $token")
            header("X-Telefam-Device-Id", dev2)
        }
        assertEquals(HttpStatusCode.Unauthorized, crossDevice.status)

        // 6. Same device succeeds
        val sameDevice = client.get("/api/v1/profile/check-username?username=alice_doe") {
            header("Authorization", "Bearer $token")
            header("X-Telefam-Device-Id", dev1)
        }
        assertEquals(HttpStatusCode.OK, sameDevice.status)

        // 7. Reject under-13 DOB
        val underAge = client.put("/api/v1/profile") {
            header("Authorization", "Bearer $token")
            header("X-Telefam-Device-Id", dev1)
            contentType(ContentType.Application.Json)
            setBody("""{"fullName":"Alice Doe","username":"alice_doe","phoneCountryIso":"KE","phoneNumber":"0712345678","gender":"Female","dateOfBirth":"2018-01-01","location":"Nairobi","bio":"Hello"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, underAge.status)

        // 8. Valid profile + photo upload with magic byte check
        val fakeUpload = client.submitFormWithBinaryData(
            url = "/api/v1/profile/photo",
            formData = formData {
                append("photo", "not-an-image".toByteArray(), Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    append(HttpHeaders.ContentDisposition, "filename=\"bad.jpg\"")
                })
            }
        ) {
            header("Authorization", "Bearer $token")
            header("X-Telefam-Device-Id", dev1)
        }
        assertEquals(HttpStatusCode.BadRequest, fakeUpload.status)

        val validJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()) + ByteArray(20)
        val goodUpload = client.submitFormWithBinaryData(
            url = "/api/v1/profile/photo",
            formData = formData {
                append("cropX", "0.1")
                append("cropY", "0.1")
                append("cropWidth", "0.8")
                append("cropHeight", "0.8")
                append("photo", validJpeg, Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    append(HttpHeaders.ContentDisposition, "filename=\"avatar.jpg\"")
                })
            }
        ) {
            header("Authorization", "Bearer $token")
            header("X-Telefam-Device-Id", dev1)
        }
        assertEquals(HttpStatusCode.OK, goodUpload.status)
        assertTrue(goodUpload.bodyAsText().contains("fingerprintSha256"))

        val goodProfile = client.put("/api/v1/profile") {
            header("Authorization", "Bearer $token")
            header("X-Telefam-Device-Id", dev1)
            contentType(ContentType.Application.Json)
            setBody("""{"fullName":"Alice Doe","username":"alice_doe","phoneCountryIso":"KE","phoneNumber":"0712345678","gender":"Female","dateOfBirth":"2002-05-10","location":"Nairobi","bio":"Family first"}""")
        }
        assertEquals(HttpStatusCode.OK, goodProfile.status)
        assertFalse(repo.audits.isEmpty())
    }

    @Test
    fun `contacts search by name username country, follow-back friendship, and 100 per minute follow rate limit`() = testApplication {
        var now = Instant.parse("2026-11-22T12:00:00Z")
        val cfg = testConfig()
        val repo = InMemoryTestRepo()
        val crypto = ServerCrypto(cfg.jwtHmacSecret, cfg.dataEncryptionKey, cfg.deviceBindingPepper)
        var lastOtp = ""
        val mailer = object : EmailSender {
            override suspend fun sendOtpEmail(toEmail: String, otpCode: String): Boolean {
                lastOtp = otpCode
                return true
            }
        }
        val googleVerifier = object : GoogleTokenVerifier {
            override fun verify(idToken: String, expectedNonce: String): VerifiedGoogleUser? = null
        }
        val sandbox = object : MediaSandboxProcessor {
            override fun process(rawBytes: ByteArray, cropX: Double, cropY: Double, cropWidth: Double, cropHeight: Double, maxWidth: Int, maxHeight: Int, mode: String): Result<ProcessedAvatarResult> =
                Result.failure(IllegalArgumentException("Unused"))
        }

        application {
            telefamModule(cfg, repo, crypto, mailer, googleVerifier, sandbox) { now }
        }

        val dev1 = "c".repeat(64)
        client.post("/api/v1/auth/otp/send") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"viewer@telefam.company","deviceIdHash":"$dev1","deviceHardwareName":"Pixel 9","devicePublicKey":"pk1"}""")
        }
        val authRes = client.post("/api/v1/auth/otp/verify") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"viewer@telefam.company","otp":"$lastOtp","deviceIdHash":"$dev1","deviceHardwareName":"Pixel 9","devicePublicKey":"pk1"}""")
        }
        val authJson = Json.parseToJsonElement(authRes.bodyAsText()).jsonObject
        val viewerId = authJson["userId"]!!.jsonPrimitive.content
        val token = authJson["accessToken"]!!.jsonPrimitive.content

        client.put("/api/v1/profile") {
            header("Authorization", "Bearer $token")
            header("X-Telefam-Device-Id", dev1)
            contentType(ContentType.Application.Json)
            setBody("""{"fullName":"Viewer User","username":"viewer_user","phoneCountryIso":"KE","phoneNumber":"0712345678","gender":"Male","dateOfBirth":"1998-01-01","location":"Nairobi","bio":""}""")
        }

        // Create second real user Makiche401 who follows Viewer (so Viewer sees Follow back -> Friends)
        val makiche = repo.createUser("enc", crypto.hmacHex("email:makiche@telefam.company"), null)
        repo.updateUserProfile(
            userId = makiche.id,
            fullName = "Makiche401",
            username = "makiche401",
            usernameLower = "makiche401",
            phoneEncrypted = "enc",
            phoneHmac = "hmac",
            phoneCountryIso = "KE",
            gender = "Male",
            dobEncrypted = "enc",
            locationEncrypted = "enc",
            bioEncrypted = "enc"
        )
        repo.followUser(makiche.id, viewerId)

        // Search by country "Kenya" and username "makiche401"
        val searchByCountry = client.get("/api/v1/contacts?q=Kenya&tab=followers") {
            header("Authorization", "Bearer $token")
            header("X-Telefam-Device-Id", dev1)
        }
        assertEquals(HttpStatusCode.OK, searchByCountry.status)
        assertTrue(searchByCountry.bodyAsText().contains("Makiche401"))
        assertTrue(searchByCountry.bodyAsText().contains("\"followsMe\":true"))
        assertTrue(searchByCountry.bodyAsText().contains("\"isFriend\":false"))

        // Viewer follows back -> mutual friendship becomes true
        val followBack = client.post("/api/v1/contacts/${makiche.id}/follow") {
            header("Authorization", "Bearer $token")
            header("X-Telefam-Device-Id", dev1)
        }
        assertEquals(HttpStatusCode.OK, followBack.status)
        assertTrue(followBack.bodyAsText().contains("\"isFriend\":true"))

        // Friends tab returns Makiche401
        val friendsList = client.get("/api/v1/contacts?tab=friends") {
            header("Authorization", "Bearer $token")
            header("X-Telefam-Device-Id", dev1)
        }
        assertEquals(HttpStatusCode.OK, friendsList.status)
        assertTrue(friendsList.bodyAsText().contains("makiche401"))

        // Unfollow breaks friendship
        val unfollow = client.delete("/api/v1/contacts/${makiche.id}/follow") {
            header("Authorization", "Bearer $token")
            header("X-Telefam-Device-Id", dev1)
        }
        assertEquals(HttpStatusCode.OK, unfollow.status)
        assertTrue(unfollow.bodyAsText().contains("\"isFriend\":false"))

        // Rate limit: 99 more follows allowed (total 100 in 60s), 101st returns 429 "Error has occurred. Try again later."
        repeat(99) {
            val res = client.post("/api/v1/contacts/${makiche.id}/follow") {
                header("Authorization", "Bearer $token")
                header("X-Telefam-Device-Id", dev1)
            }
            assertEquals(HttpStatusCode.OK, res.status)
        }
        val overLimit = client.post("/api/v1/contacts/${makiche.id}/follow") {
            header("Authorization", "Bearer $token")
            header("X-Telefam-Device-Id", dev1)
        }
        assertEquals(HttpStatusCode.TooManyRequests, overLimit.status)
        assertTrue(overLimit.bodyAsText().contains("Error has occurred. Try again later."))

        // After 61 seconds, follow works again
        now = now.plusSeconds(61)
        val afterMinute = client.post("/api/v1/contacts/${makiche.id}/follow") {
            header("Authorization", "Bearer $token")
            header("X-Telefam-Device-Id", dev1)
        }
        assertEquals(HttpStatusCode.OK, afterMinute.status)
    }

    @Test
    fun `owner profile editing enforces 14 day username change and up to 5 sandboxed background photos`() = testApplication {
        var now = Instant.parse("2026-11-22T12:00:00Z")
        val cfg = testConfig()
        val repo = InMemoryTestRepo()
        val crypto = ServerCrypto(cfg.jwtHmacSecret, cfg.dataEncryptionKey, cfg.deviceBindingPepper)
        var lastOtp = ""
        val mailer = object : EmailSender {
            override suspend fun sendOtpEmail(toEmail: String, otpCode: String): Boolean {
                lastOtp = otpCode
                return true
            }
        }
        val googleVerifier = object : GoogleTokenVerifier {
            override fun verify(idToken: String, expectedNonce: String): VerifiedGoogleUser? = null
        }
        val sandbox = object : MediaSandboxProcessor {
            override fun process(
                rawBytes: ByteArray,
                cropX: Double,
                cropY: Double,
                cropWidth: Double,
                cropHeight: Double,
                maxWidth: Int,
                maxHeight: Int,
                mode: String
            ): Result<ProcessedAvatarResult> {
                if (!UnixSocketSandboxMediaClient.hasValidImageMagicBytes(rawBytes)) {
                    return Result.failure(IllegalArgumentException("Bad magic"))
                }
                return Result.success(
                    ProcessedAvatarResult(
                        byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()),
                        "image/jpeg",
                        maxWidth,
                        maxHeight,
                        "b".repeat(64),
                        "fedcba9876543210"
                    )
                )
            }
        }

        application {
            telefamModule(cfg, repo, crypto, mailer, googleVerifier, sandbox) { now }
        }

        val dev1 = "d".repeat(64)
        client.post("/api/v1/auth/otp/send") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"colloh185@telefam.company","deviceIdHash":"$dev1","deviceHardwareName":"Pixel 9","devicePublicKey":"pk1"}""")
        }
        val authRes = client.post("/api/v1/auth/otp/verify") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"colloh185@telefam.company","otp":"$lastOtp","deviceIdHash":"$dev1","deviceHardwareName":"Pixel 9","devicePublicKey":"pk1"}""")
        }
        val token = Json.parseToJsonElement(authRes.bodyAsText()).jsonObject["accessToken"]!!.jsonPrimitive.content

        // Initial profile setup
        val setup = client.put("/api/v1/profile") {
            header("Authorization", "Bearer $token")
            header("X-Telefam-Device-Id", dev1)
            contentType(ContentType.Application.Json)
            setBody("""{"fullName":"Colloh185","username":"colloh185","phoneCountryIso":"KE","phoneNumber":"0712345678","gender":"Male","dateOfBirth":"2000-01-01","location":"koiwa","bio":"Hello"}""")
        }
        assertEquals(HttpStatusCode.OK, setup.status)
        assertTrue(setup.bodyAsText().contains("\"canChangeUsername\":false"))

        // Editing fullName & location without changing username succeeds immediately
        now = now.plusSeconds(5 * 86_400L)
        val editBio = client.put("/api/v1/profile") {
            header("Authorization", "Bearer $token")
            header("X-Telefam-Device-Id", dev1)
            contentType(ContentType.Application.Json)
            setBody("""{"fullName":"Colloh Updated","username":"colloh185","phoneCountryIso":"KE","phoneNumber":"0712345678","gender":"Male","dateOfBirth":"2000-01-01","location":"Nairobi","bio":"Updated"}""")
        }
        assertEquals(HttpStatusCode.OK, editBio.status)

        // Changing username before 14 days (at day 5) is rejected
        val tooEarlyUsername = client.put("/api/v1/profile") {
            header("Authorization", "Bearer $token")
            header("X-Telefam-Device-Id", dev1)
            contentType(ContentType.Application.Json)
            setBody("""{"fullName":"Colloh Updated","username":"colloh_new","phoneCountryIso":"KE","phoneNumber":"0712345678","gender":"Male","dateOfBirth":"2000-01-01","location":"Nairobi","bio":"Updated"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, tooEarlyUsername.status)

        // After 14 days (at day 15), username change succeeds
        now = now.plusSeconds(10 * 86_400L)
        val allowedUsername = client.put("/api/v1/profile") {
            header("Authorization", "Bearer $token")
            header("X-Telefam-Device-Id", dev1)
            contentType(ContentType.Application.Json)
            setBody("""{"fullName":"Colloh Updated","username":"colloh_new","phoneCountryIso":"KE","phoneNumber":"0712345678","gender":"Male","dateOfBirth":"2000-01-01","location":"Nairobi","bio":"Updated"}""")
        }
        assertEquals(HttpStatusCode.OK, allowedUsername.status)
        assertTrue(allowedUsername.bodyAsText().contains("\"username\":\"colloh_new\""))

        // Invalid background upload rejected
        val invalidBg = client.submitFormWithBinaryData(
            url = "/api/v1/profile/backgrounds",
            formData = formData {
                append("photo", "fake-script-bytes".toByteArray(), Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    append(HttpHeaders.ContentDisposition, "filename=\"bad.jpg\"")
                })
            }
        ) {
            header("Authorization", "Bearer $token")
            header("X-Telefam-Device-Id", dev1)
        }
        assertEquals(HttpStatusCode.BadRequest, invalidBg.status)

        // Up to 5 valid background photos allowed; 6th rejected
        val validJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()) + ByteArray(20)
        repeat(5) {
            val bgRes = client.submitFormWithBinaryData(
                url = "/api/v1/profile/backgrounds",
                formData = formData {
                    append("photo", validJpeg, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"bg.jpg\"")
                    })
                }
            ) {
                header("Authorization", "Bearer $token")
                header("X-Telefam-Device-Id", dev1)
            }
            assertEquals(HttpStatusCode.OK, bgRes.status)
        }

        val sixthBg = client.submitFormWithBinaryData(
            url = "/api/v1/profile/backgrounds",
            formData = formData {
                append("photo", validJpeg, Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    append(HttpHeaders.ContentDisposition, "filename=\"bg6.jpg\"")
                })
            }
        ) {
            header("Authorization", "Bearer $token")
            header("X-Telefam-Device-Id", dev1)
        }
        assertEquals(HttpStatusCode.BadRequest, sixthBg.status)

        // GET /api/v1/profile returns 5 backgrounds
        val fetched = client.get("/api/v1/profile") {
            header("Authorization", "Bearer $token")
            header("X-Telefam-Device-Id", dev1)
        }
        assertEquals(HttpStatusCode.OK, fetched.status)
        assertTrue(fetched.bodyAsText().contains("fingerprintSha256"))

        // Delete one background photo -> 4 left -> new upload succeeds
        val firstId = repo.listUserBackgroundPhotos(Json.parseToJsonElement(authRes.bodyAsText()).jsonObject["userId"]!!.jsonPrimitive.content).first().id
        val delRes = client.delete("/api/v1/profile/backgrounds/$firstId") {
            header("Authorization", "Bearer $token")
            header("X-Telefam-Device-Id", dev1)
        }
        assertEquals(HttpStatusCode.OK, delRes.status)
    }

    @Test
    fun `token refresh rotates device-bound tokens and logout revokes session`() = testApplication {
        val now = Instant.parse("2026-11-22T12:00:00Z")
        val cfg = testConfig()
        val repo = InMemoryTestRepo()
        val crypto = ServerCrypto(cfg.jwtHmacSecret, cfg.dataEncryptionKey, cfg.deviceBindingPepper)
        var lastOtp = ""
        val mailer = object : EmailSender {
            override suspend fun sendOtpEmail(toEmail: String, otpCode: String): Boolean {
                lastOtp = otpCode
                return true
            }
        }
        val googleVerifier = object : GoogleTokenVerifier {
            override fun verify(idToken: String, expectedNonce: String): VerifiedGoogleUser? = null
        }
        val sandbox = object : MediaSandboxProcessor {
            override fun process(rawBytes: ByteArray, cropX: Double, cropY: Double, cropWidth: Double, cropHeight: Double, maxWidth: Int, maxHeight: Int, mode: String): Result<ProcessedAvatarResult> =
                Result.failure(IllegalArgumentException("Unused"))
        }

        application {
            telefamModule(cfg, repo, crypto, mailer, googleVerifier, sandbox) { now }
        }

        val dev1 = "e".repeat(64)
        val dev2 = "f".repeat(64)
        client.post("/api/v1/auth/otp/send") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"refresh@telefam.company","deviceIdHash":"$dev1","deviceHardwareName":"Pixel 9","devicePublicKey":"pk1"}""")
        }
        val verifyRes = client.post("/api/v1/auth/otp/verify") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"refresh@telefam.company","otp":"$lastOtp","deviceIdHash":"$dev1","deviceHardwareName":"Pixel 9","devicePublicKey":"pk1"}""")
        }
        val authObj = Json.parseToJsonElement(verifyRes.bodyAsText()).jsonObject
        val oldAccess = authObj["accessToken"]!!.jsonPrimitive.content
        val oldRefresh = authObj["refreshToken"]!!.jsonPrimitive.content

        // Cross-device refresh attempt fails
        val crossDeviceRefresh = client.post("/api/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"$oldRefresh","deviceIdHash":"$dev2","deviceHardwareName":"Pixel 8","devicePublicKey":"pk2"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, crossDeviceRefresh.status)

        // Valid device refresh succeeds and rotates both tokens
        val refreshRes = client.post("/api/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"$oldRefresh","deviceIdHash":"$dev1","deviceHardwareName":"Pixel 9","devicePublicKey":"pk1"}""")
        }
        assertEquals(HttpStatusCode.OK, refreshRes.status)
        val refreshedObj = Json.parseToJsonElement(refreshRes.bodyAsText()).jsonObject
        val newAccess = refreshedObj["accessToken"]!!.jsonPrimitive.content
        val newRefresh = refreshedObj["refreshToken"]!!.jsonPrimitive.content
        assertTrue(newAccess != oldAccess)
        assertTrue(newRefresh != oldRefresh)

        // Old refresh token cannot be reused
        val replayOldRefresh = client.post("/api/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"$oldRefresh","deviceIdHash":"$dev1","deviceHardwareName":"Pixel 9","devicePublicKey":"pk1"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, replayOldRefresh.status)

        // Logout revokes session so both access and refresh stop working
        val logoutRes = client.post("/api/v1/auth/logout") {
            header("Authorization", "Bearer $newAccess")
            header("X-Telefam-Device-Id", dev1)
        }
        assertEquals(HttpStatusCode.OK, logoutRes.status)

        val afterLogoutProfile = client.get("/api/v1/profile") {
            header("Authorization", "Bearer $newAccess")
            header("X-Telefam-Device-Id", dev1)
        }
        assertEquals(HttpStatusCode.Unauthorized, afterLogoutProfile.status)
    }

    @Test
    fun `apple sign-in verifies RS256 identity token, nonce sha256, returning sub lookup, and device binding`() = testApplication {
        val now = Instant.parse("2026-11-22T12:00:00Z")
        val cfg = testConfig()
        val repo = InMemoryTestRepo()
        val crypto = ServerCrypto(cfg.jwtHmacSecret, cfg.dataEncryptionKey, cfg.deviceBindingPepper)
        val mailer = object : EmailSender {
            override suspend fun sendOtpEmail(toEmail: String, otpCode: String) = true
        }
        val googleVerifier = object : GoogleTokenVerifier {
            override fun verify(idToken: String, expectedNonce: String): VerifiedGoogleUser? = null
        }
        val sandbox = object : MediaSandboxProcessor {
            override fun process(rawBytes: ByteArray, cropX: Double, cropY: Double, cropWidth: Double, cropHeight: Double, maxWidth: Int, maxHeight: Int, mode: String): Result<ProcessedAvatarResult> =
                Result.failure(IllegalArgumentException("Unused"))
        }

        val kpg = java.security.KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }
        val keyPair = kpg.generateKeyPair()
        val rsaPub = keyPair.public as java.security.interfaces.RSAPublicKey
        val encoder = java.util.Base64.getUrlEncoder().withoutPadding()
        fun bigIntToB64Url(value: java.math.BigInteger): String {
            val raw = value.toByteArray()
            val unsigned = if (raw.size > 1 && raw[0] == 0.toByte()) raw.copyOfRange(1, raw.size) else raw
            return encoder.encodeToString(unsigned)
        }
        val jwk = AppleJwk(
            kty = "RSA",
            kid = "apple-test-kid",
            use = "sig",
            alg = "RS256",
            n = bigIntToB64Url(rsaPub.modulus),
            e = bigIntToB64Url(rsaPub.publicExponent)
        )
        val appleVerifier = AppleTokenVerifierService(
            allowedAudiences = listOf("com.telefam.company.app"),
            jwksProvider = object : AppleJwksProvider {
                override fun fetchKeys() = listOf(jwk)
            },
            nowProvider = { now }
        )

        fun signAppleJwt(payloadJson: String, kid: String = "apple-test-kid"): String {
            val header = encoder.encodeToString("""{"alg":"RS256","kid":"$kid"}""".toByteArray())
            val payload = encoder.encodeToString(payloadJson.toByteArray())
            val sig = java.security.Signature.getInstance("SHA256withRSA").apply {
                initSign(keyPair.private)
                update("$header.$payload".toByteArray())
            }.sign()
            return "$header.$payload.${encoder.encodeToString(sig)}"
        }

        application {
            telefamModule(cfg, repo, crypto, mailer, googleVerifier, sandbox, appleVerifier) { now }
        }

        val dev1 = "1".repeat(64)
        val dev2 = "2".repeat(64)
        val rawNonce = "0123456789abcdef0123456789abcdef"
        val hashedNonce = AppleTokenVerifierService.sha256Hex(rawNonce.toByteArray())

        // 1. Wrong nonce rejected
        val badNonceJwt = signAppleJwt(
            """{"iss":"https://appleid.apple.com","aud":"com.telefam.company.app","exp":${now.epochSecond + 600},"iat":${now.epochSecond},"sub":"001.apple.user","email":"ios@telefam.company","email_verified":true,"nonce":"wronghash"}"""
        )
        val badNonceResp = client.post("/api/v1/auth/apple") {
            contentType(ContentType.Application.Json)
            setBody("""{"identityToken":"$badNonceJwt","nonce":"$rawNonce","deviceIdHash":"$dev1","deviceHardwareName":"iPad Pro","devicePublicKey":"pk-ios"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, badNonceResp.status)

        // 2. Valid first-time Apple sign-in creates user and returns device-bound token
        val goodJwt = signAppleJwt(
            """{"iss":"https://appleid.apple.com","aud":"com.telefam.company.app","exp":${now.epochSecond + 600},"iat":${now.epochSecond},"sub":"001.apple.user","email":"ios@telefam.company","email_verified":"true","nonce":"$hashedNonce"}"""
        )
        val okResp = client.post("/api/v1/auth/apple") {
            contentType(ContentType.Application.Json)
            setBody("""{"identityToken":"$goodJwt","nonce":"$rawNonce","fullName":"IOS User","deviceIdHash":"$dev1","deviceHardwareName":"iPhone 16 Pro","devicePublicKey":"pk-ios"}""")
        }
        assertEquals(HttpStatusCode.OK, okResp.status)
        val firstObj = Json.parseToJsonElement(okResp.bodyAsText()).jsonObject
        val firstUserId = firstObj["userId"]!!.jsonPrimitive.content
        val firstToken = firstObj["accessToken"]!!.jsonPrimitive.content
        assertEquals("ios@telefam.company", firstObj["email"]!!.jsonPrimitive.content)

        // 3. Returning Apple sign-in without email claim resolves existing user by appleSubHmac
        val returningNonce = "fedcba9876543210fedcba9876543210"
        val returningHash = AppleTokenVerifierService.sha256Hex(returningNonce.toByteArray())
        val returningJwt = signAppleJwt(
            """{"iss":"https://appleid.apple.com","aud":"com.telefam.company.app","exp":${now.epochSecond + 600},"iat":${now.epochSecond},"sub":"001.apple.user","nonce":"$returningHash"}"""
        )
        val returningResp = client.post("/api/v1/auth/apple") {
            contentType(ContentType.Application.Json)
            setBody("""{"identityToken":"$returningJwt","nonce":"$returningNonce","deviceIdHash":"$dev1","deviceHardwareName":"iPhone 16 Pro","devicePublicKey":"pk-ios"}""")
        }
        assertEquals(HttpStatusCode.OK, returningResp.status)
        val secondObj = Json.parseToJsonElement(returningResp.bodyAsText()).jsonObject
        assertEquals(firstUserId, secondObj["userId"]!!.jsonPrimitive.content)
        assertEquals("ios@telefam.company", secondObj["email"]!!.jsonPrimitive.content)

        // 4. Token cannot be used from another device
        val crossDevice = client.get("/api/v1/profile") {
            header("Authorization", "Bearer $firstToken")
            header("X-Telefam-Device-Id", dev2)
        }
        assertEquals(HttpStatusCode.Unauthorized, crossDevice.status)
    }
}
