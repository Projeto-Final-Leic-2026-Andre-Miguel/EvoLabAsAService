package com.example.evolab.service.jobExecution

import com.example.evolab.domain.project.Project
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.command.WaitContainerResultCallback
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Volume
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient
import jakarta.annotation.PostConstruct
import jakarta.inject.Named
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant


private const val IMAGE_NAME = "ghcr.io/algorithmicsuperintelligence/openevolve:latest"

@Named
class DockerExecutionService {

    private lateinit var dockerClient: DockerClient
    private val logger = LoggerFactory.getLogger(DockerExecutionService::class.java)

    @PostConstruct
    fun init() {
        val dockerHostEnv = System.getenv("DOCKER_HOST")

        val configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder()
        if (!dockerHostEnv.isNullOrBlank()) {
            configBuilder.withDockerHost(dockerHostEnv)
            logger.info("Initializing Docker client with env DOCKER_HOST: $dockerHostEnv")
        } else {
            val os = System.getProperty("os.name").lowercase()
            val socketPath = if (os.contains("mac")) {
                val home = System.getProperty("user.home")
                val desktopSocket = File("$home/.docker/run/docker.sock")
                if (desktopSocket.exists()) {
                    "unix://${desktopSocket.absolutePath}"
                } else {
                    "unix:///var/run/docker.sock"
                }
            } else {
                "unix:///var/run/docker.sock"
            }
            logger.info("Initializing Docker client with forced socket: $socketPath")
            configBuilder.withDockerHost(socketPath)
        }

        val config = configBuilder.build()
        
        val httpClient = ZerodepDockerHttpClient.Builder()
            .dockerHost(config.dockerHost)
            .sslConfig(config.sslConfig)
            .build()
            
        dockerClient = DockerClientImpl.getInstance(config, httpClient)
        
        logger.info("Docker client built successfully")
    }

    /**
     * Utiliza API docker-java para subir o OpenEvolve! Bloqueia at a run terminar de forma async com Dispatcher IO
     */
    suspend fun runOpenEvolveContainerForProject(
        project: Project,
        yamlConfigPath: Path?,
        workerId: Int,
        environment: Map<String, String> = emptyMap(),
        onContainerStarted: suspend (String, Instant) -> Unit = { _, _ -> }
    ): DockerExecutionResult {
        return withContext(Dispatchers.IO) {
            logger.info("Worker-$workerId: A iniciar contentor do OpenEvolve para o Project ID: ${project.id}...")

            pullDockerImage(workerId)

            val tempDir = Files.createTempDirectory("evolab_project_${project.id}_").toFile()

            try {
                val cmdArgs = setupWorkspaceFiles(tempDir, project, yamlConfigPath)
                val containerPath = "/workspace"

                val containerId = createContainer(tempDir.absolutePath, containerPath, environment, cmdArgs)
                logger.info("Worker-$workerId: Contentor criado com ID $containerId")

                val startedAt = Instant.now()
                onContainerStarted(containerId, startedAt)
                dockerClient.startContainerCmd(containerId).exec()

                val logsBuilder = StringBuilder()
                collectContainerLogs(containerId, logsBuilder)

                val exitCode = waitForContainer(containerId)
                val finishedAt = Instant.now()
                logger.info("Worker-$workerId: Processo evolutivo terminou com código de saída $exitCode")

                removeContainer(containerId)

                val (actualLogs, bestSolutionCode) = extractAndPersistResults(tempDir, logsBuilder.toString(), project.id, workerId)

                return@withContext DockerExecutionResult(
                    exitCode = exitCode,
                    logs = actualLogs,
                    bestSolution = bestSolutionCode,
                    containerId = containerId,
                    startedAt = startedAt,
                    finishedAt = finishedAt
                )
            } finally {
                tempDir.deleteRecursively()
            }
        }
    }

    private fun pullDockerImage(workerId: Int) {
        try {
            dockerClient.pullImageCmd(IMAGE_NAME).exec(PullImageResultCallback()).awaitCompletion()
        } catch (e: Exception) {
            logger.warn("Worker-$workerId: Falha ao puxar a imagem $IMAGE_NAME ou ja estava atualizada. Erro: ${e.message}")
        }
    }

    private fun setupWorkspaceFiles(tempDir: File, project: Project, yamlConfigPath: Path?): MutableList<String> {
        val initialProgramFile = File(tempDir, "initial_program.py")
        initialProgramFile.writeText(project.initialProgram!!)

        val evaluatorFile = File(tempDir, "evaluator.py")
        evaluatorFile.writeText(project.evaluatorCode!!)

        val cmdArgs = mutableListOf("initial_program.py", "evaluator.py")

        if (yamlConfigPath != null) {
            val configFileInTemp = File(tempDir, "config.yaml")
            Files.copy(yamlConfigPath, configFileInTemp.toPath())
            cmdArgs.add("--config")
            cmdArgs.add("config.yaml")
        }
        return cmdArgs
    }

    private fun createContainer(hostPath: String, containerPath: String, environment: Map<String, String>, cmdArgs: List<String>): String {
        val hostConfig = HostConfig.newHostConfig()
            .withBinds(Bind(hostPath, Volume(containerPath)))
            .withNetworkMode("host")

        val createCmdResponse = dockerClient.createContainerCmd(IMAGE_NAME)
            .withHostConfig(hostConfig)
            .withWorkingDir(containerPath)
            .withEnv(environment.map { (key, value) -> "$key=$value" })
            .withCmd(cmdArgs)
            .exec()

        return createCmdResponse.id
    }

    private fun collectContainerLogs(containerId: String, logsBuilder: StringBuilder) {
        dockerClient.logContainerCmd(containerId)
            .withStdOut(true)
            .withStdErr(true)
            .withFollowStream(true)
            .exec(object : ResultCallback.Adapter<Frame>() {
                override fun onNext(frame: Frame) {
                    val msg = String(frame.payload, Charsets.UTF_8)
                    logsBuilder.append(msg)
                }
            })
    }

    private fun waitForContainer(containerId: String): Int {
        return dockerClient.waitContainerCmd(containerId)
            .exec(WaitContainerResultCallback())
            .awaitStatusCode()
    }

    private fun removeContainer(containerId: String) {
        dockerClient.removeContainerCmd(containerId).withForce(true).exec()
    }

    private fun extractAndPersistResults(tempDir: File, fallbackLogs: String, projectId: Int, workerId: Int): Pair<String, String?> {
        val openevolveOutputDir = File(tempDir, "openevolve_output")
        val logsDir = File(openevolveOutputDir, "logs")
        val logFile = logsDir.listFiles()?.firstOrNull { it.extension == "log" }
        val actualLogs = logFile?.readText() ?: fallbackLogs

        val bestDir = File(openevolveOutputDir, "best")
        val bestSolutionFile = File(bestDir, "best_program.py")
        val bestSolutionCode = if (bestSolutionFile.exists()) bestSolutionFile.readText() else null

        val persistentOutputDir = File("evolab_runs/project_${projectId}_${System.currentTimeMillis()}")
        persistentOutputDir.parentFile.mkdirs()
        if (openevolveOutputDir.exists()) {
            openevolveOutputDir.copyRecursively(persistentOutputDir, overwrite = true)
            logger.info("Worker-$workerId: Output do OpenEvolve guardado com sucesso em ${persistentOutputDir.absolutePath}")
        }
        return Pair(actualLogs, bestSolutionCode)
    }
}

data class DockerExecutionResult(
    val exitCode: Int,
    val logs: String,
    val bestSolution: String?,
    val containerId: String?,
    val startedAt: Instant?,
    val finishedAt: Instant?
)
