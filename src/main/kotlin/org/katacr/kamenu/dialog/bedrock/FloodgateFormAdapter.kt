package org.katacr.kamenu.dialog.bedrock

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.geysermc.cumulus.form.SimpleForm
import org.geysermc.cumulus.util.FormImage
import org.geysermc.floodgate.api.FloodgateApi
import org.katacr.kamenu.DialogSessionManager
import org.katacr.kamenu.KaMenu
import org.katacr.kamenu.KaScheduler
import org.katacr.kamenu.MenuActions
import org.katacr.kamenu.MenuArgumentManager
import org.katacr.kamenu.MenuListManager
import org.katacr.kamenu.MenuRequirementChecker
import org.katacr.kamenu.MenuTaskManager
import org.katacr.kamenu.TextParser
import org.katacr.kamenu.dialog.DialogAfterAction
import org.katacr.kamenu.dialog.DialogBodyDefinition
import org.katacr.kamenu.dialog.DialogBottomType
import org.katacr.kamenu.dialog.DialogButtonDefinition
import org.katacr.kamenu.dialog.DialogButtonIcon
import org.katacr.kamenu.dialog.DialogButtonIconType
import org.katacr.kamenu.dialog.DialogDefinition
import org.katacr.kamenu.dialog.DialogDefinitionCompiler
import java.net.URI
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 使用 Floodgate/Cumulus 为基岩版玩家发送带图标的 SimpleForm。
 *
 * 仅无输入组件且至少含一个合法 `icon` 的菜单会进入此路径；其余菜单回退到现有 Java Dialog 转换。
 */
class FloodgateFormAdapter : BedrockFormAdapter {
    private data class ActiveForm(
        val config: YamlConfiguration,
        val contextId: String,
        val definition: DialogDefinition,
        val buttons: List<DialogButtonDefinition>
    )

    private lateinit var plugin: KaMenu
    private lateinit var floodgate: FloodgateApi
    private lateinit var compiler: DialogDefinitionCompiler
    private val activeForms = ConcurrentHashMap<UUID, ActiveForm>()
    private val reportedInvalidIcons = ConcurrentHashMap.newKeySet<String>()
    private val reportedSendFailures = ConcurrentHashMap.newKeySet<String>()
    private val legacySerializer = LegacyComponentSerializer.legacySection()

    /** 取得 Floodgate API 并创建共享菜单编译器。 */
    override fun initialize(plugin: KaMenu) {
        this.plugin = plugin
        floodgate = FloodgateApi.getInstance()
        compiler = DialogDefinitionCompiler(plugin)
    }

    /** 仅接管 Floodgate 玩家及含潜在图标字段的菜单。 */
    override fun tryOpen(
        player: Player,
        config: YamlConfiguration,
        contextId: String,
        runOpenEvent: Boolean,
        fallback: () -> Unit
    ): Boolean {
        if (!floodgate.isFloodgatePlayer(player.uniqueId) || !containsIconConfiguration(config)) {
            return false
        }

        runOnPlayerThread(player) {
            if (!MenuRequirementChecker.check(player, config, plugin)) {
                return@runOnPlayerThread
            }

            if (runOpenEvent && !config.getList("Events.Open").isNullOrEmpty()) {
                MenuActions.executeEvent(player, config, "Open", contextId).whenComplete { shouldStop, error ->
                    if (error != null) {
                        plugin.logger.warning(
                            plugin.languageManager.getMessage(
                                "bedrock_form.open_event_failed",
                                contextId,
                                error.message.toString()
                            )
                        )
                        error.printStackTrace()
                        return@whenComplete
                    }
                    if (shouldStop != true) {
                        runOnPlayerThread(player) { openDirect(player, config, contextId, fallback) }
                    }
                }
            } else {
                openDirect(player, config, contextId, fallback)
            }
        }
        return true
    }

