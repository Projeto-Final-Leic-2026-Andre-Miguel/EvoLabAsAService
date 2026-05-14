package com.example.evolab.http.controllers

import com.example.evolab.domain.LLMCredentials.LLMCredentials
import com.example.evolab.domain.user.AuthenticatedUser
import com.example.evolab.http.model.llmCredentials.CreateLLMCredentialRequest
import com.example.evolab.http.model.llmCredentials.CreateLocalModelCredentialRequest
import com.example.evolab.http.model.problem.Problem
import com.example.evolab.service.LLMCredentials.service.LLMCredentialsService
import com.example.evolab.service.LLMCredentials.service.LLMCredentialsServiceErrors
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
@RequestMapping("/api/llm-credentials")
class LLMCredentialsController(
    private val llmCredentialsService : LLMCredentialsService
) {

    @PostMapping()
    suspend fun createLLMCredential(
        @RequestBody input: CreateLLMCredentialRequest,
        authenticatedUser: AuthenticatedUser
    ): ResponseEntity<*> {

        val result: Either<LLMCredentialsServiceErrors, LLMCredentials> = llmCredentialsService.createLLMCredential(
            userId = authenticatedUser.user.id,
            llm = input.llm,
            apiKey = input.apiKey
        )

        return when (result) {
            is Success -> {
                ResponseEntity.status(HttpStatus.CREATED)
                    .body(result.value)
            }

            is Failure -> {
                mapServiceErrors(result.value)
            }

        }

    }


    @PostMapping("/localModel")
    suspend fun createLocalModelCredential(
        @RequestBody input: CreateLocalModelCredentialRequest,
        authenticatedUser: AuthenticatedUser
    ): ResponseEntity<*> {
        val result = llmCredentialsService.createLocalModelCredential(
            userId = authenticatedUser.user.id,
            apiKey = input.apiKey,
            port = input.port,
            modelName = input.modelName
        )
        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.value)
            is Failure -> mapServiceErrors(result.value)
        }
    }





    @GetMapping("/{id}")
    fun getLLMCredentialById(
        @PathVariable id: Int,
        authenticatedUser: AuthenticatedUser
    ): ResponseEntity<*> {

        val result: Either<LLMCredentialsServiceErrors, LLMCredentials> =
            llmCredentialsService.getLLMCredentialById(authenticatedUser.user.id, id)

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Failure -> mapServiceErrors(result.value)
        }
    }

    @GetMapping("/localModel/{id}")
    fun getLocalModelCredentialById(
        @PathVariable id: Int,
        authenticatedUser: AuthenticatedUser
    ): ResponseEntity<*> {

        val result = llmCredentialsService.getLocalModelCredentialById(authenticatedUser.user.id, id)

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Failure -> mapServiceErrors(result.value)
        }
    }

    @DeleteMapping("/{id}")
    fun deleteLLMCredential(
        @PathVariable id: Int,
        authenticatedUser: AuthenticatedUser
    ): ResponseEntity<*> {

        val result: Either<LLMCredentialsServiceErrors, Int> =
            llmCredentialsService.deleteLLMCredential(authenticatedUser.user.id, id)

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK)
                .body("Credential with id '${result.value}' was successfully deleted")

            is Failure -> mapServiceErrors(result.value)
        }


    }


    @PutMapping("/{id}")
    suspend fun updateLLMCredential(
        @PathVariable id: Int,
        @RequestBody input: CreateLLMCredentialRequest,
        authenticatedUser: AuthenticatedUser
    ): ResponseEntity<*> {

        val result: Either<LLMCredentialsServiceErrors, LLMCredentials> = llmCredentialsService.updateLLMCredential(
            id = id,
            userId = authenticatedUser.user.id,
            apiKey = input.apiKey
        )

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Failure -> mapServiceErrors(result.value)
        }


    }


    @GetMapping()
    fun getAllLLMCredentialsForUser(
        authenticatedUser: AuthenticatedUser
    ): ResponseEntity<*> {

        val result: Either<LLMCredentialsServiceErrors, List<LLMCredentials>> =
            llmCredentialsService.getLLMCredentialsByUserId(authenticatedUser.user.id)

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Failure -> mapServiceErrors(result.value)
        }
    }


    @GetMapping("/all")
    fun getAllLLMCredentials(authenticatedUser: AuthenticatedUser): ResponseEntity<*> {

        val result: Either<LLMCredentialsServiceErrors, List<LLMCredentials>> =
            llmCredentialsService.getLLMCredentialsByUserId(authenticatedUser.user.id)

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Failure -> mapServiceErrors(result.value)
        }
    }

    @PostMapping("/{id}/validate")
    suspend fun validateLLMCredential(
        @PathVariable id: Int,
        authenticatedUser: AuthenticatedUser
    ): ResponseEntity<*> {
        val result: Either<LLMCredentialsServiceErrors, Boolean> =
            llmCredentialsService.validateCredential(authenticatedUser.user.id, id)

        return when (result) {
            is Success -> ResponseEntity.status(HttpStatus.OK).body(mapOf("isValid" to result.value))
            is Failure -> mapServiceErrors(result.value)
        }
    }

}

private fun mapServiceErrors(error: LLMCredentialsServiceErrors): ResponseEntity<*> {
    return when (error) {
        is LLMCredentialsServiceErrors.InvalidLLMProvider -> Problem.InvalidLLMProvider.withDetail(error.message).response(HttpStatus.BAD_REQUEST)
        is LLMCredentialsServiceErrors.LLMCredentialNotFound -> Problem.LLMCrendentialsNotFound.withDetail(error.message).response(HttpStatus.NOT_FOUND)
        is LLMCredentialsServiceErrors.InvalidApiKey -> Problem.InvalidAPIKey.withDetail(error.message).response(HttpStatus.BAD_REQUEST)
        is LLMCredentialsServiceErrors.CredentialWithProviderAlreadyInUse -> Problem.CredentialWithProviderAlreadyInUse.withDetail(error.message).response(HttpStatus.CONFLICT)
        is LLMCredentialsServiceErrors.PersistenceError -> Problem.UnknownError.withDetail(error.message).response(HttpStatus.INTERNAL_SERVER_ERROR)
        is LLMCredentialsServiceErrors.UnknownError -> Problem.UnknownError.withDetail(error.message).response(HttpStatus.INTERNAL_SERVER_ERROR)
        is LLMCredentialsServiceErrors.UnauthorizedAccess -> Problem.UnauthorizedAccess.withDetail(error.message).response(HttpStatus.FORBIDDEN)
    }
}
