@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

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
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 菜单动作执行中心。
 *
 * 负责把 YAML 中的动作节点解析为实际行为，支持普通字符串动作、条件 Map、
 * 嵌套动作列表、动作包调用、目标选择器、概率/单行延迟修饰符、wait 延迟、return 中断和外部插件动作。
 *
 * 动作执行是“异步串行”的：每个节点按顺序执行，遇到 `wait:` 会返回一个 future，
 * 后续动作会在等待完成后继续，因此变量和条件会在真正执行到该节点时才解析；
 * `{wait: ...}` 则把当前行独立调度，不等待该行完成。
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

    /** 单行动作中提取出的条件、概率与独立延迟修饰符。 */
    private data class ActionModifiers(
        val action: String,
        val condition: String?,
        val chance: String?,
        val delay: String?
    )

    /**
     * 单次动作列表执行上下文。
     *
     * 这里集中保存玩家、变量、菜单配置和生命周期标记，避免在递归执行 wait/条件/actions 包时丢失上下文。
     */
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

    /**
     * 已解析的动作包或菜单内动作组。
     *
     * id 用于阻止动作组直接调用自身，避免无限递归。
     */
    private data class ResolvedActionList(
        val actions: List<Any>,
        val id: String
    )

    /** `open` / `force-open` 动作解析出的目标菜单和传入参数。 */
    private data class MenuOpenRequest(
        val menuId: String,
        val arguments: List<String>
    )

    /**
     * 动作类型枚举
     */
    private enum class ActionType {
        MULTITARGET,  // 支持多目标的动作
        SINGLE_TARGET_ONLY  // 只对单个玩家有意义的动作
    }

    private val pointsAliasPattern = Regex(
        """^(give|add|deposit|take|remove|withdraw)-?points?\s*:\s*(.*)$""",
        RegexOption.IGNORE_CASE
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
            trimmedAction.startsWith("set-args:") -> ActionType.SINGLE_TARGET_ONLY
            trimmedAction.startsWith("del-args") -> ActionType.SINGLE_TARGET_ONLY
            trimmedAction.startsWith("wait:") -> ActionType.SINGLE_TARGET_ONLY
            trimmedAction.startsWith("return") -> ActionType.SINGLE_TARGET_ONLY

            // 支持多目标的动作
            else -> ActionType.MULTITARGET
        }
    }

    /** 将 源菜单 风格的点券动作名转换为 KaMenu 的 add/take 操作。 */
    private fun parsePointsAlias(action: String): Pair<String, String>? {
        val match = pointsAliasPattern.matchEntire(action.trim()) ?: return null
        val type = when (match.groupValues[1].lowercase()) {
            "give", "add", "deposit" -> "add"
            else -> "take"
        }
        return type to match.groupValues[2].trim()
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

    /**
     * 注册外部动作命名空间。
     *
     * 外部动作会在内置动作前尝试执行，适合其他插件扩展 `namespace:payload`。
     * namespace 只能是冒号前缀，不包含冒号本身。
     */
    fun registerExternalActionHandler(namespace: String, handler: KaMenuActionHandler): Boolean {
        val normalized = namespace.trim().lowercase()
        if (normalized.isEmpty() || normalized.contains(":")) {
            return false
        }
        externalActionHandlers[normalized] = handler
        return true
    }

    /**
     * 注销外部动作命名空间。
     */
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

    /**
     * 查找动作组。
     *
     * 查找顺序固定为：当前菜单 `Events.Click.<name>` 优先，全局 actions 包其次。
     * 这样菜单可以覆盖同名全局包，便于局部定制。
     */
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

    /**
     * 把 actions 调用参数合并到变量表。
     *
     * 例如 `actions: hello,玩家,生存服` 会生成 `{arg:0}=玩家`、`{arg:1}=生存服`。
     */
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

    /** 解析 `菜单ID 参数...`，参数分隔和引号规则与 actions 包参数保持一致。 */
    private fun parseMenuOpenRequest(raw: String): MenuOpenRequest? {
        val parts = ActionArgumentParser.splitArguments(raw)
        val menuId = parts.firstOrNull()?.takeIf(String::isNotBlank) ?: return null
        return MenuOpenRequest(menuId, parts.drop(1))
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
     * 执行菜单按钮路径下的动作列表。
     *
     * Paper callback 与 Spigot custom-click session 都通过此入口复用动作编排、输入变量和关闭生命周期。
     * `closesDialogAfterAction` 表示客户端会在点击后自动关闭当前 Dialog。
     */
    fun executeConfigActionPath(
        player: Player,
        config: YamlConfiguration,
        path: String,
        variables: Map<String, String> = emptyMap(),
        menuOpener: ((Player, String) -> Unit)? = null,
        closesDialogAfterAction: Boolean = false,
        contextId: String? = null
    ): CompletableFuture<Boolean> {
        val actionList = config.getList(path)
        if (actionList == null || actionList.isEmpty()) {
            completeDialogCloseLifecycle(player, config, MenuTaskManager.currentToken(player), closesDialogAfterAction)
            return CompletableFuture.completedFuture(false)
        }

        val resolvedMenuOpener = menuOpener ?: { target: Player, menuName: String ->
            plugin?.let { kaMenu ->
                KaScheduler.runPlayer(target, Runnable {
                    MenuUI.openMenu(target, menuName, kaMenu.menuManager, kaMenu)
                })
            }
            Unit
        }
        val initialTaskToken = MenuTaskManager.currentToken(player)
        val handledMenuLifecycle = AtomicBoolean(false)
        return executeActionList(
            player,
            actionList.map { it ?: Any() },
            variables,
            resolvedMenuOpener,
            config = config,
            contextId = contextId,
            handledMenuLifecycle = handledMenuLifecycle
        ).whenComplete { _, error ->
            if (error != null) {
                plugin?.logger?.severe("按钮动作执行失败: ${error.message}")
                error.printStackTrace()
            }
            if (!handledMenuLifecycle.get()) {
                completeDialogCloseLifecycle(player, config, initialTaskToken, closesDialogAfterAction)
            }
        }
    }

    /**
     * 执行编译后 Dialog 按钮绑定的动作。
     *
     * 普通按钮使用配置路径，repeat 补位等合成按钮使用内存动作列表；两者共享相同的关闭生命周期。
     */
    fun executeDialogButton(
        player: Player,
        config: YamlConfiguration,
        actionPath: String,
        actionOverride: List<*>?,
        variables: Map<String, String> = emptyMap(),
        closesDialogAfterAction: Boolean = false,
        contextId: String? = null
    ): CompletableFuture<Boolean> {
        if (actionOverride == null) {
            return executeConfigActionPath(
                player,
                config,
                actionPath,
                variables,
                closesDialogAfterAction = closesDialogAfterAction,
                contextId = contextId
            )
        }

        val initialTaskToken = MenuTaskManager.currentToken(player)
        return executeActionGroup(
            player,
            config,
            actionOverride,
            variables,
            contextId = contextId
        ).whenComplete { _, _ ->
            completeDialogCloseLifecycle(player, config, initialTaskToken, closesDialogAfterAction)
        }
    }

    /** 执行可点击文本引用的菜单内或全局 actions 包，并支持 `{arg:n}` 参数。 */
    fun executeActionReference(
        player: Player,
        config: YamlConfiguration,
        rawCall: String,
        variables: Map<String, String> = emptyMap(),
        contextId: String? = null,
        closesDialogAfterAction: Boolean = false
    ): CompletableFuture<Boolean> {
        val initialTaskToken = MenuTaskManager.currentToken(player)
        val handledMenuLifecycle = AtomicBoolean(false)
        val actionCall = parseActionCall(rawCall)
        val actionList = findActionList(config, actionCall.name)
        if (actionList == null) {
            MenuUI.sendMessage(player, TextParser.parseText(actionListNotFoundMessage(actionCall.name, config), player))
            completeDialogCloseLifecycle(player, config, initialTaskToken, closesDialogAfterAction)
            return CompletableFuture.completedFuture(false)
        }

        val menuOpener: (Player, String) -> Unit = { target, menuName ->
            plugin?.let { kaMenu ->
                KaScheduler.runPlayer(target, Runnable {
                    MenuUI.openMenu(target, menuName, kaMenu.menuManager, kaMenu)
                })
            }
        }
        return executeActionList(
            player = player,
            actionList = actionList.actions,
            variables = mergeActionArguments(variables, actionCall.arguments),
            menuOpener = menuOpener,
            config = config,
            contextId = contextId,
            actionListId = actionList.id,
            handledMenuLifecycle = handledMenuLifecycle
        ).whenComplete { _, _ ->
            if (!handledMenuLifecycle.get()) {
                completeDialogCloseLifecycle(player, config, initialTaskToken, closesDialogAfterAction)
            }
        }
    }

    /**
     * 根据菜单 Settings 创建 Paper callback 生命周期配置。
     *
     * uses 固定为 1，确保每个 callback 只能触发一次。
     */
    private fun buildCallbackOptions(config: YamlConfiguration): ClickCallback.Options {
        return ClickCallback.Options.builder()
            .uses(1)
            .lifetime(Duration.ofSeconds(DialogSessionManager.lifetimeSeconds(config)))
            .build()
    }

    private fun completeDialogCloseLifecycle(
        player: Player,
        config: YamlConfiguration,
        initialTaskToken: Long?,
        closesDialogAfterAction: Boolean
    ) {
        if (!closesDialogAfterAction) {
            return
        }
        val argumentContext = MenuArgumentManager.currentContext(player)
        DialogSessionManager.cancel(player)
        if (initialTaskToken == null) {
            MenuArgumentManager.clearIfCurrent(player, argumentContext)
            return
        }
        if (MenuTaskManager.currentToken(player) != initialTaskToken ||
            MenuArgumentManager.currentContext(player) != argumentContext
        ) {
            return
        }

        if (config.contains("Events.Close")) {
            executeEvent(player, config, "Close").whenComplete { _, error ->
                if (error != null) {
                    plugin?.logger?.severe("Close 事件执行失败: ${error.message}")
                    error.printStackTrace()
                }
                if (MenuTaskManager.currentToken(player) == initialTaskToken &&
                    MenuArgumentManager.currentContext(player) == argumentContext
                ) {
                    MenuTaskManager.cancel(player)
                    MenuArgumentManager.clearIfCurrent(player, argumentContext)
                }
            }
        } else {
            MenuTaskManager.cancel(player)
            MenuArgumentManager.clearIfCurrent(player, argumentContext)
        }
    }

    /**
     * 按顺序执行动作列表。
     * wait 是序列中的暂停节点；条件和普通动作都会在真正轮到该节点时解析变量。
     *
     * 返回值含义：`true` 表示动作列表遇到 return 或等价中断，调用方应停止后续流程。
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
        val start = if (baseDelay > 0) delayTicks(player, baseDelay) else CompletableFuture.completedFuture(false)
        return start.thenCompose { executeActionSequence(context, actionList) }
            .exceptionally { error ->
                plugin?.logger?.severe("动作执行失败: ${error.message}")
                error.printStackTrace()
                false
            }
    }

    /**
     * 执行菜单或周期任务传入的一组动作。
     *
     * 与 [executeStandaloneActions] 不同，这里有菜单配置，因此支持菜单本地 actions 包、
     * reset、Close 生命周期、Events.Tasks 上下文等菜单相关能力。
     */
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
                KaScheduler.runPlayer(p, Runnable {
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

    /**
     * 执行脱离菜单配置的动作列表。
     *
     * 用于自定义指令等场景。由于没有菜单配置，菜单本地 actions、reset 和 Close 事件不可用，
     * 但全局 actions 包、内置动作、外部动作和变量仍可使用。
     */
    fun executeStandaloneActions(
        player: Player,
        actions: List<*>,
        variables: Map<String, String> = emptyMap(),
        asyncDataOperations: Boolean = true
    ): CompletableFuture<Boolean> {
        val menuOpener: (Player, String) -> Unit = { p, menuName ->
            val kaMenu = Bukkit.getPluginManager().getPlugin("KaMenu") as? KaMenu
            if (kaMenu != null) {
                KaScheduler.runPlayer(p, Runnable {
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

    /**
     * 递归执行动作序列。
     *
     * 这里不用简单 for 循环，是因为 `wait:` 会异步完成；递归链可以保证等待后继续下一个节点。
     */
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

    /**
     * 执行单个 YAML 动作节点。
     *
     * Map 表示条件分支，List 表示嵌套动作列表，String 表示普通动作文本。
     */
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

    /**
     * 提取单行动作修饰符。
     *
     * 支持行尾 `{condition: ...}`、`{chance: ...}` 和 `{wait: ...}`；
     * 修饰符会在动作交给变量解析器前移除，避免被当作内置变量或 MiniMessage 标签。
     */
    private fun parseActionModifiers(action: String): ActionModifiers {
        var remaining = action.trimEnd()
        var condition: String? = null
        var chance: String? = null
        var delay: String? = null
        while (remaining.isNotEmpty()) {
            val parsedCondition = InlineConditionResolver.parse(remaining)
            if (parsedCondition != null) {
                condition = parsedCondition.condition
                remaining = parsedCondition.content
                continue
            }
            val parsedChance = InlineConditionResolver.parseTrailingModifier(remaining, "chance")
            if (parsedChance != null) {
                chance = parsedChance.second
                remaining = parsedChance.first
                continue
            }
            val parsedWait = InlineConditionResolver.parseTrailingModifier(remaining, "wait")
            if (parsedWait != null) {
                delay = parsedWait.second
                remaining = parsedWait.first
                continue
            }
            break
        }
        return ActionModifiers(
            action = remaining.trim(),
            condition = condition,
            chance = chance,
            delay = delay
        )
    }

    /** 解析并判定 `0..100` 概率；非法值会跳过该动作并输出本地化警告。 */
    private fun passesActionChance(context: ActionExecutionContext, rawChance: String?): Boolean {
        if (rawChance == null) return true
        val resolved = TextResolver.resolve(context.player, rawChance, context.variables, context.config).trim()
        val chance = resolved.removeSuffix("%").trim().toDoubleOrNull()
        if (chance == null || !chance.isFinite()) {
            plugin?.logger?.warning(message("actions.modifier_invalid_chance", resolved, context.player.name))
            return false
        }
        if (chance <= 0.0) return false
        if (chance >= 100.0) return true
        return ThreadLocalRandom.current().nextDouble(100.0) < chance
    }

    /** 解析单行动作延迟 tick；非法值按不延迟处理并输出本地化警告。 */
    private fun parseActionDelay(context: ActionExecutionContext, rawDelay: String?): Long {
        if (rawDelay == null) return 0L
        val resolved = TextResolver.resolve(context.player, rawDelay, context.variables, context.config).trim()
        val delay = resolved.toLongOrNull()
        if (delay == null || delay < 0L) {
            plugin?.logger?.warning(message("actions.modifier_invalid_delay", resolved, context.player.name))
            return 0L
        }
        return delay
    }

    /**
     * 独立调度一行动作，并立即把控制权交还当前动作序列。
     *
     * 这与 `wait:` 不同：延迟动作不会阻塞后续行，也不会把延迟后的 `return` 传播回原动作链。
     */
    private fun scheduleDetachedAction(context: ActionExecutionContext, action: String, delay: Long) {
        KaScheduler.runPlayerLater(context.player, delay, Runnable {
            if (!context.player.isOnline) return@Runnable
            executeActionString(context, action).whenComplete { _, error ->
                if (error != null) {
                    plugin?.logger?.warning(message(
                        "actions.delayed_action_failed",
                        context.player.name,
                        error.message ?: error.javaClass.simpleName
                    ))
                }
            }
        })
    }

    /**
     * 执行字符串动作中的控制指令。
     *
     * `wait`、`return`、`actions`、`page`、`stop-current-task` 会影响执行序列本身，
     * 其他动作会交给 [executeSingleAction] 执行。
     */
    private fun executeActionString(
        context: ActionExecutionContext,
        action: String
    ): CompletableFuture<Boolean> {
        val modifiers = parseActionModifiers(action)
        if (modifiers.action.isEmpty() ||
            (modifiers.condition != null && !ConditionExpressionEngine.checkCondition(
                context.player,
                modifiers.condition,
                context.variables,
                context.config
            ) { null }) ||
            !passesActionChance(context, modifiers.chance)
        ) {
            return CompletableFuture.completedFuture(false)
        }
        val actionDelay = parseActionDelay(context, modifiers.delay)
        if (actionDelay > 0L) {
            scheduleDetachedAction(context, modifiers.action, actionDelay)
            return CompletableFuture.completedFuture(false)
        }

        val controlAction = TextResolver.resolve(
            context.player,
            modifiers.action,
            context.variables,
            context.config
        ).trim()

        return when {
            controlAction.startsWith("wait:", ignoreCase = true) -> {
                val ticks = controlAction.substringAfter(":", "").trim().toLongOrNull() ?: 0L
                delayTicks(context.player, ticks).thenApply { false }
            }
            controlAction.equals("refresh", ignoreCase = true) ||
                controlAction.startsWith("refresh:", ignoreCase = true) -> {
                val target = controlAction.substringAfter(":", "").trim()
                plugin?.takeIf { it.containerMenusReady }
                    ?.containerMenuService
                    ?.refreshFromAction(context.player, target)
                CompletableFuture.completedFuture(false)
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
                        MenuUI.sendMessage(context.player, TextParser.parseText(actionListNotFoundMessage(actionCall.name, context.config)))
                        CompletableFuture.completedFuture(false)
                    } else if (subActionList.id == context.actionListId) {
                        MenuUI.sendMessage(context.player, TextParser.parseText(plugin?.languageManager?.getMessage("actions.action_list_self_call", actionCall.name)))
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
                    modifiers.action,
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

    private fun delayTicks(player: Player, ticks: Long): CompletableFuture<Boolean> {
        if (ticks <= 0) {
            return CompletableFuture.completedFuture(false)
        }

        val future = CompletableFuture<Boolean>()
        KaScheduler.runPlayerLater(player, ticks, Runnable {
            future.complete(false)
        })
        return future
    }

    /**
     * 处理 repeat 按钮分页动作。
     *
     * 语法：`page: <listId> next|prev|+N|-N|pageNumber`。
     */
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
     *
     * Open 事件必须等待整个动作列表完成；如果中途遇到 `return`，调用方应停止打开菜单。
     * 事件动作没有输入组件响应，因此不能读取输入捕获变量。
     *
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
                KaScheduler.runPlayer(p, Runnable {
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
     *
     * 用于调用方明确需要同步结果的场景。若动作内包含 wait，仍会通过 future 链等待完成。
     *
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
                KaScheduler.runPlayer(p, Runnable {
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
     * 检查动作列表中是否包含 wait 动作。
     *
     * 主要用于旧逻辑兼容和判断是否需要异步等待事件结果。
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
     * 执行单个动作（支持目标选择器）。
     *
     * 目标选择器形如 `{player:*}` 或 `{player:condition}`，只有多目标安全的动作会扩散到多个玩家。
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
     * 对单个玩家执行动作。
     *
     * 这里是内置动作分发表。进入此方法前会完成变量解析；若命中外部 namespace handler，
     * 外部 handler 会优先消费动作，返回后不再执行内置逻辑。
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

        val pointsAlias = parsePointsAlias(finalCmd)

        when {
            // tell: 普通消息
            finalCmd.startsWith("tell:") ->
                MenuUI.sendMessage(player, TextParser.parseText(finalCmd.removePrefix("tell:").trim()))

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
                        MenuUI.sendMessage(player, TextParser.parseText(message("javascript.execution_failed_user", error)))
                    }
                } else {
                    MenuUI.sendMessage(player, TextParser.parseText(message("javascript.unavailable")))
                }
            }

            // actionbar: ActionBar 消息
            finalCmd.startsWith("actionbar:") -> {
                val message = finalCmd.removePrefix("actionbar:").trim()
                MenuUI.sendActionBar(player, TextParser.parseText(message))
            }

            // title: 发送标题
            finalCmd.startsWith("title:") -> {
                val args = finalCmd.removePrefix("title:").trim()
                KaScheduler.runPlayer(player, Runnable {
                    ActionHandlers.parseAndSendTitle(player, args)
                })
            }

            // hovertext: 可点击文本
            finalCmd.startsWith("hovertext:") -> {
                val text = finalCmd.removePrefix("hovertext:").trim()
                MenuUI.sendClickableText(player, text, config, contextId)
            }

            // command: 玩家执行指令
            finalCmd.startsWith("command:") -> {
                val cmd = finalCmd.removePrefix("command:").trim()
                KaScheduler.runPlayer(player, Runnable {
                    player.performCommand(cmd)
                })
            }

            // chat: 玩家执行指令
            finalCmd.startsWith("chat:") -> {
                val cmd = finalCmd.removePrefix("chat:").trim()
                KaScheduler.runPlayer(player, Runnable {
                    player.chat(cmd)
                })
            }

            // console: 控制台执行指令
            finalCmd.startsWith("console:") -> {
                val cmd = finalCmd.removePrefix("console:").trim()
                KaScheduler.runGlobal(Runnable {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
                })
            }

            // sound: 播放声音 (支持音量和音调参数)
            finalCmd.startsWith("sound:") -> {
                val args = finalCmd.removePrefix("sound:").trim()
                KaScheduler.runPlayer(player, Runnable {
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
                val request = parseMenuOpenRequest(finalCmd.removePrefix("open:").trim())
                if (request != null) {
                    handledMenuLifecycle?.set(true)
                    val kaMenu = plugin
                    if (kaMenu != null) {
                        KaScheduler.runPlayer(player, Runnable {
                            MenuUI.openMenu(
                                player,
                                request.menuId,
                                kaMenu.menuManager,
                                kaMenu,
                                request.arguments,
                                variables
                            )
                        })
                    } else if (request.arguments.isEmpty()) {
                        menuOpener(player, request.menuId)
                    }
                }
            }

            // force-open: 强制打开菜单（不执行 Events.Open）
            finalCmd.startsWith("force-open:") -> {
                val request = parseMenuOpenRequest(finalCmd.removePrefix("force-open:").trim())
                val kaMenu = plugin
                if (request != null && kaMenu != null) {
                    handledMenuLifecycle?.set(true)
                    KaScheduler.runPlayer(player, Runnable {
                        MenuUI.forceOpenMenu(
                            player,
                            request.menuId,
                            kaMenu.menuManager,
                            kaMenu,
                            request.arguments,
                            variables
                        )
                    })
                }
            }

            // reset: 重新打开当前菜单（不执行 Events.Open）
            finalCmd.trim() == "reset" -> {
                if (config != null) {
                    val kaMenu = Bukkit.getPluginManager().getPlugin("KaMenu") as? KaMenu
                    if (kaMenu != null) {
                        val currentMenuId = kaMenu.menuManager.getMenuId(config)
                        val currentArguments = MenuArgumentManager.current(player)
                        if (currentMenuId != null) {
                            KaScheduler.runPlayer(player, Runnable {
                                handledMenuLifecycle?.set(true)
                                MenuUI.forceOpenMenu(
                                    player,
                                    currentMenuId,
                                    kaMenu.menuManager,
                                    kaMenu,
                                    currentArguments
                                )
                            })
                        } else {
                            KaScheduler.runPlayer(player, Runnable {
                                handledMenuLifecycle?.set(true)
                                MenuUI.forceOpenConfig(
                                    player,
                                    config,
                                    kaMenu,
                                    contextId ?: "external",
                                    currentArguments
                                )
                            })
                        }
                    }
                }
            }

            // set-args: 替换当前菜单参数，并刷新当前 Container 或 Dialog
            finalCmd.startsWith("set-args:", ignoreCase = true) -> {
                val arguments = ActionArgumentParser.splitArguments(finalCmd.substringAfter(":", ""))
                replaceMenuArgumentsAndRefresh(
                    player,
                    arguments,
                    config,
                    contextId,
                    handledMenuLifecycle
                )
            }

            // del-args: 清理当前菜单参数，不主动刷新菜单
            finalCmd.equals("del-args", ignoreCase = true) -> MenuArgumentManager.clear(player)

            // force-close: 强制关闭菜单（不执行 Events.Close）
            finalCmd.trim() == "force-close" -> {
                handledMenuLifecycle?.set(true)
                KaScheduler.runPlayer(player, Runnable {
                    DialogSessionManager.cancel(player)
                    MenuTaskManager.cancel(player)
                    MenuUI.closeDialog(player)
                    MenuArgumentManager.clear(player)
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
                        val argumentContext = MenuArgumentManager.currentContext(player)
                        val containerSessionId = plugin
                            ?.takeIf { it.containerMenusReady }
                            ?.containerMenuService
                            ?.currentSessionId(player)
                        // 异步执行 Close 事件，不等待结果（避免阻塞）
                        executeEvent(player, config, "Close").whenComplete { result, error ->
                            if (error != null) {
                                plugin?.logger?.severe("Close 事件执行失败: ${error.message}")
                                error.printStackTrace()
                            } else if (!result) {
                                // Close 事件中没有 return，关闭菜单
                                KaScheduler.runPlayer(player, Runnable {
                                    if (containerSessionId != null) {
                                        if (plugin?.containerMenuService?.closeSilentlyIfCurrent(player, containerSessionId) == true) {
                                            MenuArgumentManager.clearIfCurrent(player, argumentContext)
                                        }
                                        return@Runnable
                                    }
                                    DialogSessionManager.cancel(player)
                                    MenuTaskManager.cancel(player)
                                    MenuUI.closeDialog(player)
                                    MenuArgumentManager.clearIfCurrent(player, argumentContext)
                                })
                            } else if (containerSessionId != null) {
                                // Container 的 Close 被 return 拦截时保留当前会话，并重新解析完整显示状态。
                                KaScheduler.runPlayer(player, Runnable {
                                    val service = plugin?.containerMenuService ?: return@Runnable
                                    if (service.currentSessionId(player) == containerSessionId) {
                                        service.refreshFromAction(player, "*")
                                    }
                                })
                            }
                        }
                        return  // 提前返回，不在这里关闭菜单
                    }
                }
                // 没有 Close 事件，直接关闭菜单
                val argumentContext = MenuArgumentManager.currentContext(player)
                KaScheduler.runPlayer(player, Runnable {
                    val containerSessionId = plugin
                        ?.takeIf { it.containerMenusReady }
                        ?.containerMenuService
                        ?.currentSessionId(player)
                    if (containerSessionId != null) {
                        if (plugin?.containerMenuService?.closeSilentlyIfCurrent(player, containerSessionId) == true) {
                            MenuArgumentManager.clearIfCurrent(player, argumentContext)
                        }
                        return@Runnable
                    }
                    DialogSessionManager.cancel(player)
                    MenuTaskManager.cancel(player)
                    MenuUI.closeDialog(player)
                    MenuArgumentManager.clearIfCurrent(player, argumentContext)
                })
            }

            // actions: 执行 Events.Click 下的动作列表
            finalCmd.startsWith("actions:") -> {
                if (config != null) {
                    val actionCall = parseActionCall(finalCmd.removePrefix("actions:").trim())
                    if (actionCall.name.isNotEmpty()) {
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
                            MenuUI.sendMessage(player, TextParser.parseText(actionListNotFoundMessage(actionCall.name, config)))
                        }
                    }
                }
            }

            // set-data: 设置玩家数据
            finalCmd.startsWith("set-data:") -> {
                val args = finalCmd.removePrefix("set-data:").trim()
                ActionHandlers.parseDataAction(args, player.uniqueId.toString(), "data") { uuid, key, value ->
                    if (asyncDataOperations) {
                        // 异步执行数据库操作，避免阻塞主线程
                        KaScheduler.runAsync(Runnable {
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
                            KaScheduler.runAsync(Runnable {
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
                            KaScheduler.runAsync(Runnable {
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
                            KaScheduler.runAsync(Runnable {
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
                            KaScheduler.runAsync(Runnable {
                                databaseManager?.setPlayerList(player.uniqueId, key, values)
                            })
                        } else {
                            databaseManager?.setPlayerList(player.uniqueId, key, values)
                        }
                    },
                    addAction = { key, values, unique ->
                        if (asyncDataOperations) {
                            KaScheduler.runAsync(Runnable {
                                databaseManager?.addPlayerListValues(player.uniqueId, key, values, unique)
                            })
                        } else {
                            databaseManager?.addPlayerListValues(player.uniqueId, key, values, unique)
                        }
                    },
                    removeAction = { key, values ->
                        if (asyncDataOperations) {
                            KaScheduler.runAsync(Runnable {
                                databaseManager?.removePlayerListValues(player.uniqueId, key, values)
                            })
                        } else {
                            databaseManager?.removePlayerListValues(player.uniqueId, key, values)
                        }
                    },
                    clearAction = { key ->
                        if (asyncDataOperations) {
                            KaScheduler.runAsync(Runnable {
                                databaseManager?.clearPlayerList(player.uniqueId, key)
                            })
                        } else {
                            databaseManager?.clearPlayerList(player.uniqueId, key)
                        }
                    },
                    deleteAction = { key ->
                        if (asyncDataOperations) {
                            KaScheduler.runAsync(Runnable {
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
                            KaScheduler.runAsync(Runnable {
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
                            KaScheduler.runAsync(Runnable {
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
                            KaScheduler.runAsync(Runnable {
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
                            KaScheduler.runAsync(Runnable {
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
                            KaScheduler.runAsync(Runnable {
                                databaseManager?.setGlobalList(key, values)
                            })
                        } else {
                            databaseManager?.setGlobalList(key, values)
                        }
                    },
                    addAction = { key, values, unique ->
                        if (asyncDataOperations) {
                            KaScheduler.runAsync(Runnable {
                                databaseManager?.addGlobalListValues(key, values, unique)
                            })
                        } else {
                            databaseManager?.addGlobalListValues(key, values, unique)
                        }
                    },
                    removeAction = { key, values ->
                        if (asyncDataOperations) {
                            KaScheduler.runAsync(Runnable {
                                databaseManager?.removeGlobalListValues(key, values)
                            })
                        } else {
                            databaseManager?.removeGlobalListValues(key, values)
                        }
                    },
                    clearAction = { key ->
                        if (asyncDataOperations) {
                            KaScheduler.runAsync(Runnable {
                                databaseManager?.clearGlobalList(key)
                            })
                        } else {
                            databaseManager?.clearGlobalList(key)
                        }
                    },
                    deleteAction = { key ->
                        if (asyncDataOperations) {
                            KaScheduler.runAsync(Runnable {
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
                        KaScheduler.runAsync(Runnable {
                            databaseManager?.setGlobalData(key, value)
                        })
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
                KaScheduler.runPlayer(player, Runnable {
                    ActionHandlers.parseAndSendToast(player, args)
                })
            }

            // money: 操作玩家金币
            finalCmd.startsWith("money:") -> {
                val args = finalCmd.removePrefix("money:").trim()
                KaScheduler.runPlayer(player, Runnable {
                    ActionHandlers.parseAndHandleMoney(player, args, variables)
                })
            }

            // points: 使用 PlayerPoints 增减点券
            finalCmd.startsWith("points:") -> {
                val args = finalCmd.removePrefix("points:").trim()
                KaScheduler.runPlayer(player, Runnable {
                    ActionHandlers.parseAndHandlePoints(player, args, variables)
                })
            }

            // add-points/take-points 等 源菜单 兼容别名
            pointsAlias != null -> {
                KaScheduler.runPlayer(player, Runnable {
                    ActionHandlers.parseAndHandlePoints(
                        player,
                        pointsAlias.second,
                        variables,
                        forcedType = pointsAlias.first
                    )
                })
            }

            // stock-item: 物品给予/扣除
            finalCmd.startsWith("stock-item:") -> {
                val args = finalCmd.removePrefix("stock-item:").trim()
                KaScheduler.runPlayer(player, Runnable {
                    ActionHandlers.parseAndHandleStockItem(player, args, variables)
                })
            }

            // item: 普通物品给予/扣除
            finalCmd.startsWith("item:") -> {
                val args = finalCmd.removePrefix("item:").trim()
                KaScheduler.runPlayer(player, Runnable {
                    ActionHandlers.parseAndHandleItem(player, args, variables)
                })
            }

            // server: 传送到指定服务器（支持 BungeeCord/Velocity）
            finalCmd.startsWith("server:") -> {
                val serverName = finalCmd.removePrefix("server:").trim()
                KaScheduler.runPlayer(player, Runnable {
                    ActionHandlers.parseAndHandleServer(player, serverName)
                })
            }

            // tppos: 传送到指定坐标
            finalCmd.startsWith("tppos:") -> {
                val args = finalCmd.removePrefix("tppos:").trim()
                KaScheduler.runPlayer(player, Runnable {
                    ActionHandlers.parseAndHandleTppos(player, args)
                })
            }
        }
    }

    /** 替换当前菜单参数，并按当前 UI 类型选择原地刷新或强制重开。 */
    private fun replaceMenuArgumentsAndRefresh(
        player: Player,
        arguments: List<String>,
        config: YamlConfiguration?,
        contextId: String?,
        handledMenuLifecycle: AtomicBoolean?
    ) {
        MenuArgumentManager.activate(player, arguments)
        val kaMenu = plugin ?: return
        val containerService = kaMenu.takeIf { it.containerMenusReady }?.containerMenuService
        if (containerService?.currentSessionId(player) != null) {
            containerService.refreshFromAction(player, "*")
            return
        }
        if (config == null) return

        handledMenuLifecycle?.set(true)
        val menuId = kaMenu.menuManager.getMenuId(config)
        KaScheduler.runPlayer(player, Runnable {
            if (menuId != null) {
                MenuUI.forceOpenMenu(player, menuId, kaMenu.menuManager, kaMenu, arguments)
            } else {
                MenuUI.forceOpenConfig(player, config, kaMenu, contextId ?: "external", arguments)
            }
        })
    }

    /**
     * 执行测试动作（用于 /kamenu action 指令）
     * @param player 玩家对象
     * @param actionString 动作字符串
     * @return 是否成功执行
     */
    fun executeTestAction(player: Player, actionString: String): Boolean {
        if (plugin == null) {
            MenuUI.sendMessage(player, TextParser.parseText(languageManager?.getMessage("actions.test_failed", "插件未初始化") ?: "§c插件未初始化，无法执行动作"))
            return false
        }

        try {
            val menuOpener: (Player, String) -> Unit = { p, menuName ->
                KaScheduler.runPlayer(p, Runnable {
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
            MenuUI.sendMessage(player, TextParser.parseText(e.message?.let { languageManager?.getMessage("actions.test_failed", it) } ?: "§c动作执行失败: ${e.message}"))
            plugin?.logger?.severe("测试动作执行失败: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * 解析可点击文本 (使用 Adventure API)
     * 格式: <text='显示文字';hover='悬停文字';hover_item='物品来源';actions='动作列表路径';copy='复制文本';command='指令';url='链接';newline='false'>
     * 注意: 只有包含 text= 参数的标签才会被解析为可点击文本，其他的 <...> 标签会被保留给 MiniMessage 处理
     */
    fun parseClickableText(rawText: String): Component {
        return parseClickableText(rawText, null, null, null)
    }

    /**
     * 解析可点击文本 (使用 Adventure API) - 带上下文版本
     * 格式: <text='显示文字';hover='悬停文字';actions='动作列表路径';copy='复制文本';command='指令';url='链接';newline='false'>
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
        val forceOraxenResolver = OraxenTextAdapter.containsGlyphTag(rawText)
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
            return TextParser.parseText(rawText, player, forceOraxenResolver)
        }

        // 按位置升序排序，从前往后处理
        replacements.sortBy { it.first.first }

        // 按顺序拼接：MiniMessage 文本 + hovertext 组件
        val mainBuilder = Component.text()
        var lastEnd = 0

        replacements.forEach { (range, component) ->
            // 添加 hovertext 之前的文本（包含 MiniMessage）
            if (range.first > lastEnd) {
                mainBuilder.append(TextParser.parseText(rawText.substring(lastEnd, range.first), player, forceOraxenResolver))
            }
            // 添加 hovertext 组件
            mainBuilder.append(component)
            lastEnd = range.last + 1
        }

        // 添加最后剩余的文本
        if (lastEnd < rawText.length) {
            mainBuilder.append(TextParser.parseText(rawText.substring(lastEnd), player, forceOraxenResolver))
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
        var hoverItem = ""
        var command = ""
        var copy = ""
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
                    "hover_item", "hover-item" -> hoverItem = value.removeSurrounding("`").removeSurrounding("'").removeSurrounding("\"")
                    "command" -> command = value.removeSurrounding("`").removeSurrounding("'").removeSurrounding("\"")
                    "copy" -> copy = value.removeSurrounding("`").removeSurrounding("'").removeSurrounding("\"")
                    "url" -> url = value.removeSurrounding("`").removeSurrounding("'").removeSurrounding("\"")
                    "actions" -> actions = value.removeSurrounding("`").removeSurrounding("'").removeSurrounding("\"")
                    "newline" -> newline = value.removeSurrounding("`").removeSurrounding("'").removeSurrounding("\"").equals("true", ignoreCase = true)
                }
            }
        }

        // 只有包含 text= 参数且 text 不为空时才返回组件
        return if (hasTextParam && text.isNotEmpty()) {
            var component = createAdventureClickableTextInternal(
                text,
                hover,
                command,
                copy,
                url,
                actions,
                newline,
                player,
                config,
                menuOpener
            )
            if (player != null && hoverItem.isNotBlank()) {
                resolveHoverItem(player, hoverItem)?.let { item ->
                    component = component.hoverEvent(MenuUI.itemHover(item))
                }
            }
            component
        } else {
            null
        }
    }

    /**
     * 将可点击文本的 hover_item 来源解析为完整 ItemStack。
     *
     * 支持主副手、背包槽位、四个护甲槽、保存物品和基础材质；返回克隆物品，避免悬浮展示修改原物品。
     */
    fun resolveHoverItem(player: Player, rawSource: String): org.bukkit.inventory.ItemStack? {
        val source = rawSource.trim()
        val lowerSource = source.lowercase()
        val item = when {
            lowerSource == "hand" || lowerSource == "mainhand" || lowerSource == "main_hand" ->
                player.inventory.itemInMainHand

            lowerSource == "offhand" || lowerSource == "off_hand" ->
                player.inventory.itemInOffHand

            lowerSource == "helmet" || lowerSource == "head" || lowerSource == "armor:helmet" ->
                player.inventory.helmet

            lowerSource == "chestplate" || lowerSource == "chest" || lowerSource == "armor:chestplate" ->
                player.inventory.chestplate

            lowerSource == "leggings" || lowerSource == "legs" || lowerSource == "armor:leggings" ->
                player.inventory.leggings

            lowerSource == "boots" || lowerSource == "feet" || lowerSource == "armor:boots" ->
                player.inventory.boots

            lowerSource.startsWith("slot:") -> {
                val slot = source.substringAfter(':').trim().toIntOrNull()
                slot?.takeIf { it in 0 until player.inventory.size }
                    ?.let { player.inventory.getItem(it) }
            }

            lowerSource.startsWith("stock:") -> {
                val itemName = source.substringAfter(':').trim()
                itemName.takeIf { it.isNotEmpty() }?.let { itemManager?.getItem(it) }
            }

            lowerSource.startsWith("material:") -> {
                val materialName = source.substringAfter(':').trim()
                ExternalItemAdapter.create(materialName, player = player)
                    ?: MaterialUtils.matchMaterial(materialName)?.let { org.bukkit.inventory.ItemStack(it) }
            }

            else -> ExternalItemAdapter.create(source, player = player)
        }

        return item
            ?.takeIf { it.amount > 0 && !it.type.isAir }
            ?.clone()
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
    ): Component = createAdventureClickableTextInternal(
        text,
        hoverText,
        command,
        "",
        url,
        actions,
        newline,
        player,
        config,
        menuOpener
    )

    /** 创建包含 copy 点击行为的可点击文本，并统一处理互斥点击动作。 */
    private fun createAdventureClickableTextInternal(
        text: String,
        hoverText: String,
        command: String,
        copy: String,
        url: String,
        actions: String,
        newline: Boolean,
        player: Player?,
        config: YamlConfiguration?,
        menuOpener: ((Player, String) -> Unit)?
    ): Component {
        var component = TextParser.parseText(text, player)

        // 添加点击事件
        if (actions.isNotEmpty()) {
            // 使用 ClickCallback 执行动作列表
            if (player != null && config != null && menuOpener != null) {
                component = component.clickEvent(ClickEvent.callback({ audience ->
                    if (audience is Player) {
                        val actionCall = parseActionCall(actions)
                        val actionList = findActionList(config, actionCall.name)

                        if (actionList != null) {
                            KaScheduler.runPlayer(audience, Runnable {
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
                        } else {
                            MenuUI.sendMessage(audience, TextParser.parseText(actionListNotFoundMessage(actionCall.name, config)))
                        }
                    }
                }, buildCallbackOptions(config)))
            }
        } else if (copy.isNotEmpty()) {
            component = component.clickEvent(ClickEvent.copyToClipboard(copy))
        } else if (command.isNotEmpty()) {
            component = component.clickEvent(ClickEvent.runCommand(command))
        } else if (url.isNotEmpty()) {
            component = component.clickEvent(ClickEvent.openUrl(url))
        }

        // 添加悬停事件
        if (hoverText.isNotEmpty()) {
            component = component.hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(TextParser.parseText(hoverText, player)))
        }

        // 添加换行
        if (newline) {
            component = component.append(Component.newline())
        }

        return component
    }
}
