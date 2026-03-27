package com.example.evolab.service.configService

sealed class ConfigError {
    data object ConfigNotFound : ConfigError()

    data object AccessDenied : ConfigError()

    data object InvalidModelName : ConfigError()

    data object InvalidMaxIterations : ConfigError()

    data object InvalidCheckpointInterval : ConfigError()

    data object ErrorDeletingConfig : ConfigError()

    data object ErrorCreatingTemporaryConfigFile : ConfigError()

    data object ErrorCleaningTemporaryConfigFile : ConfigError()
}
