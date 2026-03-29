package org.katacr.kamenu

import org.bukkit.Material

/**
 * 物品材质工具类
 * 用于处理材质名称的规范化匹配
 */
object MaterialUtils {

    /**
     * 规范化材质名称并匹配对应的 Material 枚举
     * 支持以下格式：
     * - 标准格式: DIAMOND_SWORD
     * - 小写: diamond_sword
     * - 混合大小写: DiAMond swORd
     * - 短杠: Diamond-Sword
     * - 空格: diamond sword
     * - 下划线: diamond_sword
     *
     * 示例：
     * - "diamond_sword" -> Material.DIAMOND_SWORD
     * - "Diamond_Sword" -> Material.DIAMOND_SWORD
     * - "Diamond-Sword" -> Material.DIAMOND_SWORD
     * - "diamond sword" -> Material.DIAMOND_SWORD
     * - "diAMond swORd" -> Material.DIAMOND_SWORD
     *
     * @param materialName 材质名称（各种格式）
     * @return 匹配的 Material，如果未找到则返回 null
     */
    fun matchMaterial(materialName: String): Material? {
        // 1. 规范化材质名称
        val normalized = normalizeMaterialName(materialName)

        // 2. 尝试匹配
        return Material.matchMaterial(normalized)
    }

    /**
     * 规范化材质名称
     * 1. 转换为大写
     * 2. 将短杠、空格替换为下划线
     *
     * @param materialName 原始材质名称
     * @return 规范化后的材质名称
     */
    fun normalizeMaterialName(materialName: String): String {
        return materialName
            .uppercase()                          // 转换为大写
            .replace("-", "_")                      // 将短杠替换为下划线
            .replace(" ", "_")                     // 将空格替换为下划线
            .replace(Regex("_+"), "_")            // 合并多个下划线
            .trim()                               // 去除首尾空白
    }
}
