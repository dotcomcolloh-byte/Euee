package com.telefam.company.shared

import com.telefam.company.shared.model.AppScreen
import com.telefam.company.shared.model.BackgroundPhotoItem
import com.telefam.company.shared.model.ContactItem
import com.telefam.company.shared.model.ContactsTab
import com.telefam.company.shared.model.CountryOption
import com.telefam.company.shared.model.HomeTab
import com.telefam.company.shared.model.NativeHttpAction
import com.telefam.company.shared.model.ProfileContentTab
import com.telefam.company.shared.model.ProfileUiState
import com.telefam.company.shared.model.TelefamUiState
import com.telefam.company.shared.model.ThemeMode
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

object TelefamJsonParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun parseUiState(jsonStr: String): TelefamUiState {
        if (jsonStr.isBlank()) return TelefamUiState()
        val obj = runCatching { json.parseToJsonElement(jsonStr).jsonObject }.getOrNull()
            ?: return TelefamUiState()
        val p = obj["profile"]?.let { runCatching { it.jsonObject }.getOrNull() } ?: JsonObject(emptyMap())
        val bgArr = p["backgroundPhotos"]?.let { runCatching { it.jsonArray }.getOrNull() }
        val backgrounds = bgArr?.mapNotNull { el ->
            val o = runCatching { el.jsonObject }.getOrNull() ?: return@mapNotNull null
            val id = o.str("id")
            val imageUrl = o.str("imageUrl")
            if (id.isBlank() || imageUrl.isBlank()) null
            else BackgroundPhotoItem(
                id = id,
                imageUrl = imageUrl,
                fingerprintSha256 = o.str("fingerprintSha256"),
                fingerprintDhash = o.str("fingerprintDhash"),
                width = o.int("width", 0),
                height = o.int("height", 0)
            )
        } ?: emptyList()

