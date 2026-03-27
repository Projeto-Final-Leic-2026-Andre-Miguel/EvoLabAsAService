package com.example.evolab.service.LLMCredentials.service

import com.example.evolab.domain.LLMCredentials.LLM
import com.example.evolab.domain.LLMCredentials.LLMCredentials
import com.example.evolab.repo.repoLLMCredentials.RepositoryLLMCredentials
import com.example.evolab.repo.transactions.TransactionManager
import com.example.evolab.service.LLMCredentials.Validator.LLMCrendentialsValidator
import com.example.evolab.service.LLMCredentials.Validator.LLMValidatorErrors
import com.example.evolab.service.auxiliary.Either
import com.example.evolab.service.auxiliary.Failure
import com.example.evolab.service.auxiliary.Success
import com.example.evolab.service.auxiliary.failure
import com.example.evolab.service.auxiliary.success
import jakarta.inject.Named


@Named
class LLMCredentialsServiceImp(
    private val repoLLMCredentials: RepositoryLLMCredentials,
    private val llmValidator: LLMCrendentialsValidator,
    private val trxManager: TransactionManager,
) : LLMCredentialsService {

    override suspend fun createLLMCredential(
        userId: Int,
        llm: LLM,
        apiKey: String?,
    ): Either<LLMCredentialsServiceErrors, LLMCredentials> =

        when (val validated = llmValidator.validateApiKeyForLLM(llm, apiKey)) {
            is Failure-> failure(mapValidatorError(validated.value))
            is Success ->  // Depois de validade a api_key...
                trxManager.run {
                    if (!canCreateLLMCredential(userId, llm)) {
                        return@run failure(
                            LLMCredentialsServiceErrors.CredentialWithProviderAlreadyInUse(
                                "User already has credentials for provider '$llm'",
                            ),
                        )
                    }

                    val apiKeyEncrypted: String = "por fazer"

                    TODO("FALTA VER QUAL O MELHOR METODO DE GUARDAR A CHAVE ENCRPITADA DO CLIENTE.")

                    val created = repoLLMCredentials.createLLMCredential(userId, llm, apiKeyEncrypted)
                    return@run success(created)
                }
        }

    override fun getLLMCredentialById(id: Int): Either<LLMCredentialsServiceErrors, LLMCredentials> =
        trxManager.run {
            val credential = getLLMCredentialsById(id)
                ?: return@run failure(
                    LLMCredentialsServiceErrors.LLMCredentialNotFound("Credential with id '$id' was not found"),
                )
            return@run success(credential)
        }

    override fun getLLMCredentialsByUserId(userId: Int): Either<LLMCredentialsServiceErrors, List<LLMCredentials>> =
        trxManager.run {
            success(getAllUserCredentialsById(userId))
        }

    override suspend fun updateLLMCredential(
        id: Int,
        llm: LLM,
        apiKey: String?,
    ): Either<LLMCredentialsServiceErrors, LLMCredentials> {
        val validated = llmValidator.validateApiKeyForLLM(llm, apiKey)

        if (!validated) {
            return failure(mapValidatorError(validated.value))
        }


        return trxManager.run {
            val existing = repoLLmCredentials.findById(id)
                ?: return@run failure(
                    LLMCredentialsServiceErrors.LLMCredentialNotFound("Credential with id '$id' was not found"),
                )

            val duplicatedProviderInSameUser =
                repoLLmCredentials
                    .findAllByUserId(existing.userId)
                    .any { it.id != id && it.llm == llm }

            if (duplicatedProviderInSameUser) {
                return@run failure(
                    LLMCredentialsServiceErrors.CredentialWithProviderAlreadyInUse(
                        "User already has credentials for provider '$llm'",
                    ),
                )
            }

            val updated = existing.copy(llm = llm, apiKeyEncrypted = validatedKey)
            repoLLmCredentials.save(updated)
            success(updated)
        }
    }

    override fun deleteLLMCredential(id: Int): Either<LLMCredentialsServiceErrors, LLMCredentials> =
        trxManager.run {
            val existing = repoLLmCredentials.findById(id)
                ?: return@run failure(
                    LLMCredentialsServiceErrors.LLMCredentialNotFound("Credential with id '$id' was not found"),
                )

            val deleted = repoLLmCredentials.deleteById(id)
            if (!deleted) {
                return@run failure(
                    LLMCredentialsServiceErrors.PersistenceError("Could not delete credential with id '$id'"),
                )
            }

            success(existing)
        }

    /**
     * Valida se o utilizador tem ou não alguma credencial com o provider especificado
     *
     * */
    private fun canCreateLLMCredential(userId: Int, llm: LLM): Boolean =
        repoLLMCredentials.findAllByUserId(userId).none { credential -> credential.llm == llm }

    private fun getLLMCredentialsById(id : Int): LLMCredentials? =
        repoLLMCredentials.findById(id)

    private fun getAllUserCredentialsById(userId : Int): List<LLMCredentials> =
        repoLLMCredentials.findAllByUserId(userId)

    private fun mapValidatorError(error: LLMValidatorErrors): LLMCredentialsServiceErrors =
        when (error) {
            is LLMValidatorErrors.UnsupportedLLM -> LLMCredentialsServiceErrors.InvalidLLMProvider(error.message)
            is LLMValidatorErrors.InvalidAPIKey -> LLMCredentialsServiceErrors.InvalidApiKey(error.message)
            is LLMValidatorErrors.InvalidLLM -> LLMCredentialsServiceErrors.InvalidLLMProvider(error.message)
            is LLMValidatorErrors.InvalidKeyFormat -> LLMCredentialsServiceErrors.InvalidApiKey(error.message)
            is LLMValidatorErrors.UnknownError -> LLMCredentialsServiceErrors.InvalidApiKey(error.message)
        }
}

