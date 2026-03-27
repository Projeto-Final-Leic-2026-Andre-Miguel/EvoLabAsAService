package com.example.evolab.app

import com.example.evolab.domain.user.UsersDomainConfig
import com.example.evolab.repo.repoCheckpoints.RepositoryCheckpoints
import com.example.evolab.repo.repoCheckpoints.RepositoryCheckpointsJdbi
import com.example.evolab.repo.repoConfig.RepositoryConfig
import com.example.evolab.repo.repoConfig.RepositoryConfigJdbi
import com.example.evolab.repo.repoJobs.RepositoryJobs
import com.example.evolab.repo.repoJobs.RepositoryJobsJdbi
import com.example.evolab.repo.repoLLMCredentials.RepositoryLLMCredentials
import com.example.evolab.repo.repoLLMCredentials.RepositoryLLMCredentialsJdbi
import com.example.evolab.repo.repoMetrics.RepositoryMetrics
import com.example.evolab.repo.repoMetrics.RepositoryMetricsJdbi
import com.example.evolab.repo.repoProject.RepositoryProject
import com.example.evolab.repo.repoProject.RepositoryProjectJdbi
import com.example.evolab.repo.repoToken.RepositoryToken
import com.example.evolab.repo.repoToken.RepositoryTokenJdbi
import com.example.evolab.repo.repoUser.RepositoryUser
import com.example.evolab.repo.repoUser.RepositoryUserJdbi
import com.example.evolab.repo.transactions.TransactionManager
import com.example.evolab.repo.transactions.TransactionManagerJdbi
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.postgres.PostgresPlugin
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import pt.isel.domain.authentication.Sha256TokenEncoder
import pt.isel.domain.token.TokenEncoder
import java.time.Clock
import java.time.Duration

@Configuration
class AppConfig {

}