package org.katacr.kamenu

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.nio.file.Files
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.StandardCopyOption

/** 启动时将旧物品绑定归并到独立文件；失败时保留原始文件，不启用不完整的绑定。 */
internal object ItemBindingFileMigration {
    data class Result(val converted: Int = 0, val backup: List<String> = emptyList())

    private val itemFields = listOf(
        "material", "target-name", "target-lore", "data", "custom-model-data", "ignore-case", "translate-colors"
    )
    private val legacyFields = itemFields.toSet() + setOf(
        "enabled", "menu", "actions", "require-sneaking", "trigger", "click-target", "cooldown-ms",
        "lore", "name", "custom_model_data"
    )

    /** 先备份并保存独立文件，再移除 config.yml 旧节；重复调用不会再次产生绑定。 */
    fun prepare(configFile: File, targetFile: File): Result {
        val config = load(configFile)
        val legacy = config.getConfigurationSection("listeners.item-lore")
        val target = if (targetFile.isFile) load(targetFile) else YamlConfiguration()
        val bindings = target.getConfigurationSection(ItemBindingManager.ROOT_KEY)
        val fromFile = bindings?.getKeys(false).orEmpty()
        val oldFileIds = fromFile.filter { bindings?.getConfigurationSection(it)?.getConfigurationSection("item") == null }
        if (legacy == null && oldFileIds.isEmpty()) return Result()
        if (target.contains(ItemBindingManager.ROOT_KEY) && bindings == null) {
            error("item-bindings must be a section")
        }
        val converted = YamlConfiguration()
        val output = converted.createSection(ItemBindingManager.ROOT_KEY)
        var count = 0
        val old = legacy?.getKeys(false).orEmpty().map { id ->
            id to (legacy?.getConfigurationSection(id) ?: error("Invalid legacy item binding '$id'"))
        }
        // config.yml 原先优先于 item_bindings.yml；按此顺序写入以保留匹配优先级。
        old.forEach { (id, entry) ->
            val name = if (id in fromFile) "legacy_$id" else id
            count += writeConverted(output, name, entry)
        }
        fromFile.forEach { id ->
            val entry = bindings?.getConfigurationSection(id) ?: error("Invalid item binding '$id'")
            if (id in oldFileIds) count += writeConverted(output, id, entry)
            else {
                if (output.contains(id)) {
                    if (output.getConfigurationSection(id)?.let(::leafValues) == leafValues(entry)) return@forEach
                    error("Conflicting item binding '$id'")
                }
                copySection(entry, output.createSection(id))
            }
        }

        val backups = mutableListOf<String>()
        for (file in listOf(configFile.takeIf { legacy != null }, targetFile.takeIf { it.isFile }).filterNotNull()) {
            val backup = File(file.parentFile, "${file.name}.before-item-binding-migration-${System.currentTimeMillis()}.bak")
            Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
            backups += backup.name
        }
        saveAtomically(converted, targetFile)
        if (legacy != null) {
            config.set("listeners.item-lore", null)
            saveAtomically(config, configFile)
        }
        return Result(count, backups)
    }

    /** 将旧平铺条目转换成嵌套结构；原来允许任意潜行状态的右键拆为两条。 */
    private fun writeConverted(output: ConfigurationSection, name: String, entry: ConfigurationSection): Int {
        val unknown = entry.getKeys(false) - legacyFields
        if (unknown.isNotEmpty()) error("Unknown fields in item binding '$name': $unknown")
        val legacyTrigger = entry.getString("trigger")?.trim()?.lowercase()
        val type = when (legacyTrigger) {
            null, "right-click", "sneak-right-click" -> "right-click"
            "left-click", "sneak-left-click" -> "left-click"
            "f", "sneak-f" -> "f"
            "q", "sneak-q" -> "q"
            else -> error("Invalid trigger in item binding '$name': $legacyTrigger")
        }
        val target = entry.getString("click-target") ?: "both"
        if (ItemBindingClickTarget.parse(target) == null || (type == "f" || type == "q") && entry.contains("click-target")) {
            error("Invalid click-target in item binding '$name'")
        }
        val menu = entry.getString("menu")?.trim().orEmpty()
        val actions = if (menu.isNotEmpty()) listOf("open: $menu") else entry.getList("actions")
        if (actions.isNullOrEmpty()) error("Missing menu/actions in item binding '$name'")
        val matcher = linkedMapOf<String, Any>()
        itemFields.forEach { field ->
            val alias = when (field) {
                "target-name" -> "name"
                "target-lore" -> "lore"
                "custom-model-data" -> "custom_model_data"
                else -> field
            }
            (entry.get(field) ?: entry.get(alias))?.let { matcher[field] = it }
        }
        if (matcher.keys.none { it in setOf("material", "target-name", "target-lore", "data", "custom-model-data") }) {
            error("Missing item matcher in item binding '$name'")
        }
        val sneaking = legacyTrigger?.startsWith("sneak-") == true ||
            (legacyTrigger == null && entry.getBoolean("require-sneaking", false))
        val both = legacyTrigger == null && !sneaking
        val states = if (both) listOf(false, true) else listOf(sneaking)
        states.forEach { state ->
            val id = if (state && both) "${name}__sneaking" else name
            if (output.contains(id)) error("Conflicting item binding '$id'")
            val binding = output.createSection(id)
            binding.set("enabled", entry.getBoolean("enabled", false))
            val item = binding.createSection("item")
            matcher.forEach(item::set)
            val gesture = binding.createSection("trigger")
            gesture.set("type", type)
            gesture.set("sneaking", state)
            if (type == "left-click" || type == "right-click") gesture.set("target", target)
            entry.get("cooldown-ms")?.let { gesture.set("cooldown-ms", it) }
            binding.set("actions", actions)
        }
        return states.size
    }

    /** 保留已采用新结构的条目，并避免 YAML section 被当作字符串写入。 */
    private fun copySection(source: ConfigurationSection, target: ConfigurationSection) {
        source.getKeys(false).forEach { key ->
            val child = source.getConfigurationSection(key)
            if (child != null) copySection(child, target.createSection(key)) else target.set(key, source.get(key))
        }
    }

    /** 忽略 YAML 配置节对象的身份，仅比较叶子字段，供中断后的幂等重试。 */
    private fun leafValues(section: ConfigurationSection): Map<String, Any?> = section.getKeys(true)
        .filterNot(section::isConfigurationSection)
        .associateWith(section::get)

    /** 严格解析 YAML；静态 loadConfiguration 会吞掉语法异常并产生空配置。 */
    private fun load(file: File): YamlConfiguration = YamlConfiguration().apply { load(file) }

    /** 临时文件完整写入后原子替换，防止中途失败留下半份配置。 */
    private fun saveAtomically(config: YamlConfiguration, target: File) {
        target.parentFile?.mkdirs()
        val temp = Files.createTempFile(target.parentFile.toPath(), ".${target.name}.", ".tmp").toFile()
        try {
            config.save(temp)
            if (target.exists()) runCatching {
                Files.setPosixFilePermissions(temp.toPath(), Files.getPosixFilePermissions(target.toPath()))
            }
            try {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            if (temp.exists()) temp.delete()
        }
    }
}
