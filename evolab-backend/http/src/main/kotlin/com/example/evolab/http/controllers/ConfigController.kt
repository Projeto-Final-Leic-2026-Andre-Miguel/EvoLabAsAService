package com.example.evolab.http.controllers

import com.example.evolab.domain.user.AuthenticatedUser
import com.example.evolab.http.model.config.CreateConfigInput
import com.example.evolab.http.model.config.GenerateTemporaryConfigFileInput
import com.example.evolab.http.model.config.TemporaryConfigFileOutput
import com.example.evolab.http.model.config.UpdateConfigInput
import com.example.evolab.http.model.config.UserConfigOutput
import com.example.evolab.http.model.problem.Problem
import com.example.evolab.service.auxiliary.Either
import com.example.evolab.service.auxiliary.Failure
import com.example.evolab.service.auxiliary.Success
import com.example.evolab.service.configService.ConfigError
import com.example.evolab.service.configService.OpenEvolvePayloadBuilder
import com.example.evolab.service.configService.ConfigService
import com.example.evolab.service.project.ProjectService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/configs")
class ConfigController(
    private val configService: ConfigService,
    private val projectService: ProjectService,
) {
    @PostMapping
    fun createConfig(
        @RequestBody input: CreateConfigInput,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> =
        when (
            val result =
                configService.createConfig(
                    userId = authenticatedUser.user.id,
                    projectId = input.projectId,
                    llmCredentialsId = input.llmCredentialsId,
                    modelName = input.modelName,
                    maxIter = input.maxIter,
                    checkPointInterval = input.checkPointInterval,
                    additionalParams = input.additionalParams,
                )
        ) {
            is Success -> {
                val config = result.value
                val projectId =
                    when (val projectsResult = projectService.getAllProjectsFromUser(authenticatedUser.user.id)) {
                        is Success -> projectsResult.value.firstOrNull { it.configId == config.id }?.id
                        is Failure -> null
                    }
                val output =
                    UserConfigOutput(
                        configId = config.id,
                        projectId = projectId,
                        userId = config.userId,
                        llmCredentialsId = config.llmCredentialsId,
                        modelName = config.modelName,
                        maxIter = config.maxIter,
                        checkPointInterval = config.checkPointInterval,
                        additionalParams = config.additionalParams,
                        createdAt = config.createdAt,
                    )
                ResponseEntity.status(HttpStatus.CREATED)
                    .header("Location", "/api/configs/${config.id}")
                    .body(output)
            }

            is Failure -> mapServiceErrors(result.value)
        }

    @GetMapping("/{id}")
    fun getConfigById(
        @PathVariable id: Int,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> =
        when (
            val result: Either<ConfigError, *> =
                configService.getConfigById(
                    configId = id,
                    userId = authenticatedUser.user.id,
                )
        ) {
            is Success -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Failure -> mapServiceErrors(result.value)
        }

    @GetMapping("/me")
    fun listConfigsByUser(authenticatedUser: AuthenticatedUser): ResponseEntity<*> {
        val userId = authenticatedUser.user.id
        val configs = configService.listConfigsByUser(userId)
        val projectsByConfigId =
            when (val projectsResult = projectService.getAllProjectsFromUser(userId)) {
                is Success -> projectsResult.value.filter { it.configId != null }.associateBy { it.configId!! }
                is Failure -> emptyMap()
            }

        val output =
            configs.map { config ->
                UserConfigOutput(
                    configId = config.id,
                    projectId = projectsByConfigId[config.id]?.id,
                    userId = config.userId,
                    llmCredentialsId = config.llmCredentialsId,
                    modelName = config.modelName,
                    maxIter = config.maxIter,
                    checkPointInterval = config.checkPointInterval,
                    additionalParams = config.additionalParams,
                    createdAt = config.createdAt,
                )
            }

        return ResponseEntity.status(HttpStatus.OK).body(output)
    }

    @PostMapping("/{id}/temporary-file")
    fun generateTemporaryConfigFile(
        @PathVariable id: Int,
        @RequestBody input: GenerateTemporaryConfigFileInput,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val userId = authenticatedUser.user.id

        val configResult =
            configService.getConfigById(
                configId = id,
                userId = userId,
            )

        val config =
            when (configResult) {
                is Success -> configResult.value
                is Failure -> return mapServiceErrors(configResult.value)
            }

        val projectResult = projectService.getProject(input.projectId, userId)
        when (projectResult) {
            is Success -> {
                // Keep going: project exists and belongs to the authenticated user.
            }

            is Failure -> {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build<Unit>()
            }
        }

        val jobId = input.jobId ?: config.id
        val runtimeConfig = configService.buildRuntimeConfig(config, input.projectId, jobId)
        val payload = OpenEvolvePayloadBuilder.build(runtimeConfig)

        return when (val fileResult = configService.generateTemporaryConfigFile(payload)) {
            is Success -> ResponseEntity.status(HttpStatus.CREATED).body(TemporaryConfigFileOutput(fileResult.value.toString()))
            is Failure -> mapServiceErrors(fileResult.value)
        }
    }

    @PutMapping("/{id}")
    fun updateConfig(
        @PathVariable id: Int,
        @RequestBody input: UpdateConfigInput,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val userId = authenticatedUser.user.id
        val result =
            configService.updateConfig(
                configId = id,
                userId = userId,
                modelName = input.modelName,
                maxIter = input.maxIter,
                checkPointInterval = input.checkPointInterval,
                additionalParams = input.additionalParams,
            )

        return when (result) {
            is Success -> {
                val config = result.value
                val projectId =
                    when (val projectsResult = projectService.getAllProjectsFromUser(userId)) {
                        is Success -> projectsResult.value.firstOrNull { it.configId == config.id }?.id
                        is Failure -> null
                    }
                val output =
                    UserConfigOutput(
                        configId = config.id,
                        projectId = projectId,
                        userId = config.userId,
                        llmCredentialsId = config.llmCredentialsId,
                        modelName = config.modelName,
                        maxIter = config.maxIter,
                        checkPointInterval = config.checkPointInterval,
                        additionalParams = config.additionalParams,
                        createdAt = config.createdAt,
                    )
                ResponseEntity.status(HttpStatus.OK).body(output)
            }
            is Failure -> mapServiceErrors(result.value)
        }
    }

    @DeleteMapping("/{id}")
    fun deleteConfig(
        @PathVariable id: Int,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val result = configService.deleteConfig(id, authenticatedUser.user.id)

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK)
                .body("Config with id '$id' was successfully deleted")

            is Failure -> mapServiceErrors(result.value)
        }
    }

    private fun mapServiceErrors(error: ConfigError): ResponseEntity<*> {
        return when (error) {
            is ConfigError.ConfigNotFound -> Problem.ConfigNotFound.response(HttpStatus.NOT_FOUND)
            is ConfigError.ProjectNotFound -> Problem.ProjectNotFound.response(HttpStatus.NOT_FOUND)
            is ConfigError.AccessDenied -> Problem.ConfigAccessDenied.response(HttpStatus.FORBIDDEN)
            is ConfigError.ProjectNotEditable -> Problem.InvalidProjectStatus.response(HttpStatus.CONFLICT)
            is ConfigError.InvalidModelName,
            is ConfigError.InvalidMaxIterations,
            is ConfigError.InvalidCheckpointInterval,
            -> Problem.InvalidProjectInput.response(HttpStatus.BAD_REQUEST)

            is ConfigError.InvalidOpenEvolveConfig ->
                Problem.InvalidProjectInput.withDetail(error.reason).response(HttpStatus.BAD_REQUEST)

            is ConfigError.ErrorDeletingConfig,
            is ConfigError.ErrorCreatingTemporaryConfigFile,
            is ConfigError.ErrorCleaningTemporaryConfigFile,
            -> Problem.UnknownError.response(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}

