package org.katacr.kamenu.dialog

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.katacr.kamenu.ConditionUtils
import org.katacr.kamenu.ExternalItemAdapter
import org.katacr.kamenu.InputCaptureUtils
import org.katacr.kamenu.JavaScriptManager
import org.katacr.kamenu.KaMenu
import org.katacr.kamenu.MaterialUtils
import org.katacr.kamenu.MenuListManager
import org.katacr.kamenu.TextParser
import org.katacr.kamenu.TextResolver
import java.net.URL
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 将 KaMenu YAML 转换为平台中立的 [DialogDefinition]。
 *
 * 编译阶段完成条件、变量、repeat 分页和输入配置解析；各平台渲染器不再直接理解 YAML 结构。
 */
class DialogDefinitionCompiler(private val plugin: KaMenu) {
    private data class DropdownOption(val id: String, val display: String)
    private data class RepeatItem(val values: Map<String, String>)
    private val reportedLimitations = ConcurrentHashMap.newKeySet<String>()

    /** 编译一份已加载或内存中的菜单配置。 */
    fun compile(player: Player, config: YamlConfiguration, contextId: String): DialogDefinition {
        val inputTypes = linkedMapOf<String, String>()
        val inputRemoveChars = linkedMapOf<String, String>()
        val checkboxMappings = linkedMapOf<String, Pair<String, String>>()
        val inputKeys = mutableListOf<String>()

        val title = getString(player, config, "Title", "KaMenu")
            .let { resolve(player, it, contextId, config = config) }
        val settings = compileSettings(player, config)
        val body = compileBody(player, config, contextId)
        val inputs = compileInputs(player, config, inputKeys, inputTypes, inputRemoveChars, checkboxMappings)
        val schema = DialogInputSchema(inputKeys, inputTypes, inputRemoveChars, checkboxMappings)
        val bottom = compileBottom(player, config, contextId)
        return DialogDefinition(title, body, inputs, bottom, settings, schema)
    }

    private fun compileSettings(player: Player, config: YamlConfiguration): DialogSettings {
        val settings = config.getConfigurationSection("Settings")
        val canEscape = settings?.let { getBoolean(player, it, "can_escape", true) } ?: true
        val pause = settings?.let { getBoolean(player, it, "pause", false) } ?: false
        val afterRaw = settings?.let { getString(player, it, "after_action", "CLOSE") } ?: "CLOSE"
        val afterAction = runCatching { DialogAfterAction.valueOf(afterRaw.uppercase()) }
            .getOrDefault(DialogAfterAction.CLOSE)
        return DialogSettings(canEscape, pause, afterAction)
    }

    private fun compileBody(
        player: Player,
        config: YamlConfiguration,
        contextId: String
    ): List<DialogBodyDefinition> {
        val result = mutableListOf<DialogBodyDefinition>()
        val section = config.getConfigurationSection("Body") ?: return result
        for (key in section.getKeys(false)) {
            val type = getType(player, section, "$key.type", "message").lowercase()
            when (type) {
                "none" -> Unit
                "message" -> {
                    val path = "$key.text"
                    val raw = getString(player, section, path, "")
                    val resolved = resolve(player, raw, contextId, config = config)
                    val text = normalizeMessageText(section.isList(path), resolved)
                    val width = getInt(player, section, "$key.width", 0).takeIf { it > 0 }
                    result += DialogBodyDefinition.Message(text, width)
                }

                "item" -> result += compileItemBody(player, section, key, contextId, config)
                else -> warnOnce(
                    "$contextId:$key:type:$type",
                    "spigot_dialog.unsupported_body",
                    contextId,
                    key,
                    type
                )
            }
        }
        return result
    }

