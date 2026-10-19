package org.katacr.kamenu

import org.bukkit.event.block.Action
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ItemBindingClickTargetTest {
    @Test
    fun `defaults to both and rejects invalid config`() {
        assertEquals(ItemBindingClickTarget.BOTH, ItemBindingClickTarget.parse(null))
        assertEquals(ItemBindingClickTarget.AIR, ItemBindingClickTarget.parse(" AIR "))
        assertNull(ItemBindingClickTarget.parse("anywhere"))
    }

    @Test
    fun `air only binding never intercepts blocks`() {
        val air = ItemBindingClickTarget.AIR
        assertTrue(air.accepts(Action.RIGHT_CLICK_AIR))
        assertFalse(air.accepts(Action.RIGHT_CLICK_BLOCK))
        assertTrue(air.accepts(Action.LEFT_CLICK_AIR))
        assertFalse(air.accepts(Action.LEFT_CLICK_BLOCK))
        assertTrue(ItemBindingClickTarget.BOTH.accepts(Action.RIGHT_CLICK_BLOCK))
        assertTrue(ItemBindingClickTarget.BOTH.accepts(Action.LEFT_CLICK_AIR))
        assertTrue(ItemBindingClickTarget.BLOCK.accepts(Action.RIGHT_CLICK_BLOCK))
        assertTrue(ItemBindingClickTarget.BLOCK.accepts(Action.LEFT_CLICK_BLOCK))
        assertFalse(ItemBindingClickTarget.BLOCK.accepts(Action.RIGHT_CLICK_AIR))
    }
}
