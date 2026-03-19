package pt.isel.domain

import java.time.Instant

enum class AuthProvider {
	LOCAL,
	GOOGLE,
}

data class User(
	val id: Int,
	val name: String,
	val email: String,
	val passwordHash: String?,
	val authProvider: AuthProvider,
	val providerId: String?,
	val createdAt: Instant,
)
