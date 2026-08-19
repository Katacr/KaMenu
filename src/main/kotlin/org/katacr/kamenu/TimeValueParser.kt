package org.katacr.kamenu

/**
 * 时长字符串解析工具。
 *
 * 用于把 `ttl` 等动作参数解析为毫秒：纯数字视作秒，也支持 `d`/`h`/`m`/`s`
 * 后缀组合（如 `1d2h30m15s`），大小写不敏感。解析失败或为负数时返回 null，
 * 调用方应安全失败并告警。
 */
object TimeValueParser {

    private val compositePattern = Regex("^[0-9]+([dhms][0-9]+)*[dhms]$")
    private val tokenPattern = Regex("([0-9]+)([dhms])")

    private const val SECOND_MILLIS = 1_000L
    private const val MINUTE_MILLIS = 60 * SECOND_MILLIS
    private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
    private const val DAY_MILLIS = 24 * HOUR_MILLIS

    /**
     * 解析时长字符串为毫秒。
     *
     * @return 非负毫秒数；格式非法或为负数时返回 null。
     */
    fun parseMillis(raw: String): Long? {
        val text = raw.trim().lowercase()
        if (text.isEmpty()) return null

        text.toLongOrNull()?.let { plain ->
            if (plain < 0) return null
            return saturatingSecondsToMillis(plain)
        }

        if (!compositePattern.matches(text)) return null

        var millis = 0L
        for (match in tokenPattern.findAll(text)) {
            val number = match.groupValues[1].toLong()
            if (number < 0) return null
            val unitMillis = when (match.groupValues[2]) {
                "d" -> DAY_MILLIS
                "h" -> HOUR_MILLIS
                "m" -> MINUTE_MILLIS
                else -> SECOND_MILLIS
            }
            val addend = try {
                Math.multiplyExact(number, unitMillis)
            } catch (_: ArithmeticException) {
                return null
            }
            millis = try {
                Math.addExact(millis, addend)
            } catch (_: ArithmeticException) {
                return null
            }
        }
        return millis.coerceAtLeast(0L)
    }

    /** 秒转毫秒，超出 Long 范围时饱和到 Long.MAX_VALUE。 */
    private fun saturatingSecondsToMillis(seconds: Long): Long {
        return if (seconds > Long.MAX_VALUE / SECOND_MILLIS) {
            Long.MAX_VALUE
        } else {
            seconds * SECOND_MILLIS
        }
    }
}