package com.example.evolab.app

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

@Configuration
class AppConfig {

    // ADICIONAR AQUI OS INTERCEPTORS E ARGUMENT RESOLVERS


}

