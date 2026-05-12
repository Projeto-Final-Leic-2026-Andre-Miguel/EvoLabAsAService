package com.example.evolab.http.controllers

import com.example.evolab.domain.evolution.EvolutionStatus
import com.example.evolab.domain.job.Job
import com.example.evolab.http.model.job.CreateJobInput
import com.example.evolab.http.model.problem.Problem
import com.example.evolab.service.auxiliary.Either
import com.example.evolab.service.auxiliary.Failure
import com.example.evolab.service.auxiliary.Success
import com.example.evolab.service.jobsService.JobService
import com.example.evolab.service.jobsService.JobServiceErrors
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class JobController(
    private val jobService: JobService,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(JobController::class.java)
    }
    @PostMapping("/api/jobs")
    fun createJob(
        @RequestBody input: CreateJobInput,
    ): ResponseEntity<*> =
        try {
            val result = jobService.createJob(
                projectId = input.projectId,
                status = input.status,
            )

            when (result) {
                is Success ->
                    ResponseEntity
                        .status(HttpStatus.CREATED)
                        .header("Location", "/api/jobs/${result.value}")
                        .body(mapOf("id" to result.value))

                is Failure -> mapServiceErrors(result.value)
            }
        } catch (e: Exception) {
            logger.error("Unexpected error in JobController", e)
            Problem.UnknownError.response(HttpStatus.INTERNAL_SERVER_ERROR)
        }

    @GetMapping("/api/jobs/{id}")
    fun getJobById(
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        val result: Either<JobServiceErrors, Job> = jobService.getJobById(id)

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Failure -> mapServiceErrors(result.value)
        }
    }

    @GetMapping("/api/projects/{projectId}/jobs")
    fun getJobsByProjectId(
        @PathVariable projectId: Int,
    ): ResponseEntity<*> {
        val result: Either<JobServiceErrors, List<Job>> = jobService.getJobsByProjectId(projectId)

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Failure -> mapServiceErrors(result.value)
        }
    }

    @GetMapping("/api/jobs")
    fun getAllJobs(
        @RequestParam(required = false) status: String?,
    ): ResponseEntity<*> {
        val result: Either<JobServiceErrors, List<Job>> =
            if (status != null) {
                val evolutionStatus = runCatching { EvolutionStatus.valueOf(status.uppercase()) }.getOrNull()
                    ?: return Problem.InvalidJobInput.response(HttpStatus.BAD_REQUEST)
                jobService.getJobsByStatus(evolutionStatus)
            } else {
                jobService.getAllJobs()
            }

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Failure -> mapServiceErrors(result.value)
        }
    }

    @DeleteMapping("/api/jobs/{id}")
    fun deleteJob(
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        val result: Either<JobServiceErrors, Int> = jobService.deleteJob(id)

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.NO_CONTENT).build<Unit>()
            is Failure -> mapServiceErrors(result.value)
        }
    }

    private fun mapServiceErrors(error: JobServiceErrors): ResponseEntity<*> =
        when (error) {
            is JobServiceErrors.JobNotFound -> Problem.JobNotFound.response(HttpStatus.NOT_FOUND)
            is JobServiceErrors.InvalidJobInput -> Problem.InvalidJobInput.response(HttpStatus.BAD_REQUEST)
        }
}
