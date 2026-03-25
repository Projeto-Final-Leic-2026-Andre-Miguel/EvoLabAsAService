package com.example.evolab.repo.repoToken

import com.example.evolab.domain.token.Token
import com.example.evolab.domain.token.TokenValidationInfo
import com.example.evolab.domain.user.User

interface RepositoryToken {
	fun createToken(
		token: Token,
		maxTokens: Int,
	)

	fun findByTokenValidation(tokenValidation: TokenValidationInfo): Token?

	fun findAllByUserId(userId: Int): List<Token>

	fun getTokenByTokenValidationInfo(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>?

	fun updateTokenLastUsed(
		tokenValidationInfo: TokenValidationInfo,
		now: Long,
	)

	fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int
}