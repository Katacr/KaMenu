package org.katacr.kamenu.container

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration

/**
 * 将容器菜单 YAML 编译为平台无关的 [ContainerMenuDefinition]。
 *
 * 此解析器只负责静态结构和类型校验，不访问玩家、库存或数据库。玩家变量、条件和物品创建留到渲染阶段。
 */
object ContainerMenuParser {
    private val dialogOnlySections = listOf("Body", "Inputs", "Bottom")
    private val knownDisplayProperties = setOf(
        "material",
        "name",
        "lore",
        "amount",
        "custom_model_data",
        "item_model",
        "skull_owner",
        "skull_texture",
        "glow",
        "unbreakable",
        "enchantments",
        "item_flags"
    )
    private val furnaceProperties = setOf("burn_progress", "cook_progress")
    private val anvilProperties = setOf(
        "input",
        "remove_chars",
        "repair_cost",
        "maximum_repair_cost",
        "repair_item_count"
    )
    private val knownContainerProperties = furnaceProperties + anvilProperties

    /** 判断一份 YAML 是否使用 Container 菜单结构。 */
    fun isContainerMenu(config: YamlConfiguration): Boolean {
        if (config.contains("Layout")) return true
        val type = config.getString("Type")?.trim().orEmpty()
        return ContainerMenuType.entries.any { it.name.equals(type, ignoreCase = true) }
    }

    /** 解析一份容器菜单配置；任何 ERROR 都会使 definition 为空。 */
    fun parse(menuId: String, config: YamlConfiguration): ContainerMenuParseResult {
        val diagnostics = mutableListOf<ContainerMenuDiagnostic>()
        validateMenuFamily(config, diagnostics)
        val type = parseType(config, diagnostics)
        val title = freeze(config.get("Title") ?: "KaMenu")
        val minClickDelayMillis = parseMinClickDelay(
            config.get("Settings.min_click_delay"),
            diagnostics
        )
        val progressInterval = parseUpdateInterval(config.get("Progress-Update"), "Progress-Update", diagnostics)
        if (progressInterval != null && type?.isFurnace != true) {
            diagnostics += error(
                "progress_update.unsupported_type",
                "Progress-Update",
                "Progress-Update is only supported by furnace container types."
            )
        }
        val update = ContainerUpdateDefinition(
            menuIntervalTicks = parseUpdateInterval(config.get("Update"), "Update", diagnostics),
            titleIntervalTicks = parseUpdateInterval(config.get("Title-Update"), "Title-Update", diagnostics),
            progressIntervalTicks = progressInterval
        )
        val properties = parseProperties(config, type, diagnostics)
        val progressWatchers = parseProgressWatchers(config, type, properties, diagnostics)
        val layoutRows = parseLayoutRows(config, diagnostics)
        val layoutResult = if (layoutRows != null && type != null) {
            ContainerLayoutParser.parse(layoutRows, type)
        } else {
            null
        }
        if (layoutResult != null) diagnostics += layoutResult.diagnostics
        val layout = layoutResult?.definition
        val buttons = parseButtons(config, diagnostics)
        val freeSlots = if (layout != null) {
            parseFreeSlots(config, type, layout, diagnostics)
        } else {
            ContainerFreeSlotsDefinition.EMPTY
        }

        if (layout != null) {
            validateButtonReferences(layout, buttons, diagnostics)
        }

        if (diagnostics.any { it.severity == ContainerDiagnosticSeverity.ERROR } || type == null || layout == null) {
            return ContainerMenuParseResult(null, diagnostics.toList())
        }
        return ContainerMenuParseResult(
            ContainerMenuDefinition(
                id = menuId,
                type = type,
                title = title,
                layout = layout,
                freeSlots = freeSlots,
                properties = properties,
                buttons = buttons.toMap(),
                update = update,
                minClickDelayMillis = minClickDelayMillis,
                progressWatchers = progressWatchers
            ),
            diagnostics.toList()
        )
    }

