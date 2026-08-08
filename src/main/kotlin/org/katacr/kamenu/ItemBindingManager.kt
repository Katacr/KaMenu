@file:Suppress("DEPRECATION", "UnstableApiUsage")

package org.katacr.kamenu

import org.bukkit.ChatColor
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 统一加载并匹配物品右键菜单绑定。
 *
 * 兼容 `config.yml > listeners.item-lore`，并额外读取独立的 `item_bindings.yml`，
 * 使迁移器可以持久化第三方菜单绑定而不重写用户主配置。
 */
class ItemBindingManager(private val plugin: KaMenu) {
    private val storageFile = File(plugin.dataFolder, FILE_NAME)

    @Volatile
    private var bindings: List<ItemBinding> = emptyList()
    private val lastUseByPlayer = ConcurrentHashMap<UUID, Long>()

    /** 重新加载主配置旧格式和独立绑定文件。 */
    fun reload(): Int {
        val loaded = mutableListOf<ItemBinding>()
        plugin.config.getConfigurationSection("listeners.item-lore")?.let { section ->
            loaded += parseBindings(section, "listeners.item-lore", requireMaterial = true)
        }
        loadConfiguration().getConfigurationSection(ROOT_KEY)?.let { section ->
            loaded += parseBindings(section, ROOT_KEY, requireMaterial = false)
        }
        bindings = loaded
        return loaded.size
    }

    /** 返回首个匹配当前手持物品和潜行状态的目标菜单。 */
    fun findMenu(player: Player, item: ItemStack): String? {
        val binding = bindings.firstOrNull { candidate ->
            (!candidate.requireSneaking || player.isSneaking) && candidate.matcher.matches(item)
        } ?: return null
        val now = System.currentTimeMillis()
        val lastUse = lastUseByPlayer[player.uniqueId]
        if (lastUse != null && binding.cooldownMillis > 0L && now - lastUse < binding.cooldownMillis) return null
        if (binding.cooldownMillis > 0L) lastUseByPlayer[player.uniqueId] = now
        return binding.menu
    }

    /** 清理离线玩家的物品绑定冷却状态。 */
    fun clear(playerId: UUID) {
        lastUseByPlayer.remove(playerId)
    }

    /** 读取独立绑定配置；文件不存在时返回空配置。 */
    fun loadConfiguration(): YamlConfiguration = if (storageFile.isFile) {
        YamlConfiguration.loadConfiguration(storageFile)
    } else {
        YamlConfiguration()
    }

    /** 保存独立绑定配置，并创建尚不存在的插件目录。 */
    fun saveConfiguration(configuration: YamlConfiguration) {
        storageFile.parentFile?.mkdirs()
        configuration.save(storageFile)
    }

    private fun parseBindings(
        section: ConfigurationSection,
        rootPath: String,
        requireMaterial: Boolean
    ): List<ItemBinding> = section.getKeys(false).mapNotNull { id ->
        val path = "$rootPath.$id"
        val binding = section.getConfigurationSection(id) ?: return@mapNotNull invalid(path, "entry must be a section")
        if (!binding.getBoolean("enabled", false)) return@mapNotNull null
        val menu = binding.getString("menu")?.trim().orEmpty()
        if (menu.isEmpty()) return@mapNotNull invalid(path, "menu is required")

        val material = binding.getString("material")?.trim()?.takeIf(String::isNotEmpty)
        if (requireMaterial && material == null) return@mapNotNull invalid(path, "material is required")
        val rawLore = binding.get("target-lore") ?: binding.get("lore")
        if (rawLore is Collection<*> && rawLore.isNotEmpty()) {
            return@mapNotNull invalid(path, "target-lore must be a string, empty list, or omitted")
        }
        val rawName = binding.get("target-name") ?: binding.get("name")
        if (rawName is Collection<*>) return@mapNotNull invalid(path, "target-name must be a string")
        val lore = optionalText(rawLore)
        val name = optionalText(rawName)

        val rawData = binding.get("data")
        val data = rawData?.toString()?.toIntOrNull()
        if (rawData != null && data == null) return@mapNotNull invalid(path, "data must be an integer")
        val rawCustomModelData = binding.get("custom-model-data") ?: binding.get("custom_model_data")
        val customModelData = rawCustomModelData?.toString()?.toIntOrNull()
        if (rawCustomModelData != null && customModelData == null) {
            return@mapNotNull invalid(path, "custom-model-data must be an integer")
        }
        val rawCooldown = binding.get("cooldown-ms")
        val cooldownMillis = rawCooldown?.toString()?.toLongOrNull() ?: 0L
        if (rawCooldown != null && (rawCooldown.toString().toLongOrNull() == null || cooldownMillis < 0L)) {
            return@mapNotNull invalid(path, "cooldown-ms must be a non-negative integer")
        }
        if (material == null && lore == null && name == null && data == null && customModelData == null) {
            return@mapNotNull invalid(path, "at least one item matcher is required")
        }

        ItemBinding(
            menu = menu,
            requireSneaking = binding.getBoolean("require-sneaking", false),
            cooldownMillis = cooldownMillis,
            matcher = ItemMatcher(
                material,
                lore,
                name,
                data,
                customModelData,
                binding.getBoolean("ignore-case", false),
                binding.getBoolean("translate-colors", false)
            )
        )
    }

    private fun optionalText(raw: Any?): String? = when (raw) {
        null -> null
        is String -> raw.takeUnless(String::isBlank)
        is Collection<*> -> null
        else -> raw.toString().takeUnless(String::isBlank)
    }

    private fun invalid(path: String, reason: String): Nothing? {
        plugin.logger.warning("[KaMenu] Invalid item binding '$path': $reason")
        return null
    }

    private data class ItemBinding(
        val menu: String,
        val requireSneaking: Boolean,
        val cooldownMillis: Long,
        val matcher: ItemMatcher
    )

    private data class ItemMatcher(
        val material: String?,
        val lore: String?,
        val name: String?,
        val data: Int?,
        val customModelData: Int?,
        val ignoreCase: Boolean,
        val translateColors: Boolean
    ) {
        fun matches(item: ItemStack): Boolean {
            if (material != null && !isMaterialMatch(item, material)) return false
            if (data != null && item.durability.toInt() != data) return false
            val meta = item.itemMeta
            if (customModelData != null && ItemPropertyReader.getCustomModelId(meta) != customModelData) return false
            if (name != null) {
                val displayName = meta?.takeIf { it.hasDisplayName() }?.displayName ?: return false
                if (!displayName.contains(format(name), ignoreCase = ignoreCase)) return false
            }
            if (lore != null) {
                val target = format(lore)
                if (meta?.lore.orEmpty().none { line -> line.contains(target, ignoreCase = ignoreCase) }) return false
            }
            return true
        }

        private fun isMaterialMatch(item: ItemStack, target: String): Boolean {
            if (ExternalItemAdapter.matches(item, target)) return true
            return item.type.name.equals(MaterialUtils.normalizeMaterialName(target), ignoreCase = true)
        }

        private fun format(text: String): String = if (translateColors) {
            ChatColor.translateAlternateColorCodes('&', text)
        } else {
            text
        }
    }

    companion object {
        const val FILE_NAME = "item_bindings.yml"
        const val ROOT_KEY = "item-bindings"
    }
}
