package org.katacr.kamenu

import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 全局 JavaScript 包管理器。
 *
 * 递归扫描 `plugins/KaMenu/js` 下的 js 文件，每个文件代表一个可复用脚本包，
 * 文件相对路径去掉 `.js` 后作为包 ID，例如 `example/message.js` -> `example/message`。
 *
 * 包调用使用 `{js:[packageId],arg1,arg2}` 或动作 `js: [packageId],arg1,arg2`。
 */
class JavaScriptPackageManager(private val plugin: KaMenu) {
    private val packages = ConcurrentHashMap<String, String>()
    private val jsDir = File(plugin.dataFolder, "js")
    private val defaultPackageResource = "js/example/message.js"

    /**
     * 包加载结果。
     *
     * 用于 reload 指令反馈和后续诊断。
     */
    data class LoadResult(
        val total: Int = 0,
        val success: Int = 0,
        val failed: Int = 0
    )

    /**
     * 重载所有 JavaScript 包并返回成功加载的数量。
     *
     * 兼容旧调用点；需要总数/失败数时使用 [loadPackagesWithResult]。
     */
    fun loadPackages(): Int {
        return loadPackagesWithResult().success
    }

    /**
     * 扫描 JS 包目录并替换内存缓存。
     *
     * 加载时会校验包 ID、文件大小、重复 ID，并通过 Nashorn 做语法校验。
     * 语法错误的脚本不会进入缓存，避免运行时才暴露基础语法问题。
     */
    fun loadPackagesWithResult(): LoadResult {
        val shouldReleaseDefaultPackage = !jsDir.exists()
        if (!jsDir.exists() && !jsDir.mkdirs()) {
            warn("packages.javascript_folder_create_failed", jsDir.absolutePath)
            return LoadResult(failed = 1)
        }
        if (shouldReleaseDefaultPackage) {
            releaseDefaultPackage()
        }

        val loaded = LinkedHashMap<String, String>()
        var total = 0
        var failed = 0

        jsDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("js", ignoreCase = true) }
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
                    warn("packages.javascript_package_too_large", packageId, file.absolutePath, file.length().toString(), PackageRules.MAX_PACKAGE_SIZE_LABEL)
                    failed++
                    return@forEach
                }
                if (loaded.containsKey(packageId)) {
                    warn("packages.javascript_duplicate_id", packageId, file.absolutePath)
                    failed++
                    return@forEach
                }

                try {
                    val script = file.readText(Charsets.UTF_8)
                    val syntaxError = JavaScriptManager.validateSyntax(script)
                    if (syntaxError != null) {
                        warn("packages.javascript_syntax_invalid", packageId, file.absolutePath, syntaxError)
                        failed++
                        return@forEach
                    }
                    loaded[packageId] = script
                } catch (e: Exception) {
                    warn("packages.javascript_load_failed", packageId, file.absolutePath, e.message ?: e.javaClass.simpleName)
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
     * 获取已加载脚本源码。
     */
    fun getScript(packageId: String): String? {
        return packages[packageId]
    }

    fun getPackageIds(): Set<String> {
        return packages.keys.toSortedSet()
    }

    private fun toPackageId(file: File): String {
        return file.relativeTo(jsDir)
            .invariantSeparatorsPath
            .removeSuffix(".js")
            .trim('/')
    }

    private fun releaseDefaultPackage() {
        try {
            plugin.saveResource(defaultPackageResource, false)
        } catch (e: Exception) {
            warn("packages.javascript_default_release_failed", defaultPackageResource, e.message ?: e.javaClass.simpleName)
        }
    }

    private fun warn(key: String, vararg args: String) {
        plugin.logger.warning(plugin.languageManager.getMessage(key, *args))
    }
}
