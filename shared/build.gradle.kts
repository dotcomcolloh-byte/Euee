plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

group = "com.telefam.company"
version = "1.0.0"

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    jvmToolchain(21)

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
