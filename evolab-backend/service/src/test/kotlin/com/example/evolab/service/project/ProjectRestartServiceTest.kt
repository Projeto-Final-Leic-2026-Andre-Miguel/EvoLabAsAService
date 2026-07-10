package com.example.evolab.service.project

import com.example.evolab.domain.LLMCredentials.LLM
import com.example.evolab.domain.LLMCredentials.LLMCredentials
import com.example.evolab.domain.LLMCredentials.LocalModelCredentials
import com.example.evolab.domain.checkpoint.Checkpoint
import com.example.evolab.domain.config.Config
import com.example.evolab.domain.evolution.EvolutionStatus
import com.example.evolab.domain.job.Job
import com.example.evolab.domain.metrics.Metric
import com.example.evolab.domain.project.Project
import com.example.evolab.domain.statistics.UserStatistics
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
import com.example.evolab.repo.repoStatistics.RepositoryStatistics
import com.example.evolab.repo.repoToken.RepositoryToken
import com.example.evolab.repo.repoUser.RepositoryUser
import com.example.evolab.repo.transactions.Transaction
import com.example.evolab.repo.transactions.TransactionManager
import com.example.evolab.service.auxiliary.Either
import com.example.evolab.service.jobExecution.JobQueue
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProjectRestartServiceTest {
    @Test
    fun `restart completed project creates a fresh created job`() {
        val projectRepo = InMemoryProjectRepository()
        val jobRepo = InMemoryJobRepository()
        val project = projectRepo.seed(userId = 7, status = EvolutionStatus.COMPLETED)
        val service = ProjectServiceImp(InMemoryTransactionManager(projectRepo, jobRepo), JobQueue())

        val result = service.restartProject(project.id, userId = 7)

        val restarted = assertRight(result)
        assertEquals(EvolutionStatus.CREATED, restarted.status)
        assertEquals(EvolutionStatus.CREATED, projectRepo.findById(project.id)?.status)

        val jobs = jobRepo.findAllByProjectId(project.id)
        assertEquals(1, jobs.size)
        assertEquals(project.id, jobs.single().projectId)
        assertEquals(EvolutionStatus.CREATED, jobs.single().status)
        assertNull(jobs.single().bestSolution)
        assertNull(jobs.single().executionLogs)
        assertNull(jobs.single().failureReason)
    }

    @Test
    fun `restart failed project creates a fresh created job`() {
        val projectRepo = InMemoryProjectRepository()
        val jobRepo = InMemoryJobRepository()
        val project = projectRepo.seed(userId = 7, status = EvolutionStatus.FAILED)
        val service = ProjectServiceImp(InMemoryTransactionManager(projectRepo, jobRepo), JobQueue())

        val result = service.restartProject(project.id, userId = 7)

        assertRight(result)
        assertEquals(EvolutionStatus.CREATED, projectRepo.findById(project.id)?.status)
        assertEquals(EvolutionStatus.CREATED, jobRepo.findAllByProjectId(project.id).single().status)
    }

    @Test
    fun `restart non terminal project does not create a job`() {
        val projectRepo = InMemoryProjectRepository()
        val jobRepo = InMemoryJobRepository()
        val project = projectRepo.seed(userId = 7, status = EvolutionStatus.RUNNING)
        val service = ProjectServiceImp(InMemoryTransactionManager(projectRepo, jobRepo), JobQueue())

        val result = service.restartProject(project.id, userId = 7)

        assertTrue(result is Either.Left)
        assertEquals(EvolutionStatus.RUNNING, projectRepo.findById(project.id)?.status)
        assertTrue(jobRepo.findAllByProjectId(project.id).isEmpty())
    }

    private fun <L, R> assertRight(result: Either<L, R>): R {
        assertTrue(result is Either.Right)
        return result.value
    }
}

private class InMemoryTransactionManager(
    private val projectRepo: RepositoryProject,
    private val jobRepo: RepositoryJobs,
) : TransactionManager {
    override fun <R> run(block: Transaction.() -> R): R =
        InMemoryTransaction(projectRepo, jobRepo).block()
}

