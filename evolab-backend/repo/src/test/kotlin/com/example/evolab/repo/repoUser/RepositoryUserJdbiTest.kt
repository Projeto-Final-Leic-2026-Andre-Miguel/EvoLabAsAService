package com.example.evolab.repo.repoUser

import com.example.evolab.domain.token.TokenValidationInfo
import com.example.evolab.domain.user.AuthProvider
import com.example.evolab.repo.support.RepositoryDbTestSupport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RepositoryUserJdbiTest : RepositoryDbTestSupport() {
    @Test
    fun createLocalUserAndFindByEmailWorks() {
        val repo = RepositoryUserJdbi(handle)
        val email = "local-${System.nanoTime()}@test.dev"

        val created = repo.createLocalUser("Local User", email, "hash-1")
        val found = repo.findByEmail(email)

        assertNotNull(found)
        assertEquals(created.id, found!!.id)
        assertEquals("Local User", found.name)
        assertEquals(email, found.email)
        assertEquals("hash-1", found.passwordHash)
        assertEquals(1L, repo.count())
    }

    @Test
    fun createOAuthUserAndFindByProviderWorks() {
        val repo = RepositoryUserJdbi(handle)
        val providerId = "google-sub-${System.nanoTime()}"

        val created =
            repo.createOAuthUser(
                name = "OAuth User",
                email = "oauth-${System.nanoTime()}@test.dev",
                provider = AuthProvider.GOOGLE,
                providerId = providerId,
            )

        val found = repo.findByProvider(AuthProvider.GOOGLE, providerId)

        assertNotNull(found)
        assertEquals(created.id, found!!.id)
        assertEquals(providerId, found.providerId)
    }

    @Test
    fun findByTokenValidationReturnsUser() {
        val repo = RepositoryUserJdbi(handle)
        val userId = insertUser(email = "token-user-${System.nanoTime()}@test.dev")
        val tokenValue = "token-validation-${System.nanoTime()}"
        insertToken(tokenValue, userId, createdAt = 10L, lastUsedAt = 20L)

        val found = repo.findByTokenValidation(TokenValidationInfo(tokenValue))

        assertNotNull(found)
        assertEquals(userId, found!!.id)
    }

    @Test
    fun saveUpdatesUserFields() {
        val repo = RepositoryUserJdbi(handle)
        val created = repo.createLocalUser("Before", "save-${System.nanoTime()}@test.dev", "hash-before")

        val updated =
            created.copy(
                name = "After",
                email = "after-${System.nanoTime()}@test.dev",
                passwordHash = "hash-after",
            )

        repo.save(updated)
        val found = repo.findById(created.id)

        assertNotNull(found)
        assertEquals("After", found!!.name)
        assertEquals("hash-after", found.passwordHash)
    }

    @Test
    fun deleteByIdRemovesUser() {
        val repo = RepositoryUserJdbi(handle)
        val created = repo.createLocalUser("Delete Me", "delete-${System.nanoTime()}@test.dev", "hash")

        val deleted = repo.deleteById(created.id)
        val found = repo.findById(created.id)

        assertTrue(deleted)
        assertNull(found)
    }
}

