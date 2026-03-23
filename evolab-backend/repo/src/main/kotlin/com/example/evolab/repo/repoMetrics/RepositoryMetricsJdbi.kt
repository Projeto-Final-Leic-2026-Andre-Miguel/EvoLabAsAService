package com.example.evolab.repo.repoMetrics

import com.example.evolab.domain.metrics.Metric
import org.jdbi.v3.core.Handle

class RepositoryMetricsJdbi(
    private val handle: Handle,
) : RepositoryMetrics {

    override fun createMetric(
        jobId: Int,
        iteration: Int,
        fitnessScore: Double,
        executionTime: Double?,
    ): Int =
        handle
            .createQuery(MetricsSql.CREATE_METRIC)
            .bind("jobId", jobId)
            .bind("iteration", iteration)
            .bind("fitnessScore", fitnessScore)
            .bind("executionTime", executionTime)
            .mapTo(Int::class.java)
            .one()

    override fun findAllByJobId(jobId: Int): List<Metric> =
        handle
            .createQuery(MetricsSql.FIND_ALL_BY_JOB_ID)
            .bind("jobId", jobId)
            .map { rs, _ -> rs.toMetric() }
            .list()

    override fun findByJobIdAndIteration(jobId: Int, iteration: Int): Metric? =
        handle
            .createQuery(MetricsSql.FIND_BY_JOB_ID_AND_ITERATION)
            .bind("jobId", jobId)
            .bind("iteration", iteration)
            .map { rs, _ -> rs.toMetric() }
            .findOne()
            .orElse(null)

    override fun findById(id: Int): Metric? =
        handle
            .createQuery(MetricsSql.FIND_BY_ID)
            .bind("id", id)
            .map { rs, _ -> rs.toMetric() }
            .findOne()
            .orElse(null)

    override fun findAll(): List<Metric> =
        handle
            .createQuery(MetricsSql.FIND_ALL)
            .map { rs, _ -> rs.toMetric() }
            .list()

    override fun save(entity: Metric) {
        handle
            .createUpdate(MetricsSql.SAVE)
            .bind("id", entity.id)
            .bind("jobId", entity.jobId)
            .bind("iteration", entity.iteration)
            .bind("fitnessScore", entity.fitnessScore)
            .bind("executionTime", entity.executionTime)
            .execute()
    }

    override fun deleteById(id: Int): Boolean =
        handle
            .createUpdate(MetricsSql.DELETE_BY_ID)
            .bind("id", id)
            .execute() > 0

    override fun clear() {
        handle
            .createUpdate(MetricsSql.CLEAR)
            .execute()
    }

    private fun java.sql.ResultSet.toMetric(): Metric =
        Metric(
            id = getInt("id"),
            jobId = getInt("jobId"),
            iteration = getInt("iteration"),
            fitnessScore = getDouble("fitnessScore"),
            executionTime = getDouble("executionTime").let { if (wasNull()) null else it },
            createdAt = getTimestamp("createdAt").toInstant(),
        )
}


