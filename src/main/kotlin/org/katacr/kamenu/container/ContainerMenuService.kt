@file:Suppress("DEPRECATION", "UnstableApiUsage")

package org.katacr.kamenu.container

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.AnvilInventory
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.InventoryView
import org.bukkit.persistence.PersistentDataType
import org.katacr.kamenu.DialogSessionManager
import org.katacr.kamenu.InputCaptureUtils
import org.katacr.kamenu.KaMenu
import org.katacr.kamenu.KaScheduler
import org.katacr.kamenu.KaTaskHandle
import org.katacr.kamenu.MenuArgumentManager
import org.katacr.kamenu.MenuActions
import org.katacr.kamenu.MenuListManager
import org.katacr.kamenu.MenuRequirementChecker
import org.katacr.kamenu.MenuTaskManager
import org.katacr.kamenu.MenuUI
import org.katacr.kamenu.TextParser
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

/**
 * Container 菜单运行时服务。
 *
 * 负责打开只读 Bukkit 库存、维护玩家会话、执行点击动作与菜单事件，并调度整体、标题和按钮刷新。
 */
class ContainerMenuService(private val plugin: KaMenu) {
    /** 一次待打开的库存及其可选真实视图能力。 */
    private data class InventoryWindow(
        val inventory: Inventory,
        val view: InventoryView? = null,
        val alreadyOpen: Boolean = false,
        val supportsAnvilInput: Boolean = false
    )

    private data class ActiveSession(
        val sessionId: UUID,
        val playerId: UUID,
        val menuId: String,
        val generation: Long,
        val config: YamlConfiguration,
        val definition: ContainerMenuDefinition,
        val holder: ContainerMenuHolder,
        val supportsAnvilInput: Boolean,
        val refreshHandles: MutableList<KaTaskHandle> = mutableListOf(),
        val refreshRunning: AtomicBoolean = AtomicBoolean(false),
        val lastClickAtMillis: AtomicLong = AtomicLong(0L),
        @Volatile var titleFrame: Int = 0,
        @Volatile var currentTitle: String = "",
        @Volatile var rebindingTitle: Boolean = false,
        @Volatile var anvilInput: String = "",
        @Volatile var anvilInputInitialized: Boolean = false,
        @Volatile var furnaceWarningLogged: Boolean = false,
        val progressStates: MutableMap<String, ProgressWatcherState> = mutableMapOf()
    )

    /** 记录单个进度监听器在当前菜单会话中的边沿触发状态。 */
    private data class ProgressWatcherState(
        var initialized: Boolean = false,
        var previousValue: Int = 0,
        var matched: Boolean = false
    )

    private val sessions = ConcurrentHashMap<UUID, ActiveSession>()
    private val itemRenderer = ContainerItemRenderer(plugin)
    private val displayMarkerKey = NamespacedKey(plugin, "container_display")

    /** 普通打开已加载容器菜单，并执行依赖检查和 Events.Open。 */
    fun openMenu(player: Player, menuId: String) {
        openMenu(player, menuId, executeOpenEvent = true)
    }

    /** 强制重新打开已加载容器菜单，不重复执行 Events.Open。 */
    fun forceOpenMenu(player: Player, menuId: String) {
        openMenu(player, menuId, executeOpenEvent = false)
    }

    /** 普通打开外部或内置资源提供的内存 Container 配置。 */
    fun openConfig(player: Player, config: YamlConfiguration, contextId: String) {
        openConfig(player, config, contextId, executeOpenEvent = true)
    }

    /** 强制重新打开内存 Container 配置，不重复执行 Events.Open。 */
    fun forceOpenConfig(player: Player, config: YamlConfiguration, contextId: String) {
        openConfig(player, config, contextId, executeOpenEvent = false)
    }

    /** 在玩家所属线程启动容器菜单打开流程。 */
    private fun openMenu(player: Player, menuId: String, executeOpenEvent: Boolean) {
        runOnPlayerThread(player) {
            val definition = plugin.menuManager.getContainerMenu(menuId)
            val config = plugin.menuManager.getMenuConfig(menuId)
            if (definition == null || config == null) {
                player.sendMessage(plugin.languageManager.getMessage("menu.not_found", menuId))
                return@runOnPlayerThread
            }
            if (!MenuRequirementChecker.check(player, config, plugin)) {
                return@runOnPlayerThread
            }

            if (!executeOpenEvent || config.getList("Events.Open").isNullOrEmpty()) {
                render(player, menuId, config, definition)
                return@runOnPlayerThread
            }
            MenuActions.executeEvent(player, config, "Open", menuId).whenComplete { shouldStop, error ->
                if (error != null) {
                    plugin.logger.severe(
                        plugin.languageManager.getMessage("container.open_event_failed", menuId, error.message.toString())
                    )
                    error.printStackTrace()
                } else if (shouldStop != true) {
                    runOnPlayerThread(player) {
                        val currentDefinition = plugin.menuManager.getContainerMenu(menuId)
                        val currentConfig = plugin.menuManager.getMenuConfig(menuId)
                        if (currentDefinition != null && currentConfig != null) {
                            render(player, menuId, currentConfig, currentDefinition)
                        }
                    }
                }
            }
        }
    }

