package org.katacr.kamenu.migration

import org.bukkit.configuration.ConfigurationSection
import java.util.LinkedHashMap

/**
 * 保留 YAML 声明顺序的 源菜单 中立源节点。
 *
 * Bukkit `ConfigurationSection` 只在读取入口使用；后续转换器通过该节点访问属性，
 * 从而统一执行 源菜单 正则键解析和别名冲突诊断。
 */
internal class TrMenuSourceSection private constructor(
    private val values: LinkedHashMap<String, Any?>
) {
    private val resolutionCache = mutableMapOf<TrMenuSourceProperty, TrMenuKeyResolver.Resolution>()
    private val reportedCollisions = mutableSetOf<TrMenuSourceProperty>()

    val keys: List<String>
        get() = values.keys.toList()

    /** 按源文件中的精确键读取动态节点，例如图标 ID、任务 ID 或函数 ID。 */
    fun value(key: String): Any? = values[key]

    /** 按 源菜单 语义键读取值，并在同义键冲突时记录所采用的首个键。 */
    fun value(
        property: TrMenuSourceProperty,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): Any? {
        val resolution = resolutionCache.getOrPut(property) {
            TrMenuKeyResolver.resolve(values.keys, property)
        }
        if (reportedCollisions.add(property)) {
            reportCollision(resolution, path, diagnostics)
        }
        return resolution.selectedKey?.let(values::get)
    }

    /** 按 源菜单 语义键读取子节点；标量值由调用方根据字段要求报告类型错误。 */
    fun section(
        property: TrMenuSourceProperty,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): TrMenuSourceSection? = value(property, path, diagnostics) as? TrMenuSourceSection

    /** 返回当前节点中保持声明顺序的全部原始条目。 */
    fun entries(): List<Pair<String, Any?>> = values.entries.map { it.key to it.value }

    /** 一次不区分大小写的点路径查找结果。 */
    data class LocatedValue(val path: String, val value: Any?)

    /** 按 源菜单 配置语义查找任意深层节点，并返回源文件中的实际键大小写。 */
    fun find(path: String): LocatedValue? {
        val segments = path.split('.').map(String::trim).filter(String::isNotEmpty)
        if (segments.isEmpty()) return null
        var section = this
        val actualSegments = mutableListOf<String>()
        segments.forEachIndexed { index, segment ->
            val actualKey = section.values.keys.firstOrNull { it.equals(segment, ignoreCase = true) }
                ?: return null
            actualSegments += actualKey
            val value = section.values[actualKey]
            if (index == segments.lastIndex) {
                return LocatedValue(actualSegments.joinToString("."), value)
            }
            section = value as? TrMenuSourceSection ?: return null
        }
        return null
    }

    private fun reportCollision(
        resolution: TrMenuKeyResolver.Resolution,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ) {
        if (!resolution.hasCollision) return
        val selected = resolution.selectedKey ?: return
        val ignored = resolution.matchingKeys.drop(1).joinToString(", ")
        diagnostics.add(
            code = "TRM_KEY_ALIAS_COLLISION",
            severity = TrMenuMigrationSeverity.WARNING,
            compatibility = TrMenuMigrationCompatibility.APPROXIMATE,
            path = path,
            message = "Multiple keys match ${resolution.property.canonicalKey}; using '$selected' and ignoring: $ignored."
        )
    }

    companion object {
        /** 将 Bukkit YAML 节点递归转换为保持顺序的中立源节点。 */
        fun from(section: ConfigurationSection): TrMenuSourceSection {
            val values = LinkedHashMap<String, Any?>()
            section.getValues(false).forEach { (key, value) ->
                values[key] = normalize(value)
            }
            return TrMenuSourceSection(values)
        }

        private fun fromMap(map: Map<*, *>): TrMenuSourceSection {
            val values = LinkedHashMap<String, Any?>()
            map.forEach { (key, value) ->
                if (key != null) values[key.toString()] = normalize(value)
            }
            return TrMenuSourceSection(values)
        }

        private fun normalize(value: Any?): Any? = when (value) {
            is ConfigurationSection -> from(value)
            is Map<*, *> -> fromMap(value)
            is List<*> -> value.map(::normalize)
            else -> value
        }
    }
}
