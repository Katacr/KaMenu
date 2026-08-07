package org.katacr.kamenu.dialog

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.md_5.bungee.api.ChatMessageType
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.katacr.kamenu.KaMenu
import org.katacr.kamenu.MenuActions
import org.katacr.kamenu.MenuManager
import org.katacr.kamenu.TextParser
import java.util.Locale

/**
 * 没有原生 Dialog API 时的降级实现。
 *
 * 低版本核心仍可使用 Container、actions、变量和自定义指令；Dialog 菜单不会被强行转换成
 * 其他界面，而是向执行者发送本地化的不支持提示。这样配置错误和平台能力不足不会阻止
 * 整个插件加载。
 */
class NoDialogPlatformAdapter : DialogPlatformAdapter {
    private val legacySerializer = LegacyComponentSerializer.legacySection()

    override val platformName: String = "Unavailable"

    override fun initialize(plugin: KaMenu) = Unit

    override fun openMenu(player: Player, menuId: String, manager: MenuManager, plugin: KaMenu) {
        sendUnsupported(player, plugin)
    }

    override fun openConfig(player: Player, config: YamlConfiguration, plugin: KaMenu, contextId: String) {
        sendUnsupported(player, plugin)
    }

    override fun forceOpenMenu(player: Player, menuId: String, manager: MenuManager, plugin: KaMenu) {
        sendUnsupported(player, plugin)
    }

    override fun forceOpenConfig(player: Player, config: YamlConfiguration, plugin: KaMenu, contextId: String) {
        sendUnsupported(player, plugin)
    }

    override fun close(player: Player) = Unit

    override fun sendMessage(sender: CommandSender, message: Component) {
        if (sender is Player) {
            sender.spigot().sendMessage(*BungeeComponentSerializer.get().serialize(message))
        } else {
            sender.sendMessage(legacySerializer.serialize(message))
        }
    }

    override fun sendActionBar(player: Player, message: Component) {
        player.spigot().sendMessage(
            ChatMessageType.ACTION_BAR,
            *BungeeComponentSerializer.get().serialize(message)
        )
    }

    override fun showTitle(
        player: Player,
        title: Component,
        subtitle: Component,
        fadeIn: Int,
        stay: Int,
        fadeOut: Int
    ) {
        player.sendTitle(
            legacySerializer.serialize(title),
            legacySerializer.serialize(subtitle),
            fadeIn.coerceAtLeast(0),
            stay.coerceAtLeast(0),
            fadeOut.coerceAtLeast(0)
        )
    }

    override fun sendClickableText(
        player: Player,
        rawText: String,
        config: YamlConfiguration?,
        contextId: String?
    ) {
        sendMessage(player, MenuActions.parseClickableText(rawText, player, config, null))
    }

    override fun itemName(item: ItemStack): Component {
        val meta = item.itemMeta
        val displayName = meta?.getDisplayName().orEmpty()
        if (displayName.isNotEmpty()) {
            return TextParser.parseText(displayName)
        }
        val fallback = item.type.name.lowercase(Locale.ROOT)
            .split('_')
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercaseChar() } }
        return Component.text(fallback)
    }

    override fun itemLore(meta: ItemMeta): List<Component> =
        meta.getLore().orEmpty().map { TextParser.parseText(it) }

    override fun itemModel(meta: ItemMeta): String? = null

    override fun shutdown() = Unit

    private fun sendUnsupported(player: Player, plugin: KaMenu) {
        sendMessage(player, TextParser.parseText(plugin.languageManager.getMessage("dialog.unsupported")))
    }
}
