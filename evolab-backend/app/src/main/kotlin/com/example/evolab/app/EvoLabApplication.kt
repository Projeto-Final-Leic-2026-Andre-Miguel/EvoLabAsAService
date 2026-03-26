package com.example.evolab.app

import com.example.evolab.repo.transactions.TransactionManager
import com.example.evolab.repo.transactions.TransactionManagerJdbi
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.postgres.PostgresPlugin
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@SpringBootApplication
class EvoLabApplication{

    @Bean
    fun jdbi() : Jdbi =
        Jdbi.create(
                PGSimpleDataSource().apply {
                    setURL(Environment.getUrl())
                }
            )

    @Bean
    fun transactionManager(jdbi : Jdbi): TransactionManager = TransactionManagerJdbi(jdbi)

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

}

fun main() {
    runApplication<EvoLabApplication>()
}

