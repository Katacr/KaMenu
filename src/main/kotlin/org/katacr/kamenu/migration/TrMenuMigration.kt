package org.katacr.kamenu.migration

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.katacr.kamenu.JavaScriptManager
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale

/** 批量扫描、转换、校验并原子写入 源菜单 菜单。 */
class TrMenuMigration(
    private val syntaxValidator: (String) -> String? = JavaScriptManager::validateSyntax
) {
    /** 同名自定义指令冲突。 */
    data class CommandConflict(
        val command: String,
        val existingValue: String,
        val migratedMenuId: String
    )

    /** 源菜单 Bindings.Commands 合并结果。 */
    data class CommandMergeResult(
        val total: Int = 0,
        val added: Int = 0,
        val replaced: Int = 0,
        val unchanged: Int = 0,
        val conflicts: List<CommandConflict> = emptyList(),
        val invalidConfig: Boolean = false
    )

    /** 同名物品绑定冲突。 */
    data class ItemBindingConflict(
        val id: String,
        val existingValue: String,
        val migratedMenuId: String
    )

    /** 源菜单 Bindings.Items 合并结果。 */
    data class ItemBindingMergeResult(
        val total: Int = 0,
        val added: Int = 0,
        val replaced: Int = 0,
        val unchanged: Int = 0,
        val conflicts: List<ItemBindingConflict> = emptyList(),
        val invalidConfig: Boolean = false
    )

    private data class ParsedCandidate(
        val source: File,
        val target: File,
        val sourceMenuId: String,
        val targetMenuId: String?,
        val parseResult: TrMenuSourceParseResult
    )

    /**
     * 迁移一个文件或目录。
     *
     * [menuRoot] 是 KaMenu 的 `menus` 根目录，用于生成目标菜单 ID；[targetDirectory]
     * 必须位于其内部。
     */
    fun migrate(
        source: File,
        targetDirectory: File,
        menuRoot: File,
        overwrite: Boolean = false
    ): TrMenuMigrationBatchResult {
        val started = System.nanoTime()
        if (!source.exists()) {
            return batchFailure(source, "Source file or directory does not exist.", started)
        }
        val files = collectFiles(source)
        if (files.isEmpty()) {
            return batchFailure(source, "No TrMenu YAML files were found.", started)
        }

        val parser = TrMenuSourceParser()
        val candidates = files.map { file ->
            val relative = if (source.isDirectory) file.relativeTo(source) else File(file.name)
            val targetRelative = File(relative.parentFile, "${relative.nameWithoutExtension}.yml")
            val target = File(targetDirectory, targetRelative.path)
            val sourceId = file.nameWithoutExtension
            ParsedCandidate(
                source = file,
                target = target,
                sourceMenuId = sourceId,
                targetMenuId = target.toMenuId(menuRoot),
                parseResult = parser.parse(file, sourceId)
            )
        }
        val duplicates = candidates.groupBy { it.sourceMenuId.lowercase(Locale.ROOT) }
            .filterValues { it.size > 1 }
            .keys
        val targetIds = candidates
            .filter { it.sourceMenuId.lowercase(Locale.ROOT) !in duplicates }
            .mapNotNull { candidate -> candidate.targetMenuId?.let { candidate.sourceMenuId.lowercase(Locale.ROOT) to it } }
            .toMap()
        val converter = TrMenuMenuConverter(
            menuIdResolver = { sourceId -> targetIds[sourceId.lowercase(Locale.ROOT)] },
            syntaxValidator = syntaxValidator
        )

        val results = candidates.map { candidate ->
            migrateCandidate(candidate, duplicates, converter, overwrite)
        }
        return TrMenuMigrationBatchResult(results, elapsedMillis(started))
    }

    /** 将成功迁移文件的 Bindings.Commands 合并到 custom_commands.yml。 */
    fun mergeBoundCommands(
        result: TrMenuMigrationBatchResult,
        menuRoot: File,
        config: FileConfiguration,
        overwrite: Boolean = false
    ): CommandMergeResult {
        val rawSection = config.get("custom-commands")
        val section = config.getConfigurationSection("custom-commands")
            ?: if (rawSection == null) config.createSection("custom-commands")
            else return CommandMergeResult(invalidConfig = true)
        val keysByLowercase = section.getKeys(false)
            .associateBy { it.lowercase(Locale.ROOT) }
            .toMutableMap()
        val conflicts = mutableListOf<CommandConflict>()
        var total = 0
        var added = 0
        var replaced = 0
        var unchanged = 0

        result.files.filter(TrMenuMigrationFileResult::migrated).forEach { file ->
            val menuId = file.target?.toMenuId(menuRoot) ?: return@forEach
            file.boundCommands.forEach { command ->
                total++
                val existingKey = keysByLowercase[command]
                if (existingKey == null) {
                    section.set(command, menuId)
                    keysByLowercase[command] = command
                    added++
                } else {
                    val existing = section.get(existingKey)
                    when {
                        existing is String && existing.trim() == menuId -> unchanged++
                        overwrite -> {
                            section.set(existingKey, menuId)
                            replaced++
                        }
                        else -> conflicts += CommandConflict(command, describeConfigValue(existing), menuId)
                    }
                }
            }
        }
        return CommandMergeResult(total, added, replaced, unchanged, conflicts)
    }

    /** 将成功迁移文件的 Bindings.Items 合并到 item_bindings.yml。 */
    fun mergeBoundItems(
        result: TrMenuMigrationBatchResult,
        config: FileConfiguration,
        overwrite: Boolean = false
    ): ItemBindingMergeResult {
        val rawSection = config.get("item-bindings")
        val section = config.getConfigurationSection("item-bindings")
            ?: if (rawSection == null) config.createSection("item-bindings")
            else return ItemBindingMergeResult(invalidConfig = true)
        val conflicts = mutableListOf<ItemBindingConflict>()
        var total = 0
        var added = 0
        var replaced = 0
        var unchanged = 0

        result.files.filter(TrMenuMigrationFileResult::migrated).forEach { file ->
            file.boundItems.forEach { binding ->
                total++
                val existing = section.getConfigurationSection(binding.id)
                val rawExisting = section.get(binding.id)
                when {
                    rawExisting == null -> {
                        writeBinding(section, binding)
                        added++
                    }
                    existing != null && bindingEquals(existing, binding.values) -> unchanged++
                    overwrite -> {
                        section.set(binding.id, null)
                        writeBinding(section, binding)
                        replaced++
                    }
                    else -> conflicts += ItemBindingConflict(
                        binding.id,
                        existing?.getString("menu") ?: describeConfigValue(rawExisting),
                        binding.values["menu"].toString()
                    )
                }
            }
        }
        return ItemBindingMergeResult(total, added, replaced, unchanged, conflicts)
    }

    private fun migrateCandidate(
        candidate: ParsedCandidate,
        duplicates: Set<String>,
        converter: TrMenuMenuConverter,
        overwrite: Boolean
    ): TrMenuMigrationFileResult {
        val diagnostics = TrMenuMigrationDiagnostics()
        candidate.parseResult.issues.forEach(diagnostics::add)
        if (candidate.sourceMenuId.lowercase(Locale.ROOT) in duplicates) {
            diagnostics.add(
                "TRM_DUPLICATE_MENU_ID",
                TrMenuMigrationSeverity.ERROR,
                TrMenuMigrationCompatibility.INVALID,
                candidate.source.path,
                "Multiple TrMenu files use menu ID '${candidate.sourceMenuId}'. TrMenu open actions cannot resolve a unique target."
            )
        }
        val targetMenuId = candidate.targetMenuId
        if (targetMenuId == null) {
            diagnostics.add(
                "TRM_TARGET_OUTSIDE_MENU_ROOT",
                TrMenuMigrationSeverity.ERROR,
                TrMenuMigrationCompatibility.INVALID,
                candidate.target.path,
                "Migration target must be inside the KaMenu menus directory."
            )
        }
        if (candidate.target.exists() && !overwrite) {
            diagnostics.add(
                "TRM_TARGET_EXISTS",
                TrMenuMigrationSeverity.ERROR,
                TrMenuMigrationCompatibility.INVALID,
                candidate.target.path,
                "Target already exists; use overwrite to replace it."
            )
        }
        val sourceMenu = candidate.parseResult.menu
        if (sourceMenu == null || targetMenuId == null || diagnostics.hasErrors) {
            return TrMenuMigrationFileResult(candidate.source, candidate.target, false, diagnostics.issues)
        }

        val converted = converter.convert(sourceMenu, targetMenuId, diagnostics)
            ?: return TrMenuMigrationFileResult(candidate.source, candidate.target, false, diagnostics.issues)
        try {
            saveAtomically(converted.config, candidate.target)
        } catch (error: Exception) {
            diagnostics.add(
                "TRM_TARGET_WRITE_FAILED",
                TrMenuMigrationSeverity.ERROR,
                TrMenuMigrationCompatibility.INVALID,
                candidate.target.path,
                "Failed to write migrated menu: ${error.message ?: error.javaClass.simpleName}"
            )
            return TrMenuMigrationFileResult(candidate.source, candidate.target, false, diagnostics.issues)
        }
        return TrMenuMigrationFileResult(
            candidate.source,
            candidate.target,
            true,
            diagnostics.issues,
            converted.boundCommands,
            converted.boundItems
        )
    }

    private fun saveAtomically(config: YamlConfiguration, target: File) {
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, ".${target.name}.${System.nanoTime()}.tmp")
        try {
            config.save(temporary)
            try {
                Files.move(
                    temporary.toPath(),
                    target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    private fun collectFiles(source: File): List<File> = if (source.isDirectory) {
        source.walkTopDown()
            .filter { file -> file.isFile && file.extension.lowercase(Locale.ROOT) in setOf("yml", "yaml") }
            .sortedBy { it.relativeTo(source).path.lowercase(Locale.ROOT) }
            .toList()
    } else if (source.isFile && source.extension.lowercase(Locale.ROOT) in setOf("yml", "yaml")) {
        listOf(source)
    } else emptyList()

    private fun File.toMenuId(menuRoot: File): String? {
        val rootPath = menuRoot.absoluteFile.normalize().toPath()
        val targetPath = absoluteFile.normalize().toPath()
        if (!targetPath.startsWith(rootPath)) return null
        return rootPath.relativize(targetPath)
            .toString()
            .replace(File.separatorChar, '/')
            .removeSuffix(".yml")
            .takeIf(String::isNotBlank)
    }

    private fun batchFailure(
        source: File,
        message: String,
        started: Long
    ): TrMenuMigrationBatchResult = TrMenuMigrationBatchResult(
        files = listOf(
            TrMenuMigrationFileResult(
                source,
                null,
                false,
                listOf(
                    TrMenuMigrationIssue(
                        "TRM_SOURCE_INVALID",
                        TrMenuMigrationSeverity.ERROR,
                        TrMenuMigrationCompatibility.INVALID,
                        source.path,
                        message
                    )
                )
            )
        ),
        elapsedMillis = elapsedMillis(started)
    )

    private fun elapsedMillis(started: Long): Long = (System.nanoTime() - started) / 1_000_000L

    private fun writeBinding(section: ConfigurationSection, binding: TrMenuBoundItem) {
        val target = section.createSection(binding.id)
        binding.values.forEach(target::set)
    }

    private fun bindingEquals(section: ConfigurationSection, values: Map<String, Any>): Boolean {
        if (section.getKeys(false) != values.keys) return false
        return values.all { (key, value) -> section.get(key) == value }
    }

    private fun describeConfigValue(value: Any?): String = when (value) {
        null -> "null"
        is String, is Number, is Boolean -> value.toString()
        else -> "<${value.javaClass.simpleName}>"
    }
}
