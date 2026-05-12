package com.example.evolab.repo.repoJobs

import com.example.evolab.domain.evolution.EvolutionStatus
import com.example.evolab.domain.job.Job
import com.example.evolab.repo.Repository
import java.time.Instant

interface RepositoryJobs : Repository<Job> {
    fun createJob(
        projectId: Int,
        status: EvolutionStatus = EvolutionStatus.CREATED,
        containerId: String? = null,
        startedAt: Instant? = null,
        finishedAt: Instant? = null,
        bestSolution: String? = null,
        executionLogs: String? = null,
        failureReason: String? = null,
    ): Int

    fun findAllByProjectId(projectId: Int): List<Job>

    fun findAllByStatus(status: EvolutionStatus): List<Job>

    fun findByContainerId(containerId: String): Job?
}

