package org.katacr.kamenu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HMCCosmeticsItemAdapterTest {
    @Test
    fun `maps supported slot references`() {
        assertEquals("HELMET", HMCCosmeticsItemAdapter.slotName("HMCC_HAT"))
        assertEquals("BACKPACK", HMCCosmeticsItemAdapter.slotName("hmcc_backpack"))
        assertEquals("OFFHAND", HMCCosmeticsItemAdapter.slotName("HMCC_OFFHAND"))
        assertEquals("BALLOON", HMCCosmeticsItemAdapter.slotName("HMCC_BALLOON"))
    }

    @Test
    fun `rejects unknown slot references`() {
        assertNull(HMCCosmeticsItemAdapter.slotName("HMCC_HELMET"))
        assertNull(HMCCosmeticsItemAdapter.slotName("HEAD"))
    }
}