    /** 读取 Container 菜单的玩家点击最小间隔；单位为毫秒，0 表示不限制。 */
    private fun parseMinClickDelay(
        raw: Any?,
        diagnostics: MutableList<ContainerMenuDiagnostic>
    ): Long {
        if (raw == null) return 0L
        val millis = when (raw) {
            is Byte, is Short, is Int, is Long -> (raw as Number).toLong()
            is Float, is Double -> {
                val number = (raw as Number).toDouble()
                if (number.isFinite() && number % 1.0 == 0.0) number.toLong() else null
            }
            is String -> raw.trim().toLongOrNull()
            else -> null
        }
        if (millis == null) {
            diagnostics += error(
                "settings.invalid_min_click_delay",
                "Settings.min_click_delay",
                "Settings.min_click_delay must be a non-negative integer number of milliseconds."
            )
            return 0L
        }
        if (millis < 0L) {
            diagnostics += error(
                "settings.invalid_min_click_delay",
                "Settings.min_click_delay",
                "Settings.min_click_delay must be a non-negative integer number of milliseconds."
            )
            return 0L
        }
        return millis
    }

    /** 拒绝在同一文件中混用 Container 布局和 Dialog 专属组件，避免运行时错误分派。 */
    private fun validateMenuFamily(
        config: YamlConfiguration,
        diagnostics: MutableList<ContainerMenuDiagnostic>
    ) {
        dialogOnlySections.filter(config::contains).forEach { section ->
            diagnostics += error(
                "menu.conflicting_dialog_section",
                section,
                "Container menus cannot define the Dialog-only section '$section'."
            )
        }
    }

    private fun parseType(
        config: YamlConfiguration,
        diagnostics: MutableList<ContainerMenuDiagnostic>
    ): ContainerMenuType? {
        val raw = config.get("Type") ?: return ContainerMenuType.CHEST
        if (raw !is String) {
            diagnostics += error("menu.invalid_type_value", "Type", "Type must be a string.")
            return null
        }
        return ContainerMenuType.entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
            ?: run {
                diagnostics += error(
                    "menu.unsupported_type",
                    "Type",
                    "Unsupported container type '$raw'. Supported types: " +
                        ContainerMenuType.entries.joinToString { it.name } + "."
                )
                null
            }
    }

    /** 冻结并校验 `Properties` 中与容器类型相关的动态属性。 */
    private fun parseProperties(
        config: YamlConfiguration,
        type: ContainerMenuType?,
        diagnostics: MutableList<ContainerMenuDiagnostic>
    ): ContainerPropertiesDefinition {
        val section = config.getConfigurationSection("Properties")
        if (section == null) {
            if (config.contains("Properties")) {
                diagnostics += error("properties.invalid", "Properties", "Properties must be a YAML section.")
            }
            return ContainerPropertiesDefinition.EMPTY
        }

        val allowed = when {
            type?.isFurnace == true -> furnaceProperties
            type == ContainerMenuType.ANVIL -> anvilProperties
            else -> emptySet()
        }
        val values = linkedMapOf<String, ContainerConfigValue>()
        section.getKeys(false).forEach { property ->
            values[property] = freeze(section.get(property))
            when {
                property in allowed -> Unit
                property in knownContainerProperties -> diagnostics += warning(
                    "properties.unsupported_for_type",
                    "Properties.$property",
                    "Property '$property' does not apply to container type ${type?.name ?: "UNKNOWN"}."
                )
                else -> diagnostics += warning(
                    "properties.unknown",
                    "Properties.$property",
                    "Unknown container property '$property' is preserved but will not be applied."
                )
            }
        }
        return ContainerPropertiesDefinition(values.toMap())
    }

