package pt.isel.service.userService

import com.example.evolab.domain.user.User
import com.example.evolab.domain.user.UsersDomainConfig
import com.example.evolab.repo.repoUser.RepositoryUser
import com.example.evolab.service.auxiliary.Either
import com.example.evolab.service.auxiliary.failure
import com.example.evolab.service.auxiliary.success
import jakarta.inject.Named
import org.springframework.security.crypto.password.PasswordEncoder
import pt.isel.domain.authentication.PasswordValidationInfo
import pt.isel.domain.token.TokenEncoder
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64.getUrlDecoder
import java.util.Base64.getUrlEncoder

@Named
class UserAuthService(
    private val passwordEncoder: PasswordEncoder,
    private val tokenEncoder: TokenEncoder,
    private val config: UsersDomainConfig,
    private val repoUsers: RepositoryUser,
    private val clock: Clock,
) {
    private fun validatePassword(
        password: String,
        validationInfo: PasswordValidationInfo,
    ) = passwordEncoder.matches(
        password,
        validationInfo.validationInfo,
    )

    private fun createPasswordValidationInformation(password: String) =
        PasswordValidationInfo(
            validationInfo = passwordEncoder.encode(password),
        )

    // TODO it could be better
    fun isSafePassword(password: String) = password.length > 4

    /**
     *
     */

    fun createLocalUser(
        name: String,
        email: String,
        password: String
    ): Either<UserError, User> {

        TODO("Criar lógica associada à criação de um user Local")


    }

    fun createOAuthUser(
    ): Either<UserError, User> {

        TODO("Criar lógica associada à criação de um user com o OAuth," +
                "aqui temos de validar que informações guardamos porque não vamos ter acesso nem ao email " +
                "nem à palavra-passe portanto é necessário ver o repoUserJdbi para perceber como vamos guardar informações." +
                "")


    }

    fun createUser(
        name: String,
        email: String,
        password: String,
    ): Either<UserError, User> {
        if (!isSafePassword(password)) {
            return failure(UserError.InsecurePassword)
        }
        if (repoUsers.findByEmail(email) != null) {
            return failure(UserError.AlreadyUsedEmailAddress)
        }


            val passwordValidationInfo = createPasswordValidationInformation(password)
            val newUser = repoUsers.createUser(name, email, passwordValidationInfo, invitationCode)


        return success(user)
    }

    fun deleteUser(userId: Int): Either<UserError, Boolean> {
        val user = repoUsers.findById(userId) ?: return failure(UserError.UserNotFound)
        val deleted = repoUsers.deleteById(user.id)
        return if (deleted) success(true) else failure(UserError.ErrorDeletingUser)
    }

    fun getAllUsers(): List<User> = repoUsers.findAll()

    fun createToken(
        email: String,
        password: String,
    ): Either<UserError, TokenExternalInfo> { // TO DO: Replace by Either
        require(email.isNotBlank()) { "Email cannot be blank" } // Replace by Either.Failure
        require(password.isNotBlank()) { "Password cannot be blank" } // Replace by Either.Failure

        val user =
            repoUsers.findByEmail(email) ?: return failure(UserError.UserNotFound) // Replace by Either.Failure

        if (!validatePassword(password, user.passwordValidation)) {
            return failure(UserError.InvalidCredentials) // Replace by Either.Failure
        }
        val tokenValue = generateTokenValue()
        val now = clock.instant()
        val newToken =
            Token(
                tokenEncoder.createValidationInformation(tokenValue),
                user.id,
                createdAt = now,
                lastUsedAt = now,
            )
        repoUsers.createToken(newToken, config.maxTokensPerUser)
        return success(
            TokenExternalInfo(
                tokenValue,
                getTokenExpiration(newToken),
            ),
        )
    }

    fun revokeToken(token: String): Boolean {
        val tokenValidationInfo = tokenEncoder.createValidationInformation(token)
        repoUsers.removeTokenByValidationInfo(tokenValidationInfo)
        return true
    }

    fun getUserByToken(token: String): User? {
        if (!canBeToken(token)) {
            return null
        }
        val tokenValidationInfo = tokenEncoder.createValidationInformation(token)

        val userAndToken: Pair<User, Token>? = repoUsers.getTokenByTokenValidationInfo(tokenValidationInfo)
        return if (userAndToken != null && isTokenTimeValid(clock, userAndToken.second)) {
            repoUsers.updateTokenLastUsed(userAndToken.second, clock.instant())
            userAndToken.first
        } else {
            null
        }
    }

    private fun canBeToken(token: String): Boolean =
        try {
            getUrlDecoder().decode(token).size == config.tokenSizeInBytes
        } catch (ex: IllegalArgumentException) {
            false
        }

    private fun isTokenTimeValid(
        clock: Clock,
        token: Token,
    ): Boolean {
        val now = clock.instant()
        return token.createdAt <= now &&
            Duration.between(now, token.createdAt) <= config.tokenTtl &&
            Duration.between(now, token.lastUsedAt) <= config.tokenRollingTtl
    }

    private fun generateTokenValue(): String =
        ByteArray(config.tokenSizeInBytes).let { byteArray ->
            SecureRandom.getInstanceStrong().nextBytes(byteArray)
            getUrlEncoder().encodeToString(byteArray)
        }

    private fun getTokenExpiration(token: Token): Instant {
        val absoluteExpiration = token.createdAt + config.tokenTtl
        val rollingExpiration = token.lastUsedAt + config.tokenRollingTtl
        return if (absoluteExpiration < rollingExpiration) {
            absoluteExpiration
        } else {
            rollingExpiration
        }
    }
}
