package org.katacr.kamenu

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/** 验证发布包中的核心 YAML 资源可被 Bukkit 配置解析器读取。 */
class ResourceYamlTest {
    @Test
    fun `loads plugin and language yaml resources`() {
        listOf("plugin.yml", "lang/zh_CN.yml", "lang/en_US.yml").forEach { resource ->
            val stream = javaClass.classLoader.getResourceAsStream(resource)
            assertNotNull(stream, "Missing classpath resource: $resource")
            val source = stream!!.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val config = YamlConfiguration()
            config.loadFromString(source)
            assertFalse(config.getKeys(true).isEmpty(), "Parsed resource is empty: $resource")
        }
    }
}
