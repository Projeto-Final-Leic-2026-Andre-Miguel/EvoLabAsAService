package com.example.evolab.service.metrics

import com.example.evolab.domain.metrics.Metric
import com.example.evolab.service.auxiliary.Either

interface MetricService {
    fun createMetric(
        userId: Int,
        jobId: Int,
        iteration: Int,
        fitnessScore: Double,
        executionTime: Double?,
    ): Either<MetricServiceErrors, Metric>

    fun getMetric(
        metricId: Int,
        userId: Int,
    ): Either<MetricServiceErrors, Metric>

    fun getMetricsByJobId(
        jobId: Int,
        userId: Int,
    ): Either<MetricServiceErrors, List<Metric>>

    fun getMetricByJobAndIteration(
        jobId: Int,
        iteration: Int,
        userId: Int,
    ): Either<MetricServiceErrors, Metric>

    fun getAllMetrics(): Either<MetricServiceErrors, List<Metric>>

    fun deleteMetric(metricId: Int): Either<MetricServiceErrors, Int>
}
