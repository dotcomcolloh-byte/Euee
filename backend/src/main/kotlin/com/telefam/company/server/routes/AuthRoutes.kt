package com.telefam.company.server.routes

import com.telefam.company.server.auth.AppleTokenVerifier
import com.telefam.company.server.auth.GoogleTokenVerifier
import com.telefam.company.server.config.ServerConfig
import com.telefam.company.server.crypto.JwtClaims
import com.telefam.company.server.crypto.ServerCrypto
import com.telefam.company.server.db.AuditLogEntry
import com.telefam.company.server.db.BackgroundPhotoRecord
import com.telefam.company.server.db.ProfileStatsRecord
import com.telefam.company.server.db.TelefamRepository
import com.telefam.company.server.db.UserRecord
import com.telefam.company.server.email.EmailSender
import com.telefam.company.server.model.AppleAuthRequest
import com.telefam.company.server.model.AuthResponse
import com.telefam.company.server.model.BackgroundPhotoDto
import com.telefam.company.server.model.ErrorResponse
import com.telefam.company.server.model.GoogleAuthRequest
import com.telefam.company.server.model.OtpLockResponse
import com.telefam.company.server.model.OtpSendResponse
import com.telefam.company.server.model.ProfileDto
import com.telefam.company.server.model.RefreshTokenRequest
import com.telefam.company.server.model.SendOtpRequest
import com.telefam.company.server.model.VerifyOtpRequest
import com.telefam.company.server.validation.Validators
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import java.time.Instant
import java.util.UUID

