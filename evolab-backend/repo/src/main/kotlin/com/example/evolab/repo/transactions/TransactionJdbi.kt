package com.example.evolab.repo.transactions


import com.example.evolab.repo.repoLLMCredentials.RepositoryLLMCredentialsJdbi
import com.example.evolab.repo.repoUser.RepositoryUserJdbi
import org.jdbi.v3.core.Handle

class TransactionJdbi(
    private val handle: Handle,
) : Transaction {
    override val repoUsers = RepositoryUserJdbi(handle)

    override val repoLLmCredentials = RepositoryLLMCredentialsJdbi(handle)


    override fun rollback() {
        handle.rollback()
    }
}