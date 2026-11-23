package com.telefam.company.server.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class ServerCrypto(
    private val jwtSecret: ByteArray,
    private val encryptionKey: ByteArray,
    private val pepper: ByteArray
) {
    private val rng = SecureRandom()

    fun generateSixDigitOtp(): String = "%06d".format(rng.nextInt(1_000_000))

    fun generateOpaqueToken(): String {
        val bytes = ByteArray(32).also { rng.nextBytes(it) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun hmacHex(data: String, key: ByteArray = pepper): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    fun sha256Hex(data: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }

    fun constantTimeEquals(a: String, b: String): Boolean =
        MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))

    fun encryptAtRest(plaintext: String, aad: String): String {
        val iv = ByteArray(12).also { rng.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(encryptionKey, "AES"), GCMParameterSpec(128, iv))
        cipher.updateAAD(aad.toByteArray(Charsets.UTF_8))
        val ct = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = iv + ct
        return Base64.getEncoder().encodeToString(combined)
    }

    fun decryptAtRest(ciphertextB64: String, aad: String): String {
        val combined = Base64.getDecoder().decode(ciphertextB64)
        require(combined.size > 12) { "Invalid ciphertext" }
        val iv = combined.copyOfRange(0, 12)
        val ct = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(encryptionKey, "AES"), GCMParameterSpec(128, iv))
        cipher.updateAAD(aad.toByteArray(Charsets.UTF_8))
        return String(cipher.doFinal(ct), Charsets.UTF_8)
    }

    fun issueDeviceBoundJwt(userId: String, deviceIdHash: String, sessionId: String, ttlSeconds: Long = 86_400L): String {
        val nowSec = System.currentTimeMillis() / 1000L
        val expSec = nowSec + ttlSeconds
        val nonceBytes = ByteArray(12).also { rng.nextBytes(it) }
        val tid = b64UrlBytes(nonceBytes)
        val header = b64Url("""{"alg":"HS256","typ":"JWT"}""")
        val payload = b64Url("""{"sub":"$userId","did":"$deviceIdHash","jti":"$sessionId","tid":"$tid","iat":$nowSec,"exp":$expSec}""")
        val signingInput = "$header.$payload"
        val sig = b64UrlBytes(hmacBytes(signingInput, jwtSecret))
        return "$signingInput.$sig"
    }

    fun verifyDeviceBoundJwt(token: String, expectedDeviceIdHash: String): JwtClaims? {
        val parts = token.split('.')
        if (parts.size != 3) return null
        val signingInput = "${parts[0]}.${parts[1]}"
        val expectedSig = b64UrlBytes(hmacBytes(signingInput, jwtSecret))
        if (!constantTimeEquals(expectedSig, parts[2])) return null

        val payloadJson = runCatching {
            String(Base64.getUrlDecoder().decode(parts[1]), Charsets.UTF_8)
        }.getOrNull() ?: return null

        val sub = extractJsonField(payloadJson, "sub") ?: return null
        val did = extractJsonField(payloadJson, "did") ?: return null
        val jti = extractJsonField(payloadJson, "jti") ?: return null
        val exp = extractJsonLong(payloadJson, "exp") ?: return null

        if (System.currentTimeMillis() / 1000L > exp) return null
        // Strict per-device binding check: token issued for Device A fails on Device B!
        if (!constantTimeEquals(did, expectedDeviceIdHash)) return null

        return JwtClaims(userId = sub, deviceIdHash = did, sessionId = jti, expiresAtSec = exp)
    }

    private fun hmacBytes(data: String, key: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    private fun b64Url(s: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(s.toByteArray(Charsets.UTF_8))

    private fun b64UrlBytes(b: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(b)

    private fun extractJsonField(json: String, key: String): String? {
        val match = Regex(""""$key"\s*:\s*"([^"]+)"""").find(json)
        return match?.groupValues?.get(1)
    }

    private fun extractJsonLong(json: String, key: String): Long? {
        val match = Regex(""""$key"\s*:\s*(\d+)""").find(json)
        return match?.groupValues?.get(1)?.toLongOrNull()
    }
}

data class JwtClaims(
    val userId: String,
    val deviceIdHash: String,
    val sessionId: String,
    val expiresAtSec: Long
)
