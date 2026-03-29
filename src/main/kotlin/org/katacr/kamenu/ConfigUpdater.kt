package org.katacr.kamenu

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * 配置文件更新管理器
 * 用于处理配置文件的版本升级、合并和备份
 */
object ConfigUpdater {

    private var languageManager: LanguageManager? = null

    /**
     * 设置语言管理器
     * @param lm 语言管理器实例
     */
    fun setLanguageManager(lm: LanguageManager) {
        languageManager = lm
    }

    /**
     * 获取本地化消息
     * 如果语言管理器未初始化，返回键本身
     */
    private fun getMessage(key: String, vararg args: Any): String {
        val lm = languageManager
        return if (lm != null) {
            lm.getMessage(key, *args)
        } else {
            // 如果语言管理器未初始化，使用硬编码的英文消息
            when (key) {
                "config_update.detected_old_version" -> "Detected old version config file (v${args[0]}), updating to v${args[1]}..."
                "config_update.default_config_not_exist" -> "Default config file not found, skipping config update"
                "config_update.detected_deprecated_config" -> "Detected deprecated config item in user config: ${args[0]}"
                "config_update.update_success" -> "Config file updated successfully! Old version: v${args[0]} -> New version: v${args[1]}"
                "config_update.backup_location" -> "Old config backed up to: ${args[0]}"
                "config_update.save_failed" -> "Failed to save config file: ${args[0]}"
                "config_update.backup_success" -> "Old config backed up to: ${args[0]}"
                "config_update.backup_failed" -> "Failed to backup config file: ${args[0]}"
                else -> key
            }
        }
    }

    /**
     * 当前配置文件版本
     * 每次配置文件结构变更时需要增加此版本号
     */
    private const val CURRENT_CONFIG_VERSION = 1

    /**
     * 配置版本键名
     */
    private const val CONFIG_VERSION_KEY = "config-version"

    /**
     * 检查并更新配置文件
     * 流程：记录用户配置 → 备份 → 覆盖新配置 → 写入用户值
     * @param plugin 插件实例
     * @param configFile 配置文件
     * @return 是否进行了更新
     */
    fun checkAndUpdateConfig(plugin: JavaPlugin, configFile: File): Boolean {
        // 1. 记录用户当前的所有配置
        val userConfigValues = extractUserConfigValues(configFile)

        // 2. 检查配置版本
        val configVersion = userConfigValues[CONFIG_VERSION_KEY]?.toString()?.toIntOrNull() ?: 0

        // 如果配置已是最新版本，无需更新
        if (configVersion >= CURRENT_CONFIG_VERSION) {
            return false
        }

        plugin.logger.info(getMessage("config_update.detected_old_version", configVersion.toString(), CURRENT_CONFIG_VERSION.toString()))

        // 3. 备份旧配置文件
        backupConfig(plugin, configFile, configVersion)

        // 4. 从插件内部复制新配置文件覆盖用户的配置
        if (!copyDefaultConfig(plugin, configFile)) {
            return false
        }

        // 5. 将用户自定义的值写入到新配置文件
        writeUserValues(plugin, configFile, userConfigValues)

        plugin.logger.info(getMessage("config_update.update_success", configVersion.toString(), CURRENT_CONFIG_VERSION.toString()))
        return true
    }

    /**
     * 提取用户配置中的所有键值对
     * @param configFile 配置文件
     * @return 用户配置的键值对映射
     */
    private fun extractUserConfigValues(configFile: File): Map<String, Any?> {
        val config = YamlConfiguration.loadConfiguration(configFile)
        val userValues = mutableMapOf<String, Any?>()

        config.getKeys(true).forEach { key ->
            if (config.isString(key) || config.isBoolean(key) || config.isInt(key) || config.isDouble(key) || config.isList(key)) {
                userValues[key] = config.get(key)
            }
        }

        return userValues
    }

    /**
     * 从插件内部复制默认配置文件
     * @param plugin 插件实例
     * @param configFile 目标配置文件
     * @return 是否成功
     */
    private fun copyDefaultConfig(plugin: JavaPlugin, configFile: File): Boolean {
        try {
            // 删除旧配置文件
            if (configFile.exists()) {
                Files.delete(configFile.toPath())
            }

            // 从插件内部复制新配置文件
            plugin.saveResource("config.yml", false)
            return true
        } catch (e: IOException) {
            plugin.logger.severe(getMessage("config_update.save_failed", e.message ?: "unknown error"))
            return false
        }
    }

    /**
     * 将用户自定义的值写入到新配置文件
     * @param plugin 插件实例
     * @param configFile 配置文件
     * @param userValues 用户配置值
     */
    private fun writeUserValues(plugin: JavaPlugin, configFile: File, userValues: Map<String, Any?>) {
        try {
            val config = YamlConfiguration.loadConfiguration(configFile)

            var preservedCount = 0
            userValues.forEach { (key, value) ->
                // 跳过版本号
                if (key == CONFIG_VERSION_KEY) {
                    return@forEach
                }

                // 只写入新配置文件中存在的键（保留用户的自定义值）
                if (config.contains(key)) {
                    config.set(key, value)
                    preservedCount++
                }
            }

            config.save(configFile)
            plugin.logger.info("Preserved $preservedCount user custom values")
        } catch (e: IOException) {
            plugin.logger.warning(getMessage("config_update.save_failed", e.message ?: "unknown error"))
        }
    }

    /**
     * 备份配置文件
     * @param plugin 插件实例
     * @param configFile 配置文件
     * @param configVersion 旧配置版本号
     */
    private fun backupConfig(plugin: JavaPlugin, configFile: File, configVersion: Int) {
        val backupFile = File(configFile.parent, getBackupFileName(configFile, configVersion))

        try {
            Files.copy(
                configFile.toPath(),
                backupFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
            plugin.logger.info(getMessage("config_update.backup_success", backupFile.name))
        } catch (e: IOException) {
            plugin.logger.warning(getMessage("config_update.backup_failed", e.message ?: "unknown error"))
        }
    }

    /**
     * 获取备份文件名
     * @param configFile 配置文件
     * @param configVersion 配置版本号
     * @return 备份文件名
     */
    private fun getBackupFileName(configFile: File, configVersion: Int): String {
        val timestamp = System.currentTimeMillis()
        val baseName = configFile.nameWithoutExtension
        return "${baseName}_v${configVersion}_backup_${timestamp}.${configFile.extension}"
    }

    /**
     * 获取当前配置版本
     * @param config 配置文件
     * @return 配置版本号
     */
    fun getConfigVersion(config: FileConfiguration): Int {
        return config.getInt(CONFIG_VERSION_KEY, 0)
    }

    /**
     * 设置配置版本
     * @param config 配置文件
     * @param version 版本号
     */
    fun setConfigVersion(config: FileConfiguration, version: Int) {
        config.set(CONFIG_VERSION_KEY, version)
    }
}
