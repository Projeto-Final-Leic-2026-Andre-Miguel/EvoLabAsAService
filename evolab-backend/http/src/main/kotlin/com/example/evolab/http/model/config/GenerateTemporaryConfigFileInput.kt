package com.example.evolab.http.model.config

data class GenerateTemporaryConfigFileInput(
    val projectId: Int,
    val jobId: Int? = null,
)

