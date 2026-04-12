package com.example.evolab.service.jobExecution

import com.example.evolab.domain.LLMCredentials.LLMCredentials
import com.example.evolab.domain.config.Config
import com.example.evolab.domain.evolution.EvolutionStatus
import com.example.evolab.domain.project.Project
import com.example.evolab.repo.transactions.Transaction
import com.example.evolab.repo.transactions.TransactionManager
import com.example.evolab.service.auxiliary.Failure
import com.example.evolab.service.auxiliary.Success
import com.example.evolab.service.configService.ConfigService
import com.example.evolab.service.configService.OpenEvolvePayloadBuilder
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

@Named
class WorkerPoolManager(
    private val jobQueue: JobQueue,
    private val dockerExecutionService: DockerExecutionService,
    private val trxManager: TransactionManager,
    private val configService: ConfigService,
    private val encryptionService: EncryptionService,
    private val poolSize: Int = 3,
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
                // Project is pulled from the queue. If it is empty, execution is suspended.
                val projectFromQueue = jobQueue.dequeue()
                currentProjectId = projectFromQueue.id

                logger.info("Worker-$workerId pegou no Projeto: ${projectFromQueue.id}")

                var yamlConfigPath: Path? = null

                val setupData =
                    trxManager.run {
                        val configId =
                            projectFromQueue.configId
                                ?: throw IllegalStateException("Project '${projectFromQueue.id}' has no config assigned")
                        val config =
                            findConfigById(configId)
                                ?: throw IllegalStateException("Config with id '$configId' was not found")
                        val credentials =
                            findCredentialById(config.llmCredentialsId)
                                ?: throw IllegalStateException("Credential with id '${config.llmCredentialsId}' was not found")

                        repoProjects.save(projectFromQueue.copy(status = EvolutionStatus.RUNNING))

                        val newJobId =
                            repoJobs.createJob(
                                projectId = projectFromQueue.id,
                                status = EvolutionStatus.RUNNING,
                            )

                        val decryptedKey =
                            credentials.apiKeyEncrypted?.let { encryptionService.decrypt(it) }
                                ?: throw IllegalStateException("Encrypted API key is missing for credential id '${credentials.id}'")
                        val apiKeyVariableName = OpenEvolvePayloadBuilder.apiKeyEnvironmentVariableName(credentials.llm)
                        val runtimeConfig = configService.buildRuntimeConfig(config, projectFromQueue.id, newJobId)
                        val runtimePayload =
                            OpenEvolvePayloadBuilder.build(
                                config = runtimeConfig,
                                apiKeyValue = OpenEvolvePayloadBuilder.apiKeyEnvironmentPlaceholder(apiKeyVariableName),
                            )

                        Triple(newJobId, runtimePayload, mapOf(apiKeyVariableName to decryptedKey))
                    }

                val (jobId, runtimePayload, containerEnvironment) = setupData

                try {
                    when (val configResult = configService.generateTemporaryConfigFile(runtimePayload)) {
                        is Success -> yamlConfigPath = configResult.value
                        is Failure -> throw IllegalStateException("Falha ao gerar o Config YAML: ${configResult.value}")
                    }

                    val result =
                        dockerExecutionService.runOpenEvolveContainerForProject(
                            project = projectFromQueue,
                            yamlConfigPath = yamlConfigPath,
                            workerId = workerId,
                            environment = containerEnvironment,
                        )

                    val isSuccess = result.exitCode == 0
                    val finalStatus = if (isSuccess) EvolutionStatus.COMPLETED else EvolutionStatus.FAILED

                    trxManager.run {
                        val job = repoJobs.findById(jobId)
                        if (job != null) {
                            val updatedJob =
                                job.copy(
                                    status = finalStatus,
                                    bestSolution = result.bestSolution,
                                    executionLogs = result.logs,
                                )
                            repoJobs.save(updatedJob)
                        }

                        repoProjects.save(projectFromQueue.copy(status = finalStatus))
                    }

                    logger.info("Worker-$workerId terminou o Job $jobId do Projeto ${projectFromQueue.id} -> $finalStatus")
                } finally {
                    yamlConfigPath?.let { configService.cleanupTemporaryConfigFile(it) }
                }
            } catch (e: CancellationException) {
                logger.info("Worker-$workerId foi cancelado. Desligando...")
                break
            } catch (e: Exception) {
                logger.error("Worker-$workerId falhou criticamente no processamento:", e)
                currentProjectId?.let { failedId ->
                    try {
                        trxManager.run {
                            val projectFail = repoProjects.findById(failedId)
                            if (projectFail != null) {
                                repoProjects.save(projectFail.copy(status = EvolutionStatus.FAILED))
                            }
                        }
                    } catch (updateEx: Exception) {
                        logger.error("Falha ao registar o estado de erro do projeto na BD:", updateEx)
                    }
                }
            }
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
}
