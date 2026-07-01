@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

sealed interface CustomCommandDefinition {
    data class OpenMenu(val menuId: String) : CustomCommandDefinition
    data class RunActions(val actions: List<Any>) : CustomCommandDefinition
}

/**
 * 自定义指令处理器
 * 用于处理 config.yml 中配置的自定义指令，可直接打开菜单或执行动作队列。
 */
class CustomCommand(
    private val plugin: KaMenu,
    private val definition: CustomCommandDefinition,
    commandName: String
) : Command(commandName) {

    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        // 自定义指令仅限玩家使用
        if (sender !is Player) {
            sender.sendMessage(plugin.languageManager.getMessage("command.player_only"))
            return true
        }

        when (val commandDefinition = definition) {
            is CustomCommandDefinition.OpenMenu -> {
                MenuUI.openMenu(sender, commandDefinition.menuId, plugin.menuManager, plugin)
            }
            is CustomCommandDefinition.RunActions -> {
                MenuActions.executeStandaloneActions(
                    sender,
                    commandDefinition.actions,
                    buildCommandVariables(commandLabel, args)
                ).whenComplete { _, error ->
                    if (error != null) {
                        plugin.logger.severe("Custom command /$commandLabel action execution failed: ${error.message}")
                        error.printStackTrace()
                    }
                }
            }
        }
        return true
    }

    private fun buildCommandVariables(commandLabel: String, args: Array<out String>): Map<String, String> {
        val variables = mutableMapOf<String, String>()
        variables["command"] = commandLabel
        variables["args"] = args.joinToString(" ")
        variables["arg_count"] = args.size.toString()
        args.forEachIndexed { index, value ->
            variables["arg:$index"] = value
        }
        return variables
    }

    override fun tabComplete(
        sender: CommandSender,
        alias: String,
        args: Array<out String>
    ): List<String> {
        // 自定义指令不需要 tab 补全
        return emptyList()
    }
}
