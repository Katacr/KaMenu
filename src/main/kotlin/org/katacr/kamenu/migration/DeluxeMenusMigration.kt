package org.katacr.kamenu.migration

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.Locale

/**
 * 将 DeluxeMenus 的 GUI 菜单转换为 KaMenu V2 Container 菜单。
 *
 * 转换器只处理文件和 YAML 数据，不依赖 Bukkit 运行时，也不会执行 DM 动作。
 * 同一槽位的多个 DM 物品会按照 priority 合并成一个 KaMenu 按钮，并通过
 * 条件分支保持 DM 的优先级语义。无法可靠映射的条件、物品或动作会记录在报告中，
 * 默认不会把未知行为直接写入输出文件。
 */
class DeluxeMenusMigration {
    /** 单个迁移问题；path 使用 DM 配置路径，便于用户回到源文件修正。 */
    data class Issue(
        val severity: Severity,
        val path: String,
        val message: String
    )

    /** 迁移问题级别。ERROR 会阻止目标文件生成，WARNING 允许生成但需要人工复核。 */
    enum class Severity {
        WARNING,
        ERROR
    }

    /** 单个文件的迁移结果。 */
    data class FileResult(
        val source: File,
        val target: File?,
        val migrated: Boolean,
        val issues: List<Issue>,
        val openCommands: List<String> = emptyList()
    ) {
        val warnings: Int
            get() = issues.count { it.severity == Severity.WARNING }

        val errors: Int
            get() = issues.count { it.severity == Severity.ERROR }
    }

    /** 批量迁移结果。 */
    data class BatchResult(val files: List<FileResult>) {
        val migrated: Int
            get() = files.count { it.migrated }

        val failed: Int
            get() = files.count { !it.migrated }

        val warnings: Int
            get() = files.sumOf(FileResult::warnings)

        val errors: Int
            get() = files.sumOf(FileResult::errors)
    }

    /** 同名 KaMenu 自定义指令与 DM 打开指令发生冲突时的报告。 */
    data class CommandConflict(
        val command: String,
        val existingValue: String,
        val migratedMenuId: String
    )

    /** 将迁移结果合并到 custom_commands.yml 的 custom-commands 节后统计。 */
    data class CommandMergeResult(
        val total: Int = 0,
        val added: Int = 0,
        val replaced: Int = 0,
        val unchanged: Int = 0,
        val conflicts: List<CommandConflict> = emptyList(),
        val invalidConfig: Boolean = false
    )

    private data class Candidate(
        val id: String,
        val section: ConfigurationSection,
        val priority: Int,
        val sourceIndex: Int,
        val slots: List<Int>,
        val viewCondition: String?
    )

    private data class CandidateCondition(
        val raw: String,
        val effective: String
    )

    private data class Requirement(
        val expression: String,
        val optional: Boolean
    )

    private val supportedClickTypes = listOf(
        "click",
        "left_click",
        "right_click",
        "shift_left_click",
        "shift_right_click",
        "middle_click"
    )

    /** 迁移一个文件或目录；目录会递归处理其中的 YAML 文件。 */
    fun migrate(source: File, targetDirectory: File, overwrite: Boolean = false): BatchResult {
        if (!source.exists()) {
            return BatchResult(
                listOf(
                    FileResult(
                        source = source,
                        target = null,
                        migrated = false,
                        issues = listOf(Issue(Severity.ERROR, source.path, "Source file or directory does not exist."))
                    )
                )
            )
        }

        val files = if (source.isDirectory) {
            source.walkTopDown()
                .filter { it.isFile && it.extension.equals("yml", ignoreCase = true) }
                .sortedBy { it.relativeTo(source).path.lowercase(Locale.ROOT) }
                .toList()
        } else {
            listOf(source)
        }

        if (files.isEmpty()) {
            return BatchResult(
                listOf(
                    FileResult(
                        source = source,
                        target = null,
                        migrated = false,
                        issues = listOf(Issue(Severity.ERROR, source.path, "No YAML files were found."))
                    )
                )
            )
        }

        return BatchResult(files.map { file ->
            val relative = if (source.isDirectory) file.relativeTo(source) else File(file.name)
            val target = File(targetDirectory, relative.path)
            migrateFile(file, target, overwrite)
        })
    }

