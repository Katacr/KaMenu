package org.katacr.kamenu.container

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.katacr.kamenu.JavaScriptManager
import org.katacr.kamenu.TextResolver

/** 一个 repeat 数据项及其可在显示和动作中引用的 `{item.*}` 变量。 */
data class ContainerRepeatItem(val variables: Map<String, String>)

/** Container repeat 当前页对应的 1-based 页码和 0-based 半开数据区间。 */
data class ContainerRepeatPageInfo(
    val page: Int,
    val pages: Int,
    val total: Int,
    val start: Int,
    val end: Int
) {
    companion object {
        /** 根据总条目、当前布局槽位容量和 0-based 请求页计算有效页信息。 */
        fun create(total: Int, capacity: Int, requestedPageIndex: Int): ContainerRepeatPageInfo {
            val safeCapacity = capacity.coerceAtLeast(1)
            val safeTotal = total.coerceAtLeast(0)
            val pages = ((safeTotal.toLong() + safeCapacity - 1L) / safeCapacity)
                .coerceAtLeast(1L)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
            val pageIndex = requestedPageIndex.coerceIn(0, pages - 1)
            val start = minOf(pageIndex.toLong() * safeCapacity, safeTotal.toLong()).toInt()
            val end = minOf(start.toLong() + safeCapacity, safeTotal.toLong()).toInt()
            return ContainerRepeatPageInfo(pageIndex + 1, pages, safeTotal, start, end)
        }
    }
}

/** 解析 repeat source 返回值，并统一生成稳定的 item 字段和原始序号变量。 */
object ContainerRepeatSupport {
    /** 求值文本/JavaScript 来源并把返回结果标准化为 repeat 数据项。 */
    fun resolveSource(
        player: Player,
        config: YamlConfiguration,
        source: String,
        split: String?,
        trimItems: Boolean,
        variables: Map<String, String>
    ): List<ContainerRepeatItem> {
        val trimmedSource = source.trim()
        val result = if (trimmedSource.startsWith("[") && trimmedSource.endsWith("]")) {
            JavaScriptManager.executePredefinedFunctionWithArgs(
                player,
                trimmedSource.substring(1, trimmedSource.length - 1).trim(),
                "",
                config
            )
        } else {
            TextResolver.resolve(player, trimmedSource, variables, config)
        }
        return itemsFromAny(result, split, trimItems)
    }

    /** 将 JS 集合、JSON 字符串或普通字符串列表转换为 repeat 数据项。 */
    fun itemsFromAny(value: Any?, split: String?, trimItems: Boolean): List<ContainerRepeatItem> = when (value) {
        null -> emptyList()
        is Iterable<*> -> value.mapIndexedNotNull(::item)
        is Array<*> -> value.mapIndexedNotNull(::item)
        is String -> if (!split.isNullOrEmpty()) {
            value.split(split)
                .map { if (trimItems) it.trim() else it }
                .filter(String::isNotEmpty)
                .mapIndexed(::scalarItem)
        } else if (looksLikeJsonCollection(value)) {
            JavaScriptManager.parseJsonCompatible(value)
                ?.takeIf { it !is String }
                ?.let { itemsFromAny(it, null, trimItems) }
                .orEmpty()
        } else {
            value.lines()
                .filter(String::isNotBlank)
                .map { if (trimItems) it.trim() else it }
                .mapIndexed(::scalarItem)
        }
        else -> emptyList()
    }

    private fun item(index: Int, value: Any?): ContainerRepeatItem? = when (value) {
        null -> null
        is Map<*, *> -> ContainerRepeatItem(
            value.entries.associate { (key, child) -> "item.$key" to (child?.toString() ?: "") }
                .toMutableMap()
                .apply {
                    put("item.index", index.toString())
                    put("item.number", (index + 1).toString())
                }
        )
        else -> scalarItem(index, value.toString())
    }

    private fun scalarItem(index: Int, value: String) = ContainerRepeatItem(
        mapOf(
            "item.value" to value,
            "item.text" to value,
            "item.index" to index.toString(),
            "item.number" to (index + 1).toString()
        )
    )

    /** 只把外形完整的 JSON 数组/对象交给 JSON 解析器。 */
    private fun looksLikeJsonCollection(value: String): Boolean {
        val trimmed = value.trim()
        return (trimmed.startsWith('[') && trimmed.endsWith(']')) ||
            (trimmed.startsWith('{') && trimmed.endsWith('}'))
    }
}
