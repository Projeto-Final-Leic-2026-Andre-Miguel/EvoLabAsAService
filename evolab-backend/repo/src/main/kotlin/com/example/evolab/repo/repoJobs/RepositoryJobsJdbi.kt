package com.example.evolab.repo.repoJobs

import com.example.evolab.domain.evolution.EvolutionStatus
import com.example.evolab.domain.job.Job
import org.jdbi.v3.core.Handle
import java.time.Instant

class RepositoryJobsJdbi(
    private val handle: Handle,
) : RepositoryJobs {

    override fun createJob(
        projectId: Int,
        status: EvolutionStatus,
        containerId: String?,
        startedAt: Instant?,
        finishedAt: Instant?,
        bestSolution: String?,
        executionLogs: String?,
        failureReason: String?,
    ): Int =
        handle
            .createQuery(JobsSql.CREATE_JOB)
            .bind("projectId", projectId)
            .bind("status", status.name)
            .bind("containerId", containerId)
            .bind("startedAt", startedAt)
            .bind("finishedAt", finishedAt)
            .bind("bestSolution", bestSolution)
            .bind("executionLogs", executionLogs)
            .bind("failureReason", failureReason)
            .mapTo(Int::class.java)
            .one()

    override fun findAllByProjectId(projectId: Int): List<Job> =
        handle
            .createQuery(JobsSql.FIND_ALL_BY_PROJECT_ID)
            .bind("projectId", projectId)
            .map { rs, _ -> rs.toJob() }
            .list()

    override fun findAllByStatus(status: EvolutionStatus): List<Job> =
        handle
            .createQuery(JobsSql.FIND_ALL_BY_STATUS)
            .bind("status", status.name)
            .map { rs, _ -> rs.toJob() }
            .list()

    override fun findByContainerId(containerId: String): Job? =
        handle
            .createQuery(JobsSql.FIND_BY_CONTAINER_ID)
            .bind("containerId", containerId)
            .map { rs, _ -> rs.toJob() }
            .findOne()
            .orElse(null)

    override fun findById(id: Int): Job? =
        handle
            .createQuery(JobsSql.FIND_BY_ID)
            .bind("id", id)
            .map { rs, _ -> rs.toJob() }
            .findOne()
            .orElse(null)

    override fun findAll(): List<Job> =
        handle
            .createQuery(JobsSql.FIND_ALL)
            .map { rs, _ -> rs.toJob() }
            .list()

    override fun save(entity: Job) {
        handle
            .createUpdate(JobsSql.SAVE)
            .bind("id", entity.id)
            .bind("projectId", entity.projectId)
            .bind("status", entity.status.name)
            .bind("containerId", entity.containerId)
            .bind("startedAt", entity.startedAt)
            .bind("finishedAt", entity.finishedAt)
            .bind("bestSolution", entity.bestSolution)
            .bind("executionLogs", entity.executionLogs)
            .bind("failureReason", entity.failureReason)
            .execute()
    }

    override fun deleteById(id: Int): Boolean =
        handle
            .createUpdate(JobsSql.DELETE_BY_ID)
            .bind("id", id)
            .execute() > 0

    override fun clear() {
        handle
            .createUpdate(JobsSql.CLEAR)
            .execute()
    }

    private fun java.sql.ResultSet.toJob(): Job =
        Job(
            id = getInt("id"),
            projectId = getInt("projectId"),
            status = EvolutionStatus.valueOf(getString("status")),
            containerId = getString("containerId"),
            startedAt = getTimestamp("startedAt")?.toInstant(),
            finishedAt = getTimestamp("finishedAt")?.toInstant(),
            bestSolution = getString("bestSolution"),
            executionLogs = getString("executionLogs"),
            failureReason = getString("failureReason"),
            createdAt = getTimestamp("createdAt").toInstant(),
        )
}


