package org.katacr.kamenu

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** 验证物品 Base64 序列化不依赖服务端内置的 SnakeYAML 私有类。 */
class SerializationUtilTest {

    @Test
    fun `round trips binary data with JDK Base64`() {
        val source = ByteArray(512) { index -> (index * 31).toByte() }

        val encoded = SerializationUtil.encodeBase64(source)

        assertArrayEquals(source, SerializationUtil.decodeBase64(encoded))
    }

    @Test
    fun `decodes legacy line wrapped Base64 records`() {
        val source = ByteArray(512) { index -> (index * 17).toByte() }
        val encoded = SerializationUtil.encodeBase64(source)
        val legacyWrapped = encoded.chunked(76).joinToString("\n", postfix = "\n")

        assertArrayEquals(source, SerializationUtil.decodeBase64(legacyWrapped))
        assertEquals(false, encoded.contains('\n'))
    }
}
