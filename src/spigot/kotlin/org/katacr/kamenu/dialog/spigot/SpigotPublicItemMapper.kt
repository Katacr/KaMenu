package org.katacr.kamenu.dialog.spigot

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.chat.ComponentSerializer
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.profile.PlayerTextures
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Locale

/**
 * 仅使用 Bukkit/Spigot 公共 API 将 ItemStack 的可见属性映射为 Dialog item JSON。
 *
 * 映射器不读取 NMS、PDC 的私有值或未知数据组件；未公开属性会被安全忽略。
 */
internal class SpigotPublicItemMapper {
    /** 将 Bukkit ItemStack 转换为原版 Dialog item body 接受的结构化 JSON。 */
    fun map(item: ItemStack): JsonObject {
        val result = JsonObject().apply {
            addProperty("id", item.type.key.toString())
            if (item.amount != 1) {
                addProperty("count", item.amount)
            }
        }
        val meta = item.itemMeta ?: return result
        val components = JsonObject()
        addTextComponents(meta, components)
        addModelComponents(meta, components)
        addEnchantments("minecraft:enchantments", meta.enchants, components)
        if (meta is EnchantmentStorageMeta) {
            addEnchantments("minecraft:stored_enchantments", meta.storedEnchants, components)
        }
        addDurabilityComponents(meta, components)
        addDisplayComponents(meta, components)
        addTooltipDisplay(meta, components)
        addLeatherColor(meta, components)
        addProfile(meta, components)
        if (components.size() > 0) {
            result.add("components", components)
        }
        return result
    }

    /** 映射自定义名称、固定物品名和 Lore。 */
    private fun addTextComponents(meta: ItemMeta, components: JsonObject) {
        if (meta.hasDisplayName()) {
            components.add("minecraft:custom_name", legacyText(meta.displayName))
        }
        if (meta.hasItemName()) {
            components.add("minecraft:item_name", legacyText(meta.itemName))
        }
        meta.lore?.takeIf { meta.hasLore() }?.let { lines ->
            val lore = JsonArray()
            lines.forEach { lore.add(legacyText(it)) }
            components.add("minecraft:lore", lore)
        }
    }

    /** 映射旧整数和新版多字段自定义模型数据，以及 item_model。 */
    private fun addModelComponents(meta: ItemMeta, components: JsonObject) {
        if (meta.hasCustomModelDataComponent() || meta.hasCustomModelData()) {
            val modelData = meta.customModelDataComponent
            val value = JsonObject()
            addNumbers(value, "floats", modelData.floats)
            addBooleans(value, "flags", modelData.flags)
            addStrings(value, "strings", modelData.strings)
            if (modelData.colors.isNotEmpty()) {
                val colors = JsonArray()
                modelData.colors.forEach { colors.add(it.asRGB()) }
                value.add("colors", colors)
            }
            if (value.size() > 0) {
                components.add("minecraft:custom_model_data", value)
            }
        }
        meta.itemModel?.takeIf { meta.hasItemModel() }?.let {
            components.addProperty("minecraft:item_model", it.toString())
        }
    }

    /** 映射普通附魔或附魔书的储存附魔。 */
    private fun addEnchantments(
        componentKey: String,
        enchantments: Map<Enchantment, Int>,
        components: JsonObject
    ) {
        if (enchantments.isEmpty()) return
        val value = JsonObject()
        enchantments.forEach { (enchantment, level) ->
            value.addProperty(enchantment.key.toString(), level)
        }
        components.add(componentKey, value)
    }

    /** 映射耐久、最大耐久与不可破坏属性。 */
    private fun addDurabilityComponents(meta: ItemMeta, components: JsonObject) {
        if (meta is Damageable) {
            if (meta.hasDamage()) {
                components.addProperty("minecraft:damage", meta.damage)
            }
            if (meta.hasMaxDamage()) {
                components.addProperty("minecraft:max_damage", meta.maxDamage)
            }
        }
        if (meta.isUnbreakable) {
            components.add("minecraft:unbreakable", JsonObject())
        }
    }

    /** 映射影响图标或 Tooltip 的通用显示属性。 */
    private fun addDisplayComponents(meta: ItemMeta, components: JsonObject) {
        if (meta.hasEnchantmentGlintOverride()) {
            components.addProperty("minecraft:enchantment_glint_override", meta.enchantmentGlintOverride)
        }
        meta.tooltipStyle?.takeIf { meta.hasTooltipStyle() }?.let {
            components.addProperty("minecraft:tooltip_style", it.toString())
        }
        if (meta.hasMaxStackSize()) {
            components.addProperty("minecraft:max_stack_size", meta.maxStackSize)
        }
        if (meta.hasRarity()) {
            components.addProperty("minecraft:rarity", meta.rarity.name.lowercase(Locale.ROOT))
        }
    }

