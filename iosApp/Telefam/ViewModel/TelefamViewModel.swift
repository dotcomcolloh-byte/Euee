import Combine
import Foundation
import SwiftUI

@MainActor
final class TelefamViewModel: ObservableObject {
    @Published private(set) var uiState = TelefamUiState()
    @Published private(set) var filteredContacts: [ContactItem] = []
    @Published private(set) var allCachedContacts: [ContactItem] = []

    let bridge: TelefamCoreBridge
    let countries: [CountryOption]
    private let appleSignIn = AppleSignInCoordinator()

    private struct CachedContactRecord: Codable {
        var contact: ContactItem
        var pendingAction: String
    }

    private struct PendingProfileSyncEnvelope: Codable {
        var profileJson: String
        var pendingSync: Bool
    }

    private var cachedContactRecords: [CachedContactRecord] = []
    private var pendingProfileSync = false
    private var lastSyncedUserId = ""

    private var tickerCancellable: AnyCancellable?
    private var syncLoopTask: Task<Void, Never>?
    private var usernameCheckTask: Task<Void, Never>?
    private var isSyncingContacts = false
    private var isSyncingProfile = false
    private var isRefreshingSession = false

    init(bridge: TelefamCoreBridge = TelefamCoreBridge()) {
        self.bridge = bridge
        let loadedCountries = bridge.getCountries()
        self.countries = loadedCountries.isEmpty
            ? [CountryOption(iso2: "KE", name: "Kenya", dialCode: "+254", placeholder: "712345678")]
            : loadedCountries

        self.uiState = bridge.getUiState()
        hydrateFromEncryptedCacheIfSignedIn(userId: uiState.userId)
        recomputeFilteredContacts()

        bridge.onConnectivityChanged = { [weak self] online in
            guard let self else { return }
            Task { @MainActor in
                let s = self.uiState
                if online && s.screen == .home && !s.userId.isEmpty {
                    await self.syncOwnerProfileSilently(userId: s.userId)
                    await self.syncContactsSilently(userId: s.userId)
                }
            }
        }

        startTicker()
        startSilentBackgroundSyncLoop()
    }

    deinit {
        tickerCancellable?.cancel()
        syncLoopTask?.cancel()
        usernameCheckTask?.cancel()
    }

    // MARK: - Loops

    private func startTicker() {
        tickerCancellable = Timer.publish(every: 1.0, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                guard let self else { return }
                let s = self.uiState
                if s.resendRemainingSec > 0 || s.lockRemainingSec > 0 {
                    self.applyEnvelope(self.bridge.tick())
                }
            }
    }

    private func startSilentBackgroundSyncLoop() {
        syncLoopTask = Task { [weak self] in
            while let self, !Task.isCancelled {
                let s = self.uiState
                if s.screen == .home && !s.userId.isEmpty && self.bridge.isOnline {
                    await self.syncOwnerProfileSilently(userId: s.userId)
                    await self.syncContactsSilently(userId: s.userId)
                }
                try? await Task.sleep(nanoseconds: 25_000_000_000)
            }
        }
    }

    private func hydrateFromEncryptedCacheIfSignedIn(userId: String) {
        guard !userId.isEmpty else { return }
        lastSyncedUserId = userId

        if let rawProfileCache = bridge.loadEncryptedCache(fileName: "owner_profile_cache.enc", userId: userId) {
            if let envelope = try? JSONDecoder().decode(PendingProfileSyncEnvelope.self, from: Data(rawProfileCache.utf8)) {
                pendingProfileSync = envelope.pendingSync
                applyEnvelope(bridge.applyCachedOwnerProfile(envelope.profileJson))
            } else {
                applyEnvelope(bridge.applyCachedOwnerProfile(rawProfileCache))
            }
        }

        if let rawContacts = bridge.loadEncryptedCache(fileName: "contacts_cache.enc", userId: userId),
           let records = try? JSONDecoder().decode([CachedContactRecord].self, from: Data(rawContacts.utf8)) {
            cachedContactRecords = records
        } else {
            cachedContactRecords = []
        }
        recomputeFilteredContacts()

        if bridge.isOnline && uiState.screen == .home {
            Task {
                await syncOwnerProfileSilently(userId: userId)
                await syncContactsSilently(userId: userId)
            }
        }
    }

    // MARK: - Auth & Navigation Intents

