package com.example.evolab.service.jobExecution

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class OpenEvolveExecutionOutcomeDeciderTest {
    @Test
    fun returnsFailureWhenExitCodeIsNotZero() {
        val reason = OpenEvolveExecutionOutcomeDecider.failureReason(2, "")

        assertEquals("Container exit code was 2", reason)
    }

    @Test
    fun returnsFailureWhenLogsContainTerminalGenerationError() {
        val reason =
            OpenEvolveExecutionOutcomeDecider.failureReason(
                0,
                "2026-01-01 - ERROR - LLM generation failed: Error code: 429",
            )

        assertNotNull(reason)
    }

    @Test
    fun returnsSuccessWhenExitCodeIsZeroAndLogsHaveNoFatalMarkers() {
        val reason =
            OpenEvolveExecutionOutcomeDecider.failureReason(
                0,
                "Evolution complete!",
            )

        assertNull(reason)
    }
}

