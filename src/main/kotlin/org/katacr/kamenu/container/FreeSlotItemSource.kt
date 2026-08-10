package org.katacr.kamenu.container

/** 解析 Container 按钮的 `[FREE:<id>]` 真实物品预览来源。 */
object FreeSlotItemSource {
    /** 返回自由槽位 ID；非自由槽物品来源或空 ID 返回 null。 */
    fun parseId(source: String): String? {
        val normalized = source.trim()
        if (!normalized.startsWith("[FREE:", ignoreCase = true) || !normalized.endsWith(']')) return null
        return normalized.substring(6, normalized.length - 1).trim().takeIf(String::isNotEmpty)
    }
}
