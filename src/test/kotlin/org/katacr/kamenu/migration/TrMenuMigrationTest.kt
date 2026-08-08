package org.katacr.kamenu.migration

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.katacr.kamenu.container.ContainerMenuParser
import java.io.File

/** 验证 TrMenu 批量 ID 映射、原子输出和自定义指令合并。 */
class TrMenuMigrationTest {
    @TempDir
    lateinit var temporaryDirectory: File

    @Test
    fun `migrates cross file opens and merges command bindings`() {
        val source = File(temporaryDirectory, "source").apply { mkdirs() }
        val menuRoot = File(temporaryDirectory, "plugins/KaMenu/menus").apply { mkdirs() }
        val target = File(menuRoot, "trmenu_migrated")
        File(source, "Main.yml").writeText(
            """
            Layout: ['A        ']
            Bindings:
              Commands: [main]
              Items:
                - 'material:COMPASS,lore:&aOpen Menu,model-data:1001'
            Icons:
              A:
                display: { material: STONE }
                actions:
                  all: ['open: Other:0 vip']
            """.trimIndent()
        )
        File(source, "Other.yml").writeText(
            """
            Layout: ['B        ']
            Icons:
              B:
                display: { material: PAPER }
            """.trimIndent()
        )

        val migrator = TrMenuMigration(syntaxValidator = { null })
        val result = migrator.migrate(source, target, menuRoot)

        assertEquals(2, result.migrated)
        assertEquals(0, result.failed)
        val main = YamlConfiguration.loadConfiguration(File(target, "Main.yml"))
        assertEquals(
            listOf("open: trmenu_migrated/Other vip"),
            main.getStringList("Buttons.A.actions.all")
        )
        assertTrue(ContainerMenuParser.parse("trmenu_migrated/Main", main).definition != null)

        val commands = YamlConfiguration()
        val merge = migrator.mergeBoundCommands(result, menuRoot, commands)
        assertEquals(1, merge.added)
        assertEquals("trmenu_migrated/Main", commands.getString("custom-commands.main"))

        val itemBindings = YamlConfiguration()
        val itemMerge = migrator.mergeBoundItems(result, itemBindings)
        assertEquals(1, itemMerge.added)
        val bindingPath = "item-bindings.trmenu_trmenu_migrated_main_1"
        assertEquals("trmenu_migrated/Main", itemBindings.getString("$bindingPath.menu"))
        assertEquals("COMPASS", itemBindings.getString("$bindingPath.material"))
        assertEquals("&aOpen Menu", itemBindings.getString("$bindingPath.target-lore"))
        assertEquals(1001, itemBindings.getInt("$bindingPath.custom-model-data"))

        val unchanged = migrator.mergeBoundItems(result, itemBindings)
        assertEquals(1, unchanged.unchanged)
        assertEquals(0, unchanged.conflicts.size)
    }

    @Test
    fun `rejects duplicate trmenu ids across directories`() {
        val source = File(temporaryDirectory, "duplicates").apply { mkdirs() }
        val menuRoot = File(temporaryDirectory, "menus").apply { mkdirs() }
        listOf("one", "two").forEach { folder ->
            File(source, folder).mkdirs()
            File(source, "$folder/Shop.yml").writeText(
                "Layout: ['A        ']\nIcons:\n  A:\n    display: { material: STONE }"
            )
        }

        val result = TrMenuMigration { null }.migrate(source, File(menuRoot, "trmenu_migrated"), menuRoot)

        assertEquals(0, result.migrated)
        assertEquals(2, result.failed)
        assertTrue(result.files.all { file -> file.issues.any { it.code == "TRM_DUPLICATE_MENU_ID" } })
        assertFalse(File(menuRoot, "trmenu_migrated/one/Shop.yml").exists())
    }
}
