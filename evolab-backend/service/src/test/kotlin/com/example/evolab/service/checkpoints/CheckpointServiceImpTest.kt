package com.example.evolab.service.checkpoints

import com.example.evolab.domain.LLMCredentials.LLM
import com.example.evolab.domain.LLMCredentials.LLMCredentials
import com.example.evolab.domain.checkpoint.Checkpoint
import com.example.evolab.domain.config.Config
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
import java.time.Instant

class CheckpointServiceImpTest {
    @Test
    fun createCheckpointCreatesCheckpointWhenInputIsValid() {
        val projectRepo = FakeRepositoryProject()
        val jobRepo = FakeRepositoryJobs()
        val metricRepo = FakeRepositoryMetrics()
        val checkpointRepo = FakeRepositoryCheckpoints()
        val project = projectRepo.seed(userId = 1)
        val job = jobRepo.seed(projectId = project.id)
        val metric = metricRepo.seed(jobId = job.id, iteration = 1)
        val service = createService(checkpointRepo, metricRepo, jobRepo, projectRepo)

        val result = service.createCheckpoint(1, job.id, metric.id, 1, "checkpoint_1.py")

        val checkpoint = assertRight(result)
        assertEquals(job.id, checkpoint.jobId)
        assertEquals(metric.id, checkpoint.metricsId)
        assertEquals(1, checkpoint.iteration)
        assertEquals("checkpoint_1.py", checkpoint.solution)
        assertEquals(checkpoint, checkpointRepo.findById(checkpoint.id))
    }

    @Test
    fun createCheckpointReturnsJobNotFoundWhenJobDoesNotExist() {
        val service = createService()

        val result = service.createCheckpoint(1, 999, 1, 1, "checkpoint.py")

        assertLeftEquals(result, CheckpointServiceErrors.JobNotFound("Job with id '999' was not found"))
    }

    @Test
    fun createCheckpointReturnsMetricNotFoundWhenMetricDoesNotExist() {
        val projectRepo = FakeRepositoryProject()
        val jobRepo = FakeRepositoryJobs()
        val project = projectRepo.seed(userId = 1)
        val job = jobRepo.seed(projectId = project.id)
        val service = createService(jobRepo = jobRepo, projectRepo = projectRepo)

        val result = service.createCheckpoint(1, job.id, 999, 1, "checkpoint.py")

        assertLeftEquals(result, CheckpointServiceErrors.MetricNotFound("Metric with id '999' was not found"))
    }

    @Test
    fun createCheckpointRejectsAccessToAnotherUsersJob() {
        val projectRepo = FakeRepositoryProject()
        val jobRepo = FakeRepositoryJobs()
        val metricRepo = FakeRepositoryMetrics()
        val project = projectRepo.seed(userId = 1)
        val job = jobRepo.seed(projectId = project.id)
        val metric = metricRepo.seed(jobId = job.id, iteration = 1)
        val service = createService(metricRepo = metricRepo, jobRepo = jobRepo, projectRepo = projectRepo)

        val result = service.createCheckpoint(2, job.id, metric.id, 1, "checkpoint.py")

        assertLeftEquals(
            result,
            CheckpointServiceErrors.CheckpointAccessDenied(
                "User with id '2' cannot access checkpoints for job with id '${job.id}'",
            ),
        )
    }

    @Test
    fun createCheckpointRejectsMetricFromAnotherJob() {
        val projectRepo = FakeRepositoryProject()
        val jobRepo = FakeRepositoryJobs()
        val metricRepo = FakeRepositoryMetrics()
        val project = projectRepo.seed(userId = 1)
        val job1 = jobRepo.seed(projectId = project.id)
        val job2 = jobRepo.seed(projectId = project.id)
        val metric = metricRepo.seed(jobId = job2.id, iteration = 1)
        val service = createService(metricRepo = metricRepo, jobRepo = jobRepo, projectRepo = projectRepo)

        val result = service.createCheckpoint(1, job1.id, metric.id, 1, "checkpoint.py")

        assertLeftEquals(
            result,
            CheckpointServiceErrors.MetricDoesNotBelongToJob(
                "Metric with id '${metric.id}' does not belong to job with id '${job1.id}'",
            ),
        )
    }

    @Test
    fun createCheckpointRejectsInvalidIteration() {
        val projectRepo = FakeRepositoryProject()
        val jobRepo = FakeRepositoryJobs()
        val metricRepo = FakeRepositoryMetrics()
        val project = projectRepo.seed(userId = 1)
        val job = jobRepo.seed(projectId = project.id)
        val metric = metricRepo.seed(jobId = job.id, iteration = 1)
        val service = createService(metricRepo = metricRepo, jobRepo = jobRepo, projectRepo = projectRepo)

        val result = service.createCheckpoint(1, job.id, metric.id, 0, "checkpoint.py")

        assertLeftEquals(
            result,
            CheckpointServiceErrors.InvalidCheckpointInput("Checkpoint iteration must be greater than 0"),
        )
    }

