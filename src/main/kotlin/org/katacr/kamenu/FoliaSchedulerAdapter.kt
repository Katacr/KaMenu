@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

/**
 * Folia 专用调度实现，仅由 [KaScheduler] 在确认 Folia API 存在后反射加载。
 */
class FoliaSchedulerPlatformAdapter : FoliaSchedulerAdapter {
    override fun isPlayerThread(player: Player): Boolean = Bukkit.isOwnedByCurrentRegion(player)

    override fun runPlayer(plugin: Plugin, player: Player, task: Runnable): KaTaskHandle =
        player.scheduler.run(plugin, Consumer { task.run() }, null).toHandle()

    override fun runPlayerLater(plugin: Plugin, player: Player, delayTicks: Long, task: Runnable): KaTaskHandle =
        player.scheduler.runDelayed(plugin, Consumer { task.run() }, null, delayTicks).toHandle()

    override fun runPlayerTimer(
        plugin: Plugin,
        player: Player,
        delayTicks: Long,
        periodTicks: Long,
        task: Runnable
    ): KaTaskHandle = player.scheduler.runAtFixedRate(plugin, Consumer { task.run() }, null, delayTicks, periodTicks).toHandle()

    override fun runGlobal(plugin: Plugin, task: Runnable): KaTaskHandle =
        Bukkit.getGlobalRegionScheduler().run(plugin, Consumer { task.run() }).toHandle()

    override fun runGlobalLater(plugin: Plugin, delayTicks: Long, task: Runnable): KaTaskHandle =
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, Consumer { task.run() }, delayTicks).toHandle()

    override fun runAsync(plugin: Plugin, task: Runnable): KaTaskHandle =
        Bukkit.getAsyncScheduler().runNow(plugin, Consumer { task.run() }).toHandle()

    override fun runAsyncLater(plugin: Plugin, delayMillis: Long, task: Runnable): KaTaskHandle =
        Bukkit.getAsyncScheduler().runDelayed(plugin, Consumer { task.run() }, delayMillis, TimeUnit.MILLISECONDS).toHandle()

    override fun teleport(player: Player, location: Location) {
        player.teleportAsync(location)
    }

    override fun cancelPluginTasks(plugin: Plugin) {
        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin)
        Bukkit.getAsyncScheduler().cancelTasks(plugin)
    }

    private fun ScheduledTask?.toHandle(): KaTaskHandle =
        if (this == null) KaTaskHandle.NOOP else KaTaskHandle { cancel() }
}
