@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.net.JarURLConnection

class MenuManager(private val plugin: KaMenu) {
    private val menus = mutableMapOf<String, YamlConfiguration>()

    data class LoadResult(
        val total: Int = 0,
        val success: Int = 0,
        val failed: Int = 0
    ) {
        operator fun plus(other: LoadResult): LoadResult {
            return LoadResult(
                total = total + other.total,
                success = success + other.success,
                failed = failed + other.failed
            )
        }
    }

    data class ReleaseResult(
        val saved: Int = 0,
        val skipped: Int = 0,
        val failed: Int = 0
    ) {
        operator fun plus(other: ReleaseResult): ReleaseResult {
            return ReleaseResult(
                saved = saved + other.saved,
                skipped = skipped + other.skipped,
                failed = failed + other.failed
            )
        }
    }

    fun loadMenus() {
        val folder = File(plugin.dataFolder, "menus")
        if (!folder.exists()) folder.mkdirs()

        // 递归加载所有菜单文件
        loadMenusRecursively(folder, "")
    }

    fun releaseExampleMenus(language: String = plugin.config.getString("language", "zh_CN") ?: "zh_CN", overwrite: Boolean = false): ReleaseResult {
        val folder = File(plugin.dataFolder, "menus")
        if (!folder.exists()) folder.mkdirs()

        val sourcePath = if (language.equals("en_US", ignoreCase = true)) {
            "menus/exampleEN"
        } else {
            "menus/example"
        }
        return saveDefaultMenus(File(folder, "example"), sourcePath, overwrite)
    }

    /**
     * 从 jar 包内递归释放所有默认菜单文件和文件夹到服务器
     * @param folder 目标文件夹
     * @param resourcePath jar 包内的资源路径前缀
     */
    private fun saveDefaultMenus(folder: File, resourcePath: String, overwrite: Boolean = false): ReleaseResult {
        try {
            if (!folder.exists()) folder.mkdirs()

            // 尝试从 jar 包中获取资源
            val url = plugin.javaClass.classLoader.getResource(resourcePath)

            if (url == null) {
                plugin.logger.warning(plugin.languageManager.getMessage("manager.resource_not_found", resourcePath))
                return ReleaseResult(failed = 1)
            }

            return when (url.protocol) {
                "file" -> {
                    // IDE 开发环境，直接从文件系统读取
                    val sourceDir = File(url.toURI())
                    saveDefaultMenusFromFileSystem(folder, sourceDir, overwrite)
                }
                "jar" -> {
                    // 生产环境，从 jar 包读取
                    val jarConnection = url.openConnection() as JarURLConnection
                    val jarFile = File(jarConnection.jarFileURL.toURI())
                    saveDefaultMenusFromJar(folder, jarFile, resourcePath, overwrite)
                }
                else -> {
                    plugin.logger.warning(plugin.languageManager.getMessage("manager.unsupported_protocol", url.protocol))
                    ReleaseResult(failed = 1)
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning(plugin.languageManager.getMessage("manager.save_error", e.message ?: "Unknown error"))
            return ReleaseResult(failed = 1)
        }
    }

    /**
     * 从文件系统（IDE环境）复制菜单文件
     */
    private fun saveDefaultMenusFromFileSystem(targetFolder: File, sourceDir: File, overwrite: Boolean): ReleaseResult {
        var result = ReleaseResult()
        sourceDir.listFiles()?.forEach { file ->
            val targetFile = File(targetFolder, file.name)

            if (file.isDirectory) {
                // 递归创建子文件夹
                if (!targetFile.exists()) {
                    targetFile.mkdirs()

                }
                result += saveDefaultMenusFromFileSystem(targetFile, file, overwrite)
            } else if (file.name.endsWith(".yml")) {
                // 复制 yml 文件
                if (targetFile.exists() && !overwrite) {
                    result += ReleaseResult(skipped = 1)
                } else {
                    try {
                        file.copyTo(targetFile, overwrite = true)
                        result += ReleaseResult(saved = 1)
                    } catch (e: Exception) {
                        plugin.logger.warning(plugin.languageManager.getMessage("manager.save_error", e.message ?: "Unknown error"))
                        result += ReleaseResult(failed = 1)
                    }
                }
            }
        }
        return result
    }

    /**
     * 从 jar 包中提取菜单文件
     */
    private fun saveDefaultMenusFromJar(targetFolder: File, jarFile: File, resourcePath: String, overwrite: Boolean): ReleaseResult {
        var result = ReleaseResult()
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
                        if (targetFile.exists() && !overwrite) {
                            result += ReleaseResult(skipped = 1)
                        } else {
                            zip.getInputStream(entry).use { input ->
                                targetFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            result += ReleaseResult(saved = 1)
                        }
                    }
            }
        } catch (e: Exception) {
            plugin.logger.warning(plugin.languageManager.getMessage("manager.jar_extract_error", e.message ?: "Unknown error"))
            result += ReleaseResult(failed = 1)
        }
        return result
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
            }
        }
    }

    private fun loadMenusRecursivelyWithResult(folder: File, prefix: String): LoadResult {
        var result = LoadResult()
        folder.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                val newPrefix = if (prefix.isEmpty()) file.name else "$prefix/${file.name}"
                result += loadMenusRecursivelyWithResult(file, newPrefix)
            } else if (file.extension.equals("yml", ignoreCase = true)) {
                result += LoadResult(total = 1)
                val menuId = if (prefix.isEmpty()) file.nameWithoutExtension else "$prefix/${file.nameWithoutExtension}"
                try {
                    val config = YamlConfiguration.loadConfiguration(file)
                    menus[menuId] = config
                    result += LoadResult(success = 1)
                } catch (e: Exception) {
                    plugin.logger.warning(plugin.languageManager.getMessage("manager.menu_load_failed", file.absolutePath, e.message ?: e.javaClass.simpleName))
                    result += LoadResult(failed = 1)
                }
            }
        }
        return result
    }

    fun getMenuConfig(id: String): YamlConfiguration? {
        return menus[id]
    }

    /**
     * 根据配置实例反查菜单ID
     */
    fun getMenuId(config: YamlConfiguration): String? {
        return menus.entries.find { it.value === config }?.key
    }
    fun getAllMenuIds(): List<String> {
        return menus.keys.toList()
    }
    fun reload(): Int {
        menus.clear()
        loadMenus()
        return getAllMenuIds().size
    }

    fun reloadWithResult(): LoadResult {
        menus.clear()
        val folder = File(plugin.dataFolder, "menus")
        if (!folder.exists()) folder.mkdirs()
        return loadMenusRecursivelyWithResult(folder, "")
    }

    fun clear() {
        menus.clear()
    }
}
