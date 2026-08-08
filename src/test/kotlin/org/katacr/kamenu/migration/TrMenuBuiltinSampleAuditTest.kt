package org.katacr.kamenu.migration

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.katacr.kamenu.container.ContainerMenuParser
import java.io.File

/** 按需审计外部 TrMenu stable-v3 内置样例，CI 未提供路径时自动跳过。 */
class TrMenuBuiltinSampleAuditTest {
    @TempDir
    lateinit var temporaryDirectory: File

    @Test
    fun `audits trmenu built in samples without producing invalid target menus`() {
        val sourcePath = System.getProperty("trmenu.sample.dir") ?: System.getenv("TRMENU_SAMPLE_DIR")
        val source = sourcePath?.let(::File)
        assumeTrue(source?.isDirectory == true, "Set TRMENU_SAMPLE_DIR to the TrMenu menus directory")
        val menuRoot = File(temporaryDirectory, "menus").apply { mkdirs() }
        val target = File(menuRoot, "trmenu_migrated")

        val result = TrMenuMigration { null }.migrate(source!!, target, menuRoot)
        val issueCounts = result.files.flatMap { it.issues }
            .groupingBy { it.code }
            .eachCount()
            .toSortedMap()
        println(
            "TRMENU_SAMPLE_AUDIT files=${result.files.size} migrated=${result.migrated} " +
                "failed=${result.failed} warnings=${result.warnings} errors=${result.errors} issues=$issueCounts"
        )
        result.files.forEach { file ->
            val codes = file.issues.groupingBy { it.code }.eachCount().toSortedMap()
            println("TRMENU_SAMPLE_FILE file=${file.source.name} migrated=${file.migrated} issues=$codes")
        }

        assertTrue(result.files.isNotEmpty())
        result.files.filter(TrMenuMigrationFileResult::migrated).forEach { file ->
            val config = YamlConfiguration.loadConfiguration(file.target!!)
            val menuId = menuRoot.toPath().relativize(file.target.toPath()).toString()
                .replace(File.separatorChar, '/')
                .removeSuffix(".yml")
            val parsed = ContainerMenuParser.parse(menuId, config)
            assertTrue(parsed.definition != null, "Generated target failed validation: ${file.target}")
            assertEquals(0, parsed.diagnostics.count { it.severity.name == "ERROR" })
        }
    }
}
