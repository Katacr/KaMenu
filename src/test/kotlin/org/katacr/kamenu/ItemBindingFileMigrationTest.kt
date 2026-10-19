package org.katacr.kamenu

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ItemBindingFileMigrationTest {
    @TempDir lateinit var folder: File

    @Test
    fun `merges legacy config before file converts menus and splits non-sneaking legacy right clicks`() {
        val config = File(folder, "config.yml")
        config.writeText("""
            listeners:
              swap-hand:
                enabled: true
              item-lore:
                sword:
                  enabled: true
                  material: STONE_SWORD
                  menu: old/menu
                  require-sneaking: false
            """.trimIndent())
        val file = File(folder, "item_bindings.yml")
        file.writeText("""
            item-bindings:
              sword:
                enabled: true
                material: DIAMOND_SWORD
                trigger: sneak-q
                actions: ['tell: hello']
            """.trimIndent())
        val originalConfig = config.readText()
        val result = ItemBindingFileMigration.prepare(config, file)
        assertEquals(3, result.converted)
        assertEquals(2, result.backup.size)
        val out = YamlConfiguration.loadConfiguration(file)
        assertEquals(listOf("legacy_sword", "legacy_sword__sneaking", "sword"), out.getConfigurationSection("item-bindings")!!.getKeys(false).toList())
        assertEquals(false, out.getBoolean("item-bindings.legacy_sword.trigger.sneaking"))
        assertEquals(true, out.getBoolean("item-bindings.legacy_sword__sneaking.trigger.sneaking"))
        assertEquals(listOf("open: old/menu"), out.getStringList("item-bindings.legacy_sword.actions"))
        assertEquals("q", out.getString("item-bindings.sword.trigger.type"))
        assertTrue(out.getBoolean("item-bindings.sword.trigger.sneaking"))
        val saved = YamlConfiguration.loadConfiguration(config)
        assertFalse(saved.contains("listeners.item-lore"))
        assertTrue(saved.getBoolean("listeners.swap-hand.enabled"))
        assertEquals(0, ItemBindingFileMigration.prepare(config, file).converted)
        // 模拟目标已保存但主配置写入失败后的恢复：再次迁移不能生成重复条目。
        config.writeText(originalConfig)
        ItemBindingFileMigration.prepare(config, file)
        assertEquals(3, YamlConfiguration.loadConfiguration(file).getConfigurationSection("item-bindings")!!.getKeys(false).size)
    }

    @Test
    fun `invalid legacy entry never removes original files`() {
        val config = File(folder, "config.yml")
        config.writeText("listeners:\n  item-lore:\n    invalid:\n      menu: my/menu\n      unknown: true\n")
        val file = File(folder, "item_bindings.yml")
        file.writeText("item-bindings: {}\n")
        val before = config.readText() to file.readText()
        assertThrows(IllegalStateException::class.java) { ItemBindingFileMigration.prepare(config, file) }
        assertEquals(before, config.readText() to file.readText())
    }

    @Test
    fun `flat click binding keeps explicit cooldown and target`() {
        val config = File(folder, "config.yml").apply { writeText("listeners: {}\n") }
        val file = File(folder, "item_bindings.yml")
        file.writeText("""
            item-bindings:
              knife:
                enabled: true
                material: 'ce:food:knife'
                trigger: sneak-right-click
                click-target: air
                cooldown-ms: 900
                actions: ['command: fd recipe cutting_board']
            """.trimIndent())
        assertEquals(1, ItemBindingFileMigration.prepare(config, file).converted)
        val out = YamlConfiguration.loadConfiguration(file)
        assertEquals("ce:food:knife", out.getString("item-bindings.knife.item.material"))
        assertEquals("right-click", out.getString("item-bindings.knife.trigger.type"))
        assertTrue(out.getBoolean("item-bindings.knife.trigger.sneaking"))
        assertEquals("air", out.getString("item-bindings.knife.trigger.target"))
        assertEquals(900L, out.getLong("item-bindings.knife.trigger.cooldown-ms"))
        assertEquals(0, ItemBindingFileMigration.prepare(config, file).converted)
    }
}
