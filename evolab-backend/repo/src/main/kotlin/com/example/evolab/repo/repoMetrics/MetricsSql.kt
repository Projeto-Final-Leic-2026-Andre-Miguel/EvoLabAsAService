package com.example.evolab.repo.repoMetrics

object MetricsSql {
    private const val BASE_SELECT = """
        SELECT
            id,
            job_id AS \"jobId\",
            iteration,
            fitness_score AS \"fitnessScore\",
            execution_time AS \"executionTime\",
            created_at AS \"createdAt\"
        FROM metrics
    """

    const val CREATE_METRIC = """
        INSERT INTO metrics (
            job_id,
            iteration,
            fitness_score,
            execution_time
        )
        VALUES (
            :jobId,
            :iteration,
            :fitnessScore,
            :executionTime
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

    const val FIND_ALL_BY_JOB_ID = """
        $BASE_SELECT
        WHERE job_id = :jobId
        ORDER BY iteration
    """

    const val FIND_BY_JOB_ID_AND_ITERATION = """
        $BASE_SELECT
        WHERE job_id = :jobId AND iteration = :iteration
    """

    const val SAVE = """
        UPDATE metrics
        SET job_id = :jobId,
            iteration = :iteration,
            fitness_score = :fitnessScore,
            execution_time = :executionTime
        WHERE id = :id
    """

    const val DELETE_BY_ID = """
        DELETE FROM metrics
        WHERE id = :id
    """

    const val CLEAR = """
        DELETE FROM metrics
    """
}

