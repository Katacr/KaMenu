package org.katacr.kamenu

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * 条件表达式求值引擎
 * 使用 AST 解析表达式，保留现有条件语法。
 */
object ConditionExpressionEngine {
    private var plugin: KaMenu? = null

    private val builtinPredicates = mutableMapOf<String, (Player, String) -> Boolean>()

    private sealed interface Expr
    private data class Literal(val value: Boolean) : Expr
    private data class Compare(val left: String, val op: String, val right: String) : Expr
    private data class BuiltinCall(val negated: Boolean, val name: String, val arg: String) : Expr
    private data class Not(val expr: Expr) : Expr
    private data class And(val left: Expr, val right: Expr) : Expr
    private data class Or(val left: Expr, val right: Expr) : Expr

    private enum class TokenType {
        IDENT, STRING, NUMBER, TRUE, FALSE,
        DOT, LPAREN, RPAREN, AND, OR, EQ, NE, GT, GE, LT, LE, BANG,
        EOF
    }

    private data class Token(val type: TokenType, val text: String)

    init {
        registerBuiltinPredicate("isNum") { _, value -> value.toDoubleOrNull() != null }
        registerBuiltinPredicate("isPosNum") { _, value -> value.toDoubleOrNull()?.let { it > 0 } ?: false }
        registerBuiltinPredicate("isInt") { _, value -> value.toIntOrNull() != null }
        registerBuiltinPredicate("isPosInt") { _, value -> value.toIntOrNull()?.let { it > 0 } ?: false }
        registerBuiltinPredicate("hasPerm") { player, value -> player.hasPermission(value) }
        registerBuiltinPredicate("hasMoney") { player, value ->
            val amount = value.toDoubleOrNull() ?: return@registerBuiltinPredicate false
            checkPlayerMoney(player, amount)
        }
        registerBuiltinPredicate("hasStockItem") { player, value ->
            val params = value.split(";", limit = 2)
            if (params.size != 2) false else {
                val itemName = params[0].trim()
                val requiredAmount = params[1].trim().toIntOrNull() ?: 1
                getPlayerStockItemCount(player, itemName) >= requiredAmount
            }
        }
        registerBuiltinPredicate("hasItem") { player, value ->
            if (!value.startsWith("[") || !value.endsWith("]")) false
            else checkPlayerHasItem(player, value.substring(1, value.length - 1).trim())
        }
    }

    fun setPlugin(kamenu: KaMenu) {
        plugin = kamenu
    }

    fun registerBuiltinPredicate(name: String, predicate: (Player, String) -> Boolean) {
        builtinPredicates[name] = predicate
    }

    fun checkCondition(player: Player, condition: String?): Boolean {
        if (condition == null || condition.isBlank()) return true
        val processed = TextResolver.resolve(player, condition)
        return try {
            evaluate(player, parse(processed))
        } catch (ex: IllegalArgumentException) {
            plugin?.logger?.warning("Invalid condition expression '$condition': ${ex.message}")
            false
        }
    }

    private fun evaluate(player: Player, expr: Expr): Boolean {
        return when (expr) {
            is Literal -> expr.value
            is Not -> !evaluate(player, expr.expr)
            is And -> evaluate(player, expr.left) && evaluate(player, expr.right)
            is Or -> evaluate(player, expr.left) || evaluate(player, expr.right)
            is BuiltinCall -> {
                val predicate = builtinPredicates[expr.name] ?: return false
                val result = predicate(player, expr.arg)
                if (expr.negated) !result else result
            }
            is Compare -> compare(player, expr.left, expr.op, expr.right)
        }
    }

