@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import io.papermc.paper.registry.data.dialog.action.DialogAction
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.title.Title
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.SoundCategory
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.time.Duration
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import java.util.concurrent.CompletableFuture

/**
 * 菜单动作处理器
 * 负责解析和执行菜单中的各种动作
 */
object MenuActions {
    private val serializer = LegacyComponentSerializer.legacyAmpersand()
    private val miniMessage = MiniMessage.miniMessage()
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
     * 将 MiniMessage 格式转换为 Adventure Component
     * 支持丰富的格式：<red>、<gradient:red:blue>、<bold> 等
     * 同时也兼容 Legacy 颜色代码（&a, &b 等）
     */
    fun miniMessage(text: String?): Component =
        if (text == null) Component.empty() else miniMessage.deserialize(text)

    /**
     * 智能解析文本格式（自动检测 MiniMessage 或 Legacy）
     * 如果文本包含 MiniMessage 标签，则使用 MiniMessage 解析以支持所有高级特性（点击、悬停等）
     * 否则使用 Legacy 颜色代码解析
     * 注意: hovertext 格式 (<text=...>) 应该先在 parseClickableText 中处理
     * @param text 文本内容
     * @return Adventure Component
     */
    /**
     * Legacy 颜色代码到 MiniMessage 标签的映射
     */
    private val legacyToMiniMessageMap = mapOf(
        // 颜色代码
        "&0" to "<black>", "§0" to "<black>",
        "&1" to "<dark_blue>", "§1" to "<dark_blue>",
        "&2" to "<dark_green>", "§2" to "<dark_green>",
        "&3" to "<dark_aqua>", "§3" to "<dark_aqua>",
        "&4" to "<dark_red>", "§4" to "<dark_red>",
        "&5" to "<dark_purple>", "§5" to "<dark_purple>",
        "&6" to "<gold>", "§6" to "<gold>",
        "&7" to "<gray>", "§7" to "<gray>",
        "&8" to "<dark_gray>", "§8" to "<dark_gray>",
        "&9" to "<blue>", "§9" to "<blue>",
        "&a" to "<green>", "§a" to "<green>",
        "&b" to "<aqua>", "§b" to "<aqua>",
        "&c" to "<red>", "§c" to "<red>",
        "&d" to "<light_purple>", "§d" to "<light_purple>",
        "&e" to "<yellow>", "§e" to "<yellow>",
        "&f" to "<white>", "§f" to "<white>",
        // 格式化代码
        "&k" to "<obfuscated>", "§k" to "<obfuscated>",
        "&l" to "<bold>", "§l" to "<bold>",
        "&m" to "<strikethrough>", "§m" to "<strikethrough>",
        "&n" to "<underline>", "§n" to "<underline>",
        "&o" to "<italic>", "§o" to "<italic>",
        "&r" to "<reset>", "§r" to "<reset>",
        // 大写版本
        "&A" to "<green>", "§A" to "<green>",
        "&B" to "<aqua>", "§B" to "<aqua>",
        "&C" to "<red>", "§C" to "<red>",
        "&D" to "<light_purple>", "§D" to "<light_purple>",
        "&E" to "<yellow>", "§E" to "<yellow>",
        "&F" to "<white>", "§F" to "<white>",
        "&K" to "<obfuscated>", "§K" to "<obfuscated>",
        "&L" to "<bold>", "§L" to "<bold>",
        "&M" to "<strikethrough>", "§M" to "<strikethrough>",
        "&N" to "<underline>", "§N" to "<underline>",
        "&O" to "<italic>", "§O" to "<italic>",
        "&R" to "<reset>", "§R" to "<reset>"
    )

    /**
     * 将 Legacy 颜色代码转换为 MiniMessage 标签
     * @param text 包含 Legacy 颜色代码的文本
     * @return 转换后的文本
     */
    private fun convertLegacyToMiniMessage(text: String): String {
        var result = text
        legacyToMiniMessageMap.forEach { (legacy, mini) ->
            result = result.replace(legacy, mini)
        }
        return result
    }