    /** 解析内存配置并在玩家所属线程启动 Container 打开流程。 */
    private fun openConfig(
        player: Player,
        config: YamlConfiguration,
        contextId: String,
        executeOpenEvent: Boolean
    ) {
        val parseResult = ContainerMenuParser.parse(contextId, config)
        val definition = parseResult.definition
        if (!parseResult.succeeded || definition == null) {
            parseResult.diagnostics.forEach { diagnostic ->
                val key = when (diagnostic.severity) {
                    ContainerDiagnosticSeverity.WARNING -> "manager.container_diagnostic_warning"
                    ContainerDiagnosticSeverity.ERROR -> "manager.container_diagnostic_error"
                }
                val message = plugin.languageManager.getMessage(
                    key,
                    contextId,
                    diagnostic.code,
                    diagnostic.path,
                    diagnostic.message
                )
                when (diagnostic.severity) {
                    ContainerDiagnosticSeverity.WARNING -> plugin.logger.warning(message)
                    ContainerDiagnosticSeverity.ERROR -> plugin.logger.severe(message)
                }
            }
            player.sendMessage(plugin.languageManager.getMessage("container.config_invalid", contextId))
            return
        }

        runOnPlayerThread(player) {
            if (!MenuRequirementChecker.check(player, config, plugin)) {
                return@runOnPlayerThread
            }
            if (!executeOpenEvent || config.getList("Events.Open").isNullOrEmpty()) {
                render(player, contextId, config, definition)
                return@runOnPlayerThread
            }
            MenuActions.executeEvent(player, config, "Open", contextId).whenComplete { shouldStop, error ->
                if (error != null) {
                    plugin.logger.severe(
                        plugin.languageManager.getMessage(
                            "container.open_event_failed",
                            contextId,
                            error.message.toString()
                        )
                    )
                    error.printStackTrace()
                } else if (shouldStop != true) {
                    runOnPlayerThread(player) {
                        render(player, contextId, config, definition)
                    }
                }
            }
        }
    }

    /** 创建库存、渲染初始内容并挂载菜单任务与刷新任务。 */
    private fun render(
        player: Player,
        menuId: String,
        config: YamlConfiguration,
        definition: ContainerMenuDefinition
    ) {
        if (!player.isOnline || !MenuRequirementChecker.check(player, config, plugin)) return
        terminate(player.uniqueId, closeInventory = false, clearInventory = true)
        DialogSessionManager.cancel(player)

        val generation = plugin.menuManager.getGeneration()
        val sessionId = UUID.randomUUID()
        val holder = ContainerMenuHolder(sessionId, player.uniqueId, menuId, generation)
        val initialInput = resolveInitialAnvilInput(player, config, definition)
        val initialVariables = if (definition.type == ContainerMenuType.ANVIL) {
            mapOf("input" to initialInput)
        } else {
            emptyMap()
        }
        val initialTitle = resolveTitle(player, config, definition, 0, initialVariables)
        val window = createInventoryWindow(player, holder, definition, initialTitle)
        holder.bindInventory(window.inventory)
        val session = ActiveSession(
            sessionId = sessionId,
            playerId = player.uniqueId,
            menuId = menuId,
            generation = generation,
            config = config,
            definition = definition,
            holder = holder,
            supportsAnvilInput = window.supportsAnvilInput,
            anvilInput = initialInput,
            anvilInputInitialized = definition.properties.contains("input"),
            currentTitle = initialTitle
        )
        sessions[player.uniqueId] = session

        try {
            refreshButtons(player, session, definition.buttons.keys)
            initializeAnvilInputItem(player, session)
            openWindow(player, window)
            bindOpenedInventory(player, session.holder)
            applyViewProperties(player, session)
            refreshAnvilResultSlot(player, session)
            if (definition.type == ContainerMenuType.ANVIL && !window.supportsAnvilInput) {
                plugin.logger.warning(plugin.languageManager.getMessage("container.anvil_input_unsupported", menuId))
            }
            MenuTaskManager.attachMenu(player, config, menuId)
            scheduleRefreshes(player, session)
        } catch (error: RuntimeException) {
            terminate(player.uniqueId, closeInventory = true, clearInventory = true)
            plugin.logger.severe(
                plugin.languageManager.getMessage("container.render_failed", menuId, error.message.toString())
            )
            throw error
        }
    }

    /** 处理可信容器槽位点击，并按 all、通用数字键、具体点击类型的顺序执行动作。 */
    fun handleClick(
        player: Player,
        inventory: Inventory,
        slot: Int,
        clickType: ContainerClickType,
        hotbarButton: Int?
    ) {
        val session = validSession(player, inventory) ?: return
        val buttonId = session.definition.layout.buttonAt(slot) ?: return
        val button = session.definition.buttons[buttonId] ?: return
        if (!isButtonVisible(player, session, button)) return

        val actionTypes = linkedSetOf(ContainerClickType.ALL)
        if (clickType.name.startsWith("NUMBER_KEY")) {
            actionTypes += ContainerClickType.NUMBER_KEY
        }
        actionTypes += clickType
        val actions = actionTypes.flatMap { button.actions[it].orEmpty() }
        if (actions.isEmpty()) return
        if (!consumeClickCooldown(session)) return

        val variables = sessionVariables(session) + mapOf(
            "slot" to slot.toString(),
            "button" to buttonId,
            "click" to clickType.configKey,
            "hotbar_button" to (hotbarButton?.toString() ?: "")
        )
        MenuActions.executeActionGroup(
            player = player,
            config = session.config,
            actions = actions,
            variables = variables,
            contextId = session.menuId
        )
    }

