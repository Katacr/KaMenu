package org.katacr.kamenu.migration

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 验证 TrMenu Functions 的脚本过滤、变量调用和动作守卫转换。 */
class TrMenuFunctionConverterTest {
    @Test
    fun `converts nested function ids and text calls`() {
        val (registry, diagnostics) = registry(
            """
            Functions:
              flash: "args[0] === 'on' ? '&aOn' : '&7Off'"
              status:
                label: |-
                  function label() {
                    return vars('%player_name%') + ':' + args[0]
                  }
                  label()
            """
        )

        assertEquals(setOf("flash", "status.label"), registry.scripts().keys)
        assertEquals(
            "State: {js:[flash],`on`} / {js:[status.label],`vip`}",
            registry.rewriteText("State: \${flash_on} / \${status.label_vip}", "Title", diagnostics)
        )
        assertTrue(diagnostics.issues.isEmpty())
    }

    @Test
    fun `rejects private bindings without rejecting words inside strings`() {
        val (registry, diagnostics) = registry(
            """
            Functions:
              safe: "'session and utils are text'"
              unsafe: "utils.query(args[0])"
            """
        )

        assertTrue(registry.scripts().containsKey("safe"))
        assertFalse(registry.scripts().containsKey("unsafe"))
        assertTrue(diagnostics.issues.any { it.code == "TRM_FUNCTION_UNSUPPORTED_BINDING" })
    }

    @Test
    fun `creates one evaluation guard and reuses it`() {
        val (registry, diagnostics) = registry(
            """
            Functions:
              access: "args[0] === 'vip'"
            """
        )

        val first = registry.convertGuard("access vip", "Actions[0]", diagnostics)
        val second = registry.convertGuard("access admin", "Actions[1]", diagnostics)

        assertNotNull(first)
        assertEquals("{js:[__trmenu_guard.access],`vip`} == true", first?.get("condition"))
        assertEquals(listOf("return"), first?.get("deny"))
        assertEquals("{js:[__trmenu_guard.access],`admin`} == true", second?.get("condition"))
        assertEquals(2, registry.scripts().size)
    }

    @Test
    fun `strict text conversion fails for missing function`() {
        val (registry, diagnostics) = registry("Functions: {}")

        val result = registry.rewriteText("\${missing_value}", "condition", diagnostics, strict = true)

        assertEquals(null, result)
        assertTrue(diagnostics.issues.any { it.code == "TRM_FUNCTION_REFERENCE_UNSUPPORTED" })
    }

    @Test
    fun `removes functions that depend on rejected functions`() {
        val (registry, diagnostics) = registry(
            """
            Functions:
              caller: 'vars("${'$'}{unsafe_value}")'
              unsafe: 'utils.query(args[0])'
            """
        )

        assertTrue(registry.scripts().isEmpty())
        assertTrue(diagnostics.issues.any {
            it.code == "TRM_FUNCTION_REFERENCE_UNSUPPORTED" && it.path == "Functions.caller"
        })
    }

    private fun registry(yaml: String): Pair<TrMenuFunctionRegistry, TrMenuMigrationDiagnostics> {
        val config = YamlConfiguration()
        config.loadFromString(yaml.trimIndent())
        val diagnostics = TrMenuMigrationDiagnostics()
        return TrMenuFunctionRegistry.create(
            TrMenuSourceSection.from(config),
            diagnostics,
            syntaxValidator = { null }
        ) to diagnostics
    }
}
