package com.example.evolab.http.model.config

import java.time.Instant

data class UserConfigOutput(
    val configId: Int,
    val projectId: Int?,
    val userId: Int,
    val llmCredentialsId: Int,
    val modelName: String,
    val maxIter: Int,
    val checkPointInterval: Int,
    val additionalParams: Map<String, String>,
    val createdAt: Instant,
)

