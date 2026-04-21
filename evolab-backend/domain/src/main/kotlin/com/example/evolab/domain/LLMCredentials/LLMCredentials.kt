package com.example.evolab.domain.LLMCredentials

import java.time.Instant



data class LLMCredentials(
  val id : Int,
  val userId : Int,
  val llm : LLM,
  val apiKeyEncrypted : String,
  val createdAt : Instant
)

data class LocalModelCredentials(
  val id : Int,
  val userId : Int,
  val llm : LLM,
  val apiKeyEncrypted : String,
  val port : Int,
  val modelName : String,
  val createdAt : Instant
)