    /** 将单个 DM 文件转换为 KaMenu V2 YAML；失败时不会留下半成品目标文件。 */
    fun migrateFile(source: File, target: File, overwrite: Boolean = false): FileResult {
        val issues = mutableListOf<Issue>()
        if (target.exists() && !overwrite) {
            issues += issue(Severity.ERROR, target.path, "Target already exists; use overwrite to replace it.")
            return FileResult(source, target, migrated = false, issues)
        }

        val config = runCatching { YamlConfiguration.loadConfiguration(source) }.getOrElse { error ->
            issues += issue(Severity.ERROR, source.path, "Failed to read YAML: ${error.message ?: error.javaClass.simpleName}")
            return FileResult(source, target, migrated = false, issues)
        }

        val items = config.getConfigurationSection("items")
        if (items == null) {
            issues += issue(Severity.ERROR, "items", "DeluxeMenus file does not contain an items section.")
            return FileResult(source, target, migrated = false, issues)
        }

        val size = parseSize(config.get("size"), issues)
        if (size == null) return FileResult(source, target, migrated = false, issues)

        val candidates = parseCandidates(items, size, issues)
        if (candidates.isEmpty()) {
            issues += issue(Severity.ERROR, "items", "No valid menu items were found.")
            return FileResult(source, target, migrated = false, issues)
        }

        val openCommands = parseOpenCommands(config, issues)
        val itemUpdateInterval = resolveItemUpdateInterval(config, candidates)
        val output = YamlConfiguration()
        output.options().header(
            "Migrated from DeluxeMenus. Review the migration report before using this menu."
        )
        output.set("Type", "CHEST")
        output.set("Title", config.get("menu_title") ?: source.nameWithoutExtension)
        output.set("Layout", buildLayout(size, candidates, issues))
        writeOpenEvent(output, config, issues)
        writeButtons(output, candidates, size, itemUpdateInterval, issues)

        if (issues.any { it.severity == Severity.ERROR }) {
            return FileResult(source, target, migrated = false, issues)
        }

        try {
            target.parentFile?.mkdirs()
            output.save(target)
        } catch (error: Exception) {
            issues += issue(
                Severity.ERROR,
                target.path,
                "Failed to write migrated menu: ${error.message ?: error.javaClass.simpleName}"
            )
            return FileResult(source, target, migrated = false, issues)
        }

        return FileResult(source, target, migrated = true, issues, openCommands)
    }

    /**
     * 将已迁移菜单的 `open_command` 合并到 KaMenu custom_commands.yml 的
     * `custom-commands` 节点。
     *
     * 默认保留已有同名配置；[overwrite] 为 true 时才替换冲突项。此方法只修改传入的
     * [config] 内存对象，调用方负责保存 custom_commands.yml 和重载运行时指令。
     */
    fun mergeOpenCommands(
        result: BatchResult,
        menuRoot: File,
        config: FileConfiguration,
        overwrite: Boolean = false
    ): CommandMergeResult {
        val rawSection = config.get("custom-commands")
        val section = config.getConfigurationSection("custom-commands")
            ?: if (rawSection == null) config.createSection("custom-commands") else return CommandMergeResult(invalidConfig = true)
        val keysByLowercase = section.getKeys(false).associateBy { it.lowercase(Locale.ROOT) }.toMutableMap()
        val conflicts = mutableListOf<CommandConflict>()
        var total = 0
        var added = 0
        var replaced = 0
        var unchanged = 0

        result.files.filter(FileResult::migrated).forEach { file ->
            val target = file.target ?: return@forEach
            val menuId = target.toMenuId(menuRoot) ?: return@forEach
            file.openCommands.forEach { command ->
                total++
                val existingKey = keysByLowercase[command]
                if (existingKey == null) {
                    section.set(command, menuId)
                    keysByLowercase[command] = command
                    added++
                    return@forEach
                }

                val existing = section.get(existingKey)
                if (existing is String && existing.trim() == menuId) {
                    unchanged++
                } else if (overwrite) {
                    section.set(existingKey, menuId)
                    replaced++
                } else {
                    conflicts += CommandConflict(command, describeConfigValue(existing), menuId)
                }
            }
        }

        return CommandMergeResult(total, added, replaced, unchanged, conflicts)
    }