    /** 解析并校验 `Events.Progress` 下的会话级熔炉进度监听器。 */
    private fun parseProgressWatchers(
        config: YamlConfiguration,
        type: ContainerMenuType?,
        properties: ContainerPropertiesDefinition,
        diagnostics: MutableList<ContainerMenuDiagnostic>
    ): Map<String, ContainerProgressWatcherDefinition> {
        val section = config.getConfigurationSection("Events.Progress")
        if (section == null) {
            if (config.contains("Events.Progress")) {
                diagnostics += error(
                    "progress.invalid",
                    "Events.Progress",
                    "Events.Progress must be a YAML section."
                )
            }
            return emptyMap()
        }
        if (type?.isFurnace != true) {
            diagnostics += error(
                "progress.unsupported_type",
                "Events.Progress",
                "Events.Progress is only supported by furnace container types."
            )
        }

        val result = linkedMapOf<String, ContainerProgressWatcherDefinition>()
        section.getKeys(false).forEach { id ->
            val path = "Events.Progress.$id"
            val watcher = section.getConfigurationSection(id)
            if (watcher == null) {
                diagnostics += error("progress.invalid_watcher", path, "Progress watcher '$id' must be a YAML section.")
                return@forEach
            }

            val source = (watcher.get("source") as? String)?.trim()?.lowercase().orEmpty()
            if (source !in furnaceProperties) {
                diagnostics += error(
                    "progress.invalid_source",
                    "$path.source",
                    "Progress source must be burn_progress or cook_progress."
                )
            } else if (!properties.contains(source)) {
                diagnostics += error(
                    "progress.source_not_configured",
                    "$path.source",
                    "Progress source '$source' is not configured under Properties."
                )
            }

            val condition = (watcher.get("condition") as? String)?.takeIf { it.isNotBlank() }
            if (condition == null) {
                diagnostics += error(
                    "progress.invalid_condition",
                    "$path.condition",
                    "Progress condition must be a non-empty string."
                )
            }

            val triggerInitial = when (val raw = watcher.get("trigger_initial")) {
                null -> false
                is Boolean -> raw
                else -> {
                    diagnostics += error(
                        "progress.invalid_trigger_initial",
                        "$path.trigger_initial",
                        "trigger_initial must be a boolean."
                    )
                    false
                }
            }

            val actions = when (val raw = watcher.get("actions")) {
                is String -> listOf(raw)
                is List<*> -> raw.map(::freezeActionValue)
                else -> {
                    diagnostics += error(
                        "progress.invalid_actions",
                        "$path.actions",
                        "Progress actions must be a string or list."
                    )
                    emptyList()
                }
            }
            if (actions.isEmpty()) {
                diagnostics += error(
                    "progress.empty_actions",
                    "$path.actions",
                    "Progress actions cannot be empty."
                )
            }

            if (source in furnaceProperties && condition != null && actions.isNotEmpty()) {
                result[id] = ContainerProgressWatcherDefinition(
                    id = id,
                    source = source,
                    condition = condition,
                    triggerInitial = triggerInitial,
                    actions = actions
                )
            }
        }
        return result.toMap()
    }

    private fun parseLayoutRows(
        config: YamlConfiguration,
        diagnostics: MutableList<ContainerMenuDiagnostic>
    ): List<String>? {
        val raw = config.get("Layout")
        if (raw !is List<*>) {
            diagnostics += error("layout.missing", "Layout", "Layout must be a list of strings.")
            return null
        }
        val invalidIndex = raw.indexOfFirst { it !is String }
        if (invalidIndex >= 0) {
            diagnostics += error(
                "layout.invalid_row_type",
                "Layout[$invalidIndex]",
                "Every Layout row must be a string."
            )
            return null
        }
        return raw.filterIsInstance<String>()
    }

