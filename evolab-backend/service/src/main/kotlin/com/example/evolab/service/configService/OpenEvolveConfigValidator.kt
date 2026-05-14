package com.example.evolab.service.configService

object OpenEvolveConfigValidator {
    fun validateOpenEvolveConfig(config: OpenEvolveConfig): ConfigError.InvalidOpenEvolveConfig? {
        if (config.maxIterations <= 0) return ConfigError.InvalidOpenEvolveConfig("max_iterations must be > 0")
        if (config.checkpointInterval != null && config.checkpointInterval <= 0) {
            return ConfigError.InvalidOpenEvolveConfig("checkpoint_interval must be > 0")
        }
        if (config.maxCodeLength != null && config.maxCodeLength <= 0) {
            return ConfigError.InvalidOpenEvolveConfig("max_code_length must be > 0")
        }

        if (config.llm.models.isEmpty()) return ConfigError.InvalidOpenEvolveConfig("llm.models cannot be empty")
        if (config.llm.models.any { it.name.isBlank() || it.weight <= 0.0 }) {
            return ConfigError.InvalidOpenEvolveConfig("llm.models must have non-empty names and positive weights")
        }
        val reasoningEffort = config.llm.reasoningEffort?.trim()?.lowercase()
        if (reasoningEffort != null && reasoningEffort !in setOf("none", "minimal", "low", "medium", "high")) {
            return ConfigError.InvalidOpenEvolveConfig("llm.reasoning_effort must be one of: none, minimal, low, medium, high")
        }

        val apiKey = config.llm.apiKey?.trim()
        if (!apiKey.isNullOrEmpty() && !apiKey.matches(Regex("""\$\{[A-Z0-9_]+}"""))) {
            return ConfigError.InvalidOpenEvolveConfig(
                "llm.api_key must use environment placeholder syntax (e.g. \${OPENAI_API_KEY}); secret keys belong to LLM credentials module",
            )
        }

        if (config.prompt.systemMessage.isBlank()) {
            return ConfigError.InvalidOpenEvolveConfig("prompt.system_message is required")
        }
        if ((config.prompt.numTopPrograms ?: 0) < 0 || (config.prompt.numDiversePrograms ?: 0) < 0) {
            return ConfigError.InvalidOpenEvolveConfig("prompt program counters must be >= 0")
        }

        if (config.database.populationSize <= 0 || config.database.archiveSize <= 0 || config.database.numIslands <= 0) {
            return ConfigError.InvalidOpenEvolveConfig("database sizes and num_islands must be > 0")
        }
        if (config.database.migrationInterval <= 0 || config.database.migrationRate !in 0.0..1.0) {
            return ConfigError.InvalidOpenEvolveConfig("database migration settings are invalid")
        }
        if (config.database.featureDimensions.isEmpty()) {
            return ConfigError.InvalidOpenEvolveConfig("database.feature_dimensions cannot be empty")
        }
        when (val bins = config.database.featureBins) {
            is FeatureBins.Uniform -> if (bins.value <= 0) return ConfigError.InvalidOpenEvolveConfig("feature_bins must be > 0")
            is FeatureBins.PerDimension -> if (bins.values.isEmpty() || bins.values.values.any { it <= 0 }) {
                return ConfigError.InvalidOpenEvolveConfig("feature_bins map must be non-empty with positive values")
            }
        }

        if (config.evaluator.timeout <= 0 || config.evaluator.maxRetries < 0) {
            return ConfigError.InvalidOpenEvolveConfig("evaluator.timeout must be > 0 and max_retries >= 0")
        }
        if (config.evaluator.cascadeEvaluation == true && config.evaluator.cascadeThresholds.isEmpty()) {
            return ConfigError.InvalidOpenEvolveConfig("cascade_thresholds are required when cascade_evaluation is true")
        }
        if (config.evaluator.cascadeThresholds.zipWithNext().any { (a, b) -> a >= b }) {
            return ConfigError.InvalidOpenEvolveConfig("cascade_thresholds must be strictly increasing")
        }

        val builtInFeatures = setOf("complexity", "diversity")
        val customFeatures = config.database.featureDimensions.filter { it !in builtInFeatures }.toSet()
        if (!config.evaluator.declaredCustomMetrics.containsAll(customFeatures)) {
            return ConfigError.InvalidOpenEvolveConfig("evaluator must declare custom feature metrics used in database.feature_dimensions")
        }

        return null
    }
}
