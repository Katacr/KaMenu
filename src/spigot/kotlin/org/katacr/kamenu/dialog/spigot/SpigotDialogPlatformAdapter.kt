package org.katacr.kamenu.dialog.spigot

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ClickEventCustom
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.dialog.ConfirmationDialog
import net.md_5.bungee.api.dialog.Dialog
import net.md_5.bungee.api.dialog.DialogBase
import net.md_5.bungee.api.dialog.MultiActionDialog
import net.md_5.bungee.api.dialog.NoticeDialog
import net.md_5.bungee.api.dialog.action.Action
import net.md_5.bungee.api.dialog.action.ActionButton
import net.md_5.bungee.api.dialog.action.CustomClickAction
import net.md_5.bungee.api.dialog.action.StaticAction
import net.md_5.bungee.api.dialog.body.DialogBody
import net.md_5.bungee.api.dialog.body.PlainMessageBody
import net.md_5.bungee.api.dialog.input.BooleanInput
import net.md_5.bungee.api.dialog.input.DialogInput
import net.md_5.bungee.api.dialog.input.InputOption
import net.md_5.bungee.api.dialog.input.NumberRangeInput
import net.md_5.bungee.api.dialog.input.SingleOptionInput
import net.md_5.bungee.api.dialog.input.TextInput
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCustomClickEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.katacr.kamenu.DialogSessionManager
import org.katacr.kamenu.InputCaptureUtils
import org.katacr.kamenu.KaMenu
import org.katacr.kamenu.MenuActions
import org.katacr.kamenu.MenuManager
import org.katacr.kamenu.MenuRequirementChecker
import org.katacr.kamenu.MenuTaskManager
import org.katacr.kamenu.PauseEntryDatapackManager
import org.katacr.kamenu.TextParser
import org.katacr.kamenu.dialog.DialogAfterAction
import org.katacr.kamenu.dialog.DialogBodyDefinition
import org.katacr.kamenu.dialog.DialogBottomType
import org.katacr.kamenu.dialog.DialogButtonDefinition
import org.katacr.kamenu.dialog.ClickableTextTagScanner
import org.katacr.kamenu.dialog.DialogDefinition
import org.katacr.kamenu.dialog.DialogDefinitionCompiler
import org.katacr.kamenu.dialog.DialogInputDefinition
import org.katacr.kamenu.dialog.DialogInputSchema
import org.katacr.kamenu.dialog.DialogOptionDefinition
import org.katacr.kamenu.dialog.DialogPlatformAdapter
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 使用 Spigot 1.21.6+ 的 Bungee Dialog API 渲染 KaMenu 的中立 Dialog 定义。
 *
 * 按钮通过一次性 custom-click session 回传输入，随后复用 KaMenu 的通用 actions、Events 与 Tasks 执行器。
 * session 只在服务端保存可信动作路径和玩家身份，客户端提交内容仅作为已声明 input 的候选值。
 */
class SpigotDialogPlatformAdapter : DialogPlatformAdapter, Listener {
    companion object {
        private const val MAX_INPUT_VALUE_LENGTH = 32768
    }

    /** 服务端保存的可信按钮回调上下文。 */
    private data class CallbackSession(
        val playerId: UUID,
        val config: YamlConfiguration,
        val actionPath: String?,
        val actionReference: String?,
        val actionOverride: List<*>?,
        val initialVariables: Map<String, String>,
        val inputSchema: DialogInputSchema,
        val inputDefinitions: List<DialogInputDefinition>,
        val contextId: String,
        val closesDialogAfterAction: Boolean,
        val expiresAtMillis: Long
    )

    /** 保存一次 Dialog 渲染期间各按钮共同使用的平台中立上下文。 */
    private data class RenderContext(
        val player: Player,
        val config: YamlConfiguration,
        val inputSchema: DialogInputSchema,
        val inputDefinitions: List<DialogInputDefinition>,
        val contextId: String,
        val closesDialogAfterAction: Boolean
    )

