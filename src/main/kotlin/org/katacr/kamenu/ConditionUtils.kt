package org.katacr.kamenu

import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * 条件判断工具类
 * 支持复杂的逻辑表达式、PAPI 变量和多种比较运算符
 */
object ConditionUtils {

    /**
     * 动作执行器接口
     * 用于在条件判断后执行相应的动作
     */
    fun interface ActionExecutor {
        fun execute(action: String)
    }

    /**
     * 解析并检查条件字符串 (支持 &&, || 复合条件)
     * 例如: "%player_is_op% == true && %player_level% >= 10"
     */
    fun checkCondition(player: Player, condition: String?): Boolean {
        if (condition == null || condition.isBlank()) return true

        // 先进行 PAPI 变量替换
        var processed = condition
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                processed = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, processed)
            } catch (e: Exception) {
                // PAPI 解析失败，忽略
            }
        }

        // 解析逻辑表达式（支持 && 和 ||）
        return parseLogicalExpression(processed)
    }

    /**
     * 执行条件判断动作
     * 格式:
     *   - condition: "%player_is_op% == true"
     *     meet:
     *       - 'open: manage'
     *       - 'tell: 管理员专属'
     *     deny:
     *       - 'tell: 没有权限'
     *
     * @param player 玩家对象
     * @param conditionMap 条件映射，包含 condition、meet、deny 键
     * @param executor 动作执行器，用于执行具体的动作
     */
    fun executeConditionalAction(
        player: Player,
        conditionMap: Map<*, *>,
        executor: ActionExecutor
    ) {
        val condition = conditionMap["condition"] as? String ?: return
        val meetActions = conditionMap["meet"] as? List<*> ?: emptyList<Any>()
        val denyActions = conditionMap["deny"] as? List<*> ?: emptyList<Any>()

        // 检查条件
        val conditionMet = checkCondition(player, condition)

        // 根据条件结果执行相应的动作列表
        val actionsToExecute = if (conditionMet) meetActions else denyActions

        // 执行动作列表
        for (action in actionsToExecute) {
            if (action is String) {
                executor.execute(action)
            }
        }
    }

    /**
     * 获取条件值（支持条件判断的单值返回）
     * 格式:
     *   - condition: "%player_is_op% == true"
     *     meet: "管理员专属文本"
     *     deny: "普通玩家文本"
     *
     * @param player 玩家对象
     * @param conditionMap 条件映射，包含 condition、meet、deny 键
     * @return 条件满足时的 meet 值，否则返回 deny 值
     */
    fun getConditionalValue(
        player: Player,
        conditionMap: Map<*, *>,
        defaultValue: String = ""
    ): String {
        val condition = conditionMap["condition"] as? String ?: return defaultValue
        val meet = conditionMap["meet"] as? String ?: defaultValue
        val deny = conditionMap["deny"] as? String ?: defaultValue

        // 检查条件并返回相应的值
        return if (checkCondition(player, condition)) {
            meet
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
     * 递归解析逻辑表达式（支持 && 和 ||）
     * 优先级：&& 高于 ||
     */
    private fun parseLogicalExpression(expression: String): Boolean {
        val trimmed = expression.trim()

        // 1. 先检查是否包含 ||（优先级最低）
        val orParts = splitByOperator(trimmed, "||")
        if (orParts.size > 1) {
            // 如果有 ||，先解析第一部分，如果为 true 则直接返回 true（短路求值）
            val firstPart = orParts[0]
            val firstResult = parseLogicalExpression(firstPart)
            if (firstResult) return true // || 短路求值

            // 否则继续解析剩余部分
            val remaining = trimmed.substring(firstPart.length).trim().removePrefix("||").trim()
            return parseLogicalExpression(remaining)
        }

        // 2. 再检查是否包含 &&（优先级高于 ||）
        val andParts = splitByOperator(trimmed, "&&")
        if (andParts.size > 1) {
            // 如果有 &&，先解析第一部分，如果为 false 则直接返回 false（短路求值）
            val firstPart = andParts[0]
            val firstResult = parseLogicalExpression(firstPart)
            if (!firstResult) return false // && 短路求值

            // 否则继续解析剩余部分
            val remaining = trimmed.substring(firstPart.length).trim().removePrefix("&&").trim()
            return parseLogicalExpression(remaining)
        }

        // 3. 如果没有逻辑运算符，则为基本条件
        return evaluateSingleCondition(trimmed)
    }

    /**
     * 按运算符分割表达式（考虑括号和优先级）
     */
    private fun splitByOperator(expression: String, operator: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var parenDepth = 0
        var i = 0

        while (i < expression.length) {
            val char = expression[i]

            when {
                char == '(' -> {
                    parenDepth++
                    current.append(char)
                }
                char == ')' -> {
                    parenDepth--
                    current.append(char)
                }
                parenDepth > 0 -> {
                    // 括号内不分割
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
     * 评估单个条件（如 "5 >= 3"）
     */
    private fun evaluateSingleCondition(condition: String): Boolean {
        val trimmed = condition.trim()

        // 如果是括号包裹的表达式，去掉括号后递归解析
        if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
            val inner = trimmed.substring(1, trimmed.length - 1).trim()
            // 确保括号是成对的
            var parenCount = 0
            var isCompletePair = true
            for (char in inner) {
                when (char) {
                    '(' -> parenCount++
                    ')' -> parenCount--
                }
                if (parenCount < 0) {
                    isCompletePair = false
                    break
                }
            }
            if (isCompletePair && parenCount == 0) {
                return parseLogicalExpression(inner)
            }
        }

        // 匹配比较运算符
        val regex = "(>=|<=|==|!=|>|<)".toRegex()
        val match = regex.find(trimmed) ?: return false

        val op = match.value
        val parts = trimmed.split(op, limit = 2)
        val left = parts[0].trim()
        val right = parts[1].trim()

        return when (op) {
            "==" -> left.equals(right, ignoreCase = true)
            "!=" -> !left.equals(right, ignoreCase = true)
            ">"  -> left.toDoubleDefault(0.0) > right.toDoubleDefault(0.0)
            ">=" -> left.toDoubleDefault(0.0) >= right.toDoubleDefault(0.0)
            "<"  -> left.toDoubleDefault(0.0) < right.toDoubleDefault(0.0)
            "<=" -> left.toDoubleDefault(0.0) <= right.toDoubleDefault(0.0)
            else -> false
        }
    }

    private fun String.toDoubleDefault(default: Double): Double = this.toDoubleOrNull() ?: default
}
