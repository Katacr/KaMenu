package org.katacr.kamenu

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player

/**
 * 条件值解析器。
 *
 * 用于读取 YAML 中既可以是普通值、列表，也可以是条件分支的配置项。
 * 典型结构：
 *
 * `condition: hasPerm.vip`
 * `allow: "&aVIP"`
 * `deny: "&7普通玩家"`
 *
 * MenuUI 读取 Title、Body、Bottom 等配置时会通过这里统一处理条件化值。
 */
object ConditionValueResolver {
    /**
     * 读取值时的期望类型。
     *
     * AUTO 用于兼容“字符串或列表都允许”的老配置。
     */
    enum class ValueType { STRING, LIST, AUTO }

    private fun <T> getConditionValue(
        conditionMap: Map<*, *>,
        player: Player,
        defaultValue: T,
        menuConfig: YamlConfiguration?,
        converter: (Any, Player) -> T
    ): T {
        val condition = conditionMap["condition"] as? String ?: return defaultValue
        val allow = conditionMap["allow"]
        val deny = conditionMap["deny"]

        val result = if (ConditionExpressionEngine.checkCondition(player, condition, emptyMap(), menuConfig) { null }) {
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
    ): String = getConditionString(player, conditionMap, defaultValue, null)

    /** 解析条件字符串，并保留当前菜单的 JavaScript 包上下文。 */
    fun getConditionString(
        player: Player,
        conditionMap: Map<*, *>,
        defaultValue: String,
        menuConfig: YamlConfiguration?
    ): String = getConditionValue(conditionMap, player, defaultValue, menuConfig) { value, _ ->
        (value as? String)?.replace("\\n", "\n") ?: defaultValue
    }

    fun getConditionList(
        player: Player,
        conditionMap: Map<*, *>,
        defaultValue: List<String> = emptyList()
    ): List<String> = getConditionList(player, conditionMap, defaultValue, null)

    /** 解析条件字符串列表，并保留当前菜单的 JavaScript 包上下文。 */
    fun getConditionList(
        player: Player,
        conditionMap: Map<*, *>,
        defaultValue: List<String>,
        menuConfig: YamlConfiguration?
    ): List<String> = getConditionValue(conditionMap, player, defaultValue, menuConfig) { value, p ->
        resolveConditionValueToList(p, value, defaultValue, menuConfig)
    }

    fun getConditionStringOrList(
        player: Player,
        conditionMap: Map<*, *>,
        defaultValue: String = ""
    ): String = getConditionStringOrList(player, conditionMap, defaultValue, null)

    /** 解析字符串或列表条件值，并保留当前菜单的 JavaScript 包上下文。 */
    fun getConditionStringOrList(
        player: Player,
        conditionMap: Map<*, *>,
        defaultValue: String,
        menuConfig: YamlConfiguration?
    ): String = getConditionValue(conditionMap, player, defaultValue, menuConfig) { value, p ->
        resolveConditionValueToString(p, value, defaultValue, menuConfig)
    }

    private fun resolveConditionValueToList(
        player: Player,
        value: Any?,
        defaultValue: List<String>,
        menuConfig: YamlConfiguration?
    ): List<String> {
        return when (value) {
            is String -> listOf(value)
            is Map<*, *> -> if (value.containsKey("condition")) {
                getConditionList(player, value, defaultValue, menuConfig)
            } else {
                defaultValue
            }
            is List<*> -> {
                if (value.any { it is Map<*, *> }) {
                    getFirstConditionList(player, value, defaultValue, menuConfig)
                } else {
                    value.filterIsInstance<String>()
                }
            }
            else -> defaultValue
        }
    }

    private fun resolveConditionValueToString(
        player: Player,
        value: Any?,
        defaultValue: String,
        menuConfig: YamlConfiguration?
    ): String {
        return when (value) {
            is String -> value.replace("\\n", "\n")
            is Map<*, *> -> if (value.containsKey("condition")) {
                getConditionStringOrList(player, value, defaultValue, menuConfig)
            } else {
                defaultValue
            }
            is List<*> -> {
                if (value.any { it is Map<*, *> }) {
                    getFirstConditionStringOrList(player, value, defaultValue, menuConfig)
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
    ): String = getFirstConditionString(player, conditions, defaultValue, null)

    /** 选择首个匹配字符串，并保留当前菜单的 JavaScript 包上下文。 */
    fun getFirstConditionString(
        player: Player,
        conditions: List<*>,
        defaultValue: String,
        menuConfig: YamlConfiguration?
    ): String = getFirstMatch(conditions, player, defaultValue) { p, map, default ->
        getConditionString(p, map, default, menuConfig)
    }

    fun getFirstConditionList(
        player: Player,
        conditions: List<*>,
        defaultValue: List<String> = emptyList()
    ): List<String> = getFirstConditionList(player, conditions, defaultValue, null)

    /** 选择首个匹配字符串列表，并保留当前菜单的 JavaScript 包上下文。 */
    fun getFirstConditionList(
        player: Player,
        conditions: List<*>,
        defaultValue: List<String>,
        menuConfig: YamlConfiguration?
    ): List<String> = getFirstMatch(conditions, player, defaultValue) { p, map, default ->
        getConditionList(p, map, default, menuConfig)
    }

    fun getFirstConditionStringOrList(
        player: Player,
        conditions: List<*>,
        defaultValue: String = ""
    ): String = getFirstConditionStringOrList(player, conditions, defaultValue, null)

    /** 选择首个匹配字符串或列表，并保留当前菜单的 JavaScript 包上下文。 */
    fun getFirstConditionStringOrList(
        player: Player,
        conditions: List<*>,
        defaultValue: String,
        menuConfig: YamlConfiguration?
    ): String = getFirstMatch(conditions, player, defaultValue) { p, map, default ->
        getConditionStringOrList(p, map, default, menuConfig)
    }

    private fun <T> readSectionValue(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: T,
        converter: (String, Player, YamlConfiguration?) -> T
    ): T {
        val menuConfig = section.root as? YamlConfiguration
        if (section.isList(path)) {
            val list = section.getList(path) ?: return defaultValue
            val firstItem = list.firstOrNull()
            if (firstItem is Map<*, *>) {
                val result = getFirstConditionStringOrList(player, list, "", menuConfig)
                return converter(result.ifEmpty { defaultValue.toString() }, player, menuConfig)
            }
            val stringList = list.filterIsInstance<String>()
            return if (stringList.isNotEmpty()) {
                converter(stringList.joinToString("\n"), player, menuConfig)
            } else {
                defaultValue
            }
        }

        val value = section.getString(path) ?: return defaultValue
        return converter(value, player, menuConfig)
    }

    fun getString(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: String = ""
    ): String = readSectionValue(player, section, path, defaultValue) { value, _, menuConfig ->
        TextResolver.resolve(player, value, menuConfig = menuConfig).replace("\\n", "\n")
    }

    fun getInt(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: Int = 0
    ): Int = readSectionValue(player, section, path, defaultValue) { value, _, menuConfig ->
        TextResolver.resolve(player, value, menuConfig = menuConfig).toIntOrNull() ?: defaultValue
    }

    fun getDouble(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: Double = 0.0
    ): Double = readSectionValue(player, section, path, defaultValue) { value, _, menuConfig ->
        TextResolver.resolve(player, value, menuConfig = menuConfig).toDoubleOrNull() ?: defaultValue
    }

    fun getBoolean(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: Boolean = false
    ): Boolean = readSectionValue(player, section, path, defaultValue) { value, _, menuConfig ->
        TextResolver.resolve(player, value, menuConfig = menuConfig).toBooleanStrictOrNull() ?: defaultValue
    }

    fun getStringList(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: List<String> = emptyList()
    ): List<String> {
        val menuConfig = section.root as? YamlConfiguration
        if (section.isList(path)) {
            val list = section.getList(path) ?: return defaultValue
            val firstItem = list.firstOrNull()
            return if (firstItem is Map<*, *>) {
                val conditions = list.filterIsInstance<Map<*, *>>()
                getFirstConditionList(player, conditions, defaultValue, menuConfig)
                    .map { TextResolver.resolve(player, it, menuConfig = menuConfig) }
            } else {
                list.filterIsInstance<String>().map { TextResolver.resolve(player, it, menuConfig = menuConfig) }
            }
        }

        val value = section.getString(path)
        return if (!value.isNullOrEmpty()) {
            listOf(TextResolver.resolve(player, value, menuConfig = menuConfig))
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
