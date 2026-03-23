package com.example.evolab.repo.repoCheckpoints

object CheckpointsSql {
    private const val BASE_SELECT = """
        SELECT
            id,
            job_id AS \"jobId\",
            metrics_id AS \"metricsId\",
            iteration,
            solution,
            created_at AS \"createdAt\"
        FROM checkpoints
    """

    const val CREATE_CHECKPOINT = """
        INSERT INTO checkpoints (
            job_id,
            metrics_id,
            iteration,
            solution
        )
        VALUES (
            :jobId,
            :metricsId,
            :iteration,
            :solution
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

    const val FIND_ALL_BY_METRICS_ID = """
        $BASE_SELECT
        WHERE metrics_id = :metricsId
        ORDER BY id
    """

    const val FIND_BY_JOB_ID_AND_ITERATION = """
        $BASE_SELECT
        WHERE job_id = :jobId AND iteration = :iteration
    """

    const val SAVE = """
        UPDATE checkpoints
        SET job_id = :jobId,
            metrics_id = :metricsId,
            iteration = :iteration,
            solution = :solution
        WHERE id = :id
    """

    const val DELETE_BY_ID = """
        DELETE FROM checkpoints
        WHERE id = :id
    """

    const val CLEAR = """
        DELETE FROM checkpoints
    """
}