    /** 生成包含完整 Bukkit ItemStack 的跨平台物品定义。 */
    private fun compileItemBody(
        player: Player,
        section: ConfigurationSection,
        key: String,
        contextId: String,
        config: YamlConfiguration
    ): DialogBodyDefinition.Item {
        val configuredMaterial = resolve(
            player,
            getString(player, section, "$key.material", "paper"),
            contextId,
            config = config
        )
        val item = resolveItemStack(
            player,
            configuredMaterial,
            getInt(player, section, "$key.amount", 1),
            contextId,
            key
        )
        if (!isSlotReference(configuredMaterial)) {
            applyConfiguredItemMeta(player, item, section, key, contextId, config)
        }
        val description = getString(player, section, "$key.description", "")
            .takeIf { it.isNotEmpty() }
            ?.let { resolve(player, it, contextId, config = config) }
        return DialogBodyDefinition.Item(
            componentId = key,
            itemStack = item,
            description = description,
            descriptionWidth = getInt(player, section, "$key.description_width", 0).takeIf { it > 0 },
            showOverlays = getBoolean(player, section, "$key.show_overlays", true),
            showTooltip = getBoolean(player, section, "$key.show_tooltip", true),
            width = getInt(player, section, "$key.width", 16),
            height = getInt(player, section, "$key.height", 16)
        )
    }

    /** 将菜单中显式配置的名称、Lore、模型和头颅属性覆盖到物品上。 */
    private fun applyConfiguredItemMeta(
        player: Player,
        item: ItemStack,
        section: ConfigurationSection,
        key: String,
        contextId: String,
        config: YamlConfiguration
    ) {
        val meta = item.itemMeta ?: return
        val name = resolve(player, getString(player, section, "$key.name", ""), contextId, config = config)
        if (name.isNotEmpty()) {
            meta.setDisplayName(toLegacy(player, name))
        }
        val lore = getStringList(player, section, "$key.lore")
        if (lore.isNotEmpty()) {
            meta.lore = lore.map { toLegacy(player, resolve(player, it, contextId, config = config)) }
        }

        val customModelData = resolve(
            player,
            getString(player, section, "$key.custom_model_data", ""),
            contextId,
            config = config
        )
        if (customModelData.isNotEmpty()) {
            customModelData.toIntOrNull()?.let(meta::setCustomModelData) ?: warnOnce(
                "$contextId:$key:custom-model-data:$customModelData",
                "spigot_dialog.invalid_item_property",
                contextId,
                key,
                "custom_model_data",
                customModelData
            )
        }

        val itemModel = resolve(
            player,
            getString(player, section, "$key.item_model", ""),
            contextId,
            config = config
        )
        if (itemModel.isNotEmpty()) {
            val modelKey = if (':' in itemModel) NamespacedKey.fromString(itemModel) else NamespacedKey.minecraft(itemModel)
            if (modelKey != null) {
                meta.itemModel = modelKey
            } else {
                warnOnce(
                    "$contextId:$key:item-model:$itemModel",
                    "spigot_dialog.invalid_item_property",
                    contextId,
                    key,
                    "item_model",
                    itemModel
                )
            }
        }

        if (meta is SkullMeta) {
            val texture = resolve(
                player,
                getString(player, section, "$key.skull_texture", ""),
                contextId,
                config = config
            )
            val owner = resolve(
                player,
                getString(player, section, "$key.skull_owner", ""),
                contextId,
                config = config
            )
            when {
                texture.isNotEmpty() -> applySkullTexture(meta, texture, contextId, key)
                owner.isNotEmpty() -> meta.owningPlayer = Bukkit.getOfflinePlayer(owner)
            }
        }
        item.itemMeta = meta
    }

