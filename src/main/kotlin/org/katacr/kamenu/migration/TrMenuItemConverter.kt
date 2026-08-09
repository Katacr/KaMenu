package org.katacr.kamenu.migration

/** 一层待转换的 源菜单 点击动作来源，顺序即最终动作拼接顺序。 */
internal data class TrMenuActionLayer(
    val raw: Any?,
    val path: String
)

/** 已展开继承语义的 源菜单 图标状态。 */
internal data class TrMenuButtonStateConversion(
    val path: String,
    val priority: Int?,
    val condition: String?,
    val display: Map<String, Any>,
    val actionLayers: List<TrMenuActionLayer>
)

/** 一个 源菜单 图标的默认状态、条件变体和目标刷新周期。 */
internal data class TrMenuButtonVisualConversion(
    val placement: TrMenuButtonPlacement,
    val viewCondition: String?,
    val updateIntervalTicks: Long?,
    val defaultState: TrMenuButtonStateConversion,
    val variants: List<TrMenuButtonStateConversion>
)

/**
 * 将 源菜单 图标显示字段和 nested icons 展开为 KaMenu 按钮状态。
 *
 * 该转换器不会执行材质来源脚本，也不转换动作和条件；它只保留动作层与原始条件，
 * 供后续专用转换器处理。
 */
internal class TrMenuItemConverter {
    private enum class InheritedField {
        NAME,
        LORE,
        AMOUNT,
        GLOW,
        ENCHANTMENTS,
        FLAGS,
        NBT,
        TOOLTIP,
        ITEM_MODEL,
        HIDE_TOOLTIP,
        UNBREAKABLE,
        DATA,
        ACTIONS
    }

    /** 转换布局中所有可见图标；默认图标缺少材质时记录文件级错误。 */
    fun convert(
        layout: TrMenuLayoutConversion,
        diagnostics: TrMenuMigrationDiagnostics
    ): List<TrMenuButtonVisualConversion> = layout.buttons.mapNotNull { placement ->
        convertButton(placement, diagnostics)
    }

    private fun convertButton(
        placement: TrMenuButtonPlacement,
        diagnostics: TrMenuMigrationDiagnostics
    ): TrMenuButtonVisualConversion? {
        val section = placement.section
        val path = placement.path
        val displaySection = section.section(TrMenuSourceProperty.ICON_DISPLAY, "$path.display", diagnostics)
        val defaultDisplay = buildDisplay(displaySection, null, emptySet(), "$path.display", diagnostics)
        if (!defaultDisplay.containsKey("material")) {
            diagnostics.add(
                code = "TRM_ITEM_MATERIAL_MISSING",
                severity = TrMenuMigrationSeverity.ERROR,
                compatibility = TrMenuMigrationCompatibility.INVALID,
                path = "$path.display.material",
                message = "Default TrMenu icon '${placement.sourceId}' does not define a material."
            )
            return null
        }

        val defaultActions = section.value(TrMenuSourceProperty.ACTIONS, "$path.actions", diagnostics)
        val viewCondition = section.value(TrMenuSourceProperty.CONDITION, "$path.condition", diagnostics)
            ?.toString()
            ?.trim()
            ?.takeIf(String::isNotEmpty)
        val defaultState = TrMenuButtonStateConversion(
            path = path,
            priority = null,
            condition = null,
            display = defaultDisplay,
            actionLayers = listOfNotNull(defaultActions?.let { TrMenuActionLayer(it, "$path.actions") })
        )
        val variants = parseVariants(section, defaultState, diagnostics)
        val updateInterval = resolveUpdateInterval(section, path, diagnostics)
        return TrMenuButtonVisualConversion(placement, viewCondition, updateInterval, defaultState, variants)
    }

