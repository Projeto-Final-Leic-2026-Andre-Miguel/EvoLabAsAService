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

    implementation("org.jdbi:jdbi3-core:3.47.0")
    implementation("org.jdbi:jdbi3-kotlin:3.47.0")
    implementation("org.jdbi:jdbi3-postgres:3.47.0")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

