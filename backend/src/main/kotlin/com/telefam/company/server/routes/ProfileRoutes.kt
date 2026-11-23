package com.telefam.company.server.routes

import com.telefam.company.server.config.ServerConfig
import com.telefam.company.server.crypto.ServerCrypto
import com.telefam.company.server.db.AuditLogEntry
import com.telefam.company.server.db.TelefamRepository
import com.telefam.company.server.media.MediaSandboxProcessor
import com.telefam.company.server.media.UnixSocketSandboxMediaClient
import com.telefam.company.server.model.BackgroundPhotosResponse
import com.telefam.company.server.model.ErrorResponse
import com.telefam.company.server.model.PhotoUploadResponse
import com.telefam.company.server.model.ProfileUpdateRequest
import com.telefam.company.server.model.ProfileUpdateResponse
import com.telefam.company.server.model.UsernameCheckResponse
import com.telefam.company.server.validation.Validators
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.UUID

private const val MAX_BACKGROUND_PHOTOS = 5
private const val USERNAME_COOLDOWN_SECONDS = 14L * 86_400L

fun Route.profileRoutes(
    config: ServerConfig,
    repo: TelefamRepository,
    crypto: ServerCrypto,
    sandboxProcessor: MediaSandboxProcessor,
    nowProvider: () -> Instant = { Instant.now() }
) {
    get("/api/v1/profile") {
        val claims = call.authenticateDeviceBound(repo, crypto) ?: return@get
        val user = repo.findUserById(claims.userId)
        if (user == null) {
            call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))
            return@get
        }
        val stats = repo.getProfileStats(claims.userId)
        val backgrounds = repo.listUserBackgroundPhotos(claims.userId)
        call.respond(
            HttpStatusCode.OK,
            ProfileUpdateResponse(
                profileCompleted = user.profileCompleted,
                profile = user.toProfileDto(config, crypto, stats, backgrounds, nowProvider())
            )
        )
    }

    get("/api/v1/profile/check-username") {
        val claims = call.authenticateDeviceBound(repo, crypto) ?: return@get
        val username = call.request.queryParameters["username"].orEmpty().trim().lowercase()
        if (!Validators.isValidUsername(username)) {
            call.respond(HttpStatusCode.OK, UsernameCheckResponse(username, available = false))
            return@get
        }
        val available = repo.isUsernameAvailable(username, claims.userId)
        call.respond(HttpStatusCode.OK, UsernameCheckResponse(username, available))
    }

    post("/api/v1/profile/photo") {
        val claims = call.authenticateDeviceBound(repo, crypto) ?: return@post
        var cropX = 0.0
        var cropY = 0.0
        var cropW = 1.0
        var cropH = 1.0
        var photoBytes: ByteArray? = null

        val multipart = call.receiveMultipart(formFieldLimit = 10 * 1024 * 1024)
        multipart.forEachPart { part ->
            when (part) {
                is PartData.FormItem -> {
                    when (part.name) {
                        "cropX" -> cropX = part.value.toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0
                        "cropY" -> cropY = part.value.toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0
                        "cropWidth" -> cropW = part.value.toDoubleOrNull()?.takeIf { it.isFinite() } ?: 1.0
                        "cropHeight" -> cropH = part.value.toDoubleOrNull()?.takeIf { it.isFinite() } ?: 1.0
                    }
                }
                is PartData.FileItem -> {
                    if (part.name == "photo") {
                        photoBytes = part.provider().readRemaining(10L * 1024 * 1024 + 1).readByteArray()
                    }
                }
                else -> {}
            }
            part.dispose()
        }

        val raw = photoBytes
        if (raw == null || raw.isEmpty() || raw.size > 10 * 1024 * 1024) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid image size"))
            return@post
        }
        if (!UnixSocketSandboxMediaClient.hasValidImageMagicBytes(raw)) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid image file"))
            return@post
        }

        val processed = sandboxProcessor.process(raw, cropX, cropY, cropW, cropH, 512, 512, "avatar").getOrElse {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid image file"))
            return@post
        }

        val objectKey = "${UUID.randomUUID()}.jpg"
        val baseDir = Path.of(config.avatarStorageDir).toAbsolutePath().normalize()
        Files.createDirectories(baseDir)
        val resolved = baseDir.resolve(objectKey).normalize()
        require(resolved.startsWith(baseDir)) { "Path traversal blocked" }
        Files.write(resolved, processed.jpegBytes)

        repo.updateUserAvatar(
            userId = claims.userId,
            objectKey = objectKey,
            sha256Hex = processed.sha256Hex,
            dhashHex = processed.dhashHex,
            mimeType = processed.mimeType,
            byteSize = processed.jpegBytes.size,
            width = processed.width,
            height = processed.height
        )

        val devId = call.request.headers["X-Telefam-Device-Id"].orEmpty()
        val hw = call.request.headers["X-Telefam-Hardware"].orEmpty().take(128)
        repo.appendSecurityAuditLog(
            AuditLogEntry(claims.userId, "PHOTO_UPLOADED", "INFO", hw, devId, call.clientIp(), devId, true, """{"sha256":"${processed.sha256Hex}","dhash":"${processed.dhashHex}"}""")
        )

        call.respond(
            HttpStatusCode.OK,
            PhotoUploadResponse(
                avatarUrl = "${config.publicBaseUrl}/api/v1/media/avatar/$objectKey",
                fingerprintSha256 = processed.sha256Hex,
                fingerprintDhash = processed.dhashHex,
                width = processed.width,
                height = processed.height
            )
        )
    }

    post("/api/v1/profile/backgrounds") {
        val claims = call.authenticateDeviceBound(repo, crypto) ?: return@post
        val currentCount = repo.countUserBackgroundPhotos(claims.userId)
        if (currentCount >= MAX_BACKGROUND_PHOTOS) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Maximum 5 background photos allowed"))
            return@post
        }

        var cropX = 0.0
        var cropY = 0.0
        var cropW = 1.0
        var cropH = 1.0
        var photoBytes: ByteArray? = null

        val multipart = call.receiveMultipart(formFieldLimit = 10 * 1024 * 1024)
        multipart.forEachPart { part ->
            when (part) {
                is PartData.FormItem -> {
                    when (part.name) {
                        "cropX" -> cropX = part.value.toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0
                        "cropY" -> cropY = part.value.toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0
                        "cropWidth" -> cropW = part.value.toDoubleOrNull()?.takeIf { it.isFinite() } ?: 1.0
                        "cropHeight" -> cropH = part.value.toDoubleOrNull()?.takeIf { it.isFinite() } ?: 1.0
                    }
                }
                is PartData.FileItem -> {
                    if (part.name == "photo") {
                        photoBytes = part.provider().readRemaining(10L * 1024 * 1024 + 1).readByteArray()
                    }
                }
                else -> {}
            }
            part.dispose()
        }

        val raw = photoBytes
        if (raw == null || raw.isEmpty() || raw.size > 10 * 1024 * 1024) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid image size"))
            return@post
        }
        if (!UnixSocketSandboxMediaClient.hasValidImageMagicBytes(raw)) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid image file"))
            return@post
        }

        val processed = sandboxProcessor.process(raw, cropX, cropY, cropW, cropH, 1440, 900, "background").getOrElse {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid image file"))
            return@post
        }

        val objectKey = "${UUID.randomUUID()}.jpg"
        val baseDir = Path.of(config.avatarStorageDir).toAbsolutePath().normalize()
        Files.createDirectories(baseDir)
        val resolved = baseDir.resolve(objectKey).normalize()
        require(resolved.startsWith(baseDir)) { "Path traversal blocked" }
        Files.write(resolved, processed.jpegBytes)

        repo.addUserBackgroundPhoto(
            userId = claims.userId,
            objectKey = objectKey,
            sha256Hex = processed.sha256Hex,
            dhashHex = processed.dhashHex,
            mimeType = processed.mimeType,
            byteSize = processed.jpegBytes.size,
            width = processed.width,
            height = processed.height
        )

        val devId = call.request.headers["X-Telefam-Device-Id"].orEmpty()
        val hw = call.request.headers["X-Telefam-Hardware"].orEmpty().take(128)
        repo.appendSecurityAuditLog(
            AuditLogEntry(claims.userId, "BACKGROUND_PHOTO_UPLOADED", "INFO", hw, devId, call.clientIp(), devId, true, """{"sha256":"${processed.sha256Hex}","dhash":"${processed.dhashHex}"}""")
        )

        val backgrounds = repo.listUserBackgroundPhotos(claims.userId).map { it.toBackgroundDto(config) }
        call.respond(HttpStatusCode.OK, BackgroundPhotosResponse(backgroundPhotos = backgrounds))
    }

    delete("/api/v1/profile/backgrounds/{photoId}") {
        val claims = call.authenticateDeviceBound(repo, crypto) ?: return@delete
        val photoId = call.parameters["photoId"].orEmpty().trim()
        val validUuid = runCatching { UUID.fromString(photoId) }.getOrNull()
        if (validUuid == null) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid photo id"))
            return@delete
        }

        val deletedObjectKey = repo.deleteUserBackgroundPhoto(claims.userId, photoId)
        if (deletedObjectKey == null) {
            call.respond(HttpStatusCode.NotFound, ErrorResponse("Background photo not found"))
            return@delete
        }

        val baseDir = Path.of(config.avatarStorageDir).toAbsolutePath().normalize()
        val resolved = baseDir.resolve(deletedObjectKey).normalize()
        if (resolved.startsWith(baseDir)) {
            runCatching { Files.deleteIfExists(resolved) }
        }

        val devId = call.request.headers["X-Telefam-Device-Id"].orEmpty()
        val hw = call.request.headers["X-Telefam-Hardware"].orEmpty().take(128)
        repo.appendSecurityAuditLog(
            AuditLogEntry(claims.userId, "BACKGROUND_PHOTO_DELETED", "INFO", hw, devId, call.clientIp(), devId, true, """{"photoId":"$photoId"}""")
        )

        val backgrounds = repo.listUserBackgroundPhotos(claims.userId).map { it.toBackgroundDto(config) }
        call.respond(HttpStatusCode.OK, BackgroundPhotosResponse(backgroundPhotos = backgrounds))
    }

    get("/api/v1/media/avatar/{key}") {
        val key = call.parameters["key"].orEmpty()
        if (!Regex("^[a-f0-9-]{36}\\.jpg$").matches(key)) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }
        val baseDir = Path.of(config.avatarStorageDir).toAbsolutePath().normalize()
        val resolved = baseDir.resolve(key).normalize()
        if (!resolved.startsWith(baseDir) || !Files.exists(resolved)) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }
        call.respondBytes(Files.readAllBytes(resolved), ContentType.Image.JPEG)
    }

    get("/api/v1/media/background/{key}") {
        val key = call.parameters["key"].orEmpty()
        if (!Regex("^[a-f0-9-]{36}\\.jpg$").matches(key)) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }
        val baseDir = Path.of(config.avatarStorageDir).toAbsolutePath().normalize()
        val resolved = baseDir.resolve(key).normalize()
        if (!resolved.startsWith(baseDir) || !Files.exists(resolved)) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }
        call.respondBytes(Files.readAllBytes(resolved), ContentType.Image.JPEG)
    }

    put("/api/v1/profile") {
        val claims = call.authenticateDeviceBound(repo, crypto) ?: return@put
        val req = call.receive<ProfileUpdateRequest>()
        val existingUser = repo.findUserById(claims.userId)
        if (existingUser == null) {
            call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))
            return@put
        }

        val now = nowProvider()
        val fullName = req.fullName.trim()
        val username = req.username.trim().lowercase()
        val location = req.location.trim()
        val bio = req.bio.trim()

        if (fullName.length !in 2..64) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Enter a valid full name"))
            return@put
        }
        if (!Validators.isValidUsername(username)) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid username format"))
            return@put
        }

        val isChangingExistingUsername = !existingUser.usernameLower.isNullOrBlank() && existingUser.usernameLower != username
        if (isChangingExistingUsername) {
            val lastChanged = existingUser.usernameChangedAt
            if (lastChanged != null && now.isBefore(lastChanged.plusSeconds(USERNAME_COOLDOWN_SECONDS))) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Username can only be changed every 14 days"))
                return@put
            }
        }

        if (!repo.isUsernameAvailable(username, claims.userId)) {
            call.respond(HttpStatusCode.Conflict, ErrorResponse("Username is already taken"))
            return@put
        }
        val normalizedPhone = Validators.normalizeAndValidatePhone(req.phoneCountryIso, req.phoneNumber)
            .getOrElse {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(it.message ?: "Invalid phone number"))
                return@put
            }
        if (!Validators.isValidGender(req.gender)) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Select a valid gender"))
            return@put
        }
        val dobError = Validators.validateAge13Plus(req.dateOfBirth)
        if (dobError != null) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(dobError))
            return@put
        }
        if (location.length !in 2..80) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Enter a valid location"))
            return@put
        }
        if (bio.length > 150) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Bio must be 150 characters or less"))
            return@put
        }

        val nextUsernameChangedAt = if (existingUser.usernameLower.isNullOrBlank() || isChangingExistingUsername) {
            now
        } else {
            null
        }

        val updated = repo.updateUserProfile(
            userId = claims.userId,
            fullName = fullName,
            username = username,
            usernameLower = username,
            phoneEncrypted = crypto.encryptAtRest(normalizedPhone, claims.userId),
            phoneHmac = crypto.hmacHex("phone:$normalizedPhone"),
            phoneCountryIso = req.phoneCountryIso.trim().uppercase(),
            gender = req.gender,
            dobEncrypted = crypto.encryptAtRest(req.dateOfBirth.trim(), claims.userId),
            locationEncrypted = crypto.encryptAtRest(location, claims.userId),
            bioEncrypted = crypto.encryptAtRest(bio, claims.userId),
            usernameChangedAt = nextUsernameChangedAt
        )

        val devId = call.request.headers["X-Telefam-Device-Id"].orEmpty()
        val hw = call.request.headers["X-Telefam-Hardware"].orEmpty().take(128)
        repo.appendSecurityAuditLog(
            AuditLogEntry(claims.userId, "PROFILE_UPDATED", "INFO", hw, devId, call.clientIp(), devId, true, "{}")
        )

        val stats = repo.getProfileStats(claims.userId)
        val backgrounds = repo.listUserBackgroundPhotos(claims.userId)
        call.respond(
            HttpStatusCode.OK,
            ProfileUpdateResponse(
                profileCompleted = true,
                profile = updated.toProfileDto(config, crypto, stats, backgrounds, now)
            )
        )
    }
}
