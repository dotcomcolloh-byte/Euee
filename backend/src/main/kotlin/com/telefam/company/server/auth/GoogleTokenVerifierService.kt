package com.telefam.company.server.auth

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory

data class VerifiedGoogleUser(
    val subject: String,
    val email: String,
    val fullName: String
)

interface GoogleTokenVerifier {
    fun verify(idToken: String, expectedNonce: String): VerifiedGoogleUser?
}

class GoogleTokenVerifierService(
    private val webClientId: String
) : GoogleTokenVerifier {

    private val verifier: GoogleIdTokenVerifier by lazy {
        GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(listOf(webClientId))
            .setIssuers(listOf("https://accounts.google.com", "accounts.google.com"))
            .build()
    }

    override fun verify(idToken: String, expectedNonce: String): VerifiedGoogleUser? {
        if (webClientId.isBlank() || webClientId.startsWith("CHANGE_ME")) return null
        val parsed = runCatching { verifier.verify(idToken) }.getOrNull() ?: return null
        val payload = parsed.payload
        if (payload.emailVerified != true) return null
        val tokenNonce = payload["nonce"] as? String
        if (expectedNonce.isNotBlank() && tokenNonce != expectedNonce) return null
        val email = payload.email ?: return null
        val subject = payload.subject ?: return null
        val name = (payload["name"] as? String).orEmpty()
        return VerifiedGoogleUser(subject = subject, email = email.lowercase(), fullName = name)
    }
}
