package com.example.evolab.repo.support

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.postgres.PostgresPlugin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.postgresql.ds.PGSimpleDataSource

abstract class RepositoryDbTestSupport {
    protected lateinit var jdbi: Jdbi
    protected lateinit var handle: Handle

    @BeforeEach
    fun setUpDb() {
        val dataSource =
            PGSimpleDataSource().apply {
                setURL(dbUrl())
            }

        jdbi =
            Jdbi
                .create(dataSource)
                .installPlugin(KotlinPlugin())
                .installPlugin(PostgresPlugin())

        handle = jdbi.open()
        cleanupDatabase()
    }

    @AfterEach
    fun tearDownDb() {
        cleanupDatabase()
        if (this::handle.isInitialized) {
            handle.close()
        }
    }

    protected fun insertUser(
        name: String = "user",
        email: String,
        passwordHash: String? = "hash",
        authProvider: String = "LOCAL",
        providerId: String = "LOCAL",
    ): Int =
        handle
            .createUpdate(
                """
                INSERT INTO users (name, email, password_hash, auth_provider, provider_id)
                VALUES (:name, :email, :passwordHash, CAST(:authProvider AS auth_provider), :providerId)
                """.trimIndent(),
            ).bind("name", name)
            .bind("email", email)
            .bind("passwordHash", passwordHash)
            .bind("authProvider", authProvider)
            .bind("providerId", providerId)
            .executeAndReturnGeneratedKeys()
            .mapTo(Int::class.java)
            .one()

    protected fun insertToken(
        tokenValidation: String,
        userId: Int,
        createdAt: Long,
        lastUsedAt: Long,
    ) {
        handle
            .createUpdate(
                """
                INSERT INTO tokens (token_validation, user_id, created_at, last_used_at)
                VALUES (:tokenValidation, :userId, :createdAt, :lastUsedAt)
                """.trimIndent(),
            ).bind("tokenValidation", tokenValidation)
            .bind("userId", userId)
            .bind("createdAt", createdAt)
            .bind("lastUsedAt", lastUsedAt)
            .execute()
    }

    protected fun insertLlmCredential(
        userId: Int,
        provider: String = "OPENAI",
        apiKeyEncrypted: String = "encrypted-key",
    ): Int =
        handle
            .createUpdate(
                """
                INSERT INTO llm_credentials (user_id, provider, api_key_encrypted)
                VALUES (:userId, CAST(:provider AS llm_provider), :apiKeyEncrypted)
                """.trimIndent(),
            ).bind("userId", userId)
            .bind("provider", provider)
            .bind("apiKeyEncrypted", apiKeyEncrypted)
            .executeAndReturnGeneratedKeys()
            .mapTo(Int::class.java)
            .one()

    private fun cleanupDatabase() {
        val tables =
            listOf(
                "checkpoints",
                "metrics",
                "jobs",
                "projects",
                "evolution_configs",
                "llm_credentials",
                "tokens",
                "users",
            )

        tables.forEach { table ->
            handle.createUpdate("DELETE FROM $table").execute()
        }
    }

    private fun dbUrl(): String =
        System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/evolab?user=evolabuser&password=changeit"
}

