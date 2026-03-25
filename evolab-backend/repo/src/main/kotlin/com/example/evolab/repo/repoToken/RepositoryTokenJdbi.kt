package com.example.evolab.repo.repoToken

import com.example.evolab.domain.token.Token
import com.example.evolab.domain.token.TokenValidationInfo
import com.example.evolab.domain.user.AuthProvider
import com.example.evolab.domain.user.User
import com.example.evolab.repo.repoUser.RepositoryUserJdbi
import org.jdbi.v3.core.Handle
import org.slf4j.LoggerFactory

class RepositoryTokenJdbi(
	private val handle: Handle,
) : RepositoryToken {

	override fun createToken(
		token: Token,
		maxTokens: Int,
	) {
		handle
			.createUpdate(TokenSql.DELETE_OLDEST_TOKENS)
			.bind("userId", token.userId)
			.bind("offset", maxTokens - 1)
			.execute()

		logger.info("{} tokens deleted when creating new token")

		handle
			.createUpdate(TokenSql.CREATE_TOKEN)
			.bind("tokenValidation", token.tokenValidation)
			.bind("userId", token.userId)
			.bind("createdAt", token.createdAt)
			.bind("lastUsedAt", token.lastUsedAt)
			.execute()
	}

	override fun findByTokenValidation(tokenValidation: TokenValidationInfo): Token? =
		handle
			.createQuery(TokenSql.FIND_BY_TOKEN_VALIDATION)
			.bind("tokenValidation", tokenValidation)
			.map { rs, _ -> rs.toToken() }
			.findOne()
			.orElse(null)

	override fun findAllByUserId(userId: Int): List<Token> =
		handle
			.createQuery(TokenSql.FIND_ALL_BY_USER_ID)
			.bind("userId", userId)
			.map { rs, _ -> rs.toToken() }
			.list()

	override fun getTokenByTokenValidationInfo(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>? =
		handle
			.createQuery(TokenSql.GET_USER_AND_TOKEN_BY_VALIDATION)
			.bind("tokenValidation", tokenValidationInfo.validationInfo)
			.map { rs, _ ->
				val user =
					User(
						id = rs.getInt("u_id"),
						name = rs.getString("u_name"),
						email = rs.getString("u_email"),
						passwordHash = rs.getString("u_password_hash"),
						authProvider = AuthProvider.valueOf(rs.getString("u_auth_provider")),
						providerId = rs.getString("u_provider_id"),
						createdAt = rs.getTimestamp("u_created_at").toInstant(),
					)
				val token =
					Token(
						tokenValidation = rs.getString("t_token_validation"),
						userId = rs.getInt("t_user_id"),
						createdAt = rs.getLong("t_created_at"),
						lastUsedAt = rs.getLong("t_last_used_at"),
					)
				Pair(user, token)
			}
			.findOne()
			.orElse(null)

	override fun updateTokenLastUsed(
		tokenValidationInfo: TokenValidationInfo,
		now: Long,
	) {
		handle
			.createUpdate(TokenSql.UPDATE_TOKEN_LAST_USED)
			.bind("lastUsedAt", now)
			.bind("tokenValidation", tokenValidationInfo.validationInfo)
			.execute()
	}

	override fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int =
		handle
			.createUpdate(TokenSql.REMOVE_TOKEN_BY_VALIDATION)
			.bind("tokenValidation", tokenValidationInfo.validationInfo)
			.execute()

	private fun java.sql.ResultSet.toToken(): Token =
		Token(
			tokenValidation = getString("tokenValidation"),
			userId = getInt("userId"),
			createdAt = getLong("createdAt"),
			lastUsedAt = getLong("lastUsedAt"),
		)

	companion object {
		private val logger = LoggerFactory.getLogger(RepositoryUserJdbi::class.java)

	}
}