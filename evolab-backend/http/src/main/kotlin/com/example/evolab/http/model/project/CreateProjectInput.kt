package com.example.evolab.http.model.project

data class CreateProjectInput(
    val name: String,
    val description: String?,
    val initialProgram: String?,
    val evaluatorCode: String?,
)

