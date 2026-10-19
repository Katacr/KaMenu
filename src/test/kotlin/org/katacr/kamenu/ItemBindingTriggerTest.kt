package org.katacr.kamenu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ItemBindingTriggerTest {
    @Test
    fun `four types and boolean sneaking combine into eight exact gestures`() {
        val types = listOf("left-click", "right-click", "f", "q")
        val parsed = types.map { ItemBindingTrigger.parse(it)!! }
        assertEquals(4, parsed.toSet().size)
        for (sneaking in listOf(false, true)) for (trigger in parsed) {
            assertTrue(trigger.matches(trigger, true))
            assertFalse(trigger.matches(trigger, false))
            for (other in parsed - trigger) assertFalse(trigger.matches(other, sneaking))
        }
        assertEquals(ItemBindingTrigger.F, ItemBindingTrigger.parse(" F "))
        assertNull(ItemBindingTrigger.parse(null))
        assertNull(ItemBindingTrigger.parse("sneak-q"))
    }
}
