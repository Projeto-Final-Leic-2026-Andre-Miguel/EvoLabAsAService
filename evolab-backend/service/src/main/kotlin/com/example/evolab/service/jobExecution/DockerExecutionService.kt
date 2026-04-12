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
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
//import com.github.dockerjava.zerodep.ZerodepDockerHttpClient
import jakarta.annotation.PostConstruct
import jakarta.inject.Named
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path


private const val IMAGE_NAME = "ghcr.io/algorithmicsuperintelligence/openevolve:latest"

@Named
class DockerExecutionService {

    private lateinit var dockerClient: DockerClient
    private val logger = LoggerFactory.getLogger(DockerExecutionService::class.java)

    @PostConstruct
    fun init() {
        // O docker-java tem por base: unix:///var/run/docker.sock no macOS/Linux
        val config = DefaultDockerClientConfig.createDefaultConfigBuilder().build()
        val httpClient = ApacheDockerHttpClient.Builder()
            .dockerHost(config.dockerHost)
            .build()
        
        dockerClient = DockerClientImpl.getInstance(config, httpClient)
    }

    /**
     * Utiliza API docker-java para subir o OpenEvolve! Bloqueia at a run terminar de forma async com Dispatcher IO
     */
    suspend fun runOpenEvolveContainerForProject(
        project: Project,
        yamlConfigPath: Path?,
        workerId: Int,
        environment: Map<String, String> = emptyMap(),
    ): DockerExecutionResult {
        return withContext(Dispatchers.IO) {
            logger.info("Worker-$workerId: A iniciar contentor do OpenEvolve para o Project ID: ${project.id}...")

            try {
                dockerClient.pullImageCmd(IMAGE_NAME).exec(PullImageResultCallback()).awaitCompletion()
            } catch (e: Exception) {
                logger.warn("Worker-$workerId: Falha ao puxar a imagem $IMAGE_NAME ou ja estava atualizada. Erro: ${e.message}")
            }

            val tempDir = Files.createTempDirectory("evolab_project_${project.id}_").toFile()
            
            try {
                val initialProgramFile = File(tempDir, "initial_program.py")
                initialProgramFile.writeText(project.initialProgram!!)

                val evaluatorFile = File(tempDir, "evaluator.py")
                evaluatorFile.writeText(project.evaluatorCode!!)

                val cmdArgs = mutableListOf("python", "-m", "openevolve", "initial_program.py", "evaluator.py")

                // Mover o yaml gerado pelo ConfigService para o tempDir
                if (yamlConfigPath != null) {
                    val configFileInTemp = File(tempDir, "config.yaml")
                    Files.copy(yamlConfigPath, configFileInTemp.toPath())
                    cmdArgs.add("--config")
                    cmdArgs.add("config.yaml")
                }

                val hostPath = tempDir.absolutePath
                val containerPath = "/app"

                // 2. Criar configuração do contentor
                val hostConfig = HostConfig.newHostConfig()
                    .withBinds(Bind(hostPath, Volume(containerPath)))

                val createCmdResponse = dockerClient.createContainerCmd(IMAGE_NAME)
                    .withHostConfig(hostConfig)
                    .withWorkingDir(containerPath)
                    .withEnv(environment.map { (key, value) -> "$key=$value" })
                    .withCmd(cmdArgs)
                    .exec()

                val containerId = createCmdResponse.id
                logger.info("Worker-$workerId: Contentor criado com ID $containerId")

                // 3. Iniciar contentor
                dockerClient.startContainerCmd(containerId).exec()

                // Buffer para capturar os logs stdOut e stdErr
                val logsBuilder = StringBuilder()

                // 4. Capturar logs (assincrono)
                dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(object : ResultCallback.Adapter<Frame>() {
                        override fun onNext(frame: Frame) {
                            val msg = String(frame.payload, Charsets.UTF_8)
                            logsBuilder.append(msg)
                            // logger.debug("[$containerId] \${frame.streamType}: $msg")
                        }
                    })

                // 5. Esperar que o processo evolutivo termine
                val exitCode = dockerClient.waitContainerCmd(containerId)
                    .exec(WaitContainerResultCallback())
                    .awaitStatusCode()

                logger.info("Worker-$workerId: Processo evolutivo terminou com código de saída $exitCode")

                // 6. Limpar o docker
                dockerClient.removeContainerCmd(containerId).withForce(true).exec()

                // A ler uma eventual output "best_solution.py" (se openEvolve a gerar)
                // Isto depende de como a biblioteca guarda o ficheiro resolvido. 
                // Assumindo que escreve um best_solution.py no dir atual:
                val bestSolutionFile = File(tempDir, "best_solution.py")
                val bestSolutionCode = if (bestSolutionFile.exists()) bestSolutionFile.readText() else null

                return@withContext DockerExecutionResult(
                    exitCode = exitCode,
                    logs = logsBuilder.toString(),
                    bestSolution = bestSolutionCode
                )
            } finally {
                // 7. Cleanup: Apagar ficheiros temporários host
                tempDir.deleteRecursively()
            }
        }
    }
}

data class DockerExecutionResult(
    val exitCode: Int,
    val logs: String,
    val bestSolution: String?
)
