package org.katacr.kamenu

import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.PluginManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.katacr.kamenu.container.ContainerMenuParser
import org.katacr.kamenu.container.ContainerValueResolver
import java.lang.reflect.Proxy
import java.util.UUID
import java.util.logging.Logger

/** 验证展示字符串列表的条件候选兼容语义与混合列表顺序。 */
class ConditionalStringListResolverTest {
    private val player = proxy(Player::class.java) { method ->
        when (method.name) {
            "getUniqueId" -> PLAYER_ID
            "getName" -> "ListResolverTest"
            "isOnline" -> true
            "hasPermission" -> false
            else -> defaultValue(method.returnType)
        }
    }

    @Test
    fun `dialog fields preserve ordered static and conditional lines`() {
        val config = YamlConfiguration().apply {
            loadFromString(
                """
                Body:
                  inline:
                    type: message
                    text:
                      - 'inline-keep {condition: true}'
                      - 'inline-drop {condition: false}'
                      - 'inline-safe {condition: {arg:0} == true}'
                  message:
                    text:
                      - 'message-1'
                      - condition: 'true'
                        allow:
                          - 'conditional-1'
                          - condition: 'false'
                            allow: 'ignored'
                            deny: 'nested-deny'
                          - 'conditional-2'
                      - 'message-2'
                  item:
                    lore:
                      - 'lore-1'
                      - condition: 'false'
                        allow: 'ignored'
                        deny:
                          - 'lore-deny-1'
                          - 'lore-deny-2'
                      - 'lore-2'
                Bottom:
                  buttons:
                    test:
                      tooltip:
                        - 'tooltip-1'
                        - condition: 'true'
                          allow:
                            - 'tooltip-allow-1'
                            - 'tooltip-allow-2'
                        - 'tooltip-2'
                """.trimIndent()
            )
        }

        assertEquals(
            "message-1\nconditional-1\nnested-deny\nconditional-2\nmessage-2",
            ConditionValueResolver.getString(player, config, "Body.message.text")
        )
        assertEquals(
            listOf("lore-1", "lore-deny-1", "lore-deny-2", "lore-2"),
            ConditionValueResolver.getStringList(player, config, "Body.item.lore")
        )
        assertEquals(
            listOf("tooltip-1", "tooltip-allow-1", "tooltip-allow-2", "tooltip-2"),
            ConditionValueResolver.getStringList(player, config, "Bottom.buttons.test.tooltip")
        )
        assertEquals(
            listOf("inline-keep"),
            ConditionValueResolver.getInlineConditionalStringList(
                player,
                config,
                "Body.inline.text",
                mapOf("arg:0" to "true || true")
            )
        )
    }

    @Test
    fun `dialog all-condition list keeps first non-empty candidate`() {
        val config = YamlConfiguration().apply {
            loadFromString(
                """
                value:
                  - condition: 'false'
                    allow: 'ignored'
                  - condition: 'true'
                    allow:
                      - 'selected-1'
                      - 'selected-2'
                  - condition: 'true'
                    allow: 'later'
                """.trimIndent()
            )
        }

        assertEquals(
            listOf("selected-1", "selected-2"),
            ConditionValueResolver.getStringList(player, config, "value")
        )
    }

    @Test
    fun `container lore supports mixed lines and candidate lists`() {
        val config = YamlConfiguration().apply {
            loadFromString(
                """
                Type: CHEST
                Title: Test
                Layout:
                  - 'TUV      '
                Buttons:
                  T:
                    display:
                      material: BOOK
                      lore:
                        - 'mixed-1'
                        - condition: 'true'
                          allow:
                            - 'mixed-allow-1'
                            - 'mixed-allow-2'
                        - 'mixed-2'
                  U:
                    display:
                      material: PAPER
                      lore:
                        - condition: 'false'
                          allow: 'ignored'
                        - condition: 'true'
                          allow:
                            - 'candidate-1'
                            - 'candidate-2'
                        - condition: 'true'
                          allow: 'later'
                  V:
                    display:
                      material: MAP
                      lore:
                        - 'container-inline-keep {condition: true}'
                        - 'container-inline-drop {condition: false}'
                        - 'container-inline-safe {condition: {arg:0} == true}'
                """.trimIndent()
            )
        }
        val definition = requireNotNull(ContainerMenuParser.parse("test/conditional-lists", config).definition)
        val resolver = ContainerValueResolver(player, config)

        assertEquals(
            listOf("mixed-1", "mixed-allow-1", "mixed-allow-2", "mixed-2"),
            resolver.strings(definition.buttons.getValue("T").display["lore"])
        )
        assertEquals(
            listOf("candidate-1", "candidate-2"),
            resolver.strings(definition.buttons.getValue("U").display["lore"])
        )
        val contextualResolver = ContainerValueResolver(
            player,
            config,
            mapOf("arg:0" to "true || true")
        )
        assertEquals(
            listOf("container-inline-keep"),
            contextualResolver.inlineConditionalStrings(definition.buttons.getValue("V").display["lore"])
        )
    }

