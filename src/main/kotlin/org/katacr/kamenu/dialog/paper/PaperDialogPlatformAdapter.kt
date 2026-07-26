@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu.dialog.paper

import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.katacr.kamenu.KaMenu
import org.katacr.kamenu.MenuManager
import org.katacr.kamenu.MenuActions
import org.katacr.kamenu.PaperMenuUI
import org.katacr.kamenu.dialog.DialogPlatformAdapter
import java.time.Duration

/**
 * 使用 Paper/Folia 原生 Dialog API 渲染 KaMenu 菜单。
 *
 * 当前实现委托给既有 Paper 渲染器，确保 1.7.0 重构不改变原有菜单行为。
 */
class PaperDialogPlatformAdapter : DialogPlatformAdapter {
    override val platformName: String = "Paper"
    private lateinit var plugin: KaMenu

    override fun initialize(plugin: KaMenu) {
        this.plugin = plugin
        PaperMenuUI.init(plugin)
    }

    override fun openMenu(player: Player, menuId: String, manager: MenuManager, plugin: KaMenu) {
        PaperMenuUI.openMenu(player, menuId, manager, plugin)
    }

    override fun openConfig(player: Player, config: YamlConfiguration, plugin: KaMenu, contextId: String) {
        PaperMenuUI.openConfig(player, config, plugin, contextId)
    }

    override fun forceOpenMenu(player: Player, menuId: String, manager: MenuManager, plugin: KaMenu) {
        PaperMenuUI.forceOpenMenu(player, menuId, manager, plugin)
    }

    override fun forceOpenConfig(player: Player, config: YamlConfiguration, plugin: KaMenu, contextId: String) {
        PaperMenuUI.forceOpenConfig(player, config, plugin, contextId)
    }

    override fun close(player: Player) {
        player.closeDialog()
    }

    override fun sendMessage(sender: CommandSender, message: Component) {
        sender.sendMessage(message)
    }

    override fun sendActionBar(player: Player, message: Component) {
        player.sendActionBar(message)
    }

    override fun showTitle(
        player: Player,
        title: Component,
        subtitle: Component,
        fadeIn: Int,
        stay: Int,
        fadeOut: Int
    ) {
        val times = Title.Times.times(
            Duration.ofMillis(fadeIn.coerceAtLeast(0) * 50L),
            Duration.ofMillis(stay.coerceAtLeast(0) * 50L),
            Duration.ofMillis(fadeOut.coerceAtLeast(0) * 50L)
        )
        player.showTitle(Title.title(title, subtitle, times))
    }

    override fun sendClickableText(player: Player, rawText: String, config: YamlConfiguration?, contextId: String?) {
        val component = MenuActions.parseClickableText(rawText, player, config) { target, menuId ->
            PaperMenuUI.openMenu(target, menuId, plugin.menuManager, plugin)
        }
        player.sendMessage(component)
    }

    override fun itemName(item: ItemStack): Component = item.effectiveName()

    override fun itemLore(meta: ItemMeta): List<Component> = meta.lore().orEmpty()

    override fun itemModel(meta: ItemMeta): String? = meta.itemModel?.toString()

    override fun shutdown() = Unit
}
