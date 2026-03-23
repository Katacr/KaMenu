@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class MenuManager(private val plugin: KaMenu) {
    private val menus = mutableMapOf<String, YamlConfiguration>()

    fun loadMenus() {
        val folder = File(plugin.dataFolder, "menus")
        if (!folder.exists()) folder.mkdirs()

        // 首次加载时释放所有默认菜单
        if (folder.listFiles()?.isEmpty() == true) {
            saveDefaultMenus(folder, "menus")
        }

        // 递归加载所有菜单文件
        loadMenusRecursively(folder, "")
    }

    /**
     * 从 jar 包内递归释放所有默认菜单文件和文件夹到服务器
     * @param folder 目标文件夹
     * @param resourcePath jar 包内的资源路径前缀
     */
    private fun saveDefaultMenus(folder: File, resourcePath: String) {
        try {
            // 尝试从 jar 包中获取资源
            val url = plugin.javaClass.classLoader.getResource(resourcePath)

            if (url == null) {
                plugin.logger.warning(plugin.languageManager.getMessage("manager.resource_not_found", resourcePath))
                return
            }

            when (url.protocol) {
                "file" -> {
                    // IDE 开发环境，直接从文件系统读取
                    val sourceDir = File(url.toURI())
                    saveDefaultMenusFromFileSystem(folder, sourceDir, resourcePath)
                }
                "jar" -> {
                    // 生产环境，从 jar 包读取
                    val jarPath = url.path.substringBefore("!")
                    val jarFile = File(jarPath.substringAfter("file:"))
                    saveDefaultMenusFromJar(folder, jarFile, resourcePath)
                }
                else -> {
                    plugin.logger.warning(plugin.languageManager.getMessage("manager.unsupported_protocol", url.protocol))
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning(plugin.languageManager.getMessage("manager.save_error", e.message ?: "Unknown error"))
        }
    }

    /**
     * 从文件系统（IDE环境）复制菜单文件
     */
    private fun saveDefaultMenusFromFileSystem(targetFolder: File, sourceDir: File, resourcePath: String) {
        sourceDir.listFiles()?.forEach { file ->
            val targetFile = File(targetFolder, file.name)

            if (file.isDirectory) {
                // 递归创建子文件夹
                if (!targetFile.exists()) {
                    targetFile.mkdirs()
                    plugin.logger.info(plugin.languageManager.getMessage("manager.folder_created", resourcePath, file.name))
                }
                saveDefaultMenusFromFileSystem(targetFile, file, "$resourcePath/${file.name}")
            } else if (file.name.endsWith(".yml")) {
                // 复制 yml 文件
                if (!targetFile.exists()) {
                    file.copyTo(targetFile, overwrite = false)
                    plugin.logger.info(plugin.languageManager.getMessage("manager.file_saved", resourcePath, file.name))
                }
            }
        }
    }

    /**
     * 从 jar 包中提取菜单文件
     */
    private fun saveDefaultMenusFromJar(targetFolder: File, jarFile: File, resourcePath: String) {
        try {
            java.util.zip.ZipFile(jarFile).use { zip ->
                zip.entries().asSequence()
                    .filter { it.name.startsWith("$resourcePath/") }
                    .filter { !it.isDirectory }
                    .filter { it.name.endsWith(".yml") }
                    .forEach { entry ->
                        val relativePath = entry.name.substringAfter("$resourcePath/")
                        val targetFile = File(targetFolder, relativePath)

                        // 确保父文件夹存在
                        targetFile.parentFile?.mkdirs()

                        // 提取文件
                        if (!targetFile.exists()) {
                            zip.getInputStream(entry).use { input ->
                                targetFile.outputStream().use { output ->
                                    input.copyTo(output)
                                    plugin.logger.info(plugin.languageManager.getMessage("manager.file_saved", resourcePath, relativePath))
                                }
                            }
                        }
                    }
            }
        } catch (e: Exception) {
            plugin.logger.warning(plugin.languageManager.getMessage("manager.jar_extract_error", e.message ?: "Unknown error"))
        }
    }

    /**
     * 递归加载文件夹中的所有菜单文件
     * @param folder 要扫描的文件夹
     * @param prefix 菜单 ID 前缀（用于子文件夹）
     */
    private fun loadMenusRecursively(folder: File, prefix: String) {
        folder.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                // 递归处理子文件夹
                val newPrefix = if (prefix.isEmpty()) file.name else "$prefix/${file.name}"
                loadMenusRecursively(file, newPrefix)
            } else if (file.extension == "yml") {
                // 加载菜单文件
                val menuId = if (prefix.isEmpty()) file.nameWithoutExtension else "$prefix/${file.nameWithoutExtension}"
                val config = YamlConfiguration.loadConfiguration(file)
                menus[menuId] = config
                plugin.logger.info(plugin.languageManager.getMessage("menu.loaded", menuId))
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
        plugin.logger.info(plugin.languageManager.getMessage("menu.reloaded", menus.size))
    }
    fun clear() {
        menus.clear()
    }
}