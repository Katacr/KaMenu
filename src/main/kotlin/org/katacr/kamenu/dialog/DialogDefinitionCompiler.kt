package org.katacr.kamenu.dialog

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.katacr.kamenu.ConditionUtils
import org.katacr.kamenu.ConditionValueResolver
import org.katacr.kamenu.InputCaptureUtils
import org.katacr.kamenu.InlineConditionResolver
import org.katacr.kamenu.JavaScriptManager
import org.katacr.kamenu.KaMenu
import org.katacr.kamenu.MenuItemFactory
import org.katacr.kamenu.MenuItemSpec
import org.katacr.kamenu.MenuListManager
import org.katacr.kamenu.TextResolver
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
    private val itemFactory = MenuItemFactory(plugin)

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
                    val resolved = if (section.isList(path)) {
                        getInlineConditionalStringList(player, section, path, contextId).joinToString("\n")
                    } else {
                        resolve(player, getString(player, section, path, ""), contextId, config = config)
                    }
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
        val item = itemFactory.create(
            player = player,
            spec = MenuItemSpec(
                source = configuredMaterial,
                amount = getInt(player, section, "$key.amount", 1),
                name = resolve(player, getString(player, section, "$key.name", ""), contextId, config = config)
                    .takeIf(String::isNotEmpty),
                lore = getInlineConditionalStringList(player, section, "$key.lore", contextId)
                    .takeIf(List<String>::isNotEmpty),
                customModelData = resolve(
                    player,
                    getString(player, section, "$key.custom_model_data", ""),
                    contextId,
                    config = config
                ).takeIf(String::isNotEmpty),
                itemModel = resolve(
                    player,
                    getString(player, section, "$key.item_model", ""),
                    contextId,
                    config = config
                ).takeIf(String::isNotEmpty),
                skullOwner = resolve(
                    player,
                    getString(player, section, "$key.skull_owner", ""),
                    contextId,
                    config = config
                ).takeIf(String::isNotEmpty),
                skullTexture = resolve(
                    player,
                    getString(player, section, "$key.skull_texture", ""),
                    contextId,
                    config = config
                ).takeIf(String::isNotEmpty)
            ),
            contextId = contextId,
            componentId = key,
            applyOverridesToSlotSource = false
        )
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

    /** 对同一菜单组件的兼容限制只输出一次本地化警告。 */
    private fun warnOnce(id: String, messageKey: String, vararg args: Any) {
        if (reportedLimitations.add(id)) {
            plugin.logger.warning(plugin.languageManager.getMessage(messageKey, *args))
        }
    }


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
                val selfVariables = selfVariables(buttonSection, "$key.show-condition")
                if (showCondition != null && !ConditionUtils.checkCondition(player, showCondition, selfVariables, config) {
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
            val contextualVariables = variables + selfVariables(section, "item.show-condition")
            if (showCondition != null && !ConditionUtils.checkCondition(player, showCondition, contextualVariables, config) {
                    dynamicVariable(player, contextId, it)
                }) null
            else button(player, config, section, "item", "{item.text}", "Bottom.buttons.$listId.item.actions", variables, contextId)
        }
        return buttons + repeatPaddingButtons(player, section, buttons.size, columns)
    }

    /** 为 repeat 当前页补齐矩阵尾部，并让补位按钮点击后重新渲染当前菜单。 */
    private fun repeatPaddingButtons(
        player: Player,
        section: ConfigurationSection,
        count: Int,
        columns: Int
    ): List<DialogButtonDefinition> {
        val paddingCount = (columns - count % columns) % columns
        val itemWidth = getInt(player, section, "item.width", 0).takeIf { it > 0 }
        return List(paddingCount) {
            DialogButtonDefinition(
                text = "",
                tooltip = null,
                width = itemWidth,
                actionPath = "",
                actionOverride = listOf("reset"),
                syntheticPadding = true
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
        val contextualVariables = variables + selfVariables(actualSection, "$key.text")
        val text = resolve(player, getString(player, actualSection, "$key.text", defaultText), contextId, contextualVariables, config)
        val tooltip = getInlineConditionalStringList(player, actualSection, "$key.tooltip", contextId, variables)
            .joinToString("\n")
            .takeIf { it.isNotEmpty() }
        return DialogButtonDefinition(
            text = text,
            tooltip = tooltip,
            width = getInt(player, actualSection, "$key.width", 0).takeIf { it > 0 },
            actionPath = actionPath,
            variables = contextualVariables,
            icon = buttonIcon(player, actualSection, key, contextId, contextualVariables, config)
        )
    }

    /** 解析按钮的基岩版 URL 或资源包路径图标，并允许 repeat item 变量参与取值。 */
    private fun buttonIcon(
        player: Player,
        section: ConfigurationSection,
        key: String,
        contextId: String,
        variables: Map<String, String>,
        config: YamlConfiguration
    ): DialogButtonIcon? {
        val path = "$key.icon"
        if (section.isConfigurationSection(path)) {
            val value = resolve(
                player,
                getString(player, section, "$path.value", ""),
                contextId,
                variables,
                config
            ).trim()
            if (value.isEmpty()) return null
            val rawType = resolve(
                player,
                getString(player, section, "$path.type", ""),
                contextId,
                variables,
                config
            ).trim().lowercase()
            val type = when (rawType) {
                "url" -> DialogButtonIconType.URL
                "path" -> DialogButtonIconType.PATH
                "" -> if (value.startsWith("http://", true) || value.startsWith("https://", true)) {
                    DialogButtonIconType.URL
                } else {
                    DialogButtonIconType.PATH
                }
                else -> {
                    warnOnce(
                        "$contextId:$path:type",
                        "bedrock_form.invalid_icon_type",
                        contextId,
                        path
                    )
                    return null
                }
            }
            return DialogButtonIcon(type, value)
        }

        val value = resolve(
            player,
            getString(player, section, path, ""),
            contextId,
            variables,
            config
        ).trim()
        if (value.isEmpty()) return null
        val type = if (value.startsWith("http://", true) || value.startsWith("https://", true)) {
            DialogButtonIconType.URL
        } else {
            DialogButtonIconType.PATH
        }
        return DialogButtonIcon(type, value)
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
        is String -> if (!split.isNullOrEmpty()) value.split(split)
                .map { if (trimItems) it.trim() else it }
                .filter { it.isNotEmpty() }
                .mapIndexed { index, item -> scalarRepeatItem(index, item) }
            else if (looksLikeJsonCollection(value)) {
                JavaScriptManager.parseJsonCompatible(value)?.takeIf { it !is String }
                    ?.let { repeatItemsFromAny(it, null, trimItems) }
                    ?: emptyList()
            } else {
                value.lines().filter { it.isNotBlank() }
                    .mapIndexed { index, item -> scalarRepeatItem(index, if (trimItems) item.trim() else item) }
            }
        else -> emptyList()
    }

    /** 仅让外形完整的 JSON 数组或对象进入 JSON 解析器，普通列表字符串不会产生解析警告。 */
    private fun looksLikeJsonCollection(value: String): Boolean {
        val trimmed = value.trim()
        return (trimmed.startsWith('[') && trimmed.endsWith(']')) ||
            (trimmed.startsWith('{') && trimmed.endsWith('}'))
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

    /** 根据字段所在组件生成 `{self:*}` 所需的 ID 与 YAML 根路径。 */
    private fun selfVariables(section: ConfigurationSection, path: String): Map<String, String> {
        val componentId = path.substringBefore('.').trim()
        val parentPath = section.currentPath.orEmpty().trim('.')
        val componentPath = listOf(parentPath, componentId).filter(String::isNotEmpty).joinToString(".")
        return mapOf("self:id" to componentId, "self:path" to componentPath)
    }

    private fun getString(player: Player, section: ConfigurationSection, path: String, default: String): String =
        ConditionValueResolver.getString(player, section, path, default, selfVariables(section, path))

    private fun getStringList(player: Player, section: ConfigurationSection, path: String): List<String> =
        ConditionValueResolver.getStringList(player, section, path, emptyList(), selfVariables(section, path))

    /** 解析展示列表，并按当前组件上下文过滤带行尾条件的字符串。 */
    private fun getInlineConditionalStringList(
        player: Player,
        section: ConfigurationSection,
        path: String,
        contextId: String,
        extraVariables: Map<String, String> = emptyMap()
    ): List<String> {
        val variables = extraVariables + selfVariables(section, path)
        return ConditionValueResolver.getInlineConditionalStringList(player, section, path, variables) { key ->
            dynamicVariable(player, contextId, key)
        }
    }

    private fun getBoolean(player: Player, section: ConfigurationSection, path: String, default: Boolean): Boolean =
        ConditionValueResolver.getBoolean(player, section, path, default, selfVariables(section, path))

    private fun getInt(player: Player, section: ConfigurationSection, path: String, default: Int): Int =
        ConditionValueResolver.getInt(player, section, path, default, selfVariables(section, path))

    private fun getDouble(player: Player, section: ConfigurationSection, path: String, default: Double): Double =
        ConditionValueResolver.getDouble(player, section, path, default, selfVariables(section, path))

    private fun getType(player: Player, section: ConfigurationSection, path: String, default: String): String =
        ConditionValueResolver.getType(player, section, path, default, selfVariables(section, path))
}