    private val callbacks = ConcurrentHashMap<String, CallbackSession>()
    private val playerCallbacks = ConcurrentHashMap<UUID, MutableSet<String>>()
    private val reportedItemMappingFailures = ConcurrentHashMap.newKeySet<String>()
    private val itemMapper = SpigotPublicItemMapper()
    private lateinit var plugin: KaMenu
    private lateinit var compiler: DialogDefinitionCompiler

    override val platformName: String = "Spigot"

    /** 在运行时能力检测完成后注册 Spigot custom-click 监听器。 */
    override fun initialize(plugin: KaMenu) {
        this.plugin = plugin
        compiler = DialogDefinitionCompiler(plugin)
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    /** 在服务端主线程打开已加载菜单。 */
    override fun openMenu(player: Player, menuId: String, manager: MenuManager, plugin: KaMenu) {
        runOnPrimaryThread(player) {
            val config = manager.getMenuConfig(menuId)
            if (config == null) {
                player.sendMessage(plugin.languageManager.getMessage("menu.not_found", menuId))
                return@runOnPrimaryThread
            }
            openWithLifecycle(player, config, menuId)
        }
    }

    /** 在服务端主线程打开外部内存配置。 */
    override fun openConfig(player: Player, config: YamlConfiguration, plugin: KaMenu, contextId: String) {
        runOnPrimaryThread(player) { openWithLifecycle(player, config, contextId) }
    }

    /** 强制打开菜单，不重复执行 Events.Open。 */
    override fun forceOpenMenu(player: Player, menuId: String, manager: MenuManager, plugin: KaMenu) {
        runOnPrimaryThread(player) {
            val config = manager.getMenuConfig(menuId)
            if (config == null) {
                player.sendMessage(plugin.languageManager.getMessage("menu.not_found", menuId))
                return@runOnPrimaryThread
            }
            openDirect(player, config, menuId)
        }
    }

    /** 强制打开外部内存菜单，不重复执行 Events.Open。 */
    override fun forceOpenConfig(player: Player, config: YamlConfiguration, plugin: KaMenu, contextId: String) {
        runOnPrimaryThread(player) { openDirect(player, config, contextId) }
    }

    /** 清除原生 Dialog 及其 KaMenu 回调状态。 */
    override fun close(player: Player) {
        clearCallbacks(player.uniqueId)
        player.clearDialog()
    }

    /** 使用 Spigot Bungee Chat API 发送 Adventure 富文本。 */
    override fun sendMessage(sender: CommandSender, message: Component) {
        sender.spigot().sendMessage(*BungeeComponentSerializer.get().serialize(message))
    }

    /** 使用 Spigot Bungee Chat API 发送 ActionBar。 */
    override fun sendActionBar(player: Player, message: Component) {
        player.spigot().sendMessage(
            ChatMessageType.ACTION_BAR,
            *BungeeComponentSerializer.get().serialize(message)
        )
    }

    /** Spigot 仅暴露字符串标题 API，因此转换为保留颜色的 legacy 文本。 */
    override fun showTitle(
        player: Player,
        title: Component,
        subtitle: Component,
        fadeIn: Int,
        stay: Int,
        fadeOut: Int
    ) {
        val titleText = BaseComponent.toLegacyText(*BungeeComponentSerializer.get().serialize(title))
        val subtitleText = BaseComponent.toLegacyText(*BungeeComponentSerializer.get().serialize(subtitle))
        player.sendTitle(titleText, subtitleText, fadeIn.coerceAtLeast(0), stay.coerceAtLeast(0), fadeOut.coerceAtLeast(0))
    }

    /** 发送支持 actions 服务端回调的 Spigot 可点击聊天文本。 */
    override fun sendClickableText(
        player: Player,
        rawText: String,
        config: YamlConfiguration?,
        contextId: String?
    ) {
        val resolvedConfig = config ?: YamlConfiguration()
        val emptySchema = DialogInputSchema(emptyList(), emptyMap(), emptyMap(), emptyMap())
        val context = RenderContext(
            player,
            resolvedConfig,
            emptySchema,
            emptyList(),
            contextId ?: "hovertext",
            false
        )
        player.spigot().sendMessage(clickableText(context, rawText))
    }

    /** 使用 Spigot 字符串元数据构造可跨平台消费的名称组件。 */
    override fun itemName(item: ItemStack): Component {
        val meta = item.itemMeta
        if (meta?.hasDisplayName() == true) {
            return LegacyComponentSerializer.legacySection().deserialize(meta.displayName)
        }
        val fallback = item.type.name.lowercase(Locale.ROOT)
            .split('_')
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercaseChar() } }
        return Component.translatable(item.translationKey, fallback)
    }

    /** 将 Spigot legacy Lore 转换为 Adventure 组件。 */
    override fun itemLore(meta: ItemMeta): List<Component> {
        if (!meta.hasLore()) return emptyList()
        return meta.lore.orEmpty().map(LegacyComponentSerializer.legacySection()::deserialize)
    }

    /** 读取 Spigot ItemModel 命名空间键。 */
    override fun itemModel(meta: ItemMeta): String? =
        meta.itemModel?.takeIf { meta.hasItemModel() }?.toString()

    /** 插件关闭时释放全部待处理回调状态。 */
    override fun shutdown() {
        callbacks.clear()
        playerCallbacks.clear()
        reportedItemMappingFailures.clear()
    }

    /** 验证并消费一次性按钮 session，然后执行平台中立的 KaMenu 动作列表。 */
    @EventHandler
    fun onCustomClick(event: PlayerCustomClickEvent) {
        if (event.id.toString() == PauseEntryDatapackManager.ACTION_KEY) {
            val values = primitiveValues(event.data)
            runOnPrimaryThread(event.player) {
                plugin.pauseEntryDatapackManager.handleRegisteredTarget(
                    event.player,
                    values["target"],
                    values
                )
            }
            return
        }
        dispatchCallback(event.player, event.id.toString(), event.data)
    }

    /** 将 Spigot custom-click JSON 中的基础值转换为平台中立输入表。 */
    private fun primitiveValues(payload: JsonElement?): Map<String, String> {
        if (payload?.isJsonObject != true) return emptyMap()
        return buildMap {
            payload.asJsonObject.entrySet().forEach { (key, value) ->
                if (value.isJsonPrimitive) put(key, value.asString)
            }
        }
    }

    /** 校验并原子消费玩家绑定的一次性回调，然后执行对应 KaMenu 动作。 */
    private fun dispatchCallback(player: Player, callbackId: String, payload: JsonElement?) {
        val session = callbacks[callbackId] ?: return
        if (session.playerId != player.uniqueId) return
        if (!callbacks.remove(callbackId, session)) return
        removePlayerCallbackIndex(session.playerId, callbackId)
        if (session.expiresAtMillis < System.currentTimeMillis()) return

        val variables = session.initialVariables.toMutableMap()
        variables.putAll(captureInputs(payload, session))
        if (session.closesDialogAfterAction) {
            clearCallbacks(player.uniqueId)
        }
        if (session.actionReference != null) {
            MenuActions.executeActionReference(
                player,
                session.config,
                session.actionReference,
                variables,
                session.contextId,
                session.closesDialogAfterAction
            )
        } else if (session.actionOverride != null) {
            MenuActions.executeActionGroup(
                player,
                session.config,
                session.actionOverride,
                variables,
                contextId = session.contextId
            )
        } else {
            MenuActions.executeConfigActionPath(
                player,
                session.config,
                session.actionPath.orEmpty(),
                variables,
                null,
                session.closesDialogAfterAction,
                session.contextId
            )
        }
    }

    /** 玩家离线时释放其回调状态。 */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        clearCallbacks(event.player.uniqueId)
    }

    private fun openWithLifecycle(player: Player, config: YamlConfiguration, contextId: String) {
        if (!MenuRequirementChecker.check(player, config, plugin)) {
            return
        }

        val openActions = config.getList("Events.Open")
        if (openActions.isNullOrEmpty()) {
            openDirect(player, config, contextId)
            return
        }
        MenuActions.executeEvent(player, config, "Open", contextId).whenComplete { shouldStop, error ->
            if (error != null) {
                plugin.logger.severe(
                    plugin.languageManager.getMessage(
                        "spigot_dialog.open_event_failed",
                        contextId,
                        error.message.toString()
                    )
                )
                error.printStackTrace()
                return@whenComplete
            }
            if (shouldStop != true) {
                runOnPrimaryThread(player) { openDirect(player, config, contextId) }
            }
        }
    }

    private fun openDirect(player: Player, config: YamlConfiguration, contextId: String) {
        clearCallbacks(player.uniqueId)
        if (!MenuRequirementChecker.check(player, config, plugin)) return

        val definition = try {
            compiler.compile(player, config, contextId)
        } catch (exception: RuntimeException) {
            plugin.logger.warning(
                plugin.languageManager.getMessage(
                    "spigot_dialog.compile_failed",
                    contextId,
                    exception.message.toString()
                )
            )
            exception.printStackTrace()
            return
        }

        try {
            player.showDialog(dialog(player, config, definition, contextId))
            DialogSessionManager.attach(player, config, contextId)
            MenuTaskManager.attachMenu(player, config, contextId)
        } catch (exception: RuntimeException) {
            clearCallbacks(player.uniqueId)
            plugin.logger.warning(
                plugin.languageManager.getMessage(
                    "spigot_dialog.render_failed",
                    contextId,
                    exception.message.toString()
                )
            )
            exception.printStackTrace()
        }
    }

    private fun dialog(
        player: Player,
        config: YamlConfiguration,
        definition: DialogDefinition,
        contextId: String
    ): Dialog {
        val context = RenderContext(
            player,
            config,
            definition.inputSchema,
            definition.inputs,
            contextId,
            definition.settings.afterAction == DialogAfterAction.CLOSE
        )
        val base = DialogBase(text(player, definition.title))
            .body(body(context, definition.body))
            .inputs(inputs(player, definition.inputs))
            .canCloseWithEscape(definition.settings.canEscape)
            .pause(definition.settings.pause)
            .afterAction(DialogBase.AfterAction.valueOf(definition.settings.afterAction.name))
        return when (definition.bottom.type) {
            DialogBottomType.NOTICE -> notice(context, base, definition.bottom.buttons)
            DialogBottomType.CONFIRMATION -> confirmation(context, base, definition.bottom.buttons)
            DialogBottomType.MULTI -> multi(context, base, definition)
        }
    }

    private fun notice(
        context: RenderContext,
        base: DialogBase,
        definitions: List<DialogButtonDefinition>
    ): NoticeDialog = if (definitions.isEmpty()) {
        NoticeDialog(base)
    } else {
        NoticeDialog(base, button(context, definitions.first()))
    }

    private fun confirmation(
        context: RenderContext,
        base: DialogBase,
        definitions: List<DialogButtonDefinition>
    ): ConfirmationDialog {
        val yes = definitions.getOrNull(0) ?: DialogButtonDefinition("确认", null, null, "")
        val no = definitions.getOrNull(1) ?: DialogButtonDefinition("取消", null, null, "")
        return ConfirmationDialog(base, button(context, yes), button(context, no))
    }

    private fun multi(context: RenderContext, base: DialogBase, definition: DialogDefinition): Dialog {
        val buttons = definition.bottom.buttons.map { button(context, it) }
        if (buttons.isEmpty()) return NoticeDialog(base)
        val exit = definition.bottom.exit?.let { button(context, it) }
        return MultiActionDialog(base, buttons, definition.bottom.columns, exit)
    }

    private fun body(
        context: RenderContext,
        definitions: List<DialogBodyDefinition>
    ): List<DialogBody> = definitions.map { definition ->
        when (definition) {
            is DialogBodyDefinition.Message -> PlainMessageBody(
                clickableText(context, definition.text),
                definition.width
            )

            is DialogBodyDefinition.Item -> {
                val description = definition.description?.let {
                    PlainMessageBody(clickableText(context, it), definition.descriptionWidth)
                }
                SpigotItemDialogBody(
                    mapItem(context, definition),
                    description,
                    definition.showOverlays,
                    definition.showTooltip,
                    definition.width,
                    definition.height
                )
            }
        }
    }

    /** 使用 Bukkit 公共 API 映射物品；单个属性异常时回退到基础物品。 */
    private fun mapItem(context: RenderContext, definition: DialogBodyDefinition.Item): JsonObject {
        val item = definition.itemStack
        return try {
            itemMapper.map(item)
        } catch (error: RuntimeException) {
            warnItemMappingOnce(context.contextId, definition.componentId, describeError(error))
            SpigotItemDialogBody.basicItem(item.type.key.toString(), item.amount)
        }
    }

    /** 同一菜单组件的公共属性映射错误只记录一次，避免周期刷新刷屏。 */
    private fun warnItemMappingOnce(contextId: String, componentId: String, error: String?) {
        if (reportedItemMappingFailures.add("$contextId:$componentId")) {
            plugin.logger.warning(
                plugin.languageManager.getMessage(
                    "spigot_dialog.item_mapping_failed",
                    contextId,
                    componentId,
                    error ?: "unknown error"
                )
            )
        }
    }

    /** 展开包装异常，给管理员保留真正的属性映射失败原因。 */
    private fun describeError(error: Throwable): String {
        var cause = error
        while (cause.cause != null && cause.cause !== cause) {
            cause = cause.cause!!
        }
        return "${cause.javaClass.simpleName}: ${cause.message ?: "no message"}"
    }

    private fun inputs(player: Player, definitions: List<DialogInputDefinition>): List<DialogInput> =
        definitions.map { definition ->
            when (definition) {
                is DialogInputDefinition.Text -> TextInput(
                    definition.key,
                    definition.width,
                    text(player, definition.label),
                    definition.labelVisible,
                    definition.initial,
                    definition.maxLength
                ).apply {
                    definition.multiline?.let { multiline(TextInput.Multiline(it.maxLines, it.height)) }
                }

                is DialogInputDefinition.Slider -> NumberRangeInput(
                    definition.key,
                    definition.width,
                    text(player, definition.label),
                    definition.format,
                    definition.minimum,
                    definition.maximum,
                    definition.step,
                    definition.initial
                )

                is DialogInputDefinition.Checkbox -> BooleanInput(
                    definition.key,
                    text(player, definition.label),
                    definition.initial,
                    definition.onTrue,
                    definition.onFalse
                )

                is DialogInputDefinition.Dropdown -> SingleOptionInput(
                    definition.key,
                    definition.width,
                    text(player, definition.label),
                    definition.labelVisible,
                    definition.options.map { option(player, it) }
                )
            }
        }

    private fun option(player: Player, option: DialogOptionDefinition): InputOption =
        InputOption(option.id, text(player, option.display), option.initial)

    private fun button(context: RenderContext, definition: DialogButtonDefinition): ActionButton {
        val tooltip = definition.tooltip?.let { text(context.player, it) }
        return ActionButton(
            text(context.player, definition.text),
            tooltip,
            definition.width,
            action(context, definition)
        )
    }

    /** 保留纯客户端动作，其余按钮注册服务端一次性 callback。 */
    private fun action(context: RenderContext, definition: DialogButtonDefinition): Action {
        val path = definition.actionPath
        val actions = definition.actionOverride ?: if (path.isBlank()) emptyList<Any>() else context.config.getList(path)
        val onlyAction = actions?.singleOrNull()
        if (onlyAction is String) {
            val trimmed = onlyAction.trim()
            if (trimmed.startsWith("url:")) {
                return StaticAction(ClickEvent(ClickEvent.Action.OPEN_URL, trimmed.substring(4).trim()))
            }
            if (trimmed.startsWith("copy:")) {
                return StaticAction(ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, trimmed.substring(5).trim()))
            }
        }
        val callbackId = registerCallback(
            context,
            path,
            null,
            definition.actionOverride,
            definition.variables,
            context.inputSchema,
            context.inputDefinitions
        )
        return CustomClickAction(callbackId)
    }

    /** 解析正文可点击文本，并将 actions 引用绑定到服务端一次性 callback。 */
    private fun clickableText(context: RenderContext, rawText: String): BaseComponent {
        val tags = ClickableTextTagScanner.scan(rawText)
        if (tags.isEmpty()) return text(context.player, rawText)

        val result = TextComponent()
        var cursor = 0
        tags.forEach { tag ->
            result.addExtra(text(context.player, rawText.substring(cursor, tag.startIndex)))
            val attributes = attributes(tag.content)
            val clickable = text(context.player, attributes["text"].orEmpty())
            attributes["hover"]?.let {
                clickable.hoverEvent = HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    arrayOf(text(context.player, it))
                )
            }
            (attributes["hover_item"] ?: attributes["hover-item"])?.let { source ->
                MenuActions.resolveHoverItem(context.player, source)?.let { hoverItem ->
                    clickable.hoverEvent = HoverEvent(
                        HoverEvent.Action.SHOW_ITEM,
                        SpigotHoverItemContent.from(itemMapper.map(hoverItem))
                    )
                }
            }
            clickable.clickEvent = when {
                "actions" in attributes -> {
                    val emptySchema = DialogInputSchema(emptyList(), emptyMap(), emptyMap(), emptyMap())
                    val callbackId = registerCallback(
                        context,
                        null,
                        attributes["actions"],
                        null,
                        emptyMap(),
                        emptySchema,
                        emptyList()
                    )
                    ClickEventCustom(callbackId, "")
                }

                "copy" in attributes -> ClickEvent(
                    ClickEvent.Action.COPY_TO_CLIPBOARD,
                    attributes.getValue("copy")
                )

                "command" in attributes -> ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    commandValue(attributes.getValue("command"))
                )

                "url" in attributes -> ClickEvent(
                    ClickEvent.Action.OPEN_URL,
                    attributes.getValue("url")
                )

                else -> null
            }
            result.addExtra(clickable)
            if (attributes["newline"].toBoolean()) {
                result.addExtra(TextComponent("\n"))
            }
            cursor = tag.endIndex + 1
        }
        result.addExtra(text(context.player, rawText.substring(cursor)))
        return result
    }

    private fun attributes(content: String): Map<String, String> = buildMap {
        content.split(';').forEach { part ->
            val equals = part.indexOf('=')
            if (equals < 0) return@forEach
            val key = part.substring(0, equals).trim().lowercase(Locale.ROOT)
            var value = part.substring(equals + 1).trim()
            if (
                (value.startsWith('`') && value.endsWith('`')) ||
                (value.startsWith('\'') && value.endsWith('\'')) ||
                (value.startsWith('"') && value.endsWith('"'))
            ) {
                value = value.substring(1, value.length - 1)
            }
            put(key, value)
        }
    }

    private fun commandValue(command: String): String = if (command.startsWith('/')) command else "/$command"

    private fun text(player: Player, value: String): BaseComponent {
        val parsed = TextParser.parseText(value, player)
        return TextComponent(*BungeeComponentSerializer.get().serialize(parsed))
    }

    /** 注册一个仅允许指定玩家在当前菜单有效期内消费一次的可信回调。 */
    private fun registerCallback(
        context: RenderContext,
        actionPath: String?,
        actionReference: String?,
        actionOverride: List<*>?,
        variables: Map<String, String>,
        inputSchema: DialogInputSchema,
        inputDefinitions: List<DialogInputDefinition>
    ): String {
        val key = "kamenu:dialog_${UUID.randomUUID().toString().replace("-", "")}"
        callbacks[key] = CallbackSession(
            context.player.uniqueId,
            context.config,
            actionPath,
            actionReference,
            actionOverride,
            variables.toMap(),
            inputSchema,
            inputDefinitions.toList(),
            context.contextId,
            context.closesDialogAfterAction,
            System.currentTimeMillis() + DialogSessionManager.lifetimeSeconds(context.config) * 1000L
        )
        playerCallbacks.computeIfAbsent(context.player.uniqueId) {
            ConcurrentHashMap.newKeySet<String>()
        }.add(key)
        return key
    }

    /** 从玩家索引移除已消费或已失效的回调。 */
    private fun removePlayerCallbackIndex(playerId: UUID, key: String) {
        val keys = playerCallbacks[playerId] ?: return
        keys.remove(key)
        if (keys.isEmpty()) {
            playerCallbacks.remove(playerId, keys)
        }
    }

    /** 清理玩家当前 Dialog 注册的全部未消费回调。 */
    private fun clearCallbacks(playerId: UUID) {
        playerCallbacks.remove(playerId)?.forEach(callbacks::remove)
    }

    /** 从不可信 JSON 中只提取并校验当前 Dialog 声明过的输入字段。 */
    private fun captureInputs(payload: JsonElement?, session: CallbackSession): Map<String, String> {
        val objectValue = if (payload?.isJsonObject == true) payload.asJsonObject else JsonObject()
        val definitions = session.inputDefinitions.associateBy(DialogInputDefinition::key)
        val rawValues = buildMap<String, String> {
            session.inputSchema.keys.forEach { key ->
                val element = objectValue.get(key)
                if (element?.isJsonPrimitive != true) return@forEach
                validateInput(element.asString, definitions[key])?.let { put(key, it) }
            }
        }
        val schema = InputCaptureUtils.Schema(
            session.inputSchema.keys,
            session.inputSchema.types,
            session.inputSchema.removeChars,
            session.inputSchema.checkboxMappings
        )
        return InputCaptureUtils.captureVariables(plugin, rawValues, schema)
    }

    /** 按输入组件定义限制长度、范围和可选值，拒绝伪造值。 */
    private fun validateInput(value: String, definition: DialogInputDefinition?): String? {
        if (value.length > MAX_INPUT_VALUE_LENGTH || definition == null) return null
        return when (definition) {
            is DialogInputDefinition.Text -> value.take(definition.maxLength)
            is DialogInputDefinition.Slider -> {
                val number = value.toDoubleOrNull() ?: return null
                value.takeIf {
                    number.isFinite() && number >= definition.minimum && number <= definition.maximum
                }
            }

            is DialogInputDefinition.Dropdown -> value.takeIf { candidate ->
                definition.options.any { it.id == candidate }
            }

            is DialogInputDefinition.Checkbox -> value.takeIf {
                it.equals("true", true) || it.equals("false", true) ||
                    it == "1" || it == "0" ||
                    it.equals("yes", true) || it.equals("no", true) ||
                    it == definition.onTrue || it == definition.onFalse
            }
        }
    }

    private fun runOnPrimaryThread(@Suppress("UNUSED_PARAMETER") player: Player, task: () -> Unit) {
        if (Bukkit.isPrimaryThread()) {
            task()
        } else {
            Bukkit.getScheduler().runTask(plugin, Runnable(task))
        }
    }
}