    func onEmailChanged(_ email: String) {
        applyEnvelope(bridge.setEmail(email))
    }

    func onOtpChanged(_ otp: String) {
        applyEnvelope(bridge.setOtp(otp))
    }

    func onChangeEmail() {
        applyEnvelope(bridge.goBackToEmail())
    }

    func onSendOtp() {
        Task {
            let env = bridge.prepareSendOtp()
            await dispatchPreparedEnvelope(env)
        }
    }

    func onVerifyOtp() {
        Task {
            let env = bridge.prepareVerifyOtp()
            await dispatchPreparedEnvelope(env)
        }
    }

    func onContinueWithApple() {
        Task {
            guard bridge.isOnline else {
                applyEnvelope(bridge.setError(TelefamStrings.noInternet))
                return
            }
            do {
                let creds = try await appleSignIn.signIn()
                let env = bridge.prepareAppleAuth(credentials: creds)
                await dispatchPreparedEnvelope(env)
            } catch AppleSignInCoordinator.AppleSignInError.cancelled {
                return
            } catch {
                applyEnvelope(bridge.setError("Apple Sign-In unavailable"))
            }
        }
    }

    func onCycleTheme() {
        applyEnvelope(bridge.cycleTheme())
    }

    func onComingSoon(_ label: String) {
        applyEnvelope(bridge.comingSoon(label))
    }

    func onClearError() {
        applyEnvelope(bridge.clearError())
    }

    func onClearNotice() {
        applyEnvelope(bridge.clearNotice())
    }

    func onShowNotice(_ message: String) {
        uiState.noticeMessage = message
    }

    func onSelectHomeTab(_ tab: HomeTab) {
        let key: String
        switch tab {
        case .chats: key = "chats"
        case .contacts: key = "contacts"
        case .profile: key = "profile"
        }
        applyEnvelope(bridge.setHomeTab(key))
        let uid = uiState.userId
        guard !uid.isEmpty else { return }
        Task {
            switch tab {
            case .contacts:
                await syncContactsSilently(userId: uid)
            case .profile:
                await syncOwnerProfileSilently(userId: uid)
                await syncContactsSilently(userId: uid)
            case .chats:
                break
            }
        }
    }

    func onSelectProfileContentTab(_ tabKey: String) {
        applyEnvelope(bridge.setProfileContentTab(tabKey))
    }

    func onSelectContactsTab(_ tabKey: String) {
        applyEnvelope(bridge.setContactsTab(tabKey))
        recomputeFilteredContacts()
        let uid = uiState.userId
        if !uid.isEmpty {
            Task { await syncContactsSilently(userId: uid) }
        }
    }

    func onContactsSearchChanged(_ query: String) {
        applyEnvelope(bridge.setContactsSearch(query))
        recomputeFilteredContacts()
    }

    // MARK: - Profile Setup & Editing

    func onProfileFieldChanged(field: String, value: String) {
        applyEnvelope(bridge.updateProfileField(field: field, value: value))
        if field == "username" {
            onCheckUsername(uiState.profile.username)
        }
    }

    func onCheckUsername(_ username: String) {
        usernameCheckTask?.cancel()
        usernameCheckTask = Task {
            try? await Task.sleep(nanoseconds: 450_000_000)
            guard !Task.isCancelled else { return }
            let env = bridge.prepareCheckUsername(username)
            await dispatchPreparedEnvelope(env)
        }
    }

    func onUploadCroppedAvatar(data: Data, mimeType: String, cropX: Double, cropY: Double, cropW: Double, cropH: Double) {
        Task {
            let env = bridge.prepareUploadPhoto(
                mimeType: mimeType,
                byteSize: Int64(data.count),
                cropX: cropX,
                cropY: cropY,
                cropW: cropW,
                cropH: cropH
            )
            applyEnvelope(env)
            guard let action = env?.httpAction else { return }
            let result = await bridge.executeMultipartUpload(
                action,
                imageBytes: data,
                mimeType: mimeType,
                cropX: cropX,
                cropY: cropY,
                cropW: cropW,
                cropH: cropH
            )
            let next = bridge.onHttpResult(
                requestId: action.requestId,
                statusCode: result.statusCode,
                responseBody: result.body,
                networkError: result.networkError
            )
            applyEnvelope(next)
            let uid = uiState.userId
            if !result.networkError && (200..<300).contains(result.statusCode) && !uid.isEmpty {
                saveProfileCache(userId: uid, profileJson: bridge.serializeOwnerProfile(), pendingSync: false)
            }
        }
    }

