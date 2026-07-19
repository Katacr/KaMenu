@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import io.papermc.paper.connection.PlayerGameConnection
import io.papermc.paper.event.player.PlayerCustomClickEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

/**
 * ESC 暂停菜单数据包入口点击监听器。
 *
 * 数据包 Dialog 只负责发送固定 custom click；真正的菜单打开逻辑始终在插件运行期执行。
 */
class PauseEntryListener(private val plugin: KaMenu) : Listener {

    @EventHandler
    fun onCustomClick(event: PlayerCustomClickEvent) {
        if (event.identifier != PauseEntryDatapackManager.ACTION_KEY) {
            return
        }

        val player = (event.commonConnection as? PlayerGameConnection)?.player ?: return
        val payload = event.tag?.string()
        val targetId = event.dialogResponseView?.getText("target")
            ?: readPayloadValue(payload, "target")
            ?: event.dialogResponseView?.getText("button")?.let { "button:$it" }
            ?: readPayloadValue(payload, "button")?.let { "button:$it" }
        val response = event.dialogResponseView
        KaScheduler.runPlayer(player, Runnable {
            plugin.pauseEntryDatapackManager.handleRegisteredTarget(player, targetId, response)
        })
    }

    /**
     * 从原始 SNBT 中兜底读取正文或按钮的目标 id。
     *
     * 新版 Paper 可通过 DialogResponseView 读取 additions；保留该解析是为了兼容不同构建。
     */
    private fun readPayloadValue(payload: String?, key: String): String? {
        if (payload.isNullOrBlank()) {
            return null
        }
        return Regex("""\b${Regex.escape(key)}\s*:\s*"((?:\\.|[^"\\])*)"""")
            .find(payload)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace("\\\"", "\"")
            ?.replace("\\\\", "\\")
    }
}
