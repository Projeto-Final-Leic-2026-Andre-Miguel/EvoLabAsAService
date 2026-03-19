package pt.isel.repo.repoToken

import pt.isel.domain.Token
import pt.isel.domain.User
import pt.isel.repo.Repository
import kotlin.time.Instant

interface RepositoryToken : Repository<Token> {
    fun createToken(token: Token, maxTokens: Int)

    fun findByValidation(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>?

    fun updateTokenLastUsed(token: Token, now: Instant)

    fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int

    fun removeTokensByUserId(userId: Int): Int
}