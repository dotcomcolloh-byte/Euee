package com.telefam.company.server.model

import kotlinx.serialization.Serializable

@Serializable
data class SendOtpRequest(
    val email: String,
    val deviceIdHash: String,
    val deviceHardwareName: String,
    val devicePublicKey: String
)

@Serializable
data class VerifyOtpRequest(
    val email: String,
    val otp: String,
    val deviceIdHash: String,
    val deviceHardwareName: String,
    val devicePublicKey: String
)

@Serializable
data class GoogleAuthRequest(
    val idToken: String,
    val nonce: String,
    val deviceIdHash: String,
    val deviceHardwareName: String,
    val devicePublicKey: String
)

@Serializable
data class AppleAuthRequest(
    val identityToken: String,
    val authorizationCode: String = "",
    val nonce: String,
    val fullName: String = "",
    val emailHint: String = "",
    val deviceIdHash: String,
    val deviceHardwareName: String,
    val devicePublicKey: String
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String,
    val deviceIdHash: String,
    val deviceHardwareName: String = "",
    val devicePublicKey: String = ""
)

@Serializable
data class ProfileUpdateRequest(
    val fullName: String,
    val username: String,
    val phoneCountryIso: String,
    val phoneNumber: String,
    val gender: String,
    val dateOfBirth: String,
    val location: String,
    val bio: String = ""
)

@Serializable
data class BackgroundPhotoDto(
    val id: String,
    val imageUrl: String,
    val fingerprintSha256: String,
    val fingerprintDhash: String,
    val width: Int,
    val height: Int
)

@Serializable
data class BackgroundPhotosResponse(
    val backgroundPhotos: List<BackgroundPhotoDto>
)

@Serializable
data class ProfileDto(
    val fullName: String = "",
    val username: String = "",
    val phoneCountryIso: String = "KE",
    val phoneNumber: String = "",
    val gender: String = "",
    val dateOfBirth: String = "",
    val location: String = "",
    val bio: String = "",
    val avatarUrl: String = "",
    val avatarFingerprintSha256: String = "",
    val usernameChangedAtEpochSec: Long = 0L,
    val nextUsernameChangeAtEpochSec: Long = 0L,
    val canChangeUsername: Boolean = true,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val likesCount: Int = 0,
    val postsCount: Int = 0,
    val backgroundPhotos: List<BackgroundPhotoDto> = emptyList()
)

@Serializable
data class AuthResponse(
    val userId: String,
    val email: String,
    val accessToken: String,
    val refreshToken: String,
    val profileCompleted: Boolean,
    val profile: ProfileDto
)

@Serializable
data class OtpSendResponse(
    val message: String,
    val resendCooldownSeconds: Int
)

@Serializable
data class OtpLockResponse(
    val message: String,
    val failedAttempts: Int = 0,
    val lockCycles: Int = 0,
    val lockRemainingSeconds: Int = 0,
    val resendRemainingSeconds: Int = 0
)

@Serializable
data class UsernameCheckResponse(
    val username: String,
    val available: Boolean
)

@Serializable
data class PhotoUploadResponse(
    val avatarUrl: String,
    val fingerprintSha256: String,
    val fingerprintDhash: String,
    val width: Int,
    val height: Int
)

@Serializable
data class ProfileUpdateResponse(
    val profileCompleted: Boolean,
    val profile: ProfileDto
)

@Serializable
data class ErrorResponse(
    val message: String
)

@Serializable
data class ContactDto(
    val userId: String,
    val fullName: String,
    val username: String,
    val countryIso: String,
    val countryName: String,
    val avatarUrl: String,
    val iFollow: Boolean,
    val followsMe: Boolean,
    val isFriend: Boolean
)

@Serializable
data class ContactsListResponse(
    val contacts: List<ContactDto>
)

@Serializable
data class FollowActionResponse(
    val targetUserId: String,
    val iFollow: Boolean,
    val followsMe: Boolean,
    val isFriend: Boolean
)
