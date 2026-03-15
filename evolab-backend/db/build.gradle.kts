plugins {
    kotlin("jvm") version "2.2.21"
}

group = "com.example.evolab"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // JDBI
    implementation("org.jdbi:jdbi3-core:3.47.0")
    implementation("org.jdbi:jdbi3-kotlin:3.47.0")
    implementation("org.jdbi:jdbi3-postgres:3.47.0")

    // PostgreSQL driver
    implementation("org.postgresql:postgresql:42.7.4")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
    environment(
        "DB_URL",
        "jdbc:postgresql://localhost:5432/evolab?user=evolabuser&password=changeit"
    )
}

/**
 * Docker tasks for the EvoLab database container.
 *
 * Useful commands:
 *   - Start:   ./gradlew :evolab-backend:db:dbUp
 *   - Stop:    ./gradlew :evolab-backend:db:dbDown
 *   - Connect: docker exec -ti evolab-db psql -d evolab -U evolabuser -W
 *
 * Data is persisted in the Docker volume "evolab-db-data".
 * To fully reset the DB (drop all data):
 *   docker compose -f evolab-backend/db/docker-compose.yml down -v
 */

val dockerComposePath =
    rootProject.layout.projectDirectory
        .file("evolab-backend/db/docker-compose.yml")
        .toString()

val dockerExe =
    when {
        org.gradle.internal.os.OperatingSystem.current().isMacOsX -> "/usr/local/bin/docker"
        org.gradle.internal.os.OperatingSystem.current().isWindows -> "docker"
        else -> "docker"
    }

tasks.register<Exec>("dbUp") {
    group = "docker"
    description = "Start the EvoLab PostgreSQL container"
    commandLine(dockerExe, "compose", "-f", dockerComposePath, "up", "-d", "--build", "--force-recreate", "evolab-db")
}

tasks.register<Exec>("dbDown") {
    group = "docker"
    description = "Stop the EvoLab PostgreSQL container (data is preserved)"
    commandLine(dockerExe, "compose", "-f", dockerComposePath, "down", "evolab-db")
}

tasks.register<Exec>("dbDownVolumes") {
    group = "docker"
    description = "Stop the EvoLab PostgreSQL container AND delete all data (volume)"
    commandLine(dockerExe, "compose", "-f", dockerComposePath, "down", "-v", "evolab-db")
}

