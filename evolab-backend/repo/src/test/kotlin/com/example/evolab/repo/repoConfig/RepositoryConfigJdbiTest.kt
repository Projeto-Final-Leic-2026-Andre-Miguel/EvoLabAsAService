package com.example.evolab.repo.repoConfig

import com.example.evolab.repo.support.RepositoryDbTestSupport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RepositoryConfigJdbiTest : RepositoryDbTestSupport() {
    @Test
    fun createConfigAndFindByIdWorks() {
        val repo = RepositoryConfigJdbi(handle)
        val userId = insertUser(email = "cfg-${System.nanoTime()}@test.dev")
        val llmCredentialId = insertLlmCredential(userId)

        val id =
            repo.createConfig(
                userId = userId,
                llmCredentialsId = llmCredentialId,
                modelName = "gpt-4o-mini",
                maxIter = 50,
                checkPointInterval = 5,
                additionalParams = mapOf("temperature" to "0.3"),
            )

        val found = repo.findById(id)

        assertNotNull(found)
        assertEquals(userId, found!!.userId)
        assertEquals(llmCredentialId, found.llmCredentialsId)
        assertEquals("gpt-4o-mini", found.modelName)
        assertEquals("0.3", found.additionalParams["temperature"])
    }

    @Test
    fun findAllFiltersWork() {
        val repo = RepositoryConfigJdbi(handle)
        val user1 = insertUser(email = "cfg-u1-${System.nanoTime()}@test.dev")
        val user2 = insertUser(email = "cfg-u2-${System.nanoTime()}@test.dev")
        val cred1 = insertLlmCredential(user1)
        val cred2 = insertLlmCredential(user2, provider = "GEMINI")

        repo.createConfig(user1, cred1, "model-a", 10, 1, emptyMap())
        repo.createConfig(user1, cred1, "model-b", 20, 2, emptyMap())
        repo.createConfig(user2, cred2, "model-a", 30, 3, emptyMap())

        val byUser = repo.findAllByUserId(user1)
        val byCredential = repo.findAllByLlmCredentialId(cred1)
        val byModel = repo.findAllByModelName("model-a")

        assertEquals(2, byUser.size)
        assertEquals(2, byCredential.size)
        assertEquals(2, byModel.size)
    }

    @Test
    fun saveUpdatesConfig() {
        val repo = RepositoryConfigJdbi(handle)
        val userId = insertUser(email = "cfg-save-${System.nanoTime()}@test.dev")
        val llmCredentialId = insertLlmCredential(userId)

        val id = repo.createConfig(userId, llmCredentialId, "model-old", 10, 1, mapOf("k" to "v1"))
        val current = repo.findById(id)!!

        val updated =
            current.copy(
                modelName = "model-new",
                maxIter = 99,
                checkPointInterval = 9,
                additionalParams = mapOf("k" to "v2"),
            )

        repo.save(updated)
        val found = repo.findById(id)

        assertNotNull(found)
        assertEquals("model-new", found!!.modelName)
        assertEquals(99, found.maxIter)
        assertEquals("v2", found.additionalParams["k"])
    }

    @Test
    fun deleteByIdRemovesConfig() {
        val repo = RepositoryConfigJdbi(handle)
        val userId = insertUser(email = "cfg-del-${System.nanoTime()}@test.dev")
        val llmCredentialId = insertLlmCredential(userId)

        val id = repo.createConfig(userId, llmCredentialId, "model", 10, 1, emptyMap())

        val deleted = repo.deleteById(id)
        val found = repo.findById(id)

        assertTrue(deleted)
        assertNull(found)
    }
}

