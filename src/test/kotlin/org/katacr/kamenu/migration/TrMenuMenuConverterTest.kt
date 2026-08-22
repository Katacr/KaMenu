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
                  lore:
                    - '&7Visible {condition=perm *shop.view}'
                    - '&eAdmin {requirement=perm *shop.admin}'
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
        val variants = config.getMapList("Buttons.A.variants")
        val fallbackDisplay = variants[1]["display"] as Map<*, *>
        assertEquals(
            listOf(
                "&7Visible {condition: hasPerm.shop.view}",
                "&eAdmin {condition: hasPerm.shop.admin}"
            ),
            fallbackDisplay["lore"]
        )
        assertEquals(2, variants.size)
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
    fun `migrates node references templates and icon id paths`() {
        val (result, diagnostics) = convert(
            """
            Title: '{node:Shared.Title}'
            Shared:
              Title: '&8Reference Shop'
              Message: '&aBuy {0} for {1}'
            Layout: ['A        ']
            Icons:
              A:
                display:
                  material: PAPER
                  name: '{node:Shared.Title}'
                  lore:
                    - '{node:Icons.@iconId@.display.material}'
                    - '{node:Shared.Message_five_10 coins}'
                actions:
                  left: ['tell: {node:Shared.Title}']
            """
        )

        assertNotNull(result)
        assertFalse(diagnostics.hasErrors)
        val config = result!!.config
        assertEquals("{ref:trmenu.Shared.Title}", config.getString("Title"))
        assertEquals("&8Reference Shop", config.getString("References.trmenu.Shared.Title"))
        assertEquals("&aBuy {refarg:0} for {refarg:1}", config.getString("References.trmenu.Shared.Message"))
        assertEquals("PAPER", config.getString("References.trmenu.Icons.A.display.material"))
        val display = config.get("Buttons.A.display") as Map<*, *>
        assertEquals(
            listOf(
                "{ref:trmenu.Icons.A.display.material}",
                "{ref:[trmenu.Shared.Message;`five`;`10 coins`]}"
            ),
            display["lore"]
        )
        val actions = config.get("Buttons.A.actions") as Map<*, *>
        assertEquals(
            listOf("tell: {ref:trmenu.Shared.Title}"),
            actions["left"]
        )
        assertTrue(diagnostics.issues.none { it.code == "TRM_VARIABLE_UNSUPPORTED" })
    }

    @Test
    fun `keeps underscores introduced by icon id out of node arguments`() {
        val (result, diagnostics) = convert(
            """
            Layout: ['         ']
            Icons:
              shop_item:
                display:
                  material: PAPER
                  slot: 0
                  lore:
                    - '{node:Icons.@iconId@.display.material}'
            """
        )

        assertNotNull(result)
        assertFalse(diagnostics.hasErrors)
        val config = result!!.config
        assertEquals("PAPER", config.getString("References.trmenu.Icons.shop_item.display.material"))
        val display = config.get("Buttons.shop_item.display") as Map<*, *>
        assertEquals(
            listOf("{ref:trmenu.Icons.shop_item.display.material}"),
            display["lore"]
        )
        assertTrue(diagnostics.issues.none { it.code.startsWith("TRM_NODE_") })
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

    @Test
    fun `migrates multi-page menu with page switching and default page`() {
        val (result, diagnostics) = convert(
            """
            Title: '&8Multi &7| &f{page_display}/{pages}'
            Options:
              Default-Layout: 0
            Layout:
              - ['A        ']
              - ['B        ']
            Icons:
              A:
                display:
                  material: STONE
                  name: '&aNext'
                actions:
                  all: ['page: 1']
              B:
                display:
                  material: PAPER
                  name: '&aPrev'
                actions:
                  all: ['page: 0']
            """
        )

        assertNotNull(result)
        assertFalse(diagnostics.hasErrors, diagnostics.issues.map { "${it.code}: ${it.message}" }.joinToString("\n"))
        val config = result!!.config
        val layout = config.getList("Layout")
        assertEquals(2, layout?.size)
        assertEquals(0, config.getInt("Settings.default_page"))
        val aActions = (config.get("Buttons.A.actions") as? Map<*, *>)?.get("all") as? List<*>
        val bActions = (config.get("Buttons.B.actions") as? Map<*, *>)?.get("all") as? List<*>
        assertEquals(listOf("page: 1"), aActions?.mapNotNull { it.toString() })
        assertEquals(listOf("page: 0"), bActions?.mapNotNull { it.toString() })

        val parsed = ContainerMenuParser.parse("trmenu_migrated/multi", config)
        assertNotNull(parsed.definition)
        assertEquals(2, parsed.definition!!.pageCount)
        val titleScalar = parsed.definition!!.title as? org.katacr.kamenu.container.ContainerConfigValue.Scalar
        assertEquals("&8Multi &7| &f{page_display}/{pages}", titleScalar?.value?.toString())
    }

    @Test
    fun `migrates icon with variable slot expression to runtime slot`() {
        val (result, diagnostics) = convert(
            """
            Title: '&8Shop'
            Layout: ['A        ']
            Icons:
              A:
                display:
                  material: STONE
                  name: '&aStatic'
              moving:
                display:
                  material: PAPER
                  name: '&aDynamic'
                  slots:
                    - '%player_empty_slots%'
            """
        )

        assertNotNull(result)
        assertFalse(diagnostics.hasErrors, diagnostics.issues.map { "${it.code}: ${it.message}" }.joinToString("\n"))
        val config = result!!.config
        assertEquals("%player_empty_slots%", config.getString("Buttons.moving.slot"))
        assertNull(config.get("Buttons.A.slot"))
        val parsed = ContainerMenuParser.parse("trmenu_migrated/shop", config)
        assertNotNull(parsed.definition)
        assertEquals(
            setOf("moving"),
            parsed.definition!!.layout.dynamicSlotButtons
        )
    }

    @Test
    fun `migrates animated slot frames to cycling frames`() {
        val (result, diagnostics) = convert(
            """
            Title: '&8Shop'
            Layout: ['         ']
            Icons:
              moving:
                display:
                  material: PAPER
                  name: '&aCycle'
                  slots:
                    - [8]
                    - [9]
                    - [10]
            """
        )

        assertNotNull(result)
        assertFalse(diagnostics.hasErrors, diagnostics.issues.map { "${it.code}: ${it.message}" }.joinToString("\n"))
        val config = result!!.config
        val slotValue = config.get("Buttons.moving.slot")
        assertTrue(slotValue is List<*>)
        assertEquals(listOf(listOf("8"), listOf("9"), listOf("10")), slotValue)
        val parsed = ContainerMenuParser.parse("trmenu_migrated/shop", config)
        assertNotNull(parsed.definition)
        val button = parsed.definition!!.buttons["moving"]
        assertNull(button!!.slot)
        assertEquals(3, button.slotFrames?.size)
    }

    @Test
    fun `migrates deeply nested condition action tree without dropping branches`() {
        val (result, diagnostics) = convert(
            """
            Title: '&8Shop'
            Layout: ['A        ']
            Icons:
              A:
                display:
                  material: CRAFTING_TABLE
                  name: '&7点击合成'
                action:
                  all:
                    - condition: 'check papi *%player_empty_slots% >= *2'
                      deny:
                        - 'tell: 背包空间不足'
                      actions:
                        - condition: 'check papi *%vip% >= *10'
                          deny:
                            - 'tell: 活力值不足'
                          actions:
                            - 'tell: 合成成功'
                            - 'console: give item'
            """
        )

        assertNotNull(result)
        assertFalse(diagnostics.hasErrors, diagnostics.issues.map { "${it.code}: ${it.message}" }.joinToString("\n"))
        val config = result!!.config
        val actionsAll = (config.get("Buttons.A.actions") as? Map<*, *>)?.get("all") as? List<*>
        assertNotNull(actionsAll, "config=\n${config.saveToString()}")
        assertEquals(1, actionsAll!!.size)
        val outer = actionsAll[0] as Map<*, *>
        assertEquals("%player_empty_slots% >= 2", outer["condition"])
        assertEquals(listOf("tell: 背包空间不足"), outer["deny"])
        val innerAllow = outer["allow"] as List<*>
        assertEquals(1, innerAllow.size)
        val inner = innerAllow[0] as Map<*, *>
        assertEquals("%vip% >= 10", inner["condition"])
        assertEquals(listOf("tell: 活力值不足"), inner["deny"])
        assertEquals(listOf("tell: 合成成功", "console: give item"), inner["allow"])
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
