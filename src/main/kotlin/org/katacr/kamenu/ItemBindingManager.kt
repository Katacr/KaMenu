@file:Suppress("DEPRECATION", "UnstableApiUsage")

package org.katacr.kamenu

import org.bukkit.ChatColor
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 统一加载并匹配物品右键菜单绑定。
 *
 * 只读取独立的 `item_bindings.yml`；旧主配置由启动迁移器提前转入此文件。
 */
class ItemBindingManager(private val plugin: KaMenu) {
    private val storageFile = File(plugin.dataFolder, FILE_NAME)

    @Volatile
    private var bindings: List<ItemBinding> = emptyList()
    private val lastUseByPlayer = ConcurrentHashMap<UUID, Long>()

    /** 重新加载统一的独立物品绑定文件。 */
    fun reload(): Int {
        val loaded = mutableListOf<ItemBinding>()
        loadConfiguration().getConfigurationSection(ROOT_KEY)?.let { section ->
            loaded += parseBindings(section, ROOT_KEY)
        }
        bindings = loaded
        return loaded.size
    }

    /** 匹配结果：打开菜单或执行动作包，二者必有其一。 */
    data class ResolvedBinding(
        val menu: String?,
        val actions: List<Any>?
    )

    /** 返回首个匹配手势和物品的绑定；F/Q 冷却中也视为已匹配，避免执行通用动作或丢弃。 */
    internal fun findBinding(player: Player, item: ItemStack, trigger: ItemBindingTrigger, action: Action? = null): ResolvedBinding? {
        val binding = bindings.firstOrNull { candidate ->
            candidate.trigger.matches(trigger, candidate.sneaking == player.isSneaking) &&
                (action == null || candidate.clickTarget.accepts(action)) && candidate.matcher.matches(item)
        } ?: return null
        val now = System.currentTimeMillis()
        val lastUse = lastUseByPlayer[player.uniqueId]
        if (lastUse != null && binding.cooldownMillis > 0L && now - lastUse < binding.cooldownMillis) {
            return if (trigger == ItemBindingTrigger.F || trigger == ItemBindingTrigger.Q
            ) ResolvedBinding(null, null) else null
        }
        if (binding.cooldownMillis > 0L) lastUseByPlayer[player.uniqueId] = now
        return ResolvedBinding(binding.menu, binding.actions)
    }

    /** 兼容原有右键匹配调用；左键及 F/Q 应使用带 trigger 的内部方法。 */
    fun findBinding(player: Player, item: ItemStack, action: Action): ResolvedBinding? {
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return null
        return findBinding(player, item, ItemBindingTrigger.RIGHT_CLICK, action)
    }

