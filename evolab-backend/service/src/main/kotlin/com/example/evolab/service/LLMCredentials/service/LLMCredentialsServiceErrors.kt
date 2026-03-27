package com.example.evolab.service.LLMCredentials.service

sealed class LLMCredentialsServiceErrors {

    data class CredentialWithProviderAlreadyInUse(val message: String) : LLMCredentialsServiceErrors()

    data class InvalidLLMProvider(val message: String) : LLMCredentialsServiceErrors()

    data class InvalidApiKey(val message: String) : LLMCredentialsServiceErrors()

    data class LLMCredentialNotFound(val message: String) : LLMCredentialsServiceErrors()

    data class PersistenceError(val message: String) : LLMCredentialsServiceErrors()

}