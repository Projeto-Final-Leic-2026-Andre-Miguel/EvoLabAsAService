package com.example.evolab.http.model.config

data class UpdateConfigInput(
    val modelName: String,
    val maxIter: Int,
    val checkPointInterval: Int,
    val additionalParams: Map<String, String>,
)

