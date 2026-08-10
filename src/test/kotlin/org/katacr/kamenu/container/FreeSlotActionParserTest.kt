package org.katacr.kamenu.container

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** 验证自由槽动作的单槽、多材料和非法参数解析。 */
class FreeSlotActionParserTest {

    @Test
    fun `parses single and multi item consumption`() {
        assertEquals(
            mapOf("input" to 2),
            requirements("type=consume;id=input;amount=2")
        )
        assertEquals(
            mapOf("diamond" to 3, "emerald" to 1),
            requirements("type=consume;items=diamond:1,emerald:1,diamond:2")
        )
    }

    @Test
    fun `uses one as default single item amount`() {
        assertEquals(mapOf("input" to 1), requirements("type=consume;id=input"))
    }

    @Test
    fun `rejects missing malformed and non positive amounts`() {
        assertNull(requirements("type=consume"))
        assertNull(requirements("type=consume;items=input"))
        assertNull(requirements("type=consume;items=input:0"))
        assertNull(requirements("type=consume;id=input;amount=-1"))
    }

    private fun requirements(raw: String): Map<String, Int>? {
        return FreeSlotActionParser.consumeRequirements(FreeSlotActionParser.arguments(raw))
    }
}
