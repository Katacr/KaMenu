package org.katacr.kamenu.api

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.katacr.kamenu.MenuActions
import org.katacr.kamenu.MenuUI

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
            MenuUI.openMenu(player, menuId, menuManager, plugin!!)
            return true
        } catch (e: Exception) {
            plugin!!.logger.warning("打开菜单失败: $menuId, 错误: ${e.message}")
            return false
        }
    }

    /**
     * 从内存 YAML 字符串打开菜单。
     */
    @JvmStatic
    @JvmOverloads
    fun openYaml(player: Player, yaml: String, contextId: String = "external"): Boolean {
        val currentPlugin = plugin ?: return false
        return try {
            val config = YamlConfiguration()
            config.loadFromString(yaml)
            openConfig(player, config, contextId)
        } catch (e: Exception) {
            currentPlugin.logger.warning("外部菜单 YAML 解析失败: contextId=$contextId, 错误: ${e.message}")
            false
        }
    }

    /**
     * 从内存配置打开菜单。
     */
    @JvmStatic
    @JvmOverloads
    fun openConfig(player: Player, config: YamlConfiguration, contextId: String = "external"): Boolean {
        val currentPlugin = plugin ?: return false
        return try {
            if (Bukkit.isPrimaryThread()) {
                MenuUI.openConfig(player, config, currentPlugin, contextId)
            } else {
                Bukkit.getScheduler().runTask(currentPlugin, Runnable {
                    MenuUI.openConfig(player, config, currentPlugin, contextId)
                })
            }
            true
        } catch (e: Exception) {
            currentPlugin.logger.warning("打开外部菜单失败: contextId=$contextId, 错误: ${e.message}")
            false
        }
    }

    /**
     * 注册外部动作命名空间。
     */
    @JvmStatic
    fun registerActionHandler(namespace: String, handler: KaMenuActionHandler): Boolean {
        val normalized = namespace.trim().lowercase()
        if (normalized.isEmpty() || normalized.contains(":")) {
            plugin?.logger?.warning("外部 action handler 注册失败: namespace 非法: $namespace")
            return false
        }
        return MenuActions.registerExternalActionHandler(normalized, handler)
    }

    /**
     * 注销外部动作命名空间。
     */
    @JvmStatic
    fun unregisterActionHandler(namespace: String) {
        val normalized = namespace.trim().lowercase()
        if (normalized.isNotEmpty()) {
            MenuActions.unregisterExternalActionHandler(normalized)
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
