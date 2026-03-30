package com.example.evolab.http.model.project

import com.example.evolab.domain.evolution.EvolutionStatus

data class UpdateProjectStatusInput(
    val status: EvolutionStatus,
)