    /** 解析 `Free-Slots` 并校验范围、重复槽位和 Layout 按钮冲突。 */
    private fun parseFreeSlots(
        config: YamlConfiguration,
        type: ContainerMenuType?,
        layout: ContainerLayoutDefinition,
        diagnostics: MutableList<ContainerMenuDiagnostic>
    ): ContainerFreeSlotsDefinition {
        val section = config.getConfigurationSection("Free-Slots")
        if (section == null) {
            if (config.contains("Free-Slots")) {
                diagnostics += error("free_slots.invalid", "Free-Slots", "Free-Slots must be a YAML section.")
            }
            return ContainerFreeSlotsDefinition.EMPTY
        }
        if (section.getKeys(false).isEmpty()) return ContainerFreeSlotsDefinition.EMPTY
        if (type?.isFurnace == true || type == ContainerMenuType.ANVIL) {
            diagnostics += error(
                "free_slots.unsupported_type",
                "Free-Slots",
                "Free-Slots are not supported by furnace or anvil container types in this version."
            )
        }

        val byId = linkedMapOf<String, ContainerFreeSlotDefinition>()
        val idBySlot = linkedMapOf<Int, String>()
        section.getKeys(false).forEach { id ->
            val path = "Free-Slots.$id"
            if (!FREE_SLOT_ID.matches(id)) {
                diagnostics += error(
                    "free_slot.invalid_id",
                    path,
                    "Free slot IDs may only contain letters, numbers, '_', '-' and '.'."
                )
                return@forEach
            }
            val freeSlot = section.getConfigurationSection(id)
            if (freeSlot == null) {
                diagnostics += error("free_slot.invalid", path, "Free slot '$id' must be a YAML section.")
                return@forEach
            }
            validateKnownKeys(
                freeSlot,
                setOf("slots", "place", "take", "events", "return"),
                path,
                "free_slot.unknown_key",
                diagnostics
            )

            val slots = parseFreeSlotIndexes(freeSlot.get("slots"), "$path.slots", layout.size, diagnostics)
            slots.forEach { slot ->
                val previousId = idBySlot[slot]
                if (previousId != null) {
                    diagnostics += error(
                        "free_slot.duplicate_slot",
                        "$path.slots",
                        "Slot $slot is already assigned to Free-Slots.$previousId."
                    )
                } else {
                    idBySlot[slot] = id
                    if (layout.buttonAt(slot) != null) {
                        diagnostics += error(
                        "free_slot.button_conflict",
                        "$path.slots",
                        "Slot $slot is also occupied by Layout button '${layout.buttonAt(slot)}'."
                    )
                    }
                }
            }

            byId[id] = ContainerFreeSlotDefinition(
                id = id,
                slots = slots,
                place = parseFreeSlotRule(freeSlot, "place", path, diagnostics),
                take = parseFreeSlotRule(freeSlot, "take", path, diagnostics),
                events = parseFreeSlotEvents(freeSlot, path, diagnostics),
                returnRule = parseFreeSlotReturn(freeSlot, path, diagnostics)
            )
        }
        return ContainerFreeSlotsDefinition(byId.toMap(), idBySlot.toMap())
    }

    /** 读取自由槽位的 0-based 物理槽位列表。 */
    private fun parseFreeSlotIndexes(
        raw: Any?,
        path: String,
        inventorySize: Int,
        diagnostics: MutableList<ContainerMenuDiagnostic>
    ): List<Int> {
        val values = raw as? List<*>
        if (values == null || values.isEmpty()) {
            diagnostics += error("free_slot.invalid_slots", path, "slots must be a non-empty integer list.")
            return emptyList()
        }
        val result = mutableListOf<Int>()
        values.forEachIndexed { index, value ->
            val slot = exactInt(value)
            when {
                slot == null -> diagnostics += error(
                    "free_slot.invalid_slot",
                    "$path[$index]",
                    "Free slot indexes must be integers."
                )
                slot !in 0 until inventorySize -> diagnostics += error(
                    "free_slot.slot_out_of_range",
                    "$path[$index]",
                    "Free slot index $slot is outside the top inventory range 0..${inventorySize - 1}."
                )
                slot in result -> diagnostics += error(
                    "free_slot.duplicate_slot",
                    "$path[$index]",
                    "Free slot index $slot is duplicated in the same group."
                )
                else -> result += slot
            }
        }
        return result
    }

    /** 解析自由槽位放入或取出规则。 */
    private fun parseFreeSlotRule(
        section: ConfigurationSection,
        key: String,
        parentPath: String,
        diagnostics: MutableList<ContainerMenuDiagnostic>
    ): ContainerFreeSlotRuleDefinition {
        val path = "$parentPath.$key"
        val rule = section.getConfigurationSection(key)
        if (rule == null) {
            if (section.contains(key)) {
                diagnostics += error("free_slot.invalid_rule", path, "$key must be a YAML section.")
            }
            return ContainerFreeSlotRuleDefinition(enabled = true, condition = null)
        }
        validateKnownKeys(rule, setOf("enabled", "condition"), path, "free_slot.unknown_rule_key", diagnostics)
        val enabled = parseBoolean(rule.get("enabled"), "$path.enabled", true, diagnostics)
        val condition = when (val raw = rule.get("condition")) {
            null -> null
            is String -> raw.trim().takeIf(String::isNotEmpty)
            else -> {
                diagnostics += error(
                    "free_slot.invalid_condition",
                    "$path.condition",
                    "condition must be a string."
                )
                null
            }
        }
        return ContainerFreeSlotRuleDefinition(enabled, condition)
    }

