package com.telefam.company.shared.model

import kotlinx.serialization.Serializable

enum class AppScreen { AUTH_EMAIL, AUTH_OTP, PROFILE_SETUP, HOME }
enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class HomeTab { CHATS, CONTACTS, PROFILE }
enum class ProfileContentTab { POSTS, SAVED, REPOSTS, LOCKED }
enum class ContactsTab { ALL, SUGGESTIONS, FOLLOWERS, FOLLOWING, FRIENDS }

@Serializable
data class CountryOption(
    val iso2: String,
    val name: String,
    val dialCode: String,
    val placeholder: String
)

@Serializable
data class ContactItem(
    val userId: String = "",
    val fullName: String = "",
    val username: String = "",
    val countryIso: String = "KE",
    val countryName: String = "Kenya",
    val avatarUrl: String = "",
    val iFollow: Boolean = false,
    val followsMe: Boolean = false,
    val isFriend: Boolean = false
)

@Serializable
data class BackgroundPhotoItem(
    val id: String = "",
    val imageUrl: String = "",
    val fingerprintSha256: String = "",
    val fingerprintDhash: String = "",
    val width: Int = 0,
    val height: Int = 0
)

@Serializable
data class ProfileUiState(
    val fullName: String = "",
    val username: String = "",
    val usernameChecking: Boolean = false,
    val usernameChecked: Boolean = false,
    val usernameAvailable: Boolean = false,
    val usernameStatusMsg: String = "",
    val phoneCountryIso: String = "KE",
    val phoneDialCode: String = "+254",
    val phoneCountryName: String = "Kenya",
    val phoneNumber: String = "",
    val phoneError: String = "",
    val gender: String = "",
    val dateOfBirth: String = "",
    val dobError: String = "",
    val location: String = "",
    val bio: String = "",
    val avatarUrl: String = "",
    val photoUploading: Boolean = false,
    val usernameChangedAtEpochSec: Long = 0L,
    val nextUsernameChangeAtEpochSec: Long = 0L,
    val canChangeUsername: Boolean = true,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val likesCount: Int = 0,
    val postsCount: Int = 0,
    val backgroundPhotos: List<BackgroundPhotoItem> = emptyList(),
    val backgroundUploading: Boolean = false
)

@Serializable
data class TelefamUiState(
    val userId: String = "",
    val screen: AppScreen = AppScreen.AUTH_EMAIL,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val homeTab: HomeTab = HomeTab.CHATS,
    val profileContentTab: ProfileContentTab = ProfileContentTab.POSTS,
    val contactsTab: ContactsTab = ContactsTab.ALL,
    val contactsSearchQuery: String = "",
    val email: String = "",
    val otp: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    val noticeMessage: String = "",
    val resendRemainingSec: Int = 0,
    val resendTimerText: String = "00:00",
    val canResend: Boolean = true,
    val failedAttempts: Int = 0,
    val lockCycles: Int = 0,
    val lockRemainingSec: Int = 0,
    val lockTimerText: String = "00:00",
    val integrityCompromised: Boolean = false,
    val sealedVaultB64: String = "",
    val profile: ProfileUiState = ProfileUiState()
)

data class NativeHttpAction(
    val requestId: String,
    val method: String,
    val url: String,
    val headers: Map<String, String>,
    val body: String,
    val extra: String
)
