package com.example.evolab.service.LLMCredentials

import com.example.evolab.domain.LLMCredentials.LLM
import com.example.evolab.domain.LLMCredentials.LLMCredentials


/**
 * Service interface for managing LLM credentials.
 * This service provides methods to create, retrieve, update and delete LLM credentials for users.
 *
 */


interface LLMCredentialsService {

    fun createLLMCredential(
        userId: Int,
        llm: LLM,
        apiKey: String?
    ): Int

    fun getLLMCredentialById(id: Int): LLMCredentials?

    fun getLLMCredentialsByUserId(userId: Int): List<LLMCredentials>

    fun updateLLMCredential(
        id: Int,
        llm: LLM,
        apiKey: String?
    ): Boolean

    fun deleteLLMCredential(id: Int): Boolean
}