private class InMemoryTransaction(
    override val repoProjects: RepositoryProject,
    override val repoJobs: RepositoryJobs,
) : Transaction {
    override val repoUsers: RepositoryUser = unusedRepositoryUser()
    override val repoLLmCredentials: RepositoryLLMCredentials = unusedRepositoryLLMCredentials()
    override val repoConfigs: RepositoryConfig = unusedRepositoryConfig()
    override val repoMetrics: RepositoryMetrics = unusedRepositoryMetrics()
    override val repoCheckpoints: RepositoryCheckpoints = unusedRepositoryCheckpoints()
    override val repoTokens: RepositoryToken = unusedRepositoryToken()
    override val repoStatistics: RepositoryStatistics = unusedRepositoryStatistics()

    override fun rollback() = Unit
}

private class InMemoryProjectRepository : RepositoryProject {
    private val projects = linkedMapOf<Int, Project>()
    private var nextId = 1

    fun seed(
        userId: Int,
        status: EvolutionStatus,
        name: String = "Project",
    ): Project =
        Project(
            id = nextId++,
            userId = userId,
            configId = 1,
            name = name,
            description = "Description",
            initialProgram = "def solve(x): return x",
            evaluatorCode = "def evaluate(candidate): return {'combined_score': 1.0}",
            status = status,
            createdAt = Instant.now(),
        ).also { projects[it.id] = it }

    override fun createProject(
        userId: Int,
        name: String,
        description: String?,
        configId: Int?,
        initialProgram: String?,
        evaluatorCode: String?,
        status: EvolutionStatus,
    ): Project =
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
        ).also { projects[it.id] = it }

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
    override fun clear() = projects.clear()
}

private class InMemoryJobRepository : RepositoryJobs {
    private val jobs = linkedMapOf<Int, Job>()
    private var nextId = 1

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
                failureReason = failureReason,
                createdAt = Instant.now(),
            )
        return id
    }

    override fun findAllByProjectId(projectId: Int): List<Job> = jobs.values.filter { it.projectId == projectId }
    override fun findAllByStatus(status: EvolutionStatus): List<Job> = jobs.values.filter { it.status == status }
    override fun findByContainerId(containerId: String): Job? = jobs.values.find { it.containerId == containerId }
    override fun findById(id: Int): Job? = jobs[id]
    override fun findAll(): List<Job> = jobs.values.toList()
    override fun save(entity: Job) {
        jobs[entity.id] = entity
    }
    override fun deleteById(id: Int): Boolean = jobs.remove(id) != null
    override fun clear() = jobs.clear()
}

private fun unusedRepositoryUser() =
    object : RepositoryUser {
        override fun createLocalUser(name: String, email: String, passwordHash: String): User = unused()
        override fun createOAuthUser(name: String, email: String, provider: AuthProvider, providerId: String): User = unused()
        override fun findByEmail(email: String): User? = unused()
        override fun findByProvider(provider: AuthProvider, providerId: String): User? = unused()
        override fun findByTokenValidation(tokenValidationInfo: TokenValidationInfo): User? = unused()
        override fun count(): Long = unused()
        override fun findById(id: Int): User? = unused()
        override fun findAll(): List<User> = unused()
        override fun save(entity: User) = unused()
        override fun deleteById(id: Int): Boolean = unused()
        override fun clear() = unused()
    }

private fun unusedRepositoryLLMCredentials() =
    object : RepositoryLLMCredentials {
        override fun createLLMCredential(userId: Int, provider: LLM, apiKeyEncrypted: String): LLMCredentials = unused()
        override fun createLocalModelCredential(userId: Int, apiKeyEncrypted: String, port: Int, modelName: String): LocalModelCredentials = unused()
        override fun findAllByUserId(userId: Int): List<LLMCredentials> = unused()
        override fun findAllByProvider(provider: LLM): List<LLMCredentials> = unused()
        override fun findLocalModelCredentialById(id: Int): LocalModelCredentials? = unused()
        override fun findById(id: Int): LLMCredentials? = unused()
        override fun findAll(): List<LLMCredentials> = unused()
        override fun save(entity: LLMCredentials) = unused()
        override fun deleteById(id: Int): Boolean = unused()
        override fun clear() = unused()
    }

