package org.katacr.kamenu.migration

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 验证 TrMenu Reaction 排序、Close return 差异和周期任务转换。 */
class TrMenuEventConverterTest {
    @Test
    fun `sorts open reactions and converts conditional branches`() {
        val (result, diagnostics) = convert(
            """
            Events:
              Open:
                - priority: 20
                  actions: ['tell: later']
                - priority: 10
                  condition: 'perm *shop.use'
                  actions: ['tell: allowed']
                  deny-actions: ['tell: denied']
            """
        )

        val branch = result.open.first() as Map<*, *>
        assertEquals("hasPerm.shop.use", branch["condition"])
        assertEquals(listOf("tell: allowed"), branch["allow"])
        assertEquals(listOf("tell: denied"), branch["deny"])
        assertEquals("tell: later", result.open[1])
        assertTrue(diagnostics.issues.isEmpty())
    }

    @Test
    fun `fails closed when open condition is unsupported`() {
        val (result, diagnostics) = convert(
            """
            Events:
              Open:
                - condition: 'js: player.isOp()'
                  actions: ['console: give {player} diamond']
                  deny-actions: ['tell: denied']
            """
        )

        assertEquals(listOf("tell: denied", "return"), result.open)
        assertTrue(diagnostics.issues.any { it.code == "TRM_CONDITION_UNSUPPORTED" })
    }

    @Test
    fun `removes close return and truncates later actions`() {
        val (result, diagnostics) = convert(
            """
            Events:
              Close:
                - 'tell: before'
                - 'return'
                - 'tell: after'
            """
        )

        assertEquals(listOf("tell: before"), result.close)
        assertTrue(diagnostics.issues.any { it.code == "TRM_CLOSE_RETURN_APPROXIMATE" })
    }

    @Test
    fun `creates independent auto tasks for every trmenu task branch`() {
        val (result, diagnostics) = convert(
            """
            Tasks:
              refresh.ui:
                period: 40
                task:
                  - condition: 'perm *menu.view'
                    actions: ['refresh: *']
                  - condition: 'check *1 =? *1'
                    actions: ['tell: tick']
            """
        )

        assertEquals(setOf("refresh_ui_1", "refresh_ui_2"), result.tasks.keys)
        assertEquals(40L, result.tasks.getValue("refresh_ui_1")["interval"])
        assertEquals(false, result.tasks.getValue("refresh_ui_1")["run_immediately"])
        assertEquals(2, diagnostics.issues.count { it.code == "TRM_TASK_INITIAL_DELAY_APPROXIMATE" })
        assertFalse(diagnostics.hasErrors)
    }

    private fun convert(yaml: String): Pair<TrMenuEventConversion, TrMenuMigrationDiagnostics> {
        val config = TrMenuSourceParser.createSourceConfiguration()
        config.loadFromString(yaml.trimIndent())
        val diagnostics = TrMenuMigrationDiagnostics()
        val converter = TrMenuEventConverter(TrMenuActionConverter())
        return converter.convert(TrMenuSourceSection.from(config), diagnostics) to diagnostics
    }
}
