package com.telefam.company.server.db

import java.time.Instant

data class UserRecord(
    val id: String,
    val emailEncrypted: String,
    val emailHmac: String,
    val googleSubHmac: String?,
    val fullName: String,
    val username: String?,
    val usernameLower: String?,
    val phoneEncrypted: String?,
    val phoneCountryIso: String,
    val gender: String?,
    val dobEncrypted: String?,
    val locationEncrypted: String?,
    val bioEncrypted: String?,
    val avatarObjectKey: String?,
    val avatarFingerprintSha256: String?,
    val avatarFingerprintDhash: String?,
    val profileCompleted: Boolean,
    val usernameChangedAt: Instant? = null,
    val appleSubHmac: String? = null
)

data class ProfileStatsRecord(
    val followersCount: Int,
    val followingCount: Int,
    val likesCount: Int,
    val postsCount: Int
)

data class BackgroundPhotoRecord(
    val id: String,
    val userId: String,
    val objectKey: String,
    val sha256Hex: String,
    val dhashHex: String,
    val mimeType: String,
    val byteSize: Int,
    val width: Int,
    val height: Int,
    val displayOrder: Int
)

data class OtpChallengeRecord(
    val id: String,
    val emailHmac: String,
    val deviceIdHash: String,
    val otpHmac: String,
    val expiresAt: Instant,
    val resendAvailableAt: Instant,
    val failedAttempts: Int,
    val lockCycles: Int,
    val lockedUntil: Instant?,
    val consumed: Boolean
)

data class DeviceSessionRecord(
    val id: String,
    val userId: String,
    val deviceIdHash: String,
    val accessTokenHmac: String,
    val expiresAt: Instant,
    val revoked: Boolean
)

data class AuditLogEntry(
    val userId: String?,
    val eventType: String,
    val severity: String,
    val deviceHardwareName: String,
    val imeiOrDeviceHash: String,
    val ipAddress: String,
    val deviceIdHash: String,
    val success: Boolean,
    val detailsJson: String
)

interface TelefamRepository {
    fun findOtpChallenge(emailHmac: String, deviceIdHash: String): OtpChallengeRecord?
    fun upsertOtpChallenge(
        emailHmac: String,
        deviceIdHash: String,
        otpHmac: String,
        expiresAt: Instant,
        resendAvailableAt: Instant,
        failedAttempts: Int,
        lockCycles: Int,
        lockedUntil: Instant?,
        consumed: Boolean
    )

    fun updateOtpFailureState(
        emailHmac: String,
        deviceIdHash: String,
        failedAttempts: Int,
        lockCycles: Int,
        lockedUntil: Instant?
    )

    fun markOtpConsumed(emailHmac: String, deviceIdHash: String)

    fun findUserByEmailHmac(emailHmac: String): UserRecord?
    fun findUserByAppleSubHmac(appleSubHmac: String): UserRecord?
    fun findUserById(userId: String): UserRecord?
    fun createUser(
        emailEncrypted: String,
        emailHmac: String,
        googleSubHmac: String?,
        appleSubHmac: String? = null
    ): UserRecord
    fun linkAppleSub(userId: String, appleSubHmac: String)
    fun isUsernameAvailable(usernameLower: String, excludeUserId: String): Boolean

    fun upsertUserDevice(
        userId: String,
        deviceIdHash: String,
        hardwareName: String,
        imeiAttestationHash: String,
        publicKeySpki: String,
        ipAddress: String
    )

    fun createDeviceSession(
        sessionId: String,
        userId: String,
        deviceIdHash: String,
        accessTokenHmac: String,
        refreshTokenEncrypted: String,
        refreshTokenHmac: String,
        expiresAt: Instant
    )

    fun findActiveSession(sessionId: String, userId: String, deviceIdHash: String): DeviceSessionRecord?
    fun findSessionByRefreshHmac(refreshTokenHmac: String, deviceIdHash: String): DeviceSessionRecord?
    fun rotateSessionTokens(
        sessionId: String,
        userId: String,
        newAccessTokenHmac: String,
        newRefreshTokenEncrypted: String,
        newRefreshTokenHmac: String,
        newExpiresAt: Instant
    )
    fun revokeSession(sessionId: String, userId: String)

    fun updateUserAvatar(
        userId: String,
        objectKey: String,
        sha256Hex: String,
        dhashHex: String,
        mimeType: String,
        byteSize: Int,
        width: Int,
        height: Int
    )

    fun updateUserProfile(
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
        usernameChangedAt: Instant? = null
    ): UserRecord

    fun getProfileStats(userId: String): ProfileStatsRecord
    fun listUserBackgroundPhotos(userId: String): List<BackgroundPhotoRecord>
    fun countUserBackgroundPhotos(userId: String): Int
    fun addUserBackgroundPhoto(
        userId: String,
        objectKey: String,
        sha256Hex: String,
        dhashHex: String,
        mimeType: String,
        byteSize: Int,
        width: Int,
        height: Int
    ): BackgroundPhotoRecord
    fun deleteUserBackgroundPhoto(userId: String, photoId: String): String?

    fun appendSecurityAuditLog(entry: AuditLogEntry)

    fun listDirectoryContacts(viewerUserId: String): List<ContactDirectoryRecord>
    fun countRecentFollows(userId: String, since: Instant): Int
    fun recordFollowRateEvent(userId: String, at: Instant)
    fun followUser(followerId: String, targetUserId: String): FollowEdgeResult?
    fun unfollowUser(followerId: String, targetUserId: String): FollowEdgeResult?
}

data class ContactDirectoryRecord(
    val userId: String,
    val fullName: String,
    val username: String,
    val phoneCountryIso: String,
    val avatarObjectKey: String?,
    val iFollow: Boolean,
    val followsMe: Boolean
)

data class FollowEdgeResult(
    val targetUserId: String,
    val iFollow: Boolean,
    val followsMe: Boolean
)
