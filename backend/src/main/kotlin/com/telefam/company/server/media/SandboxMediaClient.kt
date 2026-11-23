package com.telefam.company.server.media

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.file.Path
import java.util.Base64

data class ProcessedAvatarResult(
    val jpegBytes: ByteArray,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val sha256Hex: String,
    val dhashHex: String
)

interface MediaSandboxProcessor {
    fun process(
        rawBytes: ByteArray,
        cropX: Double,
        cropY: Double,
        cropWidth: Double,
        cropHeight: Double,
        maxWidth: Int = 512,
        maxHeight: Int = 512,
        mode: String = "avatar"
    ): Result<ProcessedAvatarResult>
}

class UnixSocketSandboxMediaClient(
    private val socketPath: String
) : MediaSandboxProcessor {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class SandboxRequest(
        val cropX: Double,
        val cropY: Double,
        val cropWidth: Double,
        val cropHeight: Double,
        val maxWidth: Int = 512,
        val maxHeight: Int = 512,
        val mode: String = "avatar"
    )

    @Serializable
    private data class SandboxResponse(
        val ok: Boolean,
        val error: String = "",
        val mime: String = "image/jpeg",
        val width: Int = 0,
        val height: Int = 0,
        val sha256Hex: String = "",
        val dhashHex: String = "",
        val jpegBase64: String = ""
    )

    override fun process(
        rawBytes: ByteArray,
        cropX: Double,
        cropY: Double,
        cropWidth: Double,
        cropHeight: Double,
        maxWidth: Int,
        maxHeight: Int,
        mode: String
    ): Result<ProcessedAvatarResult> {
        if (rawBytes.isEmpty() || rawBytes.size > 10 * 1024 * 1024) {
            return Result.failure(IllegalArgumentException("Invalid image size"))
        }
        if (!hasValidImageMagicBytes(rawBytes)) {
            return Result.failure(IllegalArgumentException("Unsupported image format"))
        }

        return runCatching {
            val addr = UnixDomainSocketAddress.of(Path.of(socketPath))
            SocketChannel.open(addr).use { channel ->
                val metaBytes = json.encodeToString(
                    SandboxRequest.serializer(),
                    SandboxRequest(cropX, cropY, cropWidth, cropHeight, maxWidth, maxHeight, mode)
                ).toByteArray(Charsets.UTF_8)

                writeFramed(channel, metaBytes)
                writeFramed(channel, rawBytes)

                val respLenBuf = ByteBuffer.allocate(4)
                readFully(channel, respLenBuf)
                respLenBuf.flip()
                val respLen = respLenBuf.int
                require(respLen in 1..(16 * 1024 * 1024)) { "Invalid sandbox response length" }

                val respBuf = ByteBuffer.allocate(respLen)
                readFully(channel, respBuf)
                val respObj = json.decodeFromString(
                    SandboxResponse.serializer(),
                    String(respBuf.array(), Charsets.UTF_8)
                )
                require(respObj.ok) { respObj.error.ifEmpty { "Sandbox rejected image" } }

                ProcessedAvatarResult(
                    jpegBytes = Base64.getDecoder().decode(respObj.jpegBase64),
                    mimeType = respObj.mime,
                    width = respObj.width,
                    height = respObj.height,
                    sha256Hex = respObj.sha256Hex,
                    dhashHex = respObj.dhashHex
                )
            }
        }
    }

    private fun writeFramed(ch: SocketChannel, data: ByteArray) {
        val lenBuf = ByteBuffer.allocate(4).putInt(data.size).flip()
        while (lenBuf.hasRemaining()) ch.write(lenBuf)
        val payloadBuf = ByteBuffer.wrap(data)
        while (payloadBuf.hasRemaining()) ch.write(payloadBuf)
    }

    private fun readFully(ch: SocketChannel, dst: ByteBuffer) {
        while (dst.hasRemaining()) {
            val n = ch.read(dst)
            if (n < 0) error("Sandbox closed connection")
        }
    }

    companion object {
        fun hasValidImageMagicBytes(data: ByteArray): Boolean {
            if (data.size < 12) return false
            val isJpeg = data[0] == 0xFF.toByte() && data[1] == 0xD8.toByte() && data[2] == 0xFF.toByte()
            val isPng = data[0] == 0x89.toByte() && data[1] == 0x50.toByte() &&
                data[2] == 0x4E.toByte() && data[3] == 0x47.toByte() &&
                data[4] == 0x0D.toByte() && data[5] == 0x0A.toByte() &&
                data[6] == 0x1A.toByte() && data[7] == 0x0A.toByte()
            val isWebp = data[0] == 'R'.code.toByte() && data[1] == 'I'.code.toByte() &&
                data[2] == 'F'.code.toByte() && data[3] == 'F'.code.toByte() &&
                data[8] == 'W'.code.toByte() && data[9] == 'E'.code.toByte() &&
                data[10] == 'B'.code.toByte() && data[11] == 'P'.code.toByte()
            return isJpeg || isPng || isWebp
        }
    }
}
