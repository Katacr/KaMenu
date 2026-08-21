package org.katacr.kamenu

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.AnvilInventory

/**
 * 铁砧输入捕获监听器。
 *
 * 仅在 InputCaptureManager 存在活跃的 ANVIL 类型会话时拦截铁砧点击和关闭事件；
 * 其他情况下完全不干涉 ContainerMenuListener 的正常铁砧菜单逻辑。
 */
class AnvilInputListener : Listener {

    @EventHandler(priority = EventPriority.HIGH)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? org.bukkit.entity.Player ?: return
        if (!InputCaptureManager.hasActiveSession(player.uniqueId)) return
        val inventory = event.clickedInventory ?: return
        if (inventory !is AnvilInventory) return
        // 结果槽（slot 2）左键确认
        if (event.rawSlot == 2) {
            event.isCancelled = true
            KaScheduler.runPlayer(player) {
                InputCaptureManager.handleAnvilConfirm(player, inventory)
            }
        } else {
            // 取消其他槽位点击，防止玩家取走提示物品
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? org.bukkit.entity.Player ?: return
        if (!InputCaptureManager.hasActiveSession(player.uniqueId)) return
        val inventory = event.inventory
        if (inventory !is AnvilInventory) return
        KaScheduler.runPlayer(player) {
            InputCaptureManager.handleAnvilClose(player, inventory)
        }
    }
}
