package org.katacr.kamenu.container

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.katacr.kamenu.KaMenu
import org.katacr.kamenu.MenuItemFactory
import org.katacr.kamenu.MenuItemSpec

/** 将容器按钮的动态 display 定义转换为 Bukkit ItemStack。 */
class ContainerItemRenderer(private val plugin: KaMenu) {
    private val itemFactory = MenuItemFactory(plugin)

    /** 为指定玩家渲染一个按钮物品。 */
    fun render(
        player: Player,
        config: YamlConfiguration,
        menuId: String,
        button: ContainerButtonDefinition,
        display: ContainerItemDefinition = button.display,
        variables: Map<String, String> = emptyMap()
    ): ItemStack {
        val values = ContainerValueResolver(player, config, variables)
        val spec = MenuItemSpec(
            source = values.string(display["material"], "PAPER"),
            amount = values.int(display["amount"], 1),
            name = display["name"]?.let { values.string(it) },
            lore = display["lore"]?.let(values::inlineConditionalStrings),
            customModelData = display["custom_model_data"]?.let { values.string(it) },
            itemModel = display["item_model"]?.let { values.string(it) },
            skullOwner = display["skull_owner"]?.let { values.string(it) },
            skullTexture = display["skull_texture"]?.let { values.string(it) },
            glow = values.boolean(display["glow"]),
            unbreakable = display["unbreakable"]?.let { values.boolean(it) },
            enchantments = values.integerMap(display["enchantments"]),
            itemFlags = values.strings(display["item_flags"]).toSet()
        )
        return itemFactory.create(player, spec, menuId, button.id)
    }
}
