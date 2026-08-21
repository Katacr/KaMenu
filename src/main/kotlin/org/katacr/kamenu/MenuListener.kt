@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import org.bukkit.Bukkit
import org.bukkit.event.block.Action
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.EquipmentSlot

/**
 * Bukkit 事件监听器。
 *
 * 根据 config.yml 和 item_bindings.yml，把交换副手、右键绑定物品、右键玩家等入口映射为打开菜单。
 * 同时负责玩家进服向导提示、更新提示，以及退出时清理菜单周期任务和临时元数据。
 */
class MenuListener(private val plugin: KaMenu) : Listener {

    @EventHandler
    fun onSwapHand(event: PlayerSwapHandItemsEvent) {
        val player = event.player
        val config = plugin.config

        // 检查 swap-hand 监听器是否启用
        val enabled = config.getBoolean("listeners.swap-hand.enabled", false)
        if (!enabled) return

        // 读取配置
        val menuName = config.getString("listeners.swap-hand.menu") ?: return
        val requireSneaking = config.getBoolean("listeners.swap-hand.require-sneaking", false)

        // 判断潜行条件
        if (requireSneaking && !player.isSneaking) return

        // 取消交换动作，打开菜单
        event.isCancelled = true
        MenuUI.openMenu(player, menuName, plugin.menuManager, plugin)
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        // 只处理右键点击物品
        if (event.action != Action.RIGHT_CLICK_AIR &&
            event.action != Action.RIGHT_CLICK_BLOCK) return
        if (event.hand != EquipmentSlot.HAND) return

        val player = event.player
        val item = event.item ?: return
        val menuName = plugin.itemBindingManager.findMenu(player, item) ?: return
        event.isCancelled = true
        MenuUI.openMenu(player, menuName, plugin.menuManager, plugin)
    }

    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        // 只处理右键玩家
        if (event.rightClicked !is org.bukkit.entity.Player) return
        val targetPlayer = event.rightClicked as org.bukkit.entity.Player
        
        val player = event.player
        val config = plugin.config

        // 检查右键玩家监听器是否启用
        val enabled = config.getBoolean("listeners.player-click.enabled", false)
        if (!enabled) return

        // 读取配置
        val menuName = config.getString("listeners.player-click.menu") ?: return
        val requireSneaking = config.getBoolean("listeners.player-click.require-sneaking", false)

        // 判断潜行条件（普通右键不潜行，Shift右键需要潜行）
        if (requireSneaking && !player.isSneaking) return

        // 设置 meta 数据：player 为被点击的玩家，供菜单内 {meta:player} 或后续动作读取。
        plugin.metaDataManager.setPlayerMeta(player.uniqueId, "player", targetPlayer.name)

        // 取消事件，打开菜单
        event.isCancelled = true
        MenuUI.openMenu(player, menuName, plugin.menuManager, plugin)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (player.isOp && plugin.menuManager.getAllMenuIds().isEmpty()) {
            KaScheduler.runPlayerLater(player, 80L, Runnable {
                if (!player.isOnline || !player.isOp || plugin.menuManager.getAllMenuIds().isNotEmpty()) {
                    return@Runnable
                }
                val message = plugin.languageManager.getMessage("command.guide_join_hint")
                MenuUI.sendMessage(player, MenuActions.parseClickableText(message))
            })
        }

        if (plugin.config.getBoolean("check-update", true)) {
            KaScheduler.runPlayerLater(player, 100L, Runnable {
                UpdateChecker.notifyIfUpdateAvailable(player)
            }) // 5秒延迟，避免被进服消息冲掉
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        plugin.itemBindingManager.clear(event.player.uniqueId)
        MenuUI.discardPlayer(event.player)
        DialogSessionManager.cancel(event.player)
        MenuTaskManager.cancel(event.player)
        MenuListManager.clear(event.player)
        InputCaptureManager.cancelForQuit(event.player.uniqueId)
        // 清理该玩家的元数据缓存
        plugin.metaDataManager.clearPlayerMeta(event.player.uniqueId)
    }

}