fun Route.authRoutes(
    config: ServerConfig,
    repo: TelefamRepository,
    crypto: ServerCrypto,
    emailSender: EmailSender,
    googleVerifier: GoogleTokenVerifier,
    appleVerifier: AppleTokenVerifier,
    nowProvider: () -> Instant = { Instant.now() }
) {
    post("/api/v1/auth/otp/send") {
        val req = call.receive<SendOtpRequest>()
        val email = req.email.trim().lowercase()
        val ip = call.clientIp()
        val imeiHash = crypto.hmacHex("imei:${req.deviceIdHash}:${req.devicePublicKey}")

        if (!Validators.isValidEmail(email) || req.deviceIdHash.length < 32) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid request"))
            return@post
        }

        val emailHmac = crypto.hmacHex("email:$email")
        val now = nowProvider()
        val existing = repo.findOtpChallenge(emailHmac, req.deviceIdHash)

        if (existing?.lockedUntil != null && existing.lockedUntil.isAfter(now)) {
            val remSec = (existing.lockedUntil.epochSecond - now.epochSecond).coerceAtLeast(1).toInt()
            repo.appendSecurityAuditLog(
                AuditLogEntry(null, "OTP_SEND_BLOCKED_LOCKED", "WARN", req.deviceHardwareName.take(128), imeiHash, ip, req.deviceIdHash, false, """{"remSec":$remSec}""")
            )
            call.respond(
                HttpStatusCode.TooManyRequests,
                OtpLockResponse("Too many attempts", existing.failedAttempts, existing.lockCycles, lockRemainingSeconds = remSec)
            )
            return@post
        }

        if (existing != null && !existing.consumed && existing.resendAvailableAt.isAfter(now)) {
            val remSec = (existing.resendAvailableAt.epochSecond - now.epochSecond).coerceAtLeast(1).toInt()
            call.respond(
                HttpStatusCode.TooManyRequests,
                OtpLockResponse("Resend cooldown active", existing.failedAttempts, existing.lockCycles, resendRemainingSeconds = remSec)
            )
            return@post
        }

        val otp = crypto.generateSixDigitOtp()
        val otpHmac = crypto.hmacHex("otp:$emailHmac:${req.deviceIdHash}:$otp")
        val sent = emailSender.sendOtpEmail(email, otp)
        if (!sent) {
            repo.appendSecurityAuditLog(
                AuditLogEntry(null, "OTP_EMAIL_FAILED", "ERROR", req.deviceHardwareName.take(128), imeiHash, ip, req.deviceIdHash, false, "{}")
            )
            call.respond(HttpStatusCode.ServiceUnavailable, ErrorResponse("Error has occurred"))
            return@post
        }

        repo.upsertOtpChallenge(
            emailHmac = emailHmac,
            deviceIdHash = req.deviceIdHash,
            otpHmac = otpHmac,
            expiresAt = now.plusSeconds(300),
            resendAvailableAt = now.plusSeconds(60),
            failedAttempts = existing?.failedAttempts ?: 0,
            lockCycles = existing?.lockCycles ?: 0,
            lockedUntil = null,
            consumed = false
        )

        repo.appendSecurityAuditLog(
            AuditLogEntry(null, "OTP_SENT", "INFO", req.deviceHardwareName.take(128), imeiHash, ip, req.deviceIdHash, true, "{}")
        )
        call.respond(HttpStatusCode.OK, OtpSendResponse("OTP sent", resendCooldownSeconds = 60))
    }

    post("/api/v1/auth/otp/verify") {
        val req = call.receive<VerifyOtpRequest>()
        val email = req.email.trim().lowercase()
        val ip = call.clientIp()
        val imeiHash = crypto.hmacHex("imei:${req.deviceIdHash}:${req.devicePublicKey}")
        val now = nowProvider()

        if (!Validators.isValidEmail(email) || req.otp.length != 6 || req.deviceIdHash.length < 32) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid OTP format"))
            return@post
        }

        val emailHmac = crypto.hmacHex("email:$email")
        val challenge = repo.findOtpChallenge(emailHmac, req.deviceIdHash)
        if (challenge == null || challenge.consumed) {
            call.respond(HttpStatusCode.Unauthorized, OtpLockResponse("Request a new OTP code"))
            return@post
        }

        if (challenge.lockedUntil != null && challenge.lockedUntil.isAfter(now)) {
            val remSec = (challenge.lockedUntil.epochSecond - now.epochSecond).coerceAtLeast(1).toInt()
            call.respond(
                HttpStatusCode.TooManyRequests,
                OtpLockResponse("Locked", challenge.failedAttempts, challenge.lockCycles, lockRemainingSeconds = remSec)
            )
            return@post
        }

        if (challenge.expiresAt.isBefore(now)) {
            call.respond(HttpStatusCode.Unauthorized, OtpLockResponse("OTP expired. Request a new code"))
            return@post
        }

        val candidateHmac = crypto.hmacHex("otp:$emailHmac:${req.deviceIdHash}:${req.otp}")
        if (!crypto.constantTimeEquals(candidateHmac, challenge.otpHmac)) {
            val nextFailures = challenge.failedAttempts + 1
            if (nextFailures >= 3) {
                val nextCycles = challenge.lockCycles + 1
                // 60 seconds 3 times, then 12 minutes (720 seconds)
                val lockDurationSec = if (nextCycles <= 3) 60L else 720L
                val lockedUntil = now.plusSeconds(lockDurationSec)
                repo.updateOtpFailureState(emailHmac, req.deviceIdHash, 0, nextCycles, lockedUntil)
                repo.appendSecurityAuditLog(
                    AuditLogEntry(null, "OTP_LOCKED", "WARN", req.deviceHardwareName.take(128), imeiHash, ip, req.deviceIdHash, false, """{"cycles":$nextCycles,"sec":$lockDurationSec}""")
                )
                call.respond(
                    HttpStatusCode.TooManyRequests,
                    OtpLockResponse("Too many wrong attempts", 0, nextCycles, lockRemainingSeconds = lockDurationSec.toInt())
                )
            } else {
                repo.updateOtpFailureState(emailHmac, req.deviceIdHash, nextFailures, challenge.lockCycles, null)
                repo.appendSecurityAuditLog(
                    AuditLogEntry(null, "OTP_INVALID", "WARN", req.deviceHardwareName.take(128), imeiHash, ip, req.deviceIdHash, false, """{"attempts":$nextFailures}""")
                )
                call.respond(
                    HttpStatusCode.Unauthorized,
                    OtpLockResponse("Invalid OTP code", nextFailures, challenge.lockCycles)
                )
            }
            return@post
        }

        repo.markOtpConsumed(emailHmac, req.deviceIdHash)
        val user = repo.findUserByEmailHmac(emailHmac)
            ?: repo.createUser(crypto.encryptAtRest(email, emailHmac), emailHmac, null)

        val authResp = issueDeviceBoundSession(
            config, repo, crypto, user, email, req.deviceIdHash,
            req.deviceHardwareName, imeiHash, req.devicePublicKey, ip, "OTP_LOGIN_SUCCESS", now
        )
        call.respond(HttpStatusCode.OK, authResp)
    }

    post("/api/v1/auth/google") {
        val req = call.receive<GoogleAuthRequest>()
        val ip = call.clientIp()
        val imeiHash = crypto.hmacHex("imei:${req.deviceIdHash}:${req.devicePublicKey}")
        val now = nowProvider()

        val verified = googleVerifier.verify(req.idToken, req.nonce)
        if (verified == null) {
            repo.appendSecurityAuditLog(
                AuditLogEntry(null, "GOOGLE_AUTH_FAILED", "WARN", req.deviceHardwareName.take(128), imeiHash, ip, req.deviceIdHash, false, "{}")
            )
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Google authentication failed"))
            return@post
        }

        val emailHmac = crypto.hmacHex("email:${verified.email}")
        val googleSubHmac = crypto.hmacHex("gsub:${verified.subject}")
        val user = repo.findUserByEmailHmac(emailHmac)
            ?: repo.createUser(crypto.encryptAtRest(verified.email, emailHmac), emailHmac, googleSubHmac)

        val authResp = issueDeviceBoundSession(
            config, repo, crypto, user, verified.email, req.deviceIdHash,
            req.deviceHardwareName, imeiHash, req.devicePublicKey, ip, "GOOGLE_LOGIN_SUCCESS", now
        )
        call.respond(HttpStatusCode.OK, authResp)
    }

    post("/api/v1/auth/apple") {
        val req = call.receive<AppleAuthRequest>()
        val ip = call.clientIp()
        val imeiHash = crypto.hmacHex("imei:${req.deviceIdHash}:${req.devicePublicKey}")
        val now = nowProvider()

        if (req.deviceIdHash.length < 32 || req.identityToken.isBlank() || req.nonce.length < 16) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid request"))
            return@post
        }

        val verified = appleVerifier.verify(req.identityToken, req.nonce, req.fullName)
        if (verified == null) {
            repo.appendSecurityAuditLog(
                AuditLogEntry(null, "APPLE_AUTH_FAILED", "WARN", req.deviceHardwareName.take(128), imeiHash, ip, req.deviceIdHash, false, "{}")
            )
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Apple authentication failed"))
            return@post
        }

        val appleSubHmac = crypto.hmacHex("asub:${verified.subject}")
        var user = repo.findUserByAppleSubHmac(appleSubHmac)
        var resolvedEmail = verified.email

        if (user == null) {
            if (resolvedEmail.isBlank()) {
                repo.appendSecurityAuditLog(
                    AuditLogEntry(null, "APPLE_AUTH_MISSING_EMAIL", "WARN", req.deviceHardwareName.take(128), imeiHash, ip, req.deviceIdHash, false, "{}")
                )
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Apple authentication failed"))
                return@post
            }
            val emailHmac = crypto.hmacHex("email:$resolvedEmail")
            val byEmail = repo.findUserByEmailHmac(emailHmac)
            user = if (byEmail != null) {
                repo.linkAppleSub(byEmail.id, appleSubHmac)
                byEmail.copy(appleSubHmac = appleSubHmac)
            } else {
                repo.createUser(
                    emailEncrypted = crypto.encryptAtRest(resolvedEmail, emailHmac),
                    emailHmac = emailHmac,
                    googleSubHmac = null,
                    appleSubHmac = appleSubHmac
                )
            }
        } else if (resolvedEmail.isBlank()) {
            resolvedEmail = runCatching { crypto.decryptAtRest(user.emailEncrypted, user.emailHmac) }.getOrDefault("")
        }

        val authResp = issueDeviceBoundSession(
            config, repo, crypto, user, resolvedEmail, req.deviceIdHash,
            req.deviceHardwareName, imeiHash, req.devicePublicKey, ip, "APPLE_LOGIN_SUCCESS", now
        )
        call.respond(HttpStatusCode.OK, authResp)
    }

    post("/api/v1/auth/refresh") {
        val req = call.receive<RefreshTokenRequest>()
        val ip = call.clientIp()
        val now = nowProvider()
        val imeiHash = crypto.hmacHex("imei:${req.deviceIdHash}:${req.devicePublicKey}")
        val hw = req.deviceHardwareName.take(128)

        if (req.refreshToken.isBlank() || req.deviceIdHash.length < 32) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid request"))
            return@post
        }

        val refreshHmac = crypto.hmacHex("rt:${req.refreshToken}")
        val session = repo.findSessionByRefreshHmac(refreshHmac, req.deviceIdHash)
        if (session == null || session.revoked || session.expiresAt.isBefore(now)) {
            repo.appendSecurityAuditLog(
                AuditLogEntry(null, "TOKEN_REFRESH_DENIED", "WARN", hw, imeiHash, ip, req.deviceIdHash, false, "{}")
            )
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Unauthorized"))
            return@post
        }

        val user = repo.findUserById(session.userId)
        if (user == null) {
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Unauthorized"))
            return@post
        }

        val newAccessToken = crypto.issueDeviceBoundJwt(user.id, req.deviceIdHash, session.id, ttlSeconds = 86_400L)
        val newRefreshToken = crypto.generateOpaqueToken()
        repo.rotateSessionTokens(
            sessionId = session.id,
            userId = user.id,
            newAccessTokenHmac = crypto.hmacHex("at:$newAccessToken"),
            newRefreshTokenEncrypted = crypto.encryptAtRest(newRefreshToken, "${user.id}:${req.deviceIdHash}"),
            newRefreshTokenHmac = crypto.hmacHex("rt:$newRefreshToken"),
            newExpiresAt = now.plusSeconds(86_400L)
        )

        repo.appendSecurityAuditLog(
            AuditLogEntry(user.id, "TOKEN_REFRESHED", "INFO", hw, imeiHash, ip, req.deviceIdHash, true, """{"sessionId":"${session.id}"}""")
        )

        val email = runCatching { crypto.decryptAtRest(user.emailEncrypted, user.emailHmac) }.getOrDefault("")
        val stats = repo.getProfileStats(user.id)
        val backgrounds = repo.listUserBackgroundPhotos(user.id)
        call.respond(
            HttpStatusCode.OK,
            AuthResponse(
                userId = user.id,
                email = email,
                accessToken = newAccessToken,
                refreshToken = newRefreshToken,
                profileCompleted = user.profileCompleted,
                profile = user.toProfileDto(config, crypto, stats, backgrounds, now)
            )
        )
    }

    post("/api/v1/auth/logout") {
        val claims = call.authenticateDeviceBound(repo, crypto) ?: return@post
        repo.revokeSession(claims.sessionId, claims.userId)
        val devId = call.request.headers["X-Telefam-Device-Id"].orEmpty()
        val hw = call.request.headers["X-Telefam-Hardware"].orEmpty().take(128)
        repo.appendSecurityAuditLog(
            AuditLogEntry(claims.userId, "LOGOUT", "INFO", hw, devId, call.clientIp(), devId, true, "{}")
        )
        call.respond(HttpStatusCode.OK, mapOf("ok" to true))
    }
}