    /** 消耗当前 Container 会话的点击冷却；时间戳使用系统绝对时间，单位为毫秒。 */
    private fun consumeClickCooldown(session: ActiveSession): Boolean {
        val delayMillis = session.definition.minClickDelayMillis
        if (delayMillis <= 0L) return true

        val nowMillis = System.currentTimeMillis()
        while (true) {
            val previousMillis = session.lastClickAtMillis.get()
            val elapsedMillis = nowMillis - previousMillis
            if (previousMillis > 0L && elapsedMillis >= 0L && elapsedMillis < delayMillis) {
                return false
            }
            if (session.lastClickAtMillis.compareAndSet(previousMillis, nowMillis)) {
                return true
            }
        }
    }

    /** 判断顶部库存是否属于玩家当前有效的 Container 会话。 */
    fun ownsInventory(player: Player, inventory: Inventory): Boolean {
        return validSession(player, inventory) != null
    }

    /** 判断库存是否由 KaMenu 创建；即使活动会话意外丢失，也用于保持交互拦截。 */
    fun isManagedInventory(inventory: Inventory): Boolean {
        return inventory.holder is ContainerMenuHolder || inventory.contents.any(::isDisplayItem)
    }

    /**
     * 接收 Bukkit 铁砧重命名结果，并重新生成结果槽物品。
     *
     * 返回 null 表示结果槽应保持为空；调用前必须先通过 [ownsInventory] 验证库存归属。
     */
    fun prepareAnvilResult(player: Player, inventory: Inventory, rawInput: String): ItemStack? {
        val session = validSession(player, inventory) ?: return null
        if (session.definition.type != ContainerMenuType.ANVIL || !session.supportsAnvilInput) return null
        session.anvilInput = sanitizeAnvilInput(player, session.config, session.definition, rawInput)
        session.anvilInputInitialized = true
        applyAnvilProperties(player, session)

        val buttonId = session.definition.layout.buttonAt(ANVIL_RESULT_SLOT) ?: return null
        val button = session.definition.buttons[buttonId] ?: return null
        if (!isButtonVisible(player, session, button)) return null
        return markDisplayItem(
            itemRenderer.render(player, session.config, session.menuId, button, sessionVariables(session)),
            session.sessionId
        )
    }

    /** 在首次打开或标题重绑后恢复由 KaMenu 控制的铁砧结果槽。 */
    private fun refreshAnvilResultSlot(player: Player, session: ActiveSession) {
        if (session.definition.type != ContainerMenuType.ANVIL || !session.supportsAnvilInput) return
        session.holder.inventory.setItem(
            ANVIL_RESULT_SLOT,
            prepareAnvilResult(player, session.holder.inventory, session.anvilInput)
        )
    }

    /**
     * 执行 `refresh:` 动作。
     *
     * `*` 刷新标题、属性和全部按钮，`title`、`properties` 可分别刷新，其他值匹配按钮 ID。
     */
    fun refreshFromAction(player: Player, target: String): Boolean {
        val session = sessions[player.uniqueId] ?: return false
        val normalized = target.trim()
        val buttonIds = when {
            normalized == "*" -> session.definition.buttons.keys
            normalized.equals("title", ignoreCase = true) -> emptySet()
            normalized.equals("properties", ignoreCase = true) -> emptySet()
            session.definition.buttons.containsKey(normalized) -> setOf(normalized)
            else -> {
                plugin.logger.warning(
                    plugin.languageManager.getMessage("container.refresh_target_not_found", session.menuId, normalized)
                )
                return false
            }
        }
        runOnPlayerThread(player) {
            refresh(
                player = player,
                session = session,
                refreshTitle = normalized == "*" || normalized.equals("title", ignoreCase = true),
                advanceTitle = false,
                refreshProperties = normalized == "*" || normalized.equals("properties", ignoreCase = true),
                buttonIds = buttonIds
            )
        }
        return true
    }

    /** 玩家主动关闭库存时结束会话，并执行一次 Events.Close。 */
    fun handleClose(player: Player, inventory: Inventory) {
        val session = sessions[player.uniqueId] ?: return
        if (session.holder.inventory !== inventory || session.rebindingTitle) return
        if (!sessions.remove(player.uniqueId, session)) return
        val argumentContext = MenuArgumentManager.currentContext(player)
        val sessionArguments = argumentContext?.arguments.orEmpty()
        cleanupSession(session, clearInventory = true)
        MenuTaskManager.cancel(player)
        MenuListManager.clear(player)

        if (session.config.contains("Events.Close")) {
            MenuActions.executeEvent(player, session.config, "Close", session.menuId).whenComplete { shouldKeepOpen, error ->
                if (error != null) {
                    plugin.logger.severe(
                        plugin.languageManager.getMessage(
                            "container.close_event_failed",
                            session.menuId,
                            error.message.toString()
                        )
                    )
                    error.printStackTrace()
                    MenuArgumentManager.clearIfCurrent(player, argumentContext)
                } else if (shouldKeepOpen == true) {
                    // InventoryCloseEvent 返回后服务端还会完成原窗口关闭；下一 tick 再重开，避免产生无会话的死菜单。
                    KaScheduler.runPlayerLater(player, 1L, Runnable {
                        if (!player.isOnline) {
                            MenuArgumentManager.clearIfCurrent(player, argumentContext)
                            return@Runnable
                        }
                        val hasNoReplacementMenu = sessions[player.uniqueId] == null &&
                            !DialogSessionManager.isActive(player) &&
                            player.openInventory.type == InventoryType.CRAFTING
                        if (hasNoReplacementMenu) {
                            if (plugin.menuManager.getMenuId(session.config) != null) {
                                MenuUI.forceOpenMenu(
                                    player,
                                    session.menuId,
                                    plugin.menuManager,
                                    plugin,
                                    sessionArguments
                                )
                            } else {
                                MenuUI.forceOpenConfig(
                                    player,
                                    session.config,
                                    plugin,
                                    session.menuId,
                                    sessionArguments
                                )
                            }
                        } else if (sessions[player.uniqueId] == null && !DialogSessionManager.isActive(player)) {
                            MenuArgumentManager.clearIfCurrent(player, argumentContext)
                        }
                    })
                } else {
                    MenuArgumentManager.clearIfCurrent(player, argumentContext)
                }
            }
        } else {
            MenuArgumentManager.clearIfCurrent(player, argumentContext)
        }
    }

