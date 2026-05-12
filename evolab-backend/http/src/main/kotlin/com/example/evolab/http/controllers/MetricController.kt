package com.example.evolab.http.controllers

import com.example.evolab.domain.metrics.Metric
import com.example.evolab.domain.user.AuthenticatedUser
import com.example.evolab.http.model.metric.CreateMetricInput
import com.example.evolab.http.model.problem.Problem
import com.example.evolab.service.auxiliary.Either
import com.example.evolab.service.auxiliary.Failure
import com.example.evolab.service.auxiliary.Success
import com.example.evolab.service.metrics.MetricService
import com.example.evolab.service.metrics.MetricServiceErrors
import org.slf4j.LoggerFactory
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
class MetricController(
    private val metricService: MetricService,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(MetricController::class.java)
    }
    @PostMapping("/api/jobs/{jobId}/metrics")
    fun createMetric(
        @PathVariable jobId: Int,
        @RequestBody input: CreateMetricInput,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> =
        try {
            val result: Either<MetricServiceErrors, Metric> =
                metricService.createMetric(
                    userId = authenticatedUser.user.id,
                    jobId = jobId,
                    iteration = input.iteration,
                    fitnessScore = input.fitnessScore,
                    executionTime = input.executionTime,
                )

            when (result) {
                is Success ->
                    ResponseEntity
                        .status(HttpStatus.CREATED)
                        .header("Location", "/api/metrics/${result.value.id}")
                        .body(result.value)

                is Failure -> mapServiceErrors(result.value)
            }
        } catch (e: Exception) {
            logger.error("Unexpected error in MetricController", e)
            Problem.UnknownError.response(HttpStatus.INTERNAL_SERVER_ERROR)
        }

    @GetMapping("/api/jobs/{jobId}/metrics")
    fun getMetricsByJobId(
        @PathVariable jobId: Int,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val result =
            metricService.getMetricsByJobId(
                jobId = jobId,
                userId = authenticatedUser.user.id,
            )

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Failure -> mapServiceErrors(result.value)
        }
    }

    @GetMapping("/api/jobs/{jobId}/metrics/{iteration}")
    fun getMetricByJobAndIteration(
        @PathVariable jobId: Int,
        @PathVariable iteration: Int,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val result: Either<MetricServiceErrors, Metric> =
            metricService.getMetricByJobAndIteration(
                jobId = jobId,
                iteration = iteration,
                userId = authenticatedUser.user.id,
            )

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Failure -> mapServiceErrors(result.value)
        }
    }

    @GetMapping("/api/metrics/{id}")
    fun getMetric(
        @PathVariable id: Int,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val result: Either<MetricServiceErrors, Metric> =
            metricService.getMetric(
                metricId = id,
                userId = authenticatedUser.user.id,
            )

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Failure -> mapServiceErrors(result.value)
        }
    }

    @GetMapping("/api/metrics")
    fun getAllMetrics(): ResponseEntity<*> {
        val result = metricService.getAllMetrics()
        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Failure -> mapServiceErrors(result.value)
        }
    }

    @DeleteMapping("/api/metrics/{id}")
    fun deleteMetric(
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        val result = metricService.deleteMetric(id)
        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.NO_CONTENT).build<Unit>()
            is Failure -> mapServiceErrors(result.value)
        }
    }

    private fun mapServiceErrors(error: MetricServiceErrors): ResponseEntity<*> =
        when (error) {
            is MetricServiceErrors.MetricNotFound -> Problem.MetricNotFound.response(HttpStatus.NOT_FOUND)
            is MetricServiceErrors.JobNotFound -> Problem.JobNotFound.response(HttpStatus.NOT_FOUND)
            is MetricServiceErrors.ProjectNotFound -> Problem.ProjectNotFound.response(HttpStatus.NOT_FOUND)
            is MetricServiceErrors.MetricAccessDenied -> Problem.MetricAccessDenied.response(HttpStatus.FORBIDDEN)
            is MetricServiceErrors.InvalidMetricInput -> Problem.InvalidMetricInput.response(HttpStatus.BAD_REQUEST)
            is MetricServiceErrors.DuplicateMetricForIteration ->
                Problem.DuplicateMetricForIteration.response(HttpStatus.CONFLICT)
        }
}
