@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerSwapHandItemsEvent

class MenuListener(private val plugin: KaMenu) : Listener {

    @EventHandler
    fun onSwapHand(event: PlayerSwapHandItemsEvent) {
        val player = event.player
        val config = plugin.config

        // 读取配置
        val enabled = config.getBoolean("listeners.swap-hand.enabled", false)
        val requireSneak = config.getBoolean("listeners.swap-hand.require-sneaking", true)
        val menuName = config.getString("listeners.swap-hand.menu") ?: return

        if (enabled) {
            // 判断潜行条件
            if (requireSneak && !player.isSneaking) return

            // 取消交换动作的动画，直接打开菜单
            event.isCancelled = true
            MenuUI.openMenu(player, menuName, plugin.menuManager, plugin)
        }
    }
}