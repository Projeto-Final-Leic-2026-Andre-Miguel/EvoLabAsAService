package com.example.evolab.app

import com.example.evolab.domain.user.UsersDomainConfig
import com.example.evolab.repo.transactions.TransactionManager
import com.example.evolab.repo.transactions.TransactionManagerJdbi
import org.jdbi.v3.core.Jdbi
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import pt.isel.domain.authentication.Sha256TokenEncoder
import pt.isel.domain.token.TokenEncoder
import java.time.Clock
import java.time.Duration

@SpringBootApplication(scanBasePackages = ["com.example.evolab", "pt.isel"])
class EvoLabApplication {


    @Bean
    fun jdbi() : Jdbi =
        Jdbi.create(
            PGSimpleDataSource().apply {
                setURL(Environment.getUrl())
            }
        )

    @Bean
    fun transactionManager(jdbi: Jdbi): TransactionManager = TransactionManagerJdbi(jdbi)

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

