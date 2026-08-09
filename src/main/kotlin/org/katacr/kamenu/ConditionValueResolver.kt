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
                if (isConditionCandidateList(value)) {
                    getFirstConditionList(player, value, defaultValue, menuConfig)
                } else {
                    resolveOrderedStringEntries(player, value, defaultValue, menuConfig)
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
                if (isConditionCandidateList(value)) {
                    getFirstConditionStringOrList(player, value, defaultValue, menuConfig)
                } else {
                    val list = resolveOrderedStringEntries(player, value, emptyList(), menuConfig)
                    if (list.isNotEmpty()) {
                        list.joinToString("\n") { it.replace("\\n", "\n") }
                    } else defaultValue
                }
            }
            else -> defaultValue
        }
    }

    /** 判断列表是否完全由条件候选组成；这种列表继续使用首个非空分支语义。 */
    private fun isConditionCandidateList(values: List<*>): Boolean {
        return values.isNotEmpty() && values.all { value ->
            value is Map<*, *> && value.containsKey("condition")
        }
    }

    /**
     * 按 YAML 顺序展开静态字符串、条件分支和嵌套列表。
     *
     * 纯条件子列表仍按首个非空分支处理，混合列表中的条件结果则插入其声明位置。
     */
    private fun resolveOrderedStringEntries(
        player: Player,
        values: List<*>,
        defaultValue: List<String>,
        menuConfig: YamlConfiguration?
    ): List<String> {
        if (isConditionCandidateList(values)) {
            return getFirstConditionList(player, values, defaultValue, menuConfig)
        }

        val resolved = buildList {
            values.forEach { value ->
                when (value) {
                    is String -> add(value)
                    is Map<*, *> -> if (value.containsKey("condition")) {
                        addAll(getConditionList(player, value, emptyList(), menuConfig))
                    }
                    is List<*> -> addAll(resolveOrderedStringEntries(player, value, emptyList(), menuConfig))
                }
            }
        }
        return resolved.ifEmpty { defaultValue }
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
            if (isConditionCandidateList(list)) {
                val result = getFirstConditionStringOrList(player, list, "", menuConfig)
                return converter(result.ifEmpty { defaultValue.toString() }, player, menuConfig)
            }
            val stringList = resolveOrderedStringEntries(player, list, emptyList(), menuConfig)
            return if (stringList.isNotEmpty()) {
                converter(stringList.joinToString("\n"), player, menuConfig)
            } else {
                defaultValue
            }
        }

        val value = section.getString(path) ?: return defaultValue
        return converter(value, player, menuConfig)
    }

    /** 读取带组件变量的字段，并保持混合条件列表的原有声明顺序。 */
    private fun <T> readSectionValue(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: T,
        variables: Map<String, String>,
        converter: (String, Player, YamlConfiguration?) -> T
    ): T {
        val menuConfig = section.root as? YamlConfiguration
        val raw = section.get(path) ?: return defaultValue
        val entries = resolveContextualEntries(player, raw, menuConfig, variables)
        if (entries.isEmpty()) return defaultValue
        return converter(entries.joinToString("\n"), player, menuConfig)
    }

    /** 在组件上下文中递归展开静态值、条件分支和列表。 */
    private fun resolveContextualEntries(
        player: Player,
        value: Any?,
        menuConfig: YamlConfiguration?,
        variables: Map<String, String>
    ): List<String> = when (value) {
        null -> emptyList()
        is String, is Number, is Boolean -> listOf(value.toString())
        is ConfigurationSection -> resolveContextualEntries(
            player,
            value.getValues(false),
            menuConfig,
            variables
        )
        is Map<*, *> -> {
            val condition = value["condition"]?.toString()
            if (condition == null) {
                emptyList()
            } else {
                val branch = if (ConditionExpressionEngine.checkCondition(
                        player,
                        condition,
                        variables,
                        menuConfig
                    ) { null }
                ) {
                    value["allow"] ?: value["actions"]
                } else {
                    value["deny"]
                }
                resolveContextualEntries(player, branch, menuConfig, variables)
            }
        }
        is List<*> -> {
            if (isConditionCandidateList(value)) {
                value.firstNotNullOfOrNull { candidate ->
                    resolveContextualEntries(player, candidate, menuConfig, variables).takeIf(List<String>::isNotEmpty)
                }.orEmpty()
            } else {
                value.flatMap { entry -> resolveContextualEntries(player, entry, menuConfig, variables) }
            }
        }
        else -> emptyList()
    }

    fun getString(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: String = ""
    ): String = readSectionValue(player, section, path, defaultValue) { value, _, menuConfig ->
        TextResolver.resolve(player, value, menuConfig = menuConfig).replace("\\n", "\n")
    }

    /** 读取字符串并注入当前组件变量，供 `{self:*}` 等上下文引用使用。 */
    fun getString(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: String,
        variables: Map<String, String>
    ): String = readSectionValue(player, section, path, defaultValue, variables) { value, _, menuConfig ->
        TextResolver.resolve(player, value, variables, menuConfig).replace("\\n", "\n")
    }

    fun getInt(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: Int = 0
    ): Int = readSectionValue(player, section, path, defaultValue) { value, _, menuConfig ->
        TextResolver.resolve(player, value, menuConfig = menuConfig).toIntOrNull() ?: defaultValue
    }

    /** 读取带当前组件变量的整数。 */
    fun getInt(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: Int,
        variables: Map<String, String>
    ): Int = readSectionValue(player, section, path, defaultValue, variables) { value, _, menuConfig ->
        TextResolver.resolve(player, value, variables, menuConfig).toIntOrNull() ?: defaultValue
    }

    fun getDouble(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: Double = 0.0
    ): Double = readSectionValue(player, section, path, defaultValue) { value, _, menuConfig ->
        TextResolver.resolve(player, value, menuConfig = menuConfig).toDoubleOrNull() ?: defaultValue
    }

    /** 读取带当前组件变量的浮点数。 */
    fun getDouble(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: Double,
        variables: Map<String, String>
    ): Double = readSectionValue(player, section, path, defaultValue, variables) { value, _, menuConfig ->
        TextResolver.resolve(player, value, variables, menuConfig).toDoubleOrNull() ?: defaultValue
    }

    fun getBoolean(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: Boolean = false
    ): Boolean = readSectionValue(player, section, path, defaultValue) { value, _, menuConfig ->
        TextResolver.resolve(player, value, menuConfig = menuConfig).toBooleanStrictOrNull() ?: defaultValue
    }

    /** 读取带当前组件变量的布尔值。 */
    fun getBoolean(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: Boolean,
        variables: Map<String, String>
    ): Boolean = readSectionValue(player, section, path, defaultValue, variables) { value, _, menuConfig ->
        TextResolver.resolve(player, value, variables, menuConfig).toBooleanStrictOrNull() ?: defaultValue
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
            return if (isConditionCandidateList(list)) {
                getFirstConditionList(player, list, defaultValue, menuConfig)
                    .map { TextResolver.resolve(player, it, menuConfig = menuConfig) }
            } else {
                resolveOrderedStringEntries(player, list, defaultValue, menuConfig)
                    .map { TextResolver.resolve(player, it, menuConfig = menuConfig) }
            }
        }

        val value = section.getString(path)
        return if (!value.isNullOrEmpty()) {
            listOf(TextResolver.resolve(player, value, menuConfig = menuConfig))
        } else {
            defaultValue
        }
    }

    /** 读取字符串列表并为每一行注入当前组件变量。 */
    fun getStringList(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: List<String>,
        variables: Map<String, String>
    ): List<String> {
        val menuConfig = section.root as? YamlConfiguration
        val raw = section.get(path) ?: return defaultValue
        return resolveContextualEntries(player, raw, menuConfig, variables)
            .map { TextResolver.resolve(player, it, variables, menuConfig) }
            .ifEmpty { defaultValue }
    }

    /** 先判断字符串行尾条件，再解析保留正文中的变量，避免变量内容改变条件表达式结构。 */
    fun getInlineConditionalStringList(
        player: Player,
        section: ConfigurationSection,
        path: String,
        variables: Map<String, String> = emptyMap(),
        dynamicResolver: (String) -> String? = { null }
    ): List<String> {
        val menuConfig = section.root as? YamlConfiguration
        val raw = section.get(path) ?: return emptyList()
        return resolveContextualEntries(player, raw, menuConfig, variables)
            .mapNotNull { InlineConditionResolver.resolve(player, it, variables, menuConfig, dynamicResolver) }
            .map { TextResolver.resolve(player, it, variables, dynamicResolver, menuConfig) }
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

    /** 读取带当前组件变量的类型字段。 */
    fun getType(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: String,
        variables: Map<String, String>
    ): String {
        val value = getString(player, section, path, defaultValue, variables)
        return value.ifEmpty { "none" }
    }
}
