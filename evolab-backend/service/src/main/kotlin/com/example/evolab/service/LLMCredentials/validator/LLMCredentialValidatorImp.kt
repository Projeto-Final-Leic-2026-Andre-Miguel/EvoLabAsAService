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
import io.ktor.http.HttpStatusCode
import jakarta.inject.Named


@Named
class LLMCredentialValidatorImp(
    private val client : HttpClient
) : LLMCrendentialsValidator {


    companion object {

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
        TODO("Not yet implemented")
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