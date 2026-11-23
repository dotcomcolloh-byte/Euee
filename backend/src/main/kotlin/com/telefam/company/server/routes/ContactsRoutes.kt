package com.telefam.company.server.routes

import com.telefam.company.server.config.ServerConfig
import com.telefam.company.server.crypto.ServerCrypto
import com.telefam.company.server.db.AuditLogEntry
import com.telefam.company.server.db.TelefamRepository
import com.telefam.company.server.model.ContactDto
import com.telefam.company.server.model.ContactsListResponse
import com.telefam.company.server.model.ErrorResponse
import com.telefam.company.server.model.FollowActionResponse
import com.telefam.company.server.validation.Validators
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.time.Instant
import java.util.UUID

private const val MAX_FOLLOWS_PER_MINUTE = 100
private const val FOLLOW_RATE_ERROR = "Error has occurred. Try again later."

fun Route.contactsRoutes(
    config: ServerConfig,
    repo: TelefamRepository,
    crypto: ServerCrypto,
    nowProvider: () -> Instant
) {
    get("/api/v1/contacts") {
        val claims = call.authenticateDeviceBound(repo, crypto) ?: return@get
        val query = call.request.queryParameters["q"].orEmpty().trim().lowercase()
        val tab = call.request.queryParameters["tab"].orEmpty().trim().lowercase()

        val allContacts = repo.listDirectoryContacts(claims.userId).map { rec ->
            val countryName = Validators.countryName(rec.phoneCountryIso)
            val avatarUrl = rec.avatarObjectKey?.takeIf { it.isNotBlank() }?.let {
                "${config.publicBaseUrl}/api/v1/media/avatar/$it"
            } ?: ""
            ContactDto(
                userId = rec.userId,
                fullName = rec.fullName,
                username = rec.username,
                countryIso = rec.phoneCountryIso,
                countryName = countryName,
                avatarUrl = avatarUrl,
                iFollow = rec.iFollow,
                followsMe = rec.followsMe,
                isFriend = rec.iFollow && rec.followsMe
            )
        }

        val filtered = allContacts.filter { c ->
            val tabOk = when (tab) {
                "", "all" -> true
                "suggestions" -> !c.iFollow && !c.followsMe
                "followers" -> c.followsMe
                "following" -> c.iFollow
                "friends" -> c.iFollow && c.followsMe
                else -> true
            }
            val qOk = if (query.isEmpty()) {
                true
            } else {
                c.fullName.lowercase().contains(query) ||
                    c.username.lowercase().contains(query) ||
                    c.countryName.lowercase().contains(query)
            }
            tabOk && qOk
        }

        call.respond(HttpStatusCode.OK, ContactsListResponse(contacts = filtered))
    }

    post("/api/v1/contacts/{targetUserId}/follow") {
        val claims = call.authenticateDeviceBound(repo, crypto) ?: return@post
        val targetUserId = call.parameters["targetUserId"].orEmpty().trim()
        val validUuid = runCatching { UUID.fromString(targetUserId) }.getOrNull()
        if (validUuid == null || targetUserId == claims.userId) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid target user"))
            return@post
        }

        val now = nowProvider()
        val windowStart = now.minusSeconds(60)
        val recentCount = repo.countRecentFollows(claims.userId, windowStart)
        val devId = call.request.headers["X-Telefam-Device-Id"].orEmpty()
        val hw = call.request.headers["X-Telefam-Hardware"].orEmpty().take(128)

        if (recentCount >= MAX_FOLLOWS_PER_MINUTE) {
            repo.appendSecurityAuditLog(
                AuditLogEntry(
                    userId = claims.userId,
                    eventType = "FOLLOW_RATE_LIMIT",
                    severity = "WARN",
                    deviceHardwareName = hw,
                    imeiOrDeviceHash = devId,
                    ipAddress = call.clientIp(),
                    deviceIdHash = devId,
                    success = false,
                    detailsJson = """{"recentCount":$recentCount}"""
                )
            )
            call.respond(HttpStatusCode.TooManyRequests, ErrorResponse(FOLLOW_RATE_ERROR))
            return@post
        }

        val result = repo.followUser(claims.userId, targetUserId)
        if (result == null) {
            call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))
            return@post
        }
        repo.recordFollowRateEvent(claims.userId, now)
        repo.appendSecurityAuditLog(
            AuditLogEntry(
                userId = claims.userId,
                eventType = "CONTACT_FOLLOWED",
                severity = "INFO",
                deviceHardwareName = hw,
                imeiOrDeviceHash = devId,
                ipAddress = call.clientIp(),
                deviceIdHash = devId,
                success = true,
                detailsJson = """{"targetUserId":"$targetUserId","isFriend":${result.iFollow && result.followsMe}}"""
            )
        )

        call.respond(
            HttpStatusCode.OK,
            FollowActionResponse(
                targetUserId = result.targetUserId,
                iFollow = result.iFollow,
                followsMe = result.followsMe,
                isFriend = result.iFollow && result.followsMe
            )
        )
    }

    delete("/api/v1/contacts/{targetUserId}/follow") {
        val claims = call.authenticateDeviceBound(repo, crypto) ?: return@delete
        val targetUserId = call.parameters["targetUserId"].orEmpty().trim()
        val validUuid = runCatching { UUID.fromString(targetUserId) }.getOrNull()
        if (validUuid == null || targetUserId == claims.userId) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid target user"))
            return@delete
        }

        val result = repo.unfollowUser(claims.userId, targetUserId)
        if (result == null) {
            call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))
            return@delete
        }

        val devId = call.request.headers["X-Telefam-Device-Id"].orEmpty()
        val hw = call.request.headers["X-Telefam-Hardware"].orEmpty().take(128)
        repo.appendSecurityAuditLog(
            AuditLogEntry(
                userId = claims.userId,
                eventType = "CONTACT_UNFOLLOWED",
                severity = "INFO",
                deviceHardwareName = hw,
                imeiOrDeviceHash = devId,
                ipAddress = call.clientIp(),
                deviceIdHash = devId,
                success = true,
                detailsJson = """{"targetUserId":"$targetUserId"}"""
            )
        )

        call.respond(
            HttpStatusCode.OK,
            FollowActionResponse(
                targetUserId = result.targetUserId,
                iFollow = result.iFollow,
                followsMe = result.followsMe,
                isFriend = false
            )
        )
    }
}
