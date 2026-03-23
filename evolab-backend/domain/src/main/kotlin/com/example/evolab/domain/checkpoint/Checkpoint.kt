package com.example.evolab.domain.checkpoint

import java.time.Instant

data class Checkpoint(
    val id: Int,
    val jobId: Int,
    val metricsId: Int,
    val iteration: Int,
    val solution: String,
    val createdAt: Instant,
)

