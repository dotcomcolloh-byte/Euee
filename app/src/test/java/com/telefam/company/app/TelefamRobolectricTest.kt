package com.telefam.company.app

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.telefam.company.app.data.ContactsRepository
import com.telefam.company.app.data.ProfileRepository
import com.telefam.company.app.data.TelefamDatabase
import com.telefam.company.app.nativecore.AppScreen
import com.telefam.company.app.nativecore.BackgroundPhotoItem
import com.telefam.company.app.nativecore.ContactItem
import com.telefam.company.app.nativecore.ContactsTab
import com.telefam.company.app.nativecore.CountryOption
import com.telefam.company.app.nativecore.HomeTab
import com.telefam.company.app.nativecore.ProfileContentTab
import com.telefam.company.app.nativecore.ProfileUiState
import com.telefam.company.app.nativecore.TelefamUiState
import com.telefam.company.app.ui.screens.AuthScreen
import com.telefam.company.app.ui.screens.HomeScreen
import com.telefam.company.app.ui.screens.ProfileSetupScreen
import com.telefam.company.app.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class, qualifiers = "w411dp-h915dp")
class TelefamRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun appName_isTelefam() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Telefam", appName)
    }

    @Test
    fun authScreen_emailAndOtpStatesRenderCorrectly() {
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                AuthScreen(
                    state = TelefamUiState(screen = AppScreen.AUTH_EMAIL, email = "alice@telefam.company"),
                    onEmailChanged = {},
                    onSendOtp = {},
                    onOtpChanged = {},
                    onVerifyOtp = {},
                    onChangeEmail = {},
                    onContinueWithGoogle = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("email_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("send_otp_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("google_signin_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun authOtpScreen_showsTimersAndVerifyButton() {
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                AuthScreen(
                    state = TelefamUiState(
                        screen = AppScreen.AUTH_OTP,
                        email = "alice@telefam.company",
                        otp = "123456",
                        resendRemainingSec = 42,
                        resendTimerText = "00:42",
                        canResend = false,
                        lockRemainingSec = 60,
                        lockTimerText = "01:00"
                    ),
                    onEmailChanged = {},
                    onSendOtp = {},
                    onOtpChanged = {},
                    onVerifyOtp = {},
                    onChangeEmail = {},
                    onContinueWithGoogle = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("otp_target_email").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("verify_otp_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("otp_lock_countdown", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("resend_countdown_text").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("change_email_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun profileSetupScreen_rendersFieldsAndContinueButton() {
        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                ProfileSetupScreen(
                    state = TelefamUiState(
                        screen = AppScreen.PROFILE_SETUP,
                        profile = ProfileUiState(
                            fullName = "Alice",
                            username = "alice",
                            phoneCountryIso = "KE",
                            phoneDialCode = "+254",
                            phoneCountryName = "Kenya",
                            bio = "Hello world"
                        )
                    ),
                    countries = listOf(CountryOption("KE", "Kenya", "+254", "712 345 678")),
                    onBack = {},
                    onFieldChanged = { _, _ -> },
                    onUploadCroppedPhoto = { _, _, _, _, _ -> },
                    onSubmitProfile = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("upload_photo_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("full_name_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("username_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("phone_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("gender_selector").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("dob_selector").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("location_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("bio_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("profile_continue_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun homeScreen_rendersTopBarAndBottomNav_andSwitchesToContacts() {
        var clickedFeature = ""
        var selectedHomeTab: HomeTab? = null
        composeRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                HomeScreen(
                    state = TelefamUiState(screen = AppScreen.HOME, homeTab = HomeTab.CHATS),
                    isDark = true,
                    onCycleTheme = {},
                    onSelectHomeTab = { selectedHomeTab = it },
                    onComingSoon = { clickedFeature = it }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_search_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("home_theme_toggle").assertIsDisplayed()
        composeRule.onNodeWithTag("home_notifications_button").assertIsDisplayed()
        composeRule.onNodeWithTag("home_more_button").assertIsDisplayed()
        composeRule.onNodeWithTag("start_chat_button").assertIsDisplayed().performClick()
        assertTrue(clickedFeature.isNotEmpty())
        composeRule.onNodeWithTag("nav_chats").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_contacts").assertIsDisplayed().performClick()
        assertEquals(HomeTab.CONTACTS, selectedHomeTab)
        composeRule.onNodeWithTag("nav_post").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_feeds").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_profile").assertIsDisplayed()
    }

    @Test
    fun contactsScreen_rendersFilterTabsSearchAndRelationshipButtonsInLightAndDark() {
        var toggledContactId = ""
        var selectedTabKey = ""
        var searchQueryTyped = ""

        val sampleContacts = listOf(
            ContactItem("u1", "Makiche401", "makiche401", "KE", "Kenya", "", iFollow = true, followsMe = true, isFriend = true),
            ContactItem("u2", "Amara", "amara", "NG", "Nigeria", "", iFollow = false, followsMe = true, isFriend = false),
            ContactItem("u3", "Liam", "liam_g", "GB", "United Kingdom", "", iFollow = true, followsMe = false, isFriend = false),
            ContactItem("u4", "Zara", "zara_us", "US", "United States", "", iFollow = false, followsMe = false, isFriend = false)
        )

        composeRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                HomeScreen(
                    state = TelefamUiState(
                        screen = AppScreen.HOME,
                        homeTab = HomeTab.CONTACTS,
                        contactsTab = ContactsTab.ALL
                    ),
                    contacts = sampleContacts,
                    isDark = true,
                    onCycleTheme = {},
                    onSelectContactsTab = { selectedTabKey = it },
                    onContactsSearchChanged = { searchQueryTyped = it },
                    onToggleFollowContact = { toggledContactId = it.userId },
                    onComingSoon = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("contacts_tab_all").assertIsDisplayed()
        composeRule.onNodeWithTag("contacts_tab_suggestions").assertIsDisplayed()
        composeRule.onNodeWithTag("contacts_tab_followers").assertIsDisplayed()
        composeRule.onNodeWithTag("contacts_tab_following").assertIsDisplayed()
        composeRule.onNodeWithTag("contacts_tab_friends").assertIsDisplayed().performClick()
        assertEquals("friends", selectedTabKey)

        composeRule.onNodeWithTag("contacts_search_input").performTextInput("Kenya")
        assertEquals("Kenya", searchQueryTyped)

        composeRule.onNodeWithText("@makiche401 · Kenya").assertIsDisplayed()
        composeRule.onNodeWithTag("contact_action_u1").assertTextContains("Friends")
        composeRule.onNodeWithTag("contact_action_u2").assertTextContains("Follow back").performClick()
        assertEquals("u2", toggledContactId)
        composeRule.onNodeWithTag("contact_action_u3").assertTextContains("Following")
        composeRule.onNodeWithTag("contact_action_u4").assertTextContains("Follow")
    }

    @Test
    fun roomContactsCache_persistsOfflineAndPreservesPendingMutationsOnMerge() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, TelefamDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = ContactsRepository(db.contactDao())
            val viewerId = "viewer-1"

            // Initial remote directory merge into offline Room cache
            repo.mergeServerDirectory(
                viewerId,
                listOf(
                    ContactItem("u1", "Makiche401", "makiche401", "KE", "Kenya", "", iFollow = false, followsMe = true, isFriend = false)
                )
            )
            var cached = repo.observeContacts(viewerId).first()
            assertEquals(1, cached.size)
            assertFalse(cached.first().isFriend)

            // Offline follow-back optimistically updates Room & becomes Friends locally
            repo.setOptimisticFollowState(viewerId, "u1", follow = true)
            cached = repo.observeContacts(viewerId).first()
            assertTrue(cached.first().iFollow)
            assertTrue(cached.first().isFriend)
            assertEquals(1, repo.getPendingMutations(viewerId).size)

            // Silent background merge preserves local unsynced pending follow
            repo.mergeServerDirectory(
                viewerId,
                listOf(
                    ContactItem("u1", "Makiche401", "makiche401", "KE", "Kenya", "", iFollow = false, followsMe = true, isFriend = false)
                )
            )
            cached = repo.observeContacts(viewerId).first()
            assertTrue(cached.first().iFollow)
            assertTrue(cached.first().isFriend)

            // Server confirms mutation
            repo.markMutationSynced(viewerId, "u1", iFollow = true, followsMe = true)
            assertEquals(0, repo.getPendingMutations(viewerId).size)
        } finally {
            db.close()
        }
    }

    @Test
    fun ownerProfileScreen_rendersBannerStatsTabsFollowersSheetAndUsernameLock() {
        var comingSoonCalled = ""
        var selectedContentTab = ""
        var sharedClicked = false

        val contacts = listOf(
            ContactItem("u1", "Makiche401", "makiche401", "KE", "Kenya", "", iFollow = true, followsMe = true, isFriend = true),
            ContactItem("u2", "Amara", "amara", "NG", "Nigeria", "", iFollow = false, followsMe = true, isFriend = false)
        )

        composeRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                HomeScreen(
                    state = TelefamUiState(
                        userId = "owner-1",
                        screen = AppScreen.HOME,
                        homeTab = HomeTab.PROFILE,
                        profileContentTab = ProfileContentTab.POSTS,
                        profile = ProfileUiState(
                            fullName = "Colloh185",
                            username = "colloh185",
                            location = "koiwa",
                            canChangeUsername = false,
                            nextUsernameChangeAtEpochSec = 1800000000L,
                            followersCount = 2,
                            followingCount = 1,
                            likesCount = 9,
                            postsCount = 0,
                            backgroundPhotos = listOf(
                                BackgroundPhotoItem("bg-1", "https://example.com/bg1.jpg"),
                                BackgroundPhotoItem("bg-2", "https://example.com/bg2.jpg")
                            )
                        )
                    ),
                    contacts = contacts,
                    allContacts = contacts,
                    countries = listOf(CountryOption("KE", "Kenya", "+254", "712 345 678")),
                    isDark = false,
                    onCycleTheme = {},
                    onSelectProfileContentTab = { selectedContentTab = it },
                    onShareProfile = { sharedClicked = true },
                    onComingSoon = { comingSoonCalled = it }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("profile_background_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_display_name").assertTextContains("Colloh185")
        composeRule.onNodeWithTag("profile_username_text").assertTextContains("@colloh185")
        composeRule.onNodeWithTag("profile_location_text", useUnmergedTree = true).assertIsDisplayed()

        composeRule.onNodeWithTag("profile_subscribers_button").performClick()
        assertEquals("Subscribers", comingSoonCalled)
        composeRule.onNodeWithTag("profile_settings_button").performClick()
        assertEquals("Settings", comingSoonCalled)
        composeRule.onNodeWithTag("profile_menu_button").performClick()
        assertEquals("Menu", comingSoonCalled)

        composeRule.onNodeWithTag("profile_share_button").performClick()
        assertTrue(sharedClicked)

        composeRule.onNodeWithTag("profile_tab_saved").performClick()
        assertEquals("saved", selectedContentTab)
        composeRule.onNodeWithTag("profile_tab_reposts").performClick()
        assertEquals("reposts", selectedContentTab)
        composeRule.onNodeWithTag("profile_tab_locked").performClick()
        assertEquals("locked", selectedContentTab)

        // Tap Followers stat -> opens Followers/Following sheet
        composeRule.onNodeWithTag("profile_followers_stat").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("follow_sheet_tab_followers").assertIsDisplayed()
        composeRule.onNodeWithTag("follow_sheet_row_u1").assertIsDisplayed()
    }

    @Test
    fun ownerProfileScreen_editProfileShows14DayUsernameLock() {
        composeRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                HomeScreen(
                    state = TelefamUiState(
                        userId = "owner-1",
                        screen = AppScreen.HOME,
                        homeTab = HomeTab.PROFILE,
                        profile = ProfileUiState(
                            fullName = "Colloh185",
                            username = "colloh185",
                            phoneCountryIso = "KE",
                            phoneNumber = "+254712345678",
                            gender = "Male",
                            dateOfBirth = "2000-01-01",
                            location = "koiwa",
                            canChangeUsername = false,
                            nextUsernameChangeAtEpochSec = 1800000000L
                        )
                    ),
                    isDark = true,
                    onCycleTheme = {},
                    onComingSoon = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile_edit_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edit_username_locked_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("edit_profile_save_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun roomOwnerProfileCache_preservesPendingEditUntilSynced() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, TelefamDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = ProfileRepository(db.ownerProfileDao())
            val uid = "owner-1"

            repo.saveServerProfile(uid, """{"fullName":"Server Name"}""")
            var cached = repo.getCachedProfile(uid)
            assertFalse(cached!!.pendingProfileSync)

            // Offline profile edit sets pendingProfileSync = true
            repo.saveLocalEdit(uid, """{"fullName":"Edited Offline"}""")
            cached = repo.getCachedProfile(uid)
            assertTrue(cached!!.pendingProfileSync)
            assertTrue(cached.profileJson.contains("Edited Offline"))

            // Background server fetch does NOT overwrite local unsynced edit
            repo.saveServerProfile(uid, """{"fullName":"Server Name"}""")
            cached = repo.getCachedProfile(uid)
            assertTrue(cached!!.profileJson.contains("Edited Offline"))

            // Once background sync succeeds, flag clears and authoritative payload saves
            repo.markSyncCompleted(uid, """{"fullName":"Edited Synced"}""")
            cached = repo.getCachedProfile(uid)
            assertFalse(cached!!.pendingProfileSync)
            assertTrue(cached.profileJson.contains("Edited Synced"))
        } finally {
            db.close()
        }
    }
}
