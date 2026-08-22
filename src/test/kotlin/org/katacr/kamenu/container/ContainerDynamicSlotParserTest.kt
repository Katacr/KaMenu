package org.katacr.kamenu.container

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 验证 Container 按钮级运行时槽位表达式的解析。 */
class ContainerDynamicSlotParserTest {
    private fun parse(yaml: String): ContainerMenuParseResult {
        val config = YamlConfiguration()
        config.loadFromString(yaml.trimIndent())
        return ContainerMenuParser.parse("test/menu", config)
    }

    @Test
    fun `parses integer slot expression on button`() {
        val result = parse(
            """
            Type: CHEST
            Size: 1
            Layout: ['         ']
            Buttons:
              moving:
                slot: 4
                display:
                  material: STONE
            """
        )
        assertFalse(result.diagnostics.any { it.severity == ContainerDiagnosticSeverity.ERROR })
        val button = result.definition!!.buttons["moving"]
        assertEquals("4", (button!!.slot as? ContainerConfigValue.Scalar)?.value?.toString())
        assertTrue(result.definition!!.layout.dynamicSlotButtons.contains("moving"))
    }

    @Test
    fun `parses variable slot expression on button`() {
        val result = parse(
            """
            Type: CHEST
            Size: 1
            Layout: ['         ']
            Buttons:
              moving:
                slot: '%player_empty_slots%'
                display:
                  material: STONE
            """
        )
        assertFalse(result.diagnostics.any { it.severity == ContainerDiagnosticSeverity.ERROR })
        val button = result.definition!!.buttons["moving"]
        assertEquals("%player_empty_slots%", (button!!.slot as? ContainerConfigValue.Scalar)?.value?.toString())
        assertTrue(result.definition!!.layout.dynamicSlotButtons.contains("moving"))
    }

    @Test
    fun `dynamic slot button is not reported as unused layout button`() {
        val result = parse(
            """
            Type: CHEST
            Size: 1
            Layout: ['A        ']
            Buttons:
              A:
                display:
                  material: STONE
              moving:
                slot: 5
                display:
                  material: PAPER
            """
        )
        assertFalse(result.diagnostics.any { it.severity == ContainerDiagnosticSeverity.ERROR })
        assertTrue(result.diagnostics.none { it.code == "button.unused" && it.path == "Buttons.moving" })
        assertTrue(result.definition!!.layout.slotsByButton.containsKey("A"))
        assertFalse(result.definition!!.layout.slotsByButton.containsKey("moving"))
    }

    @Test
    fun `static layout buttons still resolve normally without slot field`() {
        val result = parse(
            """
            Type: CHEST
            Size: 1
            Layout: ['A        ']
            Buttons:
              A:
                display:
                  material: STONE
            """
        )
        assertFalse(result.diagnostics.any { it.severity == ContainerDiagnosticSeverity.ERROR })
        assertTrue(result.definition!!.layout.slotsByButton["A"]?.contains(0) == true)
        assertNull(result.definition!!.buttons["A"]!!.slot)
        assertTrue(result.definition!!.layout.dynamicSlotButtons.isEmpty())
    }

    @Test
    fun `accepts list slot expression resolving to multiple slots`() {
        val result = parse(
            """
            Type: CHEST
            Size: 1
            Layout: ['         ']
            Buttons:
              moving:
                slot: [8, 9, 10]
                display:
                  material: STONE
            """
        )
        assertFalse(result.diagnostics.any { it.severity == ContainerDiagnosticSeverity.ERROR })
        val button = result.definition!!.buttons["moving"]
        val slot = button!!.slot as? ContainerConfigValue.Sequence
        assertEquals(listOf("8", "9", "10"), slot?.values?.map { (it as ContainerConfigValue.Scalar).value.toString() })
        assertTrue(result.definition!!.layout.dynamicSlotButtons.contains("moving"))
    }

    @Test
    fun `accepts mixed list of static and variable slots`() {
        val result = parse(
            """
            Type: CHEST
            Size: 1
            Layout: ['         ']
            Buttons:
              moving:
                slot: [4, '%player_empty_slots%']
                display:
                  material: STONE
            """
        )
        assertFalse(result.diagnostics.any { it.severity == ContainerDiagnosticSeverity.ERROR })
        val button = result.definition!!.buttons["moving"]
        val slot = button!!.slot as? ContainerConfigValue.Sequence
        assertEquals(listOf("4", "%player_empty_slots%"), slot?.values?.map { (it as ContainerConfigValue.Scalar).value.toString() })
    }

    @Test
    fun `parses list of lists slot as animated frames`() {
        val result = parse(
            """
            Type: CHEST
            Size: 1
            Layout: ['         ']
            Buttons:
              moving:
                slot: [[8], [9], [10]]
                display:
                  material: STONE
            """
        )
        assertFalse(result.diagnostics.any { it.severity == ContainerDiagnosticSeverity.ERROR })
        val button = result.definition!!.buttons["moving"]
        assertNull(button!!.slot)
        val frames = button.slotFrames
        assertEquals(3, frames?.size)
        val first = frames!![0] as ContainerConfigValue.Sequence
        assertEquals(listOf("8"), first.values.map { (it as ContainerConfigValue.Scalar).value.toString() })
        val second = frames[1] as ContainerConfigValue.Sequence
        assertEquals(listOf("9"), second.values.map { (it as ContainerConfigValue.Scalar).value.toString() })
        assertTrue(result.definition!!.layout.dynamicSlotButtons.contains("moving"))
    }
}