    /** 将 Base64 头颅纹理转换为 Bukkit PlayerProfile，避免依赖 Paper ProfileProperty。 */
    private fun applySkullTexture(meta: SkullMeta, texture: String, contextId: String, componentId: String) {
        val skinUrl = runCatching {
            val decoded = String(Base64.getDecoder().decode(texture), Charsets.UTF_8)
            val rawUrl = Regex("\\\"url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
                .find(decoded)?.groupValues?.get(1)
                ?: error("texture URL is missing")
            URL(rawUrl.replace("\\\\/", "/"))
        }.getOrElse {
            warnOnce(
                "$contextId:$componentId:skull-texture",
                "spigot_dialog.invalid_item_property",
                contextId,
                componentId,
                "skull_texture",
                "invalid Base64 texture"
            )
            return
        }
        val profile = Bukkit.createPlayerProfile(UUID.nameUUIDFromBytes(texture.toByteArray()), "custom_head")
        val textures = profile.textures
        textures.skin = skinUrl
        profile.setTextures(textures)
        meta.ownerProfile = profile
    }

    private fun toLegacy(player: Player, text: String): String =
        LegacyComponentSerializer.legacySection().serialize(TextParser.parseText(text, player))

    /** 对同一菜单组件的兼容限制只输出一次本地化警告。 */
    private fun warnOnce(id: String, messageKey: String, vararg args: Any) {
        if (reportedLimitations.add(id)) {
            plugin.logger.warning(plugin.languageManager.getMessage(messageKey, *args))
        }
    }

    /** 解析槽位、手持、外部物品 ID 或原版材质为完整 ItemStack。 */
    private fun resolveItemStack(
        player: Player,
        source: String,
        defaultAmount: Int,
        contextId: String,
        componentId: String
    ): ItemStack {
        val normalized = source.trim()
        val slot = normalized.substringAfter("slot:", missingDelimiterValue = "").trim().toIntOrNull()
        val bracketItem = resolveBracketSlot(player, normalized, contextId, componentId)
        val inventoryItem = bracketItem ?: when {
            normalized.equals("hand", true) || normalized.equals("mainhand", true) || normalized.equals("main_hand", true) ->
                player.inventory.itemInMainHand
            normalized.equals("offhand", true) || normalized.equals("off_hand", true) -> player.inventory.itemInOffHand
            slot != null && slot in 0 until player.inventory.size -> player.inventory.getItem(slot)
            else -> null
        }
        if (inventoryItem != null && inventoryItem.type != Material.AIR && inventoryItem.amount > 0) {
            return inventoryItem.clone()
        }
        return ExternalItemAdapter.create(normalized, defaultAmount, player)
            ?: ItemStack(MaterialUtils.matchMaterial(normalized) ?: Material.PAPER, defaultAmount.coerceAtLeast(1))
    }

    /** 解析 Paper 既有的 [HEAD:Player]、[MAINHAND] 等装备槽位写法。 */
    private fun resolveBracketSlot(
        player: Player,
        source: String,
        contextId: String,
        componentId: String
    ): ItemStack? {
        if (!isSlotReference(source)) return null
        val parts = source.substring(1, source.length - 1).split(':', limit = 2)
        val targetName = parts.getOrNull(1)?.takeIf { it.isNotBlank() }
        val target = targetName?.let(Bukkit::getPlayer) ?: if (targetName == null) player else null
        if (target == null || !target.isOnline) {
            warnOnce(
                "$contextId:$componentId:offline-slot:$targetName",
                "spigot_dialog.invalid_item_slot",
                contextId,
                componentId,
                targetName ?: ""
            )
            return ItemStack(Material.PAPER)
        }
        val slotName = parts[0].uppercase()
        val slotItem = when (slotName) {
            "HEAD" -> target.inventory.helmet
            "CHEST" -> target.inventory.chestplate
            "LEGGINGS" -> target.inventory.leggings
            "BOOTS" -> target.inventory.boots
            "MAINHAND" -> target.inventory.itemInMainHand
            "OFFHAND" -> target.inventory.itemInOffHand
            else -> {
                warnOnce(
                    "$contextId:$componentId:unknown-slot:$slotName",
                    "spigot_dialog.invalid_item_slot",
                    contextId,
                    componentId,
                    slotName
                )
                return ItemStack(Material.PAPER)
            }
        }
        if (slotItem != null && slotItem.type != Material.AIR && slotItem.amount > 0) {
            return slotItem.clone()
        }
        if (slotName == "HEAD") {
            return ItemStack(Material.PLAYER_HEAD).apply {
                val skullMeta = itemMeta as SkullMeta
                skullMeta.owningPlayer = target
                itemMeta = skullMeta
            }
        }
        return ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE).apply {
            val emptyMeta = itemMeta
            emptyMeta?.setDisplayName("无")
            itemMeta = emptyMeta
        }
    }

    private fun isSlotReference(source: String): Boolean = source.startsWith('[') && source.endsWith(']')

