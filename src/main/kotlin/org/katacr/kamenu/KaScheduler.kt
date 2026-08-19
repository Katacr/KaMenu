package org.katacr.kamenu

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

/**
 * Paper/Folia 调度桥接层。
 *
 * KaMenu 的菜单、动作和周期任务大量围绕玩家对象运行。Folia 没有传统 Bukkit 主线程，
 * 因此玩家相关逻辑必须进入玩家 EntityScheduler；全局配置/控制台逻辑进入 GlobalRegionScheduler；
 * 数据库、网络等非 Bukkit 对象操作进入 AsyncScheduler。
 */
object KaScheduler {
    private lateinit var plugin: Plugin

    private val foliaAdapter: FoliaSchedulerAdapter? by lazy {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
            Class.forName("org.katacr.kamenu.FoliaSchedulerPlatformAdapter")
                .getDeclaredConstructor()
                .newInstance() as FoliaSchedulerAdapter
        } catch (_: ReflectiveOperationException) {
            null
        } catch (_: LinkageError) {
            null
        }
    }

    val folia: Boolean
        get() = foliaAdapter != null

    /** 判断当前线程是否允许安全访问指定玩家。 */
    fun isPlayerThread(player: Player): Boolean {
        return foliaAdapter?.isPlayerThread(player) ?: Bukkit.isPrimaryThread()
    }

    fun init(plugin: Plugin) {
        this.plugin = plugin
    }

    fun runPlayer(player: Player, task: Runnable): KaTaskHandle {
        return foliaAdapter?.runPlayer(plugin, player, task) ?: run {
            Bukkit.getScheduler().runTask(plugin, task).toHandle()
        }
    }

    fun runPlayerLater(player: Player, delayTicks: Long, task: Runnable): KaTaskHandle {
        val delay = delayTicks.coerceAtLeast(1L)
        return foliaAdapter?.runPlayerLater(plugin, player, delay, task) ?: run {
            Bukkit.getScheduler().runTaskLater(plugin, task, delay).toHandle()
        }
    }

    fun runPlayerTimer(player: Player, delayTicks: Long, periodTicks: Long, task: Runnable): KaTaskHandle {
        val delay = delayTicks.coerceAtLeast(1L)
        val period = periodTicks.coerceAtLeast(1L)
        return foliaAdapter?.runPlayerTimer(plugin, player, delay, period, task) ?: run {
            Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period).toHandle()
        }
    }

    fun runGlobal(task: Runnable): KaTaskHandle {
        return foliaAdapter?.runGlobal(plugin, task) ?: run {
            Bukkit.getScheduler().runTask(plugin, task).toHandle()
        }
    }

    fun runGlobalLater(delayTicks: Long, task: Runnable): KaTaskHandle {
        val delay = delayTicks.coerceAtLeast(1L)
        return foliaAdapter?.runGlobalLater(plugin, delay, task) ?: run {
            Bukkit.getScheduler().runTaskLater(plugin, task, delay).toHandle()
        }
    }

    fun runAsync(task: Runnable): KaTaskHandle {
        return foliaAdapter?.runAsync(plugin, task) ?: run {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task).toHandle()
        }
    }

    fun runAsyncLater(delayTicks: Long, task: Runnable): KaTaskHandle {
        val delayMillis = delayTicks.coerceAtLeast(1L) * 50L
        return foliaAdapter?.runAsyncLater(plugin, delayMillis, task) ?: run {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks.coerceAtLeast(1L)).toHandle()
        }
    }

    /**
     * 在异步线程按固定周期执行任务。
     *
     * 用于数据库清理等非 Bukkit 对象操作；Folia 走 AsyncScheduler，Bukkit 走异步调度器。
     */
    fun runAsyncTimer(delayMillis: Long, periodMillis: Long, task: Runnable): KaTaskHandle {
        return foliaAdapter?.runAsyncTimer(plugin, delayMillis.coerceAtLeast(1L), periodMillis.coerceAtLeast(1L), task) ?: run {
            val delayTicks = (delayMillis / 50).coerceAtLeast(1L)
            val periodTicks = (periodMillis / 50).coerceAtLeast(1L)
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks).toHandle()
        }
    }

    /** 在当前核心允许的玩家线程上执行坐标传送。 */
    fun teleport(player: Player, location: Location) {
        val adapter = foliaAdapter
        if (adapter != null) {
            adapter.teleport(player, location)
        } else if (Bukkit.isPrimaryThread()) {
            player.teleport(location)
        } else {
            runPlayer(player, Runnable { player.teleport(location) })
        }
    }

    fun cancelPluginTasks() {
        val adapter = foliaAdapter
        if (adapter != null) {
            adapter.cancelPluginTasks(plugin)
        } else {
            Bukkit.getScheduler().cancelTasks(plugin)
        }
    }

    private fun BukkitTask.toHandle(): KaTaskHandle = KaTaskHandle { cancel() }

}

/**
 * Folia 调度实现的中立契约。
 *
 * 共享调度器只通过此接口访问 Folia，避免 Spigot 类加载器解析 Paper/Folia 类型。
 */
interface FoliaSchedulerAdapter {
    fun isPlayerThread(player: Player): Boolean
    fun runPlayer(plugin: Plugin, player: Player, task: Runnable): KaTaskHandle
    fun runPlayerLater(plugin: Plugin, player: Player, delayTicks: Long, task: Runnable): KaTaskHandle
    fun runPlayerTimer(plugin: Plugin, player: Player, delayTicks: Long, periodTicks: Long, task: Runnable): KaTaskHandle
    fun runGlobal(plugin: Plugin, task: Runnable): KaTaskHandle
    fun runGlobalLater(plugin: Plugin, delayTicks: Long, task: Runnable): KaTaskHandle
    fun runAsync(plugin: Plugin, task: Runnable): KaTaskHandle
    fun runAsyncLater(plugin: Plugin, delayMillis: Long, task: Runnable): KaTaskHandle
    fun runAsyncTimer(plugin: Plugin, delayMillis: Long, periodMillis: Long, task: Runnable): KaTaskHandle
    fun teleport(player: Player, location: Location)
    fun cancelPluginTasks(plugin: Plugin)
}

/**
 * 统一任务句柄，隐藏 BukkitTask 与 Folia ScheduledTask 差异。
 */
class KaTaskHandle(private val cancelAction: () -> Unit) {
    fun cancel() {
        cancelAction()
    }

    companion object {
        val NOOP = KaTaskHandle {}
    }
}
