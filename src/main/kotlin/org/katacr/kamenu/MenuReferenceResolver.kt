package org.katacr.kamenu

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration

/**
 * 解析菜单内的共享引用、当前组件引用和原始配置引用。
 *
 * 引用始终限制在调用方传入的菜单配置中，并通过引用栈阻止循环和过深展开。
 */
internal object MenuReferenceResolver {
    private const val MAX_DEPTH = 16
    private val referenceArgumentPattern = Regex("\\{refarg:(\\d+)}", RegexOption.IGNORE_CASE)

    private enum class Scope(val prefix: String) {
        REFERENCE("ref"),
        CONFIG("config"),
        SELF("self")
    }

    private data class ReferenceMatch(
        val start: Int,
        val endExclusive: Int,
        val scope: Scope,
        val content: String
    )

    private data class ReferenceCall(
        val path: String,
        val arguments: List<String>
    )

    /** 引用无法安全解析时抛出的可定位配置异常。 */
    class MenuReferenceException(message: String) : IllegalArgumentException(message)

    /**
     * 展开文本中的所有菜单引用。
     *
     * [replacement] 仅应用于最外层替换，条件解析可借此对最终值进行安全编码。
     */
    fun resolve(
        text: String,
        config: YamlConfiguration?,
        variables: Map<String, String> = emptyMap(),
        replacement: (value: String, sourceIndex: Int) -> String = { value, _ -> value }
    ): String {
        if (!containsReference(text)) return text
        val menuConfig = config ?: throw MenuReferenceException(
            "Menu reference requires a menu configuration: $text"
        )
        return resolveInternal(text, menuConfig, variables, emptyList(), replacement)
    }

    private fun resolveInternal(
        text: String,
        config: YamlConfiguration,
        variables: Map<String, String>,
        stack: List<String>,
        replacement: (String, Int) -> String
    ): String {
        val output = StringBuilder(text.length)
        var cursor = 0
        while (cursor < text.length) {
            val match = findReference(text, cursor)
            if (match == null) {
                output.append(text, cursor, text.length)
                break
            }
            output.append(text, cursor, match.start)
            val value = resolveReference(match, config, variables, stack)
            output.append(replacement(value, match.start))
            cursor = match.endExclusive
        }
        return output.toString()
    }

    private fun resolveReference(
        match: ReferenceMatch,
        config: YamlConfiguration,
        variables: Map<String, String>,
        stack: List<String>
    ): String {
        if (stack.size >= MAX_DEPTH) {
            throw MenuReferenceException("Menu reference depth exceeds $MAX_DEPTH: ${stack.joinToString(" -> ")}")
        }

        val call = parseCall(match.content)
        val requestedPath = replaceContextVariables(call.path, variables).trim()
        if (requestedPath.isEmpty()) {
            throw MenuReferenceException("Menu reference path cannot be empty at index ${match.start}.")
        }
        val fullPath = when (match.scope) {
            Scope.REFERENCE -> "References.$requestedPath"
            Scope.CONFIG -> requestedPath
            Scope.SELF -> {
                if (requestedPath.equals("id", ignoreCase = true) ||
                    requestedPath.equals("path", ignoreCase = true)
                ) {
                    return variables["self:${requestedPath.lowercase()}"]
                        ?: throw MenuReferenceException("{self:$requestedPath} is unavailable outside a menu component context.")
                }
                val selfPath = variables["self:path"]
                    ?: throw MenuReferenceException("{self:$requestedPath} is unavailable outside a menu component context.")
                "$selfPath.$requestedPath"
            }
        }
        val actualPath = findPath(config, fullPath)
            ?: throw MenuReferenceException("Menu reference path '$fullPath' does not exist.")
        val identity = actualPath.lowercase()
        if (identity in stack) {
            throw MenuReferenceException(
                "Circular menu reference detected: ${(stack + identity).joinToString(" -> ")}"
            )
        }

        val rawValue = config.get(actualPath)
        var value = stringify(rawValue, actualPath)
        val arguments = call.arguments.map { replaceContextVariables(it, variables) }
        value = value.replace(referenceArgumentPattern) { argument ->
            val index = argument.groupValues[1].toInt()
            arguments.getOrNull(index) ?: throw MenuReferenceException(
                "Menu reference '$actualPath' requires refarg $index, but only ${arguments.size} argument(s) were supplied."
            )
        }
        return resolveInternal(value, config, variables, stack + identity) { nested, _ -> nested }
    }

