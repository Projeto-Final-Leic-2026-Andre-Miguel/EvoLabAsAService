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


    /**
     * Aqui tanto o ficheiro de configuração, o programa inciial e o avaliador, podem ser nulos, mas enquanto o forem,
     * o projeto não pode começar, permanecendo em CREATED e apenas quando preenchidos podem começar a evoluir, passando
     *
     * */

    override fun createProject(
        userId: Int,
        configId: Int?,
        name: String,
        description: String?,
        initialProgram: String?,
        evaluatorCode: String?
    ): Either<ProjectServiceErrors, Project> =
        trxManager.run{

            if(findProjectByName(userId,name))
                 failure(ProjectServiceErrors.DuplicateProjectName("User with id '$userId' already has a project with name '$name'"))

            val project = repoProjects.createProject(
                userId = userId,
                configId = configId,
                name = name,
                description = description,
                initialProgram = initialProgram,
                evaluatorCode = evaluatorCode
            )

             success(project)
        }


    override fun updateProjectDetails(
        projectId: Int,
        userId: Int,
        name: String?,
        description: String?,
        configId: Int?,
        initialProgram: String?,
        evaluatorCode: String?
    ): Either<ProjectServiceErrors, Project> {
        TODO("Not yet implemented")
    }

    override fun updateProjectStatus(
        projectId: Int,
        newStatus: EvolutionStatus
    ): Either<ProjectServiceErrors, Project> {
        TODO("Not yet implemented")
    }

    override fun getProject(
        projectId: Int,
        userId: Int
    ): Either<ProjectServiceErrors, Project> {
        TODO("Not yet implemented")
    }

    override fun getAllProjectsFromUser(userId: Int): Either<ProjectServiceErrors, List<Project>> {
        TODO("Not yet implemented")
    }

    override fun getAllProjects(): Either<ProjectServiceErrors, List<Project>> {
        TODO("Not yet implemented")
    }

    override fun deleteProject(
        projectId: Int,
        userId: Int
    ): Either<ProjectServiceErrors, Project> {
        TODO("Not yet implemented")
    }

    private fun Transaction.findProjectByName(userId : Int, name : String): Boolean =
        repoProjects.findAllByUserId(userId).any{ it.name == name }


}