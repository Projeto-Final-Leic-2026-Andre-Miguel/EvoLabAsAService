package com.example.evolab.service.configService

object OpenEvolveConfigAuxiliary {
    fun toOpenEvolveConfig(payload: Map<String, Any>): OpenEvolveConfig? {
        val llm = payload["llm"] as? Map<*, *> ?: return null
        val prompt = payload["prompt"] as? Map<*, *> ?: return null
        val database = payload["database"] as? Map<*, *> ?: return null
        val evaluator = payload["evaluator"] as? Map<*, *> ?: return null
        val evolutionTrace = payload["evolution_trace"] as? Map<*, *>

        return OpenEvolveConfig(
            maxIterations = (payload["max_iterations"] as? Number)?.toInt() ?: return null,
            checkpointInterval = (payload["checkpoint_interval"] as? Number)?.toInt(),
            logLevel = payload["log_level"] as? String,
            logDir = payload["log_dir"] as? String,
            randomSeed = (payload["random_seed"] as? Number)?.toInt(),
            diffBasedEvolution = payload["diff_based_evolution"] as? Boolean,
            maxCodeLength = (payload["max_code_length"] as? Number)?.toInt(),
            earlyStoppingPatience = (payload["early_stopping_patience"] as? Number)?.toInt(),
            convergenceThreshold = (payload["convergence_threshold"] as? Number)?.toDouble(),
            earlyStoppingMetric = payload["early_stopping_metric"] as? String,
            llm = llm.toLlmConfig() ?: return null,
            prompt = prompt.toPromptConfig() ?: return null,
            database = database.toDatabaseConfig() ?: return null,
            evaluator = evaluator.toEvaluatorConfig() ?: return null,
            evolutionTrace = evolutionTrace?.toEvolutionTraceConfig(),
        )
    }

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

