package com.example.evolab.service.configService

import com.example.evolab.domain.config.Config
import com.example.evolab.repo.repoConfig.RepositoryConfig
import com.example.evolab.service.auxiliary.Either
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.Instant

class ConfigServiceImplTest {
    @Test
    fun createConfigRejectsInvalidModelName() {
        val service = ConfigServiceImpl(FakeRepositoryConfig())

        val result = service.createConfig(1, 1, " ", 10, 1, emptyMap())

        assertLeftEquals(result, ConfigError.InvalidModelName)
    }

    @Test
    fun createConfigRejectsInvalidMaxIter() {
        val service = ConfigServiceImpl(FakeRepositoryConfig())

        val result = service.createConfig(1, 1, "model", 0, 1, emptyMap())

        assertLeftEquals(result, ConfigError.InvalidMaxIterations)
    }

    @Test
    fun createConfigRejectsInvalidCheckpoint() {
        val service = ConfigServiceImpl(FakeRepositoryConfig())

        val result = service.createConfig(1, 1, "model", 5, 6, emptyMap())

        assertLeftEquals(result, ConfigError.InvalidCheckpointInterval)
    }

    @Test
    fun createConfigReturnsCreatedConfig() {
        val repo = FakeRepositoryConfig()
        val service = ConfigServiceImpl(repo)

        val result = service.createConfig(7, 10, "model", 20, 5, mapOf("k" to "v"))
        val config = assertRight(result)

        assertEquals(7, config.userId)
        assertEquals("model", config.modelName)
    }

    @Test
    fun getConfigByIdRejectsAccessFromDifferentUser() {
        val repo = FakeRepositoryConfig()
        val config = repo.seed(userId = 1)
        val service = ConfigServiceImpl(repo)

        val result = service.getConfigById(config.id, userId = 2)

        assertLeftEquals(result, ConfigError.AccessDenied)
    }

    @Test
    fun updateConfigPersistsNewValues() {
        val repo = FakeRepositoryConfig()
        val current = repo.seed(userId = 1)
        val service = ConfigServiceImpl(repo)

        val result =
            service.updateConfig(
                configId = current.id,
                userId = 1,
                modelName = "new-model",
                maxIter = 100,
                checkPointInterval = 10,
                additionalParams = mapOf("temperature" to "0.1"),
            )

        val updated = assertRight(result)
        assertEquals("new-model", updated.modelName)
        assertEquals(100, updated.maxIter)
        assertEquals("0.1", updated.additionalParams["temperature"])
    }

    @Test
    fun deleteConfigReturnsErrorWhenRepositoryDeleteFails() {
        val repo = FakeRepositoryConfig(forceDeleteFailure = true)
        val current = repo.seed(userId = 1)
        val service = ConfigServiceImpl(repo)

        val result = service.deleteConfig(current.id, userId = 1)

        assertLeftEquals(result, ConfigError.ErrorDeletingConfig)
    }

    @Test
    fun buildRuntimeConfigEnrichesAdditionalParamsWithProjectAndJob() {
        val service = ConfigServiceImpl(FakeRepositoryConfig())
        val config =
            Config(
                id = 1,
                userId = 7,
                llmCredentialsId = 10,
                modelName = "model",
                maxIter = 20,
                checkPointInterval = 5,
                additionalParams = mapOf("a" to "b"),
                createdAt = Instant.now(),
            )

        val runtime = service.buildRuntimeConfig(config, projectId = 99, jobId = 42)

        assertEquals("99", runtime.additionalParams["projectId"])
        assertEquals("42", runtime.additionalParams["jobId"])
        assertEquals("b", runtime.additionalParams["a"])
    }

    @Test
    fun generateAndCleanupTemporaryConfigFileWorks() {
        val service = ConfigServiceImpl(FakeRepositoryConfig())

        val fileResult = service.generateTemporaryConfigFile(validRuntimePayload())
        val path = assertRight(fileResult)

        assertTrue(Files.exists(path))
        assertTrue(path.toString().endsWith(".yaml"))
        assertTrue(Files.readString(path).contains("max_iterations: 100"))

        val cleanup = service.cleanupTemporaryConfigFile(path)
        val deleted = assertRight(cleanup)

        assertTrue(deleted)
    }

