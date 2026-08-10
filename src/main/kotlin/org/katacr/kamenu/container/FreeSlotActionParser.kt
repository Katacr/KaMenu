package org.katacr.kamenu.container

/** 解析 `free-slot` 动作参数，保持运行时库存事务代码只处理已校验数据。 */
internal object FreeSlotActionParser {

    /** 将 `key=value;key=value` 参数解析为大小写不敏感的键映射。 */
    fun arguments(raw: String): Map<String, String> = raw
        .split(';')
        .mapNotNull { token ->
            val separator = token.indexOf('=')
            if (separator <= 0) null else token.substring(0, separator).trim().lowercase() to
                token.substring(separator + 1).trim()
        }
        .toMap()

    /** 解析单槽或多材料消费要求；重复 ID 会合并数量，非法输入返回 null。 */
    fun consumeRequirements(arguments: Map<String, String>): Map<String, Int>? {
        val entries = arguments["items"]?.split(',')?.map(String::trim)?.filter(String::isNotEmpty)
            ?: listOfNotNull(arguments["id"]?.let { "$it:${arguments["amount"] ?: "1"}" })
        if (entries.isEmpty()) return null
        val requirements = linkedMapOf<String, Int>()
        entries.forEach { entry ->
            val separator = entry.lastIndexOf(':')
            if (separator <= 0) return null
            val id = entry.substring(0, separator).trim().takeIf(String::isNotEmpty) ?: return null
            val amount = entry.substring(separator + 1).trim().toIntOrNull()?.takeIf { it > 0 } ?: return null
            requirements[id] = (requirements[id] ?: 0) + amount
        }
        return requirements
    }
}
