package com.example.evolab.http.controllers

import com.example.evolab.domain.checkpoint.Checkpoint
import com.example.evolab.domain.user.AuthenticatedUser
import com.example.evolab.http.model.checkpoint.CreateCheckpointInput
import com.example.evolab.http.model.problem.Problem
import com.example.evolab.service.auxiliary.Either
import com.example.evolab.service.auxiliary.Failure
import com.example.evolab.service.auxiliary.Success
import com.example.evolab.service.checkpoints.CheckpointService
import com.example.evolab.service.checkpoints.CheckpointServiceErrors
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class CheckpointController(
    private val checkpointService: CheckpointService,
) {
    @PostMapping("/api/jobs/{jobId}/checkpoints")
    fun createCheckpoint(
        @PathVariable jobId: Int,
        @RequestBody input: CreateCheckpointInput,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> =
        try {
            val result: Either<CheckpointServiceErrors, Checkpoint> =
                checkpointService.createCheckpoint(
                    userId = authenticatedUser.user.id,
                    jobId = jobId,
                    metricsId = input.metricsId,
                    iteration = input.iteration,
                    solution = input.solution,
                )

            when (result) {
                is Success ->
                    ResponseEntity
                        .status(HttpStatus.CREATED)
                        .header("Location", "/api/checkpoints/${result.value.id}")
                        .body(result.value)

                is Failure -> mapServiceErrors(result.value)
            }
        } catch (_: Exception) {
            Problem.UnknownError.response(HttpStatus.INTERNAL_SERVER_ERROR)
        }

    @GetMapping("/api/jobs/{jobId}/checkpoints")
    fun getCheckpointsByJobId(
        @PathVariable jobId: Int,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val result =
            checkpointService.getCheckpointsByJobId(
                jobId = jobId,
                userId = authenticatedUser.user.id,
            )

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Failure -> mapServiceErrors(result.value)
        }
    }

    @GetMapping("/api/jobs/{jobId}/checkpoints/{iteration}")
    fun getCheckpointByJobAndIteration(
        @PathVariable jobId: Int,
        @PathVariable iteration: Int,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val result: Either<CheckpointServiceErrors, Checkpoint> =
            checkpointService.getCheckpointByJobAndIteration(
                jobId = jobId,
                iteration = iteration,
                userId = authenticatedUser.user.id,
            )

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Failure -> mapServiceErrors(result.value)
        }
    }

    @GetMapping("/api/checkpoints/{id}")
    fun getCheckpoint(
        @PathVariable id: Int,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val result: Either<CheckpointServiceErrors, Checkpoint> =
            checkpointService.getCheckpoint(
                checkpointId = id,
                userId = authenticatedUser.user.id,
            )

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Failure -> mapServiceErrors(result.value)
        }
    }

    @GetMapping("/api/metrics/{metricsId}/checkpoints")
    fun getCheckpointsByMetricsId(
        @PathVariable metricsId: Int,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val result =
            checkpointService.getCheckpointsByMetricsId(
                metricsId = metricsId,
                userId = authenticatedUser.user.id,
            )

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Failure -> mapServiceErrors(result.value)
        }
    }

    @GetMapping("/api/checkpoints")
    fun getAllCheckpoints(): ResponseEntity<*> {
        val result = checkpointService.getAllCheckpoints()
        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Failure -> mapServiceErrors(result.value)
        }
    }

    @DeleteMapping("/api/checkpoints/{id}")
    fun deleteCheckpoint(
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        val result = checkpointService.deleteCheckpoint(id)
        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.NO_CONTENT).build<Unit>()
            is Failure -> mapServiceErrors(result.value)
        }
    }

    private fun mapServiceErrors(error: CheckpointServiceErrors): ResponseEntity<*> =
        when (error) {
            is CheckpointServiceErrors.CheckpointNotFound -> Problem.CheckpointNotFound.response(HttpStatus.NOT_FOUND)
            is CheckpointServiceErrors.JobNotFound -> Problem.JobNotFound.response(HttpStatus.NOT_FOUND)
            is CheckpointServiceErrors.MetricNotFound -> Problem.MetricNotFound.response(HttpStatus.NOT_FOUND)
            is CheckpointServiceErrors.ProjectNotFound -> Problem.ProjectNotFound.response(HttpStatus.NOT_FOUND)
            is CheckpointServiceErrors.CheckpointAccessDenied -> Problem.CheckpointAccessDenied.response(HttpStatus.FORBIDDEN)
            is CheckpointServiceErrors.InvalidCheckpointInput ->
                Problem.InvalidCheckpointInput.response(HttpStatus.BAD_REQUEST)
            is CheckpointServiceErrors.DuplicateCheckpointForIteration ->
                Problem.DuplicateCheckpointForIteration.response(HttpStatus.CONFLICT)
            is CheckpointServiceErrors.MetricDoesNotBelongToJob ->
                Problem.MetricDoesNotBelongToJob.response(HttpStatus.CONFLICT)
        }
}
