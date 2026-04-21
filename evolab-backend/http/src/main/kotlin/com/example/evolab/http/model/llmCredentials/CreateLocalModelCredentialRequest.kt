package com.example.evolab.http.model.llmCredentials

import com.example.evolab.domain.LLMCredentials.LLM

data class CreateLocalModelCredentialRequest(
    val llm: LLM = LLM.LOCAL_MODEL,
    val apiKey: String,
    val port: Int,
    val modelName: String
)

