package com.example.evolab.http.model.metric

data class CreateMetricInput(
    val iteration: Int,
    val fitnessScore: Double,
    val executionTime: Double?,
)
