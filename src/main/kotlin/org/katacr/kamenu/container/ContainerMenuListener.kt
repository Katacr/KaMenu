@file:Suppress("DEPRECATION")

package org.katacr.kamenu.container

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

/** 拦截 KaMenu 容器库存交互，并将可信点击上下文转交给 [ContainerMenuService]。 */
class ContainerMenuListener(private val service: ContainerMenuService) : Listener {

    /** 取消容器视图内的所有库存事务，只让顶部有效按钮进入动作系统。 */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val inventory = event.view.topInventory
        val active = service.ownsInventory(player, inventory)
        if (!active && !service.isManagedInventory(inventory)) return
        event.isCancelled = true
        if (!active) return
        val slot = event.rawSlot
        if (slot !in 0 until inventory.size) return
        if (event.currentItem?.type == null || event.currentItem?.type == Material.AIR) return

        val (clickType, hotbarButton) = mapClick(event.click, event.hotbarButton) ?: return
        service.handleClick(player, inventory, slot, clickType, hotbarButton)
    }

    /** 禁止光标拖拽向容器或玩家库存批量写入物品。 */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        val inventory = event.view.topInventory
        if (service.ownsInventory(player, inventory) || service.isManagedInventory(inventory)) {
            event.isCancelled = true
        }
    }

    /** 捕获真实铁砧的重命名文本，并让 KaMenu 控制结果槽物品。 */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPrepareAnvil(event: PrepareAnvilEvent) {
        val inventory = event.inventory
        val player = inventory.viewers.firstOrNull() as? Player ?: return
        if (!service.ownsInventory(player, inventory)) return
        event.result = service.prepareAnvilResult(player, inventory, inventory.renameText.orEmpty())
    }

    /** 玩家关闭容器时结束对应会话。 */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val inventory = event.view.topInventory
        if (!service.ownsInventory(player, inventory)) return
        service.handleClose(player, inventory)
    }

    /** 玩家离线时丢弃会话和刷新任务。 */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        service.discard(event.player)
    }

    /** 玩家加入时清理异常停服前可能遗留的展示物品。 */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        service.removeLeakedItems(event.player)
    }

    /** 将 Bukkit ClickType 映射为配置中稳定的容器点击键。 */
    private fun mapClick(click: ClickType, hotbarIndex: Int): Pair<ContainerClickType, Int?>? {
        return when (click) {
            ClickType.LEFT -> ContainerClickType.LEFT to null
            ClickType.RIGHT -> ContainerClickType.RIGHT to null
            ClickType.SHIFT_LEFT -> ContainerClickType.SHIFT_LEFT to null
            ClickType.SHIFT_RIGHT -> ContainerClickType.SHIFT_RIGHT to null
            ClickType.MIDDLE -> ContainerClickType.MIDDLE to null
            ClickType.DROP -> ContainerClickType.DROP to null
            ClickType.CONTROL_DROP -> ContainerClickType.CONTROL_DROP to null
            ClickType.DOUBLE_CLICK -> ContainerClickType.DOUBLE_CLICK to null
            ClickType.SWAP_OFFHAND -> ContainerClickType.OFFHAND to null
            ClickType.NUMBER_KEY -> {
                val number = hotbarIndex + 1
                val type = ContainerClickType.entries.firstOrNull { it.configKey == "number_key_$number" }
                    ?: ContainerClickType.NUMBER_KEY
                type to number
            }
            else -> null
        }
    }
}
