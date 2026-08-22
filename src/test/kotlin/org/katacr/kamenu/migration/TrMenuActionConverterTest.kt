package org.katacr.kamenu.migration

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 验证 TrMenu 动作别名、组合修饰符和结构化动作转换。 */
class TrMenuActionConverterTest {
    private val converter = TrMenuActionConverter(
        menuIdResolver = { id -> mapOf("Other" to "trmenu_migrated/other")[id] },
        iconIdResolver = { id -> mapOf("Old" to "new_button")[id] }
    )

    @Test
    fun `converts direct sound title open and storage actions`() {
        val diagnostics = TrMenuMigrationDiagnostics()
        val result = converter.convert(
            listOf(
                "sound: BLOCK_NOTE_BLOCK_PLING-1-2;ENTITY_PLAYER_LEVELUP-0.5-1",
                "title: `Main title` `Sub title` 10 20 30",
                "open: Other:0 {0} vip",
                "set-meta: mode sell",
                "del-gdata: old_key"
            ),
            "actions",
            diagnostics
        )

        assertEquals(
            listOf(
                "sound: BLOCK_NOTE_BLOCK_PLING;volume=1.0;pitch=2.0",
                "sound: ENTITY_PLAYER_LEVELUP;volume=0.5;pitch=1.0",
                "title: title=`Main title`;subtitle=`Sub title`;in=10;keep=20;out=30",
                "open: trmenu_migrated/other {arg:0} vip",
                "meta: type=set;key=mode;var=`sell`",
                "gdata: type=delete;key=old_key"
            ),
            result
        )
        assertTrue(diagnostics.issues.isEmpty())
    }

    @Test
    fun `copies shared chance and delay to combined actions`() {
        val diagnostics = TrMenuMigrationDiagnostics()
        val result = converter.convert(
            "tell: one &&& sound: ENTITY_EXPERIENCE_ORB_PICKUP-1-1 {Chance=25} {Delay=20}",
            "actions[0]",
            diagnostics
        )

        assertEquals(
            listOf(
                "tell: one {chance: 25} {wait: 20}",
                "sound: ENTITY_EXPERIENCE_ORB_PICKUP;volume=1.0;pitch=1.0 {chance: 25} {wait: 20}"
            ),
            result
        )
    }

    @Test
    fun `converts menu argument update and clear actions`() {
        val diagnostics = TrMenuMigrationDiagnostics()

        val result = converter.convert(
            listOf(
                "set-args: {0} {1} {3}",
                "del-args"
            ),
            "actions",
            diagnostics
        )

        assertEquals(
            listOf(
                "set-args: {arg:0} {arg:1} {arg:3}",
                "del-args"
            ),
            result
        )
        assertTrue(diagnostics.issues.isEmpty())
        assertTrue(diagnostics.issues.none { it.code == "TRM_ACTION_UNSUPPORTED" })
    }

    @Test
    fun `wraps supported condition modifier`() {
        val diagnostics = TrMenuMigrationDiagnostics()
        val result = converter.convert(
            "tell: allowed {condition=perm *shop.use}",
            "actions[0]",
            diagnostics
        )

        val branch = result.single() as Map<*, *>
        assertEquals("hasPerm.shop.use", branch["condition"])
        assertEquals(listOf("tell: allowed"), branch["allow"])
    }

    @Test
    fun `rewrites refresh IDs and filters unsafe actions`() {
        val diagnostics = TrMenuMigrationDiagnostics()
        val result = converter.convert(
            listOf(
                "refresh: Old;*",
                "js: player.setHealth(0)",
                "unknown-action: value"
            ),
            "actions",
            diagnostics
        )

        assertEquals(listOf("refresh: new_button", "refresh: *"), result)
        assertEquals(2, diagnostics.issues.count { it.code == "TRM_ACTION_UNSUPPORTED" })
    }

    @Test
    fun `converts page switching action`() {
        val diagnostics = TrMenuMigrationDiagnostics()
        val result = converter.convert(
            listOf("page: 1", "page: next", "page: prev", "page: +2", "page: -1"),
            "actions",
            diagnostics
        )

        assertEquals(listOf("page: 1", "page: next", "page: prev", "page: +2", "page: -1"), result)
        assertTrue(diagnostics.issues.none { it.code == "TRM_ACTION_UNSUPPORTED" })
    }

