@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import io.papermc.paper.dialog.DialogResponseView

/**
 * Dialog 输入捕获的公共解析与清理工具。
 *
 * 普通 KaMenu 菜单和静态暂停菜单共用此实现，确保 `$(input)`、checkbox 映射、
 * 全局首尾空格处理及 `remove_chars` 的行为一致。
 */
object InputCaptureUtils {
    private const val INPUT_TRIM_EDGE_SPACES_PATH = "input-capture.trim-edge-spaces"
    private const val INPUT_REMOVE_CHAR_LISTS_PATH = "input-capture.remove-char-lists"

    /**
     * 描述一次 Dialog 中需要从客户端响应读取的输入字段。
     */
    data class Schema(
        val keys: List<String>,
        val types: Map<String, String>,
        val removeChars: Map<String, String>,
        val checkboxMappings: Map<String, Pair<String, String>>
    ) {
        companion object {
            val EMPTY = Schema(emptyList(), emptyMap(), emptyMap(), emptyMap())
        }
    }

    /**
     * 从 Paper Dialog 响应中读取并清理全部已注册输入。
     */
    fun captureVariables(
        plugin: KaMenu?,
        response: DialogResponseView?,
        schema: Schema
    ): Map<String, String> {
        if (response == null || schema.keys.isEmpty()) {
            return schema.keys.associateWith { "" }
        }

        return buildMap {
            schema.keys.forEach { key ->
                val value = when {
                    response.getFloat(key) != null -> formatFloatValue(
                        response.getFloat(key)!!,
                        schema.types[key],
                        schema.checkboxMappings[key]
                    )

                    response.getText(key) != null -> response.getText(key).orEmpty()
                    response.getBoolean(key) != null -> formatBooleanValue(
                        response.getBoolean(key)!!,
                        schema.checkboxMappings[key]
                    )
                    else -> ""
                }
                put(key, sanitizeInputValue(plugin, key, value, schema))
            }
        }
    }

    /**
     * 解析 input.remove_chars，支持全局命名预设、列表和转义字符。
     */
    fun resolveRemoveChars(plugin: KaMenu, value: Any?): String {
        return resolveRemoveChars(plugin, value, emptySet())
    }

    private fun resolveRemoveChars(plugin: KaMenu, value: Any?, seenPresets: Set<String>): String {
        return when (value) {
            null -> ""
            is String -> {
                val presetName = value.trim()
                val presetSection = plugin.config.getConfigurationSection(INPUT_REMOVE_CHAR_LISTS_PATH)
                if (presetName.isNotEmpty() && presetSection?.contains(presetName) == true && presetName !in seenPresets) {
                    resolveRemoveChars(plugin, presetSection.get(presetName), seenPresets + presetName)
                } else {
                    decodeRemoveChars(value)
                }
            }

            is List<*> -> value.joinToString("") { resolveRemoveChars(plugin, it, seenPresets) }
            else -> decodeRemoveChars(value.toString())
        }
    }

    private fun formatFloatValue(
        value: Float,
        inputType: String?,
        checkboxMapping: Pair<String, String>?
    ): String {
        if (value != value.toInt().toFloat()) {
            return value.toString()
        }
        val integer = value.toInt()
        if (inputType == "checkbox" && checkboxMapping != null) {
            return if (integer == 1) checkboxMapping.first else checkboxMapping.second
        }
        return integer.toString()
    }

    private fun formatBooleanValue(
        value: Boolean,
        checkboxMapping: Pair<String, String>?
    ): String {
        return checkboxMapping?.let { if (value) it.first else it.second } ?: value.toString()
    }

    private fun sanitizeInputValue(
        plugin: KaMenu?,
        key: String,
        rawValue: String,
        schema: Schema
    ): String {
        if (schema.types[key] != "text") {
            return rawValue
        }

        var value = rawValue
        if (plugin?.config?.getBoolean(INPUT_TRIM_EDGE_SPACES_PATH, false) == true) {
            value = value.trim()
        }
        val removeSet = schema.removeChars[key].orEmpty().toSet()
        if (removeSet.isNotEmpty()) {
            value = value.filterNot { it in removeSet }
        }
        return value
    }

    private fun decodeRemoveChars(value: String): String {
        val result = StringBuilder()
        var index = 0
        while (index < value.length) {
            val char = value[index]
            if (char == '\\' && index + 1 < value.length) {
                when (val escaped = value[index + 1]) {
                    's' -> result.append(' ')
                    'n' -> result.append('\n')
                    'r' -> result.append('\r')
                    't' -> result.append('\t')
                    '\\' -> result.append('\\')
                    else -> result.append(char).append(escaped)
                }
                index += 2
            } else {
                result.append(char)
                index++
            }
        }
        return result.toString()
    }
}
