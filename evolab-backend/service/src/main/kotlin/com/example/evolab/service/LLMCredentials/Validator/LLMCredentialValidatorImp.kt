package com.example.evolab.service.LLMCredentials.Validator

import com.example.evolab.service.auxiliary.Either
import com.example.evolab.service.auxiliary.failure
import com.example.evolab.service.auxiliary.success
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import jakarta.inject.Named

// api key = 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

@Named
class LLMCredentialValidatorImp(
    private val client : HttpClient

) : LLMCrendentialsValidator {


    companion object {

        private const val GEMINI_ENDPOINT = "https://gemini.googleapis.com/v1Beta/models"
        private const val OPENAI_ENDPOINT = "https://api.openai.com/v1/models"


        private const val GEMINI_KEY_PREFIX = "AIza"
        private const val OPENAI_KEY_PREFIX = "sk-"

    }


    override suspend fun openAiKeyValidator(apiKey: String): Either<LLMValidatorErrors, Boolean> {


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

    override suspend fun geminiKeyValidator(apiKey: String): Either<LLMValidatorErrors, Boolean> {

        if (!validateKeyFormat(apiKey, GEMINI_KEY_PREFIX)) {
            return failure(LLMValidatorErrors.InvalidKeyFormat("Gemini API key must start with '$GEMINI_KEY_PREFIX'"))
        }

        return try {

            val response = client.get(GEMINI_ENDPOINT) {
                header("x-goog-api-key", apiKey)
            }

              when(response.status) {

                  HttpStatusCode.OK -> success(true)
                  HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> failure(LLMValidatorErrors.InvalidAPIKey("Invalid Gemini API key"))
                    else -> failure(LLMValidatorErrors.InvalidLLM("InvalidLLM specified for validation"))
                }


             } catch (e: Exception) {
                     failure(LLMValidatorErrors.UnknownError("Network error occurred while validating Gemini API key:${e.message}"))

            }
        }



    override suspend fun localModelsKeyValidator(apiKey: String): Either<LLMValidatorErrors, Boolean> {
        TODO("Not yet implemented")
    }


    private fun validateKeyFormat(apiKey: String, prefix: String): Boolean {
        return apiKey.startsWith(prefix)
    }


}