    /** 关闭活动 Cumulus 表单，并使延迟到达的旧回调失效。 */
    override fun close(player: Player): Boolean {
        val removed = activeForms.remove(player.uniqueId) ?: return false
        runCatching { floodgate.closeForm(player.uniqueId) }
            .onFailure { warnSendFailure(removed.contextId, it) }
        return true
    }

    /** 玩家离线后仅移除会话，客户端连接关闭会自动移除表单。 */
    override fun discard(player: Player) {
        activeForms.remove(player.uniqueId)
    }

    /** 清除表单状态并尽力关闭仍在线的基岩版表单。 */
    override fun shutdown() {
        val playerIds = activeForms.keys.toList()
        activeForms.clear()
        playerIds.forEach { playerId -> runCatching { floodgate.closeForm(playerId) } }
        reportedInvalidIcons.clear()
        reportedSendFailures.clear()
    }

    /** 完成依赖检查、跨平台编译和 SimpleForm 适用性判断。 */
    private fun openDirect(
        player: Player,
        config: YamlConfiguration,
        contextId: String,
        fallback: () -> Unit
    ) {
        if (!player.isOnline || !MenuRequirementChecker.check(player, config, plugin)) return
        val definition = try {
            compiler.compile(player, config, contextId)
        } catch (exception: RuntimeException) {
            plugin.logger.warning(
                plugin.languageManager.getMessage(
                    "bedrock_form.compile_failed",
                    contextId,
                    exception.message.toString()
                )
            )
            exception.printStackTrace()
            fallback()
            return
        }

        val buttons = visibleButtons(definition).map { button ->
            button.copy(icon = button.icon?.let { validateIcon(contextId, button.actionPath, it) })
        }
        if (definition.inputs.isNotEmpty() || buttons.none { it.icon != null } ||
            buttons.any { hasClientStaticAction(config, it) }
        ) {
            discard(player)
            fallback()
            return
        }

        val session = ActiveForm(config, contextId, definition, buttons)
        activeForms.put(player.uniqueId, session)
        if (!send(player, session)) {
            activeForms.remove(player.uniqueId, session)
            fallback()
            return
        }
        DialogSessionManager.attach(player, config, contextId)
        MenuTaskManager.attachMenu(player, config, contextId)
    }

    /** 构建并发送 Cumulus SimpleForm，按钮索引直接对应编译后的 KaMenu 按钮。 */
    private fun send(player: Player, session: ActiveForm): Boolean {
        if (!player.isOnline || activeForms[player.uniqueId] !== session) return false
        val builder = SimpleForm.builder()
            .title(toLegacy(session.definition.title, player))
            .content(content(session, player))
            .validResultHandler { response ->
                runOnPlayerThread(player) {
                    handleSelection(player, session, response.clickedButtonId())
                }
            }
        builder.closedResultHandler(Runnable {
            runOnPlayerThread(player) { handleClosed(player, session) }
        })
        builder.invalidResultHandler(Runnable {
            runOnPlayerThread(player) { handleInvalid(player, session) }
        })

        session.buttons.forEach { button ->
            val text = toLegacy(button.text, player)
            val icon = button.icon
            if (icon == null) {
                builder.button(text)
            } else {
                val type = when (icon.type) {
                    DialogButtonIconType.URL -> FormImage.Type.URL
                    DialogButtonIconType.PATH -> FormImage.Type.PATH
                }
                builder.button(text, type, icon.value)
            }
        }

        val result = runCatching { floodgate.sendForm(player.uniqueId, builder.build()) }
            .onFailure { warnSendFailure(session.contextId, it) }
        val sent = result.getOrDefault(false)
        if (!sent && result.isSuccess && reportedSendFailures.add(session.contextId)) {
            plugin.logger.warning(plugin.languageManager.getMessage("bedrock_form.send_rejected", session.contextId))
        }
        return sent
    }

