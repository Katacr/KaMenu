package org.katacr.kamenu.container

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 验证 Container repeat 按钮定义、Layout 容量和错误配置诊断。 */
class ContainerRepeatParserTest {
    private fun parse(yaml: String): ContainerMenuParseResult {
        val config = YamlConfiguration()
        config.loadFromString(yaml.trimIndent())
        return ContainerMenuParser.parse("test/repeat", config)
    }

    @Test
    fun `parses repeat template against all matching layout slots`() {
        val result = parse(
            """
            Type: CHEST
            Layout:
              - 'AAAABBCCC'
              - 'AAAABBCCC'
              - 'AAAADDEEE'
            Buttons:
              A:
                type: repeat
                source: '[items]'
                trim: false
                split: ','
                item:
                  display:
                    material: PAPER
                    name: '{item.name}'
                  actions:
                    all: 'tell: {item.name}'
                empty:
                  display:
                    material: BARRIER
                    name: '&cEmpty'
              B:
                display:
                  material: STONE
            """
        )

        assertFalse(result.diagnostics.any { it.severity == ContainerDiagnosticSeverity.ERROR })
        val definition = result.definition!!
        assertEquals(12, definition.layout.slotsByButton["A"]?.size)
        val repeat = definition.buttons["A"]?.repeat
        assertNotNull(repeat)
        assertEquals("[items]", repeat?.source)
        assertEquals(",", repeat?.split)
        assertFalse(repeat!!.trim)
        assertEquals("PAPER", (repeat.itemDisplay["material"] as ContainerConfigValue.Scalar).value)
        assertEquals("&cEmpty", (repeat.emptyDisplay!!["name"] as ContainerConfigValue.Scalar).value)
        assertEquals(listOf("tell: {item.name}"), repeat.itemActions[ContainerClickType.ALL])
        assertNull(definition.buttons["B"]?.repeat)
    }

    @Test
    fun `rejects repeat button not referenced by layout`() {
        val result = parse(
            """
            Type: CHEST
            Layout: ['         ']
            Buttons:
              A:
                type: repeat
                source: '[items]'
                item:
                  display:
                    material: PAPER
            """
        )

        assertNull(result.definition)
        assertTrue(result.diagnostics.any { it.code == "button.repeat_unused" && it.path == "Buttons.A" })
    }

    @Test
    fun `rejects mixing repeat template with normal display and variants`() {
        val result = parse(
            """
            Type: CHEST
            Layout: ['A        ']
            Buttons:
              A:
                type: repeat
                source: '[items]'
                display:
                  material: STONE
                item:
                  display:
                    material: PAPER
            """
        )

        assertNull(result.definition)
        assertTrue(result.diagnostics.any { it.code == "button.repeat_mixed_definition" })
    }
}
