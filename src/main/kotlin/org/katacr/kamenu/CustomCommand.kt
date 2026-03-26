@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * 自定义指令处理器
 * 用于处理 config.yml 中配置的自定义指令，直接打开指定的菜单
 */
class CustomCommand(
    private val plugin: KaMenu,
    private val menuId: String,
    commandName: String
) : Command(commandName) {

    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        // 自定义指令仅限玩家使用
        if (sender !is Player) {
            sender.sendMessage(plugin.languageManager.getMessage("command.player_only"))
            return true
        }

        // 打开配置的菜单
        MenuUI.openMenu(sender, menuId, plugin.menuManager, plugin)
        return true
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
