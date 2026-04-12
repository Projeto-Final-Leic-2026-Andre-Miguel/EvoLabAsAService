package com.example.evolab.service.configService

import com.example.evolab.domain.config.Config
import com.example.evolab.domain.LLMCredentials.LLM
import com.example.evolab.domain.LLMCredentials.LLMCredentials
import com.example.evolab.domain.checkpoint.Checkpoint
import com.example.evolab.domain.evolution.EvolutionStatus
import com.example.evolab.domain.job.Job
import com.example.evolab.domain.metrics.Metric
import com.example.evolab.domain.project.Project
import com.example.evolab.domain.token.Token
import com.example.evolab.domain.token.TokenValidationInfo
import com.example.evolab.domain.user.AuthProvider
import com.example.evolab.domain.user.User
import com.example.evolab.repo.repoCheckpoints.RepositoryCheckpoints
import com.example.evolab.repo.repoConfig.RepositoryConfig
import com.example.evolab.repo.repoJobs.RepositoryJobs
import com.example.evolab.repo.repoLLMCredentials.RepositoryLLMCredentials
import com.example.evolab.repo.repoMetrics.RepositoryMetrics
import com.example.evolab.repo.repoProject.RepositoryProject
import com.example.evolab.repo.repoToken.RepositoryToken
import com.example.evolab.repo.repoUser.RepositoryUser
import com.example.evolab.repo.transactions.Transaction
import com.example.evolab.repo.transactions.TransactionManager
import com.example.evolab.service.auxiliary.Either
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.Instant

class ConfigServiceImplTest {
    @Test
    fun createConfigRejectsInvalidModelName() {
        val service = createService()

        val result = service.createConfig(1, null, 1, " ", 10, 1, emptyMap())

        assertLeftEquals(result, ConfigError.InvalidModelName)
    }

    @Test
    fun createConfigRejectsInvalidMaxIter() {
        val service = createService()

        val result = service.createConfig(1, null, 1, "model", 0, 1, emptyMap())

        assertLeftEquals(result, ConfigError.InvalidMaxIterations)
    }

    @Test
    fun createConfigRejectsInvalidCheckpoint() {
        val service = createService()

        val result = service.createConfig(1, null, 1, "model", 5, 6, emptyMap())

        assertLeftEquals(result, ConfigError.InvalidCheckpointInterval)
    }

    @Test
    fun createConfigReturnsCreatedConfig() {
        val repo = FakeRepositoryConfig()
        val service = createService(configRepo = repo)

        val result = service.createConfig(7, null, 10, "model", 20, 5, mapOf("k" to "v"))
        val config = assertRight(result)

        assertEquals(7, config.userId)
        assertEquals("model", config.modelName)
    }

    @Test
    fun createConfigAssignsConfigToProjectWhenProjectIdIsProvided() {
        val configRepo = FakeRepositoryConfig()
        val projectRepo = FakeRepositoryProject()
        val project = projectRepo.seed(userId = 7, configId = null, status = EvolutionStatus.CREATED)
        val service = createService(configRepo = configRepo, projectRepo = projectRepo)

        val result = service.createConfig(7, project.id, 10, "model", 20, 5, mapOf("k" to "v"))
        val created = assertRight(result)

        assertEquals(created.id, projectRepo.findById(project.id)?.configId)
    }

    @Test
    fun createConfigRejectsUnknownProject() {
        val service = createService()

        val result = service.createConfig(7, 999, 10, "model", 20, 5, emptyMap())

        assertLeftEquals(result, ConfigError.ProjectNotFound)
    }

    @Test
    fun getConfigByIdRejectsAccessFromDifferentUser() {
        val repo = FakeRepositoryConfig()
        val config = repo.seed(userId = 1)
        val service = createService(configRepo = repo)

        val result = service.getConfigById(config.id, userId = 2)

        assertLeftEquals(result, ConfigError.AccessDenied)
    }

    @Test
    fun updateConfigPersistsNewValues() {
        val repo = FakeRepositoryConfig()
        val current = repo.seed(userId = 1)
        val service = createService(configRepo = repo)

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
        val service = createService(configRepo = repo)

        val result = service.deleteConfig(current.id, userId = 1)

        assertLeftEquals(result, ConfigError.ErrorDeletingConfig)
    }

