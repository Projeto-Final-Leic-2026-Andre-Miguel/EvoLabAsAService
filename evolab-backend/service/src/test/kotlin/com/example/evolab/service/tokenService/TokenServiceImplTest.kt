/*package com.example.evolab.service.tokenService

import com.example.evolab.domain.token.Token
import com.example.evolab.domain.token.TokenValidationInfo
import com.example.evolab.domain.user.AuthProvider
import com.example.evolab.domain.user.User
import com.example.evolab.domain.user.UsersDomainConfig
import com.example.evolab.repo.repoToken.RepositoryToken
import com.example.evolab.repo.repoUser.RepositoryUser
import com.example.evolab.service.auxiliary.Either
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import pt.isel.domain.token.TokenEncoder
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64

class TokenServiceImplTest {
    private val fixedInstant = Instant.parse("2026-03-30T12:00:00Z")
    private val clock: Clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)
    private val config = UsersDomainConfig(tokenSizeInBytes = 8, tokenTtl = Duration.ofHours(24), tokenRollingTtl = Duration.ofHours(1), maxTokensPerUser = 3)
    private val encoder = SimplePasswordEncoder()
    private val tokenEncoder = FakeTokenEncoder()

    @Test
    fun createTokenRejectsBlankCredentials() {
        val service = TokenServiceImpl(encoder, tokenEncoder, config, FakeRepositoryUser(), FakeRepositoryToken(), clock)

        val result = service.createToken("", "")

        assertLeftEquals(result, TokenError.InvalidCredentials)
    }

    @Test
    fun createTokenRejectsMissingUser() {
        val service = TokenServiceImpl(encoder, tokenEncoder, config, FakeRepositoryUser(), FakeRepositoryToken(), clock)

        val result = service.createToken("missing@test.dev", "pass")

        assertLeftEquals(result, TokenError.InvalidCredentials)
    }

    @Test
    fun createTokenRejectsUserWithoutPasswordHash() {
        val users = FakeRepositoryUser()
        users.upsert(
            User(1, "oauth", "oauth@test.dev", null, AuthProvider.GOOGLE, "google-1", fixedInstant),
        )
        val service = TokenServiceImpl(encoder, tokenEncoder, config, users, FakeRepositoryToken(), clock)

        val result = service.createToken("oauth@test.dev", "pass")

        assertLeftEquals(result, TokenError.InvalidCredentials)
    }

    @Test
    fun createTokenCreatesTokenWhenCredentialsAreValid() {
        val users = FakeRepositoryUser()
        users.upsert(User(1, "u", "u@test.dev", "enc:12345", AuthProvider.LOCAL, null, fixedInstant))
        val tokens = FakeRepositoryToken()
        val service = TokenServiceImpl(encoder, tokenEncoder, config, users, tokens, clock)

        val result = service.createToken("u@test.dev", "12345")
        val external = assertRight(result)

        assertNotNull(tokens.lastCreatedToken)
        assertEquals(1, tokens.lastCreatedToken!!.userId)
        assertEquals(fixedInstant.plus(config.tokenRollingTtl), external.tokenExpiration)
    }

    @Test
    fun revokeTokenRejectsInvalidFormat() {
        val service = TokenServiceImpl(encoder, tokenEncoder, config, FakeRepositoryUser(), FakeRepositoryToken(), clock)

        val result = service.revokeToken("not-base64")

        assertLeftEquals(result, TokenError.InvalidTokenFormat)
    }

    @Test
    fun revokeTokenReturnsNotFoundWhenNothingRemoved() {
        val service = TokenServiceImpl(encoder, tokenEncoder, config, FakeRepositoryUser(), FakeRepositoryToken(), clock)

        val result = service.revokeToken(validToken(config.tokenSizeInBytes))

        assertLeftEquals(result, TokenError.TokenNotFound)
    }

    @Test
    fun getUserByTokenReturnsExpiredWhenTokenOutsideTtl() {
        val users = FakeRepositoryUser()
        val tokens = FakeRepositoryToken()
        val raw = validToken(config.tokenSizeInBytes)
        val info = tokenEncoder.createValidationInformation(raw)
        users.tokensToUsers[info.validationInfo] = User(1, "u", "u@test.dev", "enc:1", AuthProvider.LOCAL, null, fixedInstant)
        tokens.tokensByValidation[info.validationInfo] =
            Token(
                tokenValidation = info.validationInfo,
                userId = 1,
                createdAt = fixedInstant.minus(Duration.ofDays(3)).toEpochMilli(),
                lastUsedAt = fixedInstant.minus(Duration.ofDays(3)).toEpochMilli(),
            )

        val service = TokenServiceImpl(encoder, tokenEncoder, config, users, tokens, clock)
        val result = service.getUserByToken(raw)

        assertLeftEquals(result, TokenError.TokenExpired)
    }

    @Test
    fun getUserByTokenUpdatesLastUsedWhenValid() {
        val users = FakeRepositoryUser()
        val tokens = FakeRepositoryToken()
        val raw = validToken(config.tokenSizeInBytes)
        val info = tokenEncoder.createValidationInformation(raw)
        val user = User(1, "u", "u@test.dev", "enc:1", AuthProvider.LOCAL, null, fixedInstant)
        users.tokensToUsers[info.validationInfo] = user
        tokens.tokensByValidation[info.validationInfo] =
            Token(
                tokenValidation = info.validationInfo,
                userId = 1,
                createdAt = fixedInstant.minus(Duration.ofMinutes(10)).toEpochMilli(),
                lastUsedAt = fixedInstant.minus(Duration.ofMinutes(5)).toEpochMilli(),
            )

        val service = TokenServiceImpl(encoder, tokenEncoder, config, users, tokens, clock)
        val result = service.getUserByToken(raw)

        val foundUser = assertRight(result)
        assertEquals(user.id, foundUser.id)
        assertEquals(fixedInstant.toEpochMilli(), tokens.lastUpdatedLastUsed)
    }

    private fun validToken(size: Int): String =
        Base64.getUrlEncoder().encodeToString(ByteArray(size) { 7 })

    private fun <L, R> assertRight(result: Either<L, R>): R {
        assertTrue(result is Either.Right)
        return (result as Either.Right).value
    }

    private fun <L, R> assertLeftEquals(result: Either<L, R>, expected: L) {
        assertTrue(result is Either.Left)
        assertEquals(expected, (result as Either.Left).value)
    }
}

private class SimplePasswordEncoder : PasswordEncoder {
    override fun encode(rawPassword: CharSequence): String = "enc:$rawPassword"

    override fun matches(rawPassword: CharSequence, encodedPassword: String): Boolean =
        encodedPassword == "enc:$rawPassword"
}

private class FakeTokenEncoder : TokenEncoder {
    override fun createValidationInformation(token: String): TokenValidationInfo =
        TokenValidationInfo("v:$token")
}

private class FakeRepositoryUser : RepositoryUser {
    private val usersByEmail = mutableMapOf<String, User>()
    val tokensToUsers = mutableMapOf<String, User>()

    fun upsert(user: User) {
        usersByEmail[user.email] = user
    }

    override fun createLocalUser(name: String, email: String, passwordHash: String): User =
        error("not needed in token tests")

    override fun createOAuthUser(name: String, email: String, provider: AuthProvider, providerId: String): User =
        error("not needed in token tests")

    override fun findByEmail(email: String): User? = usersByEmail[email]

    override fun findByProvider(provider: AuthProvider, providerId: String): User? = null

    override fun findByTokenValidation(tokenValidationInfo: TokenValidationInfo): User? =
        tokensToUsers[tokenValidationInfo.validationInfo]

    override fun count(): Long = usersByEmail.size.toLong()

    override fun findById(id: Int): User? = usersByEmail.values.firstOrNull { it.id == id }

    override fun findAll(): List<User> = usersByEmail.values.toList()

    override fun save(entity: User) {
        usersByEmail[entity.email] = entity
    }

    override fun deleteById(id: Int): Boolean = false

    override fun clear() {
        usersByEmail.clear()
        tokensToUsers.clear()
    }
}

private class FakeRepositoryToken : RepositoryToken {
    val tokensByValidation = mutableMapOf<String, Token>()
    var lastCreatedToken: Token? = null
    var lastUpdatedLastUsed: Long? = null

    override fun createToken(token: Token, maxTokens: Int) {
        lastCreatedToken = token
        tokensByValidation[token.tokenValidation] = token
    }

    override fun findByTokenValidation(tokenValidation: TokenValidationInfo): Token? =
        tokensByValidation[tokenValidation.validationInfo]

    override fun findAllByUserId(userId: Int): List<Token> =
        tokensByValidation.values.filter { it.userId == userId }

    override fun getTokenByTokenValidationInfo(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>? = null

    override fun updateTokenLastUsed(tokenValidationInfo: TokenValidationInfo, now: Long) {
        lastUpdatedLastUsed = now
        val existing = tokensByValidation[tokenValidationInfo.validationInfo] ?: return
        tokensByValidation[tokenValidationInfo.validationInfo] = existing.copy(lastUsedAt = now)
    }

    override fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int {
        return if (tokensByValidation.remove(tokenValidationInfo.validationInfo) != null) 1 else 0
    }
}

*/