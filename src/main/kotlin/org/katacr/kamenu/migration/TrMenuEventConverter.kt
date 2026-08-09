package org.katacr.kamenu.migration

import java.util.LinkedHashMap

/** 已转换的菜单生命周期事件和自动周期任务。 */
internal data class TrMenuEventConversion(
    val open: List<Any>,
    val close: List<Any>,
    val tasks: Map<String, Map<String, Any>>
)

/**
 * 将 源菜单 Reaction 和 Tasks 转换为 KaMenu Container 生命周期动作。
 *
 * 源菜单 Close 中的 return 只截断动作，不阻止窗口关闭，因此此处会专门移除该语义冲突。
 */
internal class TrMenuEventConverter(
    private val actionConverter: TrMenuActionConverter,
    private val conditionConverter: TrMenuConditionConverter = TrMenuConditionConverter()
) {
    private data class SourceReaction(
        val order: Int,
        val priority: Int,
        val raw: Any?,
        val path: String
    )

    /** 转换顶层 Events 和 Tasks；不存在的部分返回空集合。 */
    fun convert(
        root: TrMenuSourceSection,
        diagnostics: TrMenuMigrationDiagnostics
    ): TrMenuEventConversion {
        val events = root.section(TrMenuSourceProperty.EVENTS, "Events", diagnostics)
        val open = events?.value(TrMenuSourceProperty.EVENT_OPEN, "Events.Open", diagnostics)
            ?.let { convertReactions(it, "Events.Open", EventKind.OPEN, diagnostics) }
            .orEmpty()
        val close = events?.value(TrMenuSourceProperty.EVENT_CLOSE, "Events.Close", diagnostics)
            ?.let { convertReactions(it, "Events.Close", EventKind.CLOSE, diagnostics) }
            .orEmpty()

        events?.value(TrMenuSourceProperty.EVENT_CLICK, "Events.Click", diagnostics)?.let {
            diagnostics.add(
                code = "TRM_EVENT_CLICK_IGNORED",
                severity = TrMenuMigrationSeverity.WARNING,
                compatibility = TrMenuMigrationCompatibility.APPROXIMATE,
                path = "Events.Click",
                message = "Events.Click is not executed by the analyzed TrMenu stable-v3 inventory runtime and was not mapped to KaMenu named actions."
            )
        }

        return TrMenuEventConversion(open, close, convertTasks(root, diagnostics))
    }

    /** 转换图标点击类型下的普通 Reaction；条件失败时只保留 deny 分支。 */
    fun convertActionReactions(
        raw: Any?,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): List<Any> = convertReactions(raw, path, EventKind.ACTION, diagnostics)

    private fun convertReactions(
        raw: Any?,
        path: String,
        kind: EventKind,
        diagnostics: TrMenuMigrationDiagnostics
    ): List<Any> {
        val values = if (raw is List<*>) raw else listOf(raw)
        val reactions = values.mapIndexed { index, value ->
            val section = value as? TrMenuSourceSection
            val priority = section
                ?.value(TrMenuSourceProperty.PRIORITY, "$path[$index].priority", diagnostics)
                ?.toString()
                ?.toIntOrNull()
                ?: index
            SourceReaction(index, priority, value, "$path[$index]")
        }.sortedWith(compareBy<SourceReaction> { it.priority }.thenBy { it.order })

        val converted = reactions.flatMap { reaction ->
            convertReaction(reaction, kind, diagnostics)
        }
        return if (kind == EventKind.CLOSE) sanitizeCloseActions(converted, path, diagnostics) else converted
    }

    private fun convertReaction(
        reaction: SourceReaction,
        kind: EventKind,
        diagnostics: TrMenuMigrationDiagnostics
    ): List<Any> {
        val section = reaction.raw as? TrMenuSourceSection
            ?: return actionConverter.convert(reaction.raw, reaction.path, diagnostics)
        val condition = section.value(
            TrMenuSourceProperty.CONDITION,
            "${reaction.path}.condition",
            diagnostics
        )?.toString()?.trim().orEmpty()
        val allowRaw = section.value(
            TrMenuSourceProperty.ACTIONS,
            "${reaction.path}.actions",
            diagnostics
        )
        val denyRaw = section.value(
            TrMenuSourceProperty.DENY_ACTIONS,
            "${reaction.path}.deny-actions",
            diagnostics
        )
        val allow = actionConverter.convert(allowRaw, "${reaction.path}.actions", diagnostics)
        val deny = actionConverter.convert(denyRaw, "${reaction.path}.deny-actions", diagnostics)
        if (condition.isEmpty()) return allow

        val expression = conditionConverter.convert(condition, "${reaction.path}.condition", diagnostics)
        if (expression == null) {
            return when (kind) {
                EventKind.OPEN -> (deny + "return").distinctConsecutive()
                EventKind.CLOSE, EventKind.ACTION -> deny
            }
        }
        return listOf(
            linkedMapOf<String, Any>(
                "condition" to expression,
                "allow" to allow,
                "deny" to deny
            )
        )
    }

    private fun convertTasks(
        root: TrMenuSourceSection,
        diagnostics: TrMenuMigrationDiagnostics
    ): Map<String, Map<String, Any>> {
        val sourceTasks = root.section(TrMenuSourceProperty.TASKS, "Tasks", diagnostics) ?: return emptyMap()
        val output = LinkedHashMap<String, Map<String, Any>>()
        sourceTasks.entries().forEach { (sourceTaskId, rawTask) ->
            val task = rawTask as? TrMenuSourceSection
            if (task == null) {
                invalidTask("Tasks.$sourceTaskId", "Task definition must be a YAML section.", diagnostics)
                return@forEach
            }
            val period = task.value(TrMenuSourceProperty.PERIOD, "Tasks.$sourceTaskId.period", diagnostics)
                ?.toString()
                ?.toLongOrNull()
            if (period == null || period <= 0L) {
                invalidTask("Tasks.$sourceTaskId.period", "Task period must be a positive tick value.", diagnostics)
                return@forEach
            }
            val rawEntries = task.value(TrMenuSourceProperty.TASKS, "Tasks.$sourceTaskId.task", diagnostics)
            val entries = if (rawEntries is List<*>) rawEntries else listOfNotNull(rawEntries)
            entries.forEachIndexed { index, rawEntry ->
                val entryPath = "Tasks.$sourceTaskId.task[$index]"
                val section = rawEntry as? TrMenuSourceSection
                if (section == null) {
                    invalidTask(entryPath, "Task entry must contain condition and actions.", diagnostics)
                    return@forEachIndexed
                }
                val condition = section.value(
                    TrMenuSourceProperty.CONDITION,
                    "$entryPath.condition",
                    diagnostics
                )?.toString()?.trim().orEmpty()
                val expression = conditionConverter.convert(condition, "$entryPath.condition", diagnostics)
                    ?: return@forEachIndexed
                val actionsRaw = section.value(
                    TrMenuSourceProperty.ACTIONS,
                    "$entryPath.actions",
                    diagnostics
                )
                val actions = actionConverter.convert(actionsRaw, "$entryPath.actions", diagnostics)
                if (actions.isEmpty()) {
                    invalidTask("$entryPath.actions", "Task entry has no migratable actions.", diagnostics)
                    return@forEachIndexed
                }
                val taskId = uniqueTaskId(sourceTaskId, index, entries.size, output.keys)
                output[taskId] = linkedMapOf(
                    "mode" to "auto",
                    "interval" to period,
                    "repeat" to -1,
                    "run_immediately" to false,
                    "skip_if_running" to true,
                    "actions" to listOf(
                        linkedMapOf<String, Any>(
                            "condition" to expression,
                            "allow" to actions,
                            "deny" to emptyList<Any>()
                        )
                    )
                )
                diagnostics.add(
                    code = "TRM_TASK_INITIAL_DELAY_APPROXIMATE",
                    severity = TrMenuMigrationSeverity.WARNING,
                    compatibility = TrMenuMigrationCompatibility.APPROXIMATE,
                    path = entryPath,
                    message = "TrMenu starts this task after about 5 ticks; KaMenu starts it after the configured $period tick interval."
                )
            }
        }
        return output
    }

    private fun sanitizeCloseActions(
        actions: List<Any>,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): List<Any> {
        val result = mutableListOf<Any>()
        for (action in actions) {
            if (action is String && action.trim().matches(Regex("(?i)return|break"))) {
                diagnostics.add(
                    code = "TRM_CLOSE_RETURN_APPROXIMATE",
                    severity = TrMenuMigrationSeverity.WARNING,
                    compatibility = TrMenuMigrationCompatibility.APPROXIMATE,
                    path = path,
                    message = "TrMenu Close return only stops later actions; it was removed so KaMenu does not reopen the inventory."
                )
                break
            }
            if (action is Map<*, *>) {
                val condition = action["condition"] as? String ?: continue
                val allow = sanitizeCloseActions((action["allow"] as? List<*>).orEmpty().filterNotNull(), "$path.allow", diagnostics)
                val deny = sanitizeCloseActions((action["deny"] as? List<*>).orEmpty().filterNotNull(), "$path.deny", diagnostics)
                result += linkedMapOf<String, Any>("condition" to condition, "allow" to allow, "deny" to deny)
            } else {
                result += action
            }
        }
        return result
    }

    private fun uniqueTaskId(
        sourceId: String,
        index: Int,
        count: Int,
        existing: Set<String>
    ): String {
        val base = sourceId.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "task" }
        val indexed = if (count <= 1) base else "${base}_${index + 1}"
        if (indexed !in existing) return indexed
        var suffix = 2
        while ("${indexed}_$suffix" in existing) suffix++
        return "${indexed}_$suffix"
    }

    private fun invalidTask(path: String, message: String, diagnostics: TrMenuMigrationDiagnostics) {
        diagnostics.add(
            code = "TRM_TASK_INVALID",
            severity = TrMenuMigrationSeverity.WARNING,
            compatibility = TrMenuMigrationCompatibility.INVALID,
            path = path,
            message = message
        )
    }

    private fun List<Any>.distinctConsecutive(): List<Any> =
        filterIndexed { index, value -> index == 0 || this[index - 1] != value }

    private enum class EventKind {
        OPEN,
        CLOSE,
        ACTION
    }
}
