@file:Suppress("UnstableApiUsage", "UNCHECKED_CAST")

package org.katacr.kamenu

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandMap
import org.bukkit.command.SimpleCommandMap
import org.bukkit.configuration.ConfigurationSection
import java.lang.reflect.Field

/**
 * 自定义指令管理器
 * 负责注册和取消注册 config.yml 中配置的自定义指令
 */
class CustomCommandManager(private val plugin: KaMenu) {

    data class RegistrationResult(
        val total: Int = 0,
        val success: Int = 0,
        val failed: Int = 0
    )

    private val registeredCommands = mutableMapOf<String, Command>()
    private var commandMap: CommandMap? = null
    private var knownCommandsField: Field? = null
    private var knownCommands: MutableMap<String, Command>? = null

    init {
        try {
            // 通过反射获取CommandMap
            commandMap = getCommandMap()
            knownCommandsField = SimpleCommandMap::class.java.getDeclaredField("knownCommands")
            knownCommandsField!!.isAccessible = true
            knownCommands = knownCommandsField!!.get(commandMap) as MutableMap<String, Command>
        } catch (e: Exception) {
            warn("custom_commands.manager_initialize_failed", e.message ?: e.javaClass.simpleName)
        }
    }

    /**
     * 通过反射获取服务器的CommandMap
     */
    private fun getCommandMap(): CommandMap {
        val server = Bukkit.getServer()
        val commandMapField = server.javaClass.getDeclaredField("commandMap")
        commandMapField.isAccessible = true
        return commandMapField.get(server) as CommandMap
    }

    /**
     * 从配置文件加载并注册自定义指令
     * @return 成功注册的指令数量
     */
    fun registerCustomCommands(): Int {
        return registerCustomCommandsWithResult().success
    }

    fun registerCustomCommandsWithResult(): RegistrationResult {
        // 先清除所有已注册的自定义指令
        unregisterAllCustomCommands()

        val customCommandsSection = plugin.config.getConfigurationSection("custom-commands") ?: return RegistrationResult()

        // 获取所有自定义指令配置
        val commands = customCommandsSection.getKeys(false)
        if (commands.isEmpty()) {
            return RegistrationResult()
        }

        var successCount = 0
        var failedCount = 0

        commands.forEach { commandName ->
            val definition = parseCommandDefinition(customCommandsSection, commandName) ?: run {
                warn("custom_commands.invalid_config", commandName)
                failedCount++
                return@forEach
            }

            try {
                registerCommand(commandName, definition)
                successCount++
            } catch (e: Exception) {
                warn("custom_commands.register_failed", commandName, e.message ?: e.javaClass.simpleName)
                failedCount++
            }
        }

        return RegistrationResult(total = commands.size, success = successCount, failed = failedCount)
    }

    private fun parseCommandDefinition(
        customCommandsSection: ConfigurationSection,
        commandName: String
    ): CustomCommandDefinition? {
        val rawValue = customCommandsSection.get(commandName)

        if (rawValue is String) {
            val menuId = rawValue.trim()
            if (menuId.isEmpty()) {
                return null
            }
            if (plugin.menuManager.getMenuConfig(menuId) == null) {
                warn("custom_commands.menu_not_found", commandName, menuId)
                return null
            }
            return CustomCommandDefinition.OpenMenu(menuId)
        }

        val commandSection = customCommandsSection.getConfigurationSection(commandName) ?: return null
        val argSuggestions = parseArgSuggestions(commandSection, commandName)

        val menuId = commandSection.getString("menu")?.trim()
        if (!menuId.isNullOrEmpty()) {
            if (plugin.menuManager.getMenuConfig(menuId) == null) {
                warn("custom_commands.menu_not_found", commandName, menuId)
                return null
            }
            return CustomCommandDefinition.OpenMenu(menuId, argSuggestions)
        }

        val actions = commandSection.getList("actions") ?: return null
        if (actions.isEmpty()) {
            return null
        }

        return CustomCommandDefinition.RunActions(actions.map { it ?: Any() }, argSuggestions)
    }

    private fun parseArgSuggestions(
        commandSection: ConfigurationSection,
        commandName: String
    ): Map<Int, Any> {
        val argsSection = commandSection.getConfigurationSection("args") ?: return emptyMap()
        val suggestions = linkedMapOf<Int, Any>()

        argsSection.getKeys(false).forEach { key ->
            val index = key.toIntOrNull()
            if (index == null || index < 0) {
                warn("custom_commands.invalid_args_key", commandName, key)
                return@forEach
            }

            val value = argsSection.get(key)
            if (value != null) {
                suggestions[index] = value
            }
        }

        return suggestions
    }

    private fun warn(key: String, vararg args: String) {
        plugin.logger.warning(plugin.languageManager.getMessage(key, *args))
    }

    /**
     * 注册单个自定义指令
     */
    private fun registerCommand(commandName: String, definition: CustomCommandDefinition) {
        val commandMap = this.commandMap ?: throw RuntimeException("CommandMap not initialized")

        // 创建自定义指令
        val command = CustomCommand(plugin, definition, commandName)

        // 注册指令
        commandMap.register("kamenu", command)

        // 保存注册的指令引用以便后续取消注册
        registeredCommands[commandName.lowercase()] = command
    }

    /**
     * 取消注册所有自定义指令
     */
    fun unregisterAllCustomCommands() {
        if (registeredCommands.isEmpty()) {
            return
        }

        val knownCommands = this.knownCommands ?: return
        val commandMap = this.commandMap

        registeredCommands.forEach { (_, command) ->
            val keysToRemove = knownCommands
                .filterValues { it === command }
                .keys
                .toList()

            keysToRemove.forEach { key ->
                knownCommands.remove(key, command)
            }

            if (commandMap != null) {
                command.unregister(commandMap)
            }
        }

        registeredCommands.clear()
    }

    /**
     * 清空管理器
     */
    fun clear() {
        unregisterAllCustomCommands()
    }

    /**
     * 获取已注册的指令数量
     */
    fun getRegisteredCommandCount(): Int {
        return registeredCommands.size
    }
}
