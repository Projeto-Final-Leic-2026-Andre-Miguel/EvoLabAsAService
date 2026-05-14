package com.example.evolab.service.metrics

import com.example.evolab.domain.metrics.Metric
import com.example.evolab.repo.transactions.Transaction
import com.example.evolab.repo.transactions.TransactionManager
import com.example.evolab.service.auxiliary.Either
import com.example.evolab.service.auxiliary.failure
import com.example.evolab.service.auxiliary.success
import jakarta.inject.Named

@Named
class MetricServiceImp(
    private val trxManager: TransactionManager,
) : MetricService {
    override fun createMetric(
        userId: Int,
        jobId: Int,
        iteration: Int,
        fitnessScore: Double,
        executionTime: Double?,
    ): Either<MetricServiceErrors, Metric> =
        trxManager.run {
            try {
                validateIteration(iteration)?.let { return@run it }
                validateExecutionTime(executionTime)?.let { return@run it }
                ensureOwnedJob(jobId, userId) ?: return@run failureJobNotFound(jobId)

                if (repoMetrics.findByJobIdAndIteration(jobId, iteration) != null) {
                    return@run failure(
                        MetricServiceErrors.DuplicateMetricForIteration(
                            "Job with id '$jobId' already has a metric for iteration '$iteration'",
                        ),
                    )
                }

                val metricId =
                    repoMetrics.createMetric(
                        jobId = jobId,
                        iteration = iteration,
                        fitnessScore = fitnessScore,
                        executionTime = executionTime,
                    )

                val createdMetric =
                    repoMetrics.findById(metricId)
                        ?: return@run failure(
                            MetricServiceErrors.MetricNotFound(
                                "Metric with id '$metricId' was not found after creation",
                            ),
                        )

                success(createdMetric)
            } catch (e: MetricAccessDeniedException) {
                failure(
                    MetricServiceErrors.MetricAccessDenied(
                        "User with id '${e.userId}' cannot access metrics for job with id '${e.jobId}'",
                    ),
                )
            } catch (e: MissingProjectException) {
                failure(
                    MetricServiceErrors.ProjectNotFound(
                        "Project with id '${e.projectId}' was not found for job '${e.jobId}'",
                    ),
                )
            }
        }

    override fun getMetric(
        metricId: Int,
        userId: Int,
    ): Either<MetricServiceErrors, Metric> =
        trxManager.run {
            try {
                val metric =
                    repoMetrics.findById(metricId)
                        ?: return@run failure(
                            MetricServiceErrors.MetricNotFound("Metric with id '$metricId' was not found"),
                        )

                ensureOwnedJob(metric.jobId, userId) ?: return@run failureJobNotFound(metric.jobId)
                success(metric)
            } catch (e: MetricAccessDeniedException) {
                failure(
                    MetricServiceErrors.MetricAccessDenied(
                        "User with id '${e.userId}' cannot access metrics for job with id '${e.jobId}'",
                    ),
                )
            } catch (e: MissingProjectException) {
                failure(
                    MetricServiceErrors.ProjectNotFound(
                        "Project with id '${e.projectId}' was not found for job '${e.jobId}'",
                    ),
                )
            }
        }

    override fun getMetricsByJobId(
        jobId: Int,
        userId: Int,
    ): Either<MetricServiceErrors, List<Metric>> =
        trxManager.run {
            try {
                ensureOwnedJob(jobId, userId) ?: return@run failureJobNotFound(jobId)
                success(repoMetrics.findAllByJobId(jobId))
            } catch (e: MetricAccessDeniedException) {
                failure(
                    MetricServiceErrors.MetricAccessDenied(
                        "User with id '${e.userId}' cannot access metrics for job with id '${e.jobId}'",
                    ),
                )
            } catch (e: MissingProjectException) {
                failure(
                    MetricServiceErrors.ProjectNotFound(
                        "Project with id '${e.projectId}' was not found for job '${e.jobId}'",
                    ),
                )
            }
        }

    override fun getMetricByJobAndIteration(
        jobId: Int,
        iteration: Int,
        userId: Int,
    ): Either<MetricServiceErrors, Metric> =
        trxManager.run {
            try {
                validateIteration(iteration)?.let { return@run it }
                ensureOwnedJob(jobId, userId) ?: return@run failureJobNotFound(jobId)

                val metric =
                    repoMetrics.findByJobIdAndIteration(jobId, iteration)
                        ?: return@run failure(
                            MetricServiceErrors.MetricNotFound(
                                "Metric for job with id '$jobId' and iteration '$iteration' was not found",
                            ),
                        )

                success(metric)
            } catch (e: MetricAccessDeniedException) {
                failure(
                    MetricServiceErrors.MetricAccessDenied(
                        "User with id '${e.userId}' cannot access metrics for job with id '${e.jobId}'",
                    ),
                )
            } catch (e: MissingProjectException) {
                failure(
                    MetricServiceErrors.ProjectNotFound(
                        "Project with id '${e.projectId}' was not found for job '${e.jobId}'",
                    ),
                )
            }
        }

    override fun getAllMetrics(userId: Int): Either<MetricServiceErrors, List<Metric>> =
        trxManager.run {
            val ownedProjectIds = repoProjects.findAllByUserId(userId).map { it.id }.toSet()
            val ownedJobIds = repoJobs.findAll().filter { it.projectId in ownedProjectIds }.map { it.id }.toSet()
            success(repoMetrics.findAll().filter { it.jobId in ownedJobIds })
        }

    override fun deleteMetric(metricId: Int, userId: Int): Either<MetricServiceErrors, Int> =
        trxManager.run {
            try {
                val metric = repoMetrics.findById(metricId)
                    ?: return@run failure(MetricServiceErrors.MetricNotFound("Metric with id '$metricId' was not found"))

                ensureOwnedJob(metric.jobId, userId) ?: return@run failureJobNotFound(metric.jobId)
            } catch (e: MetricAccessDeniedException) {
                return@run failure(
                    MetricServiceErrors.MetricAccessDenied(
                        "User with id '${e.userId}' cannot access metrics for job with id '${e.jobId}'",
                    ),
                )
            } catch (e: MissingProjectException) {
                return@run failure(
                    MetricServiceErrors.ProjectNotFound(
                        "Project with id '${e.projectId}' was not found for job '${e.jobId}'",
                    ),
                )
            }

            val deleted = repoMetrics.deleteById(metricId)
            if (!deleted) return@run failure(MetricServiceErrors.MetricNotFound("Metric with id '$metricId' was not found"))
            success(metricId)
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
            throw MetricAccessDeniedException(jobId, userId)
        }

        return job.id
    }

    private fun validateIteration(iteration: Int): Either<MetricServiceErrors, Nothing>? =
        if (iteration <= 0) {
            failure(MetricServiceErrors.InvalidMetricInput("Metric iteration must be greater than 0"))
        } else {
            null
        }

    private fun validateExecutionTime(executionTime: Double?): Either<MetricServiceErrors, Nothing>? =
        if (executionTime != null && executionTime < 0) {
            failure(MetricServiceErrors.InvalidMetricInput("Metric executionTime cannot be negative"))
        } else {
            null
        }

    private fun failureJobNotFound(jobId: Int) =
        failure(MetricServiceErrors.JobNotFound("Job with id '$jobId' was not found"))

    private class MetricAccessDeniedException(
        val jobId: Int,
        val userId: Int,
    ) : RuntimeException()

    private class MissingProjectException(
        val jobId: Int,
        val projectId: Int,
    ) : RuntimeException()
}
