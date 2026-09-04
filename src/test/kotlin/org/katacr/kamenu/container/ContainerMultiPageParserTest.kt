package org.katacr.kamenu.container

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContainerMultiPageParserTest {

    @Test
    fun `parses single-page layout as one-page list`() {
        val result = parse(
            """
            Type: CHEST
            Layout:
              - '#########'
              - '#       #'
              - '####X####'
            Buttons:
              '#':
                display: { material: STONE, name: ' ' }
              X:
                display: { material: BARRIER, name: 'X' }
            """
        )
        val def = result.definition!!
        assertEquals(1, def.pageCount)
        assertNotNull(def.layout)
        assertEquals(3, def.layout.rows)
    }

    @Test
    fun `parses multi-page nested layout`() {
        val result = parse(
            """
            Type: CHEST
            Layout:
              - - '#########'
                - '#  N    #'
                - '####X####'
              - - 'P########'
                - '#       #'
                - '####X####'
            Buttons:
              '#':
                display: { material: STONE, name: ' ' }
              X:
                display: { material: BARRIER, name: 'X' }
              N:
                display: { material: ARROW, name: 'Next' }
              P:
                display: { material: ARROW, name: 'Prev' }
            """
        )
        val def = result.definition!!
        assertEquals(2, def.pageCount)
        assertEquals(3, def.layouts[0].rows)
        assertEquals(3, def.layouts[1].rows)
        assertTrue(def.layouts[0].slotsByButton.containsKey("N"))
        assertFalse(def.layouts[0].slotsByButton.containsKey("P"))
        assertTrue(def.layouts[1].slotsByButton.containsKey("P"))
        assertFalse(def.layouts[1].slotsByButton.containsKey("N"))
    }

    @Test
    fun `parses default_page setting`() {
        val result = parse(
            """
            Type: CHEST
            Settings:
              default_page: 1
            Layout:
              - - '#########'
                - '####X####'
              - - '#########'
                - '####X####'
            Buttons:
              '#':
                display: { material: STONE, name: ' ' }
              X:
                display: { material: BARRIER, name: 'X' }
            """
        )
        val def = result.definition!!
        assertNotNull(def.defaultPage)
        assertTrue(def.defaultPage is ContainerConfigValue.Scalar)
        assertEquals(1, (def.defaultPage as ContainerConfigValue.Scalar).value)
    }

    @Test
    fun `allows missing button on second page`() {
        val result = parse(
            """
            Type: CHEST
            Layout:
              - - '#########'
                - '####X####'
              - - '########Y'
                - '####X####'
            Buttons:
              '#':
                display: { material: STONE, name: ' ' }
              X:
                display: { material: BARRIER, name: 'X' }
            """
        )
        assertTrue(result.succeeded)
        assertTrue(result.diagnostics.none { it.code == "layout.button_not_found" })
    }

    @Test
    fun `normalizes short and long layout rows without diagnostics`() {
        val result = ContainerLayoutParser.parse(listOf("A", "ABCDEFGHIJK"), ContainerMenuType.CHEST)

        assertTrue(result.diagnostics.isEmpty())
        val definition = result.definition!!
        assertEquals(18, definition.slots.size)
        assertEquals(0, definition.slots[0].column)
        assertEquals(null, definition.slots[1].buttonId)
        assertEquals(null, definition.slots[8].buttonId)
        assertEquals("A", definition.slots[9].buttonId)
        assertEquals("I", definition.slots[17].buttonId)
    }

    @Test
    fun `reports error for invalid page type`() {
        val result = parse(
            """
            Type: CHEST
            Layout:
              - - '#########'
                - '####X####'
              - 'not_a_list'
            Buttons:
              '#':
                display: { material: STONE, name: ' ' }
              X:
                display: { material: BARRIER, name: 'X' }
            """
        )
        assertNull(result.definition)
        assertTrue(result.diagnostics.any { it.code == "layout.invalid_page_type" })
    }

    @Test
    fun `different row counts across pages are allowed`() {
        val result = parse(
            """
            Type: CHEST
            Layout:
              - - '#########'
                - '#########'
                - '#########'
              - - '#########'
                - '####X####'
            Buttons:
              '#':
                display: { material: STONE, name: ' ' }
              X:
                display: { material: BARRIER, name: 'X' }
            """
        )
        val def = result.definition!!
        assertEquals(2, def.pageCount)
        assertEquals(3, def.layouts[0].rows)
        assertEquals(2, def.layouts[1].rows)
    }

    private fun parse(source: String): ContainerMenuParseResult {
        val config = YamlConfiguration()
        config.loadFromString(source.trimIndent())
        return ContainerMenuParser.parse("test/multi_page", config)
    }
}
