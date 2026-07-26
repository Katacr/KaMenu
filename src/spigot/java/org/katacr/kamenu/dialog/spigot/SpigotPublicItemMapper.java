package org.katacr.kamenu.dialog.spigot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 仅使用 Bukkit/Spigot 公共 API 将 ItemStack 的可见属性映射为 Dialog item JSON。
 *
 * 映射器不读取 NMS、PDC 的私有值或未知数据组件；未公开属性会被安全忽略。
 */
final class SpigotPublicItemMapper {
    /** 将 Bukkit ItemStack 转换为原版 Dialog item body 接受的结构化 JSON。 */
    JsonObject map(ItemStack item) {
        JsonObject result = new JsonObject();
        result.addProperty("id", item.getType().getKey().toString());
        if (item.getAmount() != 1) {
            result.addProperty("count", item.getAmount());
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return result;
        }
        JsonObject components = new JsonObject();
        addTextComponents(meta, components);
        addModelComponents(meta, components);
        addEnchantments("minecraft:enchantments", meta.getEnchants(), components);
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            addEnchantments("minecraft:stored_enchantments", storageMeta.getStoredEnchants(), components);
        }
        addDurabilityComponents(meta, components);
        addDisplayComponents(meta, components);
        addTooltipDisplay(meta, components);
        addLeatherColor(meta, components);
        addProfile(meta, components);
        if (!components.isEmpty()) {
            result.add("components", components);
        }
        return result;
    }

    /** 映射自定义名称、固定物品名和 Lore。 */
    private void addTextComponents(ItemMeta meta, JsonObject components) {
        if (meta.hasDisplayName()) {
            components.add("minecraft:custom_name", legacyText(meta.getDisplayName()));
        }
        if (meta.hasItemName()) {
            components.add("minecraft:item_name", legacyText(meta.getItemName()));
        }
        if (meta.hasLore() && meta.getLore() != null) {
            JsonArray lore = new JsonArray();
            meta.getLore().forEach(line -> lore.add(legacyText(line)));
            components.add("minecraft:lore", lore);
        }
    }

    /** 映射旧整数和新版多字段自定义模型数据，以及 item_model。 */
    private void addModelComponents(ItemMeta meta, JsonObject components) {
        if (meta.hasCustomModelDataComponent() || meta.hasCustomModelData()) {
            CustomModelDataComponent modelData = meta.getCustomModelDataComponent();
            JsonObject value = new JsonObject();
            addNumbers(value, "floats", modelData.getFloats());
            addBooleans(value, "flags", modelData.getFlags());
            addStrings(value, "strings", modelData.getStrings());
            if (!modelData.getColors().isEmpty()) {
                JsonArray colors = new JsonArray();
                modelData.getColors().stream().map(Color::asRGB).forEach(colors::add);
                value.add("colors", colors);
            }
            if (!value.isEmpty()) {
                components.add("minecraft:custom_model_data", value);
            }
        }
        if (meta.hasItemModel() && meta.getItemModel() != null) {
            components.addProperty("minecraft:item_model", meta.getItemModel().toString());
        }
    }

    /** 映射普通附魔或附魔书的储存附魔。 */
    private void addEnchantments(
            String componentKey,
            Map<Enchantment, Integer> enchantments,
            JsonObject components
    ) {
        if (enchantments.isEmpty()) {
            return;
        }
        JsonObject value = new JsonObject();
        enchantments.forEach((enchantment, level) -> value.addProperty(enchantment.getKey().toString(), level));
        components.add(componentKey, value);
    }

    /** 映射耐久、最大耐久与不可破坏属性。 */
    private void addDurabilityComponents(ItemMeta meta, JsonObject components) {
        if (meta instanceof Damageable damageable) {
            if (damageable.hasDamage()) {
                components.addProperty("minecraft:damage", damageable.getDamage());
            }
            if (damageable.hasMaxDamage()) {
                components.addProperty("minecraft:max_damage", damageable.getMaxDamage());
            }
        }
        if (meta.isUnbreakable()) {
            components.add("minecraft:unbreakable", new JsonObject());
        }
    }

    /** 映射影响图标或 Tooltip 的通用显示属性。 */
    private void addDisplayComponents(ItemMeta meta, JsonObject components) {
        if (meta.hasEnchantmentGlintOverride()) {
            components.addProperty("minecraft:enchantment_glint_override", meta.getEnchantmentGlintOverride());
        }
        if (meta.hasTooltipStyle() && meta.getTooltipStyle() != null) {
            components.addProperty("minecraft:tooltip_style", meta.getTooltipStyle().toString());
        }
        if (meta.hasMaxStackSize()) {
            components.addProperty("minecraft:max_stack_size", meta.getMaxStackSize());
        }
        if (meta.hasRarity()) {
            components.addProperty("minecraft:rarity", meta.getRarity().name().toLowerCase(Locale.ROOT));
        }
    }

    /** 将 Bukkit ItemFlag 转换为新版 tooltip_display 隐藏组件集合。 */
    private void addTooltipDisplay(ItemMeta meta, JsonObject components) {
        Set<String> hiddenComponents = new LinkedHashSet<>();
        for (ItemFlag flag : meta.getItemFlags()) {
            String component = hiddenComponent(flag);
            if (component != null) {
                hiddenComponents.add(component);
            }
        }
        if (!meta.isHideTooltip() && hiddenComponents.isEmpty()) {
            return;
        }
        JsonObject tooltip = new JsonObject();
        if (meta.isHideTooltip()) {
            tooltip.addProperty("hide_tooltip", true);
        }
        if (!hiddenComponents.isEmpty()) {
            JsonArray hidden = new JsonArray();
            hiddenComponents.forEach(hidden::add);
            tooltip.add("hidden_components", hidden);
        }
        components.add("minecraft:tooltip_display", tooltip);
    }

    /** 兼容 Spigot 中 getComponent() 为空的旧 ItemFlag 别名。 */
    private String hiddenComponent(ItemFlag flag) {
        NamespacedKey component = flag.getComponent();
        if (component != null) {
            return component.toString();
        }
        return switch (flag.name()) {
            case "HIDE_ENCHANTS" -> "minecraft:enchantments";
            case "HIDE_ATTRIBUTES" -> "minecraft:attribute_modifiers";
            case "HIDE_UNBREAKABLE" -> "minecraft:unbreakable";
            case "HIDE_DESTROYS" -> "minecraft:can_break";
            case "HIDE_PLACED_ON" -> "minecraft:can_place_on";
            case "HIDE_DYE" -> "minecraft:dyed_color";
            case "HIDE_ARMOR_TRIM" -> "minecraft:trim";
            default -> null;
        };
    }

    /** 映射皮革护甲染色。 */
    private void addLeatherColor(ItemMeta meta, JsonObject components) {
        if (meta instanceof LeatherArmorMeta leatherMeta) {
            components.addProperty("minecraft:dyed_color", leatherMeta.getColor().asRGB());
        }
    }

    /** 使用 Bukkit PlayerProfile 生成玩家头颅所需的公开 profile 数据。 */
    private void addProfile(ItemMeta meta, JsonObject components) {
        if (!(meta instanceof SkullMeta skullMeta)) {
            return;
        }
        PlayerProfile profile = skullMeta.getOwnerProfile();
        if (profile == null) {
            return;
        }
        JsonObject value = new JsonObject();
        if (profile.getName() != null && !profile.getName().isBlank()) {
            value.addProperty("name", profile.getName());
        }
        if (profile.getUniqueId() != null) {
            value.add("id", uuid(profile.getUniqueId().getMostSignificantBits(), profile.getUniqueId().getLeastSignificantBits()));
        }
        JsonArray properties = textureProperties(profile.getTextures());
        if (!properties.isEmpty()) {
            value.add("properties", properties);
        }
        if (!value.isEmpty()) {
            components.add("minecraft:profile", value);
        }
    }

    /** 将公开皮肤和披风 URL 组合为无需签名的标准 textures 属性。 */
    private JsonArray textureProperties(PlayerTextures textures) {
        JsonArray properties = new JsonArray();
        URL skin = textures.getSkin();
        URL cape = textures.getCape();
        if (skin == null && cape == null) {
            return properties;
        }
        JsonObject textureRoot = new JsonObject();
        JsonObject textureValues = new JsonObject();
        if (skin != null) {
            JsonObject skinValue = new JsonObject();
            skinValue.addProperty("url", skin.toString());
            if (textures.getSkinModel() == PlayerTextures.SkinModel.SLIM) {
                JsonObject metadata = new JsonObject();
                metadata.addProperty("model", "slim");
                skinValue.add("metadata", metadata);
            }
            textureValues.add("SKIN", skinValue);
        }
        if (cape != null) {
            JsonObject capeValue = new JsonObject();
            capeValue.addProperty("url", cape.toString());
            textureValues.add("CAPE", capeValue);
        }
        textureRoot.add("textures", textureValues);
        String encoded = Base64.getEncoder().encodeToString(
                textureRoot.toString().getBytes(StandardCharsets.UTF_8));
        JsonObject property = new JsonObject();
        property.addProperty("name", "textures");
        property.addProperty("value", encoded);
        properties.add(property);
        return properties;
    }

    private JsonElement legacyText(String text) {
        BaseComponent[] components = TextComponent.fromLegacyText(text);
        return JsonParser.parseString(ComponentSerializer.toString(components));
    }

    /** 将 UUID 转为原版 profile codec 使用的四整数数组。 */
    private JsonArray uuid(long mostSignificantBits, long leastSignificantBits) {
        JsonArray value = new JsonArray();
        value.add((int) (mostSignificantBits >> 32));
        value.add((int) mostSignificantBits);
        value.add((int) (leastSignificantBits >> 32));
        value.add((int) leastSignificantBits);
        return value;
    }

    private void addNumbers(JsonObject target, String key, Iterable<? extends Number> values) {
        JsonArray array = new JsonArray();
        values.forEach(array::add);
        if (!array.isEmpty()) {
            target.add(key, array);
        }
    }

    private void addBooleans(JsonObject target, String key, Iterable<Boolean> values) {
        JsonArray array = new JsonArray();
        values.forEach(array::add);
        if (!array.isEmpty()) {
            target.add(key, array);
        }
    }

    private void addStrings(JsonObject target, String key, Iterable<String> values) {
        JsonArray array = new JsonArray();
        values.forEach(array::add);
        if (!array.isEmpty()) {
            target.add(key, array);
        }
    }
}
