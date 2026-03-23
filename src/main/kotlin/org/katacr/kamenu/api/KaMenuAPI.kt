package org.katacr.kamenu.api

import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin

/**
 * KaMenu 公开 API
 * 供其他插件调用以打开 KaMenu 菜单
 */
object KaMenuAPI {
    private var plugin: KaMenuPlugin? = null

    /**
     * 初始化 API（由 KaMenu 插件内部调用）
     */
    internal fun init(plugin: KaMenuPlugin) {
        this.plugin = plugin
    }

    /**
     * 打开指定的菜单
     * @param player 目标玩家
     * @param menuId 菜单 ID（如 "main_menu" 或 "shop/weapons"）
     * @return 是否成功打开菜单
     */
    @JvmStatic
    fun openMenu(player: Player, menuId: String): Boolean {
        if (plugin == null) {
            return false
        }
        try {
            val menuManager = plugin!!.menuManager
            org.katacr.kamenu.MenuUI.openMenu(player, menuId, menuManager, plugin!!)
            return true
        } catch (e: Exception) {
            plugin!!.logger.warning("打开菜单失败: $menuId, 错误: ${e.message}")
            return false
        }
    }

    /**
     * 检查 KaMenu 是否已加载
     * @return KaMenu 是否可用
     */
    @JvmStatic
    fun isAvailable(): Boolean = plugin != null

    /**
     * 获取 KaMenu 插件实例
     * @return KaMenu 插件实例，如果未加载则返回 null
     */
    @JvmStatic
    fun getPlugin(): KaMenuPlugin? = plugin
}

/**
 * KaMenu 插件类型别名
 */
private typealias KaMenuPlugin = org.katacr.kamenu.KaMenu
