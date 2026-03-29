@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import org.bukkit.event.block.Action
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.EquipmentSlot

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

        // 检查物品是否有 lore
        if (!item.hasItemMeta() || !item.itemMeta.hasLore()) return

        val lore = item.itemMeta.lore ?: return
        val itemMaterial = item.type.name

        val config = plugin.config

        // 遍历 listeners.item-lore 下的所有配置项
        val itemLoreSection = config.getConfigurationSection("listeners.item-lore") ?: return

        for (key in itemLoreSection.getKeys(false)) {
            // 检查此配置是否启用
            if (!config.getBoolean("listeners.item-lore.$key.enabled", false)) continue

            // 获取配置参数
            val targetMaterial = config.getString("listeners.item-lore.$key.material") ?: continue
            val targetLore = config.getString("listeners.item-lore.$key.target-lore") ?: continue
            val menuName = config.getString("listeners.item-lore.$key.menu") ?: continue
            val requireSneaking = config.getBoolean("listeners.item-lore.$key.require-sneaking", false)

            // 判断潜行条件
            if (requireSneaking && !player.isSneaking) continue

            // 检查 material 是否匹配（使用规范化的材质匹配）
            if (!isMaterialMatch(itemMaterial, targetMaterial)) continue

            // 检查物品 lore 是否包含目标文本
            val hasTargetLore = lore.any { loreLine ->
                loreLine.contains(targetLore)
            }

            if (hasTargetLore) {
                // 取消事件，打开菜单
                event.isCancelled = true
                MenuUI.openMenu(player, menuName, plugin.menuManager, plugin)
                return // 找到匹配后立即返回
            }
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        // 清理该玩家的元数据缓存
        plugin.metaDataManager.clearPlayerMeta(event.player.uniqueId)
    }

    /**
     * 检查两个材质名称是否匹配（使用规范化比较）
     * @param itemMaterial 物品的材质名称（Material.name）
     * @param targetMaterial 配置中的材质名称（可能包含短杠、空格、混合大小写）
     * @return 是否匹配
     */
    private fun isMaterialMatch(itemMaterial: String, targetMaterial: String): Boolean {
        // 尝试规范化目标材质名称并匹配
        val normalizedTarget = MaterialUtils.normalizeMaterialName(targetMaterial)
        return itemMaterial.equals(normalizedTarget, ignoreCase = true)
    }
}
