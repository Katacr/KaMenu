package org.katacr.kamenu

object ActionArgumentParser {
    data class Call(
        val name: String,
        val arguments: List<String>
    )

    private val packageNamePattern = Regex("[A-Za-z0-9_./-]+")

    fun parseCall(raw: String): Call {
        val parts = splitArguments(raw)
        val name = parts.firstOrNull().orEmpty()
        return Call(name, parts.drop(1))
    }

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
