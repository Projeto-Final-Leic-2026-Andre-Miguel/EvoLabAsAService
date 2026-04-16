package com.example.evolab.service.LLMCredentials.validator

import com.example.evolab.domain.LLMCredentials.LLM
import com.example.evolab.service.auxiliary.Either
import com.example.evolab.service.auxiliary.Failure
import com.example.evolab.service.auxiliary.Success
import com.example.evolab.service.auxiliary.failure
import com.example.evolab.service.auxiliary.success
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import jakarta.inject.Named


@Named
class LLMCredentialValidatorImp(
    private val client : HttpClient
) : LLMCrendentialsValidator {


    companion object {
        private const val LOCAL_MODEL_VALIDATION_PREFIX = "LOCAL_MODEL::"
        private const val LOCAL_MODEL_VALIDATION_SEPARATOR = "::MODEL::"
        private const val GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val OPENAI_ENDPOINT = "https://api.openai.com/v1/models"


        private const val GEMINI_KEY_PREFIX = "AIza"
        private const val OPENAI_KEY_PREFIX = "sk-"

    }


    private suspend fun openAiKeyValidator(apiKey: String): Either<LLMValidatorErrors, Boolean> {


        if (!validateKeyFormat(apiKey, OPENAI_KEY_PREFIX)) {
            return failure(LLMValidatorErrors.InvalidKeyFormat("OpenAI API key must start with '$OPENAI_KEY_PREFIX'"))
        }

        return try {

            val response = client.get(OPENAI_ENDPOINT) {
                header("Authorization", "Bearer $apiKey")
            }

            when (response.status) {
                HttpStatusCode.OK -> success(true)
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> failure(LLMValidatorErrors.InvalidAPIKey("Invalid OpenAI API key"))
                else -> failure(LLMValidatorErrors.InvalidLLM("InvalidLLM specified for validation"))
            }

        } catch (e: Exception) {
            failure(LLMValidatorErrors.UnknownError("Network error occurred while validating OpenAI API key: ${e.message}"))
        }

    }

    private suspend fun geminiKeyValidator(apiKey: String): Either<LLMValidatorErrors, Boolean> {

        if (!validateKeyFormat(apiKey, GEMINI_KEY_PREFIX)) {
            return failure(LLMValidatorErrors.InvalidKeyFormat("Gemini API key must start with '$GEMINI_KEY_PREFIX'"))
        }

        return try {

            val response = client.get(GEMINI_ENDPOINT) {
                header("x-goog-api-key", apiKey)
            }

              when(response.status) {

                  HttpStatusCode.OK -> success(true)
                  HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden, HttpStatusCode.BadRequest -> failure(LLMValidatorErrors.InvalidAPIKey("Invalid Gemini API key"))
                    else -> failure(LLMValidatorErrors.InvalidLLM("InvalidLLM specified for validation"))
                }


             } catch (e: Exception) {
                     failure(LLMValidatorErrors.UnknownError("Network error occurred while validating Gemini API key:${e.message}"))

            }
        }



    private suspend fun localModelsKeyValidator(apiKey: String): Either<LLMValidatorErrors, Boolean> {
        val localConfig = parseLocalModelValidationPayload(apiKey)
        if (localConfig == null) {
            // Keep this fallback permissive for flows that still validate a stored LOCAL_MODEL secret directly.
            return success(apiKey.isNotBlank())
        }

        val modelsEndpoint = "${localConfig.apiBase}/models"

        return try {
            val response = client.get(modelsEndpoint)

            when (response.status) {
                HttpStatusCode.OK -> {
                    val responseBody = response.bodyAsText()
                    val modelIdPattern = Regex(""""id"\s*:\s*"${Regex.escape(localConfig.modelName)}"""")

                    if (modelIdPattern.containsMatchIn(responseBody)) {
                        success(true)
                    } else {
                        failure(
                            LLMValidatorErrors.InvalidLLM(
                                "Model '${localConfig.modelName}' was not found at LOCAL_MODEL endpoint '${localConfig.apiBase}'",
                            ),
                        )
                    }
                }

                HttpStatusCode.NotFound ->
                    failure(
                        LLMValidatorErrors.InvalidLLM(
                            "LOCAL_MODEL apiBase '${localConfig.apiBase}' is not exposing an OpenAI-compatible /models endpoint",
                        ),
                    )

                else ->
                    failure(
                        LLMValidatorErrors.InvalidLLM(
                            "LOCAL_MODEL endpoint '${localConfig.apiBase}' responded with status ${response.status.value}",
                        ),
                    )
            }
        } catch (e: Exception) {
            failure(
                LLMValidatorErrors.InvalidLLM(
                    "Could not reach LOCAL_MODEL endpoint '${localConfig.apiBase}': ${e.message}",
                ),
            )
        }
    }


    override suspend fun validateApiKeyForLLM(
        llm: LLM,
        apiKey: String?,
    ): Either<LLMValidatorErrors, Boolean> {

        val normalizedApiKey = normalizeApiKey(apiKey)
            ?: return failure(LLMValidatorErrors.InvalidKeyFormat("API key cannot be null or blank"))

        val validationResult =
            when (llm) {
                LLM.OPENAI -> openAiKeyValidator(normalizedApiKey)
                LLM.GEMINI -> geminiKeyValidator(normalizedApiKey)
                LLM.LOCAL_MODEL -> localModelsKeyValidator(normalizedApiKey)
            }

        return when (validationResult) {
            is Failure -> failure(validationResult.value)
            is Success -> success(true)
        }
    }

    private fun parseLocalModelValidationPayload(apiKey: String): LocalModelValidationPayload? {
        if (!apiKey.startsWith(LOCAL_MODEL_VALIDATION_PREFIX)) return null

        val payload = apiKey.removePrefix(LOCAL_MODEL_VALIDATION_PREFIX)
        val separatorIndex = payload.indexOf(LOCAL_MODEL_VALIDATION_SEPARATOR)
        if (separatorIndex < 0) return null

        val apiBase =
            payload.substring(0, separatorIndex)
                .trim()
                .trimEnd('/')
                .takeIf { it.isNotBlank() }
                ?: return null

        val modelName =
            payload.substring(separatorIndex + LOCAL_MODEL_VALIDATION_SEPARATOR.length)
                .trim()
                .takeIf { it.isNotBlank() }
                ?: return null

        return LocalModelValidationPayload(apiBase = apiBase, modelName = modelName)
    }


    private fun validateKeyFormat(apiKey: String, prefix: String): Boolean {
        return apiKey.startsWith(prefix)
    }

    /**
     * Função responsavél por validar se a api_key está no formato esperado ou não, retornando null caso não esteja.
     *
     * */
    private fun normalizeApiKey(apiKey: String?): String? =
        apiKey
            ?.trim()
            ?.takeIf { it.isNotBlank() && it.none(Char::isWhitespace) }



}

private data class LocalModelValidationPayload(
    val apiBase: String,
    val modelName: String,
)
