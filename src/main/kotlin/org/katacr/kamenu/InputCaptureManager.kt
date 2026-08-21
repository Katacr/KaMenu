package org.katacr.kamenu

import org.katacr.kamenu.container.ContainerMenuService
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * 统一输入捕获运行时：管理每玩家的分层捕获会话。
 *
 * 会话由 `input:` 动作发起，聊天框类型由 [ChatInputListener] 喂入消息；
 * 全部层通过校验后执行 complete 动作组并让等待中的动作链继续，超时/取消则中断链。
 * 变量通过 TextResolver 的 `{input:key}` / `{input_*}` 模式回查本管理器。
 */
object InputCaptureManager {

    /** 会话结束原因；COMPLETE 之外的值都会让等待中的动作链按 `return` 语义中断。 */
    enum class EndReason { COMPLETE, CANCEL, TIMEOUT, MAX_ATTEMPTS, MANUAL, SILENT }

    /** 每玩家的活跃捕获会话；字段仅由玩家线程访问。 */
    class CaptureSession internal constructor(
        val playerId: UUID,
        val definition: InputCaptureDefinition,
        val config: YamlConfiguration?,
        val contextId: String?,
        val initialVariables: Map<String, String>,
        val future: CompletableFuture<EndReason>
    ) {
        val captured = LinkedHashMap<String, String>()
        @Volatile var currentLayer = 0
        @Volatile var layerAttempts = 0
        @Volatile var candidate = ""
        @Volatile var cancelReason = ""
        @Volatile var timeoutTask: KaTaskHandle? = null
        @Volatile var ended = false
        /** anvil 类型：挂起的容器会话快照，捕获完成后用于恢复原菜单。 */
        @Volatile var suspendedContainer: ContainerMenuService.SuspendedSession? = null
        /** anvil 类型：当前铁砧视图，用于关闭和识别事件来源。 */
        @Volatile var anvilView: org.bukkit.inventory.InventoryView? = null
    }

    /** 捕获结束后的变量快照，供会话移除后 `{input:key}` 继续解析。 */
    private class ResolutionSnapshot(
        val values: Map<String, String>,
        val candidate: String,
        val layer: Int,
        val attempt: Int,
        val cancelReason: String
    )

    private var plugin: KaMenu? = null
    private val sessions = ConcurrentHashMap<UUID, CaptureSession>()
    private val lastResolution = ConcurrentHashMap<UUID, ResolutionSnapshot>()

    fun init(kamenu: KaMenu) {
        plugin = kamenu
    }

    fun hasActiveSession(playerId: UUID): Boolean = sessions.containsKey(playerId)

    /** 返回玩家最近一次捕获的层值表，供动作链合并变量。 */
    fun lastValues(playerId: UUID): Map<String, String> =
        lastResolution[playerId]?.values ?: emptyMap()

    /**
     * 发起一次捕获并返回结束原因 future。
     *
     * 若玩家已有会话则静默替换：旧会话直接以 SILENT 结束，不执行其 cancel 动作组。
     */
    fun start(
        player: Player,
        definition: InputCaptureDefinition,
        config: YamlConfiguration?,
        contextId: String?,
        variables: Map<String, String>
    ): CompletableFuture<InputCaptureManager.EndReason> {
        if (!player.isOnline) {
            return CompletableFuture.completedFuture(EndReason.SILENT)
        }
        val future = CompletableFuture<EndReason>()
        val session = CaptureSession(
            playerId = player.uniqueId,
            definition = definition,
            config = config,
            contextId = contextId,
            initialVariables = variables,
            future = future
        )
        sessions.put(player.uniqueId, session)?.let { previous ->
            abortSilently(previous)
        }
        scheduleTimeout(player, session)
        when (definition.type) {
            InputCaptureDefinition.CaptureType.CHAT -> executePrompt(player, session)
            InputCaptureDefinition.CaptureType.ANVIL -> {
                val currentPlugin = plugin
                if (currentPlugin != null && currentPlugin.containerMenusReady) {
                    session.suspendedContainer = currentPlugin.containerMenuService.suspendForInput(player)
                }
                executePrompt(player, session)
                openAnvilForCurrentLayer(player, session)
            }
            InputCaptureDefinition.CaptureType.DIALOG -> {
                executePrompt(player, session)
                openDialogForCurrentLayer(player, session)
            }
        }
        return future
    }

