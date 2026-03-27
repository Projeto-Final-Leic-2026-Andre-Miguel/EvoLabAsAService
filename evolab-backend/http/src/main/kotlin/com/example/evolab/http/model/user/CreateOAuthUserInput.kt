package com.example.evolab.http.model.user

import com.example.evolab.domain.user.AuthProvider

data class CreateOAuthUserInput(
    val name: String,
    val email: String,
    val provider: AuthProvider,
    val providerId: String,
)