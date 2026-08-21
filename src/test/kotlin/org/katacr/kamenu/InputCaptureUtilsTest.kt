package org.katacr.kamenu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InputCaptureUtilsTest {

    private val schema = InputCaptureUtils.Schema(
        keys = listOf("enabled"),
        types = mapOf("enabled" to "checkbox"),
        removeChars = emptyMap(),
        checkboxMappings = mapOf("enabled" to ("enabled" to "disabled"))
    )

    @Test
    fun `checkbox true remains true mapping`() {
        val values = InputCaptureUtils.captureVariables(null, mapOf("enabled" to "true"), schema)

        assertEquals("enabled", values["enabled"])
    }

    @Test
    fun `checkbox false remains false mapping`() {
        val values = InputCaptureUtils.captureVariables(null, mapOf("enabled" to "false"), schema)

        assertEquals("disabled", values["enabled"])
    }

    @Test
    fun `checkbox accepts native boolean numeric values`() {
        val values = InputCaptureUtils.captureVariables(null, mapOf("enabled" to "1"), schema)

        assertEquals("enabled", values["enabled"])
    }
}