    /** 静默关闭当前容器菜单；用于动作已经处理 Close 生命周期或内部菜单跳转。 */
    fun closeSilently(player: Player): Boolean {
        terminate(player.uniqueId, closeInventory = true, clearInventory = true) ?: return false
        MenuTaskManager.cancel(player)
        MenuListManager.clear(player)
        return true
    }

    /** 返回玩家当前容器会话 ID，供异步生命周期回调识别原菜单。 */
    fun currentSessionId(player: Player): UUID? {
        return sessions[player.uniqueId]?.sessionId
    }

    /** 仅当玩家仍处于指定容器会话时静默关闭，避免异步回调误关后来打开的菜单。 */
    fun closeSilentlyIfCurrent(player: Player, expectedSessionId: UUID): Boolean {
        terminate(
            playerId = player.uniqueId,
            closeInventory = true,
            clearInventory = true,
            expectedSessionId = expectedSessionId
        ) ?: return false
        MenuTaskManager.cancel(player)
        MenuListManager.clear(player)
        return true
    }

    /** 重载或插件停用前静默关闭所有活动容器菜单。 */
    fun closeAllSilently() {
        sessions.keys.toList().forEach { playerId ->
            val player = Bukkit.getPlayer(playerId)
            if (player != null && player.isOnline) {
                runOnPlayerThread(player) { closeSilently(player) }
            } else {
                terminate(playerId, closeInventory = false, clearInventory = true)
            }
        }
    }

    /** 玩家退出时丢弃容器状态，不执行 Close 事件。 */
    fun discard(player: Player) {
        terminate(player.uniqueId, closeInventory = false, clearInventory = true)
    }

    /** 清理因异常热卸载等情况进入玩家库存或光标的展示物品。 */
    fun removeLeakedItems(player: Player) {
        val topInventory = player.openInventory.topInventory
        if (topInventory.contents.any(::isDisplayItem)) {
            topInventory.clear()
            player.closeInventory()
        }
        player.inventory.contents = player.inventory.contents.map { item ->
            item?.takeUnless(::isDisplayItem)
        }.toTypedArray()
        if (isDisplayItem(player.itemOnCursor)) {
            player.setItemOnCursor(ItemStack(Material.AIR))
        }
    }

    /** 插件停用时关闭全部容器会话。 */
    fun shutdown() {
        closeAllSilently()
    }

    /** 插件热加载时为当前在线玩家安排展示物品恢复检查。 */
    fun cleanupOnlinePlayers() {
        Bukkit.getOnlinePlayers().forEach { player ->
            runOnPlayerThread(player) { removeLeakedItems(player) }
        }
    }

    /** 为整体、标题和同周期按钮组建立 Folia 安全的玩家定时任务。 */
    private fun scheduleRefreshes(player: Player, session: ActiveSession) {
        session.definition.update.menuIntervalTicks?.let { interval ->
            session.refreshHandles += KaScheduler.runPlayerTimer(player, interval, interval, Runnable {
                refresh(
                    player,
                    session,
                    refreshTitle = true,
                    advanceTitle = false,
                    refreshProperties = true,
                    session.definition.buttons.keys
                )
            })
        }
        session.definition.update.titleIntervalTicks?.let { interval ->
            session.refreshHandles += KaScheduler.runPlayerTimer(player, interval, interval, Runnable {
                refresh(
                    player,
                    session,
                    refreshTitle = true,
                    advanceTitle = true,
                    refreshProperties = false,
                    emptySet()
                )
            })
        }
        session.definition.update.progressIntervalTicks?.let { interval ->
            session.refreshHandles += KaScheduler.runPlayerTimer(player, interval, interval, Runnable {
                refresh(
                    player,
                    session,
                    refreshTitle = false,
                    advanceTitle = false,
                    refreshProperties = true,
                    emptySet()
                )
            })
        }
        session.definition.buttons.values
            .filter { it.updateIntervalTicks != null }
            .groupBy { it.updateIntervalTicks!! }
            .forEach { (interval, buttons) ->
                val buttonIds = buttons.mapTo(linkedSetOf(), ContainerButtonDefinition::id)
                session.refreshHandles += KaScheduler.runPlayerTimer(player, interval, interval, Runnable {
                    refresh(
                        player,
                        session,
                        refreshTitle = false,
                        advanceTitle = false,
                        refreshProperties = false,
                        buttonIds
                    )
                })
            }
    }