    /** 读取并校验 DM 的单个或列表形式 `open_command`。 */
    private fun parseOpenCommands(source: YamlConfiguration, issues: MutableList<Issue>): List<String> {
        if (!source.contains("open_command")) return emptyList()
        val raw = source.get("open_command")
        val values = when (raw) {
            is String -> listOf(raw)
            is List<*> -> raw.mapNotNull { it?.toString() }
            else -> {
                issues += issue(Severity.WARNING, "open_command", "open_command must be a string or string list and was skipped.")
                return emptyList()
            }
        }
        val commandPattern = Regex("^[a-z0-9][a-z0-9_-]*$")
        return values.mapNotNull { value ->
            val command = value.trim().removePrefix("/").lowercase(Locale.ROOT)
            if (commandPattern.matches(command)) {
                command
            } else {
                issues += issue(Severity.WARNING, "open_command", "Invalid command '$value' was skipped.")
                null
            }
        }.distinct()
    }

    /** 将目标菜单文件转换为相对 `menus` 根目录的 KaMenu 菜单 ID。 */
    private fun File.toMenuId(menuRoot: File): String? {
        val normalizedRoot = menuRoot.absoluteFile.normalize().toPath()
        val normalizedTarget = absoluteFile.normalize().toPath()
        if (!normalizedTarget.startsWith(normalizedRoot)) return null
        val relative = normalizedRoot.relativize(normalizedTarget).toString().replace(File.separatorChar, '/')
        return relative.removeSuffix(".yml").takeIf { it.isNotBlank() }
    }

    /** 为冲突报告生成稳定、简短的现有配置描述。 */
    private fun describeConfigValue(value: Any?): String = when (value) {
        null -> "null"
        is String, is Number, is Boolean -> value.toString()
        is ConfigurationSection -> "<section>"
        else -> "<${value.javaClass.simpleName}>"
    }

    /** 将 DM 的秒级 update_interval 转换为按钮级 tick 周期；DM 缺省值为 10 秒。 */
    private fun resolveItemUpdateInterval(
        source: YamlConfiguration,
        candidates: Map<Int, List<Candidate>>
    ): Long? {
        if (candidates.values.flatten().none { it.section.getBoolean("update", false) }) return null
        val configuredSeconds = when (val configured = source.get("update_interval")) {
            is Number -> configured.toInt()
            else -> configured?.toString()?.toIntOrNull()
        }
        val seconds = configuredSeconds?.takeIf { it > 0 } ?: 10
        return seconds * 20L
    }

    /** 读取 DM 菜单尺寸；V2 箱子菜单只接受 9 的倍数且最大 54。 */
    private fun parseSize(raw: Any?, issues: MutableList<Issue>): Int? {
        val size = (raw as? Number)?.toInt() ?: raw?.toString()?.toIntOrNull() ?: 54
        if (size !in 9..54 || size % 9 != 0) {
            issues += issue(
                Severity.ERROR,
                "size",
                "Only chest sizes from 9 to 54 in multiples of 9 are supported; found $size."
            )
            return null
        }
        return size
    }

    /** 将 DM items 按槽位展开，保留 priority、view_requirement 和原始配置顺序。 */
    private fun parseCandidates(
        items: ConfigurationSection,
        size: Int,
        issues: MutableList<Issue>
    ): Map<Int, List<Candidate>> {
        val bySlot = linkedMapOf<Int, MutableList<Candidate>>()
        items.getKeys(false).forEachIndexed { sourceIndex, id ->
            val section = items.getConfigurationSection(id)
            if (section == null) {
                issues += issue(Severity.WARNING, "items.$id", "Item entry is not a YAML section and was skipped.")
                return@forEachIndexed
            }

            val slots = parseSlots(section, "items.$id", size, issues)
            if (slots.isEmpty()) return@forEachIndexed
            val viewCondition = requirementExpression(section, "view_requirement", issues)
            val candidate = Candidate(
                id = id,
                section = section,
                priority = section.getInt("priority", 1),
                sourceIndex = sourceIndex,
                slots = slots,
                viewCondition = viewCondition
            )
            slots.forEach { slot -> bySlot.getOrPut(slot, ::mutableListOf) += candidate }
        }
        return bySlot.mapValues { (_, values) ->
            values.sortedWith(compareBy<Candidate> { it.priority }.thenBy { it.sourceIndex })
        }
    }

