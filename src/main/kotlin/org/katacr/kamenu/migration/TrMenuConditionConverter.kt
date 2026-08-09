package org.katacr.kamenu.migration

/**
 * 将受限 源菜单 Kether 条件转换为 KaMenu 条件表达式。
 *
 * 解析器只接受迁移蓝图声明的纯判断子集；任何未完全消费或无法识别的语法都会安全失败，
 * 不会删除条件后放行原动作。
 */
internal class TrMenuConditionConverter(
    private val variableConverter: TrMenuVariableConverter? = null
) {
    private data class Token(val text: String, val quoted: Boolean = false)

    private class Unsupported(message: String) : IllegalArgumentException(message)
    private val itemMatcherConverter = variableConverter?.let(::TrMenuItemMatcherConverter)
    private val privateUtilityConverter = variableConverter?.let(::TrMenuPrivateUtilityConverter)

    /** 转换一条条件；不支持时记录诊断并返回 null。 */
    fun convert(
        raw: String?,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): String? {
        val original = raw?.trim().orEmpty()
        privateUtilityConverter?.convertCondition(original, path, diagnostics)?.let { return it }
        val source = variableConverter
            ?.rewrite(original, path, diagnostics, strict = true)
            ?.trim()
            ?: if (variableConverter == null) original else return null
        if (source.isEmpty()) return null
        return try {
            val parser = Parser(tokenize(source), path, diagnostics)
            parser.parseComplete()
        } catch (error: Unsupported) {
            diagnostics.add(
                code = "TRM_CONDITION_UNSUPPORTED",
                severity = TrMenuMigrationSeverity.WARNING,
                compatibility = TrMenuMigrationCompatibility.UNSUPPORTED,
                path = path,
                message = error.message ?: "Unsupported TrMenu condition '$source'."
            )
            null
        }
    }

    private inner class Parser(
        private val tokens: List<Token>,
        private val path: String,
        private val diagnostics: TrMenuMigrationDiagnostics
    ) {
        private var index = 0

        fun parseComplete(): String {
            val expression = parseExpression()
            if (index != tokens.size) unsupported("Unexpected token '${peek()?.text}'.")
            return expression
        }

        private fun parseExpression(): String {
            val keyword = peek()?.text?.lowercase() ?: unsupported("Unexpected end of condition.")
            return when (keyword) {
                "any" -> parseGroup("||")
                "all" -> parseGroup("&&")
                "not" -> {
                    take()
                    "!(${parseExpression()})"
                }
                "check" -> parseCheck()
                "perm", "permission" -> {
                    take()
                    "hasPerm.${parseOperand()}"
                }
                "mtc" -> parseTypeCheck()
                "money", "eco", "economy" -> {
                    take()
                    "hasMoney.${parseOperand()}"
                }
                "points", "point" -> {
                    take()
                    "hasPoints.${parseOperand()}"
                }
                "item" -> {
                    take()
                    val matcher = parseOperand()
                    itemMatcherConverter?.convertCondition(matcher, path, diagnostics)
                        ?: unsupported("TrMenu item condition cannot be converted safely.")
                }
                "true" -> take().text.lowercase()
                "false" -> take().text.lowercase()
                else -> unsupported("Unsupported TrMenu condition keyword '${peek()?.text}'.")
            }
        }

        private fun parseGroup(operator: String): String {
            take()
            expect("[")
            val children = mutableListOf<String>()
            while (peek()?.text != "]") {
                if (peek() == null) unsupported("Unclosed condition group.")
                children += parseExpression()
            }
            take()
            if (children.isEmpty()) unsupported("Condition group cannot be empty.")
            return children.joinToString(" $operator ", "(", ")")
        }

        private fun parseCheck(): String {
            take()
            val left = parseOperand()
            val firstOperator = take().text.lowercase()
            val (operator, approximate) = when (firstOperator) {
                ">", ">=", "<", "<=" -> firstOperator to false
                "=?", "is" -> {
                    if (firstOperator == "is" && peek()?.text.equals("not", true)) {
                        take()
                        "!=" to false
                    } else {
                        "==" to false
                    }
                }
                "==", "!=" -> firstOperator to true
                "has", "in" -> unsupported("TrMenu collection operator '$firstOperator' has no safe KaMenu mapping.")
                else -> unsupported("Unsupported check operator '$firstOperator'.")
            }
            val right = parseOperand()
            if (approximate) {
                diagnostics.add(
                    code = "TRM_CONDITION_CASE_APPROXIMATE",
                    severity = TrMenuMigrationSeverity.WARNING,
                    compatibility = TrMenuMigrationCompatibility.APPROXIMATE,
                    path = path,
                    message = "Operator '$firstOperator' becomes KaMenu's case-insensitive '$operator' comparison."
                )
            }
            return "$left $operator $right"
        }

        private fun parseTypeCheck(): String {
            take()
            val type = take().text.lowercase()
            val predicate = when (type) {
                "int", "integer" -> "isInt"
                "double", "number", "num" -> "isNum"
                else -> unsupported("Unsupported mtc type '$type'.")
            }
            return "$predicate.${parseOperand()}"
        }

        private fun parseOperand(): String {
            val token = take()
            val lower = token.text.lowercase()
            if (!token.quoted && lower in setOf("vars", "var", "papi", "placeholder")) {
                val value = parseOperand()
                if (value.contains("\${") || value.contains("{ke:", true) || value.contains("{node:", true)) {
                    unsupported("TrMenu '$lower' operand contains a private function or script expression.")
                }
                return value
            }
            if (!token.quoted && token.text.startsWith("&") && token.text.length > 1) {
                return "{meta:${token.text.substring(1)}}"
            }
            if (!token.quoted) {
                normalizeVariable(token.text)?.let { return it }
            }
            val literal = token.text.removePrefix("*")
            if (literal.isEmpty()) unsupported("Condition operand cannot be empty.")
            return quoteIfNeeded(literal, token.quoted)
        }

        private fun normalizeVariable(value: String): String? {
            Regex("^\\{(meta|data|gdata):\\s*(.+?)\\}$", RegexOption.IGNORE_CASE)
                .matchEntire(value)
                ?.let { match ->
                    return "{${match.groupValues[1].lowercase()}:${match.groupValues[2].trim()}}"
                }
            Regex("^\\{(\\d+)}$").matchEntire(value)?.let { match ->
                return "{arg:${match.groupValues[1]}}"
            }
            if (value.startsWith("%") && value.endsWith("%") && value.length > 2) return value
            return null
        }

        private fun quoteIfNeeded(value: String, force: Boolean): String {
            if (!force && value.none { it.isWhitespace() } && value.none { it in "()&|!=<>\"'" }) return value
            return "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        }

        private fun expect(text: String) {
            val actual = take().text
            if (actual != text) unsupported("Expected '$text' but found '$actual'.")
        }

        private fun peek(): Token? = tokens.getOrNull(index)

        private fun take(): Token = tokens.getOrNull(index++) ?: unsupported("Unexpected end of condition.")

        private fun unsupported(message: String): Nothing = throw Unsupported(message)
    }

    private fun tokenize(input: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var index = 0
        while (index < input.length) {
            if (input[index].isWhitespace()) {
                index++
                continue
            }
            if (input[index] == '[' || input[index] == ']') {
                tokens += Token(input[index].toString())
                index++
                continue
            }
            if (input[index] == '\'' || input[index] == '"' || input[index] == '`') {
                val quote = input[index++]
                val value = StringBuilder()
                var closed = false
                while (index < input.length) {
                    val char = input[index++]
                    if (char == '\\' && index < input.length) {
                        value.append(input[index++])
                    } else if (char == quote) {
                        closed = true
                        break
                    } else {
                        value.append(char)
                    }
                }
                if (!closed) throw Unsupported("Unclosed quoted condition value.")
                tokens += Token(value.toString(), quoted = true)
                continue
            }
            if (input[index] == '{' || (input[index] == '$' && input.getOrNull(index + 1) == '{')) {
                val start = index
                if (input[index] == '$') index++
                var depth = 0
                while (index < input.length) {
                    when (input[index]) {
                        '{' -> depth++
                        '}' -> {
                            depth--
                            if (depth == 0) {
                                index++
                                break
                            }
                        }
                    }
                    index++
                }
                if (depth != 0) throw Unsupported("Unclosed variable expression.")
                tokens += Token(input.substring(start, index))
                continue
            }
            val two = input.substring(index, minOf(index + 2, input.length))
            if (two in setOf(">=", "<=", "==", "!=", "=?")) {
                tokens += Token(two)
                index += 2
                continue
            }
            if (input[index] == '>' || input[index] == '<') {
                tokens += Token(input[index].toString())
                index++
                continue
            }
            val start = index
            while (index < input.length && !input[index].isWhitespace() && input[index] !in "[]<>") {
                if (input[index] == '=' || input[index] == '!') break
                index++
            }
            if (index == start) {
                tokens += Token(input[index].toString())
                index++
            } else {
                tokens += Token(input.substring(start, index))
            }
        }
        return tokens
    }
}
