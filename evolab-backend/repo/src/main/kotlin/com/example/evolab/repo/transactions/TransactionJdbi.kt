package com.example.evolab.repo.transactions


import com.example.evolab.repo.repoUser.RepositoryUserJdbi
import org.jdbi.v3.core.Handle

class TransactionJdbi(
    private val handle: Handle,
) : Transaction {
    override val repoUsers = RepositoryUserJdbi(handle)

    override fun rollback() {
        handle.rollback()
    }
}