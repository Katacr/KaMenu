package org.katacr.kamenu.migration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/** 验证迁移日志目录、元数据、UTF-8 内容和颜色代码清理。 */
class MigrationLogWriterTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `writes searchable migration report`() {
        val logDirectory = temporaryDirectory.resolve("logs/migration").toFile()
        val writer = MigrationLogWriter(logDirectory)

        val result = writer.write(
            migrationType = "TrMenu",
            source = temporaryDirectory.resolve("source").toFile(),
            target = temporaryDirectory.resolve("target").toFile(),
            overwrite = true,
            lines = listOf("§aMigration complete", "§e[WARNING] Icons.shop: unsupported action")
        )

        assertEquals(logDirectory.canonicalFile, result.parentFile.canonicalFile)
        assertTrue(result.name.matches(Regex("migration-\\d{8}-\\d{6}-\\d{3}-trmenu\\.log")))
        val content = result.readText(Charsets.UTF_8)
        assertTrue(content.contains("Type: TrMenu"))
        assertTrue(content.contains("Overwrite: true"))
        assertTrue(content.contains("[WARNING] Icons.shop: unsupported action"))
        assertFalse(content.contains('§'))
    }
}
