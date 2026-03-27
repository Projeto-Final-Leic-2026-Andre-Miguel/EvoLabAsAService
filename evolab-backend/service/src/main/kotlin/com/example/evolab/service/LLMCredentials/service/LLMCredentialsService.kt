package com.example.evolab.service.LLMCredentials.service

import com.example.evolab.domain.LLMCredentials.LLM
import com.example.evolab.domain.LLMCredentials.LLMCredentials
import com.example.evolab.service.auxiliary.Either


/**
 * Service interface for managing LLM credentials.
 * This service provides methods to create, retrieve, update and delete LLM credentials for users.
 *
 */


interface LLMCredentialsService {

   suspend fun createLLMCredential(
        userId: Int,
        llm: LLM,
        apiKey: String?

    ): Either<LLMCredentialsServiceErrors, LLMCredentials>

    fun getLLMCredentialById(id: Int): Either<LLMCredentialsServiceErrors, LLMCredentials>

    fun getLLMCredentialsByUserId(userId: Int): Either<LLMCredentialsServiceErrors,List<LLMCredentials>>

    fun updateLLMCredential(
        id: Int,
        llm: LLM,
        apiKey: String?
    ): Either<LLMCredentialsServiceErrors, LLMCredentials>

    fun deleteLLMCredential(id: Int): Either<LLMCredentialsServiceErrors, LLMCredentials>
}