    /** 原子消费一次性按钮响应，并在玩家线程执行原有 KaMenu 动作。 */
    private fun handleSelection(player: Player, session: ActiveForm, buttonIndex: Int) {
        val button = session.buttons.getOrNull(buttonIndex)
        if (button == null) {
            handleInvalid(player, session)
            return
        }
        if (!activeForms.remove(player.uniqueId, session)) return
        executeButton(player, session, button)
    }

    /** 无效响应不触发业务动作，仍在原菜单生命周期内重新发送表单。 */
    private fun handleInvalid(player: Player, session: ActiveForm) {
        if (activeForms[player.uniqueId] !== session) return
        if (!send(player, session)) {
            finishClose(player, session)
        }
    }

    /** 按 can_escape 及底部布局规则处理客户端关闭表单。 */
    private fun handleClosed(player: Player, session: ActiveForm) {
        if (activeForms[player.uniqueId] !== session) return
        if (!session.definition.settings.canEscape) {
            if (!send(player, session)) finishClose(player, session)
            return
        }

        val cancelButton = when (session.definition.bottom.type) {
            DialogBottomType.NOTICE -> session.buttons.firstOrNull()
            DialogBottomType.CONFIRMATION -> session.buttons.getOrNull(1)
            DialogBottomType.MULTI -> session.definition.bottom.exit
        }
        if (cancelButton != null && activeForms.remove(player.uniqueId, session)) {
            executeButton(player, session, cancelButton)
        } else {
            finishClose(player, session)
        }
    }

    /** 执行配置路径或 repeat 合成动作，并保留 Settings.after_action 的关闭语义。 */
    private fun executeButton(player: Player, session: ActiveForm, button: DialogButtonDefinition) {
        MenuActions.executeDialogButton(
            player = player,
            config = session.config,
            actionPath = button.actionPath,
            actionOverride = button.actionOverride,
            variables = button.variables,
            closesDialogAfterAction = session.definition.settings.afterAction == DialogAfterAction.CLOSE,
            contextId = session.contextId
        )
    }

    /** 没有可执行取消按钮时运行 Close 事件并清理菜单任务。 */
    private fun finishClose(player: Player, session: ActiveForm) {
        if (!activeForms.remove(player.uniqueId, session)) return
        val initialTaskToken = MenuTaskManager.currentToken(player)
        val argumentContext = MenuArgumentManager.currentContext(player)
        val finish = { shouldKeepOpen: Boolean ->
            runOnPlayerThread(player) {
                if (shouldKeepOpen && player.isOnline && MenuTaskManager.currentToken(player) == initialTaskToken &&
                    activeForms.putIfAbsent(player.uniqueId, session) == null
                ) {
                    if (!send(player, session)) {
                        activeForms.remove(player.uniqueId, session)
                        clearLifecycle(player, initialTaskToken, argumentContext)
                    }
                } else {
                    clearLifecycle(player, initialTaskToken, argumentContext)
                }
            }
        }

        if (session.config.contains("Events.Close")) {
            MenuActions.executeEvent(player, session.config, "Close", session.contextId).whenComplete { shouldStop, error ->
                if (error != null) {
                    plugin.logger.warning(
                        plugin.languageManager.getMessage(
                            "bedrock_form.close_event_failed",
                            session.contextId,
                            error.message.toString()
                        )
                    )
                    error.printStackTrace()
                }
                finish(error == null && shouldStop == true)
            }
        } else {
            finish(false)
        }
    }

    /** 仅在没有新基岩表单且任务 token 未变化时清理旧菜单生命周期。 */
    private fun clearLifecycle(
        player: Player,
        initialTaskToken: Long?,
        argumentContext: MenuArgumentManager.Context?
    ) {
        if (activeForms.containsKey(player.uniqueId) || MenuTaskManager.currentToken(player) != initialTaskToken) return
        DialogSessionManager.cancel(player)
        MenuTaskManager.cancel(player)
        MenuListManager.clear(player)
        MenuArgumentManager.clearIfCurrent(player, argumentContext)
    }

