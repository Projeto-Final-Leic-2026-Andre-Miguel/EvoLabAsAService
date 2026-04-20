package com.example.evolab.http.model.job

import com.example.evolab.domain.evolution.EvolutionStatus

data class CreateJobInput(
    val projectId: Int,
    val status: EvolutionStatus = EvolutionStatus.CREATED,
)
