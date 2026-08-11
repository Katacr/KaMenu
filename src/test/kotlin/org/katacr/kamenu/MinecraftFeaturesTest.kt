package org.katacr.kamenu

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 验证由 Bukkit 版本字符串驱动的客户端能力边界。 */
class MinecraftFeaturesTest {
    @Test
    fun `sprite objects require minecraft 1_21_9 or newer`() {
        assertFalse(MinecraftFeatures.supportsSpriteObjects("1.16.5-R0.1-SNAPSHOT"))
        assertFalse(MinecraftFeatures.supportsSpriteObjects("1.21-R0.1-SNAPSHOT"))
        assertFalse(MinecraftFeatures.supportsSpriteObjects("1.21.8-R0.1-SNAPSHOT"))
        assertTrue(MinecraftFeatures.supportsSpriteObjects("1.21.9-R0.1-SNAPSHOT"))
        assertTrue(MinecraftFeatures.supportsSpriteObjects("1.21.10-R0.1-SNAPSHOT"))
        assertTrue(MinecraftFeatures.supportsSpriteObjects("1.22-R0.1-SNAPSHOT"))
        assertTrue(MinecraftFeatures.supportsSpriteObjects("26.1-R0.1-SNAPSHOT"))
        assertTrue(MinecraftFeatures.supportsSpriteObjects("26.2-R0.1-SNAPSHOT"))
    }

    @Test
    fun `unknown versions fail closed`() {
        assertFalse(MinecraftFeatures.supportsSpriteObjects("unknown"))
        assertFalse(MinecraftFeatures.supportsSpriteObjects(""))
    }
}
