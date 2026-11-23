package com.telefam.company.app.nativecore

import org.json.JSONArray
import org.json.JSONObject

enum class AppScreen { AUTH_EMAIL, AUTH_OTP, PROFILE_SETUP, HOME }
enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class HomeTab { CHATS, CONTACTS, PROFILE }
enum class ProfileContentTab { POSTS, SAVED, REPOSTS, LOCKED }
enum class ContactsTab { ALL, SUGGESTIONS, FOLLOWERS, FOLLOWING, FRIENDS }

data class CountryOption(
    val iso2: String,
    val name: String,
    val dialCode: String,
    val placeholder: String
)

data class ContactItem(
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

data class BackgroundPhotoItem(
    val id: String,
    val imageUrl: String,
    val fingerprintSha256: String = "",
    val fingerprintDhash: String = "",
    val width: Int = 0,
    val height: Int = 0
)

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

object TelefamNativeCore {
    external fun nativeInit(
        baseUrl: String,
        isDebugBuild: Boolean,
        hardwareName: String,
        deviceIdHash: String,
        devicePublicKeyB64: String,
        runtimeCertSha256: String,
        expectedReleaseCertSha256: String,
        masterKey: ByteArray,
        sealedVaultB64: String,
        nowMs: Long
    ): String

    external fun nativeSetEmail(email: String): String
    external fun nativeSetOtp(otp: String): String
    external fun nativeGoBackToEmail(): String
    external fun nativeUpdateProfileField(
        field: String,
        value: String,
        curYear: Int,
        curMonth: Int,
        curDay: Int
    ): String

    external fun nativeSetHomeTab(tab: String): String
    external fun nativeSetProfileContentTab(tab: String): String
    external fun nativeSetContactsTab(tab: String): String
    external fun nativeSetContactsSearch(query: String): String
    external fun nativeCheckAndRecordFollowRate(nowMs: Long): Boolean

    external fun nativeApplyLocalProfileEdit(
        editJson: String,
        curYear: Int,
        curMonth: Int,
        curDay: Int,
        nowMs: Long
    ): Boolean

    external fun nativeApplyCachedOwnerProfile(cachedJson: String, nowMs: Long): String
    external fun nativeSerializeOwnerProfile(): String

    external fun nativeTick(nowMs: Long): String
    external fun nativeCycleTheme(): String
    external fun nativeComingSoon(feature: String): String
    external fun nativeSetError(rawError: String): String
    external fun nativeClearError(): String
    external fun nativeClearNotice(): String
    external fun nativeLogout(nowMs: Long): String

    external fun nativePrepareSendOtp(isOnline: Boolean, nowMs: Long, random16: ByteArray): String
    external fun nativePrepareVerifyOtp(isOnline: Boolean, nowMs: Long, random16: ByteArray): String
    external fun nativePrepareGoogleAuth(
        idToken: String,
        nonce: String,
        isOnline: Boolean,
        nowMs: Long,
        random16: ByteArray
    ): String

    external fun nativePrepareCheckUsername(
        username: String,
        isOnline: Boolean,
        nowMs: Long,
        random16: ByteArray
    ): String

    external fun nativePrepareUploadPhoto(
        mimeType: String,
        byteSize: Long,
        cropX: Double,
        cropY: Double,
        cropW: Double,
        cropH: Double,
        isOnline: Boolean,
        nowMs: Long,
        random16: ByteArray
    ): String

    external fun nativePrepareSubmitProfile(
        curYear: Int,
        curMonth: Int,
        curDay: Int,
        isOnline: Boolean,
        nowMs: Long,
        random16: ByteArray
    ): String

    external fun nativePrepareFetchOwnerProfile(isOnline: Boolean, nowMs: Long, random16: ByteArray): String
    external fun nativePrepareSyncEditedProfile(
        profileJson: String,
        isOnline: Boolean,
        nowMs: Long,
        random16: ByteArray
    ): String

    external fun nativePrepareUploadBackgroundPhoto(
        mimeType: String,
        byteSize: Long,
        cropX: Double,
        cropY: Double,
        cropW: Double,
        cropH: Double,
        isOnline: Boolean,
        nowMs: Long,
        random16: ByteArray
    ): String

    external fun nativePrepareDeleteBackgroundPhoto(
        photoId: String,
        isOnline: Boolean,
        nowMs: Long,
        random16: ByteArray
    ): String

    external fun nativePrepareFetchContacts(isOnline: Boolean, nowMs: Long, random16: ByteArray): String
    external fun nativePrepareFollowAction(
        targetUserId: String,
        follow: Boolean,
        isOnline: Boolean,
        nowMs: Long,
        random16: ByteArray
    ): String

    external fun nativePrepareRefreshToken(isOnline: Boolean, nowMs: Long, random16: ByteArray): String
    external fun nativePrepareLogout(isOnline: Boolean, nowMs: Long, random16: ByteArray): String

    external fun nativeFilterContacts(contactsArrayJson: String): String
    external fun nativeParseContactsSyncPayload(serverResponseJson: String): String

    external fun nativeOnHttpResult(
        requestId: String,
        statusCode: Int,
        responseBody: String,
        networkError: Boolean,
        isOnline: Boolean,
        nowMs: Long,
        vaultNonce12: ByteArray
    ): String

    external fun nativeGetUiState(): String
    external fun nativeGetCountries(): String

    fun parseUiState(jsonStr: String): TelefamUiState {
        val obj = JSONObject(jsonStr)
        val p = obj.optJSONObject("profile") ?: JSONObject()
        val bgArr = p.optJSONArray("backgroundPhotos")
        val backgrounds = if (bgArr != null) {
            List(bgArr.length()) { idx ->
                val o = bgArr.optJSONObject(idx) ?: JSONObject()
                BackgroundPhotoItem(
                    id = o.optString("id"),
                    imageUrl = o.optString("imageUrl"),
                    fingerprintSha256 = o.optString("fingerprintSha256"),
                    fingerprintDhash = o.optString("fingerprintDhash"),
                    width = o.optInt("width", 0),
                    height = o.optInt("height", 0)
                )
            }.filter { it.id.isNotBlank() && it.imageUrl.isNotBlank() }
        } else {
            emptyList()
        }
        return TelefamUiState(
            userId = obj.optString("userId"),
            screen = when (obj.optString("screen")) {
                "AUTH_OTP" -> AppScreen.AUTH_OTP
                "PROFILE_SETUP" -> AppScreen.PROFILE_SETUP
                "HOME" -> AppScreen.HOME
                else -> AppScreen.AUTH_EMAIL
            },
            themeMode = when (obj.optString("themeMode")) {
                "LIGHT" -> ThemeMode.LIGHT
                "DARK" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            },
            homeTab = when (obj.optString("homeTab")) {
                "CONTACTS" -> HomeTab.CONTACTS
                "PROFILE" -> HomeTab.PROFILE
                else -> HomeTab.CHATS
            },
            profileContentTab = when (obj.optString("profileContentTab")) {
                "SAVED" -> ProfileContentTab.SAVED
                "REPOSTS" -> ProfileContentTab.REPOSTS
                "LOCKED" -> ProfileContentTab.LOCKED
                else -> ProfileContentTab.POSTS
            },
            contactsTab = when (obj.optString("contactsTab")) {
                "SUGGESTIONS" -> ContactsTab.SUGGESTIONS
                "FOLLOWERS" -> ContactsTab.FOLLOWERS
                "FOLLOWING" -> ContactsTab.FOLLOWING
                "FRIENDS" -> ContactsTab.FRIENDS
                else -> ContactsTab.ALL
            },
            contactsSearchQuery = obj.optString("contactsSearchQuery"),
            email = obj.optString("email"),
            otp = obj.optString("otp"),
            isLoading = obj.optBoolean("isLoading"),
            errorMessage = obj.optString("errorMessage"),
            noticeMessage = obj.optString("noticeMessage"),
            resendRemainingSec = obj.optInt("resendRemainingSec"),
            resendTimerText = obj.optString("resendTimerText", "00:00"),
            canResend = obj.optBoolean("canResend"),
            failedAttempts = obj.optInt("failedAttempts"),
            lockCycles = obj.optInt("lockCycles"),
            lockRemainingSec = obj.optInt("lockRemainingSec"),
            lockTimerText = obj.optString("lockTimerText", "00:00"),
            integrityCompromised = obj.optBoolean("integrityCompromised"),
            sealedVaultB64 = obj.optString("sealedVaultB64"),
            profile = ProfileUiState(
                fullName = p.optString("fullName"),
                username = p.optString("username"),
                usernameChecking = p.optBoolean("usernameChecking"),
                usernameChecked = p.optBoolean("usernameChecked"),
                usernameAvailable = p.optBoolean("usernameAvailable"),
                usernameStatusMsg = p.optString("usernameStatusMsg"),
                phoneCountryIso = p.optString("phoneCountryIso", "KE"),
                phoneDialCode = p.optString("phoneDialCode", "+254"),
                phoneCountryName = p.optString("phoneCountryName", "Kenya"),
                phoneNumber = p.optString("phoneNumber"),
                phoneError = p.optString("phoneError"),
                gender = p.optString("gender"),
                dateOfBirth = p.optString("dateOfBirth"),
                dobError = p.optString("dobError"),
                location = p.optString("location"),
                bio = p.optString("bio"),
                avatarUrl = p.optString("avatarUrl"),
                photoUploading = p.optBoolean("photoUploading"),
                usernameChangedAtEpochSec = p.optLong("usernameChangedAtEpochSec", 0L),
                nextUsernameChangeAtEpochSec = p.optLong("nextUsernameChangeAtEpochSec", 0L),
                canChangeUsername = p.optBoolean("canChangeUsername", true),
                followersCount = p.optInt("followersCount", 0),
                followingCount = p.optInt("followingCount", 0),
                likesCount = p.optInt("likesCount", 0),
                postsCount = p.optInt("postsCount", 0),
                backgroundPhotos = backgrounds,
                backgroundUploading = p.optBoolean("backgroundUploading", false)
            )
        )
    }

    fun parseHttpAction(jsonStr: String): NativeHttpAction? {
        val obj = JSONObject(jsonStr)
        if (obj.optString("action") != "HTTP") return null
        val hdrs = mutableMapOf<String, String>()
        val hObj = obj.optJSONObject("headers")
        if (hObj != null) {
            val keys = hObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                hdrs[k] = hObj.optString(k)
            }
        }
        return NativeHttpAction(
            requestId = obj.optString("requestId"),
            method = obj.optString("method"),
            url = obj.optString("url"),
            headers = hdrs,
            body = obj.optString("body"),
            extra = obj.optString("extra")
        )
    }

    fun parseCountries(jsonStr: String): List<CountryOption> {
        val arr = JSONArray(jsonStr)
        return List(arr.length()) { idx ->
            val o = arr.getJSONObject(idx)
            CountryOption(
                iso2 = o.optString("iso2"),
                name = o.optString("name"),
                dialCode = o.optString("dialCode"),
                placeholder = o.optString("placeholder")
            )
        }
    }

    fun parseContactsList(jsonStr: String): List<ContactItem> {
        if (jsonStr.isBlank()) return emptyList()
        val arr = JSONArray(jsonStr)
        return List(arr.length()) { idx ->
            val o = arr.getJSONObject(idx)
            val iFollow = o.optBoolean("iFollow", false)
            val followsMe = o.optBoolean("followsMe", false)
            ContactItem(
                userId = o.optString("userId"),
                fullName = o.optString("fullName"),
                username = o.optString("username"),
                countryIso = o.optString("countryIso", "KE"),
                countryName = o.optString("countryName", "Kenya"),
                avatarUrl = o.optString("avatarUrl"),
                iFollow = iFollow,
                followsMe = followsMe,
                isFriend = o.optBoolean("isFriend", iFollow && followsMe)
            )
        }
    }

    fun serializeContactsList(items: List<ContactItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
            obj.put("userId", item.userId)
            obj.put("fullName", item.fullName)
            obj.put("username", item.username)
            obj.put("countryIso", item.countryIso)
            obj.put("countryName", item.countryName)
            obj.put("avatarUrl", item.avatarUrl)
            obj.put("iFollow", item.iFollow)
            obj.put("followsMe", item.followsMe)
            obj.put("isFriend", item.isFriend)
            arr.put(obj)
        }
        return arr.toString()
    }
}
