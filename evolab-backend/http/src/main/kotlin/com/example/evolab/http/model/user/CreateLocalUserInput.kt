package com.example.evolab.http.model.user

data class CreateLocalUserInput(
    val name: String,
    val email: String,
    val password: String,
)