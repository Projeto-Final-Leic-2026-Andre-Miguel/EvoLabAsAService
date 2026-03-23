package com.example.evolab.domain.LLMCredentials

import java.time.Instant

enum class LLM {
    OPENAI,
    GEMINI,
    LOCAL_MODELS
}


data class LLMCredentials(
  val id : Int,
  val userId : Int,
  val llm : LLM,
  val apiKeyEncrypted : String?,
  val createdAt : Instant
)


//id (PK)
//user_id (FK -> users.id)
//provider        (OPENAI, GEMINI, LOCAL_MODEl, ˝etc)
//api_key_encrypted
//created_at