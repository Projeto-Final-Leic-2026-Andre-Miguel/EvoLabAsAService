package com.example.evolab.service.jobsService

import com.example.evolab.domain.evolution.EvolutionStatus
import com.example.evolab.domain.job.Job
import com.example.evolab.service.auxiliary.Either
import java.time.Instant

interface JobService {

    fun createJob(
        projectId: Int,
        status: EvolutionStatus = EvolutionStatus.CREATED,
        containerId: String? = null,
        startedAt: Instant? = null,
        finishedAt: Instant? = null,
        bestSolution: String? = null,
        executionLogs: String? = null,
        failureReason: String? = null,
    ): Either<JobServiceErrors, Int>

    fun createJobForUser(
        userId: Int,
        projectId: Int,
        status: EvolutionStatus = EvolutionStatus.CREATED,
        containerId: String? = null,
        startedAt: Instant? = null,
        finishedAt: Instant? = null,
        bestSolution: String? = null,
        executionLogs: String? = null,
        failureReason: String? = null,
    ): Either<JobServiceErrors, Int>

    fun getJobById(id: Int): Either<JobServiceErrors, Job>

    fun getJobById(id: Int, userId: Int): Either<JobServiceErrors, Job>

    fun getJobsByProjectId(projectId: Int): Either<JobServiceErrors, List<Job>>

    fun getJobsByProjectId(projectId: Int, userId: Int): Either<JobServiceErrors, List<Job>>

    fun getJobsByStatus(status: EvolutionStatus): Either<JobServiceErrors, List<Job>>

    fun getJobsByStatus(status: EvolutionStatus, userId: Int): Either<JobServiceErrors, List<Job>>

    fun getJobByContainerId(containerId: String): Either<JobServiceErrors, Job>

    fun getAllJobs(): Either<JobServiceErrors, List<Job>>

    fun getAllJobs(userId: Int): Either<JobServiceErrors, List<Job>>

    fun saveJob(job: Job): Either<JobServiceErrors, Job>

    fun deleteJob(id: Int): Either<JobServiceErrors, Int>

    fun deleteJob(id: Int, userId: Int): Either<JobServiceErrors, Int>
}
