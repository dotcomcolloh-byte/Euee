package com.telefam.company.desktop

import com.telefam.company.desktop.nativecore.DesktopNativeCore
import com.telefam.company.desktop.platform.CachedContactRecord
import com.telefam.company.desktop.platform.CachedOwnerProfileRecord
import com.telefam.company.desktop.platform.DesktopOfflineCachePayload
import com.telefam.company.desktop.platform.DesktopSecurityBridge
import com.telefam.company.shared.TelefamJsonParser
import com.telefam.company.shared.model.ContactItem
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TelefamDesktopTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun masterKeyAndEncryptedOfflineCacheRoundTrip() {
        val dir = tempFolder.newFolder("telefam-vault-test")
        val bridge = DesktopSecurityBridge(customAppDir = dir)

        val key1 = bridge.getOrCreateVaultMasterKey()
        val key2 = bridge.getOrCreateVaultMasterKey()
        assertEquals(32, key1.size)
        assertArrayEquals(key1, key2)

        val (devHash, devPub) = bridge.deviceIdHashAndPublicKey()
        assertEquals(64, devHash.length)
        assertTrue(devPub.isNotBlank())

        bridge.saveSealedVault("c2VhbGVkLWRhdGE=")
        assertEquals("c2VhbGVkLWRhdGE=", bridge.loadSealedVault())

        val cache = DesktopOfflineCachePayload(
            profiles = listOf(
                CachedOwnerProfileRecord(
                    userId = "u1",
                    profileJson = """{"fullName":"Colloh185","username":"colloh185"}""",
                    pendingProfileSync = true,
                    updatedAtMs = 123456L
                )
            ),
            contacts = listOf(
                CachedContactRecord(
                    ownerUserId = "u1",
                    contact = ContactItem(
                        userId = "u2",
                        fullName = "Makiche401",
                        username = "makiche401",
                        countryIso = "KE",
                        countryName = "Kenya",
                        iFollow = true,
                        followsMe = true,
                        isFriend = true
                    ),
                    pendingAction = "",
                    updatedAtMs = 123456L
                )
            )
        )
        bridge.saveOfflineCache(cache)

        val rawFileBytes = File(dir, "offline_cache.enc").readBytes()
        val rawStr = String(rawFileBytes)
        assertFalse("Offline cache must be AES-GCM encrypted", rawStr.contains("colloh185"))
        assertFalse("Offline cache must be AES-GCM encrypted", rawStr.contains("makiche401"))

        val loaded = bridge.loadOfflineCache()
        assertEquals(cache, loaded)
    }

    @Test
    fun desktopNativeCoreDeclaresAllSharedJniSymbols() {
        val expectedMethods = setOf(
            "nativeInit",
            "nativeSetEmail",
            "nativeSetOtp",
            "nativeGoBackToEmail",
            "nativeUpdateProfileField",
            "nativeSetHomeTab",
            "nativeSetProfileContentTab",
            "nativeSetContactsTab",
            "nativeSetContactsSearch",
            "nativeCheckAndRecordFollowRate",
            "nativeApplyLocalProfileEdit",
            "nativeApplyCachedOwnerProfile",
            "nativeSerializeOwnerProfile",
            "nativeTick",
            "nativeCycleTheme",
            "nativeComingSoon",
            "nativeSetError",
            "nativeClearError",
            "nativeClearNotice",
            "nativeLogout",
            "nativePrepareSendOtp",
            "nativePrepareVerifyOtp",
            "nativePrepareGoogleAuth",
            "nativePrepareAppleAuth",
            "nativePrepareCheckUsername",
            "nativePrepareUploadPhoto",
            "nativePrepareSubmitProfile",
            "nativePrepareFetchOwnerProfile",
            "nativePrepareSyncEditedProfile",
            "nativePrepareUploadBackgroundPhoto",
            "nativePrepareDeleteBackgroundPhoto",
            "nativePrepareFetchContacts",
            "nativePrepareFollowAction",
            "nativePrepareRefreshToken",
            "nativePrepareLogout",
            "nativeFilterContacts",
            "nativeParseContactsSyncPayload",
            "nativeOnHttpResult",
            "nativeGetUiState",
            "nativeGetCountries"
        )
        val declared = DesktopNativeCore::class.java.declaredMethods.map { it.name }.toSet()
        for (name in expectedMethods) {
            assertTrue("Missing JNI binding $name in DesktopNativeCore", name in declared)
        }
    }

    @Test
    fun bridgeProvidesDistinctNonEmptyUrlNoncesAndConfigDefaults() {
        val dir = tempFolder.newFolder("telefam-nonce-test")
        val bridge = DesktopSecurityBridge(customAppDir = dir)
        val n1 = bridge.generateUrlNonce()
        val n2 = bridge.generateUrlNonce()
        assertTrue(n1.length >= 24)
        assertNotEquals(n1, n2)
        assertEquals("https://api.telefam.company", bridge.config.apiBaseUrl)
        assertEquals(
            "Error has occurred",
            TelefamJsonParser.sanitizeError("internal stacktrace", isDebug = false)
        )
    }
}
