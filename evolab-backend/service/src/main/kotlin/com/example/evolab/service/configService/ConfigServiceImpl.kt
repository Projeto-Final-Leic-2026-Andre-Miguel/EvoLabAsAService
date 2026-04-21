package com.example.evolab.service.configService

import com.example.evolab.domain.LLMCredentials.LLM
import com.example.evolab.domain.config.Config
import com.example.evolab.domain.evolution.EvolutionStatus
import com.example.evolab.repo.repoConfig.RepositoryConfig
import com.example.evolab.repo.repoLLMCredentials.RepositoryLLMCredentials
import com.example.evolab.repo.repoProject.RepositoryProject
import com.example.evolab.service.auxiliary.Either
import com.example.evolab.service.auxiliary.failure
import com.example.evolab.service.auxiliary.success
import jakarta.inject.Named
import java.nio.file.Files
import java.nio.file.Path

@Named
class ConfigServiceImpl(
    private val repoConfig: RepositoryConfig,
    private val repoProject: RepositoryProject,
    private val repoLLMCredentials: RepositoryLLMCredentials,
) : ConfigService {
    override fun createConfig(
        userId: Int,
        projectId: Int?,
        llmCredentialsId: Int,
        modelName: String,
        maxIter: Int,
        checkPointInterval: Int,
        additionalParams: Map<String, String>,
    ): Either<ConfigError, Config> {

        val credential =
            repoLLMCredentials.findById(llmCredentialsId)
                ?: return failure(ConfigError.InvalidOpenEvolveConfig("LLM credential '$llmCredentialsId' was not found"))

        val (finalModelName, finalParams) = if (credential.llm == LLM.LOCAL_MODEL) {
            val localCred = repoLLMCredentials.findLocalModelCredentialById(llmCredentialsId)
                ?: return failure(ConfigError.InvalidOpenEvolveConfig("Local model credential '$llmCredentialsId' was not found"))
            
            // Force the model name and append the docker host base url if not provided
            val updatedParams = additionalParams.toMutableMap()
            if (updatedParams["llm.api_base"].isNullOrBlank()) {
                updatedParams["llm.api_base"] = "http://host.docker.internal:${localCred.port}/v1"
            }
            Pair(localCred.modelName, updatedParams)
        } else {
            Pair(modelName, additionalParams)
        }

        validateInput(finalModelName, maxIter, checkPointInterval)?.let { return failure(it) }

            val project =
                projectId?.let { projectId ->
                    val current = repoProject.findById(projectId) ?: return failure(ConfigError.ProjectNotFound)
                    if (current.userId != userId) return failure(ConfigError.AccessDenied)
                    if (current.status != EvolutionStatus.CREATED) return failure(ConfigError.ProjectNotEditable)
                    current
                }

            val normalizedParams =
                try {
                    normalizeAdditionalParams(credential, finalModelName, finalParams)
                } catch (e: IllegalStateException) {
                    return failure(ConfigError.InvalidOpenEvolveConfig(e.message ?: "Invalid LLM configuration"))
                }

            val id =
                repoConfig.createConfig(
                    userId = userId,
                    llmCredentialsId = llmCredentialsId,
                    modelName = finalModelName,
                    maxIter = maxIter,
                    checkPointInterval = checkPointInterval,
                    additionalParams = normalizedParams,
                )

            val created = repoConfig.findById(id) ?: return failure(ConfigError.ConfigNotFound)

            if (project != null) {
                repoProject.save(project.copy(configId = created.id))
            }

            return success(created)
        }


    override fun getConfigById(configId: Int, userId: Int): Either<ConfigError, Config> {
        val config = repoConfig.findById(configId) ?: return failure(ConfigError.ConfigNotFound)
        if (config.userId != userId) return failure(ConfigError.AccessDenied)
        return success(config)
    }

    override fun listConfigsByUser(userId: Int): List<Config> = repoConfig.findAllByUserId(userId)

    override fun updateConfig(
        configId: Int,
        userId: Int,
        modelName: String,
        maxIter: Int,
        checkPointInterval: Int,
        additionalParams: Map<String, String>,
    ): Either<ConfigError, Config> {
        val current = repoConfig.findById(configId) ?: return failure(ConfigError.ConfigNotFound)
        if (current.userId != userId) return failure(ConfigError.AccessDenied)

        val credential =
            repoLLMCredentials.findById(current.llmCredentialsId)
                ?: return failure(ConfigError.InvalidOpenEvolveConfig("LLM credential '${current.llmCredentialsId}' was not found"))

        val (finalModelName, finalParams) = if (credential.llm == com.example.evolab.domain.LLMCredentials.LLM.LOCAL_MODEL) {
            val localCred = repoLLMCredentials.findLocalModelCredentialById(current.llmCredentialsId)
                ?: return failure(ConfigError.InvalidOpenEvolveConfig("Local model credential '${current.llmCredentialsId}' was not found"))
            
            val updatedParams = additionalParams.toMutableMap()
            if (updatedParams["llm.api_base"].isNullOrBlank()) {
                updatedParams["llm.api_base"] = "http://host.docker.internal:${localCred.port}/v1"
            }
            Pair(localCred.modelName, updatedParams)
        } else {
            Pair(modelName, additionalParams)
        }

        validateInput(finalModelName, maxIter, checkPointInterval)?.let { return failure(it) }

        val normalizedParams =
            try {
                normalizeAdditionalParams(credential, finalModelName, finalParams)
            } catch (e: IllegalStateException) {
                return failure(ConfigError.InvalidOpenEvolveConfig(e.message ?: "Invalid LLM configuration"))
            }

        val updated = current.copy(
            modelName = finalModelName,
            maxIter = maxIter,
            checkPointInterval = checkPointInterval,
            additionalParams = normalizedParams,
        )

        repoConfig.save(updated)
        return success(updated)
    }

    override fun deleteConfig(configId: Int, userId: Int): Either<ConfigError, Boolean> {
        val current = repoConfig.findById(configId) ?: return failure(ConfigError.ConfigNotFound)
        if (current.userId != userId) return failure(ConfigError.AccessDenied)

        val deleted = repoConfig.deleteById(configId)
        return if (deleted) success(true) else failure(ConfigError.ErrorDeletingConfig)
    }

    override fun buildRuntimeConfig(
        config: Config,
        projectId: Int,
        jobId: Int,
    ): Config {
        val runtimeParams =
            config.additionalParams +
                mapOf(
                    "projectId" to projectId.toString(),
                    "jobId" to jobId.toString(),
                )

        return config.copy(additionalParams = runtimeParams)
    }

    override fun generateTemporaryConfigFile(
        runtimeConfig: Map<String, Any>,
    ): Either<ConfigError, Path> {
        val openEvolveConfig = OpenEvolveConfigParser.toOpenEvolveConfig(runtimeConfig)
            ?: return failure(ConfigError.InvalidOpenEvolveConfig("Invalid OpenEvolve payload structure"))

        OpenEvolveConfigValidator.validateOpenEvolveConfig(openEvolveConfig)?.let { return failure(it) }

        return try {
            val tempFile = Files.createTempFile("evolab-config-", ".yaml")
            Files.writeString(tempFile, OpenEvolveYamlRenderer.renderOpenEvolveYaml(openEvolveConfig))
            success(tempFile)
        } catch (_: Exception) {
            failure(ConfigError.ErrorCreatingTemporaryConfigFile)
        }
    }

    override fun cleanupTemporaryConfigFile(path: Path): Either<ConfigError, Boolean> {
        return try {
            success(Files.deleteIfExists(path))
        } catch (_: Exception) {
            failure(ConfigError.ErrorCleaningTemporaryConfigFile)
        }
    }

    private fun validateInput(
        modelName: String,
        maxIter: Int,
        checkPointInterval: Int,
    ): ConfigError? {
        if (modelName.isBlank()) return ConfigError.InvalidModelName
        if (maxIter <= 0) return ConfigError.InvalidMaxIterations
        if (checkPointInterval <= 0 || checkPointInterval > maxIter) return ConfigError.InvalidCheckpointInterval
        return null
    }

    private fun normalizeAdditionalParams(
        credential: com.example.evolab.domain.LLMCredentials.LLMCredentials,
        modelName: String,
        additionalParams: Map<String, String>,
    ): Map<String, String> {
        val resolvedApiBase =
            OpenEvolvePayloadBuilder.resolveApiBase(
                llm = credential.llm,
                modelName = modelName,
                configuredApiBase = additionalParams["llm.api_base"],
            )

        return additionalParams + mapOf("llm.api_base" to resolvedApiBase)
    }
}
