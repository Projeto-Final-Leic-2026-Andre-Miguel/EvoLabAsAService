package com.example.evolab.service.configService

internal object ConfigInputValidator {
    fun validate(
        modelName: String,
        maxIter: Int,
        checkPointInterval: Int,
    ): ConfigError? {
        if (modelName.isBlank()) return ConfigError.InvalidModelName
        if (maxIter <= 0) return ConfigError.InvalidMaxIterations
        if (checkPointInterval <= 0) return ConfigError.InvalidCheckpointInterval
        if (checkPointInterval > maxIter) return ConfigError.CheckpointIntervalExceedsMaxIterations
        return null
    }
}