private fun unusedRepositoryConfig() =
    object : RepositoryConfig {
        override fun createConfig(userId: Int, llmCredentialsId: Int, modelName: String, maxIter: Int, checkPointInterval: Int, additionalParams: Map<String, String>): Int = unused()
        override fun findAllByUserId(userId: Int): List<Config> = unused()
        override fun findAllByLlmCredentialId(llmCredentialsId: Int): List<Config> = unused()
        override fun findAllByModelName(modelName: String): List<Config> = unused()
        override fun findById(id: Int): Config? = unused()
        override fun findAll(): List<Config> = unused()
        override fun save(entity: Config) = unused()
        override fun deleteById(id: Int): Boolean = unused()
        override fun clear() = unused()
    }

private fun unusedRepositoryMetrics() =
    object : RepositoryMetrics {
        override fun createMetric(jobId: Int, iteration: Int, fitnessScore: Double, executionTime: Double?): Int = unused()
        override fun findAllByJobId(jobId: Int): List<Metric> = unused()
        override fun findByJobIdAndIteration(jobId: Int, iteration: Int): Metric? = unused()
        override fun findById(id: Int): Metric? = unused()
        override fun findAll(): List<Metric> = unused()
        override fun save(entity: Metric) = unused()
        override fun deleteById(id: Int): Boolean = unused()
        override fun clear() = unused()
    }

private fun unusedRepositoryCheckpoints() =
    object : RepositoryCheckpoints {
        override fun createCheckpoint(jobId: Int, metricsId: Int, iteration: Int, solution: String): Int = unused()
        override fun findAllByJobId(jobId: Int): List<Checkpoint> = unused()
        override fun findAllByMetricsId(metricsId: Int): List<Checkpoint> = unused()
        override fun findByJobIdAndIteration(jobId: Int, iteration: Int): Checkpoint? = unused()
        override fun findById(id: Int): Checkpoint? = unused()
        override fun findAll(): List<Checkpoint> = unused()
        override fun save(entity: Checkpoint) = unused()
        override fun deleteById(id: Int): Boolean = unused()
        override fun clear() = unused()
    }

private fun unusedRepositoryToken() =
    object : RepositoryToken {
        override fun createToken(token: Token, maxTokens: Int) = unused()
        override fun findByTokenValidation(tokenValidation: TokenValidationInfo): Token? = unused()
        override fun findAllByUserId(userId: Int): List<Token> = unused()
        override fun getTokenByTokenValidationInfo(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>? = unused()
        override fun updateTokenLastUsed(tokenValidationInfo: TokenValidationInfo, now: Long) = unused()
        override fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int = unused()
    }

private fun unusedRepositoryStatistics() =
    object : RepositoryStatistics {
        override fun findByUserId(userId: Int): UserStatistics? = unused()
        override fun getOrCreate(userId: Int): UserStatistics = unused()
        override fun incrementProjectsCreated(userId: Int, projectId: Int, projectName: String, projectCreatedAt: Instant): UserStatistics = unused()
        override fun incrementProjectsExecuted(userId: Int): UserStatistics = unused()
        override fun incrementProjectOutcome(userId: Int, status: EvolutionStatus): UserStatistics = unused()
        override fun incrementCredentialsCreated(userId: Int): UserStatistics = unused()
        override fun incrementConfigsCreated(userId: Int): UserStatistics = unused()
        override fun findById(id: Int): UserStatistics? = unused()
        override fun findAll(): List<UserStatistics> = unused()
        override fun save(entity: UserStatistics) = unused()
        override fun deleteById(id: Int): Boolean = unused()
        override fun clear() = unused()
    }

private fun unused(): Nothing = error("unused in ProjectRestartServiceTest")
