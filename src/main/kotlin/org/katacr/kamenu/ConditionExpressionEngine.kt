package org.katacr.kamenu

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player

/**
 * 条件表达式求值引擎。
 *
 * 使用简单 AST 解析 `&&`、`||`、`!`、括号、比较表达式和内置判断函数。
 * 变量会先经 [TextResolver.resolveForCondition] 安全替换，再进入词法解析，避免用户输入中的空格、
 * 换行或 YAML 片段破坏表达式结构。
 *
 * 支持示例：
 * `hasPerm.kamenu.admin && %player_level% >= 10`
 * `isNull.{data:nickname} || inGlist.{player_name};allowed_players`
 */
object ConditionExpressionEngine {
    private var plugin: KaMenu? = null

    private val builtinPredicates = mutableMapOf<String, (Player, String) -> Boolean>()

    /**
     * AST 节点。
     *
     * 这里保持私有，外部只通过 checkCondition 使用条件系统，避免暴露解析细节。
     */
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
        registerBuiltinPredicate("isNull") { _, value -> isNullLikeValue(value) }
        registerBuiltinPredicate("isPass") { _, value -> value.isEmpty() }
        registerBuiltinPredicate("isTrue") { _, value -> parseBoolean(value) == true }
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
        registerBuiltinPredicate("inList") { _, value -> checkValueInGroup(value) }
        registerBuiltinPredicate("inGlist") { _, value -> checkValueInGroup(value) }
    }

    fun setPlugin(kamenu: KaMenu) {
        plugin = kamenu
    }

    /**
     * 注册内置判断函数。
     *
     * 函数语法为 `name.argument`，参数在进入 predicate 前会被解码为原始字符串。
     */
    fun registerBuiltinPredicate(name: String, predicate: (Player, String) -> Boolean) {
        builtinPredicates[name] = predicate
    }

    /**
     * 检查条件是否成立。
     *
     * 空条件视为通过。非法表达式会记录 warning 并返回 false，避免配置错误误放行。
     */
    fun checkCondition(player: Player, condition: String?): Boolean {
        return checkCondition(player, condition, emptyMap())
    }

    /**
     * 检查条件是否成立，并携带动作/按钮上下文变量。
     *
     * variables 支持 `{arg:0}`、`{item.value}`、输入捕获等调用方注入的值。
     */
    fun checkCondition(player: Player, condition: String?, variables: Map<String, String>): Boolean {
        return checkCondition(player, condition, variables) { null }
    }

    /**
     * 检查条件是否成立，并支持动态变量解析。
     *
     * repeat 分页等运行态变量会通过 dynamicResolver 注入。
     */
    fun checkCondition(
        player: Player,
        condition: String?,
        variables: Map<String, String>,
        dynamicResolver: (String) -> String?
    ): Boolean {
        return checkCondition(player, condition, variables, null, dynamicResolver)
    }

    fun checkCondition(
        player: Player,
        condition: String?,
        variables: Map<String, String>,
        menuConfig: YamlConfiguration?,
        dynamicResolver: (String) -> String?
    ): Boolean {
        if (condition == null || condition.isBlank()) return true
        val processed = preserveDelimitedFunctionArguments(TextResolver.resolveForCondition(player, condition, variables, menuConfig, dynamicResolver))
        return try {
            evaluate(player, parse(processed))
        } catch (ex: IllegalArgumentException) {
            plugin?.logger?.warning("Invalid condition expression '$condition': ${ex.message}")
            false
        }
    }

    /**
     * 递归求值 AST。
     *
     * And/Or 保持短路语义，避免不必要的右侧条件计算。
     */
    private fun evaluate(player: Player, expr: Expr): Boolean {
        return when (expr) {
            is Literal -> expr.value
            is Not -> !evaluate(player, expr.expr)
            is And -> evaluate(player, expr.left) && evaluate(player, expr.right)
            is Or -> evaluate(player, expr.left) || evaluate(player, expr.right)
            is BuiltinCall -> {
                val predicate = builtinPredicates[expr.name] ?: return false
                val result = predicate(player, TextResolver.decodeConditionArgumentMarkers(expr.arg))
                if (expr.negated) !result else result
            }
            is Compare -> compare(player, expr.left, expr.op, expr.right)
        }
    }

    private fun compare(player: Player, leftExpr: String, op: String, rightExpr: String): Boolean {
        val left = resolveValueFunction(leftExpr)
        val right = resolveValueFunction(rightExpr)

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

    /**
     * 表达式解析器。
     *
     * 优先级从低到高为：OR、AND、NOT、括号/原子表达式。
     */
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

            return parseAtom()
        }

        private fun parseAtom(): Expr {
            val left = parseOperandText()

            if (peek().type == TokenType.DOT && isBuiltinPredicateName(left) && (isBuiltinCallAhead() || supportsEmptyArgument(left))) {
                advance()
                val name = left
                val arg = parseBuiltinArgument(name)
                return BuiltinCall(false, name, arg)
            }

            if (peek().type == TokenType.DOT && left.startsWith("!") && isBuiltinPredicateName(left.removePrefix("!"))) {
                val name = left.removePrefix("!")
                advance()
                val arg = parseBuiltinArgument(name)
                return BuiltinCall(true, name, arg)
            }

            val fullLeft = parseValueOperandText(left, stopAtComparison = true)
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
            return parseValueOperandText(parseOperandText(), stopAtComparison = false)
        }

        private fun parseValueOperandText(first: String, stopAtComparison: Boolean): String {
            if (peek().type == TokenType.DOT && isValueFunctionName(first)) {
                advance()
                return "$first.${parseDelimitedArgument(stopAtComparison)}"
            }
            return parseRemainingDottedText(first)
        }

        private fun parseBuiltinArgument(name: String): String {
            if (!supportsDelimitedArgument(name)) {
                return parseDottedOperandText()
            }

            if (isExpressionBoundary(peek(), stopAtComparison = false)) {
                return ""
            }

            return parseDelimitedArgument(stopAtComparison = false)
        }

        private fun parseDelimitedArgument(stopAtComparison: Boolean): String {
            val builder = StringBuilder()
            var previousWasDot = false
            while (!isExpressionBoundary(peek(), stopAtComparison)) {
                val token = advance()
                if (token.type == TokenType.DOT) {
                    builder.append('.')
                    previousWasDot = true
                } else {
                    if (builder.isNotEmpty() && !previousWasDot) {
                        builder.append(' ')
                    }
                    builder.append(token.text)
                    previousWasDot = false
                }
            }
            return builder.toString()
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

        private fun isValueFunctionName(name: String): Boolean {
            return name == "getLength"
        }

        private fun supportsEmptyArgument(name: String): Boolean {
            return isBuiltinPredicateName(name)
        }

        private fun supportsDelimitedArgument(name: String): Boolean {
            return isBuiltinPredicateName(name)
        }

        private fun isExpressionBoundary(token: Token, stopAtComparison: Boolean): Boolean {
            return token.type == TokenType.AND ||
                token.type == TokenType.OR ||
                token.type == TokenType.RPAREN ||
                token.type == TokenType.EOF ||
                (stopAtComparison && token.type in comparisonTokens())
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

        var totalCount = 0

        for (item in getUniqueInventoryItems(player)) {
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

    private fun checkValueInGroup(paramsStr: String): Boolean {
        val parts = paramsStr.split(";", limit = 2)
        if (parts.size != 2) return false

        val target = parts[0].trim()
        if (target.isEmpty()) return false

        val group = decodeGroupValues(parts[1].trim())
        return group.any { it.equals(target, ignoreCase = true) }
    }

    private fun decodeGroupValues(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return when {
            raw.startsWith("[") && raw.endsWith("]") -> DatabaseManager.decodeStringList(raw)
            raw.contains('\n') -> raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
            raw.contains(",") -> raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            else -> listOf(raw.trim())
        }
    }

    fun getPlayerItemCount(player: Player, paramsStr: String): Int {
        var materialName = ""
        var loreText: String? = null
        var itemModel: String? = null
        var customModelIdText: String? = null

        paramsStr.split(";").forEach { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                when (parts[0].trim().lowercase()) {
                    "mats" -> materialName = parts[1].trim()
                    "lore" -> loreText = parts[1].trim()
                    "model", "item_model" -> itemModel = parts[1].trim()
                    "cmd", "custom_model_data", "custom_model_id" -> customModelIdText = parts[1].trim()
                }
            }
        }

        if (materialName.isEmpty()) return 0
        val material = MaterialUtils.matchMaterial(materialName) ?: return 0
        val customModelId = customModelIdText?.toIntOrNull()
        if (customModelIdText != null && customModelId == null) return 0

        var totalCount = 0

        for (item in getUniqueInventoryItems(player)) {
            if (!item.isEmpty && item.type == material) {
                val itemMeta = item.itemMeta
                if (loreText != null) {
                    if (!itemMeta.hasLore()) continue
                    val loreMatched = itemMeta.lore()?.any { line ->
                        val plainText = LegacyComponentSerializer.legacySection().serialize(line)
                        plainText.contains(loreText, ignoreCase = true)
                    } ?: false
                    if (!loreMatched) continue
                }

                if (itemModel != null) {
                    val actualItemModel = ItemPropertyReader.getItemModel(itemMeta) ?: continue
                    if (!actualItemModel.equals(itemModel, ignoreCase = true)) continue
                }

                if (customModelId != null) {
                    if (ItemPropertyReader.getCustomModelId(itemMeta) != customModelId) continue
                }

                totalCount += item.amount
            }
        }

        return totalCount
    }

    /**
     * 获取玩家背包中参与条件计数的唯一物品堆。
     *
     * storageContents 已包含当前主手所在的快捷栏槽位，因此这里只额外加入护甲和副手，
     * 避免 hasItem / hasStockItem 对主手物品重复计数。
     */
    private fun getUniqueInventoryItems(player: Player): List<org.bukkit.inventory.ItemStack> {
        val inventory = player.inventory
        return buildList {
            addAll(inventory.storageContents.filterNotNull())
            addAll(inventory.armorContents.filterNotNull())
            add(inventory.itemInOffHand)
        }
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

    private fun isNullLikeValue(value: String): Boolean {
        val normalized = value.trim()
        return normalized.isEmpty() || normalized.equals("null", ignoreCase = true)
    }

    private fun resolveValueFunction(value: String): String {
        return when {
            value.startsWith("getLength.") -> TextResolver.decodeConditionArgumentMarkers(value.removePrefix("getLength.")).length.toString()
            else -> value
        }
    }

    private fun preserveDelimitedFunctionArguments(text: String): String {
        val predicateNames = builtinPredicates.keys
        val valueNames = setOf("getLength")
        val functionNames = predicateNames + valueNames
        val builder = StringBuilder()
        var index = 0

        while (index < text.length) {
            val name = if (isFunctionBoundaryBefore(text, index)) {
                functionNames.firstOrNull { text.startsWith("$it.", index) }
            } else {
                null
            }
            if (name == null) {
                builder.append(text[index])
                index++
                continue
            }

            val argumentStart = index + name.length + 1
            val argumentEnd = findDelimitedArgumentEnd(text, argumentStart, stopAtComparison = name in valueNames)
            val argument = text.substring(argumentStart, argumentEnd)
            builder.append(name)
                .append(".\"")
                .append(escapeConditionString(argument))
                .append("\"")
            index = argumentEnd
        }

        return builder.toString()
    }

    private fun isFunctionBoundaryBefore(text: String, index: Int): Boolean {
        if (index == 0) return true
        return when (text[index - 1]) {
            '!', '(', ' ', '\t', '\n', '\r', '&', '|' -> true
            else -> false
        }
    }

    private fun findDelimitedArgumentEnd(text: String, start: Int, stopAtComparison: Boolean): Int {
        var index = start
        while (index < text.length) {
            if (text.startsWith("&&", index) || text.startsWith("||", index) || text[index] == ')') {
                return trimSyntaxWhitespaceBeforeBoundary(text, start, index)
            }
            if (stopAtComparison && isComparisonAt(text, index)) {
                return trimSyntaxWhitespaceBeforeBoundary(text, start, index)
            }
            index++
        }
        return text.length
    }

    private fun trimSyntaxWhitespaceBeforeBoundary(text: String, start: Int, boundary: Int): Int {
        var end = boundary
        while (end > start && text[end - 1].isWhitespace()) {
            end--
        }
        return end
    }

    private fun isComparisonAt(text: String, index: Int): Boolean {
        return text.startsWith("==", index) ||
            text.startsWith("!=", index) ||
            text.startsWith(">=", index) ||
            text.startsWith("<=", index) ||
            text[index] == '>' ||
            text[index] == '<'
    }

    private fun escapeConditionString(value: String): String {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
    }

    private fun parseBoolean(value: String): Boolean? = when (value.trim().lowercase()) {
        "true", "yes", "1" -> true
        "false", "no", "0" -> false
        else -> null
    }
}
