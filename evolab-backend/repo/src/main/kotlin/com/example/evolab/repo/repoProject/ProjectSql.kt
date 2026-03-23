package com.example.evolab.repo.repoProject

object ProjectSql {
    private const val BASE_SELECT = """
        SELECT
            id,
            user_id AS \"userId\",
            config_id AS \"configId\",
            name,
            description,
            initial_program AS \"initialProgram\",
            evaluator_code AS \"evaluatorCode\",
            status,
            created_at AS \"createdAt\"
        FROM projects
    """

    const val CREATE_PROJECT = """
        INSERT INTO projects (
            user_id,
            config_id,
            name,
            description,
            initial_program,
            evaluator_code,
            status
        )
        VALUES (
            :userId,
            :configId,
            :name,
            :description,
            :initialProgram,
            :evaluatorCode,
            CAST(:status AS job_status)
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

    const val FIND_ALL_BY_CONFIG_ID = """
        $BASE_SELECT
        WHERE config_id = :configId
        ORDER BY id
    """

    const val FIND_ALL_BY_STATUS = """
        $BASE_SELECT
        WHERE status = CAST(:status AS job_status)
        ORDER BY id
    """

    const val FIND_ALL_BY_NAME = """
        $BASE_SELECT
        WHERE name = :name
        ORDER BY id
    """

    const val SAVE = """
        UPDATE projects
        SET user_id = :userId,
            config_id = :configId,
            name = :name,
            description = :description,
            initial_program = :initialProgram,
            evaluator_code = :evaluatorCode,
            status = CAST(:status AS job_status)
        WHERE id = :id
    """

    const val DELETE_BY_ID = """
        DELETE FROM projects
        WHERE id = :id
    """

    const val CLEAR = """
        DELETE FROM projects
    """
}

