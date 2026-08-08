package org.katacr.kamenu.migration

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 验证 TrMenu 属性正则、声明顺序和动态键边界。 */
class TrMenuKeyResolverTest {
    @Test
    fun `uses first matching alias in source order`() {
        val result = TrMenuKeyResolver.resolve(
            listOf("Name", "Title", "unrelated"),
            TrMenuSourceProperty.TITLE
        )

        assertEquals("Name", result.selectedKey)
        assertEquals(listOf("Name", "Title"), result.matchingKeys)
        assertTrue(result.hasCollision)
        assertTrue(result.usedAlias)
    }

    @Test
    fun `supports short plural and optional separator aliases`() {
        assertEquals(
            "Shapes",
            TrMenuKeyResolver.resolve(listOf("Shapes"), TrMenuSourceProperty.LAYOUT).selectedKey
        )
        assertEquals(
            "TransferArguments",
            TrMenuKeyResolver.resolve(
                listOf("TransferArguments"),
                TrMenuSourceProperty.OPTION_ARGUMENTS
            ).selectedKey
        )
        assertEquals(
            "Textures",
            TrMenuKeyResolver.resolve(listOf("Textures"), TrMenuSourceProperty.ICON_MATERIAL).selectedKey
        )
    }

    @Test
    fun `does not guess unknown spelling`() {
        val result = TrMenuKeyResolver.resolve(
            listOf("menu-title", "icon_id"),
            TrMenuSourceProperty.TITLE
        )

        assertEquals(null, result.selectedKey)
        assertTrue(result.matchingKeys.isEmpty())
        assertFalse(result.hasCollision)
    }

    @Test
    fun `preserves dots in dynamic ids`() {
        val config = TrMenuSourceParser.createSourceConfiguration()
        config.loadFromString(
            """
            Tasks:
              refresh.ui:
                period: 20
            """.trimIndent()
        )

        val root = TrMenuSourceSection.from(config)
        val tasks = root.value("Tasks") as TrMenuSourceSection

        assertEquals(listOf("refresh.ui"), tasks.keys)
        assertTrue(tasks.value("refresh.ui") is TrMenuSourceSection)
    }
}
