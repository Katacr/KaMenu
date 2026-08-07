package org.katacr.kamenu.container

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import java.util.UUID

/**
 * KaMenu 虚拟容器库存的可信身份载体。
 *
 * 监听器不使用标题识别菜单，而是校验玩家、会话 ID、菜单 ID 和加载代际。
 */
class ContainerMenuHolder(
    val sessionId: UUID,
    val playerId: UUID,
    val menuId: String,
    val generation: Long
) : InventoryHolder {
    private lateinit var currentInventory: Inventory

    /** 返回当前会话使用的库存；标题重绑时该引用会替换。 */
    override fun getInventory(): Inventory = currentInventory

    /** 绑定初次创建或标题兼容重建后的库存。 */
    fun bindInventory(inventory: Inventory) {
        currentInventory = inventory
    }
}
