package org.katacr.kamenu.migration

/** 统一改写 TrMenu 参数、数据变量和已迁移的菜单 Function 调用。 */
internal class TrMenuVariableConverter(
    private val functions: TrMenuFunctionRegistry? = null
) {
    /** strict 模式遇到私有表达式或未知 Function 时返回 null。 */
    fun rewrite(
        raw: String,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics,
        strict: Boolean
    ): String? {
        var rewritten = raw
            .replace(Regex("\\{(\\d+)}")) { match -> "{arg:${match.groupValues[1]}" + "}" }
            .replace(Regex("\\{(m|meta):\\s*(.+?)}", RegexOption.IGNORE_CASE)) { match ->
                "{meta:${match.groupValues[2].trim()}" + "}"
            }
            .replace(Regex("\\{(d|data):\\s*(.+?)}", RegexOption.IGNORE_CASE)) { match ->
                "{data:${match.groupValues[2].trim()}" + "}"
            }
            .replace(Regex("\\{(g|gdata|globaldata):\\s*(.+?)}", RegexOption.IGNORE_CASE)) { match ->
                "{gdata:${match.groupValues[2].trim()}" + "}"
            }

        if (PRIVATE_EXPRESSION.containsMatchIn(rewritten)) {
            diagnostics.add(
                code = "TRM_VARIABLE_UNSUPPORTED",
                severity = TrMenuMigrationSeverity.WARNING,
                compatibility = TrMenuMigrationCompatibility.UNSUPPORTED,
                path = path,
                message = "TrMenu private script expression in '$raw' cannot be migrated safely."
            )
            if (strict) return null
        }

        if (rewritten.contains("\${")) {
            val registry = functions
            if (registry == null) {
                diagnostics.add(
                    code = "TRM_FUNCTION_REFERENCE_UNSUPPORTED",
                    severity = TrMenuMigrationSeverity.WARNING,
                    compatibility = TrMenuMigrationCompatibility.UNSUPPORTED,
                    path = path,
                    message = "TrMenu Function reference in '$raw' has no migrated Functions registry."
                )
                if (strict) return null
            } else {
                rewritten = registry.rewriteText(rewritten, path, diagnostics, strict) ?: return null
            }
        }
        return rewritten
    }

    /** 递归改写标题、默认参数或物品显示等结构化值。 */
    fun rewriteValue(
        raw: Any?,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics,
        strict: Boolean = false
    ): Any? = when (raw) {
        null -> null
        is String -> rewrite(raw, path, diagnostics, strict)
        is List<*> -> raw.mapIndexedNotNull { index, value ->
            rewriteValue(value, "$path[$index]", diagnostics, strict)
        }
        is Map<*, *> -> raw.entries.associateTo(linkedMapOf()) { (key, value) ->
            key.toString() to rewriteValue(value, "$path.${key.toString()}", diagnostics, strict)
        }
        else -> raw
    }

    companion object {
        private val PRIVATE_EXPRESSION = Regex(
            "(?:\\$?\\{(?:ke|kether|node|jexl|nova|novalang|novascript|javascript|js):)",
            RegexOption.IGNORE_CASE
        )
    }
}
