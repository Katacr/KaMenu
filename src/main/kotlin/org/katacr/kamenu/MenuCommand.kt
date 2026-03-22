@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class MenuCommand(private val plugin: KaMenu) : TabExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (sender !is Player) {
            sender.sendMessage("§c只有玩家可以执行此指令！")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§e用法: /km open <菜单名> 或 /km reload")
            return true
        }

        if (args[0].equals("reload", ignoreCase = true)) {
            if (!sender.hasPermission("kamenu.admin")) {
                sender.sendMessage("§c你没有权限执行此指令！")
                return true
            }
            plugin.reloadConfig()
            plugin.menuManager.reload()

            sender.sendMessage("§a[KaMenu] 配置与菜单已重载成功！")
            return true
        }

        if (args[0].equals("open", ignoreCase = true)) {
            if (args.size < 2) {
                sender.sendMessage("§c请输入菜单名！")
                return true
            }
            MenuUI.openMenu(sender, args[1], plugin.menuManager)
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