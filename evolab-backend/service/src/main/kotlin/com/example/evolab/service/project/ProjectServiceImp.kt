package com.example.evolab.service.project

import com.example.evolab.domain.evolution.EvolutionStatus
import com.example.evolab.domain.project.Project
import com.example.evolab.repo.transactions.Transaction
import com.example.evolab.repo.transactions.TransactionManager
import com.example.evolab.service.auxiliary.Either
import com.example.evolab.service.auxiliary.Failure
import com.example.evolab.service.auxiliary.Success
import com.example.evolab.service.auxiliary.failure
import com.example.evolab.service.auxiliary.success
import com.example.evolab.service.jobExecution.JobQueue
import jakarta.inject.Named

@Named
class ProjectServiceImp(
    private val trxManager: TransactionManager,
    private val jobQueue: JobQueue,
) : ProjectService {

    override fun createProject(
        userId: Int,
        name: String,
        description: String?,
        configId: Int?,
        initialProgram: String?,
        evaluatorCode: String?,
    ): Either<ProjectServiceErrors, Project> =
        trxManager.run {
            validateName(name)?.let { return@run it }

            if(configId != null && !findConfigById(configId)) {
                return@run failure(ProjectServiceErrors.ConfigNotFound("Config with id '$configId' was not found"))
            }

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
                    configId = configId,
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

            // Se o valor recebido for null, o campo nao deve ser atualizado.
            // So atualizamos os campos que foram efetivamente enviados na request.
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

    // StartExperimentation: adicionar o projeto ao request da nossa queue para, quando
    // houver um worker disponivel, ele comecar a trabalhar.
    // O pedido HTTP para isto devera ser um POST vazio no controller.
    override fun startExperimentation(
        projectId: Int,
        userId: Int,
    ): Either<ProjectServiceErrors, Project> =
        trxManager.run {
            val project = findRequiredProject(projectId) ?: return@run failureNotFound(projectId)

            validateOwnership(project, userId)?.let { return@run it }
            validateConfigAccess(project.configId, userId)?.let { return@run it }
            validateExperimentationStart(project)?.let { return@run it }

            val queuedProject = project.copy(status = EvolutionStatus.QUEUED)

            when (val enqueueResult = jobQueue.enqueue(queuedProject)) {
                is Success -> {
                    repoProjects.save(queuedProject)
                    success(queuedProject)
                }

                is Failure ->
                    failure(ProjectServiceErrors.ExecutionQueueUnavailable(enqueueResult.value))
            }
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

    private fun validateExperimentationStart(project: Project): Either<ProjectServiceErrors, Nothing>? {
        if (project.status != EvolutionStatus.CREATED) {
            return failure(
                ProjectServiceErrors.InvalidProjectStatus(
                    "Project with id '${project.id}' is in status '${project.status}' and cannot be queued for experimentation",
                ),
            )
        }

        return validateStatusTransition(project, EvolutionStatus.QUEUED)
    }

    private fun Project.isReadyToStart(): Boolean =
        configId != null && !initialProgram.isNullOrBlank() && !evaluatorCode.isNullOrBlank()

    private fun Transaction.findProjectByName(userId: Int, name: String): Boolean =
        repoProjects.findAllByUserId(userId).any { it.name == name }

    private fun Transaction.findConfigById(configId: Int) : Boolean =
        repoConfigs.findById(configId) != null


}

// Depois do LLM Credentials, testo tudo, user, llm, projects, etc etc
