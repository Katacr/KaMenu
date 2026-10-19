package org.katacr.kamenu.container

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 验证 repeat 返回值标准化及分页边界计算。 */
class ContainerRepeatSupportTest {
    @Test
    fun `converts maps and scalar values into item variables`() {
        val items = ContainerRepeatSupport.itemsFromAny(
            listOf(mapOf("id" to "home", "name" to "Home"), "mine", 7),
            split = null,
            trimItems = true
        )

        assertEquals(3, items.size)
        assertEquals("home", items[0].variables["item.id"])
        assertEquals("0", items[0].variables["item.index"])
        assertEquals("1", items[0].variables["item.number"])
        assertEquals("mine", items[1].variables["item.value"])
        assertEquals("7", items[2].variables["item.text"])
    }

    @Test
    fun `splits string values and trims when configured`() {
        val items = ContainerRepeatSupport.itemsFromAny("home, mine,", ",", true)

        assertEquals(listOf("home", "mine"), items.map { it.variables["item.value"] })
    }

    @Test
    fun `computes page variables using a half open range`() {
        val page = ContainerRepeatPageInfo.create(total = 25, capacity = 12, requestedPageIndex = 1)

        assertEquals(2, page.page)
        assertEquals(3, page.pages)
        assertEquals(25, page.total)
        assertEquals(12, page.start)
        assertEquals(24, page.end)
    }

    @Test
    fun `clamps requests and reports an empty list as one page`() {
        val lastPage = ContainerRepeatPageInfo.create(total = 25, capacity = 12, requestedPageIndex = 99)
        val empty = ContainerRepeatPageInfo.create(total = 0, capacity = 12, requestedPageIndex = 99)

        assertEquals(3, lastPage.page)
        assertEquals(0, empty.page - 1)
        assertEquals(1, empty.pages)
        assertEquals(0, empty.total)
        assertEquals(0, empty.start)
        assertEquals(0, empty.end)
        assertTrue(ContainerRepeatSupport.itemsFromAny(null, null, true).isEmpty())
    }
}
