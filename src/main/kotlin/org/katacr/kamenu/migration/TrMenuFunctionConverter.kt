package org.katacr.kamenu.migration

import org.katacr.kamenu.ActionArgumentParser
import java.util.LinkedHashMap

/** 已迁移的菜单 Functions，以及供动作守卫调用的生成脚本。 */
internal class TrMenuFunctionRegistry private constructor(
    private val scriptsById: LinkedHashMap<String, String>,
    private val targetIdsBySource: Map<String, String>,
    private val syntaxValidator: (String) -> String?
) {
    private val reportedVariableIssues = mutableSetOf<String>()
    private val guardIdsByFunction = mutableMapOf<String, String>()

    /** 返回可按 `JavaScript.<id>` 写入目标菜单的扁平脚本表。 */
    fun scripts(): Map<String, String> = LinkedHashMap(scriptsById)

    /**
     * 将文本中的 TrMenu `${function_arg}` 调用改写为 KaMenu 菜单 JavaScript 调用。
     *
     * strict 为 true 时，未知或不安全函数会让整个值转换失败；展示文本可使用 false 保留原文并报警。
     */
    fun rewriteText(
        raw: String,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics,
        strict: Boolean = false
    ): String? {
        var failed = false
        val rewritten = INTERNAL_FUNCTION_PATTERN.replace(raw) { match ->
            val payload = match.groupValues[1]
            val parts = payload.split('_')
            val sourceId = parts.firstOrNull().orEmpty()
            val targetId = targetIdsBySource[sourceId]
            if (targetId == null) {
                failed = true
                reportVariableIssue(
                    "TRM_FUNCTION_REFERENCE_UNSUPPORTED",
                    path,
                    "TrMenu function '$sourceId' was not migrated; reference '${match.value}' was kept.",
                    diagnostics
                )
                match.value
            } else {
                buildJavaScriptVariable(targetId, parts.drop(1))
            }
        }
        return if (strict && failed) null else rewritten
    }

    /** 将 `function:/run:` 调用转换为一次求值、假值时中断动作链的条件节点。 */
    fun convertGuard(
        raw: String,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): Map<String, Any>? {
        val call = ActionArgumentParser.parseCall(raw)
        val targetId = targetIdsBySource[call.name]
        if (targetId == null) {
            reportVariableIssue(
                "TRM_FUNCTION_REFERENCE_UNSUPPORTED",
                path,
                "TrMenu function '${call.name}' was not migrated and cannot be used as an action guard.",
                diagnostics
            )
            return null
        }

        val existingGuardId = guardIdsByFunction[targetId]
        val guardId = if (existingGuardId != null) {
            existingGuardId
        } else {
            val generatedId = uniqueGuardId(targetId)
            val targetScript = scriptsById.getValue(targetId)
            val guardScript = buildGuardScript(targetScript)
            val syntaxError = syntaxValidator(guardScript)
            if (syntaxError != null) {
                diagnostics.add(
                    code = "TRM_FUNCTION_SYNTAX_UNSUPPORTED",
                    severity = TrMenuMigrationSeverity.WARNING,
                    compatibility = TrMenuMigrationCompatibility.UNSUPPORTED,
                    path = path,
                    message = "Generated guard for TrMenu function '$call.name' failed JavaScript validation: $syntaxError"
                )
                return null
            }
            scriptsById[generatedId] = guardScript
            guardIdsByFunction[targetId] = generatedId
            generatedId
        }

        return linkedMapOf(
            "condition" to "${buildJavaScriptVariable(guardId, call.arguments)} == true",
            "allow" to emptyList<Any>(),
            "deny" to listOf("return")
        )
    }

    private fun uniqueGuardId(targetId: String): String {
        val base = "__trmenu_guard.${targetId.replace(Regex("[^A-Za-z0-9_.-]"), "_")}"
        if (!scriptsById.containsKey(base)) return base
        var suffix = 2
        while (scriptsById.containsKey("${base}_$suffix")) suffix++
        return "${base}_$suffix"
    }

    private fun buildGuardScript(targetScript: String): String {
        val escaped = targetScript
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
        return """
            var __trmenu_result = eval("$escaped");
            var __trmenu_value = String(__trmenu_result).toLowerCase();
            __trmenu_value !== "false" && __trmenu_value !== "no" && __trmenu_value !== "off";
        """.trimIndent()
    }

    private fun buildJavaScriptVariable(targetId: String, args: List<String>): String {
        val encodedArgs = args.joinToString(",", prefix = if (args.isEmpty()) "" else ",") { argument ->
            "`${argument.replace("`", "\\`")}`"
        }
        return "{js:[$targetId]$encodedArgs}"
    }

    private fun reportVariableIssue(
        code: String,
        path: String,
        message: String,
        diagnostics: TrMenuMigrationDiagnostics
    ) {
        if (!reportedVariableIssues.add("$code|$path|$message")) return
        diagnostics.add(
            code = code,
            severity = TrMenuMigrationSeverity.WARNING,
            compatibility = TrMenuMigrationCompatibility.UNSUPPORTED,
            path = path,
            message = message
        )
    }

    companion object {
        private val INTERNAL_FUNCTION_PATTERN = Regex("\\$\\{([^{}]+)}")

        /** 读取菜单 Functions 节点，过滤私有绑定并生成可调用注册表。 */
        fun create(
            root: TrMenuSourceSection,
            diagnostics: TrMenuMigrationDiagnostics,
            syntaxValidator: (String) -> String? = { null }
        ): TrMenuFunctionRegistry {
            val source = root.section(TrMenuSourceProperty.FUNCTIONS, "Functions", diagnostics)
                ?: return TrMenuFunctionRegistry(linkedMapOf(), emptyMap(), syntaxValidator)
            val flattened = linkedMapOf<String, String>()
            flattenFunctions(source, "", flattened, diagnostics)

            val targetIds = linkedMapOf<String, String>()
            val reservedTargetIds = linkedSetOf<String>()
            flattened.keys.forEach { sourceId ->
                val targetId = uniqueTargetId(sourceId, reservedTargetIds)
                targetIds[sourceId] = targetId
                reservedTargetIds += targetId
            }
            val scripts = linkedMapOf<String, String>()
            flattened.forEach { (sourceId, rawScript) ->
                val targetId = targetIds.getValue(sourceId)
                val converted = convertScript(rawScript, sourceId, targetId, targetIds, diagnostics)
                    ?: return@forEach
                val syntaxError = syntaxValidator(converted)
                if (syntaxError != null) {
                    diagnostics.add(
                        code = "TRM_FUNCTION_SYNTAX_UNSUPPORTED",
                        severity = TrMenuMigrationSeverity.WARNING,
                        compatibility = TrMenuMigrationCompatibility.UNSUPPORTED,
                        path = "Functions.$sourceId",
                        message = "TrMenu function '$sourceId' failed KaMenu JavaScript validation: $syntaxError"
                    )
                    return@forEach
                }
                scripts[targetId] = converted
            }
            removeBrokenDependencies(scripts, targetIds, diagnostics)
            val migratedIds = targetIds.filterValues(scripts::containsKey)
            return TrMenuFunctionRegistry(scripts, migratedIds, syntaxValidator)
        }

        private fun removeBrokenDependencies(
            scripts: LinkedHashMap<String, String>,
            targetIds: Map<String, String>,
            diagnostics: TrMenuMigrationDiagnostics
        ) {
            val sourceIdsByTarget = targetIds.entries.associate { it.value to it.key }
            var changed: Boolean
            do {
                changed = false
                scripts.entries.toList().forEach { (targetId, script) ->
                    val missing = GENERATED_CALL_PATTERN.findAll(script)
                        .map { it.groupValues[1] }
                        .firstOrNull { it !in scripts }
                        ?: return@forEach
                    scripts.remove(targetId)
                    changed = true
                    val sourceId = sourceIdsByTarget[targetId] ?: targetId
                    diagnostics.add(
                        code = "TRM_FUNCTION_REFERENCE_UNSUPPORTED",
                        severity = TrMenuMigrationSeverity.WARNING,
                        compatibility = TrMenuMigrationCompatibility.UNSUPPORTED,
                        path = "Functions.$sourceId",
                        message = "TrMenu function '$sourceId' depends on skipped function '$missing' and was skipped."
                    )
                }
            } while (changed)
        }

        private fun flattenFunctions(
            section: TrMenuSourceSection,
            prefix: String,
            output: LinkedHashMap<String, String>,
            diagnostics: TrMenuMigrationDiagnostics
        ) {
            section.entries().forEach { (key, value) ->
                val id = if (prefix.isEmpty()) key else "$prefix.$key"
                when (value) {
                    is TrMenuSourceSection -> flattenFunctions(value, id, output, diagnostics)
                    null -> Unit
                    is String, is Number, is Boolean -> output[id] = value.toString()
                    else -> diagnostics.add(
                        code = "TRM_FUNCTION_SOURCE_INVALID",
                        severity = TrMenuMigrationSeverity.WARNING,
                        compatibility = TrMenuMigrationCompatibility.INVALID,
                        path = "Functions.$id",
                        message = "TrMenu Function '$id' must be a scalar JavaScript value and was skipped."
                    )
                }
            }
        }

        private fun uniqueTargetId(sourceId: String, existing: Set<String>): String {
            val sanitized = sourceId
                .split('.')
                .joinToString(".") { segment ->
                    segment.replace(Regex("[^A-Za-z0-9_/-]"), "_").ifBlank { "function" }
                }
            if (sanitized !in existing) return sanitized
            var suffix = 2
            while ("${sanitized}_$suffix" in existing) suffix++
            return "${sanitized}_$suffix"
        }

        private fun convertScript(
            raw: String,
            sourceId: String,
            targetId: String,
            knownIds: Map<String, String>,
            diagnostics: TrMenuMigrationDiagnostics
        ): String? {
            val normalized = if (raw.contains("function")) {
                raw
            } else {
                "function def() { ${if (raw.contains("return")) "" else "return "}$raw }\ndef()"
            }
            val rewrittenVariables = INTERNAL_FUNCTION_PATTERN.replace(normalized) { match ->
                val parts = match.groupValues[1].split('_')
                val nestedTarget = knownIds[parts.firstOrNull().orEmpty()]
                if (nestedTarget == null) match.value else {
                    val args = parts.drop(1).joinToString(",", prefix = if (parts.size <= 1) "" else ",") {
                        "`${it.replace("`", "\\`")}`"
                    }
                    "{js:[$nestedTarget]$args}"
                }
            }
            val rewritten = rewrittenVariables
                .replace(Regex("\\bsender\\b"), "player")
                .replace(Regex("\\bbukkitServer\\b"), "server")
                .replace(Regex("\\bmetas\\s*\\("), "meta(")
                .replace(Regex("\\bdatas\\s*\\("), "data(")
                .replace(Regex("\\bgdatas\\s*\\("), "gdata(")

            val codeOnly = stripStringsAndComments(rewritten)
            val privateBinding = PRIVATE_BINDINGS.firstOrNull { binding ->
                Regex("\\b${Regex.escape(binding)}\\b").containsMatchIn(codeOnly)
            }
            if (privateBinding != null || Regex("\\bJava\\s*\\.\\s*type\\s*\\(").containsMatchIn(codeOnly)) {
                diagnostics.add(
                    code = "TRM_FUNCTION_UNSUPPORTED_BINDING",
                    severity = TrMenuMigrationSeverity.WARNING,
                    compatibility = TrMenuMigrationCompatibility.UNSUPPORTED,
                    path = "Functions.$sourceId",
                    message = "TrMenu function '$sourceId' uses private or unsafe binding '${privateBinding ?: "Java.type"}' and was skipped."
                )
                return null
            }
            if (rewritten.contains(INTERNAL_FUNCTION_PATTERN)) {
                diagnostics.add(
                    code = "TRM_FUNCTION_REFERENCE_UNSUPPORTED",
                    severity = TrMenuMigrationSeverity.WARNING,
                    compatibility = TrMenuMigrationCompatibility.UNSUPPORTED,
                    path = "Functions.$sourceId",
                    message = "TrMenu function '$sourceId' references a Function that was not migrated before '$targetId'."
                )
                return null
            }
            return rewritten
        }

        private fun stripStringsAndComments(script: String): String {
            val result = StringBuilder(script.length)
            var index = 0
            var quote: Char? = null
            var lineComment = false
            var blockComment = false
            while (index < script.length) {
                val char = script[index]
                val next = script.getOrNull(index + 1)
                when {
                    lineComment -> {
                        if (char == '\n') {
                            lineComment = false
                            result.append('\n')
                        } else result.append(' ')
                    }
                    blockComment -> {
                        if (char == '*' && next == '/') {
                            result.append("  ")
                            index++
                            blockComment = false
                        } else result.append(if (char == '\n') '\n' else ' ')
                    }
                    quote != null -> {
                        if (char == '\\') {
                            result.append("  ")
                            index++
                        } else {
                            if (char == quote) quote = null
                            result.append(if (char == '\n') '\n' else ' ')
                        }
                    }
                    char == '/' && next == '/' -> {
                        result.append("  ")
                        index++
                        lineComment = true
                    }
                    char == '/' && next == '*' -> {
                        result.append("  ")
                        index++
                        blockComment = true
                    }
                    char == '\'' || char == '"' || char == '`' -> {
                        quote = char
                        result.append(' ')
                    }
                    else -> result.append(char)
                }
                index++
            }
            return result.toString()
        }

        private val PRIVATE_BINDINGS = listOf(
            "session",
            "config",
            "utils",
            "funs",
            "funcs",
            "kes",
            "nodes",
            "keInt",
            "keDouble",
            "nodeInt",
            "nodeDouble"
        )
        private val GENERATED_CALL_PATTERN = Regex("\\{js:\\[([^]]+)]")
    }
}
