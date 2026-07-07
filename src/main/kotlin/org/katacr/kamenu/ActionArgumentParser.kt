package org.katacr.kamenu

/**
 * actions/js 包调用参数解析器。
 *
 * 支持逗号或空白分隔参数，并允许使用单引号、双引号或反引号包裹包含空格的参数。
 * 包调用专用语法为 `[package/id],arg1,arg2`；非方括号内容不会被当作包调用解析。
 */
object ActionArgumentParser {
    /**
     * 解析后的调用信息。
     *
     * name 是动作包或 JS 包 ID，arguments 会按顺序暴露为 `{arg:0}` 或 JS `args[0]`。
     */
    data class Call(
        val name: String,
        val arguments: List<String>
    )

    private val packageNamePattern = Regex("[A-Za-z0-9_./-]+")

    /**
     * 解析 actions 动作包调用。
     *
     * 示例：`hello,Steve,survival` -> name=`hello`, args=`Steve`,`survival`。
     */
    fun parseCall(raw: String): Call {
        val parts = splitArguments(raw)
        val name = parts.firstOrNull().orEmpty()
        return Call(name, parts.drop(1))
    }

    /**
     * 解析方括号包调用。
     *
     * 仅当文本以 `[name]` 开头且 name 符合包 ID 规则时返回结果；
     * 用于区分 JS 内联表达式和全局 JS 包调用。
     */
    fun parseBracketCall(raw: String): Call? {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("[")) {
            return null
        }

        val closeBracketIndex = trimmed.indexOf(']')
        if (closeBracketIndex <= 1) {
            return null
        }

        val name = trimmed.substring(1, closeBracketIndex).trim()
        if (!packageNamePattern.matches(name)) {
            return null
        }

        val tail = trimmed.substring(closeBracketIndex + 1)
        if (tail.isNotEmpty() && !tail.first().isWhitespace() && tail.first() != ',') {
            return null
        }

        return Call(name, splitArguments(tail))
    }

    /**
     * 拆分调用参数。
     *
     * 分隔符为英文逗号或任意空白；引号内的分隔符会保留。
     */
    fun splitArguments(raw: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var quote: Char? = null
        var escaping = false

        fun flush() {
            val value = stripQuotes(current.toString().trim())
            if (value.isNotEmpty()) {
                result.add(value)
            }
            current.clear()
        }

        for (ch in raw) {
            if (escaping) {
                current.append(ch)
                escaping = false
                continue
            }

            if (ch == '\\') {
                escaping = true
                continue
            }

            if (quote != null) {
                if (ch == quote) {
                    quote = null
                } else {
                    current.append(ch)
                }
                continue
            }

            when {
                ch == '\'' || ch == '"' || ch == '`' -> quote = ch
                ch == ',' || ch.isWhitespace() -> flush()
                else -> current.append(ch)
            }
        }

        if (escaping) {
            current.append('\\')
        }

        flush()
        return result
    }

    private fun stripQuotes(value: String): String {
        return value.removeSurrounding("`").removeSurrounding("'").removeSurrounding("\"")
    }
}