    /**
     * 处理一条聊天消息；返回 true 表示消息属于活跃会话（监听器已取消广播）。
     *
     * 调用方负责保证运行在玩家线程；命令消息不会到达此方法（AsyncPlayerChatEvent 不含命令）。
     */
    fun handleChatMessage(player: Player, message: String): Boolean {
        val session = sessions[player.uniqueId] ?: return false
        if (session.ended || session.definition.type != InputCaptureDefinition.CaptureType.CHAT) {
            return false
        }
        val trimmed = message.trim()
        if (trimmed.isEmpty()) {
            return true
        }
        val cancelText = session.definition.cancelText
        if (cancelText != null && trimmed.equals(cancelText, ignoreCase = true)) {
            endSession(player, session, EndReason.CANCEL)
            return true
        }
        val layer = session.definition.layers[session.currentLayer]
        if (trimmed.length > layer.maxLength) {
            sendLang(player, "input-capture.too-long", layer.maxLength.toString())
            return true
        }

        session.candidate = trimmed
        val validate = layer.validateCondition
        if (validate != null && !ConditionUtils.checkCondition(
                player, validate, session.initialVariables, session.config
            ) { null }
        ) {
            executeGroup(player, session, layer.invalidActions)
            session.layerAttempts++
            if (session.layerAttempts >= session.definition.maxAttempts) {
                endSession(player, session, EndReason.MAX_ATTEMPTS)
            } else {
                executePrompt(player, session)
            }
            return true
        }

        session.captured[layer.key] = trimmed
        session.candidate = ""
        session.layerAttempts = 0
        session.currentLayer++
        if (session.currentLayer < session.definition.layers.size) {
            scheduleTimeout(player, session)
            executePrompt(player, session)
            return true
        }
        completeSession(player, session)
        return true
    }

    /**
     * 铁砧确认按钮点击：捕获重命名文本并推进层。
     *
     * 由 AnvilInputListener 在玩家线程调用；返回 true 表示事件属于活跃铁砧会话。
     */
    fun handleAnvilConfirm(player: Player, inventory: org.bukkit.inventory.AnvilInventory): Boolean {
        val session = sessions[player.uniqueId] ?: return false
        if (session.ended || session.definition.type != InputCaptureDefinition.CaptureType.ANVIL) return false
        if (session.anvilView?.topInventory !== inventory) return false

        val rawInput = inventory.renameText?.trim().orEmpty()
        if (rawInput.isEmpty()) return true

        val cancelText = session.definition.cancelText
        if (cancelText != null && rawInput.equals(cancelText, ignoreCase = true)) {
            closeAnvilView(player, session)
            endSession(player, session, EndReason.CANCEL)
            return true
        }

        if (!validateAndAdvance(player, session, rawInput)) return true

        if (session.currentLayer >= session.definition.layers.size) {
            closeAnvilView(player, session)
            completeSession(player, session)
        } else {
            closeAnvilView(player, session)
            openAnvilForCurrentLayer(player, session)
        }
        return true
    }

    /**
     * 铁砧界面关闭事件：如果是玩家主动 Esc（非程序关闭），视为取消。
     *
     * 程序关闭（closeAnvilView）会先置 ended 或切换层，此时事件被忽略。
     */
    fun handleAnvilClose(player: Player, inventory: org.bukkit.inventory.Inventory): Boolean {
        val session = sessions[player.uniqueId] ?: return false
        if (session.ended || session.definition.type != InputCaptureDefinition.CaptureType.ANVIL) return false
        if (session.anvilView?.topInventory !== inventory) return false
        endSession(player, session, EndReason.CANCEL)
        return true
    }