    /** 解析 DM 的 slot 和 slots，支持整数、整数列表以及 `0-8` 范围。 */
    private fun parseSlots(
        section: ConfigurationSection,
        path: String,
        size: Int,
        issues: MutableList<Issue>
    ): List<Int> {
        val rawValues = if (section.isList("slots")) {
            section.getList("slots").orEmpty()
        } else {
            listOf(section.get("slot") ?: 0)
        }
        val result = linkedSetOf<Int>()
        rawValues.forEach { raw ->
            val text = raw.toString().trim()
            val range = Regex("^(\\d+)\\s*-\\s*(\\d+)$").matchEntire(text)
            if (range != null) {
                val start = range.groupValues[1].toInt()
                val end = range.groupValues[2].toInt()
                if (start > end) {
                    issues += issue(Severity.WARNING, "$path.slots", "Slot range '$text' is reversed and was skipped.")
                } else {
                    (start..end).forEach { result += it }
                }
            } else {
                text.toIntOrNull()?.let(result::add)
                    ?: issues.add(issue(Severity.WARNING, "$path.slots", "Invalid slot '$text' was skipped."))
            }
        }
        val invalid = result.filter { it !in 0 until size }
        invalid.forEach { slot ->
            issues += issue(Severity.WARNING, "$path.slot", "Slot $slot is outside the migrated inventory and was skipped.")
            result.remove(slot)
        }
        return result.toList()
    }

    /** 生成 V2 Layout；每个物理槽位使用独立命名按钮，便于合并 DM priority 候选。 */
    private fun buildLayout(
        size: Int,
        candidates: Map<Int, List<Candidate>>,
        issues: MutableList<Issue>
    ): List<String> {
        val tokens = Array(size) { " " }
        for (slot in 0 until size) {
            if (candidates[slot].isNullOrEmpty()) continue
            tokens[slot] = "`dm_slot_$slot`"
        }
        return tokens.toList().chunked(9).map { row -> row.joinToString("") }
    }

    /** 写入 DM open_commands 和 open_requirement；打开条件失败时用 return 阻止菜单显示。 */
    private fun writeOpenEvent(
        output: YamlConfiguration,
        source: YamlConfiguration,
        issues: MutableList<Issue>
    ) {
        val actions = collectRequirementActions(source, "open_requirement", "success_commands", issues).toMutableList().apply {
            addAll(translateActions(source.getList("open_commands").orEmpty(), "open_commands", issues))
        }
        val requirement = source.getConfigurationSection("open_requirement")?.let {
            requirementExpression(source, "open_requirement", issues)
        }
        val deny = translateActions(
            source.getList("open_requirement.deny_commands").orEmpty(),
            "open_requirement.deny_commands",
            issues
        ).toMutableList().apply {
            addAll(collectRequirementActions(source, "open_requirement", "deny_commands", issues))
            if ("return" !in this) add("return")
        }

        if (requirement != null) {
            val branch = linkedMapOf<String, Any>(
                "condition" to requirement,
                "allow" to actions,
                "deny" to deny
            )
            output.set("Events.Open", listOf(branch))
        } else if (actions.isNotEmpty()) {
            output.set("Events.Open", actions)
        }

    }

    /** 按槽位写入按钮和点击动作。 */
    private fun writeButtons(
        output: YamlConfiguration,
        candidates: Map<Int, List<Candidate>>,
        size: Int,
        itemUpdateInterval: Long?,
        issues: MutableList<Issue>
    ) {
        candidates.forEach { (slot, slotCandidates) ->
            val conditions = buildCandidateConditions(slotCandidates, issues)
            val button = linkedMapOf<String, Any>(
                "display" to buildDisplay(slotCandidates, conditions, issues)
            )
            val visible = if (conditions.any { it.raw == "true" }) {
                null
            } else {
                conditions.map { it.raw }.distinct().joinToString(" || ")
            }
            if (visible != null) button["view_condition"] = visible
            if (itemUpdateInterval != null && slotCandidates.any { it.section.getBoolean("update", false) }) {
                button["update"] = itemUpdateInterval
            }

            val actions = linkedMapOf<String, Any>()
            supportedClickTypes.forEach { clickType ->
                val clickActions = buildClickActions(slotCandidates, conditions, clickType, issues)
                if (clickActions.isNotEmpty()) actions[clickKey(clickType)] = clickActions
            }
            if (actions.isNotEmpty()) button["actions"] = actions
            output.set("Buttons.dm_slot_$slot", button)
        }

        val highestSlot = candidates.keys.maxOrNull()
        if (highestSlot != null && highestSlot >= size) {
            issues += issue(Severity.ERROR, "items", "A generated button exceeded the target inventory size.")
        }
    }

