package com.example.evolab.service.checkpoints

sealed class CheckpointServiceErrors {
    data class CheckpointNotFound(
        val message: String,
    ) : CheckpointServiceErrors()

    data class JobNotFound(
        val message: String,
    ) : CheckpointServiceErrors()

    data class MetricNotFound(
        val message: String,
    ) : CheckpointServiceErrors()

    data class ProjectNotFound(
        val message: String,
    ) : CheckpointServiceErrors()

    data class CheckpointAccessDenied(
        val message: String,
    ) : CheckpointServiceErrors()

    data class InvalidCheckpointInput(
        val message: String,
    ) : CheckpointServiceErrors()

    data class DuplicateCheckpointForIteration(
        val message: String,
    ) : CheckpointServiceErrors()

    data class MetricDoesNotBelongToJob(
        val message: String,
    ) : CheckpointServiceErrors()
}
