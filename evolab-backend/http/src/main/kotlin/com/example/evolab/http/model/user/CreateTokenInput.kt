package com.example.evolab.http.model.user

data class CreateTokenInput(
    val email: String,
    val password: String,
)