    /** 关闭铁砧视图；置空引用以阻止后续 handleClose 误判。 */
    private fun closeAnvilView(player: Player, session: CaptureSession) {
        session.anvilView = null
        player.closeInventory()
    }

    /** 为当前层打开铁砧界面并放置命名提示物品。 */
    private fun openAnvilForCurrentLayer(player: Player, session: CaptureSession) {
        val layer = session.definition.layers[session.currentLayer]
        val title = layer.title ?: plugin?.languageManager?.getMessage("input-capture.default-prompt", session.definition.cancelText ?: "") ?: "Input"
        val opened = AnvilViewFactory.openInputAnvil(player, title)
        if (opened == null) {
            plugin?.logger?.warning("无法打开铁砧输入界面，回退为取消。玩家: ${player.name}")
            endSession(player, session, EndReason.CANCEL)
            return
        }
        session.anvilView = opened.view
        if (!opened.alreadyOpen) {
            player.openInventory(opened.view)
        }
        val inv = opened.view.topInventory as? org.bukkit.inventory.AnvilInventory ?: return
        val item = org.bukkit.inventory.ItemStack(org.bukkit.Material.PAPER)
        val meta = item.itemMeta
        meta?.setDisplayName(layer.title ?: " ")
        item.itemMeta = meta
        inv.setItem(0, item)
        inv.maximumRepairCost = 0
        inv.repairCost = 0
    }

    /**
     * 校验输入并推进层；校验失败时执行 invalid 组并重试或超限取消。
     *
     * 返回 true 表示正常推进（无论是否进入下一层），false 表示会话已结束。
     */
    private fun validateAndAdvance(player: Player, session: CaptureSession, rawInput: String): Boolean {
        val layer = session.definition.layers[session.currentLayer]
        if (rawInput.length > layer.maxLength) {
            sendLang(player, "input-capture.too-long", layer.maxLength.toString())
            return true
        }
        session.candidate = rawInput
        val validate = layer.validateCondition
        if (validate != null && !ConditionUtils.checkCondition(
                player, validate, session.initialVariables, session.config
            ) { null }
        ) {
            executeGroup(player, session, layer.invalidActions)
            session.layerAttempts++
            if (session.layerAttempts >= session.definition.maxAttempts) {
                endSession(player, session, EndReason.MAX_ATTEMPTS)
                return false
            }
            session.candidate = ""
            return true
        }
        session.captured[layer.key] = rawInput
        session.candidate = ""
        session.layerAttempts = 0
        session.currentLayer++
        return true
    }

    /**
     * Dialog 确认回调：从 `input-dialog-complete:` 动作接收已捕获的文本值。
     *
     * 由 MenuActions 在玩家线程调用；值已经过 Dialog 输入清理。
     */
    fun handleDialogComplete(player: Player, key: String, value: String) {
        val session = sessions[player.uniqueId] ?: return
        if (session.ended || session.definition.type != InputCaptureDefinition.CaptureType.DIALOG) return
        val layer = session.definition.layers[session.currentLayer]
        if (key != layer.key) return

        val cancelText = session.definition.cancelText
        if (cancelText != null && value.trim().equals(cancelText, ignoreCase = true)) {
            endSession(player, session, EndReason.CANCEL)
            return
        }

        if (!validateAndAdvance(player, session, value.trim())) return

        if (session.currentLayer >= session.definition.layers.size) {
            completeSession(player, session)
        } else {
            scheduleTimeout(player, session)
            openDialogForCurrentLayer(player, session)
        }
    }