private fun issueDeviceBoundSession(
    config: ServerConfig,
    repo: TelefamRepository,
    crypto: ServerCrypto,
    user: UserRecord,
    email: String,
    deviceIdHash: String,
    hardwareName: String,
    imeiHash: String,
    publicKeySpki: String,
    ip: String,
    eventType: String,
    now: Instant
): AuthResponse {
    repo.upsertUserDevice(user.id, deviceIdHash, hardwareName.take(128), imeiHash, publicKeySpki, ip)

    val sessionId = UUID.randomUUID().toString()
    val accessToken = crypto.issueDeviceBoundJwt(user.id, deviceIdHash, sessionId, ttlSeconds = 86_400L)
    val refreshToken = crypto.generateOpaqueToken()

    repo.createDeviceSession(
        sessionId = sessionId,
        userId = user.id,
        deviceIdHash = deviceIdHash,
        accessTokenHmac = crypto.hmacHex("at:$accessToken"),
        refreshTokenEncrypted = crypto.encryptAtRest(refreshToken, "${user.id}:$deviceIdHash"),
        refreshTokenHmac = crypto.hmacHex("rt:$refreshToken"),
        expiresAt = now.plusSeconds(86_400L)
    )

    repo.appendSecurityAuditLog(
        AuditLogEntry(user.id, eventType, "INFO", hardwareName.take(128), imeiHash, ip, deviceIdHash, true, """{"sessionId":"$sessionId"}""")
    )

    val stats = repo.getProfileStats(user.id)
    val backgrounds = repo.listUserBackgroundPhotos(user.id)
    return AuthResponse(
        userId = user.id,
        email = email,
        accessToken = accessToken,
        refreshToken = refreshToken,
        profileCompleted = user.profileCompleted,
        profile = user.toProfileDto(config, crypto, stats, backgrounds, now)
    )
}

