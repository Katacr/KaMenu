package org.katacr.kamenu

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

/**
 * 条件判断工具类
 * 支持复杂的逻辑表达式、PAPI 变量和多种比较运算符
 */
object ConditionUtils {
    private var languageManager: LanguageManager? = null
    private var plugin: KaMenu? = null

    /**
     * 自定义异常：表示这不是一个内置条件，应该继续按普通比较处理
     */
    private class NotBuiltinConditionException : Exception()

    /**
     * 设置语言管理器引用
     */
    fun setLanguageManager(manager: LanguageManager) {
        languageManager = manager
    }

    /**
     * 设置插件引用
     */
    fun setPlugin(kamenu: KaMenu) {
        plugin = kamenu
    }

    /**
     * 解析变量（PAPI + 内置变量）
     * @param player 玩家对象
     * @param text 原始文本
     * @return 解析后的文本
     */
    fun resolveVariables(player: Player, text: String): String {
        var result = text

        // 1. 解析内置变量 {data:key}、{gdata:key} 和 {meta:key}
        if (plugin != null) {
            result = result.replace(Regex("\\{data:([^}]+)}")) { matchResult ->
                val key = matchResult.groupValues[1]
                plugin!!.databaseManager.getPlayerData(player.uniqueId, key)
                    ?: languageManager?.getMessage("papi.data_not_found", key) ?: "null"
            }
            result = result.replace(Regex("\\{gdata:([^}]+)}")) { matchResult ->
                val key = matchResult.groupValues[1]
                plugin!!.databaseManager.getGlobalData(key)
                    ?: languageManager?.getMessage("papi.data_not_found", key) ?: "null"
            }
            result = result.replace(Regex("\\{meta:([^}]+)}")) { matchResult ->
                val key = matchResult.groupValues[1]
                plugin!!.metaDataManager.getPlayerMeta(player.uniqueId, key)
            }
        }

        // 2. 解析 PAPI 变量
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, result)
            } catch (_: Exception) {
                // PAPI 解析失败，忽略
            }
        }

        return result
    }

    /**
     * 解析并检查条件字符串 (支持 &&, || 复合条件)
     * 例如: "%player_is_op% == true && %player_level% >= 10"
     */
    fun checkCondition(player: Player, condition: String?): Boolean {
        if (condition == null || condition.isBlank()) {
            return true
        }

        var processed = condition

        // 1. 先解析内置变量 {data:key}、{gdata:key} 和 {meta:key}
        if (plugin != null) {
            processed = processed.replace(Regex("\\{data:([^}]+)}")) { matchResult ->
                val key = matchResult.groupValues[1]
                plugin!!.databaseManager.getPlayerData(player.uniqueId, key) ?: "null"
            }
            processed = processed.replace(Regex("\\{gdata:([^}]+)}")) { matchResult ->
                val key = matchResult.groupValues[1]
                plugin!!.databaseManager.getGlobalData(key) ?: "null"
            }
            processed = processed.replace(Regex("\\{meta:([^}]+)}")) { matchResult ->
                val key = matchResult.groupValues[1]
                plugin!!.metaDataManager.getPlayerMeta(player.uniqueId, key)
            }
        }

        // 2. 进行 PAPI 变量替换
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                processed = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, processed)
            } catch (_: Exception) {
                // PAPI 解析失败，忽略
            }
        }

        // 解析逻辑表达式（支持 && 和 ||）
        return parseLogicalExpression(player, processed)
    }

    /**
     * 获取条件值（支持条件判断的单值返回）
     * 格式:
     *   - condition: "%player_is_op% == true"
     *     allow: "管理员专属文本"
     *     deny: "普通玩家文本"
     *
     * @param player 玩家对象
     * @param conditionMap 条件映射，包含 condition、allow、deny 键
     * @return 条件满足时的 allow 值，否则返回 deny 值
     */
    fun getConditionalValue(
        player: Player,
        conditionMap: Map<*, *>,
        defaultValue: String = ""
    ): String {
        val condition = conditionMap["condition"] as? String ?: return defaultValue
        val allow = conditionMap["allow"] as? String ?: defaultValue
        val deny = conditionMap["deny"] as? String ?: defaultValue

        // 检查条件并返回相应的值
        return if (checkCondition(player, condition)) {
            allow
        } else {
            deny
        }
    }

    /**
     * 从列表中获取条件值（支持多个条件判断）
     * 遍历条件列表，返回第一个匹配条件的 meet 值，否则返回默认值
     *
     * @param player 玩家对象
     * @param conditions 条件列表
     * @param defaultValue 默认值
     * @return 第一个匹配条件的 meet 值，否则返回默认值
     */
    fun getConditionalValueFromList(
        player: Player,
        conditions: List<*>,
        defaultValue: String = ""
    ): String {
        for (condition in conditions) {
            if (condition is Map<*, *>) {
                val result = getConditionalValue(player, condition)
                if (result.isNotEmpty()) {
                    return result
                }
            }
        }
        return defaultValue
    }

    /**
     * 获取条件列表值（支持条件判断的列表返回）
     * 格式:
     *   - condition: "%player_is_op% == true"
     *     allow:
     *       - '管理员行1'
     *       - '管理员行2'
     *     deny:
     *       - '普通玩家行1'
     *       - '普通玩家行2'
     *
     * @param player 玩家对象
     * @param conditionMap 条件映射，包含 condition、allow、deny 键
     * @return 条件满足时的 allow 列表，否则返回 deny 列表
     */
    fun getConditionalList(
        player: Player,
        conditionMap: Map<*, *>,
        defaultValue: List<String> = emptyList()
    ): List<String> {
        val condition = conditionMap["condition"] as? String ?: return defaultValue
        val allow = (conditionMap["allow"] as? List<*>)?.filterIsInstance<String>() ?: defaultValue
        val deny = (conditionMap["deny"] as? List<*>)?.filterIsInstance<String>() ?: defaultValue

        // 检查条件并返回相应的列表
        return if (checkCondition(player, condition)) {
            allow
        } else {
            deny
        }
    }

    /**
     * 从列表中获取条件列表值（支持多个条件判断）
     * 遍历条件列表，返回第一个匹配条件的 allow 列表，否则返回默认列表
     *
     * @param player 玩家对象
     * @param conditions 条件列表
     * @param defaultValue 默认列表
     * @return 第一个匹配条件的 allow 列表，否则返回默认列表
     */
    fun getConditionalListFromList(
        player: Player,
        conditions: List<*>,
        defaultValue: List<String> = emptyList()
    ): List<String> {
        for (condition in conditions) {
            if (condition is Map<*, *>) {
                val result = getConditionalList(player, condition)
                if (result.isNotEmpty()) {
                    return result
                }
            }
        }
        return defaultValue
    }

    /**
     * 递归解析逻辑表达式（支持 && 和 ||）
     * 优先级：&& 高于 ||
     */
    private fun parseLogicalExpression(player: Player, expression: String): Boolean {
        val trimmed = expression.trim()
        plugin?.logger?.info("[DEBUG] parseLogicalExpression - Expression: '$trimmed'")

        // 1. 先检查是否包含 ||（优先级最低）
        val orParts = splitByOperator(trimmed, "||")
        if (orParts.size > 1) {
            plugin?.logger?.info("[DEBUG] parseLogicalExpression - Found OR operator with ${orParts.size} parts")
            // 如果有 ||，先解析第一部分，如果为 true 则直接返回 true（短路求值）
            val firstPart = orParts[0]
            val firstResult = parseLogicalExpression(player, firstPart)
            plugin?.logger?.info("[DEBUG] parseLogicalExpression - OR first part: '$firstPart' = $firstResult")
            if (firstResult) return true // || 短路求值

            // 否则继续解析剩余部分
            val remaining = trimmed.substring(firstPart.length).trim().removePrefix("||").trim()
            return parseLogicalExpression(player, remaining)
        }

        // 2. 再检查是否包含 &&（优先级高于 ||）
        val andParts = splitByOperator(trimmed, "&&")
        if (andParts.size > 1) {
            plugin?.logger?.info("[DEBUG] parseLogicalExpression - Found AND operator with ${andParts.size} parts")
            // 如果有 &&，先解析第一部分，如果为 false 则直接返回 false（短路求值）
            val firstPart = andParts[0]
            val firstResult = parseLogicalExpression(player, firstPart)
            plugin?.logger?.info("[DEBUG] parseLogicalExpression - AND first part: '$firstPart' = $firstResult")
            if (!firstResult) return false // && 短路求值

            // 否则继续解析剩余部分
            val remaining = trimmed.substring(firstPart.length).trim().removePrefix("&&").trim()
            return parseLogicalExpression(player, remaining)
        }

        // 3. 如果没有逻辑运算符，则为基本条件
        val result = evaluateSingleCondition(player, trimmed)
        plugin?.logger?.info("[DEBUG] parseLogicalExpression - No operators found, evaluating single condition: $result")
        return result
    }

    /**
     * 按运算符分割表达式（考虑括号、引号和优先级）
     */
    private fun splitByOperator(expression: String, operator: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var parenDepth = 0
        var inSingleQuote = false
        var inDoubleQuote = false
        var i = 0

        while (i < expression.length) {
            val char = expression[i]

            when {
                char == '\\' -> {
                    // 转义字符，跳过下一个字符
                    current.append(char)
                    i++
                    if (i < expression.length) {
                        current.append(expression[i])
                    }
                }
                char == '\'' && !inDoubleQuote -> {
                    inSingleQuote = !inSingleQuote
                    current.append(char)
                }
                char == '"' && !inSingleQuote -> {
                    inDoubleQuote = !inDoubleQuote
                    current.append(char)
                }
                char == '(' && !inSingleQuote && !inDoubleQuote -> {
                    parenDepth++
                    current.append(char)
                }
                char == ')' && !inSingleQuote && !inDoubleQuote -> {
                    parenDepth--
                    current.append(char)
                }
                parenDepth > 0 || inSingleQuote || inDoubleQuote -> {
                    // 括号内、引号内不分割
                    current.append(char)
                }
                else -> {
                    // 检查是否匹配运算符
                    if (i + operator.length <= expression.length &&
                        expression.substring(i, i + operator.length) == operator) {
                        // 匹配到运算符
                        result.add(current.toString().trim())
                        current = StringBuilder()
                        i += operator.length - 1 // 跳过运算符
                    } else {
                        current.append(char)
                    }
                }
            }
            i++
        }

        if (current.isNotEmpty()) {
            result.add(current.toString().trim())
        }

        return result
    }

    /**
     * 评估单个条件（如 "5 >= 3" 或 "method.value"）
     */
    private fun evaluateSingleCondition(player: Player, condition: String): Boolean {
        val trimmed = condition.trim()
        plugin?.logger?.info("[DEBUG] evaluateSingleCondition - Condition: '$trimmed'")

        // 处理内置条件方法 method.value 格式
        // 检查是否包含 . 且不在引号内
        if (trimmed.contains(".") && !trimmed.matches(Regex("\".*\".*\\..*"))) {
            plugin?.logger?.info("[DEBUG] evaluateSingleCondition - Detected builtin condition format")
            try {
                val result = evaluateBuiltinCondition(player, trimmed)
                plugin?.logger?.info("[DEBUG] evaluateSingleCondition - Builtin condition result: $result")
                return result
            } catch (e: NotBuiltinConditionException) {
                plugin?.logger?.info("[DEBUG] evaluateSingleCondition - Not a builtin condition, continuing with comparison")
            }
        }

        // 如果是括号包裹的表达式，去掉括号后递归解析
        if (trimmed.startsWith("(") && trimmed.endsWith(")") && !trimmed.matches(Regex("\".*\".*\\(.*\\)"))) {
            val inner = trimmed.substring(1, trimmed.length - 1).trim()
            plugin?.logger?.info("[DEBUG] evaluateSingleCondition - Found parentheses, inner: '$inner'")
            // 确保括号是成对的（考虑引号）
            var parenCount = 0
            var inSingleQuote = false
            var inDoubleQuote = false
            var isCompletePair = true
            for (char in inner) {
                when (char) {
                    '\\' -> continue // 跳过转义字符的下一个字符
                    '\'' if !inDoubleQuote -> inSingleQuote = !inSingleQuote
                    '"' if !inSingleQuote -> inDoubleQuote = !inDoubleQuote
                    '(' if !inSingleQuote && !inDoubleQuote -> parenCount++
                    ')' if !inSingleQuote && !inDoubleQuote -> parenCount--
                }
                if (parenCount < 0) {
                    isCompletePair = false
                    break
                }
            }
            if (isCompletePair && parenCount == 0 && !inSingleQuote && !inDoubleQuote) {
                val result = parseLogicalExpression(player, inner)
                plugin?.logger?.info("[DEBUG] evaluateSingleCondition - Parentheses evaluation result: $result")
                return result
            }
        }

        // 匹配比较运算符
        val regex = "(>=|<=|==|!=|>|<)".toRegex()
        val match = regex.find(trimmed) ?: run {
            plugin?.logger?.warning("[DEBUG] evaluateSingleCondition - No operator found in condition: '$trimmed'")
            return false
        }

        val op = match.value
        val parts = trimmed.split(op, limit = 2)
        val left = parseQuotedString(parts[0].trim())
        val right = parseQuotedString(parts[1].trim())

        plugin?.logger?.info("[DEBUG] evaluateSingleCondition - Operator: $op, Left: '$left', Right: '$right'")

        val result = when (op) {
            "==" -> compareEquals(left, right)
            "!=" -> !compareEquals(left, right)
            ">"  -> left.toDoubleDefault(0.0) > right.toDoubleDefault(0.0)
            ">=" -> left.toDoubleDefault(0.0) >= right.toDoubleDefault(0.0)
            "<"  -> left.toDoubleDefault(0.0) < right.toDoubleDefault(0.0)
            "<=" -> left.toDoubleDefault(0.0) <= right.toDoubleDefault(0.0)
            else -> false
        }
        plugin?.logger?.info("[DEBUG] evaluateSingleCondition - Comparison result: $result")
        return result
    }

    /**
     * 解析并执行内置条件方法
     * 格式: "method.value" 或 "!method.value" 或 "method.\"value\"" 或 "!method.\"value\""
     * 支持 ! 前缀进行反向判断
     * 支持使用双引号包裹值以避免特殊字符被误解析
     * 支持的方法:
     *   - isNum: 判断是否为数字（整数或小数）
     *   - isPosNum: 判断是否为正数（大于0）
     *   - isInt: 判断是否为整数
     *   - isPosInt: 判断是否为正整数（大于0）
     *   - hasPerm: 判断玩家是否拥有权限 (value应为权限节点)
     *   - hasMoney: 判断玩家是否有足够的金币 (value应为金额)
     * @throws NotBuiltinConditionException 如果这不是一个有效的内置条件格式
     */
    private fun evaluateBuiltinCondition(player: Player, condition: String): Boolean {
        val trimmed = condition.trim()
        plugin?.logger?.info("[DEBUG] evaluateBuiltinCondition - Condition: '$trimmed'")

        // 检查是否为反向判断
        val isNegative = trimmed.startsWith("!")
        val conditionWithoutNegation = if (isNegative) trimmed.substring(1).trim() else trimmed
        plugin?.logger?.info("[DEBUG] evaluateBuiltinCondition - Is negative: $isNegative")

        // 查找第一个点号，但要考虑引号
        var dotIndex = -1
        var inQuote = false
        var i = 0
        while (i < conditionWithoutNegation.length) {
            val char = conditionWithoutNegation[i]
            when (char) {
                '\\' -> {
                    // 跳过转义字符的下一个字符
                    i++
                }
                '"' -> {
                    inQuote = !inQuote
                }
                '.' if !inQuote -> {
                    dotIndex = i
                    break
                }
            }
            i++
        }

        if (dotIndex == -1) {
            plugin?.logger?.warning("[DEBUG] evaluateBuiltinCondition - No dot found in condition")
            throw NotBuiltinConditionException()
        }

        val method = conditionWithoutNegation.take(dotIndex).trim()
        var value = conditionWithoutNegation.substring(dotIndex + 1).trim()

        // 如果值被双引号包裹，则去掉引号
        if (value.startsWith("\"") && value.endsWith("\"") && value.length >= 2) {
            value = value.substring(1, value.length - 1)
        }

        plugin?.logger?.info("[DEBUG] evaluateBuiltinCondition - Method: '$method', Value: '$value'")

        val result = when (method) {
            "isNum" -> {
                val numResult = value.toDoubleOrNull() != null
                plugin?.logger?.info("[DEBUG] evaluateBuiltinCondition - isNum('$value') = $numResult")
                numResult
            }
            "isPosNum" -> {
                val numResult = value.toDoubleOrNull()?.let { it > 0 } ?: false
                plugin?.logger?.info("[DEBUG] evaluateBuiltinCondition - isPosNum('$value') = $numResult")
                numResult
            }
            "isInt" -> {
                val intResult = value.toIntOrNull() != null
                plugin?.logger?.info("[DEBUG] evaluateBuiltinCondition - isInt('$value') = $intResult")
                intResult
            }
            "isPosInt" -> {
                val intResult = value.toIntOrNull()?.let { it > 0 } ?: false
                plugin?.logger?.info("[DEBUG] evaluateBuiltinCondition - isPosInt('$value') = $intResult")
                intResult
            }
            "hasPerm" -> {
                val permResult = player.hasPermission(value)
                plugin?.logger?.info("[DEBUG] evaluateBuiltinCondition - hasPerm('$value') = $permResult")
                permResult
            }
            "hasMoney" -> {
                val amount = value.toDoubleOrNull()
                if (amount == null) {
                    plugin?.logger?.warning("[DEBUG] evaluateBuiltinCondition - hasMoney - invalid amount: '$value'")
                    false
                } else {
                    val moneyResult = checkPlayerMoney(player, amount)
                    plugin?.logger?.info("[DEBUG] evaluateBuiltinCondition - hasMoney($amount) = $moneyResult")
                    moneyResult
                }
            }
            else -> {
                // 未知方法，说明这不是内置条件，应该按普通比较处理
                plugin?.logger?.info("[DEBUG] evaluateBuiltinCondition - Unknown method '$method', treating as normal comparison")
                throw NotBuiltinConditionException()
            }
        }

        // 返回判断结果（如果为反向判断则取反）
        val finalResult = if (isNegative) !result else result
        plugin?.logger?.info("[DEBUG] evaluateBuiltinCondition - Final result: $finalResult")
        return finalResult
    }

    /**
     * 检查玩家是否有足够的金币
     * 支持 Vault 和其他经济插件
     */
    private fun checkPlayerMoney(player: Player, amount: Double): Boolean {
        // 检查Vault经济插件
        val economy = Bukkit.getPluginManager().getPlugin("Vault")
        if (economy != null && economy.isEnabled) {
            try {
                val economyProvider = Bukkit.getServicesManager().getRegistration(
                    net.milkbowl.vault.economy.Economy::class.java
                )
                if (economyProvider != null) {
                    val econ = economyProvider.provider
                    return econ.getBalance(player) >= amount
                }
            } catch (_: Exception) {
                // Vault 检查失败，忽略
            }
        }

        return false
    }

    private fun String.toDoubleDefault(default: Double): Double = this.toDoubleOrNull() ?: default

    /**
     * 解析引号包裹的字符串
     * 支持单引号和双引号包裹，如果未用引号包裹则直接返回原字符串
     * @param str 原始字符串
     * @return 去除引号后的字符串
     */
    private fun parseQuotedString(str: String): String {
        val trimmed = str.trim()

        // 检查是否被双引号包裹
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length >= 2) {
            // 检查引号是否匹配（避免像 "test 这样的情况）
            val content = trimmed.substring(1, trimmed.length - 1)
            // 检查内容中是否有未转义的引号
            if (!content.contains("\"")) {
                return content
            }
        }

        // 检查是否被单引号包裹
        if (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length >= 2) {
            val content = trimmed.substring(1, trimmed.length - 1)
            if (!content.contains("'")) {
                return content
            }
        }

        // 未用引号包裹，直接返回
        return trimmed
    }

    /**
     * 比较两个值是否相等（支持数值和字符串）
     * 优先使用数值比较，失败则使用字符串比较
     */
    private fun compareEquals(left: String, right: String): Boolean {
        // 1. 先尝试数值比较
        val leftNum = left.toDoubleOrNull()
        val rightNum = right.toDoubleOrNull()

        if (leftNum != null && rightNum != null) {
            // 两边都是数字，使用数值比较
            return leftNum == rightNum
        }

        // 2. 尝试布尔值比较
        val leftBool = parseBoolean(left)
        val rightBool = parseBoolean(right)

        if (leftBool != null && rightBool != null) {
            return leftBool == rightBool
        }

        // 3. 字符串比较（忽略大小写）
        return left.equals(right, ignoreCase = true)
    }

    /**
     * 解析字符串为布尔值
     * 支持的 true 值：true, yes, 1, t, y
     * 支持的 false 值：false, no, 0, f, n
     * 其他情况返回 null
     */
    private fun parseBoolean(value: String): Boolean? {
        val normalized = value.trim().lowercase()
        return when (normalized) {
            "true", "yes", "1", "t", "y" -> true
            "false", "no", "0", "f", "n" -> false
            else -> null
        }
    }

    /**
     * 从 ConfigurationSection 获取适合当前玩家的值（支持条件判断）
     * @param player 玩家对象
     * @param section 配置节
     * @param path 配置路径（相对于 section）
     * @param defaultValue 默认值
     * @return 字符串值
     */
    fun getConditionalValueFromSection(player: Player, section: ConfigurationSection, path: String, defaultValue: String = ""): String {
        plugin?.logger?.info("[DEBUG] getConditionalValueFromSection - Player: ${player.name}, Path: $path, Default: $defaultValue")
        // 检查该路径下是否为列表格式（条件判断）
        if (section.isList(path)) {
            plugin?.logger?.info("[DEBUG] getConditionalValueFromSection - Path is list format")
            val conditions = section.getList(path) ?: return defaultValue
            val value = getConditionalValueFromList(player, conditions, defaultValue)
            val resolved = resolveVariables(player, value)
            plugin?.logger?.info("[DEBUG] getConditionalValueFromSection - Result (list): $resolved")
            return resolved
        } else {
            // 简单字符串值
            val value = section.getString(path, defaultValue) ?: defaultValue
            val resolved = resolveVariables(player, value)
            plugin?.logger?.info("[DEBUG] getConditionalValueFromSection - Result (string): $resolved")
            return resolved
        }
    }

    /**
     * 从 ConfigurationSection 获取适合当前玩家的整数值（支持条件判断）
     * @param player 玩家对象
     * @param section 配置节
     * @param path 配置路径（相对于 section）
     * @param defaultValue 默认值
     * @return 整数值
     */
    fun getConditionalIntFromSection(player: Player, section: ConfigurationSection, path: String, defaultValue: Int = 0): Int {
        plugin?.logger?.info("[DEBUG] getConditionalIntFromSection - Player: ${player.name}, Path: $path, Default: $defaultValue")
        if (section.isList(path)) {
            plugin?.logger?.info("[DEBUG] getConditionalIntFromSection - Path is list format")
            val conditions = section.getList(path) ?: return defaultValue
            val stringValue = getConditionalValueFromList(player, conditions, defaultValue.toString())
            val resolved = resolveVariables(player, stringValue)
            val result = resolved.toIntOrNull() ?: defaultValue
            plugin?.logger?.info("[DEBUG] getConditionalIntFromSection - Result (list): $result")
            return result
        } else {
            val value = section.getString(path, defaultValue.toString()) ?: defaultValue.toString()
            val resolved = resolveVariables(player, value)
            val result = resolved.toIntOrNull() ?: defaultValue
            plugin?.logger?.info("[DEBUG] getConditionalIntFromSection - Result (string): $result")
            return result
        }
    }

    /**
     * 从 ConfigurationSection 获取适合当前玩家的双精度浮点数值（支持条件判断）
     * @param player 玩家对象
     * @param section 配置节
     * @param path 配置路径（相对于 section）
     * @param defaultValue 默认值
     * @return 双精度浮点数值
     */
    fun getConditionalDoubleFromSection(player: Player, section: ConfigurationSection, path: String, defaultValue: Double = 0.0): Double {
        plugin?.logger?.info("[DEBUG] getConditionalDoubleFromSection - Player: ${player.name}, Path: $path, Default: $defaultValue")
        if (section.isList(path)) {
            plugin?.logger?.info("[DEBUG] getConditionalDoubleFromSection - Path is list format")
            val conditions = section.getList(path) ?: return defaultValue
            val stringValue = getConditionalValueFromList(player, conditions, defaultValue.toString())
            val resolved = resolveVariables(player, stringValue)
            val result = resolved.toDoubleOrNull() ?: defaultValue
            plugin?.logger?.info("[DEBUG] getConditionalDoubleFromSection - Result (list): $result")
            return result
        } else {
            val value = section.getString(path, defaultValue.toString()) ?: defaultValue.toString()
            val resolved = resolveVariables(player, value)
            val result = resolved.toDoubleOrNull() ?: defaultValue
            plugin?.logger?.info("[DEBUG] getConditionalDoubleFromSection - Result (string): $result")
            return result
        }
    }

    /**
     * 从 ConfigurationSection 获取适合当前玩家的布尔值（支持条件判断）
     * @param player 玩家对象
     * @param section 配置节
     * @param path 配置路径（相对于 section）
     * @param defaultValue 默认值
     * @return 布尔值
     */
    fun getConditionalBooleanFromSection(player: Player, section: ConfigurationSection, path: String, defaultValue: Boolean = false): Boolean {
        plugin?.logger?.info("[DEBUG] getConditionalBooleanFromSection - Player: ${player.name}, Path: $path, Default: $defaultValue")
        if (section.isList(path)) {
            plugin?.logger?.info("[DEBUG] getConditionalBooleanFromSection - Path is list format")
            val conditions = section.getList(path) ?: return defaultValue
            val stringValue = getConditionalValueFromList(player, conditions, defaultValue.toString())
            val resolved = resolveVariables(player, stringValue)
            val result = resolved.toBooleanStrictOrNull() ?: defaultValue
            plugin?.logger?.info("[DEBUG] getConditionalBooleanFromSection - Result (list): $result")
            return result
        } else {
            val value = section.getString(path, defaultValue.toString()) ?: defaultValue.toString()
            val resolved = resolveVariables(player, value)
            val result = resolved.toBooleanStrictOrNull() ?: defaultValue
            plugin?.logger?.info("[DEBUG] getConditionalBooleanFromSection - Result (string): $result")
            return result
        }
    }

    /**
     * 从 ConfigurationSection 获取适合当前玩家的列表值（支持条件判断）
     * @param player 玩家对象
     * @param section 配置节
     * @param path 配置路径（相对于 section）
     * @param defaultValue 默认列表
     * @return 列表值
     */
    fun getConditionalListFromSection(player: Player, section: ConfigurationSection, path: String, defaultValue: List<String> = emptyList()): List<String> {
        plugin?.logger?.info("[DEBUG] getConditionalListFromSection - Player: ${player.name}, Path: $path")
        if (section.isList(path)) {
            val firstItem = section.getList(path)?.firstOrNull()
            // 检查是否为条件判断格式（第一个元素是 Map）
            if (firstItem is Map<*, *>) {
                plugin?.logger?.info("[DEBUG] getConditionalListFromSection - Path is conditional list format")
                val conditions = section.getList(path) ?: return defaultValue
                val list = getConditionalListFromList(player, conditions, defaultValue)
                val result = list.map { resolveVariables(player, it) }
                plugin?.logger?.info("[DEBUG] getConditionalListFromSection - Result (conditional): $result")
                return result
            } else {
                // 普通字符串列表
                plugin?.logger?.info("[DEBUG] getConditionalListFromSection - Path is normal list format")
                val list = section.getStringList(path)
                val result = list.map { resolveVariables(player, it) }
                plugin?.logger?.info("[DEBUG] getConditionalListFromSection - Result (normal): $result")
                return result
            }
        } else {
            plugin?.logger?.info("[DEBUG] getConditionalListFromSection - Path is not a list, returning default")
            return defaultValue
        }
    }

    /**
     * 从 ConfigurationSection 获取适合当前玩家的类型值（支持条件判断和 'none'）
     * @param player 玩家对象
     * @param section 配置节
     * @param path 配置路径（相对于 section）
     * @param defaultValue 默认值
     * @return 类型值，如果类型为 'none' 或为空则返回 'none'
     */
    fun getConditionalTypeFromSection(player: Player, section: ConfigurationSection, path: String, defaultValue: String = ""): String {
        val rawValue = getConditionalValueFromSection(player, section, path, defaultValue)
        if (rawValue.isEmpty()) {
            return "none"
        }

        // 检查是否为列表格式（条件判断）
        if (section.isList(path)) {
            val firstItem = section.getList(path)?.firstOrNull()
            // 检查是否为条件判断格式（第一个元素是 Map）
            if (firstItem is Map<*, *>) {
                val conditions = section.getList(path) ?: return "none"
                val result = getConditionalValueFromList(player, conditions, defaultValue)
                return result.ifEmpty { "none" }
            }
        }

        // 返回解析后的值
        return rawValue
    }
}
