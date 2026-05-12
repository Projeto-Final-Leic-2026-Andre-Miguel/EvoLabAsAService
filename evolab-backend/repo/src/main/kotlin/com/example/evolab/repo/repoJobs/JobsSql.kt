package com.example.evolab.repo.repoJobs

object JobsSql {
    private const val BASE_SELECT = """
        SELECT
            id,
            project_id AS \"projectId\",
            status,
            container_id AS \"containerId\",
            started_at AS \"startedAt\",
            finished_at AS \"finishedAt\",
            best_solution AS \"bestSolution\",
            execution_logs AS \"executionLogs\",
            failure_reason AS \"failureReason\",
            created_at AS \"createdAt\"
        FROM jobs
    """

    const val CREATE_JOB = """
        INSERT INTO jobs (
            project_id,
            status,
            container_id,
            started_at,
            finished_at,
            best_solution,
            execution_logs,
            failure_reason
        )
        VALUES (
            :projectId,
            CAST(:status AS job_status),
            :containerId,
            :startedAt,
            :finishedAt,
            :bestSolution,
            :executionLogs,
            :failureReason
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

    const val FIND_ALL_BY_PROJECT_ID = """
        $BASE_SELECT
        WHERE project_id = :projectId
        ORDER BY id
    """

    const val FIND_ALL_BY_STATUS = """
        $BASE_SELECT
        WHERE status = CAST(:status AS job_status)
        ORDER BY id
    """

    const val FIND_BY_CONTAINER_ID = """
        $BASE_SELECT
        WHERE container_id = :containerId
    """

    const val SAVE = """
        UPDATE jobs
        SET project_id = :projectId,
            status = CAST(:status AS job_status),
            container_id = :containerId,
            started_at = :startedAt,
            finished_at = :finishedAt,
            best_solution = :bestSolution,
            execution_logs = :executionLogs,
            failure_reason = :failureReason
        WHERE id = :id
    """

    const val DELETE_BY_ID = """
        DELETE FROM jobs
        WHERE id = :id
    """

    const val CLEAR = """
        DELETE FROM jobs
    """
}

