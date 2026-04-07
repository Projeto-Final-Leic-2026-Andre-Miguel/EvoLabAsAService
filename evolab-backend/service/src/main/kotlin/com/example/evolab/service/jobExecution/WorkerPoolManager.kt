package com.example.evolab.service.jobExecution

import com.example.evolab.domain.LLMCredentials.LLMCredentials
import com.example.evolab.domain.config.Config
import jakarta.inject.Named
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import jakarta.annotation.PreDestroy
import jakarta.annotation.PostConstruct
import kotlin.coroutines.coroutineContext
import com.example.evolab.repo.transactions.TransactionManager
import com.example.evolab.domain.evolution.EvolutionStatus
import com.example.evolab.domain.project.Project
import com.example.evolab.repo.transactions.Transaction
import com.example.evolab.service.auxiliary.Failure
import com.example.evolab.service.auxiliary.Success
import com.example.evolab.service.auxiliary.failure
import com.example.evolab.service.configService.ConfigService
import com.example.evolab.service.project.ProjectService
import com.example.evolab.service.security.EncryptionService
import java.nio.file.Path

@Named
class WorkerPoolManager(
    private val jobQueue: JobQueue,
    private val dockerExecutionService: DockerExecutionService,
    private val trxManager: TransactionManager,
    private val configService: ConfigService,
    private val encryptionService: EncryptionService,
    private val poolSize: Int = 3
) {
    private val logger = LoggerFactory.getLogger(WorkerPoolManager::class.java)
    private val poolScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * O PostConstruct garante que esta corrotina arranca mal o backend(Service) fique operacional.
     */
    @PostConstruct
    fun startWorkers() {
        logger.info("A inicializar $poolSize Workers para a Fila de Execução do OpenEvolve...")

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

                // Projet is pulled from the Queue, and if its empty, it execution is suspended
                val projectFromQueue = jobQueue.dequeue()
                currentProjectId = projectFromQueue.id

                logger.info("Worker-$workerId pegou no Projeto: \${projectFromQueue.id}")

                var yamlConfigPath: Path? = null


                val setupData = trxManager.run {

                    val config = findConfigById(projectFromQueue.configId!!)
                    val credentials = findCrendentialById(config?.llmCredentialsId!! )

                    repoProjects.save(projectFromQueue.copy(status = EvolutionStatus.RUNNING))

                    val newJobId = repoJobs.createJob(
                        projectId = projectFromQueue.id,
                        status = EvolutionStatus.RUNNING
                    )

                    val decryptedKey = credentials!!.apiKeyEncrypted?.let { encryptionService.decrypt(it) } ?: throw Exception("Error on Decryption for credential id: \${credentials.id}")
                    
                    // Extrair todos os payload parameters para gerar o yaml temporario
                    val runtimeMap = mutableMapOf<String, Any>()
                    runtimeMap["max_iterations"] = config.maxIter
                    runtimeMap["checkpoint_interval"] = config.checkPointInterval
                    
                    // Construir a estrutura exigida pelo parser:
                    val llmMap = mutableMapOf<String, Any>()
                    llmMap["models"] = listOf(mapOf("name" to config.modelName, "weight" to 1.0))
                    llmMap["api_key"] = decryptedKey
                    runtimeMap["llm"] = llmMap
                    // Adicionar parametros adicionais fornecidos pelo utilizador
                    config.additionalParams.forEach { (k, v) -> runtimeMap[k] = v }

                    Pair(newJobId, runtimeMap)
                }

                val (jobId, runtimeMap) = setupData

                try {
                    // 3. Aproveitar o ConfigService para validar e gerar o YAML no disco host
                    when(val configResult = configService.generateTemporaryConfigFile(runtimeMap)) {
                        is Success -> yamlConfigPath = configResult.value
                        is Failure -> throw IllegalStateException("Falha ao gerar o Config YAML: \${configResult.value}")
                    }

                    val result = dockerExecutionService.runOpenEvolveContainerForProject(
                        project = projectFromQueue,
                        yamlConfigPath = yamlConfigPath,
                        workerId = workerId
                    )
                    
                    val isSuccess = result.exitCode == 0
                    val finalStatus = if (isSuccess) EvolutionStatus.COMPLETED else EvolutionStatus.FAILED

                    trxManager.run {
                        val job = repoJobs.findById(jobId)
                        if (job != null) {
                            val updatedJob = job.copy(
                                status = finalStatus,
                                bestSolution = result.bestSolution,
                                executionLogs = result.logs
                            )
                            repoJobs.save(updatedJob)
                        }
                        
                        repoProjects.save(projectFromQueue.copy(status = finalStatus))
                    }

                    logger.info("Worker-$workerId terminou o Job $jobId do Projeto \${project.id} -> \$finalStatus")

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
    }


    private fun Transaction.findProjectById(projectId: Int): Project? =
            repoProjects.findById(projectId)

    private fun Transaction.findConfigById(configId : Int) : Config? =
            repoConfigs.findById(configId)

    private fun Transaction.findCrendentialById(credentialId : Int) : LLMCredentials? =
        repoLLmCredentials.findById(credentialId)


}
