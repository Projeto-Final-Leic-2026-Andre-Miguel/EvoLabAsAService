package com.example.evolab.service.jobExecution

object OpenEvolveExecutionOutcomeDecider {
    private val openAiModelName = Regex("""(?i)\bmodel\s+[`'"]?([a-z0-9._:/-]+)[`'"]?\s+does not exist""")
    private val geminiModelName = Regex("""(?i)\bmodels/([a-z0-9._:-]+)""")
    private val anthropicModelName = Regex("""(?i)\bmodel(?:\s+name)?\s*[:=]\s*[`'"]?([a-z0-9._:/-]+)""")

    private val fatalLogMarkers =
        listOf(
            "All 4 attempts failed with error:",
            "LLM generation failed:",
        )

    fun failureReason(
        exitCode: Int,
        logs: String,
    ): String? {
        modelNotFoundReason(logs)?.let { return it }

        if (exitCode != 0) {
            return "Container exit code was $exitCode"
        }

        val marker = fatalLogMarkers.firstOrNull { logs.contains(it, ignoreCase = true) }
        if (marker != null) {
            return "OpenEvolve reported terminal generation errors in logs ('$marker')"
        }

        return null
    }

    private fun modelNotFoundReason(logs: String): String? {
        val isOpenAi = logs.contains("model", ignoreCase = true) && logs.contains("does not exist", ignoreCase = true)
        val isGemini =
            logs.contains("models/", ignoreCase = true) &&
                (logs.contains("is not found", ignoreCase = true) || logs.contains("NOT_FOUND", ignoreCase = true))
        val isAnthropic =
            logs.contains("not_found_error", ignoreCase = true) && logs.contains("model", ignoreCase = true)

        if (!isOpenAi && !isGemini && !isAnthropic) return null

        val modelName =
            when {
                isOpenAi -> openAiModelName.find(logs)?.groupValues?.get(1)
                isGemini -> geminiModelName.find(logs)?.groupValues?.get(1)
                else -> anthropicModelName.find(logs)?.groupValues?.get(1)
            }

        return if (modelName != null) {
            "The model '$modelName' was not recognized by the LLM provider. Check the exact model id in your Configuration."
        } else {
            "The configured model was not recognized by the LLM provider. Check the exact model id in your Configuration."
        }
    }
}