    func onUploadBackgroundPhoto(data: Data, mimeType: String, cropX: Double, cropY: Double, cropW: Double, cropH: Double) {
        Task {
            let env = bridge.prepareUploadBackgroundPhoto(
                mimeType: mimeType,
                byteSize: Int64(data.count),
                cropX: cropX,
                cropY: cropY,
                cropW: cropW,
                cropH: cropH
            )
            applyEnvelope(env)
            guard let action = env?.httpAction else { return }
            let result = await bridge.executeMultipartUpload(
                action,
                imageBytes: data,
                mimeType: mimeType,
                cropX: cropX,
                cropY: cropY,
                cropW: cropW,
                cropH: cropH
            )
            let next = bridge.onHttpResult(
                requestId: action.requestId,
                statusCode: result.statusCode,
                responseBody: result.body,
                networkError: result.networkError
            )
            applyEnvelope(next)
            let uid = uiState.userId
            if !result.networkError && (200..<300).contains(result.statusCode) && !uid.isEmpty {
                saveProfileCache(userId: uid, profileJson: bridge.serializeOwnerProfile(), pendingSync: false)
            }
        }
    }

    func onDeleteBackgroundPhoto(_ photoId: String) {
        Task {
            let env = bridge.prepareDeleteBackgroundPhoto(photoId)
            applyEnvelope(env)
            guard let action = env?.httpAction else { return }
            let result = await bridge.executeJsonRequest(action)
            let next = bridge.onHttpResult(
                requestId: action.requestId,
                statusCode: result.statusCode,
                responseBody: result.body,
                networkError: result.networkError
            )
            applyEnvelope(next)
            let uid = uiState.userId
            if !result.networkError && (200..<300).contains(result.statusCode) && !uid.isEmpty {
                saveProfileCache(userId: uid, profileJson: bridge.serializeOwnerProfile(), pendingSync: false)
            }
        }
    }

    func onSubmitProfile() {
        Task {
            let env = bridge.prepareSubmitProfile()
            await dispatchPreparedEnvelope(env)
        }
    }

    func onSaveEditedProfile(
        fullName: String,
        username: String,
        phoneCountryIso: String,
        phoneNumber: String,
        gender: String,
        dateOfBirth: String,
        location: String,
        bio: String
    ) -> Bool {
        let payloadDict: [String: String] = [
            "fullName": fullName,
            "username": username,
            "phoneCountryIso": phoneCountryIso,
            "phoneNumber": phoneNumber,
            "gender": gender,
            "dateOfBirth": dateOfBirth,
            "location": location,
            "bio": bio
        ]
        guard let payloadData = try? JSONSerialization.data(withJSONObject: payloadDict),
              let payloadJson = String(data: payloadData, encoding: .utf8) else {
            return false
        }
        let ok = bridge.applyLocalProfileEdit(payloadJson)
        uiState = bridge.getUiState()
        guard ok else { return false }

        let uid = uiState.userId
        if !uid.isEmpty {
            let snapshot = bridge.serializeOwnerProfile()
            saveProfileCache(userId: uid, profileJson: snapshot, pendingSync: true)
            Task { await syncOwnerProfileSilently(userId: uid) }
        }
        return true
    }

    // MARK: - Contacts Follow & Offline Sync

    func onToggleFollowContact(_ contact: ContactItem) {
        let uid = uiState.userId
        guard !uid.isEmpty else { return }
        let wantFollow = !contact.iFollow
        if wantFollow && !bridge.checkAndRecordFollowRate() {
            uiState = bridge.getUiState()
            return
        }
        cachedContactRecords = cachedContactRecords.map { rec in
            guard rec.contact.userId == contact.userId else { return rec }
            let updated = rec.contact.withFollowState(
                iFollow: wantFollow,
                isFriend: wantFollow && rec.contact.followsMe
            )
            return CachedContactRecord(contact: updated, pendingAction: wantFollow ? "FOLLOW" : "UNFOLLOW")
        }
        saveContactsCache(userId: uid)
        recomputeFilteredContacts()

        Task {
            await syncContactsSilently(userId: uid)
            await syncOwnerProfileSilently(userId: uid)
        }
    }

