package org.katacr.kamenu

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer

/**
 * KaMenu PlaceholderAPI 扩展
 * 支持的变量格式：
 * - %kamenu_data_key% - 获取玩家数据
 * - %kamenu_gdata_key% - 获取全局数据
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

            else -> null
        }
    }
}