    private fun parseVariants(
        section: TrMenuSourceSection,
        defaultState: TrMenuButtonStateConversion,
        diagnostics: TrMenuMigrationDiagnostics
    ): List<TrMenuButtonStateConversion> {
        val raw = section.value(TrMenuSourceProperty.ICON_SUB_ICONS, "${defaultState.path}.icons", diagnostics)
            ?: return emptyList()
        val entries = if (raw is List<*>) raw else listOf(raw)
        return entries.mapIndexedNotNull { index, value ->
            val variant = value as? TrMenuSourceSection
            if (variant == null) {
                diagnostics.add(
                    code = "TRM_ICON_VARIANT_INVALID",
                    severity = TrMenuMigrationSeverity.WARNING,
                    compatibility = TrMenuMigrationCompatibility.INVALID,
                    path = "${defaultState.path}.icons[$index]",
                    message = "Nested icon variant must be a YAML section and was skipped."
                )
                return@mapIndexedNotNull null
            }
            val path = "${defaultState.path}.icons[$index]"
            val inherit = parseInheritedFields(
                variant.value(TrMenuSourceProperty.INHERIT, "$path.inherit", diagnostics),
                "$path.inherit",
                diagnostics
            )
            val append = parseInheritedFields(
                variant.value(TrMenuSourceProperty.APPEND, "$path.append", diagnostics),
                "$path.append",
                diagnostics
            )
            val variantDisplay = variant.section(TrMenuSourceProperty.ICON_DISPLAY, "$path.display", diagnostics)
            val display = buildDisplay(variantDisplay, defaultState.display, inherit, "$path.display", diagnostics)
            val ownActions = variant.value(TrMenuSourceProperty.ACTIONS, "$path.actions", diagnostics)
            val actionLayers = when {
                InheritedField.ACTIONS in inherit -> listOfNotNull(
                    defaultState.actionLayers.firstOrNull(),
                    ownActions?.let { TrMenuActionLayer(it, "$path.actions") }
                )
                InheritedField.ACTIONS in append -> listOfNotNull(
                    ownActions?.let { TrMenuActionLayer(it, "$path.actions") },
                    defaultState.actionLayers.firstOrNull()
                )
                else -> listOfNotNull(ownActions?.let { TrMenuActionLayer(it, "$path.actions") })
            }
            val priority = parseInteger(
                variant.value(TrMenuSourceProperty.PRIORITY, "$path.priority", diagnostics)
            ) ?: index
            val condition = variant.value(TrMenuSourceProperty.CONDITION, "$path.condition", diagnostics)
                ?.toString()
                ?.trim()
                ?.takeIf(String::isNotEmpty)
            TrMenuButtonStateConversion(path, priority, condition, display, actionLayers)
        }.sortedBy { it.priority }
    }

    private fun buildDisplay(
        display: TrMenuSourceSection?,
        parent: Map<String, Any>?,
        inherit: Set<InheritedField>,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): Map<String, Any> {
        val result = linkedMapOf<String, Any>()
        val materialFrames = stringFrames(
            display?.value(TrMenuSourceProperty.ICON_MATERIAL, "$path.material", diagnostics)
        )
        if (materialFrames.isNotEmpty()) {
            if (materialFrames.size > 1) reportFirstFrame("material", path, diagnostics)
            result.putAll(convertMaterial(materialFrames.first(), "$path.material", diagnostics))
        } else if (parent != null) {
            copyProperties(parent, result, "material", "skull_owner", "skull_texture", "custom_model_data")
        }

        val nameFrames = stringFrames(display?.value(TrMenuSourceProperty.ICON_NAME, "$path.name", diagnostics))
        when {
            nameFrames.isNotEmpty() -> {
                if (nameFrames.size > 1) reportFirstFrame("name", path, diagnostics)
                result["name"] = nameFrames.first()
            }
            parent != null && InheritedField.NAME in inherit -> copyProperties(parent, result, "name")
        }

        val loreFrames = loreFrames(display?.value(TrMenuSourceProperty.ICON_LORE, "$path.lore", diagnostics))
        when {
            loreFrames.isNotEmpty() -> {
                if (loreFrames.size > 1) reportFirstFrame("lore", path, diagnostics)
                result["lore"] = loreFrames.first().flatMap { it.split('\n') }
            }
            parent != null && InheritedField.LORE in inherit -> copyProperties(parent, result, "lore")
        }

        inheritOrRead(
            result,
            parent,
            inherit,
            InheritedField.AMOUNT,
            "amount",
            display?.value(TrMenuSourceProperty.ICON_AMOUNT, "$path.amount", diagnostics) ?: "1"
        )

        val enchantRaw = display?.value(TrMenuSourceProperty.ICON_ENCHANT, "$path.enchant", diagnostics)
        val explicitGlow = display?.value(TrMenuSourceProperty.ICON_SHINY, "$path.shiny", diagnostics)
        val glow = explicitGlow ?: enchantRaw?.takeIf { isBooleanText(it) }
        inheritOrRead(result, parent, inherit, InheritedField.GLOW, "glow", glow ?: "false")

        val enchantments = parseEnchantments(enchantRaw, "$path.enchant", diagnostics)
        inheritOrRead(
            result,
            parent,
            inherit,
            InheritedField.ENCHANTMENTS,
            "enchantments",
            enchantments.takeIf(Map<String, Any>::isNotEmpty)
        )

        val flags = stringList(display?.value(TrMenuSourceProperty.ICON_FLAGS, "$path.flags", diagnostics))
        inheritOrRead(
            result,
            parent,
            inherit,
            InheritedField.FLAGS,
            "item_flags",
            flags.takeIf(List<String>::isNotEmpty)
        )

        val itemModel = display?.value(TrMenuSourceProperty.ICON_ITEM_MODEL, "$path.model", diagnostics)
            ?.toString()
            ?.takeIf(String::isNotBlank)
        inheritOrRead(result, parent, inherit, InheritedField.ITEM_MODEL, "item_model", itemModel)

        val unbreakable = display?.value(TrMenuSourceProperty.ICON_UNBREAKABLE, "$path.unbreakable", diagnostics)
            ?.toString()
            ?.takeIf(String::isNotBlank)
        inheritOrRead(result, parent, inherit, InheritedField.UNBREAKABLE, "unbreakable", unbreakable ?: "false")

        reportUnsupportedDisplay(display, path, diagnostics)
        return result
    }

