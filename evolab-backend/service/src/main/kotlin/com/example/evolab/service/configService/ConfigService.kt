package com.example.evolab.service.configService

import com.example.evolab.domain.config.Config
import com.example.evolab.service.auxiliary.Either
import java.nio.file.Path

interface ConfigService {
    fun createConfig(
        userId: Int,
        llmCredentialsId: Int,
        modelName: String,
        maxIter: Int,
        checkPointInterval: Int,
        additionalParams: Map<String, String>,
    ): Either<ConfigError, Config>

    fun getConfigById(configId: Int, userId: Int): Either<ConfigError, Config>

    fun listConfigsByUser(userId: Int): List<Config>

    fun updateConfig(
        configId: Int,
        userId: Int,
        modelName: String,
        maxIter: Int,
        checkPointInterval: Int,
        additionalParams: Map<String, String>,
    ): Either<ConfigError, Config>

    fun deleteConfig(configId: Int, userId: Int): Either<ConfigError, Boolean>

    fun buildRuntimeConfig(
        config: Config,
        projectId: Int,
        jobId: Int,
    ): Config

    fun generateTemporaryConfigFile(
        runtimeConfig: Map<String, Any>,
    ): Either<ConfigError, Path>

    fun cleanupTemporaryConfigFile(path: Path): Either<ConfigError, Boolean>
}