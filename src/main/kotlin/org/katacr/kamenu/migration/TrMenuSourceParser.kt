package org.katacr.kamenu.migration

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/**
 * 安全读取 源菜单 YAML 文件并建立保持声明顺序的源模型。
 *
 * 该阶段不执行源菜单中的 JavaScript、Kether、动作或 PlaceholderAPI。
 */
internal class TrMenuSourceParser {
    /** 读取一个 源菜单 YAML 文件；格式错误不会产生部分源模型。 */
    fun parse(source: File, menuId: String): TrMenuSourceParseResult {
        val diagnostics = TrMenuMigrationDiagnostics()
        if (!source.isFile) {
            diagnostics.add(
                code = "TRM_YAML_INVALID",
                severity = TrMenuMigrationSeverity.ERROR,
                compatibility = TrMenuMigrationCompatibility.INVALID,
                path = source.path,
                message = "Source menu is not a readable file."
            )
            return TrMenuSourceParseResult(null, diagnostics.issues)
        }

        val config = createSourceConfiguration()
        try {
            config.load(source)
        } catch (error: Exception) {
            diagnostics.add(
                code = "TRM_YAML_INVALID",
                severity = TrMenuMigrationSeverity.ERROR,
                compatibility = TrMenuMigrationCompatibility.INVALID,
                path = source.path,
                message = "Failed to read YAML: ${error.message ?: error.javaClass.simpleName}"
            )
            return TrMenuSourceParseResult(null, diagnostics.issues)
        }

        return TrMenuSourceParseResult(
            menu = TrMenuSourceMenu(source, menuId, TrMenuSourceSection.from(config)),
            issues = diagnostics.issues
        )
    }

    companion object {
        /** 创建不会把 源菜单 动态 ID 中点号解释成配置路径的 YAML 读取器。 */
        fun createSourceConfiguration(): YamlConfiguration = YamlConfiguration().also { config ->
            config.options().pathSeparator('\u001F')
        }
    }
}
