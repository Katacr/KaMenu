package org.katacr.kamenu

import java.io.File
import java.util.concurrent.ConcurrentHashMap

class JavaScriptPackageManager(private val plugin: KaMenu) {
    private val packages = ConcurrentHashMap<String, String>()
    private val jsDir = File(plugin.dataFolder, "js")
    private val defaultPackageResource = "js/example/message.js"

    data class LoadResult(
        val total: Int = 0,
        val success: Int = 0,
        val failed: Int = 0
    )

    fun loadPackages(): Int {
        return loadPackagesWithResult().success
    }

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