    @Test
    fun createCheckpointRejectsBlankSolution() {
        val projectRepo = FakeRepositoryProject()
        val jobRepo = FakeRepositoryJobs()
        val metricRepo = FakeRepositoryMetrics()
        val project = projectRepo.seed(userId = 1)
        val job = jobRepo.seed(projectId = project.id)
        val metric = metricRepo.seed(jobId = job.id, iteration = 1)
        val service = createService(metricRepo = metricRepo, jobRepo = jobRepo, projectRepo = projectRepo)

        val result = service.createCheckpoint(1, job.id, metric.id, 1, " ")

        assertLeftEquals(
            result,
            CheckpointServiceErrors.InvalidCheckpointInput("Checkpoint solution cannot be blank"),
        )
    }

    @Test
    fun createCheckpointRejectsDuplicateCheckpointForIteration() {
        val projectRepo = FakeRepositoryProject()
        val jobRepo = FakeRepositoryJobs()
        val metricRepo = FakeRepositoryMetrics()
        val checkpointRepo = FakeRepositoryCheckpoints()
        val project = projectRepo.seed(userId = 1)
        val job = jobRepo.seed(projectId = project.id)
        val metric = metricRepo.seed(jobId = job.id, iteration = 2)
        checkpointRepo.seed(jobId = job.id, metricsId = metric.id, iteration = 2)
        val service = createService(checkpointRepo, metricRepo, jobRepo, projectRepo)

        val result = service.createCheckpoint(1, job.id, metric.id, 2, "checkpoint_2.py")

        assertLeftEquals(
            result,
            CheckpointServiceErrors.DuplicateCheckpointForIteration(
                "Job with id '${job.id}' already has a checkpoint for iteration '2'",
            ),
        )
    }

    @Test
    fun getCheckpointsByJobIdReturnsCheckpointsForOwnedJob() {
        val projectRepo = FakeRepositoryProject()
        val jobRepo = FakeRepositoryJobs()
        val metricRepo = FakeRepositoryMetrics()
        val checkpointRepo = FakeRepositoryCheckpoints()
        val project = projectRepo.seed(userId = 1)
        val job = jobRepo.seed(projectId = project.id)
        val metric = metricRepo.seed(jobId = job.id, iteration = 1)
        val first = checkpointRepo.seed(jobId = job.id, metricsId = metric.id, iteration = 1)
        val second = checkpointRepo.seed(jobId = job.id, metricsId = metric.id, iteration = 2)
        val service = createService(checkpointRepo, metricRepo, jobRepo, projectRepo)

        val result = service.getCheckpointsByJobId(job.id, 1)

        assertEquals(listOf(first, second), assertRight(result))
    }

    @Test
    fun getCheckpointsByMetricsIdReturnsCheckpointsForOwnedMetric() {
        val projectRepo = FakeRepositoryProject()
        val jobRepo = FakeRepositoryJobs()
        val metricRepo = FakeRepositoryMetrics()
        val checkpointRepo = FakeRepositoryCheckpoints()
        val project = projectRepo.seed(userId = 1)
        val job = jobRepo.seed(projectId = project.id)
        val metric = metricRepo.seed(jobId = job.id, iteration = 1)
        val first = checkpointRepo.seed(jobId = job.id, metricsId = metric.id, iteration = 1)
        val second = checkpointRepo.seed(jobId = job.id, metricsId = metric.id, iteration = 2)
        val service = createService(checkpointRepo, metricRepo, jobRepo, projectRepo)

        val result = service.getCheckpointsByMetricsId(metric.id, 1)

        assertEquals(listOf(first, second), assertRight(result))
    }

    @Test
    fun getCheckpointReturnsOwnedCheckpointById() {
        val projectRepo = FakeRepositoryProject()
        val jobRepo = FakeRepositoryJobs()
        val metricRepo = FakeRepositoryMetrics()
        val checkpointRepo = FakeRepositoryCheckpoints()
        val project = projectRepo.seed(userId = 1)
        val job = jobRepo.seed(projectId = project.id)
        val metric = metricRepo.seed(jobId = job.id, iteration = 1)
        val checkpoint = checkpointRepo.seed(jobId = job.id, metricsId = metric.id, iteration = 3)
        val service = createService(checkpointRepo, metricRepo, jobRepo, projectRepo)

        val result = service.getCheckpoint(checkpoint.id, 1)

        assertEquals(checkpoint, assertRight(result))
    }

