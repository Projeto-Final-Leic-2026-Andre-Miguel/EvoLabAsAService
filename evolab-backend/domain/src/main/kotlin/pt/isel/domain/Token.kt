package pt.isel.domain

data class Token(
	val tokenValidation: String,
	val userId: Int,
	val createdAt: Long,
	val lastUsedAt: Long,
)
