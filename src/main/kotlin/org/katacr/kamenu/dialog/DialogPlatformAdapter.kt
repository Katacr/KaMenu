package org.katacr.kamenu.dialog

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.katacr.kamenu.KaMenu
import org.katacr.kamenu.MenuManager

/**
 * 隔离 KaMenu 菜单入口与具体服务器核心的 Dialog API。
 *
 * 接口只暴露 Bukkit 和 KaMenu 自身类型，避免 Spigot 在加载共享代码时解析 Paper 专属类。
 */
interface DialogPlatformAdapter {
    val platformName: String

    /** 初始化平台实现所需的监听器与运行时状态。 */
    fun initialize(plugin: KaMenu)

    /** 按菜单 ID 打开普通菜单，并保留该平台支持的 Open 生命周期行为。 */
    fun openMenu(player: Player, menuId: String, manager: MenuManager, plugin: KaMenu)

    /** 打开内存 YAML 菜单。 */
    fun openConfig(player: Player, config: YamlConfiguration, plugin: KaMenu, contextId: String)

    /** 强制打开菜单，不执行 Open 生命周期。 */
    fun forceOpenMenu(player: Player, menuId: String, manager: MenuManager, plugin: KaMenu)

    /** 强制打开内存配置，不执行 Open 生命周期。 */
    fun forceOpenConfig(player: Player, config: YamlConfiguration, plugin: KaMenu, contextId: String)

    /** 关闭玩家当前显示的原生 Dialog。 */
    fun close(player: Player)

    /** 向玩家或控制台发送 Adventure 文本。 */
    fun sendMessage(sender: CommandSender, message: Component)

    /** 向玩家发送 ActionBar 文本。 */
    fun sendActionBar(player: Player, message: Component)

    /** 向玩家发送带 tick 时长的标题。 */
    fun showTitle(player: Player, title: Component, subtitle: Component, fadeIn: Int, stay: Int, fadeOut: Int)

    /** 解析并发送 KaMenu 可点击文本；动态 actions 由各平台回调机制承载。 */
    fun sendClickableText(player: Player, rawText: String, config: YamlConfiguration?, contextId: String?)

    /** 读取物品名称并保留客户端可本地化的翻译组件。 */
    fun itemName(item: ItemStack): Component

    /** 读取物品 Lore 并转换为 Adventure 组件。 */
    fun itemLore(meta: ItemMeta): List<Component>

    /** 读取物品 ItemModel 的完整命名空间键。 */
    fun itemModel(meta: ItemMeta): String?

    /** 构造物品悬浮事件；现代适配器可以额外写入完整物品数据。 */
    fun itemHover(item: ItemStack): HoverEvent<HoverEvent.ShowItem> =
        HoverEvent.showItem(Key.key(item.type.key.toString()), item.amount)

    /** 插件关闭时释放平台实现持有的状态。 */
    fun shutdown()
}
