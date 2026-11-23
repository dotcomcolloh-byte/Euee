package com.telefam.company.app.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.telefam.company.app.BuildConfig
import com.telefam.company.app.data.ContactEntity
import com.telefam.company.app.data.ContactsRepository
import com.telefam.company.app.data.ProfileRepository
import com.telefam.company.app.data.TelefamDatabase
import com.telefam.company.app.nativecore.AppScreen
import com.telefam.company.app.nativecore.ContactItem
import com.telefam.company.app.nativecore.CountryOption
import com.telefam.company.app.nativecore.HomeTab
import com.telefam.company.app.nativecore.TelefamNativeCore
import com.telefam.company.app.nativecore.TelefamUiState
import com.telefam.company.app.platform.DeviceSecurityBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class TelefamViewModel(application: Application) : AndroidViewModel(application) {

    private val bridge = DeviceSecurityBridge(application.applicationContext)
    private val db = TelefamDatabase.getInstance(application.applicationContext)
    private val contactsRepository = ContactsRepository(db.contactDao())
    private val profileRepository = ProfileRepository(db.ownerProfileDao())
    private val syncMutex = Mutex()
    private val profileSyncMutex = Mutex()
    private val refreshMutex = Mutex()

    private val _uiState = MutableStateFlow(TelefamUiState())
    val uiState: StateFlow<TelefamUiState> = _uiState.asStateFlow()

    val contacts: StateFlow<List<ContactItem>> = _uiState
        .map { Triple(it.userId, it.contactsTab, it.contactsSearchQuery) }
        .distinctUntilChanged()
        .flatMapLatest { (userId, _, _) ->
            if (userId.isBlank()) {
                flowOf(emptyList())
            } else {
                contactsRepository.observeContacts(userId).map { cached ->
                    val serialized = TelefamNativeCore.serializeContactsList(cached)
                    val filteredJson = TelefamNativeCore.nativeFilterContacts(serialized)
                    TelefamNativeCore.parseContactsList(filteredJson)
                }
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Unfiltered snapshot stream used by Owner Profile Followers/Following bottom sheet
    val allCachedContacts: StateFlow<List<ContactItem>> = _uiState
        .map { it.userId }
        .distinctUntilChanged()
        .flatMapLatest { userId ->
            if (userId.isBlank()) flowOf(emptyList())
            else contactsRepository.observeContacts(userId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val countries: List<CountryOption> by lazy {
        TelefamNativeCore.parseCountries(TelefamNativeCore.nativeGetCountries())
    }

    private var usernameCheckJob: Job? = null
    private var unregisterNetworkCallback: (() -> Unit)? = null

    init {
        val (devHash, devPubB64) = bridge.deviceIdHashAndPublicKey()
        val masterKey = bridge.getOrCreateVaultMasterKey()
        val stateJson = TelefamNativeCore.nativeInit(
            baseUrl = BuildConfig.TELEFAM_API_BASE_URL,
            isDebugBuild = BuildConfig.DEBUG,
            hardwareName = bridge.hardwareName(),
            deviceIdHash = devHash,
            devicePublicKeyB64 = devPubB64,
            runtimeCertSha256 = bridge.signingCertSha256Hex(),
            expectedReleaseCertSha256 = "",
            masterKey = masterKey,
            sealedVaultB64 = bridge.loadSealedVault(),
            nowMs = System.currentTimeMillis()
        )
        masterKey.fill(0)
        applyStateJson(stateJson)

        val initialUserId = _uiState.value.userId
        if (initialUserId.isNotBlank()) {
            viewModelScope.launch {
                val cached = withContext(Dispatchers.IO) {
                    profileRepository.getCachedProfile(initialUserId)
                }
                if (cached != null && cached.profileJson.isNotBlank()) {
                    val hydratedJson = TelefamNativeCore.nativeApplyCachedOwnerProfile(
                        cached.profileJson,
                        System.currentTimeMillis()
                    )
                    applyStateJson(hydratedJson)
                }
                if (bridge.isOnline() && _uiState.value.screen == AppScreen.HOME) {
                    syncOwnerProfileSilently(initialUserId)
                    syncContactsSilently(initialUserId)
                }
            }
        }

        unregisterNetworkCallback = bridge.registerNetworkAvailableCallback {
            val s = _uiState.value
            if (s.screen == AppScreen.HOME && s.userId.isNotBlank()) {
                viewModelScope.launch {
                    syncOwnerProfileSilently(s.userId)
                    syncContactsSilently(s.userId)
                }
            }
        }

        startTicker()
        startSilentBackgroundSyncLoop()
    }

    override fun onCleared() {
        unregisterNetworkCallback?.invoke()
        super.onCleared()
    }

    private fun startTicker() {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                val s = _uiState.value
                if (s.resendRemainingSec > 0 || s.lockRemainingSec > 0) {
                    applyStateJson(TelefamNativeCore.nativeTick(System.currentTimeMillis()))
                }
            }
        }
    }

    private fun startSilentBackgroundSyncLoop() {
        viewModelScope.launch {
            while (true) {
                val s = _uiState.value
                if (s.screen == AppScreen.HOME && s.userId.isNotBlank() && bridge.isOnline()) {
                    syncOwnerProfileSilently(s.userId)
                    syncContactsSilently(s.userId)
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

    private fun applyStateJson(json: String) {
        val parsed = TelefamNativeCore.parseUiState(json)
        val wasHome = _uiState.value.screen == AppScreen.HOME
        _uiState.value = parsed
        bridge.saveSealedVault(parsed.sealedVaultB64)
        if (!wasHome && parsed.screen == AppScreen.HOME && parsed.userId.isNotBlank()) {
            val snapshotJson = TelefamNativeCore.nativeSerializeOwnerProfile()
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    profileRepository.saveServerProfile(parsed.userId, snapshotJson)
                }
                syncOwnerProfileSilently(parsed.userId)
                syncContactsSilently(parsed.userId)
            }
        }
    }

    fun onEmailChanged(email: String) =
        applyStateJson(TelefamNativeCore.nativeSetEmail(email))

    fun onOtpChanged(otp: String) =
        applyStateJson(TelefamNativeCore.nativeSetOtp(otp))

    fun onChangeEmail() =
        applyStateJson(TelefamNativeCore.nativeGoBackToEmail())

    fun onCycleTheme() =
        applyStateJson(TelefamNativeCore.nativeCycleTheme())

    fun onComingSoon(featureLabel: String) =
        applyStateJson(TelefamNativeCore.nativeComingSoon(featureLabel))

    fun onClearError() =
        applyStateJson(TelefamNativeCore.nativeClearError())

    fun onClearNotice() =
        applyStateJson(TelefamNativeCore.nativeClearNotice())

    fun onSelectHomeTab(tab: HomeTab) {
        val label = when (tab) {
            HomeTab.CONTACTS -> "contacts"
            HomeTab.PROFILE -> "profile"
            HomeTab.CHATS -> "chats"
        }
        applyStateJson(TelefamNativeCore.nativeSetHomeTab(label))
        val uid = _uiState.value.userId
        if (uid.isNotBlank()) {
            when (tab) {
                HomeTab.CONTACTS -> viewModelScope.launch { syncContactsSilently(uid) }
                HomeTab.PROFILE -> viewModelScope.launch {
                    syncOwnerProfileSilently(uid)
                    syncContactsSilently(uid)
                }
                HomeTab.CHATS -> Unit
            }
        }
    }

    fun onSelectProfileContentTab(tabKey: String) {
        applyStateJson(TelefamNativeCore.nativeSetProfileContentTab(tabKey))
    }

    fun onSelectContactsTab(tabKey: String) {
        applyStateJson(TelefamNativeCore.nativeSetContactsTab(tabKey))
        val uid = _uiState.value.userId
        if (uid.isNotBlank()) {
            viewModelScope.launch { syncContactsSilently(uid) }
        }
    }

    fun onContactsSearchChanged(query: String) {
        applyStateJson(TelefamNativeCore.nativeSetContactsSearch(query))
    }

    fun onToggleFollowContact(contact: ContactItem) {
        val userId = _uiState.value.userId
        if (userId.isBlank()) return
        val wantFollow = !contact.iFollow
        val nowMs = System.currentTimeMillis()
        if (wantFollow) {
            val allowed = TelefamNativeCore.nativeCheckAndRecordFollowRate(nowMs)
            if (!allowed) {
                applyStateJson(TelefamNativeCore.nativeGetUiState())
                return
            }
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                contactsRepository.setOptimisticFollowState(userId, contact.userId, wantFollow)
            }
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
        val (y, m, d) = todayYmd()
        val nowMs = System.currentTimeMillis()
        val editPayload = JSONObject().apply {
            put("fullName", fullName)
            put("username", username)
            put("phoneCountryIso", phoneCountryIso)
            put("phoneNumber", phoneNumber)
            put("gender", gender)
            put("dateOfBirth", dateOfBirth)
            put("location", location)
            put("bio", bio)
        }.toString()

        val ok = TelefamNativeCore.nativeApplyLocalProfileEdit(editPayload, y, m, d, nowMs)
        applyStateJson(TelefamNativeCore.nativeGetUiState())
        if (!ok) {
            return false
        }

        val uid = _uiState.value.userId
        if (uid.isNotBlank()) {
            val serializedProfile = TelefamNativeCore.nativeSerializeOwnerProfile()
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    profileRepository.saveLocalEdit(uid, serializedProfile)
                }
                syncOwnerProfileSilently(uid)
            }
        }
        return true
    }

    fun onUploadBackgroundPhoto(
        uri: Uri,
        cropX: Double,
        cropY: Double,
        cropW: Double,
        cropH: Double
    ) {
        viewModelScope.launch {
            val ctx = getApplication<Application>().applicationContext
            val mime = ctx.contentResolver.getType(uri) ?: "image/jpeg"
            val bytes = withContext(Dispatchers.IO) {
                ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: return@launch

            val actionJson = TelefamNativeCore.nativePrepareUploadBackgroundPhoto(
                mime,
                bytes.size.toLong(),
                cropX,
                cropY,
                cropW,
                cropH,
                bridge.isOnline(),
                System.currentTimeMillis(),
                bridge.randomBytes(16)
            )
            applyStateJson(TelefamNativeCore.nativeGetUiState())
            val action = TelefamNativeCore.parseHttpAction(actionJson) ?: return@launch
            val (code, body, netErr) = withContext(Dispatchers.IO) {
                bridge.executePhotoUpload(action, bytes, mime)
            }
            val nextJson = TelefamNativeCore.nativeOnHttpResult(
                action.requestId,
                code,
                body,
                netErr,
                bridge.isOnline(),
                System.currentTimeMillis(),
                bridge.randomBytes(12)
            )
            applyStateJson(nextJson)

            val uid = _uiState.value.userId
            if (!netErr && code in 200..299 && uid.isNotBlank()) {
                val serialized = TelefamNativeCore.nativeSerializeOwnerProfile()
                withContext(Dispatchers.IO) {
                    profileRepository.saveServerProfile(uid, serialized)
                }
            }
        }
    }

    fun onDeleteBackgroundPhoto(photoId: String) {
        viewModelScope.launch {
            val actionJson = TelefamNativeCore.nativePrepareDeleteBackgroundPhoto(
                photoId,
                bridge.isOnline(),
                System.currentTimeMillis(),
                bridge.randomBytes(16)
            )
            applyStateJson(TelefamNativeCore.nativeGetUiState())
            val action = TelefamNativeCore.parseHttpAction(actionJson) ?: return@launch
            val (code, body, netErr) = withContext(Dispatchers.IO) {
                bridge.executeHttp(action)
            }
            val nextJson = TelefamNativeCore.nativeOnHttpResult(
                action.requestId,
                code,
                body,
                netErr,
                bridge.isOnline(),
                System.currentTimeMillis(),
                bridge.randomBytes(12)
            )
            applyStateJson(nextJson)

            val uid = _uiState.value.userId
            if (!netErr && code in 200..299 && uid.isNotBlank()) {
                val serialized = TelefamNativeCore.nativeSerializeOwnerProfile()
                withContext(Dispatchers.IO) {
                    profileRepository.saveServerProfile(uid, serialized)
                }
            }
        }
    }

    private suspend fun syncOwnerProfileSilently(userId: String) {
        if (!bridge.isOnline() || userId.isBlank()) return
        profileSyncMutex.withLock {
            withContext(Dispatchers.IO) {
                val cached = profileRepository.getCachedProfile(userId)
                if (cached != null && cached.pendingProfileSync && cached.profileJson.isNotBlank()) {
                    val pushActionJson = TelefamNativeCore.nativePrepareSyncEditedProfile(
                        cached.profileJson,
                        bridge.isOnline(),
                        System.currentTimeMillis(),
                        bridge.randomBytes(16)
                    )
                    val pushAction = TelefamNativeCore.parseHttpAction(pushActionJson)
                    if (pushAction != null) {
                        val (code, body, netErr) = bridge.executeHttp(pushAction)
                        val updatedJson = TelefamNativeCore.nativeOnHttpResult(
                            pushAction.requestId,
                            code,
                            body,
                            netErr,
                            bridge.isOnline(),
                            System.currentTimeMillis(),
                            bridge.randomBytes(12)
                        )
                        if (!netErr && code in 200..299) {
                            val freshProfileJson = TelefamNativeCore.nativeSerializeOwnerProfile()
                            profileRepository.markSyncCompleted(userId, freshProfileJson)
                            withContext(Dispatchers.Main) {
                                applyStateJson(updatedJson)
                            }
                        } else if (!netErr && code in 400..499) {
                            profileRepository.clearPendingFlag(userId)
                        } else {
                            return@withContext
                        }
                    }
                }

                val fetchActionJson = TelefamNativeCore.nativePrepareFetchOwnerProfile(
                    bridge.isOnline(),
                    System.currentTimeMillis(),
                    bridge.randomBytes(16)
                )
                val fetchAction = TelefamNativeCore.parseHttpAction(fetchActionJson) ?: return@withContext
                val (code, body, netErr) = bridge.executeHttp(fetchAction)
                val nextStateJson = TelefamNativeCore.nativeOnHttpResult(
                    fetchAction.requestId,
                    code,
                    body,
                    netErr,
                    bridge.isOnline(),
                    System.currentTimeMillis(),
                    bridge.randomBytes(12)
                )
                if (!netErr && code in 200..299) {
                    val freshProfileJson = TelefamNativeCore.nativeSerializeOwnerProfile()
                    profileRepository.saveServerProfile(userId, freshProfileJson)
                    withContext(Dispatchers.Main) {
                        applyStateJson(nextStateJson)
                    }
                } else if (!netErr && code == 401) {
                    refreshSessionSilently()
                }
            }
        }
    }

    private suspend fun refreshSessionSilently(): Boolean {
        if (!bridge.isOnline()) return false
        return refreshMutex.withLock {
            withContext(Dispatchers.IO) {
                val actionJson = TelefamNativeCore.nativePrepareRefreshToken(
                    bridge.isOnline(),
                    System.currentTimeMillis(),
                    bridge.randomBytes(16)
                )
                val action = TelefamNativeCore.parseHttpAction(actionJson) ?: return@withContext false
                val (code, body, netErr) = bridge.executeHttp(action)
                val nextStateJson = TelefamNativeCore.nativeOnHttpResult(
                    action.requestId,
                    code,
                    body,
                    netErr,
                    bridge.isOnline(),
                    System.currentTimeMillis(),
                    bridge.randomBytes(12)
                )
                withContext(Dispatchers.Main) {
                    applyStateJson(nextStateJson)
                }
                !netErr && code in 200..299
            }
        }
    }

    private suspend fun syncContactsSilently(userId: String) {
        if (!bridge.isOnline() || userId.isBlank()) return
        syncMutex.withLock {
            withContext(Dispatchers.IO) {
                val pending = contactsRepository.getPendingMutations(userId)
                for (item in pending) {
                    val follow = item.pendingAction == ContactEntity.PENDING_FOLLOW
                    val actionJson = TelefamNativeCore.nativePrepareFollowAction(
                        targetUserId = item.userId,
                        follow = follow,
                        isOnline = bridge.isOnline(),
                        nowMs = System.currentTimeMillis(),
                        random16 = bridge.randomBytes(16)
                    )
                    val action = TelefamNativeCore.parseHttpAction(actionJson) ?: continue
                    val (code, body, netErr) = bridge.executeHttp(action)
                    val nextStateJson = TelefamNativeCore.nativeOnHttpResult(
                        action.requestId,
                        code,
                        body,
                        netErr,
                        bridge.isOnline(),
                        System.currentTimeMillis(),
                        bridge.randomBytes(12)
                    )
                    if (!netErr && code in 200..299) {
                        val obj = runCatching { JSONObject(body) }.getOrNull()
                        val serverIFollow = obj?.optBoolean("iFollow", follow) ?: follow
                        val serverFollowsMe = obj?.optBoolean("followsMe", item.followsMe) ?: item.followsMe
                        contactsRepository.markMutationSynced(userId, item.userId, serverIFollow, serverFollowsMe)
                    } else if (!netErr && code == 429) {
                        contactsRepository.revertPendingFollowOnRateLimit(userId, item.userId)
                        withContext(Dispatchers.Main) {
                            applyStateJson(nextStateJson)
                        }
                    } else if (!netErr && code in 400..499) {
                        contactsRepository.revertPendingFollowOnRateLimit(userId, item.userId)
                    }
                }

                val fetchActionJson = TelefamNativeCore.nativePrepareFetchContacts(
                    isOnline = bridge.isOnline(),
                    nowMs = System.currentTimeMillis(),
                    random16 = bridge.randomBytes(16)
                )
                val fetchAction = TelefamNativeCore.parseHttpAction(fetchActionJson) ?: return@withContext
                val (code, body, netErr) = bridge.executeHttp(fetchAction)
                TelefamNativeCore.nativeOnHttpResult(
                    fetchAction.requestId,
                    code,
                    body,
                    netErr,
                    bridge.isOnline(),
                    System.currentTimeMillis(),
                    bridge.randomBytes(12)
                )
                if (!netErr && code in 200..299) {
                    val normalizedJson = TelefamNativeCore.nativeParseContactsSyncPayload(body)
                    val remoteContacts = TelefamNativeCore.parseContactsList(normalizedJson)
                    contactsRepository.mergeServerDirectory(userId, remoteContacts)
                }
            }
        }
    }

    fun onSendOtp() {
        val actionJson = TelefamNativeCore.nativePrepareSendOtp(
            bridge.isOnline(),
            System.currentTimeMillis(),
            bridge.randomBytes(16)
        )
        applyStateJson(TelefamNativeCore.nativeGetUiState())
        dispatchHttp(actionJson)
    }

    fun onVerifyOtp() {
        val actionJson = TelefamNativeCore.nativePrepareVerifyOtp(
            bridge.isOnline(),
            System.currentTimeMillis(),
            bridge.randomBytes(16)
        )
        applyStateJson(TelefamNativeCore.nativeGetUiState())
        dispatchHttp(actionJson)
    }

    fun onContinueWithGoogle(activityContext: Context) {
        if (!bridge.isOnline()) {
            onSendOtp()
            return
        }
        viewModelScope.launch {
            val nonce = bridge.generateUrlNonce()
            val tokenRes = bridge.requestGoogleIdToken(activityContext, nonce)
            tokenRes.onSuccess { idToken ->
                val actionJson = TelefamNativeCore.nativePrepareGoogleAuth(
                    idToken,
                    nonce,
                    bridge.isOnline(),
                    System.currentTimeMillis(),
                    bridge.randomBytes(16)
                )
                applyStateJson(TelefamNativeCore.nativeGetUiState())
                dispatchHttp(actionJson)
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Google Sign-In unavailable"
                applyStateJson(TelefamNativeCore.nativeSetError(msg))
            }
        }
    }

    fun onProfileFieldChanged(field: String, value: String) {
        val (y, m, d) = todayYmd()
        applyStateJson(TelefamNativeCore.nativeUpdateProfileField(field, value, y, m, d))
        if (field == "username") {
            onCheckUsername(_uiState.value.profile.username)
        }
    }

    fun onCheckUsername(username: String) {
        usernameCheckJob?.cancel()
        usernameCheckJob = viewModelScope.launch {
            delay(450L)
            val actionJson = TelefamNativeCore.nativePrepareCheckUsername(
                username,
                bridge.isOnline(),
                System.currentTimeMillis(),
                bridge.randomBytes(16)
            )
            applyStateJson(TelefamNativeCore.nativeGetUiState())
            dispatchHttp(actionJson)
        }
    }

    fun onUploadCroppedPhoto(
        uri: Uri,
        cropX: Double,
        cropY: Double,
        cropW: Double,
        cropH: Double
    ) {
        viewModelScope.launch {
            val ctx = getApplication<Application>().applicationContext
            val mime = ctx.contentResolver.getType(uri) ?: "image/jpeg"
            val bytes = withContext(Dispatchers.IO) {
                ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: return@launch

            val actionJson = TelefamNativeCore.nativePrepareUploadPhoto(
                mime,
                bytes.size.toLong(),
                cropX,
                cropY,
                cropW,
                cropH,
                bridge.isOnline(),
                System.currentTimeMillis(),
                bridge.randomBytes(16)
            )
            applyStateJson(TelefamNativeCore.nativeGetUiState())
            val action = TelefamNativeCore.parseHttpAction(actionJson) ?: return@launch
            val (code, body, netErr) = withContext(Dispatchers.IO) {
                bridge.executePhotoUpload(action, bytes, mime)
            }
            val nextJson = TelefamNativeCore.nativeOnHttpResult(
                action.requestId,
                code,
                body,
                netErr,
                bridge.isOnline(),
                System.currentTimeMillis(),
                bridge.randomBytes(12)
            )
            applyStateJson(nextJson)
            val uid = _uiState.value.userId
            if (!netErr && code in 200..299 && uid.isNotBlank()) {
                val serialized = TelefamNativeCore.nativeSerializeOwnerProfile()
                withContext(Dispatchers.IO) {
                    profileRepository.saveServerProfile(uid, serialized)
                }
            }
        }
    }

    fun onSubmitProfile() {
        val (y, m, d) = todayYmd()
        val actionJson = TelefamNativeCore.nativePrepareSubmitProfile(
            y,
            m,
            d,
            bridge.isOnline(),
            System.currentTimeMillis(),
            bridge.randomBytes(16)
        )
        applyStateJson(TelefamNativeCore.nativeGetUiState())
        dispatchHttp(actionJson)
    }

    fun onLogout() {
        val logoutAction = TelefamNativeCore.parseHttpAction(
            TelefamNativeCore.nativePrepareLogout(
                bridge.isOnline(),
                System.currentTimeMillis(),
                bridge.randomBytes(16)
            )
        )
        viewModelScope.launch(Dispatchers.IO) {
            if (logoutAction != null) {
                bridge.executeHttp(logoutAction)
            }
            contactsRepository.clearAll()
            profileRepository.clearAll()
        }
        applyStateJson(TelefamNativeCore.nativeLogout(System.currentTimeMillis()))
    }

    private fun dispatchHttp(actionJson: String) {
        val action = TelefamNativeCore.parseHttpAction(actionJson) ?: return
        viewModelScope.launch {
            val (code, body, netErr) = withContext(Dispatchers.IO) {
                bridge.executeHttp(action)
            }
            val nextJson = TelefamNativeCore.nativeOnHttpResult(
                action.requestId,
                code,
                body,
                netErr,
                bridge.isOnline(),
                System.currentTimeMillis(),
                bridge.randomBytes(12)
            )
            applyStateJson(nextJson)
        }
    }
}
