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

    implementation("com.github.docker-java:docker-java:3.3.6")
    implementation("com.github.docker-java:docker-java-transport-httpclient5:3.3.6")
    implementation("com.github.docker-java:docker-java-transport-zerodep:3.3.6")
    implementation("org.apache.commons:commons-compress:1.27.1")

    implementation("jakarta.annotation:jakarta.annotation-api:2.1.1")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}
