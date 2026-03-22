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


    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

