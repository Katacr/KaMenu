package org.katacr.kamenu

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

/**
 * 聊天框输入捕获监听器。
 *
 * 玩家存在活跃捕获会话时取消聊天广播，并把消息投递回玩家线程处理；
 * 命令消息不会触发 AsyncPlayerChatEvent，因此始终正常放行。
 */
class ChatInputListener : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onAsyncChat(event: AsyncPlayerChatEvent) {
        val player: Player = event.player
        if (!InputCaptureManager.hasActiveSession(player.uniqueId)) {
            return
        }
        event.isCancelled = true
        val message = event.message
        KaScheduler.runPlayer(player) { InputCaptureManager.handleChatMessage(player, message) }
    }
}
