package com.example.evolab.service.project

import com.example.evolab.domain.evolution.EvolutionStatus
import com.example.evolab.domain.project.Project
import com.example.evolab.service.auxiliary.Either

interface ProjectService {

    fun createProject(
        userId: Int,
        name: String,
        description: String?,
        configId: Int?,
        initialProgram: String?,
        evaluatorCode: String?
    ): Either<ProjectServiceErrors, Project>

    fun updateProjectDetails(
        projectId: Int,
        userId: Int,
        name: String?,
        description: String?,
        configId: Int?,
        initialProgram: String?,
        evaluatorCode: String?
    ): Either<ProjectServiceErrors, Project>

    fun updateProjectStatus(
        projectId: Int,
        newStatus: EvolutionStatus
    ): Either<ProjectServiceErrors, Project>

    fun getProject(projectId: Int, userId: Int): Either<ProjectServiceErrors, Project>

    fun getAllProjectsFromUser(userId: Int): Either<ProjectServiceErrors, List<Project>>

    fun getAllProjects(): Either<ProjectServiceErrors, List<Project>>

    fun deleteProject(projectId: Int, userId: Int): Either<ProjectServiceErrors, Int>
}
