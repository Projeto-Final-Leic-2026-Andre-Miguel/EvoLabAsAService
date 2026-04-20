package com.example.evolab.service.metrics

sealed class MetricServiceErrors {
    data class MetricNotFound(
        val message: String,
    ) : MetricServiceErrors()

    data class JobNotFound(
        val message: String,
    ) : MetricServiceErrors()

    data class ProjectNotFound(
        val message: String,
    ) : MetricServiceErrors()

    data class MetricAccessDenied(
        val message: String,
    ) : MetricServiceErrors()

    data class InvalidMetricInput(
        val message: String,
    ) : MetricServiceErrors()

    data class DuplicateMetricForIteration(
        val message: String,
    ) : MetricServiceErrors()
}
