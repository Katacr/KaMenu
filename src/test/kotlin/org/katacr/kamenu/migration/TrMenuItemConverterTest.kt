package org.katacr.kamenu.migration

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** 验证 TrMenu 物品来源、动画降级及 nested icons 继承展开。 */
class TrMenuItemConverterTest {
    @Test
    fun `converts head external item sources and material model data`() {
        val (buttons, diagnostics) = convert(
            """
            Layout: ['ABCDEFGH ']
            Icons:
              A:
                display:
                  material: 'head:Steve'
              B:
                display:
                  material: 'source:IA:example:item'
              C:
                display:
                  material: 'source:ORX:oraxen_item'
              D:
                display:
                  material: 'source:CE:example:engine_item'
              E:
                display:
                  material: 'source:ITEMSADDER:example:long_item'
              F:
                display:
                  material: 'source:ORAXEN:long_oraxen_item'
              G:
                display:
                  material: 'source:CRAFTENGINE:example:long_engine_item'
              H:
                display:
                  material: 'STONE{model-data:42}'
            """
        )

        assertFalse(diagnostics.hasErrors)
        assertEquals("PLAYER_HEAD", buttons[0].defaultState.display["material"])
        assertEquals("Steve", buttons[0].defaultState.display["skull_owner"])
        assertEquals("itemsadder:example:item", buttons[1].defaultState.display["material"])
        assertEquals("oraxen:oraxen_item", buttons[2].defaultState.display["material"])
        assertEquals("craftengine:example:engine_item", buttons[3].defaultState.display["material"])
        assertEquals("itemsadder:example:long_item", buttons[4].defaultState.display["material"])
        assertEquals("oraxen:long_oraxen_item", buttons[5].defaultState.display["material"])
        assertEquals("craftengine:example:long_engine_item", buttons[6].defaultState.display["material"])
        assertEquals("STONE", buttons[7].defaultState.display["material"])
        assertEquals("42", buttons[7].defaultState.display["custom_model_data"])
    }

    @Test
    fun `expands true inheritance and action ordering`() {
        val (buttons, diagnostics) = convert(
            """
            Layout: ['A        ']
            Icons:
              A:
                display:
                  material: STONE
                  name: Parent
                  lore: [Parent lore]
                  amount: 3
                  glow: true
                actions:
                  all: 'tell: parent'
                icons:
                  - condition: 'perm *first'
                    priority: 10
                    inherit: true
                    display: {}
                    actions:
                      all: 'tell: child'
                  - condition: 'perm *second'
                    priority: 5
                    inherit: [display_amount, display_shiny, actions]
                    append: [actions]
                    display:
                      name: Child
                    actions:
                      all: 'tell: inherited-child'
                  - condition: 'perm *third'
                    priority: 20
                    append: [actions]
                    display:
                      name: Appended
                    actions:
                      all: 'tell: appended-child'
            """
        )

        assertFalse(diagnostics.hasErrors)
        val states = buttons.single().variants
        assertEquals(listOf(5, 10, 20), states.map { it.priority })

        val inheritedActions = states[0].actionLayers.map { it.path }
        assertEquals(listOf("Icons.A.actions", "Icons.A.icons[1].actions"), inheritedActions)
        assertEquals("Child", states[0].display["name"])
        assertEquals(3, states[0].display["amount"])
        assertEquals(true, states[0].display["glow"])

        assertEquals("Parent", states[1].display["name"])
        assertEquals(listOf("Parent lore"), states[1].display["lore"])
        assertEquals("1", states[1].display["amount"])
        assertEquals("false", states[1].display["glow"])

        val appendedActions = states[2].actionLayers.map { it.path }
        assertEquals(listOf("Icons.A.icons[2].actions", "Icons.A.actions"), appendedActions)
    }

    @Test
    fun `reduces animated fields and refresh periods`() {
        val (buttons, diagnostics) = convert(
            """
            Layout: ['A        ']
            Icons:
              A:
                refresh: 40
                update: [20, 10, -1, -1]
                display:
                  material: [STONE, PAPER]
                  name: [First, Second]
                  lore:
                    - [Line one]
                    - [Line two]
            """
        )

        val button = buttons.single()
        assertEquals(10L, button.updateIntervalTicks)
        assertEquals("STONE", button.defaultState.display["material"])
        assertEquals("First", button.defaultState.display["name"])
        assertEquals(listOf("Line one"), button.defaultState.display["lore"])
        assertEquals(3, diagnostics.issues.count { it.code == "TRM_ICON_ANIMATION_FIRST_FRAME" })
        assertTrue(diagnostics.issues.any { it.code == "TRM_ICON_UPDATE_APPROXIMATE" })
    }

    @Test
    fun `preserves unsupported item source for manual replacement`() {
        val (buttons, diagnostics) = convert(
            """
            Layout: ['A        ']
            Icons:
              A:
                display:
                  material: 'source:JS:utils.getEquipment()'
            """
        )

        assertEquals("source:JS:utils.getEquipment()", buttons.single().defaultState.display["material"])
        assertTrue(diagnostics.issues.any { it.code == "TRM_ITEM_SOURCE_UNSUPPORTED" })
    }

    @Test
    fun `converts TrMenu equipment sources to KaMenu slot references`() {
        val (buttons, diagnostics) = convert(
            """
            Layout: ['ABCDEF   ']
            Icons:
              A:
                display:
                  material: 'source:JS:utils.getEquipment(vars("{0}"), "HEAD")'
              B:
                display:
                  material: 'source:JS:utils.getEquipment(vars("{0}"), "CHEST")'
              C:
                display:
                  material: 'source:JS:utils.getEquipment(vars("{0}"), "LEGS")'
              D:
                display:
                  material: 'source:JS:utils.getEquipment(vars("{0}"), "FEET")'
              E:
                display:
                  material: 'source:JS:utils.getEquipment(player, "MAINHAND")'
              F:
                display:
                  material: 'source:JS:utils.getEquipment(vars("AdminPlayer"), "OFFHAND")'
            """
        )

        assertEquals(
            listOf(
                "[HEAD:{0}]",
                "[CHEST:{0}]",
                "[LEGGINGS:{0}]",
                "[BOOTS:{0}]",
                "[MAINHAND]",
                "[OFFHAND:AdminPlayer]"
            ),
            buttons.map { it.defaultState.display["material"] }
        )
        assertTrue(diagnostics.issues.none { it.code == "TRM_ITEM_SOURCE_UNSUPPORTED" })
    }

    private fun convert(yaml: String): Pair<List<TrMenuButtonVisualConversion>, TrMenuMigrationDiagnostics> {
        val config = YamlConfiguration()
        config.loadFromString(yaml.trimIndent())
        val source = TrMenuSourceMenu(
            source = File("test.yml"),
            menuId = "test",
            root = TrMenuSourceSection.from(config)
        )
        val diagnostics = TrMenuMigrationDiagnostics()
        val layout = TrMenuLayoutConverter().convert(source, diagnostics)!!
        return TrMenuItemConverter().convert(layout, diagnostics) to diagnostics
    }
}
