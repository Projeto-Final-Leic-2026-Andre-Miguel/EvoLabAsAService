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

    override suspend fun createLLMCredential(
        userId: Int,
        llm: LLM,
        apiKey: String,
    ): Either<LLMCredentialsServiceErrors, LLMCredentials> {

        when (val validation = llmValidator.validateApiKeyForLLM(llm, apiKey)) {
            is Failure -> return failure(mapValidatorError(validation.value))
            is Success -> {  }
        }

        return trxManager.run {
            if (!canCreateLLMCredential(userId, llm)) {
                return@run failure(
                    LLMCredentialsServiceErrors.CredentialWithProviderAlreadyInUse(
                        "User already has credentials for provider '$llm'"
                    )
                )
            }

            val apiKeyEncrypted = encryptionService.encrypt(apiKey)

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
     *
     * Aqui a unica coisa que se pode atualizar é a api_key
     * A validação da chave é feita antes de entrar na transação, para evitar fazer uma transação de escrita desnecessária caso a chave seja inválida
     * Portanto começamos por verificar se a credencial pertence ou não ao utilizador, depois validamos a chave, e só depois entramos na transação para atualizar a chave na BD
     * O bloco {} em is Sucess, é intencionalmente deixado vazio, porque se a validação for bem sucedida, não é necessário fazer nada nesse momento, apenas avançar para a atualização na BD
     */

    override suspend fun updateLLMCredential(
        id: Int,
        userId: Int,
        apiKey: String,
    ): Either<LLMCredentialsServiceErrors, LLMCredentials> {

        val credential = trxManager.run {
            repoLLmCredentials.findById(id)
        } ?: return failure(LLMCredentialsServiceErrors.LLMCredentialNotFound("Credential with id '$id' not found"))

        if(credential.userId != userId) {
            return failure(
                LLMCredentialsServiceErrors.UnauthorizedAccess("User with id $userId is not the owner of credential with id '$id'"),
            )
        }


        when (val validation = llmValidator.validateApiKeyForLLM(credential.llm, apiKey)) {
            is Failure -> return failure(mapValidatorError(validation.value))
            is Success -> {  } // é validado com sucesso, não é necessário fazer nada aqui, avançar para a atualização na BD
        }

        return trxManager.run {
            val apiKeyEncrypted = encryptionService.encrypt(apiKey)

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