    func onLogout() {
        let uid = uiState.userId
        cachedContactRecords = []
        filteredContacts = []
        allCachedContacts = []
        pendingProfileSync = false
        lastSyncedUserId = ""
        if !uid.isEmpty {
            bridge.clearEncryptedUserCaches(userId: uid)
        }
        applyEnvelope(bridge.logoutLocal())
    }

    // MARK: - Private Sync Helpers

    private func dispatchPreparedEnvelope(_ envelope: NativeActionEnvelope?) async {
        applyEnvelope(envelope)
        guard let action = envelope?.httpAction else { return }
        let result = await bridge.executeJsonRequest(action)
        let next = bridge.onHttpResult(
            requestId: action.requestId,
            statusCode: result.statusCode,
            responseBody: result.body,
            networkError: result.networkError
        )
        applyEnvelope(next)
    }

    private func applyEnvelope(_ envelope: NativeActionEnvelope?) {
        guard let envelope else { return }
        let prevHome = (uiState.screen == .home)
        if let state = envelope.state {
            uiState = state
        } else {
            uiState = bridge.getUiState()
        }
        if !uiState.sealedVaultB64.isEmpty {
            bridge.saveSealedVaultB64(uiState.sealedVaultB64)
        }
        if uiState.userId != lastSyncedUserId && !uiState.userId.isEmpty {
            hydrateFromEncryptedCacheIfSignedIn(userId: uiState.userId)
        } else if !prevHome && uiState.screen == .home && !uiState.userId.isEmpty {
            let uid = uiState.userId
            saveProfileCache(userId: uid, profileJson: bridge.serializeOwnerProfile(), pendingSync: false)
            Task {
                await syncOwnerProfileSilently(userId: uid)
                await syncContactsSilently(userId: uid)
            }
        }
    }

    private func syncOwnerProfileSilently(userId: String) async {
        guard !userId.isEmpty, bridge.isOnline, !isSyncingProfile else { return }
        isSyncingProfile = true
        defer { isSyncingProfile = false }

        if pendingProfileSync {
            let snapshot = bridge.serializeOwnerProfile()
            if let pushAction = bridge.prepareSyncEditedProfile(snapshot)?.httpAction {
                let res = await bridge.executeJsonRequest(pushAction)
                let next = bridge.onHttpResult(
                    requestId: pushAction.requestId,
                    statusCode: res.statusCode,
                    responseBody: res.body,
                    networkError: res.networkError
                )
                if !res.networkError && (200..<300).contains(res.statusCode) {
                    applyEnvelope(next)
                    saveProfileCache(userId: userId, profileJson: bridge.serializeOwnerProfile(), pendingSync: false)
                } else if !res.networkError && (400..<500).contains(res.statusCode) {
                    pendingProfileSync = false
                    saveProfileCache(userId: userId, profileJson: snapshot, pendingSync: false)
                } else {
                    return
                }
            }
        }

        if let fetchAction = bridge.prepareFetchOwnerProfile()?.httpAction {
            let res = await bridge.executeJsonRequest(fetchAction)
            let next = bridge.onHttpResult(
                requestId: fetchAction.requestId,
                statusCode: res.statusCode,
                responseBody: res.body,
                networkError: res.networkError
            )
            if !res.networkError && (200..<300).contains(res.statusCode) {
                applyEnvelope(next)
                saveProfileCache(userId: userId, profileJson: bridge.serializeOwnerProfile(), pendingSync: false)
            } else if !res.networkError && res.statusCode == 401 {
                await refreshSessionSilently()
            }
        }
    }

    private func refreshSessionSilently() async {
        guard bridge.isOnline, !isRefreshingSession else { return }
        isRefreshingSession = true
        defer { isRefreshingSession = false }
        guard let action = bridge.prepareRefreshToken()?.httpAction else { return }
        let res = await bridge.executeJsonRequest(action)
        let next = bridge.onHttpResult(
            requestId: action.requestId,
            statusCode: res.statusCode,
            responseBody: res.body,
            networkError: res.networkError
        )
        applyEnvelope(next)
    }

