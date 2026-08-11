package org.katacr.kamenu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** 验证材质包纹理路径到 MiniMessage Sprite 引用的规范化规则。 */
class ItemSpriteReferenceTest {
    @Test
    fun `parses shorthand atlas mapping`() {
        assertEquals(
            ItemSpriteReference("minecraft:blocks", "custom:item/chair"),
            ItemSpriteReference.parse("blocks:custom:item/chair")
        )
    }

    @Test
    fun `normalizes resource pack texture paths`() {
        assertEquals(
            "custom:item/chair",
            ItemSpriteReference.normalizeTexture(
                "assets/custom/textures/item/chair.png",
                "minecraft"
            )
        )
        assertEquals(
            "custom:item/chair",
            ItemSpriteReference.normalizeTexture("textures/item/chair.png", "custom")
        )
    }

    @Test
    fun `rejects invalid sprite keys`() {
        assertNull(ItemSpriteReference.parse("blocks:missing_namespace"))
        assertNull(ItemSpriteReference.of("minecraft:blocks", "invalid key"))
    }
}
