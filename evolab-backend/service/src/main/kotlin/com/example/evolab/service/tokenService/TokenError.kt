package com.example.evolab.service.tokenService

sealed class TokenError {
    data object InvalidCredentials : TokenError()

    data object InvalidTokenFormat : TokenError()

    data object TokenNotFound : TokenError()

    data object TokenExpired : TokenError()
}
