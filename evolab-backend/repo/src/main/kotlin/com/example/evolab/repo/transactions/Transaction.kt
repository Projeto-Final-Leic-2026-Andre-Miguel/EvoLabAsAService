package com.example.evolab.repo.transactions

import com.example.evolab.repo.repoConfig.RepositoryConfig
import com.example.evolab.repo.repoCheckpoints.RepositoryCheckpoints
import com.example.evolab.repo.repoJobs.RepositoryJobs
import com.example.evolab.repo.repoLLMCredentials.RepositoryLLMCredentials
import com.example.evolab.repo.repoMetrics.RepositoryMetrics
import com.example.evolab.repo.repoProject.RepositoryProject
import com.example.evolab.repo.repoToken.RepositoryToken
import com.example.evolab.repo.repoUser.RepositoryUser

/**
 * The lifecycle of a Transaction is managed outside the scope of the IoC/DI container.
 * Transactions are instantiated by a TransactionManager,
 * which is managed by the IoC/DI container (e.g., Spring).
 * The implementation of Transaction is responsible for creating the
 * necessary repository instances in its constructor.
 */
interface Transaction {
    val repoUsers: RepositoryUser
    val repoLLmCredentials: RepositoryLLMCredentials
    val repoConfigs: RepositoryConfig
    val repoProjects: RepositoryProject
    val repoJobs: RepositoryJobs
    val repoMetrics: RepositoryMetrics
    val repoCheckpoints: RepositoryCheckpoints
    val repoTokens: RepositoryToken

    fun rollback()
}