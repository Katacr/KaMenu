@file:Suppress("UnstableApiUsage", "DEPRECATION")

package org.katacr.kamenu

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 物品属性变量查询服务。
 *
 * 同时服务于菜单内置变量 `{checkitem:[...]}` 和外部 PAPI `%kamenu_checkitem_[...]%`，
 * 并确保 `stock:` 只读取内存缓存、玩家背包只在 Paper/Folia 安全线程读取。
 */
class ItemPlaceholderService(private val itemManager: ItemManager) {
    private val plainSerializer = PlainTextComponentSerializer.plainText()
    private val legacySerializer = LegacyComponentSerializer.legacyAmpersand()
    private val miniSerializer = MiniMessage.miniMessage()

    /**
     * 解析查询并返回稳定的字符串结果。
     *
     * @param player 当前在线玩家；`stock:` 来源不要求玩家存在
     * @param rawQuery 方括号查询，例如 `[hand;name;fmt:plain]`
     */
    fun resolve(player: Player?, rawQuery: String): String {
        val query = parseQuery(rawQuery) ?: return ""
        return resolveProperty(resolveSource(player, query.source), query)
    }

    /** 将方括号短语法解析为不可变查询对象。 */
    private fun parseQuery(rawQuery: String): ItemQuery? {
        val trimmed = rawQuery.trim()
        if (trimmed.length !in 5..MAX_QUERY_LENGTH || !trimmed.startsWith('[') || !trimmed.endsWith(']')) {
            return null
        }

        val parts = splitSemicolonArguments(trimmed.substring(1, trimmed.length - 1)) ?: return null
        if (parts.size !in 2..3 || parts[0].isBlank() || parts[1].isBlank()) return null

        val format = if (parts.size == 3) {
            val formatOption = parts[2]
            if (!formatOption.startsWith("fmt:", ignoreCase = true)) return null
            TextFormat.fromId(formatOption.substringAfter(':').trim()) ?: return null
        } else {
            TextFormat.PLAIN
        }
        return ItemQuery(parts[0].trim(), parts[1].trim(), format)
    }

    /** 按分号拆分参数，同时保留反引号内部的分号和空格。 */
    private fun splitSemicolonArguments(raw: String): List<String>? {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inBackticks = false
        var escaping = false

        fun flush() {
            result.add(current.toString().trim())
            current.clear()
        }

        raw.forEach { char ->
            if (escaping) {
                current.append(char)
                escaping = false
            } else if (char == '\\') {
                escaping = true
            } else if (inBackticks) {
                if (char == '`') inBackticks = false else current.append(char)
            } else {
                when (char) {
                    '`' -> inBackticks = true
                    ';' -> flush()
                    else -> current.append(char)
                }
            }
        }

        if (inBackticks) return null
        if (escaping) current.append('\\')
        flush()
        return result
    }

    /** 从玩家背包或保存物品缓存取得物品克隆。 */
    private fun resolveSource(player: Player?, rawSource: String): ItemStack? {
        val source = rawSource.trim()
        val sourceLower = source.lowercase()
        if (sourceLower.startsWith("stock:")) {
            val stockName = source.substringAfter(':').trim()
            return stockName.takeIf(String::isNotEmpty)?.let(itemManager::getItem)
        }

        if (player == null || !canReadInventory(player)) return null

        val item = when {
            sourceLower == "hand" -> player.inventory.itemInMainHand
            sourceLower == "offhand" -> player.inventory.itemInOffHand
            sourceLower.startsWith("slot:") -> {
                val slot = source.substringAfter(':').trim().toIntOrNull()
                if (slot != null && slot in 0 until player.inventory.size) player.inventory.getItem(slot) else null
            }
            else -> null
        }
        return item?.takeUnless { it.type == Material.AIR || it.isEmpty }?.clone()
    }

    /** 判断当前线程是否允许读取该玩家的背包。 */
    private fun canReadInventory(player: Player): Boolean {
        if (!player.isOnline) return false
        return if (KaScheduler.folia) Bukkit.isOwnedByCurrentRegion(player) else Bukkit.isPrimaryThread()
    }

