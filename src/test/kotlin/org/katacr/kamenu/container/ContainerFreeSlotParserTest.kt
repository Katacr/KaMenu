package org.katacr.kamenu.container

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 验证 Free-Slots 的静态结构、默认值和槽位安全校验。 */
class ContainerFreeSlotParserTest {

    @Test
    fun `parses named free slots with rules events and defaults`() {
        val result = parse(
            """
            Type: CHEST
            Layout:
              - '#########'
              - '####  ###'
              - '#########'
            Free-Slots:
              diamond:
                slots: [13]
                place:
                  condition: '{free:incoming.material} == DIAMOND'
                take:
                  enabled: false
                events:
                  place:
                    - 'refresh: confirm'
                  deny_take: 'actionbar: locked'
              emerald:
                slots: [14]
            Buttons:
              '#':
                display:
                  material: STONE
            """
        )

        assertTrue(result.succeeded, result.diagnostics.joinToString())
        assertNotNull(result.definition)
        val definition = result.definition!!
        assertNotNull(definition.freeSlots.byId["diamond"])
        val diamond = definition.freeSlots.byId.getValue("diamond")
        assertEquals(listOf(13), diamond.slots)
        assertEquals("{free:incoming.material} == DIAMOND", diamond.place.condition)
        assertFalse(diamond.take.enabled)
        assertEquals(listOf("refresh: confirm"), diamond.events.place)
        assertEquals(listOf("actionbar: locked"), diamond.events.denyTake)
        assertTrue(diamond.returnRule.onClose)
        assertEquals(ContainerFreeSlotOverflowPolicy.PENDING, diamond.returnRule.overflow)
        assertEquals("diamond", definition.freeSlots.idBySlot[13])
        assertEquals("emerald", definition.freeSlots.idBySlot[14])
        assertEquals("diamond", definition.freeSlots.at(13)?.id)
    }

    @Test
    fun `rejects layout button overlap and cross group duplicate slot`() {
        val result = parse(
            """
            Layout:
              - '#########'
              - '####A####'
            Free-Slots:
              first:
                slots: [13]
              second:
                slots: [13]
            Buttons:
              '#':
                display:
                  material: STONE
              A:
                display:
                  material: DIAMOND
            """
        )

        assertFalse(result.succeeded)
        assertNull(result.definition)
        assertTrue(result.diagnostics.any { it.code == "free_slot.button_conflict" })
        assertTrue(result.diagnostics.any { it.code == "free_slot.duplicate_slot" })
    }

    @Test
    fun `rejects out of range slots unknown events and special containers`() {
        val result = parse(
            """
            Type: FURNACE
            Layout:
              - '   '
            Free-Slots:
              input:
                slots: [3]
                events:
                  click:
                    - 'tell: invalid'
            """
        )

        assertFalse(result.succeeded)
        assertTrue(result.diagnostics.any { it.code == "free_slots.unsupported_type" })
        assertTrue(result.diagnostics.any { it.code == "free_slot.slot_out_of_range" })
        assertTrue(result.diagnostics.any { it.code == "free_slot.unknown_event" })
    }

    @Test
    fun `rejects empty duplicate and non integer slot lists`() {
        val result = parse(
            """
            Layout:
              - '         '
            Free-Slots:
              empty:
                slots: []
              invalid:
                slots: [1, 1, '2']
            """
        )

        assertFalse(result.succeeded)
        assertTrue(result.diagnostics.any { it.code == "free_slot.invalid_slots" })
        assertTrue(result.diagnostics.any { it.code == "free_slot.duplicate_slot" })
        assertTrue(result.diagnostics.any { it.code == "free_slot.invalid_slot" })
    }

    private fun parse(source: String): ContainerMenuParseResult {
        val config = YamlConfiguration()
        config.loadFromString(source.trimIndent())
        return ContainerMenuParser.parse("test/free_slots", config)
    }
}
