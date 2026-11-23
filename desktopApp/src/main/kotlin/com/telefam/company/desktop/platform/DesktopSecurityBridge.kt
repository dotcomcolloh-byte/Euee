package com.telefam.company.desktop.platform

import com.telefam.company.shared.TelefamJsonParser
import com.telefam.company.shared.model.ContactItem
import com.telefam.company.shared.model.NativeHttpAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.CertificatePinner
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Serializable
data class CachedOwnerProfileRecord(
    val userId: String,
    val profileJson: String,
    val pendingProfileSync: Boolean = false,
    val updatedAtMs: Long = 0L
)

@Serializable
data class CachedContactRecord(
    val ownerUserId: String,
    val contact: ContactItem,
    val pendingAction: String = "",
    val updatedAtMs: Long = 0L
)

@Serializable
data class DesktopOfflineCachePayload(
    val profiles: List<CachedOwnerProfileRecord> = emptyList(),
    val contacts: List<CachedContactRecord> = emptyList()
)

data class DesktopRuntimeConfig(
    val apiBaseUrl: String,
    val tlsPinSha256: String,
    val googleClientId: String,
    val googleClientSecret: String,
    val isDebugBuild: Boolean
)

class DesktopSecurityBridge(
    private val customAppDir: File? = null
) {

    private val rng = SecureRandom()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val config: DesktopRuntimeConfig by lazy { loadConfig() }

    val appDataDir: File by lazy {
        val dir = customAppDir ?: resolveOsConfigDirectory()
        if (!dir.exists()) {
            dir.mkdirs()
        }
        hardenDirectoryPermissions(dir)
        dir
    }

    private val masterKeyFile: File get() = File(appDataDir, "vault_master.key")
    private val ecKeyPairFile: File get() = File(appDataDir, "device_ec.key")
    private val sealedVaultFile: File get() = File(appDataDir, "sealed_vault.dat")
    private val offlineCacheFile: File get() = File(appDataDir, "offline_cache.enc")

    val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)

        val pin = config.tlsPinSha256.trim()
        val host = runCatching { URI(config.apiBaseUrl).host }.getOrNull()
        if (pin.isNotBlank() && pin != "NONE" && pin.startsWith("sha256/") && !host.isNullOrBlank()) {
            builder.certificatePinner(CertificatePinner.Builder().add(host, pin).build())
        }
        builder.build()
    }

    fun isOnline(): Boolean {
        return runCatching {
            val uri = URI(config.apiBaseUrl)
            val host = uri.host ?: return false
            val port = if (uri.port > 0) uri.port else if (uri.scheme.equals("https", true)) 443 else 80
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 2500)
                true
            }
        }.getOrDefault(false)
    }

    fun hardwareName(): String {
        val os = System.getProperty("os.name").orEmpty().ifBlank { "Desktop" }
        val arch = System.getProperty("os.arch").orEmpty().ifBlank { "x64" }
        val host = runCatching { InetAddress.getLocalHost().hostName }.getOrDefault("workstation")
        return "$os $arch ($host)".take(96)
    }

    fun deviceIdHashAndPublicKey(): Pair<String, String> {
        val pubBytes = getOrCreateDesktopEcPublicKey()
        val osName = System.getProperty("os.name").orEmpty()
        val osUser = System.getProperty("user.name").orEmpty()
        val machineSalt = appDataDir.canonicalPath
        val digest = MessageDigest.getInstance("SHA-256").apply {
            update(pubBytes)
            update(osName.toByteArray(StandardCharsets.UTF_8))
            update(osUser.toByteArray(StandardCharsets.UTF_8))
            update(machineSalt.toByteArray(StandardCharsets.UTF_8))
        }.digest()
        val idHashHex = digest.joinToString("") { "%02x".format(it) }
        val pubB64 = Base64.getEncoder().encodeToString(pubBytes)
        return idHashHex to pubB64
    }

    @Synchronized
    fun getOrCreateVaultMasterKey(): ByteArray {
        val wrappingSecret = deriveLocalWrappingKey()
        if (masterKeyFile.isFile) {
            val decrypted = runCatching {
                val rawBytes = masterKeyFile.readBytes()
                decryptAesGcm(wrappingSecret, rawBytes)
            }.getOrNull()
            if (decrypted != null && decrypted.size == 32) {
                return decrypted
            }
        }
        val key = ByteArray(32).also { rng.nextBytes(it) }
        val sealed = encryptAesGcm(wrappingSecret, key)
        masterKeyFile.writeBytes(sealed)
        hardenFilePermissions(masterKeyFile)
        return key
    }

    @Synchronized
    fun loadSealedVault(): String {
        if (!sealedVaultFile.isFile) return ""
        return runCatching { sealedVaultFile.readText(StandardCharsets.UTF_8).trim() }.getOrDefault("")
    }

    @Synchronized
    fun saveSealedVault(sealedB64: String) {
        if (sealedB64.isEmpty()) {
            if (sealedVaultFile.exists()) sealedVaultFile.delete()
            return
        }
        sealedVaultFile.writeText(sealedB64, StandardCharsets.UTF_8)
        hardenFilePermissions(sealedVaultFile)
    }

    @Synchronized
    fun loadOfflineCache(): DesktopOfflineCachePayload {
        if (!offlineCacheFile.isFile) return DesktopOfflineCachePayload()
        return runCatching {
            val key = getOrCreateVaultMasterKey()
            try {
                val cipherBytes = offlineCacheFile.readBytes()
                val plain = decryptAesGcm(SecretKeySpec(key, "AES"), cipherBytes)
                json.decodeFromString<DesktopOfflineCachePayload>(String(plain, StandardCharsets.UTF_8))
            } finally {
                key.fill(0)
            }
        }.getOrDefault(DesktopOfflineCachePayload())
    }

    @Synchronized
    fun saveOfflineCache(payload: DesktopOfflineCachePayload) {
        val key = getOrCreateVaultMasterKey()
        try {
            val plain = json.encodeToString(payload).toByteArray(StandardCharsets.UTF_8)
            val encrypted = encryptAesGcm(SecretKeySpec(key, "AES"), plain)
            offlineCacheFile.writeBytes(encrypted)
            hardenFilePermissions(offlineCacheFile)
        } finally {
            key.fill(0)
        }
    }

    fun randomBytes(count: Int): ByteArray = ByteArray(count).also { rng.nextBytes(it) }

    fun generateUrlNonce(): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes(24))

    fun pickImageFile(): Pair<ByteArray, String>? {
        val dialog = FileDialog(null as Frame?, "Select Image", FileDialog.LOAD).apply {
            filenameFilter = FilenameFilter { _, name ->
                val lower = name.lowercase(Locale.US)
                lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                    lower.endsWith(".png") || lower.endsWith(".webp")
            }
            isVisible = true
        }
        val dir = dialog.directory ?: return null
        val fileName = dialog.file ?: return null
        val file = File(dir, fileName)
        if (!file.isFile || file.length() <= 0L || file.length() > 16L * 1024L * 1024L) {
            return null
        }
        val lower = file.name.lowercase(Locale.US)
        val mime = when {
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".webp") -> "image/webp"
            else -> "image/jpeg"
        }
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return null
        return bytes to mime
    }

    fun requestGoogleIdTokenViaBrowser(nonce: String): Result<String> {
        val clientId = config.googleClientId.trim()
        if (clientId.isEmpty() || clientId.startsWith("YOUR_")) {
            return Result.failure(IllegalStateException("Google Sign-In client ID is not configured"))
        }
        return runCatching {
            val codeVerifier = generateUrlNonce() + generateUrlNonce()
            val challengeBytes = MessageDigest.getInstance("SHA-256")
                .digest(codeVerifier.toByteArray(StandardCharsets.US_ASCII))
            val codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes)
            val stateParam = generateUrlNonce()

            val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
            server.soTimeout = 120_000
            val redirectUri = "http://127.0.0.1:${server.localPort}/callback"

            val authUrl = buildString {
                append("https://accounts.google.com/o/oauth2/v2/auth")
                append("?client_id=").append(urlEnc(clientId))
                append("&redirect_uri=").append(urlEnc(redirectUri))
                append("&response_type=code")
                append("&scope=").append(urlEnc("openid email profile"))
                append("&code_challenge=").append(urlEnc(codeChallenge))
                append("&code_challenge_method=S256")
                append("&nonce=").append(urlEnc(nonce))
                append("&state=").append(urlEnc(stateParam))
                append("&prompt=select_account")
            }

            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                server.close()
                throw IllegalStateException("System browser unavailable")
            }
            Desktop.getDesktop().browse(URI(authUrl))

            val authCode = server.use { srv ->
                srv.accept().use { client ->
                    val reader = client.getInputStream().bufferedReader(StandardCharsets.UTF_8)
                    val requestLine = reader.readLine().orEmpty()
                    val query = requestLine.substringAfter("GET ", "").substringBefore(" HTTP/")
                        .substringAfter("?", "")
                    val params = parseQueryParams(query)
                    val responseHtml = if (params["state"] == stateParam && !params["code"].isNullOrBlank()) {
                        "<html><body><h3>Telefam sign-in completed. You can close this window.</h3></body></html>"
                    } else {
                        "<html><body><h3>Sign-in cancelled.</h3></body></html>"
                    }
                    val httpResp = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nConnection: close\r\n\r\n$responseHtml"
                    client.getOutputStream().write(httpResp.toByteArray(StandardCharsets.UTF_8))
                    client.getOutputStream().flush()
                    if (params["state"] != stateParam) throw IllegalStateException("OAuth state mismatch")
                    params["code"] ?: throw IllegalStateException("Authorization code missing")
                }
            }

            val formBuilder = FormBody.Builder()
                .add("client_id", clientId)
                .add("code", authCode)
                .add("code_verifier", codeVerifier)
                .add("grant_type", "authorization_code")
                .add("redirect_uri", redirectUri)
            if (config.googleClientSecret.isNotBlank()) {
                formBuilder.add("client_secret", config.googleClientSecret)
            }
            val tokenReq = Request.Builder()
                .url("https://oauth2.googleapis.com/token")
                .post(formBuilder.build())
                .build()
            okHttpClient.newCall(tokenReq).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Google token exchange failed")
                }
                val parsed = json.parseToJsonElement(raw).jsonObject
                (parsed["id_token"] as? JsonPrimitive)?.contentOrNull
                    ?: throw IllegalStateException("ID token missing")
            }
        }
    }

    fun executeHttp(action: NativeHttpAction): Triple<Int, String, Boolean> {
        return try {
            val reqBuilder = Request.Builder().url(action.url)
            action.headers.forEach { (k, v) ->
                if (!k.equals("Content-Type", ignoreCase = true)) {
                    reqBuilder.addHeader(k, v)
                }
            }
            val jsonMedia = "application/json; charset=utf-8".toMediaType()
            when (action.method) {
                "GET" -> reqBuilder.get()
                "DELETE" -> if (action.body.isEmpty()) {
                    reqBuilder.delete()
                } else {
                    reqBuilder.delete(action.body.toRequestBody(jsonMedia))
                }
                "PUT" -> reqBuilder.put(action.body.toRequestBody(jsonMedia))
                "POST" -> reqBuilder.post(action.body.toRequestBody(jsonMedia))
                else -> reqBuilder.method(action.method, action.body.toRequestBody(jsonMedia))
            }
            okHttpClient.newCall(reqBuilder.build()).execute().use { resp ->
                Triple(resp.code, resp.body?.string().orEmpty(), false)
            }
        } catch (_: Exception) {
            Triple(0, "", true)
        }
    }

    fun executePhotoUpload(
        action: NativeHttpAction,
        imageBytes: ByteArray,
        mimeType: String
    ): Triple<Int, String, Boolean> {
        return try {
            val crop = TelefamJsonParser.parseCropExtra(action.extra)
            val ext = when {
                mimeType.contains("png", true) -> "png"
                mimeType.contains("webp", true) -> "webp"
                else -> "jpg"
            }
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("cropX", crop.cropX.toString())
                .addFormDataPart("cropY", crop.cropY.toString())
                .addFormDataPart("cropWidth", crop.cropWidth.toString())
                .addFormDataPart("cropHeight", crop.cropHeight.toString())
                .addFormDataPart(
                    "photo",
                    "upload.$ext",
                    imageBytes.toRequestBody(mimeType.toMediaType())
                )
                .build()

            val reqBuilder = Request.Builder().url(action.url).post(multipart)
            action.headers.forEach { (k, v) ->
                if (!k.equals("Content-Type", ignoreCase = true)) {
                    reqBuilder.addHeader(k, v)
                }
            }
            okHttpClient.newCall(reqBuilder.build()).execute().use { resp ->
                Triple(resp.code, resp.body?.string().orEmpty(), false)
            }
        } catch (_: Exception) {
            Triple(0, "", true)
        }
    }

    private fun getOrCreateDesktopEcPublicKey(): ByteArray {
        val wrappingSecret = deriveLocalWrappingKey()
        if (ecKeyPairFile.isFile) {
            val loaded = runCatching {
                val plain = decryptAesGcm(wrappingSecret, ecKeyPairFile.readBytes())
                val obj = json.parseToJsonElement(String(plain, StandardCharsets.UTF_8)).jsonObject
                val pubB64 = (obj["pub"] as? JsonPrimitive)?.contentOrNull.orEmpty()
                val privB64 = (obj["priv"] as? JsonPrimitive)?.contentOrNull.orEmpty()
                val pubBytes = Base64.getDecoder().decode(pubB64)
                val privBytes = Base64.getDecoder().decode(privB64)
                val kf = KeyFactory.getInstance("EC")
                kf.generatePrivate(PKCS8EncodedKeySpec(privBytes))
                kf.generatePublic(X509EncodedKeySpec(pubBytes)).encoded
            }.getOrNull()
            if (loaded != null && loaded.isNotEmpty()) {
                return loaded
            }
        }
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"), rng)
        val pair = kpg.generateKeyPair()
        val encoded = """{"pub":"${Base64.getEncoder().encodeToString(pair.public.encoded)}","priv":"${Base64.getEncoder().encodeToString(pair.private.encoded)}"}"""
        ecKeyPairFile.writeBytes(encryptAesGcm(wrappingSecret, encoded.toByteArray(StandardCharsets.UTF_8)))
        hardenFilePermissions(ecKeyPairFile)
        return pair.public.encoded
    }

    private fun deriveLocalWrappingKey(): SecretKey {
        val salt = buildString {
            append(System.getProperty("user.name").orEmpty())
            append("|")
            append(System.getProperty("os.name").orEmpty())
            append("|")
            append(appDataDir.canonicalPath)
            append("|telefam-desktop-vault-v1")
        }.toByteArray(StandardCharsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(salt)
        return SecretKeySpec(digest, "AES")
    }

    private fun encryptAesGcm(key: SecretKey, plain: ByteArray): ByteArray {
        val iv = ByteArray(12).also { rng.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(plain)
        return ByteArray(iv.size + ciphertext.size).apply {
            System.arraycopy(iv, 0, this, 0, iv.size)
            System.arraycopy(ciphertext, 0, this, iv.size, ciphertext.size)
        }
    }

    private fun decryptAesGcm(key: SecretKey, combined: ByteArray): ByteArray {
        require(combined.size > 12) { "Corrupted encrypted payload" }
        val iv = combined.copyOfRange(0, 12)
        val ciphertext = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun resolveOsConfigDirectory(): File {
        val os = System.getProperty("os.name").orEmpty().lowercase(Locale.US)
        val home = System.getProperty("user.home").orEmpty()
        return when {
            os.contains("win") -> {
                val appData = System.getenv("APPDATA")?.takeIf { it.isNotBlank() }
                if (appData != null) File(appData, "Telefam") else File(home, "AppData/Roaming/Telefam")
            }
            os.contains("mac") || os.contains("darwin") -> {
                File(home, "Library/Application Support/Telefam")
            }
            else -> {
                val xdg = System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }
                if (xdg != null) File(xdg, "Telefam") else File(home, ".config/Telefam")
            }
        }
    }

    private fun hardenDirectoryPermissions(dir: File) {
        runCatching {
            val perms = setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE
            )
            Files.setPosixFilePermissions(dir.toPath(), perms)
        }
    }

    private fun hardenFilePermissions(file: File) {
        runCatching {
            val perms = setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
            )
            Files.setPosixFilePermissions(file.toPath(), perms)
        }
    }

    private fun loadConfig(): DesktopRuntimeConfig {
        val dotenv = readDotEnvMap()
        fun prop(key: String, default: String = ""): String =
            System.getProperty(key)?.takeIf { it.isNotBlank() }
                ?: System.getenv(key)?.takeIf { it.isNotBlank() }
                ?: dotenv[key]?.takeIf { it.isNotBlank() }
                ?: default

        val baseUrl = prop("TELEFAM_API_BASE_URL", "https://api.telefam.company").trimEnd('/')
        val tlsPin = prop("TELEFAM_TLS_PIN_SHA256", "NONE")
        val googleClientId = prop("GOOGLE_DESKTOP_CLIENT_ID")
            .ifBlank { prop("GOOGLE_WEB_CLIENT_ID") }
        val googleClientSecret = prop("GOOGLE_DESKTOP_CLIENT_SECRET")
        val debugMode = prop("TELEFAM_DEBUG", "false").equals("true", ignoreCase = true)
        return DesktopRuntimeConfig(
            apiBaseUrl = baseUrl,
            tlsPinSha256 = tlsPin,
            googleClientId = googleClientId,
            googleClientSecret = googleClientSecret,
            isDebugBuild = debugMode
        )
    }

    private fun readDotEnvMap(): Map<String, String> {
        val candidates = listOf(File(".env"), File("../.env"))
        val envFile = candidates.firstOrNull { it.isFile } ?: return emptyMap()
        val result = mutableMapOf<String, String>()
        runCatching {
            envFile.readLines(StandardCharsets.UTF_8).forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("#")) return@forEach
                val idx = line.indexOf('=')
                if (idx > 0) {
                    val key = line.substring(0, idx).trim()
                    val value = line.substring(idx + 1).trim().removeSurrounding("\"").removeSurrounding("'")
                    result[key] = value
                }
            }
        }
        return result
    }

    private fun urlEnc(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)

    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query.split('&').mapNotNull { part ->
            val idx = part.indexOf('=')
            if (idx <= 0) null
            else part.substring(0, idx) to java.net.URLDecoder.decode(part.substring(idx + 1), StandardCharsets.UTF_8)
        }.toMap()
    }
}
