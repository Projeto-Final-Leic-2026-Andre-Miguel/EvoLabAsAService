package pt.isel.repo.repoToken

import org.jdbi.v3.core.Handle
import pt.isel.domain.Token
import pt.isel.domain.User
import kotlin.time.Instant


class RepositoryTokenJdbi (
    private val handle: Handle,
) : RepositoryToken{

    override fun createToken(
        token: Token,
        maxTokens: Int,
    ) = TODO("Implement token creation with max tokens policy")

    override fun findByValidation(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>? =
        TODO("Implement token lookup by validation info")

    override fun updateTokenLastUsed(token: Token, now: Instant) = TODO("Implement token last_used_at update")

    override fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int =
        TODO("Implement token delete by validation info")

    override fun removeTokensByUserId(userId: Int): Int = TODO("Implement token delete by user id")

}