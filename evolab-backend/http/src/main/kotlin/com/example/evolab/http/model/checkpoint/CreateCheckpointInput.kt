package com.example.evolab.http.model.checkpoint

data class CreateCheckpointInput(
    val metricsId: Int,
    val iteration: Int,
    val solution: String,
)
