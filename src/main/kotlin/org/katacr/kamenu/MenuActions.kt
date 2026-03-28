@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import io.papermc.paper.registry.data.dialog.action.DialogAction
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.title.Title
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.SoundCategory
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.time.Duration
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer

/**
 * 菜单动作处理器
 * 负责解析和执行菜单中的各种动作
 */
object MenuActions {
    private val serializer = LegacyComponentSerializer.legacyAmpersand()
    private var languageManager: LanguageManager? = null
    private var databaseManager: DatabaseManager? = null
    private var metaDataManager: MetaDataManager? = null
    private var economy: Economy? = null
    private var plugin: KaMenu? = null
    private var itemManager: ItemManager? = null

    /**
     * 延迟动作数据类
     */
    private data class DeferredAction(
        val delay: Long,
        val action: String,
        val variables: Map<String, String>
    )

    /**
     * 设置语言管理器引用
     */
    fun setLanguageManager(manager: LanguageManager) {
        languageManager = manager
    }

    /**
     * 设置数据库管理器引用
     */
    fun setDatabaseManager(manager: DatabaseManager) {
        databaseManager = manager
    }

    /**
     * 设置元数据管理器引用
     */
    fun setMetaDataManager(manager: MetaDataManager) {
        metaDataManager = manager
    }

    /**
     * 设置经济系统引用
     */
    fun setEconomy(econ: Economy?) {
        economy = econ
    }

    /**
     * 设置插件引用
     */
    fun setPlugin(kamenu: KaMenu) {
        plugin = kamenu
    }

    /**
     * 设置物品管理器引用
     */
    fun setItemManager(manager: ItemManager) {
        itemManager = manager
    }

    /**
     * 将颜色代码转换为 Adventure Component
     */
    internal fun color(text: String?): Component =
        if (text == null) Component.empty() else serializer.deserialize(text)

