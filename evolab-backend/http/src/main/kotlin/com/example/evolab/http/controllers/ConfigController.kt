package com.example.evolab.http.controllers

import com.example.evolab.domain.user.AuthenticatedUser
import com.example.evolab.http.model.config.CreateConfigInput
import com.example.evolab.http.model.config.GenerateTemporaryConfigFileInput
import com.example.evolab.http.model.config.TemporaryConfigFileOutput
import com.example.evolab.http.model.config.UpdateConfigInput
import com.example.evolab.http.model.config.UserConfigOutput
import com.example.evolab.service.auxiliary.Either
import com.example.evolab.service.auxiliary.Failure
import com.example.evolab.service.auxiliary.Success
import com.example.evolab.service.configService.ConfigError
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
    ): ResponseEntity<*> {
        val result =
            configService.createConfig(
                userId = authenticatedUser.user.id,
                llmCredentialsId = input.llmCredentialsId,
                modelName = input.modelName,
                maxIter = input.maxIter,
                checkPointInterval = input.checkPointInterval,
                additionalParams = input.additionalParams,
            )

        return when (result) {
            is Success -> {
                ResponseEntity.status(HttpStatus.CREATED)
                    .header("Location", "/api/configs/${result.value.id}")
                    .body(result.value)
            }

            is Failure -> mapServiceErrors(result.value)
        }
    }

    @GetMapping("/{id}")
    fun getConfigById(
        @PathVariable id: Int,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val result: Either<ConfigError, *> =
            configService.getConfigById(
                configId = id,
                userId = authenticatedUser.user.id,
            )

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Failure -> mapServiceErrors(result.value)
        }
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
                //TODO: Project Endpoint not implemented yet
                //return ResponseEntity.status(HttpStatus.BAD_REQUEST).build<Unit>()
            }
        }

        val jobId = input.jobId ?: config.id
        val runtimeConfig = configService.buildRuntimeConfig(config, input.projectId, jobId)
        val payload = buildOpenEvolvePayload(runtimeConfig)

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
        val result =
            configService.updateConfig(
                configId = id,
                userId = authenticatedUser.user.id,
                modelName = input.modelName,
                maxIter = input.maxIter,
                checkPointInterval = input.checkPointInterval,
                additionalParams = input.additionalParams,
            )

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).body(result.value)
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
            is ConfigError.ConfigNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND).build<Unit>()
            is ConfigError.AccessDenied -> ResponseEntity.status(HttpStatus.FORBIDDEN).build<Unit>()
            is ConfigError.InvalidModelName,
            is ConfigError.InvalidMaxIterations,
            is ConfigError.InvalidCheckpointInterval,
            is ConfigError.InvalidOpenEvolveConfig,
            -> ResponseEntity.status(HttpStatus.BAD_REQUEST).build<Unit>()

            is ConfigError.ErrorDeletingConfig,
            is ConfigError.ErrorCreatingTemporaryConfigFile,
            is ConfigError.ErrorCleaningTemporaryConfigFile,
            -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Unit>()
        }
    }

    private fun buildOpenEvolvePayload(config: com.example.evolab.domain.config.Config): Map<String, Any> {
        val p = config.additionalParams

        val featureDimensions =
            p["database.feature_dimensions"]
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: listOf("complexity", "diversity")

        val declaredCustomMetrics =
            featureDimensions
                .filter { it !in setOf("complexity", "diversity") }

        return mapOf(
            "max_iterations" to config.maxIter,
            "checkpoint_interval" to config.checkPointInterval,
            "diff_based_evolution" to p["diff_based_evolution"].toBooleanStrictOrNullOrDefault(true),
            "llm" to
                mapOf(
                    "models" to listOf(mapOf("name" to config.modelName, "weight" to 1.0)),
                    "api_base" to p["llm.api_base"],
                    "api_key" to p["llm.api_key"],
                    "temperature" to p["llm.temperature"].toDoubleOrNullOrDefault(0.7),
                    "top_p" to p["llm.top_p"].toDoubleOrNullOrDefault(0.95),
                    "max_tokens" to p["llm.max_tokens"].toIntOrNullOrDefault(4096),
                    "timeout" to p["llm.timeout"].toIntOrNullOrDefault(60),
                    "retries" to p["llm.retries"].toIntOrNullOrDefault(3),
                ),
            "prompt" to
                mapOf(
                    "system_message" to
                        (p["prompt.system_message"]
                            ?: "You are an OpenEvolve assistant. Propose iterative improvements while preserving correctness."),
                    "num_top_programs" to p["prompt.num_top_programs"].toIntOrNullOrDefault(3),
                    "num_diverse_programs" to p["prompt.num_diverse_programs"].toIntOrNullOrDefault(2),
                    "include_artifacts" to p["prompt.include_artifacts"].toBooleanStrictOrNullOrDefault(true),
                ),
            "database" to
                mapOf(
                    "population_size" to p["database.population_size"].toIntOrNullOrDefault(100),
                    "archive_size" to p["database.archive_size"].toIntOrNullOrDefault(50),
                    "num_islands" to p["database.num_islands"].toIntOrNullOrDefault(4),
                    "migration_interval" to p["database.migration_interval"].toIntOrNullOrDefault(10),
                    "migration_rate" to p["database.migration_rate"].toDoubleOrNullOrDefault(0.1),
                    "elite_selection_ratio" to p["database.elite_selection_ratio"].toDoubleOrNullOrDefault(0.1),
                    "exploration_ratio" to p["database.exploration_ratio"].toDoubleOrNullOrDefault(0.2),
                    "exploitation_ratio" to p["database.exploitation_ratio"].toDoubleOrNullOrDefault(0.7),
                    "feature_dimensions" to featureDimensions,
                    "feature_bins" to p["database.feature_bins"].toIntOrNullOrDefault(10),
                ),
            "evaluator" to
                mapOf(
                    "timeout" to p["evaluator.timeout"].toIntOrNullOrDefault(300),
                    "max_retries" to p["evaluator.max_retries"].toIntOrNullOrDefault(3),
                    "parallel_evaluations" to p["evaluator.parallel_evaluations"].toIntOrNullOrDefault(4),
                    "cascade_evaluation" to p["evaluator.cascade_evaluation"].toBooleanStrictOrNullOrDefault(true),
                    "cascade_thresholds" to
                        listOf(
                            p["evaluator.cascade_threshold_1"].toDoubleOrNullOrDefault(0.5),
                            p["evaluator.cascade_threshold_2"].toDoubleOrNullOrDefault(0.75),
                            p["evaluator.cascade_threshold_3"].toDoubleOrNullOrDefault(0.9),
                        ),
                    "declared_custom_metrics" to declaredCustomMetrics,
                ),
        )
    }

    private fun String?.toIntOrNullOrDefault(default: Int): Int = this?.toIntOrNull() ?: default

    private fun String?.toDoubleOrNullOrDefault(default: Double): Double = this?.toDoubleOrNull() ?: default

    private fun String?.toBooleanStrictOrNullOrDefault(default: Boolean): Boolean =
        this?.toBooleanStrictOrNull() ?: default
}