    /** 执行一轮刷新；同一会话上一轮未完成时跳过，避免重入。 */
    private fun refresh(
        player: Player,
        session: ActiveSession,
        refreshTitle: Boolean,
        advanceTitle: Boolean,
        refreshProperties: Boolean,
        buttonIds: Collection<String>
    ) {
        if (validSession(player, session.holder.inventory) !== session) return
        if (!session.refreshRunning.compareAndSet(false, true)) return
        try {
            if (refreshTitle) refreshTitle(player, session, advanceTitle)
            if (buttonIds.isNotEmpty()) refreshButtons(player, session, buttonIds)
            if (refreshProperties) applyViewProperties(player, session)
        } catch (error: RuntimeException) {
            plugin.logger.warning(
                plugin.languageManager.getMessage("container.refresh_failed", session.menuId, error.message.toString())
            )
        } finally {
            session.refreshRunning.set(false)
        }
    }

    /** 重新计算按钮显示条件和物品，仅写入实际发生变化的槽位。 */
    private fun refreshButtons(player: Player, session: ActiveSession, buttonIds: Collection<String>) {
        val inventory = session.holder.inventory
        val variables = sessionVariables(session)
        buttonIds.forEach { buttonId ->
            val button = session.definition.buttons[buttonId] ?: return@forEach
            val item = if (isButtonVisible(player, session, button)) {
                markDisplayItem(
                    itemRenderer.render(player, session.config, session.menuId, button, variables),
                    session.sessionId
                )
            } else {
                null
            }
            session.definition.layout.slotsByButton[buttonId].orEmpty().forEach { slot ->
                val rendered = item?.clone()?.let { candidate ->
                    if (slot == ANVIL_INPUT_SLOT && session.definition.type == ContainerMenuType.ANVIL &&
                        session.anvilInputInitialized
                    ) {
                        applyAnvilInputName(session, candidate)
                    } else {
                        candidate
                    }
                }
                if (inventory.getItem(slot) != rendered) {
                    inventory.setItem(slot, rendered)
                }
            }
        }
    }

    /** 判断按钮当前是否对玩家可见。 */
    private fun isButtonVisible(
        player: Player,
        session: ActiveSession,
        button: ContainerButtonDefinition
    ): Boolean {
        val condition = button.viewCondition ?: return true
        return org.katacr.kamenu.ConditionUtils.checkCondition(
            player,
            condition,
            sessionVariables(session),
            session.config
        ) { null }
    }

    /** 解析标题帧并在标题变化时原地更新或兼容性重绑库存。 */
    private fun refreshTitle(player: Player, session: ActiveSession, advance: Boolean) {
        val frames = resolveTitleFrames(player, session.config, session.definition, sessionVariables(session))
        if (advance && frames.size > 1) {
            session.titleFrame = (session.titleFrame + 1) % frames.size
        } else {
            session.titleFrame = session.titleFrame.coerceIn(0, frames.lastIndex)
        }
        val title = toLegacy(player, frames[session.titleFrame])
        if (title == session.currentTitle) return

        val view = player.openInventory
        if (view.topInventory !== session.holder.inventory) return
        session.currentTitle = title
        val setTitle = view.javaClass.methods.firstOrNull { method ->
            method.name == "setTitle" && method.parameterTypes.contentEquals(arrayOf(String::class.java))
        }
        if (setTitle != null && runCatching { setTitle.invoke(view, title) }.isSuccess) return

        session.rebindingTitle = true
        try {
            val oldInventory = session.holder.inventory
            val replacement = createInventoryWindow(player, session.holder, session.definition, title)
            replacement.inventory.contents = oldInventory.contents.map { it?.clone() }.toTypedArray()
            session.holder.bindInventory(replacement.inventory)
            initializeAnvilInputItem(player, session)
            openWindow(player, replacement)
            bindOpenedInventory(player, session.holder)
            oldInventory.clear()
            applyViewProperties(player, session)
            refreshAnvilResultSlot(player, session)
        } finally {
            session.rebindingTitle = false
        }
    }

    /** 取得初始标题或指定标题帧，并转换为 Legacy 文本。 */
    private fun resolveTitle(
        player: Player,
        config: YamlConfiguration,
        definition: ContainerMenuDefinition,
        frame: Int,
        variables: Map<String, String> = emptyMap()
    ): String {
        val frames = resolveTitleFrames(player, config, definition, variables)
        return toLegacy(player, frames[frame.coerceIn(0, frames.lastIndex)])
    }

    /** 解析标题中的条件值、PAPI 和内置变量。 */
    private fun resolveTitleFrames(
        player: Player,
        config: YamlConfiguration,
        definition: ContainerMenuDefinition,
        variables: Map<String, String> = emptyMap()
    ): List<String> {
        return ContainerValueResolver(player, config, variables).strings(definition.title).ifEmpty { listOf("KaMenu") }
    }

    /** 按容器类型创建库存；铁砧优先创建能接收原版重命名输入的真实视图。 */
    private fun createInventoryWindow(
        player: Player,
        holder: ContainerMenuHolder,
        definition: ContainerMenuDefinition,
        title: String
    ): InventoryWindow {
        if (definition.type == ContainerMenuType.CHEST) {
            return InventoryWindow(Bukkit.createInventory(holder, definition.layout.size, title))
        }
        if (definition.type == ContainerMenuType.ANVIL) {
            createModernAnvilView(player, title)?.let { view ->
                return InventoryWindow(view.topInventory, view, supportsAnvilInput = true)
            }
            openPaperAnvil(player, title)?.let { view ->
                return InventoryWindow(
                    inventory = view.topInventory,
                    view = view,
                    alreadyOpen = true,
                    supportsAnvilInput = true
                )
            }
        }

        // 非箱子库存不传 Holder，避免 Paper 把熔炉包装成缺少进度 API 的通用 CraftContainer。
        return InventoryWindow(
            Bukkit.createInventory(null, InventoryType.valueOf(definition.type.name), title)
        )
    }

