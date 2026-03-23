package com.example.evolab.repo.repoCheckpoints

import com.example.evolab.domain.checkpoint.Checkpoint
import org.jdbi.v3.core.Handle

class RepositoryCheckpointsJdbi(
    private val handle: Handle,
) : RepositoryCheckpoints {

    override fun createCheckpoint(
        jobId: Int,
        metricsId: Int,
        iteration: Int,
        solution: String,
    ): Int =
        handle
            .createQuery(CheckpointsSql.CREATE_CHECKPOINT)
            .bind("jobId", jobId)
            .bind("metricsId", metricsId)
            .bind("iteration", iteration)
            .bind("solution", solution)
            .mapTo(Int::class.java)
            .one()

    override fun findAllByJobId(jobId: Int): List<Checkpoint> =
        handle
            .createQuery(CheckpointsSql.FIND_ALL_BY_JOB_ID)
            .bind("jobId", jobId)
            .map { rs, _ -> rs.toCheckpoint() }
            .list()

    override fun findAllByMetricsId(metricsId: Int): List<Checkpoint> =
        handle
            .createQuery(CheckpointsSql.FIND_ALL_BY_METRICS_ID)
            .bind("metricsId", metricsId)
            .map { rs, _ -> rs.toCheckpoint() }
            .list()

    override fun findByJobIdAndIteration(jobId: Int, iteration: Int): Checkpoint? =
        handle
            .createQuery(CheckpointsSql.FIND_BY_JOB_ID_AND_ITERATION)
            .bind("jobId", jobId)
            .bind("iteration", iteration)
            .map { rs, _ -> rs.toCheckpoint() }
            .findOne()
            .orElse(null)

    override fun findById(id: Int): Checkpoint? =
        handle
            .createQuery(CheckpointsSql.FIND_BY_ID)
            .bind("id", id)
            .map { rs, _ -> rs.toCheckpoint() }
            .findOne()
            .orElse(null)

    override fun findAll(): List<Checkpoint> =
        handle
            .createQuery(CheckpointsSql.FIND_ALL)
            .map { rs, _ -> rs.toCheckpoint() }
            .list()

    override fun save(entity: Checkpoint) {
        handle
            .createUpdate(CheckpointsSql.SAVE)
            .bind("id", entity.id)
            .bind("jobId", entity.jobId)
            .bind("metricsId", entity.metricsId)
            .bind("iteration", entity.iteration)
            .bind("solution", entity.solution)
            .execute()
    }

    override fun deleteById(id: Int): Boolean =
        handle
            .createUpdate(CheckpointsSql.DELETE_BY_ID)
            .bind("id", id)
            .execute() > 0

    override fun clear() {
        handle
            .createUpdate(CheckpointsSql.CLEAR)
            .execute()
    }

    private fun java.sql.ResultSet.toCheckpoint(): Checkpoint =
        Checkpoint(
            id = getInt("id"),
            jobId = getInt("jobId"),
            metricsId = getInt("metricsId"),
            iteration = getInt("iteration"),
            solution = getString("solution"),
            createdAt = getTimestamp("createdAt").toInstant(),
        )
}