    @Test
    fun getCheckpointByJobAndIterationReturnsOwnedCheckpoint() {
        val projectRepo = FakeRepositoryProject()
        val jobRepo = FakeRepositoryJobs()
        val metricRepo = FakeRepositoryMetrics()
        val checkpointRepo = FakeRepositoryCheckpoints()
        val project = projectRepo.seed(userId = 1)
        val job = jobRepo.seed(projectId = project.id)
        val metric = metricRepo.seed(jobId = job.id, iteration = 1)
        val checkpoint = checkpointRepo.seed(jobId = job.id, metricsId = metric.id, iteration = 4)
        val service = createService(checkpointRepo, metricRepo, jobRepo, projectRepo)

        val result = service.getCheckpointByJobAndIteration(job.id, 4, 1)

        assertEquals(checkpoint, assertRight(result))
    }

    @Test
    fun getCheckpointReturnsNotFoundWhenCheckpointDoesNotExist() {
        val service = createService()

        val result = service.getCheckpoint(123, 1)

        assertLeftEquals(
            result,
            CheckpointServiceErrors.CheckpointNotFound("Checkpoint with id '123' was not found"),
        )
    }

    private fun createService(
        checkpointRepo: FakeRepositoryCheckpoints = FakeRepositoryCheckpoints(),
        metricRepo: FakeRepositoryMetrics = FakeRepositoryMetrics(),
        jobRepo: FakeRepositoryJobs = FakeRepositoryJobs(),
        projectRepo: FakeRepositoryProject = FakeRepositoryProject(),
    ) = CheckpointServiceImp(FakeTransactionManager(checkpointRepo, metricRepo, jobRepo, projectRepo))

    private fun <L, R> assertRight(result: Either<L, R>): R {
        assertTrue(result is Either.Right)
        return (result as Either.Right).value
    }

    private fun <L, R> assertLeftEquals(result: Either<L, R>, expected: L) {
        assertTrue(result is Either.Left)
        assertEquals(expected, (result as Either.Left).value)
    }
}

private class FakeRepositoryCheckpoints : RepositoryCheckpoints {
    private val checkpoints = linkedMapOf<Int, Checkpoint>()
    private var nextId = 1

    fun seed(
        jobId: Int,
        metricsId: Int,
        iteration: Int,
        solution: String = "checkpoint.py",
    ): Checkpoint {
        val id = createCheckpoint(jobId, metricsId, iteration, solution)
        return checkpoints[id]!!
    }

    override fun createCheckpoint(
        jobId: Int,
        metricsId: Int,
        iteration: Int,
        solution: String,
    ): Int {
        val id = nextId++
        checkpoints[id] =
            Checkpoint(
                id = id,
                jobId = jobId,
                metricsId = metricsId,
                iteration = iteration,
                solution = solution,
                createdAt = Instant.now(),
            )
        return id
    }

    override fun findAllByJobId(jobId: Int): List<Checkpoint> = checkpoints.values.filter { it.jobId == jobId }

    override fun findAllByMetricsId(metricsId: Int): List<Checkpoint> = checkpoints.values.filter { it.metricsId == metricsId }

    override fun findByJobIdAndIteration(jobId: Int, iteration: Int): Checkpoint? =
        checkpoints.values.firstOrNull { it.jobId == jobId && it.iteration == iteration }

    override fun findById(id: Int): Checkpoint? = checkpoints[id]

    override fun findAll(): List<Checkpoint> = checkpoints.values.toList()

    override fun save(entity: Checkpoint) {
        checkpoints[entity.id] = entity
    }

    override fun deleteById(id: Int): Boolean = checkpoints.remove(id) != null

    override fun clear() {
        checkpoints.clear()
    }
}

private class FakeRepositoryMetrics : RepositoryMetrics {
    private val metrics = linkedMapOf<Int, Metric>()
    private var nextId = 1

    fun seed(
        jobId: Int,
        iteration: Int,
        fitnessScore: Double = 1.0,
        executionTime: Double? = 0.5,
    ): Metric {
        val id = createMetric(jobId, iteration, fitnessScore, executionTime)
        return metrics[id]!!
    }

    override fun createMetric(
        jobId: Int,
        iteration: Int,
        fitnessScore: Double,
        executionTime: Double?,
    ): Int {
        val id = nextId++
        metrics[id] =
            Metric(
                id = id,
                jobId = jobId,
                iteration = iteration,
                fitnessScore = fitnessScore,
                executionTime = executionTime,
                createdAt = Instant.now(),
            )
        return id
    }

