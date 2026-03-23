@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import io.papermc.paper.registry.data.dialog.action.DialogAction
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.SoundCategory
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.time.Duration

/**
 * 菜单动作处理器
 * 负责解析和执行菜单中的各种动作
 */
object MenuActions {
    private val serializer = LegacyComponentSerializer.legacyAmpersand()
    private var languageManager: LanguageManager? = null
    private var databaseManager: DatabaseManager? = null
    private var plugin: KaMenu? = null

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
     * 设置插件引用
     */
    fun setPlugin(kamenu: KaMenu) {
        plugin = kamenu
    }

    /**
     * 将颜色代码转换为 Adventure Component
     */
    internal fun color(text: String?): Component =
        if (text == null) Component.empty() else serializer.deserialize(text)

    /**
     * 解析变量（内置变量 + PAPI）
     * @param player 玩家对象
     * @param text 原始文本
     * @return 解析后的文本
     */
    private fun resolveVariables(player: Player, text: String): String {
        var result = text

        // 1. 解析内置变量 {data:key} 和 {gdata:key}
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

        // 2. 解析 PAPI 变量
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

            executeActionList(player, actionList.map { it ?: Any() }, variables, menuOpener)
        }, ClickCallback.Options.builder().lifetime(Duration.ofMinutes(5)).build())
    }

    /**
     * 执行动作列表（支持简单动作、列表动作和条件判断动作）
     */
    private fun executeActionList(
        player: Player,
        actionList: List<Any>,
        variables: Map<String, String>,
        menuOpener: (Player, String) -> Unit,
        baseDelay: Long = 0L
    ) {
        val actionsToExecute = mutableListOf<DeferredAction>()
        var currentDelay = baseDelay

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
                    executeActionList(player, actionsToUse.map { it ?: Any() }, variables, menuOpener, currentDelay)
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
            executeDeferredAction(player, deferred, menuOpener)
        }
    }

    /**
     * 执行延迟动作
     */
    private fun executeDeferredAction(
        player: Player,
        deferred: DeferredAction,
        menuOpener: (Player, String) -> Unit
    ) {
        if (deferred.delay > 0) {
            Bukkit.getScheduler().runTaskLater(plugin ?: return, Runnable {
                executeSingleAction(player, deferred.action, deferred.variables, menuOpener)
            }, deferred.delay)
        } else {
            executeSingleAction(player, deferred.action, deferred.variables, menuOpener)
        }
    }

    /**
     * 执行单个动作
     */
    private fun executeSingleAction(
        player: Player,
        action: String,
        variables: Map<String, String>,
        menuOpener: (Player, String) -> Unit
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
                // Paper API 会自动处理
                player.closeInventory()
            }

            // set-data: 设置玩家数据
            finalCmd.startsWith("set-data:") -> {
                val args = finalCmd.removePrefix("set-data:").trim().split(" ", limit = 2)
                if (args.size >= 2) {
                    val key = args[0]
                    val value = args[1]
                    databaseManager?.setPlayerData(player.uniqueId, key, value)
                }
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
            val soundKey = org.bukkit.NamespacedKey.minecraft(soundName.lowercase())
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
    private fun parseClickableText(rawText: String): Component {
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
    private fun createAdventureClickableText(
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
    private fun createAdventureText(text: String): Component {
        return color(text)
    }
}