    @Test
    fun buildRuntimeConfigEnrichesAdditionalParamsWithProjectAndJob() {
        val service = createService()
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
        val service = createService()

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
    fun generateTemporaryConfigFileWorksWithPayloadBuiltFromConfigAndEnvironmentPlaceholder() {
        val service = createService()
        val config =
            Config(
                id = 1,
                userId = 7,
                llmCredentialsId = 10,
                modelName = "gpt-4.1-mini",
                maxIter = 20,
                checkPointInterval = 5,
                additionalParams =
                    mapOf(
                        "llm.temperature" to "0.6",
                        "database.population_size" to "150",
                    ),
                createdAt = Instant.now(),
            )

        val payload =
            OpenEvolvePayloadBuilder.build(
                config = config,
                apiKeyValue = OpenEvolvePayloadBuilder.apiKeyEnvironmentPlaceholder("OPENAI_API_KEY"),
            )
        val llmPayload = payload["llm"] as Map<*, *>

        assertEquals("\${OPENAI_API_KEY}", llmPayload["api_key"])

        val fileResult = service.generateTemporaryConfigFile(payload)
        val path = assertRight(fileResult)

        assertTrue(Files.exists(path))
        assertTrue(Files.readString(path).contains("\${OPENAI_API_KEY}"))
        assertRight(service.cleanupTemporaryConfigFile(path))
    }

    @Test
    fun generateTemporaryConfigFileReturnsErrorWhenPayloadIsInvalid() {
        val service = createService()
        val invalidPayload = mapOf("max_iterations" to 100)

        val result = service.generateTemporaryConfigFile(invalidPayload)

        assertTrue(result is Either.Left)
        assertTrue((result as Either.Left).value is ConfigError.InvalidOpenEvolveConfig)
    }

    @Test
    fun generateTemporaryConfigFileReturnsErrorWhenCustomFeaturesAreNotDeclared() {
        val service = createService()
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

    private fun createService(
        configRepo: FakeRepositoryConfig = FakeRepositoryConfig(),
        projectRepo: FakeRepositoryProject = FakeRepositoryProject(),
    ) = ConfigServiceImpl(configRepo, FakeTransactionManager(configRepo, projectRepo))
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

private class FakeRepositoryProject : RepositoryProject {
    private val projects = linkedMapOf<Int, Project>()
    private var nextId = 1

    fun seed(
        userId: Int,
        configId: Int?,
        status: EvolutionStatus,
    ): Project {
        val project =
            Project(
                id = nextId++,
                userId = userId,
                configId = configId,
                name = "project-$nextId",
                description = null,
                initialProgram = "def solve(x): return x",
                evaluatorCode = "def evaluate(candidate): return 1.0",
                status = status,
                createdAt = Instant.now(),
            )
        projects[project.id] = project
        return project
    }

    override fun createProject(
        userId: Int,
        name: String,
        description: String?,
        initialProgram: String?,
        evaluatorCode: String?,
        status: EvolutionStatus,
    ): Project = error("unused")

    override fun findAllByUserId(userId: Int): List<Project> = projects.values.filter { it.userId == userId }

    override fun findAllByConfigId(configId: Int): List<Project> = projects.values.filter { it.configId == configId }

    override fun findAllByStatus(status: EvolutionStatus): List<Project> = projects.values.filter { it.status == status }

    override fun findAllByName(name: String): List<Project> = projects.values.filter { it.name == name }

    override fun findById(id: Int): Project? = projects[id]

    override fun findAll(): List<Project> = projects.values.toList()

    override fun save(entity: Project) {
        projects[entity.id] = entity
    }

    override fun deleteById(id: Int): Boolean = projects.remove(id) != null

    override fun clear() {
        projects.clear()
    }
}

private class FakeTransactionManager(
    private val configRepo: RepositoryConfig,
    private val projectRepo: RepositoryProject,
) : TransactionManager {
    override fun <R> run(block: Transaction.() -> R): R = FakeTransaction(configRepo, projectRepo).block()
}

private class FakeTransaction(
    override val repoConfigs: RepositoryConfig,
    override val repoProjects: RepositoryProject,
) : Transaction {
    override val repoUsers: RepositoryUser =
        object : RepositoryUser {
            override fun createLocalUser(name: String, email: String, passwordHash: String): User = error("unused")
            override fun createOAuthUser(name: String, email: String, provider: AuthProvider, providerId: String): User = error("unused")
            override fun findByEmail(email: String): User? = error("unused")
            override fun findByProvider(provider: AuthProvider, providerId: String): User? = error("unused")
            override fun findByTokenValidation(tokenValidationInfo: TokenValidationInfo): User? = error("unused")
            override fun count(): Long = error("unused")
            override fun findById(id: Int): User? = error("unused")
            override fun findAll(): List<User> = error("unused")
            override fun save(entity: User) = error("unused")
            override fun deleteById(id: Int): Boolean = error("unused")
            override fun clear() = error("unused")
        }
    override val repoLLmCredentials: RepositoryLLMCredentials =
        object : RepositoryLLMCredentials {
            override fun createLLMCredential(userId: Int, provider: LLM, apiKeyEncrypted: String): LLMCredentials = error("unused")
            override fun findAllByUserId(userId: Int): List<LLMCredentials> = error("unused")
            override fun findAllByProvider(provider: LLM): List<LLMCredentials> = error("unused")
            override fun findById(id: Int): LLMCredentials? = error("unused")
            override fun findAll(): List<LLMCredentials> = error("unused")
            override fun save(entity: LLMCredentials) = error("unused")
            override fun deleteById(id: Int): Boolean = error("unused")
            override fun clear() = error("unused")
        }
    override val repoJobs: RepositoryJobs =
        object : RepositoryJobs {
            override fun createJob(projectId: Int, status: EvolutionStatus, containerId: String?, startedAt: Instant?, finishedAt: Instant?, bestSolution: String?, executionLogs: String?): Int = error("unused")
            override fun findAllByProjectId(projectId: Int): List<Job> = error("unused")
            override fun findAllByStatus(status: EvolutionStatus): List<Job> = error("unused")
            override fun findByContainerId(containerId: String): Job? = error("unused")
            override fun findById(id: Int): Job? = error("unused")
            override fun findAll(): List<Job> = error("unused")
            override fun save(entity: Job) = error("unused")
            override fun deleteById(id: Int): Boolean = error("unused")
            override fun clear() = error("unused")
        }
    override val repoMetrics: RepositoryMetrics =
        object : RepositoryMetrics {
            override fun createMetric(jobId: Int, iteration: Int, fitnessScore: Double, executionTime: Double?): Int = error("unused")
            override fun findAllByJobId(jobId: Int): List<Metric> = error("unused")
            override fun findByJobIdAndIteration(jobId: Int, iteration: Int): Metric? = error("unused")
            override fun findById(id: Int): Metric? = error("unused")
            override fun findAll(): List<Metric> = error("unused")
            override fun save(entity: Metric) = error("unused")
            override fun deleteById(id: Int): Boolean = error("unused")
            override fun clear() = error("unused")
        }
    override val repoCheckpoints: RepositoryCheckpoints =
        object : RepositoryCheckpoints {
            override fun createCheckpoint(jobId: Int, metricsId: Int, iteration: Int, solution: String): Int = error("unused")
            override fun findAllByJobId(jobId: Int): List<Checkpoint> = error("unused")
            override fun findAllByMetricsId(metricsId: Int): List<Checkpoint> = error("unused")
            override fun findByJobIdAndIteration(jobId: Int, iteration: Int): Checkpoint? = error("unused")
            override fun findById(id: Int): Checkpoint? = error("unused")
            override fun findAll(): List<Checkpoint> = error("unused")
            override fun save(entity: Checkpoint) = error("unused")
            override fun deleteById(id: Int): Boolean = error("unused")
            override fun clear() = error("unused")
        }
    override val repoTokens: RepositoryToken =
        object : RepositoryToken {
            override fun createToken(token: Token, maxTokens: Int) = error("unused")
            override fun findByTokenValidation(tokenValidation: TokenValidationInfo): Token? = error("unused")
            override fun findAllByUserId(userId: Int): List<Token> = error("unused")
            override fun getTokenByTokenValidationInfo(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>? = error("unused")
            override fun updateTokenLastUsed(tokenValidationInfo: TokenValidationInfo, now: Long) = error("unused")
            override fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int = error("unused")
        }

    override fun rollback() {}
}


