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
import pt.isel.service.userService.UserError
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64

@Named
class TokenService(
    private val passwordEncoder: PasswordEncoder,
    private val tokenEncoder: TokenEncoder,
    private val config: UsersDomainConfig,
    private val repoUsers: RepositoryUser,
    private val repoTokens: RepositoryToken,
    private val clock: Clock,
) {
    private fun validatePassword(
        password: String,
        passwordHash: String,
    ) = passwordEncoder.matches(
        password,
        passwordHash,
    )

    fun createToken(
        email: String,
        password: String,
    ): Either<UserError, TokenExternalInfo> {
        require(email.isNotBlank()) { "Email cannot be blank" }
        require(password.isNotBlank()) { "Password cannot be blank" }

        val user = repoUsers.findByEmail(email) ?: return failure(UserError.UserNotFound)

        val userPasswordHash = user.passwordHash ?: return failure(UserError.InvalidCredentials)
        if (!validatePassword(password, userPasswordHash)) {
            return failure(UserError.InvalidCredentials)
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

    fun revokeToken(token: String): Boolean {
        val tokenValidationInfo = tokenEncoder.createValidationInformation(token)
        repoTokens.removeTokenByValidationInfo(tokenValidationInfo)
        return true
    }

    fun getUserByToken(token: String): User? {
        if (!canBeToken(token)) {
            return null
        }

        val tokenValidationInfo = tokenEncoder.createValidationInformation(token)
        val userAndToken = repoTokens.getTokenByTokenValidationInfo(tokenValidationInfo)
        return if (userAndToken != null && isTokenTimeValid(userAndToken.second)) {
            repoTokens.updateTokenLastUsed(tokenValidationInfo, clock.instant().toEpochMilli())
            userAndToken.first
        } else {
            null
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