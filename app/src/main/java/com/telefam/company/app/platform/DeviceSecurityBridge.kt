package com.telefam.company.app.platform

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.telefam.company.app.BuildConfig
import com.telefam.company.app.nativecore.NativeHttpAction
import okhttp3.CertificatePinner
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URI
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class DeviceSecurityBridge(private val appContext: Context) {

    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val rng = SecureRandom()

    val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)

        val pin = BuildConfig.TELEFAM_TLS_PIN_SHA256.trim()
        val host = runCatching { URI(BuildConfig.TELEFAM_API_BASE_URL).host }.getOrNull()
        if (pin != "NONE" && pin.startsWith("sha256/") && !host.isNullOrBlank()) {
            builder.certificatePinner(
                CertificatePinner.Builder().add(host, pin).build()
            )
        }
        builder.build()
    }

    fun isOnline(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun registerNetworkAvailableCallback(onAvailable: () -> Unit): () -> Unit {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return {}
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                onAvailable()
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                ) {
                    onAvailable()
                }
            }
        }
        val registered = runCatching {
            cm.registerDefaultNetworkCallback(callback)
            true
        }.getOrDefault(false)
        return if (registered) {
            { runCatching { cm.unregisterNetworkCallback(callback) } }
        } else {
            {}
        }
    }

    fun hardwareName(): String =
        "${Build.MANUFACTURER} ${Build.MODEL} (${Build.HARDWARE})".take(96)

    @SuppressLint("HardwareIds")
    fun deviceIdHashAndPublicKey(): Pair<String, String> {
        val pubBytes = getOrCreateHardwareEcPublicKey()
        val androidId = runCatching {
            Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull().orEmpty()
        val digest = MessageDigest.getInstance("SHA-256").apply {
            update(pubBytes)
            update(androidId.toByteArray(Charsets.UTF_8))
            update(Build.FINGERPRINT.toByteArray(Charsets.UTF_8))
        }.digest()
        val idHashHex = digest.joinToString("") { "%02x".format(it) }
        val pubB64 = Base64.encodeToString(pubBytes, Base64.NO_WRAP)
        return idHashHex to pubB64
    }

    fun getOrCreateVaultMasterKey(): ByteArray {
        val existing = prefs.getString(KEY_WRAPPED_MASTER, null)
        val ivStr = prefs.getString(KEY_WRAPPED_IV, null)
        if (existing != null && ivStr != null) {
            runCatching {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    getOrCreateKeystoreAesKey(),
                    GCMParameterSpec(128, Base64.decode(ivStr, Base64.NO_WRAP))
                )
                return cipher.doFinal(Base64.decode(existing, Base64.NO_WRAP))
            }
        }
        val raw = ByteArray(32).also { rng.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKeystoreAesKey())
        val enc = cipher.doFinal(raw)
        prefs.edit()
            .putString(KEY_WRAPPED_MASTER, Base64.encodeToString(enc, Base64.NO_WRAP))
            .putString(KEY_WRAPPED_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
        return raw
    }

    fun loadSealedVault(): String = prefs.getString(KEY_SEALED_VAULT, "") ?: ""

    fun saveSealedVault(sealedB64: String) {
        if (sealedB64.isEmpty()) {
            prefs.edit().remove(KEY_SEALED_VAULT).apply()
        } else {
            prefs.edit().putString(KEY_SEALED_VAULT, sealedB64).apply()
        }
    }

    @Suppress("DEPRECATION")
    fun signingCertSha256Hex(): String {
        return runCatching {
            val pm = appContext.packageManager
            val sigBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(
                    appContext.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
            } else {
                val info = pm.getPackageInfo(appContext.packageName, PackageManager.GET_SIGNATURES)
                info.signatures?.firstOrNull()?.toByteArray()
            } ?: return ""
            MessageDigest.getInstance("SHA-256").digest(sigBytes)
                .joinToString("") { "%02x".format(it) }
        }.getOrDefault("")
    }

    fun randomBytes(count: Int): ByteArray = ByteArray(count).also { rng.nextBytes(it) }

    fun generateUrlNonce(): String =
        Base64.encodeToString(randomBytes(24), Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)

    suspend fun requestGoogleIdToken(activityContext: Context, nonce: String): Result<String> {
        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
        if (webClientId.isEmpty() || webClientId.startsWith("YOUR_")) {
            return Result.failure(IllegalStateException("Google Sign-In client ID is not configured"))
        }
        return runCatching {
            val credentialManager = CredentialManager.create(activityContext)
            val option = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .setNonce(nonce)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()
            val result = credentialManager.getCredential(activityContext, request)
            val googleCred = GoogleIdTokenCredential.createFrom(result.credential.data)
            googleCred.idToken
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
            when (action.method) {
                "GET" -> reqBuilder.get()
                "DELETE" -> if (action.body.isEmpty()) {
                    reqBuilder.delete()
                } else {
                    reqBuilder.delete(action.body.toRequestBody("application/json; charset=utf-8".toMediaType()))
                }
                "PUT" -> reqBuilder.put(action.body.toRequestBody("application/json; charset=utf-8".toMediaType()))
                "POST" -> reqBuilder.post(action.body.toRequestBody("application/json; charset=utf-8".toMediaType()))
                else -> reqBuilder.method(action.method, action.body.toRequestBody("application/json".toMediaType()))
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
            val extra = JSONObject(action.extra)
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("cropX", extra.optDouble("cropX", 0.0).toString())
                .addFormDataPart("cropY", extra.optDouble("cropY", 0.0).toString())
                .addFormDataPart("cropWidth", extra.optDouble("cropWidth", 1.0).toString())
                .addFormDataPart("cropHeight", extra.optDouble("cropHeight", 1.0).toString())
                .addFormDataPart(
                    "photo",
                    "avatar.jpg",
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

    private fun getOrCreateKeystoreAesKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(KEYSTORE_AES_ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_AES_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    private fun getOrCreateHardwareEcPublicKey(): ByteArray {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = ks.getCertificate(KEYSTORE_EC_ALIAS)?.publicKey?.encoded
        if (existing != null) return existing
        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
        kpg.initialize(
            KeyGenParameterSpec.Builder(
                KEYSTORE_EC_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build()
        )
        return kpg.generateKeyPair().public.encoded
    }

    companion object {
        private const val PREFS_NAME = "telefam_secure_vault"
        private const val KEY_WRAPPED_MASTER = "wrapped_master"
        private const val KEY_WRAPPED_IV = "wrapped_iv"
        private const val KEY_SEALED_VAULT = "sealed_vault_v1"
        private const val KEYSTORE_AES_ALIAS = "TelefamVaultKeyV1"
        private const val KEYSTORE_EC_ALIAS = "TelefamDeviceIdKeyV1"
    }
}
