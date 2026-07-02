package org.katacr.kamenu

import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * 统一文本解析入口
 * 负责输入变量、内置变量与 PAPI 变量的串联解析。
 */
object TextResolver {
    private var plugin: KaMenu? = null
    private var languageManager: LanguageManager? = null

    private val dataPattern = Regex("\\{data:([^}]+)}")
    private val globalDataPattern = Regex("\\{gdata:([^}]+)}")
    private val listPattern = Regex("\\{list:([^}]+)}")
    private val globalListPattern = Regex("\\{glist:([^}]+)}")
    private val metaPattern = Regex("\\{meta:([^}]+)}")
    private val argPattern = Regex("\\{arg:([^}]+)}")
    private val jsPattern = Regex("\\{js:([^}]+)}")

    fun setPlugin(kamenu: KaMenu) {
        plugin = kamenu
    }

    fun setLanguageManager(manager: LanguageManager) {
        languageManager = manager
    }

    fun resolve(player: Player, text: String?, variables: Map<String, String> = emptyMap()): String {
        var result = text ?: return ""

        variables.forEach { (key, value) ->
            if (!key.startsWith("item.") && !key.startsWith("list.") && !key.startsWith("arg:")) {
                result = result.replace("\$($key)", value)
            }
        }

        variables.forEach { (key, value) ->
            result = result.replace("{$key}", value)
        }

        result = result.replace(argPattern) { match ->
            variables["arg:${match.groupValues[1]}"] ?: ""
        }

        val currentPlugin = plugin
        if (currentPlugin != null) {
            result = result.replace("{language}", currentPlugin.config.getString("language", "zh_CN") ?: "zh_CN")

            result = result.replace(dataPattern) { match ->
                val key = match.groupValues[1]
                currentPlugin.databaseManager.getPlayerData(player.uniqueId, key)
                    ?: languageManager?.getMessage("papi.data_not_found", key)
                    ?: "null"
            }
            result = result.replace(globalDataPattern) { match ->
                val key = match.groupValues[1]
                currentPlugin.databaseManager.getGlobalData(key)
                    ?: languageManager?.getMessage("papi.data_not_found", key)
                    ?: "null"
            }
            result = result.replace(listPattern) { match ->
                val key = match.groupValues[1]
                currentPlugin.databaseManager.getPlayerListJson(player.uniqueId, key)
            }
            result = result.replace(globalListPattern) { match ->
                val key = match.groupValues[1]
                currentPlugin.databaseManager.getGlobalListJson(key)
            }
            result = result.replace(metaPattern) { match ->
                val key = match.groupValues[1]
                currentPlugin.metaDataManager.getPlayerMeta(player.uniqueId, key)
            }
        }

        result = result.replace(jsPattern) { match ->
            val script = match.groupValues[1].trim()
            if (script.isEmpty() || !JavaScriptManager.isAvailable()) {
                ""
            } else {
                JavaScriptManager.evaluateWithContext(player, script)?.toString() ?: ""
            }
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, result)
            } catch (_: Exception) {
                // PAPI 解析失败时保留原值
            }
        }

        return result
    }

    fun resolve(
        player: Player,
        text: String?,
        variables: Map<String, String> = emptyMap(),
        dynamicResolver: (String) -> String?
    ): String {
        var result = resolve(player, text, variables)
        result = result.replace(Regex("\\{([^{}]+)}")) { match ->
            dynamicResolver(match.groupValues[1]) ?: match.value
        }
        return result
    }
}
