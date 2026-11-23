package com.telefam.company.shared

import com.telefam.company.shared.model.AppScreen
import com.telefam.company.shared.model.ContactItem
import com.telefam.company.shared.model.ContactsTab
import com.telefam.company.shared.model.HomeTab
import com.telefam.company.shared.model.ProfileContentTab
import com.telefam.company.shared.model.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TelefamSharedParserTest {

    @Test
    fun parseUiStateDecodesAllTabsAndBackgrounds() {
        val raw = """
            {
              "userId": "u-123",
              "screen": "HOME",
              "themeMode": "DARK",
              "homeTab": "PROFILE",
              "profileContentTab": "REPOSTS",
              "contactsTab": "FRIENDS",
              "contactsSearchQuery": "kenya",
              "email": "user@telefam.company",
              "profile": {
                "fullName": "Alex",
                "username": "alex",
                "phoneCountryIso": "KE",
                "phoneDialCode": "+254",
                "followersCount": 14,
                "followingCount": 8,
                "likesCount": 42,
                "postsCount": 0,
                "canChangeUsername": false,
                "nextUsernameChangeAtEpochSec": 1800000000,
                "backgroundPhotos": [
                  {
                    "id": "bg-1",
                    "imageUrl": "https://api.telefam.company/media/bg1.jpg",
                    "fingerprintSha256": "abc",
                    "width": 1600,
                    "height": 900
                  }
                ]
              }
            }
        """.trimIndent()

        val state = TelefamJsonParser.parseUiState(raw)
        assertEquals("u-123", state.userId)
        assertEquals(AppScreen.HOME, state.screen)
        assertEquals(ThemeMode.DARK, state.themeMode)
        assertEquals(HomeTab.PROFILE, state.homeTab)
        assertEquals(ProfileContentTab.REPOSTS, state.profileContentTab)
        assertEquals(ContactsTab.FRIENDS, state.contactsTab)
        assertEquals("kenya", state.contactsSearchQuery)
        assertEquals(14, state.profile.followersCount)
        assertEquals(8, state.profile.followingCount)
        assertEquals(1, state.profile.backgroundPhotos.size)
        assertEquals("bg-1", state.profile.backgroundPhotos.first().id)
    }

    @Test
    fun parseHttpActionHandlesHttpAndNone() {
        assertNull(TelefamJsonParser.parseHttpAction("""{"action":"NONE"}"""))
        val action = TelefamJsonParser.parseHttpAction(
            """{"action":"HTTP","requestId":"r1","method":"POST","url":"https://api.telefam.company/api/v1/auth/apple","headers":{"Content-Type":"application/json"},"body":"{}","extra":""}"""
        )
        assertNotNull(action)
        assertEquals("r1", action.requestId)
        assertEquals("POST", action.method)
        assertEquals("application/json", action.headers["Content-Type"])
    }

    @Test
    fun contactsRoundTripAndSanitizedErrorsWork() {
        val list = listOf(
            ContactItem(
                userId = "u2",
                fullName = "Makiche401",
                username = "makiche401",
                countryIso = "KE",
                countryName = "Kenya",
                iFollow = true,
                followsMe = true,
                isFriend = true
            )
        )
        val encoded = TelefamJsonParser.serializeContactsList(list)
        val decoded = TelefamJsonParser.parseContactsList(encoded)
        assertEquals(list, decoded)
        assertTrue(decoded.first().isFriend)
        assertEquals("Error has occurred", TelefamJsonParser.sanitizeError("db constraint failed", isDebug = false))
    }
}
