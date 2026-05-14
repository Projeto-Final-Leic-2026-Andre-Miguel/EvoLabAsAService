
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
    title: String? = null,
    val detail: String? = null,
) {
    @Suppress("unused")
    val type = typeUri.toString()
    val title = title ?: typeUri.toString().split("/").last()

    fun response(status: HttpStatus): ResponseEntity<Any> =
        ResponseEntity
            .status(status)
            .header("Content-Type", MEDIA_TYPE)
            .body(this)

    fun withDetail(detail: String?): Problem =
        DetailedProblem(URI(type), title, detail)

    private class DetailedProblem(
        typeUri: URI,
        title: String,
        detail: String?,
    ) : Problem(typeUri, title, detail)

    data object InvalidLLMProvider : Problem(URI("$PROBLEM_URI_PATH/invalid-llm-provider"))

    data object InvalidAPIKey : Problem(URI("$PROBLEM_URI_PATH/invalid-api-key"))

    data object LLMCrendentialsNotFound : Problem(URI("$PROBLEM_URI_PATH/llm-credentials-not-found"))

    data object CredentialWithProviderAlreadyInUse : Problem(URI("$PROBLEM_URI_PATH/credential-with-provider-already-in-use"))

    data object PersistenceError : Problem(URI("$PROBLEM_URI_PATH/persistence-error"))

    data object UnknownError : Problem(
        URI("$PROBLEM_URI_PATH/unknown-error"),
        title = "An unexpected error occurred",
    )

    data object UnauthorizedAccess : Problem(URI("$PROBLEM_URI_PATH/unauthorized-access"))

    data object ProjectNotFound : Problem(URI("$PROBLEM_URI_PATH/project-not-found"))

    data object ConfigNotFound : Problem(URI("$PROBLEM_URI_PATH/config-not-found"))

    data object InvalidProjectInput : Problem(URI("$PROBLEM_URI_PATH/invalid-project-input"))

    data object NotProjectOwner : Problem(URI("$PROBLEM_URI_PATH/not-project-owner"))

    data object ConfigAccessDenied : Problem(URI("$PROBLEM_URI_PATH/config-access-denied"))

    data object DuplicateProjectName : Problem(URI("$PROBLEM_URI_PATH/duplicate-project-name"))

    data object InvalidProjectStatus : Problem(URI("$PROBLEM_URI_PATH/invalid-project-status"))

    data object ExecutionQueueUnavailable : Problem(URI("$PROBLEM_URI_PATH/execution-queue-unavailable"))

    data object MetricNotFound : Problem(URI("$PROBLEM_URI_PATH/metric-not-found"))

    data object JobNotFound : Problem(URI("$PROBLEM_URI_PATH/job-not-found"))

    data object JobAccessDenied : Problem(URI("$PROBLEM_URI_PATH/job-access-denied"))

    data object InvalidJobInput : Problem(URI("$PROBLEM_URI_PATH/invalid-job-input"))

    data object MetricAccessDenied : Problem(URI("$PROBLEM_URI_PATH/metric-access-denied"))

    data object InvalidMetricInput : Problem(URI("$PROBLEM_URI_PATH/invalid-metric-input"))

    data object DuplicateMetricForIteration : Problem(URI("$PROBLEM_URI_PATH/duplicate-metric-for-iteration"))

    data object CheckpointNotFound : Problem(URI("$PROBLEM_URI_PATH/checkpoint-not-found"))

    data object CheckpointAccessDenied : Problem(URI("$PROBLEM_URI_PATH/checkpoint-access-denied"))

    data object InvalidCheckpointInput : Problem(URI("$PROBLEM_URI_PATH/invalid-checkpoint-input"))

    data object DuplicateCheckpointForIteration : Problem(URI("$PROBLEM_URI_PATH/duplicate-checkpoint-for-iteration"))

    data object MetricDoesNotBelongToJob : Problem(URI("$PROBLEM_URI_PATH/metric-does-not-belong-to-job"))
}
