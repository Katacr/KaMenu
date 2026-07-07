@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import io.papermc.paper.registry.data.dialog.action.DialogAction
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.event.ClickEvent
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.katacr.kamenu.api.KaMenuActionHandler
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 菜单动作处理器
 * 负责解析和执行菜单中的各种动作
 */
object MenuActions {
    private var languageManager: LanguageManager? = null
    private var databaseManager: DatabaseManager? = null
    private var metaDataManager: MetaDataManager? = null
    private var economy: Economy? = null
    private var plugin: KaMenu? = null
    private var itemManager: ItemManager? = null
    private var actionPackageManager: ActionPackageManager? = null
    private var bungeeCordEnabled: Boolean = false
    private val externalActionHandlers = ConcurrentHashMap<String, KaMenuActionHandler>()

    /**
     * 解析后的动作数据类（包含目标选择器）
     */
    private data class ParsedAction(
        val action: String,
        val targetSelector: String?
    )

    private data class ActionExecutionContext(
        val player: Player,
        val variables: Map<String, String>,
        val menuOpener: (Player, String) -> Unit,
        val config: YamlConfiguration?,
        val asyncDataOperations: Boolean,
        val taskRef: MenuTaskManager.TaskExecutionRef? = null,
        val contextId: String? = null,
        val actionListId: String? = null,
        val handledMenuLifecycle: AtomicBoolean = AtomicBoolean(false)
    )

    private data class ResolvedActionList(
        val actions: List<Any>,
        val id: String
    )

    private const val INPUT_TRIM_EDGE_SPACES_PATH = "input-capture.trim-edge-spaces"

    /**
     * 动作类型枚举
     */
    private enum class ActionType {
        MULTITARGET,  // 支持多目标的动作
        SINGLE_TARGET_ONLY  // 只对单个玩家有意义的动作
    }

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

    fun setActionPackageManager(manager: ActionPackageManager) {
        actionPackageManager = manager
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
     * 设置 BungeeCord 启用状态
     */
    fun setBungeeCordEnabled(enabled: Boolean) {
        bungeeCordEnabled = enabled
    }

    /**
     * 解析目标选择器
     * 从动作字符串中提取 {player: ...} 部分
     * @param action 原始动作字符串
     * @return ParsedAction 包含动作和目标选择器
     */
    private fun parseTargetSelector(action: String): ParsedAction {
        val lower = action.lowercase()
        val start = lower.lastIndexOf("{player:")
        if (start < 0) {
            return ParsedAction(action, null)
        }

        var depth = 0
        var end = -1
        for (index in start until action.length) {
            when (action[index]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        end = index
                        break
                    }
                }
            }
        }

        if (end < 0) {
            return ParsedAction(action, null)
        }

