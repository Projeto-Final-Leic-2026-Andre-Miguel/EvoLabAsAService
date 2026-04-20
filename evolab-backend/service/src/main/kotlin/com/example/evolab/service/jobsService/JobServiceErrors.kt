package com.example.evolab.service.jobsService

sealed class JobServiceErrors {

    data class JobNotFound(val message: String) : JobServiceErrors()

    data class InvalidJobInput(val message: String) : JobServiceErrors()
}
