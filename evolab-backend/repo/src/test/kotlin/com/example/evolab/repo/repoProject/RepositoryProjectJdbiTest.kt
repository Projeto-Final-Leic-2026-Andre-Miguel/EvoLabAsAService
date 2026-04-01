package com.example.evolab.repo.repoProject

import com.example.evolab.domain.evolution.EvolutionStatus
import com.example.evolab.repo.repoConfig.RepositoryConfigJdbi
import com.example.evolab.repo.support.RepositoryDbTestSupport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RepositoryProjectJdbiTest : RepositoryDbTestSupport() {
    @Test
    fun createProjectAndFindByIdWorks() {
        val repo = RepositoryProjectJdbi(handle)
        val userId = insertUser(email = "project-${System.nanoTime()}@test.dev")

        val created =
            repo.createProject(
                userId = userId,
                name = "Projeto Demo",
                description = "descricao",
                initialProgram = "def solve(x): return x",
                evaluatorCode = "def evaluate(candidate): return 1.0",
            )

        val found = repo.findById(created.id)

        assertNotNull(found)
        assertEquals(userId, found!!.userId)
        assertNull(found.configId)
        assertEquals("Projeto Demo", found.name)
        assertEquals("descricao", found.description)
        assertEquals("def solve(x): return x", found.initialProgram)
        assertEquals("def evaluate(candidate): return 1.0", found.evaluatorCode)
        assertEquals(EvolutionStatus.CREATED, found.status)
    }

    @Test
    fun findAllFiltersWork() {
        val repo = RepositoryProjectJdbi(handle)
        val user1 = insertUser(email = "project-u1-${System.nanoTime()}@test.dev")
        val user2 = insertUser(email = "project-u2-${System.nanoTime()}@test.dev")
        val config1 = createConfigForUser(user1)
        val config2 = createConfigForUser(user2)

        val alpha1 =
            repo.createProject(
                userId = user1,
                name = "alpha",
                description = null,
                initialProgram = "def solve(x): return x",
                evaluatorCode = "def evaluate(candidate): return 1.0",
            )
        val beta =
            repo.createProject(
                userId = user1,
                name = "beta",
                description = null,
                initialProgram = "def solve(x): return x + 1",
                evaluatorCode = "def evaluate(candidate): return 2.0",
            )
        val alpha2 =
            repo.createProject(
                userId = user2,
                name = "alpha",
                description = null,
                initialProgram = "def solve(x): return x - 1",
                evaluatorCode = "def evaluate(candidate): return 3.0",
            )

        repo.save(beta.copy(configId = config1, status = EvolutionStatus.QUEUED))
        repo.save(alpha2.copy(configId = config2, status = EvolutionStatus.COMPLETED))

        val byUser = repo.findAllByUserId(user1)
        val byConfig = repo.findAllByConfigId(config1)
        val byStatus = repo.findAllByStatus(EvolutionStatus.QUEUED)
        val byName = repo.findAllByName("alpha")

        assertEquals(2, byUser.size)
        assertEquals(1, byConfig.size)
        assertEquals(beta.id, byConfig.first().id)
        assertEquals(1, byStatus.size)
        assertEquals(beta.id, byStatus.first().id)
        assertEquals(2, byName.size)
        assertTrue(byName.map { it.id }.containsAll(listOf(alpha1.id, alpha2.id)))
    }

    @Test
    fun saveUpdatesProject() {
        val repo = RepositoryProjectJdbi(handle)
        val userId = insertUser(email = "project-save-${System.nanoTime()}@test.dev")
        val configId = createConfigForUser(userId)

        val created =
            repo.createProject(
                userId = userId,
                name = "old-name",
                description = "old-description",
                initialProgram = "def solve(x): return x",
                evaluatorCode = "def evaluate(candidate): return 1.0",
            )

        val updated =
            created.copy(
                configId = configId,
                name = "new-name",
                description = "new-description",
                initialProgram = "def solve(x): return x * 2",
                evaluatorCode = "def evaluate(candidate): return 2.0",
                status = EvolutionStatus.RUNNING,
            )

        repo.save(updated)
        val found = repo.findById(created.id)

        assertNotNull(found)
        assertEquals(configId, found!!.configId)
        assertEquals("new-name", found.name)
        assertEquals("new-description", found.description)
        assertEquals("def solve(x): return x * 2", found.initialProgram)
        assertEquals("def evaluate(candidate): return 2.0", found.evaluatorCode)
        assertEquals(EvolutionStatus.RUNNING, found.status)
    }

    @Test
    fun deleteByIdRemovesProject() {
        val repo = RepositoryProjectJdbi(handle)
        val userId = insertUser(email = "project-del-${System.nanoTime()}@test.dev")
        val created =
            repo.createProject(
                userId = userId,
                name = "delete-me",
                description = null,
                initialProgram = "def solve(x): return x",
                evaluatorCode = "def evaluate(candidate): return 1.0",
            )

        val deleted = repo.deleteById(created.id)
        val found = repo.findById(created.id)

        assertTrue(deleted)
        assertNull(found)
    }

    private fun createConfigForUser(userId: Int): Int {
        val repo = RepositoryConfigJdbi(handle)
        val llmCredentialId = insertLlmCredential(userId)
        return repo.createConfig(
            userId = userId,
            llmCredentialsId = llmCredentialId,
            modelName = "gpt-4o-mini",
            maxIter = 20,
            checkPointInterval = 5,
            additionalParams = emptyMap(),
        )
    }
}
