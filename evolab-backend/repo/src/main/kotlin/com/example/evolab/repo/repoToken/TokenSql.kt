package com.example.evolab.repo.repoToken

object TokenSql {
    private const val BASE_SELECT = """
		SELECT
			token_validation AS \"tokenValidation\",
			user_id AS \"userId\",
			created_at AS \"createdAt\",
			last_used_at AS \"lastUsedAt\"
		FROM tokens
	"""

    const val CREATE_TOKEN = """
		INSERT INTO tokens (
			token_validation,
			user_id,
			created_at,
			last_used_at
		)
		VALUES (
			:tokenValidation,
			:userId,
			:createdAt,
			:lastUsedAt
		)
	"""

    const val FIND_BY_TOKEN_VALIDATION = """
		$BASE_SELECT
		WHERE token_validation = :tokenValidation
	"""

    const val FIND_ALL_BY_USER_ID = """
		$BASE_SELECT
		WHERE user_id = :userId
		ORDER BY last_used_at DESC, created_at DESC
	"""

    const val GET_USER_AND_TOKEN_BY_VALIDATION = """
		SELECT
			u.id AS u_id,
			u.name AS u_name,
			u.email AS u_email,
			u.password_hash AS u_password_hash,
			u.auth_provider AS u_auth_provider,
			u.provider_id AS u_provider_id,
			u.created_at AS u_created_at,
			t.token_validation AS t_token_validation,
			t.user_id AS t_user_id,
			t.created_at AS t_created_at,
			t.last_used_at AS t_last_used_at
		FROM tokens t
		JOIN users u ON u.id = t.user_id
		WHERE t.token_validation = :tokenValidation
	"""

    const val DELETE_OLDEST_TOKENS = """
		DELETE FROM tokens
		WHERE token_validation IN (
			SELECT token_validation
			FROM tokens
			WHERE user_id = :userId
            ORDER BY last_used_at DESC, created_at DESC
            OFFSET :offset
		)
	"""

    const val UPDATE_TOKEN_LAST_USED = """
		UPDATE tokens
		SET last_used_at = :lastUsedAt
		WHERE token_validation = :tokenValidation
	"""

    const val REMOVE_TOKEN_BY_VALIDATION = """
		DELETE FROM tokens
		WHERE token_validation = :tokenValidation
	"""
}