    /** 为当前层合成最小 Dialog 配置并通过平台适配器打开。 */
    private fun openDialogForCurrentLayer(player: Player, session: CaptureSession) {
        val currentPlugin = plugin ?: return
        val layer = session.definition.layers[session.currentLayer]
        val label = layer.title
            ?: currentPlugin.languageManager.getMessage("input-capture.dialog-label")
            ?: "Input"
        val confirmText = currentPlugin.languageManager.getMessage("input-capture.dialog-confirm") ?: "&aConfirm"
        val cancelText = currentPlugin.languageManager.getMessage("input-capture.dialog-cancel") ?: "&cCancel"

        val config = YamlConfiguration().apply {
            set("Title", label)
            set("Settings.lifetime", session.definition.timeoutSeconds)
            set("Settings.can_escape", true)
            set("Settings.after_action", "close")
            set("Inputs.${layer.key}.type", "input")
            set("Inputs.${layer.key}.text", label)
            set("Inputs.${layer.key}.default", "")
            set("Inputs.${layer.key}.max_length", layer.maxLength)
            set("Bottom.type", "confirmation")
            set("Bottom.confirm.text", confirmText)
            set("Bottom.confirm.actions", listOf("input-dialog-complete: key=${layer.key}"))
            set("Bottom.deny.text", cancelText)
            set("Bottom.deny.actions", listOf("input-dialog-cancel:"))
        }
        val dialogContextId = "input-capture:${session.definition.id}"
        MenuUI.openConfig(player, config, currentPlugin, dialogContextId)
    }

    /** `input-cancel:` 动作入口：手动取消活跃会话；返回是否确实取消了会话。 */
    fun cancelByAction(player: Player): Boolean {
        val session = sessions[player.uniqueId] ?: return false
        endSession(player, session, EndReason.MANUAL)
        return true
    }

    /** 玩家退出时静默清理，不执行任何动作组。 */
    fun cancelForQuit(playerId: UUID) {
        sessions.remove(playerId)?.let { abortSilently(it) }
        lastResolution.remove(playerId)
    }

    /** 插件卸载时清理全部会话与快照。 */
    fun clearAll() {
        sessions.values.toList().forEach { abortSilently(it) }
        sessions.clear()
        lastResolution.clear()
    }

    /** 解析 `{input:key}`：优先活跃会话的已捕获层值，其次最近快照；未知键返回空串。 */
    fun resolveValue(playerId: UUID, key: String): String {
        sessions[playerId]?.let { session ->
            if (session.definition.layers.any { it.key == key }) {
                return session.captured[key] ?: ""
            }
        }
        return lastResolution[playerId]?.values?.get(key) ?: ""
    }

    /** 解析 `{input_*}` 系统属性：candidate/layer/attempt/cancel_reason。 */
    fun resolveAttribute(playerId: UUID, name: String): String {
        sessions[playerId]?.let { session ->
            return when (name) {
                "candidate" -> session.candidate
                "layer" -> (session.currentLayer + 1).toString()
                "attempt" -> (session.layerAttempts + 1).toString()
                "cancel_reason" -> session.cancelReason
                else -> ""
            }
        }
        val snapshot = lastResolution[playerId] ?: return ""
        return when (name) {
            "candidate" -> snapshot.candidate
            "layer" -> snapshot.layer.toString()
            "attempt" -> snapshot.attempt.toString()
            "cancel_reason" -> snapshot.cancelReason
            else -> ""
        }
    }

    /** 全部层完成：写快照、执行 complete 组、恢复容器菜单、放行等待中的动作链。 */
    private fun completeSession(player: Player, session: CaptureSession) {
        if (session.ended) return
        session.ended = true
        session.timeoutTask?.cancel()
        sessions.remove(session.playerId, session)
        storeSnapshot(session)
        closeDialogIfActive(player, session)
        executeGroup(player, session, session.definition.completeActions)
        resumeContainerIfRequested(player, session)
        session.future.complete(EndReason.COMPLETE)
    }