    /** 计算 DM priority 候选的互斥条件，使后续低优先级物品不会同时覆盖同一槽位。 */
    private fun buildCandidateConditions(
        candidates: List<Candidate>,
        issues: MutableList<Issue>
    ): List<CandidateCondition> {
        val result = mutableListOf<CandidateCondition>()
        val previous = mutableListOf<String>()
        candidates.forEach { candidate ->
            val raw = candidate.viewCondition ?: "true"
            val effective = if (previous.isEmpty()) {
                raw
            } else {
                "$raw && !(${previous.joinToString(" || ")})"
            }
            result += CandidateCondition(raw, effective)
            previous += raw
        }
        if (result.isEmpty()) {
            issues += issue(Severity.ERROR, "items", "No candidates were available for a generated slot.")
        }
        return result
    }

    /** 将 DM 物品显示字段转换为 V2 display；多候选字段使用首个匹配条件分支。 */
    private fun buildDisplay(
        candidates: List<Candidate>,
        conditions: List<CandidateCondition>,
        issues: MutableList<Issue>
    ): Map<String, Any> {
        val display = linkedMapOf<String, Any>()
        val properties = listOf("material", "name", "lore", "amount", "item_flags", "enchantments", "custom_model_data", "item_model", "skull_owner", "skull_texture", "unbreakable")
        properties.forEach { property ->
            val values = candidates.mapIndexedNotNull { index, candidate ->
                val raw = readDisplayValue(candidate.section, property, issues)
                raw?.let { conditionValue(conditions[index].effective, it) }
            }
            if (values.isNotEmpty()) {
                display[property] = if (values.size == 1 && conditions.size == 1) values.first()["allow"]!! else values
            }
        }
        if (!display.containsKey("material")) {
            display["material"] = "PAPER"
            issues += issue(Severity.WARNING, "items", "A generated button has no material and was set to PAPER.")
        }
        return display
    }

    /** 读取 DM 显示属性，并将旧字段或私有材质 ID 转换为 V2 表达。 */
    private fun readDisplayValue(
        section: ConfigurationSection,
        property: String,
        issues: MutableList<Issue>
    ): Any? {
        return when (property) {
            "material" -> {
                val raw = section.getString("material")?.trim() ?: return null
                if (section.contains("data")) {
                    issues += issue(
                        Severity.WARNING,
                        "items.${section.name}.data",
                        "Legacy material data is not supported by KaMenu and was ignored."
                    )
                }
                when {
                    raw.startsWith("head-", ignoreCase = true) -> "PLAYER_HEAD"
                    raw.startsWith("basehead-", ignoreCase = true) -> "PLAYER_HEAD"
                    raw.startsWith("hdb-", ignoreCase = true) -> {
                        issues += issue(Severity.WARNING, "items.${section.name}.material", "HeadDatabase material '$raw' needs manual conversion.")
                        "PAPER"
                    }
                    else -> raw
                }
            }
            "name" -> section.get("display_name")
            "lore" -> section.get("lore")
            "amount" -> section.get("amount")
            "custom_model_data" -> section.get("custom_model_data") ?: section.get("model_data")
            "item_model" -> section.get("item_model")
            "unbreakable" -> section.get("unbreakable")?.takeIf { it.toString().equals("true", true) }
            "skull_owner" -> section.getString("material")?.takeIf { it.startsWith("head-", true) }?.substringAfter('-')
            "skull_texture" -> section.getString("material")?.takeIf { it.startsWith("basehead-", true) }?.substringAfter('-')
            "item_flags" -> {
                val flags = section.getStringList("item_flags").toMutableList()
                if (section.getBoolean("hide_attributes", false)) flags += "HIDE_ATTRIBUTES"
                if (section.getBoolean("hide_enchantments", false)) flags += "HIDE_ENCHANTS"
                if (section.getBoolean("hide_unbreakable", false)) flags += "HIDE_UNBREAKABLE"
                if (section.getBoolean("hide_effects", false)) flags += "HIDE_POTION_EFFECTS"
                flags.distinct().takeIf { it.isNotEmpty() }
            }
            "enchantments" -> {
                val result = linkedMapOf<String, Int>()
                section.getStringList("enchantments").forEach { raw ->
                    val parts = raw.split(';', limit = 2)
                    val level = parts.getOrNull(1)?.trim()?.toIntOrNull()
                    if (parts.size == 2 && level != null) result[parts[0].trim().lowercase()] = level
                    else issues += issue(Severity.WARNING, "items.${section.name}.enchantments", "Invalid enchantment '$raw' was skipped.")
                }
                result.takeIf { it.isNotEmpty() }
            }
            else -> null
        }
    }

