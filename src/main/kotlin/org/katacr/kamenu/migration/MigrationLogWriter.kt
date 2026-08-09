package org.katacr.kamenu.migration

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** 将一次菜单迁移的完整报告写入独立 UTF-8 日志文件。 */
class MigrationLogWriter(private val logDirectory: File) {
    /** 写入迁移元数据和报告行，并返回生成的日志文件。 */
    fun write(
        migrationType: String,
        source: File,
        target: File,
        overwrite: Boolean,
        lines: List<String>
    ): File {
        if (!logDirectory.exists() && !logDirectory.mkdirs()) {
            throw IllegalStateException("Cannot create migration log directory: ${logDirectory.absolutePath}")
        }
        if (!logDirectory.isDirectory) {
            throw IllegalStateException("Migration log path is not a directory: ${logDirectory.absolutePath}")
        }

        val generatedAt = LocalDateTime.now()
        val safeType = migrationType.lowercase().replace(unsafeFileNameChars, "-").trim('-').ifEmpty { "unknown" }
        val logFile = File(
            logDirectory,
            "migration-${generatedAt.format(fileNameTimeFormatter)}-$safeType.log"
        )
        val content = buildString {
            appendLine("KaMenu Migration Report")
            appendLine("Generated: ${generatedAt.format(reportTimeFormatter)}")
            appendLine("Type: $migrationType")
            appendLine("Source: ${source.absoluteFile.normalize().path}")
            appendLine("Output: ${target.absoluteFile.normalize().path}")
            appendLine("Overwrite: $overwrite")
            appendLine()
            lines.forEach { line -> appendLine(stripLegacyFormatting(line)) }
        }
        logFile.writeText(content, Charsets.UTF_8)
        return logFile
    }

    companion object {
        private val fileNameTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
        private val reportTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        private val unsafeFileNameChars = Regex("[^a-z0-9_-]+")
        private val legacyFormatting = Regex("(?i)§[0-9A-FK-ORX]")

        /** 移除聊天颜色控制符，使日志可直接搜索和复制。 */
        fun stripLegacyFormatting(value: String): String = value.replace(legacyFormatting, "")
    }
}
