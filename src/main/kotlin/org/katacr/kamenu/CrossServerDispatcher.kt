package org.katacr.kamenu

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.UUID

/**
 * 跨服动作分发器，通过 kaproxy:main 插件消息通道把 KaMenu 动作转发到其他后端。
 *
 * KaProxy 根据自身 config 的服务器范围列表路由，后端收到后本地求值选择器并执行。
 * 本类自行实现协议编解码，不引用 KaProxy 的 Java 类，保证 Java 16 / Spigot 1.16.5 编译基线不受影响。
 */
class CrossServerDispatcher(private val plugin: KaMenu) {

    companion object {
        private const val CHANNEL = "kaproxy:main"
        private const val MAGIC = 0x4B415058
        private const val VERSION: Short = 1
        private const val MODULE_KAMENU = "kamenu"
        private const val ACTION_DISPATCH = "dispatch"
        private const val ACTION_EXECUTE = "execute"

        /** 跨服允许执行的动作前缀白名单。 */
        val ALLOWED_ACTIONS: Set<String> = setOf(
            "tell:", "actionbar:", "title:", "sound:", "hovertext:",
            "money:", "points:",
            "data:", "gdata:", "tmpdata:", "meta:",
            "set-data:", "set-gdata:", "set-meta:"
        )
    }

    @Volatile
    private var enabled = false

    private val listener = object : PluginMessageListener {
        override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
            if (channel != CHANNEL) return
            handleIncomingPacket(message)
        }
    }


    /** 初始化：注册 kaproxy:main 通道。 */
    fun setup() {
        val kaproxyEnabled = plugin.config.getBoolean("kaproxy.enabled", false)
        if (!kaproxyEnabled) {
            plugin.logger.info(plugin.languageManager.getMessage("cross-server.channel-disabled"))
            return
        }
        try {
            plugin.server.messenger.registerIncomingPluginChannel(plugin, CHANNEL, listener)
            plugin.server.messenger.registerOutgoingPluginChannel(plugin, CHANNEL)
            enabled = true
            plugin.logger.info(plugin.languageManager.getMessage("cross-server.channel-enabled"))
        } catch (e: Exception) {
            plugin.logger.warning(plugin.languageManager.getMessage("cross-server.channel-register-failed", e.message ?: ""))
        }
    }

    /** 关闭时注销通道。 */
    fun shutdown() {
        if (enabled) {
            plugin.server.messenger.unregisterIncomingPluginChannel(plugin, CHANNEL, listener)
            plugin.server.messenger.unregisterOutgoingPluginChannel(plugin, CHANNEL)
            enabled = false
        }
    }

    fun isEnabled(): Boolean = enabled

    /**
     * 把动作编码并发送给 KaProxy。
     *
     * 通过任意在线玩家携带发送（plugin message 需要玩家连接）。
     */
    fun dispatch(sourcePlayer: Player, action: String, selector: String, variables: Map<String, String>) {
        if (!enabled) return
        val carrier = Bukkit.getOnlinePlayers().firstOrNull() ?: return
        try {
            val packet = encodePacket(ACTION_DISPATCH) { output ->
                output.writeUTF(sourcePlayer.uniqueId.toString())
                output.writeUTF(sourcePlayer.name)
                output.writeUTF(selector)
                output.writeUTF(action)
                output.writeInt(variables.size)
                variables.forEach { (k, v) ->
                    output.writeUTF(k)
                    output.writeUTF(v)
                }
            }
            carrier.sendPluginMessage(plugin, CHANNEL, packet)
        } catch (e: IOException) {
            plugin.logger.warning(plugin.languageManager.getMessage("cross-server.dispatch-failed", e.message ?: ""))
        }
    }

    /**
     * 处理从 KaProxy 转发来的跨服动作包。
     *
     * 解码后在本服本地求值选择器并对命中玩家执行动作。
     */
    private fun handleIncomingPacket(data: ByteArray) {
        try {
            val packet = decodePacket(data) ?: return
            if (packet.module != MODULE_KAMENU || packet.action != ACTION_EXECUTE) return
            val input = packet.input
            val sourceServer = input.readUTF()
            // 读取源玩家信息、选择器、动作文本和变量
            val sourcePlayerUuid = input.readUTF()
            val sourcePlayerName = input.readUTF()
            val selector = input.readUTF()
            val action = input.readUTF()
            val variableCount = input.readInt()
            val variables = mutableMapOf<String, String>()
            repeat(variableCount) {
                val k = input.readUTF()
                val v = input.readUTF()
                variables[k] = v
            }

            // 安全校验：只允许白名单内的动作跨服执行
            if (!isActionAllowed(action)) {
                plugin.logger.warning(plugin.languageManager.getMessage("cross-server.action-rejected", action, sourceServer))
                return
            }

            // 在主线程执行选择器求值和动作派发
            KaScheduler.runGlobal(Runnable {
                val targets = MenuActions.getTargetPlayersPublic(Bukkit.getPlayer(UUID.fromString(sourcePlayerUuid)) ?: return@Runnable, selector)
                for (target in targets) {
                    MenuActions.executeActionForPlayerPublic(target, action, variables)
                }
            })
        } catch (e: IOException) {
            plugin.logger.warning(plugin.languageManager.getMessage("cross-server.decode-failed", e.message ?: ""))
        } catch (e: Exception) {
            plugin.logger.warning(plugin.languageManager.getMessage("cross-server.handle-error", e.message ?: ""))
        }
    }

    /** 检查动作是否在跨服白名单内。 */
    private fun isActionAllowed(action: String): Boolean {
        val trimmed = action.trim().lowercase()
        return ALLOWED_ACTIONS.any { trimmed.startsWith(it) }
    }

    // ==================== 协议编解码 ====================

    /** 编码 kaproxy:main 信封包。 */
    private fun encodePacket(action: String, writer: (DataOutputStream) -> Unit): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { output ->
            output.writeInt(MAGIC)
            output.writeShort(VERSION.toInt())
            output.writeUTF(MODULE_KAMENU)
            output.writeUTF(action)
            writer(output)
        }
        return bytes.toByteArray()
    }

    /** 解码 kaproxy:main 信封包。 */
    private fun decodePacket(data: ByteArray): DecodedPacket? {
        if (data.size > 1_048_576) return null
        val input = DataInputStream(ByteArrayInputStream(data))
        if (input.readInt() != MAGIC) return null
        if (input.readShort() != VERSION) return null
        val module = input.readUTF()
        val action = input.readUTF()
        return DecodedPacket(module, action, input)
    }

    private data class DecodedPacket(val module: String, val action: String, val input: DataInputStream)
}
