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
import com.example.evolab.service.jobExecution.JobQueue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class ProjectServiceImpTest {
    @Test
    fun createProjectRejectsBlankName() {
        val service = createService()

        val result = service.createProject(1, " ", null, null, null, null)

        assertLeftEquals(result, ProjectServiceErrors.InvalidProjectInput("Project name cannot be blank"))
    }

    @Test
    fun createProjectRejectsDuplicateNameForSameUser() {
        val repo = FakeRepositoryProject()
        repo.seed(userId = 1, name = "Projeto Demo")
        val service = createService(projectRepo = repo)

        val result = service.createProject(1, "Projeto Demo", null, null, null, null)

        assertLeftEquals(
            result,
            ProjectServiceErrors.DuplicateProjectName("User with id '1' already has a project with name 'Projeto Demo'"),
        )
    }

    @Test
    fun createProjectCreatesProjectWithNullConfig() {
        val repo = FakeRepositoryProject()
        val service = createService(projectRepo = repo)

        val result =
            service.createProject(
                userId = 7,
                name = "Projeto Demo",
                description = "descricao",
                configId = null,
                initialProgram = "def solve(x): return x",
                evaluatorCode = "def evaluate(candidate): return 1.0",
            )

        val project = assertRight(result)

        assertEquals(7, project.userId)
        assertNull(project.configId)
        assertEquals(EvolutionStatus.CREATED, project.status)
        assertEquals(project, repo.findById(project.id))
    }

    @Test
    fun updateProjectDetailsReturnsNotFoundWhenProjectDoesNotExist() {
        val service = createService()

        val result = service.updateProjectDetails(999, 1, "new", null, null, null, null)

        assertLeftEquals(result, ProjectServiceErrors.ProjectNotFound("Project with id '999' was not found"))
    }

    @Test
    fun updateProjectDetailsRejectsNonOwner() {
        val repo = FakeRepositoryProject()
        val project = repo.seed(userId = 1)
        val service = createService(projectRepo = repo)

        val result = service.updateProjectDetails(project.id, 2, "new", null, null, null, null)

        assertLeftEquals(
            result,
            ProjectServiceErrors.NotProjectOwner("User with id '2' is not the owner of project with id '${project.id}'"),
        )
    }

    @Test
    fun updateProjectDetailsRejectsUnknownConfig() {
        val repo = FakeRepositoryProject()
        val project = repo.seed(userId = 1)
        val service = createService(projectRepo = repo)

        val result = service.updateProjectDetails(project.id, 1, null, "descricao", 999, null, null)

        assertLeftEquals(result, ProjectServiceErrors.ConfigNotFound("Config with id '999' was not found"))
    }

    @Test
    fun updateProjectDetailsRejectsConfigFromAnotherUser() {
        val projectRepo = FakeRepositoryProject()
        val configRepo = FakeRepositoryConfig()
        val project = projectRepo.seed(userId = 1)
        val config = configRepo.seed(userId = 2)
        val service = createService(projectRepo = projectRepo, configRepo = configRepo)

        val result = service.updateProjectDetails(project.id, 1, null, null, config.id, null, null)

        assertLeftEquals(
            result,
            ProjectServiceErrors.ConfigAccessDenied("User with id '1' cannot use config with id '${config.id}'"),
        )
    }

    @Test
    fun updateProjectDetailsPersistsNewValues() {
        val projectRepo = FakeRepositoryProject()
        val configRepo = FakeRepositoryConfig()
        val project = projectRepo.seed(userId = 1)
        val config = configRepo.seed(userId = 1)
        val service = createService(projectRepo = projectRepo, configRepo = configRepo)

        val result =
            service.updateProjectDetails(
                projectId = project.id,
                userId = 1,
                name = "Projeto Atualizado",
                description = "descricao nova",
                configId = config.id,
                initialProgram = "def solve(x): return x * 2",
                evaluatorCode = "def evaluate(candidate): return 2.0",
            )

        val updated = assertRight(result)

        assertEquals("Projeto Atualizado", updated.name)
        assertEquals("descricao nova", updated.description)
        assertEquals(config.id, updated.configId)
        assertEquals("def solve(x): return x * 2", updated.initialProgram)
        assertEquals("def evaluate(candidate): return 2.0", updated.evaluatorCode)
        assertEquals(updated, projectRepo.findById(project.id))
    }

    @Test
    fun updateProjectStatusRejectsTransitionWhenProjectIsNotReady() {
        val repo = FakeRepositoryProject()
        val project = repo.seed(userId = 1, configId = null, initialProgram = "def solve(x): return x", evaluatorCode = null)
        val service = createService(projectRepo = repo)

        val result = service.updateProjectStatus(project.id, EvolutionStatus.QUEUED)

        assertLeftEquals(
            result,
            ProjectServiceErrors.InvalidProjectStatus(
                "Project with id '${project.id}' cannot move to 'QUEUED' without configId, initialProgram and evaluatorCode",
            ),
        )
    }

    @Test
    fun updateProjectStatusUpdatesProjectWhenReady() {
        val repo = FakeRepositoryProject()
        val project =
            repo.seed(
                userId = 1,
                configId = 10,
                initialProgram = "def solve(x): return x",
                evaluatorCode = "def evaluate(candidate): return 1.0",
            )
        val service = createService(projectRepo = repo)

        val result = service.updateProjectStatus(project.id, EvolutionStatus.QUEUED)

        val updated = assertRight(result)
        assertEquals(EvolutionStatus.QUEUED, updated.status)
        assertEquals(EvolutionStatus.QUEUED, repo.findById(project.id)?.status)
    }

    @Test
    fun startExperimentationRejectsNonOwner() {
        val projectRepo = FakeRepositoryProject()
        val project = projectRepo.seed(userId = 1, configId = 10)
        val service = createService(projectRepo = projectRepo)

        val result = service.startExperimentation(project.id, 2)

        assertLeftEquals(
            result,
            ProjectServiceErrors.NotProjectOwner("User with id '2' is not the owner of project with id '${project.id}'"),
        )
    }

    @Test
    fun startExperimentationRejectsProjectThatIsNotReady() {
        val projectRepo = FakeRepositoryProject()
        val project = projectRepo.seed(userId = 1, configId = null, evaluatorCode = null)
        val service = createService(projectRepo = projectRepo)

        val result = service.startExperimentation(project.id, 1)

        assertLeftEquals(
            result,
            ProjectServiceErrors.InvalidProjectStatus(
                "Project with id '${project.id}' cannot move to 'QUEUED' without configId, initialProgram and evaluatorCode",
            ),
        )
    }

    @Test
    fun startExperimentationRejectsUnknownConfig() {
        val projectRepo = FakeRepositoryProject()
        val project = projectRepo.seed(userId = 1, configId = 999)
        val service = createService(projectRepo = projectRepo)

        val result = service.startExperimentation(project.id, 1)

        assertLeftEquals(result, ProjectServiceErrors.ConfigNotFound("Config with id '999' was not found"))
    }

    @Test
    fun startExperimentationRejectsConfigFromAnotherUser() {
        val projectRepo = FakeRepositoryProject()
        val configRepo = FakeRepositoryConfig()
        val foreignConfig = configRepo.seed(userId = 2)
        val project = projectRepo.seed(userId = 1, configId = foreignConfig.id)
        val service = createService(projectRepo = projectRepo, configRepo = configRepo)

        val result = service.startExperimentation(project.id, 1)

        assertLeftEquals(
            result,
            ProjectServiceErrors.ConfigAccessDenied("User with id '1' cannot use config with id '${foreignConfig.id}'"),
        )
    }

    @Test
    fun startExperimentationRejectsProjectWithNonCreatedStatus() {
        val projectRepo = FakeRepositoryProject()
        val configRepo = FakeRepositoryConfig()
        val config = configRepo.seed(userId = 1)
        val project = projectRepo.seed(userId = 1, configId = config.id, status = EvolutionStatus.RUNNING)
        val service = createService(projectRepo = projectRepo, configRepo = configRepo)

        val result = service.startExperimentation(project.id, 1)

        assertLeftEquals(
            result,
            ProjectServiceErrors.InvalidProjectStatus(
                "Project with id '${project.id}' is in status 'RUNNING' and cannot be queued for experimentation",
            ),
        )
    }

    @Test
    fun startExperimentationQueuesProjectAndPersistsQueuedStatus() =
        runBlocking {
            val projectRepo = FakeRepositoryProject()
            val configRepo = FakeRepositoryConfig()
            val jobQueue = JobQueue()
            val config = configRepo.seed(userId = 1)
            val project = projectRepo.seed(userId = 1, configId = config.id)
            val service = createService(projectRepo = projectRepo, configRepo = configRepo, jobQueue = jobQueue)

            val result = service.startExperimentation(project.id, 1)

            val queuedProject = assertRight(result)
            assertEquals(EvolutionStatus.QUEUED, queuedProject.status)
            assertEquals(EvolutionStatus.QUEUED, projectRepo.findById(project.id)?.status)
            assertEquals(queuedProject, jobQueue.dequeue())
        }

    @Test
    fun startExperimentationReturnsQueueUnavailableWhenQueueIsFull() {
        val projectRepo = FakeRepositoryProject()
        val configRepo = FakeRepositoryConfig()
        val jobQueue = JobQueue()
        repeat(100) { index ->
            jobQueue.enqueue(
                projectRepo.seed(
                    userId = index + 100,
                    name = "Projeto $index",
                    configId = index + 1,
                ),
            )
        }
        val config = configRepo.seed(userId = 1)
        val project = projectRepo.seed(userId = 1, configId = config.id, name = "Projeto Principal")
        val service = createService(projectRepo = projectRepo, configRepo = configRepo, jobQueue = jobQueue)

        val result = service.startExperimentation(project.id, 1)

        assertLeftEquals(
            result,
            ProjectServiceErrors.ExecutionQueueUnavailable(
                "A fila esta cheia. O sistema esta sobrecarregado. Project ID: ${project.id}",
            ),
        )
        assertEquals(EvolutionStatus.CREATED, projectRepo.findById(project.id)?.status)
    }

    @Test
    fun deleteProjectRemovesOwnedProject() {
        val repo = FakeRepositoryProject()
        val project = repo.seed(userId = 1)
        val service = createService(projectRepo = repo)

        val result = service.deleteProject(project.id, 1)

        val deletedId = assertRight(result)
        assertEquals(project.id, deletedId)
        assertNull(repo.findById(project.id))
    }

    private fun createService(
        projectRepo: FakeRepositoryProject = FakeRepositoryProject(),
        configRepo: FakeRepositoryConfig = FakeRepositoryConfig(),
        jobQueue: JobQueue = JobQueue(),
    ) = ProjectServiceImp(FakeTransactionManager(projectRepo, configRepo), jobQueue)

    private fun <L, R> assertRight(result: Either<L, R>): R {
        assertTrue(result is Either.Right)
        return (result as Either.Right).value
    }

    private fun <L, R> assertLeftEquals(result: Either<L, R>, expected: L) {
        assertTrue(result is Either.Left)
        assertEquals(expected, (result as Either.Left).value)
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

private class FakeRepositoryConfig : RepositoryConfig {
    private val configs = linkedMapOf<Int, Config>()
    private var nextId = 1

    fun seed(
        userId: Int,
        llmCredentialsId: Int = 1,
        modelName: String = "gpt-4o-mini",
    ): Config {
        val id =
            createConfig(
                userId = userId,
                llmCredentialsId = llmCredentialsId,
                modelName = modelName,
                maxIter = 20,
                checkPointInterval = 5,
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

    override fun deleteById(id: Int): Boolean = configs.remove(id) != null

    override fun clear() {
        configs.clear()
    }
}

private class FakeTransactionManager(
    private val projectRepo: RepositoryProject,
    private val configRepo: RepositoryConfig,
) : TransactionManager {
    override fun <R> run(block: Transaction.() -> R): R = FakeTransaction(projectRepo, configRepo).block()
}

private class FakeTransaction(
    override val repoProjects: RepositoryProject,
    override val repoConfigs: RepositoryConfig,
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
            override fun createLocalModelCredential(userId: Int, apiKeyEncrypted: String, port: Int, modelName: String): LocalModelCredentials = error("unused")
            override fun findAllByUserId(userId: Int): List<LLMCredentials> = error("unused")
            override fun findAllByProvider(provider: LLM): List<LLMCredentials> = error("unused")
            override fun findLocalModelCredentialById(id: Int): LocalModelCredentials? = error("unused")
            override fun findById(id: Int): LLMCredentials? = error("unused")
            override fun findAll(): List<LLMCredentials> = error("unused")
            override fun save(entity: LLMCredentials) = error("unused")
            override fun deleteById(id: Int): Boolean = error("unused")
            override fun clear() = error("unused")
        }
    override val repoJobs: RepositoryJobs =
        object : RepositoryJobs {
            override fun createJob(projectId: Int, status: EvolutionStatus, containerId: String?, startedAt: Instant?, finishedAt: Instant?, bestSolution: String?, executionLogs: String?, failureReason: String?): Int = error("unused")
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