    private fun convertMaterial(
        raw: String,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): Map<String, Any> {
        val result = linkedMapOf<String, Any>()
        var material = raw.trim()
        TrMenuPrivateUtilityConverter.convertEquipmentSource(material)?.let { equipmentSource ->
            result["material"] = equipmentSource
            return result
        }
        val modelData = Regex("(?i)[<{]model-?data[:=](\\d+?)[>}]").find(material)
        if (modelData != null) {
            result["custom_model_data"] = modelData.groupValues[1]
            material = material.replace(modelData.value, "")
        }
        listOf(
            "data-value" to Regex("(?i)[<{]data-?value[:=](.+?)[>}]"),
            "dye" to Regex("(?i)[<{]dye[:=](\\d{1,3},\\d{1,3},\\d{1,3})[>}]"),
            "banner" to Regex("(?i)[<{]banner[:=](.+?)[>}]")
        ).forEach { (feature, regex) ->
            regex.find(material)?.let { match ->
                diagnostics.add(
                    code = "TRM_ITEM_META_UNSUPPORTED",
                    severity = TrMenuMigrationSeverity.WARNING,
                    compatibility = TrMenuMigrationCompatibility.UNSUPPORTED,
                    path = path,
                    message = "TrMenu material metadata '$feature' is not supported and was removed."
                )
                material = material.replace(match.value, "")
            }
        }

        val head = Regex("(?i)^[<{]?(?:(?:player|custom|textured?)-?)?(?:head|skull)[:=](\\S+)[>}]?$")
            .matchEntire(material)
        if (head != null) {
            val value = head.groupValues[1]
            result["material"] = "PLAYER_HEAD"
            if (looksLikeTexture(value)) result["skull_texture"] = value else result["skull_owner"] = value
            return result
        }

        val source = Regex("(?i)^[<{]?source[:=](.+)[>}]?$").matchEntire(material)
        if (source != null) {
            val payload = source.groupValues[1]
            val separator = payload.indexOfAny(charArrayOf(':', '='))
            if (separator > 0 && separator < payload.lastIndex) {
                val provider = payload.substring(0, separator).replace("-", "").uppercase()
                val id = payload.substring(separator + 1)
                val mapped = when (provider) {
                    "ITEMSADDER", "IA" -> "itemsadder:$id"
                    "ORAXEN", "ORX" -> "oraxen:$id"
                    "CRAFTENGINE", "CE" -> "craftengine:$id"
                    else -> null
                }
                if (mapped != null) {
                    result["material"] = mapped
                    return result
                }
            }
            reportUnsupportedMaterial(raw, path, diagnostics)
            result["material"] = raw
            return result
        }

        if (Regex("(?i)^[<{]?(?:repo|mod)[:=]").containsMatchIn(material) ||
            material.trimStart().startsWith("{")
        ) {
            reportUnsupportedMaterial(raw, path, diagnostics)
        }
        result["material"] = material.ifBlank { raw }
        return result
    }

