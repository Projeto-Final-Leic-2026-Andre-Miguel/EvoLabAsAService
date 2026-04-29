package com.example.evolab.service.jobExecution

import com.example.evolab.domain.LLMCredentials.LLMCredentials
import com.example.evolab.domain.config.Config
import com.example.evolab.domain.evolution.EvolutionStatus
import com.example.evolab.domain.job.Job
import com.example.evolab.domain.project.Project
import com.example.evolab.repo.transactions.Transaction
import com.example.evolab.repo.transactions.TransactionManager
import com.example.evolab.service.auxiliary.Failure
import com.example.evolab.service.auxiliary.Success
import com.example.evolab.service.configService.ConfigService
import com.example.evolab.service.configService.OpenEvolvePayloadBuilder
import com.example.evolab.service.checkpoints.CheckpointService
import com.example.evolab.service.jobsService.JobService
import com.example.evolab.service.metrics.MetricService
import com.example.evolab.service.security.EncryptionService
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.inject.Named
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Instant


private const val WORKERS = 3


@Named
class WorkerPoolManager(
    private val jobQueue: JobQueue,
    private val dockerExecutionService: DockerExecutionService,
    private val trxManager: TransactionManager,
    private val configService: ConfigService,
    private val encryptionService: EncryptionService,
    private val jobService: JobService,
    private val metricService: MetricService,
    private val checkpointService: CheckpointService,
    private val poolSize: Int = WORKERS,
) {
    private val logger = LoggerFactory.getLogger(WorkerPoolManager::class.java)
    private val poolScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * O PostConstruct garante que esta corrotina arranca mal o backend(Service) fique operacional.
     */
    @PostConstruct
    fun startWorkers() {
        logger.info("A inicializar $poolSize Workers para a Fila de Execucao do OpenEvolve...")

        repeat(poolSize) { workerId ->
            poolScope.launch {
                workerLoop(workerId)
            }
        }
    }

    private suspend fun workerLoop(workerId: Int) {

        while (currentCoroutineContext().isActive) {
            var currentProjectId: Int? = null

            try {

                val projectFromQueue = jobQueue.dequeue() // waits until there´s an available project to process

                currentProjectId = projectFromQueue.id

                logger.info("Worker-$workerId pegou no Projeto: ${projectFromQueue.id}")

                val newJobId = createInitialJob(projectFromQueue)
                var yamlConfigPath: Path? = null

                try {
                    val (runtimePayload, containerEnvironment) = setupExecutionEnvironment(projectFromQueue, newJobId)

                    val configResult = configService.generateTemporaryConfigFile(runtimePayload)
                    if (configResult is Failure) {
                        throw IllegalStateException("Falha ao gerar o Config YAML: ${configResult.value}")
                    }
                    yamlConfigPath = (configResult as Success).value

                    val result = dockerExecutionService.runOpenEvolveContainerForProject(
                        project = projectFromQueue,
                        yamlConfigPath = yamlConfigPath,
                        workerId = workerId,
                        environment = containerEnvironment,
                        onContainerStarted = { containerId, startedAt ->
                            updateJobStartContainerInfo(newJobId, containerId, startedAt, workerId)
                        }
                    )

                    processExecutionResult(result, newJobId, projectFromQueue, workerId)

                } finally { // clean-up the config file generated for this execution, if it was created
                    yamlConfigPath?.let { configService.cleanupTemporaryConfigFile(it) }
                }
            } catch (e: CancellationException) {
                logger.info("Worker-$workerId foi cancelado. Desligando...")
                break
            } catch (e: Exception) {
                logger.error("Worker-$workerId falhou criticamente no processamento:", e)
                currentProjectId?.let { handleCriticalFailure(it) }
            }
        }
    }

    private fun createInitialJob(project: Project): Int {
        return when (val result = jobService.createJob(
            projectId = project.id,
            status = EvolutionStatus.RUNNING,
        )) {
            is Success -> result.value
            is Failure -> throw IllegalStateException("Failed to create job for project '${project.id}'")
        }
    }

    private fun setupExecutionEnvironment(project: Project, jobId: Int): Pair<Map<String, Any>, Map<String, String>> {
        return trxManager.run {

            val configId = project.configId
                ?: throw IllegalStateException("Project '${project.id}' has no config assigned")

            val config = findConfigById(configId)
                ?: throw IllegalStateException("Config with id '$configId' was not found")

            val credentials = findCredentialById(config.llmCredentialsId)
                ?: throw IllegalStateException("Credential with id '${config.llmCredentialsId}' was not found")

           saveProjectState(project.copy(status = EvolutionStatus.RUNNING))

            val decryptedKey = credentials.apiKeyEncrypted.takeIf { it.isNotBlank() }
                ?.let { encryptionService.decrypt(it) } ?: ""

            val apiKeyVariableName = OpenEvolvePayloadBuilder.apiKeyEnvironmentVariableName(credentials.llm)

            var runtimeConfig = configService.buildRuntimeConfig(config, project.id, jobId)
            
            val resolvedApiBase = OpenEvolvePayloadBuilder.resolveApiBase(
                credentials.llm,
                config.modelName,
                config.additionalParams["llm.api_base"]
            )
            
            runtimeConfig = runtimeConfig.copy(
                additionalParams = runtimeConfig.additionalParams + mapOf("llm.api_base" to resolvedApiBase)
            )

            val runtimePayload = OpenEvolvePayloadBuilder.build(
                config = runtimeConfig,
                apiKeyValue = OpenEvolvePayloadBuilder.apiKeyEnvironmentPlaceholder(apiKeyVariableName),
            )

            Pair(runtimePayload, mapOf(apiKeyVariableName to decryptedKey))
        }
    }

    private fun updateJobStartContainerInfo(jobId: Int, containerId: String, startedAt: Instant, workerId: Int) {
        when (val startedJobResult = jobService.getJobById(jobId)) {
            is Success -> {
                val startedJob = startedJobResult.value.copy(
                    containerId = containerId,
                    startedAt = startedAt
                )
                saveJob(startedJob)
            }
            is Failure -> logger.warn("Worker-$workerId: Job $jobId not found to update container info")
        }
    }

    private fun processExecutionResult(result: DockerExecutionResult, jobId: Int, project: Project, workerId: Int) {

        val failureReason = OpenEvolveExecutionOutcomeDecider.failureReason(result.exitCode, result.logs)

        val finalStatus = if (failureReason == null) EvolutionStatus.COMPLETED else EvolutionStatus.FAILED

        if (failureReason != null) {
            logger.warn("Worker-$workerId: Job $jobId marcado como FAILED. Motivo: $failureReason")
        }

        when (val jobResult = jobService.getJobById(jobId)) {
            is Success -> {
                val updatedJob = jobResult.value.copy(
                    status = finalStatus,
                    finishedAt = result.finishedAt,
                    bestSolution = result.bestSolution,
                    executionLogs = result.logs,
                )
                saveJob(updatedJob)
            }
            is Failure -> logger.warn("Worker-$workerId: Job $jobId not found for final update")
        }

        trxManager.run {
            saveProjectState(project.copy(status = finalStatus))
        }

        for (cp in result.parsedCheckpoints) {
            val metricResult = metricService.createMetric(
                userId = project.userId,
                jobId = jobId,
                iteration = cp.iteration,
                fitnessScore = cp.fitnessScore,
                executionTime = null,
            )
            when (metricResult) {
                is Failure -> logger.warn("Worker-$workerId: Falha ao guardar métrica da iteração ${cp.iteration}: ${metricResult.value}")
                is Success -> {
                    val checkpointResult = checkpointService.createCheckpoint(
                        userId = project.userId,
                        jobId = jobId,
                        metricsId = metricResult.value.id,
                        iteration = cp.iteration,
                        solution = cp.solution,
                    )
                    if (checkpointResult is Failure) {
                        logger.warn("Worker-$workerId: Falha ao guardar checkpoint da iteração ${cp.iteration}: ${checkpointResult.value}")
                    }
                }
            }
        }

        logger.info("Worker-$workerId terminou o Job $jobId do Projeto ${project.id} -> $finalStatus")
    }

    private fun handleCriticalFailure(failedProjectId: Int) {
        try {
            trxManager.run {
                val projectFail = repoProjects.findById(failedProjectId)
                if (projectFail != null) {
                    saveProjectState(projectFail.copy(status = EvolutionStatus.FAILED))
                }
            }
        } catch (updateEx: Exception) {
            logger.error("Falha ao registar o estado de erro do projeto na BD:", updateEx)
        }
    }

    @PreDestroy
    fun shutdown() {
        poolScope.cancel()
        // TODO: If we want to clean this up well, we should join the remaining jobs here.
    }

    private fun Transaction.findProjectById(projectId: Int): Project? =
        repoProjects.findById(projectId)

    private fun Transaction.findConfigById(configId: Int): Config? =
        repoConfigs.findById(configId)

    private fun Transaction.findCredentialById(credentialId: Int): LLMCredentials? =
        repoLLmCredentials.findById(credentialId)


    private fun Transaction.saveProjectState(project : Project) = repoProjects.save(project)

    private fun saveJob(startedJob : Job) = jobService.saveJob(startedJob)

}
