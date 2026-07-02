package org.katacr.kamenu

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
/**
 * KaMenu PlaceholderAPI 扩展
 * 支持的变量格式：
 * - %kamenu_data_key% - 获取玩家数据
 * - %kamenu_gdata_key% - 获取全局数据
 * - %kamenu_list_key% - 获取玩家列表数据（JSON 数组）
 * - %kamenu_glist_key% - 获取全局列表数据（JSON 数组）
 * - %kamenu_list_size_key% - 获取玩家列表长度
 * - %kamenu_glist_size_key% - 获取全局列表长度
 * - %kamenu_online_players% - 获取在线玩家名称列表（JSON 数组）
 * - %kamenu_meta_key% - 获取玩家元数据（内存缓存）
 * - %kamenu_hasstockitem_物品名% - 获取玩家背包中指定存储库物品的数量
 * - %kamenu_hasitem_[mats=材质;lore=描述;model=模型]% - 获取玩家背包中符合条件的物品数量
 */
class KaMenuExpansion(private val plugin: KaMenu) : PlaceholderExpansion() {

    // 变量的前缀，即 %kamenu_xxx% 中的 kamenu
    override fun getIdentifier(): String = "kamenu"

    override fun getAuthor(): String = plugin.description.authors.toString()

    override fun getVersion(): String = plugin.description.version

    // 必须返回 true，PAPI 才会加载这个扩展
    override fun persist(): Boolean = true

    // 核心逻辑：处理变量请求
    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        val paramsLower = params.lowercase()

        return when {
            // 在线玩家列表: %kamenu_online_players%
            paramsLower == "online_players" -> {
                DatabaseManager.encodeStringList(Bukkit.getOnlinePlayers().map { it.name })
            }

            // 玩家数据: %kamenu_data_key%
            paramsLower.startsWith("data_") -> {
                if (player == null) return null
                val key = params.substring(5)
                plugin.databaseManager.getPlayerData(player.uniqueId, key)
                    ?: plugin.languageManager.getMessage("papi.data_not_found", key)
            }

            // 全局数据: %kamenu_gdata_key%
            paramsLower.startsWith("gdata_") -> {
                val key = params.substring(6)
                plugin.databaseManager.getGlobalData(key)
                    ?: plugin.languageManager.getMessage("papi.data_not_found", key)
            }

            // 玩家列表长度: %kamenu_list_size_key%
            paramsLower.startsWith("list_size_") -> {
                if (player == null) return null
                val key = params.substring(10)
                plugin.databaseManager.getPlayerList(player.uniqueId, key).size.toString()
            }

            // 全局列表长度: %kamenu_glist_size_key%
            paramsLower.startsWith("glist_size_") -> {
                val key = params.substring(11)
                plugin.databaseManager.getGlobalList(key).size.toString()
            }

            // 玩家列表数据: %kamenu_list_key%
            paramsLower.startsWith("list_") -> {
                if (player == null) return null
                val key = params.substring(5)
                plugin.databaseManager.getPlayerListJson(player.uniqueId, key)
            }

            // 全局列表数据: %kamenu_glist_key%
            paramsLower.startsWith("glist_") -> {
                val key = params.substring(6)
                plugin.databaseManager.getGlobalListJson(key)
            }

            // 玩家元数据: %kamenu_meta_key%
            paramsLower.startsWith("meta_") -> {
                if (player == null) return null
                val key = params.substring(5)
                plugin.metaDataManager.getPlayerMeta(player.uniqueId, key)
            }

            // 存储库物品数量: %kamenu_hasstockitem_物品名%
            paramsLower.startsWith("hasstockitem_") -> {
                if (player == null) return "0"
                val itemName = params.substring(14)
                val count = player.player?.let { ConditionUtils.getPlayerStockItemCount(it, itemName) }
                count.toString()
            }

            // 背包物品数量: %kamenu_hasitem_[mats=材质;lore=描述;model=模型]%
            paramsLower.startsWith("hasitem_") -> {
                if (player == null) return "0"
                val paramsStr = params.substring(8)
                // 去除方括号
                val cleanParams = if (paramsStr.startsWith("[") && paramsStr.endsWith("]")) {
                    paramsStr.substring(1, paramsStr.length - 1)
                } else {
                    paramsStr
                }
                val count = player.player?.let { ConditionUtils.getPlayerItemCount(it, cleanParams) }
                count.toString()
            }

            else -> null
        }
    }
}
