package org.katacr.kamenu.container

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 验证自由槽普通左右键对槽位与光标数量的守恒计算。 */
class FreeSlotCursorPlannerTest {

    @Test
    fun `left click places the complete cursor stack`() {
        val plan = FreeSlotCursorPlanner.plan(null, item(Material.DIAMOND, 12), rightClick = false)!!

        assertEquals(12, plan.slotAfter?.amount)
        assertNull(plan.cursorAfter)
        assertTrue(plan.place)
        assertFalse(plan.take)
    }

    @Test
    fun `right click places one item`() {
        val plan = FreeSlotCursorPlanner.plan(null, item(Material.DIAMOND, 12), rightClick = true)!!

        assertEquals(1, plan.slotAfter?.amount)
        assertEquals(11, plan.cursorAfter?.amount)
    }

    @Test
    fun `left and right pickup preserve the total amount`() {
        val left = FreeSlotCursorPlanner.plan(item(Material.DIAMOND, 5), null, rightClick = false)!!
        val right = FreeSlotCursorPlanner.plan(item(Material.DIAMOND, 5), null, rightClick = true)!!

        assertNull(left.slotAfter)
        assertEquals(5, left.cursorAfter?.amount)
        assertEquals(2, right.slotAfter?.amount)
        assertEquals(3, right.cursorAfter?.amount)
    }

    @Test
    fun `left click merges up to material capacity`() {
        val plan = FreeSlotCursorPlanner.plan(
            item(Material.DIAMOND, 60),
            item(Material.DIAMOND, 10),
            rightClick = false
        )!!

        assertEquals(64, plan.slotAfter?.amount)
        assertEquals(6, plan.cursorAfter?.amount)
    }

    @Test
    fun `left click swaps different materials while right click rejects`() {
        val left = FreeSlotCursorPlanner.plan(
            item(Material.DIAMOND, 3),
            item(Material.EMERALD, 2),
            rightClick = false
        )!!

        assertEquals(Material.EMERALD, left.slotAfter?.type)
        assertEquals(Material.DIAMOND, left.cursorAfter?.type)
        assertTrue(left.place)
        assertTrue(left.take)
        assertNull(
            FreeSlotCursorPlanner.plan(
                item(Material.DIAMOND, 3),
                item(Material.EMERALD, 2),
                rightClick = true
            )
        )
    }

    private fun item(material: Material, amount: Int): ItemStack = TestItemStack(material, amount)

    /** 在无 Bukkit Server 的单元测试中提供稳定的克隆和同类判断。 */
    private class TestItemStack(material: Material, amount: Int) : ItemStack(material, amount) {
        override fun isSimilar(stack: ItemStack?): Boolean = stack?.type == type

        public override fun clone(): ItemStack = TestItemStack(type, amount)
    }
}
