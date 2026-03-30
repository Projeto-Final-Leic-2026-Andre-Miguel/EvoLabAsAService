package com.example.evolab.http.model.project

data class UpdateProjectDetailsInput(
    val name: String?,
    val description: String?,
    val configId: Int?,
    val initialProgram: String?,
    val evaluatorCode: String?,
)

