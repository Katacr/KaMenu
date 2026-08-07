package org.katacr.kamenu

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

/**
 * 管理插件根目录的 `custom_commands.yml`，并负责从旧版 config.yml 迁移自定义指令。
 *
 * 新文件保留原有的 `custom-commands` 根节，迁移在 ConfigUpdater 运行前执行，
 * 避免旧配置在主配置升级时被丢弃。
 */
object CustomCommandFileManager {
    private const val RESOURCE_PATH = "custom_commands.yml"
    private const val FILE_NAME = "custom_commands.yml"

    /** 旧配置迁移结果，用于启动时向控制台报告迁移状态。 */
    data class PreparationResult(
        val legacyFound: Boolean = false,
        val migratedCount: Int = 0,
        val backupName: String? = null,
        val error: String? = null
    )

    /** 返回插件根目录下的自定义指令文件。 */
    fun file(plugin: JavaPlugin): File = File(plugin.dataFolder, FILE_NAME)

    /** 确保独立配置文件存在，并在必要时迁移旧 config.yml 中的自定义指令。 */
    fun prepare(plugin: JavaPlugin, legacyConfigFile: File): PreparationResult {
        val targetFile = file(plugin)
        val legacyConfig = YamlConfiguration.loadConfiguration(legacyConfigFile)
        val legacySection = legacyConfig.getConfigurationSection("custom-commands")

        return try {
            if (legacySection == null) {
                ensureDefaultFile(plugin, targetFile)
                return PreparationResult()
            }

            val targetConfig = if (targetFile.exists()) {
                YamlConfiguration.loadConfiguration(targetFile)
            } else {
                YamlConfiguration()
            }
            val backupName = saveLegacyBackup(plugin, legacySection)
            var migratedCount = 0

            val targetSection = targetConfig.getConfigurationSection("custom-commands")
                ?: targetConfig.createSection("custom-commands")
            legacySection.getKeys(false).forEach { commandName ->
                if (!targetSection.contains(commandName)) {
                    copyValue(legacySection, targetSection, commandName, overwrite = true)
                    migratedCount++
                }
            }

            targetConfig.save(targetFile)
            legacyConfig.set("custom-commands", null)
            legacyConfig.save(legacyConfigFile)

            PreparationResult(
                legacyFound = true,
                migratedCount = migratedCount,
                backupName = backupName
            )
        } catch (error: Exception) {
            PreparationResult(
                legacyFound = legacySection != null,
                error = error.message ?: error.javaClass.simpleName
            )
        }
    }

    /** 读取独立自定义指令配置；文件缺失时释放插件内置示例文件。 */
    fun load(plugin: JavaPlugin): YamlConfiguration {
        val targetFile = file(plugin)
        ensureDefaultFile(plugin, targetFile)
        return YamlConfiguration.loadConfiguration(targetFile)
    }

    /** 将自定义指令配置保存回 custom_commands.yml。 */
    fun save(plugin: JavaPlugin, configuration: YamlConfiguration) {
        configuration.save(file(plugin))
    }

    /** 释放默认文件，调用方负责处理可能的文件系统异常。 */
    private fun ensureDefaultFile(plugin: JavaPlugin, targetFile: File) {
        if (!targetFile.exists()) {
            plugin.saveResource(RESOURCE_PATH, false)
        }
    }

    /** 备份旧配置中的自定义指令，确保迁移前后始终保留可恢复副本。 */
    private fun saveLegacyBackup(plugin: JavaPlugin, legacySection: ConfigurationSection): String {
        val backupFile = File(
            plugin.dataFolder,
            "custom_commands_legacy_backup_${System.currentTimeMillis()}.yml"
        )
        val backupConfig = YamlConfiguration()
        val backupSection = backupConfig.createSection("custom-commands")
        legacySection.getKeys(false).forEach { commandName ->
            copyValue(legacySection, backupSection, commandName, overwrite = true)
        }
        backupConfig.save(backupFile)
        return backupFile.name
    }

    /** 递归复制配置值，保留对象形式指令中的 actions 和 args 子节。 */
    private fun copyValue(
        source: ConfigurationSection,
        target: ConfigurationSection,
        key: String,
        overwrite: Boolean
    ) {
        if (!overwrite && target.contains(key)) {
            return
        }

        val sourceSection = source.getConfigurationSection(key)
        if (sourceSection == null) {
            target.set(key, source.get(key))
            return
        }

        val targetSection = target.getConfigurationSection(key)
            ?: target.createSection(key)
        sourceSection.getKeys(false).forEach { childKey ->
            copyValue(sourceSection, targetSection, childKey, overwrite)
        }
    }
}
