package org.katacr.kamenu

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 验证输入捕获定义的三种解析路径与校验诊断。 */
class InputCaptureParserTest {

    private fun loadConfig(yaml: String): YamlConfiguration {
        val config = YamlConfiguration()
        config.loadFromString(yaml)
        return config
    }

    @Test
    fun `parses named definition with layers`() {
        val config = loadConfig(
            """
            Input-Captures:
              buy_item:
                type: chat
                timeout: 1m30s
                cancel: exit
                max-attempts: 5
                layers:
                  - key: item_name
                    max-length: 32
                    prompt:
                      - 'tell: &e请输入物品名称'
                  - key: amount
                    prompt:
                      - 'tell: &e请输入数量'
                    validate: '%checknumber_{input_candidate}% >= 1'
                    invalid:
                      - 'tell: &c数量无效'
                complete-actions:
                  - 'tell: bought {input:item_name} x{input:amount}'
                timeout-actions:
                  - 'tell: timed out'
                cancel-actions:
                  - 'tell: cancelled'
            """.trimIndent()
        )

        val result = InputCaptureParser.parseNamed("buy_item", config)
        val definition = result.definition
        assertNotNull(definition, "errors=${result.errors}")
        assertEquals(InputCaptureDefinition.CaptureType.CHAT, definition!!.type)
        assertEquals(90L, definition.timeoutSeconds)
        assertEquals("exit", definition.cancelText)
        assertEquals(5, definition.maxAttempts)
        assertEquals(2, definition.layers.size)
        assertEquals("item_name", definition.layers[0].key)
        assertEquals(32, definition.layers[0].maxLength)
        assertEquals(1, definition.layers[0].promptActions.size)
        assertNull(definition.layers[0].validateCondition)
        assertEquals("%checknumber_{input_candidate}% >= 1", definition.layers[1].validateCondition)
        assertEquals(1, definition.completeActions.size)
        assertEquals(1, definition.timeoutActions.size)
        assertEquals(1, definition.cancelActions.size)
    }

    @Test
    fun `reports missing section and empty layers`() {
        val missing = InputCaptureParser.parseNamed("nope", loadConfig("Input-Captures: {}"))
        assertNull(missing.definition)
        assertTrue(missing.errors.first().contains("does not exist"))

        val emptyLayers = InputCaptureParser.parseNamed(
            "bad",
            loadConfig(
                """
                Input-Captures:
                  bad:
                    type: chat
                    layers: []
                """.trimIndent()
            )
        )
        assertNull(emptyLayers.definition)
        assertTrue(emptyLayers.errors.any { it.contains("at least one layer") })
    }

    @Test
    fun `rejects invalid duplicate or missing keys`() {
        val result = InputCaptureParser.parseNamed(
            "bad",
            loadConfig(
                """
                Input-Captures:
                  bad:
                    layers:
                      - key: 'has space'
                      - key: amount
                      - key: amount
                      - prompt: []
                """.trimIndent()
            )
        )
        assertNull(result.definition)
        assertTrue(result.errors.any { it.contains("'has space'") })
        assertTrue(result.errors.any { it.contains("duplicates") })
        assertTrue(result.errors.any { it.contains("is required") })
    }

    @Test
    fun `rejects unknown type and invalid timeout`() {
        val result = InputCaptureParser.parseNamed(
            "bad",
            loadConfig(
                """
                Input-Captures:
                  bad:
                    type: telepathy
                    timeout: abc
                    layers:
                      - key: answer
                """.trimIndent()
            )
        )
        assertNull(result.definition)
        assertTrue(result.errors.any { it.contains("telepathy") })
        assertTrue(result.errors.any { it.contains("timeout") })
    }

    @Test
    fun `parses inline single layer definition`() {
        val result = InputCaptureParser.parseInline("type=chat;key=answer;timeout=45;cancel=exit;max-length=64")
        val definition = result.definition
        assertNotNull(definition)
        assertEquals("inline", definition!!.id)
        assertEquals(InputCaptureDefinition.CaptureType.CHAT, definition.type)
        assertEquals(45L, definition.timeoutSeconds)
        assertEquals("exit", definition.cancelText)
        assertEquals(64, definition.layers.single().maxLength)
        assertEquals("answer", definition.layers.single().key)
        assertTrue(definition.completeActions.isEmpty())
    }

    @Test
    fun `inline requires valid key and rejects bad values`() {
        assertNull(InputCaptureParser.parseInline("type=chat").definition)
        assertNull(InputCaptureParser.parseInline("key=bad key!").definition)
        assertNull(InputCaptureParser.parseInline("key=ok;timeout=nope").definition)
    }

    @Test
    fun `applies overrides on top of named definition`() {
        val base = InputCaptureParser.parseNamed(
            "buy_item",
            loadConfig(
                """
                Input-Captures:
                  buy_item:
                    timeout: 60
                    cancel: exit
                    max-attempts: 3
                    layers:
                      - key: item_name
                        max-length: 32
                """.trimIndent()
            )
        ).definition
        assertNotNull(base)

        val overridden = InputCaptureParser.applyOverrides(base!!, "timeout=30s;max-length=16;max-attempts=7").definition
        assertNotNull(overridden)
        assertEquals(30L, overridden!!.timeoutSeconds)
        assertEquals(16, overridden.layers.single().maxLength)
        assertEquals(7, overridden.maxAttempts)
        // 未覆盖字段保持原值
        assertEquals("exit", overridden.cancelText)
        assertEquals("item_name", overridden.layers.single().key)
    }
}