private const val USERNAME_COOLDOWN_SECONDS = 14L * 86_400L

fun BackgroundPhotoRecord.toBackgroundDto(config: ServerConfig) = BackgroundPhotoDto(
    id = id,
    imageUrl = "${config.publicBaseUrl}/api/v1/media/background/$objectKey",
    fingerprintSha256 = sha256Hex,
    fingerprintDhash = dhashHex,
    width = width,
    height = height
)

fun UserRecord.toProfileDto(
    config: ServerConfig,
    crypto: ServerCrypto,
    stats: ProfileStatsRecord = ProfileStatsRecord(0, 0, 0, 0),
    backgrounds: List<BackgroundPhotoRecord> = emptyList(),
    now: Instant = Instant.now()
): ProfileDto {
    val avatarUrl = avatarObjectKey?.let { "${config.publicBaseUrl}/api/v1/media/avatar/$it" }.orEmpty()
    val changedAtSec = usernameChangedAt?.epochSecond ?: 0L
    val nextChangeSec = if (usernameChangedAt != null) usernameChangedAt.epochSecond + USERNAME_COOLDOWN_SECONDS else 0L
    val canChange = usernameChangedAt == null || !now.isBefore(usernameChangedAt.plusSeconds(USERNAME_COOLDOWN_SECONDS))
    return ProfileDto(
        fullName = fullName,
        username = username.orEmpty(),
        phoneCountryIso = phoneCountryIso,
        phoneNumber = phoneEncrypted?.let { runCatching { crypto.decryptAtRest(it, id) }.getOrDefault("") }.orEmpty(),
        gender = gender.orEmpty(),
        dateOfBirth = dobEncrypted?.let { runCatching { crypto.decryptAtRest(it, id) }.getOrDefault("") }.orEmpty(),
        location = locationEncrypted?.let { runCatching { crypto.decryptAtRest(it, id) }.getOrDefault("") }.orEmpty(),
        bio = bioEncrypted?.let { runCatching { crypto.decryptAtRest(it, id) }.getOrDefault("") }.orEmpty(),
        avatarUrl = avatarUrl,
        avatarFingerprintSha256 = avatarFingerprintSha256.orEmpty(),
        usernameChangedAtEpochSec = changedAtSec,
        nextUsernameChangeAtEpochSec = nextChangeSec,
        canChangeUsername = canChange,
        followersCount = stats.followersCount,
        followingCount = stats.followingCount,
        likesCount = stats.likesCount,
        postsCount = stats.postsCount,
        backgroundPhotos = backgrounds.map { it.toBackgroundDto(config) }
    )
}

