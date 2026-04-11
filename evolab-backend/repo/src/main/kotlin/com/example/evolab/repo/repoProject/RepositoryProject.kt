package com.example.evolab.repo.repoProject

import com.example.evolab.domain.evolution.EvolutionStatus
import com.example.evolab.domain.project.Project
import com.example.evolab.repo.Repository

interface RepositoryProject : Repository<Project> {
    fun createProject(
        userId: Int,
        name: String,
        description: String?,
        configId: Int?,
        initialProgram: String?,
        evaluatorCode: String?,
        status: EvolutionStatus = EvolutionStatus.CREATED,
    ): Project

    fun findAllByUserId(userId: Int): List<Project>

    fun findAllByConfigId(configId: Int): List<Project>

    fun findAllByStatus(status: EvolutionStatus): List<Project>

    fun findAllByName(name: String): List<Project>
}

