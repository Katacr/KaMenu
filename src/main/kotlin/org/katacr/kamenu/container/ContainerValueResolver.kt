package org.katacr.kamenu.container

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.katacr.kamenu.ConditionUtils
import org.katacr.kamenu.InlineConditionResolver
import org.katacr.kamenu.TextResolver

/**
 * 将加载阶段冻结的 [ContainerConfigValue] 解析为指定玩家当前可见的运行时值。
 *
 * 字符串叶节点统一经过 TextResolver；包含 `condition/allow/deny` 的 Map 通过现有条件引擎选择分支。
 */
class ContainerValueResolver(
    private val player: Player,
    private val config: YamlConfiguration,
    private val variables: Map<String, String> = emptyMap()
) {

    /** 解析任意冻结值，并保留普通 Map/List 的结构。 */
    fun resolve(value: ContainerConfigValue?): Any? {
        return resolveValue(value, resolveText = true)
    }

    /** 先判断 Lore 行尾条件，再解析保留正文中的变量。 */
    fun inlineConditionalStrings(value: ContainerConfigValue?): List<String> {
        return flatten(resolveValue(value, resolveText = false))
            .map(Any::toString)
            .mapNotNull { InlineConditionResolver.resolve(player, it, variables, config) }
            .map { TextResolver.resolve(player, it, variables, menuConfig = config).replace("\\n", "\n") }
    }

    /** 按调用场景选择保留原始字符串或立即解析文本变量。 */
    private fun resolveValue(value: ContainerConfigValue?, resolveText: Boolean): Any? {
        return when (value) {
            null, ContainerConfigValue.Null -> null
            is ContainerConfigValue.Scalar -> resolveScalar(value.value, resolveText)
            is ContainerConfigValue.Mapping -> resolveMapping(value, resolveText)
            is ContainerConfigValue.Sequence -> resolveSequence(value, resolveText)
        }
    }

    /** 将值解析为字符串；列表会使用换行连接。 */
    fun string(value: ContainerConfigValue?, defaultValue: String = ""): String {
        return when (val resolved = resolve(value)) {
            null -> defaultValue
            is List<*> -> resolved.filterNotNull().joinToString("\n")
            else -> resolved.toString()
        }
    }

    /** 将值解析为字符串列表，供标题帧、Lore 和物品标志使用。 */
    fun strings(value: ContainerConfigValue?): List<String> {
        return flatten(resolve(value)).map(Any::toString)
    }

    /** 将动态值解析为整数，解析失败时返回默认值。 */
    fun int(value: ContainerConfigValue?, defaultValue: Int): Int {
        return string(value).trim().toIntOrNull() ?: defaultValue
    }

    /** 将动态值解析为布尔值，并兼容 true、yes、1。 */
    fun boolean(value: ContainerConfigValue?, defaultValue: Boolean = false): Boolean {
        return when (string(value).trim().lowercase()) {
            "true", "yes", "1" -> true
            "false", "no", "0" -> false
            else -> defaultValue
        }
    }

    /** 将附魔配置解析为键到整数等级的映射，同时兼容 Map 与 `key:level` 列表。 */
    fun integerMap(value: ContainerConfigValue?): Map<String, Int> {
        return when (val resolved = resolve(value)) {
            is Map<*, *> -> resolved.entries.mapNotNull { (key, level) ->
                level?.toString()?.trim()?.toIntOrNull()?.let { key.toString() to it }
            }.toMap()

            is List<*> -> resolved.mapNotNull { entry ->
                val raw = entry?.toString() ?: return@mapNotNull null
                val separator = raw.lastIndexOf(':')
                if (separator <= 0 || separator == raw.lastIndex) return@mapNotNull null
                raw.substring(separator + 1).trim().toIntOrNull()?.let {
                    raw.substring(0, separator).trim() to it
                }
            }.toMap()

            else -> emptyMap()
        }
    }

    /** 解析字符串、数字和布尔标量；仅字符串需要变量替换。 */
    private fun resolveScalar(value: Any, resolveText: Boolean): Any {
        return if (value is String && resolveText) {
            TextResolver.resolve(player, value, variables, menuConfig = config).replace("\\n", "\n")
        } else {
            value
        }
    }

    /** 解析普通映射或条件分支映射。 */
    private fun resolveMapping(value: ContainerConfigValue.Mapping, resolveText: Boolean): Any? {
        if (value.values.containsKey("condition")) {
            return resolveConditional(value, resolveText)
        }
        return value.values.mapValues { (_, child) -> resolveValue(child, resolveText) }
    }

    /** 解析普通列表；列表全部为条件 Map 时沿用 KaMenu 的“首个非空分支”规则。 */
    private fun resolveSequence(value: ContainerConfigValue.Sequence, resolveText: Boolean): Any? {
        val isConditionCandidateList = value.values.isNotEmpty() && value.values.all { child ->
            child is ContainerConfigValue.Mapping && child.values.containsKey("condition")
        }
        if (isConditionCandidateList) {
            value.values.filterIsInstance<ContainerConfigValue.Mapping>().forEach { condition ->
                val resolved = resolveConditional(condition, resolveText)
                if (isMeaningful(resolved)) return resolved
            }
            return emptyList<Any>()
        }
        return value.values.mapNotNull { resolveValue(it, resolveText) }
    }

    /** 使用现有条件引擎选择 allow 或 deny，并继续递归解析选中值。 */
    private fun resolveConditional(value: ContainerConfigValue.Mapping, resolveText: Boolean): Any? {
        val conditionValue = value.values["condition"]
        val condition = (conditionValue as? ContainerConfigValue.Scalar)?.value?.toString()
            ?: string(conditionValue)
        val branch = if (ConditionUtils.checkCondition(player, condition, variables, config) { null }) {
            value.values["allow"] ?: value.values["actions"]
        } else {
            value.values["deny"]
        }
        return resolveValue(branch, resolveText)
    }

    /** 判断条件列表中的结果是否足以结束首匹配搜索。 */
    private fun isMeaningful(value: Any?): Boolean {
        return when (value) {
            null -> false
            is String -> value.isNotEmpty()
            is Collection<*> -> value.isNotEmpty()
            is Map<*, *> -> value.isNotEmpty()
            else -> true
        }
    }

    /** 将嵌套列表展开为字符串属性使用的一维值序列。 */
    private fun flatten(value: Any?): List<Any> {
        return when (value) {
            null -> emptyList()
            is Iterable<*> -> value.flatMap(::flatten)
            else -> listOf(value)
        }
    }
}