    private fun parseInheritedFields(
        raw: Any?,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): Set<InheritedField> {
        if (raw == null) return emptySet()
        if (raw !is List<*> && raw.toString().equals("true", ignoreCase = true)) {
            return setOf(InheritedField.NAME, InheritedField.LORE)
        }
        val values = if (raw is List<*>) raw else listOf(raw)
        return values.mapNotNull { value ->
            val token = value?.toString()?.trim()?.uppercase().orEmpty()
            val normalized = if (token.startsWith("ICON_")) token else "ICON_$token"
            when {
                token == "ACTIONS" -> InheritedField.ACTIONS
                normalized == "ICON_DISPLAY_NAME" -> InheritedField.NAME
                normalized == "ICON_DISPLAY_LORE" -> InheritedField.LORE
                normalized == "ICON_DISPLAY_AMOUNT" -> InheritedField.AMOUNT
                normalized == "ICON_DISPLAY_SHINY" -> InheritedField.GLOW
                normalized == "ICON_DISPLAY_ENCHANT" -> InheritedField.ENCHANTMENTS
                normalized == "ICON_DISPLAY_FLAGS" -> InheritedField.FLAGS
                normalized == "ICON_DISPLAY_NBT" -> InheritedField.NBT
                normalized == "ICON_DISPLAY_TOOLTIP" -> InheritedField.TOOLTIP
                normalized == "ICON_DISPLAY_ITEM_MODEL" -> InheritedField.ITEM_MODEL
                normalized == "ICON_DISPLAY_HIDE_TOOLTIP" -> InheritedField.HIDE_TOOLTIP
                normalized == "ICON_DISPLAY_UNBREAKABLE" -> InheritedField.UNBREAKABLE
                normalized == "ICON_DISPLAY_DATA" -> InheritedField.DATA
                else -> {
                    diagnostics.add(
                        code = "TRM_ICON_INHERIT_UNKNOWN",
                        severity = TrMenuMigrationSeverity.WARNING,
                        compatibility = TrMenuMigrationCompatibility.APPROXIMATE,
                        path = path,
                        message = "Unknown TrMenu inherit/append field '${value.toString()}' was ignored."
                    )
                    null
                }
            }
        }.toSet()
    }

    private fun resolveUpdateInterval(
        section: TrMenuSourceSection,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): Long? {
        val refresh = parseLong(section.value(TrMenuSourceProperty.ICON_REFRESH, "$path.refresh", diagnostics))
            ?.takeIf { it > 0 }
        val updatesRaw = section.value(TrMenuSourceProperty.ICON_UPDATE, "$path.update", diagnostics)
        val updates = (if (updatesRaw is List<*>) updatesRaw else listOfNotNull(updatesRaw))
            .mapNotNull(::parseLong)
            .filter { it > 0 }
        val intervals = listOfNotNull(refresh) + updates
        if (intervals.distinct().size > 1) {
            diagnostics.add(
                code = "TRM_ICON_UPDATE_APPROXIMATE",
                severity = TrMenuMigrationSeverity.WARNING,
                compatibility = TrMenuMigrationCompatibility.APPROXIMATE,
                path = "$path.update",
                message = "Different TrMenu refresh/update periods were reduced to one ${intervals.minOrNull()} tick button refresh."
            )
        }
        return intervals.minOrNull()
    }

    private fun reportUnsupportedDisplay(
        display: TrMenuSourceSection?,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ) {
        if (display == null) return
        listOf(
            TrMenuSourceProperty.ICON_DATA to "data",
            TrMenuSourceProperty.ICON_NBT to "nbt",
            TrMenuSourceProperty.ICON_TOOLTIP to "tooltip",
            TrMenuSourceProperty.ICON_HIDE_TOOLTIP to "hide_tooltip"
        ).forEach { (property, name) ->
            val raw = display.value(property, "$path.$name", diagnostics) ?: return@forEach
            if (raw.toString().isBlank() || raw.toString().equals("false", true)) return@forEach
            diagnostics.add(
                code = "TRM_ITEM_PROPERTY_UNSUPPORTED",
                severity = TrMenuMigrationSeverity.WARNING,
                compatibility = TrMenuMigrationCompatibility.UNSUPPORTED,
                path = "$path.$name",
                message = "TrMenu item property '$name' has no portable KaMenu Container mapping and was skipped."
            )
        }
    }

