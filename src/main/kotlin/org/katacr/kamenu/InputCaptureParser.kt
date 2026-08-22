package org.katacr.kamenu

import org.bukkit.configuration.file.YamlConfiguration

/**
 * 输入捕获定义解析器。
 *
 * 支持三种来源：菜单 YAML 的 `Input-Captures.<id>` 命名定义、`input:` 动作的内联参数、
 * 以及命名定义叠加内联覆盖参数。解析为纯静态过程，便于单元测试。
 */
object InputCaptureParser {

    /** 解析结果：definition 为 null 时 errors 至少包含一条诊断。 */
    data class Result(
        val definition: InputCaptureDefinition?,
        val errors: List<String>
    )

    private val validKeyPattern = Regex("[A-Za-z0-9_-]+")

    /** 解析默认参数，供无插件环境测试使用。 */
    const val DEFAULT_TIMEOUT_SECONDS = 60L
    const val DEFAULT_MAX_LENGTH = 256
    const val DEFAULT_MAX_ATTEMPTS = 3
    const val MIN_TIMEOUT_SECONDS = 5L
    const val MAX_TIMEOUT_SECONDS = 86_400L

    /**
     * 解析菜单内命名定义 `Input-Captures.<id>`。
     *
     * defaultTimeoutSeconds / defaultMaxLength 来自全局 config，供未显式配置时兜底。
     */
    fun parseNamed(
        id: String,
        config: YamlConfiguration?,
        defaultTimeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
        defaultMaxLength: Int = DEFAULT_MAX_LENGTH
    ): Result {
        val errors = mutableListOf<String>()
        val section = config?.getConfigurationSection("Input-Captures.$id")
        if (section == null) {
            errors.add("Input-Captures.$id does not exist")
            return Result(null, errors)
        }

        val typeRaw = section.getString("type", "chat") ?: "chat"
        val type = InputCaptureDefinition.CaptureType.fromConfig(typeRaw)
        if (type == null) {
            errors.add("Input-Captures.$id.type '$typeRaw' is not chat/anvil/dialog")
        }

        val timeoutSeconds = parseTimeout(section.getString("timeout"), defaultTimeoutSeconds)
        if (timeoutSeconds == null) {
            errors.add("Input-Captures.$id.timeout has an invalid duration value")
        }

        val cancelText = section.getString("cancel")?.trim()?.takeIf { it.isNotEmpty() }
        val maxAttempts = section.getInt("max-attempts", DEFAULT_MAX_ATTEMPTS).coerceIn(1, 100)
        val reopen = section.getBoolean("reopen", true)

        val layers = mutableListOf<InputCaptureDefinition.InputLayer>()
        val seenKeys = mutableSetOf<String>()
        val rawLayers = section.getMapList("layers")
        if (rawLayers.isEmpty()) {
            errors.add("Input-Captures.$id.layers must contain at least one layer")
        }
        rawLayers.forEachIndexed { index, rawLayer ->
            val layerErrorsBefore = errors.size
            val layer = parseLayer(rawLayer, "$id.layers[$index]", defaultMaxLength, errors)
            if (errors.size == layerErrorsBefore && layer != null) {
                if (!seenKeys.add(layer.key)) {
                    errors.add("Input-Captures.$id.layers[$index].key '${layer.key}' duplicates an earlier layer key")
                } else {
                    layers.add(layer)
                }
            }
        }

        if (errors.isNotEmpty()) {
            return Result(null, errors)
        }
        return Result(
            InputCaptureDefinition(
                id = id,
                type = type ?: InputCaptureDefinition.CaptureType.CHAT,
                timeoutSeconds = timeoutSeconds ?: defaultTimeoutSeconds,
                cancelText = cancelText,
                maxAttempts = maxAttempts,
                reopen = reopen,
                layers = layers,
                // 动作组键统一使用 -actions 后缀，避免与标量键 timeout/cancel 冲突
                completeActions = section.getList("complete-actions").orEmpty().filterNotNull(),
                timeoutActions = section.getList("timeout-actions").orEmpty().filterNotNull(),
                cancelActions = section.getList("cancel-actions").orEmpty().filterNotNull()
            ),
            emptyList()
        )
    }

