package org.katacr.kamenu.migration

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.katacr.kamenu.container.ContainerLayoutParser
import org.katacr.kamenu.container.ContainerMenuType
import java.io.File

/** 验证 TrMenu 单页布局、显式槽位和严格拒绝路径。 */
class TrMenuLayoutConverterTest {
    @Test
    fun `converts aliases and explicit static slots`() {
        val (result, diagnostics) = convert(
            """
            InventoryType: HOPPER
            Shape: 'A    '
            Buttons:
              A:
                Displays:
                  Texture: STONE
              named-button:
                Display:
                  Material: PAPER
                  Positions: [4]
            """
        )

        assertNotNull(result)
        assertFalse(diagnostics.hasErrors)
        assertEquals(ContainerMenuType.HOPPER, result?.type)
        assertEquals(listOf("A   `named-button`"), result?.pages?.first())
        assertEquals(listOf(0), result?.buttons?.first { it.sourceId == "A" }?.slots)
        assertEquals(listOf(4), result?.buttons?.first { it.sourceId == "named-button" }?.slots)
        assertNotNull(ContainerLayoutParser.parse(result!!.pages.first(), result.type).definition)
    }

    @Test
    fun `preserves animated explicit slots as cycling frames`() {
        val (result, diagnostics) = convert(
            """
            Type: CHEST
            Size: 1
            Layout: ['         ']
            Icons:
              moving:
                display:
                  material: STONE
                  slots:
                    - [1]
                    - [2]
            """
        )

        assertNotNull(result)
        assertEquals(listOf("1", "2"), result?.buttons?.single()?.slotFrames)
        assertTrue(diagnostics.issues.any { it.code == "TRM_ICON_SLOT_ANIMATION" })
    }

    @Test
    fun `converts multiple layout pages`() {
        val (result, diagnostics) = convert(
            """
            Layout:
              - ['A        ']
              - ['B        ']
            Icons:
              A:
                display:
                  material: STONE
              B:
                display:
                  material: PAPER
            """
        )

        assertNotNull(result)
        assertFalse(diagnostics.hasErrors)
        assertEquals(2, result?.pages?.size)
        assertEquals(listOf("A        "), result?.pages?.get(0))
        assertEquals(listOf("B        "), result?.pages?.get(1))
        assertEquals(0, result?.buttons?.first { it.sourceId == "A" }?.pageIndex)
        assertEquals(1, result?.buttons?.first { it.sourceId == "B" }?.pageIndex)
    }

    @Test
    fun `rejects two icons occupying one slot`() {
        val (result, diagnostics) = convert(
            """
            Type: CHEST
            Size: 1
            Icons:
              first:
                display:
                  material: STONE
                  slot: 0
              second:
                display:
                  material: PAPER
                  slot: 0
            """
        )

        assertNull(result)
        assertTrue(diagnostics.issues.any { it.code == "TRM_ICON_SLOT_COLLISION" })
    }

    private fun convert(yaml: String): Pair<TrMenuLayoutConversion?, TrMenuMigrationDiagnostics> {
        val config = YamlConfiguration()
        config.loadFromString(yaml.trimIndent())
        val source = TrMenuSourceMenu(
            source = File("test.yml"),
            menuId = "test",
            root = TrMenuSourceSection.from(config)
        )
        val diagnostics = TrMenuMigrationDiagnostics()
        return TrMenuLayoutConverter().convert(source, diagnostics) to diagnostics
    }
}