    /** 在现代 Bukkit/Paper 上通过 MenuType 构建真实铁砧视图，不静态引用新版本类。 */
    private fun createModernAnvilView(player: Player, title: String): InventoryView? {
        return runCatching {
            val menuTypeClass = Class.forName("org.bukkit.inventory.MenuType")
            val anvilType = menuTypeClass.getField("ANVIL").get(null)
            val typedMenuClass = Class.forName("org.bukkit.inventory.MenuType\$Typed")
            val builder = typedMenuClass.getMethod("builder").invoke(anvilType)
            val locationBuilderClass = Class.forName(
                "org.bukkit.inventory.view.builder.LocationInventoryViewBuilder"
            )
            val inventoryBuilderClass = Class.forName("org.bukkit.inventory.view.builder.InventoryViewBuilder")

            locationBuilderClass.getMethod("checkReachable", Boolean::class.javaPrimitiveType)
                .invoke(builder, false)

            val titleMethod = inventoryBuilderClass.methods.firstOrNull {
                it.name == "title" && it.parameterCount == 1 && it.parameterTypes[0] == String::class.java
            } ?: inventoryBuilderClass.methods.firstOrNull {
                it.name == "title" && it.parameterCount == 1 &&
                    it.parameterTypes[0].name == "net.kyori.adventure.text.Component"
            }
            if (titleMethod != null) {
                val titleValue = if (titleMethod.parameterTypes[0] == String::class.java) {
                    title
                } else {
                    TextParser.parseText(title, player)
                }
                titleMethod.invoke(builder, titleValue)
            }

            inventoryBuilderClass.getMethod("build", org.bukkit.entity.HumanEntity::class.java)
                .invoke(builder, player) as InventoryView
        }.getOrNull()
    }

    /** 在没有 MenuType 的 Paper 版本上调用其扩展 openAnvil API；Spigot 缺少该方法时返回 null。 */
    private fun openPaperAnvil(player: Player, title: String): InventoryView? {
        return runCatching {
            val method = player.javaClass.methods.first {
                it.name == "openAnvil" && it.parameterCount == 2 &&
                    it.parameterTypes[1] == Boolean::class.javaPrimitiveType
            }
            val view = method.invoke(player, null, true) as? InventoryView ?: return@runCatching null
            view.javaClass.methods.firstOrNull {
                it.name == "setTitle" && it.parameterTypes.contentEquals(arrayOf(String::class.java))
            }?.let { runCatching { it.invoke(view, title) } }
            view
        }.getOrNull()
    }

    /** 打开尚未显示的库存或 InventoryView；旧 Paper openAnvil 返回的视图已经打开。 */
    private fun openWindow(player: Player, window: InventoryWindow) {
        if (window.alreadyOpen) return
        if (window.view != null) {
            player.openInventory(window.view)
        } else {
            player.openInventory(window.inventory)
        }
    }

    /** 真实方块容器打开后可能返回同一底层库存的新包装对象，统一绑定到玩家实际视图。 */
    private fun bindOpenedInventory(player: Player, holder: ContainerMenuHolder) {
        val opened = player.openInventory.topInventory
        require(opened.type == holder.inventory.type && opened.size == holder.inventory.size) {
            "The server did not open the requested ${holder.inventory.type} inventory."
        }
        holder.bindInventory(opened)
    }

    /** 解析并应用熔炉进度或铁砧等级参数。 */
    private fun applyViewProperties(player: Player, session: ActiveSession) {
        val view = player.openInventory
        if (view.topInventory !== session.holder.inventory) return
        when {
            session.definition.type.isFurnace -> applyFurnaceProgress(player, session, view)
            session.definition.type == ContainerMenuType.ANVIL -> applyAnvilProperties(player, session)
        }
    }

    /** 将 0..100 的配置百分比写入火焰和加工箭头；旧版普通熔炉回退到 InventoryView.Property。 */
    private fun applyFurnaceProgress(player: Player, session: ActiveSession, view: InventoryView) {
        val properties = session.definition.properties
        val resolver = ContainerValueResolver(player, session.config, sessionVariables(session))
        val burn = properties["burn_progress"]?.let { resolveProgress(resolver.string(it)) }
        val cook = properties["cook_progress"]?.let { resolveProgress(resolver.string(it)) }
        if (burn == null && cook == null) return

        val burnMethod = view.javaClass.methods.firstOrNull {
            it.name == "setBurnTime" && it.parameterTypes.contentEquals(TWO_INT_PARAMETERS)
        }
        val cookMethod = view.javaClass.methods.firstOrNull {
            it.name == "setCookTime" && it.parameterTypes.contentEquals(TWO_INT_PARAMETERS)
        }
        val modernSupported = (burn == null || burnMethod != null) && (cook == null || cookMethod != null)
        val modernApplied = modernSupported && runCatching {
                if (burn != null) burnMethod!!.invoke(view, burn, PROGRESS_TOTAL)
                if (cook != null) cookMethod!!.invoke(view, cook, PROGRESS_TOTAL)
            }.isSuccess

        if (!modernApplied) {
            val legacyApplied = runCatching {
                val burnApplied = burn == null || (
                    view.setProperty(InventoryView.Property.TICKS_FOR_CURRENT_FUEL, PROGRESS_TOTAL) &&
                        view.setProperty(InventoryView.Property.BURN_TIME, burn)
                    )
                val cookApplied = cook == null || (
                    view.setProperty(InventoryView.Property.TICKS_FOR_CURRENT_SMELTING, PROGRESS_TOTAL) &&
                        view.setProperty(InventoryView.Property.COOK_TIME, cook)
                    )
                burnApplied && cookApplied
            }.getOrDefault(false)
            if (!legacyApplied && !session.furnaceWarningLogged) {
                session.furnaceWarningLogged = true
                plugin.logger.warning(
                    plugin.languageManager.getMessage(
                        "container.furnace_progress_unsupported",
                        session.menuId,
                        session.definition.type.name
                    )
                )
            }
        }

        evaluateProgressWatchers(player, session, burn, cook)
    }

