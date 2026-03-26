plugins {
    kotlin("jvm") version "2.2.21"
}

group = "com.example.evolab"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":evolab-backend:domain"))
    implementation(project(":evolab-backend:repo"))

    implementation("jakarta.inject:jakarta.inject-api:2.0.1")
    implementation("org.springframework.security:spring-security-crypto:6.5.1")

    implementation("io.ktor:ktor-client-core:3.1.2")
    implementation("io.ktor:ktor-client-cio:3.1.2") // engine
    implementation("io.ktor:ktor-client-content-negotiation:3.1.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.2")



    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

