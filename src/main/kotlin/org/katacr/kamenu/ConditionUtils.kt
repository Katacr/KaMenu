package org.katacr.kamenu

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

/**
 * 条件判断工具类
 * 支持复杂的逻辑表达式、PAPI 变量和多种比较运算符
 *
 * 核心功能：
 * 1. 变量解析 - resolveVariables() 解析 {data:}, {gdata:}, {meta:}, PAPI 变量
 * 2. 条件检查 - checkCondition() 检查条件是否满足
 * 3. 条件值获取 - getConditionString/OrList/List 系列方法从条件Map中获取值，支持多层嵌套
 * 4. 配置读取 - get() 系列方法从 ConfigurationSection 读取并解析变量
 *
 * 嵌套条件支持示例：
 * ```
 * tooltip:
 *   - condition: '{data:sign_day} >= 1'
 *     allow: '&7已领取'
 *     deny:
 *       - condition: "{data:last_date} == %server_time%"
         allow:
           - condition: 'hasPerm.admin'
             allow: '&c管理员今日已签到'
             deny: '&c玩家今日已签到'
         deny: '&a可领取奖励'
 * ```
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

    // ==================== 核心：变量解析 ====================

    /**
     * 解析所有变量（PAPI + 内置变量）
     * 支持：{data:key}, {gdata:key}, {meta:key}, %papi_variable%
     * @param player 玩家对象
     * @param text 原始文本
     * @return 解析后的文本
     */
    fun resolveVariables(player: Player, text: String): String {
        var result = text

        // 1. 解析内置变量
        if (plugin != null) {
            result = result.replace(Regex("\\{data:([^}]+)}")) { match ->
                val key = match.groupValues[1]
                plugin!!.databaseManager.getPlayerData(player.uniqueId, key)
                    ?: languageManager?.getMessage("papi.data_not_found", key) ?: "null"
            }
            result = result.replace(Regex("\\{gdata:([^}]+)}")) { match ->
                val key = match.groupValues[1]
                plugin!!.databaseManager.getGlobalData(key)
                    ?: languageManager?.getMessage("papi.data_not_found", key) ?: "null"
            }
            result = result.replace(Regex("\\{meta:([^}]+)}")) { match ->
                val key = match.groupValues[1]
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
     * 解析条件中的变量（供内部使用）
     */
    private fun resolveConditionVariables(player: Player, condition: String): String {
        var result = condition

        if (plugin != null) {
            result = result.replace(Regex("\\{data:([^}]+)}")) { match ->
                val key = match.groupValues[1]
                plugin!!.databaseManager.getPlayerData(player.uniqueId, key) ?: "null"
            }
            result = result.replace(Regex("\\{gdata:([^}]+)}")) { match ->
                val key = match.groupValues[1]
                plugin!!.databaseManager.getGlobalData(key) ?: "null"
            }
            result = result.replace(Regex("\\{meta:([^}]+)}")) { match ->
                val key = match.groupValues[1]
                plugin!!.metaDataManager.getPlayerMeta(player.uniqueId, key)
            }
        }

        // 解析 PAPI 变量
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, result)
            } catch (_: Exception) {
                // PAPI 解析失败，忽略
            }
        }

        return result
    }

    // ==================== 核心：条件检查 ====================

    /**
     * 解析并检查条件字符串 (支持 &&, || 复合条件)
     * 例如: "%player_is_op% == true && %player_level% >= 10"
     */
    fun checkCondition(player: Player, condition: String?): Boolean {
        if (condition == null || condition.isBlank()) {
            return true
        }

        val processed = resolveConditionVariables(player, condition)
        return parseLogicalExpression(player, processed)
    }

    // ==================== 核心：条件值获取 ====================

    /**
     * 从条件 Map 中获取值
     * @param conditionMap 条件 Map，包含 condition、allow、deny 键
     * @param player 玩家对象
     * @param allowType allow 值的类型：STRING（字符串）、LIST（列表）、AUTO（自动检测）
     * @param denyType deny 值的类型：同上
     * @param defaultValue 默认值
     * @return 条件满足时的值，否则返回默认值
     */
    enum class ValueType { STRING, LIST, AUTO }

    @Suppress("UNUSED_PARAMETER")
    private fun <T> getConditionValue(
        conditionMap: Map<*, *>,
        player: Player,
        allowType: ValueType,
        denyType: ValueType,
        defaultValue: T,
        converter: (Any, Player) -> T
    ): T {
        val condition = conditionMap["condition"] as? String ?: return defaultValue
        val allow = conditionMap["allow"]
        val deny = conditionMap["deny"]

        val result = if (checkCondition(player, condition)) {
            if (allow != null) converter(allow, player) else defaultValue
        } else {
            if (deny != null) converter(deny, player) else defaultValue
        }

        return result ?: defaultValue
    }

    /**
     * 获取条件值（allow/deny 只支持字符串）
     * 格式:
     *   - condition: "%player_is_op% == true"
     *     allow: "管理员专属文本"
     *     deny: "普通玩家文本"
     */
    fun getConditionString(
        player: Player,
        conditionMap: Map<*, *>,
        defaultValue: String = ""
    ): String {
        return getConditionValue(
            conditionMap, player,
            ValueType.STRING, ValueType.STRING,
            defaultValue
        ) { value, _ ->
            (value as? String)?.replace("\\n", "\n") ?: defaultValue
        }
    }

    /**
     * 获取条件列表值（allow/deny 支持列表或嵌套条件判断）
     * 格式:
     *   - condition: "%player_is_op% == true"
     *     allow:
     *       - '管理员行1'
     *       - '管理员行2'
     *     deny:
     *       - '普通玩家行1'
     *       - '普通玩家行2'
     */
    fun getConditionList(
        player: Player,
        conditionMap: Map<*, *>,
        defaultValue: List<String> = emptyList()
    ): List<String> {
        return getConditionValue(
            conditionMap, player,
            ValueType.LIST, ValueType.LIST,
            defaultValue
        ) { value, p ->
            resolveConditionValueToList(p, value, defaultValue)
        }
    }

    /**
     * 递归解析条件值为列表
     * 支持：字符串、条件判断（Map）、条件判断列表（List）
     * 字符串会被包装为单元素列表
     */
    private fun resolveConditionValueToList(player: Player, value: Any?, defaultValue: List<String>): List<String> {
        return when (value) {
            is String -> listOf(value)
            is Map<*, *> -> {
                // 检查是否为条件判断
                if (value.containsKey("condition")) {
                    // 递归处理嵌套条件判断
                    getConditionList(player, value, defaultValue)
                } else {
                    defaultValue
                }
            }
            is List<*> -> {
                // 检查是否为条件判断列表（包含 Map 元素）
                if (value.any { it is Map<*, *> }) {
                    getFirstConditionList(player, value, defaultValue)
                } else {
                    // 普通字符串列表
                    value.filterIsInstance<String>()
                }
            }
            else -> defaultValue
        }
    }

    /**
     * 获取条件值（allow/deny 支持字符串、列表或嵌套条件判断，自动检测）
     * 列表会被 \n 连接成字符串
     * 支持多层嵌套的条件判断
     */
    fun getConditionStringOrList(
        player: Player,
        conditionMap: Map<*, *>,
        defaultValue: String = ""
    ): String {
        return getConditionValue(
            conditionMap, player,
            ValueType.AUTO, ValueType.AUTO,
            defaultValue
        ) { value, p ->
            resolveConditionValueToString(p, value, defaultValue)
        }
    }

    /**
     * 递归解析条件值为字符串
     * 支持：字符串、条件判断（Map）、条件判断列表（List）
     */
    private fun resolveConditionValueToString(player: Player, value: Any?, defaultValue: String): String {
        return when (value) {
            is String -> value.replace("\\n", "\n")
            is Map<*, *> -> {
                // 检查是否为条件判断
                if (value.containsKey("condition")) {
                    // 递归处理嵌套条件判断
                    getConditionStringOrList(player, value, defaultValue)
                } else {
                    defaultValue
                }
            }
            is List<*> -> {
                // 检查是否为条件判断列表（包含 Map 元素）
                if (value.any { it is Map<*, *> }) {
                    getFirstConditionStringOrList(player, value, defaultValue)
                } else {
                    // 普通字符串列表
                    val list = value.filterIsInstance<String>()
                    if (list.isNotEmpty()) {
                        list.joinToString("\n") { it.replace("\\n", "\n") }
                    } else defaultValue
                }
            }
            else -> defaultValue
        }
    }

    /**
     * 从条件列表中获取第一个匹配条件的值
     * 遍历条件列表，返回第一个非空结果
     */
    private fun <T> getFirstMatch(
        conditions: List<*>,
        player: Player,
        defaultValue: T,
        getter: (Player, Map<*, *>, T) -> T
    ): T {
        for (condition in conditions) {
            if (condition is Map<*, *>) {
                val result = getter(player, condition, defaultValue)
                val isNonEmpty = when {
                    result === defaultValue -> false
                    result is String -> result.isNotEmpty()
                    result is Collection<*> -> result.isNotEmpty()
                    result != null -> true
                    else -> false
                }
                if (isNonEmpty) {
                    @Suppress("UNCHECKED_CAST")
                    return result
                }
            }
        }
        return defaultValue
    }

    /**
     * 从条件列表中获取字符串值
     */
    fun getFirstConditionString(
        player: Player,
        conditions: List<*>,
        defaultValue: String = ""
    ): String = getFirstMatch(conditions, player, defaultValue) { p, map, default ->
        getConditionString(p, map, default)
    }

    /**
     * 从条件列表中获取列表值
     */
    fun getFirstConditionList(
        player: Player,
        conditions: List<*>,
        defaultValue: List<String> = emptyList()
    ): List<String> = getFirstMatch(conditions, player, defaultValue) { p, map, default ->
        getConditionList(p, map, default)
    }

    /**
     * 从条件列表中获取字符串或列表值
     */
    fun getFirstConditionStringOrList(
        player: Player,
        conditions: List<*>,
        defaultValue: String = ""
    ): String = getFirstMatch(conditions, player, defaultValue) { p, map, default ->
        getConditionStringOrList(p, map, default)
    }

    // ==================== 核心：从 ConfigurationSection 读取 ====================

    /**
     * 从 ConfigurationSection 获取值（统一入口）
     * 支持：
     * 1. 简单值：直接返回并解析变量
     * 2. 列表值：如果是条件判断格式（第一个元素是 Map），则遍历条件
     * 3. 列表值：如果是普通字符串列表，则连接后返回
     *
     * @param player 玩家对象
     * @param section 配置节
     * @param path 配置路径
     * @param defaultValue 默认值
     * @param typeConverter 类型转换器
     * @return 解析后的值
     */
    private fun <T> getValue(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: T,
        typeConverter: (String, Player) -> T
    ): T {
        if (section.isList(path)) {
            val list = section.getList(path) ?: return defaultValue
            val firstItem = list.firstOrNull()

            // 检查是否为条件判断格式
            if (firstItem is Map<*, *>) {
                val result = getFirstConditionStringOrList(player, list, "")
                return typeConverter(result.ifEmpty { defaultValue.toString() }, player)
            } else {
                // 普通字符串列表，连接后返回
                val stringList = list.filterIsInstance<String>()
                if (stringList.isNotEmpty()) {
                    return typeConverter(stringList.joinToString("\n"), player)
                }
                return defaultValue
            }
        } else {
            // 简单字符串值
            val value = section.getString(path) ?: return defaultValue
            return typeConverter(value, player)
        }
    }

    /**
     * 获取字符串值
     */
    fun getString(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: String = ""
    ): String = getValue(player, section, path, defaultValue) { value, _ ->
        value.replace("\\n", "\n")
    }

    /**
     * 获取整数
     */
    fun getInt(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: Int = 0
    ): Int = getValue(player, section, path, defaultValue) { value, _ ->
        resolveVariables(player, value).toIntOrNull() ?: defaultValue
    }

    /**
     * 获取双精度浮点数
     */
    fun getDouble(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: Double = 0.0
    ): Double = getValue(player, section, path, defaultValue) { value, _ ->
        resolveVariables(player, value).toDoubleOrNull() ?: defaultValue
    }

    /**
     * 获取布尔值
     */
    fun getBoolean(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: Boolean = false
    ): Boolean = getValue(player, section, path, defaultValue) { value, _ ->
        resolveVariables(player, value).toBooleanStrictOrNull() ?: defaultValue
    }

    /**
     * 获取字符串列表
     * 支持：
     * 1. 字符串类型：将单个字符串包装为列表
     * 2. 列表类型：条件判断格式或普通字符串列表
     */
    fun getStringList(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: List<String> = emptyList()
    ): List<String> {
        if (section.isList(path)) {
            val list = section.getList(path) ?: return defaultValue
            val firstItem = list.firstOrNull()

            if (firstItem is Map<*, *>) {
                // 条件判断格式，支持嵌套条件
                val conditions = list.filterIsInstance<Map<*, *>>()
                return getFirstConditionList(player, conditions, defaultValue).map { resolveVariables(player, it) }
            } else {
                // 普通字符串列表
                return list.filterIsInstance<String>().map { resolveVariables(player, it) }
            }
        } else {
            // 非列表类型，尝试获取字符串值并转换为列表
            val value = section.getString(path)
            if (value != null && value.isNotEmpty()) {
                return listOf(resolveVariables(player, value))
            }
            return defaultValue
        }
    }

    /**
     * 获取类型值（处理 'none'）
     */
    fun getType(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: String = ""
    ): String {
        val value = getString(player, section, path, defaultValue)
        return value.ifEmpty { "none" }
    }

    // ==================== 向后兼容别名 ====================

    @Deprecated("使用 getConditionString 代替", ReplaceWith("getConditionString(player, conditionMap, defaultValue)"))
    fun getConditionalValue(
        player: Player,
        conditionMap: Map<*, *>,
        defaultValue: String = ""
    ): String = getConditionString(player, conditionMap, defaultValue)

    @Deprecated("使用 getFirstConditionString 代替", ReplaceWith("getFirstConditionString(player, conditions, defaultValue)"))
    fun getConditionalValueFromList(
        player: Player,
        conditions: List<*>,
        defaultValue: String = ""
    ): String = getFirstConditionString(player, conditions, defaultValue)

    @Deprecated("使用 getConditionStringOrList 代替", ReplaceWith("getConditionStringOrList(player, conditionMap, defaultValue)"))
    fun getConditionalValueOrList(
        player: Player,
        conditionMap: Map<*, *>,
        defaultValue: String = ""
    ): String = getConditionStringOrList(player, conditionMap, defaultValue)

    @Deprecated("使用 getFirstConditionStringOrList 代替", ReplaceWith("getFirstConditionStringOrList(player, conditions, defaultValue)"))
    fun getConditionalValueOrListFromList(
        player: Player,
        conditions: List<*>,
        defaultValue: String = ""
    ): String = getFirstConditionStringOrList(player, conditions, defaultValue)

    @Deprecated("使用 getConditionList 代替", ReplaceWith("getConditionList(player, conditionMap, defaultValue)"))
    fun getConditionalList(
        player: Player,
        conditionMap: Map<*, *>,
        defaultValue: List<String> = emptyList()
    ): List<String> = getConditionList(player, conditionMap, defaultValue)

    @Deprecated("使用 getFirstConditionList 代替", ReplaceWith("getFirstConditionList(player, conditions, defaultValue)"))
    fun getConditionalListFromList(
        player: Player,
        conditions: List<*>,
        defaultValue: List<String> = emptyList()
    ): List<String> = getFirstConditionList(player, conditions, defaultValue)

    @Deprecated("使用 getString 代替", ReplaceWith("getString(player, section, path, defaultValue)"))
    fun getConditionalValueFromSection(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: String = ""
    ): String = getString(player, section, path, defaultValue)

    @Deprecated("使用 getString 代替", ReplaceWith("getString(player, section, path, defaultValue)"))
    fun getConditionalValueOrListFromSection(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: String = ""
    ): String = getString(player, section, path, defaultValue)

    @Deprecated("使用 getInt 代替", ReplaceWith("getInt(player, section, path, defaultValue)"))
    fun getConditionalIntFromSection(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: Int = 0
    ): Int = getInt(player, section, path, defaultValue)

    @Deprecated("使用 getDouble 代替", ReplaceWith("getDouble(player, section, path, defaultValue)"))
    fun getConditionalDoubleFromSection(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: Double = 0.0
    ): Double = getDouble(player, section, path, defaultValue)

    @Deprecated("使用 getBoolean 代替", ReplaceWith("getBoolean(player, section, path, defaultValue)"))
    fun getConditionalBooleanFromSection(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: Boolean = false
    ): Boolean = getBoolean(player, section, path, defaultValue)

    @Deprecated("使用 getStringList 代替", ReplaceWith("getStringList(player, section, path, defaultValue)"))
    fun getConditionalListFromSection(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: List<String> = emptyList()
    ): List<String> = getStringList(player, section, path, defaultValue)

    @Deprecated("使用 getType 代替", ReplaceWith("getType(player, section, path, defaultValue)"))
    fun getConditionalTypeFromSection(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: String = ""
    ): String = getType(player, section, path, defaultValue)

    // ==================== 条件表达式解析（内部使用） ====================

    /**
     * 递归解析逻辑表达式（支持 && 和 ||）
     * 优先级：&& 高于 ||
     */
    private fun parseLogicalExpression(player: Player, expression: String): Boolean {
        val trimmed = expression.trim()

        // 1. 先检查是否包含 ||（优先级最低）
        val orParts = splitByOperator(trimmed, "||")
        if (orParts.size > 1) {
            val firstPart = orParts[0]
            val firstResult = parseLogicalExpression(player, firstPart)
            if (firstResult) return true // || 短路求值

            val remaining = trimmed.substring(firstPart.length).trim().removePrefix("||").trim()
            return parseLogicalExpression(player, remaining)
        }

        // 2. 再检查是否包含 &&（优先级高于 ||）
        val andParts = splitByOperator(trimmed, "&&")
        if (andParts.size > 1) {
            val firstPart = andParts[0]
            val firstResult = parseLogicalExpression(player, firstPart)
            if (!firstResult) return false // && 短路求值

            val remaining = trimmed.substring(firstPart.length).trim().removePrefix("&&").trim()
            return parseLogicalExpression(player, remaining)
        }

        // 3. 如果没有逻辑运算符，则为基本条件
        return evaluateSingleCondition(player, trimmed)
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
                    current.append(char)
                    i++
                    if (i < expression.length) current.append(expression[i])
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
                    current.append(char)
                }
                else -> {
                    if (i + operator.length <= expression.length &&
                        expression.substring(i, i + operator.length) == operator) {
                        result.add(current.toString().trim())
                        current = StringBuilder()
                        i += operator.length - 1
                    } else {
                        current.append(char)
                    }
                }
            }
            i++
        }

        if (current.isNotEmpty()) result.add(current.toString().trim())
        return result
    }

    /**
     * 评估单个条件
     */
    private fun evaluateSingleCondition(player: Player, condition: String): Boolean {
        val trimmed = condition.trim()

        // 处理内置条件方法 method.value 格式
        if (trimmed.contains(".") && !trimmed.matches(Regex("\".*\".*\\..*"))) {
            try {
                return evaluateBuiltinCondition(player, trimmed)
            } catch (_: NotBuiltinConditionException) {
                // 不是内置条件，继续普通比较
            }
        }

        // 处理括号包裹的表达式
        if (trimmed.startsWith("(") && trimmed.endsWith(")") && !trimmed.matches(Regex("\".*\".*\\(.*\\)"))) {
            val inner = trimmed.substring(1, trimmed.length - 1).trim()
            if (isCompleteParens(inner)) {
                return parseLogicalExpression(player, inner)
            }
        }

        // 匹配比较运算符
        val regex = "(>=|<=|==|!=|>|<)".toRegex()
        val match = regex.find(trimmed) ?: return false

        val op = match.value
        val parts = trimmed.split(op, limit = 2)
        val left = parseQuotedString(parts[0].trim())
        val right = parseQuotedString(parts[1].trim())

        return when (op) {
            "==" -> compareEquals(left, right)
            "!=" -> !compareEquals(left, right)
            ">"  -> (left.toDoubleOrNull() ?: 0.0) > (right.toDoubleOrNull() ?: 0.0)
            ">=" -> (left.toDoubleOrNull() ?: 0.0) >= (right.toDoubleOrNull() ?: 0.0)
            "<"  -> (left.toDoubleOrNull() ?: 0.0) < (right.toDoubleOrNull() ?: 0.0)
            "<=" -> (left.toDoubleOrNull() ?: 0.0) <= (right.toDoubleOrNull() ?: 0.0)
            else -> false
        }
    }

    /**
     * 检查括号是否成对
     */
    private fun isCompleteParens(inner: String): Boolean {
        var parenCount = 0
        var inSingleQuote = false
        var inDoubleQuote = false
        for (char in inner) {
            when (char) {
                '\\' -> continue
                '\'' if !inDoubleQuote -> inSingleQuote = !inSingleQuote
                '"' if !inSingleQuote -> inDoubleQuote = !inDoubleQuote
                '(' if !inSingleQuote && !inDoubleQuote -> parenCount++
                ')' if !inSingleQuote && !inDoubleQuote -> parenCount--
            }
            if (parenCount < 0) return false
        }
        return parenCount == 0 && !inSingleQuote && !inDoubleQuote
    }

    /**
     * 解析并执行内置条件方法
     */
    private fun evaluateBuiltinCondition(player: Player, condition: String): Boolean {
        val trimmed = condition.trim()
        val isNegative = trimmed.startsWith("!")
        val conditionWithoutNegation = if (isNegative) trimmed.substring(1).trim() else trimmed

        val dotIndex = findDotOutsideQuotes(conditionWithoutNegation)
        if (dotIndex == -1) throw NotBuiltinConditionException()

        val method = conditionWithoutNegation.take(dotIndex).trim()
        var value = conditionWithoutNegation.substring(dotIndex + 1).trim()

        // 去掉双引号
        if (value.startsWith("\"") && value.endsWith("\"") && value.length >= 2) {
            value = value.substring(1, value.length - 1)
        }

        val result = when (method) {
            "isNum" -> value.toDoubleOrNull() != null
            "isPosNum" -> value.toDoubleOrNull()?.let { it > 0 } ?: false
            "isInt" -> value.toIntOrNull() != null
            "isPosInt" -> value.toIntOrNull()?.let { it > 0 } ?: false
            "hasPerm" -> player.hasPermission(value)
            "hasMoney" -> {
                val amount = value.toDoubleOrNull() ?: return false
                checkPlayerMoney(player, amount)
            }
            "hasStockItem" -> {
                val params = value.split(";", limit = 2)
                if (params.size != 2) false
                else {
                    val itemName = params[0].trim()
                    val requiredAmount = params[1].trim().toIntOrNull() ?: 1
                    getPlayerStockItemCount(player, itemName) >= requiredAmount
                }
            }
            "hasItem" -> {
                if (!value.startsWith("[") || !value.endsWith("]")) false
                else checkPlayerHasItem(player, value.substring(1, value.length - 1).trim())
            }
            else -> throw NotBuiltinConditionException()
        }

        return if (isNegative) !result else result
    }

    /**
     * 在引号外查找点号位置
     */
    private fun findDotOutsideQuotes(str: String): Int {
        var inQuote = false
        var i = 0
        while (i < str.length) {
            when (str[i]) {
                '\\' -> i++ // 跳过转义字符
                '"' -> inQuote = !inQuote
                '.' if !inQuote -> return i
            }
            i++
        }
        return -1
    }

    /**
     * 检查玩家是否有足够的金币
     */
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

    // ==================== 物品检查（供外部使用） ====================

    /**
     * 获取玩家背包中指定存储库物品的数量
     */
    fun getPlayerStockItemCount(player: Player, itemName: String): Int {
        if (plugin == null) return 0
        val savedItem = plugin!!.itemManager.getItem(itemName) ?: return 0

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

    /**
     * 检查玩家背包中是否有足够的物品
     */
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

    /**
     * 获取玩家背包中符合条件的物品数量
     */
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
                        val plainText = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                            .serialize(line)
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

    /**
     * 解析引号包裹的字符串
     */
    private fun parseQuotedString(str: String): String {
        val trimmed = str.trim()

        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length >= 2) {
            val content = trimmed.substring(1, trimmed.length - 1)
            if (!content.contains("\"")) return content
        }

        if (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length >= 2) {
            val content = trimmed.substring(1, trimmed.length - 1)
            if (!content.contains("'")) return content
        }

        return trimmed
    }

    /**
     * 比较两个值是否相等（支持数值和字符串）
     */
    private fun compareEquals(left: String, right: String): Boolean {
        val leftNum = left.toDoubleOrNull()
        val rightNum = right.toDoubleOrNull()

        if (leftNum != null && rightNum != null) return leftNum == rightNum

        val leftBool = parseBoolean(left)
        val rightBool = parseBoolean(right)

        if (leftBool != null && rightBool != null) return leftBool == rightBool

        return left.equals(right, ignoreCase = true)
    }

    /**
     * 解析布尔值
     */
    private fun parseBoolean(value: String): Boolean? = when (value.trim().lowercase()) {
        "true", "yes", "1" -> true
        "false", "no", "0" -> false
        else -> null
    }
}