    /** 为单候选或多候选字段包装条件分支。 */
    private fun conditionValue(condition: String, value: Any): Map<String, Any> = linkedMapOf(
        "condition" to condition,
        "allow" to value
    )

    /** 转换某个按钮在一种点击类型下的动作和点击条件。 */
    private fun buildClickActions(
        candidates: List<Candidate>,
        conditions: List<CandidateCondition>,
        clickType: String,
        issues: MutableList<Issue>
    ): List<Any> {
        val result = mutableListOf<Any>()
        candidates.forEachIndexed { index, candidate ->
            val commandPath = if (clickType == "click") "click_commands" else "${clickType}_commands"
            if (!candidate.section.contains(commandPath)) return@forEachIndexed
            val requirementPath = if (clickType == "click") "click_requirement" else "${clickType}_requirement"
            val translated = collectRequirementActions(candidate.section, requirementPath, "success_commands", issues).toMutableList().apply {
                addAll(
                    translateActions(
                        candidate.section.getList(commandPath).orEmpty(),
                        "items.${candidate.id}.$commandPath",
                        issues
                    )
                )
            }
            if (translated.isEmpty()) return@forEachIndexed

            val clickRequirement = requirementExpression(
                candidate.section,
                requirementPath,
                issues
            )
            val denyPath = "$requirementPath.deny_commands"
            val deny = translateActions(candidate.section.getList(denyPath).orEmpty(), "items.${candidate.id}.$denyPath", issues).toMutableList().apply {
                addAll(collectRequirementActions(candidate.section, requirementPath, "deny_commands", issues))
            }
            val viewCondition = conditions[index].effective
            if (clickRequirement == null) {
                if (viewCondition == "true") {
                    result.addAll(translated)
                } else {
                    result += linkedMapOf<String, Any>("condition" to viewCondition, "allow" to translated)
                }
                return@forEachIndexed
            }

            val clickBranch = linkedMapOf<String, Any>(
                "condition" to clickRequirement,
                "allow" to translated
            )
            if (deny.isNotEmpty()) clickBranch["deny"] = deny
            if (viewCondition == "true") {
                result += clickBranch
            } else {
                result += linkedMapOf<String, Any>(
                    "condition" to viewCondition,
                    "allow" to listOf(clickBranch)
                )
            }
        }
        return result
    }

    /** 将 DM 点击名称映射为 Container 的稳定点击键。 */
    private fun clickKey(clickType: String): String = when (clickType) {
        "click" -> "all"
        "left_click" -> "left"
        "right_click" -> "right"
        "shift_left_click" -> "shift_left"
        "shift_right_click" -> "shift_right"
        "middle_click" -> "middle"
        else -> clickType
    }

    /** 收集 requirement 节点内的 success_commands 或 deny_commands，并转换为 KaMenu 动作。 */
    private fun collectRequirementActions(
        parent: ConfigurationSection,
        path: String,
        actionKey: String,
        issues: MutableList<Issue>
    ): List<String> {
        val requirements = parent.getConfigurationSection("$path.requirements") ?: return emptyList()
        return requirements.getKeys(false).flatMap { id ->
            val actionPath = "$path.requirements.$id.$actionKey"
            val raw = parent.getList(actionPath).orEmpty()
            translateActions(raw, actionPath, issues)
        }
    }

