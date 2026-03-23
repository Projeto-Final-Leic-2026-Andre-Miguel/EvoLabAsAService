package com.example.evolab.repo.repoMetrics

import com.example.evolab.domain.metrics.Metric
import com.example.evolab.repo.Repository

interface RepositoryMetrics : Repository<Metric> {
    fun createMetric(
        jobId: Int,
        iteration: Int,
        fitnessScore: Double,
        executionTime: Double?,
    ): Int

    fun findAllByJobId(jobId: Int): List<Metric>

    fun findByJobIdAndIteration(jobId: Int, iteration: Int): Metric?
}

