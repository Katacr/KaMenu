package org.katacr.kamenu.migration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 验证 TrMenu 通用变量改写和安全失败策略。 */
class TrMenuVariableConverterTest {
    @Test
    fun `rewrites arguments storage aliases and migrated functions`() {
        val config = TrMenuSourceParser.createSourceConfiguration()
        config.loadFromString("Functions:\n  label: 'args[0]'")
        val diagnostics = TrMenuMigrationDiagnostics()
        val functions = TrMenuFunctionRegistry.create(TrMenuSourceSection.from(config), diagnostics)
        val converter = TrMenuVariableConverter(functions)

        val result = converter.rewrite(
            "{0} {m: mode} {d: coins} {globaldata: season} \${label_vip}",
            "Title",
            diagnostics,
            strict = true
        )

        assertEquals(
            "{arg:0} {meta:mode} {data:coins} {gdata:season} {js:[label],`vip`}",
            result
        )
    }

    @Test
    fun `rejects private expressions only in strict contexts`() {
        val diagnostics = TrMenuMigrationDiagnostics()
        val converter = TrMenuVariableConverter()

        assertNull(converter.rewrite("{ke: perm *admin}", "condition", diagnostics, strict = true))
        assertEquals(
            "Text {ke: perm *admin}",
            converter.rewrite("Text {ke: perm *admin}", "Title", diagnostics, strict = false)
        )
        assertTrue(diagnostics.issues.all { it.code == "TRM_VARIABLE_UNSUPPORTED" })
    }
}
