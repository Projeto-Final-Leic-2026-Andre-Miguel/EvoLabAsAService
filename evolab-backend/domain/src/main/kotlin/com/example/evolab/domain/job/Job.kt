package com.example.evolab.domain.job

import com.example.evolab.domain.evolution.EvolutionStatus
import java.time.Instant

data class Job(
    val id: Int,
    val projectId: Int,
    val status: EvolutionStatus,
    val containerId: String?,
    val startedAt: Instant?,
    val finishedAt: Instant?,
    val bestSolution: String?,
    val executionLogs: String?,
    val createdAt: Instant,
    val failureReason: String? = null,
)