    /** 解析自由槽位事务事件动作列表。 */
    private fun parseFreeSlotEvents(
        section: ConfigurationSection,
        parentPath: String,
        diagnostics: MutableList<ContainerMenuDiagnostic>
    ): ContainerFreeSlotEventsDefinition {
        val path = "$parentPath.events"
        val events = section.getConfigurationSection("events")
        if (events == null) {
            if (section.contains("events")) {
                diagnostics += error("free_slot.invalid_events", path, "events must be a YAML section.")
            }
            return ContainerFreeSlotEventsDefinition(emptyList(), emptyList(), emptyList(), emptyList())
        }
        validateKnownKeys(
            events,
            setOf("place", "take", "deny_place", "deny_take"),
            path,
            "free_slot.unknown_event",
            diagnostics
        )
        return ContainerFreeSlotEventsDefinition(
            place = parseActionList(events.get("place"), "$path.place", diagnostics),
            take = parseActionList(events.get("take"), "$path.take", diagnostics),
            denyPlace = parseActionList(events.get("deny_place"), "$path.deny_place", diagnostics),
            denyTake = parseActionList(events.get("deny_take"), "$path.deny_take", diagnostics)
        )
    }

    /** 解析自由槽位正常关闭返还设置；安全恢复路径不能被配置关闭。 */
    private fun parseFreeSlotReturn(
        section: ConfigurationSection,
        parentPath: String,
        diagnostics: MutableList<ContainerMenuDiagnostic>
    ): ContainerFreeSlotReturnDefinition {
        val path = "$parentPath.return"
        val returnSection = section.getConfigurationSection("return")
        if (returnSection == null) {
            if (section.contains("return")) {
                diagnostics += error("free_slot.invalid_return", path, "return must be a YAML section.")
            }
            return ContainerFreeSlotReturnDefinition(true, ContainerFreeSlotOverflowPolicy.PENDING)
        }
        validateKnownKeys(
            returnSection,
            setOf("on_close", "overflow"),
            path,
            "free_slot.unknown_return_key",
            diagnostics
        )
        val onClose = parseBoolean(returnSection.get("on_close"), "$path.on_close", true, diagnostics)
        val overflow = when (val raw = returnSection.get("overflow")) {
            null -> ContainerFreeSlotOverflowPolicy.PENDING
            is String -> ContainerFreeSlotOverflowPolicy.entries.firstOrNull {
                it.name.equals(raw.trim(), ignoreCase = true)
            } ?: run {
                diagnostics += error(
                    "free_slot.invalid_overflow",
                    "$path.overflow",
                    "overflow currently supports only 'pending'."
                )
                ContainerFreeSlotOverflowPolicy.PENDING
            }
            else -> {
                diagnostics += error(
                    "free_slot.invalid_overflow",
                    "$path.overflow",
                    "overflow must be a string."
                )
                ContainerFreeSlotOverflowPolicy.PENDING
            }
        }
        return ContainerFreeSlotReturnDefinition(onClose, overflow)
    }

    /** 解析可选布尔配置，并在错误类型时保留安全默认值。 */
    private fun parseBoolean(
        raw: Any?,
        path: String,
        defaultValue: Boolean,
        diagnostics: MutableList<ContainerMenuDiagnostic>
    ): Boolean {
        if (raw == null) return defaultValue
        if (raw is Boolean) return raw
        diagnostics += error("free_slot.invalid_boolean", path, "$path must be a boolean.")
        return defaultValue
    }

