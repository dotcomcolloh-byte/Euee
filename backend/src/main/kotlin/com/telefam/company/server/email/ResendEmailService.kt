package com.telefam.company.server.email

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable

interface EmailSender {
    suspend fun sendOtpEmail(toEmail: String, otpCode: String): Boolean
}

class ResendEmailService(
    private val apiKey: String,
    private val fromEmail: String
) : EmailSender {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json() }
    }

    @Serializable
    private data class ResendRequest(
        val from: String,
        val to: List<String>,
        val subject: String,
        val html: String
    )

    override suspend fun sendOtpEmail(toEmail: String, otpCode: String): Boolean {
        if (apiKey.isBlank() || apiKey.startsWith("re_CHANGE")) return false
        val html = """
            <div style="font-family:sans-serif;max-width:440px;margin:0 auto;padding:24px;border:1px solid #eee;border-radius:12px;">
              <h2 style="color:#C80E18;margin:0 0 8px;">Telefam Verification</h2>
              <p style="color:#4B5563;margin:0 0 16px;">Use this verification code to sign in. It expires in 5 minutes.</p>
              <div style="font-size:28px;font-weight:700;letter-spacing:6px;color:#111827;background:#FDF2F2;padding:14px 20px;border-radius:10px;text-align:center;">$otpCode</div>
            </div>
        """.trimIndent()
        val resp = client.post("https://api.resend.com/emails") {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(
                ResendRequest(
                    from = fromEmail,
                    to = listOf(toEmail),
                    subject = "Your Telefam verification code",
                    html = html
                )
            )
        }
        return resp.status.isSuccess()
    }
}
