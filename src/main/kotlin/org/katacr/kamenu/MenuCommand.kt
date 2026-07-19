@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.InputStreamReader

/**
 * `/km` / `/kamenu` 主命令处理器。
 *
 * 负责管理员入口：打开菜单、重载指定模块、释放示例、打开向导、测试动作和切换语言。
 * reload 支持第三层目标：all/menu/actions/js/lang/config；不传目标时等同 all。
 */
class MenuCommand(private val plugin: KaMenu) : TabExecutor {

    /**
     * 可重载的运行时模块。
     *
     * CONFIG 包含 config.yml 及依赖它的自定义指令注册。
     */
    private enum class ReloadTarget(val id: String) {
        ALL("all"),
        MENU("menu"),
        ACTIONS("actions"),
        JS("js"),
        LANG("lang"),
        CONFIG("config");

        companion object {
            fun parse(raw: String?): ReloadTarget? {
                if (raw.isNullOrBlank()) {
                    return ALL
                }
                return entries.firstOrNull { it.id.equals(raw, ignoreCase = true) }
            }

            fun ids(): List<String> = entries.map { it.id }
        }
    }

    /**
     * 单个 reload 目标的执行结果。
     */
    private data class ReloadResult(
        val target: ReloadTarget,
        val total: Int,
        val success: Int,
        val failed: Int,
        val durationMs: Long
    )

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
            val target = ReloadTarget.parse(args.getOrNull(1))
            if (target == null) {
                sender.sendMessage(plugin.languageManager.getMessage("command.reload_unknown_target", args[1]))
                sender.sendMessage(plugin.languageManager.getMessage("command.reload_usage"))
                return true
            }
            reloadRuntime(target).forEach { result ->
                sender.sendMessage(reloadMessage(result))
            }
            return true
        }

        if (args[0].equals("guide", ignoreCase = true)) {
            if (!sender.hasPermission("kamenu.admin")) {
                sender.sendMessage(plugin.languageManager.getMessage("command.no_permission"))
                return true
            }
            if (sender !is Player) {
                sender.sendMessage(plugin.languageManager.getMessage("command.player_only"))
                return true
            }
            val guideConfig = loadInternalGuide()
            if (guideConfig == null) {
                sender.sendMessage(plugin.languageManager.getMessage("command.guide_missing", "internal/guide.yml"))
                return true
            }
            MenuUI.openConfig(sender, guideConfig, plugin, "internal:guide")
            return true
        }

        if (args[0].equals("language", ignoreCase = true) || args[0].equals("lang", ignoreCase = true)) {
            if (!sender.hasPermission("kamenu.admin")) {
                sender.sendMessage(plugin.languageManager.getMessage("command.no_permission"))
                return true
            }
            if (args.size < 2) {
                sender.sendMessage(plugin.languageManager.getMessage("command.language_usage"))
                return true
            }
            val language = args[1]
            if (!plugin.languageManager.isLanguageAvailable(language)) {
                sender.sendMessage(plugin.languageManager.getMessage("command.language_unknown", language))
                sender.sendMessage(plugin.languageManager.getMessage("command.language_available", plugin.languageManager.getAvailableLanguages().joinToString(", ")))
                sender.sendMessage(plugin.languageManager.getMessage("command.language_usage"))
                return true
            }

            plugin.config.set("language", language)
            plugin.saveConfig()
            val results = reloadRuntime(ReloadTarget.ALL)
            val menuCount = results.firstOrNull { it.target == ReloadTarget.MENU }?.success
                ?: plugin.menuManager.getAllMenuIds().size
            val commandCount = results.firstOrNull { it.target == ReloadTarget.CONFIG }?.success ?: 0
            sender.sendMessage(plugin.languageManager.getMessage("command.language_set_reload", language, menuCount.toString(), commandCount.toString()))
            return true
        }

        if (
            args[0].equals("examples", ignoreCase = true) ||
            args[0].equals("example", ignoreCase = true) ||
            args[0].equals("release-examples", ignoreCase = true)
        ) {
            if (!sender.hasPermission("kamenu.admin")) {
                sender.sendMessage(plugin.languageManager.getMessage("command.no_permission"))
                return true
            }
            val optionArgs = args.drop(1)
            val language = optionArgs.firstOrNull { it.equals("zh_CN", ignoreCase = true) || it.equals("en_US", ignoreCase = true) }
                ?: plugin.config.getString("language", "zh_CN")
                ?: "zh_CN"
            val overwrite = optionArgs.any {
                it.equals("overwrite", ignoreCase = true) || it.equals("true", ignoreCase = true)
            }
            val result = plugin.menuManager.releaseExampleMenus(language, overwrite)
            val menuCount = plugin.menuManager.reload()
            sender.sendMessage(
                plugin.languageManager.getMessage(
                    "command.examples_released",
                    language,
                    result.saved.toString(),
                    result.skipped.toString(),
                    result.failed.toString(),
                    menuCount.toString()
                )
            )
            return true
        }

        if (args[0].equals("pause", ignoreCase = true)) {
            if (!sender.hasPermission("kamenu.admin")) {
                sender.sendMessage(plugin.languageManager.getMessage("command.no_permission"))
                return true
            }
            if (args.size < 2) {
                sender.sendMessage(plugin.languageManager.getMessage("pause_entry.usage"))
                return true
            }

            when (args[1].lowercase()) {
                "register" -> {
                    if (args.size > 2) {
                        sender.sendMessage(plugin.languageManager.getMessage("pause_entry.register_usage"))
                        return true
                    }
                    val success = plugin.pauseEntryDatapackManager.register()
                    if (success) {
                        val info = plugin.pauseEntryDatapackManager.info()
                        sender.sendMessage(
                            plugin.languageManager.getMessage(
                                "pause_entry.registered_file",
                                info.sourceFile.absolutePath,
                                info.datapackFolder.absolutePath
                            )
                        )
                        sender.sendMessage(plugin.languageManager.getMessage("pause_entry.restart_required"))
                    } else {
                        sender.sendMessage(plugin.languageManager.getMessage("pause_entry.register_failed"))
                    }
                    return true
                }
                "unregister" -> {
                    val success = plugin.pauseEntryDatapackManager.unregister()
                    if (success) {
                        sender.sendMessage(plugin.languageManager.getMessage("pause_entry.unregistered"))
                        sender.sendMessage(plugin.languageManager.getMessage("pause_entry.restart_required"))
                    } else {
                        sender.sendMessage(plugin.languageManager.getMessage("pause_entry.unregister_failed"))
                    }
                    return true
                }
                "info" -> {
                    val info = plugin.pauseEntryDatapackManager.info()
                    sender.sendMessage(
                        plugin.languageManager.getMessage(
                            "pause_entry.info_source",
                            info.sourceFile.absolutePath,
                            info.sourceExists.toString()
                        )
                    )
                    sender.sendMessage(
                        plugin.languageManager.getMessage(
                            "pause_entry.info_datapack",
                            info.datapackFolder.absolutePath,
                            info.datapackExists.toString()
                        )
                    )
                    return true
                }
                else -> {
                    sender.sendMessage(plugin.languageManager.getMessage("pause_entry.usage"))
                    return true
                }
            }
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
                        val itemToGive = item.clone()
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
        sender.sendMessage(plugin.languageManager.getMessage("command.help_guide"))
        sender.sendMessage(plugin.languageManager.getMessage("command.help_language"))
        sender.sendMessage(plugin.languageManager.getMessage("command.help_examples"))
        sender.sendMessage(plugin.languageManager.getMessage("command.help_pause"))
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

    private fun reloadRuntime(target: ReloadTarget): List<ReloadResult> {
        return when (target) {
            ReloadTarget.ALL -> {
                MenuTaskManager.cancelAll()
                val configStart = System.nanoTime()
                plugin.reloadConfig()
                plugin.languageManager.reload()
                UpdateChecker.reload(plugin)
                val langDurationMs = elapsedMs(configStart)
                val configReloadDurationMs = langDurationMs
                val menuResult = reloadMenu(cancelTasks = false)
                val actionsResult = reloadActions()
                val jsResult = reloadJs()
                val commandStart = System.nanoTime()
                val commandResult = plugin.customCommandManager.registerCustomCommandsWithResult()
                plugin.customCommandManager.refreshOnlinePlayerCommands()
                val configResult = ReloadResult(
                    ReloadTarget.CONFIG,
                    commandResult.total,
                    commandResult.success,
                    commandResult.failed,
                    configReloadDurationMs + elapsedMs(commandStart)
                )
                val langResult = ReloadResult(ReloadTarget.LANG, 1, 1, 0, langDurationMs)
                listOf(configResult, menuResult, actionsResult, jsResult, langResult)
            }
            ReloadTarget.MENU -> listOf(reloadMenu(cancelTasks = true))
            ReloadTarget.ACTIONS -> listOf(reloadActions())
            ReloadTarget.JS -> listOf(reloadJs())
            ReloadTarget.LANG -> listOf(reloadLang())
            ReloadTarget.CONFIG -> listOf(reloadConfig())
        }
    }

    private fun reloadMessage(result: ReloadResult): String {
        return when (result.target) {
            ReloadTarget.ALL -> ""
            ReloadTarget.MENU -> plugin.languageManager.getMessage(
                "command.reload_menu_success",
                result.total.toString(),
                result.success.toString(),
                reloadFailedSegment(result.failed),
                result.durationMs.toString()
            )
            ReloadTarget.ACTIONS -> plugin.languageManager.getMessage(
                "command.reload_actions_success",
                result.total.toString(),
                result.success.toString(),
                reloadFailedSegment(result.failed),
                result.durationMs.toString()
            )
            ReloadTarget.JS -> plugin.languageManager.getMessage(
                "command.reload_js_success",
                result.total.toString(),
                result.success.toString(),
                reloadFailedSegment(result.failed),
                result.durationMs.toString()
            )
            ReloadTarget.LANG -> plugin.languageManager.getMessage(
                "command.reload_lang_success",
                result.total.toString(),
                result.success.toString(),
                reloadFailedSegment(result.failed),
                result.durationMs.toString(),
                plugin.languageManager.getCurrentLanguage()
            )
            ReloadTarget.CONFIG -> plugin.languageManager.getMessage(
                "command.reload_config_success",
                result.total.toString(),
                result.success.toString(),
                reloadFailedSegment(result.failed),
                result.durationMs.toString()
            )
        }
    }

    private fun reloadFailedSegment(failed: Int): String {
        val key = if (failed > 0) {
            "command.reload_failed_segment_warning"
        } else {
            "command.reload_failed_segment_normal"
        }
        return plugin.languageManager.getMessage(key, failed.toString())
    }

    private fun reloadMenu(cancelTasks: Boolean): ReloadResult {
        val start = System.nanoTime()
        if (cancelTasks) {
            MenuTaskManager.cancelAll()
        }
        val result = plugin.menuManager.reloadWithResult()
        return ReloadResult(ReloadTarget.MENU, result.total, result.success, result.failed, elapsedMs(start))
    }

    private fun reloadActions(): ReloadResult {
        val start = System.nanoTime()
        val result = plugin.actionPackageManager.reloadWithResult()
        return ReloadResult(ReloadTarget.ACTIONS, result.total, result.success, result.failed, elapsedMs(start))
    }

    private fun reloadJs(): ReloadResult {
        val start = System.nanoTime()
        val result = plugin.javaScriptPackageManager.reloadWithResult()
        return ReloadResult(ReloadTarget.JS, result.total, result.success, result.failed, elapsedMs(start))
    }

    private fun reloadLang(): ReloadResult {
        val start = System.nanoTime()
        plugin.languageManager.reload()
        return ReloadResult(ReloadTarget.LANG, 1, 1, 0, elapsedMs(start))
    }

    private fun reloadConfig(): ReloadResult {
        val start = System.nanoTime()
        plugin.reloadConfig()
        plugin.languageManager.reload()
        val result = plugin.customCommandManager.registerCustomCommandsWithResult()
        plugin.customCommandManager.refreshOnlinePlayerCommands()
        UpdateChecker.reload(plugin)
        return ReloadResult(ReloadTarget.CONFIG, result.total, result.success, result.failed, elapsedMs(start))
    }

    private fun elapsedMs(startNanos: Long): Long {
        return (System.nanoTime() - startNanos) / 1_000_000
    }

    private fun loadInternalGuide(): YamlConfiguration? {
        val inputStream = plugin.getResource("internal/guide.yml") ?: return null
        return try {
            inputStream.use { stream ->
                InputStreamReader(stream, Charsets.UTF_8).use { reader ->
                    YamlConfiguration.loadConfiguration(reader)
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning(plugin.languageManager.getMessage("command.guide_load_failed", e.message ?: "Unknown error"))
            null
        }
    }

    /**
     * 实现 Tab 补全功能
     */
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        // 获取当前正在输入的关键字
        val keyword = args.lastOrNull() ?: ""
        
        if (args.size == 1) return filterByKeyword(
            listOf("help", "open", "guide", "language", "examples", "pause", "reload", "action", "list", "item"),
            keyword
        )
        if (args.size == 2 && args[0].equals("open", ignoreCase = true)) {
            // 这里动态获取所有已加载的菜单 ID，按输入关键字模糊匹配
            return filterByKeyword(plugin.menuManager.getAllMenuIds(), keyword)
        }
        if (args.size == 2 && (args[0].equals("language", ignoreCase = true) || args[0].equals("lang", ignoreCase = true))) {
            return filterByKeyword(plugin.languageManager.getAvailableLanguages(), keyword)
        }
        if (args.size == 2 && args[0].equals("reload", ignoreCase = true)) {
            return filterByKeyword(ReloadTarget.ids(), keyword)
        }
        if (args.size == 2 && args[0].equals("pause", ignoreCase = true)) {
            return filterByKeyword(listOf("register", "unregister", "info"), keyword)
        }
        if (args.size == 3 && args[0].equals("pause", ignoreCase = true) && args[1].equals("register", ignoreCase = true)) {
            return emptyList()
        }
        if (args.size == 2 && (
            args[0].equals("examples", ignoreCase = true) ||
                args[0].equals("example", ignoreCase = true) ||
                args[0].equals("release-examples", ignoreCase = true)
            )
        ) {
            return filterByKeyword(listOf("zh_CN", "en_US", "overwrite"), keyword)
        }
        if (args.size == 3 && (
            args[0].equals("examples", ignoreCase = true) ||
                args[0].equals("example", ignoreCase = true) ||
                args[0].equals("release-examples", ignoreCase = true)
            )
        ) {
            return filterByKeyword(listOf("overwrite"), keyword)
        }
        if (args.size == 2 && args[0].equals("action", ignoreCase = true)) {
            // 返回在线玩家列表，按输入关键字模糊匹配
            return filterByKeyword(Bukkit.getOnlinePlayers().map { it.name }, keyword)
        }
        if (args.size == 3 && args[0].equals("action", ignoreCase = true)) {
            // 返回常用动作前缀，按输入关键字模糊匹配
            return filterByKeyword(listOf(
                "tell:", "actionbar:", "title:", "hovertext:",
                "command:", "chat:", "console:", "sound:",
                "open:", "force-open:", "close", "force-close", "reset",
                "server:", "tppos:",
                "data:", "gdata:", "list:", "glist:", "meta:",
                "set-data:", "set-gdata:", "set-meta:",
                "toast:", "money:", "stock-item:", "item:",
                "js:", "actions:", "page:",
                "run-task:", "stop-task:", "stop-current-task",
                "wait:", "return"
            ), keyword)
        }
        if (args.size == 2 && args[0].equals("item", ignoreCase = true)) {
            // 返回物品子指令，按输入关键字模糊匹配
            return filterByKeyword(listOf("save", "give", "delete"), keyword)
        }
        if (args.size == 3 && args[0].equals("item", ignoreCase = true) && args[1].equals("give", ignoreCase = true)) {
            // 返回所有保存的物品名称，按输入关键字模糊匹配
            return filterByKeyword(plugin.itemManager.getAllItemNames(), keyword)
        }
        if (args.size == 3 && args[0].equals("item", ignoreCase = true) && args[1].equals("delete", ignoreCase = true)) {
            // 返回所有保存的物品名称，按输入关键字模糊匹配
            return filterByKeyword(plugin.itemManager.getAllItemNames(), keyword)
        }
        if (args.size == 4 && args[0].equals("item", ignoreCase = true) && args[1].equals("give", ignoreCase = true)) {
            // 返回在线玩家列表，按输入关键字模糊匹配
            return filterByKeyword(Bukkit.getOnlinePlayers().map { it.name }, keyword)
        }
        return emptyList()
    }

    /**
     * 按输入关键字模糊匹配补全列表（包含该关键字即可）
     * @param list 原始补全列表
     * @param keyword 用户已输入的关键字
     * @return 过滤后的补全列表
     */
    private fun filterByKeyword(list: List<String>, keyword: String): List<String> {
        if (keyword.isEmpty()) return list
        return list.filter { it.contains(keyword, ignoreCase = true) }
    }
}