    fun renderOpenEvolveYaml(config: OpenEvolveConfig): String {
        val lines = mutableListOf<String>()
        lines.add("max_iterations: ${config.maxIterations}")
        config.checkpointInterval?.let { lines.add("checkpoint_interval: $it") }
        config.logLevel?.let { lines.add("log_level: \"$it\"") }
        config.logDir?.let { lines.add("log_dir: \"$it\"") }
        config.randomSeed?.let { lines.add("random_seed: $it") }
        config.diffBasedEvolution?.let { lines.add("diff_based_evolution: $it") }
        config.maxCodeLength?.let { lines.add("max_code_length: $it") }
        config.earlyStoppingPatience?.let { lines.add("early_stopping_patience: $it") }
        config.convergenceThreshold?.let { lines.add("convergence_threshold: $it") }
        config.earlyStoppingMetric?.let { lines.add("early_stopping_metric: \"$it\"") }

        lines.add("llm:")
        lines.add("  models:")
        config.llm.models.forEach {
            lines.add("    - name: \"${it.name}\"")
            lines.add("      weight: ${it.weight}")
        }
        if (config.llm.evaluatorModels.isNotEmpty()) {
            lines.add("  evaluator_models:")
            config.llm.evaluatorModels.forEach {
                lines.add("    - name: \"${it.name}\"")
                lines.add("      weight: ${it.weight}")
            }
        }
        config.llm.apiBase?.let { lines.add("  api_base: \"$it\"") }
        config.llm.apiKey?.let { lines.add("  api_key: \"$it\"") }
        config.llm.temperature?.let { lines.add("  temperature: $it") }
        config.llm.topP?.let { lines.add("  top_p: $it") }
        config.llm.maxTokens?.let { lines.add("  max_tokens: $it") }
        config.llm.timeout?.let { lines.add("  timeout: $it") }
        config.llm.retries?.let { lines.add("  retries: $it") }
        config.llm.retryDelay?.let { lines.add("  retry_delay: $it") }

        lines.add("prompt:")
        lines.add("  system_message: |")
        config.prompt.systemMessage.lines().forEach { lines.add("    $it") }
        config.prompt.templateDir?.let { lines.add("  template_dir: \"$it\"") }
        config.prompt.evaluatorSystemMessage?.let { lines.add("  evaluator_system_message: \"$it\"") }
        config.prompt.numTopPrograms?.let { lines.add("  num_top_programs: $it") }
        config.prompt.numDiversePrograms?.let { lines.add("  num_diverse_programs: $it") }
        config.prompt.useTemplateStochasticity?.let { lines.add("  use_template_stochasticity: $it") }
        config.prompt.templateVariations?.let { lines.add("  template_variations: $it") }
        config.prompt.includeArtifacts?.let { lines.add("  include_artifacts: $it") }
        config.prompt.maxArtifactBytes?.let { lines.add("  max_artifact_bytes: $it") }
        config.prompt.artifactSecurityFilter?.let { lines.add("  artifact_security_filter: $it") }

        lines.add("database:")
        config.database.dbPath?.let { lines.add("  db_path: \"$it\"") }
        config.database.inMemory?.let { lines.add("  in_memory: $it") }
        config.database.logPrompts?.let { lines.add("  log_prompts: $it") }
        lines.add("  population_size: ${config.database.populationSize}")
        lines.add("  archive_size: ${config.database.archiveSize}")
        lines.add("  num_islands: ${config.database.numIslands}")
        lines.add("  migration_interval: ${config.database.migrationInterval}")
        lines.add("  migration_rate: ${config.database.migrationRate}")
        lines.add("  elite_selection_ratio: ${config.database.eliteSelectionRatio}")
        lines.add("  exploration_ratio: ${config.database.explorationRatio}")
        lines.add("  exploitation_ratio: ${config.database.exploitationRatio}")
        lines.add("  feature_dimensions:")
        config.database.featureDimensions.forEach { lines.add("    - \"$it\"") }
        when (val bins = config.database.featureBins) {
            is FeatureBins.Uniform -> lines.add("  feature_bins: ${bins.value}")
            is FeatureBins.PerDimension -> {
                lines.add("  feature_bins:")
                bins.values.forEach { (k, v) -> lines.add("    $k: $v") }
            }
        }
        config.database.diversityReferenceSize?.let { lines.add("  diversity_reference_size: $it") }

        lines.add("evaluator:")
        lines.add("  timeout: ${config.evaluator.timeout}")
        lines.add("  max_retries: ${config.evaluator.maxRetries}")
        config.evaluator.cascadeEvaluation?.let { lines.add("  cascade_evaluation: $it") }
        if (config.evaluator.cascadeThresholds.isNotEmpty()) {
            lines.add("  cascade_thresholds:")
            config.evaluator.cascadeThresholds.forEach { lines.add("    - $it") }
        }
        config.evaluator.parallelEvaluations?.let { lines.add("  parallel_evaluations: $it") }
        config.evaluator.useLlmFeedback?.let { lines.add("  use_llm_feedback: $it") }
        config.evaluator.llmFeedbackWeight?.let { lines.add("  llm_feedback_weight: $it") }
        if (config.evaluator.declaredCustomMetrics.isNotEmpty()) {
            lines.add("  declared_custom_metrics:")
            config.evaluator.declaredCustomMetrics.forEach { lines.add("    - \"$it\"") }
        }

        config.evolutionTrace?.let { trace ->
            lines.add("evolution_trace:")
            lines.add("  enabled: ${trace.enabled}")
            trace.format?.let { lines.add("  format: \"$it\"") }
            trace.includeCode?.let { lines.add("  include_code: $it") }
            trace.includePrompts?.let { lines.add("  include_prompts: $it") }
            trace.outputPath?.let { lines.add("  output_path: \"$it\"") }
            trace.bufferSize?.let { lines.add("  buffer_size: $it") }
            trace.compress?.let { lines.add("  compress: $it") }
        }

        return lines.joinToString(separator = "\n", postfix = "\n")
    }

    private fun Map<*, *>.toLlmConfig(): LlmConfig? {
        val models = (this["models"] as? List<*>)?.mapNotNull { it.toModelWeight() } ?: return null
        if (models.isEmpty()) return null
        val evaluatorModels = (this["evaluator_models"] as? List<*>)?.mapNotNull { it.toModelWeight() } ?: emptyList()

        return LlmConfig(
            models = models,
            evaluatorModels = evaluatorModels,
            apiBase = this["api_base"] as? String,
            apiKey = this["api_key"] as? String,
            temperature = (this["temperature"] as? Number)?.toDouble(),
            topP = (this["top_p"] as? Number)?.toDouble(),
            maxTokens = (this["max_tokens"] as? Number)?.toInt(),
            timeout = (this["timeout"] as? Number)?.toInt(),
            retries = (this["retries"] as? Number)?.toInt(),
            retryDelay = (this["retry_delay"] as? Number)?.toDouble(),
        )
    }

