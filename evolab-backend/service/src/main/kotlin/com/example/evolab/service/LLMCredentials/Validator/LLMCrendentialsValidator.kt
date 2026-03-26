package com.example.evolab.service.LLMCredentials.Validator

import com.example.evolab.service.auxiliary.Either

/**
 * This interface defines the contract for validating API keys for different Large Language Models (LLMs).
 * It includes methods for validating API keys for OpenAI, Gemini, and Local Models.
 */


interface LLMCrendentialsValidator {

    suspend fun openAiKeyValidator(apiKey: String): Either<LLMValidatorErrors, Boolean>

    suspend fun geminiKeyValidator(apiKey: String): Either<LLMValidatorErrors, Boolean>

    suspend fun localModelsKeyValidator(apiKey: String): Either<LLMValidatorErrors, Boolean>


}