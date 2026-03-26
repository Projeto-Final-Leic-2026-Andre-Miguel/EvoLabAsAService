package com.example.evolab.service.LLMCredentials.service

import com.example.evolab.domain.LLMCredentials.LLM
import com.example.evolab.domain.LLMCredentials.LLMCredentials
import com.example.evolab.repo.repoLLMCredentials.RepositoryLLMCredentials
import com.example.evolab.repo.transactions.TransactionManager
import com.example.evolab.service.LLMCredentials.Validator.LLMCrendentialsValidator
import com.example.evolab.service.auxiliary.Either
import com.example.evolab.service.auxiliary.failure
import jakarta.inject.Named


@Named
class LLMCredentialsServiceImp(
    private val repoLLMCredentials: RepositoryLLMCredentials,
    private val llmValidator: LLMCrendentialsValidator,
    private val trxManager : TransactionManager
) : LLMCredentialsService {


    override suspend fun createLLMCredential(
        userId: Int,
        llm: LLM,
        apiKey: String?
    ): Either<LLMCredentialsServiceErrors, LLMCredentials> =

        trxManager.run {
            if (!validateLLM(llm)) {
                return@run failure(LLMCredentialsServiceErrors.InvalidLLMProvider("The provided LLM is not supported"))
            }

            if (!canCreateLLMCredential(userId, llm)) {
                return@run failure(LLMCredentialsServiceErrors.CredentialWithProviderAlreadyInUse("User already has credentials for this LLM"))
            }

            val validationResult = when(llm) {

                is LLM.OPENAI -> llmValidator.openAiKeyValidator(apiKey)
                is LLM.GEMINI -> llmValidator.geminiKeyValidator(apiKey)
                is LLM.LOCAL_MODEL -> llmValidator.localModelsKeyValidator(apiKey)
            }




        }


            override fun getLLMCredentialById(id: Int): Either<LLMCredentialsServiceErrors, LLMCredentials> {
            TODO("Not yet implemented")
        }

        override fun getLLMCredentialsByUserId(userId: Int): Either<LLMCredentialsServiceErrors, List<LLMCredentials>> {
            TODO("Not yet implemented")
        }

        override fun updateLLMCredential(
            id: Int,
            llm: LLM,
            apiKey: String?
        ): Either<LLMCredentialsServiceErrors, LLMCredentials> {
            TODO("Not yet implemented")
        }

        override fun deleteLLMCredential(id: Int): Either<LLMCredentialsServiceErrors, LLMCredentials> {
            TODO("Not yet implemented")
        }


        private fun validateApiKeyForLLM(llm: LLM, apiKey: String?): Boolean =
            when (llm) {
                LLM.OPENAI -> llmValidator.openAiKeyValidator(apiKey)
                LLM.GEMINI -> llmValidator.geminiKeyValidator(apiKey)
                LLM.LOCAL_MODEL -> llmValidator.localModelsKeyValidator(apiKey)
            }

        private fun validateLLM(llm: LLM): Boolean =

            when (llm) {
                LLM.OPENAI -> true
                LLM.GEMINI -> true
                LLM.LOCAL_MODEL -> true
                else -> false
            }

        private fun canCreateLLMCredential(userId: Int, llm: LLM): Boolean =
            repoLLMCredentails.findAllByUserId(userId).none { it.llm == llm }


        }

