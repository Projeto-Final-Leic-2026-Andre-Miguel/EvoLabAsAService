package com.example.evolab.service.configService

import com.example.evolab.domain.config.Config
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class OpenEvolvePayloadBuilderTest {
    @Test
    fun `gemini payload defaults reasoning effort to low`() {
        val config =
            config(
                modelName = "gemini-2.5-pro",
                additionalParams =
                    mapOf(
                        "llm.api_base" to "https://generativelanguage.googleapis.com/v1beta/openai/",
                    ),
            )

        val payload = OpenEvolvePayloadBuilder.build(config, apiKeyValue = "\${GEMINI_API_KEY}")
        val llm = payload["llm"] as Map<*, *>

        assertEquals("low", llm["reasoning_effort"])
    }

    @Test
    fun `explicit reasoning effort is preserved`() {
        val config =
            config(
                modelName = "gemini-2.5-pro",
                additionalParams =
                    mapOf(
                        "llm.api_base" to "https://generativelanguage.googleapis.com/v1beta/openai/",
                        "llm.reasoning_effort" to "medium",
                    ),
            )

        val payload = OpenEvolvePayloadBuilder.build(config, apiKeyValue = "\${GEMINI_API_KEY}")
        val llm = payload["llm"] as Map<*, *>

        assertEquals("medium", llm["reasoning_effort"])
    }

    @Test
    fun `openai payload does not add reasoning effort by default`() {
        val config =
            config(
                modelName = "gpt-4.1-mini",
                additionalParams = mapOf("llm.api_base" to "https://api.openai.com/v1"),
            )

        val payload = OpenEvolvePayloadBuilder.build(config, apiKeyValue = "\${OPENAI_API_KEY}")
        val llm = payload["llm"] as Map<*, *>

        assertFalse(llm.containsKey("reasoning_effort"))
    }

    @Test
    fun `rendered yaml includes reasoning effort when present`() {
        val payload =
            OpenEvolvePayloadBuilder.build(
                config(
                    modelName = "gemini-2.5-pro",
                    additionalParams = mapOf("llm.api_base" to "https://generativelanguage.googleapis.com/v1beta/openai/"),
                ),
                apiKeyValue = "\${GEMINI_API_KEY}",
            )
        val parsed = OpenEvolveConfigParser.toOpenEvolveConfig(payload)

        requireNotNull(parsed)
        val yaml = OpenEvolveYamlRenderer.renderOpenEvolveYaml(parsed)

        assertTrue(yaml.contains("reasoning_effort: \"low\""))
    }

    private fun config(
        modelName: String,
        additionalParams: Map<String, String>,
    ): Config =
        Config(
            id = 1,
            userId = 1,
            llmCredentialsId = 1,
            modelName = modelName,
            maxIter = 10,
            checkPointInterval = 5,
            additionalParams = additionalParams,
            createdAt = Instant.now(),
        )
}
