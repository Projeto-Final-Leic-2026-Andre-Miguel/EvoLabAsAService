package com.example.evolab.service.checkpoints

import com.example.evolab.domain.checkpoint.Checkpoint
import com.example.evolab.domain.metrics.Metric
import com.example.evolab.repo.transactions.Transaction
import com.example.evolab.repo.transactions.TransactionManager
import com.example.evolab.service.auxiliary.Either
import com.example.evolab.service.auxiliary.failure
import com.example.evolab.service.auxiliary.success
import jakarta.inject.Named

@Named
class CheckpointServiceImp(
    private val trxManager: TransactionManager,
) : CheckpointService {
    override fun createCheckpoint(
        userId: Int,
        jobId: Int,
        metricsId: Int,
        iteration: Int,
        solution: String,
    ): Either<CheckpointServiceErrors, Checkpoint> =
        trxManager.run {
            try {
                validateIteration(iteration)?.let { return@run it }
                validateSolution(solution)?.let { return@run it }
                ensureOwnedJob(jobId, userId) ?: return@run failureJobNotFound(jobId)
                val metric = findMetric(metricsId) ?: return@run failureMetricNotFound(metricsId)
                validateMetricBelongsToJob(metric, jobId)?.let { return@run it }

                if (repoCheckpoints.findByJobIdAndIteration(jobId, iteration) != null) {
                    return@run failure(
                        CheckpointServiceErrors.DuplicateCheckpointForIteration(
                            "Job with id '$jobId' already has a checkpoint for iteration '$iteration'",
                        ),
                    )
                }

                val checkpointId =
                    repoCheckpoints.createCheckpoint(
                        jobId = jobId,
                        metricsId = metricsId,
                        iteration = iteration,
                        solution = solution,
                    )

                val createdCheckpoint =
                    repoCheckpoints.findById(checkpointId)
                        ?: return@run failure(
                            CheckpointServiceErrors.CheckpointNotFound(
                                "Checkpoint with id '$checkpointId' was not found after creation",
                            ),
                        )

                success(createdCheckpoint)
            } catch (e: CheckpointAccessDeniedException) {
                failure(
                    CheckpointServiceErrors.CheckpointAccessDenied(
                        "User with id '${e.userId}' cannot access checkpoints for job with id '${e.jobId}'",
                    ),
                )
            } catch (e: MissingProjectException) {
                failure(
                    CheckpointServiceErrors.ProjectNotFound(
                        "Project with id '${e.projectId}' was not found for job '${e.jobId}'",
                    ),
                )
            }
        }

    override fun getCheckpoint(
        checkpointId: Int,
        userId: Int,
    ): Either<CheckpointServiceErrors, Checkpoint> =
        trxManager.run {
            try {
                val checkpoint =
                    repoCheckpoints.findById(checkpointId)
                        ?: return@run failure(
                            CheckpointServiceErrors.CheckpointNotFound(
                                "Checkpoint with id '$checkpointId' was not found",
                            ),
                        )

                ensureOwnedJob(checkpoint.jobId, userId) ?: return@run failureJobNotFound(checkpoint.jobId)
                success(checkpoint)
            } catch (e: CheckpointAccessDeniedException) {
                failure(
                    CheckpointServiceErrors.CheckpointAccessDenied(
                        "User with id '${e.userId}' cannot access checkpoints for job with id '${e.jobId}'",
                    ),
                )
            } catch (e: MissingProjectException) {
                failure(
                    CheckpointServiceErrors.ProjectNotFound(
                        "Project with id '${e.projectId}' was not found for job '${e.jobId}'",
                    ),
                )
            }
        }

    override fun getCheckpointsByJobId(
        jobId: Int,
        userId: Int,
    ): Either<CheckpointServiceErrors, List<Checkpoint>> =
        trxManager.run {
            try {
                ensureOwnedJob(jobId, userId) ?: return@run failureJobNotFound(jobId)
                success(repoCheckpoints.findAllByJobId(jobId))
            } catch (e: CheckpointAccessDeniedException) {
                failure(
                    CheckpointServiceErrors.CheckpointAccessDenied(
                        "User with id '${e.userId}' cannot access checkpoints for job with id '${e.jobId}'",
                    ),
                )
            } catch (e: MissingProjectException) {
                failure(
                    CheckpointServiceErrors.ProjectNotFound(
                        "Project with id '${e.projectId}' was not found for job '${e.jobId}'",
                    ),
                )
            }
        }

    override fun getCheckpointsByMetricsId(
        metricsId: Int,
        userId: Int,
    ): Either<CheckpointServiceErrors, List<Checkpoint>> =
        trxManager.run {
            try {
                val metric = findMetric(metricsId) ?: return@run failureMetricNotFound(metricsId)
                ensureOwnedJob(metric.jobId, userId) ?: return@run failureJobNotFound(metric.jobId)
                success(repoCheckpoints.findAllByMetricsId(metricsId))
            } catch (e: CheckpointAccessDeniedException) {
                failure(
                    CheckpointServiceErrors.CheckpointAccessDenied(
                        "User with id '${e.userId}' cannot access checkpoints for job with id '${e.jobId}'",
                    ),
                )
            } catch (e: MissingProjectException) {
                failure(
                    CheckpointServiceErrors.ProjectNotFound(
                        "Project with id '${e.projectId}' was not found for job '${e.jobId}'",
                    ),
                )
            }
        }

    override fun getCheckpointByJobAndIteration(
        jobId: Int,
        iteration: Int,
        userId: Int,
    ): Either<CheckpointServiceErrors, Checkpoint> =
        trxManager.run {
            try {
                validateIteration(iteration)?.let { return@run it }
                ensureOwnedJob(jobId, userId) ?: return@run failureJobNotFound(jobId)

                val checkpoint =
                    repoCheckpoints.findByJobIdAndIteration(jobId, iteration)
                        ?: return@run failure(
                            CheckpointServiceErrors.CheckpointNotFound(
                                "Checkpoint for job with id '$jobId' and iteration '$iteration' was not found",
                            ),
                        )

                success(checkpoint)
            } catch (e: CheckpointAccessDeniedException) {
                failure(
                    CheckpointServiceErrors.CheckpointAccessDenied(
                        "User with id '${e.userId}' cannot access checkpoints for job with id '${e.jobId}'",
                    ),
                )
            } catch (e: MissingProjectException) {
                failure(
                    CheckpointServiceErrors.ProjectNotFound(
                        "Project with id '${e.projectId}' was not found for job '${e.jobId}'",
                    ),
                )
            }
        }

    override fun getAllCheckpoints(userId: Int): Either<CheckpointServiceErrors, List<Checkpoint>> =
        trxManager.run {
            val ownedProjectIds = repoProjects.findAllByUserId(userId).map { it.id }.toSet()
            val ownedJobIds = repoJobs.findAll().filter { it.projectId in ownedProjectIds }.map { it.id }.toSet()
            success(repoCheckpoints.findAll().filter { it.jobId in ownedJobIds })
        }

    override fun deleteCheckpoint(checkpointId: Int, userId: Int): Either<CheckpointServiceErrors, Int> =
        trxManager.run {
            try {
                val checkpoint = repoCheckpoints.findById(checkpointId)
                    ?: return@run failure(CheckpointServiceErrors.CheckpointNotFound("Checkpoint with id '$checkpointId' was not found"))

                ensureOwnedJob(checkpoint.jobId, userId) ?: return@run failureJobNotFound(checkpoint.jobId)
            } catch (e: CheckpointAccessDeniedException) {
                return@run failure(
                    CheckpointServiceErrors.CheckpointAccessDenied(
                        "User with id '${e.userId}' cannot access checkpoints for job with id '${e.jobId}'",
                    ),
                )
            } catch (e: MissingProjectException) {
                return@run failure(
                    CheckpointServiceErrors.ProjectNotFound(
                        "Project with id '${e.projectId}' was not found for job '${e.jobId}'",
                    ),
                )
            }

            val deleted = repoCheckpoints.deleteById(checkpointId)
            if (!deleted) return@run failure(CheckpointServiceErrors.CheckpointNotFound("Checkpoint with id '$checkpointId' was not found"))
            success(checkpointId)
        }

    private fun Transaction.ensureOwnedJob(
        jobId: Int,
        userId: Int,
    ): Int? {
        val job = repoJobs.findById(jobId) ?: return null
        val project =
            repoProjects.findById(job.projectId)
                ?: throw MissingProjectException(jobId, job.projectId)

        if (project.userId != userId) {
            throw CheckpointAccessDeniedException(jobId, userId)
        }

        return job.id
    }

    private fun Transaction.findMetric(metricsId: Int): Metric? = repoMetrics.findById(metricsId)

    private fun validateMetricBelongsToJob(
        metric: Metric,
        jobId: Int,
    ): Either<CheckpointServiceErrors, Nothing>? =
        if (metric.jobId != jobId) {
            failure(
                CheckpointServiceErrors.MetricDoesNotBelongToJob(
                    "Metric with id '${metric.id}' does not belong to job with id '$jobId'",
                ),
            )
        } else {
            null
        }

    private fun validateIteration(iteration: Int): Either<CheckpointServiceErrors, Nothing>? =
        if (iteration <= 0) {
            failure(CheckpointServiceErrors.InvalidCheckpointInput("Checkpoint iteration must be greater than 0"))
        } else {
            null
        }

    private fun validateSolution(solution: String): Either<CheckpointServiceErrors, Nothing>? =
        if (solution.isBlank()) {
            failure(CheckpointServiceErrors.InvalidCheckpointInput("Checkpoint solution cannot be blank"))
        } else {
            null
        }

    private fun failureJobNotFound(jobId: Int) =
        failure(CheckpointServiceErrors.JobNotFound("Job with id '$jobId' was not found"))

    private fun failureMetricNotFound(metricsId: Int) =
        failure(CheckpointServiceErrors.MetricNotFound("Metric with id '$metricsId' was not found"))

    private class CheckpointAccessDeniedException(
        val jobId: Int,
        val userId: Int,
    ) : RuntimeException()

    private class MissingProjectException(
        val jobId: Int,
        val projectId: Int,
    ) : RuntimeException()
}
