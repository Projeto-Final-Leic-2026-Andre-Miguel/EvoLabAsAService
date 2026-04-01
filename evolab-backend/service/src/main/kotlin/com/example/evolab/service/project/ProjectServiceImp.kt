package com.example.evolab.service.project

import com.example.evolab.domain.evolution.EvolutionStatus
import com.example.evolab.domain.project.Project
import com.example.evolab.repo.transactions.Transaction
import com.example.evolab.repo.transactions.TransactionManager
import com.example.evolab.service.auxiliary.Either
import com.example.evolab.service.auxiliary.failure
import com.example.evolab.service.auxiliary.success
import jakarta.inject.Named

@Named
class ProjectServiceImp(
    private val trxManager: TransactionManager,
) : ProjectService {

    override fun createProject(
        userId: Int,
        name: String,
        description: String?,
        initialProgram: String?,
        evaluatorCode: String?,
    ): Either<ProjectServiceErrors, Project> =
        trxManager.run {
            validateName(name)?.let { return@run it }

            if (findProjectByName(userId, name)) {
                return@run failure(
                    ProjectServiceErrors.DuplicateProjectName(
                        "User with id '$userId' already has a project with name '$name'",
                    ),
                )
            }

            success(
                repoProjects.createProject(
                    userId = userId,
                    name = name,
                    description = description,
                    initialProgram = initialProgram,
                    evaluatorCode = evaluatorCode,
                ),
            )
        }

    override fun updateProjectDetails(
        projectId: Int,
        userId: Int,
        name: String?,
        description: String?,
        configId: Int?,
        initialProgram: String?,
        evaluatorCode: String?,
    ): Either<ProjectServiceErrors, Project> =
        trxManager.run {
            val project = findRequiredProject(projectId) ?: return@run failureNotFound(projectId)

            validateOwnership(project, userId)?.let { return@run it }
            validateMutableStatus(project)?.let { return@run it }
            validateConfigAccess(configId, userId)?.let { return@run it }

            if (name != null) {
                validateName(name)?.let { return@run it }
                if (name != project.name && findProjectByName(userId, name)) {
                    return@run failure(
                        ProjectServiceErrors.DuplicateProjectName(
                            "User with id '$userId' already has a project with name '$name'",
                        ),
                    )
                }
            }

            // aqui definimos que, caso o valor recebido seja null, é porque o campo não deve ser atualizado. Assim, só atualizamos os campos que foram efetivamente enviados na requisição.
            // para trocarmos de nome etc, os campos não pode vir a null.

            val updatedProject =
                project.copy(
                    name = name ?: project.name,
                    description = description ?: project.description,
                    configId = configId ?: project.configId,
                    initialProgram = initialProgram ?: project.initialProgram,
                    evaluatorCode = evaluatorCode ?: project.evaluatorCode,
                )

            repoProjects.save(updatedProject)
            success(updatedProject)
        }

    override fun updateProjectStatus(
        projectId: Int,
        newStatus: EvolutionStatus,
    ): Either<ProjectServiceErrors, Project> =
        trxManager.run {
            val project = findRequiredProject(projectId) ?: return@run failureNotFound(projectId)

            validateStatusTransition(project, newStatus)?.let { return@run it }

            val updatedProject = project.copy(status = newStatus)
            repoProjects.save(updatedProject)
            success(updatedProject)
        }

    override fun getProject(
        projectId: Int,
        userId: Int,
    ): Either<ProjectServiceErrors, Project> =
        trxManager.run {
            val project = findRequiredProject(projectId) ?: return@run failureNotFound(projectId)

            validateOwnership(project, userId)?.let { return@run it }
            success(project)
        }

    override fun getAllProjectsFromUser(userId: Int): Either<ProjectServiceErrors, List<Project>> =
        trxManager.run {
            success(repoProjects.findAllByUserId(userId))
        }

    override fun getAllProjects(): Either<ProjectServiceErrors, List<Project>> =
        trxManager.run {
            success(repoProjects.findAll())
        }

    override fun deleteProject(
        projectId: Int,
        userId: Int,
    ): Either<ProjectServiceErrors, Int> =
        trxManager.run {
            val project = findRequiredProject(projectId) ?: return@run failureNotFound(projectId)

            validateOwnership(project, userId)?.let { return@run it }

            val deleted = repoProjects.deleteById(projectId)
            if (!deleted) return@run failureNotFound(projectId)

            success(project.id)
        }

    private fun Transaction.findRequiredProject(projectId: Int): Project? =
        repoProjects.findById(projectId)

    private fun failureNotFound(projectId: Int) =
        failure(ProjectServiceErrors.ProjectNotFound("Project with id '$projectId' was not found"))

    private fun Transaction.validateConfigAccess(
        configId: Int?,
        userId: Int,
    ): Either<ProjectServiceErrors, Nothing>? {
        if (configId == null) return null

        val config =
            repoConfigs.findById(configId)
                ?: return failure(ProjectServiceErrors.ConfigNotFound("Config with id '$configId' was not found"))

        if (config.userId != userId) {
            return failure(
                ProjectServiceErrors.ConfigAccessDenied(
                    "User with id '$userId' cannot use config with id '$configId'",
                ),
            )
        }

        return null
    }

    private fun validateName(name: String): Either<ProjectServiceErrors, Nothing>? {
        if (name.isBlank()) {
            return failure(ProjectServiceErrors.InvalidProjectInput("Project name cannot be blank"))
        }
        return null
    }

    private fun validateOwnership(
        project: Project,
        userId: Int,
    ): Either<ProjectServiceErrors, Nothing>? {
        if (project.userId != userId) {
            return failure(
                ProjectServiceErrors.NotProjectOwner(
                    "User with id '$userId' is not the owner of project with id '${project.id}'",
                ),
            )
        }
        return null
    }

    private fun validateMutableStatus(project: Project): Either<ProjectServiceErrors, Nothing>? {
        if (project.status != EvolutionStatus.CREATED) {
            return failure(
                ProjectServiceErrors.InvalidProjectStatus(
                    "Project with id '${project.id}' is in status '${project.status}' and cannot be edited",
                ),
            )
        }
        return null
    }

    private fun validateStatusTransition(
        project: Project,
        newStatus: EvolutionStatus,
    ): Either<ProjectServiceErrors, Nothing>? {
        if (newStatus != EvolutionStatus.CREATED && !project.isReadyToStart()) {
            return failure(
                ProjectServiceErrors.InvalidProjectStatus(
                    "Project with id '${project.id}' cannot move to '$newStatus' without configId, initialProgram and evaluatorCode",
                ),
            )
        }
        return null
    }

    private fun Project.isReadyToStart(): Boolean =
        configId != null && !initialProgram.isNullOrBlank() && !evaluatorCode.isNullOrBlank()

    private fun Transaction.findProjectByName(userId: Int, name: String): Boolean =
        repoProjects.findAllByUserId(userId).any { it.name == name }
}
