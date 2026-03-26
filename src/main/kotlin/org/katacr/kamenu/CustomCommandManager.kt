@file:Suppress("UnstableApiUsage", "UNCHECKED_CAST")

package org.katacr.kamenu

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandMap
import org.bukkit.command.SimpleCommandMap
import java.lang.reflect.Field

/**
 * 自定义指令管理器
 * 负责注册和取消注册 config.yml 中配置的自定义指令
 */
class CustomCommandManager(private val plugin: KaMenu) {

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
            plugin.logger.warning("Failed to initialize CustomCommandManager: ${e.message}")
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
        // 先清除所有已注册的自定义指令
        unregisterAllCustomCommands()

        val customCommandsSection = plugin.config.getConfigurationSection("custom-commands") ?: return 0

        // 获取所有自定义指令配置
        val commands = customCommandsSection.getKeys(false)
        if (commands.isEmpty()) {
            return 0
        }

        var successCount = 0

        commands.forEach { commandName ->
            val menuId = customCommandsSection.getString(commandName)
            if (menuId == null || menuId.isBlank()) {
                return@forEach
            }

            // 检查菜单是否存在
            if (plugin.menuManager.getMenuConfig(menuId) == null) {
                return@forEach
            }

            try {
                registerCommand(commandName, menuId)
                successCount++
            } catch (e: Exception) {
                // 忽略注册失败的指令
            }
        }

        return successCount
    }

    /**
     * 注册单个自定义指令
     */
    private fun registerCommand(commandName: String, menuId: String) {
        val commandMap = this.commandMap ?: throw RuntimeException("CommandMap not initialized")

        // 创建自定义指令
        val command = CustomCommand(plugin, menuId, commandName)

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

        registeredCommands.forEach { (commandName, _) ->
            // 移除指令
            knownCommands.remove(commandName)
            knownCommands.remove("kamenu:$commandName")
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
