package com.telefam.company.server.config

import java.security.SecureRandom
import java.util.Base64

data class ServerConfig(
    val port: Int,
    val isProduction: Boolean,
    val publicBaseUrl: String,
    val databaseUrl: String,
    val dbUser: String,
    val dbPassword: String,
    val resendApiKey: String,
    val resendFromEmail: String,
    val googleWebClientId: String,
    val appleBundleIds: List<String> = listOf("com.telefam.company.app"),
    val jwtHmacSecret: ByteArray,
    val dataEncryptionKey: ByteArray,
    val deviceBindingPepper: ByteArray,
    val sandboxSocketPath: String,
    val avatarStorageDir: String
) {
    companion object {
        private fun random32(): ByteArray = ByteArray(32).also { SecureRandom().nextBytes(it) }

        fun fromEnv(env: Map<String, String> = System.getenv()): ServerConfig {
            val isProd = (env["TELEFAM_ENV"] ?: "production").equals("production", ignoreCase = true)
            fun decodeOrRandom(key: String): ByteArray {
                val raw = env[key]?.trim().orEmpty()
                if (raw.isEmpty() || raw.startsWith("CHANGE_ME")) {
                    check(!isProd) { "Missing required production secret: $key" }
                    return random32()
                }
                return Base64.getDecoder().decode(raw)
            }

            val appleIds = (env["APPLE_BUNDLE_IDS"] ?: "com.telefam.company.app")
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("CHANGE_ME") }

            return ServerConfig(
                port = env["PORT"]?.toIntOrNull() ?: 8080,
                isProduction = isProd,
                publicBaseUrl = (env["PUBLIC_BASE_URL"] ?: "https://api.telefam.company").trimEnd('/'),
                databaseUrl = env["DATABASE_URL"] ?: "jdbc:postgresql://localhost:5432/telefam",
                dbUser = env["DB_USER"] ?: "telefam_app",
                dbPassword = env["DB_PASSWORD"] ?: "",
                resendApiKey = env["RESEND_API_KEY"] ?: "",
                resendFromEmail = env["RESEND_FROM_EMAIL"] ?: "Telefam <noreply@telefam.company>",
                googleWebClientId = env["GOOGLE_WEB_CLIENT_ID"] ?: "",
                appleBundleIds = appleIds,
                jwtHmacSecret = decodeOrRandom("JWT_HMAC_SECRET"),
                dataEncryptionKey = decodeOrRandom("DATA_ENCRYPTION_KEY_B64"),
                deviceBindingPepper = decodeOrRandom("DEVICE_BINDING_PEPPER"),
                sandboxSocketPath = env["SANDBOX_SOCKET_PATH"] ?: "/run/sandbox/processor.sock",
                avatarStorageDir = env["AVATAR_STORAGE_DIR"] ?: "/var/lib/telefam/avatars"
            )
        }
    }
}
