@file:Suppress("DEPRECATION", "UnstableApiUsage")

package org.katacr.kamenu

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.net.URL
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 菜单展示物品的解析结果。
 *
 * 字段在进入此模型前已经完成玩家变量和条件值解析，因此工厂只负责 Bukkit ItemStack 构建。
 */
data class MenuItemSpec(
    val source: String,
    val amount: Int = 1,
    val name: String? = null,
    val lore: List<String>? = null,
    val customModelData: String? = null,
    val itemModel: String? = null,
    val skullOwner: String? = null,
    val skullTexture: String? = null,
    val glow: Boolean = false,
    val unbreakable: Boolean? = null,
    val enchantments: Map<String, Int> = emptyMap(),
    val itemFlags: Set<String> = emptySet()
)

/**
 * Dialog 与 Container 共用的 Bukkit 展示物品工厂。
 *
 * 支持原版材质、保存物品 `stock:*`、外部物品 ID、玩家装备槽和背包槽位引用。
 * 所有返回值均为克隆或新建物品，不会修改玩家背包和保存物品缓存中的原对象。
 */
class MenuItemFactory(private val plugin: KaMenu) {
    private val reportedWarnings = ConcurrentHashMap.newKeySet<String>()

    /** 根据已解析物品规格创建独立 ItemStack。 */
    fun create(
        player: Player,
        spec: MenuItemSpec,
        contextId: String,
        componentId: String,
        applyOverridesToSlotSource: Boolean = true
    ): ItemStack {
        val item = resolveSource(player, spec.source, spec.amount, contextId, componentId)
        if (applyOverridesToSlotSource || !isSlotReference(spec.source)) {
            applyMeta(player, item, spec, contextId, componentId)
        }
        return item
    }

    /** 判断字符串是否为 `[HEAD:Player]`、`[MAINHAND]` 等装备槽位引用。 */
    fun isSlotReference(source: String): Boolean {
        val normalized = source.trim()
        return normalized.startsWith('[') && normalized.endsWith(']')
    }

    /** 解析物品来源，并在无效来源时回退为 PAPER。 */
    private fun resolveSource(
        player: Player,
        source: String,
        defaultAmount: Int,
        contextId: String,
        componentId: String
    ): ItemStack {
        val normalized = source.trim()
        val slot = normalized.substringAfter("slot:", missingDelimiterValue = "").trim().toIntOrNull()
        val inventoryItem = resolveBracketSlot(player, normalized, contextId, componentId) ?: when {
            normalized.equals("hand", true) || normalized.equals("mainhand", true) ||
                normalized.equals("main_hand", true) -> player.inventory.itemInMainHand

            normalized.equals("offhand", true) || normalized.equals("off_hand", true) ->
                player.inventory.itemInOffHand

            slot != null && slot in 0 until player.inventory.size -> player.inventory.getItem(slot)
            else -> null
        }
        if (inventoryItem != null && inventoryItem.type != Material.AIR && inventoryItem.amount > 0) {
            return inventoryItem.clone()
        }

        if (normalized.startsWith("stock:", ignoreCase = true)) {
            val itemName = normalized.substringAfter(':').trim()
            plugin.itemManager.getItem(itemName)?.let { savedItem ->
                savedItem.amount = clampAmount(defaultAmount, savedItem.maxStackSize)
                return savedItem
            }
        }

        ExternalItemAdapter.create(normalized, defaultAmount, player)?.let { externalItem ->
            externalItem.amount = clampAmount(defaultAmount, externalItem.maxStackSize)
            return externalItem
        }

        val material = MaterialUtils.matchMaterial(normalized)
        if (material == null) {
            warn("item_renderer.invalid_material", contextId, componentId, normalized)
        }
        val item = ItemStack(material ?: Material.PAPER)
        item.amount = clampAmount(defaultAmount, item.maxStackSize)
        return item
    }

    /** 解析 `[HEAD:Player]`、`[MAINHAND]` 等玩家装备槽位写法。 */
    private fun resolveBracketSlot(
        player: Player,
        source: String,
        contextId: String,
        componentId: String
    ): ItemStack? {
        if (!isSlotReference(source)) return null
        val normalized = source.trim()
        val parts = normalized.substring(1, normalized.length - 1).split(':', limit = 2)
        val targetName = parts.getOrNull(1)?.trim()?.takeIf(String::isNotEmpty)
        val target = targetName?.let(Bukkit::getPlayer) ?: if (targetName == null) player else null
        if (target == null || !target.isOnline) {
            warn("item_renderer.invalid_item_slot", contextId, componentId, targetName ?: "")
            return ItemStack(Material.PAPER)
        }

        val slotName = parts[0].trim().uppercase()
        val slotItem = when (slotName) {
            "HEAD" -> target.inventory.helmet
            "CHEST" -> target.inventory.chestplate
            "LEGGINGS" -> target.inventory.leggings
            "BOOTS" -> target.inventory.boots
            "MAINHAND" -> target.inventory.itemInMainHand
            "OFFHAND" -> target.inventory.itemInOffHand
            else -> {
                warn("item_renderer.invalid_item_slot", contextId, componentId, slotName)
                return ItemStack(Material.PAPER)
            }
        }
        if (slotItem != null && slotItem.type != Material.AIR && slotItem.amount > 0) {
            return slotItem.clone()
        }
        if (slotName == "HEAD") {
            return ItemStack(Material.PLAYER_HEAD).apply {
                val skullMeta = itemMeta as SkullMeta
                skullMeta.owningPlayer = target
                itemMeta = skullMeta
            }
        }
        return ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
    }

