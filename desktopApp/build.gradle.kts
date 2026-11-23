import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.File

plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.compose") version "2.1.20"
    id("org.jetbrains.compose") version "1.7.3"
    kotlin("plugin.serialization") version "2.1.20"
}

group = "com.telefam.company.desktop"
version = "1.0.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.foundation)
    implementation(compose.runtime)
    implementation(compose.ui)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

kotlin {
    jvmToolchain(21)
}

val nativeBuildDir = layout.buildDirectory.dir("native-cpp")

tasks.register("buildDesktopNative") {
    group = "build"
    description = "Builds shared C++17 telefam_core shared library via CMake when host toolchain is available."
    doLast {
        val cmakeBin = listOf(
            System.getenv("CMAKE_BIN"),
            "/opt/android/sdk/cmake/3.22.1/bin/cmake",
            "cmake"
        ).firstOrNull { !it.isNullOrBlank() && (it == "cmake" || File(it).canExecute()) }

        val hasCompiler = listOf("g++", "clang++", "c++", "cl.exe").any { exe ->
            System.getenv("PATH")?.split(File.pathSeparator)
                ?.any { dir -> File(dir, exe).canExecute() } == true
        }

        if (cmakeBin == null || !hasCompiler) {
            logger.lifecycle("Host C++ compiler or CMake unavailable; skipping host telefam_core build.")
            return@doLast
        }

        val outDir = nativeBuildDir.get().asFile
        outDir.mkdirs()
        val cppSrc = rootProject.file("../shared/cpp").canonicalPath

        fun runCmd(vararg args: String) {
            val process = ProcessBuilder(*args)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            if (output.isNotBlank()) logger.lifecycle(output)
            if (exitCode != 0) {
                throw GradleException("CMake command failed ($exitCode): ${args.joinToString(" ")}")
            }
        }

        runCmd(cmakeBin, "-S", cppSrc, "-B", outDir.absolutePath, "-DCMAKE_BUILD_TYPE=Release")
        runCmd(cmakeBin, "--build", outDir.absolutePath, "--target", "telefam_core", "--config", "Release")
    }
}

compose.desktop {
    application {
        mainClass = "com.telefam.company.desktop.MainKt"

        jvmArgs(
            "-Dtelefam.native.dir=${nativeBuildDir.get().asFile.absolutePath}"
        )

        nativeDistributions {
            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Msi,
                TargetFormat.Exe,
                TargetFormat.Deb,
                TargetFormat.AppImage
            )
            packageName = "Telefam"
            packageVersion = "1.0.0"
            description = "Telefam Desktop Client"
            vendor = "Telefam"
            copyright = "Copyright (c) Telefam"

            windows {
                menuGroup = "Telefam"
                upgradeUuid = "3c26728c-94de-4d8a-81c9-943fb42450f1"
                perUserInstall = true
            }
            macOS {
                bundleID = "com.telefam.company.app.desktop"
            }
            linux {
                packageName = "telefam"
                menuGroup = "Network"
                appCategory = "Network"
            }
        }
    }
}
