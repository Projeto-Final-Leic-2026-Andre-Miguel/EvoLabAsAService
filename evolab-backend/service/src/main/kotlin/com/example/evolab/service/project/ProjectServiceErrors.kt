package com.example.evolab.service.project

sealed class ProjectServiceErrors {

    data class ProjectNotFound(val message: String) : ProjectServiceErrors()

    data class InvalidProjectInput(val message: String) : ProjectServiceErrors()

    data class NotProjectOwner(val message: String) : ProjectServiceErrors()

    data class DuplicateProjectName(val name: String) : ProjectServiceErrors()


}