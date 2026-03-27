package com.example.evolab.service.tokenService

import com.example.evolab.domain.token.Token
import com.example.evolab.domain.user.User
import com.example.evolab.domain.user.UsersDomainConfig
import com.example.evolab.repo.repoToken.RepositoryToken
import com.example.evolab.repo.repoUser.RepositoryUser
import com.example.evolab.service.auxiliary.Either
import com.example.evolab.service.auxiliary.failure
import com.example.evolab.service.auxiliary.success
import jakarta.inject.Named
import org.springframework.security.crypto.password.PasswordEncoder
import pt.isel.domain.token.TokenEncoder
import pt.isel.domain.token.TokenExternalInfo
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64

@Named
class TokenServiceImpl(
    private val passwordEncoder: PasswordEncoder,
    private val tokenEncoder: TokenEncoder,
    private val config: UsersDomainConfig,
    private val repoUsers: RepositoryUser,
    private val repoTokens: RepositoryToken,
    private val clock: Clock,
) : TokenService {
    private fun validatePassword(
        password: String,
        passwordHash: String,
    ) = passwordEncoder.matches(
        password,
        passwordHash,
    )

    override fun createToken(
        email: String,
        password: String,
    ): Either<TokenError, TokenExternalInfo> {
        if (email.isBlank() || password.isBlank()) {
            return failure(TokenError.InvalidCredentials)
        }

        val user = repoUsers.findByEmail(email) ?: return failure(TokenError.InvalidCredentials)

        val userPasswordHash = user.passwordHash ?: return failure(TokenError.InvalidCredentials)
        if (!validatePassword(password, userPasswordHash)) {
            return failure(TokenError.InvalidCredentials)
        }

        val tokenValue = generateTokenValue()
        val now = clock.instant().toEpochMilli()
        val newToken =
            Token(
                tokenValidation = tokenEncoder.createValidationInformation(tokenValue).validationInfo,
                userId = user.id,
                createdAt = now,
                lastUsedAt = now,
            )

        repoTokens.createToken(newToken, config.maxTokensPerUser)
        return success(TokenExternalInfo(tokenValue, getTokenExpiration(newToken)))
    }

    override fun revokeToken(token: String): Either<TokenError, Boolean> {
        if (!canBeToken(token)) {
            return failure(TokenError.InvalidTokenFormat)
        }

        val tokenValidationInfo = tokenEncoder.createValidationInformation(token)
        val removed = repoTokens.removeTokenByValidationInfo(tokenValidationInfo)
        return if (removed > 0) success(true) else failure(TokenError.TokenNotFound)
    }

    override fun getUserByToken(token: String): Either<TokenError, User> {
        if (!canBeToken(token)) {
            return failure(TokenError.InvalidTokenFormat)
        }

        val tokenValidationInfo = tokenEncoder.createValidationInformation(token)
        val userAndToken =
            repoTokens.getTokenByTokenValidationInfo(tokenValidationInfo)
                ?: return failure(TokenError.TokenNotFound)

        return if (isTokenTimeValid(userAndToken.second)) {
            repoTokens.updateTokenLastUsed(tokenValidationInfo, clock.instant().toEpochMilli())
            success(userAndToken.first)
        } else {
            failure(TokenError.TokenExpired)
        }
    }

    private fun canBeToken(token: String): Boolean =
        try {
            Base64.getUrlDecoder().decode(token).size == config.tokenSizeInBytes
        } catch (_: IllegalArgumentException) {
            false
        }

    private fun isTokenTimeValid(token: Token): Boolean {
        val now = clock.instant()
        val createdAt = Instant.ofEpochMilli(token.createdAt)
        val lastUsedAt = Instant.ofEpochMilli(token.lastUsedAt)
        return createdAt <= now &&
            lastUsedAt <= now &&
            Duration.between(createdAt, now) <= config.tokenTtl &&
            Duration.between(lastUsedAt, now) <= config.tokenRollingTtl
    }

    private fun generateTokenValue(): String =
        ByteArray(config.tokenSizeInBytes).let { byteArray ->
            SecureRandom.getInstanceStrong().nextBytes(byteArray)
            Base64.getUrlEncoder().encodeToString(byteArray)
        }

    private fun getTokenExpiration(token: Token): Instant {
        val absoluteExpiration = Instant.ofEpochMilli(token.createdAt).plus(config.tokenTtl)
        val rollingExpiration = Instant.ofEpochMilli(token.lastUsedAt).plus(config.tokenRollingTtl)
        return minOf(absoluteExpiration, rollingExpiration)
    }
}

