package com.example.evolab.service.configService

data class OpenEvolveConfig(
    val maxIterations: Int,
    val checkpointInterval: Int?,
    val logLevel: String?,
    val logDir: String?,
    val randomSeed: Int?,
    val diffBasedEvolution: Boolean?,
    val maxCodeLength: Int?,
    val earlyStoppingPatience: Int?,
    val convergenceThreshold: Double?,
    val earlyStoppingMetric: String?,
    val llm: LlmConfig,
    val prompt: PromptConfig,
    val database: DatabaseConfig,
    val evaluator: EvaluatorConfig,
    val evolutionTrace: EvolutionTraceConfig?,
)

data class LlmConfig(
    val models: List<LlmModelWeight>,
    val evaluatorModels: List<LlmModelWeight>,
    val apiBase: String?,
    val apiKey: String?,
    val temperature: Double?,
    val topP: Double?,
    val maxTokens: Int?,
    val timeout: Int?,
    val retries: Int?,
    val retryDelay: Double?,
)

data class LlmModelWeight(
    val name: String,
    val weight: Double,
)

data class PromptConfig(
    val templateDir: String?,
    val systemMessage: String,
    val evaluatorSystemMessage: String?,
    val numTopPrograms: Int?,
    val numDiversePrograms: Int?,
    val useTemplateStochasticity: Boolean?,
    val templateVariations: Int?,
    val includeArtifacts: Boolean?,
    val maxArtifactBytes: Int?,
    val artifactSecurityFilter: Boolean?,
)

data class DatabaseConfig(
    val dbPath: String?,
    val inMemory: Boolean?,
    val logPrompts: Boolean?,
    val populationSize: Int,
    val archiveSize: Int,
    val numIslands: Int,
    val migrationInterval: Int,
    val migrationRate: Double,
    val eliteSelectionRatio: Double,
    val explorationRatio: Double,
    val exploitationRatio: Double,
    val featureDimensions: List<String>,
    val featureBins: FeatureBins,
    val diversityReferenceSize: Int?,
)

sealed class FeatureBins {
    data class Uniform(
        val value: Int,
    ) : FeatureBins()

    data class PerDimension(
        val values: Map<String, Int>,
    ) : FeatureBins()
}

data class EvaluatorConfig(
    val timeout: Int,
    val maxRetries: Int,
    val cascadeEvaluation: Boolean?,
    val cascadeThresholds: List<Double>,
    val parallelEvaluations: Int?,
    val useLlmFeedback: Boolean?,
    val llmFeedbackWeight: Double?,
    val declaredCustomMetrics: List<String>,
)

data class EvolutionTraceConfig(
    val enabled: Boolean,
    val format: String?,
    val includeCode: Boolean?,
    val includePrompts: Boolean?,
    val outputPath: String?,
    val bufferSize: Int?,
    val compress: Boolean?,
)

