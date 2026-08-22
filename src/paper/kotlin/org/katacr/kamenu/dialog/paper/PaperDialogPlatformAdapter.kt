@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu.dialog.paper

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.title.Title
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.katacr.kamenu.KaMenu
import org.katacr.kamenu.KaScheduler
import org.katacr.kamenu.MenuManager
import org.katacr.kamenu.MenuActions
import org.katacr.kamenu.MenuRequirementChecker
import org.katacr.kamenu.DialogSessionManager
import org.katacr.kamenu.MenuTaskManager
import org.katacr.kamenu.MenuUI
import org.katacr.kamenu.PauseEntryListener
import org.katacr.kamenu.dialog.DialogPlatformAdapter
import org.katacr.kamenu.dialog.DialogDefinitionCompiler
import java.time.Duration

/**
 * 使用 Paper/Folia 原生 Dialog API 渲染 KaMenu 菜单。
 *
 * 菜单配置先由共享编译器转换，再交给 Paper 专用渲染器构建原生对象。
 */
class PaperDialogPlatformAdapter : DialogPlatformAdapter {
    override val platformName: String = "Paper"
    private lateinit var plugin: KaMenu
    private lateinit var compiler: DialogDefinitionCompiler
    private lateinit var renderer: PaperDialogRenderer

    override fun initialize(plugin: KaMenu) {
        this.plugin = plugin
        compiler = DialogDefinitionCompiler(plugin)
        renderer = PaperDialogRenderer(plugin)
        plugin.server.pluginManager.registerEvents(PauseEntryListener(plugin), plugin)
    }

    override fun openMenu(player: Player, menuId: String, manager: MenuManager, plugin: KaMenu) {
        val config = manager.getMenuConfig(menuId)
        if (config == null) {
            player.sendMessage(plugin.languageManager.getMessage("menu.not_found", menuId))
            return
        }
        openConfig(player, config, plugin, menuId)
    }

    override fun openConfig(player: Player, config: YamlConfiguration, plugin: KaMenu, contextId: String) {
        runOnPlayerThread(player) {
            openConfigDirect(player, config, plugin, contextId)
        }
    }

    override fun forceOpenMenu(player: Player, menuId: String, manager: MenuManager, plugin: KaMenu) {
        val config = manager.getMenuConfig(menuId)
        if (config == null) {
            player.sendMessage(plugin.languageManager.getMessage("menu.not_found", menuId))
            return
        }
        forceOpenConfig(player, config, plugin, menuId)
    }

    override fun forceOpenConfig(player: Player, config: YamlConfiguration, plugin: KaMenu, contextId: String) {
        runOnPlayerThread(player) {
            render(player, config, plugin, contextId)
        }
    }

    /** 执行普通打开生命周期；依赖检查必须早于 Events.Open。 */
    private fun openConfigDirect(player: Player, config: YamlConfiguration, plugin: KaMenu, contextId: String) {
        if (!MenuRequirementChecker.check(player, config, plugin)) {
            return
        }

        val openActions = config.getList("Events.Open")
        if (openActions.isNullOrEmpty()) {
            render(player, config, plugin, contextId)
            return
        }

        MenuActions.executeEvent(player, config, "Open", contextId).whenComplete { shouldStop, error ->
            if (error != null) {
                plugin.logger.severe(
                    plugin.languageManager.getMessage(
                        "paper_dialog.open_event_failed",
                        contextId,
                        error.message.toString()
                    )
                )
                error.printStackTrace()
                return@whenComplete
            }
            if (shouldStop != true) {
                runOnPlayerThread(player) {
                    render(player, config, plugin, contextId)
                }
            }
        }
    }

    /** 编译并显示菜单；编译失败保持原有 Paper 行为，由异常暴露配置错误。 */
    private fun render(player: Player, config: YamlConfiguration, plugin: KaMenu, contextId: String) {
        if (!MenuRequirementChecker.check(player, config, plugin)) {
            return
        }
        val definition = compiler.compile(player, config, contextId)
        renderer.show(player, config, contextId, definition)
        DialogSessionManager.attach(player, config, contextId)
        MenuTaskManager.attachMenu(player, config, contextId)
    }

    /** 在当前平台允许的玩家线程中执行渲染和生命周期操作。 */
    private fun runOnPlayerThread(player: Player, action: () -> Unit) {
        if (KaScheduler.isPlayerThread(player)) {
            action()
        } else {
            KaScheduler.runPlayer(player, Runnable(action))
        }
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
        val component = MenuActions.parseClickableText(rawText, player, config, { target, menuId ->
            MenuUI.openMenu(target, menuId, plugin.menuManager, plugin)
        }, contextId)
        player.sendMessage(component)
    }

    override fun itemName(item: ItemStack): Component = item.effectiveName()

    override fun itemLore(meta: ItemMeta): List<Component> = meta.lore().orEmpty()

    override fun itemModel(meta: ItemMeta): String? = meta.itemModel?.toString()

    /** Paper 可使用原生物品序列化，保留名称、Lore、组件和自定义模型等完整信息。 */
    override fun itemHover(item: ItemStack): HoverEvent<HoverEvent.ShowItem> = item.asHoverEvent()

    override fun shutdown() = Unit
}
