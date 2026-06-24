package org.katacr.kamenu

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 管理菜单打开期间的周期任务。
 * 同一玩家同一 contextId 只维护一个生命周期，任务以 Events.Tasks 下的 taskId 去重。
 */
object MenuTaskManager {
    private lateinit var plugin: KaMenu
    private val tokenCounter = AtomicLong()
    private val sessions = ConcurrentHashMap<UUID, MenuTaskSession>()

    private data class MenuTaskSession(
        val playerId: UUID,
        val contextId: String,
        val token: Long,
        @Volatile var config: YamlConfiguration,
        val tasks: ConcurrentHashMap<String, RunningTask> = ConcurrentHashMap()
    )

    private data class RunningTask(
        val id: String,
        val token: Long,
        val taskSection: ConfigurationSection,
        val actions: List<*>,
        val interval: Long,
        val skipIfRunning: Boolean,
        val remainingRuns: AtomicInteger,
        val running: AtomicBoolean = AtomicBoolean(false),
        @Volatile var bukkitTask: BukkitTask? = null,
        val stopping: AtomicBoolean = AtomicBoolean(false)
    )

    data class TaskExecutionRef(
        val playerId: UUID,
        val sessionToken: Long,
        val taskId: String
    )

    fun init(plugin: KaMenu) {
        this.plugin = plugin
    }

    fun attachMenu(player: Player, config: YamlConfiguration, contextId: String) {
        val tasksSection = config.getConfigurationSection("Events.Tasks")
        if (tasksSection == null || tasksSection.getKeys(false).isEmpty()) {
            cancel(player.uniqueId)
            return
        }

        val existing = sessions[player.uniqueId]
        val session = if (existing != null && existing.contextId == contextId) {
            existing.config = config
            existing
        } else {
            cancel(player.uniqueId)
            MenuTaskSession(
                playerId = player.uniqueId,
                contextId = contextId,
                token = tokenCounter.incrementAndGet(),
                config = config
            ).also { sessions[player.uniqueId] = it }
        }

        for (taskId in tasksSection.getKeys(false)) {
            val taskSection = tasksSection.getConfigurationSection(taskId) ?: continue
            val mode = taskSection.getString("mode", "auto")?.lowercase() ?: "auto"
            if (mode == "manual") {
                continue
            }
            runTask(player, taskId, null)
        }

        @Suppress("UNUSED_VARIABLE")
        val retainedSession = session
    }

