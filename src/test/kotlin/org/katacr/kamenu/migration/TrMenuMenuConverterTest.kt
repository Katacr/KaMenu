package org.katacr.kamenu.migration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.katacr.kamenu.container.ContainerMenuParser
import java.io.File

/** 验证完整 TrMenu 源菜单能组装并通过 KaMenu Container 目标解析。 */
class TrMenuMenuConverterTest {
    @Test
    fun `builds a validated menu with variants functions events and settings`() {
        val (result, diagnostics) = convert(
            """
            Title: '&8Shop ${'$'}{label_vip}'
            Title-Update: 20
            Layout: ['A        ']
            Options:
              Arguments: true
              Default-Arguments: [guest]
              Min-Click-Delay: 250
              Depend-Expansions: [player]
            Bindings:
              Commands: ['(?i)shop', 'shop-admin']
            Events:
              Open: ['tell: open {0}']
            Functions:
              label: 'args[0]'
            Icons:
              A:
                condition: 'perm *shop.view'
                update: 40
                display:
                  material: PAPER
                  name: '&a${'$'}{label_default}'
                actions:
                  all: ['function: label ok', 'tell: default']
                icons:
                  - priority: 1
                    condition: 'perm *shop.vip'
                    display:
                      material: DIAMOND
                      name: '&bVIP'
                    actions:
                      left: ['tell: vip']
            """
        )

        assertNotNull(result)
        assertFalse(diagnostics.hasErrors)
        val config = result!!.config
        assertEquals("&8Shop {js:[label],`vip`}", config.getString("Title"))
        assertEquals(250L, config.getLong("Settings.min_click_delay"))
        assertEquals(listOf("shop", "shop-admin"), result.boundCommands)
        assertTrue(config.contains("JavaScript.__trmenu_guard.label"))
        assertEquals("hasPerm.shop.view", config.getString("Buttons.A.view_condition"))
        assertEquals(2, config.getMapList("Buttons.A.variants").size)
        assertNotNull(ContainerMenuParser.parse("trmenu_migrated/shop", config).definition)
    }

    @Test
    fun `rejects private scripts in required item properties`() {
        val (result, diagnostics) = convert(
            """
            Layout: ['A        ']
            Icons:
              A:
                display:
                  material: '{ke: papi %dynamic_material%}'
            """
        )

        assertNull(result)
        assertTrue(diagnostics.issues.any { it.code == "TRM_ITEM_DYNAMIC_PROPERTY_UNSUPPORTED" })
    }

    @Test
    fun `maps TrMenu equipment utility calls without private JavaScript`() {
        val (result, diagnostics) = convert(
            """
            Title: 'Profile {0}'
            Layout: ['A        ']
            Events:
              Open:
                - condition: '${'$'} utils.isPlayerOnline(vars("{0}"))'
                  deny: ['return']
            Icons:
              A:
                display:
                  material: 'source:JS:utils.getEquipment(vars("{0}"), "HEAD")'
                icons:
                  - condition: '${'$'} !utils.hasEquipment(vars("{0}"), "HEAD")'
                    display:
                      material: 'head:{0}'
            """
        )

        assertNotNull(result)
        assertFalse(diagnostics.hasErrors)
        val config = result!!.config
        val variants = config.getMapList("Buttons.A.variants")
        val fallbackDisplay = variants[1]["display"] as Map<*, *>
        assertEquals("[HEAD:{arg:0}]", fallbackDisplay["material"])
        assertEquals(
            "!(hasEquipment.[HEAD;{arg:0}])",
            variants[0]["condition"]
        )
        assertTrue(diagnostics.issues.none { it.code == "TRM_ITEM_SOURCE_UNSUPPORTED" })
        assertTrue(diagnostics.issues.none { it.code == "TRM_CONDITION_UNSUPPORTED" })
    }

    @Test
    fun `converts item bindings and reports unsupported free slots and menu lang`() {
        val (result, diagnostics) = convert(
            """
            Layout: ['A        ']
            Free-Slots: [1]
            Options:
              Free-Slots: [2]
            Bindings:
              Items: ['material:compass']
            Lang:
              zh_CN:
                Title: '&8商店'
            Icons:
              A:
                display:
                  material: PAPER
            """
        )

        assertNotNull(result)
        assertFalse(diagnostics.hasErrors)
        assertEquals(
            setOf("Free-Slots", "Options.Free-Slots"),
            diagnostics.issues
                .filter { it.code == "TRM_OPTION_UNSUPPORTED" }
                .map { it.path }
                .toSet()
        )
        assertEquals(1, result?.boundItems?.size)
        assertEquals("compass", result?.boundItems?.single()?.values?.get("material"))
        assertTrue(diagnostics.issues.any { it.code == "TRM_BINDING_ITEM_INTERVAL_DEFAULT" })
        assertTrue(diagnostics.issues.any { it.code == "TRM_LANG_UNSUPPORTED" })
    }

    private fun convert(yaml: String): Pair<TrMenuMenuConversion?, TrMenuMigrationDiagnostics> {
        val config = TrMenuSourceParser.createSourceConfiguration()
        config.loadFromString(yaml.trimIndent())
        val source = TrMenuSourceMenu(File("shop.yml"), "shop", TrMenuSourceSection.from(config))
        val diagnostics = TrMenuMigrationDiagnostics()
        val converter = TrMenuMenuConverter(
            menuIdResolver = { id -> if (id == "shop") "trmenu_migrated/shop" else null },
            syntaxValidator = { null }
        )
        return converter.convert(source, "trmenu_migrated/shop", diagnostics) to diagnostics
    }
}
