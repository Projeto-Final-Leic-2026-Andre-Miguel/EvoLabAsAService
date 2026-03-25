package com.example.evolab.repo.repoConfig

object ConfigSql {
    private const val BASE_SELECT = """
        SELECT
            id,
            user_id AS \"userId\",
            llm_credential_id AS \"llmCredentialsId\",
            model_name AS \"modelName\",
            max_iterations AS \"maxIter\",
            checkpoint_interval AS \"checkPointInterval\",
            additional_params::text AS \"additionalParamsJson\",
            created_at AS \"createdAt\"
        FROM evolution_configs
    """

    const val CREATE_CONFIG = """
        INSERT INTO evolution_configs (
            user_id,
            llm_credential_id,
            model_name,
            max_iterations,
            checkpoint_interval,
            additional_params
        )
        VALUES (
            :userId,
            :llmCredentialsId,
            :modelName,
            :maxIter,
            :checkPointInterval,
            CAST(:additionalParams AS jsonb)
        )
        RETURNING id
    """

    const val FIND_BY_ID = """
        $BASE_SELECT
        WHERE id = :id
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

    const val FIND_ALL_BY_LLM_CREDENTIAL_ID = """
        $BASE_SELECT
        WHERE llm_credential_id = :llmCredentialsId
        ORDER BY id
    """

    const val FIND_ALL_BY_MODEL_NAME = """
        $BASE_SELECT
        WHERE model_name = :modelName
        ORDER BY id
    """

    const val SAVE = """
        UPDATE evolution_configs
        SET user_id = :userId,
            llm_credential_id = :llmCredentialsId,
            model_name = :modelName,
            max_iterations = :maxIter,
            checkpoint_interval = :checkPointInterval,
            additional_params = CAST(:additionalParams AS jsonb)
        WHERE id = :id
    """

    const val DELETE_BY_ID = """
        DELETE FROM evolution_configs
        WHERE id = :id
    """

    const val CLEAR = """
        DELETE FROM evolution_configs
    """
}