    /** 读取目标属性；物品或属性不存在时返回对应类型的空值。 */
    private fun resolveProperty(item: ItemStack?, query: ItemQuery): String {
        val property = query.property.lowercase()
        if (item == null) return emptyValue(property)

        val meta = item.itemMeta
        return when {
            property == "type" -> item.type.key.toString()
            property == "amt" -> item.amount.toString()
            property == "name" -> serializeText(item.effectiveName(), query.format)
            property == "lore" -> serializeLore(meta.lore().orEmpty(), query.format)
            property.startsWith("lore:") -> {
                val lineNumber = property.substringAfter(':').toIntOrNull()
                val index = lineNumber?.takeIf { it >= 1 }?.minus(1) ?: -1
                meta.lore()?.getOrNull(index)?.let { serializeText(it, query.format) }.orEmpty()
            }
            property == "enchants" -> serializeEnchantments(meta.enchants)
            property.startsWith("ench:") -> findEnchantmentLevel(meta.enchants, query.property.substringAfter(':')).toString()
            property == "model" || property == "item_model" -> ItemPropertyReader.getItemModel(meta).orEmpty()
            property == "cmd" || property == "custom_model_data" || property == "custom_model_id" ->
                ItemPropertyReader.getCustomModelId(meta)?.toString().orEmpty()
            property == "dmg" -> damageValues(item).damage.toString()
            property == "dura" -> damageValues(item).remaining.toString()
            property == "dura_pct" -> damageValues(item).remainingPercent
            else -> ""
        }
    }

    /** 根据属性类型返回一致的缺失值。 */
    private fun emptyValue(property: String): String {
        return when {
            property == "lore" || property == "enchants" -> "[]"
            property == "amt" || property == "dmg" || property == "dura" ||
                property == "dura_pct" || property.startsWith("ench:") -> "0"
            else -> ""
        }
    }

    /** 把 Adventure 文本转换为 plain、legacy 或 MiniMessage 字符串。 */
    private fun serializeText(component: Component, format: TextFormat): String {
        return when (format) {
            TextFormat.PLAIN -> plainSerializer.serialize(component)
            TextFormat.LEGACY -> legacySerializer.serialize(component)
            TextFormat.MINI -> miniSerializer.serialize(component)
        }
    }

    /** 将 Lore 转为 JSON 字符串数组，保证引号和换行被正确转义。 */
    private fun serializeLore(lore: List<Component>, format: TextFormat): String {
        return JsonArray().apply { lore.forEach { add(serializeText(it, format)) } }.toString()
    }

    /** 将附魔表转为按 NamespacedKey 排序的 JSON 对象数组。 */
    private fun serializeEnchantments(enchantments: Map<Enchantment, Int>): String {
        return JsonArray().apply {
            enchantments.entries.sortedBy { it.key.key.toString() }.forEach { (enchantment, level) ->
                add(JsonObject().apply {
                    addProperty("key", enchantment.key.toString())
                    addProperty("level", level)
                })
            }
        }.toString()
    }

    /** 按完整附魔键查询等级；原版附魔允许省略 `minecraft:`。 */
    private fun findEnchantmentLevel(enchantments: Map<Enchantment, Int>, rawKey: String): Int {
        val keyText = rawKey.trim().let { if (':' in it) it else "minecraft:$it" }
        val key = NamespacedKey.fromString(keyText.lowercase()) ?: return 0
        return enchantments.entries.firstOrNull { it.key.key == key }?.value ?: 0
    }

    /** 计算已损耗耐久、剩余耐久及剩余百分比。 */
    private fun damageValues(item: ItemStack): DamageValues {
        val damageable = item.itemMeta as? Damageable ?: return DamageValues.EMPTY
        val maxDamage = if (damageable.hasMaxDamage()) damageable.maxDamage else item.type.maxDurability.toInt()
        if (maxDamage <= 0) return DamageValues.EMPTY

        val damage = damageable.damage.coerceAtLeast(0)
        val remaining = (maxDamage - damage).coerceAtLeast(0)
        val percentage = BigDecimal.valueOf(remaining.toLong())
            .multiply(BigDecimal.valueOf(100L))
            .divide(BigDecimal.valueOf(maxDamage.toLong()), 2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
        return DamageValues(damage, remaining, percentage)
    }

    /** 解析后的物品查询。 */
    private data class ItemQuery(val source: String, val property: String, val format: TextFormat)

    /** 支持的 Adventure 文本输出格式。 */
    private enum class TextFormat {
        PLAIN,
        LEGACY,
        MINI;

        companion object {
            /** 将配置短名称转换为文本格式。 */
            fun fromId(id: String): TextFormat? = entries.firstOrNull { it.name.equals(id, ignoreCase = true) }
        }
    }

    /** 物品耐久查询的统一结果。 */
    private data class DamageValues(val damage: Int, val remaining: Int, val remainingPercent: String) {
        companion object {
            val EMPTY = DamageValues(0, 0, "0")
        }
    }

    private companion object {
        const val MAX_QUERY_LENGTH = 512
    }
}
