package com.example.evolab.repo.transactions


import com.example.evolab.repo.repoConfig.RepositoryConfigJdbi
import com.example.evolab.repo.repoCheckpoints.RepositoryCheckpointsJdbi
import com.example.evolab.repo.repoJobs.RepositoryJobsJdbi
import com.example.evolab.repo.repoLLMCredentials.RepositoryLLMCredentialsJdbi
import com.example.evolab.repo.repoMetrics.RepositoryMetricsJdbi
import com.example.evolab.repo.repoProject.RepositoryProjectJdbi
import com.example.evolab.repo.repoStatistics.RepositoryStatisticsJdbi
import com.example.evolab.repo.repoToken.RepositoryTokenJdbi
import com.example.evolab.repo.repoUser.RepositoryUserJdbi
import org.jdbi.v3.core.Handle

class TransactionJdbi(
    private val handle: Handle,
) : Transaction {
    override val repoUsers = RepositoryUserJdbi(handle)

    override val repoTokens = RepositoryTokenJdbi(handle)

    override val repoLLmCredentials = RepositoryLLMCredentialsJdbi(handle)

    override val repoConfigs = RepositoryConfigJdbi(handle)

    override val repoProjects = RepositoryProjectJdbi(handle)

    override val repoJobs = RepositoryJobsJdbi(handle)

    override val repoMetrics = RepositoryMetricsJdbi(handle)

    override val repoCheckpoints = RepositoryCheckpointsJdbi(handle)

    override val repoStatistics = RepositoryStatisticsJdbi(handle)

    override fun rollback() {
        handle.rollback()
    }
}
