package com.example.evolab.service.configService

import com.example.evolab.domain.config.Config
import com.example.evolab.domain.evolution.EvolutionStatus
import com.example.evolab.repo.repoConfig.RepositoryConfig
import com.example.evolab.repo.transactions.TransactionManager
import com.example.evolab.service.auxiliary.Either
import com.example.evolab.service.auxiliary.failure
import com.example.evolab.service.auxiliary.success
import jakarta.inject.Named
import java.nio.file.Files
import java.nio.file.Path

@Named
class ConfigServiceImpl(
    private val repoConfig: RepositoryConfig,
    private val trxManager: TransactionManager,
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
        validateInput(modelName, maxIter, checkPointInterval)?.let { return failure(it) }

        return trxManager.run {
            val project =
                projectId?.let { projectId ->
                    val current = repoProjects.findById(projectId) ?: return@run failure(ConfigError.ProjectNotFound)
                    if (current.userId != userId) return@run failure(ConfigError.AccessDenied)
                    if (current.status != EvolutionStatus.CREATED) return@run failure(ConfigError.ProjectNotEditable)
                    current
                }

            val id =
                repoConfigs.createConfig(
                    userId = userId,
                    llmCredentialsId = llmCredentialsId,
                    modelName = modelName,
                    maxIter = maxIter,
                    checkPointInterval = checkPointInterval,
                    additionalParams = additionalParams,
                )

            val created = repoConfigs.findById(id) ?: return@run failure(ConfigError.ConfigNotFound)

            if (project != null) {
                repoProjects.save(project.copy(configId = created.id))
            }

            success(created)
        }
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
        validateInput(modelName, maxIter, checkPointInterval)?.let { return failure(it) }

        val current = repoConfig.findById(configId) ?: return failure(ConfigError.ConfigNotFound)
        if (current.userId != userId) return failure(ConfigError.AccessDenied)

        val updated = current.copy(
            modelName = modelName,
            maxIter = maxIter,
            checkPointInterval = checkPointInterval,
            additionalParams = additionalParams,
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
        val openEvolveConfig = OpenEvolveConfigAuxiliary.toOpenEvolveConfig(runtimeConfig)
            ?: return failure(ConfigError.InvalidOpenEvolveConfig("Invalid OpenEvolve payload structure"))

        OpenEvolveConfigAuxiliary.validateOpenEvolveConfig(openEvolveConfig)?.let { return failure(it) }

        return try {
            val tempFile = Files.createTempFile("evolab-config-", ".yaml")
            Files.writeString(tempFile, OpenEvolveConfigAuxiliary.renderOpenEvolveYaml(openEvolveConfig))
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
}
