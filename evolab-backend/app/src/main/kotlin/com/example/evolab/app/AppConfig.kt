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

	@Bean
	fun dbUrl(
		@Value("\${DB_URL:jdbc:postgresql://localhost:5432/db?user=dbuser&password=changeit}") dbUrl: String,
	): String = dbUrl

	@Bean
	fun jdbi(dbUrl: String): Jdbi =
		Jdbi.create(dbUrl)
			.installPlugin(KotlinPlugin())
			.installPlugin(PostgresPlugin())

	@Bean
	fun transactionManager(jdbi: Jdbi): TransactionManager = TransactionManagerJdbi(jdbi)

	@Bean
	fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}

