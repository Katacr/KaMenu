@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import org.katacr.kamenu.container.ContainerDiagnosticSeverity
import org.katacr.kamenu.container.ContainerMenuDefinition
import org.katacr.kamenu.container.ContainerMenuDiagnostic
import org.katacr.kamenu.container.ContainerMenuParser
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.net.JarURLConnection

/** MenuManager 识别出的菜单渲染类型。 */
enum class MenuKind {
    DIALOG,
    CONTAINER
}

/**
 * 菜单文件管理器。
 *
 * 负责递归扫描 `plugins/KaMenu/menus` 下的 yml 文件，并以相对路径作为菜单 ID 缓存到内存。
 * 例如 `menus/example/main_menu.yml` 会注册为 `example/main_menu`。
 *
 * 此类只管理文件菜单；外部插件通过 API 打开的内存菜单不会写入这里。
 */
class MenuManager(private val plugin: KaMenu) {
    private val menus = mutableMapOf<String, YamlConfiguration>()
    private val menuKinds = mutableMapOf<String, MenuKind>()
    private val containerMenus = mutableMapOf<String, ContainerMenuDefinition>()
    private val containerDiagnostics = mutableMapOf<String, List<ContainerMenuDiagnostic>>()
    @Volatile
    private var generation: Long = 0L

    /**
     * 菜单加载统计。
     */
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

    /**
     * 示例菜单释放统计。
     */
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

    /**
     * 加载所有菜单文件。
     *
     * 用于插件启动时的快速加载；reload 指令需要详细统计时使用 [reloadWithResult]。
     */
    fun loadMenus() {
        generation++
        val folder = File(plugin.dataFolder, "menus")
        if (!folder.exists()) folder.mkdirs()

        loadMenusRecursively(folder, "")
    }

