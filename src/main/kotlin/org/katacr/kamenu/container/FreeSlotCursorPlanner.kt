package org.katacr.kamenu.container

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/** 计算取消原版点击后，自由槽与玩家光标之间的一次标准左右键搬运。 */
internal object FreeSlotCursorPlanner {

    /** 一次点击前后的隔离快照，以及是否包含放入或取出语义。 */
    data class Plan(
        val slotBefore: ItemStack?,
        val slotAfter: ItemStack?,
        val cursorBefore: ItemStack?,
        val cursorAfter: ItemStack?,
        val place: Boolean,
        val take: Boolean
    )

    /** 根据服务端当前槽位、光标和左右键计算最终状态；无变化时返回 null。 */
    fun plan(slot: ItemStack?, cursor: ItemStack?, rightClick: Boolean): Plan? {
        val normalizedSlot = copy(slot)
        val normalizedCursor = copy(cursor)

        if (normalizedCursor == null) {
            normalizedSlot ?: return null
            val moved = if (rightClick) (normalizedSlot.amount + 1) / 2 else normalizedSlot.amount
            val cursorAfter = normalizedSlot.clone().apply { amount = moved }
            val slotAfter = normalizedSlot.clone().apply { amount -= moved }.takeIf(::present)
            return Plan(normalizedSlot, slotAfter, null, cursorAfter, place = false, take = true)
        }

        if (normalizedSlot == null) {
            val moved = if (rightClick) 1 else normalizedCursor.amount
            val slotAfter = normalizedCursor.clone().apply { amount = moved }
            val cursorAfter = normalizedCursor.clone().apply { amount -= moved }.takeIf(::present)
            return Plan(null, slotAfter, normalizedCursor, cursorAfter, place = true, take = false)
        }

        if (normalizedSlot.isSimilar(normalizedCursor)) {
            val capacity = normalizedSlot.maxStackSize - normalizedSlot.amount
            val moved = minOf(if (rightClick) 1 else normalizedCursor.amount, capacity)
            if (moved <= 0) return null
            val slotAfter = normalizedSlot.clone().apply { amount += moved }
            val cursorAfter = normalizedCursor.clone().apply { amount -= moved }.takeIf(::present)
            return Plan(normalizedSlot, slotAfter, normalizedCursor, cursorAfter, place = true, take = false)
        }

        if (rightClick) return null
        return Plan(
            slotBefore = normalizedSlot,
            slotAfter = normalizedCursor,
            cursorBefore = normalizedCursor,
            cursorAfter = normalizedSlot,
            place = true,
            take = true
        )
    }

    private fun copy(item: ItemStack?): ItemStack? = item?.takeIf(::present)?.clone()

    private fun present(item: ItemStack): Boolean = item.type != Material.AIR && item.amount > 0
}
