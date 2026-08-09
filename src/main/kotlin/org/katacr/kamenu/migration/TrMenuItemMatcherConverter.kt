package org.katacr.kamenu.migration

/** 将 源菜单 ItemMatcher 的可证明兼容子集转换为 KaMenu item/hasItem 语法。 */
internal class TrMenuItemMatcherConverter(
    private val variables: TrMenuVariableConverter
) {
    /** 转换 give-item/take-item；一条 源菜单 多组 matcher 会展开为多条 KaMenu 动作。 */
    fun convertAction(
        raw: String,
        operation: String,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): List<Any> {
        val rewritten = variables.rewrite(raw, path, diagnostics, strict = true) ?: return emptyList()
        return rewritten.split(Regex("\\s*;\\s*")).mapNotNull { group ->
            val matcher = parseGroup(group, path, diagnostics) ?: return@mapNotNull null
            if (matcher.material == null) {
                unsupported(group, path, diagnostics, "ItemMatcher requires a material for portable migration.")
                return@mapNotNull null
            }
            if (matcher.data != null || matcher.name != null || matcher.head != null || matcher.customModelId != null) {
                unsupported(group, path, diagnostics, "This ItemMatcher uses data, name, head, or model-data that KaMenu item actions cannot preserve.")
                return@mapNotNull null
            }
            if (operation == "give" && matcher.lore != null) {
                unsupported(group, path, diagnostics, "KaMenu item give ignores lore construction, so this matcher was skipped.")
                return@mapNotNull null
            }
            buildString {
                append("item: type=").append(operation)
                append(";mats=").append(matcher.material)
                append(";amount=").append(matcher.amount ?: "1")
                if (operation == "take") matcher.lore?.let { append(";lore=").append(it) }
            }
        }
    }

    /** 转换 Kether `item <matcher>` 条件；多组 matcher 使用 AND 保持 源菜单 语义。 */
    fun convertCondition(
        raw: String,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): String? {
        val groups = raw.split(Regex("\\s*;\\s*")).mapNotNull { group ->
            val matcher = parseGroup(group, path, diagnostics) ?: return@mapNotNull null
            if (matcher.material == null || matcher.data != null || matcher.name != null || matcher.head != null) {
                unsupported(group, path, diagnostics, "Item condition requires material and cannot contain data, name, or head traits.")
                return null
            }
            buildString {
                append("hasItem.[mats=").append(matcher.material)
                append(";amount=").append(matcher.amount ?: "1")
                matcher.lore?.let { append(";lore=").append(it) }
                matcher.customModelId?.let { append(";custom_model_id=").append(it) }
                append(']')
            }
        }
        if (groups.isEmpty()) return null
        return if (groups.size == 1) groups.single() else groups.joinToString(" && ", "(", ")")
    }

    /** 将 源菜单 右键绑定 ItemMatcher 转换为 KaMenu 独立物品绑定字段。 */
    fun convertBinding(
        raw: String,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): Map<String, Any>? {
        val merged = linkedMapOf<String, String>()
        val groups = raw.split(Regex("\\s*;\\s*")).filter(String::isNotBlank)
        if (groups.isEmpty()) {
            unsupported(raw, path, diagnostics, "Item binding has no matcher traits.")
            return null
        }
        for (group in groups) {
            val matcher = parseGroup(group, path, diagnostics) ?: return null
            if (matcher.head != null) {
                unsupported(group, path, diagnostics, "Head owner or texture matching has no portable Bukkit-only equivalent.")
                return null
            }
            val traits = linkedMapOf(
                "material" to matcher.material,
                "target-lore" to matcher.lore,
                "target-name" to matcher.name,
                "data" to matcher.data,
                "custom-model-data" to matcher.customModelId
            )
            for ((key, value) in traits) {
                if (value == null) continue
                val existing = merged[key]
                if (existing != null && existing != value) {
                    unsupported(raw, path, diagnostics, "Multiple matcher groups require conflicting '$key' values.")
                    return null
                }
                merged[key] = value
            }
        }
        if (merged.isEmpty()) {
            unsupported(raw, path, diagnostics, "Amount-only bindings would match every held item because TrMenu ignores amount here.")
            return null
        }
        listOf("data", "custom-model-data").forEach { key ->
            val value = merged[key] ?: return@forEach
            if (value.toIntOrNull() == null) {
                unsupported(raw, path, diagnostics, "Binding trait '$key' must be an integer.")
                return null
            }
        }
        return linkedMapOf<String, Any>().also { output ->
            merged.forEach { (key, value) ->
                output[key] = if (key == "data" || key == "custom-model-data") value.toInt() else value
            }
        }
    }

    private fun parseGroup(
        raw: String,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): Matcher? {
        val values = linkedMapOf<String, String>()
        raw.split(',').forEach { trait ->
            val split = trait.split(':', '=', limit = 2)
            if (split.size != 2) return@forEach
            val key = split[0].trim().lowercase()
            if (key.startsWith('!')) {
                unsupported(raw, path, diagnostics, "Opposed ItemMatcher traits are not supported.")
                return null
            }
            val canonical = when {
                key.matches(Regex("mat(erial)?s?")) -> "material"
                key.matches(Regex("(amount|amt)s?")) -> "amount"
                key.matches(Regex("datas?")) -> "data"
                key.matches(Regex("model-?datas?")) -> "model-data"
                key.matches(Regex("names?")) -> "name"
                key.matches(Regex("lores?")) -> "lore"
                key.matches(Regex("(head|skull|texture)s?")) -> "head"
                else -> {
                    unsupported(raw, path, diagnostics, "Unknown ItemMatcher trait '$key'.")
                    return null
                }
            }
            values[canonical] = split[1].trim()
        }
        if (values.isEmpty()) return null
        return Matcher(
            material = values["material"],
            amount = values["amount"],
            lore = values["lore"],
            customModelId = values["model-data"],
            data = values["data"],
            name = values["name"],
            head = values["head"]
        )
    }

    private fun unsupported(
        raw: String,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics,
        reason: String
    ) {
        diagnostics.add(
            "TRM_ITEM_MATCHER_UNSUPPORTED",
            TrMenuMigrationSeverity.WARNING,
            TrMenuMigrationCompatibility.UNSUPPORTED,
            path,
            "$reason Source: $raw"
        )
    }

    private data class Matcher(
        val material: String?,
        val amount: String?,
        val lore: String?,
        val customModelId: String?,
        val data: String?,
        val name: String?,
        val head: String?
    )
}
