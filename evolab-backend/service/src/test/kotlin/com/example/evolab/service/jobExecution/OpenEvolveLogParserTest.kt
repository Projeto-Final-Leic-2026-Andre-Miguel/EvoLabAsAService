package com.example.evolab.service.jobExecution

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OpenEvolveLogParserTest {
    @Test
    fun parsesIterationMetricsWhenCombinedScoreIsNotFirstMetric() {
        val logs =
            """
            2026-05-13 19:24:14,382 - openevolve.process_parallel - INFO - Iteration 1: Program 26acd0fc-627e-4675-9ebe-2ce055dd83db (parent: d181f3af-efbd-4dd7-9e42-8283899b8032) completed in 10.86s
            2026-05-13 19:24:14,382 - openevolve.process_parallel - INFO - Metrics: runs_successfully=1.0000, value_score=0.6379, distance_score=0.2505, combined_score=0.5941, reliability_score=1.0000
            2026-05-13 19:24:40,978 - openevolve.process_parallel - INFO - Iteration 9: Program 8fdf41de-887c-4b53-9fd6-000dc53bbcb3 (parent: d181f3af-efbd-4dd7-9e42-8283899b8032) completed in 14.58s
            2026-05-13 19:24:40,978 - openevolve.process_parallel - INFO - Metrics: runs_successfully=1.0000, value_score=0.9996, distance_score=0.9937, combined_score=1.4969, reliability_score=1.0000
            """.trimIndent()

        val result = OpenEvolveLogParser.parseIterationMetrics(logs)

        assertEquals(2, result.size)
        assertEquals(1, result[0].iteration)
        assertEquals(0.5941, result[0].fitnessScore)
        assertEquals(10.86, result[0].executionTime)
        assertEquals(9, result[1].iteration)
        assertEquals(1.4969, result[1].fitnessScore)
        assertEquals(14.58, result[1].executionTime)
    }
}