    /** 在不区分大小写的前提下查找 Bukkit 点路径。 */
    private fun findPath(config: YamlConfiguration, requestedPath: String): String? {
        return config.getKeys(true).firstOrNull { it.equals(requestedPath, ignoreCase = true) }
    }

    /** 将标量或列表转换为文本；Map/Section 必须使用后续结构引用能力处理。 */
    private fun stringify(value: Any?, path: String): String = when (value) {
        null -> ""
        is ConfigurationSection, is Map<*, *> -> throw MenuReferenceException(
            "Menu reference '$path' points to a section and cannot be embedded in text."
        )
        is Iterable<*> -> value.joinToString("\n") { entry -> stringifyListEntry(entry, path) }
        is Array<*> -> value.joinToString("\n") { entry -> stringifyListEntry(entry, path) }
        else -> value.toString()
    }

    private fun stringifyListEntry(value: Any?, path: String): String = when (value) {
        null -> ""
        is ConfigurationSection, is Map<*, *>, is Iterable<*>, is Array<*> -> throw MenuReferenceException(
            "Menu reference '$path' contains a nested structure and cannot be embedded in text."
        )
        else -> value.toString()
    }

    /** 支持 `{arg:*}` 等已注入上下文参与引用路径和引用参数。 */
    private fun replaceContextVariables(value: String, variables: Map<String, String>): String {
        var result = value
        variables.forEach { (key, replacement) ->
            result = result.replace("{$key}", replacement)
            result = result.replace("\$($key)", replacement)
        }
        return result
    }

    private fun parseCall(content: String): ReferenceCall {
        val trimmed = content.trim()
        if (!trimmed.startsWith('[') || !trimmed.endsWith(']')) {
            return ReferenceCall(trimmed, emptyList())
        }
        val parts = splitSemicolonArguments(trimmed.substring(1, trimmed.length - 1))
        return ReferenceCall(parts.firstOrNull().orEmpty(), parts.drop(1))
    }

    /** 使用分号拆分引用参数，并允许三种引号和反斜杠转义。 */
    private fun splitSemicolonArguments(raw: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var quote: Char? = null
        var escaping = false

        fun flush() {
            result += current.toString().trim()
            current.clear()
        }

        raw.forEach { character ->
            if (escaping) {
                current.append(character)
                escaping = false
            } else if (character == '\\') {
                escaping = true
            } else if (quote != null) {
                if (character == quote) quote = null else current.append(character)
            } else {
                when (character) {
                    '\'', '"', '`' -> quote = character
                    ';' -> flush()
                    else -> current.append(character)
                }
            }
        }
        if (escaping) current.append('\\')
        flush()
        return result
    }

    private fun containsReference(text: String): Boolean {
        return Scope.entries.any { text.contains("{${it.prefix}:", ignoreCase = true) }
    }

    /** 从指定位置寻找下一个引用，并使用括号平衡避免参数中的变量提前截断。 */
    private fun findReference(text: String, fromIndex: Int): ReferenceMatch? {
        var index = fromIndex
        while (index < text.length) {
            if (text[index] != '{') {
                index++
                continue
            }
            val scope = Scope.entries.firstOrNull { candidate ->
                text.regionMatches(index + 1, "${candidate.prefix}:", 0, candidate.prefix.length + 1, ignoreCase = true)
            }
            if (scope == null) {
                index++
                continue
            }
            val contentStart = index + scope.prefix.length + 2
            var cursor = contentStart
            var depth = 1
            var quote: Char? = null
            var escaping = false
            while (cursor < text.length) {
                val character = text[cursor]
                if (escaping) {
                    escaping = false
                } else if (character == '\\') {
                    escaping = true
                } else if (quote != null) {
                    if (character == quote) quote = null
                } else {
                    when (character) {
                        '\'', '"', '`' -> quote = character
                        '{' -> depth++
                        '}' -> {
                            depth--
                            if (depth == 0) {
                                return ReferenceMatch(
                                    start = index,
                                    endExclusive = cursor + 1,
                                    scope = scope,
                                    content = text.substring(contentStart, cursor)
                                )
                            }
                        }
                    }
                }
                cursor++
            }
            throw MenuReferenceException("Unclosed menu reference starting at index $index: $text")
        }
        return null
    }
}