    /** 超时/取消/超限/手动结束：执行对应动作组并中断动作链。 */
    private fun endSession(player: Player, session: CaptureSession, reason: EndReason) {
        if (session.ended) return
        session.ended = true
        session.timeoutTask?.cancel()
        sessions.remove(session.playerId, session)
        session.cancelReason = when (reason) {
            EndReason.CANCEL -> "cancel"
            EndReason.TIMEOUT -> "timeout"
            EndReason.MAX_ATTEMPTS -> "max-attempts"
            EndReason.MANUAL -> "manual"
            else -> ""
        }
        storeSnapshot(session)
        closeDialogIfActive(player, session)
        val actions = if (reason == EndReason.TIMEOUT) {
            session.definition.timeoutActions
        } else {
            session.definition.cancelActions
        }
        if (actions.isEmpty()) {
            val key = if (reason == EndReason.TIMEOUT) "input-capture.timeout" else "input-capture.cancelled"
            sendLang(player, key)
        } else {
            executeGroup(player, session, actions)
        }
        resumeContainerIfRequested(player, session)
        session.future.complete(reason)
    }

    /** 静默中止（替换/退出/卸载）：不执行动作组，仅让等待链中断。 */
    private fun abortSilently(session: CaptureSession) {
        if (session.ended) {
            return
        }
        session.ended = true
        session.timeoutTask?.cancel()
        sessions.remove(session.playerId, session)
        storeSnapshot(session)
        session.future.complete(EndReason.SILENT)
    }

    /** DIALOG 类型会话结束时关闭可能仍打开的 Dialog 窗口并取消会话超时。 */
    private fun closeDialogIfActive(player: Player, session: CaptureSession) {
        if (session.definition.type != InputCaptureDefinition.CaptureType.DIALOG) return
        DialogSessionManager.cancel(player)
        MenuUI.closeDialog(player)
    }

    /** 若定义了 reopen=true 且存在挂起的容器会话，在下一 tick 恢复原菜单。 */
    private fun resumeContainerIfRequested(player: Player, session: CaptureSession) {
        if (!session.definition.reopen) return
        val snapshot = session.suspendedContainer ?: return
        val currentPlugin = plugin ?: return
        if (!currentPlugin.containerMenusReady) return
        KaScheduler.runPlayerLater(player, 1L, Runnable {
            if (player.isOnline) {
                currentPlugin.containerMenuService.resumeAfterInput(player, snapshot)
            }
        })
    }

    /** 执行当前层 prompt；未配置时聊天类型回退到默认语言提示。 */
    private fun executePrompt(player: Player, session: CaptureSession) {
        val layer = session.definition.layers[session.currentLayer]
        if (layer.promptActions.isEmpty()) {
            if (session.definition.type == InputCaptureDefinition.CaptureType.CHAT) {
                val cancelHint = session.definition.cancelText ?: "-"
                sendLang(player, "input-capture.default-prompt", cancelHint)
            }
            return
        }
        executeGroup(player, session, layer.promptActions)
    }

    /** 以会话上下文执行动作组；config/contextId 供动作包与变量解析使用。 */
    private fun executeGroup(player: Player, session: CaptureSession, actions: List<Any>) {
        if (actions.isEmpty()) {
            return
        }
        MenuActions.executeActionGroup(
            player,
            session.config ?: YamlConfiguration(),
            actions,
            session.initialVariables,
            contextId = session.contextId
        )
    }

    /** 调度本层超时任务；实例守卫保证过期定时器不会误伤新会话。 */
    private fun scheduleTimeout(player: Player, session: CaptureSession) {
        session.timeoutTask?.cancel()
        val ticks = session.definition.timeoutSeconds * 20L
        session.timeoutTask = KaScheduler.runPlayerLater(player, ticks, Runnable {
            val current = sessions[player.uniqueId]
            if (current === session && !session.ended && sessions.remove(player.uniqueId, session)) {
                endSession(player, session, EndReason.TIMEOUT)
            }
        })
    }

    private fun storeSnapshot(session: CaptureSession) {
        lastResolution[session.playerId] = ResolutionSnapshot(
            values = session.captured.toMap(),
            candidate = session.candidate,
            layer = session.currentLayer,
            attempt = session.layerAttempts,
            cancelReason = session.cancelReason
        )
    }

    private fun sendLang(player: Player, key: String, vararg args: String) {
        val text = plugin?.languageManager?.getMessage(key, *args) ?: return
        MenuUI.sendMessage(player, TextParser.parseText(text))
    }
}
