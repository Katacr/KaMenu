package org.katacr.kamenu

import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

/**
 * 跨核心实现 `hide_tooltip` 与 `hide_components` 两个 tooltip 隐藏属性。
 *
 * 借鉴 nightcore 的双核心策略：
 * - `hide_tooltip`：通过公开反射调用 `ItemMeta.setHideTooltip(boolean)`，Paper/Spigot 1.20.5+ 均可用；
 *   旧核心缺少该方法时静默跳过。
 * - `hide_components`：Paper 上写入 `TooltipDisplay.hiddenComponents(...)`（全部组件减去白名单），
 *   非 Paper（或组件 API 不可用）时退化为 nightcore 使用的 8 个 `ItemFlag` 近似。
 *
 * 全部通过反射调用，保持对 1.16.5 的编译兼容；`hide_tooltip` 优先于 `hide_components`（与 nightcore 一致）。
 */
object ItemTooltipCompat {
    /** Paper 组件隐藏时保留的组件（名称、模型、lore 与 tooltip 自身），与 nightcore 白名单一致。 */
    private val COMPONENT_WHITELIST = setOf(
        "minecraft:lore",
        "minecraft:item_name",
        "minecraft:item_model",
        "minecraft:custom_name",
        "minecraft:custom_model_data",
        "minecraft:tooltip_display",
        "minecraft:tooltip_style"
    )

    /** 非 Paper 核心降级隐藏的原版 ItemFlag，与 nightcore `SpigotBridge` 保持一致。 */
    private val SPIGOT_FLAG_NAMES = listOf(
        "HIDE_ATTRIBUTES",
        "HIDE_UNBREAKABLE",
        "HIDE_ENCHANTS",
        "HIDE_PLACED_ON",
        "HIDE_DESTROYS",
        "HIDE_DYE",
        "HIDE_ARMOR_TRIM",
        "HIDE_ADDITIONAL_TOOLTIP"
    )

    private val dataComponentTypesClass: Class<*>? by lazy {
        runCatching { Class.forName("io.papermc.paper.datacomponent.DataComponentTypes") }.getOrNull()
    }

    /** 当前核心是否提供 Paper 数据组件 API（可写入 TooltipDisplay）。 */
    private fun paperComponentsAvailable(): Boolean = dataComponentTypesClass != null

    /**
     * 应用 tooltip 隐藏属性。
     *
     * @param item 已写入名称/lore/模型的展示物品
     * @param hideTooltip 隐藏整个 tooltip（优先于 hideComponents）
     * @param hideComponents 仅隐藏原版自动组件行，保留自定义名称与 lore
     * @return 是否成功应用；两参数均为 false 时直接返回 true
     */
    fun apply(item: ItemStack, hideTooltip: Boolean, hideComponents: Boolean): Boolean {
        if (!hideTooltip && !hideComponents) return true
        if (hideTooltip) {
            return setHideTooltip(item)
        }
        return if (paperComponentsAvailable()) {
            applyPaperHiddenComponents(item) || applySpigotHiddenFlags(item)
        } else {
            applySpigotHiddenFlags(item)
        }
    }

    /** 反射调用 `ItemMeta.setHideTooltip(true)`；旧核心缺少该方法时返回 false。 */
    private fun setHideTooltip(item: ItemStack): Boolean = runCatching {
        val method = sequenceOf(ItemMeta::class.java, item.itemMeta?.javaClass)
            .filterNotNull()
            .flatMap { it.methods.asSequence() }
            .firstOrNull {
                it.name == "setHideTooltip" &&
                    it.parameterCount == 1 &&
                    it.parameterTypes[0] == java.lang.Boolean.TYPE
            }
            ?: return false
        val meta = item.itemMeta ?: return false
        method.isAccessible = true
        method.invoke(meta, true)
        item.itemMeta = meta
        true
    }.getOrDefault(false)

    /** Paper：把「全部数据组件 − 白名单」写入 TOOLTIP_DISPLAY.hidden_components。 */
    private fun applyPaperHiddenComponents(item: ItemStack): Boolean = runCatching {
        val typesClass = dataComponentTypesClass ?: return false
        val tooltipDisplayType = typesClass.getField("TOOLTIP_DISPLAY").get(null) ?: return false

        val registry = Class.forName("org.bukkit.Registry")
            .getField("DATA_COMPONENT_TYPE")
            .get(null) as? Iterable<*> ?: return false

        val hidden = linkedSetOf<Any>()
        for (component in registry) {
            if (component == null) continue
            val key = runCatching {
                component.javaClass.getMethod("getKey").invoke(component)?.toString()
            }.getOrNull()
            if (key != null && key in COMPONENT_WHITELIST) continue
            hidden.add(component)
        }
        if (hidden.isEmpty()) return false

        val tooltipDisplayClass = Class.forName("io.papermc.paper.datacomponent.item.TooltipDisplay")
        val builder = tooltipDisplayClass.getMethod("tooltipDisplay").invoke(null)
        val builderClass = Class.forName("io.papermc.paper.datacomponent.item.TooltipDisplay\$Builder")
        builderClass.getMethod("hiddenComponents", Set::class.java).invoke(builder, hidden)
        val display = builderClass.getMethod("build").invoke(builder)

        // 选择泛型擦除后的 setData(DataComponentType.Valued, Object) 重载（T 擦除为 Object），
        // 避免与 setData(Valued, DataComponentBuilder) 重载混淆。
        val setData = ItemStack::class.java.methods.firstOrNull { method ->
            method.name == "setData" &&
                method.parameterCount == 2 &&
                method.parameterTypes[0].isInstance(tooltipDisplayType) &&
                method.parameterTypes[1] == Any::class.java
        } ?: return false
        setData.invoke(item, tooltipDisplayType, display)
        true
    }.getOrDefault(false)

    /** 非 Paper：用 8 个原版 ItemFlag 近似隐藏常见组件行。 */
    private fun applySpigotHiddenFlags(item: ItemStack): Boolean = runCatching {
        val meta = item.itemMeta ?: return false
        var applied = false
        SPIGOT_FLAG_NAMES.forEach { name ->
            runCatching { ItemFlag.valueOf(name) }.getOrNull()?.let {
                meta.addItemFlags(it)
                applied = true
            }
        }
        if (applied) item.itemMeta = meta
        applied
    }.getOrDefault(false)
}
