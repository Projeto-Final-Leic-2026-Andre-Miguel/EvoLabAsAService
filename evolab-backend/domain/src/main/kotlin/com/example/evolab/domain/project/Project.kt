package com.example.evolab.domain.project

import com.example.evolab.domain.evolution.EvolutionStatus
import org.w3c.dom.Text
import java.time.Instant

data class Project(
    val id: Int,
    val userId: Int,
    val configId: Int?,
    val name: String,
    val description: String?,
    val initialProgram: String,  // validar se é mesmo String ou TEXT
    val evaluatorCode: String,
    val status: EvolutionStatus,
    val createdAt: Instant,
)

