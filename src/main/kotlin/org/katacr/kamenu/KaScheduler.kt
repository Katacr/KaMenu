@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

/**
 * Paper/Folia 调度桥接层。
 *
 * KaMenu 的菜单、动作和周期任务大量围绕玩家对象运行。Folia 没有传统 Bukkit 主线程，
 * 因此玩家相关逻辑必须进入玩家 EntityScheduler；全局配置/控制台逻辑进入 GlobalRegionScheduler；
 * 数据库、网络等非 Bukkit 对象操作进入 AsyncScheduler。
 */
object KaScheduler {
    private lateinit var plugin: Plugin

    val folia: Boolean by lazy {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    fun init(plugin: Plugin) {
        this.plugin = plugin
    }

    fun runPlayer(player: Player, task: Runnable): KaTaskHandle {
        return if (folia) {
            player.scheduler.run(plugin, Consumer { task.run() }, null).toHandle()
        } else {
            Bukkit.getScheduler().runTask(plugin, task).toHandle()
        }
    }

    fun runPlayerLater(player: Player, delayTicks: Long, task: Runnable): KaTaskHandle {
        val delay = delayTicks.coerceAtLeast(1L)
        return if (folia) {
            player.scheduler.runDelayed(plugin, Consumer { task.run() }, null, delay).toHandle()
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delay).toHandle()
        }
    }

    fun runPlayerTimer(player: Player, delayTicks: Long, periodTicks: Long, task: Runnable): KaTaskHandle {
        val delay = delayTicks.coerceAtLeast(1L)
        val period = periodTicks.coerceAtLeast(1L)
        return if (folia) {
            player.scheduler.runAtFixedRate(plugin, Consumer { task.run() }, null, delay, period).toHandle()
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period).toHandle()
        }
    }

    fun runGlobal(task: Runnable): KaTaskHandle {
        return if (folia) {
            Bukkit.getGlobalRegionScheduler().run(plugin, Consumer { task.run() }).toHandle()
        } else {
            Bukkit.getScheduler().runTask(plugin, task).toHandle()
        }
    }

    fun runGlobalLater(delayTicks: Long, task: Runnable): KaTaskHandle {
        val delay = delayTicks.coerceAtLeast(1L)
        return if (folia) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, Consumer { task.run() }, delay).toHandle()
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delay).toHandle()
        }
    }

    fun runAsync(task: Runnable): KaTaskHandle {
        return if (folia) {
            Bukkit.getAsyncScheduler().runNow(plugin, Consumer { task.run() }).toHandle()
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task).toHandle()
        }
    }

    fun runAsyncLater(delayTicks: Long, task: Runnable): KaTaskHandle {
        val delayMillis = delayTicks.coerceAtLeast(1L) * 50L
        return if (folia) {
            Bukkit.getAsyncScheduler().runDelayed(plugin, Consumer { task.run() }, delayMillis, TimeUnit.MILLISECONDS).toHandle()
        } else {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks.coerceAtLeast(1L)).toHandle()
        }
    }

    fun cancelPluginTasks() {
        if (folia) {
            Bukkit.getGlobalRegionScheduler().cancelTasks(plugin)
            Bukkit.getAsyncScheduler().cancelTasks(plugin)
        } else {
            Bukkit.getScheduler().cancelTasks(plugin)
        }
    }

    private fun BukkitTask.toHandle(): KaTaskHandle = KaTaskHandle { cancel() }

    private fun ScheduledTask?.toHandle(): KaTaskHandle {
        return if (this == null) KaTaskHandle.NOOP else KaTaskHandle { cancel() }
    }
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