    /** 将名称、Lore、模型、头颅、附魔和物品标志应用到展示物品。 */
    private fun applyMeta(
        player: Player,
        item: ItemStack,
        spec: MenuItemSpec,
        contextId: String,
        componentId: String
    ) {
        val meta = item.itemMeta ?: return
        spec.name?.let { meta.setDisplayName(toLegacy(player, it)) }
        spec.lore?.let { lore -> meta.lore = lore.map { toLegacy(player, it) } }

        spec.customModelData?.takeIf(String::isNotBlank)?.let { raw ->
            raw.toIntOrNull()?.let(meta::setCustomModelData)
                ?: warn("item_renderer.invalid_item_property", contextId, componentId, "custom_model_data", raw)
        }
        spec.itemModel?.takeIf(String::isNotBlank)?.let { raw ->
            val key = if (':' in raw) NamespacedKey.fromString(raw) else NamespacedKey.minecraft(raw)
            if (key != null) {
                if (!BukkitItemMetaCompat.setItemModel(meta, key)) {
                    warn("item_renderer.invalid_item_property", contextId, componentId, "item_model", raw)
                }
            } else {
                warn("item_renderer.invalid_item_property", contextId, componentId, "item_model", raw)
            }
        }

        if (meta is SkullMeta) {
            when {
                !spec.skullTexture.isNullOrBlank() ->
                    applySkullTexture(meta, spec.skullTexture, contextId, componentId)

                !spec.skullOwner.isNullOrBlank() -> meta.owningPlayer = Bukkit.getOfflinePlayer(spec.skullOwner)
            }
        }

        spec.unbreakable?.let { meta.isUnbreakable = it }
        applyEnchantments(meta, spec.enchantments, contextId, componentId)
        applyItemFlags(meta, spec.itemFlags, contextId, componentId)
        if (spec.glow && meta.enchants.isEmpty()) {
            Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"))?.let { enchantment ->
                meta.addEnchant(enchantment, 1, true)
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            }
        }
        item.itemMeta = meta
    }

    /** 解析并应用配置中的附魔键与等级。 */
    private fun applyEnchantments(
        meta: org.bukkit.inventory.meta.ItemMeta,
        enchantments: Map<String, Int>,
        contextId: String,
        componentId: String
    ) {
        enchantments.forEach { (rawKey, level) ->
            val key = if (':' in rawKey) NamespacedKey.fromString(rawKey) else NamespacedKey.minecraft(rawKey.lowercase())
            val enchantment = key?.let(Enchantment::getByKey)
            if (enchantment == null || level <= 0) {
                warn("item_renderer.invalid_item_property", contextId, componentId, "enchantments", "$rawKey:$level")
            } else {
                meta.addEnchant(enchantment, level, true)
            }
        }
    }

    /** 解析并应用 Bukkit ItemFlag 名称。 */
    private fun applyItemFlags(
        meta: org.bukkit.inventory.meta.ItemMeta,
        flags: Set<String>,
        contextId: String,
        componentId: String
    ) {
        flags.forEach { raw ->
            val flag = runCatching { ItemFlag.valueOf(raw.trim().uppercase()) }.getOrNull()
            if (flag == null) {
                warn("item_renderer.invalid_item_property", contextId, componentId, "item_flags", raw)
            } else {
                meta.addItemFlags(flag)
            }
        }
    }

    /** 将 Base64 头颅纹理转换为 Bukkit PlayerProfile。 */
    private fun applySkullTexture(meta: SkullMeta, texture: String, contextId: String, componentId: String) {
        val skinUrl = runCatching {
            val decoded = String(Base64.getDecoder().decode(texture), Charsets.UTF_8)
            val rawUrl = Regex("\\\"url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
                .find(decoded)?.groupValues?.get(1)
                ?: error("texture URL is missing")
            URL(rawUrl.replace("\\\\/", "/"))
        }.getOrElse {
            warn("item_renderer.invalid_item_property", contextId, componentId, "skull_texture", "invalid Base64 texture")
            return
        }
        if (!BukkitItemMetaCompat.setSkullTexture(meta, skinUrl, UUID.nameUUIDFromBytes(texture.toByteArray()))) {
            warn("item_renderer.invalid_item_property", contextId, componentId, "skull_texture", "current core has no public profile API")
        }
    }

    /** 将 KaMenu 文本格式转换为 Bukkit ItemMeta 使用的 Legacy 文本。 */
    private fun toLegacy(player: Player, text: String): String {
        return LegacyComponentSerializer.legacySection().serialize(TextParser.parseText(text, player))
    }

    /** 将物品数量约束到该物品允许的堆叠范围。 */
    private fun clampAmount(amount: Int, maxStackSize: Int): Int {
        return amount.coerceAtLeast(1).coerceAtMost(maxStackSize.coerceAtLeast(1))
    }

    /** 输出本地化的物品配置警告。 */
    private fun warn(key: String, vararg args: Any) {
        val warningId = "$key:${args.joinToString("|")}"
        if (reportedWarnings.add(warningId)) {
            plugin.logger.warning(plugin.languageManager.getMessage(key, *args))
        }
    }
}
