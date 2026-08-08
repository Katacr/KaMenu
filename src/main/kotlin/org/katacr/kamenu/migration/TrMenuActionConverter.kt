package org.katacr.kamenu.migration

import org.katacr.kamenu.ActionArgumentParser

/**
 * 将 TrMenu 动作列表转换为 KaMenu 标准动作。
 *
 * 未注册、私有脚本或无法完整解析的动作会被过滤并报告，绝不会沿用 TrMenu 的默认
 * tell 行为。菜单和图标 ID 通过构造参数中的映射器统一改写。
 */
internal class TrMenuActionConverter(
    private val menuIdResolver: (String) -> String? = { it },
    private val iconIdResolver: (String) -> String? = { it },
    private val functionGuardConverter: ((String, String, TrMenuMigrationDiagnostics) -> Map<String, Any>?)? = null,
    private val variableConverter: TrMenuVariableConverter = TrMenuVariableConverter()
) {
    private data class Modifiers(
        val chance: String? = null,
        val delay: String? = null,
        val condition: String? = null,
        val players: String? = null
    ) {
        val count: Int
            get() = listOf(chance, delay, condition, players).count { it != null }
    }

    private data class ParsedPart(val content: String, val modifiers: Modifiers)

    private val conditionConverter = TrMenuConditionConverter(variableConverter)
    private val itemMatcherConverter = TrMenuItemMatcherConverter(variableConverter)
    private val combinedAction = Regex("\\s*(?:_\\|\\|_|&&&)\\s*")
    private val chancePattern = Regex("(?i)[{<](?:chance|rate|rand(?:om)?)[=:] ?([0-9.]+)[>}]")
    private val delayPattern = Regex("(?i)[{<](?:delay|wait)[=:] ?([0-9]+)[>}]")
    private val conditionPattern = Regex("(?i)[{<](?:condition|requirement)[=:] ?(.+)[>}]")
    private val playersPattern = Regex("(?i)[{<]players[=:]? ?(.*)[>}]")

    /** 转换字符串、列表或单键动作 Map。 */
    fun convert(
        raw: Any?,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): List<Any> = when (raw) {
        null -> emptyList()
        is List<*> -> raw.flatMapIndexed { index, value -> convert(value, "$path[$index]", diagnostics) }
        is TrMenuSourceSection -> convertActionMap(raw, path, diagnostics)
        else -> convertLine(raw.toString(), path, diagnostics)
    }

    private fun convertActionMap(
        section: TrMenuSourceSection,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): List<Any> {
        val entry = section.entries().firstOrNull() ?: return emptyList()
        if (section.entries().size > 1) {
            diagnostics.add(
                code = "TRM_ACTION_MAP_EXTRA_KEYS",
                severity = TrMenuMigrationSeverity.WARNING,
                compatibility = TrMenuMigrationCompatibility.APPROXIMATE,
                path = path,
                message = "TrMenu action Map uses only its first entry; extra keys were ignored."
            )
        }
        val value = entry.second
        return when (value) {
            is List<*> -> value.flatMapIndexed { index, item ->
                convertLine("${entry.first}: ${item ?: ""}", "$path.${entry.first}[$index]", diagnostics)
            }
            is TrMenuSourceSection -> {
                unsupported(entry.first, path, diagnostics, "Nested action Map values cannot be represented safely.")
                emptyList()
            }
            else -> convertLine("${entry.first}: ${value ?: ""}", path, diagnostics)
        }
    }

    private fun convertLine(
        raw: String,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): List<Any> {
        val parts = raw.split(combinedAction).filter(String::isNotBlank).map(::parseModifiers)
        if (parts.isEmpty()) return emptyList()
        val shared = parts.maxByOrNull { it.modifiers.count }?.modifiers ?: Modifiers()
        if (shared.players != null) {
            unsupported(raw, path, diagnostics, "The players modifier has no safe per-action KaMenu mapping.")
            return emptyList()
        }
        val converted = parts.flatMap { part ->
            convertSingle(part.content, path, diagnostics).mapNotNull {
                applyModifiers(it, shared, part.content, path, diagnostics)
            }
        }
        val condition = shared.condition ?: return converted
        val expression = conditionConverter.convert(condition, "$path.<condition>", diagnostics) ?: return emptyList()
        return if (converted.isEmpty()) emptyList() else listOf(
            linkedMapOf<String, Any>(
                "condition" to expression,
                "allow" to converted,
                "deny" to emptyList<Any>()
            )
        )
    }

    private fun parseModifiers(raw: String): ParsedPart {
        var content = raw
        val delay = delayPattern.find(content)?.groupValues?.get(1).also { content = content.replace(delayPattern, "") }
        val chance = chancePattern.find(content)?.groupValues?.get(1).also { content = content.replace(chancePattern, "") }
        val condition = conditionPattern.find(content)?.groupValues?.get(1).also { content = content.replace(conditionPattern, "") }
        val players = playersPattern.find(content)?.groupValues?.get(1).also { content = content.replace(playersPattern, "") }
        val modifiers = Modifiers(chance, delay, condition, players)
        return ParsedPart(content.trim(), modifiers)
    }

    private fun applyModifiers(
        action: Any,
        modifiers: Modifiers,
        raw: String,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): Any? {
        if (action !is String) {
            if (modifiers.chance != null || modifiers.delay != null) {
                unsupported(raw, path, diagnostics, "Chance/delay modifiers on a structured action cannot be preserved safely.")
                return null
            }
            return action
        }
        val suffix = buildList {
            modifiers.chance?.let { add("<chance=$it>") }
            modifiers.delay?.let { add("<delay=$it>") }
        }
        return if (suffix.isEmpty()) action else "$action ${suffix.joinToString(" ")}"
    }

    private fun convertSingle(
        raw: String,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): List<Any> {
        val trimmed = raw.trim()
        val separator = trimmed.indexOf(':')
        val key = (if (separator >= 0) trimmed.substring(0, separator) else trimmed).trim().lowercase()
        val content = if (separator >= 0) trimmed.substring(separator + 1).trim() else ""
        return when {
            key.matches(Regex("tell|message|msg|talk")) -> convertTextAction("tell", content, path, diagnostics)
            key.matches(Regex("action(bar)?s?")) -> convertTextAction("actionbar", content, path, diagnostics)
            key.matches(Regex("command|cmd|player|execute")) -> convertTextAction("command", content, path, diagnostics)
            key == "console" -> convertTextAction("console", content, path, diagnostics)
            key.matches(Regex("chat|send|say")) -> convertTextAction("chat", content, path, diagnostics)
            key.matches(Regex("bungee|server|connect")) -> convertTextAction("server", content, path, diagnostics)
            key.matches(Regex("close|shut")) -> listOf("close")
            key.matches(Regex("(force|silent)-?(close|shut)")) -> listOf("force-close")
            key.matches(Regex("return|break")) -> listOf("return")
            key.matches(Regex("delay|wait")) -> convertTextAction("wait", content, path, diagnostics)
            key.matches(Regex("(play)?-?sounds?")) -> convertSound(content, path, diagnostics)
            key.matches(Regex("(send)?-?(sub)?titles?")) -> convertTitle(content, path, diagnostics)
            key.matches(Regex("opens?|(open)?-?gui|(tr)?menu")) -> convertOpen(content, false, path, diagnostics)
            key.matches(Regex("(force|silent)-?(open|menu)")) -> convertOpen(content, true, path, diagnostics)
            key.matches(Regex("(icon)?-?refresh")) -> convertRefresh(content, path, diagnostics)
            key.matches(Regex("(icon)?-?update")) -> {
                diagnostics.add(
                    "TRM_ACTION_UPDATE_APPROXIMATE",
                    TrMenuMigrationSeverity.WARNING,
                    TrMenuMigrationCompatibility.APPROXIMATE,
                    path,
                    "TrMenu icon update was converted to a KaMenu button refresh."
                )
                convertRefresh(content, path, diagnostics)
            }
            key.matches(Regex("set-?(args?|arguments?)")) ->
                convertTextAction("set-args", content, path, diagnostics)
            key.matches(Regex("(del|delete|remove)-?(args?|arguments?)")) -> listOf("del-args")
            key.matches(Regex("resets?")) -> {
                diagnostics.add(
                    "TRM_ACTION_RESET_APPROXIMATE",
                    TrMenuMigrationSeverity.WARNING,
                    TrMenuMigrationCompatibility.APPROXIMATE,
                    path,
                    "TrMenu icon reset was converted to refreshing all KaMenu buttons; animation indexes cannot be preserved."
                )
                listOf("refresh")
            }
            key.matches(Regex("set-?(temp|var(iable)?|meta)s?")) -> convertSetValue("meta", content, path, diagnostics)
            key.matches(Regex("set-?datas?")) -> convertSetValue("data", content, path, diagnostics)
            key.matches(Regex("set-?(global|g)-?datas?")) -> convertSetValue("gdata", content, path, diagnostics)
            key.matches(Regex("(remove|rem|del)-?(temp|var(iable)?|meta)s?")) -> convertDeleteValue("meta", content, path, diagnostics)
            key.matches(Regex("(remove|rem|del)-?datas?")) -> convertDeleteValue("data", content, path, diagnostics)
            key.matches(Regex("(remove|rem|del)-?(global|g)-?datas?")) -> convertDeleteValue("gdata", content, path, diagnostics)
            key.matches(Regex("(give|add|deposit)-?(money|eco|coin)s?")) -> convertAmountAction("money", "add", content, path, diagnostics)
            key.matches(Regex("(take|remove|withdraw)-?(money|eco|coin)s?")) -> convertAmountAction("money", "take", content, path, diagnostics)
            key.matches(Regex("(set|modify)-?(money|eco|coin)s?")) -> convertAmountAction("money", "reset", content, path, diagnostics)
            key.matches(Regex("(give|add|deposit)-?points?")) -> convertAmountAction("points", "add", content, path, diagnostics)
            key.matches(Regex("(take|remove|withdraw)-?points?")) -> convertAmountAction("points", "take", content, path, diagnostics)
            key.matches(Regex("(give|add)-?items?")) -> itemMatcherConverter.convertAction(content, "give", path, diagnostics)
            key.matches(Regex("(take|remove)-?items?")) -> itemMatcherConverter.convertAction(content, "take", path, diagnostics)
            key.matches(Regex("(run-?)?functions?|run")) -> {
                functionGuardConverter?.invoke(content, path, diagnostics)?.let(::listOf) ?: run {
                    unsupported(raw, path, diagnostics, "Function action requires a successfully migrated TrMenu Function.")
                    emptyList()
                }
            }
            else -> {
                unsupported(raw, path, diagnostics, "Unknown or unsupported TrMenu action '$key'.")
                emptyList()
            }
        }
    }

    private fun convertSound(
        content: String,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): List<Any> = content.split(';').mapNotNull { soundEntry ->
        val parts = soundEntry.trim().split('-')
        val sound = parts.firstOrNull()?.takeIf(String::isNotBlank)
        if (sound == null) {
            unsupported(content, path, diagnostics, "Empty sound entry was skipped.")
            null
        } else {
            val volume = parts.getOrNull(1)?.toFloatOrNull() ?: 1f
            val pitch = parts.getOrNull(2)?.toFloatOrNull() ?: 1f
            variableConverter.rewrite(sound, path, diagnostics, strict = true)?.let { rewritten ->
                "sound: $rewritten;volume=$volume;pitch=$pitch"
            }
        }
    }

    private fun convertTitle(
        content: String,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): List<Any> {
        val args = ActionArgumentParser.splitArguments(content)
        if (args.isEmpty()) {
            unsupported(content, path, diagnostics, "Title action has no title text.")
            return emptyList()
        }
        val title = variableConverter.rewrite(args[0], path, diagnostics, strict = true) ?: return emptyList()
        val subtitle = variableConverter.rewrite(args.getOrElse(1) { "" }, path, diagnostics, strict = true)
            ?: return emptyList()
        val fadeIn = args.getOrNull(2)?.toIntOrNull() ?: 15
        val stay = args.getOrNull(3)?.toIntOrNull() ?: 20
        val fadeOut = args.getOrNull(4)?.toIntOrNull() ?: 15
        return listOf("title: title=${quoteParameter(title)};subtitle=${quoteParameter(subtitle)};in=$fadeIn;keep=$stay;out=$fadeOut")
    }

    private fun convertOpen(
        content: String,
        force: Boolean,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): List<Any> {
        val args = ActionArgumentParser.splitArguments(content)
        val sourceTarget = args.firstOrNull() ?: run {
            unsupported(content, path, diagnostics, "Open action has no menu ID.")
            return emptyList()
        }
        val sourceId = sourceTarget.substringBefore(':')
        val page = sourceTarget.substringAfter(':', "0").toIntOrNull()
        if (page == null || page != 0) {
            unsupported(content, path, diagnostics, "TrMenu page '$page' cannot be represented by a single-page menu.")
            return emptyList()
        }
        val targetId = menuIdResolver(sourceId)
        if (targetId == null) {
            unsupported(content, path, diagnostics, "Referenced menu '$sourceId' was not found in the migration batch.")
            return emptyList()
        }
        val rewrittenArgs = args.drop(1).map { argument ->
            variableConverter.rewrite(argument, path, diagnostics, strict = true) ?: return emptyList()
        }
        val prefix = if (force) "force-open" else "open"
        return listOf("$prefix: $targetId${rewrittenArgs.joinToString(" ", prefix = if (rewrittenArgs.isEmpty()) "" else " ")}")
    }

    private fun convertRefresh(
        content: String,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): List<Any> {
        val ids = content.split(';').map(String::trim).filter(String::isNotEmpty)
        if (ids.isEmpty()) return listOf("refresh")
        return ids.mapNotNull { sourceId ->
            if (sourceId == "*") return@mapNotNull "refresh: *"
            val targetId = iconIdResolver(sourceId)
            if (targetId == null) {
                unsupported(content, path, diagnostics, "Referenced icon '$sourceId' has no migrated target button.")
                null
            } else {
                "refresh: $targetId"
            }
        }
    }

    private fun convertSetValue(
        type: String,
        content: String,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): List<Any> = content.split(';').mapNotNull { entry ->
        val split = entry.trim().split(Regex("\\s+"), limit = 2)
        if (split.size != 2 || split[0].isBlank()) {
            unsupported(entry, path, diagnostics, "Set action requires a key and value.")
            null
        } else {
            val key = variableConverter.rewrite(split[0], path, diagnostics, strict = true)
                ?: return@mapNotNull null
            val value = variableConverter.rewrite(split[1], path, diagnostics, strict = true)
                ?: return@mapNotNull null
            "$type: type=set;key=$key;var=${quoteParameter(value)}"
        }
    }

    private fun convertDeleteValue(
        type: String,
        content: String,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): List<Any> = content.split(';').map(String::trim).filter(String::isNotEmpty).mapNotNull { key ->
        variableConverter.rewrite(key, path, diagnostics, strict = true)?.let { rewritten ->
            "$type: type=delete;key=$rewritten"
        }
    }

    private fun convertTextAction(
        type: String,
        content: String,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): List<Any> = variableConverter.rewrite(content, path, diagnostics, strict = true)
        ?.let { listOf("$type: $it") }
        .orEmpty()

    private fun convertAmountAction(
        type: String,
        operation: String,
        content: String,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): List<Any> = variableConverter.rewrite(content, path, diagnostics, strict = true)
        ?.let { listOf("$type: type=$operation;num=$it") }
        .orEmpty()

    private fun quoteParameter(value: String): String =
        "`${value.replace("`", "\\`")}`"

    private fun unsupported(
        raw: String,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics,
        reason: String
    ) {
        diagnostics.add(
            code = "TRM_ACTION_UNSUPPORTED",
            severity = TrMenuMigrationSeverity.WARNING,
            compatibility = TrMenuMigrationCompatibility.UNSUPPORTED,
            path = path,
            message = "$reason Source: $raw"
        )
    }
}
