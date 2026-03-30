package com.example.evolab.http.controllers

import com.example.evolab.domain.project.Project
import com.example.evolab.domain.user.AuthenticatedUser
import com.example.evolab.http.model.problem.Problem
import com.example.evolab.http.model.project.CreateProjectInput
import com.example.evolab.http.model.project.UpdateProjectDetailsInput
import com.example.evolab.http.model.project.UpdateProjectStatusInput
import com.example.evolab.service.project.ProjectService
import com.example.evolab.service.project.ProjectServiceErrors
import com.example.evolab.service.auxiliary.Either
import com.example.evolab.service.auxiliary.Failure
import com.example.evolab.service.auxiliary.Success
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/projects")
class ProjectController(
    private val projectService: ProjectService,
) {

    @PostMapping
    fun createProject(
        @RequestBody input: CreateProjectInput,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val result: Either<ProjectServiceErrors, Project> =
            projectService.createProject(
                userId = authenticatedUser.user.id,
                configId = input.configId,
                name = input.name,
                description = input.description,
                initialProgram = input.initialProgram,
                evaluatorCode = input.evaluatorCode,
            )

        return when (result) {
            is Success -> {
                ResponseEntity.status(HttpStatus.CREATED)
                    .header("Location", "/api/projects/${result.value.id}")
                    .body(result.value)
            }

            is Failure -> mapServiceErrors(result.value)
        }
    }

    @PutMapping("/{id}")
    fun updateProjectDetails(
        @PathVariable id: Int,
        @RequestBody input: UpdateProjectDetailsInput,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val result: Either<ProjectServiceErrors, Project> =
            projectService.updateProjectDetails(
                projectId = id,
                userId = authenticatedUser.user.id,
                name = input.name,
                description = input.description,
                configId = input.configId,
                initialProgram = input.initialProgram,
                evaluatorCode = input.evaluatorCode,
            )

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Failure -> mapServiceErrors(result.value)
        }
    }

    // Worker-driven status transition endpoint: no authenticated user ownership check.
//    @PutMapping("/{id}/status")
//    fun updateProjectStatus(
//        @PathVariable id: Int,
//        @RequestBody input: UpdateProjectStatusInput,
//    ): ResponseEntity<*> {
//        val result: Either<ProjectServiceErrors, Project> =
//            projectService.updateProjectStatus(
//                projectId = id,
//                newStatus = input.status,
//            )
//
//        return when (result) {
//            is Success -> ResponseEntity.status(HttpStatus.OK).body(result.value)
//            is Failure -> mapServiceErrors(result.value)
//        }
//    }

    @GetMapping("/{id}")
    fun getProject(
        @PathVariable id: Int,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val result: Either<ProjectServiceErrors, Project> =
            projectService.getProject(
                projectId = id,
                userId = authenticatedUser.user.id,
            )

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Failure -> mapServiceErrors(result.value)
        }
    }

    @GetMapping("/me")
    fun getAllProjectsFromUser(authenticatedUser: AuthenticatedUser): ResponseEntity<*> {
        val result = projectService.getAllProjectsFromUser(authenticatedUser.user.id)

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Failure -> mapServiceErrors(result.value)
        }
    }

    @GetMapping
    fun getAllProjects(): ResponseEntity<*> {
        val result = projectService.getAllProjects()

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Failure -> mapServiceErrors(result.value)
        }
    }

    @DeleteMapping("/{id}")
    fun deleteProject(
        @PathVariable id: Int,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val result = projectService.deleteProject(id, authenticatedUser.user.id)

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK)
                .body("Project with id '${result.value}' was successfully deleted")

            is Failure -> mapServiceErrors(result.value)
        }
    }

    private fun mapServiceErrors(error: ProjectServiceErrors): ResponseEntity<*> =
        when (error) {
            is ProjectServiceErrors.ProjectNotFound -> Problem.ProjectNotFound.response(HttpStatus.NOT_FOUND)
            is ProjectServiceErrors.InvalidProjectInput -> Problem.InvalidProjectInput.response(HttpStatus.BAD_REQUEST)
            is ProjectServiceErrors.NotProjectOwner -> Problem.NotProjectOwner.response(HttpStatus.FORBIDDEN)
            is ProjectServiceErrors.DuplicateProjectName -> Problem.DuplicateProjectName.response(HttpStatus.CONFLICT)
            is ProjectServiceErrors.InvalidProjectStatus -> Problem.InvalidProjectStatus.response(HttpStatus.CONFLICT)
        }






}