    private fun compare(player: Player, leftExpr: String, op: String, rightExpr: String): Boolean {
        val left = leftExpr
        val right = rightExpr

        return when (op) {
            "==" -> compareEquals(left, right)
            "!=" -> !compareEquals(left, right)
            ">" -> (left.toDoubleOrNull() ?: 0.0) > (right.toDoubleOrNull() ?: 0.0)
            ">=" -> (left.toDoubleOrNull() ?: 0.0) >= (right.toDoubleOrNull() ?: 0.0)
            "<" -> (left.toDoubleOrNull() ?: 0.0) < (right.toDoubleOrNull() ?: 0.0)
            "<=" -> (left.toDoubleOrNull() ?: 0.0) <= (right.toDoubleOrNull() ?: 0.0)
            else -> false
        }
    }

    private fun parse(text: String): Expr {
        val tokens = tokenize(text)
        val parser = Parser(tokens)
        return parser.parse()
    }

    private class Parser(private val tokens: List<Token>) {
        private var index = 0

        fun parse(): Expr {
            val expr = parseExpression()
            if (peek().type != TokenType.EOF) {
                throw IllegalArgumentException("Unexpected token '${peek().text}'")
            }
            return expr
        }

        private fun parseExpression(): Expr {
            return parseOr()
        }

        private fun parseOr(): Expr {
            var expr = parseAnd()
            while (match(TokenType.OR)) {
                expr = Or(expr, parseAnd())
            }
            return expr
        }

        private fun parseAnd(): Expr {
            var expr = parseUnary()
            while (match(TokenType.AND)) {
                expr = And(expr, parseUnary())
            }
            return expr
        }

        private fun parseUnary(): Expr {
            if (match(TokenType.BANG)) {
                return Not(parseUnary())
            }
            return parsePrimary()
        }

        private fun parsePrimary(): Expr {
            if (match(TokenType.LPAREN)) {
                val expr = parseExpression()
                expect(TokenType.RPAREN)
                return expr
            }

            val token = peek()
            if (token.type == TokenType.TRUE) {
                advance()
                return Literal(true)
            }
            if (token.type == TokenType.FALSE) {
                advance()
                return Literal(false)
            }

            return parseAtom()
        }

        private fun parseAtom(): Expr {
            val left = parseOperandText()

            if (peek().type == TokenType.DOT && isBuiltinPredicateName(left) && isBuiltinCallAhead()) {
                advance()
                val name = left
                val arg = parseDottedOperandText()
                return BuiltinCall(false, name, arg)
            }

            if (peek().type == TokenType.DOT && left.startsWith("!") && isBuiltinPredicateName(left.removePrefix("!"))) {
                val name = left.removePrefix("!")
                advance()
                val arg = parseDottedOperandText()
                return BuiltinCall(true, name, arg)
            }

            val fullLeft = parseRemainingDottedText(left)
            val opToken = peek()
            if (opToken.type in comparisonTokens()) {
                advance()
                val right = parseDottedOperandText()
                return Compare(fullLeft, opToken.text, right)
            }

            return if (fullLeft.equals("true", true)) {
                Literal(true)
            } else if (fullLeft.equals("false", true)) {
                Literal(false)
            } else {
                Compare(fullLeft, "==", "true")
            }
        }

        private fun parseOperandText(): String {
            val token = advance()
            return when (token.type) {
                TokenType.STRING, TokenType.IDENT, TokenType.NUMBER, TokenType.TRUE, TokenType.FALSE -> token.text
                else -> token.text
            }
        }

        private fun parseDottedOperandText(): String {
            return parseRemainingDottedText(parseOperandText())
        }

        private fun parseRemainingDottedText(first: String): String {
            val builder = StringBuilder(first)
            while (peek().type == TokenType.DOT && isOperandToken(peek(1))) {
                advance()
                builder.append('.').append(parseOperandText())
            }
            return builder.toString()
        }

        private fun isOperandToken(token: Token): Boolean {
            return token.type == TokenType.IDENT ||
                token.type == TokenType.STRING ||
                token.type == TokenType.NUMBER ||
                token.type == TokenType.TRUE ||
                token.type == TokenType.FALSE
        }

        private fun isBuiltinPredicateName(name: String): Boolean {
            return builtinPredicates.containsKey(name)
        }

