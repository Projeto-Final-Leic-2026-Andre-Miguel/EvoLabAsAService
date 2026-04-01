
package com.example.evolab.http.model.problem

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.net.URI
import kotlin.collections.last
import kotlin.text.split

private const val MEDIA_TYPE = "application/problem+json"
private const val PROBLEM_URI_PATH =
    "https://github.com/Projeto-Final-Leic-2026-Andre-Miguel/EvoLabAsAService/tree/main/docs/Problems"

sealed class Problem(
    typeUri: URI,
) {
    @Suppress("unused")
    val type = typeUri.toString()
    val title = typeUri.toString().split("/").last()

    fun response(status: HttpStatus): ResponseEntity<Any> =
        ResponseEntity
            .status(status)
            .header("Content-Type", MEDIA_TYPE)
            .body(this)

    data object InvalidLLMProvider : Problem(URI("$PROBLEM_URI_PATH/invalid-llm-provider"))

    data object InvalidAPIKey : Problem(URI("$PROBLEM_URI_PATH/invalid-api-key"))

    data object LLMCrendentialsNotFound : Problem(URI("$PROBLEM_URI_PATH/llm-credentials-not-found"))

    data object CredentialWithProviderAlreadyInUse : Problem(URI("$PROBLEM_URI_PATH/credential-with-provider-already-in-use"))

    data object PersistenceError : Problem(URI("$PROBLEM_URI_PATH/persistence-error"))

    data object UnknownError : Problem(URI("$PROBLEM_URI_PATH/unknown-error"))

    data object UnauthorizedAccess : Problem(URI("$PROBLEM_URI_PATH/unauthorized-access"))

    data object ProjectNotFound : Problem(URI("$PROBLEM_URI_PATH/project-not-found"))

    data object ConfigNotFound : Problem(URI("$PROBLEM_URI_PATH/config-not-found"))

    data object InvalidProjectInput : Problem(URI("$PROBLEM_URI_PATH/invalid-project-input"))

    data object NotProjectOwner : Problem(URI("$PROBLEM_URI_PATH/not-project-owner"))

    data object ConfigAccessDenied : Problem(URI("$PROBLEM_URI_PATH/config-access-denied"))

    data object DuplicateProjectName : Problem(URI("$PROBLEM_URI_PATH/duplicate-project-name"))

    data object InvalidProjectStatus : Problem(URI("$PROBLEM_URI_PATH/invalid-project-status"))
}
