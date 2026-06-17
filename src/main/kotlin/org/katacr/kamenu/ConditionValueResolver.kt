package org.katacr.kamenu

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

/**
 * 条件值解析器
 * 负责 condition/allow/deny 和 ConfigurationSection 的条件化读取。
 */
object ConditionValueResolver {
    enum class ValueType { STRING, LIST, AUTO }

    private fun <T> getConditionValue(
        conditionMap: Map<*, *>,
        player: Player,
        defaultValue: T,
        converter: (Any, Player) -> T
    ): T {
        val condition = conditionMap["condition"] as? String ?: return defaultValue
        val allow = conditionMap["allow"]
        val deny = conditionMap["deny"]

        val result = if (ConditionExpressionEngine.checkCondition(player, condition)) {
            if (allow != null) converter(allow, player) else defaultValue
        } else {
            if (deny != null) converter(deny, player) else defaultValue
        }

        return result ?: defaultValue
    }

    fun getConditionString(
        player: Player,
        conditionMap: Map<*, *>,
        defaultValue: String = ""
    ): String = getConditionValue(conditionMap, player, defaultValue) { value, _ ->
        (value as? String)?.replace("\\n", "\n") ?: defaultValue
    }

    fun getConditionList(
        player: Player,
        conditionMap: Map<*, *>,
        defaultValue: List<String> = emptyList()
    ): List<String> = getConditionValue(conditionMap, player, defaultValue) { value, p ->
        resolveConditionValueToList(p, value, defaultValue)
    }

    fun getConditionStringOrList(
        player: Player,
        conditionMap: Map<*, *>,
        defaultValue: String = ""
    ): String = getConditionValue(conditionMap, player, defaultValue) { value, p ->
        resolveConditionValueToString(p, value, defaultValue)
    }

    private fun resolveConditionValueToList(player: Player, value: Any?, defaultValue: List<String>): List<String> {
        return when (value) {
            is String -> listOf(value)
            is Map<*, *> -> if (value.containsKey("condition")) {
                getConditionList(player, value, defaultValue)
            } else {
                defaultValue
            }
            is List<*> -> {
                if (value.any { it is Map<*, *> }) {
                    getFirstConditionList(player, value, defaultValue)
                } else {
                    value.filterIsInstance<String>()
                }
            }
            else -> defaultValue
        }
    }

    private fun resolveConditionValueToString(player: Player, value: Any?, defaultValue: String): String {
        return when (value) {
            is String -> value.replace("\\n", "\n")
            is Map<*, *> -> if (value.containsKey("condition")) {
                getConditionStringOrList(player, value, defaultValue)
            } else {
                defaultValue
            }
            is List<*> -> {
                if (value.any { it is Map<*, *> }) {
                    getFirstConditionStringOrList(player, value, defaultValue)
                } else {
                    val list = value.filterIsInstance<String>()
                    if (list.isNotEmpty()) {
                        list.joinToString("\n") { it.replace("\\n", "\n") }
                    } else defaultValue
                }
            }
            else -> defaultValue
        }
    }

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

    fun getFirstConditionString(
        player: Player,
        conditions: List<*>,
        defaultValue: String = ""
    ): String = getFirstMatch(conditions, player, defaultValue) { p, map, default ->
        getConditionString(p, map, default)
    }

    fun getFirstConditionList(
        player: Player,
        conditions: List<*>,
        defaultValue: List<String> = emptyList()
    ): List<String> = getFirstMatch(conditions, player, defaultValue) { p, map, default ->
        getConditionList(p, map, default)
    }

    fun getFirstConditionStringOrList(
        player: Player,
        conditions: List<*>,
        defaultValue: String = ""
    ): String = getFirstMatch(conditions, player, defaultValue) { p, map, default ->
        getConditionStringOrList(p, map, default)
    }

    private fun <T> readSectionValue(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: T,
        converter: (String, Player) -> T
    ): T {
        if (section.isList(path)) {
            val list = section.getList(path) ?: return defaultValue
            val firstItem = list.firstOrNull()
            if (firstItem is Map<*, *>) {
                val result = getFirstConditionStringOrList(player, list, "")
                return converter(result.ifEmpty { defaultValue.toString() }, player)
            }
            val stringList = list.filterIsInstance<String>()
            return if (stringList.isNotEmpty()) {
                converter(stringList.joinToString("\n"), player)
            } else {
                defaultValue
            }
        }

        val value = section.getString(path) ?: return defaultValue
        return converter(value, player)
    }

    fun getString(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: String = ""
    ): String = readSectionValue(player, section, path, defaultValue) { value, _ ->
        TextResolver.resolve(player, value).replace("\\n", "\n")
    }

    fun getInt(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: Int = 0
    ): Int = readSectionValue(player, section, path, defaultValue) { value, _ ->
        TextResolver.resolve(player, value).toIntOrNull() ?: defaultValue
    }

    fun getDouble(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: Double = 0.0
    ): Double = readSectionValue(player, section, path, defaultValue) { value, _ ->
        TextResolver.resolve(player, value).toDoubleOrNull() ?: defaultValue
    }

    fun getBoolean(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: Boolean = false
    ): Boolean = readSectionValue(player, section, path, defaultValue) { value, _ ->
        TextResolver.resolve(player, value).toBooleanStrictOrNull() ?: defaultValue
    }

    fun getStringList(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: List<String> = emptyList()
    ): List<String> {
        if (section.isList(path)) {
            val list = section.getList(path) ?: return defaultValue
            val firstItem = list.firstOrNull()
            return if (firstItem is Map<*, *>) {
                val conditions = list.filterIsInstance<Map<*, *>>()
                getFirstConditionList(player, conditions, defaultValue).map { TextResolver.resolve(player, it) }
            } else {
                list.filterIsInstance<String>().map { TextResolver.resolve(player, it) }
            }
        }

        val value = section.getString(path)
        return if (!value.isNullOrEmpty()) {
            listOf(TextResolver.resolve(player, value))
        } else {
            defaultValue
        }
    }

    fun getType(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: String = ""
    ): String {
        val value = getString(player, section, path, defaultValue)
        return value.ifEmpty { "none" }
    }
}
