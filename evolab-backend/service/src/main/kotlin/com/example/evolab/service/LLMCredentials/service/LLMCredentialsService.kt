package com.example.evolab.service.LLMCredentials.service

import com.example.evolab.domain.LLMCredentials.LLM
import com.example.evolab.domain.LLMCredentials.LLMCredentials
import com.example.evolab.domain.LLMCredentials.LocalModelCredentials
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
        apiKey: String

    ): Either<LLMCredentialsServiceErrors, LLMCredentials>

    suspend fun createLocalModelCredential(
        userId: Int,
        apiKey: String,
        port: Int,
        modelName: String?
    ): Either<LLMCredentialsServiceErrors, LocalModelCredentials>

    fun getLLMCredentialById(userId: Int, id: Int): Either<LLMCredentialsServiceErrors, LLMCredentials>

    fun getLocalModelCredentialById(userId: Int, id: Int): Either<LLMCredentialsServiceErrors, LocalModelCredentials>

    fun getLLMCredentialsByUserId(userId: Int): Either<LLMCredentialsServiceErrors,List<LLMCredentials>>

    fun getAllLLMCredentials(): Either<LLMCredentialsServiceErrors, List<LLMCredentials>>

    suspend fun validateCredential(userId: Int, id: Int): Either<LLMCredentialsServiceErrors, Boolean>

    suspend fun updateLLMCredential(
        id: Int,
        userId: Int,
        apiKey: String
    ): Either<LLMCredentialsServiceErrors, LLMCredentials>

    fun deleteLLMCredential(userId: Int, id: Int): Either<LLMCredentialsServiceErrors, Int>
}