package org.katacr.kamenu

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.AnvilInventory

/**
 * 铁砧输入捕获监听器。
 *
 * 捕获期间冻结整个视图：取消顶部铁砧全部槽位与玩家背包的点击和拖拽，
 * 彻底阻止提示物品被取出或外部物品被放入；仅结果槽（slot 2）触发确认回调。
 */
class AnvilInputListener : Listener {

    @EventHandler(priority = EventPriority.HIGH)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val expectedTop = InputCaptureManager.captureAnvilTop(player.uniqueId) ?: return
        if (event.view.topInventory !== expectedTop) return
        // 冻结整个视图：无论点击顶部还是底部物品栏一律取消
        event.isCancelled = true
        if (event.clickedInventory !== expectedTop) return
        if (event.rawSlot != RESULT_SLOT) return
        val anvil = expectedTop as? AnvilInventory ?: return
        KaScheduler.runPlayer(player) {
            InputCaptureManager.handleAnvilConfirm(player, anvil)
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        val expectedTop = InputCaptureManager.captureAnvilTop(player.uniqueId) ?: return
        if (event.view.topInventory !== expectedTop) return
        event.isCancelled = true
    }

    private fun InputCaptureManager.captureAnvilTop(playerId: java.util.UUID): org.bukkit.inventory.Inventory? =
        activeSession(playerId)?.anvilView?.topInventory
}

private const val RESULT_SLOT = 2