    /** 解析整数、浮点数或带 `%` 后缀的百分比，并转换为保留两位小数的万分制进度。 */
    private fun resolveProgress(rawValue: String): Int {
        val normalized = rawValue.trim().removeSuffix("%").trim()
        val percentage = normalized.toDoubleOrNull()?.takeIf(Double::isFinite) ?: 0.0
        return (percentage.coerceIn(0.0, 100.0) * PROGRESS_PRECISION).roundToInt()
    }

    /** 计算进度监听条件，并在条件从未满足变为满足时执行一次动作列表。 */
    private fun evaluateProgressWatchers(
        player: Player,
        session: ActiveSession,
        burnProgress: Int?,
        cookProgress: Int?
    ) {
        if (session.definition.progressWatchers.isEmpty()) return
        val values = mapOf(
            "burn_progress" to burnProgress,
            "cook_progress" to cookProgress
        )
        session.definition.progressWatchers.values.forEach { watcher ->
            val current = values[watcher.source] ?: return@forEach
            val state = session.progressStates.getOrPut(watcher.id) { ProgressWatcherState() }
            val previous = if (state.initialized) state.previousValue else current
            val variables = sessionVariables(session) + mapOf(
                "progress.id" to watcher.id,
                "progress.source" to watcher.source,
                "progress.current" to progressText(current),
                "progress.previous" to progressText(previous)
            )
            val matched = org.katacr.kamenu.ConditionUtils.checkCondition(
                player,
                watcher.condition,
                variables,
                session.config
            ) { null }
            val shouldTrigger = matched && if (state.initialized) !state.matched else watcher.triggerInitial

            // 必须先提交本轮状态，避免动作中的变量修改或 refresh 造成同一边沿重复触发。
            state.initialized = true
            state.previousValue = current
            state.matched = matched

            if (shouldTrigger) {
                MenuActions.executeActionGroup(
                    player = player,
                    config = session.config,
                    actions = watcher.actions,
                    variables = variables,
                    contextId = session.menuId
                ).whenComplete { _, error ->
                    if (error != null) {
                        plugin.logger.warning(
                            plugin.languageManager.getMessage(
                                "container.progress_action_failed",
                                session.menuId,
                                watcher.id,
                                error.message ?: error.javaClass.simpleName
                            )
                        )
                    }
                }
            }
        }
    }

    /** 将万分制内部进度格式化为无多余零的百分比文本。 */
    private fun progressText(value: Int): String {
        return BigDecimal.valueOf(value.toLong(), 2).stripTrailingZeros().toPlainString()
    }

    /** 设置铁砧等级和材料消耗；repair_item_count 在旧 API 上通过能力检测忽略。 */
    private fun applyAnvilProperties(player: Player, session: ActiveSession) {
        val inventory = session.holder.inventory as? AnvilInventory ?: return
        val resolver = ContainerValueResolver(player, session.config, sessionVariables(session))
        val properties = session.definition.properties
        inventory.maximumRepairCost = properties["maximum_repair_cost"]
            ?.let { resolver.int(it, 40).coerceAtLeast(0) }
            ?: 40
        inventory.repairCost = properties["repair_cost"]?.let { resolver.int(it, 0).coerceAtLeast(0) } ?: 0
        properties["repair_item_count"]?.let { value ->
            val amount = resolver.int(value, 0).coerceAtLeast(0)
            inventory.javaClass.methods.firstOrNull {
                it.name == "setRepairCostAmount" &&
                    it.parameterTypes.contentEquals(arrayOf(Int::class.javaPrimitiveType))
            }?.let { runCatching { it.invoke(inventory, amount) } }
        }
    }

    /** 解析 Properties.input，并按全局输入规则和 remove_chars 清理。 */
    private fun resolveInitialAnvilInput(
        player: Player,
        config: YamlConfiguration,
        definition: ContainerMenuDefinition
    ): String {
        if (definition.type != ContainerMenuType.ANVIL) return ""
        val raw = definition.properties["input"]?.let {
            ContainerValueResolver(player, config).string(it)
        }.orEmpty()
        return sanitizeAnvilInput(player, config, definition, raw)
    }

