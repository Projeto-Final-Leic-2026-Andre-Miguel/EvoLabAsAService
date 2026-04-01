package com.example.evolab.http.model.config

data class CreateConfigInput(
    val projectId: Int?,
    val llmCredentialsId: Int,
    val modelName: String,
    val maxIter: Int,
    val checkPointInterval: Int,
    val additionalParams: Map<String, String>,
)

