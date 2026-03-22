@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class MenuManager(private val plugin: KaMenu) {
    private val menus = mutableMapOf<String, YamlConfiguration>()

    fun loadMenus() {
        val folder = File(plugin.dataFolder, "menus")
        if (!folder.exists()) folder.mkdirs()

        if (folder.listFiles()?.isEmpty() == true) {
            saveDefaultMenu(folder)
        }

        folder.listFiles()?.filter { it.extension == "yml" }?.forEach { file ->
            val config = YamlConfiguration.loadConfiguration(file)
            menus[file.nameWithoutExtension] = config
            plugin.logger.info("已加载菜单: ${file.name}")
        }
    }

    private fun saveDefaultMenu(folder: File) {
        val defaultMenuFile = File(folder, "demo.yml")
        if (!defaultMenuFile.exists()) {
            // 从 jar 包内的 resources 目录读取文件并保存
            plugin.getResource("demo.yml")?.use { input ->
                defaultMenuFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    fun getMenuConfig(id: String): YamlConfiguration? {
        return menus[id]
    }
    fun getAllMenuIds(): List<String> {
        return menus.keys.toList()
    }
    fun reload() {
        menus.clear()
        loadMenus()
        plugin.logger.info("§a[KaMenu] 菜单已重载，共加载 ${menus.size} 个菜单。")
    }
    fun clear() {
        menus.clear()
    }
}