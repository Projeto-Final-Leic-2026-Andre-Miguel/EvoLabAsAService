package com.example.evolab.repo.repoLLMCredentials

object LLMSql {
	private const val BASE_SELECT = """
		SELECT
			id,
			user_id AS \"userId\",
			provider AS llm,
			api_key_encrypted AS \"apiKeyEncrypted\",
			created_at AS \"createdAt\"
		FROM llm_credentials
	"""

        const val CREATE_CREDENTIAL = """
                INSERT INTO llm_credentials (user_id, provider, api_key_encrypted)
                VALUES (:userId, CAST(:provider AS llm_provider), :apiKeyEncrypted)
                RETURNING
                        id,
                        user_id AS \"userId\",
                        provider AS llm,
                        api_key_encrypted AS \"apiKeyEncrypted\",
                        created_at AS \"createdAt\"
        """

        const val CREATE_LOCAL_CREDENTIAL = """
                WITH new_cred AS (
                        INSERT INTO llm_credentials (user_id, provider, api_key_encrypted)
                        VALUES (:userId, CAST(:provider AS llm_provider), :apiKeyEncrypted)
                        RETURNING id, user_id, provider, api_key_encrypted, created_at
                ),
                new_local AS (
                        INSERT INTO local_model_credentials (credential_id, port, model_name)
                        SELECT id, :port, :modelName FROM new_cred
                        RETURNING port, model_name
                )
                SELECT
                        new_cred.id,
                        new_cred.user_id AS "userId",
                        new_cred.provider AS llm,
                        new_cred.api_key_encrypted AS "apiKeyEncrypted",
                        new_local.port,
                        new_local.model_name AS "modelName",
                        new_cred.created_at AS "createdAt"
                FROM new_cred, new_local
        """

        const val FIND_BY_ID = """
                $BASE_SELECT
                WHERE id = :id
        """

        const val FIND_LOCAL_BY_ID = """
                SELECT 
                        c.id, 
                        c.user_id AS "userId", 
                        c.provider AS llm, 
                        c.api_key_encrypted AS "apiKeyEncrypted", 
                        c.created_at AS "createdAt",
                        l.port, 
                        l.model_name AS "modelName"
                FROM llm_credentials c
                JOIN local_model_credentials l ON l.credential_id = c.id
                WHERE c.id = :id
        """

	const val FIND_ALL = """
		$BASE_SELECT
		ORDER BY id
	"""

	const val FIND_ALL_BY_USER_ID = """
		$BASE_SELECT
		WHERE user_id = :userId
		ORDER BY id
	"""

	const val FIND_ALL_BY_PROVIDER = """
		$BASE_SELECT
		WHERE provider = CAST(:provider AS llm_provider)
		ORDER BY id
	"""

	const val SAVE = """
		UPDATE llm_credentials
		SET user_id = :userId,
			provider = CAST(:provider AS llm_provider),
			api_key_encrypted = :apiKeyEncrypted
		WHERE id = :id
	"""

	const val DELETE_BY_ID = """
		DELETE FROM llm_credentials
		WHERE id = :id
	"""

	const val CLEAR = """
		DELETE FROM llm_credentials
	"""
}