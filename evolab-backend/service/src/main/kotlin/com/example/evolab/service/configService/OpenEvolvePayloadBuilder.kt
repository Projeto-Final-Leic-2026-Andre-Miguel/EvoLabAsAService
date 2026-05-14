package com.example.evolab.service.configService

import com.example.evolab.domain.LLMCredentials.LLM
import com.example.evolab.domain.config.Config

object OpenEvolvePayloadBuilder {
    private const val OPENAI_API_BASE = "https://api.openai.com/v1"
    private const val GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/openai/"

    fun build(
        config: Config,
        apiKeyValue: String? = config.additionalParams["llm.api_key"],
    ): Map<String, Any> {
        val p = config.additionalParams

        val featureDimensions =
            p["database.feature_dimensions"]
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: listOf("complexity", "diversity")

        val declaredCustomMetrics =
            featureDimensions
                .filter { it !in setOf("complexity", "diversity") }

        val llmPayload =
            mutableMapOf<String, Any>(
                "models" to listOf(mapOf("name" to config.modelName, "weight" to 1.0)),
                "api_base" to (p["llm.api_base"] ?: ""),
                "api_key" to (apiKeyValue ?: ""),
                "temperature" to p["llm.temperature"].toDoubleOrNullOrDefault(0.7),
                "top_p" to p["llm.top_p"].toDoubleOrNullOrDefault(0.95),
                "max_tokens" to p["llm.max_tokens"].toIntOrNullOrDefault(4096),
                "timeout" to p["llm.timeout"].toIntOrNullOrDefault(60),
                "retries" to p["llm.retries"].toIntOrNullOrDefault(3),
            )
        resolveReasoningEffort(config.modelName, p["llm.api_base"], p["llm.reasoning_effort"])
            ?.let { llmPayload["reasoning_effort"] = it }

        return mapOf(
            "max_iterations" to config.maxIter,
            "checkpoint_interval" to config.checkPointInterval,
            "diff_based_evolution" to p["diff_based_evolution"].toBooleanStrictOrNullOrDefault(true),
            "llm" to llmPayload,
            "prompt" to
                mapOf(
                    "system_message" to
                        (p["prompt.system_message"]
                            ?: "You are an OpenEvolve assistant. Propose iterative improvements while preserving correctness."),
                    "num_top_programs" to p["prompt.num_top_programs"].toIntOrNullOrDefault(3),
                    "num_diverse_programs" to p["prompt.num_diverse_programs"].toIntOrNullOrDefault(2),
                    "include_artifacts" to p["prompt.include_artifacts"].toBooleanStrictOrNullOrDefault(true),
                ),
            "database" to
                mapOf(
                    "population_size" to p["database.population_size"].toIntOrNullOrDefault(100),
                    "archive_size" to p["database.archive_size"].toIntOrNullOrDefault(50),
                    "num_islands" to p["database.num_islands"].toIntOrNullOrDefault(4),
                    "migration_interval" to p["database.migration_interval"].toIntOrNullOrDefault(10),
                    "migration_rate" to p["database.migration_rate"].toDoubleOrNullOrDefault(0.1),
                    "elite_selection_ratio" to p["database.elite_selection_ratio"].toDoubleOrNullOrDefault(0.1),
                    "exploration_ratio" to p["database.exploration_ratio"].toDoubleOrNullOrDefault(0.2),
                    "exploitation_ratio" to p["database.exploitation_ratio"].toDoubleOrNullOrDefault(0.7),
                    "feature_dimensions" to featureDimensions,
                    "feature_bins" to p["database.feature_bins"].toIntOrNullOrDefault(10),
                ),
            "evaluator" to
                mapOf(
                    "timeout" to p["evaluator.timeout"].toIntOrNullOrDefault(300),
                    "max_retries" to p["evaluator.max_retries"].toIntOrNullOrDefault(3),
                    "parallel_evaluations" to p["evaluator.parallel_evaluations"].toIntOrNullOrDefault(4),
                    "cascade_evaluation" to p["evaluator.cascade_evaluation"].toBooleanStrictOrNullOrDefault(true),
                    "cascade_thresholds" to
                        listOf(
                            p["evaluator.cascade_threshold_1"].toDoubleOrNullOrDefault(0.5),
                            p["evaluator.cascade_threshold_2"].toDoubleOrNullOrDefault(0.75),
                            p["evaluator.cascade_threshold_3"].toDoubleOrNullOrDefault(0.9),
                        ),
                    "declared_custom_metrics" to declaredCustomMetrics,
                ),
        )
    }

    fun apiKeyEnvironmentVariableName(llm: LLM): String = "${llm.name}_API_KEY"

    fun apiKeyEnvironmentPlaceholder(llm: LLM): String =
        apiKeyEnvironmentPlaceholder(apiKeyEnvironmentVariableName(llm))

    fun apiKeyEnvironmentPlaceholder(variableName: String): String = "${'$'}{$variableName}"

    fun resolveApiBase(
        llm: LLM,
        modelName: String,
        configuredApiBase: String?,
    ): String {
        validateProviderModelConsistency(llm, modelName)

        var configured = configuredApiBase?.trim().orEmpty()
        
        // Se usar localhost ou 127.0.0.1, temos de reescrever para o docker poder aceder ao host
        if (configured.contains("localhost") || configured.contains("127.0.0.1")) {
            configured = configured.replace("localhost", "host.docker.internal")
                .replace("127.0.0.1", "host.docker.internal")
        }
        
        if (configured.isNotBlank()) return configured

        return when (llm) {
            LLM.OPENAI -> OPENAI_API_BASE
            LLM.GEMINI -> GEMINI_API_BASE
            LLM.LOCAL_MODEL -> throw IllegalStateException("llm.api_base is required when provider is LOCAL_MODEL")
        }
    }

    fun validateProviderModelConsistency(
        llm: LLM,
        modelName: String,
    ) {
        val normalizedModel = modelName.trim().lowercase()
        val modelLooksGemini = normalizedModel.startsWith("gemini")
        val modelLooksOpenAi =
            normalizedModel.startsWith("gpt") ||
                normalizedModel.startsWith("o1") ||
                normalizedModel.startsWith("o3") ||
                normalizedModel.startsWith("o4")

        if (llm == LLM.GEMINI && modelLooksOpenAi) {
            throw IllegalStateException("Config model '$modelName' is OpenAI-like but credential provider is GEMINI")
        }

        if (llm == LLM.OPENAI && modelLooksGemini) {
            throw IllegalStateException("Config model '$modelName' is Gemini-like but credential provider is OPENAI")
        }
    }

    fun resolveReasoningEffort(
        modelName: String,
        apiBase: String?,
        configuredReasoningEffort: String?,
    ): String? {
        val configured = configuredReasoningEffort?.trim()?.lowercase()
        if (!configured.isNullOrBlank()) return configured

        val normalizedModel = modelName.trim().lowercase()
        val normalizedApiBase = apiBase?.trim()?.lowercase()?.trimEnd('/').orEmpty()
        val geminiApiBase = GEMINI_API_BASE.lowercase().trimEnd('/')
        val isGemini = normalizedModel.startsWith("gemini") ||
            normalizedApiBase == geminiApiBase

        return if (isGemini) "low" else null
    }

    private fun String?.toIntOrNullOrDefault(default: Int): Int = this?.toIntOrNull() ?: default

    private fun String?.toDoubleOrNullOrDefault(default: Double): Double = this?.toDoubleOrNull() ?: default

    private fun String?.toBooleanStrictOrNullOrDefault(default: Boolean): Boolean =
        this?.toBooleanStrictOrNull() ?: default
}
