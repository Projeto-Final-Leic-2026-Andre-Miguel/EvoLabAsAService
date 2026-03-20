package com.example.evolab.domain.token

/**
 * Strongly typed information of token hashed by a TokenEncoder.
 */
data class TokenValidationInfo(
    val validationInfo: String,
)