    private fun Any?.toModelWeight(): LlmModelWeight? {
        val model = this as? Map<*, *> ?: return null
        val name = (model["name"] as? String)?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val weight = (model["weight"] as? Number)?.toDouble() ?: return null
        return LlmModelWeight(name = name, weight = weight)
    }

    private fun Map<*, *>.toPromptConfig(): PromptConfig? {
        val systemMessage = (this["system_message"] as? String)?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return PromptConfig(
            templateDir = this["template_dir"] as? String,
            systemMessage = systemMessage,
            evaluatorSystemMessage = this["evaluator_system_message"] as? String,
            numTopPrograms = (this["num_top_programs"] as? Number)?.toInt(),
            numDiversePrograms = (this["num_diverse_programs"] as? Number)?.toInt(),
            useTemplateStochasticity = this["use_template_stochasticity"] as? Boolean,
            templateVariations = (this["template_variations"] as? Number)?.toInt(),
            includeArtifacts = this["include_artifacts"] as? Boolean,
            maxArtifactBytes = (this["max_artifact_bytes"] as? Number)?.toInt(),
            artifactSecurityFilter = this["artifact_security_filter"] as? Boolean,
        )
    }

    private fun Map<*, *>.toDatabaseConfig(): DatabaseConfig? {
        val featureDimensions = (this["feature_dimensions"] as? List<*>)?.mapNotNull { (it as? String)?.trim() } ?: return null
        val featureBinsValue = this["feature_bins"]
        val featureBins =
            when (featureBinsValue) {
                is Number -> FeatureBins.Uniform(featureBinsValue.toInt())
                is Map<*, *> -> {
                    val values = featureBinsValue.entries.associate { (key, value) ->
                        key.toString() to ((value as? Number)?.toInt() ?: -1)
                    }
                    FeatureBins.PerDimension(values)
                }

                else -> return null
            }

        return DatabaseConfig(
            dbPath = this["db_path"] as? String,
            inMemory = this["in_memory"] as? Boolean,
            logPrompts = this["log_prompts"] as? Boolean,
            populationSize = (this["population_size"] as? Number)?.toInt() ?: return null,
            archiveSize = (this["archive_size"] as? Number)?.toInt() ?: return null,
            numIslands = (this["num_islands"] as? Number)?.toInt() ?: return null,
            migrationInterval = (this["migration_interval"] as? Number)?.toInt() ?: return null,
            migrationRate = (this["migration_rate"] as? Number)?.toDouble() ?: return null,
            eliteSelectionRatio = (this["elite_selection_ratio"] as? Number)?.toDouble() ?: return null,
            explorationRatio = (this["exploration_ratio"] as? Number)?.toDouble() ?: return null,
            exploitationRatio = (this["exploitation_ratio"] as? Number)?.toDouble() ?: return null,
            featureDimensions = featureDimensions,
            featureBins = featureBins,
            diversityReferenceSize = (this["diversity_reference_size"] as? Number)?.toInt(),
        )
    }

    private fun Map<*, *>.toEvaluatorConfig(): EvaluatorConfig? {
        val thresholds = (this["cascade_thresholds"] as? List<*>)?.mapNotNull { (it as? Number)?.toDouble() } ?: emptyList()
        val customMetrics = (this["declared_custom_metrics"] as? List<*>)?.mapNotNull { (it as? String)?.trim() } ?: emptyList()

        return EvaluatorConfig(
            timeout = (this["timeout"] as? Number)?.toInt() ?: return null,
            maxRetries = (this["max_retries"] as? Number)?.toInt() ?: return null,
            cascadeEvaluation = this["cascade_evaluation"] as? Boolean,
            cascadeThresholds = thresholds,
            parallelEvaluations = (this["parallel_evaluations"] as? Number)?.toInt(),
            useLlmFeedback = this["use_llm_feedback"] as? Boolean,
            llmFeedbackWeight = (this["llm_feedback_weight"] as? Number)?.toDouble(),
            declaredCustomMetrics = customMetrics,
        )
    }

    private fun Map<*, *>.toEvolutionTraceConfig(): EvolutionTraceConfig? {
        val enabled = this["enabled"] as? Boolean ?: return null
        return EvolutionTraceConfig(
            enabled = enabled,
            format = this["format"] as? String,
            includeCode = this["include_code"] as? Boolean,
            includePrompts = this["include_prompts"] as? Boolean,
            outputPath = this["output_path"] as? String,
            bufferSize = (this["buffer_size"] as? Number)?.toInt(),
            compress = this["compress"] as? Boolean,
        )
    }
}