    /**
     * 解析 `input:` 动作的纯内联参数，如 `type=chat;key=answer;timeout=60;cancel=exit`。
     *
     * 内联定义为单层结构；prompt 与 complete/timeout/cancel 组留空，由运行时回退到默认语言文案。
     */
    fun parseInline(params: String): Result {
        val values = parseParamMap(params)
        val errors = mutableListOf<String>()

        val id = values["id"] ?: "inline"
        val key = values["key"]?.trim().orEmpty()
        if (key.isEmpty()) {
            errors.add("inline input capture requires key=<name>")
        } else if (!validKeyPattern.matches(key)) {
            errors.add("inline input capture key '$key' may only contain letters, digits, '_' and '-'")
        }

        val typeRaw = values["type"] ?: "chat"
        val type = InputCaptureDefinition.CaptureType.fromConfig(typeRaw)
        if (type == null) {
            errors.add("inline input capture type '$typeRaw' is not chat/anvil/dialog")
        }

        val timeoutSeconds = parseTimeout(values["timeout"], DEFAULT_TIMEOUT_SECONDS)
        if (timeoutSeconds == null) {
            errors.add("inline input capture timeout '${values["timeout"]}' is invalid")
        }

        val maxLength = values["max-length"]?.trim()?.toIntOrNull()
            ?.coerceIn(1, 65_536) ?: DEFAULT_MAX_LENGTH

        if (errors.isNotEmpty()) {
            return Result(null, errors)
        }
        val layer = InputCaptureDefinition.InputLayer(
            key = key,
            maxLength = maxLength,
            title = values["title"]?.trim()?.takeIf { it.isNotEmpty() },
            label = values["label"]?.trim()?.takeIf { it.isNotEmpty() },
            defaultValue = values["default"]?.trim()?.takeIf { it.isNotEmpty() },
            promptActions = emptyList(),
            validateCondition = null,
            invalidActions = emptyList()
        )
        return Result(
            InputCaptureDefinition(
                id = id,
                type = type ?: InputCaptureDefinition.CaptureType.CHAT,
                timeoutSeconds = timeoutSeconds ?: DEFAULT_TIMEOUT_SECONDS,
                cancelText = values["cancel"]?.trim()?.takeIf { it.isNotEmpty() },
                maxAttempts = values["max-attempts"]?.trim()?.toIntOrNull()
                    ?.coerceIn(1, 100) ?: DEFAULT_MAX_ATTEMPTS,
                reopen = values["reopen"]?.trim()?.lowercase() != "false",
                layers = listOf(layer),
                completeActions = emptyList(),
                timeoutActions = emptyList(),
                cancelActions = emptyList()
            ),
            emptyList()
        )
    }

    /**
     * 在命名定义基础上叠加内联覆盖参数，如 `ask_nickname;timeout=30s`。
     *
     * 仅覆盖 type/timeout/cancel/max-length/max-attempts；动作组保持命名定义原值。
     */
    fun applyOverrides(base: InputCaptureDefinition, params: String): Result {
        val values = parseParamMap(params)
        val errors = mutableListOf<String>()

        val type = values["type"]?.let {
            InputCaptureDefinition.CaptureType.fromConfig(it) ?: run {
                errors.add("override type '$it' is not chat/anvil/dialog")
                null
            }
        } ?: base.type

        val timeoutSeconds = values["timeout"]?.let {
            parseTimeout(it, base.timeoutSeconds) ?: run {
                errors.add("override timeout '$it' is invalid")
                null
            }
        } ?: base.timeoutSeconds

        if (errors.isNotEmpty()) {
            return Result(null, errors)
        }
        val overrideMaxLength = values["max-length"]?.trim()?.toIntOrNull()?.coerceIn(1, 65_536)
        return Result(
            base.copy(
                type = type,
                timeoutSeconds = timeoutSeconds,
                cancelText = values["cancel"]?.trim()?.takeIf { v -> v.isNotEmpty() } ?: base.cancelText,
                maxAttempts = values["max-attempts"]?.trim()?.toIntOrNull()
                    ?.coerceIn(1, 100) ?: base.maxAttempts,
                reopen = values["reopen"]?.trim()?.lowercase()?.let { it == "true" } ?: base.reopen,
                layers = base.layers.map { layer ->
                    if (overrideMaxLength != null) layer.copy(maxLength = overrideMaxLength) else layer
                }
            ),
            emptyList()
        )
    }

    /** 解析单层定义；诊断追加到 errors 后返回 null。 */
    private fun parseLayer(
        raw: Map<*, *>,
        path: String,
        defaultMaxLength: Int,
        errors: MutableList<String>
    ): InputCaptureDefinition.InputLayer? {
        val key = raw["key"]?.toString()?.trim().orEmpty()
        if (key.isEmpty()) {
            errors.add("Input-Captures.$path.key is required")
            return null
        }
        if (!validKeyPattern.matches(key)) {
            errors.add("Input-Captures.$path.key '$key' may only contain letters, digits, '_' and '-'")
            return null
        }
        val maxLength = (raw["max-length"] as? Number)?.toInt()
            ?.coerceIn(1, 65_536) ?: defaultMaxLength
        val title = raw["title"]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        val label = raw["label"]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        val defaultValue = raw["default"]?.toString()?.takeIf { it.isNotEmpty() }
        val prompt = (raw["prompt"] as? List<*>).orEmpty().filterNotNull()
        val validate = raw["validate"]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        val invalid = (raw["invalid"] as? List<*>).orEmpty().filterNotNull()
        return InputCaptureDefinition.InputLayer(
            key = key,
            maxLength = maxLength,
            title = title,
            label = label,
            defaultValue = defaultValue,
            promptActions = prompt,
            validateCondition = validate,
            invalidActions = invalid
        )
    }

    /** 把 `k=v;k=v` 参数文本解析为大小写不敏感的键值表。 */
    private fun parseParamMap(params: String): Map<String, String> {
        val result = linkedMapOf<String, String>()
        params.split(";").forEach { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2 && parts[0].trim().isNotEmpty()) {
                result[parts[0].trim().lowercase()] = parts[1].trim()
            }
        }
        return result
    }

    /** 解析超时时长：纯数字按秒，或 `1m30s` 组合；越界收敛到允许区间。 */
    private fun parseTimeout(raw: String?, defaultSeconds: Long): Long? {
        if (raw.isNullOrBlank()) return defaultSeconds
        val millis = TimeValueParser.parseMillis(raw.trim()) ?: return null
        return (millis / 1000L).coerceIn(MIN_TIMEOUT_SECONDS, MAX_TIMEOUT_SECONDS)
    }
}