    private fun compileInputs(
        player: Player,
        config: YamlConfiguration,
        keys: MutableList<String>,
        types: MutableMap<String, String>,
        removeChars: MutableMap<String, String>,
        checkboxMappings: MutableMap<String, Pair<String, String>>
    ): List<DialogInputDefinition> {
        val result = mutableListOf<DialogInputDefinition>()
        val section = config.getConfigurationSection("Inputs") ?: return result
        for (key in section.getKeys(false)) {
            val type = getType(player, section, "$key.type", "text").lowercase()
            if (type == "none") continue
            val label = getString(player, section, "$key.text", "")
            when (type) {
                "checkbox" -> {
                    val onTrue = getString(player, section, "$key.on_true", "true")
                    val onFalse = getString(player, section, "$key.on_false", "false")
                    types[key] = "checkbox"
                    checkboxMappings[key] = onTrue to onFalse
                    result += DialogInputDefinition.Checkbox(
                        key, label, initial = getBoolean(player, section, "$key.default", false), onTrue = onTrue, onFalse = onFalse
                    )
                }

                "slider" -> {
                    val start = getDouble(player, section, "$key.min", 0.0).toFloat()
                    val end = getDouble(player, section, "$key.max", 10.0).toFloat()
                    val range = if (start < end) start to end else 0.0f to 10.0f
                    types[key] = "number"
                    result += DialogInputDefinition.Slider(
                        key = key,
                        label = label,
                        width = 250,
                        format = getString(player, section, "$key.format", "%s: %s"),
                        minimum = range.first,
                        maximum = range.second,
                        initial = getDouble(player, section, "$key.default", range.first.toDouble()).toFloat(),
                        step = getDouble(player, section, "$key.step", 1.0).toFloat()
                    )
                }

                "input" -> {
                    types[key] = "text"
                    InputCaptureUtils.resolveRemoveChars(plugin, section.get("$key.remove_chars"))
                        .takeIf { it.isNotEmpty() }
                        ?.let { removeChars[key] = it }
                    val multiline = section.getConfigurationSection("$key.multiline")?.let {
                        DialogMultiline(
                            getInt(player, section, "$key.multiline.max_lines", 5),
                            getInt(player, section, "$key.multiline.height", 100)
                        )
                    }
                    result += DialogInputDefinition.Text(
                        key, label, getInt(player, section, "$key.width", 250),
                        !getBoolean(player, section, "$key.hide_text", false),
                        getString(player, section, "$key.default", ""),
                        getInt(player, section, "$key.max_length", 256), multiline
                    )
                }

                "dropdown" -> {
                    types[key] = "text"
                    val defaultId = getString(player, section, "$key.default_id", "")
                    val options = getStringList(player, section, "$key.options").map { raw ->
                        val option = parseDropdownOption(raw)
                        DialogOptionDefinition(option.id, option.display, option.id == defaultId)
                    }
                    result += DialogInputDefinition.Dropdown(
                        key, label, getInt(player, section, "$key.width", 200),
                        !getBoolean(player, section, "$key.hide_text", false), options
                    )
                }
            }
            keys += key
        }
        return result
    }

    private fun compileBottom(player: Player, config: YamlConfiguration, contextId: String): DialogBottomDefinition {
        val section = config.getConfigurationSection("Bottom")
        return when (section?.getString("type", "notice")?.lowercase()) {
            "multi" -> compileMultiBottom(player, config, section, contextId)
            "confirmation" -> DialogBottomDefinition(
                DialogBottomType.CONFIRMATION,
                listOf(
                    button(player, config, section, "confirm", "确认", "Bottom.confirm.actions", emptyMap(), contextId),
                    button(player, config, section, "deny", "取消", "Bottom.deny.actions", emptyMap(), contextId)
                )
            )

            else -> {
                val confirmPath = if (config.contains("Bottom.confirm.actions")) "Bottom.confirm.actions" else "Bottom.button1.actions"
                val key = if (config.contains("Bottom.confirm.text")) "confirm" else "button1"
                DialogBottomDefinition(DialogBottomType.NOTICE, listOf(
                    button(player, config, section, key, "确认", confirmPath, emptyMap(), contextId)
                ))
            }
        }
    }

