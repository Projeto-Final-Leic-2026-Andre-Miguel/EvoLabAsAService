package com.example.evolab.service.checkpoints

import com.example.evolab.domain.checkpoint.Checkpoint
import com.example.evolab.service.auxiliary.Either

interface CheckpointService {
    fun createCheckpoint(
        userId: Int,
        jobId: Int,
        metricsId: Int,
        iteration: Int,
        solution: String,
    ): Either<CheckpointServiceErrors, Checkpoint>

    fun getCheckpoint(
        checkpointId: Int,
        userId: Int,
    ): Either<CheckpointServiceErrors, Checkpoint>

    fun getCheckpointsByJobId(
        jobId: Int,
        userId: Int,
    ): Either<CheckpointServiceErrors, List<Checkpoint>>

    fun getCheckpointsByMetricsId(
        metricsId: Int,
        userId: Int,
    ): Either<CheckpointServiceErrors, List<Checkpoint>>

    fun getCheckpointByJobAndIteration(
        jobId: Int,
        iteration: Int,
        userId: Int,
    ): Either<CheckpointServiceErrors, Checkpoint>

    fun getAllCheckpoints(): Either<CheckpointServiceErrors, List<Checkpoint>>

    fun deleteCheckpoint(checkpointId: Int): Either<CheckpointServiceErrors, Int>
}
