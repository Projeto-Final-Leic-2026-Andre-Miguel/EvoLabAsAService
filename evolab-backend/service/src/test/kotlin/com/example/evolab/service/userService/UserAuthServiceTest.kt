/*package com.example.evolab.service.userService

import com.example.evolab.domain.token.TokenValidationInfo
import com.example.evolab.domain.user.AuthProvider
import com.example.evolab.domain.user.User
import com.example.evolab.repo.repoUser.RepositoryUser
import com.example.evolab.service.auxiliary.Either
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import pt.isel.service.userService.UserAuthService
import pt.isel.service.userService.UserError
import java.time.Instant

class UserAuthServiceTest {
    private val encoder = SimplePasswordEncoder()

    @Test
    fun createLocalUserRejectsInsecurePassword() {
        val repo = FakeRepositoryUser()
        val service = UserAuthService(encoder, repo)

        val result = service.createLocalUser("n", "a@test.dev", "1234")

        assertLeftEquals(result, UserError.InsecurePassword)
    }

    @Test
    fun createLocalUserRejectsDuplicateEmail() {
        val repo = FakeRepositoryUser()
        repo.createLocalUser("existing", "dup@test.dev", "hash")
        val service = UserAuthService(encoder, repo)

        val result = service.createLocalUser("n", "dup@test.dev", "12345")

        assertLeftEquals(result, UserError.AlreadyUsedEmailAddress)
    }

    @Test
    fun createLocalUserCreatesUserWithEncodedPassword() {
        val repo = FakeRepositoryUser()
        val service = UserAuthService(encoder, repo)

        val result = service.createLocalUser("new", "new@test.dev", "12345")
        val user = assertRight(result)

        assertEquals("new", user.name)
        assertEquals("new@test.dev", user.email)
        assertNotNull(user.passwordHash)
        assertTrue(user.passwordHash!!.startsWith("enc:"))
    }

    @Test
    fun createLocalUserReturnsUnexpectedErrorOnRepositoryFailure() {
        val repo = FakeRepositoryUser(throwOnCreateLocal = true)
        val service = UserAuthService(encoder, repo)

        val result = service.createLocalUser("n", "x@test.dev", "12345")

        assertLeftEquals(result, UserError.UnexpectedError)
    }

    @Test
    fun createOAuthUserReturnsExistingUserWhenProviderAlreadyExists() {
        val repo = FakeRepositoryUser()
        val existing = repo.createOAuthUser("oauth", "oauth@test.dev", AuthProvider.GOOGLE, "google-1")
        val service = UserAuthService(encoder, repo)

        val result = service.createOAuthUser("new", "other@test.dev", AuthProvider.GOOGLE, "google-1")
        val user = assertRight(result)

        assertEquals(existing.id, user.id)
        assertEquals(existing.providerId, user.providerId)
    }

    @Test
    fun deleteUserReturnsNotFoundWhenUserDoesNotExist() {
        val repo = FakeRepositoryUser()
        val service = UserAuthService(encoder, repo)

        val result = service.deleteUser(9999)

        assertLeftEquals(result, UserError.UserNotFound)
    }

    @Test
    fun deleteUserReturnsDeleteErrorWhenDeleteFails() {
        val repo = FakeRepositoryUser(forceDeleteFailure = true)
        val created = repo.createLocalUser("u", "u@test.dev", "hash")
        val service = UserAuthService(encoder, repo)

        val result = service.deleteUser(created.id)

        assertLeftEquals(result, UserError.ErrorDeletingUser)
    }

    @Test
    fun getAllUsersReturnsRepositoryList() {
        val repo = FakeRepositoryUser()
        repo.createLocalUser("a", "a@test.dev", "h1")
        repo.createLocalUser("b", "b@test.dev", "h2")
        val service = UserAuthService(encoder, repo)

        val result = service.getAllUsers()

        assertEquals(2, result.size)
    }

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

private class FakeRepositoryUser(
    private val throwOnCreateLocal: Boolean = false,
    private val forceDeleteFailure: Boolean = false,
) : RepositoryUser {
    private val users = linkedMapOf<Int, User>()
    private var nextId = 1

    override fun createLocalUser(name: String, email: String, passwordHash: String): User {
        if (throwOnCreateLocal) error("forced createLocal failure")
        val user =
            User(
                id = nextId++,
                name = name,
                email = email,
                passwordHash = passwordHash,
                authProvider = AuthProvider.LOCAL,
                providerId = null,
                createdAt = Instant.now(),
            )
        users[user.id] = user
        return user
    }

    override fun createOAuthUser(name: String, email: String, provider: AuthProvider, providerId: String): User {
        val user =
            User(
                id = nextId++,
                name = name,
                email = email,
                passwordHash = null,
                authProvider = provider,
                providerId = providerId,
                createdAt = Instant.now(),
            )
        users[user.id] = user
        return user
    }

    override fun findByEmail(email: String): User? = users.values.firstOrNull { it.email == email }

    override fun findByProvider(provider: AuthProvider, providerId: String): User? =
        users.values.firstOrNull { it.authProvider == provider && it.providerId == providerId }

    override fun findByTokenValidation(tokenValidationInfo: TokenValidationInfo): User? = null

    override fun count(): Long = users.size.toLong()

    override fun findById(id: Int): User? = users[id]

    override fun findAll(): List<User> = users.values.toList()

    override fun save(entity: User) {
        users[entity.id] = entity
    }

    override fun deleteById(id: Int): Boolean {
        if (forceDeleteFailure) return false
        return users.remove(id) != null
    }

    override fun clear() {
        users.clear()
    }
}

*/