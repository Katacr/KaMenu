package org.katacr.kamenu

import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player

/**
 * 检查菜单在渲染前声明的外部依赖。
 *
 * Paper 与 Spigot 共用该入口，当前负责 `Settings.need_placeholder` 扩展检查和管理员下载提示。
 */
object MenuRequirementChecker {
    /** 所有依赖可用时返回 true；缺失时发送本地化提示并阻止菜单渲染。 */
    fun check(player: Player, config: YamlConfiguration, plugin: KaMenu): Boolean {
        val requiredExtensions = config.getList("Settings.need_placeholder")
            ?.filterIsInstance<String>()
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            .orEmpty()
        if (requiredExtensions.isEmpty()) {
            return true
        }

        val placeholderPlugin = Bukkit.getPluginManager().getPlugin("PlaceholderAPI")
        val missingExtensions = if (placeholderPlugin?.isEnabled != true) {
            requiredExtensions
        } else {
            requiredExtensions.filter { extension ->
                runCatching { !PlaceholderAPI.isRegistered(extension) }.getOrDefault(true)
            }
        }
        if (missingExtensions.isEmpty()) {
            return true
        }

        if (player.hasPermission("kamenu.admin")) {
            val prefix = plugin.languageManager.getMessage("menu.missing_papi_extensions_prefix")
            val extensions = missingExtensions.joinToString(", ") { extension ->
                val command = "/papi ecloud download $extension"
                "<text=&e[$extension];hover=&7$command;command=$command>"
            }
            MenuUI.sendClickableText(player, "$prefix $extensions", config, "requirements")
        } else {
            MenuUI.sendMessage(
                player,
                TextParser.parseText(plugin.languageManager.getMessage("menu.missing_dependencies"), player)
            )
        }
        return false
    }
}
