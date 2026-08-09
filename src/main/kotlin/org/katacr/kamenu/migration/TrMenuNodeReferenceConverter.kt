package org.katacr.kamenu.migration

import org.bukkit.configuration.file.YamlConfiguration
import java.util.LinkedHashMap

/**
 * 将 源菜单 `{node:*}` 引用迁移为 KaMenu `References.trmenu` 引用。
 *
 * 仅复制实际被引用的源节点；图标上下文中的 `@iconId@` 会先替换为源图标 ID。
 */
internal class TrMenuNodeReferenceConverter(
    private val root: TrMenuSourceSection
) {
    private val references = LinkedHashMap<String, Any?>()
    private val nodePattern = Regex("\\$?\\{(?:node|nodes|n):\\s*([^{}]+)}", RegexOption.IGNORE_CASE)
    private val nodePrefixPattern = Regex("\\$?\\{(?:node|nodes|n):", RegexOption.IGNORE_CASE)
    private val iconIdPattern = Regex("@iconId@", RegexOption.IGNORE_CASE)
    private val positionalArgumentPattern = Regex("\\{(\\d+)}")

    /** 将一段文本内的静态 node 引用全部改写；strict 失败时返回 null。 */
    fun rewrite(
        raw: String,
        sourceIconId: String?,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics,
        strict: Boolean
    ): String? {
        var failed = false
        var rewritten = nodePattern.replace(raw) { match ->
            convertNode(match.groupValues[1], sourceIconId, path, diagnostics)?.let { return@replace it }
            failed = true
            match.value
        }
        if (nodePrefixPattern.containsMatchIn(rewritten)) {
            diagnostics.add(
                code = "TRM_NODE_DYNAMIC_UNSUPPORTED",
                severity = TrMenuMigrationSeverity.WARNING,
                compatibility = TrMenuMigrationCompatibility.UNSUPPORTED,
                path = path,
                message = "Dynamic or nested TrMenu node path in '$raw' cannot be resolved during migration."
            )
            failed = true
        }
        if (failed && strict) return null
        return rewritten
    }

    /** 把已经收集的源节点写入目标菜单，并继续改写节点值中的变量和嵌套引用。 */
    fun writeReferences(
        output: YamlConfiguration,
        variables: TrMenuVariableConverter,
        diagnostics: TrMenuMigrationDiagnostics
    ) {
        val written = mutableSetOf<String>()
        while (true) {
            val entry = references.entries.firstOrNull { it.key !in written } ?: break
            written += entry.key
            val value = rewriteTemplateValue(
                entry.value,
                "References.trmenu.${entry.key}",
                variables,
                diagnostics
            )
            output.set("References.trmenu.${entry.key}", value)
        }
    }

    private fun convertNode(
        rawContent: String,
        sourceIconId: String?,
        diagnosticPath: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): String? {
        val content = rawContent.trim()
        val separator = content.indexOf('_')
        var sourcePath = (if (separator >= 0) content.substring(0, separator) else content).trim()
        val arguments = if (separator >= 0 && separator + 1 < content.length) {
            content.substring(separator + 1).split('_')
        } else {
            emptyList()
        }
        if (iconIdPattern.containsMatchIn(content)) {
            if (sourceIconId == null) {
                diagnostics.add(
                    code = "TRM_NODE_ICON_CONTEXT_MISSING",
                    severity = TrMenuMigrationSeverity.WARNING,
                    compatibility = TrMenuMigrationCompatibility.UNSUPPORTED,
                    path = diagnosticPath,
                    message = "TrMenu @iconId@ is used outside an icon context and cannot be resolved."
                )
                return null
            }
            sourcePath = sourcePath.replace(iconIdPattern, sourceIconId)
        }
        if (sourcePath.isEmpty() || sourcePath.any { it == '{' || it == '}' || it == '%' }) {
            diagnostics.add(
                code = "TRM_NODE_DYNAMIC_UNSUPPORTED",
                severity = TrMenuMigrationSeverity.WARNING,
                compatibility = TrMenuMigrationCompatibility.UNSUPPORTED,
                path = diagnosticPath,
                message = "Dynamic TrMenu node path '$sourcePath' cannot be resolved during migration."
            )
            return null
        }

        val located = root.find(sourcePath)
        if (located == null) {
            diagnostics.add(
                code = "TRM_NODE_PATH_MISSING",
                severity = TrMenuMigrationSeverity.WARNING,
                compatibility = TrMenuMigrationCompatibility.INVALID,
                path = diagnosticPath,
                message = "TrMenu node path '$sourcePath' does not exist in the source menu."
            )
            return null
        }
        if (!isTextValue(located.value)) {
            diagnostics.add(
                code = "TRM_NODE_STRUCTURE_UNSUPPORTED",
                severity = TrMenuMigrationSeverity.WARNING,
                compatibility = TrMenuMigrationCompatibility.UNSUPPORTED,
                path = diagnosticPath,
                message = "TrMenu node path '${located.path}' points to a nested structure that cannot be embedded in text."
            )
            return null
        }
        references.putIfAbsent(located.path, located.value ?: "")

        val targetPath = "trmenu.${located.path}"
        if (arguments.isEmpty()) return "{ref:$targetPath}"
        val encodedArguments = arguments.joinToString(";") { argument ->
            "`${argument.replace("\\", "\\\\").replace("`", "\\`")}`"
        }
        return "{ref:[$targetPath;$encodedArguments]}"
    }

    private fun isTextValue(value: Any?): Boolean = when (value) {
        null, is String, is Number, is Boolean -> true
        is List<*> -> value.all { it == null || it is String || it is Number || it is Boolean }
        else -> false
    }

    private fun rewriteTemplateValue(
        value: Any?,
        path: String,
        variables: TrMenuVariableConverter,
        diagnostics: TrMenuMigrationDiagnostics
    ): Any? = when (value) {
        is String -> variables.rewrite(
            value.replace(positionalArgumentPattern) { match -> "{refarg:${match.groupValues[1]}}" },
            path,
            diagnostics,
            strict = false
        )
        is List<*> -> value.mapIndexed { index, entry ->
            rewriteTemplateValue(entry, "$path[$index]", variables, diagnostics)
        }
        else -> value
    }
}