    override fun findAllByJobId(jobId: Int): List<Metric> = metrics.values.filter { it.jobId == jobId }

    override fun findByJobIdAndIteration(jobId: Int, iteration: Int): Metric? =
        metrics.values.firstOrNull { it.jobId == jobId && it.iteration == iteration }

    override fun findById(id: Int): Metric? = metrics[id]

    override fun findAll(): List<Metric> = metrics.values.toList()

    override fun save(entity: Metric) {
        metrics[entity.id] = entity
    }

    override fun deleteById(id: Int): Boolean = metrics.remove(id) != null

    override fun clear() {
        metrics.clear()
    }
}

private class FakeRepositoryJobs : RepositoryJobs {
    private val jobs = linkedMapOf<Int, Job>()
    private var nextId = 1

    fun seed(
        projectId: Int,
        status: EvolutionStatus = EvolutionStatus.CREATED,
    ): Job {
        val id = createJob(projectId = projectId, status = status)
        return jobs[id]!!
    }

    override fun createJob(
        projectId: Int,
        status: EvolutionStatus,
        containerId: String?,
        startedAt: Instant?,
        finishedAt: Instant?,
        bestSolution: String?,
        executionLogs: String?,
        failureReason: String?,
    ): Int {
        val id = nextId++
        jobs[id] =
            Job(
                id = id,
                projectId = projectId,
                status = status,
                containerId = containerId,
                startedAt = startedAt,
                finishedAt = finishedAt,
                bestSolution = bestSolution,
                executionLogs = executionLogs,
                createdAt = Instant.now(),
                failureReason = failureReason,
            )
        return id
    }

    override fun findAllByProjectId(projectId: Int): List<Job> = jobs.values.filter { it.projectId == projectId }

    override fun findAllByStatus(status: EvolutionStatus): List<Job> = jobs.values.filter { it.status == status }

    override fun findByContainerId(containerId: String): Job? = jobs.values.firstOrNull { it.containerId == containerId }

    override fun findById(id: Int): Job? = jobs[id]

    override fun findAll(): List<Job> = jobs.values.toList()

    override fun save(entity: Job) {
        jobs[entity.id] = entity
    }

    override fun deleteById(id: Int): Boolean = jobs.remove(id) != null

    override fun clear() {
        jobs.clear()
    }
}

private class FakeRepositoryProject : RepositoryProject {
    private val projects = linkedMapOf<Int, Project>()
    private var nextId = 1

    fun seed(
        userId: Int,
        name: String = "Projeto Demo",
        description: String? = "descricao",
        configId: Int? = null,
        initialProgram: String? = "def solve(x): return x",
        evaluatorCode: String? = "def evaluate(candidate): return 1.0",
        status: EvolutionStatus = EvolutionStatus.CREATED,
    ): Project {
        val project =
            Project(
                id = nextId++,
                userId = userId,
                configId = configId,
                name = name,
                description = description,
                initialProgram = initialProgram,
                evaluatorCode = evaluatorCode,
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
        configId: Int?,
        initialProgram: String?,
        evaluatorCode: String?,
        status: EvolutionStatus,
    ): Project =
        seed(
            userId = userId,
            name = name,
            description = description,
            configId = configId,
            initialProgram = initialProgram,
            evaluatorCode = evaluatorCode,
            status = status,
        )

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
    private val checkpointRepo: RepositoryCheckpoints,
    private val metricRepo: RepositoryMetrics,
    private val jobRepo: RepositoryJobs,
    private val projectRepo: RepositoryProject,
) : TransactionManager {
    override fun <R> run(block: Transaction.() -> R): R =
        FakeTransaction(checkpointRepo, metricRepo, jobRepo, projectRepo).block()
}

private class FakeTransaction(
    override val repoCheckpoints: RepositoryCheckpoints,
    override val repoMetrics: RepositoryMetrics,
    override val repoJobs: RepositoryJobs,
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
    override val repoConfigs: RepositoryConfig =
        object : RepositoryConfig {
            override fun createConfig(
                userId: Int,
                llmCredentialsId: Int,
                modelName: String,
                maxIter: Int,
                checkPointInterval: Int,
                additionalParams: Map<String, String>,
            ): Int = error("unused")

            override fun findAllByUserId(userId: Int): List<Config> = error("unused")
            override fun findAllByLlmCredentialId(llmCredentialsId: Int): List<Config> = error("unused")
            override fun findAllByModelName(modelName: String): List<Config> = error("unused")
            override fun findById(id: Int): Config? = error("unused")
            override fun findAll(): List<Config> = error("unused")
            override fun save(entity: Config) = error("unused")
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
