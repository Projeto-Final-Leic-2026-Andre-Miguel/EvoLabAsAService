package com.example.evolab.repo.repoCheckpoints

import com.example.evolab.domain.checkpoint.Checkpoint
import com.example.evolab.repo.Repository

interface RepositoryCheckpoints : Repository<Checkpoint> {
    fun createCheckpoint(
        jobId: Int,
        metricsId: Int,
        iteration: Int,
        solution: String,
    ): Int

    fun findAllByJobId(jobId: Int): List<Checkpoint>

    fun findAllByMetricsId(metricsId: Int): List<Checkpoint>

    fun findByJobIdAndIteration(jobId: Int, iteration: Int): Checkpoint?
}

