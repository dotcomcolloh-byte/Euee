package com.telefam.company.desktop.nativecore

object DesktopNativeCore {

    init {
        DesktopNativeLoader.ensureLoaded()
    }

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

    external fun nativePrepareAppleAuth(
        identityToken: String,
        authorizationCode: String,
        nonce: String,
        fullName: String,
        emailHint: String,
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
}