    @Test
    fun generateTemporaryConfigFileReturnsErrorWhenPayloadIsInvalid() {
        val service = ConfigServiceImpl(FakeRepositoryConfig())
        val invalidPayload = mapOf("max_iterations" to 100)

        val result = service.generateTemporaryConfigFile(invalidPayload)

        assertTrue(result is Either.Left)
        assertTrue((result as Either.Left).value is ConfigError.InvalidOpenEvolveConfig)
    }

    @Test
    fun generateTemporaryConfigFileReturnsErrorWhenCustomFeaturesAreNotDeclared() {
        val service = ConfigServiceImpl(FakeRepositoryConfig())
        val payload = validRuntimePayload().toMutableMap()
        val database = (payload["database"] as Map<String, Any>).toMutableMap()
        database["feature_dimensions"] = listOf("complexity", "correctness")
        payload["database"] = database

        val result = service.generateTemporaryConfigFile(payload)

        assertTrue(result is Either.Left)
        assertTrue((result as Either.Left).value is ConfigError.InvalidOpenEvolveConfig)
    }

    private fun <L, R> assertRight(result: Either<L, R>): R {
        assertTrue(result is Either.Right)
        return (result as Either.Right).value
    }

    private fun <L, R> assertLeftEquals(result: Either<L, R>, expected: L) {
        assertTrue(result is Either.Left)
        assertEquals(expected, (result as Either.Left).value)
    }
}

private fun validRuntimePayload(): Map<String, Any> =
    mapOf(
        "max_iterations" to 100,
        "checkpoint_interval" to 10,
        "diff_based_evolution" to true,
        "llm" to
            mapOf(
                "models" to listOf(mapOf("name" to "gpt-4.1-mini", "weight" to 1.0)),
                "temperature" to 0.7,
                "top_p" to 0.95,
                "max_tokens" to 4096,
                "timeout" to 60,
                "retries" to 3,
            ),
        "prompt" to
            mapOf(
                "system_message" to "You are an OpenEvolve assistant.",
                "num_top_programs" to 3,
                "num_diverse_programs" to 2,
                "include_artifacts" to true,
            ),
        "database" to
            mapOf(
                "population_size" to 100,
                "archive_size" to 50,
                "num_islands" to 4,
                "migration_interval" to 10,
                "migration_rate" to 0.1,
                "elite_selection_ratio" to 0.1,
                "exploration_ratio" to 0.2,
                "exploitation_ratio" to 0.7,
                "feature_dimensions" to listOf("complexity", "diversity"),
                "feature_bins" to 10,
            ),
        "evaluator" to
            mapOf(
                "timeout" to 300,
                "max_retries" to 3,
                "parallel_evaluations" to 4,
                "cascade_evaluation" to true,
                "cascade_thresholds" to listOf(0.5, 0.75, 0.9),
                "declared_custom_metrics" to emptyList<String>(),
            ),
    )

private class FakeRepositoryConfig(
    private val forceDeleteFailure: Boolean = false,
) : RepositoryConfig {
    private val configs = linkedMapOf<Int, Config>()
    private var nextId = 1

    fun seed(userId: Int): Config {
        val id =
            createConfig(
                userId = userId,
                llmCredentialsId = 1,
                modelName = "seed",
                maxIter = 10,
                checkPointInterval = 2,
                additionalParams = emptyMap(),
            )
        return configs[id]!!
    }

    override fun createConfig(
        userId: Int,
        llmCredentialsId: Int,
        modelName: String,
        maxIter: Int,
        checkPointInterval: Int,
        additionalParams: Map<String, String>,
    ): Int {
        val id = nextId++
        configs[id] =
            Config(
                id = id,
                userId = userId,
                llmCredentialsId = llmCredentialsId,
                modelName = modelName,
                maxIter = maxIter,
                checkPointInterval = checkPointInterval,
                additionalParams = additionalParams,
                createdAt = Instant.now(),
            )
        return id
    }

    override fun findAllByUserId(userId: Int): List<Config> = configs.values.filter { it.userId == userId }

    override fun findAllByLlmCredentialId(llmCredentialsId: Int): List<Config> =
        configs.values.filter { it.llmCredentialsId == llmCredentialsId }

    override fun findAllByModelName(modelName: String): List<Config> =
        configs.values.filter { it.modelName == modelName }

    override fun findById(id: Int): Config? = configs[id]

    override fun findAll(): List<Config> = configs.values.toList()

    override fun save(entity: Config) {
        configs[entity.id] = entity
    }

    override fun deleteById(id: Int): Boolean {
        if (forceDeleteFailure) return false
        return configs.remove(id) != null
    }

    override fun clear() {
        configs.clear()
    }
}


