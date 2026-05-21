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
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import jakarta.inject.Named


@Named
class LLMCredentialValidatorImp(
    private val client : HttpClient
) : LLMCrendentialsValidator {


    companion object {
        private const val GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val OPENAI_ENDPOINT = "https://api.openai.com/v1/models"
        private const val ANTHROPIC_ENDPOINT = "https://api.anthropic.com/v1/messages"
        private const val ANTHROPIC_VERSION = "2023-06-01"


        private const val GEMINI_KEY_PREFIX = "AIza"
        private const val OPENAI_KEY_PREFIX = "sk-"
        private const val ANTHROPIC_KEY_PREFIX = "sk-ant-"

    }

    private suspend fun anthropicKeyValidator(apiKey: String): Either<LLMValidatorErrors, Boolean> {
        if (!validateKeyFormat(apiKey, ANTHROPIC_KEY_PREFIX)) {
            return failure(LLMValidatorErrors.InvalidKeyFormat("Anthropic API key must start with '$ANTHROPIC_KEY_PREFIX'"))
        }

        return try {
            val response = client.post(ANTHROPIC_ENDPOINT) {
                header("x-api-key", apiKey)
                header("anthropic-version", ANTHROPIC_VERSION)
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                      "model": "claude-3-haiku-20240307",
                      "max_tokens": 1,
                      "messages": [
                        { "role": "user", "content": "Hello world!" }
                      ]
                    }
                    """.trimIndent(),
                )
            }

            when (response.status) {
                HttpStatusCode.OK -> success(true)
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> failure(LLMValidatorErrors.InvalidAPIKey("Invalid Anthropic API key"))
                HttpStatusCode.BadRequest -> failure(LLMValidatorErrors.InvalidAPIKey("Anthropic API key could not be validated"))
                else -> failure(LLMValidatorErrors.InvalidLLM("InvalidLLM specified for validation"))
            }
        } catch (e: Exception) {
            failure(LLMValidatorErrors.UnknownError("Network error occurred while validating Anthropic API key: ${e.message}"))
        }
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





    override suspend fun validateLocalModelApiKey(port: Int, apiKey: String, modelName: String?): Either<LLMValidatorErrors, Boolean> {
        val urls =
            listOf(
                "http://localhost:$port/v1/models",
                "http://127.0.0.1:$port/v1/models",
                "http://host.docker.internal:$port/v1/models",
            )

        val failures = mutableListOf<String>()
        var unauthorized = false

        for (url in urls) {
            try {
                val response = client.get(url) {
                    if (apiKey.isNotBlank() && apiKey != "dummy") {
                        header("Authorization", "Bearer $apiKey")
                    }
                }

                when (response.status) {
                    HttpStatusCode.OK -> {
                        if (modelName != null && modelName.isNotBlank()) {
                            val body = response.bodyAsText()
                            if (!body.contains(modelName)) {
                                failures += "$url: model '$modelName' not found"
                                continue
                            }
                        }
                        return success(true)
                    }
                    HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> {
                        unauthorized = true
                        failures += "$url: ${response.status}"
                    }
                    else -> failures += "$url: ${response.status}"
                }
            } catch (e: Exception) {
                failures += "$url: ${e.message ?: e::class.simpleName ?: "unreachable"}"
            }
        }

        val details = failures.joinToString("; ")
        return if (unauthorized) {
            failure(LLMValidatorErrors.InvalidAPIKey("Local model endpoint rejected the configured API key. Tried: $details"))
        } else {
            failure(LLMValidatorErrors.InvalidLLM("Could not validate local model on port $port. Tried: $details"))
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
                LLM.ANTHROPIC -> anthropicKeyValidator(normalizedApiKey)
                LLM.LOCAL_MODEL -> failure(LLMValidatorErrors.InvalidLLM("Local models must be validated using validateLocalModelApiKey"))
            }

        return when (validationResult) {
            is Failure -> failure(validationResult.value)
            is Success -> success(true)
        }
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
