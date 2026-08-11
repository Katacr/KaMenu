package org.katacr.kamenu

import org.bukkit.Bukkit

/**
 * 集中判断由 Minecraft 服务器版本决定的平台能力。
 */
object MinecraftFeatures {
    private val spriteObjectsSupported by lazy(LazyThreadSafetyMode.PUBLICATION) {
        supportsSpriteObjects(Bukkit.getBukkitVersion())
    }

    /** 当前服务器是否允许向客户端发送 1.21.9 引入的 Sprite 文本组件。 */
    fun supportsSpriteObjects(): Boolean = spriteObjectsSupported

    /** 根据 Bukkit 版本字符串判断 Sprite 文本组件是否可用，供边界测试复用。 */
    internal fun supportsSpriteObjects(bukkitVersion: String): Boolean {
        val match = VERSION_PATTERN.find(bukkitVersion.trim()) ?: return false
        val major = match.groupValues[1].toIntOrNull() ?: return false
        val minor = match.groupValues[2].toIntOrNull() ?: 0
        val patch = match.groupValues[3].toIntOrNull() ?: 0
        return compareValuesBy(
            Version(major, minor, patch),
            SPRITE_MINIMUM_VERSION,
            Version::major,
            Version::minor,
            Version::patch
        ) >= 0
    }

    private data class Version(val major: Int, val minor: Int, val patch: Int)

    private val VERSION_PATTERN = Regex("^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?")
    private val SPRITE_MINIMUM_VERSION = Version(1, 21, 9)
}
