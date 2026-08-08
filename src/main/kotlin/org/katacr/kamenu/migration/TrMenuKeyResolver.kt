package org.katacr.kamenu.migration

/**
 * 按 TrMenu stable-v3 的 `Property.getSectionKey` 规则解析源 YAML 键。
 *
 * 解析器保留输入顺序并选择第一个正则命中项；调用方负责限定当前配置上下文，
 * 避免把图标 ID、函数 ID 等用户键误当成属性别名。
 */
internal object TrMenuKeyResolver {
    /** 单次键解析结果，多个 [matchingKeys] 表示源配置存在别名冲突。 */
    data class Resolution(
        val property: TrMenuSourceProperty,
        val selectedKey: String?,
        val matchingKeys: List<String>
    ) {
        val hasCollision: Boolean
            get() = matchingKeys.size > 1

        val usedAlias: Boolean
            get() = selectedKey != null && selectedKey != property.canonicalKey
    }

    /**
     * 在当前层级的有序键集合中查找 [property]。
     *
     * 正则未命中时保留 TrMenu 的标准键回退语义；通常标准键本身也会命中正则。
     */
    fun resolve(keys: Iterable<String>, property: TrMenuSourceProperty): Resolution {
        val orderedKeys = keys.toList()
        val matches = orderedKeys.filter { it.matches(property.regex) }
        val selected = matches.firstOrNull()
            ?: orderedKeys.firstOrNull { it == property.canonicalKey }

        return Resolution(property, selected, matches)
    }
}
