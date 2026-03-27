package com.example.evolab.service.LLMCredentials.Validator

sealed class LLMValidatorErrors {

    data class InvalidAPIKey(val message: String) : LLMValidatorErrors()

    data class InvalidLLM(val message: String) : LLMValidatorErrors()

    data class InvalidKeyFormat(val message: String) : LLMValidatorErrors()


    data class UnsupportedLLM(val message: String) : LLMValidatorErrors()

    data class UnknownError(val message: String) : LLMValidatorErrors()



}