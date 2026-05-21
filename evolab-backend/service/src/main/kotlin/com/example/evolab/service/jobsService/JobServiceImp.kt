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
            registerTerminalOutcomeIfNeeded(projectId, previousStatus = null, newStatus = status)
            success(id)
        }

    override fun createJobForUser(
        userId: Int,
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
            ensureOwnedProject(projectId, userId)?.let { return@run it }

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
            registerTerminalOutcomeIfNeeded(projectId, previousStatus = null, newStatus = status)
            success(id)
        }

    override fun getJobById(id: Int): Either<JobServiceErrors, Job> =
        trxManager.run {
            val job = repoJobs.findById(id)
                ?: return@run failure(JobServiceErrors.JobNotFound("Job with id '$id' was not found"))
            success(job)
        }

    override fun getJobById(id: Int, userId: Int): Either<JobServiceErrors, Job> =
        trxManager.run {
            val job = repoJobs.findById(id)
                ?: return@run failure(JobServiceErrors.JobNotFound("Job with id '$id' was not found"))
            ensureOwnedProject(job.projectId, userId)?.let { return@run it }
            success(job)
        }

    override fun getJobsByProjectId(projectId: Int): Either<JobServiceErrors, List<Job>> =
        trxManager.run {
            success(repoJobs.findAllByProjectId(projectId))
        }

    override fun getJobsByProjectId(projectId: Int, userId: Int): Either<JobServiceErrors, List<Job>> =
        trxManager.run {
            ensureOwnedProject(projectId, userId)?.let { return@run it }
            success(repoJobs.findAllByProjectId(projectId))
        }

    override fun getJobsByStatus(status: EvolutionStatus): Either<JobServiceErrors, List<Job>> =
        trxManager.run {
            success(repoJobs.findAllByStatus(status))
        }

    override fun getJobsByStatus(status: EvolutionStatus, userId: Int): Either<JobServiceErrors, List<Job>> =
        trxManager.run {
            val ownedProjectIds = repoProjects.findAllByUserId(userId).map { it.id }.toSet()
            success(repoJobs.findAllByStatus(status).filter { it.projectId in ownedProjectIds })
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

    override fun getAllJobs(userId: Int): Either<JobServiceErrors, List<Job>> =
        trxManager.run {
            val ownedProjectIds = repoProjects.findAllByUserId(userId).map { it.id }.toSet()
            success(repoJobs.findAll().filter { it.projectId in ownedProjectIds })
        }

    override fun saveJob(job: Job): Either<JobServiceErrors, Job> =
        trxManager.run {
            val previousStatus = repoJobs.findById(job.id)?.status
            repoJobs.save(job)
            registerTerminalOutcomeIfNeeded(job.projectId, previousStatus, job.status)
            success(job)
        }

    override fun deleteJob(id: Int): Either<JobServiceErrors, Int> =
        trxManager.run {
            val deleted = repoJobs.deleteById(id)
            if (!deleted) return@run failure(JobServiceErrors.JobNotFound("Job with id '$id' was not found"))
            success(id)
        }

    override fun deleteJob(id: Int, userId: Int): Either<JobServiceErrors, Int> =
        trxManager.run {
            val job = repoJobs.findById(id)
                ?: return@run failure(JobServiceErrors.JobNotFound("Job with id '$id' was not found"))
            ensureOwnedProject(job.projectId, userId)?.let { return@run it }
            val deleted = repoJobs.deleteById(id)
            if (!deleted) return@run failure(JobServiceErrors.JobNotFound("Job with id '$id' was not found"))
            success(id)
        }

    private fun com.example.evolab.repo.transactions.Transaction.ensureOwnedProject(
        projectId: Int,
        userId: Int,
    ): Either<JobServiceErrors, Nothing>? {
        val project = repoProjects.findById(projectId)
            ?: return failure(JobServiceErrors.InvalidJobInput("Project with id '$projectId' was not found"))

        if (project.userId != userId) {
            return failure(JobServiceErrors.JobAccessDenied("User with id '$userId' cannot access jobs for project with id '$projectId'"))
        }

        return null
    }

    private fun com.example.evolab.repo.transactions.Transaction.registerTerminalOutcomeIfNeeded(
        projectId: Int,
        previousStatus: EvolutionStatus?,
        newStatus: EvolutionStatus,
    ) {
        if (newStatus != EvolutionStatus.COMPLETED && newStatus != EvolutionStatus.FAILED) return
        if (previousStatus == EvolutionStatus.COMPLETED || previousStatus == EvolutionStatus.FAILED) return

        val project = repoProjects.findById(projectId) ?: return
        repoStatistics.incrementProjectOutcome(project.userId, newStatus)
    }
}
