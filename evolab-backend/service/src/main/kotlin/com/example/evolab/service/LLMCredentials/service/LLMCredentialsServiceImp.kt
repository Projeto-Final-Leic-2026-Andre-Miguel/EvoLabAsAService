package com.example.evolab.service.LLMCredentials.service

import com.example.evolab.domain.LLMCredentials.LLM
import com.example.evolab.domain.LLMCredentials.LLMCredentials
import com.example.evolab.repo.transactions.Transaction
import com.example.evolab.repo.transactions.TransactionManager
import com.example.evolab.service.LLMCredentials.validator.LLMCrendentialsValidator
import com.example.evolab.service.LLMCredentials.validator.LLMValidatorErrors
import com.example.evolab.service.auxiliary.Either
import com.example.evolab.service.auxiliary.Failure
import com.example.evolab.service.auxiliary.Success
import com.example.evolab.service.auxiliary.failure
import com.example.evolab.service.auxiliary.success
import com.example.evolab.service.security.EncryptionService
import jakarta.inject.Named


@Named
class LLMCredentialsServiceImp(
    private val llmValidator: LLMCrendentialsValidator,
    private val trxManager: TransactionManager,
    private val encryptionService: EncryptionService
) : LLMCredentialsService {
    companion object {
        private const val LOCAL_MODEL_SECRET_PLACEHOLDER = "__LOCAL_MODEL_NO_SECRET__"
        private const val LOCAL_MODEL_VALIDATION_PREFIX = "LOCAL_MODEL::"
        private const val LOCAL_MODEL_VALIDATION_SEPARATOR = "::MODEL::"
    }

    override suspend fun createLLMCredential(
        userId: Int,
        llm: LLM,
        apiKey: String?,
        apiBase: String?,
        modelName: String?,
    ): Either<LLMCredentialsServiceErrors, LLMCredentials> {
        val normalizedApiKey = apiKey?.trim()?.takeIf { it.isNotBlank() }
        val normalizedApiBase = apiBase?.trim()?.takeIf { it.isNotBlank() }
        val normalizedModelName = modelName?.trim()?.takeIf { it.isNotBlank() }

        when (llm) {
            LLM.LOCAL_MODEL -> {
                if (normalizedApiBase == null || normalizedModelName == null) {
                    return failure(
                        LLMCredentialsServiceErrors.InvalidLLMProvider(
                            "LOCAL_MODEL credentials require both apiBase and modelName in the create request",
                        ),
                    )
                }
            }

            else -> {
                if (normalizedApiBase != null || normalizedModelName != null) {
                    return failure(
                        LLMCredentialsServiceErrors.InvalidLLMProvider(
                            "apiBase and modelName are only accepted when provider is LOCAL_MODEL",
                        ),
                    )
                }
            }
        }

        val validatorInput =
            when (llm) {
                LLM.LOCAL_MODEL ->
                    "${LOCAL_MODEL_VALIDATION_PREFIX}${normalizedApiBase}${LOCAL_MODEL_VALIDATION_SEPARATOR}${normalizedModelName}"

                else -> normalizedApiKey
            }

        if (validatorInput != null) {
            when (val validation = llmValidator.validateApiKeyForLLM(llm, validatorInput)) {
                is Failure -> return failure(mapValidatorError(validation.value))
                is Success -> {  }
            }
        }

        return trxManager.run {
            if (!canCreateLLMCredential(userId, llm)) {
                return@run failure(
                    LLMCredentialsServiceErrors.CredentialWithProviderAlreadyInUse(
                        "User already has credentials for provider '$llm'"
                    )
                )
            }

            val secretToPersist =
                when (llm) {
                    LLM.LOCAL_MODEL -> normalizedApiKey ?: LOCAL_MODEL_SECRET_PLACEHOLDER
                    else ->
                        normalizedApiKey
                            ?: return@run failure(
                                LLMCredentialsServiceErrors.InvalidApiKey("API key cannot be null or blank"),
                            )
                }

            // Keep the current persistence contract until the schema allows LOCAL_MODEL credentials with no stored secret.
            val apiKeyEncrypted = encryptionService.encrypt(secretToPersist)

            val created = repoLLmCredentials.createLLMCredential(userId, llm, apiKeyEncrypted)
            success(created)
        }
    }

    override fun getLLMCredentialById(userId : Int, id: Int): Either<LLMCredentialsServiceErrors, LLMCredentials> =
        trxManager.run {
            val credential = getLLMCredentialsById(id)
                ?: return@run failure(
                    LLMCredentialsServiceErrors.LLMCredentialNotFound("Credential with id '$id' was not found"),
                )

            if(credential.userId != userId) {
                return@run failure(
                    LLMCredentialsServiceErrors.UnauthorizedAccess("User with id $userId is not the owner of credential with id '$id'"),
                )
            }
            return@run success(credential)
        }

    override fun getLLMCredentialsByUserId(userId: Int): Either<LLMCredentialsServiceErrors, List<LLMCredentials>> =
        trxManager.run {
            success(getAllUserCredentialsById(userId))
        }

    override fun getAllLLMCredentials(): Either<LLMCredentialsServiceErrors, List<LLMCredentials>> =
        trxManager.run {
            success(repoLLmCredentials.findAll())
        }

    /**
     * Atualiza a credencial existente respeitando o tipo de provider.
     * Para LOCAL_MODEL validamos apiBase + modelName; para providers cloud mantemos validação por apiKey.
     */

    override suspend fun updateLLMCredential(
        id: Int,
        userId: Int,
        apiKey: String?,
        apiBase: String?,
        modelName: String?,
    ): Either<LLMCredentialsServiceErrors, LLMCredentials> {

        val credential = trxManager.run {
            repoLLmCredentials.findById(id)
        } ?: return failure(LLMCredentialsServiceErrors.LLMCredentialNotFound("Credential with id '$id' not found"))

        if(credential.userId != userId) {
            return failure(
                LLMCredentialsServiceErrors.UnauthorizedAccess("User with id $userId is not the owner of credential with id '$id'"),
            )
        }

        val normalizedApiKey = apiKey?.trim()?.takeIf { it.isNotBlank() }
        val normalizedApiBase = apiBase?.trim()?.trimEnd('/')?.takeIf { it.isNotBlank() }
        val normalizedModelName = modelName?.trim()?.takeIf { it.isNotBlank() }

        when (credential.llm) {
            LLM.LOCAL_MODEL -> {
                if (normalizedApiBase == null || normalizedModelName == null) {
                    return failure(
                        LLMCredentialsServiceErrors.InvalidLLMProvider(
                            "LOCAL_MODEL credentials require both apiBase and modelName in the update request",
                        ),
                    )
                }
            }

            else -> {
                if (normalizedApiBase != null || normalizedModelName != null) {
                    return failure(
                        LLMCredentialsServiceErrors.InvalidLLMProvider(
                            "apiBase and modelName are only accepted when provider is LOCAL_MODEL",
                        ),
                    )
                }
            }
        }

        val validatorInput =
            when (credential.llm) {
                LLM.LOCAL_MODEL ->
                    "${LOCAL_MODEL_VALIDATION_PREFIX}${normalizedApiBase}${LOCAL_MODEL_VALIDATION_SEPARATOR}${normalizedModelName}"

                else ->
                    normalizedApiKey
                        ?: return failure(
                            LLMCredentialsServiceErrors.InvalidApiKey("API key cannot be null or blank"),
                        )
            }

        when (val validation = llmValidator.validateApiKeyForLLM(credential.llm, validatorInput)) {
            is Failure -> return failure(mapValidatorError(validation.value))
            is Success -> {  } // é validado com sucesso, não é necessário fazer nada aqui, avançar para a atualização na BD
        }

        return trxManager.run {
            val secretToPersist =
                when (credential.llm) {
                    LLM.LOCAL_MODEL -> normalizedApiKey ?: LOCAL_MODEL_SECRET_PLACEHOLDER
                    else -> normalizedApiKey!!
                }

            // Keep the current persistence contract until the schema allows LOCAL_MODEL credentials with no stored secret.
            val apiKeyEncrypted = encryptionService.encrypt(secretToPersist)

            val updated = credential.copy(apiKeyEncrypted = apiKeyEncrypted)
            repoLLmCredentials.save(updated)
            success(updated)
        }
    }

    override fun deleteLLMCredential(userId : Int,id: Int): Either<LLMCredentialsServiceErrors, Int> =
        trxManager.run {
            val existing = repoLLmCredentials.findById(id)
                ?: return@run failure(
                    LLMCredentialsServiceErrors.LLMCredentialNotFound("Credential with id '$id' was not found"),
                )

            if(existing.userId != userId) {
                return@run failure(
                    LLMCredentialsServiceErrors.UnauthorizedAccess("User with id $userId is not the owner of credential with id '$id'"),
                )
            }

            val deleted = repoLLmCredentials.deleteById(id)
            if (!deleted) {
                return@run failure(
                    LLMCredentialsServiceErrors.PersistenceError("Could not delete credential with id '$id'"),
                )
            }

            success(id) // sendo id o id da credencial eliminada
        }

    override suspend fun validateCredential(
        userId: Int,
        id: Int,
        apiBase: String?,
        modelName: String?,
    ): Either<LLMCredentialsServiceErrors, Boolean> {
        val credential = trxManager.run {
            findUserCredentialById(userId, id)
        } ?: return failure(
            LLMCredentialsServiceErrors.LLMCredentialNotFound("Credential with id '$id' was not found for user with id '$userId'"),
        )

        val normalizedApiBase = apiBase?.trim()?.trimEnd('/')?.takeIf { it.isNotBlank() }
        val normalizedModelName = modelName?.trim()?.takeIf { it.isNotBlank() }

        val validatorInput =
            when (credential.llm) {
                LLM.LOCAL_MODEL -> {
                    if (normalizedApiBase == null || normalizedModelName == null) {
                        return failure(
                            LLMCredentialsServiceErrors.InvalidLLMProvider(
                                "LOCAL_MODEL credentials require both apiBase and modelName in the validation request",
                            ),
                        )
                    }
                    "${LOCAL_MODEL_VALIDATION_PREFIX}${normalizedApiBase}${LOCAL_MODEL_VALIDATION_SEPARATOR}${normalizedModelName}"
                }

                else -> {
                    if (normalizedApiBase != null || normalizedModelName != null) {
                        return failure(
                            LLMCredentialsServiceErrors.InvalidLLMProvider(
                                "apiBase and modelName are only accepted when provider is LOCAL_MODEL",
                            ),
                        )
                    }

                    credential.apiKeyEncrypted?.let { encryptionService.decrypt(it) }
                        ?: return failure(
                            LLMCredentialsServiceErrors.InvalidApiKey("Failed to decrypt API key or key is missing"),
                        )
                }
            }

        return when (val result = llmValidator.validateApiKeyForLLM(credential.llm, validatorInput)) {
            is Failure -> failure(mapValidatorError(result.value))
            is Success -> success(true)
        }
    }

    /**
     * Valida se o utilizador tem ou não alguma credencial com o provider especificado
     *
     * */
    private fun Transaction.canCreateLLMCredential(userId: Int, llm: LLM): Boolean =
        repoLLmCredentials.findAllByUserId(userId).none { credential -> credential.llm == llm }

    private fun Transaction.getLLMCredentialsById(id : Int): LLMCredentials? =
        repoLLmCredentials.findById(id)

    private fun Transaction.getAllUserCredentialsById(userId : Int): List<LLMCredentials> =
        repoLLmCredentials.findAllByUserId(userId)

    private fun Transaction.findUserCredentialById(userId: Int, credentialId : Int) : LLMCredentials? =
        repoLLmCredentials.findAllByUserId(userId).find { credential -> credential.id == credentialId }

    private fun mapValidatorError(error: LLMValidatorErrors): LLMCredentialsServiceErrors =
        when (error) {
            is LLMValidatorErrors.UnsupportedLLM -> LLMCredentialsServiceErrors.InvalidLLMProvider(error.message)
            is LLMValidatorErrors.InvalidAPIKey -> LLMCredentialsServiceErrors.InvalidApiKey(error.message)
            is LLMValidatorErrors.InvalidLLM -> LLMCredentialsServiceErrors.InvalidLLMProvider(error.message)
            is LLMValidatorErrors.InvalidKeyFormat -> LLMCredentialsServiceErrors.InvalidApiKey(error.message)
            is LLMValidatorErrors.UnknownError -> LLMCredentialsServiceErrors.InvalidApiKey(error.message)
        }
}