    /** 将 Bukkit ItemFlag 转换为新版 tooltip_display 隐藏组件集合。 */
    private fun addTooltipDisplay(meta: ItemMeta, components: JsonObject) {
        val hiddenComponents = linkedSetOf<String>()
        meta.itemFlags.mapNotNullTo(hiddenComponents, ::hiddenComponent)
        if (!meta.isHideTooltip && hiddenComponents.isEmpty()) return

        val tooltip = JsonObject()
        if (meta.isHideTooltip) {
            tooltip.addProperty("hide_tooltip", true)
        }
        if (hiddenComponents.isNotEmpty()) {
            val hidden = JsonArray()
            hiddenComponents.forEach(hidden::add)
            tooltip.add("hidden_components", hidden)
        }
        components.add("minecraft:tooltip_display", tooltip)
    }

    /** 兼容 Spigot 中 getComponent() 为空的旧 ItemFlag 别名。 */
    private fun hiddenComponent(flag: ItemFlag): String? {
        flag.component?.let { return it.toString() }
        return when (flag.name) {
            "HIDE_ENCHANTS" -> "minecraft:enchantments"
            "HIDE_ATTRIBUTES" -> "minecraft:attribute_modifiers"
            "HIDE_UNBREAKABLE" -> "minecraft:unbreakable"
            "HIDE_DESTROYS" -> "minecraft:can_break"
            "HIDE_PLACED_ON" -> "minecraft:can_place_on"
            "HIDE_DYE" -> "minecraft:dyed_color"
            "HIDE_ARMOR_TRIM" -> "minecraft:trim"
            else -> null
        }
    }

    /** 映射皮革护甲染色。 */
    private fun addLeatherColor(meta: ItemMeta, components: JsonObject) {
        if (meta is LeatherArmorMeta) {
            components.addProperty("minecraft:dyed_color", meta.color.asRGB())
        }
    }

    /** 使用 Bukkit PlayerProfile 生成玩家头颅所需的公开 profile 数据。 */
    private fun addProfile(meta: ItemMeta, components: JsonObject) {
        if (meta !is SkullMeta) return
        val profile = meta.ownerProfile ?: return
        val value = JsonObject()
        profile.name?.takeIf(String::isNotBlank)?.let { value.addProperty("name", it) }
        profile.uniqueId?.let { value.add("id", uuid(it.mostSignificantBits, it.leastSignificantBits)) }
        val properties = textureProperties(profile.textures)
        if (properties.size() > 0) {
            value.add("properties", properties)
        }
        if (value.size() > 0) {
            components.add("minecraft:profile", value)
        }
    }

    /** 将公开皮肤和披风 URL 组合为无需签名的标准 textures 属性。 */
    private fun textureProperties(textures: PlayerTextures): JsonArray {
        val properties = JsonArray()
        val skin = textures.skin
        val cape = textures.cape
        if (skin == null && cape == null) return properties

        val textureRoot = JsonObject()
        val textureValues = JsonObject()
        skin?.let {
            val skinValue = JsonObject().apply {
                addProperty("url", it.toString())
                if (textures.skinModel == PlayerTextures.SkinModel.SLIM) {
                    add("metadata", JsonObject().apply { addProperty("model", "slim") })
                }
            }
            textureValues.add("SKIN", skinValue)
        }
        cape?.let {
            textureValues.add("CAPE", JsonObject().apply { addProperty("url", it.toString()) })
        }
        textureRoot.add("textures", textureValues)
        val encoded = Base64.getEncoder().encodeToString(
            textureRoot.toString().toByteArray(StandardCharsets.UTF_8)
        )
        properties.add(JsonObject().apply {
            addProperty("name", "textures")
            addProperty("value", encoded)
        })
        return properties
    }

    private fun legacyText(text: String): JsonElement {
        val components = TextComponent.fromLegacyText(text)
        return JsonParser.parseString(ComponentSerializer.toString(components))
    }

    /** 将 UUID 转为原版 profile codec 使用的四整数数组。 */
    private fun uuid(mostSignificantBits: Long, leastSignificantBits: Long): JsonArray = JsonArray().apply {
        add((mostSignificantBits shr 32).toInt())
        add(mostSignificantBits.toInt())
        add((leastSignificantBits shr 32).toInt())
        add(leastSignificantBits.toInt())
    }

    private fun addNumbers(target: JsonObject, key: String, values: Iterable<Number>) {
        val array = JsonArray()
        values.forEach(array::add)
        if (array.size() > 0) target.add(key, array)
    }

    private fun addBooleans(target: JsonObject, key: String, values: Iterable<Boolean>) {
        val array = JsonArray()
        values.forEach(array::add)
        if (array.size() > 0) target.add(key, array)
    }

    private fun addStrings(target: JsonObject, key: String, values: Iterable<String>) {
        val array = JsonArray()
        values.forEach(array::add)
        if (array.size() > 0) target.add(key, array)
    }
}
