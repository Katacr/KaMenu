package org.katacr.kamenu.migration

/**
 * 将已知 TrMenu `utils` 只读调用转换为 KaMenu 标准条件和物品来源。
 *
 * 这里只接受参数结构和返回语义都能静态证明等价的调用；未知方法、目标玩家经济判断和
 * 带算术表达式的参数会交回通用迁移流程报告，而不会执行 TrMenu 私有 JavaScript。
 */
internal class TrMenuPrivateUtilityConverter(
    private val variables: TrMenuVariableConverter
) {
    private val itemMatchers = TrMenuItemMatcherConverter(variables)

    /** 转换条件中的固定 `utils` 调用；不是受支持调用时返回 null。 */
    fun convertCondition(
        raw: String,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): String? {
        val parsed = parseConditionCall(raw) ?: return null
        val expression = when (parsed.call.name.lowercase()) {
            "utils.isplayeronline" -> convertPlayerOnline(parsed.call, path, diagnostics)
            "utils.hasequipment" -> convertHasEquipment(parsed.call, path, diagnostics)
            "utils.hasmoney" -> convertHasMoney(parsed.call, path, diagnostics)
            "utils.hasitem" -> convertHasItem(parsed.call, path, diagnostics)
            else -> null
        } ?: return null
        return if (parsed.negated) "!($expression)" else expression
    }

    private fun convertPlayerOnline(
        call: UtilityCall,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): String? {
        if (call.arguments.size != 1) return null
        val playerName = rewritePlayerOperand(call.arguments[0], path, diagnostics) ?: return null
        return "isPlayerOnline.$playerName"
    }

    private fun convertHasEquipment(
        call: UtilityCall,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): String? {
        if (call.arguments.size != 2) return null
        val playerName = rewritePlayerOperand(call.arguments[0], path, diagnostics) ?: return null
        val slot = normalizeEquipmentSlot(unquote(call.arguments[1])) ?: return null
        return if (playerName.isEmpty()) "hasEquipment.[$slot]" else "hasEquipment.[$slot;$playerName]"
    }

    private fun convertHasMoney(
        call: UtilityCall,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): String? {
        if (call.arguments.size != 2 || !isCurrentPlayerOperand(call.arguments[0])) return null
        val amount = rewriteSimpleValue(call.arguments[1], path, diagnostics) ?: return null
        return "hasMoney.$amount"
    }

    private fun convertHasItem(
        call: UtilityCall,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): String? {
        if (call.arguments.size != 2 || !isCurrentPlayerOperand(call.arguments[0])) return null
        val matcherRaw = unwrapVars(call.arguments[1]) ?: return null
        val matcher = variables.rewrite(matcherRaw, path, diagnostics, strict = true) ?: return null
        return itemMatchers.convertCondition(matcher, path, diagnostics)
    }

    private fun rewritePlayerOperand(
        raw: String,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): String? {
        if (isCurrentPlayerOperand(raw)) return ""
        val value = unwrapVars(raw) ?: unquote(raw)
        return variables.rewrite(value, path, diagnostics, strict = true)?.trim()
    }

    private fun rewriteSimpleValue(
        raw: String,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): String? {
        var value = unwrapVars(raw) ?: raw.trim()
        val numericFunction = parseUtilityCall(value)
        if (numericFunction != null &&
            numericFunction.name.lowercase() in setOf("funint", "fundouble") &&
            numericFunction.arguments.size == 1
        ) {
            value = "{${unquote(numericFunction.arguments.single())}}"
        }
        val rewritten = variables.rewrite(unquote(value), path, diagnostics, strict = true)?.trim() ?: return null
        return rewritten.takeIf(SIMPLE_VALUE_PATTERN::matches)
    }

    private fun parseConditionCall(raw: String): ConditionalCall? {
        var source = raw.trim()
        source = when {
            source.startsWith("$") -> source.drop(1).trim()
            source.startsWith("js:", ignoreCase = true) -> source.drop(3).trim()
            else -> return null
        }
        val negated = source.startsWith("!")
        if (negated) source = source.drop(1).trim()
        val call = parseUtilityCall(source) ?: return null
        return ConditionalCall(call, negated)
    }

    private data class ConditionalCall(val call: UtilityCall, val negated: Boolean)

    companion object {
        private val SIMPLE_VALUE_PATTERN = Regex(
            "(?:-?\\d+(?:\\.\\d+)?|%[^%]+%|\\{(?:arg|meta|data|gdata):[^}]+})",
            RegexOption.IGNORE_CASE
        )
        private data class UtilityCall(val name: String, val arguments: List<String>)

        /** 转换 `source:JS:utils.getEquipment(...)` 为 KaMenu 装备槽位物品来源。 */
        fun convertEquipmentSource(raw: String): String? {
            val source = raw.trim()
            if (!source.startsWith("source:JS:", ignoreCase = true)) return null
            val call = parseUtilityCall(source.drop("source:JS:".length)) ?: return null
            if (!call.name.equals("utils.getEquipment", ignoreCase = true) || call.arguments.size != 2) {
                return null
            }
            val playerName = when {
                isCurrentPlayerOperand(call.arguments[0]) -> ""
                else -> unwrapVars(call.arguments[0]) ?: unquote(call.arguments[0])
            }
            val slot = normalizeEquipmentSlot(unquote(call.arguments[1])) ?: return null
            return if (playerName.isBlank()) "[$slot]" else "[$slot:$playerName]"
        }

        private fun parseUtilityCall(raw: String): UtilityCall? {
            val source = raw.trim()
            val open = source.indexOf('(')
            if (open <= 0 || !source.endsWith(')')) return null
            val name = source.substring(0, open).trim()
            if (!name.matches(Regex("[A-Za-z_][A-Za-z0-9_.]*"))) return null
            val arguments = splitArguments(source.substring(open + 1, source.length - 1)) ?: return null
            return UtilityCall(name, arguments)
        }

        private fun splitArguments(raw: String): List<String>? {
            if (raw.isBlank()) return emptyList()
            val result = mutableListOf<String>()
            val current = StringBuilder()
            var quote: Char? = null
            var escaping = false
            var depth = 0
            raw.forEach { char ->
                if (escaping) {
                    current.append(char)
                    escaping = false
                    return@forEach
                }
                if (char == '\\') {
                    current.append(char)
                    escaping = true
                    return@forEach
                }
                if (quote != null) {
                    current.append(char)
                    if (char == quote) quote = null
                    return@forEach
                }
                when (char) {
                    '\'', '"', '`' -> {
                        quote = char
                        current.append(char)
                    }
                    '(', '[', '{' -> {
                        depth++
                        current.append(char)
                    }
                    ')', ']', '}' -> {
                        if (depth <= 0) return null
                        depth--
                        current.append(char)
                    }
                    ',' -> if (depth == 0) {
                        result += current.toString().trim()
                        current.clear()
                    } else current.append(char)
                    else -> current.append(char)
                }
            }
            if (quote != null || depth != 0 || escaping) return null
            result += current.toString().trim()
            return result.takeIf { values -> values.none(String::isEmpty) }
        }

        private fun unwrapVars(raw: String): String? {
            val call = parseUtilityCall(raw) ?: return null
            if (!call.name.equals("vars", ignoreCase = true) || call.arguments.size != 1) return null
            return unquote(call.arguments.single())
        }

        private fun unquote(raw: String): String {
            val value = raw.trim()
            if (value.length >= 2 && value.first() == value.last() && value.first() in "'\"`") {
                return value.substring(1, value.length - 1)
            }
            return value
        }

        private fun isCurrentPlayerOperand(raw: String): Boolean =
            raw.trim().equals("player", ignoreCase = true)

        private fun normalizeEquipmentSlot(raw: String): String? = when (raw.trim().uppercase()) {
            "HEAD", "HELMET" -> "HEAD"
            "CHEST", "CHESTPLATE" -> "CHEST"
            "LEGS", "LEGGINGS" -> "LEGGINGS"
            "FEET", "BOOTS" -> "BOOTS"
            "MAINHAND", "MAIN_HAND", "HAND" -> "MAINHAND"
            "OFFHAND", "OFF_HAND" -> "OFFHAND"
            else -> null
        }
    }
}
