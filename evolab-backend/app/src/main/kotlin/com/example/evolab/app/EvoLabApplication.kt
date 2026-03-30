package com.example.evolab.app

import com.example.evolab.domain.user.UsersDomainConfig
import com.example.evolab.repo.repoToken.RepositoryToken
import com.example.evolab.repo.repoToken.RepositoryTokenJdbi
import com.example.evolab.repo.repoUser.RepositoryUser
import com.example.evolab.repo.repoUser.RepositoryUserJdbi
import com.example.evolab.repo.transactions.TransactionManager
import com.example.evolab.repo.transactions.TransactionManagerJdbi
import org.jdbi.v3.core.Jdbi
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import pt.isel.domain.authentication.Sha256TokenEncoder
import pt.isel.domain.token.TokenEncoder
import java.time.Clock
import java.time.Duration
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.postgres.PostgresPlugin

@SpringBootApplication(scanBasePackages = ["com.example.evolab", "pt.isel"])
class EvoLabApplication {


    @Bean
    fun jdbi(): Jdbi =
        Jdbi.create(
            PGSimpleDataSource().apply {
                setURL(Environment.getUrl())
            }
        )
            .installPlugin(KotlinPlugin())
            .installPlugin(PostgresPlugin())

    @Bean(destroyMethod = "close")
    fun httpClient(): HttpClient {
        return HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
        }
    }

    @Bean
    fun transactionManager(jdbi: Jdbi): TransactionManager = TransactionManagerJdbi(jdbi)

    @Bean
    fun repositoryUser(jdbi: Jdbi): RepositoryUser = RepositoryUserJdbi(jdbi.open())

    @Bean
    fun repositoryToken(jdbi: Jdbi): RepositoryToken = RepositoryTokenJdbi(jdbi.open())

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun tokenEncoder(): TokenEncoder = Sha256TokenEncoder()

    @Bean
    fun usersDomainConfig(): UsersDomainConfig =
        UsersDomainConfig(
            tokenSizeInBytes = 32,
            tokenTtl = Duration.ofHours(24),
            tokenRollingTtl = Duration.ofHours(1),
            maxTokensPerUser = 3,
        )


    @Bean
    fun clock(): Clock = Clock.systemUTC()

}

fun main(args: Array<String>) {
    runApplication<EvoLabApplication>(*args)
}