    /** 将 DM 动作转换为 KaMenu 动作；未知动作被过滤并写入迁移报告。 */
    private fun translateActions(
        rawActions: List<*>,
        path: String,
        issues: MutableList<Issue>
    ): List<String> {
        return rawActions.mapNotNull { raw ->
            val text = raw?.toString()?.trim().orEmpty()
            if (text.isEmpty()) return@mapNotNull null
            val match = Regex("^\\[([^]]+)]\\s*(.*)$").matchEntire(text)
            if (match == null) {
                issues += issue(Severity.WARNING, path, "Action '$text' is not a recognized DeluxeMenus action and was skipped.")
                return@mapNotNull null
            }
            val type = match.groupValues[1].trim().lowercase(Locale.ROOT)
            val argument = match.groupValues[2].trim()
            when (type) {
                "message" -> "tell: $argument"
                "player" -> "command: $argument"
                "commandevent" -> {
                    issues += issue(Severity.WARNING, path, "commandevent was approximated as command.")
                    "command: $argument"
                }
                "console" -> "console: $argument"
                "openguimenu" -> "open: $argument"
                "connect" -> "server: $argument"
                "close" -> "close"
                "refresh" -> "refresh: *"
                "sound" -> "sound: $argument"
                "broadcastsound" -> {
                    issues += issue(Severity.WARNING, path, "broadcastsound was approximated as a sound played only to the viewer.")
                    "sound: $argument"
                }
                "takemoney" -> "money: type=take;num=$argument"
                "givemoney" -> "money: type=add;num=$argument"
                "json" -> {
                    issues += issue(Severity.WARNING, path, "JSON action was converted to plain tell text; JSON components need manual review.")
                    "tell: $argument"
                }
                else -> {
                    issues += issue(Severity.WARNING, path, "Unsupported DeluxeMenus action [$type] was skipped.")
                    null
                }
            }
        }
    }

    /** 将 DM requirement group 转换成 KaMenu 条件表达式。 */
    private fun requirementExpression(
        parent: ConfigurationSection,
        path: String,
        issues: MutableList<Issue>
    ): String? {
        val group = parent.getConfigurationSection("$path.requirements") ?: return null
        val requirements = group.getKeys(false).mapNotNull { id ->
            val section = group.getConfigurationSection(id)
            if (section == null) {
                issues += issue(Severity.WARNING, "$path.requirements.$id", "Requirement is not a YAML section.")
                return@mapNotNull null
            }
            val expression = requirementToExpression(section, "$path.requirements.$id", issues) ?: run {
                issues += issue(
                    Severity.WARNING,
                    "$path.requirements.$id",
                    "Requirement could not be converted and will never match."
                )
                "false"
            }
            Requirement(expression, section.getBoolean("optional", false))
        }
        if (requirements.isEmpty()) return null

        val mandatory = requirements.filterNot(Requirement::optional)
        val optional = requirements.filter(Requirement::optional)
        val configuredMinimum = parent.getInt("$path.minimum_requirements", mandatory.size)
        val minimum = configuredMinimum.coerceIn(0, requirements.size)
        if (configuredMinimum != minimum) {
            issues += issue(Severity.WARNING, "$path.minimum_requirements", "minimum_requirements was clamped to $minimum.")
        }

        val mandatoryExpression = mandatory.map(Requirement::expression).joinToString(" && ").ifEmpty { "true" }
        val optionalNeeded = (minimum - mandatory.size).coerceAtLeast(0)
        val optionalExpression = atLeast(optional.map(Requirement::expression), optionalNeeded)
        return andConditions(mandatoryExpression, optionalExpression)
    }

