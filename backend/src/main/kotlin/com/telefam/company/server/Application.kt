package com.telefam.company.server

import com.telefam.company.server.auth.AppleTokenVerifier
import com.telefam.company.server.auth.AppleTokenVerifierService
import com.telefam.company.server.auth.GoogleTokenVerifier
import com.telefam.company.server.auth.GoogleTokenVerifierService
import com.telefam.company.server.config.ServerConfig
import com.telefam.company.server.crypto.ServerCrypto
import com.telefam.company.server.db.PostgresTelefamRepository
import com.telefam.company.server.db.TelefamRepository
import com.telefam.company.server.email.EmailSender
import com.telefam.company.server.email.ResendEmailService
import com.telefam.company.server.media.MediaSandboxProcessor
import com.telefam.company.server.media.UnixSocketSandboxMediaClient
import com.telefam.company.server.model.ErrorResponse
import com.telefam.company.server.routes.authRoutes
import com.telefam.company.server.routes.contactsRoutes
import com.telefam.company.server.routes.profileRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.time.Instant

fun main() {
    val config = ServerConfig.fromEnv()
    val repo = PostgresTelefamRepository(config.databaseUrl, config.dbUser, config.dbPassword)
    val crypto = ServerCrypto(config.jwtHmacSecret, config.dataEncryptionKey, config.deviceBindingPepper)
    val emailSender = ResendEmailService(config.resendApiKey, config.resendFromEmail)
    val googleVerifier = GoogleTokenVerifierService(config.googleWebClientId)
    val appleVerifier = AppleTokenVerifierService(config.appleBundleIds)
    val sandboxClient = UnixSocketSandboxMediaClient(config.sandboxSocketPath)

    embeddedServer(Netty, port = config.port) {
        telefamModule(config, repo, crypto, emailSender, googleVerifier, sandboxClient, appleVerifier)
    }.start(wait = true)
}

fun Application.telefamModule(
    config: ServerConfig,
    repo: TelefamRepository,
    crypto: ServerCrypto,
    emailSender: EmailSender,
    googleVerifier: GoogleTokenVerifier,
    sandboxProcessor: MediaSandboxProcessor,
    appleVerifier: AppleTokenVerifier = AppleTokenVerifierService(config.appleBundleIds),
    nowProvider: () -> Instant = { Instant.now() }
) {
    install(ContentNegotiation) {
        json()
    }
    install(DefaultHeaders) {
        header("X-Content-Type-Options", "nosniff")
        header("X-Frame-Options", "DENY")
        header("Cache-Control", "no-store")
    }
    install(StatusPages) {
        exception<Throwable> { call, _ ->
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Error has occurred"))
        }
    }

    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }
        authRoutes(config, repo, crypto, emailSender, googleVerifier, appleVerifier, nowProvider)
        profileRoutes(config, repo, crypto, sandboxProcessor, nowProvider)
        contactsRoutes(config, repo, crypto, nowProvider)
    }
}