        private fun isBuiltinCallAhead(): Boolean {
            return isOperandToken(peek(1))
        }

        private fun comparisonTokens(): Set<TokenType> = setOf(TokenType.EQ, TokenType.NE, TokenType.GT, TokenType.GE, TokenType.LT, TokenType.LE)

        private fun match(type: TokenType): Boolean {
            if (peek().type == type) {
                index++
                return true
            }
            return false
        }

        private fun expect(type: TokenType) {
            if (!match(type)) {
                throw IllegalArgumentException("Invalid condition expression")
            }
        }

        private fun peek(offset: Int = 0): Token {
            val pos = index + offset
            return if (pos < tokens.size) tokens[pos] else Token(TokenType.EOF, "")
        }

        private fun advance(): Token {
            val token = peek()
            if (index < tokens.size) index++
            return token
        }
    }

    private fun tokenize(text: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < text.length) {
            when (val ch = text[i]) {
                ' ', '\t', '\n', '\r' -> i++
                '(' -> {
                    tokens.add(Token(TokenType.LPAREN, "("))
                    i++
                }
                ')' -> {
                    tokens.add(Token(TokenType.RPAREN, ")"))
                    i++
                }
                '&' -> {
                    if (i + 1 < text.length && text[i + 1] == '&') {
                        tokens.add(Token(TokenType.AND, "&&"))
                        i += 2
                    } else {
                        i++
                    }
                }
                '|' -> {
                    if (i + 1 < text.length && text[i + 1] == '|') {
                        tokens.add(Token(TokenType.OR, "||"))
                        i += 2
                    } else {
                        i++
                    }
                }
                '!' -> {
                    if (i + 1 < text.length && text[i + 1] == '=') {
                        tokens.add(Token(TokenType.NE, "!="))
                        i += 2
                    } else {
                        tokens.add(Token(TokenType.BANG, "!"))
                        i++
                    }
                }
                '=' -> {
                    if (i + 1 < text.length && text[i + 1] == '=') {
                        tokens.add(Token(TokenType.EQ, "=="))
                        i += 2
                    } else {
                        i++
                    }
                }
                '>' -> {
                    if (i + 1 < text.length && text[i + 1] == '=') {
                        tokens.add(Token(TokenType.GE, ">="))
                        i += 2
                    } else {
                        tokens.add(Token(TokenType.GT, ">"))
                        i++
                    }
                }
                '<' -> {
                    if (i + 1 < text.length && text[i + 1] == '=') {
                        tokens.add(Token(TokenType.LE, "<="))
                        i += 2
                    } else {
                        tokens.add(Token(TokenType.LT, "<"))
                        i++
                    }
                }
                '.' -> {
                    tokens.add(Token(TokenType.DOT, "."))
                    i++
                }
                '[' -> {
                    i++
                    val sb = StringBuilder()
                    while (i < text.length && text[i] != ']') {
                        sb.append(text[i])
                        i++
                    }
                    if (i < text.length && text[i] == ']') i++
                    tokens.add(Token(TokenType.STRING, "[${sb}]"))
                }
                '"', '\'' -> {
                    val quote = ch
                    val start = i + 1
                    i++
                    val sb = StringBuilder()
                    while (i < text.length && text[i] != quote) {
                        if (text[i] == '\\' && i + 1 < text.length) {
                            i++
                        }
                        sb.append(text[i])
                        i++
                    }
                    tokens.add(Token(TokenType.STRING, sb.toString()))
                    if (i < text.length && text[i] == quote) i++
                }
                else -> {
                    val start = i
                    while (i < text.length && !text[i].isWhitespace() && "()&|!=<>.".indexOf(text[i]) == -1) {
                        i++
                    }
                    val word = text.substring(start, i)
                    val type = when (word.lowercase()) {
                        "true" -> TokenType.TRUE
                        "false" -> TokenType.FALSE
                        else -> if (word.toDoubleOrNull() != null) TokenType.NUMBER else TokenType.IDENT
                    }
                    tokens.add(Token(type, word))
                }
            }
        }
        return tokens
    }

    private fun checkPlayerMoney(player: Player, amount: Double): Boolean {
        val economy = Bukkit.getPluginManager().getPlugin("Vault") ?: return false
        if (!economy.isEnabled) return false
        return try {
            val provider = Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy::class.java)
                ?: return false
            provider.provider.getBalance(player) >= amount
        } catch (_: Exception) {
            false
        }
    }

    fun getPlayerStockItemCount(player: Player, itemName: String): Int {
        val currentPlugin = plugin ?: return 0
        val savedItem = currentPlugin.itemManager.getItem(itemName) ?: return 0

        val inventory = player.inventory
        var totalCount = 0
        val allItems = buildList {
            addAll(inventory.storageContents.filterNotNull())
            addAll(inventory.armorContents.filterNotNull())
            add(inventory.itemInOffHand)
            add(inventory.itemInMainHand)
        }

        for (item in allItems) {
            if (!item.isEmpty && item.isSimilar(savedItem)) {
                totalCount += item.amount
            }
        }
        return totalCount
    }

    private fun checkPlayerHasItem(player: Player, paramsStr: String): Boolean {
        var requiredAmount = 1
        paramsStr.split(";").forEach { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2 && parts[0].trim().lowercase() == "amount") {
                requiredAmount = parts[1].trim().toIntOrNull() ?: 1
            }
        }
        return getPlayerItemCount(player, paramsStr) >= requiredAmount
    }

    fun getPlayerItemCount(player: Player, paramsStr: String): Int {
        var materialName = ""
        var loreText: String? = null
        var itemModel: String? = null

        paramsStr.split(";").forEach { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                when (parts[0].trim().lowercase()) {
                    "mats" -> materialName = parts[1].trim()
                    "lore" -> loreText = parts[1].trim()
                    "model" -> itemModel = parts[1].trim()
                }
            }
        }

        if (materialName.isEmpty()) return 0
        val material = MaterialUtils.matchMaterial(materialName) ?: return 0

        val inventory = player.inventory
        var totalCount = 0
        val allItems = buildList {
            addAll(inventory.storageContents.filterNotNull())
            addAll(inventory.armorContents.filterNotNull())
            add(inventory.itemInOffHand)
            add(inventory.itemInMainHand)
        }

        for (item in allItems) {
            if (!item.isEmpty && item.type == material) {
                if (loreText != null) {
                    val itemMeta = item.itemMeta
                    if (itemMeta?.hasLore() != true) continue
                    val loreMatched = itemMeta.lore()?.any { line ->
                        val plainText = LegacyComponentSerializer.legacySection().serialize(line)
                        plainText.contains(loreText, ignoreCase = true)
                    } ?: false
                    if (!loreMatched) continue
                }

                if (itemModel != null) {
                    val itemMeta = item.itemMeta
                    if (itemMeta?.hasItemModel() != true) continue
                    val modelKey = itemMeta.itemModel ?: continue
                    val modelStr = "${modelKey.namespace()}:${modelKey.value()}"
                    if (!modelStr.equals(itemModel, ignoreCase = true)) continue
                }

                totalCount += item.amount
            }
        }

        return totalCount
    }

    private fun compareEquals(left: String, right: String): Boolean {
        val leftNum = left.toDoubleOrNull()
        val rightNum = right.toDoubleOrNull()
        if (leftNum != null && rightNum != null) return leftNum == rightNum

        val leftBool = parseBoolean(left)
        val rightBool = parseBoolean(right)
        if (leftBool != null && rightBool != null) return leftBool == rightBool

        return left.equals(right, ignoreCase = true)
    }

    private fun parseBoolean(value: String): Boolean? = when (value.trim().lowercase()) {
        "true", "yes", "1" -> true
        "false", "no", "0" -> false
        else -> null
    }
}