    internal fun parseText(text: String?): Component {
        if (text == null) return Component.empty()

        // 检测是否包含 MiniMessage 标签（<...>，排除 <text=...> 自定义格式）
        // MiniMessage 标签特征：尖括号包裹的字母、冒号、渐变等
        val hasMiniMessageTags = text.contains(Regex("<[a-z_]+(?:[:][^>]*)?>", RegexOption.IGNORE_CASE))

        return if (hasMiniMessageTags) {
            // 检测是否包含 Legacy 颜色代码
            val hasLegacyCodes = text.contains(Regex("[&§][0-9a-fA-FlmnoOrkLKMNO]"))
            
            val textToParse = if (hasLegacyCodes) {
                // 将 Legacy 颜色代码转换为 MiniMessage 标签
                convertLegacyToMiniMessage(text)
            } else {
                text
            }
            
            // 使用 MiniMessage 解析，保留所有高级特性（点击、悬停、渐变等）
            miniMessage(textToParse)
        } else {
            // 使用 Legacy 颜色代码解析
            color(text)
        }
    }

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
            // 如果没有 actions 列表，返回一个无操作的动作
            return DialogAction.customClick({ _, _ -> }, ClickCallback.Options.builder().build())
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
                    // 未知类型的Map，返回无操作
                    return DialogAction.customClick({ _, _ -> }, ClickCallback.Options.builder().build())
                }
            } else if (firstAction !is String) {
                // 其他非String、非condition的Map类型，返回无操作
                return DialogAction.customClick({ _, _ -> }, ClickCallback.Options.builder().build())
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
                            // 检查是否为 checkbox
                            val mapping = checkboxMappings[key]
                            if (inputTypes[key] == "checkbox" && mapping != null) {
                                // checkbox：根据选中状态返回对应的映射值
                                if (intVal == 1) {
                                    mapping.first  // 选中状态返回 onTrue
                                } else {
                                    mapping.second  // 未选中状态返回 onFalse
                                }
                            } else {
                                // 非 checkbox，保持原值
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

            // 执行动作列表（支持条件判断）- 使用异步版本，支持 wait 动作和条件判断
            // 在主线程回调中启动异步执行，但不阻塞主线程
            executeActionListAsync(player, actionList.map { it ?: Any() }, variables, menuOpener, config)
        }, ClickCallback.Options.builder().lifetime(Duration.ofMinutes(5)).build())
    }

    /**
     * 执行动作列表（异步版本）- 用于主线程中的 Dialog 回调
     * 支持 wait 动作和条件判断，但不阻塞主线程
     * 使用 CompletableFuture 链保证执行顺序
     * 数据操作异步执行，确保在条件判断之前完成
     * @param initialDelay 初始延迟（从父动作链累积的延迟）
     * @return CompletableFuture<Void> 在所有动作完成后完成
     */
    private fun executeActionListAsync(
        player: Player,
        actionList: List<Any>,
        variables: Map<String, String>,
        menuOpener: (Player, String) -> Unit,
        config: YamlConfiguration?,
        initialDelay: Long = 0L
    ): CompletableFuture<Void> {
        // 创建异步执行链
        var chain: CompletableFuture<Void> = CompletableFuture.completedFuture(null)
        var currentDelay = initialDelay  // 使用传入的初始延迟

        for (action in actionList) {
            chain = chain.thenCompose { _ ->
                when (action) {
                    is Map<*, *> -> {
                        // 条件判断动作
                        val group = action
                        var conditionStr = group["condition"] as? String ?: ""

                        // 替换条件中的变量
                        variables.forEach { (key, value) ->
                            conditionStr = conditionStr.replace("$($key)", value)
                        }

                        val successActions = (group["actions"] ?: group["allow"]) as? List<*> ?: emptyList<Any>()
                        val denyActions = (group["deny"] as? List<*>) ?: emptyList<Any>()

                        val actionsToUse = if (ConditionUtils.checkCondition(player, conditionStr)) {
                            successActions
                        } else {
                            denyActions
                        }

                        // 递归执行子动作列表并等待完成，传递当前累积的延迟
                        executeActionListAsync(player, actionsToUse.map { it ?: Any() }, variables, menuOpener, config, currentDelay)
                    }
                    is List<*> -> {
                        // 普通动作列表 - 按顺序执行
                        var listDelay = currentDelay  // 使用父链传递的初始延迟
                        var subChain: CompletableFuture<Void> = CompletableFuture.completedFuture(null)

                        for (subAction in action) {
                            val actionStr = subAction?.toString() ?: continue
                            var finalAction = actionStr
                            variables.forEach { (key, value) ->
                                finalAction = finalAction.replace("$($key)", value)
                            }

                            val capturedDelay = listDelay  // 捕获当前延迟
                            when {
                                finalAction.startsWith("wait:", ignoreCase = true) -> {
                                    // wait 动作 - 只累积延迟，不执行任何操作
                                    val waitTime = finalAction.substring(5).trim().toLongOrNull() ?: 0L
                                    listDelay += waitTime
                                    // 添加一个空的 thenCompose 来保持链的顺序
                                    subChain = subChain.thenCompose { CompletableFuture.completedFuture(null) }
                                }
                                finalAction.trim() == "return" -> {
                                    // return 动作 - 中断执行
                                    break
                                }
                                finalAction.startsWith("actions:", ignoreCase = true) -> {
                                    // actions: 动作 - 执行 Events.Click 下的动作组并等待完成
                                    val actionKey = finalAction.removePrefix("actions:").trim()
                                    if (config != null && actionKey.isNotEmpty()) {
                                        val actionPath = "Events.Click.$actionKey"
                                        val actionList = config.getList(actionPath)
                                        if (actionList != null && actionList.isNotEmpty()) {
                                            // 传递当前累积的延迟，并等待动作组完成
                                            subChain = subChain.thenCompose { _ ->
                                                executeActionListAsync(
                                                    player,
                                                    actionList.map { it ?: Any() },
                                                    mapOf(),  // actions 组不继承父动作的 variables
                                                    menuOpener,
                                                    config,
                                                    capturedDelay  // 传递当前累积的延迟
                                                )
                                            }
                                        } else {
                                            // 动作组不存在或为空，继续执行
                                        }
                                    }
                                    listDelay = 0L  // 重置延迟（即使动作组不存在）
                                }
                                else -> {
                                    // 普通动作 - 应用当前累积的延迟
                                    val future = CompletableFuture<Void>()
                                    subChain = subChain.thenCompose { _ ->
                                        if (capturedDelay > 0) {
                                            // 应用累积的延迟 - 使用异步延迟任务
                                            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin ?: return@thenCompose CompletableFuture.completedFuture(null), Runnable {
                                                executeSingleAction(player, finalAction, variables, menuOpener, config, asyncDataOperations = true)
                                                future.complete(null)
                                            }, capturedDelay)
                                            listDelay = 0L  // 应用后重置延迟
                                        } else {
                                            // 无延迟，立即执行
                                            executeSingleAction(player, finalAction, variables, menuOpener, config, asyncDataOperations = true)
                                            future.complete(null)
                                        }
                                        future
                                    }
                                }
                            }
                        }
                        subChain
                    }
                    is String -> {
                        // 单个动作字符串
                        var finalAction: String = action
                        variables.forEach { (key, value) ->
                            finalAction = finalAction.replace("$($key)", value)
                        }

                        when {
                            finalAction.startsWith("wait:", ignoreCase = true) -> {
                                // wait 动作 - 只累积延迟
                                val waitTime = finalAction.substring(5).trim().toLongOrNull() ?: 0L
                                currentDelay += waitTime
                                CompletableFuture.completedFuture(null)
                            }
                            finalAction.trim() == "return" -> {
                                // return 动作 - 中断执行
                                CompletableFuture.completedFuture(null)
                            }
                            finalAction.startsWith("actions:", ignoreCase = true) -> {
                                // actions: 动作 - 执行 Events.Click 下的动作组并等待完成
                                val actionKey = finalAction.removePrefix("actions:").trim()
                                if (config != null && actionKey.isNotEmpty()) {
                                    val actionPath = "Events.Click.$actionKey"
                                    val actionList = config.getList(actionPath)
                                    if (actionList != null && actionList.isNotEmpty()) {
                                        // 传递当前累积的延迟，并等待动作组完成
                                        executeActionListAsync(
                                            player,
                                            actionList.map { it ?: Any() },
                                            mapOf(),  // actions 组不继承父动作的 variables
                                            menuOpener,
                                            config,
                                            currentDelay  // 传递当前累积的延迟
                                        )
                                    } else {
                                        CompletableFuture.completedFuture(null)
                                    }
                                } else {
                                    CompletableFuture.completedFuture(null)
                                }
                            }
                            else -> {
                                // 普通动作
                                val future = CompletableFuture<Void>()
                                if (currentDelay > 0) {
                                    // 应用累积的延迟 - 使用异步延迟任务
                                    Bukkit.getScheduler().runTaskLaterAsynchronously(plugin ?: return@thenCompose CompletableFuture.completedFuture(null), Runnable {
                                        executeSingleAction(player, finalAction, variables, menuOpener, config, asyncDataOperations = true)
                                        future.complete(null)
                                    }, currentDelay)
                                    currentDelay = 0L  // 重置延迟
                                } else {
                                    // 无延迟，立即执行
                                    executeSingleAction(player, finalAction, variables, menuOpener, config, asyncDataOperations = true)
                                    future.complete(null)
                                }
                                future
                            }
                        }
                    }
                    else -> {
                        // 忽略其他类型
                        CompletableFuture.completedFuture(null)
                    }
                }
            }
        }

        // 返回异步执行链，并添加异常处理
        return chain.exceptionally { error ->
            plugin?.logger?.severe("动作执行失败: ${error.message}")
            error.printStackTrace()
            null
        }
    }

    /**
     * 执行动作列表（同步版本）- 用于主线程中的 Dialog 回调
     * 在主线程中同步执行动作，避免使用 CompletableFuture 阻塞
     * @return Boolean 表示是否应该中断后续动作的执行（true表示中断）
     */
    private fun executeActionListSync(
        player: Player,
        actionList: List<Any>,
        variables: Map<String, String>,
        menuOpener: (Player, String) -> Unit,
        config: YamlConfiguration?
    ): Boolean {
        val actionsToExecute = mutableListOf<DeferredAction>()
        var currentDelay = 0L
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

                    // 递归执行子动作列表（同步版本）
                    val subResult = executeActionListSync(player, actionsToUse.map { it ?: Any() }, variables, menuOpener, config)
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

                        // 检查是否是 wait 动作 - 同步版本不支持 wait
                        if (finalAction.startsWith("wait:", ignoreCase = true)) {
                            // 同步版本跳过 wait 动作
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

                    // 检查是否是 wait 动作 - 同步版本不支持 wait
                    if (finalAction.startsWith("wait:", ignoreCase = true)) {
                        // 同步版本跳过 wait 动作
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

        // 按延迟时间执行所有动作（同步执行）
        actionsToExecute.forEach { deferred ->
            if (deferred.delay > 0) {
                // 同步版本使用延迟执行，但不阻塞
                Bukkit.getScheduler().runTaskLater(plugin ?: return@forEach, Runnable {
                    executeSingleAction(player, deferred.action, deferred.variables, menuOpener, config, asyncDataOperations = true)
                }, deferred.delay)
            } else {
                // 无延迟，直接执行
                executeSingleAction(player, deferred.action, deferred.variables, menuOpener, config, asyncDataOperations = true)
            }
        }

        return shouldReturn
    }

    /**
     * 执行动作列表（支持简单动作、列表动作和条件判断动作）
     * @param asyncMode 是否完全异步执行（包括wait动作）。true: 所有动作异步执行，false: 数据操作异步，其他操作同步
     * @return 包含执行结果的 CompletableFuture，Boolean 表示是否应该中断后续动作的执行（true表示中断）
     */
    private fun executeActionList(
        player: Player,
        actionList: List<Any>,
        variables: Map<String, String>,
        menuOpener: (Player, String) -> Unit,
        baseDelay: Long = 0L,
        config: YamlConfiguration? = null,
        asyncDataOperations: Boolean = true,
        asyncMode: Boolean = false
    ): CompletableFuture<Boolean> {
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
                    // 注意：在异步版本中，条件判断的子动作列表会立即开始执行，但我们不等待结果
                    // 这意味着如果子动作列表中有 return，它不会中断当前动作列表的执行
                    // 这是异步模式的设计选择，以避免阻塞
                    val ignored = executeActionList(player, actionsToUse.map { it ?: Any() }, variables, menuOpener, currentDelay, config, asyncDataOperations, asyncMode)
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
        val actionFutures = actionsToExecute.map { deferred ->
            executeDeferredAction(player, deferred, menuOpener, config, asyncDataOperations, asyncMode)
        }

        // 等待所有动作完成
        // 如果没有动作或只有 Map 类型的条件判断（这些不添加到 actionFutures），
        // 返回一个已完成的 Future，表示不应该中断
        return if (actionFutures.isEmpty()) {
            CompletableFuture.completedFuture(false)
        } else {
            CompletableFuture.allOf(*actionFutures.toTypedArray()).thenApply { shouldReturn }
        }
    }

    /**
     * 执行事件动作（如 Open、Close 等）- 异步版本
     * 事件动作不支持 $(input) 变量，因为菜单还未打开
     * @param player 玩家对象
     * @param config 菜单配置
     * @param eventName 事件名称（如 "Open"、"Close" 等）
     * @return CompletableFuture 包含是否应该中断后续操作（true表示中断，例如Open事件中遇到return）
     */
    fun executeEvent(player: Player, config: YamlConfiguration, eventName: String): CompletableFuture<Boolean> {
        val eventPath = "Events.$eventName"
        val eventActions = config.getList(eventPath) ?: return CompletableFuture.completedFuture(false)

        // 定义菜单打开器（事件中可能需要打开其他菜单）
        val menuOpener: (Player, String) -> Unit = { p, menuName ->
            val kaMenu = Bukkit.getPluginManager().getPlugin("KaMenu") as? KaMenu
            if (kaMenu != null) {
                Bukkit.getScheduler().runTask(kaMenu, Runnable {
                    MenuUI.openMenu(p, menuName, kaMenu.menuManager, kaMenu)
                })
            }
        }

        // 检查是否包含 wait 动作
        val hasWaitAction = hasWaitAction(eventActions)

        // 执行事件动作（没有输入变量，也不支持 $(input) 变量）
        // 如果有 wait 动作，使用完全异步模式；否则使用半异步模式（数据操作异步，其他操作同步）
        return executeActionList(
            player,
            eventActions.map { it ?: Any() },
            emptyMap(),
            menuOpener,
            0L,
            config,
            asyncDataOperations = true,
            asyncMode = hasWaitAction
        )
    }

    /**
     * 执行事件动作（如 Open、Close 等）- 同步版本
     * 用于不包含 wait 动作的情况
     * @param player 玩家对象
     * @param config 菜单配置
     * @param eventName 事件名称（如 "Open"、"Close" 等）
     * @return 是否应该中断后续操作（true表示中断，例如Open事件中遇到return）
     */
    fun executeEventSync(player: Player, config: YamlConfiguration, eventName: String): Boolean {
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

        // 同步执行事件动作（使用同步版本避免阻塞）
        return executeActionListSync(
            player,
            eventActions.map { it ?: Any() },
            emptyMap(),
            menuOpener,
            config
        )
    }

    /**
     * 检查动作列表中是否包含 wait 动作（公开方法）
     */
    fun hasWaitActionInList(actionList: List<*>): Boolean {
        return hasWaitAction(actionList)
    }

    /**
     * 检查动作列表中是否包含 wait 动作（内部实现）
     */
    private fun hasWaitAction(actionList: List<*>): Boolean {
        for (action in actionList) {
            when (action) {
                is Map<*, *> -> {
                    // 递归检查条件判断中的动作
                    val successActions = (action["actions"] ?: action["allow"]) as? List<*> ?: emptyList<Any>()
                    val denyActions = (action["deny"] as? List<*>) ?: emptyList<Any>()
                    if (hasWaitAction(successActions) || hasWaitAction(denyActions)) {
                        return true
                    }
                }
                is List<*> -> {
                    for (subAction in action) {
                        val actionStr = subAction?.toString() ?: continue
                        if (actionStr.trim().lowercase().startsWith("wait:")) {
                            return true
                        }
                        // 递归检查嵌套的列表
                        if (subAction is List<*> && hasWaitAction(subAction)) {
                            return true
                        }
                    }
                }
                is String -> {
                    if (action.trim().lowercase().startsWith("wait:")) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * 执行延迟动作
     * @return CompletableFuture 在动作执行完成后完成
     */
    private fun executeDeferredAction(
        player: Player,
        deferred: DeferredAction,
        menuOpener: (Player, String) -> Unit,
        config: YamlConfiguration? = null,
        asyncDataOperations: Boolean = true,
        asyncMode: Boolean = false
    ): CompletableFuture<Void> {
        val future = CompletableFuture<Void>()

        if (deferred.delay > 0 && asyncMode) {
            // 完全异步模式：延迟也是异步的
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin ?: return CompletableFuture.completedFuture(null), Runnable {
                executeSingleAction(player, deferred.action, deferred.variables, menuOpener, config, asyncDataOperations)
                future.complete(null)
            }, deferred.delay)
        } else if (deferred.delay > 0) {
            // 半异步模式：延迟在主线程执行，但数据操作异步
            Bukkit.getScheduler().runTaskLater(plugin ?: return CompletableFuture.completedFuture(null), Runnable {
                executeSingleAction(player, deferred.action, deferred.variables, menuOpener, config, asyncDataOperations)
                future.complete(null)
            }, deferred.delay)
        } else {
            // 无延迟，直接执行
            executeSingleAction(player, deferred.action, deferred.variables, menuOpener, config, asyncDataOperations)
            future.complete(null)
        }

        return future
    }

    /**
     * 执行单个动作
     */
    private fun executeSingleAction(
        player: Player,
        action: String,
        variables: Map<String, String>,
        menuOpener: (Player, String) -> Unit,
        config: YamlConfiguration? = null,
        asyncDataOperations: Boolean = true
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
                player.sendMessage(parseText(finalCmd.removePrefix("tell:").trim()))

            // js: 执行 JavaScript 代码或预定义函数
            finalCmd.startsWith("js:") -> {
                if (JavaScriptManager.isAvailable()) {
                    val jsCode = finalCmd.removePrefix("js:").trim()

                    try {
                        // 检查是否是预定义函数格式 [function_name]
                        if (jsCode.startsWith("[") && config != null) {
                            // 预处理：提取函数名和参数
                            val trimmed = jsCode.trim()
                            val closeBracketIndex = trimmed.indexOf(']')

                            if (closeBracketIndex > 0) {
                                // 提取函数名（在 [ 和 ] 之间）
                                val functionName = trimmed.substring(1, closeBracketIndex)

                                // 提取参数（在 ] 之后）
                                var firstSpaceIndex = -1
                                for (i in (closeBracketIndex + 1) until trimmed.length) {
                                    if (trimmed[i].isWhitespace()) {
                                        firstSpaceIndex = i
                                        break
                                    }
                                }

                                val argsString = if (firstSpaceIndex > closeBracketIndex) {
                                    trimmed.substring(firstSpaceIndex).trim()
                                } else {
                                    ""
                                }

                                // 执行预定义函数并传递参数
                                val result = JavaScriptManager.executePredefinedFunctionWithArgs(player, functionName, argsString, config)
                                if (result != null && result != "") {
                                    // 如果有返回值，显示给玩家（可选）
                                    player.sendMessage(parseText("§aJS Result: $result"))
                                }
                            } else {
                                // 格式错误，直接当作代码执行
                                val result = JavaScriptManager.evaluateWithContext(player, jsCode)
                                if (result != null && result != "") {
                                    player.sendMessage(parseText("§aJS Result: $result"))
                                }
                            }
                        } else {
                            // 直接执行 JavaScript 代码
                            val result = JavaScriptManager.evaluateWithContext(player, jsCode)
                            if (result != null && result != "") {
                                // 如果有返回值，显示给玩家（可选）
                                player.sendMessage(parseText("§aJS Result: $result"))
                            }
                        }
                    } catch (e: Exception) {
                        plugin?.logger?.warning("JavaScript execution error for player ${player.name}: ${e.message}")
                        player.sendMessage(parseText("§cJavaScript execution failed: ${e.message}"))
                    }
                } else {
                    player.sendMessage(parseText("§cJavaScript feature is not available. Please restart the server to complete the initial setup."))
                }
            }

            // actionbar: ActionBar 消息
            finalCmd.startsWith("actionbar:") -> {
                val message = finalCmd.removePrefix("actionbar:").trim()
                player.sendActionBar(parseText(message))
            }

            // title: 发送标题
            finalCmd.startsWith("title:") -> {
                val args = finalCmd.removePrefix("title:").trim()
                Bukkit.getScheduler().runTask(plugin ?: return, Runnable {
                    parseAndSendTitle(player, args)
                })
            }

            // hovertext: 可点击文本
            finalCmd.startsWith("hovertext:") -> {
                val text = finalCmd.removePrefix("hovertext:").trim()
                val message = parseClickableText(text)
                player.sendMessage(message)
            }

            // command: 玩家执行指令
            finalCmd.startsWith("command:") -> {
                val cmd = finalCmd.removePrefix("command:").trim()
                Bukkit.getScheduler().runTask(plugin ?: return, Runnable {
                    player.performCommand(cmd)
                })
            }

            // chat: 玩家执行指令
            finalCmd.startsWith("chat:") -> {
                val cmd = finalCmd.removePrefix("chat:").trim()
                Bukkit.getScheduler().runTask(plugin ?: return, Runnable {
                    player.chat(cmd)
                })
            }

            // console: 控制台执行指令
            finalCmd.startsWith("console:") -> {
                val cmd = finalCmd.removePrefix("console:").trim()
                Bukkit.getScheduler().runTask(plugin ?: return, Runnable {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
                })
            }

            // sound: 播放声音 (支持音量和音调参数)
            finalCmd.startsWith("sound:") -> {
                val args = finalCmd.removePrefix("sound:").trim()
                Bukkit.getScheduler().runTask(plugin ?: return, Runnable {
                    parseAndPlaySound(player, args)
                })
            }

            // open: 打开另一个对话框
            finalCmd.startsWith("open:") -> {
                val menuName = finalCmd.removePrefix("open:").trim()
                menuOpener(player, menuName)
            }

            // close: 关闭对话框
            finalCmd.startsWith("close") -> {
                // 先执行 Events.Close 事件（异步执行，不等待结果）
                if (config != null) {
                    // 检查是否有 Close 事件
                    val hasCloseEvent = config.contains("Events.Close")
                    if (hasCloseEvent) {
                        // 异步执行 Close 事件，不等待结果（避免阻塞）
                        executeEvent(player, config, "Close").whenComplete { result, error ->
                            if (error != null) {
                                plugin?.logger?.severe("Close 事件执行失败: ${error.message}")
                                error.printStackTrace()
                            } else if (!result) {
                                // Close 事件中没有 return，关闭菜单
                                Bukkit.getScheduler().runTask(plugin ?: return@whenComplete, Runnable {
                                    player.closeInventory()
                                })
                            }
                            // 如果 result 为 true（Close 事件中遇到 return），不关闭菜单
                        }
                        return  // 提前返回，不在这里关闭菜单
                    }
                }
                // 没有 Close 事件，直接关闭菜单
                Bukkit.getScheduler().runTask(plugin ?: return, Runnable {
                    player.closeInventory()
                })
            }

            // actions: 执行 Events.Click 下的动作列表
            finalCmd.startsWith("actions:") -> {
                if (config != null) {
                    val actionKey = finalCmd.removePrefix("actions:").trim()
                    if (actionKey.isNotEmpty()) {
                        // 异步执行动作列表
                        Bukkit.getScheduler().runTaskAsynchronously(plugin ?: return, Runnable {
                            val actionPath = "Events.Click.$actionKey"
                            val actionList = config.getList(actionPath)

                            if (actionList != null && actionList.isNotEmpty()) {
                                executeActionList(
                                    player,
                                    actionList.map { it ?: Any() },
                                    mapOf(),
                                    menuOpener,
                                    0L,
                                    config
                                )
                            } else {
                                player.sendMessage(parseText(plugin!!.languageManager.getMessage("actions.action_list_not_found", actionKey)))
                            }
                        })
                    }
                }
            }

            // set-data: 设置玩家数据
            finalCmd.startsWith("set-data:") -> {
                val args = finalCmd.removePrefix("set-data:").trim()
                parseDataAction(args, player.uniqueId.toString(), "data") { uuid, key, value ->
                    if (asyncDataOperations) {
                        // 异步执行数据库操作，避免阻塞主线程
                        Bukkit.getScheduler().runTaskAsynchronously(plugin ?: return@parseDataAction, Runnable {
                            databaseManager?.setPlayerData(java.util.UUID.fromString(uuid), key, value)
                        })
                    } else {
                        // 同步执行数据库操作，确保数据在菜单渲染前完成
                        databaseManager?.setPlayerData(java.util.UUID.fromString(uuid), key, value)
                    }
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
                        if (asyncDataOperations) {
                            // 异步执行数据库操作，避免阻塞主线程
                            Bukkit.getScheduler().runTaskAsynchronously(plugin ?: return@parseAndExecuteDataAction, Runnable {
                                databaseManager?.setPlayerData(player.uniqueId, key, value)
                            })
                        } else {
                            // 同步执行数据库操作，确保数据在菜单渲染前完成
                            databaseManager?.setPlayerData(player.uniqueId, key, value)
                        }
                    },
                    modifyAction = { key, delta ->
                        if (asyncDataOperations) {
                            // 异步执行数据库操作，避免阻塞主线程
                            Bukkit.getScheduler().runTaskAsynchronously(plugin ?: return@parseAndExecuteDataAction, Runnable {
                                databaseManager?.modifyPlayerData(player.uniqueId, key, delta)
                            })
                        } else {
                            // 同步执行数据库操作，确保数据在菜单渲染前完成
                            databaseManager?.modifyPlayerData(player.uniqueId, key, delta)
                        }
                    },
                    deleteAction = { key ->
                        if (asyncDataOperations) {
                            // 异步执行数据库操作，避免阻塞主线程
                            Bukkit.getScheduler().runTaskAsynchronously(plugin ?: return@parseAndExecuteDataAction, Runnable {
                                databaseManager?.deletePlayerData(player.uniqueId, key)
                            })
                        } else {
                            // 同步执行数据库操作，确保数据在菜单渲染前完成
                            databaseManager?.deletePlayerData(player.uniqueId, key)
                        }
                    }
                )
            }

            // set-gdata: 设置全局数据
            finalCmd.startsWith("set-gdata:") -> {
                val args = finalCmd.removePrefix("set-gdata:").trim()
                parseDataAction(args, "", "gdata") { _, key, value ->
                    if (asyncDataOperations) {
                        // 异步执行数据库操作，避免阻塞主线程
                        Bukkit.getScheduler().runTaskAsynchronously(plugin ?: return@parseDataAction, Runnable {
                            databaseManager?.setGlobalData(key, value)
                        })
                    } else {
                        // 同步执行数据库操作，确保数据在菜单渲染前完成
                        databaseManager?.setGlobalData(key, value)
                    }
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
                        if (asyncDataOperations) {
                            // 异步执行数据库操作，避免阻塞主线程
                            Bukkit.getScheduler().runTaskAsynchronously(plugin ?: return@parseAndExecuteDataAction, Runnable {
                                databaseManager?.setGlobalData(key, value)
                            })
                        } else {
                            // 同步执行数据库操作，确保数据在菜单渲染前完成
                            databaseManager?.setGlobalData(key, value)
                        }
                    },
                    modifyAction = { key, delta ->
                        if (asyncDataOperations) {
                            // 异步执行数据库操作，避免阻塞主线程
                            Bukkit.getScheduler().runTaskAsynchronously(plugin ?: return@parseAndExecuteDataAction, Runnable {
                                databaseManager?.modifyGlobalData(key, delta)
                            })
                        } else {
                            // 同步执行数据库操作，确保数据在菜单渲染前完成
                            databaseManager?.modifyGlobalData(key, delta)
                        }
                    },
                    deleteAction = { key ->
                        if (asyncDataOperations) {
                            // 异步执行数据库操作，避免阻塞主线程
                            Bukkit.getScheduler().runTaskAsynchronously(plugin ?: return@parseAndExecuteDataAction, Runnable {
                                databaseManager?.deleteGlobalData(key)
                            })
                        } else {
                            // 同步执行数据库操作，确保数据在菜单渲染前完成
                            databaseManager?.deleteGlobalData(key)
                        }
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
                    if (asyncDataOperations) {
                        // 异步执行数据库操作，避免阻塞主线程
                        plugin?.let { Bukkit.getScheduler().runTaskAsynchronously(it, Runnable {
                            databaseManager?.setGlobalData(key, value)
                        })}
                    } else {
                        // 同步执行数据库操作，确保数据在菜单渲染前完成
                        databaseManager?.setGlobalData(key, value)
                    }
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
                Bukkit.getScheduler().runTask(plugin ?: return, Runnable {
                    parseAndSendToast(player, args)
                })
            }

            // money: 操作玩家金币
            finalCmd.startsWith("money:") -> {
                val args = finalCmd.removePrefix("money:").trim()
                Bukkit.getScheduler().runTask(plugin ?: return, Runnable {
                    parseAndHandleMoney(player, args, variables)
                })
            }

            // stock-item: 物品给予/扣除
            finalCmd.startsWith("stock-item:") -> {
                val args = finalCmd.removePrefix("stock-item:").trim()
                Bukkit.getScheduler().runTask(plugin ?: return, Runnable {
                    parseAndHandleStockItem(player, args, variables)
                })
            }

            // item: 普通物品给予/扣除
            finalCmd.startsWith("item:") -> {
                val args = finalCmd.removePrefix("item:").trim()
                Bukkit.getScheduler().runTask(plugin ?: return, Runnable {
                    parseAndHandleItem(player, args, variables)
                })
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
            player.sendMessage(parseText(languageManager?.getMessage("actions.test_failed", "插件未初始化") ?: "§c插件未初始化，无法执行动作"))
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
            player.sendMessage(parseText(e.message?.let { languageManager?.getMessage("actions.test_failed", it) } ?: "§c动作执行失败: ${e.message}"))
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

        val titleComponent = if (title.isEmpty()) Component.empty() else parseText(title)
        val subtitleComponent = if (subtitle.isEmpty()) Component.empty() else parseText(subtitle)

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
     * 格式: <text='显示文字';hover='悬停文字';command='指令';url='链接';newline='false';actions='动作列表路径'>
     * 注意: 只有包含 text= 参数的标签才会被解析为可点击文本，其他的 <...> 标签会被保留给 MiniMessage 处理
     */
    fun parseClickableText(rawText: String): Component {
        return parseClickableText(rawText, null, null, null)
    }

    /**
     * 解析可点击文本 (使用 Adventure API) - 带上下文版本
     * 格式: <text='显示文字';hover='悬停文字';command='指令';url='链接';newline='false';actions='动作列表路径'>
     * 注意: 只有包含 text= 参数的标签才会被解析为可点击文本，其他的 <...> 标签会被保留给 MiniMessage 处理
     * @param rawText 原始文本
     * @param player 玩家对象（用于 actions 回调）
     * @param config 菜单配置（用于加载动作列表）
     * @param menuOpener 菜单打开函数
     */
    fun parseClickableText(
        rawText: String,
        player: Player?,
        config: YamlConfiguration?,
        menuOpener: ((Player, String) -> Unit)?
    ): Component {
        val replacements = mutableListOf<Pair<IntRange, Component>>()
        var currentPos = 0

        while (currentPos < rawText.length) {
            val startIndex = rawText.indexOf("<text=", currentPos, ignoreCase = true)

            if (startIndex == -1) break

            // 找到 hovertext 的结束位置
            val endIndex = findClosingBracket(rawText, startIndex)
            if (endIndex == -1) break

            // 提取完整的 hovertext 标签（包括 < >）
            val content = rawText.substring(startIndex + 1, endIndex)  // 不包括尖括号

            // 解析 hovertext（传递上下文）
            val component = parseClickableComponent(content, player, config, menuOpener)
            if (component != null) {
                // 记录替换：原始位置范围 → 组件
                replacements.add(Pair(IntRange(startIndex, endIndex), component))
                currentPos = endIndex + 1
            } else {
                currentPos = startIndex + 1
            }
        }

        // 如果没有 hovertext，直接用 parseText 处理 MiniMessage
        if (replacements.isEmpty()) {
            return parseText(rawText)
        }

        // 按位置升序排序，从前往后处理
        replacements.sortBy { it.first.first }

        // 按顺序拼接：MiniMessage 文本 + hovertext 组件
        val mainBuilder = Component.text()
        var lastEnd = 0

        replacements.forEach { (range, component) ->
            // 添加 hovertext 之前的文本（包含 MiniMessage）
            if (range.first > lastEnd) {
                mainBuilder.append(parseText(rawText.substring(lastEnd, range.first)))
            }
            // 添加 hovertext 组件
            mainBuilder.append(component)
            lastEnd = range.last + 1
        }

        // 添加最后剩余的文本
        if (lastEnd < rawText.length) {
            mainBuilder.append(parseText(rawText.substring(lastEnd)))
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
     * 只有包含 text= 参数的内容才会被解析为可点击文本，否则返回 null
     * @param content 组件内容
     * @param player 玩家对象（用于 actions 回调）
     * @param config 菜单配置（用于加载动作列表）
     * @param menuOpener 菜单打开函数
     */
    private fun parseClickableComponent(
        content: String,
        player: Player? = null,
        config: YamlConfiguration? = null,
        menuOpener: ((Player, String) -> Unit)? = null
    ): Component? {
        var text = ""
        var hover = ""
        var command = ""
        var url = ""
        var actions = ""
        var newline = false
        var hasTextParam = false

        val parts = content.split(';')
        for (part in parts) {
            val trimmed = part.trim()
            val eqIndex = trimmed.indexOf('=')

            if (eqIndex != -1) {
                val key = trimmed.take(eqIndex).trim().lowercase()
                val value = trimmed.substring(eqIndex + 1).trim()

                when (key) {
                    "text" -> {
                        text = value.removeSurrounding("`").removeSurrounding("'").removeSurrounding("\"")
                        hasTextParam = true
                    }
                    "hover" -> hover = value.removeSurrounding("`").removeSurrounding("'").removeSurrounding("\"")
                    "command" -> command = value.removeSurrounding("`").removeSurrounding("'").removeSurrounding("\"")
                    "url" -> url = value.removeSurrounding("`").removeSurrounding("'").removeSurrounding("\"")
                    "actions" -> actions = value.removeSurrounding("`").removeSurrounding("'").removeSurrounding("\"")
                    "newline" -> newline = value.removeSurrounding("`").removeSurrounding("'").removeSurrounding("\"").equals("true", ignoreCase = true)
                }
            }
        }

        // 只有包含 text= 参数且 text 不为空时才返回组件
        return if (hasTextParam && text.isNotEmpty()) {
            createAdventureClickableText(text, hover, command, url, actions, newline, player, config, menuOpener)
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
        return createAdventureClickableText(text, hoverText, command, url, "", newline, null, null, null)
    }

    /**
     * 创建可点击文本组件 (使用 Adventure API) - 带上下文版本
     * @param text 显示文本
     * @param hoverText 悬停文本
     * @param command 执行命令
     * @param url 打开链接
     * @param actions 动作列表路径（Events.Click 下的键名）
     * @param newline 是否换行
     * @param player 玩家对象
     * @param config 菜单配置
     * @param menuOpener 菜单打开函数
     */
    fun createAdventureClickableText(
        text: String,
        hoverText: String = "",
        command: String = "",
        url: String = "",
        actions: String = "",
        newline: Boolean = false,
        player: Player? = null,
        config: YamlConfiguration? = null,
        menuOpener: ((Player, String) -> Unit)? = null
    ): Component {
        var component = parseText(text)

        // 添加点击事件
        if (actions.isNotEmpty()) {
            // 使用 ClickCallback 执行动作列表
            if (player != null && config != null && menuOpener != null) {
                component = component.clickEvent(ClickEvent.callback({ audience ->
                    if (audience is Player) {
                        // 从 Events.Click 加载动作列表
                        val actionPath = "Events.Click.$actions"
                        val actionList = config.getList(actionPath)

                        if (actionList != null && actionList.isNotEmpty()) {
                            // 异步执行动作列表
                            plugin?.let {
                                Bukkit.getScheduler().runTaskAsynchronously(it, Runnable {
                                    executeActionList(
                                        audience,
                                        actionList.map { it ?: Any() },
                                        mapOf(),  // 空 variables map
                                        menuOpener,
                                        0L,
                                        config
                                    )
                                })
                            }
                        } else {
                            audience.sendMessage(parseText(plugin?.languageManager?.getMessage("actions.action_list_not_found", actions)))
                        }
                    }
                }, ClickCallback.Options.builder()
                    .lifetime(Duration.ofMinutes(5))
                    .build()))
            }
        } else if (url.isNotEmpty()) {
            component = component.clickEvent(ClickEvent.openUrl(url))
        } else if (command.isNotEmpty()) {
            component = component.clickEvent(ClickEvent.runCommand(command))
        }

        // 添加悬停事件
        if (hoverText.isNotEmpty()) {
            component = component.hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(parseText(hoverText)))
        }

        // 添加换行
        if (newline) {
            component = component.append(Component.newline())
        }

        return component
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

        val titleJson = GsonComponentSerializer.gson().serialize(parseText(title))
        val descJson = GsonComponentSerializer.gson().serialize(parseText(description))

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
                player.sendMessage(parseText(it))
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
                        player.sendActionBar(parseText(actionbarMessage))
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

        // 获取材质（使用规范化的材质匹配）
        val material = MaterialUtils.matchMaterial(finalMaterialName)
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
                        player.sendActionBar(parseText(actionbarMessage))
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
