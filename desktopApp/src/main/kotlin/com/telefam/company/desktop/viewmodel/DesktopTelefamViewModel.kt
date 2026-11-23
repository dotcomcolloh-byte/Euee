package com.telefam.company.desktop.viewmodel

import com.telefam.company.desktop.nativecore.DesktopNativeCore
import com.telefam.company.desktop.nativecore.DesktopNativeLoader
import com.telefam.company.desktop.platform.CachedContactRecord
import com.telefam.company.desktop.platform.CachedOwnerProfileRecord
import com.telefam.company.desktop.platform.DesktopOfflineCachePayload
import com.telefam.company.desktop.platform.DesktopSecurityBridge
import com.telefam.company.shared.TelefamJsonParser
import com.telefam.company.shared.TelefamStrings
import com.telefam.company.shared.model.AppScreen
import com.telefam.company.shared.model.ContactItem
import com.telefam.company.shared.model.CountryOption
import com.telefam.company.shared.model.HomeTab
import com.telefam.company.shared.model.TelefamUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopTelefamViewModel(
    private val bridge: DesktopSecurityBridge = DesktopSecurityBridge()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val syncMutex = Mutex()
    private val profileSyncMutex = Mutex()
    private val refreshMutex = Mutex()

    private val nativeReady: Boolean = DesktopNativeLoader.ensureLoaded()
    private val _uiState = MutableStateFlow(TelefamUiState())
    val uiState: StateFlow<TelefamUiState> = _uiState.asStateFlow()

    private val _cacheState = MutableStateFlow(bridge.loadOfflineCache())

    val contacts: StateFlow<List<ContactItem>> = combine(
        _uiState.map { Triple(it.userId, it.contactsTab, it.contactsSearchQuery) }.distinctUntilChanged(),
        _cacheState
    ) { (userId, _, _), cache ->
        if (userId.isBlank() || !nativeReady) {
            emptyList()
        } else {
            val cached = cache.contacts
                .filter { it.ownerUserId == userId }
                .map { it.contact }
            val serialized = TelefamJsonParser.serializeContactsList(cached)
            val filteredJson = DesktopNativeCore.nativeFilterContacts(serialized)
            TelefamJsonParser.parseContactsList(filteredJson)
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val allCachedContacts: StateFlow<List<ContactItem>> = combine(
        _uiState.map { it.userId }.distinctUntilChanged(),
        _cacheState
    ) { userId, cache ->
        if (userId.isBlank()) emptyList()
        else cache.contacts.filter { it.ownerUserId == userId }.map { it.contact }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    val countries: List<CountryOption> by lazy {
        if (!nativeReady) fallbackKenyaOption()
        else TelefamJsonParser.parseCountries(DesktopNativeCore.nativeGetCountries())
    }

    private var usernameCheckJob: Job? = null

    init {
        if (nativeReady) {
            val (devHash, devPubB64) = bridge.deviceIdHashAndPublicKey()
            val masterKey = bridge.getOrCreateVaultMasterKey()
            val stateJson = DesktopNativeCore.nativeInit(
                baseUrl = bridge.config.apiBaseUrl,
                isDebugBuild = bridge.config.isDebugBuild,
                hardwareName = bridge.hardwareName(),
                deviceIdHash = devHash,
                devicePublicKeyB64 = devPubB64,
                runtimeCertSha256 = "",
                expectedReleaseCertSha256 = "",
                masterKey = masterKey,
                sealedVaultB64 = bridge.loadSealedVault(),
                nowMs = System.currentTimeMillis()
            )
            masterKey.fill(0)
            applyStateJson(stateJson)

            val initialUserId = _uiState.value.userId
            if (initialUserId.isNotBlank()) {
                scope.launch {
                    val cached = _cacheState.value.profiles.firstOrNull { it.userId == initialUserId }
                    if (cached != null && cached.profileJson.isNotBlank()) {
                        val hydratedJson = DesktopNativeCore.nativeApplyCachedOwnerProfile(
                            cached.profileJson,
                            System.currentTimeMillis()
                        )
                        applyStateJson(hydratedJson)
                    }
                    val online = withContext(Dispatchers.IO) { bridge.isOnline() }
                    if (online && _uiState.value.screen == AppScreen.HOME) {
                        syncOwnerProfileSilently(initialUserId)
                        syncContactsSilently(initialUserId)
                    }
                }
            }
            startTicker()
            startSilentBackgroundSyncLoop()
        } else {
            _uiState.value = TelefamUiState(
                errorMessage = DesktopNativeLoader.loadErrorMessage ?: TelefamStrings.GENERIC_ERROR
            )
        }
    }

    fun close() {
        scope.cancel()
    }

    private fun startTicker() {
        scope.launch {
            while (true) {
                delay(1000L)
                val s = _uiState.value
                if (nativeReady && (s.resendRemainingSec > 0 || s.lockRemainingSec > 0)) {
                    applyStateJson(DesktopNativeCore.nativeTick(System.currentTimeMillis()))
                }
            }
        }
    }

    private fun startSilentBackgroundSyncLoop() {
        scope.launch {
            while (true) {
                val s = _uiState.value
                if (nativeReady && s.screen == AppScreen.HOME && s.userId.isNotBlank()) {
                    val online = withContext(Dispatchers.IO) { bridge.isOnline() }
                    if (online) {
                        syncOwnerProfileSilently(s.userId)
                        syncContactsSilently(s.userId)
                    }
                }
                delay(25000L)
            }
        }
    }

    private fun todayYmd(): Triple<Int, Int, Int> {
        val cal = Calendar.getInstance()
        return Triple(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun applyStateJson(jsonStr: String) {
        val parsed = TelefamJsonParser.parseUiState(jsonStr)
        val wasHome = _uiState.value.screen == AppScreen.HOME
        _uiState.value = parsed
        bridge.saveSealedVault(parsed.sealedVaultB64)
        if (!wasHome && parsed.screen == AppScreen.HOME && parsed.userId.isNotBlank() && nativeReady) {
            val snapshotJson = DesktopNativeCore.nativeSerializeOwnerProfile()
            scope.launch {
                saveProfileCache(parsed.userId, snapshotJson, pendingSync = false)
                syncOwnerProfileSilently(parsed.userId)
                syncContactsSilently(parsed.userId)
            }
        }
    }

    fun onEmailChanged(email: String) {
        if (!nativeReady) return
        applyStateJson(DesktopNativeCore.nativeSetEmail(email))
    }

    fun onOtpChanged(otp: String) {
        if (!nativeReady) return
        applyStateJson(DesktopNativeCore.nativeSetOtp(otp))
    }

    fun onChangeEmail() {
        if (!nativeReady) return
        applyStateJson(DesktopNativeCore.nativeGoBackToEmail())
    }

    fun onCycleTheme() {
        if (!nativeReady) return
        applyStateJson(DesktopNativeCore.nativeCycleTheme())
    }

    fun onComingSoon(featureLabel: String) {
        if (!nativeReady) return
        applyStateJson(DesktopNativeCore.nativeComingSoon(featureLabel))
    }

    fun onClearError() {
        if (!nativeReady) return
        applyStateJson(DesktopNativeCore.nativeClearError())
    }

    fun onClearNotice() {
        if (!nativeReady) return
        applyStateJson(DesktopNativeCore.nativeClearNotice())
    }

    fun onShowNotice(message: String) {
        _uiState.value = _uiState.value.copy(noticeMessage = message)
    }

    fun onSelectHomeTab(tab: HomeTab) {
        if (!nativeReady) return
        val label = when (tab) {
            HomeTab.CONTACTS -> "contacts"
            HomeTab.PROFILE -> "profile"
            HomeTab.CHATS -> "chats"
        }
        applyStateJson(DesktopNativeCore.nativeSetHomeTab(label))
        val uid = _uiState.value.userId
        if (uid.isNotBlank()) {
            when (tab) {
                HomeTab.CONTACTS -> scope.launch { syncContactsSilently(uid) }
                HomeTab.PROFILE -> scope.launch {
                    syncOwnerProfileSilently(uid)
                    syncContactsSilently(uid)
                }
                HomeTab.CHATS -> Unit
            }
        }
    }

    fun onSelectProfileContentTab(tabKey: String) {
        if (!nativeReady) return
        applyStateJson(DesktopNativeCore.nativeSetProfileContentTab(tabKey))
    }

    fun onSelectContactsTab(tabKey: String) {
        if (!nativeReady) return
        applyStateJson(DesktopNativeCore.nativeSetContactsTab(tabKey))
        val uid = _uiState.value.userId
        if (uid.isNotBlank()) {
            scope.launch { syncContactsSilently(uid) }
        }
    }

    fun onContactsSearchChanged(query: String) {
        if (!nativeReady) return
        applyStateJson(DesktopNativeCore.nativeSetContactsSearch(query))
    }

    fun onToggleFollowContact(contact: ContactItem) {
        if (!nativeReady) return
        val userId = _uiState.value.userId
        if (userId.isBlank()) return
        val wantFollow = !contact.iFollow
        val nowMs = System.currentTimeMillis()
        if (wantFollow) {
            val allowed = DesktopNativeCore.nativeCheckAndRecordFollowRate(nowMs)
            if (!allowed) {
                applyStateJson(DesktopNativeCore.nativeGetUiState())
                return
            }
        }
        scope.launch {
            updateCacheOptimisticFollow(userId, contact.userId, wantFollow)
            syncContactsSilently(userId)
            syncOwnerProfileSilently(userId)
        }
    }

    fun onSaveEditedProfile(
        fullName: String,
        username: String,
        phoneCountryIso: String,
        phoneNumber: String,
        gender: String,
        dateOfBirth: String,
        location: String,
        bio: String
    ): Boolean {
        if (!nativeReady) return false
        val (y, m, d) = todayYmd()
        val nowMs = System.currentTimeMillis()
        val editPayload = TelefamJsonParser.buildProfileEditJson(
            fullName = fullName,
            username = username,
            phoneCountryIso = phoneCountryIso,
            phoneNumber = phoneNumber,
            gender = gender,
            dateOfBirth = dateOfBirth,
            location = location,
            bio = bio
        )
        val ok = DesktopNativeCore.nativeApplyLocalProfileEdit(editPayload, y, m, d, nowMs)
        applyStateJson(DesktopNativeCore.nativeGetUiState())
        if (!ok) return false

        val uid = _uiState.value.userId
        if (uid.isNotBlank()) {
            val serializedProfile = DesktopNativeCore.nativeSerializeOwnerProfile()
            scope.launch {
                saveProfileCache(uid, serializedProfile, pendingSync = true)
                syncOwnerProfileSilently(uid)
            }
        }
        return true
    }

    fun onUploadCroppedAvatar(
        bytes: ByteArray,
        mime: String,
        cropX: Double,
        cropY: Double,
        cropW: Double,
        cropH: Double
    ) {
        if (!nativeReady) return
        scope.launch {
            val online = withContext(Dispatchers.IO) { bridge.isOnline() }
            val actionJson = DesktopNativeCore.nativePrepareUploadPhoto(
                mime,
                bytes.size.toLong(),
                cropX,
                cropY,
                cropW,
                cropH,
                online,
                System.currentTimeMillis(),
                bridge.randomBytes(16)
            )
            applyStateJson(DesktopNativeCore.nativeGetUiState())
            val action = TelefamJsonParser.parseHttpAction(actionJson) ?: return@launch
            val (code, body, netErr) = withContext(Dispatchers.IO) {
                bridge.executePhotoUpload(action, bytes, mime)
            }
            val nextJson = DesktopNativeCore.nativeOnHttpResult(
                action.requestId,
                code,
                body,
                netErr,
                online && !netErr,
                System.currentTimeMillis(),
                bridge.randomBytes(12)
            )
            applyStateJson(nextJson)
            val uid = _uiState.value.userId
            if (!netErr && code in 200..299 && uid.isNotBlank()) {
                val serialized = DesktopNativeCore.nativeSerializeOwnerProfile()
                saveProfileCache(uid, serialized, pendingSync = false)
            }
        }
    }

    fun onUploadBackgroundPhoto(
        bytes: ByteArray,
        mime: String,
        cropX: Double,
        cropY: Double,
        cropW: Double,
        cropH: Double
    ) {
        if (!nativeReady) return
        scope.launch {
            val online = withContext(Dispatchers.IO) { bridge.isOnline() }
            val actionJson = DesktopNativeCore.nativePrepareUploadBackgroundPhoto(
                mime,
                bytes.size.toLong(),
                cropX,
                cropY,
                cropW,
                cropH,
                online,
                System.currentTimeMillis(),
                bridge.randomBytes(16)
            )
            applyStateJson(DesktopNativeCore.nativeGetUiState())
            val action = TelefamJsonParser.parseHttpAction(actionJson) ?: return@launch
            val (code, body, netErr) = withContext(Dispatchers.IO) {
                bridge.executePhotoUpload(action, bytes, mime)
            }
            val nextJson = DesktopNativeCore.nativeOnHttpResult(
                action.requestId,
                code,
                body,
                netErr,
                online && !netErr,
                System.currentTimeMillis(),
                bridge.randomBytes(12)
            )
            applyStateJson(nextJson)
            val uid = _uiState.value.userId
            if (!netErr && code in 200..299 && uid.isNotBlank()) {
                val serialized = DesktopNativeCore.nativeSerializeOwnerProfile()
                saveProfileCache(uid, serialized, pendingSync = false)
            }
        }
    }

    fun onDeleteBackgroundPhoto(photoId: String) {
        if (!nativeReady) return
        scope.launch {
            val online = withContext(Dispatchers.IO) { bridge.isOnline() }
            val actionJson = DesktopNativeCore.nativePrepareDeleteBackgroundPhoto(
                photoId,
                online,
                System.currentTimeMillis(),
                bridge.randomBytes(16)
            )
            applyStateJson(DesktopNativeCore.nativeGetUiState())
            val action = TelefamJsonParser.parseHttpAction(actionJson) ?: return@launch
            val (code, body, netErr) = withContext(Dispatchers.IO) { bridge.executeHttp(action) }
            val nextJson = DesktopNativeCore.nativeOnHttpResult(
                action.requestId,
                code,
                body,
                netErr,
                online && !netErr,
                System.currentTimeMillis(),
                bridge.randomBytes(12)
            )
            applyStateJson(nextJson)
            val uid = _uiState.value.userId
            if (!netErr && code in 200..299 && uid.isNotBlank()) {
                val serialized = DesktopNativeCore.nativeSerializeOwnerProfile()
                saveProfileCache(uid, serialized, pendingSync = false)
            }
        }
    }

    fun onSendOtp() {
        if (!nativeReady) return
        scope.launch {
            val online = withContext(Dispatchers.IO) { bridge.isOnline() }
            val actionJson = DesktopNativeCore.nativePrepareSendOtp(
                online,
                System.currentTimeMillis(),
                bridge.randomBytes(16)
            )
            applyStateJson(DesktopNativeCore.nativeGetUiState())
            dispatchHttp(actionJson)
        }
    }

    fun onVerifyOtp() {
        if (!nativeReady) return
        scope.launch {
            val online = withContext(Dispatchers.IO) { bridge.isOnline() }
            val actionJson = DesktopNativeCore.nativePrepareVerifyOtp(
                online,
                System.currentTimeMillis(),
                bridge.randomBytes(16)
            )
            applyStateJson(DesktopNativeCore.nativeGetUiState())
            dispatchHttp(actionJson)
        }
    }

    fun onContinueWithGoogle() {
        if (!nativeReady) return
        scope.launch {
            val online = withContext(Dispatchers.IO) { bridge.isOnline() }
            if (!online) {
                onSendOtp()
                return@launch
            }
            val nonce = bridge.generateUrlNonce()
            val tokenRes = withContext(Dispatchers.IO) {
                bridge.requestGoogleIdTokenViaBrowser(nonce)
            }
            tokenRes.onSuccess { idToken ->
                val actionJson = DesktopNativeCore.nativePrepareGoogleAuth(
                    idToken,
                    nonce,
                    true,
                    System.currentTimeMillis(),
                    bridge.randomBytes(16)
                )
                applyStateJson(DesktopNativeCore.nativeGetUiState())
                dispatchHttp(actionJson)
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Google Sign-In unavailable"
                applyStateJson(DesktopNativeCore.nativeSetError(msg))
            }
        }
    }

    fun onProfileFieldChanged(field: String, value: String) {
        if (!nativeReady) return
        val (y, m, d) = todayYmd()
        applyStateJson(DesktopNativeCore.nativeUpdateProfileField(field, value, y, m, d))
        if (field == "username") {
            onCheckUsername(_uiState.value.profile.username)
        }
    }

    fun onCheckUsername(username: String) {
        if (!nativeReady) return
        usernameCheckJob?.cancel()
        usernameCheckJob = scope.launch {
            delay(450L)
            val online = withContext(Dispatchers.IO) { bridge.isOnline() }
            val actionJson = DesktopNativeCore.nativePrepareCheckUsername(
                username,
                online,
                System.currentTimeMillis(),
                bridge.randomBytes(16)
            )
            applyStateJson(DesktopNativeCore.nativeGetUiState())
            dispatchHttp(actionJson)
        }
    }

    fun onSubmitProfile() {
        if (!nativeReady) return
        scope.launch {
            val online = withContext(Dispatchers.IO) { bridge.isOnline() }
            val (y, m, d) = todayYmd()
            val actionJson = DesktopNativeCore.nativePrepareSubmitProfile(
                y,
                m,
                d,
                online,
                System.currentTimeMillis(),
                bridge.randomBytes(16)
            )
            applyStateJson(DesktopNativeCore.nativeGetUiState())
            dispatchHttp(actionJson)
        }
    }

    fun onLogout() {
        if (!nativeReady) return
        scope.launch {
            val online = withContext(Dispatchers.IO) { bridge.isOnline() }
            val logoutAction = TelefamJsonParser.parseHttpAction(
                DesktopNativeCore.nativePrepareLogout(
                    online,
                    System.currentTimeMillis(),
                    bridge.randomBytes(16)
                )
            )
            withContext(Dispatchers.IO) {
                if (logoutAction != null) {
                    bridge.executeHttp(logoutAction)
                }
                val cleared = DesktopOfflineCachePayload()
                _cacheState.value = cleared
                bridge.saveOfflineCache(cleared)
            }
            applyStateJson(DesktopNativeCore.nativeLogout(System.currentTimeMillis()))
        }
    }

    private suspend fun syncOwnerProfileSilently(userId: String) {
        if (!nativeReady || userId.isBlank()) return
        val online = withContext(Dispatchers.IO) { bridge.isOnline() }
        if (!online) return
        profileSyncMutex.withLock {
            withContext(Dispatchers.IO) {
                val cached = _cacheState.value.profiles.firstOrNull { it.userId == userId }
                if (cached != null && cached.pendingProfileSync && cached.profileJson.isNotBlank()) {
                    val pushActionJson = DesktopNativeCore.nativePrepareSyncEditedProfile(
                        cached.profileJson,
                        true,
                        System.currentTimeMillis(),
                        bridge.randomBytes(16)
                    )
                    val pushAction = TelefamJsonParser.parseHttpAction(pushActionJson)
                    if (pushAction != null) {
                        val (code, body, netErr) = bridge.executeHttp(pushAction)
                        val updatedJson = DesktopNativeCore.nativeOnHttpResult(
                            pushAction.requestId,
                            code,
                            body,
                            netErr,
                            !netErr,
                            System.currentTimeMillis(),
                            bridge.randomBytes(12)
                        )
                        if (!netErr && code in 200..299) {
                            val freshProfileJson = DesktopNativeCore.nativeSerializeOwnerProfile()
                            saveProfileCache(userId, freshProfileJson, pendingSync = false)
                            withContext(Dispatchers.Main) { applyStateJson(updatedJson) }
                        } else if (!netErr && code in 400..499) {
                            saveProfileCache(userId, cached.profileJson, pendingSync = false)
                        } else {
                            return@withContext
                        }
                    }
                }

                val fetchActionJson = DesktopNativeCore.nativePrepareFetchOwnerProfile(
                    true,
                    System.currentTimeMillis(),
                    bridge.randomBytes(16)
                )
                val fetchAction = TelefamJsonParser.parseHttpAction(fetchActionJson) ?: return@withContext
                val (code, body, netErr) = bridge.executeHttp(fetchAction)
                val nextStateJson = DesktopNativeCore.nativeOnHttpResult(
                    fetchAction.requestId,
                    code,
                    body,
                    netErr,
                    !netErr,
                    System.currentTimeMillis(),
                    bridge.randomBytes(12)
                )
                if (!netErr && code in 200..299) {
                    val freshProfileJson = DesktopNativeCore.nativeSerializeOwnerProfile()
                    saveProfileCache(userId, freshProfileJson, pendingSync = false)
                    withContext(Dispatchers.Main) { applyStateJson(nextStateJson) }
                } else if (!netErr && code == 401) {
                    refreshSessionSilently()
                }
            }
        }
    }

    private suspend fun refreshSessionSilently(): Boolean {
        val online = withContext(Dispatchers.IO) { bridge.isOnline() }
        if (!online) return false
        return refreshMutex.withLock {
            withContext(Dispatchers.IO) {
                val actionJson = DesktopNativeCore.nativePrepareRefreshToken(
                    true,
                    System.currentTimeMillis(),
                    bridge.randomBytes(16)
                )
                val action = TelefamJsonParser.parseHttpAction(actionJson) ?: return@withContext false
                val (code, body, netErr) = bridge.executeHttp(action)
                val nextStateJson = DesktopNativeCore.nativeOnHttpResult(
                    action.requestId,
                    code,
                    body,
                    netErr,
                    !netErr,
                    System.currentTimeMillis(),
                    bridge.randomBytes(12)
                )
                withContext(Dispatchers.Main) { applyStateJson(nextStateJson) }
                !netErr && code in 200..299
            }
        }
    }

    private suspend fun syncContactsSilently(userId: String) {
        if (!nativeReady || userId.isBlank()) return
        val online = withContext(Dispatchers.IO) { bridge.isOnline() }
        if (!online) return
        syncMutex.withLock {
            withContext(Dispatchers.IO) {
                val pending = _cacheState.value.contacts.filter {
                    it.ownerUserId == userId && it.pendingAction.isNotEmpty()
                }
                for (item in pending) {
                    val follow = item.pendingAction == "FOLLOW"
                    val actionJson = DesktopNativeCore.nativePrepareFollowAction(
                        targetUserId = item.contact.userId,
                        follow = follow,
                        isOnline = true,
                        nowMs = System.currentTimeMillis(),
                        random16 = bridge.randomBytes(16)
                    )
                    val action = TelefamJsonParser.parseHttpAction(actionJson) ?: continue
                    val (code, body, netErr) = bridge.executeHttp(action)
                    val nextStateJson = DesktopNativeCore.nativeOnHttpResult(
                        action.requestId,
                        code,
                        body,
                        netErr,
                        !netErr,
                        System.currentTimeMillis(),
                        bridge.randomBytes(12)
                    )
                    if (!netErr && code in 200..299) {
                        val (serverIFollow, serverFollowsMe) = TelefamJsonParser.parseFollowActionResponse(
                            body,
                            follow,
                            item.contact.followsMe
                        )
                        markContactSynced(userId, item.contact.userId, serverIFollow, serverFollowsMe)
                    } else if (!netErr && code == 429) {
                        revertPendingFollow(userId, item.contact.userId)
                        withContext(Dispatchers.Main) { applyStateJson(nextStateJson) }
                    } else if (!netErr && code in 400..499) {
                        revertPendingFollow(userId, item.contact.userId)
                    }
                }

                val fetchActionJson = DesktopNativeCore.nativePrepareFetchContacts(
                    isOnline = true,
                    nowMs = System.currentTimeMillis(),
                    random16 = bridge.randomBytes(16)
                )
                val fetchAction = TelefamJsonParser.parseHttpAction(fetchActionJson) ?: return@withContext
                val (code, body, netErr) = bridge.executeHttp(fetchAction)
                DesktopNativeCore.nativeOnHttpResult(
                    fetchAction.requestId,
                    code,
                    body,
                    netErr,
                    !netErr,
                    System.currentTimeMillis(),
                    bridge.randomBytes(12)
                )
                if (!netErr && code in 200..299) {
                    val normalizedJson = DesktopNativeCore.nativeParseContactsSyncPayload(body)
                    val remoteContacts = TelefamJsonParser.parseContactsList(normalizedJson)
                    mergeRemoteContacts(userId, remoteContacts)
                }
            }
        }
    }

    private suspend fun dispatchHttp(actionJson: String) {
        val action = TelefamJsonParser.parseHttpAction(actionJson) ?: return
        val (code, body, netErr) = withContext(Dispatchers.IO) { bridge.executeHttp(action) }
        val nextJson = DesktopNativeCore.nativeOnHttpResult(
            action.requestId,
            code,
            body,
            netErr,
            !netErr,
            System.currentTimeMillis(),
            bridge.randomBytes(12)
        )
        applyStateJson(nextJson)
    }

    private suspend fun saveProfileCache(userId: String, profileJson: String, pendingSync: Boolean) {
        withContext(Dispatchers.IO) {
            val cur = _cacheState.value
            val updatedProfiles = cur.profiles.filterNot { it.userId == userId } +
                CachedOwnerProfileRecord(userId, profileJson, pendingSync, System.currentTimeMillis())
            val next = cur.copy(profiles = updatedProfiles)
            _cacheState.value = next
            bridge.saveOfflineCache(next)
        }
    }

    private suspend fun updateCacheOptimisticFollow(ownerId: String, targetId: String, wantFollow: Boolean) {
        withContext(Dispatchers.IO) {
            val cur = _cacheState.value
            val updated = cur.contacts.map { rec ->
                if (rec.ownerUserId == ownerId && rec.contact.userId == targetId) {
                    val updatedContact = rec.contact.copy(
                        iFollow = wantFollow,
                        isFriend = wantFollow && rec.contact.followsMe
                    )
                    rec.copy(
                        contact = updatedContact,
                        pendingAction = if (wantFollow) "FOLLOW" else "UNFOLLOW",
                        updatedAtMs = System.currentTimeMillis()
                    )
                } else {
                    rec
                }
            }
            val next = cur.copy(contacts = updated)
            _cacheState.value = next
            bridge.saveOfflineCache(next)
        }
    }

    private suspend fun markContactSynced(ownerId: String, targetId: String, iFollow: Boolean, followsMe: Boolean) {
        withContext(Dispatchers.IO) {
            val cur = _cacheState.value
            val updated = cur.contacts.map { rec ->
                if (rec.ownerUserId == ownerId && rec.contact.userId == targetId) {
                    rec.copy(
                        contact = rec.contact.copy(
                            iFollow = iFollow,
                            followsMe = followsMe,
                            isFriend = iFollow && followsMe
                        ),
                        pendingAction = "",
                        updatedAtMs = System.currentTimeMillis()
                    )
                } else {
                    rec
                }
            }
            val next = cur.copy(contacts = updated)
            _cacheState.value = next
            bridge.saveOfflineCache(next)
        }
    }

    private suspend fun revertPendingFollow(ownerId: String, targetId: String) {
        withContext(Dispatchers.IO) {
            val cur = _cacheState.value
            val updated = cur.contacts.map { rec ->
                if (rec.ownerUserId == ownerId && rec.contact.userId == targetId) {
                    val reverted = rec.pendingAction != "FOLLOW"
                    rec.copy(
                        contact = rec.contact.copy(
                            iFollow = reverted,
                            isFriend = reverted && rec.contact.followsMe
                        ),
                        pendingAction = "",
                        updatedAtMs = System.currentTimeMillis()
                    )
                } else {
                    rec
                }
            }
            val next = cur.copy(contacts = updated)
            _cacheState.value = next
            bridge.saveOfflineCache(next)
        }
    }

    private suspend fun mergeRemoteContacts(ownerId: String, remote: List<ContactItem>) {
        withContext(Dispatchers.IO) {
            val cur = _cacheState.value
            val pendingMap = cur.contacts
                .filter { it.ownerUserId == ownerId && it.pendingAction.isNotEmpty() }
                .associateBy { it.contact.userId }
            val mergedOwner = remote.map { item ->
                val pending = pendingMap[item.userId]
                if (pending != null) pending else CachedContactRecord(ownerId, item, "", System.currentTimeMillis())
            }
            val otherOwners = cur.contacts.filter { it.ownerUserId != ownerId }
            val next = cur.copy(contacts = otherOwners + mergedOwner)
            _cacheState.value = next
            bridge.saveOfflineCache(next)
        }
    }

    private fun fallbackKenyaOption(): List<CountryOption> = listOf(
        CountryOption("KE", "Kenya", "+254", "712345678")
    )
}
