package org.katacr.kamenu

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class ActionPackageManager(private val plugin: KaMenu) {
    private val packages = ConcurrentHashMap<String, List<Any>>()
    private val actionsDir = File(plugin.dataFolder, "actions")
    private val defaultPackageResource = "actions/example/welcome.yml"

    data class LoadResult(
        val total: Int = 0,
        val success: Int = 0,
        val failed: Int = 0
    )

    fun loadPackages(): Int {
        return loadPackagesWithResult().success
    }

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
        info("packages.action_loaded", packages.size.toString())
        return LoadResult(total = total, success = packages.size, failed = failed)
    }

    fun reload(): Int {
        return loadPackages()
    }

    fun reloadWithResult(): LoadResult {
        return loadPackagesWithResult()
    }

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
            info("packages.action_default_released", "plugins/KaMenu/$defaultPackageResource")
        } catch (e: Exception) {
            warn("packages.action_default_release_failed", defaultPackageResource, e.message ?: e.javaClass.simpleName)
        }
    }

    private fun info(key: String, vararg args: String) {
        plugin.logger.info(plugin.languageManager.getMessage(key, *args))
    }

    private fun warn(key: String, vararg args: String) {
        plugin.logger.warning(plugin.languageManager.getMessage(key, *args))
    }
}