        return TelefamUiState(
            userId = obj.str("userId"),
            screen = when (obj.str("screen")) {
                "AUTH_OTP" -> AppScreen.AUTH_OTP
                "PROFILE_SETUP" -> AppScreen.PROFILE_SETUP
                "HOME" -> AppScreen.HOME
                else -> AppScreen.AUTH_EMAIL
            },
            themeMode = when (obj.str("themeMode")) {
                "LIGHT" -> ThemeMode.LIGHT
                "DARK" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            },
            homeTab = when (obj.str("homeTab")) {
                "CONTACTS" -> HomeTab.CONTACTS
                "PROFILE" -> HomeTab.PROFILE
                else -> HomeTab.CHATS
            },
            profileContentTab = when (obj.str("profileContentTab")) {
                "SAVED" -> ProfileContentTab.SAVED
                "REPOSTS" -> ProfileContentTab.REPOSTS
                "LOCKED" -> ProfileContentTab.LOCKED
                else -> ProfileContentTab.POSTS
            },
            contactsTab = when (obj.str("contactsTab")) {
                "SUGGESTIONS" -> ContactsTab.SUGGESTIONS
                "FOLLOWERS" -> ContactsTab.FOLLOWERS
                "FOLLOWING" -> ContactsTab.FOLLOWING
                "FRIENDS" -> ContactsTab.FRIENDS
                else -> ContactsTab.ALL
            },
            contactsSearchQuery = obj.str("contactsSearchQuery"),
            email = obj.str("email"),
            otp = obj.str("otp"),
            isLoading = obj.bool("isLoading"),
            errorMessage = obj.str("errorMessage"),
            noticeMessage = obj.str("noticeMessage"),
            resendRemainingSec = obj.int("resendRemainingSec", 0),
            resendTimerText = obj.str("resendTimerText", "00:00"),
            canResend = obj.bool("canResend", true),
            failedAttempts = obj.int("failedAttempts", 0),
            lockCycles = obj.int("lockCycles", 0),
            lockRemainingSec = obj.int("lockRemainingSec", 0),
            lockTimerText = obj.str("lockTimerText", "00:00"),
            integrityCompromised = obj.bool("integrityCompromised"),
            sealedVaultB64 = obj.str("sealedVaultB64"),
            profile = ProfileUiState(
                fullName = p.str("fullName"),
                username = p.str("username"),
                usernameChecking = p.bool("usernameChecking"),
                usernameChecked = p.bool("usernameChecked"),
                usernameAvailable = p.bool("usernameAvailable"),
                usernameStatusMsg = p.str("usernameStatusMsg"),
                phoneCountryIso = p.str("phoneCountryIso", "KE"),
                phoneDialCode = p.str("phoneDialCode", "+254"),
                phoneCountryName = p.str("phoneCountryName", "Kenya"),
                phoneNumber = p.str("phoneNumber"),
                phoneError = p.str("phoneError"),
                gender = p.str("gender"),
                dateOfBirth = p.str("dateOfBirth"),
                dobError = p.str("dobError"),
                location = p.str("location"),
                bio = p.str("bio"),
                avatarUrl = p.str("avatarUrl"),
                photoUploading = p.bool("photoUploading"),
                usernameChangedAtEpochSec = p.long("usernameChangedAtEpochSec", 0L),
                nextUsernameChangeAtEpochSec = p.long("nextUsernameChangeAtEpochSec", 0L),
                canChangeUsername = p.bool("canChangeUsername", true),
                followersCount = p.int("followersCount", 0),
                followingCount = p.int("followingCount", 0),
                likesCount = p.int("likesCount", 0),
                postsCount = p.int("postsCount", 0),
                backgroundPhotos = backgrounds,
                backgroundUploading = p.bool("backgroundUploading")
            )
        )
    }

    fun parseHttpAction(jsonStr: String): NativeHttpAction? {
        if (jsonStr.isBlank()) return null
        val obj = runCatching { json.parseToJsonElement(jsonStr).jsonObject }.getOrNull() ?: return null
        if (obj.str("action") != "HTTP") return null
        val headersObj = obj["headers"]?.let { runCatching { it.jsonObject }.getOrNull() }
        val headers = buildMap {
            headersObj?.forEach { (k, v) ->
                put(k, (v as? JsonPrimitive)?.contentOrNull ?: "")
            }
        }
        return NativeHttpAction(
            requestId = obj.str("requestId"),
            method = obj.str("method"),
            url = obj.str("url"),
            headers = headers,
            body = obj.str("body"),
            extra = obj.str("extra")
        )
    }

    fun parseCountries(jsonStr: String): List<CountryOption> {
        if (jsonStr.isBlank()) return emptyList()
        val arr = runCatching { json.parseToJsonElement(jsonStr).jsonArray }.getOrNull() ?: return emptyList()
        return arr.mapNotNull { el ->
            val o = runCatching { el.jsonObject }.getOrNull() ?: return@mapNotNull null
            CountryOption(
                iso2 = o.str("iso2"),
                name = o.str("name"),
                dialCode = o.str("dialCode"),
                placeholder = o.str("placeholder")
            )
        }
    }

    fun parseContactsList(jsonStr: String): List<ContactItem> {
        if (jsonStr.isBlank()) return emptyList()
        val arr = runCatching { json.parseToJsonElement(jsonStr).jsonArray }.getOrNull() ?: return emptyList()
        return arr.mapNotNull { el ->
            val o = runCatching { el.jsonObject }.getOrNull() ?: return@mapNotNull null
            val iFollow = o.bool("iFollow")
            val followsMe = o.bool("followsMe")
            ContactItem(
                userId = o.str("userId"),
                fullName = o.str("fullName"),
                username = o.str("username"),
                countryIso = o.str("countryIso", "KE"),
                countryName = o.str("countryName", "Kenya"),
                avatarUrl = o.str("avatarUrl"),
                iFollow = iFollow,
                followsMe = followsMe,
                isFriend = o.bool("isFriend", iFollow && followsMe)
            )
        }
    }

    fun serializeContactsList(items: List<ContactItem>): String {
        val array = JsonArray(
            items.map { item ->
                buildJsonObject {
                    put("userId", item.userId)
                    put("fullName", item.fullName)
                    put("username", item.username)
                    put("countryIso", item.countryIso)
                    put("countryName", item.countryName)
                    put("avatarUrl", item.avatarUrl)
                    put("iFollow", item.iFollow)
                    put("followsMe", item.followsMe)
                    put("isFriend", item.isFriend)
                }
            }
        )
        return json.encodeToString(array)
    }

    fun buildProfileEditJson(
        fullName: String,
        username: String,
        phoneCountryIso: String,
        phoneNumber: String,
        gender: String,
        dateOfBirth: String,
        location: String,
        bio: String
    ): String = buildJsonObject {
        put("fullName", fullName)
        put("username", username)
        put("phoneCountryIso", phoneCountryIso)
        put("phoneNumber", phoneNumber)
        put("gender", gender)
        put("dateOfBirth", dateOfBirth)
        put("location", location)
        put("bio", bio)
    }.toString()

    fun parseFollowActionResponse(body: String, defaultIFollow: Boolean, defaultFollowsMe: Boolean): Pair<Boolean, Boolean> {
        val obj = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
        val serverIFollow = obj?.get("iFollow")?.let { runCatching { it.jsonPrimitive.booleanOrNull }.getOrNull() }
            ?: defaultIFollow
        val serverFollowsMe = obj?.get("followsMe")?.let { runCatching { it.jsonPrimitive.booleanOrNull }.getOrNull() }
            ?: defaultFollowsMe
        return serverIFollow to serverFollowsMe
    }

    fun parseCropExtra(extraJson: String): CropBounds {
        val obj = runCatching { json.parseToJsonElement(extraJson).jsonObject }.getOrNull()
            ?: return CropBounds(0.0, 0.0, 1.0, 1.0)
        return CropBounds(
            cropX = obj["cropX"]?.let { runCatching { it.jsonPrimitive.content.toDoubleOrNull() }.getOrNull() } ?: 0.0,
            cropY = obj["cropY"]?.let { runCatching { it.jsonPrimitive.content.toDoubleOrNull() }.getOrNull() } ?: 0.0,
            cropWidth = obj["cropWidth"]?.let { runCatching { it.jsonPrimitive.content.toDoubleOrNull() }.getOrNull() } ?: 1.0,
            cropHeight = obj["cropHeight"]?.let { runCatching { it.jsonPrimitive.content.toDoubleOrNull() }.getOrNull() } ?: 1.0
        )
    }

    fun sanitizeError(raw: String?, isDebug: Boolean): String {
        if (!isDebug) return TelefamStrings.GENERIC_ERROR
        return raw?.takeIf { it.isNotBlank() } ?: TelefamStrings.GENERIC_ERROR
    }

    private fun JsonObject.str(key: String, default: String = ""): String =
        (this[key] as? JsonPrimitive)?.contentOrNull ?: default

    private fun JsonObject.bool(key: String, default: Boolean = false): Boolean =
        (this[key] as? JsonPrimitive)?.booleanOrNull ?: default

    private fun JsonObject.int(key: String, default: Int = 0): Int =
        (this[key] as? JsonPrimitive)?.intOrNull ?: default

    private fun JsonObject.long(key: String, default: Long = 0L): Long =
        (this[key] as? JsonPrimitive)?.longOrNull ?: default
}

data class CropBounds(
    val cropX: Double,
    val cropY: Double,
    val cropWidth: Double,
    val cropHeight: Double
)
