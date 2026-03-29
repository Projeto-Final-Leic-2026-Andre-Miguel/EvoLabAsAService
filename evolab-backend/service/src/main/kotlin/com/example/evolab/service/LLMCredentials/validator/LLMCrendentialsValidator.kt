package com.example.evolab.service.LLMCredentials.validator

import com.example.evolab.domain.LLMCredentials.LLM
import com.example.evolab.service.auxiliary.Either

/**
 * This interface defines the contract for validating API keys for different Large Language Models (LLMs).
 * It includes methods for validating API keys for OpenAI, Gemini, and Local Models.
 */


interface LLMCrendentialsValidator {

    suspend fun validateApiKeyForLLM(llm: LLM, apiKey: String?): Either<LLMValidatorErrors, Boolean>


}