package com.example.evolab.repo.repoStatistics

object StatisticsSql {
    private const val BASE_SELECT = """
        SELECT
            user_id AS "userId",
            projects_created AS "projectsCreated",
            projects_executed AS "projectsExecuted",
            projects_succeeded AS "projectsSucceeded",
            projects_failed AS "projectsFailed",
            credentials_created AS "credentialsCreated",
            configs_created AS "configsCreated",
            last_project_id AS "lastProjectId",
            last_project_name AS "lastProjectName",
            last_project_created_at AS "lastProjectCreatedAt",
            updated_at AS "updatedAt"
        FROM user_statistics
    """

    const val FIND_BY_USER_ID = """
        $BASE_SELECT
        WHERE user_id = :userId
    """

    const val FIND_ALL = """
        $BASE_SELECT
        ORDER BY user_id
    """

    const val ENSURE_ROW = """
        INSERT INTO user_statistics (user_id)
        VALUES (:userId)
        ON CONFLICT (user_id) DO UPDATE
        SET updated_at = user_statistics.updated_at
    """

    const val INCREMENT_PROJECTS_CREATED = """
        INSERT INTO user_statistics (
            user_id,
            projects_created,
            last_project_id,
            last_project_name,
            last_project_created_at,
            updated_at
        )
        VALUES (:userId, 1, :projectId, :projectName, :projectCreatedAt, NOW())
        ON CONFLICT (user_id) DO UPDATE
        SET projects_created = user_statistics.projects_created + 1,
            last_project_id = EXCLUDED.last_project_id,
            last_project_name = EXCLUDED.last_project_name,
            last_project_created_at = EXCLUDED.last_project_created_at,
            updated_at = NOW()
    """

    const val INCREMENT_PROJECTS_EXECUTED = """
        INSERT INTO user_statistics (user_id, projects_executed, updated_at)
        VALUES (:userId, 1, NOW())
        ON CONFLICT (user_id) DO UPDATE
        SET projects_executed = user_statistics.projects_executed + 1,
            updated_at = NOW()
    """

    const val INCREMENT_PROJECT_SUCCESS = """
        INSERT INTO user_statistics (user_id, projects_succeeded, updated_at)
        VALUES (:userId, 1, NOW())
        ON CONFLICT (user_id) DO UPDATE
        SET projects_succeeded = user_statistics.projects_succeeded + 1,
            updated_at = NOW()
    """

    const val INCREMENT_PROJECT_FAILURE = """
        INSERT INTO user_statistics (user_id, projects_failed, updated_at)
        VALUES (:userId, 1, NOW())
        ON CONFLICT (user_id) DO UPDATE
        SET projects_failed = user_statistics.projects_failed + 1,
            updated_at = NOW()
    """

    const val INCREMENT_CREDENTIALS_CREATED = """
        INSERT INTO user_statistics (user_id, credentials_created, updated_at)
        VALUES (:userId, 1, NOW())
        ON CONFLICT (user_id) DO UPDATE
        SET credentials_created = user_statistics.credentials_created + 1,
            updated_at = NOW()
    """

    const val INCREMENT_CONFIGS_CREATED = """
        INSERT INTO user_statistics (user_id, configs_created, updated_at)
        VALUES (:userId, 1, NOW())
        ON CONFLICT (user_id) DO UPDATE
        SET configs_created = user_statistics.configs_created + 1,
            updated_at = NOW()
    """

    const val SAVE = """
        INSERT INTO user_statistics (
            user_id,
            projects_created,
            projects_executed,
            projects_succeeded,
            projects_failed,
            credentials_created,
            configs_created,
            last_project_id,
            last_project_name,
            last_project_created_at,
            updated_at
        )
        VALUES (
            :userId,
            :projectsCreated,
            :projectsExecuted,
            :projectsSucceeded,
            :projectsFailed,
            :credentialsCreated,
            :configsCreated,
            :lastProjectId,
            :lastProjectName,
            :lastProjectCreatedAt,
            :updatedAt
        )
        ON CONFLICT (user_id) DO UPDATE
        SET projects_created = EXCLUDED.projects_created,
            projects_executed = EXCLUDED.projects_executed,
            projects_succeeded = EXCLUDED.projects_succeeded,
            projects_failed = EXCLUDED.projects_failed,
            credentials_created = EXCLUDED.credentials_created,
            configs_created = EXCLUDED.configs_created,
            last_project_id = EXCLUDED.last_project_id,
            last_project_name = EXCLUDED.last_project_name,
            last_project_created_at = EXCLUDED.last_project_created_at,
            updated_at = EXCLUDED.updated_at
    """

    const val DELETE_BY_ID = """
        DELETE FROM user_statistics
        WHERE user_id = :userId
    """

    const val CLEAR = """
        DELETE FROM user_statistics
    """
}
