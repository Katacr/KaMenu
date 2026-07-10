package org.katacr.kamenu

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 管理玩家当前 KaMenu Dialog 的有效期。
 *
 * 每次成功渲染菜单都会替换该玩家的旧会话并重新计时。超时回调通过会话实例比较，
 * 保证旧菜单的定时器不会关闭玩家后来打开的新 KaMenu 菜单。
 */
object DialogSessionManager {
    const val DEFAULT_LIFETIME_SECONDS = 300L

    private data class ActiveDialog(
        val config: YamlConfiguration,
        val contextId: String,
        @Volatile var timeoutTask: KaTaskHandle? = null
    )

    private lateinit var plugin: KaMenu
    private val activeDialogs = ConcurrentHashMap<UUID, ActiveDialog>()

    fun init(plugin: KaMenu) {
        this.plugin = plugin
    }

    /**
     * 解析菜单有效期。
     *
     * 非法 lifetime 回退到 300 秒。
     */
    fun lifetimeSeconds(config: YamlConfiguration): Long {
        val configuredLifetime = config.getLong("Settings.lifetime", DEFAULT_LIFETIME_SECONDS)
        return configuredLifetime.takeIf { it > 0L } ?: DEFAULT_LIFETIME_SECONDS
    }

    /**
     * 记录刚刚显示的菜单，并安排超时主动关闭。
     */
    fun attach(player: Player, config: YamlConfiguration, contextId: String) {
        val session = ActiveDialog(
            config = config,
            contextId = contextId
        )
        activeDialogs.put(player.uniqueId, session)?.timeoutTask?.cancel()

        val delayTicks = lifetimeSeconds(config)
            .coerceAtMost(Long.MAX_VALUE / 20L)
            .times(20L)
        session.timeoutTask = KaScheduler.runPlayerLater(player, delayTicks, Runnable {
            if (!activeDialogs.remove(player.uniqueId, session)) {
                return@Runnable
            }
            closeSession(player, session)
        })
    }

    /**
     * 当前菜单已被关闭或替换时取消超时任务。
     */
    fun cancel(player: Player) {
        activeDialogs.remove(player.uniqueId)?.timeoutTask?.cancel()
    }

    /**
     * 插件关闭时清理全部菜单超时任务。
     */
    fun clearAll() {
        activeDialogs.values.forEach { it.timeoutTask?.cancel() }
        activeDialogs.clear()
    }

    private fun closeSession(player: Player, session: ActiveDialog) {
        MenuTaskManager.cancel(player)
        MenuListManager.clear(player)
        player.closeDialog()

        if (session.config.contains("Events.Close")) {
            MenuActions.executeEvent(player, session.config, "Close", session.contextId)
                .whenComplete { _, error ->
                    if (error != null) {
                        plugin.logger.severe("Dialog 超时关闭时执行 Close 事件失败: contextId=${session.contextId}, 错误=${error.message}")
                        error.printStackTrace()
                    }
                }
        }
    }
}
