package com.example.evolab.service.configService

object OpenEvolveConfigParser {
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
            reasoningEffort = this["reasoning_effort"] as? String,
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