    /** 兼容原有只获取菜单的右键接口。 */
    fun findMenu(player: Player, item: ItemStack, action: Action): String? = findBinding(player, item, action)?.menu

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
        rootPath: String
    ): List<ItemBinding> = section.getKeys(false).mapNotNull { id ->
        val path = "$rootPath.$id"
        val binding = section.getConfigurationSection(id) ?: return@mapNotNull invalid(path, "entry must be a section")
        if (!binding.getBoolean("enabled", false)) return@mapNotNull null
        val actions = binding.getList("actions")?.map { it ?: Any() }?.takeIf { it.isNotEmpty() }
            ?: return@mapNotNull invalid(path, "actions is required")
        val item = binding.getConfigurationSection("item") ?: return@mapNotNull invalid(path, "item section is required")
        val gesture = binding.getConfigurationSection("trigger") ?: return@mapNotNull invalid(path, "trigger section is required")
        val material = item.getString("material")?.trim()?.takeIf(String::isNotEmpty)
        val rawLore = item.get("target-lore")
        if (rawLore is Collection<*> && rawLore.isNotEmpty()) {
            return@mapNotNull invalid(path, "target-lore must be a string, empty list, or omitted")
        }
        val rawName = item.get("target-name")
        if (rawName is Collection<*>) return@mapNotNull invalid(path, "target-name must be a string")
        val lore = optionalText(rawLore)
        val name = optionalText(rawName)

        val rawData = item.get("data")
        val data = rawData?.toString()?.toIntOrNull()
        if (rawData != null && data == null) return@mapNotNull invalid(path, "data must be an integer")
        val rawCustomModelData = item.get("custom-model-data")
        val customModelData = rawCustomModelData?.toString()?.toIntOrNull()
        if (rawCustomModelData != null && customModelData == null) {
            return@mapNotNull invalid(path, "custom-model-data must be an integer")
        }
        val rawCooldown = gesture.get("cooldown-ms")
        val cooldownMillis = rawCooldown?.toString()?.toLongOrNull() ?: 500L
        if (rawCooldown != null && (rawCooldown.toString().toLongOrNull() == null || cooldownMillis < 0L)) {
            return@mapNotNull invalid(path, "cooldown-ms must be a non-negative integer")
        }
        val clickTarget = ItemBindingClickTarget.parse(gesture.get("target")?.toString())
            ?: return@mapNotNull invalid(path, "trigger.target must be both, air, or block")
        val trigger = ItemBindingTrigger.parse(gesture.get("type")?.toString())
            ?: return@mapNotNull invalid(path, "trigger.type must be left-click, right-click, f, or q")
        val rawSneaking = gesture.get("sneaking")
        if (rawSneaking != null && rawSneaking !is Boolean) return@mapNotNull invalid(path, "trigger.sneaking must be a boolean")
        if (!trigger.isClick && gesture.contains("target")) {
            return@mapNotNull invalid(path, "trigger.target is only supported for left/right click triggers")
        }
        if (material == null && lore == null && name == null && data == null && customModelData == null) {
            return@mapNotNull invalid(path, "at least one item matcher is required")
        }

        ItemBinding(
            menu = null,
            actions = actions,
            sneaking = rawSneaking ?: false,
            trigger = trigger,
            clickTarget = clickTarget,
            cooldownMillis = cooldownMillis,
            matcher = ItemMatcher(
                material,
                lore,
                name,
                data,
                customModelData,
                item.getBoolean("ignore-case", false),
                item.getBoolean("translate-colors", false)
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
        val menu: String?,
        val actions: List<Any>?,
        val sneaking: Boolean,
        val trigger: ItemBindingTrigger,
        val clickTarget: ItemBindingClickTarget,
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

/** 基础手势；潜行状态单独由 trigger.sneaking 精确筛选。 */
internal enum class ItemBindingTrigger(val isClick: Boolean) {
    LEFT_CLICK(true), RIGHT_CLICK(true), F(false), Q(false);

    fun matches(input: ItemBindingTrigger, sameSneaking: Boolean): Boolean = this == input && sameSneaking

    companion object {
        /** 解析四种基础手势；空值和非法值不触发任何物品绑定。 */
        fun parse(raw: String?): ItemBindingTrigger? = when (raw?.trim()?.lowercase()) {
            "left-click" -> LEFT_CLICK
            "right-click" -> RIGHT_CLICK
            "f" -> F
            "q" -> Q
            else -> null
        }
    }
}

/** 物品点击绑定可接受的目标；不配置时维持兼容的双目标行为。 */
internal enum class ItemBindingClickTarget {
    BOTH, AIR, BLOCK;

    fun accepts(action: Action): Boolean = when (this) {
        BOTH -> action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK ||
            action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK
        AIR -> action == Action.RIGHT_CLICK_AIR || action == Action.LEFT_CLICK_AIR
        BLOCK -> action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK
    }

    companion object {
        /** 将配置值解析为右键目标，未知值应使该绑定失效而非放宽范围。 */
        fun parse(raw: String?): ItemBindingClickTarget? = entries.firstOrNull {
            it.name.equals(raw?.trim() ?: "both", ignoreCase = true)
        }
    }
}
