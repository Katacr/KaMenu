package org.katacr.kamenu

/**
 * 表示客户端资源包中的一个二维 Sprite，并负责生成安全的 MiniMessage 标签。
 */
data class ItemSpriteReference(
    val atlas: String,
    val sprite: String
) {
    /** 将 atlas 与 sprite 的完整命名空间键转换为 MiniMessage Sprite 标签。 */
    fun toMiniMessageTag(): String = "<sprite:'$atlas':'$sprite'>"

    companion object {
        private val KEY_PATTERN = Regex("^[a-z0-9_.-]+:[a-z0-9_./-]+$")

        /**
         * 解析 `blocks:namespace:path`、`items:namespace:path` 或完整 Map 字段使用的键。
         */
        fun parse(raw: String): ItemSpriteReference? {
            val parts = raw.trim().split(':', limit = 3)
            if (parts.size != 3) return null
            val atlas = normalizeAtlas(parts[0]) ?: return null
            val sprite = normalizeKey("${parts[1]}:${parts[2]}") ?: return null
            return ItemSpriteReference(atlas, sprite)
        }

        /** 使用完整 atlas 与 sprite 字段创建引用。 */
        fun of(atlas: String, sprite: String): ItemSpriteReference? {
            val normalizedAtlas = normalizeAtlas(atlas) ?: return null
            val normalizedSprite = normalizeKey(sprite) ?: return null
            return ItemSpriteReference(normalizedAtlas, normalizedSprite)
        }

        /** 将材质包纹理路径规范化为不含 `textures/` 与 `.png` 的命名空间键。 */
        fun normalizeTexture(raw: String, defaultNamespace: String): String? {
            var value = raw.trim().replace('\\', '/')
            if (value.isEmpty()) return null
            value = value.removeSuffix(".png")
            value = value.removePrefix("/")
            if (value.startsWith("assets/")) {
                val path = value.removePrefix("assets/")
                val separator = path.indexOf("/textures/")
                if (separator <= 0) return null
                value = "${path.substring(0, separator)}:${path.substring(separator + 10)}"
            } else {
                value = value.removePrefix("textures/")
            }
            if (':' !in value) value = "$defaultNamespace:$value"
            return normalizeKey(value)
        }

        private fun normalizeAtlas(raw: String): String? {
            val value = when (raw.trim().lowercase()) {
                "blocks" -> "minecraft:blocks"
                "items" -> "minecraft:items"
                else -> raw.trim().lowercase().let { if (':' in it) it else "minecraft:$it" }
            }
            return normalizeKey(value)
        }

        private fun normalizeKey(raw: String): String? {
            val normalized = raw.trim().lowercase()
            return normalized.takeIf(KEY_PATTERN::matches)
        }
    }
}
