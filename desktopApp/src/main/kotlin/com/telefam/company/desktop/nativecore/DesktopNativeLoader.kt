package com.telefam.company.desktop.nativecore

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale

object DesktopNativeLoader {

    @Volatile
    private var loaded: Boolean = false

    @Volatile
    var loadErrorMessage: String? = null
        private set

    fun ensureLoaded(): Boolean {
        if (loaded) return true
        synchronized(this) {
            if (loaded) return true
            val osName = System.getProperty("os.name").orEmpty().lowercase(Locale.US)
            val libFileName = when {
                osName.contains("win") -> "telefam_core.dll"
                osName.contains("mac") || osName.contains("darwin") -> "libtelefam_core.dylib"
                else -> "libtelefam_core.so"
            }
            val arch = System.getProperty("os.arch").orEmpty().lowercase(Locale.US)
            val archDir = when {
                arch.contains("aarch64") || arch.contains("arm64") -> "arm64"
                else -> "x64"
            }
            val osFolder = when {
                osName.contains("win") -> "windows-$archDir"
                osName.contains("mac") || osName.contains("darwin") -> "macos-$archDir"
                else -> "linux-$archDir"
            }

            // 1. Check explicit system property or CMake output directories
            val propDir = System.getProperty("telefam.native.dir")?.takeIf { it.isNotBlank() }
            val candidates = listOfNotNull(
                propDir?.let { File(it, libFileName) },
                propDir?.let { File(it, "Release/$libFileName") },
                File("build/native-cpp/$libFileName"),
                File("build/native-cpp/Release/$libFileName"),
                File("../shared/cpp/build/$libFileName")
            )
            for (candidate in candidates) {
                if (candidate.isFile) {
                    val result = runCatching {
                        System.load(candidate.canonicalPath)
                    }
                    if (result.isSuccess) {
                        loaded = true
                        loadErrorMessage = null
                        return true
                    }
                }
            }

            // 2. Check packaged resource inside JAR
            val resourcePath = "/native/$osFolder/$libFileName"
            val stream = DesktopNativeLoader::class.java.getResourceAsStream(resourcePath)
            if (stream != null) {
                val res = runCatching {
                    stream.use { input ->
                        val temp = Files.createTempFile("telefam_core_", "_$libFileName").toFile()
                        temp.deleteOnExit()
                        Files.copy(input, temp.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        System.load(temp.canonicalPath)
                    }
                }
                if (res.isSuccess) {
                    loaded = true
                    loadErrorMessage = null
                    return true
                }
            }

            // 3. Fallback to standard java.library.path
            val stdRes = runCatching {
                System.loadLibrary("telefam_core")
            }
            if (stdRes.isSuccess) {
                loaded = true
                loadErrorMessage = null
                return true
            }

            loadErrorMessage = "Native core library ($libFileName) is unavailable. Build C++ shared library with CMake first."
            return false
        }
    }
}
