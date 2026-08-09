package org.katacr.kamenu

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player

/**
 * 解析字符串末尾的单行条件修饰符，并通过 KaMenu 条件引擎决定是否保留该字符串。
 *
 * KaMenu 仅识别位于行尾的 `{condition: 表达式}` 标准写法。
 */
internal object InlineConditionResolver {
    /** 已从原字符串中分离出的正文和条件表达式。 */
    data class Parsed(val content: String, val condition: String)

    /**
     * 提取行尾条件修饰符；普通文本、未闭合修饰符或空条件返回 null。
     *
     * 从行尾反向匹配成对花括号，可保留条件中的 `{meta:*}`、`{ref:*}`
     * 和 `<` / `>` 比较符。
     */
    fun parse(source: String): Parsed? {
        val parsed = parseTrailingModifier(source, "condition") ?: return null
        return Parsed(parsed.first, parsed.second)
    }

    /** 提取指定名称的行尾花括号修饰符，并保留修饰符值中的嵌套花括号。 */
    internal fun parseTrailingModifier(source: String, name: String): Pair<String, String>? {
        val value = source.trimEnd()
        if (value.lastOrNull() != '}') return null
        var depth = 0
        var start = -1
        for (index in value.lastIndex downTo 0) {
            when (value[index]) {
                '}' -> depth++
                '{' -> {
                    depth--
                    if (depth == 0) {
                        start = index
                        break
                    }
                    if (depth < 0) return null
                }
            }
        }
        if (start < 0 || depth != 0) return null

        val body = value.substring(start + 1, value.lastIndex)
        val separator = body.indexOf(':')
        if (separator < 0 || !body.substring(0, separator).trim().equals(name, ignoreCase = true)) return null
        val modifierValue = body.substring(separator + 1).trim()
        if (modifierValue.isEmpty()) return null
        return value.substring(0, start).trimEnd() to modifierValue
    }

    /** 条件通过时返回移除修饰符后的正文，不通过时返回 null。 */
    fun resolve(
        player: Player,
        source: String,
        variables: Map<String, String> = emptyMap(),
        menuConfig: YamlConfiguration? = null,
        dynamicResolver: (String) -> String? = { null }
    ): String? {
        val parsed = parse(source) ?: return source
        return parsed.content.takeIf {
            ConditionExpressionEngine.checkCondition(
                player,
                parsed.condition,
                variables,
                menuConfig,
                dynamicResolver
            )
        }
    }
}
