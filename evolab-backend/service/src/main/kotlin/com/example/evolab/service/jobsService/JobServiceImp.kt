package com.example.evolab.service.jobsService

import com.example.evolab.domain.evolution.EvolutionStatus
import com.example.evolab.domain.job.Job
import com.example.evolab.repo.transactions.TransactionManager
import com.example.evolab.service.auxiliary.Either
import com.example.evolab.service.auxiliary.failure
import com.example.evolab.service.auxiliary.success
import jakarta.inject.Named
import java.time.Instant

@Named
class JobServiceImp(
    private val trxManager: TransactionManager,
) : JobService {

    override fun createJob(
        projectId: Int,
        status: EvolutionStatus,
        containerId: String?,
        startedAt: Instant?,
        finishedAt: Instant?,
        bestSolution: String?,
        executionLogs: String?,
        failureReason: String?,
    ): Either<JobServiceErrors, Int> =
        trxManager.run {
            val id = repoJobs.createJob(
                projectId = projectId,
                status = status,
                containerId = containerId,
                startedAt = startedAt,
                finishedAt = finishedAt,
                bestSolution = bestSolution,
                executionLogs = executionLogs,
                failureReason = failureReason,
            )
            success(id)
        }

    override fun getJobById(id: Int): Either<JobServiceErrors, Job> =
        trxManager.run {
            val job = repoJobs.findById(id)
                ?: return@run failure(JobServiceErrors.JobNotFound("Job with id '$id' was not found"))
            success(job)
        }

    override fun getJobsByProjectId(projectId: Int): Either<JobServiceErrors, List<Job>> =
        trxManager.run {
            success(repoJobs.findAllByProjectId(projectId))
        }

    override fun getJobsByStatus(status: EvolutionStatus): Either<JobServiceErrors, List<Job>> =
        trxManager.run {
            success(repoJobs.findAllByStatus(status))
        }

    override fun getJobByContainerId(containerId: String): Either<JobServiceErrors, Job> =
        trxManager.run {
            val job = repoJobs.findByContainerId(containerId)
                ?: return@run failure(JobServiceErrors.JobNotFound("Job with containerId '$containerId' was not found"))
            success(job)
        }

    override fun getAllJobs(): Either<JobServiceErrors, List<Job>> =
        trxManager.run {
            success(repoJobs.findAll())
        }

    override fun saveJob(job: Job): Either<JobServiceErrors, Job> =
        trxManager.run {
            repoJobs.save(job)
            success(job)
        }

    override fun deleteJob(id: Int): Either<JobServiceErrors, Int> =
        trxManager.run {
            val deleted = repoJobs.deleteById(id)
            if (!deleted) return@run failure(JobServiceErrors.JobNotFound("Job with id '$id' was not found"))
            success(id)
        }
}
