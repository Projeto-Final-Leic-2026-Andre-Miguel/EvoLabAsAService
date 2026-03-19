package pt.isel.repointerface

interface RepositoryToken {
    fun createToken(token: Token, maxTokens: Int)
    fun findByValidation(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>?
    fun updateTokenLastUsed(token: Token, now: Instant)
    fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int
    fun removeTokensByUserId(userId: Int): Int
}