    /** 转换一个 DM requirement 类型；不支持的类型返回 false，避免迁移后意外放行。 */
    private fun requirementToExpression(
        section: ConfigurationSection,
        path: String,
        issues: MutableList<Issue>
    ): String? {
        val type = section.getString("type")?.trim()?.lowercase(Locale.ROOT).orEmpty()
        val permission = section.getString("permission")
        return when (type) {
            "has permission", "has perm", "haspermission", "hasperm", "perm" ->
                permission?.let { "hasPerm.${it.trim()}" }
            "!has permission", "!has perm", "!haspermission", "!hasperm", "!perm", "does not have permission" ->
                permission?.let { "!hasPerm.${it.trim()}" }
            "has money", "hasmoney", "money" ->
                section.getString("amount")?.let { "hasMoney.${it.trim()}" }
            "!has money", "!hasmoney", "!money", "does not have money" ->
                section.getString("amount")?.let { "!hasMoney.${it.trim()}" }
            "has item", "item", "hasitem" -> itemRequirement(section, path, issues, negate = false)
            "!has item", "!item", "!hasitem", "does not have item" -> itemRequirement(section, path, issues, negate = true)
            "string equals", "stringequals", "equals", "string equals ignorecase", "stringequalsignorecase", "equalsignorecase" ->
                compareRequirement(section, "==")
            "!string equals", "!stringequals", "!equals", "!string equals ignorecase", "!stringequalsignorecase", "!equalsignorecase" ->
                compareRequirement(section, "!=")
            ">", "greater than" -> compareRequirement(section, ">")
            ">=", "greater than or equal to" -> compareRequirement(section, ">=")
            "<", "less than" -> compareRequirement(section, "<")
            "<=", "less than or equal to" -> compareRequirement(section, "<=")
            "==", "equal to" -> compareRequirement(section, "==")
            "!=", "not equal to" -> compareRequirement(section, "!=")
            else -> {
                issues += issue(Severity.WARNING, "$path.type", "Unsupported requirement type '$type'; it will never match.")
                null
            }
        }
    }

    /** 转换 DM has item 为 KaMenu hasItem 条件。 */
    private fun itemRequirement(
        section: ConfigurationSection,
        path: String,
        issues: MutableList<Issue>,
        negate: Boolean
    ): String? {
        val material = section.getString("material")?.trim()
        if (material.isNullOrEmpty()) {
            issues += issue(Severity.WARNING, "$path.material", "has item requirement has no material; it will never match.")
            return null
        }
        val params = mutableListOf("mats=$material", "amount=${section.getInt("amount", 1)}")
        section.getString("lore")?.let { params += "lore=$it" }
        if (section.contains("name")) {
            issues += issue(Severity.WARNING, "$path.name", "KaMenu hasItem currently does not compare item names; name was ignored.")
        }
        val expression = "hasItem.[${params.joinToString(";")}]"
        return if (negate) "!$expression" else expression
    }

    /** 转换 DM input/output 比较条件。 */
    private fun compareRequirement(section: ConfigurationSection, operator: String): String? {
        val input = section.getString("input") ?: return null
        val output = section.getString("output") ?: return null
        return "${quote(input)} $operator ${quote(output)}"
    }

    /** 生成“至少满足 N 个条件”的表达式；避免引入 DM 私有 requirement 运行时。 */
    private fun atLeast(expressions: List<String>, minimum: Int): String {
        if (minimum <= 0) return "true"
        if (minimum > expressions.size) return "false"
        if (minimum == expressions.size) return expressions.joinToString(" && ")
        val clauses = combinations(expressions, minimum).map { values ->
            "(${values.joinToString(" && ")})"
        }
        return clauses.joinToString(" || ")
    }

    /** 生成组合列表，迁移器只处理静态配置，组合数量不会进入运行时路径。 */
    private fun combinations(values: List<String>, size: Int): List<List<String>> {
        val result = mutableListOf<List<String>>()
        fun visit(start: Int, current: MutableList<String>) {
            if (current.size == size) {
                result += current.toList()
                return
            }
            for (index in start until values.size) {
                current += values[index]
                visit(index + 1, current)
                current.removeAt(current.lastIndex)
            }
        }
        visit(0, mutableListOf())
        return result
    }

    /** 合并条件并消除恒真项。 */
    private fun andConditions(left: String?, right: String?): String {
        val values = listOf(left, right).filterNot { it.isNullOrBlank() || it == "true" }
        return when (values.size) {
            0 -> "true"
            1 -> values.single()!!
            else -> values.joinToString(" && ") { "($it)" }
        }
    }

    /** 为比较条件引用添加引号并转义。 */
    private fun quote(value: String): String {
        return "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
    }

    /** 创建结构化迁移问题，集中保证报告文本不为空。 */
    private fun issue(severity: Severity, path: String, message: String): Issue =
        Issue(severity, path.ifBlank { "root" }, message.ifBlank { "Unknown migration issue." })
}
