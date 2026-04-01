package com.example.evolab.repo.repoToken

import com.example.evolab.domain.token.Token
import com.example.evolab.domain.token.TokenValidationInfo
import com.example.evolab.repo.support.RepositoryDbTestSupport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RepositoryTokenJdbiTest : RepositoryDbTestSupport() {
    @Test
    fun createAndFindByTokenValidationWorks() {
        val repo = RepositoryTokenJdbi(handle)
        val userId = insertUser(email = "tok-${System.nanoTime()}@test.dev")
        val token = Token("tv-${System.nanoTime()}", userId, 100L, 200L)

        repo.createToken(token, maxTokens = 10)

        val found = repo.findByTokenValidation(TokenValidationInfo(token.tokenValidation))

        assertNotNull(found)
        assertEquals(token.tokenValidation, found!!.tokenValidation)
        assertEquals(userId, found.userId)
    }

    @Test
    fun findAllByUserIdReturnsTokensOrderedByLastUsedDesc() {
        val repo = RepositoryTokenJdbi(handle)
        val userId = insertUser(email = "order-${System.nanoTime()}@test.dev")
        insertToken("t-1-${System.nanoTime()}", userId, createdAt = 1L, lastUsedAt = 10L)
        insertToken("t-2-${System.nanoTime()}", userId, createdAt = 2L, lastUsedAt = 50L)
        insertToken("t-3-${System.nanoTime()}", userId, createdAt = 3L, lastUsedAt = 30L)

        val tokens = repo.findAllByUserId(userId)

        assertEquals(3, tokens.size)
        assertTrue(tokens[0].lastUsedAt >= tokens[1].lastUsedAt)
        assertTrue(tokens[1].lastUsedAt >= tokens[2].lastUsedAt)
    }

    @Test
    fun getTokenByTokenValidationInfoReturnsUserAndToken() {
        val repo = RepositoryTokenJdbi(handle)
        val userId = insertUser(email = "pair-${System.nanoTime()}@test.dev")
        val tokenValue = "pair-token-${System.nanoTime()}"
        insertToken(tokenValue, userId, createdAt = 11L, lastUsedAt = 22L)

        val pair = repo.getTokenByTokenValidationInfo(TokenValidationInfo(tokenValue))

        assertNotNull(pair)
        assertEquals(userId, pair!!.first.id)
        assertEquals(tokenValue, pair.second.tokenValidation)
    }

    @Test
    fun updateTokenLastUsedChangesTimestamp() {
        val repo = RepositoryTokenJdbi(handle)
        val userId = insertUser(email = "update-last-used-${System.nanoTime()}@test.dev")
        val tokenValue = "update-token-${System.nanoTime()}"
        insertToken(tokenValue, userId, createdAt = 100L, lastUsedAt = 200L)

        repo.updateTokenLastUsed(TokenValidationInfo(tokenValue), 999L)

        val found = repo.findByTokenValidation(TokenValidationInfo(tokenValue))
        assertNotNull(found)
        assertEquals(999L, found!!.lastUsedAt)
    }

    @Test
    fun removeTokenByValidationInfoDeletesToken() {
        val repo = RepositoryTokenJdbi(handle)
        val userId = insertUser(email = "remove-${System.nanoTime()}@test.dev")
        val tokenValue = "remove-token-${System.nanoTime()}"
        insertToken(tokenValue, userId, createdAt = 1L, lastUsedAt = 2L)

        val deleted = repo.removeTokenByValidationInfo(TokenValidationInfo(tokenValue))
        val found = repo.findByTokenValidation(TokenValidationInfo(tokenValue))

        assertEquals(1, deleted)
        assertNull(found)
    }

    @Test
    fun createTokenRespectsMaxTokensKeepingNewestOnes() {
        val repo = RepositoryTokenJdbi(handle)
        val userId = insertUser(email = "limit-${System.nanoTime()}@test.dev")

        val oldest = Token("oldest-${System.nanoTime()}", userId, createdAt = 1L, lastUsedAt = 1L)
        val middle = Token("middle-${System.nanoTime()}", userId, createdAt = 2L, lastUsedAt = 2L)
        val newest = Token("newest-${System.nanoTime()}", userId, createdAt = 3L, lastUsedAt = 3L)

        repo.createToken(oldest, maxTokens = 2)
        repo.createToken(middle, maxTokens = 2)
        repo.createToken(newest, maxTokens = 2)

        val tokens = repo.findAllByUserId(userId)
        val tokenValues = tokens.map { it.tokenValidation }

        assertEquals(2, tokens.size)
        assertTrue(tokenValues.contains(middle.tokenValidation))
        assertTrue(tokenValues.contains(newest.tokenValidation))
    }
}

