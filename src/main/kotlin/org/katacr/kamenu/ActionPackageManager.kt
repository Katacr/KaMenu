package org.katacr.kamenu

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 全局 actions 包管理器。
 *
 * 递归扫描 `plugins/KaMenu/actions` 下的 yml 文件，每个文件代表一个可复用动作包，
 * 文件相对路径去掉 `.yml` 后作为包 ID，例如 `example/welcome.yml` -> `example/welcome`。
 *
 * 菜单内 `actions: packageId` 会先匹配菜单本地 `Events.Click`，找不到时再匹配这里的全局包。
 */
class ActionPackageManager(private val plugin: KaMenu) {
    private val packages = ConcurrentHashMap<String, List<Any>>()
    private val actionsDir = File(plugin.dataFolder, "actions")
    private val defaultPackageResource = "actions/example/welcome.yml"

    /**
     * 包加载结果。
     *
     * 用于 `/kamenu reload actions` 向执行者反馈总数、成功数和失败数。
     */
    data class LoadResult(
        val total: Int = 0,
        val success: Int = 0,
        val failed: Int = 0
    )

    /**
     * 重载所有动作包并返回成功加载的数量。
     *
     * 兼容旧调用点；需要详细统计时使用 [loadPackagesWithResult]。
     */
    fun loadPackages(): Int {
        return loadPackagesWithResult().success
    }

    /**
     * 扫描动作包目录并替换内存缓存。
     *
     * 加载过程会校验包 ID、文件大小、重复 ID 和根 `actions` 列表。
     * 只有所有校验通过的包才会进入缓存；失败文件只记录日志，不阻止其他包加载。
     */
    fun loadPackagesWithResult(): LoadResult {
        val shouldReleaseDefaultPackage = !actionsDir.exists()
        if (!actionsDir.exists() && !actionsDir.mkdirs()) {
            warn("packages.action_folder_create_failed", actionsDir.absolutePath)
            return LoadResult(failed = 1)
        }
        if (shouldReleaseDefaultPackage) {
            releaseDefaultPackage()
        }

        val loaded = LinkedHashMap<String, List<Any>>()
        var total = 0
        var failed = 0

        actionsDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("yml", ignoreCase = true) }
            .forEach { file ->
                total++
                val packageId = toPackageId(file)
                val idError = PackageRules.validatePackageId(packageId)
                if (idError != null) {
                    warn(idError.langKey, packageId, file.absolutePath)
                    failed++
                    return@forEach
                }
                if (file.length() > PackageRules.MAX_PACKAGE_SIZE_BYTES) {
                    warn("packages.action_package_too_large", packageId, file.absolutePath, file.length().toString(), PackageRules.MAX_PACKAGE_SIZE_LABEL)
                    failed++
                    return@forEach
                }
                if (loaded.containsKey(packageId)) {
                    warn("packages.action_duplicate_id", packageId, file.absolutePath)
                    failed++
                    return@forEach
                }

                try {
                    val config = YamlConfiguration.loadConfiguration(file)
                    val actions = config.getList("actions")
                    if (actions.isNullOrEmpty()) {
                        warn("packages.action_missing_actions", packageId, file.absolutePath)
                        failed++
                        return@forEach
                    }
                    loaded[packageId] = actions.map { it ?: Any() }
                } catch (e: Exception) {
                    warn("packages.action_load_failed", packageId, file.absolutePath, e.message ?: e.javaClass.simpleName)
                    failed++
                }
            }

        packages.clear()
        packages.putAll(loaded)
        return LoadResult(total = total, success = packages.size, failed = failed)
    }

    fun reload(): Int {
        return loadPackages()
    }

    fun reloadWithResult(): LoadResult {
        return loadPackagesWithResult()
    }

    /**
     * 获取动作包内容。
     *
     * 返回的列表是 YAML 中的原始动作节点，可包含字符串动作或条件 Map。
     */
    fun getActions(packageId: String): List<Any>? {
        return packages[packageId]
    }

    fun getPackageIds(): Set<String> {
        return packages.keys.toSortedSet()
    }

    private fun toPackageId(file: File): String {
        return file.relativeTo(actionsDir)
            .invariantSeparatorsPath
            .removeSuffix(".yml")
            .trim('/')
    }

    private fun releaseDefaultPackage() {
        try {
            plugin.saveResource(defaultPackageResource, false)
        } catch (e: Exception) {
            warn("packages.action_default_release_failed", defaultPackageResource, e.message ?: e.javaClass.simpleName)
        }
    }

    private fun warn(key: String, vararg args: String) {
        plugin.logger.warning(plugin.languageManager.getMessage(key, *args))
    }
}
