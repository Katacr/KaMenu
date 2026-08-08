package org.katacr.kamenu.migration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 验证 TrMenu 点击类型、Reaction 和继承动作层的合并。 */
class TrMenuClickActionConverterTest {
    @Test
    fun `merges click types across inherited layers`() {
        val config = TrMenuSourceParser.createSourceConfiguration()
        config.loadFromString(
            """
            parent:
              all: ['tell: parent']
              left,right:
                - condition: 'perm *shop.use'
                  actions: ['tell: click']
                  deny-actions: ['tell: denied']
            child:
              all: ['tell: child']
            """.trimIndent()
        )
        val root = TrMenuSourceSection.from(config)
        val diagnostics = TrMenuMigrationDiagnostics()
        val actions = TrMenuActionConverter()
        val converter = TrMenuClickActionConverter(TrMenuEventConverter(actions))

        val result = converter.convert(
            listOf(
                TrMenuActionLayer(root.value("parent"), "parent"),
                TrMenuActionLayer(root.value("child"), "child")
            ),
            diagnostics
        )

        assertEquals(listOf("tell: parent", "tell: child"), result["all"])
        assertEquals(result["left"], result["right"])
        val left = result.getValue("left").single() as Map<*, *>
        assertEquals("hasPerm.shop.use", left["condition"])
        assertFalse(diagnostics.hasErrors)
    }

    @Test
    fun `skips outside and drag click types but preserves trmenu unknown fallback`() {
        val config = TrMenuSourceParser.createSourceConfiguration()
        config.loadFromString(
            """
            actions:
              abroad_left_empty: ['tell: outside']
              typo_click: ['tell: fallback']
            """.trimIndent()
        )
        val root = TrMenuSourceSection.from(config)
        val diagnostics = TrMenuMigrationDiagnostics()
        val converter = TrMenuClickActionConverter(TrMenuEventConverter(TrMenuActionConverter()))

        val result = converter.convert(
            listOf(TrMenuActionLayer(root.value("actions"), "actions")),
            diagnostics
        )

        assertEquals(listOf("tell: fallback"), result["all"])
        assertTrue(diagnostics.issues.any { it.code == "TRM_CLICK_TYPE_UNSUPPORTED" })
        assertTrue(diagnostics.issues.any { it.code == "TRM_CLICK_TYPE_FALLBACK_ALL" })
    }
}