    /** 读取单个动作或动作列表，并冻结嵌套条件 Map。 */
    private fun parseActionList(
        raw: Any?,
        path: String,
        diagnostics: MutableList<ContainerMenuDiagnostic>
    ): List<Any> {
        return when (raw) {
            null -> emptyList()
            is String -> listOf(raw)
            is List<*> -> raw.map(::freezeActionValue)
            else -> {
                diagnostics += error("free_slot.invalid_event_actions", path, "Event actions must be a string or list.")
                emptyList()
            }
        }
    }

    /** 拒绝自由槽位中的未知键，避免拼写错误静默放宽物品规则。 */
    private fun validateKnownKeys(
        section: ConfigurationSection,
        known: Set<String>,
        path: String,
        code: String,
        diagnostics: MutableList<ContainerMenuDiagnostic>
    ) {
        section.getKeys(false).filterNot(known::contains).forEach { key ->
            diagnostics += error(code, "$path.$key", "Unknown free slot key '$key'.")
        }
    }

    private fun exactInt(value: Any?): Int? {
        return when (value) {
            is Byte, is Short, is Int -> (value as Number).toInt()
            is Long -> value.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt()
            else -> null
        }
    }

    private fun parseButtons(
        config: YamlConfiguration,
        diagnostics: MutableList<ContainerMenuDiagnostic>
    ): LinkedHashMap<String, ContainerButtonDefinition> {
        val result = linkedMapOf<String, ContainerButtonDefinition>()
        val section = config.getConfigurationSection("Buttons")
        if (section == null) {
            if (config.contains("Buttons")) {
                diagnostics += error("buttons.invalid", "Buttons", "Buttons must be a YAML section.")
            }
            return result
        }

        section.getKeys(false).forEach { id ->
            val buttonPath = "Buttons.$id"
            val button = section.getConfigurationSection(id)
            if (button == null) {
                diagnostics += error("button.invalid", buttonPath, "Button '$id' must be a YAML section.")
                return@forEach
            }
            parseButton(id, button, buttonPath, diagnostics)?.let { result[id] = it }
        }
        return result
    }

    private fun parseButton(
        id: String,
        section: ConfigurationSection,
        path: String,
        diagnostics: MutableList<ContainerMenuDiagnostic>
    ): ContainerButtonDefinition? {
        val hasVariants = section.contains("variants")
        if (hasVariants && (section.contains("display") || section.contains("actions"))) {
            diagnostics += error(
                "button.mixed_variants",
                path,
                "Button '$id' cannot combine variants with top-level display or actions."
            )
            return null
        }

        val display = if (!hasVariants) {
            parseDisplay(section.get("display"), "$path.display", id, diagnostics)
        } else {
            null
        }

        val viewCondition = when (val raw = section.get("view_condition")) {
            null -> null
            is String -> raw.takeIf { it.isNotBlank() }
            else -> {
                diagnostics += error(
                    "button.invalid_view_condition",
                    "$path.view_condition",
                    "view_condition must be a string."
                )
                null
            }
        }

        val updateInterval = parseUpdateInterval(section.get("update"), "$path.update", diagnostics)
        val actions = if (!hasVariants) parseActions(section.get("actions"), "$path.actions", diagnostics) else emptyMap()
        val variants = if (hasVariants) parseVariants(section.get("variants"), path, diagnostics) else emptyList()
        if (display == null && variants.isEmpty()) {
            diagnostics += error(
                "button.display_missing",
                "$path.display",
                "Button '$id' requires a display section or at least one variant."
            )
            return null
        }
        return ContainerButtonDefinition(
            id = id,
            viewCondition = viewCondition,
            updateIntervalTicks = updateInterval,
            display = display ?: ContainerItemDefinition(emptyMap()),
            actions = actions,
            variants = variants
        )
    }

