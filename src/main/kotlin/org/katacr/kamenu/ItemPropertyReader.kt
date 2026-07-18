@file:Suppress("UnstableApiUsage", "DEPRECATION")

package org.katacr.kamenu

import org.bukkit.inventory.meta.ItemMeta

/**
 * ItemMeta 常用模型属性读取器。
 *
 * 为 `checkitem`、`hasItem` 等功能提供统一的 ItemModel 和整数 CustomModelData 语义，
 * 避免不同模块分别处理新旧模型字段后出现判断差异。
 */
object ItemPropertyReader {
    /** 读取 ItemModel 的完整 NamespacedKey；未配置时返回 null。 */
    fun getItemModel(itemMeta: ItemMeta?): String? {
        return itemMeta?.itemModel?.toString()
    }

    /** 读取旧整数 CustomModelData；未配置时返回 null。 */
    fun getCustomModelId(itemMeta: ItemMeta?): Int? {
        if (itemMeta?.hasCustomModelData() != true) return null
        return itemMeta.customModelData
    }
}
