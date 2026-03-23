package com.example.evolab.repo.evolutionConfig

import com.example.evolab.domain.config.Config
import com.example.evolab.repo.Repository

interface RepositoryConfig : Repository<Config> {

    fun createConfig(
        userId: Int,
        llmCredentialsId: Int,
        modelName: String,
        maxIter: Int,
        checkPointInterval: Int,
        additionalParams: Map<String, String>,
    ): Int

    fun findAllByUserId(userId: Int): List<Config>

    fun findAllByLlmCredentialId(llmCredentialsId: Int): List<Config>

    fun findAllByModelName(modelName: String): List<Config>
}