    private func syncContactsSilently(userId: String) async {
        guard !userId.isEmpty, bridge.isOnline, !isSyncingContacts else { return }
        isSyncingContacts = true
        defer { isSyncingContacts = false }

        let pending = cachedContactRecords.filter { !$0.pendingAction.isEmpty }
        for item in pending {
            let follow = (item.pendingAction == "FOLLOW")
            guard let action = bridge.prepareFollowAction(targetUserId: item.contact.userId, follow: follow)?.httpAction else {
                continue
            }
            let res = await bridge.executeJsonRequest(action)
            let next = bridge.onHttpResult(
                requestId: action.requestId,
                statusCode: res.statusCode,
                responseBody: res.body,
                networkError: res.networkError
            )
            if !res.networkError && (200..<300).contains(res.statusCode) {
                let parsed = parseFollowResponse(body: res.body, defaultFollow: follow, defaultFollowsMe: item.contact.followsMe)
                cachedContactRecords = cachedContactRecords.map { rec in
                    guard rec.contact.userId == item.contact.userId else { return rec }
                    return CachedContactRecord(
                        contact: ContactItem(
                            userId: rec.contact.userId,
                            fullName: rec.contact.fullName,
                            username: rec.contact.username,
                            countryIso: rec.contact.countryIso,
                            countryName: rec.contact.countryName,
                            avatarUrl: rec.contact.avatarUrl,
                            iFollow: parsed.iFollow,
                            followsMe: parsed.followsMe,
                            isFriend: parsed.iFollow && parsed.followsMe
                        ),
                        pendingAction: ""
                    )
                }
            } else if !res.networkError && res.statusCode == 429 {
                revertPendingFollow(targetId: item.contact.userId)
                applyEnvelope(next)
            } else if !res.networkError && (400..<500).contains(res.statusCode) {
                revertPendingFollow(targetId: item.contact.userId)
            }
        }
        saveContactsCache(userId: userId)
        recomputeFilteredContacts()

        if let fetchAction = bridge.prepareFetchContacts()?.httpAction {
            let res = await bridge.executeJsonRequest(fetchAction)
            _ = bridge.onHttpResult(
                requestId: fetchAction.requestId,
                statusCode: res.statusCode,
                responseBody: res.body,
                networkError: res.networkError
            )
            if !res.networkError && (200..<300).contains(res.statusCode),
               let remote = bridge.parseContactsSync(res.body) {
                let pendingMap = Dictionary(
                    uniqueKeysWithValues: cachedContactRecords
                        .filter { !$0.pendingAction.isEmpty }
                        .map { ($0.contact.userId, $0) }
                )
                cachedContactRecords = remote.map { item in
                    pendingMap[item.userId] ?? CachedContactRecord(contact: item, pendingAction: "")
                }
                saveContactsCache(userId: userId)
                recomputeFilteredContacts()
            }
        }
    }

    private func revertPendingFollow(targetId: String) {
        cachedContactRecords = cachedContactRecords.map { rec in
            guard rec.contact.userId == targetId else { return rec }
            let reverted = rec.pendingAction != "FOLLOW"
            return CachedContactRecord(
                contact: rec.contact.withFollowState(
                    iFollow: reverted,
                    isFriend: reverted && rec.contact.followsMe
                ),
                pendingAction: ""
            )
        }
    }

    private func parseFollowResponse(body: String, defaultFollow: Bool, defaultFollowsMe: Bool) -> (iFollow: Bool, followsMe: Bool) {
        guard let data = body.data(using: .utf8),
              let dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return (defaultFollow, defaultFollowsMe)
        }
        let iFollow = (dict["iFollow"] as? Bool) ?? defaultFollow
        let followsMe = (dict["followsMe"] as? Bool) ?? defaultFollowsMe
        return (iFollow, followsMe)
    }

    private func recomputeFilteredContacts() {
        let raw = cachedContactRecords.map { $0.contact }
        allCachedContacts = raw
        filteredContacts = bridge.filterContacts(raw)
    }

    private func saveProfileCache(userId: String, profileJson: String, pendingSync: Bool) {
        self.pendingProfileSync = pendingSync
        let envelope = PendingProfileSyncEnvelope(profileJson: profileJson, pendingSync: pendingSync)
        if let data = try? JSONEncoder().encode(envelope),
           let str = String(data: data, encoding: .utf8) {
            bridge.saveEncryptedCache(fileName: "owner_profile_cache.enc", userId: userId, plaintext: str)
        }
    }

    private func saveContactsCache(userId: String) {
        if let data = try? JSONEncoder().encode(cachedContactRecords),
           let str = String(data: data, encoding: .utf8) {
            bridge.saveEncryptedCache(fileName: "contacts_cache.enc", userId: userId, plaintext: str)
        }
    }
}
