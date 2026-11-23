import CryptoKit
import Foundation
import Network
import Security
import UIKit

final class TelefamCoreBridge: NSObject, URLSessionDelegate {
    private let engine: TelefamEngineRef
    private let decoder = JSONDecoder()
    private let encoder = JSONEncoder()
    private let stateLock = NSLock()

    private let baseUrl: String
    private let isDebugBuild: Bool
    private let tlsPinSha256: String
    private let masterKeyData: Data
    private let deviceIdHash: String
    private let devicePublicKeyB64: String

    private let pathMonitor = NWPathMonitor()
    private let monitorQueue = DispatchQueue(label: "com.telefam.company.network")
    private(set) var isOnline: Bool = true
    var onConnectivityChanged: ((Bool) -> Void)?

    private lazy var pinnedSession: URLSession = {
        let config = URLSessionConfiguration.ephemeral
        config.timeoutIntervalForRequest = 20
        config.timeoutIntervalForResource = 45
        config.waitsForConnectivity = false
        config.requestCachePolicy = .reloadIgnoringLocalCacheData
        return URLSession(configuration: config, delegate: self, delegateQueue: nil)
    }()

    override init() {
        guard let created = telefam_engine_create() else {
            fatalError("Failed to initialize Telefam C++17 core engine")
        }
        self.engine = created

        #if DEBUG
        self.isDebugBuild = true
        #else
        self.isDebugBuild = false
        #endif

        let info = Bundle.main.infoDictionary ?? [:]
        let configuredUrl = (info["TELEFAM_API_BASE_URL"] as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if !configuredUrl.isEmpty && !configuredUrl.contains("$(") {
            self.baseUrl = configuredUrl.hasSuffix("/") ? String(configuredUrl.dropLast()) : configuredUrl
        } else {
            self.baseUrl = "https://api.telefam.company"
        }

        let configuredPin = (info["TELEFAM_TLS_PIN_SHA256"] as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        self.tlsPinSha256 = (configuredPin.contains("$(") || configuredPin.caseInsensitiveCompare("NONE") == .orderedSame) ? "" : configuredPin

        let masterKey = Self.loadOrCreateKeychainMasterKey()
        self.masterKeyData = masterKey

        let pubKeyData = Self.loadOrCreateDeviceSigningPublicKey()
        self.devicePublicKeyB64 = pubKeyData.base64EncodedString()
        self.deviceIdHash = Self.sha256Hex(pubKeyData)

        super.init()

        startNetworkMonitor()
        _ = initializeCoreEngine()
    }

    deinit {
        pathMonitor.cancel()
        telefam_engine_destroy(engine)
    }

    private func initializeCoreEngine() -> NativeActionEnvelope? {
        let hardwareName = Self.hardwareDeviceName()
        let sealedVault = loadSealedVaultB64()
        let nowMs = Self.nowEpochMs()
        let certHash = Self.bundleExecutableSha256()

        return consumeJsonAction {
            masterKeyData.withUnsafeBytes { keyPtr in
                let ptr = keyPtr.bindMemory(to: UInt8.self).baseAddress
                return telefam_init(
                    engine,
                    baseUrl,
                    isDebugBuild,
                    hardwareName,
                    deviceIdHash,
                    devicePublicKeyB64,
                    certHash,
                    certHash,
                    ptr,
                    masterKeyData.count,
                    sealedVault,
                    nowMs
                )
            }
        }
    }

    func getUiState() -> TelefamUiState {
        stateLock.lock()
        defer { stateLock.unlock() }
        guard let cStr = telefam_get_ui_state(engine) else { return TelefamUiState() }
        defer { telefam_free_string(cStr) }
        let str = String(cString: cStr)
        return (try? decoder.decode(TelefamUiState.self, from: Data(str.utf8))) ?? TelefamUiState()
    }

    func getCountries() -> [CountryOption] {
        stateLock.lock()
        defer { stateLock.unlock() }
        guard let cStr = telefam_get_countries(engine) else { return [] }
        defer { telefam_free_string(cStr) }
        let str = String(cString: cStr)
        return (try? decoder.decode([CountryOption].self, from: Data(str.utf8))) ?? []
    }

    func setEmail(_ email: String) -> NativeActionEnvelope? {
        consumeJsonAction { telefam_set_email(engine, email) }
    }

    func setOtp(_ otp: String) -> NativeActionEnvelope? {
        consumeJsonAction { telefam_set_otp(engine, otp) }
    }

    func goBackToEmail() -> NativeActionEnvelope? {
        consumeJsonAction { telefam_go_back_to_email(engine) }
    }

    func updateProfileField(field: String, value: String) -> NativeActionEnvelope? {
        let cal = Self.currentDateComponents()
        return consumeJsonAction {
            telefam_update_profile_field(engine, field, value, Int32(cal.year), Int32(cal.month), Int32(cal.day))
        }
    }

    func setHomeTab(_ tab: String) -> NativeActionEnvelope? {
        consumeJsonAction { telefam_set_home_tab(engine, tab) }
    }

    func setProfileContentTab(_ tab: String) -> NativeActionEnvelope? {
        consumeJsonAction { telefam_set_profile_content_tab(engine, tab) }
    }

    func setContactsTab(_ tab: String) -> NativeActionEnvelope? {
        consumeJsonAction { telefam_set_contacts_tab(engine, tab) }
    }

    func setContactsSearch(_ query: String) -> NativeActionEnvelope? {
        consumeJsonAction { telefam_set_contacts_search(engine, query) }
    }

    func checkAndRecordFollowRate() -> Bool {
        stateLock.lock()
        defer { stateLock.unlock() }
        return telefam_check_and_record_follow_rate(engine, Self.nowEpochMs())
    }

    func applyLocalProfileEdit(_ editJson: String) -> Bool {
        let cal = Self.currentDateComponents()
        stateLock.lock()
        defer { stateLock.unlock() }
        return telefam_apply_local_profile_edit(
            engine,
            editJson,
            Int32(cal.year),
            Int32(cal.month),
            Int32(cal.day),
            Self.nowEpochMs()
        )
    }

    func applyCachedOwnerProfile(_ cachedJson: String) -> NativeActionEnvelope? {
        consumeJsonAction {
            telefam_apply_cached_owner_profile(engine, cachedJson, Self.nowEpochMs())
        }
    }

    func serializeOwnerProfile() -> String {
        stateLock.lock()
        defer { stateLock.unlock() }
        guard let cStr = telefam_serialize_owner_profile(engine) else { return "{}" }
        defer { telefam_free_string(cStr) }
        return String(cString: cStr)
    }

    func tick() -> NativeActionEnvelope? {
        consumeJsonAction { telefam_tick(engine, Self.nowEpochMs()) }
    }

    func cycleTheme() -> NativeActionEnvelope? {
        consumeJsonAction { telefam_cycle_theme(engine) }
    }

    func comingSoon(_ label: String) -> NativeActionEnvelope? {
        consumeJsonAction { telefam_coming_soon(engine, label) }
    }

    func setError(_ rawError: String) -> NativeActionEnvelope? {
        consumeJsonAction { telefam_set_error(engine, rawError) }
    }

    func clearError() -> NativeActionEnvelope? {
        consumeJsonAction { telefam_clear_error(engine) }
    }

    func clearNotice() -> NativeActionEnvelope? {
        consumeJsonAction { telefam_clear_notice(engine) }
    }

    func logoutLocal() -> NativeActionEnvelope? {
        consumeJsonAction { telefam_logout(engine, Self.nowEpochMs()) }
    }

    func prepareSendOtp() -> NativeActionEnvelope? {
        let rnd = Self.randomBytes(16)
        return consumeJsonAction {
            rnd.withUnsafeBytes { ptr in
                telefam_prepare_send_otp(engine, isOnline, Self.nowEpochMs(), ptr.bindMemory(to: UInt8.self).baseAddress, rnd.count)
            }
        }
    }

    func prepareVerifyOtp() -> NativeActionEnvelope? {
        let rnd = Self.randomBytes(16)
        return consumeJsonAction {
            rnd.withUnsafeBytes { ptr in
                telefam_prepare_verify_otp(engine, isOnline, Self.nowEpochMs(), ptr.bindMemory(to: UInt8.self).baseAddress, rnd.count)
            }
        }
    }

    func prepareAppleAuth(credentials: AppleAuthCredentials) -> NativeActionEnvelope? {
        let rnd = Self.randomBytes(16)
        return consumeJsonAction {
            rnd.withUnsafeBytes { ptr in
                telefam_prepare_apple_auth(
                    engine,
                    credentials.identityToken,
                    credentials.authorizationCode,
                    credentials.rawNonce,
                    credentials.fullName,
                    credentials.emailHint,
                    isOnline,
                    Self.nowEpochMs(),
                    ptr.bindMemory(to: UInt8.self).baseAddress,
                    rnd.count
                )
            }
        }
    }

    func prepareCheckUsername(_ username: String) -> NativeActionEnvelope? {
        let rnd = Self.randomBytes(16)
        return consumeJsonAction {
            rnd.withUnsafeBytes { ptr in
                telefam_prepare_check_username(engine, username, isOnline, Self.nowEpochMs(), ptr.bindMemory(to: UInt8.self).baseAddress, rnd.count)
            }
        }
    }

    func prepareUploadPhoto(mimeType: String, byteSize: Int64, cropX: Double, cropY: Double, cropW: Double, cropH: Double) -> NativeActionEnvelope? {
        let rnd = Self.randomBytes(16)
        return consumeJsonAction {
            rnd.withUnsafeBytes { ptr in
                telefam_prepare_upload_photo(
                    engine,
                    mimeType,
                    byteSize,
                    cropX,
                    cropY,
                    cropW,
                    cropH,
                    isOnline,
                    Self.nowEpochMs(),
                    ptr.bindMemory(to: UInt8.self).baseAddress,
                    rnd.count
                )
            }
        }
    }

    func prepareSubmitProfile() -> NativeActionEnvelope? {
        let cal = Self.currentDateComponents()
        let rnd = Self.randomBytes(16)
        return consumeJsonAction {
            rnd.withUnsafeBytes { ptr in
                telefam_prepare_submit_profile(
                    engine,
                    Int32(cal.year),
                    Int32(cal.month),
                    Int32(cal.day),
                    isOnline,
                    Self.nowEpochMs(),
                    ptr.bindMemory(to: UInt8.self).baseAddress,
                    rnd.count
                )
            }
        }
    }

    func prepareFetchOwnerProfile() -> NativeActionEnvelope? {
        let rnd = Self.randomBytes(16)
        return consumeJsonAction {
            rnd.withUnsafeBytes { ptr in
                telefam_prepare_fetch_owner_profile(engine, isOnline, Self.nowEpochMs(), ptr.bindMemory(to: UInt8.self).baseAddress, rnd.count)
            }
        }
    }

    func prepareSyncEditedProfile(_ profileJson: String) -> NativeActionEnvelope? {
        let rnd = Self.randomBytes(16)
        return consumeJsonAction {
            rnd.withUnsafeBytes { ptr in
                telefam_prepare_sync_edited_profile(engine, profileJson, isOnline, Self.nowEpochMs(), ptr.bindMemory(to: UInt8.self).baseAddress, rnd.count)
            }
        }
    }

    func prepareUploadBackgroundPhoto(mimeType: String, byteSize: Int64, cropX: Double, cropY: Double, cropW: Double, cropH: Double) -> NativeActionEnvelope? {
        let rnd = Self.randomBytes(16)
        return consumeJsonAction {
            rnd.withUnsafeBytes { ptr in
                telefam_prepare_upload_background_photo(
                    engine,
                    mimeType,
                    byteSize,
                    cropX,
                    cropY,
                    cropW,
                    cropH,
                    isOnline,
                    Self.nowEpochMs(),
                    ptr.bindMemory(to: UInt8.self).baseAddress,
                    rnd.count
                )
            }
        }
    }

    func prepareDeleteBackgroundPhoto(_ photoId: String) -> NativeActionEnvelope? {
        let rnd = Self.randomBytes(16)
        return consumeJsonAction {
            rnd.withUnsafeBytes { ptr in
                telefam_prepare_delete_background_photo(engine, photoId, isOnline, Self.nowEpochMs(), ptr.bindMemory(to: UInt8.self).baseAddress, rnd.count)
            }
        }
    }

    func prepareFetchContacts() -> NativeActionEnvelope? {
        let rnd = Self.randomBytes(16)
        return consumeJsonAction {
            rnd.withUnsafeBytes { ptr in
                telefam_prepare_fetch_contacts(engine, isOnline, Self.nowEpochMs(), ptr.bindMemory(to: UInt8.self).baseAddress, rnd.count)
            }
        }
    }

    func prepareFollowAction(targetUserId: String, follow: Bool) -> NativeActionEnvelope? {
        let rnd = Self.randomBytes(16)
        return consumeJsonAction {
            rnd.withUnsafeBytes { ptr in
                telefam_prepare_follow_action(engine, targetUserId, follow, isOnline, Self.nowEpochMs(), ptr.bindMemory(to: UInt8.self).baseAddress, rnd.count)
            }
        }
    }

    func prepareRefreshToken() -> NativeActionEnvelope? {
        let rnd = Self.randomBytes(16)
        return consumeJsonAction {
            rnd.withUnsafeBytes { ptr in
                telefam_prepare_refresh_token(engine, isOnline, Self.nowEpochMs(), ptr.bindMemory(to: UInt8.self).baseAddress, rnd.count)
            }
        }
    }

    func onHttpResult(requestId: String, statusCode: Int, responseBody: String, networkError: Bool) -> NativeActionEnvelope? {
        let nonce = Self.randomBytes(12)
        let envelope = consumeJsonAction {
            nonce.withUnsafeBytes { ptr in
                telefam_on_http_result(
                    engine,
                    requestId,
                    Int32(statusCode),
                    responseBody,
                    networkError,
                    isOnline,
                    Self.nowEpochMs(),
                    ptr.bindMemory(to: UInt8.self).baseAddress,
                    nonce.count
                )
            }
        }
        if let sealed = envelope?.state?.sealedVaultB64, !sealed.isEmpty {
            saveSealedVaultB64(sealed)
        }
        return envelope
    }

    func filterContacts(_ allContacts: [ContactItem]) -> [ContactItem] {
        guard let encoded = try? encoder.encode(allContacts),
              let jsonStr = String(data: encoded, encoding: .utf8) else {
            return allContacts
        }
        stateLock.lock()
        defer { stateLock.unlock() }
        guard let cStr = telefam_filter_contacts(engine, jsonStr) else { return allContacts }
        defer { telefam_free_string(cStr) }
        let out = String(cString: cStr)
        let parsed = try? decoder.decode(ContactsFilterEnvelope.self, from: Data(out.utf8))
        return parsed?.contacts ?? allContacts
    }

    func parseContactsSync(_ serverBody: String) -> [ContactItem]? {
        stateLock.lock()
        defer { stateLock.unlock() }
        guard let cStr = telefam_parse_contacts_sync(engine, serverBody) else { return nil }
        defer { telefam_free_string(cStr) }
        let out = String(cString: cStr)
        let parsed = try? decoder.decode(ContactsSyncEnvelope.self, from: Data(out.utf8))
        guard parsed?.ok == true else { return nil }
        return parsed?.contacts
    }

    // MARK: - HTTP + Certificate Pinning

    func executeJsonRequest(_ action: NativeHttpAction) async -> (statusCode: Int, body: String, networkError: Bool) {
        guard let url = URL(string: action.url) else {
            return (0, "", true)
        }
        var request = URLRequest(url: url)
        request.httpMethod = action.method
        action.headers?.forEach { request.setValue($0.value, forHTTPHeaderField: $0.key) }
        if action.method.uppercased() != "GET", let body = action.body, !body.isEmpty {
            request.httpBody = Data(body.utf8)
            if request.value(forHTTPHeaderField: "Content-Type") == nil {
                request.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
            }
        }
        do {
            let (data, response) = try await pinnedSession.data(for: request)
            let code = (response as? HTTPURLResponse)?.statusCode ?? 0
            return (code, String(data: data, encoding: .utf8) ?? "", false)
        } catch {
            return (0, "", true)
        }
    }

    func executeMultipartUpload(
        _ action: NativeHttpAction,
        imageBytes: Data,
        mimeType: String,
        cropX: Double,
        cropY: Double,
        cropW: Double,
        cropH: Double
    ) async -> (statusCode: Int, body: String, networkError: Bool) {
        guard let url = URL(string: action.url) else {
            return (0, "", true)
        }
        let boundary = "TelefamBoundary-\(UUID().uuidString)"
        var request = URLRequest(url: url)
        request.httpMethod = action.method
        action.headers?.forEach { key, value in
            if key.lowercased() != "content-type" {
                request.setValue(value, forHTTPHeaderField: key)
            }
        }
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")

        var body = Data()
        func appendField(_ name: String, _ value: String) {
            body.append(Data("--\(boundary)\r\n".utf8))
            body.append(Data("Content-Disposition: form-data; name=\"\(name)\"\r\n\r\n".utf8))
            body.append(Data("\(value)\r\n".utf8))
        }
        appendField("cropX", String(format: "%.6f", cropX))
        appendField("cropY", String(format: "%.6f", cropY))
        appendField("cropW", String(format: "%.6f", cropW))
        appendField("cropH", String(format: "%.6f", cropH))

        let ext = mimeType.contains("png") ? "png" : (mimeType.contains("webp") ? "webp" : "jpg")
        body.append(Data("--\(boundary)\r\n".utf8))
        body.append(Data("Content-Disposition: form-data; name=\"photo\"; filename=\"upload.\(ext)\"\r\n".utf8))
        body.append(Data("Content-Type: \(mimeType)\r\n\r\n".utf8))
        body.append(imageBytes)
        body.append(Data("\r\n--\(boundary)--\r\n".utf8))
        request.httpBody = body

        do {
            let (data, response) = try await pinnedSession.data(for: request)
            let code = (response as? HTTPURLResponse)?.statusCode ?? 0
            return (code, String(data: data, encoding: .utf8) ?? "", false)
        } catch {
            return (0, "", true)
        }
    }

    func fetchImageData(from rawUrl: String) async -> Data? {
        let resolved: String
        if rawUrl.hasPrefix("http://") || rawUrl.hasPrefix("https://") {
            resolved = rawUrl
        } else {
            let suffix = rawUrl.hasPrefix("/") ? rawUrl : "/\(rawUrl)"
            resolved = "\(baseUrl)\(suffix)"
        }
        guard let url = URL(string: resolved) else { return nil }
        var req = URLRequest(url: url)
        req.timeoutInterval = 15
        do {
            let (data, resp) = try await pinnedSession.data(for: req)
            guard let http = resp as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
                return nil
            }
            return data
        } catch {
            return nil
        }
    }

    func urlSession(
        _ session: URLSession,
        didReceive challenge: URLAuthenticationChallenge,
        completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void
    ) {
        guard challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust,
              let trust = challenge.protectionSpace.serverTrust else {
            completionHandler(.performDefaultHandling, nil)
            return
        }

        if tlsPinSha256.isEmpty {
            completionHandler(.performDefaultHandling, nil)
            return
        }

        var error: CFError?
        guard SecTrustEvaluateWithError(trust, &error) else {
            completionHandler(.cancelAuthenticationChallenge, nil)
            return
        }

        let chain = (SecTrustCopyCertificateChain(trust) as? [SecCertificate]) ?? []
        let expected = tlsPinSha256.lowercased().replacingOccurrences(of: "sha256/", with: "")
        for cert in chain {
            if let key = SecCertificateCopyKey(cert),
               let keyData = SecKeyCopyExternalRepresentation(key, nil) as Data? {
                let spkiHash = Self.spkiSha256Base64(ecOrRsaPublicKey: keyData, secKey: key)
                if spkiHash.lowercased() == expected || Self.sha256Hex(keyData) == expected {
                    completionHandler(.useCredential, URLCredential(trust: trust))
                    return
                }
            }
        }
        completionHandler(.cancelAuthenticationChallenge, nil)
    }

    // MARK: - Encrypted Offline Storage (AES-GCM w/ Keychain Master Key)

    func saveEncryptedCache(fileName: String, userId: String, plaintext: String) {
        guard !userId.isEmpty else { return }
        let fileUrl = appSupportDirectory().appendingPathComponent("\(Self.sha256Hex(Data(userId.utf8)).prefix(16))_\(fileName)")
        guard let key = Optional(SymmetricKey(data: masterKeyData)),
              let sealed = try? AES.GCM.seal(Data(plaintext.utf8), using: key),
              let combined = sealed.combined else {
            return
        }
        try? combined.write(to: fileUrl, options: [.atomic, .completeFileProtectionUnlessOpen])
    }

    func loadEncryptedCache(fileName: String, userId: String) -> String? {
        guard !userId.isEmpty else { return nil }
        let fileUrl = appSupportDirectory().appendingPathComponent("\(Self.sha256Hex(Data(userId.utf8)).prefix(16))_\(fileName)")
        guard let combined = try? Data(contentsOf: fileUrl),
              let box = try? AES.GCM.SealedBox(combined: combined) else {
            return nil
        }
        let key = SymmetricKey(data: masterKeyData)
        guard let decrypted = try? AES.GCM.open(box, using: key) else { return nil }
        return String(data: decrypted, encoding: .utf8)
    }

    func saveSealedVaultB64(_ b64: String) {
        let fileUrl = appSupportDirectory().appendingPathComponent("vault_sealed.dat")
        let data = Data(b64.utf8)
        try? data.write(to: fileUrl, options: [.atomic, .completeFileProtectionUnlessOpen])
    }

    func loadSealedVaultB64() -> String {
        let fileUrl = appSupportDirectory().appendingPathComponent("vault_sealed.dat")
        guard let data = try? Data(contentsOf: fileUrl),
              let str = String(data: data, encoding: .utf8) else {
            return ""
        }
        return str
    }

    func clearEncryptedUserCaches(userId: String) {
        let prefix = Self.sha256Hex(Data(userId.utf8)).prefix(16)
        let dir = appSupportDirectory()
        for name in ["owner_profile_cache.enc", "contacts_cache.enc", "pending_profile_edit.enc"] {
            try? FileManager.default.removeItem(at: dir.appendingPathComponent("\(prefix)_\(name)"))
        }
        try? FileManager.default.removeItem(at: dir.appendingPathComponent("vault_sealed.dat"))
    }

    // MARK: - Private Helpers

    private func startNetworkMonitor() {
        pathMonitor.pathUpdateHandler = { [weak self] path in
            let online = (path.status == .satisfied)
            self?.isOnline = online
            self?.onConnectivityChanged?(online)
        }
        pathMonitor.start(queue: monitorQueue)
    }

    private func consumeJsonAction(_ call: () -> UnsafePointer<CChar>?) -> NativeActionEnvelope? {
        stateLock.lock()
        defer { stateLock.unlock() }
        guard let cStr = call() else { return nil }
        defer { telefam_free_string(cStr) }
        let json = String(cString: cStr)
        return try? decoder.decode(NativeActionEnvelope.self, from: Data(json.utf8))
    }

    private func appSupportDirectory() -> URL {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? FileManager.default.temporaryDirectory
        let dir = base.appendingPathComponent("TelefamVault", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }

    private static func loadOrCreateKeychainMasterKey() -> Data {
        let account = "telefam_master_key_v1"
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: "com.telefam.company.vault",
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var item: CFTypeRef?
        if SecItemCopyMatching(query as CFDictionary, &item) == errSecSuccess,
           let existing = item as? Data, existing.count == 32 {
            return existing
        }

        let generated = randomBytes(32)
        let addQuery: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: "com.telefam.company.vault",
            kSecAttrAccount as String: account,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
            kSecValueData as String: generated
        ]
        SecItemAdd(addQuery as CFDictionary, nil)
        return generated
    }

    private static func loadOrCreateDeviceSigningPublicKey() -> Data {
        let tag = Data("com.telefam.company.devicekey.v1".utf8)
        let query: [String: Any] = [
            kSecClass as String: kSecClassKey,
            kSecAttrApplicationTag as String: tag,
            kSecAttrKeyType as String: kSecAttrKeyTypeECSECPrimeRandom,
            kSecReturnRef as String: true
        ]
        var keyRef: CFTypeRef?
        if SecItemCopyMatching(query as CFDictionary, &keyRef) == errSecSuccess,
           let resolvedRef = keyRef {
            let priv = resolvedRef as! SecKey
            if let pub = SecKeyCopyPublicKey(priv),
               let pubData = SecKeyCopyExternalRepresentation(pub, nil) as Data? {
                return pubData
            }
        }

        let attributes: [String: Any] = [
            kSecAttrKeyType as String: kSecAttrKeyTypeECSECPrimeRandom,
            kSecAttrKeySizeInBits as String: 256,
            kSecPrivateKeyAttrs as String: [
                kSecAttrIsPermanent as String: true,
                kSecAttrApplicationTag as String: tag,
                kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
            ]
        ]
        var err: Unmanaged<CFError>?
        if let priv = SecKeyCreateRandomKey(attributes as CFDictionary, &err),
           let pub = SecKeyCopyPublicKey(priv),
           let pubData = SecKeyCopyExternalRepresentation(pub, nil) as Data? {
            return pubData
        }
        return randomBytes(32)
    }

    private static func randomBytes(_ count: Int) -> Data {
        var bytes = [UInt8](repeating: 0, count: count)
        _ = SecRandomCopyBytes(kSecRandomDefault, count, &bytes)
        return Data(bytes)
    }

    private static func sha256Hex(_ data: Data) -> String {
        SHA256.hash(data: data).map { String(format: "%02x", $0) }.joined()
    }

    private static func spkiSha256Base64(ecOrRsaPublicKey: Data, secKey: SecKey) -> String {
        let attrs = (SecKeyCopyAttributes(secKey) as? [String: Any]) ?? [:]
        let keySize = attrs[kSecAttrKeySizeInBits as String] as? Int ?? 0
        let keyType = attrs[kSecAttrKeyType as String] as? String
        var spki = Data()
        if keyType == (kSecAttrKeyTypeECSECPrimeRandom as String) && keySize == 256 {
            spki.append(Data([
                0x30, 0x59, 0x30, 0x13, 0x06, 0x07, 0x2A, 0x86, 0x48, 0xCE, 0x3D, 0x02, 0x01,
                0x06, 0x08, 0x2A, 0x86, 0x48, 0xCE, 0x3D, 0x03, 0x01, 0x07, 0x03, 0x42, 0x00
            ]))
        }
        spki.append(ecOrRsaPublicKey)
        return Data(SHA256.hash(data: spki)).base64EncodedString()
    }

    private static func bundleExecutableSha256() -> String {
        guard let url = Bundle.main.executableURL,
              let data = try? Data(contentsOf: url, options: .mappedIfSafe) else {
            return ""
        }
        return sha256Hex(data)
    }

    private static func hardwareDeviceName() -> String {
        var systemInfo = utsname()
        uname(&systemInfo)
        let machine = withUnsafePointer(to: &systemInfo.machine) {
            $0.withMemoryRebound(to: CChar.self, capacity: 1) { String(cString: $0) }
        }
        return "\(UIDevice.current.model) \(machine)"
    }

    private static func nowEpochMs() -> Int64 {
        Int64(Date().timeIntervalSince1970 * 1000.0)
    }

    private static func currentDateComponents() -> (year: Int, month: Int, day: Int) {
        let c = Calendar(identifier: .gregorian).dateComponents([.year, .month, .day], from: Date())
        return (c.year ?? 2026, c.month ?? 1, c.day ?? 1)
    }
}
