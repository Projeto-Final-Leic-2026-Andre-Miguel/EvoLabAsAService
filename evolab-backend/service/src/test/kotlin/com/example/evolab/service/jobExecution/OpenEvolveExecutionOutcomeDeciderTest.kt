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
    fun returnsSpecificFailureForOpenAiMissingModel() {
        val reason =
            OpenEvolveExecutionOutcomeDecider.failureReason(
                1,
                "Error: The model `gpt-4.9-missing` does not exist or you do not have access to it.",
            )

        assertEquals(
            "The model 'gpt-4.9-missing' was not recognized by the LLM provider. Check the exact model id in your Configuration.",
            reason,
        )
    }

    @Test
    fun returnsSpecificFailureForGeminiMissingModel() {
        val reason =
            OpenEvolveExecutionOutcomeDecider.failureReason(
                0,
                "google.api_core.exceptions.NotFound: 404 models/gemini-missing is not found for API version v1beta",
            )

        assertEquals(
            "The model 'gemini-missing' was not recognized by the LLM provider. Check the exact model id in your Configuration.",
            reason,
        )
    }

    @Test
    fun returnsSpecificFailureForGeminiNotFoundCode() {
        val reason =
            OpenEvolveExecutionOutcomeDecider.failureReason(
                0,
                """{"code":404,"status":"NOT_FOUND","message":"models/gemini-unknown is unavailable"}""",
            )

        assertEquals(
            "The model 'gemini-unknown' was not recognized by the LLM provider. Check the exact model id in your Configuration.",
            reason,
        )
    }

    @Test
    fun returnsSpecificFailureForAnthropicMissingModel() {
        val reason =
            OpenEvolveExecutionOutcomeDecider.failureReason(
                0,
                """anthropic.NotFoundError: {"type":"not_found_error","message":"model: claude-missing"}""",
            )

        assertEquals(
            "The model 'claude-missing' was not recognized by the LLM provider. Check the exact model id in your Configuration.",
            reason,
        )
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
