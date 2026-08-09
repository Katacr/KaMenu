package org.katacr.kamenu.migration

import java.io.File

/** 源菜单 迁移项相对源菜单行为的兼容程度。 */
enum class TrMenuMigrationCompatibility {
    EXACT,
    APPROXIMATE,
    UNSUPPORTED,
    INVALID
}

/** 源菜单 迁移诊断级别；ERROR 会阻止对应目标文件生成。 */
enum class TrMenuMigrationSeverity {
    INFO,
    WARNING,
    ERROR
}

/** 带稳定代码和源配置路径的 源菜单 迁移诊断。 */
data class TrMenuMigrationIssue(
    val code: String,
    val severity: TrMenuMigrationSeverity,
    val compatibility: TrMenuMigrationCompatibility,
    val path: String,
    val message: String
)

/** 一条可写入 KaMenu `item_bindings.yml` 的 源菜单 菜单物品绑定。 */
data class TrMenuBoundItem(
    val id: String,
    val values: Map<String, Any>
)

/** 单个 源菜单 文件的迁移结果。 */
data class TrMenuMigrationFileResult(
    val source: File,
    val target: File?,
    val migrated: Boolean,
    val issues: List<TrMenuMigrationIssue>,
    val boundCommands: List<String> = emptyList(),
    val boundItems: List<TrMenuBoundItem> = emptyList()
) {
    val warnings: Int
        get() = issues.count { it.severity == TrMenuMigrationSeverity.WARNING }

    val errors: Int
        get() = issues.count { it.severity == TrMenuMigrationSeverity.ERROR }
}

/** 批量 源菜单 迁移结果及汇总计数。 */
data class TrMenuMigrationBatchResult(
    val files: List<TrMenuMigrationFileResult>,
    val elapsedMillis: Long
) {
    val migrated: Int
        get() = files.count { it.migrated }

    val failed: Int
        get() = files.count { !it.migrated }

    val warnings: Int
        get() = files.sumOf(TrMenuMigrationFileResult::warnings)

    val errors: Int
        get() = files.sumOf(TrMenuMigrationFileResult::errors)

    val exact: Int
        get() = files.sumOf { file ->
            file.issues.count { it.compatibility == TrMenuMigrationCompatibility.EXACT }
        }

    val approximate: Int
        get() = files.sumOf { file ->
            file.issues.count { it.compatibility == TrMenuMigrationCompatibility.APPROXIMATE }
        }

    val unsupported: Int
        get() = files.sumOf { file ->
            file.issues.count { it.compatibility == TrMenuMigrationCompatibility.UNSUPPORTED }
        }
}

/** 为单个源菜单累积结构化迁移诊断。 */
internal class TrMenuMigrationDiagnostics {
    private val collected = mutableListOf<TrMenuMigrationIssue>()

    val issues: List<TrMenuMigrationIssue>
        get() = collected.toList()

    val hasErrors: Boolean
        get() = collected.any { it.severity == TrMenuMigrationSeverity.ERROR }

    /** 记录一条迁移诊断。 */
    fun add(
        code: String,
        severity: TrMenuMigrationSeverity,
        compatibility: TrMenuMigrationCompatibility,
        path: String,
        message: String
    ) {
        collected += TrMenuMigrationIssue(code, severity, compatibility, path, message)
    }

    /** 合并源解析阶段已经产生的诊断。 */
    fun add(issue: TrMenuMigrationIssue) {
        collected += issue
    }
}
