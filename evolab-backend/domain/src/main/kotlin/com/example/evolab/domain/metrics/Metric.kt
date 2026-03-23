package com.example.evolab.domain.metrics

import java.time.Instant

data class Metric(
    val id: Int,
    val jobId: Int,
    val iteration: Int,
    val fitnessScore: Double,
    val executionTime: Double?,
    val createdAt: Instant,
)

