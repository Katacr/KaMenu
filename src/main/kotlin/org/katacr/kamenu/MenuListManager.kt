package org.katacr.kamenu

import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

/**
 * repeat 按钮分页状态管理器。
 *
 * 状态按 `玩家 + contextId + listId` 隔离，同一个玩家同时打开不同来源的菜单不会共用页码。
 * 菜单关闭或玩家退出时应清理状态，避免长期缓存无效分页信息。
 */
object MenuListManager {
    /**
     * 分页状态键。
     */
    private data class ListKey(
        val playerId: UUID,
        val contextId: String,
        val listId: String
    )

    /**
     * 当前页的计算结果。
     *
     * start/end 是当前页在完整列表中的半开区间 `[start, end)`。
     */
    data class PageInfo(
        val page: Int,
        val pages: Int,
        val total: Int,
        val start: Int,
        val end: Int
    )

    private val pageStates = ConcurrentHashMap<ListKey, Int>()
    private val pageInfoStates = ConcurrentHashMap<ListKey, PageInfo>()

    fun getPage(player: Player, contextId: String, listId: String): Int {
        return pageStates[ListKey(player.uniqueId, contextId, listId)] ?: 1
    }

    fun setPage(player: Player, contextId: String, listId: String, page: Int): Int {
        val normalized = page.coerceAtLeast(1)
        pageStates[ListKey(player.uniqueId, contextId, listId)] = normalized
        return normalized
    }

    fun movePage(player: Player, contextId: String, listId: String, delta: Int): Int {
        val key = ListKey(player.uniqueId, contextId, listId)
        val current = pageStates[key] ?: 1
        val next = (current + delta).coerceAtLeast(1)
        pageStates[key] = next
        return next
    }

    /**
     * 根据列表总数刷新分页信息。
     *
     * 如果之前记录的页码超过最大页，会自动夹到最后一页。
     */
    fun updatePageInfo(
        player: Player,
        contextId: String,
        listId: String,
        pageSize: Int,
        total: Int
    ): PageInfo {
        val key = ListKey(player.uniqueId, contextId, listId)
        val normalizedPageSize = pageSize.coerceAtLeast(1)
        val pages = max(1, (total + normalizedPageSize - 1) / normalizedPageSize)
        val requestedPage = pageStates[key] ?: 1
        val page = requestedPage.coerceIn(1, pages)
        pageStates[key] = page

        val start = min((page - 1) * normalizedPageSize, total)
        val end = min(start + normalizedPageSize, total)
        val info = PageInfo(page, pages, total, start, end)
        pageInfoStates[key] = info
        return info
    }

    fun getPageInfo(player: Player, contextId: String, listId: String): PageInfo? {
        return pageInfoStates[ListKey(player.uniqueId, contextId, listId)]
    }

    fun clear(player: Player) {
        clear(player.uniqueId)
    }

    fun clear(playerId: UUID) {
        pageStates.keys.removeIf { it.playerId == playerId }
        pageInfoStates.keys.removeIf { it.playerId == playerId }
    }

    fun clearAll() {
        pageStates.clear()
        pageInfoStates.clear()
    }
}
