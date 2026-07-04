package org.katacr.kamenu

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * 统一文本解析入口
 * 负责输入变量、内置变量与 PAPI 变量的串联解析。
 */
object TextResolver {
    private var plugin: KaMenu? = null
    private var languageManager: LanguageManager? = null

    private val dataPattern = Regex("\\{data:([^}]+)}")
    private val globalDataPattern = Regex("\\{gdata:([^}]+)}")
    private val listPattern = Regex("\\{list:([^}]+)}")
    private val globalListPattern = Regex("\\{glist:([^}]+)}")
    private val metaPattern = Regex("\\{meta:([^}]+)}")
    private val argPattern = Regex("\\{arg:([^}]+)}")
    private val jsPattern = Regex("\\{js:([^}]+)}")

    fun setPlugin(kamenu: KaMenu) {
        plugin = kamenu
    }

    fun setLanguageManager(manager: LanguageManager) {
        languageManager = manager
    }

    fun resolve(player: Player, text: String?, variables: Map<String, String> = emptyMap()): String {
        var result = text ?: return ""

        variables.forEach { (key, value) ->
            if (!key.startsWith("item.") && !key.startsWith("list.") && !key.startsWith("arg:")) {
                result = result.replace("\$($key)", value)
            }
        }

        variables.forEach { (key, value) ->
            result = result.replace("{$key}", value)
        }

        result = result.replace(argPattern) { match ->
            variables["arg:${match.groupValues[1]}"] ?: ""
        }

        val currentPlugin = plugin
        if (currentPlugin != null) {
            result = result.replace("{language}", currentPlugin.config.getString("language", "zh_CN") ?: "zh_CN")

            result = result.replace(dataPattern) { match ->
                val key = match.groupValues[1]
                currentPlugin.databaseManager.getPlayerData(player.uniqueId, key)
                    ?: languageManager?.getMessage("papi.data_not_found", key)
                    ?: "null"
            }
            result = result.replace(globalDataPattern) { match ->
                val key = match.groupValues[1]
                currentPlugin.databaseManager.getGlobalData(key)
                    ?: languageManager?.getMessage("papi.data_not_found", key)
                    ?: "null"
            }
            result = result.replace(listPattern) { match ->
                val key = match.groupValues[1]
                currentPlugin.databaseManager.getPlayerListJson(player.uniqueId, key)
            }
            result = result.replace(globalListPattern) { match ->
                val key = match.groupValues[1]
                currentPlugin.databaseManager.getGlobalListJson(key)
            }
            result = result.replace(metaPattern) { match ->
                val key = match.groupValues[1]
                currentPlugin.metaDataManager.getPlayerMeta(player.uniqueId, key)
            }
        }

        result = result.replace(jsPattern) { match ->
            val script = match.groupValues[1].trim()
            if (script.isEmpty() || !JavaScriptManager.isAvailable()) {
                ""
            } else {
                JavaScriptManager.evaluateWithContext(player, script)?.toString() ?: ""
            }
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, result)
            } catch (_: Exception) {
                // PAPI 解析失败时保留原值
            }
        }

        return result
    }

    fun resolveForCondition(player: Player, text: String?, variables: Map<String, String> = emptyMap()): String {
        return resolveForCondition(player, text, variables) { null }
    }

    fun resolveForCondition(
        player: Player,
        text: String?,
        variables: Map<String, String> = emptyMap(),
        dynamicResolver: (String) -> String?
    ): String {
        var result = text ?: return ""

        variables.forEach { (key, value) ->
            if (!key.startsWith("item.") && !key.startsWith("list.") && !key.startsWith("arg:")) {
                result = replaceConditionToken(result, "\$($key)", value)
            }
        }

        variables.forEach { (key, value) ->
            result = replaceConditionToken(result, "{$key}", value)
        }

        result = replaceConditionRegex(result, argPattern) { match ->
            variables["arg:${match.groupValues[1]}"] ?: ""
        }

        val currentPlugin = plugin
        if (currentPlugin != null) {
            result = replaceConditionToken(result, "{language}", currentPlugin.config.getString("language", "zh_CN") ?: "zh_CN")

            result = replaceConditionRegex(result, dataPattern) { match ->
                val key = match.groupValues[1]
                currentPlugin.databaseManager.getPlayerData(player.uniqueId, key)
                    ?: languageManager?.getMessage("papi.data_not_found", key)
                    ?: "null"
            }
            result = replaceConditionRegex(result, globalDataPattern) { match ->
                val key = match.groupValues[1]
                currentPlugin.databaseManager.getGlobalData(key)
                    ?: languageManager?.getMessage("papi.data_not_found", key)
                    ?: "null"
            }
            result = replaceConditionRegex(result, listPattern) { match ->
                currentPlugin.databaseManager.getPlayerListJson(player.uniqueId, match.groupValues[1])
            }
            result = replaceConditionRegex(result, globalListPattern) { match ->
                currentPlugin.databaseManager.getGlobalListJson(match.groupValues[1])
            }
            result = replaceConditionRegex(result, metaPattern) { match ->
                currentPlugin.metaDataManager.getPlayerMeta(player.uniqueId, match.groupValues[1])
            }
        }

        result = replaceConditionRegex(result, jsPattern) { match ->
            val script = match.groupValues[1].trim()
            if (script.isEmpty() || !JavaScriptManager.isAvailable()) {
                ""
            } else {
                JavaScriptManager.evaluateWithContext(player, script)?.toString() ?: ""
            }
        }

        result = Regex("\\{([^{}]+)}").replace(result) { match ->
            val value = dynamicResolver(match.groupValues[1])
            if (value == null) {
                match.value
            } else {
                encodeConditionReplacement(result, match.range.first, value)
            }
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                result = Regex("%[^%]+%").replace(result) { match ->
                    val value = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, match.value)
                    if (isInsideConditionFunctionArgument(result, match.range.first)) {
                        encodeConditionArgumentValue(value)
                    } else {
                        quoteConditionValue(value)
                    }
                }
            } catch (_: Exception) {
                // PAPI 解析失败时保留原值
            }
        }

        return result
    }

    fun resolve(
        player: Player,
        text: String?,
        variables: Map<String, String> = emptyMap(),
        dynamicResolver: (String) -> String?
    ): String {
        var result = resolve(player, text, variables)
        result = result.replace(Regex("\\{([^{}]+)}")) { match ->
            dynamicResolver(match.groupValues[1]) ?: match.value
        }
        return result
    }

    private fun replaceConditionToken(text: String, token: String, value: String): String {
        return replaceConditionRegex(text, Regex(Regex.escape(token))) { value }
    }

    private fun replaceConditionRegex(text: String, pattern: Regex, valueProvider: (MatchResult) -> String): String {
        return pattern.replace(text) { match ->
            val value = valueProvider(match)
            encodeConditionReplacement(text, match.range.first, value)
        }
    }

    private fun encodeConditionReplacement(text: String, index: Int, value: String): String {
        return when {
            isInsidePapiPlaceholder(text, index) -> value
            isInsideConditionFunctionArgument(text, index) -> encodeConditionArgumentValue(value)
            isInsideConditionStringLiteral(text, index) -> escapeConditionStringContent(value)
            else -> quoteConditionValue(value)
        }
    }

    private fun isInsidePapiPlaceholder(text: String, index: Int): Boolean {
        var count = 0
        for (i in 0 until index.coerceAtMost(text.length)) {
            if (text[i] == '%') count++
        }
        return count % 2 == 1
    }

    private fun isInsideConditionStringLiteral(text: String, index: Int): Boolean {
        var quote: Char? = null
        var escaped = false
        for (i in 0 until index.coerceAtMost(text.length)) {
            val ch = text[i]
            if (escaped) {
                escaped = false
                continue
            }
            if (ch == '\\') {
                escaped = true
                continue
            }
            if (quote == null && (ch == '"' || ch == '\'')) {
                quote = ch
            } else if (quote == ch) {
                quote = null
            }
        }
        return quote != null
    }

    private fun isInsideConditionFunctionArgument(text: String, index: Int): Boolean {
        val names = conditionFunctionNames()
        var searchStart = 0
        while (searchStart < text.length) {
            val match = findNextConditionFunction(text, searchStart, names) ?: return false
            val argStart = match.first + match.second.length + 1
            val argEnd = findConditionFunctionArgumentEnd(text, argStart, stopAtComparison = match.second == "getLength")
            if (index in argStart until argEnd) {
                return true
            }
            searchStart = argEnd.coerceAtLeast(match.first + 1)
        }
        return false
    }

    private fun findNextConditionFunction(text: String, start: Int, names: Set<String>): Pair<Int, String>? {
        var index = start
        while (index < text.length) {
            if (isFunctionBoundaryBefore(text, index)) {
                val name = names.firstOrNull { text.startsWith("$it.", index) }
                if (name != null) return index to name
            }
            index++
        }
        return null
    }

    private fun conditionFunctionNames(): Set<String> {
        return setOf(
            "isNull",
            "isPass",
            "isTrue",
            "isNum",
            "isPosNum",
            "isInt",
            "isPosInt",
            "hasPerm",
            "hasMoney",
            "hasStockItem",
            "hasItem",
            "inList",
            "inGlist",
            "getLength"
        )
    }

    private fun isFunctionBoundaryBefore(text: String, index: Int): Boolean {
        if (index == 0) return true
        return when (text[index - 1]) {
            '!', '(', ' ', '\t', '\n', '\r', '&', '|' -> true
            else -> false
        }
    }

    private fun findConditionFunctionArgumentEnd(text: String, start: Int, stopAtComparison: Boolean): Int {
        var index = start
        while (index < text.length) {
            if (text.startsWith("&&", index) || text.startsWith("||", index) || text[index] == ')') {
                return trimSyntaxWhitespaceBeforeBoundary(text, start, index)
            }
            if (stopAtComparison && isComparisonAt(text, index)) {
                return trimSyntaxWhitespaceBeforeBoundary(text, start, index)
            }
            index++
        }
        return text.length
    }

    private fun trimSyntaxWhitespaceBeforeBoundary(text: String, start: Int, boundary: Int): Int {
        var end = boundary
        while (end > start && text[end - 1].isWhitespace()) {
            end--
        }
        return end
    }

    private fun isComparisonAt(text: String, index: Int): Boolean {
        return text.startsWith("==", index) ||
            text.startsWith("!=", index) ||
            text.startsWith(">=", index) ||
            text.startsWith("<=", index) ||
            text[index] == '>' ||
            text[index] == '<'
    }

    private fun quoteConditionValue(value: String): String {
        return "\"" + escapeConditionStringContent(value) + "\""
    }

    private fun escapeConditionStringContent(value: String): String {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'")
    }

    fun encodeConditionArgumentValue(value: String): String {
        val encoded = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
        return "__KAMENU_COND_ARG_${encoded}__"
    }

    fun decodeConditionArgumentMarkers(value: String): String {
        return Regex("__KAMENU_COND_ARG_([A-Za-z0-9_-]*)__").replace(value) { match ->
            val encoded = match.groupValues[1]
            try {
                String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8)
            } catch (_: IllegalArgumentException) {
                match.value
            }
        }
    }
}
