package com.telefam.company.app

import com.telefam.company.app.nativecore.AppScreen
import com.telefam.company.app.nativecore.ContactItem
import com.telefam.company.app.nativecore.ContactsTab
import com.telefam.company.app.nativecore.HomeTab
import com.telefam.company.app.nativecore.TelefamNativeCore
import com.telefam.company.app.nativecore.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TelefamUnitTest {

    @Test
    fun parseUiState_mapsScreensAndTimersCorrectly() {
        val json = """
            {
              "userId": "u-123",
              "screen": "AUTH_OTP",
              "themeMode": "DARK",
              "homeTab": "CONTACTS",
              "contactsTab": "FRIENDS",
              "contactsSearchQuery": "Kenya",
              "email": "alice@telefam.company",
              "otp": "123456",
              "isLoading": false,
              "errorMessage": "",
              "noticeMessage": "Code sent",
              "resendRemainingSec": 45,
              "resendTimerText": "00:45",
              "canResend": false,
              "failedAttempts": 2,
              "lockCycles": 1,
              "lockRemainingSec": 60,
              "lockTimerText": "01:00",
              "integrityCompromised": false,
              "sealedVaultB64": "vault-abc",
              "profile": {
                "fullName": "Alice",
                "username": "alice",
                "phoneCountryIso": "KE",
                "phoneDialCode": "+254",
                "phoneCountryName": "Kenya",
                "bio": "Hello"
              }
            }
        """.trimIndent()
        val state = TelefamNativeCore.parseUiState(json)
        assertEquals("u-123", state.userId)
        assertEquals(AppScreen.AUTH_OTP, state.screen)
        assertEquals(ThemeMode.DARK, state.themeMode)
        assertEquals(HomeTab.CONTACTS, state.homeTab)
        assertEquals(ContactsTab.FRIENDS, state.contactsTab)
        assertEquals("Kenya", state.contactsSearchQuery)
        assertEquals("00:45", state.resendTimerText)
        assertEquals("01:00", state.lockTimerText)
        assertEquals(2, state.failedAttempts)
        assertEquals(1, state.lockCycles)
        assertFalse(state.canResend)
        assertEquals("Code sent", state.noticeMessage)
        assertEquals("+254", state.profile.phoneDialCode)
        assertEquals("Hello", state.profile.bio)
    }

    @Test
    fun parseUiState_parsesOwnerProfileStatsCooldownAndBackgrounds() {
        val json = """
            {
              "userId": "u-owner",
              "screen": "HOME",
              "themeMode": "LIGHT",
              "homeTab": "PROFILE",
              "profileContentTab": "SAVED",
              "profile": {
                "fullName": "Colloh185",
                "username": "colloh185",
                "location": "koiwa",
                "usernameChangedAtEpochSec": 1700000000,
                "nextUsernameChangeAtEpochSec": 1701209600,
                "canChangeUsername": false,
                "followersCount": 14,
                "followingCount": 8,
                "likesCount": 92,
                "postsCount": 0,
                "backgroundUploading": false,
                "backgroundPhotos": [
                  {
                    "id": "bg-1",
                    "imageUrl": "https://cdn.telefam.company/bg1.jpg",
                    "fingerprintSha256": "abc123",
                    "fingerprintDhash": "0011223344556677",
                    "width": 1440,
                    "height": 900
                  }
                ]
              }
            }
        """.trimIndent()
        val state = TelefamNativeCore.parseUiState(json)
        assertEquals(HomeTab.PROFILE, state.homeTab)
        assertEquals(com.telefam.company.app.nativecore.ProfileContentTab.SAVED, state.profileContentTab)
        assertEquals("Colloh185", state.profile.fullName)
        assertEquals("colloh185", state.profile.username)
        assertEquals("koiwa", state.profile.location)
        assertFalse(state.profile.canChangeUsername)
        assertEquals(1701209600L, state.profile.nextUsernameChangeAtEpochSec)
        assertEquals(14, state.profile.followersCount)
        assertEquals(8, state.profile.followingCount)
        assertEquals(92, state.profile.likesCount)
        assertEquals(1, state.profile.backgroundPhotos.size)
        assertEquals("bg-1", state.profile.backgroundPhotos.first().id)
        assertEquals(1440, state.profile.backgroundPhotos.first().width)
    }

    @Test
    fun parseHttpAction_parsesHeadersAndIgnoresNonHttp() {
        val httpJson = """
            {
              "action": "HTTP",
              "requestId": "req-42",
              "method": "POST",
              "url": "https://api.telefam.company/v1/auth/otp/send",
              "headers": {
                "X-Telefam-Signature": "sig123",
                "X-Telefam-Device": "devHash"
              },
              "body": "{\"email\":\"alice@telefam.company\"}",
              "extra": "otp_send"
            }
        """.trimIndent()
        val action = TelefamNativeCore.parseHttpAction(httpJson)
        assertNotNull(action)
        assertEquals("req-42", action!!.requestId)
        assertEquals("POST", action.method)
        assertEquals("sig123", action.headers["X-Telefam-Signature"])
        assertEquals("otp_send", action.extra)

        val stateJson = """{"action":"STATE","state":{}}"""
        assertNull(TelefamNativeCore.parseHttpAction(stateJson))
    }

    @Test
    fun parseCountries_returnsExpectedDialCodes() {
        val countriesJson = """
            [
              {"iso2":"KE","name":"Kenya","dialCode":"+254","placeholder":"712 345 678"},
              {"iso2":"US","name":"United States","dialCode":"+1","placeholder":"201 555 0123"}
            ]
        """.trimIndent()
        val list = TelefamNativeCore.parseCountries(countriesJson)
        assertEquals(2, list.size)
        assertTrue(list.any { it.iso2 == "KE" && it.dialCode == "+254" })
        assertTrue(list.any { it.iso2 == "US" && it.dialCode == "+1" })
    }

    @Test
    fun contactsSerializationAndParsing_roundTripsRelationships() {
        val original = listOf(
            ContactItem(
                userId = "u-1",
                fullName = "Makiche401",
                username = "makiche401",
                countryIso = "KE",
                countryName = "Kenya",
                avatarUrl = "",
                iFollow = true,
                followsMe = true,
                isFriend = true
            ),
            ContactItem(
                userId = "u-2",
                fullName = "Jane",
                username = "jane",
                countryIso = "UG",
                countryName = "Uganda",
                avatarUrl = "https://cdn.telefam.company/a.jpg",
                iFollow = false,
                followsMe = true,
                isFriend = false
            )
        )
        val json = TelefamNativeCore.serializeContactsList(original)
        val parsed = TelefamNativeCore.parseContactsList(json)
        assertEquals(original, parsed)
    }
}