    private fun parseEnchantments(
        raw: Any?,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): Map<String, Any> {
        if (raw == null || isBooleanText(raw)) return emptyMap()
        val result = linkedMapOf<String, Any>()

        fun add(id: String, level: Any?) {
            val key = id.trim().lowercase()
            val value = level?.toString()?.trim().orEmpty().ifBlank { "1" }
            if (key.isNotEmpty()) result[key] = value
        }

        fun parse(value: Any?) {
            when (value) {
                null -> Unit
                is List<*> -> value.forEach(::parse)
                is TrMenuSourceSection -> {
                    val entries = value.entries()
                    val descriptorKey = entries.firstOrNull { (key, _) ->
                        key.equals("id", true) || key.equals("key", true) || key.equals("type", true) ||
                            key.equals("enchant", true) || key.equals("enchantment", true)
                    }
                    if (descriptorKey != null) {
                        val level = entries.firstOrNull { (key, _) ->
                            key.equals("level", true) || key.equals("lvl", true) || key.equals("value", true) ||
                                key.equals("amount", true)
                        }?.second
                        add(descriptorKey.second?.toString().orEmpty(), level)
                    } else {
                        entries.forEach { (key, level) -> add(key, level) }
                    }
                }
                else -> {
                    val text = value.toString().trim()
                    val whitespace = text.split(Regex("[,\\s]+"), limit = 2)
                    val colon = text.lastIndexOf(':')
                    when {
                        whitespace.size == 2 -> add(whitespace[0], whitespace[1])
                        colon in 1 until text.lastIndex && text.substring(colon + 1).toIntOrNull() != null ->
                            add(text.substring(0, colon), text.substring(colon + 1))
                        text.isNotEmpty() -> add(text, "1")
                        else -> diagnostics.add(
                            code = "TRM_ITEM_ENCHANT_INVALID",
                            severity = TrMenuMigrationSeverity.WARNING,
                            compatibility = TrMenuMigrationCompatibility.INVALID,
                            path = path,
                            message = "Empty enchantment entry was skipped."
                        )
                    }
                }
            }
        }
        parse(raw)
        return result
    }

    private fun inheritOrRead(
        target: MutableMap<String, Any>,
        parent: Map<String, Any>?,
        inherit: Set<InheritedField>,
        field: InheritedField,
        targetKey: String,
        raw: Any?
    ) {
        if (parent != null && field in inherit) {
            copyProperties(parent, target, targetKey)
        } else if (raw != null) {
            target[targetKey] = raw
        }
    }

    private fun copyProperties(
        source: Map<String, Any>,
        target: MutableMap<String, Any>,
        vararg keys: String
    ) {
        keys.forEach { key -> source[key]?.let { target[key] = it } }
    }

    private fun stringFrames(raw: Any?): List<String> = when (raw) {
        null -> emptyList()
        is List<*> -> raw.mapNotNull { it?.toString() }
        else -> listOf(raw.toString())
    }

    private fun loreFrames(raw: Any?): List<List<String>> = when (raw) {
        null -> emptyList()
        is List<*> -> if (raw.firstOrNull() is List<*>) {
            raw.mapNotNull { frame -> (frame as? List<*>)?.mapNotNull { it?.toString() } }
        } else {
            listOf(raw.mapNotNull { it?.toString() })
        }
        else -> listOf(listOf(raw.toString()))
    }

    private fun stringList(raw: Any?): List<String> = when (raw) {
        null -> emptyList()
        is List<*> -> raw.mapNotNull { it?.toString() }
        else -> listOf(raw.toString())
    }

    private fun parseInteger(raw: Any?): Int? = when (raw) {
        is Number -> raw.toInt()
        else -> raw?.toString()?.trim()?.toIntOrNull()
    }

    private fun parseLong(raw: Any?): Long? = when (raw) {
        is Number -> raw.toLong()
        else -> raw?.toString()?.trim()?.toLongOrNull()
    }

    private fun isBooleanText(value: Any): Boolean =
        value is Boolean || value.toString().trim().matches(Regex("(?i)true|false|yes|no|on|off"))

    private fun looksLikeTexture(value: String): Boolean =
        value.length >= 64 || value.startsWith("http://", true) || value.startsWith("https://", true)

    private fun reportFirstFrame(
        property: String,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ) {
        diagnostics.add(
            code = "TRM_ICON_ANIMATION_FIRST_FRAME",
            severity = TrMenuMigrationSeverity.WARNING,
            compatibility = TrMenuMigrationCompatibility.APPROXIMATE,
            path = "$path.$property",
            message = "Animated $property was reduced to its first frame."
        )
    }

    private fun reportUnsupportedMaterial(
        raw: String,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ) {
        diagnostics.add(
            code = "TRM_ITEM_SOURCE_UNSUPPORTED",
            severity = TrMenuMigrationSeverity.WARNING,
            compatibility = TrMenuMigrationCompatibility.UNSUPPORTED,
            path = path,
            message = "TrMenu item source '$raw' is preserved for manual replacement and will not be executed."
        )
    }
}
