package org.katacr.kamenu

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

/**
 * 条件判断门面
 * 保留旧 API 名称，内部委托到专门的解析器和求值器。
 */
object ConditionUtils {
    fun setLanguageManager(manager: LanguageManager) {
        TextResolver.setLanguageManager(manager)
    }

    fun setPlugin(kamenu: KaMenu) {
        ConditionExpressionEngine.setPlugin(kamenu)
        TextResolver.setPlugin(kamenu)
    }

    fun resolveVariables(player: Player, text: String): String {
        return TextResolver.resolve(player, text)
    }

    fun checkCondition(player: Player, condition: String?): Boolean {
        return ConditionExpressionEngine.checkCondition(player, condition)
    }

    fun getConditionString(
        player: Player,
        conditionMap: Map<*, *>,
        defaultValue: String = ""
    ): String = ConditionValueResolver.getConditionString(player, conditionMap, defaultValue)

    fun getConditionList(
        player: Player,
        conditionMap: Map<*, *>,
        defaultValue: List<String> = emptyList()
    ): List<String> = ConditionValueResolver.getConditionList(player, conditionMap, defaultValue)

    fun getConditionStringOrList(
        player: Player,
        conditionMap: Map<*, *>,
        defaultValue: String = ""
    ): String = ConditionValueResolver.getConditionStringOrList(player, conditionMap, defaultValue)

    fun getFirstConditionString(
        player: Player,
        conditions: List<*>,
        defaultValue: String = ""
    ): String = ConditionValueResolver.getFirstConditionString(player, conditions, defaultValue)

    fun getFirstConditionList(
        player: Player,
        conditions: List<*>,
        defaultValue: List<String> = emptyList()
    ): List<String> = ConditionValueResolver.getFirstConditionList(player, conditions, defaultValue)

    fun getFirstConditionStringOrList(
        player: Player,
        conditions: List<*>,
        defaultValue: String = ""
    ): String = ConditionValueResolver.getFirstConditionStringOrList(player, conditions, defaultValue)

    fun getString(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: String = ""
    ): String = ConditionValueResolver.getString(player, section, path, defaultValue)

    fun getInt(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: Int = 0
    ): Int = ConditionValueResolver.getInt(player, section, path, defaultValue)

    fun getDouble(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: Double = 0.0
    ): Double = ConditionValueResolver.getDouble(player, section, path, defaultValue)

    fun getBoolean(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: Boolean = false
    ): Boolean = ConditionValueResolver.getBoolean(player, section, path, defaultValue)

    fun getStringList(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: List<String> = emptyList()
    ): List<String> = ConditionValueResolver.getStringList(player, section, path, defaultValue)

    fun getType(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultValue: String = ""
    ): String = ConditionValueResolver.getType(player, section, path, defaultValue)

    fun getPlayerStockItemCount(player: Player, itemName: String): Int {
        return ConditionExpressionEngine.getPlayerStockItemCount(player, itemName)
    }

    fun getPlayerItemCount(player: Player, paramsStr: String): Int {
        return ConditionExpressionEngine.getPlayerItemCount(player, paramsStr)
    }

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
}
