package com.example.evolab.repo.repoLLMCredentials

import com.example.evolab.domain.LLMCredentials.LLM
import com.example.evolab.domain.LLMCredentials.LLMCredentials
import com.example.evolab.repo.Repository

interface RepositoryLLMCredentials : Repository<LLMCredentials> {
	// TODO(local-model): if LOCAL_MODEL stops storing secrets, make apiKeyEncrypted nullable instead of adding a separate repo flow.
	fun createLLMCredential(
		userId: Int,
		provider: LLM,
		apiKeyEncrypted: String,
	): LLMCredentials

	fun findAllByUserId(userId: Int): List<LLMCredentials>

	fun findAllByProvider(provider: LLM): List<LLMCredentials>
}