    @Test
    fun `converts untargeted refresh update and reset to all button refresh`() {
        val diagnostics = TrMenuMigrationDiagnostics()
        val result = converter.convert(
            listOf("refresh", "update", "reset"),
            "actions",
            diagnostics
        )

        assertEquals(listOf("refresh", "refresh", "refresh"), result)
        assertEquals(1, diagnostics.issues.count { it.code == "TRM_ACTION_UPDATE_APPROXIMATE" })
        assertEquals(1, diagnostics.issues.count { it.code == "TRM_ACTION_RESET_APPROXIMATE" })
        assertTrue(diagnostics.issues.none { it.code == "TRM_ACTION_UNSUPPORTED" })
    }

    @Test
    fun `expands list values in yaml action maps`() {
        val config = YamlConfiguration()
        config.loadFromString(
            """
            action:
              tell:
                - one
                - two
            """.trimIndent()
        )
        val diagnostics = TrMenuMigrationDiagnostics()
        val section = TrMenuSourceSection.from(config).value("action")

        val result = converter.convert(section, "actions[0]", diagnostics)

        assertEquals(listOf("tell: one", "tell: two"), result)
        assertTrue(diagnostics.issues.isEmpty())
    }

    @Test
    fun `unwraps yaml anchor action list wrappers`() {
        val config = YamlConfiguration()
        config.loadFromString(
            """
            defaultActions: &defaultActions
              actions:
                - 'set-meta: mode sell'
                - 'wait: 1'
                - 'sound: BLOCK_NOTE_BLOCK_PLING-1-1'
            button:
              actions:
                - *defaultActions
            """.trimIndent()
        )
        val diagnostics = TrMenuMigrationDiagnostics()
        val section = TrMenuSourceSection.from(config).value("button")

        val result = converter.convert(section, "actions[0]", diagnostics)

        assertEquals(
            listOf(
                "meta: type=set;key=mode;var=`sell`",
                "wait: 1",
                "sound: BLOCK_NOTE_BLOCK_PLING;volume=1.0;pitch=1.0"
            ),
            result
        )
        assertTrue(diagnostics.issues.isEmpty())
    }

    @Test
    fun `does not silently drop modifiers from structured guards`() {
        val guarded = TrMenuActionConverter(
            functionGuardConverter = { _, _, _ ->
                linkedMapOf(
                    "condition" to "true",
                    "allow" to emptyList<Any>(),
                    "deny" to listOf("return")
                )
            }
        )
        val diagnostics = TrMenuMigrationDiagnostics()

        val result = guarded.convert("function: access {chance=50}", "actions[0]", diagnostics)

        assertTrue(result.isEmpty())
        assertTrue(diagnostics.issues.any { it.code == "TRM_ACTION_UNSUPPORTED" })
    }

    @Test
    fun `recursively converts nested condition action tree`() {
        val config = YamlConfiguration()
        config.loadFromString(
            """
            actions:
              - condition: 'check papi *%player_empty_slots% >= *2'
                deny:
                  - 'tell: not enough space'
                actions:
                  - condition: 'check papi *%vip% >= *10'
                    deny:
                      - 'tell: not vip'
                    actions:
                      - 'tell: success'
                      - 'console: give item'
            """.trimIndent()
        )
        val diagnostics = TrMenuMigrationDiagnostics()
        val section = TrMenuSourceSection.from(config).value("actions")

        val result = converter.convert(section, "actions", diagnostics)

        assertTrue(diagnostics.issues.none { it.code == "TRM_ACTION_MAP_EXTRA_KEYS" })
        assertEquals(1, result.size)
        val outer = result[0] as Map<*, *>
        assertEquals("%player_empty_slots% >= 2", outer["condition"])
        assertEquals(listOf("tell: not enough space"), outer["deny"])
        val innerAllow = outer["allow"] as List<*>
        assertEquals(1, innerAllow.size)
        val inner = innerAllow[0] as Map<*, *>
        assertEquals("%vip% >= 10", inner["condition"])
        assertEquals(listOf("tell: not vip"), inner["deny"])
        val leafAllow = inner["allow"] as List<*>
        assertEquals(listOf("tell: success", "console: give item"), leafAllow)
    }

    @Test
    fun `converts set-title action to KaMenu set-title action`() {
        val diagnostics = TrMenuMigrationDiagnostics()
        val result = converter.convert(
            listOf(
                "set-title: '&c&l欢迎回来, %player_name%'",
                "set-title: 普通标题"
            ),
            "actions",
            diagnostics
        )

        assertEquals(
            listOf(
                "set-title: '&c&l欢迎回来, %player_name%'",
                "set-title: 普通标题"
            ),
            result
        )
        assertTrue(diagnostics.hasErrors.not())
    }
}