    /** 解析按钮的单一 display 区段，供旧格式和 variants 共同使用。 */
    private fun parseDisplay(
        raw: Any?,
        path: String,
        buttonId: String,
        diagnostics: MutableList<ContainerMenuDiagnostic>
    ): ContainerItemDefinition? {
        val values = asStringKeyedMap(raw)
        if (values == null) {
            diagnostics += error("button.display_missing", path, "Button '$buttonId' requires a display section.")
            return null
        }
        if (!values.containsKey("material")) {
            diagnostics += error(
                "button.material_missing",
                "$path.material",
                "Button '$buttonId' requires a material."
            )
        }

        val properties = linkedMapOf<String, ContainerConfigValue>()
        values.forEach { (property, value) ->
            properties[property] = freeze(value)
            if (property !in knownDisplayProperties) {
                diagnostics += warning(
                    "button.unknown_display_property",
                    "$path.$property",
                    "Unknown display property '$property' is preserved but may not be rendered."
                )
            }
        }
        return ContainerItemDefinition(properties.toMap())
    }

    /** 解析并按 priority、声明顺序稳定排序按钮变体。 */
    private fun parseVariants(
        raw: Any?,
        buttonPath: String,
        diagnostics: MutableList<ContainerMenuDiagnostic>
    ): List<ContainerButtonVariantDefinition> {
        val entries = raw as? List<*>
        if (entries == null) {
            diagnostics += error(
                "button.invalid_variants",
                "$buttonPath.variants",
                "variants must be a YAML list."
            )
            return emptyList()
        }

        val parsed = entries.mapIndexedNotNull { index, entry ->
            val path = "$buttonPath.variants[$index]"
            val values = asStringKeyedMap(entry)
            if (values == null) {
                diagnostics += error("button.invalid_variant", path, "Each variant must be a YAML section.")
                return@mapIndexedNotNull null
            }
            val display = parseDisplay(values["display"], "$path.display", "$buttonPath variant $index", diagnostics)
                ?: return@mapIndexedNotNull null
            val priority = parsePriority(values["priority"], "$path.priority", diagnostics)
            val condition = when (val value = values["condition"]) {
                null -> null
                is String -> value.trim().takeIf { it.isNotEmpty() }
                else -> {
                    diagnostics += error("button.invalid_variant_condition", "$path.condition", "condition must be a string.")
                    null
                }
            }
            ContainerButtonVariantDefinition(
                priority = priority,
                order = index,
                condition = condition,
                display = display,
                actions = parseActions(values["actions"], "$path.actions", diagnostics)
            )
        }

        return parsed.sortedWith(
            compareBy<ContainerButtonVariantDefinition> { it.priority ?: Int.MAX_VALUE }
                .thenBy { it.order }
        )
    }

    /** 解析变体 priority；未配置时返回 null，交由声明顺序决定。 */
    private fun parsePriority(
        raw: Any?,
        path: String,
        diagnostics: MutableList<ContainerMenuDiagnostic>
    ): Int? {
        if (raw == null) return null
        val priority = when (raw) {
            is Byte, is Short, is Int, is Long -> (raw as Number).toLong().takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt()
            is Float, is Double -> (raw as Number).toDouble().takeIf { it.isFinite() && it % 1.0 == 0.0 }?.toInt()
            is String -> raw.trim().toIntOrNull()
            else -> null
        }
        if (priority == null) {
            diagnostics += error(
                "button.invalid_variant_priority",
                path,
                "priority must be an integer."
            )
        }
        return priority
    }

    /** 读取静态 tick 周期；0 和负数表示禁用，非整数配置属于错误。 */
    private fun parseUpdateInterval(
        raw: Any?,
        path: String,
        diagnostics: MutableList<ContainerMenuDiagnostic>
    ): Long? {
        if (raw == null) return null
        val ticks = when (raw) {
            is Byte, is Short, is Int, is Long -> (raw as Number).toLong()
            is Float, is Double -> {
                val number = (raw as Number).toDouble()
                if (!number.isFinite() || number % 1.0 != 0.0) null else number.toLong()
            }
            is String -> raw.trim().toLongOrNull()
            else -> null
        }
        if (ticks == null) {
            diagnostics += error(
                "update.invalid_interval",
                path,
                "Update interval must be an integer number of ticks."
            )
            return null
        }
        if (ticks <= 0) return null
        if (ticks < 5) {
            diagnostics += warning(
                "update.high_frequency",
                path,
                "Update interval $ticks is very frequent and may increase menu rendering cost."
            )
        }
        return ticks
    }