    private fun compileMultiBottom(
        player: Player,
        config: YamlConfiguration,
        section: ConfigurationSection,
        contextId: String
    ): DialogBottomDefinition {
        val buttons = mutableListOf<DialogButtonDefinition>()
        val columns = section.getInt("columns", 2).coerceAtLeast(1)
        section.getConfigurationSection("buttons")?.let { buttonSection ->
            for (key in buttonSection.getKeys(false)) {
                if (buttonSection.getString("$key.type", "").equals("repeat", true)) {
                    buttons += repeatButtons(
                        player,
                        config,
                        buttonSection.getConfigurationSection(key) ?: continue,
                        key,
                        contextId,
                        columns
                    )
                    continue
                }
                val showCondition = buttonSection.getString("$key.show-condition") ?: buttonSection.getString("$key.show_condition")
                if (showCondition != null && !ConditionUtils.checkCondition(player, showCondition, emptyMap(), config) {
                        dynamicVariable(player, contextId, it)
                    }) continue
                buttons += button(player, config, buttonSection, key, "按钮", "Bottom.buttons.$key.actions", emptyMap(), contextId)
            }
        }
        val exit = section.getString("exit.text", "").takeIf { !it.isNullOrEmpty() }?.let {
            button(player, config, section, "exit", "", "Bottom.exit.actions", emptyMap(), contextId)
        }
        return DialogBottomDefinition(
            DialogBottomType.MULTI,
            buttons,
            columns,
            exit
        )
    }

    private fun repeatButtons(
        player: Player,
        config: YamlConfiguration,
        section: ConfigurationSection,
        listId: String,
        contextId: String,
        columns: Int
    ): List<DialogButtonDefinition> {
        val source = section.getString("source", "") ?: ""
        val items = resolveRepeatSource(player, config, source, section.getString("split"), getBoolean(player, section, "trim", true))
        val pageSize = getInt(player, section, "page_size", getInt(player, section, "page-size", 20)).coerceIn(1, 99)
        val page = MenuListManager.updatePageInfo(player, contextId, listId, pageSize, items.size)
        if (items.isEmpty()) {
            return section.getConfigurationSection("empty")?.let {
                listOf(button(player, config, section, "empty", "暂无数据", "Bottom.buttons.$listId.empty.actions", emptyMap(), contextId))
            } ?: emptyList()
        }
        val itemSection = section.getConfigurationSection("item") ?: return emptyList()
        val buttons = items.subList(page.start, page.end).mapIndexedNotNull { pageIndex, item ->
            val variables = item.values.toMutableMap().apply {
                put("item.page_index", pageIndex.toString())
                put("item.page_number", (pageIndex + 1).toString())
                put("list.id", listId)
                put("list.page", page.page.toString())
                put("list.pages", page.pages.toString())
                put("list.total", page.total.toString())
            }
            val showCondition = itemSection.getString("show-condition") ?: itemSection.getString("show_condition")
            if (showCondition != null && !ConditionUtils.checkCondition(player, showCondition, variables, config) {
                    dynamicVariable(player, contextId, it)
                }) null
            else button(player, config, section, "item", "{item.text}", "Bottom.buttons.$listId.item.actions", variables, contextId)
        }
        return buttons + repeatPaddingButtons(buttons.size, columns)
    }

    /** 为 repeat 当前页补齐矩阵尾部，并让补位按钮点击后重新渲染当前菜单。 */
    private fun repeatPaddingButtons(count: Int, columns: Int): List<DialogButtonDefinition> {
        val paddingCount = (columns - count % columns) % columns
        return List(paddingCount) {
            DialogButtonDefinition(
                text = "",
                tooltip = null,
                width = null,
                actionPath = "",
                actionOverride = listOf("reset")
            )
        }
    }

    private fun button(
        player: Player,
        config: YamlConfiguration,
        section: ConfigurationSection?,
        key: String,
        defaultText: String,
        actionPath: String,
        variables: Map<String, String>,
        contextId: String
    ): DialogButtonDefinition {
        val actualSection = section ?: config
        val text = resolve(player, getString(player, actualSection, "$key.text", defaultText), contextId, variables, config)
        val tooltip = getStringList(player, actualSection, "$key.tooltip")
            .map { resolve(player, it, contextId, variables, config) }
            .joinToString("\n")
            .takeIf { it.isNotEmpty() }
        return DialogButtonDefinition(
            text = text,
            tooltip = tooltip,
            width = getInt(player, actualSection, "$key.width", 0).takeIf { it > 0 },
            actionPath = actionPath,
            variables = variables
        )
    }

    private fun resolveRepeatSource(
        player: Player,
        config: YamlConfiguration,
        rawSource: String,
        split: String?,
        trimItems: Boolean
    ): List<RepeatItem> {
        val source = rawSource.trim()
        val result = if (source.startsWith("[") && source.endsWith("]")) {
            JavaScriptManager.executePredefinedFunctionWithArgs(player, source.substring(1, source.length - 1).trim(), "", config)
        } else {
            resolve(player, source, config = config)
        } ?: return emptyList()
        return repeatItemsFromAny(result, split, trimItems)
    }

