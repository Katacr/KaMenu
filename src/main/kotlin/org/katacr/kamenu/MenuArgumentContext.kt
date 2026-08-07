package org.katacr.kamenu

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 保存玩家当前菜单的参数，并将其暴露给统一变量解析器。
 *
 * 参数只属于当前玩家的菜单会话，不写入 YAML、数据库或 MetaDataManager；打开另一个菜单时会整体替换。
 */
object MenuArgumentManager {
    /** 带唯一标识的参数快照，用于异步关闭回调判断上下文是否已经被新菜单替换。 */
    data class Context(
        val id: UUID = UUID.randomUUID(),
        val arguments: List<String>
    )

    private val activeArguments = ConcurrentHashMap<UUID, Context>()

    /** 激活一个菜单会话的参数列表。列表会被复制，避免调用方后续修改影响菜单。 */
    fun activate(player: Player, arguments: List<String>): Context {
        val context = Context(arguments = arguments.toList())
        activeArguments[player.uniqueId] = context
        return context
    }

    /** 取得玩家当前菜单参数；没有活动参数时返回空列表。 */
    fun current(player: Player): List<String> = activeArguments[player.uniqueId]?.arguments.orEmpty()

    /** 取得当前参数上下文快照，供异步生命周期回调进行代际校验。 */
    fun currentContext(player: Player): Context? = activeArguments[player.uniqueId]

    /** 将当前菜单参数转换为 `{arg:n}`、`{args}` 和 `{arg_count}` 变量。 */
    fun variables(player: Player): Map<String, String> {
        val context = activeArguments[player.uniqueId] ?: return emptyMap()
        val arguments = context.arguments
        return buildMap {
            put("args", arguments.joinToString(" "))
            put("arg_count", arguments.size.toString())
            arguments.forEachIndexed { index, value -> put("arg:$index", value) }
        }
    }

    /** 合并当前菜单参数和调用方变量；动作列表的局部 `{arg:n}` 优先级更高。 */
    fun merge(player: Player, variables: Map<String, String>): Map<String, String> {
        val menuVariables = variables(player)
        if (menuVariables.isEmpty()) return variables
        return menuVariables + variables
    }

    /** 清理玩家当前菜单参数。 */
    fun clear(player: Player) {
        activeArguments.remove(player.uniqueId)
    }

    /** 仅当当前参数仍属于指定菜单会话时清理，避免旧 Close 回调影响新菜单。 */
    fun clearIfCurrent(player: Player, context: Context?) {
        if (context != null) {
            activeArguments.remove(player.uniqueId, context)
        }
    }

    /** 插件关闭时清理全部玩家参数。 */
    fun clearAll() {
        activeArguments.clear()
    }
}

/**
 * 解析目标菜单的 `Settings.pass_arguments`，负责默认参数补位和最少参数数量检查。
 */
object MenuArgumentResolver {
    /** 目标菜单参数解析结果。 */
    data class Resolution(
        val arguments: List<String>,
        val required: Int,
        val enabled: Boolean
    ) {
        /** 判断补位后的参数数量是否满足目标菜单要求。 */
        val sufficient: Boolean
            get() = !enabled || arguments.size >= required
    }

    /** 读取目标菜单设置并解析参数；PAPI、内置变量和 JavaScript 在此时完成替换。 */
    fun resolve(
        player: Player,
        config: YamlConfiguration,
        suppliedArguments: List<String>,
        sourceVariables: Map<String, String> = emptyMap()
    ): Resolution {
        val section = config.getConfigurationSection("Settings.pass_arguments")
            ?: return Resolution(emptyList(), 0, false)
        if (!section.getBoolean("enable", false)) {
            return Resolution(emptyList(), 0, false)
        }

        val defaults = readDefaults(section)
        val required = readNonNegativeInt(section, "must")
        val count = maxOf(suppliedArguments.size, defaults.size)
        val arguments = buildList {
            for (index in 0 until count) {
                val raw = suppliedArguments.getOrNull(index) ?: defaults.getOrNull(index) ?: continue
                add(TextResolver.resolve(player, raw, sourceVariables, menuConfig = config))
            }
        }
        return Resolution(arguments, required, true)
    }

    /** 读取默认参数列表；同时兼容单个字符串写法。 */
    private fun readDefaults(section: ConfigurationSection): List<String> {
        return when (val raw = section.get("default")) {
            is List<*> -> raw.mapNotNull { it?.toString() }
            null -> emptyList()
            else -> listOf(raw.toString())
        }
    }

    /** 读取非负整数设置，非法或负数值按 0 处理。 */
    private fun readNonNegativeInt(section: ConfigurationSection, key: String): Int {
        val raw = section.get(key) ?: return 0
        val value = when (raw) {
            is Byte, is Short, is Int, is Long -> (raw as Number).toLong()
            is Float, is Double -> {
                val number = (raw as Number).toDouble()
                if (number.isFinite() && number % 1.0 == 0.0) number.toLong() else return 0
            }
            is String -> raw.trim().toLongOrNull() ?: return 0
            else -> return 0
        }
        return value.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    }
}