    private fun parseActions(
        raw: Any?,
        actionsPath: String,
        diagnostics: MutableList<ContainerMenuDiagnostic>
    ): Map<ContainerClickType, List<Any>> {
        if (raw == null) return emptyMap()
        val values = asStringKeyedMap(raw)
        if (values == null) {
            diagnostics += error("button.invalid_actions", actionsPath, "actions must be a YAML section.")
            return emptyMap()
        }

        val result = linkedMapOf<ContainerClickType, List<Any>>()
        values.forEach { (key, rawActions) ->
            val clickType = ContainerClickType.fromConfigKey(key)
            if (clickType == null) {
                diagnostics += error(
                    "button.unsupported_click_type",
                    "$actionsPath.$key",
                    "Unsupported click type '$key'."
                )
                return@forEach
            }

            val actions = when (rawActions) {
                is String -> listOf(rawActions)
                is List<*> -> rawActions.map(::freezeActionValue)
                else -> {
                    diagnostics += error(
                        "button.invalid_action_list",
                        "$actionsPath.$key",
                        "Click actions must be a string or list."
                    )
                    emptyList()
                }
            }
            result[clickType] = actions
        }
        return result.toMap()
    }

    /** 将 ConfigurationSection 或变体 Map 统一转换为字符串键 Map。 */
    private fun asStringKeyedMap(raw: Any?): Map<String, Any?>? {
        return when (raw) {
            is ConfigurationSection -> raw.getKeys(false).associateWith { key -> raw.get(key) }
            is Map<*, *> -> raw.entries.associate { (key, value) -> key.toString() to value }
            else -> null
        }
    }

    private fun validateButtonReferences(
        layout: ContainerLayoutDefinition,
        buttons: Map<String, ContainerButtonDefinition>,
        diagnostics: MutableList<ContainerMenuDiagnostic>
    ) {
        layout.slotsByButton.forEach { (id, slots) ->
            if (id !in buttons) {
                val first = layout.slots[slots.first()]
                diagnostics += error(
                    "layout.button_not_found",
                    "Layout[${first.row}][${first.column}]",
                    "Layout references button '$id', but Buttons.$id is not defined."
                )
            }
        }
        buttons.keys.filterNot(layout.slotsByButton::containsKey).forEach { id ->
            diagnostics += warning(
                "button.unused",
                "Buttons.$id",
                "Button '$id' is defined but not used by Layout."
            )
        }
    }

    /** 将 Bukkit YAML 值递归复制为只读配置值。 */
    private fun freeze(value: Any?): ContainerConfigValue = when (value) {
        null -> ContainerConfigValue.Null
        is ConfigurationSection -> ContainerConfigValue.Mapping(
            value.getKeys(false).associateWith { key -> freeze(value.get(key)) }
        )
        is Map<*, *> -> ContainerConfigValue.Mapping(
            value.entries.associate { (key, child) -> key.toString() to freeze(child) }
        )
        is List<*> -> ContainerConfigValue.Sequence(value.map(::freeze))
        is String, is Number, is Boolean -> ContainerConfigValue.Scalar(value)
        else -> ContainerConfigValue.Scalar(value.toString())
    }

    /** 深复制动作条件 Map 和嵌套列表，使定义不依赖 SnakeYAML 返回的可变集合。 */
    private fun freezeActionValue(value: Any?): Any = when (value) {
        null -> ""
        is ConfigurationSection -> value.getKeys(false).associateWith { key -> freezeActionValue(value.get(key)) }
        is Map<*, *> -> value.entries.associate { (key, child) -> key.toString() to freezeActionValue(child) }
        is List<*> -> value.map(::freezeActionValue)
        is String, is Number, is Boolean -> value
        else -> value.toString()
    }

    private fun error(code: String, path: String, message: String): ContainerMenuDiagnostic =
        ContainerMenuDiagnostic(ContainerDiagnosticSeverity.ERROR, code, path, message)

    private fun warning(code: String, path: String, message: String): ContainerMenuDiagnostic =
        ContainerMenuDiagnostic(ContainerDiagnosticSeverity.WARNING, code, path, message)

    private val FREE_SLOT_ID = Regex("[A-Za-z0-9_.-]+")
}
