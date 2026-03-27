package org.katacr.kamenu

import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack

/**
 * 物品管理器
 * 用于管理保存的物品
 */
class ItemManager(private val plugin: KaMenu) {

    /**
     * 保存物品到数据库
     * @param itemName 物品名称
     * @param item 物品
     * @param playerUuid 保存者UUID（可选，用于记录保存者）
     * @return 是否保存成功
     */
    fun saveItem(itemName: String, item: ItemStack, playerUuid: String? = null): Boolean {
        if (itemName.isBlank()) {
            return false
        }

        // 克隆物品并将数量设置为1，避免保存数量信息
        val itemToSave = item.clone().apply {
            amount = 1
        }
        val itemBase64 = SerializationUtil.itemToBase64(itemToSave)

        plugin.databaseManager.connection.use { conn ->
            val dbType = plugin.config.getString("storage.type", "sqlite") ?: "sqlite"
            val isMySQL = dbType.equals("mysql", ignoreCase = true)

            val sql = if (isMySQL) {
                """
                INSERT INTO saved_items (item_name, item_data, saved_by, update_time)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    item_data = VALUES(item_data),
                    saved_by = VALUES(saved_by),
                    update_time = VALUES(update_time)
            """.trimIndent()
            } else {
                """
                INSERT INTO saved_items (item_name, item_data, saved_by, update_time)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(item_name) DO UPDATE SET
                    item_data = excluded.item_data,
                    saved_by = excluded.saved_by,
                    update_time = excluded.update_time
            """.trimIndent()
            }

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, itemName)
                stmt.setString(2, itemBase64)
                stmt.setString(3, playerUuid)
                stmt.setLong(4, System.currentTimeMillis())
                return stmt.executeUpdate() > 0
            }
        }
    }

    /**
     * 从数据库获取物品
     * @param itemName 物品名称
     * @return 物品，如果不存在返回null
     */
    fun getItem(itemName: String): ItemStack? {
        plugin.databaseManager.connection.use { conn ->
            val sql = "SELECT item_data FROM saved_items WHERE item_name = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, itemName)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) {
                        val itemData = rs.getString("item_data")
                        SerializationUtil.itemFromBase64(itemData)
                    } else {
                        null
                    }
                }
            }
        }
    }

    /**
     * 检查物品是否存在
     * @param itemName 物品名称
     * @return 是否存在
     */
    fun itemExists(itemName: String): Boolean {
        plugin.databaseManager.connection.use { conn ->
            val sql = "SELECT COUNT(*) as count FROM saved_items WHERE item_name = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, itemName)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) {
                        rs.getInt("count") > 0
                    } else {
                        false
                    }
                }
            }
        }
    }

    /**
     * 删除保存的物品
     * @param itemName 物品名称
     * @return 是否删除成功
     */
    fun deleteItem(itemName: String): Boolean {
        plugin.databaseManager.connection.use { conn ->
            val sql = "DELETE FROM saved_items WHERE item_name = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, itemName)
                return stmt.executeUpdate() > 0
            }
        }
    }

    /**
     * 获取所有保存的物品名称列表
     * @return 物品名称列表
     */
    fun getAllItemNames(): List<String> {
        val names = mutableListOf<String>()
        plugin.databaseManager.connection.use { conn ->
            val sql = "SELECT item_name FROM saved_items ORDER BY item_name"
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        names.add(rs.getString("item_name"))
                    }
                }
            }
        }
        return names
    }
}
