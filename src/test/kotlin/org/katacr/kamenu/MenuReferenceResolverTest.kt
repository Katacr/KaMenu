package org.katacr.kamenu

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** 验证菜单引用的作用域、模板参数、嵌套展开和失败保护。 */
class MenuReferenceResolverTest {
    @Test
    fun `resolves shared config and self references case insensitively`() {
        val config = YamlConfiguration().apply {
            set("References.currency", "&6Coins")
            set("References.lines", listOf("first", "second"))
            set("Title", "&8Shop")
            set("Buttons.shop.data.price", 100)
        }
        val variables = mapOf("self:id" to "shop", "self:path" to "Buttons.shop")

        assertEquals("&6Coins", MenuReferenceResolver.resolve("{ref:CURRENCY}", config, variables))
        assertEquals("first\nsecond", MenuReferenceResolver.resolve("{ref:lines}", config, variables))
        assertEquals("&8Shop", MenuReferenceResolver.resolve("{config:title}", config, variables))
        assertEquals("shop costs 100", MenuReferenceResolver.resolve("{self:id} costs {self:data.price}", config, variables))
        assertEquals("Buttons.shop", MenuReferenceResolver.resolve("{self:path}", config, variables))
    }

    @Test
    fun `resolves reference arguments nested references and quoted semicolons`() {
        val config = YamlConfiguration().apply {
            set("References.currency", "coins")
            set("References.buy", "Buy {refarg:0} {ref:currency} for {refarg:1}")
        }

        assertEquals(
            "Buy 5 coins for price;vip",
            MenuReferenceResolver.resolve("{ref:[buy;5;`price;vip`]}", config)
        )
    }

    @Test
    fun `rejects missing sections and circular references`() {
        val config = YamlConfiguration().apply {
            set("References.first", "{ref:second}")
            set("References.second", "{ref:first}")
            set("References.section.value", "nested")
        }

        assertThrows(MenuReferenceResolver.MenuReferenceException::class.java) {
            MenuReferenceResolver.resolve("{ref:missing}", config)
        }
        assertThrows(MenuReferenceResolver.MenuReferenceException::class.java) {
            MenuReferenceResolver.resolve("{ref:section}", config)
        }
        assertThrows(MenuReferenceResolver.MenuReferenceException::class.java) {
            MenuReferenceResolver.resolve("{ref:first}", config)
        }
    }
}
