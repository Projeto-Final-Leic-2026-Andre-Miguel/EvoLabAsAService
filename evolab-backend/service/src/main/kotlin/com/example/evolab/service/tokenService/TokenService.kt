package com.example.evolab.service.tokenService

import com.example.evolab.domain.user.User
import com.example.evolab.service.auxiliary.Either
import pt.isel.domain.token.TokenExternalInfo
 
interface TokenService {
    fun createToken(
        email: String,
        password: String,
    ): Either<TokenError, TokenExternalInfo>

    fun revokeToken(token: String): Either<TokenError, Boolean>

    fun getUserByToken(token: String): Either<TokenError, User>
}