    /** SimpleForm 为纵向按钮列表，因此忽略只用于 Java Dialog 矩阵补齐的合成按钮。 */
    private fun visibleButtons(definition: DialogDefinition): List<DialogButtonDefinition> {
        val buttons = definition.bottom.buttons.filterNot(DialogButtonDefinition::syntheticPadding)
        return if (definition.bottom.type == DialogBottomType.MULTI) {
            buttons + listOfNotNull(definition.bottom.exit)
        } else {
            buttons
        }
    }

    /** URL/copy 是 Java Dialog 客户端静态动作，遇到时整份菜单回退到现有渲染路径。 */
    private fun hasClientStaticAction(config: YamlConfiguration, button: DialogButtonDefinition): Boolean {
        val actions = button.actionOverride ?: button.actionPath.takeIf(String::isNotBlank)?.let(config::getList)
        val onlyAction = actions?.singleOrNull() as? String ?: return false
        val normalized = onlyAction.trim().lowercase()
        return normalized.startsWith("url:") || normalized.startsWith("copy:")
    }

    /** 校验客户端图片来源，防止控制字符、越界 URL 和路径穿越进入表单数据。 */
    private fun validateIcon(contextId: String, actionPath: String, icon: DialogButtonIcon): DialogButtonIcon? {
        val value = icon.value
        val valid = when (icon.type) {
            DialogButtonIconType.URL -> value.length <= MAX_URL_LENGTH && value.none(Char::isISOControl) && runCatching {
                val uri = URI(value)
                (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) && !uri.host.isNullOrBlank()
            }.getOrDefault(false)

            DialogButtonIconType.PATH -> value.length <= MAX_PATH_LENGTH && value.none(Char::isISOControl) &&
                !value.startsWith('/') && !value.startsWith('\\') &&
                value.replace('\\', '/').split('/').none { it == ".." }
        }
        if (valid) return icon

        val reportId = "$contextId:$actionPath:${icon.type}:$value"
        if (reportedInvalidIcons.add(reportId)) {
            plugin.logger.warning(
                plugin.languageManager.getMessage(
                    "bedrock_form.invalid_icon",
                    contextId,
                    actionPath.ifBlank { "<unknown>" }
                )
            )
        }
        return null
    }

    /** 将消息 Body 合并为基岩版 SimpleForm 的 content；物品 Body 与现有 Geyser 行为一致地忽略。 */
    private fun content(session: ActiveForm, player: Player): String = session.definition.body
        .filterIsInstance<DialogBodyDefinition.Message>()
        .joinToString("\n") { message ->
            legacySerializer.serialize(
                MenuActions.parseClickableText(message.text, player, session.config, null)
            )
        }

    /** 将 KaMenu 文本转换为 Cumulus 支持的 section-sign legacy 文本。 */
    private fun toLegacy(value: String, player: Player): String =
        legacySerializer.serialize(TextParser.parseText(value, player))

    /** 检查 YAML 中是否声明了普通或结构化 icon 字段。 */
    private fun containsIconConfiguration(config: YamlConfiguration): Boolean =
        config.getKeys(true).any { key ->
            key.startsWith("Bottom.", true) && key.endsWith(".icon", true)
        }

    /** 确保所有 Floodgate 回调重新进入 Bukkit/Paper/Folia 允许的玩家执行线程。 */
    private fun runOnPlayerThread(player: Player, action: () -> Unit) {
        if (KaScheduler.isPlayerThread(player)) {
            action()
        } else {
            KaScheduler.runPlayer(player, Runnable(action))
        }
    }

    /** 对单个会话发送失败输出本地化错误。 */
    private fun warnSendFailure(contextId: String, error: Throwable) {
        plugin.logger.warning(
            plugin.languageManager.getMessage(
                "bedrock_form.send_failed",
                contextId,
                error.message.toString()
            )
        )
    }

    private companion object {
        const val MAX_URL_LENGTH = 2048
        const val MAX_PATH_LENGTH = 512
    }
}
