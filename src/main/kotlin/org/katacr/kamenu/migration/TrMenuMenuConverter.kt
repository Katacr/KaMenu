package org.katacr.kamenu.migration

import org.bukkit.configuration.file.YamlConfiguration
import org.katacr.kamenu.container.ContainerMenuParser
import org.katacr.kamenu.container.ContainerMenuType
import java.util.Locale

/** 一份已生成并通过 Container 静态校验的 KaMenu 目标菜单。 */
internal data class TrMenuMenuConversion(
    val config: YamlConfiguration,
    val boundCommands: List<String>,
    val boundItems: List<TrMenuBoundItem>
)

/** 将一个已读取的 源菜单 源菜单组装为 KaMenu V2 Container 标准 YAML。 */
internal class TrMenuMenuConverter(
    private val menuIdResolver: (String) -> String?,
    private val syntaxValidator: (String) -> String? = { null }
) {
    /** 完整转换单个菜单；任何 ERROR 或目标解析失败都会返回 null。 */
    fun convert(
        source: TrMenuSourceMenu,
        targetMenuId: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): TrMenuMenuConversion? {
        val layout = TrMenuLayoutConverter().convert(source, diagnostics) ?: return null
        val functions = TrMenuFunctionRegistry.create(source.root, diagnostics, syntaxValidator)
        val nodeReferences = TrMenuNodeReferenceConverter(source.root)
        val variables = TrMenuVariableConverter(functions, nodeReferences)
        val conditionConverter = TrMenuConditionConverter(variables)
        val iconIds = layout.buttons.associate { it.sourceId to it.targetId }
        val actionConverter = TrMenuActionConverter(
            menuIdResolver = menuIdResolver,
            iconIdResolver = iconIds::get,
            functionGuardConverter = functions::convertGuard,
            variableConverter = variables
        )
        val eventConverter = TrMenuEventConverter(actionConverter, conditionConverter)
        val visuals = TrMenuItemConverter().convert(layout, diagnostics)
        val events = eventConverter.convert(source.root, diagnostics)

        val output = YamlConfiguration()
        output.options().header("Migrated from TrMenu stable-v3. Review migration diagnostics before use.")
        output.set("Type", layout.type.name)
        output.set(
            "Title",
            variables.rewriteValue(
                source.root.value(TrMenuSourceProperty.TITLE, "Title", diagnostics) ?: source.menuId,
                "Title",
                diagnostics
            )
        )
        output.set("Layout", layout.pages)
        writeTitleUpdate(output, source.root, diagnostics)
        writeSettings(output, source.root, variables, diagnostics)
        writeProperties(output, source.root, layout.type, diagnostics)
        writeEvents(output, events)
        writeButtons(output, visuals, variables, actionConverter, diagnostics)
        functions.scripts().forEach { (id, script) -> output.set("JavaScript.$id", script) }
        nodeReferences.writeReferences(output, variables, diagnostics)
        reportUnsupportedRootFeatures(source.root, diagnostics)

        if (diagnostics.hasErrors) return null
        val validation = ContainerMenuParser.parse(targetMenuId, output)
        validation.diagnostics.forEach { issue ->
            if (issue.severity.name != "ERROR") return@forEach
            diagnostics.add(
                code = "TRM_TARGET_VALIDATION_FAILED",
                severity = TrMenuMigrationSeverity.ERROR,
                compatibility = TrMenuMigrationCompatibility.INVALID,
                path = issue.path,
                message = issue.message
            )
        }
        if (validation.definition == null || diagnostics.hasErrors) return null
        return TrMenuMenuConversion(
            output,
            parseBoundCommands(source.root, diagnostics),
            parseBoundItems(source.root, targetMenuId, diagnostics)
        )
    }

    private fun writeTitleUpdate(
        output: YamlConfiguration,
        root: TrMenuSourceSection,
        diagnostics: TrMenuMigrationDiagnostics
    ) {
        val raw = root.value(TrMenuSourceProperty.TITLE_UPDATE, "Title-Update", diagnostics) ?: return
        val interval = raw.toString().toLongOrNull()
        if (interval == null) {
            diagnostics.add(
                "TRM_TITLE_UPDATE_INVALID",
                TrMenuMigrationSeverity.ERROR,
                TrMenuMigrationCompatibility.INVALID,
                "Title-Update",
                "Title-Update must be an integer tick value."
            )
        } else if (interval > 0L) {
            output.set("Title-Update", interval)
        }
    }

    private fun writeSettings(
        output: YamlConfiguration,
        root: TrMenuSourceSection,
        variables: TrMenuVariableConverter,
        diagnostics: TrMenuMigrationDiagnostics
    ) {
        val options = root.section(TrMenuSourceProperty.OPTIONS, "Options", diagnostics)
        val argumentsRaw = options?.value(TrMenuSourceProperty.OPTION_ARGUMENTS, "Options.Arguments", diagnostics)
        val arguments = parseBoolean(argumentsRaw, true)
        val defaultArguments = asStringList(
            options?.value(
                TrMenuSourceProperty.OPTION_DEFAULT_ARGUMENTS,
                "Options.Default-Arguments",
                diagnostics
            )
        ).mapIndexedNotNull { index, value ->
            variables.rewrite(value, "Options.Default-Arguments[$index]", diagnostics, strict = false)
        }
        if (arguments || defaultArguments.isNotEmpty()) {
            output.set("Settings.pass_arguments.enable", true)
            if (defaultArguments.isNotEmpty()) output.set("Settings.pass_arguments.default", defaultArguments)
        }
        if (!arguments && defaultArguments.isNotEmpty()) {
            diagnostics.add(
                "TRM_ARGUMENTS_APPROXIMATE",
                TrMenuMigrationSeverity.WARNING,
                TrMenuMigrationCompatibility.APPROXIMATE,
                "Options.Arguments",
                "TrMenu rejects command arguments while still providing defaults; KaMenu arguments remain enabled to preserve menu parameters."
            )
        }

        options?.value(TrMenuSourceProperty.OPTION_MIN_CLICK_DELAY, "Options.Min-Click-Delay", diagnostics)?.let { raw ->
            val delay = raw.toString().toLongOrNull()
            if (delay == null || delay < 0L) {
                diagnostics.add(
                    "TRM_MIN_CLICK_DELAY_INVALID",
                    TrMenuMigrationSeverity.ERROR,
                    TrMenuMigrationCompatibility.INVALID,
                    "Options.Min-Click-Delay",
                    "Min-Click-Delay must be a non-negative millisecond value."
                )
            } else output.set("Settings.min_click_delay", delay)
        }

        val expansions = asStringList(
            options?.value(TrMenuSourceProperty.OPTION_DEPEND_EXPANSIONS, "Options.Depend-Expansions", diagnostics)
        )
        if (expansions.isNotEmpty()) output.set("Settings.need_placeholder", expansions)

        val defaultLayout = options?.value(
            TrMenuSourceProperty.OPTION_DEFAULT_LAYOUT,
            "Options.Default-Layout",
            diagnostics
        )
        if (defaultLayout != null) {
            val text = when (defaultLayout) {
                is Number -> defaultLayout.toInt().toString()
                else -> defaultLayout.toString().trim()
            }
            if (text.toIntOrNull() != null) {
                output.set("Settings.default_page", text)
            } else {
                diagnostics.add(
                    "TRM_DEFAULT_LAYOUT_UNSUPPORTED",
                    TrMenuMigrationSeverity.WARNING,
                    TrMenuMigrationCompatibility.UNSUPPORTED,
                    "Options.Default-Layout",
                    "Default-Layout '$defaultLayout' is not a static page index and was skipped."
                )
            }
        }

        reportUnsupportedOption(
            root.value(TrMenuSourceProperty.OPTION_FREE_SLOTS, "Free-Slots", diagnostics),
            "Free-Slots",
            "Free-Slots",
            diagnostics
        )
        listOf(
            TrMenuSourceProperty.OPTION_FREE_SLOTS to "Free-Slots",
            TrMenuSourceProperty.OPTION_HIDE_PLAYER_INVENTORY to "Hide-Player-Inventory",
            TrMenuSourceProperty.OPTION_PURE_PACKET to "Pure-Packet",
            TrMenuSourceProperty.OPTION_COMMAND_FAKE_OP to "Command-Fake-Op"
        ).forEach { (property, name) ->
            reportUnsupportedOption(
                options?.value(property, "Options.$name", diagnostics),
                "Options.$name",
                name,
                diagnostics
            )
        }
    }

    /** 记录没有可移植 Container 映射的 源菜单 选项。 */
    private fun reportUnsupportedOption(
        value: Any?,
        path: String,
        name: String,
        diagnostics: TrMenuMigrationDiagnostics
    ) {
        if (value == null || value.toString().isBlank() || value.toString().equals("false", true) || value.toString() == "0") {
            return
        }
        diagnostics.add(
            code = "TRM_OPTION_UNSUPPORTED",
            severity = TrMenuMigrationSeverity.WARNING,
            compatibility = TrMenuMigrationCompatibility.UNSUPPORTED,
            path = path,
            message = "TrMenu option '$name' has no portable KaMenu Container equivalent and was skipped."
        )
    }

    private fun writeProperties(
        output: YamlConfiguration,
        root: TrMenuSourceSection,
        type: ContainerMenuType,
        diagnostics: TrMenuMigrationDiagnostics
    ) {
        val properties = root.section(TrMenuSourceProperty.PROPERTIES, "Properties", diagnostics) ?: return
        val values = properties.entries().associate { (key, value) ->
            key.uppercase(Locale.ROOT).replace('-', '_') to value
        }
        if (type.isFurnace) {
            writeFurnaceProgress(output, values, "FURNACE_BURN_TIME", "FURNACE_TICKS_FOR_CURRENT_FUEL", "burn_progress", diagnostics)
            writeFurnaceProgress(output, values, "FURNACE_COOK_TIME", "FURNACE_TICKS_FOR_CURRENT_SMELTING", "cook_progress", diagnostics)
        } else if (type == ContainerMenuType.ANVIL) {
            values["REPAIR_COST"]?.let { output.set("Properties.repair_cost", it) }
        }
        val supported = when {
            type.isFurnace -> FURNACE_PROPERTY_KEYS
            type == ContainerMenuType.ANVIL -> setOf("REPAIR_COST")
            else -> emptySet()
        }
        values.keys.filterNot(supported::contains).forEach { key ->
            diagnostics.add(
                "TRM_PROPERTY_UNSUPPORTED",
                TrMenuMigrationSeverity.WARNING,
                TrMenuMigrationCompatibility.UNSUPPORTED,
                "Properties.$key",
                "TrMenu inventory property '$key' has no target Container mapping and was skipped."
            )
        }
    }

    private fun writeFurnaceProgress(
        output: YamlConfiguration,
        values: Map<String, Any?>,
        currentKey: String,
        totalKey: String,
        targetKey: String,
        diagnostics: TrMenuMigrationDiagnostics
    ) {
        val currentRaw = values[currentKey] ?: return
        val totalRaw = values[totalKey]
        val current = currentRaw.toString().toDoubleOrNull()
        val total = totalRaw?.toString()?.toDoubleOrNull()
        if (current == null || total == null || total <= 0.0) {
            diagnostics.add(
                "TRM_PROPERTY_PROGRESS_APPROXIMATE",
                TrMenuMigrationSeverity.WARNING,
                TrMenuMigrationCompatibility.UNSUPPORTED,
                "Properties.$currentKey",
                "Furnace property '$currentKey' requires a positive '$totalKey' to convert ticks into a percentage."
            )
            return
        }
        output.set("Properties.$targetKey", (current / total * 100.0).coerceIn(0.0, 100.0))
    }

    private fun writeEvents(output: YamlConfiguration, events: TrMenuEventConversion) {
        if (events.open.isNotEmpty()) output.set("Events.Open", events.open)
        if (events.close.isNotEmpty()) output.set("Events.Close", events.close)
        events.tasks.forEach { (id, task) -> output.set("Events.Tasks.$id", task) }
    }

    private fun writeButtons(
        output: YamlConfiguration,
        visuals: List<TrMenuButtonVisualConversion>,
        variables: TrMenuVariableConverter,
        actionConverter: TrMenuActionConverter,
        diagnostics: TrMenuMigrationDiagnostics
    ) {
        visuals.forEach { visual ->
            val scopedVariables = variables.scopedToIcon(visual.placement.sourceId)
            val scopedConditions = TrMenuConditionConverter(scopedVariables)
            val scopedClicks = TrMenuClickActionConverter(
                TrMenuEventConverter(
                    actionConverter.scopedToIcon(visual.placement.sourceId),
                    scopedConditions
                )
            )
            val path = "Buttons.${visual.placement.targetId}"
            visual.placement.slotExpression?.let { expression ->
                val rewritten = variables.rewrite(expression, "$path.slot", diagnostics, strict = false)
                output.set("$path.slot", rewritten ?: expression)
            }
            visual.placement.slotFrames?.let { frames ->
                val rewrittenFrames = frames.map { frame ->
                    variables.rewrite(frame, "$path.slot", diagnostics, strict = false) ?: frame
                }
                val asLists = rewrittenFrames.map { frame -> frame.split(',').map { it.trim() }.filter { it.isNotEmpty() } }
                output.set("$path.slot", asLists)
            }
            visual.viewCondition?.let { raw ->
                val condition = scopedConditions.convert(raw, "${visual.placement.path}.condition", diagnostics) ?: "false"
                output.set("$path.view_condition", condition)
            }
            visual.updateIntervalTicks?.let { output.set("$path.update", it) }
            if (visual.variants.isEmpty()) {
                val display = rewriteDisplay(
                    visual.defaultState,
                    scopedVariables,
                    scopedConditions,
                    diagnostics
                ) ?: return@forEach
                output.set("$path.display", display)
                val actions = scopedClicks.convert(visual.defaultState.actionLayers, diagnostics)
                if (actions.isNotEmpty()) output.set("$path.actions", actions)
                return@forEach
            }

            val variants = mutableListOf<Map<String, Any>>()
            visual.variants.forEach { state ->
                val condition = scopedConditions.convert(state.condition, "${state.path}.condition", diagnostics)
                    ?: return@forEach
                val display = rewriteDisplay(state, scopedVariables, scopedConditions, diagnostics) ?: return@forEach
                variants += linkedMapOf<String, Any>().also { target ->
                    state.priority?.let { target["priority"] = it }
                    target["condition"] = condition
                    target["display"] = display
                    val actions = scopedClicks.convert(state.actionLayers, diagnostics)
                    if (actions.isNotEmpty()) target["actions"] = actions
                }
            }
            val fallbackDisplay = rewriteDisplay(
                visual.defaultState,
                scopedVariables,
                scopedConditions,
                diagnostics
            ) ?: return@forEach
            variants += linkedMapOf<String, Any>("display" to fallbackDisplay).also { target ->
                val actions = scopedClicks.convert(visual.defaultState.actionLayers, diagnostics)
                if (actions.isNotEmpty()) target["actions"] = actions
            }
            output.set("$path.variants", variants)
        }
    }

    private fun rewriteDisplay(
        state: TrMenuButtonStateConversion,
        variables: TrMenuVariableConverter,
        conditions: TrMenuConditionConverter,
        diagnostics: TrMenuMigrationDiagnostics
    ): Map<String, Any>? {
        val output = linkedMapOf<String, Any>()
        state.display.forEach { (key, value) ->
            val strict = key in STRICT_DISPLAY_FIELDS
            val fieldPath = "${state.path}.display.$key"
            val rewritten = if (key == "lore") {
                rewriteLore(value, fieldPath, variables, conditions, diagnostics)
            } else {
                variables.rewriteValue(value, fieldPath, diagnostics, strict)
            }
            if (rewritten == null && strict) {
                diagnostics.add(
                    "TRM_ITEM_DYNAMIC_PROPERTY_UNSUPPORTED",
                    TrMenuMigrationSeverity.ERROR,
                    TrMenuMigrationCompatibility.UNSUPPORTED,
                    "${state.path}.display.$key",
                    "Required item property '$key' contains an unsupported dynamic expression."
                )
                return null
            }
            if (rewritten != null) output[key] = rewritten
        }
        return output
    }

    /** 将源菜单 Lore 的内联条件转换为 KaMenu 行尾快捷条件，并过滤无法安全转换的行。 */
    private fun rewriteLore(
        raw: Any?,
        path: String,
        variables: TrMenuVariableConverter,
        conditions: TrMenuConditionConverter,
        diagnostics: TrMenuMigrationDiagnostics
    ): List<String> {
        val lines = if (raw is List<*>) raw else listOf(raw)
        return lines.mapIndexedNotNull { index, value ->
            val source = value?.toString() ?: return@mapIndexedNotNull null
            val linePath = "$path[$index]"
            val inline = parseTrMenuLoreCondition(source)
            val content = variables.rewrite(inline?.first ?: source, linePath, diagnostics, strict = false)
                ?: return@mapIndexedNotNull null
            val condition = inline?.second ?: return@mapIndexedNotNull content
            val expression = conditions.convert(condition, "$linePath.<condition>", diagnostics)
                ?: return@mapIndexedNotNull null
            if (content.isEmpty()) "{condition: $expression}" else "$content {condition: $expression}"
        }
    }

    /** 提取源菜单 Lore 行尾的 condition/requirement 修饰符，供迁移为 KaMenu 标准语法。 */
    private fun parseTrMenuLoreCondition(source: String): Pair<String, String>? {
        val value = source.trimEnd()
        val match = TRMENU_LORE_CONDITION_START.findAll(value).lastOrNull() ?: return null
        val opening = value[match.range.first]
        val closing = if (opening == '{') '}' else '>'
        if (value.lastOrNull() != closing) return null
        val condition = value.substring(match.range.last + 1, value.lastIndex).trim()
        if (condition.isEmpty()) return null
        val content = value.substring(0, match.range.first).trimEnd()
        return content to condition
    }

    private fun parseBoundCommands(
        root: TrMenuSourceSection,
        diagnostics: TrMenuMigrationDiagnostics
    ): List<String> {
        val bindings = root.section(TrMenuSourceProperty.BINDINGS, "Bindings", diagnostics) ?: return emptyList()
        val commands = asStringList(
            bindings.value(TrMenuSourceProperty.BINDING_COMMANDS, "Bindings.Commands", diagnostics)
        )
        return commands.mapNotNull { raw ->
            val command = raw.trim().removePrefix("(?i)").removePrefix("/").lowercase(Locale.ROOT)
            if (COMMAND_PATTERN.matches(command)) command else {
                diagnostics.add(
                    "TRM_BINDING_REGEX_UNSUPPORTED",
                    TrMenuMigrationSeverity.WARNING,
                    TrMenuMigrationCompatibility.UNSUPPORTED,
                    "Bindings.Commands",
                    "Regex or multi-word command binding '$raw' was skipped."
                )
                null
            }
        }.distinct()
    }

    /** 转换可由 Bukkit 物品属性证明等价的菜单右键绑定。 */
    private fun parseBoundItems(
        root: TrMenuSourceSection,
        targetMenuId: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): List<TrMenuBoundItem> {
        val bindings = root.section(TrMenuSourceProperty.BINDINGS, "Bindings", diagnostics) ?: return emptyList()
        val converter = TrMenuItemMatcherConverter(TrMenuVariableConverter())
        val sourceItems = asStringList(
            bindings.value(TrMenuSourceProperty.BINDING_ITEMS, "Bindings.Items", diagnostics)
        )
        if (sourceItems.isNotEmpty()) {
            diagnostics.add(
                "TRM_BINDING_ITEM_INTERVAL_DEFAULT",
                TrMenuMigrationSeverity.WARNING,
                TrMenuMigrationCompatibility.APPROXIMATE,
                "Bindings.Items",
                "Generated item bindings use TrMenu's default 2000ms interval; adjust cooldown-ms if settings.yml changed it."
            )
        }
        return sourceItems.mapIndexedNotNull { index, raw ->
            val values = converter.convertBinding(raw, "Bindings.Items[$index]", diagnostics) ?: return@mapIndexedNotNull null
            TrMenuBoundItem(
                id = bindingId(targetMenuId, index),
                values = linkedMapOf<String, Any>(
                    "enabled" to true,
                    "menu" to targetMenuId,
                    "require-sneaking" to false,
                    "cooldown-ms" to 2000L,
                    "ignore-case" to true,
                    "translate-colors" to true
                ).also { it.putAll(values) }
            )
        }
    }

    /** 报告不能安全转换为共享 KaMenu Container 语义的根级功能。 */
    private fun reportUnsupportedRootFeatures(
        root: TrMenuSourceSection,
        diagnostics: TrMenuMigrationDiagnostics
    ) {
        if (root.value(TrMenuSourceProperty.LANG, "Lang", diagnostics) != null) {
            diagnostics.add(
                "TRM_LANG_UNSUPPORTED",
                TrMenuMigrationSeverity.WARNING,
                TrMenuMigrationCompatibility.UNSUPPORTED,
                "Lang",
                "Menu-localized Lang nodes have no portable KaMenu Container equivalent and were skipped."
            )
        }
    }

    private fun asStringList(raw: Any?): List<String> = when (raw) {
        null -> emptyList()
        is List<*> -> raw.mapNotNull { it?.toString() }
        else -> listOf(raw.toString())
    }

    private fun parseBoolean(raw: Any?, default: Boolean): Boolean = when (raw) {
        null -> default
        is Boolean -> raw
        else -> raw.toString().equals("true", true)
    }

    companion object {
        private val COMMAND_PATTERN = Regex("^[a-z0-9][a-z0-9_-]*$")
        private val TRMENU_LORE_CONDITION_START = Regex(
            """[{<]\s*(?:condition|requirement)\s*[:=]\s*""",
            RegexOption.IGNORE_CASE
        )
        private val STRICT_DISPLAY_FIELDS = setOf("material", "amount", "custom_model_data", "item_model")
        private val FURNACE_PROPERTY_KEYS = setOf(
            "FURNACE_BURN_TIME",
            "FURNACE_TICKS_FOR_CURRENT_FUEL",
            "FURNACE_COOK_TIME",
            "FURNACE_TICKS_FOR_CURRENT_SMELTING"
        )

        /** 为目标菜单和源列表位置生成稳定且不含 YAML 路径分隔符的绑定 ID。 */
        private fun bindingId(menuId: String, index: Int): String {
            val normalized = menuId.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9_-]+"), "_").trim('_')
            return "trmenu_${normalized.ifEmpty { "menu" }}_${index + 1}"
        }
    }
}
