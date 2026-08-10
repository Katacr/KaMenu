package org.katacr.kamenu.container

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** 验证 `[FREE:<id>]` 来源语法的稳定解析边界。 */
class FreeSlotItemContextTest {

    @Test
    fun `parses free item source case insensitively`() {
        assertEquals("input", FreeSlotItemSource.parseId("[FREE:input]"))
        assertEquals("special_diamond", FreeSlotItemSource.parseId(" [free: special_diamond ] "))
    }

    @Test
    fun `rejects unrelated or empty sources`() {
        assertNull(FreeSlotItemSource.parseId("DIAMOND"))
        assertNull(FreeSlotItemSource.parseId("[MAINHAND]"))
        assertNull(FreeSlotItemSource.parseId("[FREE:]"))
    }

}
