package com.example.evolab.repo.repoLLMCredentials

import com.example.evolab.domain.LLMCredentials.LLM
import com.example.evolab.domain.LLMCredentials.LLMCredentials
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo

class RepositoryLLMCredentialsJdbi(
	private val handle: Handle,
) : RepositoryLLMCredentials {

	override fun createLLMCredential(
		userId: Int,
		provider: LLM,
		apiKeyEncrypted: String,
	): LLMCredentials =
		handle
			.createQuery(LLMSql.CREATE_CREDENTIAL)
			.bind("userId", userId)
			.bind("provider", provider.name)
			.bind("apiKeyEncrypted", apiKeyEncrypted)
			.mapTo<LLMCredentials>()
			.one()

	override fun findAllByUserId(userId: Int): List<LLMCredentials> =
		handle
			.createQuery(LLMSql.FIND_ALL_BY_USER_ID)
			.bind("userId", userId)
			.mapTo<LLMCredentials>()
			.list()

	override fun findAllByProvider(provider: LLM): List<LLMCredentials> =
		handle
			.createQuery(LLMSql.FIND_ALL_BY_PROVIDER)
			.bind("provider", provider.name)
			.mapTo<LLMCredentials>()
			.list()

	override fun findById(id: Int): LLMCredentials? =
		handle
			.createQuery(LLMSql.FIND_BY_ID)
			.bind("id", id)
			.mapTo<LLMCredentials>()
			.findOne()
			.orElse(null)

	override fun findAll(): List<LLMCredentials> =
		handle
			.createQuery(LLMSql.FIND_ALL)
			.mapTo<LLMCredentials>()
			.list()

	override fun save(entity: LLMCredentials) {
		handle
			.createUpdate(LLMSql.SAVE)
			.bind("id", entity.id)
			.bind("userId", entity.userId)
			.bind("provider", entity.llm.name)
			.bind("apiKeyEncrypted", entity.apiKeyEncrypted)
			.execute()
	}

	override fun deleteById(id: Int): Boolean =
		handle
			.createUpdate(LLMSql.DELETE_BY_ID)
			.bind("id", id)
			.execute() > 0

	override fun clear() {
		handle
			.createUpdate(LLMSql.CLEAR)
			.execute()
	}
}