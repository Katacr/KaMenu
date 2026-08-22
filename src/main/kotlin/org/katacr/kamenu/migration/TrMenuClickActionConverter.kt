package org.katacr.kamenu.migration

import java.util.LinkedHashMap

/** 将 源菜单 图标点击类型和 Reaction 层合并为 KaMenu Container actions。 */
internal class TrMenuClickActionConverter(
    private val reactionConverter: TrMenuEventConverter
) {
    /** 按继承/append 展开后的层顺序合并相同点击类型动作。 */
    fun convert(
        layers: List<TrMenuActionLayer>,
        diagnostics: TrMenuMigrationDiagnostics
    ): Map<String, List<Any>> {
        val output = LinkedHashMap<String, MutableList<Any>>()
        layers.forEach { layer ->
            val section = layer.raw as? TrMenuSourceSection
            if (section == null) {
                val rawList = layer.raw as? List<*>
                if (rawList == null) {
                    diagnostics.add(
                        code = "TRM_CLICK_ACTIONS_INVALID",
                        severity = TrMenuMigrationSeverity.WARNING,
                        compatibility = TrMenuMigrationCompatibility.INVALID,
                        path = layer.path,
                        message = "TrMenu icon actions must be a YAML section keyed by click type or a flat action list and were skipped."
                    )
                    return@forEach
                }
                // 平铺动作列表等价于 all 点击类型
                val converted = reactionConverter.convertActionReactions(rawList, layer.path, diagnostics)
                if (converted.isNotEmpty()) {
                    output.getOrPut("all") { mutableListOf() }.addAll(converted)
                }
                return@forEach
            }
            section.entries().forEach { (rawTypes, rawActions) ->
                rawTypes.split(',', ';').map(String::trim).filter(String::isNotEmpty).forEach { rawType ->
                    val targetType = mapClickType(rawType, "${layer.path}.$rawTypes", diagnostics)
                        ?: return@forEach
                    val converted = reactionConverter.convertActionReactions(
                        rawActions,
                        "${layer.path}.$rawTypes",
                        diagnostics
                    )
                    if (converted.isNotEmpty()) {
                        output.getOrPut(targetType) { mutableListOf() }.addAll(converted)
                    }
                }
            }
        }
        return output.mapValuesTo(LinkedHashMap()) { (_, actions) -> actions.toList() }
    }

    private fun mapClickType(
        raw: String,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): String? {
        val normalized = raw.trim().uppercase().replace('-', '_')
        if (normalized in SUPPORTED_TYPES) return normalized.lowercase()
        if (normalized in UNSUPPORTED_TYPES) {
            diagnostics.add(
                code = "TRM_CLICK_TYPE_UNSUPPORTED",
                severity = TrMenuMigrationSeverity.WARNING,
                compatibility = TrMenuMigrationCompatibility.UNSUPPORTED,
                path = path,
                message = "TrMenu click type '$raw' has no KaMenu button-click equivalent and was skipped."
            )
            return null
        }
        diagnostics.add(
            code = "TRM_CLICK_TYPE_FALLBACK_ALL",
            severity = TrMenuMigrationSeverity.WARNING,
            compatibility = TrMenuMigrationCompatibility.APPROXIMATE,
            path = path,
            message = "Unknown TrMenu click type '$raw' falls back to ALL in TrMenu and was migrated as 'all'."
        )
        return "all"
    }

    companion object {
        private val SUPPORTED_TYPES = setOf(
            "ALL",
            "LEFT",
            "RIGHT",
            "SHIFT_LEFT",
            "SHIFT_RIGHT",
            "MIDDLE",
            "DROP",
            "CONTROL_DROP",
            "DOUBLE_CLICK",
            "OFFHAND",
            "NUMBER_KEY",
            "NUMBER_KEY_1",
            "NUMBER_KEY_2",
            "NUMBER_KEY_3",
            "NUMBER_KEY_4",
            "NUMBER_KEY_5",
            "NUMBER_KEY_6",
            "NUMBER_KEY_7",
            "NUMBER_KEY_8",
            "NUMBER_KEY_9"
        )
        private val UNSUPPORTED_TYPES = setOf(
            "ABROAD_LEFT_EMPTY",
            "ABROAD_RIGHT_EMPTY",
            "ABROAD_LEFT_ITEM",
            "ABROAD_RIGHT_ITEM",
            "LEFT_MOUSE_DRAG_ADD",
            "RIGHT_MOUSE_DRAG_ADD",
            "MIDDLE_MOUSE_DRAG_ADD",
            "UNKNOWN"
        )
    }
}
