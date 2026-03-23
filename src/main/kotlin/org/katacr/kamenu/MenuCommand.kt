@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class MenuCommand(private val plugin: KaMenu) : TabExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (args.isEmpty()) {
            sender.sendMessage(plugin.languageManager.getMessage("command.usage"))
            return true
        }

        if (args[0].equals("reload", ignoreCase = true)) {
            if (!sender.hasPermission("kamenu.admin")) {
                sender.sendMessage(plugin.languageManager.getMessage("command.no_permission"))
                return true
            }
            plugin.reloadConfig()
            plugin.languageManager.reload()
            val menuCount = plugin.menuManager.reload()

            sender.sendMessage(plugin.languageManager.getMessage("menu.reloaded", menuCount.toString()))
            return true
        }

        if (sender !is Player) {
            sender.sendMessage(plugin.languageManager.getMessage("command.player_only"))
            return true
        }

        if (args[0].equals("open", ignoreCase = true)) {
            if (args.size < 2) {
                sender.sendMessage(plugin.languageManager.getMessage("command.no_menu_name"))
                return true
            }
            MenuUI.openMenu(sender, args[1], plugin.menuManager, plugin)
        }

        return true
    }

    /**
     * 实现 Tab 补全功能
     */
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (args.size == 1) return listOf("open", "reload")
        if (args.size == 2 && args[0].equals("open", ignoreCase = true)) {
            // 这里动态获取所有已加载的菜单 ID
            return plugin.menuManager.getAllMenuIds()
        }
        return emptyList()
    }
}