    @Test
    fun `inline condition parser preserves nested variables and only accepts line suffixes`() {
        assertEquals(
            InlineConditionResolver.Parsed("visible", "{meta:test} == true"),
            InlineConditionResolver.parse("visible {condition: {meta:test} == true}")
        )
        assertEquals(
            InlineConditionResolver.Parsed("visible", "10 > 5"),
            InlineConditionResolver.parse("visible {condition: 10 > 5}")
        )
        assertNull(InlineConditionResolver.parse("before {condition: true} after"))
        assertNull(InlineConditionResolver.parse("visible {requirement: true}"))
        assertNull(InlineConditionResolver.parse("visible <condition: true>"))
        assertNull(InlineConditionResolver.parse("visible {condition=true}"))
        assertNull(InlineConditionResolver.parse("visible {condition: }"))
        assertEquals(
            "return" to "{arg:0}",
            InlineConditionResolver.parseTrailingModifier("return {chance: {arg:0}}", "chance")
        )
        assertNull(InlineConditionResolver.parseTrailingModifier("return {chance=100}", "chance"))
        assertNull(InlineConditionResolver.parseTrailingModifier("return {delay: 20}", "wait"))
        assertEquals("visible", InlineConditionResolver.resolve(player, "visible {condition: true}"))
        assertNull(InlineConditionResolver.resolve(player, "hidden {condition: false}"))
    }

    @Test
    fun `inline action conditions skip or execute the complete action line`() {
        val config = YamlConfiguration()

        assertTrue(
            MenuActions.executeActionGroup(
                player,
                config,
                listOf("return {chance: 100} {condition: true}"),
                asyncDataOperations = false
            ).join()
        )
        assertFalse(
            MenuActions.executeActionGroup(
                player,
                config,
                listOf("return {condition: false}"),
                asyncDataOperations = false
            ).join()
        )
        assertTrue(
            MenuActions.executeActionGroup(
                player,
                config,
                listOf("return {chance: {arg:0}}"),
                variables = mapOf("arg:0" to "100"),
                asyncDataOperations = false
            ).join()
        )
    }

    @Test
    fun `menu references resolve inside condition expressions`() {
        val config = YamlConfiguration().apply {
            set("References.required", 10)
            set("Settings.threshold", 10)
            set("Buttons.shop.data.price", 10)
        }
        val variables = mapOf(
            "self:id" to "shop",
            "self:path" to "Buttons.shop"
        )

        assertTrue(
            ConditionExpressionEngine.checkCondition(
                player,
                "{ref:required} == 10 && {config:Settings.threshold} == 10 && " +
                    "{self:data.price} == 10 && {self:id} == shop && {self:path} == Buttons.shop",
                variables,
                config
            ) { null }
        )
    }

    companion object {
        private val PLAYER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")

        /** 安装最小 Bukkit 门面，使文本解析测试不依赖真实服务器。 */
        @JvmStatic
        @BeforeAll
        fun installBukkitFacade() {
            if (runCatching { Bukkit.getServer() }.getOrNull() != null) return

            val pluginManager = proxy(PluginManager::class.java) { method ->
                when (method.name) {
                    "isPluginEnabled" -> false
                    else -> defaultValue(method.returnType)
                }
            }
            val server = proxy(Server::class.java) { method ->
                when (method.name) {
                    "getPluginManager" -> pluginManager
                    "getLogger" -> Logger.getLogger("ConditionalStringListResolverTest")
                    "getName" -> "KaMenuTest"
                    "getVersion" -> "1.16.5"
                    "getBukkitVersion" -> "1.16.5-R0.1-SNAPSHOT"
                    else -> defaultValue(method.returnType)
                }
            }
            Bukkit.setServer(server)
        }

        /** 创建只实现测试所需方法的 Bukkit 接口代理。 */
        private fun <T> proxy(type: Class<T>, handler: (java.lang.reflect.Method) -> Any?): T {
            return type.cast(
                Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, _ -> handler(method) }
            )
        }

        /** 为未使用的接口方法返回对应 JVM 基础类型默认值。 */
        private fun defaultValue(type: Class<*>): Any? {
            return when (type) {
                java.lang.Boolean.TYPE -> false
                java.lang.Byte.TYPE -> 0.toByte()
                java.lang.Short.TYPE -> 0.toShort()
                java.lang.Integer.TYPE -> 0
                java.lang.Long.TYPE -> 0L
                java.lang.Float.TYPE -> 0f
                java.lang.Double.TYPE -> 0.0
                java.lang.Character.TYPE -> '\u0000'
                else -> null
            }
        }
    }
}