    /**
     * 解析变量（完整顺序：$(var) -> {data:var} -> %papi_var%）
     * @param player 玩家对象
     * @param text 原始文本
     * @param variables 输入变量映射（$(var)）
     * @return 解析后的文本
     */
    fun resolveVariablesWithInput(player: Player, text: String, variables: Map<String, String> = emptyMap()): String {
        var result = text

        // 1. 解析输入变量 $(key)
        variables.forEach { (key, value) ->
            result = result.replace("$($key)", value)
        }

        // 2. 解析内置变量 {data:key}、{gdata:key}、{meta:key}
        result = result.replace(Regex("\\{data:([^}]+)}")) { matchResult ->
            val key = matchResult.groupValues[1]
            databaseManager?.getPlayerData(player.uniqueId, key)
                ?: languageManager?.getMessage("papi.data_not_found", key) ?: "null"
        }
        result = result.replace(Regex("\\{gdata:([^}]+)}")) { matchResult ->
            val key = matchResult.groupValues[1]
            databaseManager?.getGlobalData(key)
                ?: languageManager?.getMessage("papi.data_not_found", key) ?: "null"
        }
        result = result.replace(Regex("\\{meta:([^}]+)}")) { matchResult ->
            val key = matchResult.groupValues[1]
            metaDataManager?.getPlayerMeta(player.uniqueId, key) ?: "null"
        }

        // 3. 解析 PAPI 变量
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, result)
            } catch (_: Exception) {
                // PAPI 解析失败，忽略
            }
        }

        return result
    }

    /**
     * 解析变量（内置变量 + PAPI）
     * @param player 玩家对象
     * @param text 原始文本
     * @return 解析后的文本
     */
    private fun resolveVariables(player: Player, text: String): String {
        return resolveVariablesWithInput(player, text, emptyMap())
    }

    /**
     * 从配置文件中构建一个 DialogAction 对象
     */
    fun buildActionFromConfig(
        player: Player,
        config: YamlConfiguration,
        path: String,
        inputKeys: List<String>,
        inputTypes: Map<String, String>,
        checkboxMappings: Map<String, Pair<String, String>>,
        menuOpener: (Player, String) -> Unit
    ): DialogAction {
        // 只使用 actions（复数）键
        val actionList = config.getList(path)
        if (actionList == null || actionList.isEmpty()) {
            return DialogAction.staticAction(ClickEvent.runCommand("/empty"))
        }

        // 1. 优先处理不需要服务器参与的静态动作 (url, copy)
        // 仅当只有一个动作且是特定字符串时才执行
        if (actionList.size == 1) {
            val firstAction = actionList[0]
            if (firstAction is String) {
                when {
                    firstAction.startsWith("url:") ->
                        return DialogAction.staticAction(ClickEvent.openUrl(firstAction.removePrefix("url:").trim()))
                    firstAction.startsWith("copy:") ->
                        return DialogAction.staticAction(ClickEvent.copyToClipboard(firstAction.removePrefix("copy:").trim()))
                }
            }
            // 如果是Map且包含condition键，走下面的customClick处理路径
            if (firstAction is Map<*, *>) {
                if (firstAction.containsKey("condition")) {
                    // 条件格式动作，继续执行下面的复杂逻辑
                } else {
                    return DialogAction.staticAction(ClickEvent.runCommand("/empty"))
                }
            } else if (firstAction !is String) {
                // 其他非String、非condition的Map类型，返回empty
                return DialogAction.staticAction(ClickEvent.runCommand("/empty"))
            }
        }

        // 2. 统一处理所有需要服务器参与的复杂逻辑
        // (多行指令、变量、声音、条件判断等)
        return DialogAction.customClick({ response, _ ->
            val variables = mutableMapOf<String, String>()
            inputKeys.forEach { key ->
                val value = when {
                    response.getFloat(key) != null -> {
                        val f = response.getFloat(key)!!
                        if (f == f.toInt().toFloat()) {
                            val intVal = f.toInt()
                            // 检查是否为 checkbox，且用户未配置自定义映射
                            val mapping = checkboxMappings[key]
                            if (inputTypes[key] == "checkbox" && mapping != null && mapping.first == "true" && mapping.second == "false") {
                                // 默认映射，转换为布尔字符串
                                (intVal == 1).toString()
                            } else {
                                // 用户有自定义映射或非 checkbox，保持原值
                                intVal.toString()
                            }
                        } else {
                            f.toString()
                        }
                    }
                    response.getText(key) != null -> response.getText(key)
                    response.getBoolean(key) != null -> response.getBoolean(key).toString()
                    else -> ""
                }
                variables[key] = value ?: ""
            }

            // 执行动作列表（支持条件判断）

            executeActionList(player, actionList.map { it ?: Any() }, variables, menuOpener, 0L, config)
        }, ClickCallback.Options.builder().lifetime(Duration.ofMinutes(5)).build())
    }

    /**
     * 执行动作列表（支持简单动作、列表动作和条件判断动作）
     * @return 是否应该中断后续动作的执行（true表示中断）
     */
    private fun executeActionList(
        player: Player,
        actionList: List<Any>,
        variables: Map<String, String>,
        menuOpener: (Player, String) -> Unit,
        baseDelay: Long = 0L,
        config: YamlConfiguration? = null
    ): Boolean {
        val actionsToExecute = mutableListOf<DeferredAction>()
        var currentDelay = baseDelay
        var shouldReturn = false

        for (action in actionList) {
            when (action) {
                is Map<*, *> -> {
                    // 条件判断动作 - 使用 ConditionUtils 处理
                    val group = action
                    var conditionStr = group["condition"] as? String ?: ""

                    // 支持 'actions' 和 'allow' 两个键名
                    val successActions = (group["actions"] ?: group["allow"]) as? List<*> ?: emptyList<Any>()
                    val denyActions = (group["deny"] as? List<*>) ?: emptyList<Any>()

                    // 先替换条件中的变量 $(key)
                    variables.forEach { (key, value) ->
                        conditionStr = conditionStr.replace("$($key)", value)
                    }

                    val actionsToUse = if (ConditionUtils.checkCondition(player, conditionStr)) {
                        successActions
                    } else {
                        denyActions
                    }

                    // 递归执行子动作列表（支持嵌套的条件判断），传入当前延迟作为基准
                    val subResult = executeActionList(player, actionsToUse.map { it ?: Any() }, variables, menuOpener, currentDelay, config)
                    if (subResult) {
                        shouldReturn = true
                        break
                    }
                }
                is List<*> -> {
                    // 普通动作列表 - 遍历并执行每个动作
                    action.forEach { subAction ->
                        val actionStr = subAction?.toString() ?: return@forEach
                        var finalAction = actionStr
                        variables.forEach { (key, value) ->
                            finalAction = finalAction.replace("$($key)", value)
                        }

                        // 检查是否是 wait 动作
                        if (finalAction.startsWith("wait:", ignoreCase = true)) {
                            val waitTime = finalAction.substring(5).trim().toLongOrNull() ?: 0L
                            currentDelay += waitTime
                        } else if (finalAction.trim() == "return") {
                            // return 动作，中断后续动作
                            shouldReturn = true
                            return@forEach
                        } else {
                            actionsToExecute.add(DeferredAction(currentDelay, finalAction, variables))
                        }
                    }
                }
                is String -> {
                    var finalAction: String = action
                    variables.forEach { (key, value) ->
                        finalAction = finalAction.replace("$($key)", value)
                    }

                    // 检查是否是 wait 动作
                    if (finalAction.startsWith("wait:", ignoreCase = true)) {
                        val waitTime = finalAction.substring(5).trim().toLongOrNull() ?: 0L
                        currentDelay += waitTime
                    } else if (finalAction.trim() == "return") {
                        // return 动作，中断后续动作
                        shouldReturn = true
                        break
                    } else {
                        actionsToExecute.add(DeferredAction(currentDelay, finalAction, variables))
                    }
                }
                else -> {
                    // 忽略其他类型的值（数字、布尔值等）
                }
            }
        }

        // 按延迟时间执行所有动作
        actionsToExecute.forEach { deferred ->
            executeDeferredAction(player, deferred, menuOpener, config)
        }

        return shouldReturn
    }

    /**
     * 执行事件动作（如 Open、Close 等）
     * 事件动作不支持 $(input) 变量，因为菜单还未打开
     * @param player 玩家对象
     * @param config 菜单配置
     * @param eventName 事件名称（如 "Open"、"Close" 等）
     * @return 是否应该中断后续操作（true表示中断，例如Open事件中遇到return）
     */
    fun executeEvent(player: Player, config: YamlConfiguration, eventName: String): Boolean {
        val eventPath = "Events.$eventName"
        val eventActions = config.getList(eventPath) ?: return false

        // 定义菜单打开器（事件中可能需要打开其他菜单）
        val menuOpener: (Player, String) -> Unit = { p, menuName ->
            val kaMenu = Bukkit.getPluginManager().getPlugin("KaMenu") as? KaMenu
            if (kaMenu != null) {
                Bukkit.getScheduler().runTask(kaMenu, Runnable {
                    MenuUI.openMenu(p, menuName, kaMenu.menuManager, kaMenu)
                })
            }
        }

        // 执行事件动作（没有输入变量，也不支持 $(input) 变量）
        return executeActionList(player, eventActions.map { it ?: Any() }, emptyMap(), menuOpener, 0L, config)
    }

    /**
     * 执行延迟动作
     */
    private fun executeDeferredAction(
        player: Player,
        deferred: DeferredAction,
        menuOpener: (Player, String) -> Unit,
        config: YamlConfiguration? = null
    ) {
        if (deferred.delay > 0) {
            Bukkit.getScheduler().runTaskLater(plugin ?: return, Runnable {
                executeSingleAction(player, deferred.action, deferred.variables, menuOpener, config)
            }, deferred.delay)
        } else {
            executeSingleAction(player, deferred.action, deferred.variables, menuOpener, config)
        }
    }

    /**
     * 执行单个动作
     */
    private fun executeSingleAction(
        player: Player,
        action: String,
        variables: Map<String, String>,
        menuOpener: (Player, String) -> Unit,
        config: YamlConfiguration? = null
    ) {
        var finalCmd = action
        variables.forEach { (key, value) ->
            finalCmd = finalCmd.replace("$($key)", value)
        }

        // 解析内置变量 {data:var} 和 {gdata:var}，以及 PAPI 变量
        finalCmd = resolveVariables(player, finalCmd)

        when {
            // tell: 普通消息
            finalCmd.startsWith("tell:") ->
                player.sendMessage(color(finalCmd.removePrefix("tell:").trim()))

            // actionbar: ActionBar 消息
            finalCmd.startsWith("actionbar:") -> {
                val message = finalCmd.removePrefix("actionbar:").trim()
                player.sendActionBar(color(message))
            }

            // title: 发送标题
            finalCmd.startsWith("title:") -> {
                val args = finalCmd.removePrefix("title:").trim()
                parseAndSendTitle(player, args)
            }

            // hovertext: 可点击文本
            finalCmd.startsWith("hovertext:") -> {
                val text = finalCmd.removePrefix("hovertext:").trim()
                val message = parseClickableText(text)
                player.sendMessage(message)
            }

            // command: 玩家执行指令
            finalCmd.startsWith("command:") ->
                player.performCommand(finalCmd.removePrefix("command:").trim())

            // console: 控制台执行指令
            finalCmd.startsWith("console:") ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd.removePrefix("console:").trim())

            // sound: 播放声音 (支持音量和音调参数)
            finalCmd.startsWith("sound:") -> {
                val args = finalCmd.removePrefix("sound:").trim()
                parseAndPlaySound(player, args)
            }

            // open: 打开另一个对话框
            finalCmd.startsWith("open:") -> {
                val menuName = finalCmd.removePrefix("open:").trim()
                menuOpener(player, menuName)
            }

            // close: 关闭对话框
            finalCmd.startsWith("close") -> {
                // 先执行 Events.Close 事件
                if (config != null) {
                    val shouldInterrupt = executeEvent(player, config, "Close")
                    if (shouldInterrupt) {
                        // Close 事件中遇到 return，不关闭菜单
                        return
                    }
                }
                // Close 事件中没有 return，继续关闭菜单
                player.closeInventory()
            }

            // set-data: 设置玩家数据
            finalCmd.startsWith("set-data:") -> {
                val args = finalCmd.removePrefix("set-data:").trim()
                parseDataAction(args, player.uniqueId.toString(), "data") { uuid, key, value ->
                    databaseManager?.setPlayerData(java.util.UUID.fromString(uuid), key, value)
                }
            }

            // data: 玩家数据操作
            finalCmd.startsWith("data:") -> {
                val args = finalCmd.removePrefix("data:").trim()
                parseAndExecuteDataAction(
                    args = args,
                    player = player,
                    dataType = "data",
                    setAction = { key, value ->
                        databaseManager?.setPlayerData(player.uniqueId, key, value)
                    },
                    modifyAction = { key, delta ->
                        databaseManager?.modifyPlayerData(player.uniqueId, key, delta)
                    },
                    deleteAction = { key ->
                        databaseManager?.deletePlayerData(player.uniqueId, key)
                    }
                )
            }

            // set-gdata: 设置全局数据
            finalCmd.startsWith("set-gdata:") -> {
                val args = finalCmd.removePrefix("set-gdata:").trim()
                parseDataAction(args, "", "gdata") { _, key, value ->
                    databaseManager?.setGlobalData(key, value)
                }
            }

            // gdata: 全局数据操作
            finalCmd.startsWith("gdata:") -> {
                val args = finalCmd.removePrefix("gdata:").trim()
                parseAndExecuteDataAction(
                    args = args,
                    player = player,
                    dataType = "gdata",
                    setAction = { key, value ->
                        databaseManager?.setGlobalData(key, value)
                    },
                    modifyAction = { key, delta ->
                        databaseManager?.modifyGlobalData(key, delta)
                    },
                    deleteAction = { key ->
                        databaseManager?.deleteGlobalData(key)
                    }
                )
            }

            // set-meta: 设置玩家元数据（内存缓存）
            finalCmd.startsWith("set-meta:") -> {
                val args = finalCmd.removePrefix("set-meta:").trim()
                parseDataAction(args, player.uniqueId.toString(), "meta") { uuid, key, value ->
                    metaDataManager?.setPlayerMeta(java.util.UUID.fromString(uuid), key, value)
                }
            }

            // meta: 玩家元数据操作
            finalCmd.startsWith("meta:") -> {
                val args = finalCmd.removePrefix("meta:").trim()
                parseAndExecuteDataAction(
                    args = args,
                    player = player,
                    dataType = "meta",
                    setAction = { key, value ->
                        metaDataManager?.setPlayerMeta(player.uniqueId, key, value)
                    },
                    modifyAction = { key, delta ->
                        val uuid = player.uniqueId
                        val currentValue = metaDataManager?.getPlayerMeta(uuid, key)
                        if (currentValue != null) {
                            val currentNum = currentValue.toDoubleOrNull()
                            if (currentNum != null) {
                                val numDelta = delta.toDoubleOrNull()
                                if (numDelta != null) {
                                    val newValue = (currentNum + numDelta).toString()
                                    metaDataManager?.setPlayerMeta(uuid, key, newValue)
                                } else {
                                    plugin?.logger?.warning("meta 操作失败: 变化量 '$delta' 不是数字，无法执行 add/take 操作")
                                }
                            } else {
                                plugin?.logger?.warning("meta 操作失败: 键 '$key' 的当前值 '$currentValue' 不是数字，无法执行 add/take 操作")
                            }
                        }
                    },
                    deleteAction = { key ->
                        metaDataManager?.removePlayerMeta(player.uniqueId, key)
                    }
                )
            }

            // set-gdata: 设置全局数据
            finalCmd.startsWith("set-gdata:") -> {
                val args = finalCmd.removePrefix("set-gdata:").trim().split(" ", limit = 2)
                if (args.size >= 2) {
                    val key = args[0]
                    val value = args[1]
                    databaseManager?.setGlobalData(key, value)
                }
            }

            // set-meta: 设置玩家元数据（内存缓存）
            finalCmd.startsWith("set-meta:") -> {
                val args = finalCmd.removePrefix("set-meta:").trim().split(" ", limit = 2)
                if (args.size >= 2) {
                    val key = args[0]
                    val value = args[1]
                    metaDataManager?.setPlayerMeta(player.uniqueId, key, value)
                }
            }

            // toast: 显示 Toast 通知
            finalCmd.startsWith("toast:") -> {
                val args = finalCmd.removePrefix("toast:").trim()
                parseAndSendToast(player, args)
            }

            // money: 操作玩家金币
            finalCmd.startsWith("money:") -> {
                val args = finalCmd.removePrefix("money:").trim()
                parseAndHandleMoney(player, args, variables)
            }

            // stock-item: 物品给予/扣除
            finalCmd.startsWith("stock-item:") -> {
                val args = finalCmd.removePrefix("stock-item:").trim()
                parseAndHandleStockItem(player, args, variables)
            }

            // item: 普通物品给予/扣除
            finalCmd.startsWith("item:") -> {
                val args = finalCmd.removePrefix("item:").trim()
                parseAndHandleItem(player, args, variables)
            }
        }
    }

    /**
     * 执行测试动作（用于 /kamenu action 指令）
     * @param player 玩家对象
     * @param actionString 动作字符串
     * @return 是否成功执行
     */
    fun executeTestAction(player: Player, actionString: String): Boolean {
        if (plugin == null) {
            player.sendMessage(color(languageManager?.getMessage("actions.test_failed", "插件未初始化") ?: "§c插件未初始化，无法执行动作"))
            return false
        }

        try {
            val menuOpener: (Player, String) -> Unit = { p, menuName ->
                Bukkit.getScheduler().runTask(plugin!!, Runnable {
                    MenuUI.openMenu(p, menuName, plugin!!.menuManager, plugin!!)
                })
            }

            executeSingleAction(player, actionString, emptyMap(), menuOpener, null)
            return true
        } catch (e: Exception) {
            player.sendMessage(color(e.message?.let { languageManager?.getMessage("actions.test_failed", it) } ?: "§c动作执行失败: ${e.message}"))
            plugin?.logger?.severe("测试动作执行失败: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * 解析并发送声音
     * 格式: sound_name;volume=1.0;pitch=1.0;category=master
     */
    private fun parseAndPlaySound(player: Player, args: String) {
        var soundName = ""
        var volume = 1f
        var pitch = 1f
        var category = SoundCategory.MASTER

        val params = args.split(";")
        for (param in params) {
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim().lowercase()
                val value = parts[1].trim()
                when (key) {
                    "volume" -> volume = value.toFloatOrNull() ?: 1f
                    "pitch" -> pitch = value.toFloatOrNull() ?: 1f
                    "category" -> category = value.lowercase().let { cat ->
                        when (cat) {
                            "master" -> SoundCategory.MASTER
                            "music" -> SoundCategory.MUSIC
                            "record" -> SoundCategory.RECORDS
                            "weather" -> SoundCategory.WEATHER
                            "block" -> SoundCategory.BLOCKS
                            "hostile" -> SoundCategory.HOSTILE
                            "neutral" -> SoundCategory.NEUTRAL
                            "player" -> SoundCategory.PLAYERS
                            "ambient" -> SoundCategory.AMBIENT
                            "voice" -> SoundCategory.VOICE
                            "ui" -> SoundCategory.UI
                            else -> {
                                player.sendMessage(languageManager?.getMessage("actions.unknown_sound_category", cat) ?: "§c未知的声音类别: $cat")
                                SoundCategory.MASTER
                            }
                        }
                    }
                    else -> soundName = soundName.ifEmpty { parts[0].trim() }
                }
            } else if (parts.size == 1 && param.trim().isNotEmpty()) {
                if (soundName.isEmpty()) soundName = param.trim()
            }
        }

        if (soundName.isNotEmpty()) {
            val soundKey = NamespacedKey.minecraft(soundName.lowercase())
            val sound = org.bukkit.Registry.SOUND_EVENT.get(soundKey)
            if (sound != null) {
                player.playSound(player.location, sound, category, volume, pitch)
            }
        }
    }

    /**
     * 解析并发送标题
     * 格式: title=主标题;subtitle=副标题;in=淡入时长;keep=停留时长;out=淡出时长
     */
    private fun parseAndSendTitle(player: Player, args: String) {
        var title = ""
        var subtitle = ""
        var fadeIn = 0
        var stay = 60
        var fadeOut = 20

        val params = args.split(";")
        for (param in params) {
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim().lowercase()
                val value = parts[1].trim()
                when (key) {
                    "title" -> title = value
                    "subtitle" -> subtitle = value
                    "in" -> fadeIn = value.toIntOrNull() ?: 0
                    "keep" -> stay = value.toIntOrNull() ?: 60
                    "out" -> fadeOut = value.toIntOrNull() ?: 20
                }
            }
        }

        val titleComponent = if (title.isEmpty()) Component.empty() else color(title)
        val subtitleComponent = if (subtitle.isEmpty()) Component.empty() else color(subtitle)

        // 使用 Adventure API 的 Title (Paper 推荐方式)
        val titleTimes = Title.Times.times(
            Duration.ofMillis(fadeIn * 50L),   // ticks to milliseconds
            Duration.ofMillis(stay * 50L),     // ticks to milliseconds
            Duration.ofMillis(fadeOut * 50L)   // ticks to milliseconds
        )
        val adventureTitle = Title.title(titleComponent, subtitleComponent, titleTimes)
        player.showTitle(adventureTitle)
    }

    /**
     * 解析可点击文本 (使用 Adventure API)
     * 格式: <text='显示文字';hover='悬停文字';command='指令';url='链接';newline='false'>
     */
    fun parseClickableText(rawText: String): Component {
        val mainBuilder = Component.text()
        var currentPos = 0

        while (currentPos < rawText.length) {
            val startIndex = rawText.indexOf('<', currentPos)

            if (startIndex == -1) {
                mainBuilder.append(createAdventureText(rawText.substring(currentPos)))
                break
            }

            if (startIndex > currentPos) {
                mainBuilder.append(createAdventureText(rawText.substring(currentPos, startIndex)))
            }

            val endIndex = findClosingBracket(rawText, startIndex)
            if (endIndex == -1) {
                mainBuilder.append(createAdventureText(rawText.substring(startIndex)))
                break
            }

            val content = rawText.substring(startIndex + 1, endIndex)
            val component = parseClickableComponent(content)
            if (component != null) {
                mainBuilder.append(component)
            }

            currentPos = endIndex + 1
        }

        return mainBuilder.build()
    }

    /**
     * 查找闭合的 <> 符号
     */
    private fun findClosingBracket(text: String, startIndex: Int): Int {
        var depth = 0
        var i = startIndex

        while (i < text.length) {
            when (text[i]) {
                '<' -> depth++
                '>' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
            i++
        }

        return -1
    }

    /**
     * 解析可点击组件内容 (使用 Adventure API)
     */
    private fun parseClickableComponent(content: String): Component? {
        var text = ""
        var hover = ""
        var command = ""
        var url = ""
        var newline = false

        val parts = content.split(';')
        for (part in parts) {
            val trimmed = part.trim()
            val eqIndex = trimmed.indexOf('=')

            if (eqIndex != -1) {
                val key = trimmed.take(eqIndex).trim().lowercase()
                val value = trimmed.substring(eqIndex + 1).trim()

                when (key) {
                    "text" -> text = value.removeSurrounding("`").removeSurrounding("'").removeSurrounding("\"")
                    "hover" -> hover = value.removeSurrounding("`").removeSurrounding("'").removeSurrounding("\"")
                    "command" -> command = value.removeSurrounding("`").removeSurrounding("'").removeSurrounding("\"")
                    "url" -> url = value.removeSurrounding("`").removeSurrounding("'").removeSurrounding("\"")
                    "newline" -> newline = value.removeSurrounding("`").removeSurrounding("'").removeSurrounding("\"").equals("true", ignoreCase = true)
                }
            }
        }

        return if (text.isNotEmpty()) {
            createAdventureClickableText(text, hover, command, url, newline)
        } else {
            null
        }
    }

    /**
     * 创建可点击文本组件 (使用 Adventure API)
     */
    fun createAdventureClickableText(
        text: String,
        hoverText: String = "",
        command: String = "",
        url: String = "",
        newline: Boolean = false
    ): Component {
        var component = color(text)

        // 添加点击事件
        if (url.isNotEmpty()) {
            component = component.clickEvent(ClickEvent.openUrl(url))
        } else if (command.isNotEmpty()) {
            component = component.clickEvent(ClickEvent.runCommand(command))
        }

        // 添加悬停事件
        if (hoverText.isNotEmpty()) {
            component = component.hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(color(hoverText)))
        }

        // 添加换行
        if (newline) {
            component = component.append(Component.newline())
        }

        return component
    }

    /**
     * 创建普通文本组件 (使用 Adventure API)
     */
    fun createAdventureText(text: String): Component {
        return color(text)
    }

    /**
     * 解析并发送 Toast 通知
     * 格式: frame=类型;icon=物品ID;title=标题;description=描述
     * 参数:
     * - frame: task(默认), goal, challenge
     * - icon: 物品ID (如 diamond_sword)
     * - title: 标题文本
     * - description: 描述文本
     */
    private fun parseAndSendToast(player: Player, args: String) {
        var frameType = "task"
        var iconItem = "minecraft:stone"
        var title = "提示"
        var description = ""

        // 解析参数
        args.split(";").forEach { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim().lowercase()
                val value = parts[1].trim()
                when (key) {
                    "type" -> frameType = value.lowercase()
                    "icon" -> iconItem = if (value.contains(":")) value else "minecraft:$value"
                    "msg" -> title = value
                    "description" -> description = value
                }
            }
        }

        // 生成唯一 Key
        val randomKey = NamespacedKey("kamenu", "toast_${System.currentTimeMillis()}")

        val titleJson = GsonComponentSerializer.gson().serialize(color(title))
        val descJson = GsonComponentSerializer.gson().serialize(color(description))

        val advancementJson = """
    {
      "display": {
        "icon": {
          "id": "${iconItem.lowercase()}"
        },
        "title": $titleJson,
        "description": $descJson,
        "frame": "${if (frameType in listOf("task", "goal", "challenge")) frameType else "task"}",
        "show_toast": true,
        "announce_to_chat": false,
        "hidden": true
      },
      "criteria": {
        "impossible": {
          "trigger": "minecraft:impossible"
        }
      }
    }
    """.trimIndent()

        try {
            val advancement = Bukkit.getUnsafe().loadAdvancement(randomKey, advancementJson)
            val progress = player.getAdvancementProgress(advancement)
            progress.awardCriteria("impossible")

            plugin?.let {
                Bukkit.getScheduler().runTaskLater(it, Runnable {
                    if (player.isOnline) {
                        progress.revokeCriteria("impossible")
                        Bukkit.getUnsafe().removeAdvancement(randomKey)
                    }
                }, 10L)
            }
        } catch (e: Exception) {
            plugin?.logger?.severe("Toast 发送失败: ${e.message}")
            // 打印出生成的 JSON 方便调试
            plugin?.logger?.info("生成的 JSON: $advancementJson")
        }
    }

    /**
     * 解析并处理金币操作
     * 格式: type=add;num=100 | type=take;num=100 | type=reset;num=100
     */
    private fun parseAndHandleMoney(player: Player, args: String, variables: Map<String, String> = emptyMap()) {
        if (economy == null) {
            plugin?.logger?.warning("经济系统未启用，无法执行 money 动作。玩家: ${player.name}")
            return
        }

        var type = ""
        var amountStr = "0"

        val params = args.split(";")
        for (param in params) {
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim().lowercase()
                val value = parts[1].trim()
                when (key) {
                    "type" -> type = value.lowercase()
                    "num" -> amountStr = value
                }
            }
        }

        // 使用通用的变量解析方法
        val finalAmountStr = resolveVariablesWithInput(player, amountStr, variables)
        val amount = finalAmountStr.toDoubleOrNull() ?: 0.0

        val balance = economy!!.getBalance(player)

        when (type) {
            "add" -> {
                economy!!.depositPlayer(player, amount)
            }
            "take" -> {
                if (balance >= amount) {
                    economy!!.withdrawPlayer(player, amount)
                } else {
                    plugin?.logger?.warning("玩家 ${player.name} 余额不足，无法扣除 ${amount} 金币。当前余额: $balance")
                }
            }
            "reset" -> {
                val difference = amount - balance
                if (difference > 0) {
                    economy!!.depositPlayer(player, difference)
                } else if (difference < 0) {
                    economy!!.withdrawPlayer(player, -difference)
                }
            }
            else -> {
                plugin?.logger?.warning("无效的金币操作类型: $type。玩家: ${player.name}")
            }
        }
    }

    /**
     * 解析旧的 set-data/set-gdata/set-meta 格式（向后兼容）
     * @param args 参数字符串
     * @param uuid 玩家 UUID（对于 gdata 不需要）
     * @param dataType 数据类型
     * @param action 执行动作的回调
     */
    private fun parseDataAction(
        args: String,
        uuid: String,
        dataType: String,
        action: (String, String, String) -> Unit
    ) {
        val parts = args.split(" ", limit = 2)
        if (parts.size >= 2) {
            val key = parts[0]
            val value = parts[1]
            action(uuid, key, value)
        }
    }

    /**
     * 解析并执行新的 data/gdata/meta 动作
     * @param args 参数字符串（格式: type=set;key=test;var=value 或 type=add;key=num;var=10 或 type=delete;key=num）
     * @param player 玩家对象
     * @param dataType 数据类型（data/gdata/meta）
     * @param setAction set 动作的回调
     * @param modifyAction modify（add/take）动作的回调（参数为 String，方法内部会转换为 Double）
     * @param deleteAction delete 动作的回调
     */
    private fun parseAndExecuteDataAction(
        args: String,
        player: Player,
        dataType: String,
        setAction: (String, String) -> Unit,
        modifyAction: (String, String) -> Unit,
        deleteAction: (String) -> Unit = {}
    ) {
        var type = ""
        var key = ""
        var value = ""

        // 解析参数
        args.split(";").forEach { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                val paramKey = parts[0].trim().lowercase()
                val paramValue = parts[1].trim().removeSurrounding("`").removeSurrounding("'").removeSurrounding("\"")
                when (paramKey) {
                    "type" -> type = paramValue.lowercase()
                    "key" -> key = paramValue
                    "var" -> value = paramValue
                }
            }
        }

        if (key.isEmpty()) {
            plugin?.logger?.warning("$dataType 操作失败: 缺少 key 参数。玩家: ${player.name}")
            return
        }

        when (type) {
            "set" -> {
                // set 动作：直接设置值
                setAction(key, value)
            }
            "add" -> {
                // add 动作：增加数值
                val numValue = value.toDoubleOrNull()
                if (numValue != null) {
                    modifyAction(key, numValue.toString())
                } else {
                    plugin?.logger?.warning("$dataType 操作失败: add/take 操作的值 '$value' 不是纯数字。玩家: ${player.name}")
                }
            }
            "take" -> {
                // take 动作：减少数值
                val numValue = value.toDoubleOrNull()
                if (numValue != null) {
                    modifyAction(key, (-numValue).toString())
                } else {
                    plugin?.logger?.warning("$dataType 操作失败: add/take 操作的值 '$value' 不是纯数字。玩家: ${player.name}")
                }
            }
            "delete" -> {
                // delete 动作：删除数据
                deleteAction(key)
            }
            else -> {
                plugin?.logger?.warning("$dataType 操作失败: 无效的 type 参数 '$type'，支持的类型: set, add, take, delete。玩家: ${player.name}")
            }
        }
    }

    /**
     * 解析并处理物品给予/扣除动作
     * 格式: type=give;name=物品名称;amount=数量 或 type=take;name=物品名称;amount=数量
     */
    private fun parseAndHandleStockItem(player: Player, args: String, variables: Map<String, String> = emptyMap()) {
        if (itemManager == null) {
            languageManager?.getMessage("actions.stock_item_manager_not_init", player.name)?.let {
                plugin?.logger?.warning(it)
            }
            return
        }

        var type = ""
        var itemName = ""
        var amountStr = "1"

        // 解析参数
        args.split(";").forEach { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim().lowercase()
                val value = parts[1].trim()
                when (key) {
                    "type" -> type = value.lowercase()
                    "name" -> itemName = value
                    "amount" -> amountStr = value
                }
            }
        }

        // 解析变量替换
        val finalItemName = resolveVariablesWithInput(player, itemName, variables)
        val finalAmountStr = resolveVariablesWithInput(player, amountStr, variables)
        val amount = (finalAmountStr.toIntOrNull() ?: 1).coerceAtLeast(1)

        if (finalItemName.isEmpty()) {
            languageManager?.getMessage("actions.stock_item_missing_name", player.name)?.let {
                plugin?.logger?.warning(it)
            }
            return
        }

        // 从数据库获取物品
        val item = itemManager!!.getItem(finalItemName)
        if (item == null) {
            languageManager?.getMessage("condition.stock_item_not_exist", finalItemName)?.let {
                player.sendMessage(color(it))
            }
            return
        }

        when (type) {
            "give" -> {
                // 给予物品
                val itemToGive = item.clone()
                itemToGive.amount = amount
                val leftover = player.inventory.addItem(itemToGive)

                if (leftover.isNotEmpty()) {
                    // 物品栏已满，将剩余物品掉落在地上
                    var droppedAmount = 0
                    leftover.values.forEach { item ->
                        player.world.dropItem(player.location, item)
                        droppedAmount += item.amount
                    }

                    // 发送 actionbar 提示和拾取音效
                    val actionbarMessage = languageManager?.getMessage("actions.inventory_full_actionbar", droppedAmount.toString())
                    if (actionbarMessage != null) {
                        player.sendActionBar(color(actionbarMessage))
                    }
                    player.playSound(player.location, org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f)
                }
            }
            "take" -> {
                // 扣除物品
                val inventory = player.inventory
                var remaining = amount

                // 遍历背包中的所有物品（包括盔甲、副手物品栏和背包）
                val allItems = mutableListOf<org.bukkit.inventory.ItemStack>()
                allItems.addAll(inventory.storageContents.filterNotNull())
                allItems.addAll(inventory.armorContents.filterNotNull())
                allItems.add(inventory.itemInOffHand)
                allItems.add(inventory.itemInMainHand)

                for (stack in allItems) {
                    if (remaining <= 0) break
                    if (!stack.isEmpty && stack.isSimilar(item)) {
                        val stackAmount = stack.amount
                        if (stackAmount <= remaining) {
                            // 整个堆叠都扣除
                            stack.amount = 0
                            remaining -= stackAmount
                        } else {
                            // 部分扣除
                            stack.amount -= remaining
                            remaining = 0
                        }
                    }
                }

                // 更新玩家背包
                player.updateInventory()
            }
            else -> {
                languageManager?.getMessage("actions.stock_item_unknown_type", type, player.name)?.let {
                    plugin?.logger?.warning(it)
                }
            }
        }
    }

    /**
     * 解析并处理普通物品给予/扣除动作
     * 格式: type=give;mats=材质;amount=数量 或 type=take;mats=材质;amount=数量;lore=描述;model=模型
     * lore和model是可选的，仅用于take操作
     */
    private fun parseAndHandleItem(player: Player, args: String, variables: Map<String, String> = emptyMap()) {
        var type = ""
        var materialName = ""
        var amountStr = "1"
        var loreText: String? = null
        var itemModel: String? = null

        // 解析参数
        args.split(";").forEach { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim().lowercase()
                val value = parts[1].trim()
                when (key) {
                    "type" -> type = value.lowercase()
                    "mats" -> materialName = value
                    "amount" -> amountStr = value
                    "lore" -> loreText = value
                    "model" -> itemModel = value
                }
            }
        }

        // 解析变量替换
        val finalMaterialName = resolveVariablesWithInput(player, materialName, variables)
        val finalAmountStr = resolveVariablesWithInput(player, amountStr, variables)
        val finalLoreText = loreText?.let { resolveVariablesWithInput(player, it, variables) }
        val finalItemModel = itemModel?.let { resolveVariablesWithInput(player, it, variables) }
        val amount = (finalAmountStr.toIntOrNull() ?: 1).coerceAtLeast(1)

        // 材质是必需的
        if (finalMaterialName.isEmpty()) {
            languageManager?.getMessage("actions.item_missing_material", player.name)?.let {
                plugin?.logger?.warning(it)
            }
            return
        }

        // 获取材质
        val material = org.bukkit.Material.matchMaterial(finalMaterialName)
        if (material == null) {
            languageManager?.getMessage("actions.item_invalid_material", finalMaterialName, player.name)?.let {
                plugin?.logger?.warning(it)
            }
            return
        }

        when (type) {
            "give" -> {
                // 给予物品（只需要材质和数量，忽略lore和model）
                val itemToGive = org.bukkit.inventory.ItemStack(material, amount)
                val leftover = player.inventory.addItem(itemToGive)

                if (leftover.isNotEmpty()) {
                    // 物品栏已满，将剩余物品掉落在地上
                    var droppedAmount = 0
                    leftover.values.forEach { item ->
                        player.world.dropItem(player.location, item)
                        droppedAmount += item.amount
                    }

                    // 发送 actionbar 提示和拾取音效
                    val actionbarMessage = languageManager?.getMessage("actions.inventory_full_actionbar", droppedAmount.toString())
                    if (actionbarMessage != null) {
                        player.sendActionBar(color(actionbarMessage))
                    }
                    player.playSound(player.location, org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f)
                }
            }
            "take" -> {
                // 扣除物品（支持lore和model判断）
                val inventory = player.inventory
                var remaining = amount

                // 遍历背包中的所有物品（包括盔甲、副手物品栏和背包）
                val allItems = mutableListOf<org.bukkit.inventory.ItemStack>()
                allItems.addAll(inventory.storageContents.filterNotNull())
                allItems.addAll(inventory.armorContents.filterNotNull())
                allItems.add(inventory.itemInOffHand)
                allItems.add(inventory.itemInMainHand)

                for (stack in allItems) {
                    if (remaining <= 0) break
                    if (!stack.isEmpty && stack.type == material) {
                        // 检查 lore 是否匹配（如果指定了 lore）
                        if (finalLoreText != null) {
                            val itemMeta = stack.itemMeta
                            if (itemMeta != null && itemMeta.hasLore()) {
                                val lore = itemMeta.lore()
                                // 检查 lore 中是否包含指定字符串（忽略大小写）
                                val loreMatched = lore?.any { line ->
                                    val plainText = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(line)
                                    plainText.contains(finalLoreText, ignoreCase = true)
                                } ?: false
                                if (!loreMatched) {
                                    continue
                                }
                            } else {
                                // 没有 lore，跳过
                                continue
                            }
                        }

                        // 检查 item_model 是否匹配（如果指定了 model）
                        if (finalItemModel != null) {
                            val itemMeta = stack.itemMeta
                            if (itemMeta != null && itemMeta.hasItemModel()) {
                                val modelKey = itemMeta.getItemModel()
                                if (modelKey != null) {
                                    // 格式化为 namespace:key
                                    val modelStr = "${modelKey.namespace()}:${modelKey.value()}"
                                    if (!modelStr.equals(finalItemModel, ignoreCase = true)) {
                                        continue
                                    }
                                } else {
                                    // 没有模型，跳过
                                    continue
                                }
                            } else {
                                // 没有模型，跳过
                                continue
                            }
                        }

                        // 符合所有条件，执行扣除
                        val stackAmount = stack.amount
                        if (stackAmount <= remaining) {
                            // 整个堆叠都扣除
                            stack.amount = 0
                            remaining -= stackAmount
                        } else {
                            // 部分扣除
                            stack.amount -= remaining
                            remaining = 0
                        }
                    }
                }

                // 更新玩家背包
                player.updateInventory()
            }
            else -> {
                languageManager?.getMessage("actions.item_unknown_type", type, player.name)?.let {
                    plugin?.logger?.warning(it)
                }
            }
        }
    }
}
