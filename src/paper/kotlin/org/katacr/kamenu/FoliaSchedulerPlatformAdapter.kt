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
 * Folia 专用调度实现。
 *
 * 该类只会在 [KaScheduler] 检测到 Folia API 后通过反射加载，因此不能放入公共 API
 * 编译基线。将其放在 Paper 适配 source set 中，避免低版本 Bukkit/Spigot 在链接插件时解析
 * `io.papermc.paper.threadedregions` 类型。
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

    override fun runAsyncTimer(plugin: Plugin, delayMillis: Long, periodMillis: Long, task: Runnable): KaTaskHandle =
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, Consumer { task.run() }, delayMillis, periodMillis, TimeUnit.MILLISECONDS).toHandle()

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
