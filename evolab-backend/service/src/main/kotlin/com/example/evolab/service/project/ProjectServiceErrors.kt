package com.example.evolab.service.project

sealed class ProjectServiceErrors {

    data class ProjectNotFound(val message: String) : ProjectServiceErrors()

    data class ConfigNotFound(val message: String) : ProjectServiceErrors()

    data class InvalidProjectInput(val message: String) : ProjectServiceErrors()

    data class NotProjectOwner(val message: String) : ProjectServiceErrors()

    data class ConfigAccessDenied(val message: String) : ProjectServiceErrors()

    data class DuplicateProjectName(val message: String) : ProjectServiceErrors()

    data class InvalidProjectStatus(val message: String) : ProjectServiceErrors()

    data class ExecutionQueueUnavailable(val message: String) : ProjectServiceErrors()
}