    private fun repeatItemsFromAny(value: Any?, split: String?, trimItems: Boolean): List<RepeatItem> = when (value) {
        null -> emptyList()
        is Iterable<*> -> value.mapIndexedNotNull(::repeatItem)
        is Array<*> -> value.mapIndexedNotNull(::repeatItem)
        is String -> JavaScriptManager.parseJsonCompatible(value)?.takeIf { it !is String }
            ?.let { repeatItemsFromAny(it, split, trimItems) }
            ?: if (!split.isNullOrEmpty()) value.split(split)
                .map { if (trimItems) it.trim() else it }
                .filter { it.isNotEmpty() }
                .mapIndexed { index, item -> scalarRepeatItem(index, item) }
            else value.lines().filter { it.isNotBlank() }.mapIndexed { index, item -> scalarRepeatItem(index, item.trim()) }
        else -> emptyList()
    }

    private fun repeatItem(index: Int, item: Any?): RepeatItem? = when (item) {
        null -> null
        is Map<*, *> -> RepeatItem(item.entries.associate { (key, value) -> "item.$key" to (value?.toString() ?: "") }
            .toMutableMap().apply {
                put("item.index", index.toString())
                put("item.number", (index + 1).toString())
            })
        else -> scalarRepeatItem(index, item.toString())
    }

    private fun scalarRepeatItem(index: Int, value: String) = RepeatItem(mapOf(
        "item.value" to value,
        "item.text" to value,
        "item.index" to index.toString(),
        "item.number" to (index + 1).toString()
    ))

    private fun parseDropdownOption(raw: String): DropdownOption {
        val parts = raw.split("=>", limit = 2)
        return if (parts.size == 2) DropdownOption(parts[0].trim(), parts[1].trim().ifEmpty { parts[0].trim() })
        else DropdownOption(raw, raw)
    }

    private fun normalizeMessageText(listValue: Boolean, text: String): String = when {
        listValue && (text.endsWith('\n') || text.endsWith('\r')) -> "$text "
        !listValue && text.endsWith("\r\n") -> text.dropLast(2)
        !listValue && (text.endsWith('\n') || text.endsWith('\r')) -> text.dropLast(1)
        else -> text
    }

    private fun resolve(
        player: Player,
        value: String?,
        contextId: String? = null,
        variables: Map<String, String> = emptyMap(),
        config: YamlConfiguration? = null
    ): String = TextResolver.resolve(player, value, variables, { key ->
        contextId?.let { dynamicVariable(player, it, key) }
    }, config)

    private fun dynamicVariable(player: Player, contextId: String, key: String): String? {
        val separator = key.indexOf(':')
        if (separator <= 0) return null
        val listId = key.substring(separator + 1).trim().takeIf { it.isNotEmpty() } ?: return null
        val info = MenuListManager.getPageInfo(player, contextId, listId)
        return when (key.substring(0, separator).trim().lowercase()) {
            "page" -> (info?.page ?: MenuListManager.getPage(player, contextId, listId)).toString()
            "pages" -> (info?.pages ?: 1).toString()
            "total" -> (info?.total ?: 0).toString()
            "start" -> (info?.start ?: 0).toString()
            "end" -> (info?.end ?: 0).toString()
            else -> null
        }
    }

    private fun getString(player: Player, section: ConfigurationSection, path: String, default: String): String =
        ConditionUtils.getString(player, section, path, default)

    private fun getStringList(player: Player, section: ConfigurationSection, path: String): List<String> =
        ConditionUtils.getStringList(player, section, path)

    private fun getBoolean(player: Player, section: ConfigurationSection, path: String, default: Boolean): Boolean =
        ConditionUtils.getBoolean(player, section, path, default)

    private fun getInt(player: Player, section: ConfigurationSection, path: String, default: Int): Int =
        ConditionUtils.getInt(player, section, path, default)

    private fun getDouble(player: Player, section: ConfigurationSection, path: String, default: Double): Double =
        ConditionUtils.getDouble(player, section, path, default)

    private fun getType(player: Player, section: ConfigurationSection, path: String, default: String): String =
        ConditionUtils.getType(player, section, path, default)
}
