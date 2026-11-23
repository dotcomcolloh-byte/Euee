plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    application
}

group = "com.telefam.company"
version = "1.0.0"

application {
    mainClass.set("com.telefam.company.server.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:3.1.1")
    implementation("io.ktor:ktor-server-netty-jvm:3.1.1")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.1.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.1.1")
    implementation("io.ktor:ktor-server-status-pages-jvm:3.1.1")
    implementation("io.ktor:ktor-server-default-headers-jvm:3.1.1")
    implementation("io.ktor:ktor-server-call-logging-jvm:3.1.1")

    implementation("io.ktor:ktor-client-core-jvm:3.1.1")
    implementation("io.ktor:ktor-client-cio-jvm:3.1.1")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:3.1.1")

    implementation("org.postgresql:postgresql:42.7.5")
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("org.flywaydb:flyway-core:11.3.4")
    implementation("org.flywaydb:flyway-database-postgresql:11.3.4")

    implementation("com.google.api-client:google-api-client:2.7.2")
    implementation("ch.qos.logback:logback-classic:1.5.16")

    testImplementation("io.ktor:ktor-server-test-host-jvm:3.1.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.1.20")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