    /**
     * 释放内置示例菜单到 `menus/example`。
     *
     * @param language `zh_CN` 使用中文示例，`en_US` 使用英文示例。
     * @param overwrite 是否覆盖已有示例文件。
     * @param includeDialog 是否同时释放需要原生 Dialog API 的示例。
     */
    fun releaseExampleMenus(
        language: String = plugin.config.getString("language", "zh_CN") ?: "zh_CN",
        overwrite: Boolean = false,
        includeDialog: Boolean = MenuUI.dialogSupported
    ): ReleaseResult {
        val folder = File(plugin.dataFolder, "menus")
        if (!folder.exists()) folder.mkdirs()

        val languageFolder = if (language.equals("en_US", ignoreCase = true)) {
            "en_US"
        } else {
            "zh_CN"
        }
        val targetFolder = File(folder, "example")
        var result = saveDefaultMenus(
            targetFolder,
            "examples/container/$languageFolder",
            overwrite
        )
        if (includeDialog) {
            result += saveDefaultMenus(
                targetFolder,
                "examples/dialog/$languageFolder",
                overwrite
            )
        }
        return result
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

    /** 递归加载菜单目录，并统一统计 Dialog 与 Container 菜单的加载结果。 */
    private fun loadMenusRecursively(folder: File, prefix: String): LoadResult {
        var result = LoadResult()
        folder.listFiles()?.sortedBy { it.name.lowercase() }?.forEach { file ->
            if (file.isDirectory) {
                val newPrefix = if (prefix.isEmpty()) file.name else "$prefix/${file.name}"
                result += loadMenusRecursively(file, newPrefix)
            } else if (file.extension.equals("yml", ignoreCase = true)) {
                val menuId = if (prefix.isEmpty()) file.nameWithoutExtension else "$prefix/${file.nameWithoutExtension}"
                result += loadMenuFile(file, menuId)
            }
        }
        return result
    }

    /**
     * 加载并注册单个菜单文件。
     *
     * Container 菜单只有在结构解析完全成功后才会进入公共菜单缓存；解析诊断会保留供日志和后续编辑器读取。
     */
    private fun loadMenuFile(file: File, menuId: String): LoadResult {
        removeCachedMenu(menuId, removeDiagnostics = true)
        return try {
            val config = YamlConfiguration().apply { load(file) }
            if (ContainerMenuParser.isContainerMenu(config)) {
                loadContainerMenu(file, menuId, config)
            } else {
                menus[menuId] = config
                menuKinds[menuId] = MenuKind.DIALOG
                LoadResult(total = 1, success = 1)
            }
        } catch (e: Exception) {
            plugin.logger.warning(
                plugin.languageManager.getMessage(
                    "manager.menu_load_failed",
                    file.absolutePath,
                    e.message ?: e.javaClass.simpleName
                )
            )
            LoadResult(total = 1, failed = 1)
        }
    }

    /** 解析并注册一个 Container 菜单，同时输出本地化诊断外壳。 */
    private fun loadContainerMenu(
        file: File,
        menuId: String,
        config: YamlConfiguration
    ): LoadResult {
        val parseResult = ContainerMenuParser.parse(menuId, config)
        containerDiagnostics[menuId] = parseResult.diagnostics.toList()
        logContainerDiagnostics(menuId, parseResult.diagnostics)

        val definition = parseResult.definition
        if (!parseResult.succeeded || definition == null) {
            val errorCount = parseResult.diagnostics.count { it.severity == ContainerDiagnosticSeverity.ERROR }
            plugin.logger.warning(
                plugin.languageManager.getMessage(
                    "manager.container_load_failed",
                    menuId,
                    errorCount,
                    file.absolutePath
                )
            )
            return LoadResult(total = 1, failed = 1)
        }

        menus[menuId] = config
        menuKinds[menuId] = MenuKind.CONTAINER
        containerMenus[menuId] = definition
        return LoadResult(total = 1, success = 1)
    }

    /** 将容器解析诊断写入控制台；警告和错误使用不同日志级别。 */
    private fun logContainerDiagnostics(menuId: String, diagnostics: List<ContainerMenuDiagnostic>) {
        diagnostics.forEach { diagnostic ->
            val key = when (diagnostic.severity) {
                ContainerDiagnosticSeverity.WARNING -> "manager.container_diagnostic_warning"
                ContainerDiagnosticSeverity.ERROR -> "manager.container_diagnostic_error"
            }
            val message = plugin.languageManager.getMessage(
                key,
                menuId,
                diagnostic.code,
                diagnostic.path,
                diagnostic.message
            )
            when (diagnostic.severity) {
                ContainerDiagnosticSeverity.WARNING -> plugin.logger.warning(message)
                ContainerDiagnosticSeverity.ERROR -> plugin.logger.severe(message)
            }
        }
    }

    /** 移除指定菜单的全部派生缓存，避免重载失败后残留旧定义。 */
    private fun removeCachedMenu(menuId: String, removeDiagnostics: Boolean) {
        menus.remove(menuId)
        menuKinds.remove(menuId)
        containerMenus.remove(menuId)
        if (removeDiagnostics) {
            containerDiagnostics.remove(menuId)
        }
    }

    /** 清空所有菜单配置、类型、容器定义和诊断缓存。 */
    private fun clearCaches() {
        menus.clear()
        menuKinds.clear()
        containerMenus.clear()
        containerDiagnostics.clear()
    }

    /**
     * 取得已加载菜单配置。
     */
    fun getMenuConfig(id: String): YamlConfiguration? {
        return menus[id]
    }

    /** 取得已成功加载菜单的渲染类型。 */
    fun getMenuKind(id: String): MenuKind? {
        return menuKinds[id]
    }

    /** 取得已成功解析的 Container 菜单定义。 */
    fun getContainerMenu(id: String): ContainerMenuDefinition? {
        return containerMenus[id]
    }

    /** 取得 Container 菜单最近一次加载产生的不可变诊断列表。 */
    fun getContainerDiagnostics(id: String): List<ContainerMenuDiagnostic> {
        return containerDiagnostics[id].orEmpty()
    }

    /** 返回当前菜单加载代际，用于拒绝重载前创建的旧容器会话。 */
    fun getGeneration(): Long {
        return generation
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

    /** 重新加载全部菜单并返回成功注册的菜单数量。 */
    fun reload(): Int {
        clearCaches()
        loadMenus()
        return getAllMenuIds().size
    }

    /** 重新加载全部菜单并返回包含 Container 结构校验结果的统计。 */
    fun reloadWithResult(): LoadResult {
        clearCaches()
        generation++
        val folder = File(plugin.dataFolder, "menus")
        if (!folder.exists()) folder.mkdirs()
        return loadMenusRecursively(folder, "")
    }

    /** 清空全部已加载菜单和容器解析缓存。 */
    fun clear() {
        clearCaches()
        generation++
    }
}