    /** 复用 Dialog 输入清理器规范化铁砧文本，保持 trim 和字符预设行为一致。 */
    private fun sanitizeAnvilInput(
        player: Player,
        config: YamlConfiguration,
        definition: ContainerMenuDefinition,
        rawInput: String
    ): String {
        val resolver = ContainerValueResolver(player, config, mapOf("input" to rawInput))
        val removeChars = InputCaptureUtils.resolveRemoveChars(
            plugin,
            resolver.resolve(definition.properties["remove_chars"])
        )
        val schema = InputCaptureUtils.Schema(
            keys = listOf("input"),
            types = mapOf("input" to "text"),
            removeChars = mapOf("input" to removeChars),
            checkboxMappings = emptyMap()
        )
        return InputCaptureUtils.captureVariables(plugin, mapOf("input" to rawInput), schema)["input"].orEmpty()
    }

    /** 确保铁砧左槽存在命名物品，使客户端显示重命名输入框并在重绑后恢复文本。 */
    private fun initializeAnvilInputItem(player: Player, session: ActiveSession) {
        if (session.definition.type != ContainerMenuType.ANVIL) return
        val inventory = session.holder.inventory
        val configuredInput = session.definition.properties.contains("input")
        val existing = inventory.getItem(ANVIL_INPUT_SLOT)
        if (!configuredInput && session.anvilInput.isEmpty()) {
            val displayName = existing?.itemMeta?.takeIf { it.hasDisplayName() }?.displayName
            if (!displayName.isNullOrEmpty()) {
                val plainName = PlainTextComponentSerializer.plainText().serialize(
                    LegacyComponentSerializer.legacySection().deserialize(displayName)
                )
                session.anvilInput = sanitizeAnvilInput(
                    player,
                    session.config,
                    session.definition,
                    plainName
                )
                session.anvilInputInitialized = true
            } else {
                return
            }
        }

        val item = existing?.clone() ?: markDisplayItem(ItemStack(Material.PAPER), session.sessionId)
        inventory.setItem(ANVIL_INPUT_SLOT, applyAnvilInputName(session, item))
    }

    /** 把会话输入写回铁砧左槽物品名，避免自动刷新重置客户端输入框。 */
    private fun applyAnvilInputName(session: ActiveSession, item: ItemStack): ItemStack {
        val meta = item.itemMeta ?: return item
        meta.setDisplayName(session.anvilInput.ifEmpty { " " })
        item.itemMeta = meta
        return item
    }

    /** 返回当前菜单可供显示、条件和动作统一读取的会话变量。 */
    private fun sessionVariables(session: ActiveSession): Map<String, String> {
        return if (session.definition.type == ContainerMenuType.ANVIL) {
            mapOf("input" to session.anvilInput)
        } else {
            emptyMap()
        }
    }

    /** 校验库存是否属于玩家当前且仍在本代际的活动会话。 */
    private fun validSession(player: Player, inventory: Inventory): ActiveSession? {
        val session = sessions[player.uniqueId] ?: return null
        if (session.playerId != player.uniqueId || session.holder.inventory !== inventory ||
            session.generation != plugin.menuManager.getGeneration()
        ) {
            return null
        }
        return session
    }

    /** 移除并清理一个会话，可选主动关闭客户端库存。 */
    private fun terminate(
        playerId: UUID,
        closeInventory: Boolean,
        clearInventory: Boolean,
        expectedSessionId: UUID? = null
    ): ActiveSession? {
        val session = sessions[playerId] ?: return null
        if (expectedSessionId != null && session.sessionId != expectedSessionId) return null
        if (!sessions.remove(playerId, session)) return null
        cleanupSession(session, clearInventory)
        if (closeInventory) {
            Bukkit.getPlayer(playerId)?.takeIf(Player::isOnline)?.closeInventory()
        }
        return session
    }

    /** 取消刷新任务并清空展示库存。 */
    private fun cleanupSession(session: ActiveSession, clearInventory: Boolean) {
        session.refreshHandles.forEach(KaTaskHandle::cancel)
        session.refreshHandles.clear()
        if (clearInventory) {
            session.holder.inventory.clear()
        }
    }

    /** 给展示物品写入不可转移的会话标记。 */
    private fun markDisplayItem(item: ItemStack, sessionId: UUID): ItemStack {
        val meta = item.itemMeta ?: return item
        meta.persistentDataContainer.set(displayMarkerKey, PersistentDataType.STRING, sessionId.toString())
        item.itemMeta = meta
        return item
    }

    /** 判断物品是否由 KaMenu 容器渲染器生成。 */
    private fun isDisplayItem(item: ItemStack?): Boolean {
        return item?.itemMeta?.persistentDataContainer?.has(displayMarkerKey, PersistentDataType.STRING) == true
    }

    /** 将彩色标题转换为 Bukkit Inventory 使用的 Legacy 字符串。 */
    private fun toLegacy(player: Player, text: String): String {
        return LegacyComponentSerializer.legacySection().serialize(TextParser.parseText(text, player))
    }

    /** 确保所有库存 API 都在玩家所属线程执行。 */
    private fun runOnPlayerThread(player: Player, action: () -> Unit) {
        if (KaScheduler.isPlayerThread(player)) {
            action()
        } else {
            KaScheduler.runPlayer(player, Runnable(action))
        }
    }

    companion object {
        private const val ANVIL_INPUT_SLOT = 0
        private const val ANVIL_RESULT_SLOT = 2
        private const val PROGRESS_PRECISION = 100
        private const val PROGRESS_TOTAL = 100 * PROGRESS_PRECISION
        private val TWO_INT_PARAMETERS = arrayOf(Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
    }
}
