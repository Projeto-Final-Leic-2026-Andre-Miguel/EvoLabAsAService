package com.example.evolab.domain.token

data class Token(
	val tokenValidation: String,
	val userId: Int,
	val createdAt: Long,
	val lastUsedAt: Long,
)