fun ApplicationCall.clientIp(): String {
    val forwarded = request.headers["X-Forwarded-For"]?.split(",")?.firstOrNull()?.trim()
    return if (!forwarded.isNullOrEmpty()) forwarded.take(64) else request.origin.remoteHost.take(64)
}

suspend fun ApplicationCall.authenticateDeviceBound(
    repo: TelefamRepository,
    crypto: ServerCrypto
): JwtClaims? {
    val auth = request.headers["Authorization"].orEmpty()
    val deviceId = request.headers["X-Telefam-Device-Id"].orEmpty()
    if (!auth.startsWith("Bearer ") || deviceId.length < 32) {
        respond(HttpStatusCode.Unauthorized, ErrorResponse("Unauthorized"))
        return null
    }
    val token = auth.removePrefix("Bearer ").trim()
    val claims = crypto.verifyDeviceBoundJwt(token, deviceId)
    if (claims == null) {
        repo.appendSecurityAuditLog(
            AuditLogEntry(null, "TOKEN_DEVICE_MISMATCH_OR_INVALID", "WARN", request.headers["X-Telefam-Hardware"].orEmpty().take(128), deviceId, clientIp(), deviceId, false, "{}")
        )
        respond(HttpStatusCode.Unauthorized, ErrorResponse("Unauthorized"))
        return null
    }
    val session = repo.findActiveSession(claims.sessionId, claims.userId, deviceId)
    if (session == null || !crypto.constantTimeEquals(session.accessTokenHmac, crypto.hmacHex("at:$token"))) {
        respond(HttpStatusCode.Unauthorized, ErrorResponse("Unauthorized"))
        return null
    }
    return claims
}
