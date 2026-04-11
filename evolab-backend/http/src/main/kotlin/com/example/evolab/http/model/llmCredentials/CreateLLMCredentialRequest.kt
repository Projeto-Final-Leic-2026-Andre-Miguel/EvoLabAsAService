package com.example.evolab.http.model.llmCredentials

import com.example.evolab.domain.LLMCredentials.LLM

data class CreateLLMCredentialRequest(
     val llm : LLM,
     val apiKey: String
)
