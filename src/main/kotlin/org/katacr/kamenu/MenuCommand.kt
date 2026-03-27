@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class MenuCommand(private val plugin: KaMenu) : TabExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (args.isEmpty() || args[0].equals("help", ignoreCase = true)) {
            showHelp(sender)
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
            val commandCount = plugin.customCommandManager.registerCustomCommands()

            sender.sendMessage(plugin.languageManager.getMessage("menu.reloaded", menuCount.toString(), commandCount.toString()))
            return true
        }

        if (args[0].equals("action", ignoreCase = true)) {
            if (!sender.hasPermission("kamenu.admin")) {
                sender.sendMessage(plugin.languageManager.getMessage("command.no_permission"))
                return true
            }
            if (args.size < 3) {
                sender.sendMessage(plugin.languageManager.getMessage("actions.test_usage"))
                sender.sendMessage(plugin.languageManager.getMessage("actions.test_example"))
                return true
            }
            val targetPlayer = Bukkit.getPlayer(args[1])
            if (targetPlayer == null) {
                sender.sendMessage(plugin.languageManager.getMessage("actions.test_player_offline", args[1]))
                return true
            }
            // 将 args[2] 及之后的所有参数组合成一个动作字符串
            val actionString = args.slice(2 until args.size).joinToString(" ")
            val success = MenuActions.executeTestAction(targetPlayer, actionString)
            if (!success) {
                sender.sendMessage(plugin.languageManager.getMessage("actions.test_failed", targetPlayer.name))
            }
            return true
        }

        if (args[0].equals("list", ignoreCase = true)) {
            if (!sender.hasPermission("kamenu.admin")) {
                sender.sendMessage(plugin.languageManager.getMessage("command.no_permission"))
                return true
            }
            if (sender !is Player) {
                sender.sendMessage(plugin.languageManager.getMessage("command.player_only"))
                return true
            }
            val page = if (args.size >= 2) args[1].toIntOrNull()?.coerceAtLeast(1) ?: 1 else 1
            showMenuList(sender, page)
            return true
        }

        if (args[0].equals("open", ignoreCase = true)) {
            if (!sender.hasPermission("kamenu.admin")) {
                sender.sendMessage(plugin.languageManager.getMessage("command.no_permission"))
                return true
            }
            if (args.size < 2) {
                sender.sendMessage(plugin.languageManager.getMessage("command.no_menu_name"))
                return true
            }

            // 处理目标玩家参数
            val menuId = args[1]
            val targetPlayer: Player?

            if (args.size >= 3) {
                // 指定了玩家
                targetPlayer = Bukkit.getPlayer(args[2])
                if (targetPlayer == null) {
                    sender.sendMessage(plugin.languageManager.getMessage("command.player_not_found", args[2]))
                    return true
                }
            } else {
                // 没有指定玩家，只能由玩家自己打开
                if (sender !is Player) {
                    sender.sendMessage(plugin.languageManager.getMessage("command.player_required"))
                    return true
                }
                targetPlayer = sender
            }

            MenuUI.openMenu(targetPlayer, menuId, plugin.menuManager, plugin)
            return true
        }

        if (args[0].equals("item", ignoreCase = true)) {
            if (!sender.hasPermission("kamenu.admin")) {
                sender.sendMessage(plugin.languageManager.getMessage("command.no_permission"))
                return true
            }
            if (args.size < 2) {
                sender.sendMessage(plugin.languageManager.getMessage("item.item_usage"))
                return true
            }

            val itemAction = args[1]
            when (itemAction.lowercase()) {
                "save" -> {
                    // /km item save <物品名称>
                    if (args.size < 3) {
                        sender.sendMessage(plugin.languageManager.getMessage("item.item_save_usage"))
                        return true
                    }
                    if (sender !is Player) {
                        sender.sendMessage(plugin.languageManager.getMessage("command.player_only"))
                        return true
                    }
                    val itemName = args[2]
                    val itemInHand = sender.inventory.itemInMainHand
                    if (itemInHand.type.isAir) {
                        sender.sendMessage(plugin.languageManager.getMessage("item.item_empty_hand"))
                        return true
                    }
                    val success = plugin.itemManager.saveItem(itemName, itemInHand.clone(), sender.uniqueId.toString())
                    if (success) {
                        sender.sendMessage(plugin.languageManager.getMessage("item.item_saved", itemName))
                    } else {
                        sender.sendMessage(plugin.languageManager.getMessage("item.item_save_failed"))
                    }
                }
                "give" -> {
                    // /km item give <物品> [玩家] [数量]
                    if (args.size < 3) {
                        sender.sendMessage(plugin.languageManager.getMessage("item.item_give_usage"))
                        return true
                    }
                    val itemName = args[2]
                    if (!plugin.itemManager.itemExists(itemName)) {
                        sender.sendMessage(plugin.languageManager.getMessage("item.item_not_exist", itemName))
                        return true
                    }

                    // 解析玩家和数量参数
                    val targetPlayer: Player?
                    var amount = 1

                    when {
                        args.size >= 5 -> {
                            // /km item give <物品> <玩家> <数量>
                            targetPlayer = Bukkit.getPlayer(args[3])
                            if (targetPlayer == null) {
                                sender.sendMessage(plugin.languageManager.getMessage("command.player_not_found", args[3]))
                                return true
                            }
                            amount = args[4].toIntOrNull() ?: 1
                        }
                        args.size == 4 -> {
                            // 判断args[3]是玩家还是数量
                            if (args[3].toIntOrNull() != null) {
                                // args[3]是数量，没有指定玩家
                                if (sender !is Player) {
                                    sender.sendMessage(plugin.languageManager.getMessage("command.player_required"))
                                    return true
                                }
                                targetPlayer = sender
                                amount = args[3].toIntOrNull() ?: 1
                            } else {
                                // args[3]是玩家，数量默认为1
                                targetPlayer = Bukkit.getPlayer(args[3])
                                if (targetPlayer == null) {
                                    sender.sendMessage(plugin.languageManager.getMessage("command.player_not_found", args[3]))
                                    return true
                                }
                            }
                        }
                        else -> {
                            // 没有指定玩家和数量
                            if (sender !is Player) {
                                sender.sendMessage(plugin.languageManager.getMessage("command.player_required"))
                                return true
                            }
                            targetPlayer = sender
                        }
                    }

                    val item = plugin.itemManager.getItem(itemName)
                    if (item != null) {
                        // 克隆物品并设置数量
                        val itemToGive = item.clone() as org.bukkit.inventory.ItemStack
                        itemToGive.amount = amount.coerceAtLeast(1)
                        val leftover = targetPlayer.inventory.addItem(itemToGive)
                        if (leftover.isEmpty()) {
                            sender.sendMessage(plugin.languageManager.getMessage("item.item_given", itemName, targetPlayer.name, amount.toString()))
                        } else {
                            // 物品栏已满，将剩余物品掉落在地上
                            var droppedAmount = 0
                            leftover.values.forEach { item ->
                                targetPlayer.world.dropItem(targetPlayer.location, item)
                                droppedAmount += item.amount
                            }

                            // 给予者发送消息
                            sender.sendMessage(plugin.languageManager.getMessage("item.item_inventory_full", itemName, targetPlayer.name, droppedAmount.toString()))

                            // 目标玩家收到 actionbar 提示和拾取音效
                            val actionbarMessage = plugin.languageManager.getMessage("actions.inventory_full_actionbar", droppedAmount.toString())
                            targetPlayer.sendActionBar(org.bukkit.ChatColor.translateAlternateColorCodes('&', actionbarMessage))
                            targetPlayer.playSound(targetPlayer.location, org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f)
                        }
                    } else {
                        sender.sendMessage(plugin.languageManager.getMessage("item.item_get_failed", itemName))
                    }
                }
                "delete" -> {
                    // /km item delete <物品名称>
                    if (args.size < 3) {
                        sender.sendMessage(plugin.languageManager.getMessage("item.item_delete_usage"))
                        return true
                    }
                    val itemName = args[2]
                    if (!plugin.itemManager.itemExists(itemName)) {
                        sender.sendMessage(plugin.languageManager.getMessage("item.item_not_exist", itemName))
                        return true
                    }

                    val deleted = plugin.itemManager.deleteItem(itemName)
                    if (deleted) {
                        sender.sendMessage(plugin.languageManager.getMessage("item.item_deleted", itemName))
                    } else {
                        sender.sendMessage(plugin.languageManager.getMessage("item.item_delete_failed", itemName))
                    }
                }
                else -> {
                    sender.sendMessage(plugin.languageManager.getMessage("item.item_unknown_action", itemAction))
                    sender.sendMessage(plugin.languageManager.getMessage("item.item_usage"))
                }
            }
            return true
        }

        return true
    }

    /**
     * 显示帮助信息
     */
    private fun showHelp(sender: CommandSender) {
        val version = plugin.description.version

        // 空行分隔
        sender.sendMessage("")

        // 标题
        sender.sendMessage(plugin.languageManager.getMessage("command.help_title"))

        // 版本信息
        sender.sendMessage(plugin.languageManager.getMessage("command.help_description", version))

        // 空行
        sender.sendMessage("")

        // 指令列表
        sender.sendMessage(plugin.languageManager.getMessage("command.help_open"))
        sender.sendMessage(plugin.languageManager.getMessage("command.help_list"))
        sender.sendMessage(plugin.languageManager.getMessage("command.help_reload"))
        sender.sendMessage(plugin.languageManager.getMessage("command.help_action"))
        sender.sendMessage(plugin.languageManager.getMessage("command.help_item"))
        sender.sendMessage(plugin.languageManager.getMessage("command.help_help"))

        // 空行
        sender.sendMessage("")

        // 页脚 - 可点击的文档链接
        val docUrl = if (plugin.languageManager.getCurrentLanguage() == "zh_CN") {
            "https://katacr.gitbook.io/plugins/kamenu-cn"
        } else {
            "https://katacr.gitbook.io/plugins/kamenu-en"
        }
        val footerText = plugin.languageManager.getMessage("command.help_footer")
        val clickText = plugin.languageManager.getMessage("command.help_footer_click_text")
        val hoverText = plugin.languageManager.getMessage("command.help_footer_hover")
        val clickableFooter = "$footerText <text='$clickText';hover='$hoverText';url='$docUrl'>"
        val parsedFooter = MenuActions.parseClickableText(clickableFooter)
        sender.sendMessage(parsedFooter)

        // 空行
        sender.sendMessage("")
    }

    /**
     * 显示菜单列表（分页）
     */
    private fun showMenuList(player: Player, page: Int) {
        val menus = plugin.menuManager.getAllMenuIds()
        val pageSize = 10
        val totalPages = (menus.size + pageSize - 1) / pageSize.coerceAtLeast(1)

        if (menus.isEmpty()) {
            player.sendMessage(plugin.languageManager.getMessage("menu_list.no_menus"))
            return
        }

        val validPage = page.coerceIn(1, totalPages)
        val startIndex = (validPage - 1) * pageSize
        val endIndex = (startIndex + pageSize).coerceAtMost(menus.size)
        val currentMenus = menus.subList(startIndex, endIndex)

        // 空行分隔
        player.sendMessage("")

        // 标题
        player.sendMessage(plugin.languageManager.getMessage("menu_list.title"))

        // 菜单列表（带序号）
        currentMenus.forEachIndexed { index, menuId ->
            val number = startIndex + index + 1
            val menuItemText = plugin.languageManager.getMessage("menu_list.menu_item", menuId)
            val message = MenuActions.parseClickableText("§f$number. $menuItemText")
            player.sendMessage(message)
        }

        // 分页信息 + 翻页按钮（同一行）
        val pageInfo = plugin.languageManager.getMessage("menu_list.page_info", validPage.toString(), totalPages.toString())
        if (totalPages > 1) {
            val prevButton = if (validPage > 1) {
                plugin.languageManager.getMessage("menu_list.prev_page_click", (validPage - 1).toString())
            } else {
                "§7" + plugin.languageManager.getMessage("menu_list.prev_page")
            }
            val nextButton = if (validPage < totalPages) {
                plugin.languageManager.getMessage("menu_list.next_page_click", (validPage + 1).toString())
            } else {
                "§7" + plugin.languageManager.getMessage("menu_list.next_page")
            }
            val lineText = "$prevButton  $pageInfo  $nextButton"
            val lineMessage = MenuActions.parseClickableText(lineText)
            player.sendMessage(lineMessage)
        } else {
            player.sendMessage(pageInfo)
        }

        // 空行分隔
        player.sendMessage("")
    }

    /**
     * 实现 Tab 补全功能
     */
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (args.size == 1) return listOf("help", "open", "reload", "action", "list", "item")
        if (args.size == 2 && args[0].equals("open", ignoreCase = true)) {
            // 这里动态获取所有已加载的菜单 ID
            return plugin.menuManager.getAllMenuIds()
        }
        if (args.size == 2 && args[0].equals("action", ignoreCase = true)) {
            // 返回在线玩家列表
            return Bukkit.getOnlinePlayers().map { it.name }
        }
        if (args.size == 3 && args[0].equals("action", ignoreCase = true)) {
            // 返回常用动作前缀
            return listOf(
                "tell:", "actionbar:", "title:", "hovertext:",
                "command:", "console:", "sound:",
                "open:", "close",
                "data:", "gdata:", "meta:",
                "set-data:", "set-gdata:", "set-meta:",
                "toast:", "money:"
            )
        }
        if (args.size == 2 && args[0].equals("item", ignoreCase = true)) {
            // 返回物品子指令
            return listOf("save", "give", "delete")
        }
        if (args.size == 3 && args[0].equals("item", ignoreCase = true) && args[1].equals("give", ignoreCase = true)) {
            // 返回所有保存的物品名称
            return plugin.itemManager.getAllItemNames()
        }
        if (args.size == 3 && args[0].equals("item", ignoreCase = true) && args[1].equals("delete", ignoreCase = true)) {
            // 返回所有保存的物品名称
            return plugin.itemManager.getAllItemNames()
        }
        if (args.size == 4 && args[0].equals("item", ignoreCase = true) && args[1].equals("give", ignoreCase = true)) {
            // 返回在线玩家列表
            return Bukkit.getOnlinePlayers().map { it.name }
        }
        return emptyList()
    }
}