    fun runTask(player: Player, taskId: String, repeatOverride: Int? = null): Boolean {
        val session = sessions[player.uniqueId] ?: return false
        val taskSection = session.config.getConfigurationSection("Events.Tasks.$taskId") ?: run {
            plugin.logger.warning("周期任务不存在: contextId=${session.contextId}, task=$taskId")
            return false
        }

        if (session.tasks.containsKey(taskId)) {
            return true
        }

        val actions = taskSection.getList("actions")
        if (actions.isNullOrEmpty()) {
            plugin.logger.warning("周期任务 actions 为空: contextId=${session.contextId}, task=$taskId")
            return false
        }

        val interval = taskSection.getLong("interval", 20L).coerceAtLeast(1L)
        val runImmediately = taskSection.getBoolean("run_immediately", false)
        val skipIfRunning = taskSection.getBoolean("skip_if_running", true)
        val configuredRepeat = repeatOverride ?: taskSection.getInt("repeat", -1)
        val remainingRuns = AtomicInteger(if (configuredRepeat <= 0) -1 else configuredRepeat)
        val task = RunningTask(
            id = taskId,
            token = session.token,
            taskSection = taskSection,
            actions = actions,
            interval = interval,
            skipIfRunning = skipIfRunning,
            remainingRuns = remainingRuns
        )

        if (session.tasks.putIfAbsent(taskId, task) != null) {
            return true
        }

        if (runImmediately) {
            executeTaskRound(player.uniqueId, session.token, taskId)
        }

        if (sessions[player.uniqueId]?.token != session.token || !session.tasks.containsKey(taskId)) {
            return true
        }

        if (task.remainingRuns.get() == 0) {
            if (!task.running.get()) {
                finishTask(player.uniqueId, session.token, taskId, runEndActions = true)
            }
            return true
        }

        task.bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            executeTaskRound(player.uniqueId, session.token, taskId)
        }, interval, interval)
        return true
    }

    fun runAllTasks(player: Player, repeatOverride: Int? = null): Boolean {
        val session = sessions[player.uniqueId] ?: return false
        val tasksSection = session.config.getConfigurationSection("Events.Tasks") ?: return false
        var startedAny = false
        for (taskId in tasksSection.getKeys(false)) {
            if (runTask(player, taskId, repeatOverride)) {
                startedAny = true
            }
        }
        return startedAny
    }

    fun stopTask(player: Player, taskId: String, runEndActions: Boolean = true): Boolean {
        val session = sessions[player.uniqueId] ?: return false
        return finishTask(player.uniqueId, session.token, taskId, runEndActions)
    }

    fun stopAllTasks(player: Player, runEndActions: Boolean = true): Boolean {
        val session = sessions[player.uniqueId] ?: return false
        val taskIds = session.tasks.keys.toList()
        var stoppedAny = false
        for (taskId in taskIds) {
            if (finishTask(player.uniqueId, session.token, taskId, runEndActions)) {
                stoppedAny = true
            }
        }
        return stoppedAny
    }

    fun stopTask(ref: TaskExecutionRef, runEndActions: Boolean = true): Boolean {
        return finishTask(ref.playerId, ref.sessionToken, ref.taskId, runEndActions)
    }

    fun currentToken(player: Player): Long? {
        return sessions[player.uniqueId]?.token
    }

    fun cancel(player: Player) {
        cancel(player.uniqueId)
    }

    fun cancel(playerId: UUID) {
        val session = sessions.remove(playerId) ?: return
        val taskIds = session.tasks.keys.toList()
        taskIds.forEach { finishTask(playerId, session.token, it, runEndActions = true, removeSessionIfEmpty = false) }
        session.tasks.clear()
    }

    fun cancelAll() {
        val playerIds = sessions.keys.toList()
        playerIds.forEach { cancel(it) }
    }

    private fun executeTaskRound(playerId: UUID, token: Long, taskId: String) {
        val session = sessions[playerId]
        if (session == null || session.token != token) {
            return
        }
        val task = session.tasks[taskId] ?: return

        val player = Bukkit.getPlayer(playerId)
        if (player == null || !player.isOnline) {
            cancel(playerId)
            return
        }

        if (task.stopping.get()) {
            return
        }
        if (task.skipIfRunning && !task.running.compareAndSet(false, true)) {
            return
        }
        if (!task.skipIfRunning) {
            task.running.set(true)
        }

        val before = task.remainingRuns.get()
        if (before == 0) {
            task.running.set(false)
            finishTask(playerId, token, taskId, runEndActions = true)
            return
        }
        if (before > 0 && !task.remainingRuns.compareAndSet(before, before - 1)) {
            task.running.set(false)
            return
        }

        MenuActions.executeActionGroup(
            player = player,
            config = session.config,
            actions = task.actions,
            variables = emptyMap(),
            taskRef = TaskExecutionRef(playerId, token, taskId)
        ).whenComplete { _, error ->
            task.running.set(false)
            if (error != null) {
                plugin.logger.severe("周期任务执行失败: contextId=${session.contextId}, task=$taskId, 错误: ${error.message}")
                error.printStackTrace()
            }
            if (sessions[playerId]?.token == token && task.remainingRuns.get() == 0) {
                finishTask(playerId, token, taskId, runEndActions = true)
            }
        }
    }

    private fun finishTask(
        playerId: UUID,
        token: Long,
        taskId: String,
        runEndActions: Boolean,
        removeSessionIfEmpty: Boolean = true
    ): Boolean {
        val session = sessions[playerId]
        if (session == null || session.token != token) {
            return false
        }
        val task = session.tasks.remove(taskId) ?: return false
        if (!task.stopping.compareAndSet(false, true)) {
            return true
        }

        task.bukkitTask?.cancel()
        if (runEndActions) {
            runEndActions(playerId, session, task)
        }
        if (removeSessionIfEmpty && session.tasks.isEmpty() && session.config.getConfigurationSection("Events.Tasks") == null) {
            sessions.remove(playerId, session)
        }
        return true
    }

    private fun runEndActions(playerId: UUID, session: MenuTaskSession, task: RunningTask) {
        val endActions = task.taskSection.getList("on_end") ?: task.taskSection.getList("end_actions") ?: return
        if (endActions.isEmpty()) {
            return
        }

        val player = Bukkit.getPlayer(playerId) ?: return
        MenuActions.executeActionGroup(
            player = player,
            config = session.config,
            actions = endActions,
            variables = emptyMap()
        ).whenComplete { _, error ->
            if (error != null) {
                plugin.logger.severe("周期任务结束动作执行失败: contextId=${session.contextId}, task=${task.id}, 错误: ${error.message}")
                error.printStackTrace()
            }
        }
    }
}
