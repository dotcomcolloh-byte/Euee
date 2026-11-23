package com.telefam.company.server.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.math.BigInteger
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.RSAPublicKeySpec
import java.time.Instant
import java.util.Base64
import java.util.concurrent.atomic.AtomicReference

data class VerifiedAppleUser(
    val subject: String,
    val email: String,
    val fullName: String
)

interface AppleTokenVerifier {
    fun verify(identityToken: String, expectedRawNonce: String, fullNameHint: String = ""): VerifiedAppleUser?
}

@Serializable
data class AppleJwk(
    val kty: String,
    val kid: String,
    val use: String = "sig",
    val alg: String = "RS256",
    val n: String,
    val e: String
)

@Serializable
private data class AppleJwksPayload(
    val keys: List<AppleJwk> = emptyList()
)

interface AppleJwksProvider {
    fun fetchKeys(): List<AppleJwk>
}

class HttpAppleJwksProvider : AppleJwksProvider {
    private val client = HttpClient(CIO)
    private val json = Json { ignoreUnknownKeys = true }

    override fun fetchKeys(): List<AppleJwk> = runBlocking {
        val body = client.get("https://appleid.apple.com/auth/keys").bodyAsText()
        json.decodeFromString(AppleJwksPayload.serializer(), body).keys
    }
}

class AppleTokenVerifierService(
    private val allowedAudiences: List<String>,
    private val jwksProvider: AppleJwksProvider = HttpAppleJwksProvider(),
    private val nowProvider: () -> Instant = { Instant.now() }
) : AppleTokenVerifier {

    private data class CachedJwks(
        val fetchedAtEpochSec: Long,
        val keysByKid: Map<String, AppleJwk>
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val cache = AtomicReference<CachedJwks?>(null)

    override fun verify(identityToken: String, expectedRawNonce: String, fullNameHint: String): VerifiedAppleUser? {
        val validAudiences = allowedAudiences.map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("CHANGE_ME") }
        if (validAudiences.isEmpty()) return null
        if (expectedRawNonce.length < 16) return null

        val parts = identityToken.split('.')
        if (parts.size != 3) return null

        val headerBytes = runCatching { Base64.getUrlDecoder().decode(parts[0]) }.getOrNull() ?: return null
        val payloadBytes = runCatching { Base64.getUrlDecoder().decode(parts[1]) }.getOrNull() ?: return null
        val sigBytes = runCatching { Base64.getUrlDecoder().decode(parts[2]) }.getOrNull() ?: return null

        val headerObj = runCatching { json.parseToJsonElement(headerBytes.decodeToString()).jsonObject }.getOrNull() ?: return null
        val payloadObj = runCatching { json.parseToJsonElement(payloadBytes.decodeToString()).jsonObject }.getOrNull() ?: return null

        val alg = headerObj["alg"]?.jsonPrimitive?.content.orEmpty()
        val kid = headerObj["kid"]?.jsonPrimitive?.content.orEmpty()
        if (alg != "RS256" || kid.isBlank()) return null

        val jwk = resolveJwk(kid) ?: return null
        if (jwk.kty != "RSA") return null

        val verifiedSig = runCatching {
            val modulus = BigInteger(1, Base64.getUrlDecoder().decode(jwk.n))
            val exponent = BigInteger(1, Base64.getUrlDecoder().decode(jwk.e))
            val pubKey = KeyFactory.getInstance("RSA").generatePublic(RSAPublicKeySpec(modulus, exponent))
            val sig = Signature.getInstance("SHA256withRSA")
            sig.initVerify(pubKey)
            sig.update("${parts[0]}.${parts[1]}".toByteArray(Charsets.UTF_8))
            sig.verify(sigBytes)
        }.getOrDefault(false)
        if (!verifiedSig) return null

        val iss = payloadObj["iss"]?.jsonPrimitive?.content.orEmpty()
        if (iss != "https://appleid.apple.com") return null

        val aud = payloadObj["aud"]?.jsonPrimitive?.content.orEmpty()
        if (aud !in validAudiences) return null

        val nowSec = nowProvider().epochSecond
        val exp = payloadObj["exp"]?.jsonPrimitive?.longOrNull ?: return null
        val iat = payloadObj["iat"]?.jsonPrimitive?.longOrNull ?: 0L
        if (exp <= nowSec || iat > nowSec + 300) return null

        val tokenNonce = payloadObj["nonce"]?.jsonPrimitive?.content.orEmpty().lowercase()
        val expectedNonceSha256 = sha256Hex(expectedRawNonce.toByteArray(Charsets.UTF_8))
        if (tokenNonce.isBlank() ||
            !MessageDigest.isEqual(tokenNonce.toByteArray(Charsets.UTF_8), expectedNonceSha256.toByteArray(Charsets.UTF_8))
        ) {
            return null
        }

        val subject = payloadObj["sub"]?.jsonPrimitive?.content?.trim().orEmpty()
        if (subject.isBlank()) return null

        val email = payloadObj["email"]?.jsonPrimitive?.content?.trim()?.lowercase().orEmpty()
        val emailVerifiedEl = payloadObj["email_verified"]?.jsonPrimitive
        if (email.isNotBlank() && emailVerifiedEl != null) {
            val verifiedBool = emailVerifiedEl.booleanOrNull
                ?: emailVerifiedEl.content.equals("true", ignoreCase = true)
            if (!verifiedBool) return null
        }

        return VerifiedAppleUser(
            subject = subject,
            email = email,
            fullName = fullNameHint.trim().take(80)
        )
    }

    private fun resolveJwk(kid: String): AppleJwk? {
        val nowSec = nowProvider().epochSecond
        val current = cache.get()
        if (current != null && (nowSec - current.fetchedAtEpochSec) < 3600L) {
            val hit = current.keysByKid[kid]
            if (hit != null) return hit
        }
        val fetched = runCatching { jwksProvider.fetchKeys() }.getOrNull() ?: return current?.keysByKid?.get(kid)
        val map = fetched.filter { it.kid.isNotBlank() }.associateBy { it.kid }
        cache.set(CachedJwks(nowSec, map))
        return map[kid]
    }

    companion object {
        fun sha256Hex(data: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(data)
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
