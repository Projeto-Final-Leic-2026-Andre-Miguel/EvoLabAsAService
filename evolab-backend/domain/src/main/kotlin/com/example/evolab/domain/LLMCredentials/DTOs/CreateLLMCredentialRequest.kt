package com.example.evolab.domain.LLMCredentials.DTOs

import com.example.evolab.domain.LLMCredentials.LLM

data class CreateLLMCredentialRequest(
    val llm: LLM,
    val apiKey: String
)
