package com.example.evolab.repo.repoConfig

import com.example.evolab.domain.config.Config
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.jdbi.v3.core.Handle

class RepositoryConfigJdbi(
    private val handle: Handle,
) : RepositoryConfig {

    private val objectMapper = jacksonObjectMapper()

    override fun createConfig(
        userId: Int,
        llmCredentialsId: Int,
        modelName: String,
        maxIter: Int,
        checkPointInterval: Int,
        additionalParams: Map<String, String>,
    ): Int =
        handle
            .createQuery(ConfigSql.CREATE_CONFIG)
            .bind("userId", userId)
            .bind("llmCredentialsId", llmCredentialsId)
            .bind("modelName", modelName)
            .bind("maxIter", maxIter)
            .bind("checkPointInterval", checkPointInterval)
            .bind("additionalParams", objectMapper.writeValueAsString(additionalParams))
            .mapTo(Int::class.java)
            .one()

    override fun findAllByUserId(userId: Int): List<Config> =
        handle
            .createQuery(ConfigSql.FIND_ALL_BY_USER_ID)
            .bind("userId", userId)
            .map { rs, _ -> rs.toConfig() }
            .list()

    override fun findAllByLlmCredentialId(llmCredentialsId: Int): List<Config> =
        handle
            .createQuery(ConfigSql.FIND_ALL_BY_LLM_CREDENTIAL_ID)
            .bind("llmCredentialsId", llmCredentialsId)
            .map { rs, _ -> rs.toConfig() }
            .list()

    override fun findAllByModelName(modelName: String): List<Config> =
        handle
            .createQuery(ConfigSql.FIND_ALL_BY_MODEL_NAME)
            .bind("modelName", modelName)
            .map { rs, _ -> rs.toConfig() }
            .list()

    override fun findById(id: Int): Config? =
        handle
            .createQuery(ConfigSql.FIND_BY_ID)
            .bind("id", id)
            .map { rs, _ -> rs.toConfig() }
            .findOne()
            .orElse(null)

    override fun findAll(): List<Config> =
        handle
            .createQuery(ConfigSql.FIND_ALL)
            .map { rs, _ -> rs.toConfig() }
            .list()

    override fun save(entity: Config) {
        handle
            .createUpdate(ConfigSql.SAVE)
            .bind("id", entity.id)
            .bind("userId", entity.userId)
            .bind("llmCredentialsId", entity.llmCredentialsId)
            .bind("modelName", entity.modelName)
            .bind("maxIter", entity.maxIter)
            .bind("checkPointInterval", entity.checkPointInterval)
            .bind("additionalParams", objectMapper.writeValueAsString(entity.additionalParams))
            .execute()
    }

    override fun deleteById(id: Int): Boolean =
        handle
            .createUpdate(ConfigSql.DELETE_BY_ID)
            .bind("id", id)
            .execute() > 0

    override fun clear() {
        handle
            .createUpdate(ConfigSql.CLEAR)
            .execute()
    }

    private fun java.sql.ResultSet.toConfig(): Config =
        Config(
            id = getInt("id"),
            userId = getInt("userId"),
            llmCredentialsId = getInt("llmCredentialsId"),
            modelName = getString("modelName"),
            maxIter = getInt("maxIter"),
            checkPointInterval = getInt("checkPointInterval"),
            additionalParams = parseAdditionalParams(getString("additionalParamsJson")),
            createdAt = getTimestamp("createdAt").toInstant(),
        )

    private fun parseAdditionalParams(value: String?): Map<String, String> {
        if (value.isNullOrBlank()) return emptyMap()
        return objectMapper.readValue(value)
    }
}