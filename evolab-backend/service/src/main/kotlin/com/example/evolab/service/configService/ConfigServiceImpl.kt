package com.example.evolab.service.configService

import com.example.evolab.domain.config.Config
import com.example.evolab.service.auxiliary.Either
import jakarta.inject.Named
import java.nio.file.Path

@Named
class ConfigServiceImpl(
    // deps: TransactionManager, ObjectMapper, Clock, etc.
) : ConfigService {
    override fun createConfig(
        userId: Int,
        llmCredentialsId: Int,
        modelName: String,
        maxIter: Int,
        checkPointInterval: Int,
        additionalParams: Map<String, String>,
    ): Either<ConfigError, Config> {
        TODO("Not yet implemented")
    }

    override fun getConfigById(configId: Int, userId: Int): Either<ConfigError, Config> {
        TODO("Not yet implemented")
    }

    override fun listConfigsByUser(userId: Int): List<Config> {
        TODO("Not yet implemented")
    }

    override fun updateConfig(
        configId: Int,
        userId: Int,
        modelName: String,
        maxIter: Int,
        checkPointInterval: Int,
        additionalParams: Map<String, String>,
    ): Either<ConfigError, Config> {
        TODO("Not yet implemented")
    }

    override fun deleteConfig(configId: Int, userId: Int): Either<ConfigError, Boolean> {
        TODO("Not yet implemented")
    }

    override fun buildRuntimeConfig(
        config: Config,
        projectId: Int,
        jobId: Int,
    ): Map<String, Any> {
        TODO("Not yet implemented")
    }

    override fun generateTemporaryConfigFile(
        runtimeConfig: Map<String, Any>,
    ): Path {
        TODO("Not yet implemented")
    }

    override fun cleanupTemporaryConfigFile(path: Path): Boolean {
        TODO("Not yet implemented")
    }
}

