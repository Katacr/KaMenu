package org.katacr.kamenu.migration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 验证 TrMenu ItemMatcher 基础 trait 到 KaMenu item/hasItem 的转换。 */
class TrMenuItemMatcherConverterTest {
    @Test
    fun `converts basic give take and item condition matchers`() {
        val diagnostics = TrMenuMigrationDiagnostics()
        val converter = TrMenuItemMatcherConverter(TrMenuVariableConverter())

        assertEquals(
            listOf("item: type=give;mats=DIAMOND;amount=3"),
            converter.convertAction("material:DIAMOND,amt:3", "give", "actions", diagnostics)
        )
        assertEquals(
            listOf("item: type=take;mats=PAPER;amount=2;lore=Ticket"),
            converter.convertAction("mat:PAPER,amount:2,lore:Ticket", "take", "actions", diagnostics)
        )
        assertEquals(
            "hasItem.[mats=PAPER;amount=1;custom_model_id=1001]",
            converter.convertCondition("material:PAPER,model-data:1001", "condition", diagnostics)
        )
        assertTrue(diagnostics.issues.isEmpty())
    }

    @Test
    fun `rejects unsupported traits instead of weakening the matcher`() {
        val diagnostics = TrMenuMigrationDiagnostics()
        val converter = TrMenuItemMatcherConverter(TrMenuVariableConverter())

        val result = converter.convertAction("material:STONE,!name:Admin", "take", "actions", diagnostics)

        assertTrue(result.isEmpty())
        assertTrue(diagnostics.issues.any { it.code == "TRM_ITEM_MATCHER_UNSUPPORTED" })
    }

    @Test
    fun `condition parser accepts supported item matcher`() {
        val diagnostics = TrMenuMigrationDiagnostics()
        val condition = TrMenuConditionConverter(TrMenuVariableConverter()).convert(
            "item *material:DIAMOND,amount:5",
            "condition",
            diagnostics
        )

        assertEquals("hasItem.[mats=DIAMOND;amount=5]", condition)
    }

    @Test
    fun `converts supported right click item binding traits and ignores amount`() {
        val diagnostics = TrMenuMigrationDiagnostics()
        val binding = TrMenuItemMatcherConverter(TrMenuVariableConverter()).convertBinding(
            "material:PAPER,amount:64,lore:&aMenu;model-data:1001,name:Selector,data:2",
            "Bindings.Items[0]",
            diagnostics
        )

        assertEquals(
            mapOf(
                "material" to "PAPER",
                "target-lore" to "&aMenu",
                "target-name" to "Selector",
                "data" to 2,
                "custom-model-data" to 1001
            ),
            binding
        )
        assertTrue(diagnostics.issues.isEmpty())
    }

    @Test
    fun `rejects unsafe right click item bindings`() {
        val diagnostics = TrMenuMigrationDiagnostics()
        val converter = TrMenuItemMatcherConverter(TrMenuVariableConverter())

        assertEquals(null, converter.convertBinding("amount:1", "Bindings.Items[0]", diagnostics))
        assertEquals(null, converter.convertBinding("material:PLAYER_HEAD,head:texture", "Bindings.Items[1]", diagnostics))
        assertEquals(null, converter.convertBinding("material:STONE,!lore:Admin", "Bindings.Items[2]", diagnostics))
        assertEquals(3, diagnostics.issues.count { it.code == "TRM_ITEM_MATCHER_UNSUPPORTED" })
    }
}
