package pt.isel.service.userService

import com.example.evolab.domain.user.AuthProvider
import com.example.evolab.domain.user.User
import com.example.evolab.repo.repoUser.RepositoryUser
import com.example.evolab.service.auxiliary.Either
import com.example.evolab.service.auxiliary.failure
import com.example.evolab.service.auxiliary.success
import jakarta.inject.Named
import org.springframework.security.crypto.password.PasswordEncoder
import pt.isel.domain.authentication.PasswordValidationInfo

@Named
class UserAuthService(
    private val passwordEncoder: PasswordEncoder,
    private val repoUsers: RepositoryUser,
) {
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
        if (!isSafePassword(password)) {
            return failure(UserError.InsecurePassword)
        }
        if (repoUsers.findByEmail(email) != null) {
            return failure(UserError.AlreadyUsedEmailAddress)
        }

        val passwordValidationInfo = createPasswordValidationInformation(password)
        return try {
            val newUser = repoUsers.createLocalUser(name, email, passwordValidationInfo.validationInfo)
            success(newUser)
        } catch (e: Exception) {
            failure(UserError.UnexpectedError)
        }
    }


    fun createOAuthUser(
        name: String,
        email: String,
        provider: AuthProvider,
        providerId: String,
    ): Either<UserError, User> {
        if (repoUsers.findByEmail(email) != null) {
            return failure(UserError.AlreadyUsedEmailAddress)
        }

        val existingOAuth = repoUsers.findByProvider(provider, providerId)
        if (existingOAuth != null) {
            return success(existingOAuth)
        }

        return try {
            val newUser = repoUsers.createOAuthUser(name, email, provider, providerId)
            success(newUser)
        } catch (e: Exception) {
            failure(UserError.UnexpectedError)
        }
    }


    fun deleteUser(userId: Int): Either<UserError, Boolean> {
        val user = repoUsers.findById(userId) ?: return failure(UserError.UserNotFound)
        val deleted = repoUsers.deleteById(user.id)
        return if (deleted) success(true) else failure(UserError.ErrorDeletingUser)
    }

    fun getAllUsers(): List<User> = repoUsers.findAll()

}