        val selector = action.substring(start + "{player:".length, end).trim()
        val actionWithoutSelector = action.removeRange(start, end + 1).trimEnd()
        return ParsedAction(actionWithoutSelector, selector)
    }

    /**
     * 获取动作类型
     * @param action 动作字符串
     * @return 动作类型（MULTITARGET 或 SINGLE_TARGET_ONLY）
     */
    private fun getActionType(action: String): ActionType {
        val trimmedAction = action.trim().lowercase()

        return when {
            // 只对单个玩家有意义的动作
            trimmedAction.startsWith("server:") -> ActionType.SINGLE_TARGET_ONLY
            trimmedAction.startsWith("actions:") -> ActionType.SINGLE_TARGET_ONLY
            trimmedAction.startsWith("run-task:") -> ActionType.SINGLE_TARGET_ONLY
            trimmedAction.startsWith("stop-task:") -> ActionType.SINGLE_TARGET_ONLY
            trimmedAction.startsWith("stop-current-task") -> ActionType.SINGLE_TARGET_ONLY
            trimmedAction.startsWith("page:") -> ActionType.SINGLE_TARGET_ONLY
            trimmedAction.startsWith("wait:") -> ActionType.SINGLE_TARGET_ONLY
            trimmedAction.startsWith("return") -> ActionType.SINGLE_TARGET_ONLY

            // 支持多目标的动作
            else -> ActionType.MULTITARGET
        }
    }

    /**
     * 根据目标选择器获取玩家列表
     * @param player 当前玩家
     * @param selector 目标选择器（null、*、all 或条件表达式）
     * @return 目标玩家列表
     */
    private fun getTargetPlayers(player: Player, selector: String?): List<Player> {
        if (selector == null) {
            // 没有指定目标，返回当前玩家
            return listOf(player)
        }

        val trimmedSelector = selector.trim()

        return when (trimmedSelector.lowercase()) {
            "*", "all" -> {
                // 所有在线玩家
                Bukkit.getOnlinePlayers().toList()
            }
            else -> {
                // 条件选择，遍历所有在线玩家检查条件
                val targetPlayers = mutableListOf<Player>()
                for (onlinePlayer in Bukkit.getOnlinePlayers()) {
                    try {
                        if (ConditionUtils.checkCondition(onlinePlayer, trimmedSelector)) {
                            targetPlayers.add(onlinePlayer)
                        }
                    } catch (e: Exception) {
                        // 条件检查失败，跳过此玩家
                        plugin?.logger?.warning("目标选择器条件检查失败: ${e.message}")
                    }
                }
                targetPlayers
            }
        }
    }

    fun registerExternalActionHandler(namespace: String, handler: KaMenuActionHandler): Boolean {
        val normalized = namespace.trim().lowercase()
        if (normalized.isEmpty() || normalized.contains(":")) {
            return false
        }
        externalActionHandlers[normalized] = handler
        return true
    }

    fun unregisterExternalActionHandler(namespace: String) {
        val normalized = namespace.trim().lowercase()
        if (normalized.isNotEmpty()) {
            externalActionHandlers.remove(normalized)
        }
    }

    private fun dispatchExternalAction(
        player: Player,
        action: String,
        variables: Map<String, String>,
        config: YamlConfiguration?
    ): Boolean {
        val trimmed = action.trim()
        val colonIndex = trimmed.indexOf(':')
        if (colonIndex <= 0) {
            return false
        }

        val namespace = trimmed.substring(0, colonIndex).trim().lowercase()
        val handler = externalActionHandlers[namespace] ?: return false

        return try {
            handler.execute(player, trimmed, variables, config)
        } catch (e: Exception) {
            plugin?.logger?.warning("外部 action handler 执行失败: namespace=$namespace, action=$trimmed, 错误: ${e.message}")
            e.printStackTrace()
            true
        }
    }


    /**
     * 解析变量（完整顺序：$(var) -> {data:var} -> %papi_var%）
     * @param player 玩家对象
     * @param text 原始文本
     * @param variables 输入变量映射（$(var)）
     * @return 解析后的文本
     */
    /**
     * 解析变量（内置变量 + PAPI）
     * @param player 玩家对象
     * @param text 原始文本
     * @return 解析后的文本
     */
    private fun resolveVariables(player: Player, text: String): String {
        return ActionHandlers.resolveVariables(player, text)
    }

    private fun parseActionCall(raw: String): ActionArgumentParser.Call {
        return ActionArgumentParser.parseCall(raw)
    }

    private fun findActionList(config: YamlConfiguration?, actionName: String): ResolvedActionList? {
        if (actionName.isEmpty()) {
            return null
        }

        val localActions = config
            ?.getList("Events.Click.$actionName")
            ?.takeIf { it.isNotEmpty() }
            ?.map { it ?: Any() }
        if (localActions != null) {
            return ResolvedActionList(localActions, "menu:$actionName")
        }

        val packageActions = actionPackageManager?.getActions(actionName) ?: return null
        return ResolvedActionList(packageActions, "package:$actionName")
    }

    private fun actionListNotFoundMessage(actionName: String, config: YamlConfiguration?): String {
        val key = if (config == null) {
            "actions.action_list_not_found_global"
        } else {
            "actions.action_list_not_found_with_global"
        }
        return plugin?.languageManager?.getMessage(key, actionName)
            ?: plugin?.languageManager?.getMessage("actions.action_list_not_found", actionName)
            ?: "§cError: Action list '$actionName' not found"
    }

    private fun message(key: String, vararg args: Any): String {
        return plugin?.languageManager?.getMessage(key, *args)
            ?: languageManager?.getMessage(key, *args)
            ?: key
    }

    private fun mergeActionArguments(variables: Map<String, String>, args: List<String>): Map<String, String> {
        if (args.isEmpty()) {
            return variables
        }

        val merged = variables.toMutableMap()
        args.forEachIndexed { index, value ->
            merged["arg:$index"] = value
        }
        return merged
    }

    /**
     * 解析条件动作 Map，并返回当前玩家应执行的分支。
     * 支持 actions/allow 作为成功分支，deny 作为失败分支。
     */
    private fun selectConditionalActions(
        player: Player,
        group: Map<*, *>,
        variables: Map<String, String>,
        config: YamlConfiguration?
    ): List<*> {
        val condition = group["condition"] as? String ?: ""
        val (successActions, denyActions) = getConditionalBranches(group)
        return if (ConditionUtils.checkCondition(player, condition, variables, config) { null }) {
            successActions
        } else {
            denyActions
        }
    }

    private fun getConditionalBranches(group: Map<*, *>): Pair<List<*>, List<*>> {
        val successActions = (group["actions"] ?: group["allow"]) as? List<*> ?: emptyList<Any>()
        val denyActions = (group["deny"] as? List<*>) ?: emptyList<Any>()
        return successActions to denyActions
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
        inputRemoveChars: Map<String, String> = emptyMap(),
        checkboxMappings: Map<String, Pair<String, String>>,
        menuOpener: (Player, String) -> Unit,
        closesDialogAfterAction: Boolean = false,
        initialVariables: Map<String, String> = emptyMap(),
        contextId: String? = null
    ): DialogAction {
        // 只使用 actions（复数）键
        val actionList = config.getList(path)
        if (actionList == null || actionList.isEmpty()) {
            return DialogAction.customClick({ _, _ ->
                completeDialogCloseLifecycle(player, config, MenuTaskManager.currentToken(player), closesDialogAfterAction)
            }, ClickCallback.Options.builder().build())
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
            val initialTaskToken = MenuTaskManager.currentToken(player)
            val variables = initialVariables.toMutableMap()
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
                variables[key] = sanitizeInputValue(key, value ?: "", inputTypes, inputRemoveChars)
            }

            val handledMenuLifecycle = AtomicBoolean(false)
            executeActionList(
                player,
                actionList.map { it ?: Any() },
                variables,
                menuOpener,
                config = config,
                contextId = contextId,
                handledMenuLifecycle = handledMenuLifecycle
            )
                .whenComplete { _, error ->
                    if (error != null) {
                        plugin?.logger?.severe("按钮动作执行失败: ${error.message}")
                        error.printStackTrace()
                    }
                    if (!handledMenuLifecycle.get()) {
                        completeDialogCloseLifecycle(player, config, initialTaskToken, closesDialogAfterAction)
                    }
                }
        }, ClickCallback.Options.builder().lifetime(Duration.ofMinutes(5)).build())
    }

    private fun sanitizeInputValue(
        key: String,
        rawValue: String,
        inputTypes: Map<String, String>,
        inputRemoveChars: Map<String, String>
    ): String {
        if (inputTypes[key] != "text") {
            return rawValue
        }

        var value = rawValue
        if (plugin?.config?.getBoolean(INPUT_TRIM_EDGE_SPACES_PATH, false) == true) {
            value = value.trim()
        }

        val removeChars = inputRemoveChars[key].orEmpty()
        if (removeChars.isNotEmpty()) {
            val removeSet = removeChars.toSet()
            value = value.filterNot { it in removeSet }
        }

        return value
    }

    private fun completeDialogCloseLifecycle(
        player: Player,
        config: YamlConfiguration,
        initialTaskToken: Long?,
        closesDialogAfterAction: Boolean
    ) {
        if (!closesDialogAfterAction || initialTaskToken == null) {
            return
        }
        if (MenuTaskManager.currentToken(player) != initialTaskToken) {
            return
        }

        if (config.contains("Events.Close")) {
            executeEvent(player, config, "Close").whenComplete { _, error ->
                if (error != null) {
                    plugin?.logger?.severe("Close 事件执行失败: ${error.message}")
                    error.printStackTrace()
                }
                if (MenuTaskManager.currentToken(player) == initialTaskToken) {
                    MenuTaskManager.cancel(player)
                }
            }
        } else {
            MenuTaskManager.cancel(player)
        }
    }

    /**
     * 按顺序执行动作列表。
     * wait 是序列中的暂停节点；条件和普通动作都会在真正轮到该节点时解析变量。
     */
    private fun executeActionList(
        player: Player,
        actionList: List<Any>,
        variables: Map<String, String>,
        menuOpener: (Player, String) -> Unit,
        baseDelay: Long = 0L,
        config: YamlConfiguration? = null,
        asyncDataOperations: Boolean = true,
        taskRef: MenuTaskManager.TaskExecutionRef? = null,
        contextId: String? = null,
        actionListId: String? = null,
        handledMenuLifecycle: AtomicBoolean = AtomicBoolean(false)
    ): CompletableFuture<Boolean> {
        val context = ActionExecutionContext(
            player = player,
            variables = variables,
            menuOpener = menuOpener,
            config = config,
            asyncDataOperations = asyncDataOperations,
            taskRef = taskRef,
            contextId = contextId,
            actionListId = actionListId,
            handledMenuLifecycle = handledMenuLifecycle
        )
        val start = if (baseDelay > 0) delayTicks(baseDelay) else CompletableFuture.completedFuture(false)
        return start.thenCompose { executeActionSequence(context, actionList) }
            .exceptionally { error ->
                plugin?.logger?.severe("动作执行失败: ${error.message}")
                error.printStackTrace()
                false
            }
    }

    fun executeActionGroup(
        player: Player,
        config: YamlConfiguration,
        actions: List<*>,
        variables: Map<String, String> = emptyMap(),
        asyncDataOperations: Boolean = true,
        taskRef: MenuTaskManager.TaskExecutionRef? = null,
        contextId: String? = null
    ): CompletableFuture<Boolean> {
        val menuOpener: (Player, String) -> Unit = { p, menuName ->
            val kaMenu = Bukkit.getPluginManager().getPlugin("KaMenu") as? KaMenu
            if (kaMenu != null) {
                Bukkit.getScheduler().runTask(kaMenu, Runnable {
                    MenuUI.openMenu(p, menuName, kaMenu.menuManager, kaMenu)
                })
            }
        }

        return executeActionList(
            player,
            actions.map { it ?: Any() },
            variables,
            menuOpener,
            config = config,
            asyncDataOperations = asyncDataOperations,
            taskRef = taskRef,
            contextId = contextId
        )
    }

    fun executeStandaloneActions(
        player: Player,
        actions: List<*>,
        variables: Map<String, String> = emptyMap(),
        asyncDataOperations: Boolean = true
    ): CompletableFuture<Boolean> {
        val menuOpener: (Player, String) -> Unit = { p, menuName ->
            val kaMenu = Bukkit.getPluginManager().getPlugin("KaMenu") as? KaMenu
            if (kaMenu != null) {
                Bukkit.getScheduler().runTask(kaMenu, Runnable {
                    MenuUI.openMenu(p, menuName, kaMenu.menuManager, kaMenu)
                })
            }
        }

        return executeActionList(
            player,
            actions.map { it ?: Any() },
            variables,
            menuOpener,
            config = null,
            asyncDataOperations = asyncDataOperations
        )
    }

    private fun executeActionSequence(
        context: ActionExecutionContext,
        actionList: List<Any>,
        index: Int = 0
    ): CompletableFuture<Boolean> {
        if (index >= actionList.size) {
            return CompletableFuture.completedFuture(false)
        }

        return executeActionNode(context, actionList[index]).thenCompose { shouldReturn ->
            if (shouldReturn) {
                CompletableFuture.completedFuture(true)
            } else {
                executeActionSequence(context, actionList, index + 1)
            }
        }
    }

    private fun executeActionNode(
        context: ActionExecutionContext,
        action: Any
    ): CompletableFuture<Boolean> {
        return when (action) {
            is Map<*, *> -> {
                val actionsToUse = selectConditionalActions(context.player, action, context.variables, context.config)
                executeActionSequence(context, actionsToUse.map { it ?: Any() })
            }
            is List<*> -> executeActionSequence(context, action.map { it ?: Any() })
            is String -> executeActionString(context, action)
            else -> CompletableFuture.completedFuture(false)
        }
    }

    private fun executeActionString(
        context: ActionExecutionContext,
        action: String
    ): CompletableFuture<Boolean> {
        val controlAction = TextResolver.resolve(context.player, action, context.variables, context.config).trim()

        return when {
            controlAction.startsWith("wait:", ignoreCase = true) -> {
                val ticks = controlAction.substringAfter(":", "").trim().toLongOrNull() ?: 0L
                delayTicks(ticks).thenApply { false }
            }
            controlAction.equals("return", ignoreCase = true) -> {
                CompletableFuture.completedFuture(true)
            }
            controlAction.equals("stop-current-task", ignoreCase = true) -> {
                context.taskRef?.let { MenuTaskManager.stopTask(it) }
                CompletableFuture.completedFuture(true)
            }
            controlAction.startsWith("page:", ignoreCase = true) -> {
                handlePageAction(context.player, controlAction, context.config, context.contextId)
                CompletableFuture.completedFuture(false)
            }
            controlAction.startsWith("actions:", ignoreCase = true) -> {
                val actionCall = parseActionCall(controlAction.substringAfter(":", "").trim())
                if (actionCall.name.isEmpty()) {
                    CompletableFuture.completedFuture(false)
                } else {
                    val subActionList = findActionList(context.config, actionCall.name)
                    if (subActionList == null) {
                        context.player.sendMessage(TextParser.parseText(actionListNotFoundMessage(actionCall.name, context.config)))
                        CompletableFuture.completedFuture(false)
                    } else if (subActionList.id == context.actionListId) {
                        context.player.sendMessage(TextParser.parseText(plugin?.languageManager?.getMessage("actions.action_list_self_call", actionCall.name)))
                        CompletableFuture.completedFuture(false)
                    } else {
                        val childContext = context.copy(
                            variables = mergeActionArguments(context.variables, actionCall.arguments),
                            actionListId = subActionList.id
                        )
                        executeActionSequence(childContext, subActionList.actions)
                    }
                }
            }
            else -> {
                executeSingleAction(
                    context.player,
                    action,
                    context.variables,
                    context.menuOpener,
                    context.config,
                    context.asyncDataOperations,
                    context.contextId,
                    context.handledMenuLifecycle
                )
                CompletableFuture.completedFuture(false)
            }
        }
    }

    private fun delayTicks(ticks: Long): CompletableFuture<Boolean> {
        if (ticks <= 0) {
            return CompletableFuture.completedFuture(false)
        }

        val currentPlugin = plugin ?: return CompletableFuture.completedFuture(false)
        val future = CompletableFuture<Boolean>()
        Bukkit.getScheduler().runTaskLater(currentPlugin, Runnable {
            future.complete(false)
        }, ticks)
        return future
    }

    private fun handlePageAction(player: Player, action: String, config: YamlConfiguration?, contextId: String?) {
        val currentPlugin = plugin ?: return
        val currentConfig = config ?: return
        val args = action.substringAfter(":", "").trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (args.size < 2) {
            return
        }

        val listId = args[0]
        val operation = args[1].lowercase()
        val resolvedContextId = contextId ?: currentPlugin.menuManager.getMenuId(currentConfig) ?: "external:${System.identityHashCode(currentConfig)}"

        when {
            operation == "next" -> MenuListManager.movePage(player, resolvedContextId, listId, 1)
            operation == "prev" || operation == "previous" -> MenuListManager.movePage(player, resolvedContextId, listId, -1)
            operation.startsWith("+") -> MenuListManager.movePage(player, resolvedContextId, listId, operation.drop(1).toIntOrNull() ?: 0)
            operation.startsWith("-") -> MenuListManager.movePage(player, resolvedContextId, listId, operation.toIntOrNull() ?: 0)
            else -> operation.toIntOrNull()?.let { MenuListManager.setPage(player, resolvedContextId, listId, it) }
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
    fun executeEvent(player: Player, config: YamlConfiguration, eventName: String, contextId: String? = null): CompletableFuture<Boolean> {
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

        // 执行事件动作（没有输入变量，也不支持 $(input) 变量）
        return executeActionList(
            player,
            eventActions.map { it ?: Any() },
            emptyMap(),
            menuOpener,
            0L,
            config,
            asyncDataOperations = true,
            contextId = contextId
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
    fun executeEventSync(player: Player, config: YamlConfiguration, eventName: String, contextId: String? = null): Boolean {
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

        return executeActionList(
            player,
            eventActions.map { it ?: Any() },
            emptyMap(),
            menuOpener,
            config = config,
            contextId = contextId
        ).get()
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
                    val (successActions, denyActions) = getConditionalBranches(action)
                    if (hasWaitAction(successActions) || hasWaitAction(denyActions)) {
                        return true
                    }
                }
                is List<*> -> {
                    for (subAction in action) {
                        val actionStr = subAction?.toString() ?: continue
                        if (actionStr.trim().startsWith("wait:", ignoreCase = true)) {
                            return true
                        }
                        // 递归检查嵌套的列表
                        if (subAction is List<*> && hasWaitAction(subAction)) {
                            return true
                        }
                    }
                }
                is String -> {
                    if (action.trim().startsWith("wait:", ignoreCase = true)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * 执行单个动作（支持目标选择器）
     */
    private fun executeSingleAction(
        player: Player,
        action: String,
        variables: Map<String, String>,
        menuOpener: (Player, String) -> Unit,
        config: YamlConfiguration? = null,
        asyncDataOperations: Boolean = true,
        contextId: String? = null,
        handledMenuLifecycle: AtomicBoolean? = null
    ) {
        // 解析目标选择器
        val parsed = parseTargetSelector(action)
        val actionWithoutSelector = parsed.action
        val selector = parsed.targetSelector

        // 获取动作类型
        val actionType = getActionType(actionWithoutSelector)

        // 根据动作类型决定是否支持多目标
        when {
            // 不支持多目标的动作，只对当前玩家执行
            actionType == ActionType.SINGLE_TARGET_ONLY || selector == null -> {
                executeActionForPlayer(player, actionWithoutSelector, variables, menuOpener, config, asyncDataOperations, contextId, handledMenuLifecycle)
            }

            // 支持多目标的动作，获取所有目标玩家并执行
            actionType == ActionType.MULTITARGET -> {
                val targetPlayers = getTargetPlayers(player, selector)

                if (targetPlayers.isEmpty()) {
                    return
                }

                // 对每个目标玩家执行动作
                targetPlayers.forEach { targetPlayer ->
                    executeActionForPlayer(targetPlayer, actionWithoutSelector, variables, menuOpener, config, asyncDataOperations, contextId, handledMenuLifecycle)
                }
            }
        }
    }

    /**
     * 对单个玩家执行动作
     */
    private fun executeActionForPlayer(
        player: Player,
        action: String,
        variables: Map<String, String>,
        menuOpener: (Player, String) -> Unit,
        config: YamlConfiguration? = null,
        asyncDataOperations: Boolean = true,
        contextId: String? = null,
        handledMenuLifecycle: AtomicBoolean? = null
    ) {
        // 解析输入变量、动作包参数、内置变量、JavaScript 与 PAPI 变量
        val finalCmd = TextResolver.resolve(player, action, variables, config)

        if (dispatchExternalAction(player, finalCmd, variables, config)) {
            return
        }

        when {
            // tell: 普通消息
            finalCmd.startsWith("tell:") ->
                player.sendMessage(TextParser.parseText(finalCmd.removePrefix("tell:").trim()))

            // js: 执行 JavaScript 代码或 JavaScript 包
            finalCmd.startsWith("js:") -> {
                if (JavaScriptManager.isAvailable()) {
                    val jsCode = finalCmd.removePrefix("js:").trim()

                    try {
                        val jsCall = ActionArgumentParser.parseBracketCall(jsCode)
                        if (jsCall != null) {
                            JavaScriptManager.executePredefinedFunctionWithArgs(player, jsCall.name, jsCall.arguments, config)
                        } else {
                            JavaScriptManager.evaluateWithContext(player, jsCode)
                        }
                    } catch (e: Exception) {
                        val error = e.message ?: e.javaClass.simpleName
                        plugin?.logger?.warning(message("javascript.execution_error_player", player.name, error))
                        player.sendMessage(TextParser.parseText(message("javascript.execution_failed_user", error)))
                    }
                } else {
                    player.sendMessage(TextParser.parseText(message("javascript.unavailable")))
                }
            }

            // actionbar: ActionBar 消息
            finalCmd.startsWith("actionbar:") -> {
                val message = finalCmd.removePrefix("actionbar:").trim()
                player.sendActionBar(TextParser.parseText(message))
            }

            // title: 发送标题
            finalCmd.startsWith("title:") -> {
                val args = finalCmd.removePrefix("title:").trim()
                Bukkit.getScheduler().runTask(plugin ?: return, Runnable {
                    ActionHandlers.parseAndSendTitle(player, args)
                })
            }

            // hovertext: 可点击文本
            finalCmd.startsWith("hovertext:") -> {
                val text = finalCmd.removePrefix("hovertext:").trim()
                val message = parseClickableText(text, player, config, menuOpener)
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
                    ActionHandlers.parseAndPlaySound(player, args)
                })
            }

            // run-task: 开始执行 Events.Tasks 下的任务，可选指定次数，如 run-task: test 10
            finalCmd.startsWith("run-task:") -> {
                val args = finalCmd.removePrefix("run-task:").trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
                val taskId = args.getOrNull(0)
                val repeat = args.getOrNull(1)?.toIntOrNull()
                if (!taskId.isNullOrEmpty()) {
                    if (taskId == "*") {
                        MenuTaskManager.runAllTasks(player, repeat)
                    } else {
                        MenuTaskManager.runTask(player, taskId, repeat)
                    }
                }
            }

            // stop-task: 停止 Events.Tasks 下正在运行的任务
            finalCmd.startsWith("stop-task:") -> {
                val taskId = finalCmd.removePrefix("stop-task:").trim()
                if (taskId.isNotEmpty()) {
                    if (taskId == "*") {
                        MenuTaskManager.stopAllTasks(player)
                    } else {
                        MenuTaskManager.stopTask(player, taskId)
                    }
                }
            }

            // open: 打开另一个对话框（会执行 Events.Open）
            finalCmd.startsWith("open:") -> {
                val menuName = finalCmd.removePrefix("open:").trim()
                handledMenuLifecycle?.set(true)
                menuOpener(player, menuName)
            }

            // force-open: 强制打开菜单（不执行 Events.Open）
            finalCmd.startsWith("force-open:") -> {
                val menuName = finalCmd.removePrefix("force-open:").trim()
                val kaMenu = Bukkit.getPluginManager().getPlugin("KaMenu") as? KaMenu
                if (kaMenu != null) {
                    Bukkit.getScheduler().runTask(kaMenu, Runnable {
                        handledMenuLifecycle?.set(true)
                        MenuUI.forceOpenMenu(player, menuName, kaMenu.menuManager, kaMenu)
                    })
                }
            }

            // reset: 重新打开当前菜单（不执行 Events.Open）
            finalCmd.trim() == "reset" -> {
                if (config != null) {
                    val kaMenu = Bukkit.getPluginManager().getPlugin("KaMenu") as? KaMenu
                    if (kaMenu != null) {
                        val currentMenuId = kaMenu.menuManager.getMenuId(config)
                        if (currentMenuId != null) {
                            Bukkit.getScheduler().runTask(kaMenu, Runnable {
                                handledMenuLifecycle?.set(true)
                                MenuUI.forceOpenMenu(player, currentMenuId, kaMenu.menuManager, kaMenu)
                            })
                        }
                    }
                }
            }

            // force-close: 强制关闭菜单（不执行 Events.Close）
            finalCmd.trim() == "force-close" -> {
                handledMenuLifecycle?.set(true)
                Bukkit.getScheduler().runTask(plugin ?: return, Runnable {
                    MenuTaskManager.cancel(player)
                    player.closeInventory()
                })
            }

            // close: 关闭对话框
            finalCmd.startsWith("close") -> {
                handledMenuLifecycle?.set(true)
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
                                    MenuTaskManager.cancel(player)
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
                    MenuTaskManager.cancel(player)
                    player.closeInventory()
                })
            }

            // actions: 执行 Events.Click 下的动作列表
            finalCmd.startsWith("actions:") -> {
                if (config != null) {
                    val actionCall = parseActionCall(finalCmd.removePrefix("actions:").trim())
                    if (actionCall.name.isNotEmpty()) {
                        // 异步执行动作列表
                        Bukkit.getScheduler().runTaskAsynchronously(plugin ?: return, Runnable {
                            val actionList = findActionList(config, actionCall.name)

                            if (actionList != null) {
                                executeActionList(
                                    player,
                                    actionList.actions,
                                    mergeActionArguments(variables, actionCall.arguments),
                                    menuOpener,
                                    0L,
                                    config,
                                    contextId = contextId,
                                    actionListId = actionList.id
                                )
                            } else {
                                player.sendMessage(TextParser.parseText(actionListNotFoundMessage(actionCall.name, config)))
                            }
                        })
                    }
                }
            }

            // set-data: 设置玩家数据
            finalCmd.startsWith("set-data:") -> {
                val args = finalCmd.removePrefix("set-data:").trim()
                ActionHandlers.parseDataAction(args, player.uniqueId.toString(), "data") { uuid, key, value ->
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
                ActionHandlers.parseAndExecuteDataAction(
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

            // list: 玩家列表数据操作
            finalCmd.startsWith("list:") -> {
                val args = finalCmd.removePrefix("list:").trim()
                ActionHandlers.parseAndExecuteListAction(
                    args = args,
                    player = player,
                    dataType = "list",
                    setAction = { key, values ->
                        if (asyncDataOperations) {
                            Bukkit.getScheduler().runTaskAsynchronously(plugin ?: return@parseAndExecuteListAction, Runnable {
                                databaseManager?.setPlayerList(player.uniqueId, key, values)
                            })
                        } else {
                            databaseManager?.setPlayerList(player.uniqueId, key, values)
                        }
                    },
                    addAction = { key, values, unique ->
                        if (asyncDataOperations) {
                            Bukkit.getScheduler().runTaskAsynchronously(plugin ?: return@parseAndExecuteListAction, Runnable {
                                databaseManager?.addPlayerListValues(player.uniqueId, key, values, unique)
                            })
                        } else {
                            databaseManager?.addPlayerListValues(player.uniqueId, key, values, unique)
                        }
                    },
                    removeAction = { key, values ->
                        if (asyncDataOperations) {
                            Bukkit.getScheduler().runTaskAsynchronously(plugin ?: return@parseAndExecuteListAction, Runnable {
                                databaseManager?.removePlayerListValues(player.uniqueId, key, values)
                            })
                        } else {
                            databaseManager?.removePlayerListValues(player.uniqueId, key, values)
                        }
                    },
                    clearAction = { key ->
                        if (asyncDataOperations) {
                            Bukkit.getScheduler().runTaskAsynchronously(plugin ?: return@parseAndExecuteListAction, Runnable {
                                databaseManager?.clearPlayerList(player.uniqueId, key)
                            })
                        } else {
                            databaseManager?.clearPlayerList(player.uniqueId, key)
                        }
                    },
                    deleteAction = { key ->
                        if (asyncDataOperations) {
                            Bukkit.getScheduler().runTaskAsynchronously(plugin ?: return@parseAndExecuteListAction, Runnable {
                                databaseManager?.deletePlayerData(player.uniqueId, key)
                            })
                        } else {
                            databaseManager?.deletePlayerData(player.uniqueId, key)
                        }
                    }
                )
            }

            // set-gdata: 设置全局数据
            finalCmd.startsWith("set-gdata:") -> {
                val args = finalCmd.removePrefix("set-gdata:").trim()
                ActionHandlers.parseDataAction(args, "", "gdata") { _, key, value ->
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
                ActionHandlers.parseAndExecuteDataAction(
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

            // glist: 全局列表数据操作
            finalCmd.startsWith("glist:") -> {
                val args = finalCmd.removePrefix("glist:").trim()
                ActionHandlers.parseAndExecuteListAction(
                    args = args,
                    player = player,
                    dataType = "glist",
                    setAction = { key, values ->
                        if (asyncDataOperations) {
                            Bukkit.getScheduler().runTaskAsynchronously(plugin ?: return@parseAndExecuteListAction, Runnable {
                                databaseManager?.setGlobalList(key, values)
                            })
                        } else {
                            databaseManager?.setGlobalList(key, values)
                        }
                    },
                    addAction = { key, values, unique ->
                        if (asyncDataOperations) {
                            Bukkit.getScheduler().runTaskAsynchronously(plugin ?: return@parseAndExecuteListAction, Runnable {
                                databaseManager?.addGlobalListValues(key, values, unique)
                            })
                        } else {
                            databaseManager?.addGlobalListValues(key, values, unique)
                        }
                    },
                    removeAction = { key, values ->
                        if (asyncDataOperations) {
                            Bukkit.getScheduler().runTaskAsynchronously(plugin ?: return@parseAndExecuteListAction, Runnable {
                                databaseManager?.removeGlobalListValues(key, values)
                            })
                        } else {
                            databaseManager?.removeGlobalListValues(key, values)
                        }
                    },
                    clearAction = { key ->
                        if (asyncDataOperations) {
                            Bukkit.getScheduler().runTaskAsynchronously(plugin ?: return@parseAndExecuteListAction, Runnable {
                                databaseManager?.clearGlobalList(key)
                            })
                        } else {
                            databaseManager?.clearGlobalList(key)
                        }
                    },
                    deleteAction = { key ->
                        if (asyncDataOperations) {
                            Bukkit.getScheduler().runTaskAsynchronously(plugin ?: return@parseAndExecuteListAction, Runnable {
                                databaseManager?.deleteGlobalData(key)
                            })
                        } else {
                            databaseManager?.deleteGlobalData(key)
                        }
                    }
                )
            }

            // set-meta: 设置玩家元数据（内存缓存）
            finalCmd.startsWith("set-meta:") -> {
                val args = finalCmd.removePrefix("set-meta:").trim()
                ActionHandlers.parseDataAction(args, player.uniqueId.toString(), "meta") { uuid, key, value ->
                    metaDataManager?.setPlayerMeta(java.util.UUID.fromString(uuid), key, value)
                }
            }

            // meta: 玩家元数据操作
            finalCmd.startsWith("meta:") -> {
                val args = finalCmd.removePrefix("meta:").trim()
                ActionHandlers.parseAndExecuteDataAction(
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
                    ActionHandlers.parseAndSendToast(player, args)
                })
            }

            // money: 操作玩家金币
            finalCmd.startsWith("money:") -> {
                val args = finalCmd.removePrefix("money:").trim()
                Bukkit.getScheduler().runTask(plugin ?: return, Runnable {
                    ActionHandlers.parseAndHandleMoney(player, args, variables)
                })
            }

            // stock-item: 物品给予/扣除
            finalCmd.startsWith("stock-item:") -> {
                val args = finalCmd.removePrefix("stock-item:").trim()
                Bukkit.getScheduler().runTask(plugin ?: return, Runnable {
                    ActionHandlers.parseAndHandleStockItem(player, args, variables)
                })
            }

            // item: 普通物品给予/扣除
            finalCmd.startsWith("item:") -> {
                val args = finalCmd.removePrefix("item:").trim()
                Bukkit.getScheduler().runTask(plugin ?: return, Runnable {
                    ActionHandlers.parseAndHandleItem(player, args, variables)
                })
            }

            // server: 传送到指定服务器（支持 BungeeCord/Velocity）
            finalCmd.startsWith("server:") -> {
                val serverName = finalCmd.removePrefix("server:").trim()
                Bukkit.getScheduler().runTask(plugin ?: return, Runnable {
                    ActionHandlers.parseAndHandleServer(player, serverName)
                })
            }

            // tppos: 传送到指定坐标
            finalCmd.startsWith("tppos:") -> {
                val args = finalCmd.removePrefix("tppos:").trim()
                Bukkit.getScheduler().runTask(plugin ?: return, Runnable {
                    ActionHandlers.parseAndHandleTppos(player, args)
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
            player.sendMessage(TextParser.parseText(languageManager?.getMessage("actions.test_failed", "插件未初始化") ?: "§c插件未初始化，无法执行动作"))
            return false
        }

        try {
            val menuOpener: (Player, String) -> Unit = { p, menuName ->
                Bukkit.getScheduler().runTask(plugin!!, Runnable {
                    MenuUI.openMenu(p, menuName, plugin!!.menuManager, plugin!!)
                })
            }

            executeActionList(
                player,
                listOf(actionString),
                emptyMap(),
                menuOpener,
                config = null,
                asyncDataOperations = true
            )
            return true
        } catch (e: Exception) {
            player.sendMessage(TextParser.parseText(e.message?.let { languageManager?.getMessage("actions.test_failed", it) } ?: "§c动作执行失败: ${e.message}"))
            plugin?.logger?.severe("测试动作执行失败: ${e.message}")
            e.printStackTrace()
            return false
        }
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
            return TextParser.parseText(rawText)
        }

        // 按位置升序排序，从前往后处理
        replacements.sortBy { it.first.first }

        // 按顺序拼接：MiniMessage 文本 + hovertext 组件
        val mainBuilder = Component.text()
        var lastEnd = 0

        replacements.forEach { (range, component) ->
            // 添加 hovertext 之前的文本（包含 MiniMessage）
            if (range.first > lastEnd) {
                mainBuilder.append(TextParser.parseText(rawText.substring(lastEnd, range.first)))
            }
            // 添加 hovertext 组件
            mainBuilder.append(component)
            lastEnd = range.last + 1
        }

        // 添加最后剩余的文本
        if (lastEnd < rawText.length) {
            mainBuilder.append(TextParser.parseText(rawText.substring(lastEnd)))
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
        var component = TextParser.parseText(text)

        // 添加点击事件
        if (actions.isNotEmpty()) {
            // 使用 ClickCallback 执行动作列表
            if (player != null && config != null && menuOpener != null) {
                component = component.clickEvent(ClickEvent.callback({ audience ->
                    if (audience is Player) {
                        val actionCall = parseActionCall(actions)
                        val actionList = findActionList(config, actionCall.name)

                        if (actionList != null) {
                            // 异步执行动作列表
                            plugin?.let {
                                Bukkit.getScheduler().runTaskAsynchronously(it, Runnable {
                                    executeActionList(
                                        audience,
                                        actionList.actions,
                                        mergeActionArguments(emptyMap(), actionCall.arguments),
                                        menuOpener,
                                        0L,
                                        config,
                                        actionListId = actionList.id
                                    )
                                })
                            }
                        } else {
                            audience.sendMessage(TextParser.parseText(actionListNotFoundMessage(actionCall.name, config)))
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
            component = component.hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(TextParser.parseText(hoverText)))
        }

        // 添加换行
        if (newline) {
            component = component.append(Component.newline())
        }

        return component
    }
}
