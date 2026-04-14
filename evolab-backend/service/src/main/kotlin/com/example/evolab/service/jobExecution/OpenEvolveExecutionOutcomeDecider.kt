package com.example.evolab.service.jobExecution

object OpenEvolveExecutionOutcomeDecider {
    private val fatalLogMarkers =
        listOf(
            "All 4 attempts failed with error:",
            "LLM generation failed:",
        )

    fun failureReason(
        exitCode: Int,
        logs: String,
    ): String? {
        if (exitCode != 0) {
            return "Container exit code was $exitCode"
        }

        val marker = fatalLogMarkers.firstOrNull { logs.contains(it, ignoreCase = true) }
        if (marker != null) {
            return "OpenEvolve reported terminal generation errors in logs ('$marker')"
        }

        return null
    }
}

