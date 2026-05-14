package com.example.evolab.service.configService

object OpenEvolveYamlRenderer {
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
        config.llm.reasoningEffort?.let { lines.add("  reasoning_effort: \"$it\"") }

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
}
