package com.example.evolab.service.configService

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConfigInputValidatorTest {
    @Test
    fun `accepts checkpoints below or equal to max iterations`() {
        assertNull(ConfigInputValidator.validate("model", 10, 5))
        assertNull(ConfigInputValidator.validate("model", 5, 5))
    }

    @Test
    fun `rejects non-positive max iterations`() {
        assertEquals(ConfigError.InvalidMaxIterations, ConfigInputValidator.validate("model", 0, 1))
    }

    @Test
    fun `rejects non-positive checkpoint interval`() {
        assertEquals(ConfigError.InvalidCheckpointInterval, ConfigInputValidator.validate("model", 5, 0))
    }

    @Test
    fun `distinguishes checkpoint interval above max iterations`() {
        assertEquals(
            ConfigError.CheckpointIntervalExceedsMaxIterations,
            ConfigInputValidator.validate("model", 4, 5),
        )
    }
}
