@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player

/**
 * 文本解析器
 * 统一处理文本格式转换：Legacy 颜色代码、十六进制颜色、物品图标、MiniMessage
 *
 * 提取自 MenuUI 和 MenuActions 中的重复代码，避免两处维护。
 */
object TextParser {

    private val serializer = LegacyComponentSerializer.legacyAmpersand()
    private val miniMessage = MiniMessage.miniMessage()

    // ==================== 预编译正则表达式（性能优化） ====================

    /** 匹配 &#RRGGBB 或 &#RRGGBBAA 格式的十六进制颜色 */
    private val HEX_PATTERN = Regex("&#([0-9a-fA-F]{6})([0-9a-fA-F]{2})?")

    /** 匹配 &item:[材质名称] 格式的物品图标语法 */
    private val ITEM_SPRITE_PATTERN = Regex("&item:\\[([^\\]]+)\\]")

    /** 检测 MiniMessage 标签特征：尖括号包裹的字母、冒号等 */
    private val MINI_MESSAGE_TAG_PATTERN = Regex("<[a-z_]+(?:[:][^>]*)?>", RegexOption.IGNORE_CASE)

    /** 检测 Legacy 颜色/格式代码 */
    private val LEGACY_CODE_PATTERN = Regex("[&§][0-9a-fA-FlmnoOrkLKMNO]")

    // ==================== Legacy → MiniMessage 映射表 ====================

    private val legacyToMiniMessageMap = mapOf(
        // 颜色代码
        "&0" to "<black>", "§0" to "<black>",
        "&1" to "<dark_blue>", "§1" to "<dark_blue>",
        "&2" to "<dark_green>", "§2" to "<dark_green>",
        "&3" to "<dark_aqua>", "§3" to "<dark_aqua>",
        "&4" to "<dark_red>", "§4" to "<dark_red>",
        "&5" to "<dark_purple>", "§5" to "<dark_purple>",
        "&6" to "<gold>", "§6" to "<gold>",
        "&7" to "<gray>", "§7" to "<gray>",
        "&8" to "<dark_gray>", "§8" to "<dark_gray>",
        "&9" to "<blue>", "§9" to "<blue>",
        "&a" to "<green>", "§a" to "<green>",
        "&b" to "<aqua>", "§b" to "<aqua>",
        "&c" to "<red>", "§c" to "<red>",
        "&d" to "<light_purple>", "§d" to "<light_purple>",
        "&e" to "<yellow>", "§e" to "<yellow>",
        "&f" to "<white>", "§f" to "<white>",
        // 格式化代码
        "&k" to "<obfuscated>", "§k" to "<obfuscated>",
        "&l" to "<bold>", "§l" to "<bold>",
        "&m" to "<strikethrough>", "§m" to "<strikethrough>",
        "&n" to "<underline>", "§n" to "<underline>",
        "&o" to "<italic>", "§o" to "<italic>",
        "&r" to "<reset>", "§r" to "<reset>",
        // 大写版本
        "&A" to "<green>", "§A" to "<green>",
        "&B" to "<aqua>", "§B" to "<aqua>",
        "&C" to "<red>", "§C" to "<red>",
        "&D" to "<light_purple>", "§D" to "<light_purple>",
        "&E" to "<yellow>", "§E" to "<yellow>",
        "&F" to "<white>", "§F" to "<white>",
        "&K" to "<obfuscated>", "§K" to "<obfuscated>",
        "&L" to "<bold>", "§L" to "<bold>",
        "&M" to "<strikethrough>", "§M" to "<strikethrough>",
        "&N" to "<underline>", "§N" to "<underline>",
        "&O" to "<italic>", "§O" to "<italic>",
        "&R" to "<reset>", "§R" to "<reset>"
    )

    // ==================== 公开 API ====================

    /**
     * 将 Legacy 颜色代码转换为 Adventure Component（&a, &b 等）
     */
    fun color(text: String?): Component =
        if (text == null) Component.empty() else serializer.deserialize(text)

    /**
     * 将 MiniMessage 格式转换为 Adventure Component
     * 支持丰富的格式：<red>、<gradient:red:blue>、<bold> 等
     */
    fun miniMessage(text: String?): Component =
        if (text == null) Component.empty() else miniMessage.deserialize(text)

    /**
     * 智能解析文本格式（自动检测 MiniMessage 或 Legacy）
     *
     * 处理流程：
     * 1. 将 &#RRGGBB 格式转换为 MiniMessage <color:#RRGGBB> 标签
     * 2. 将 &item:[材质] 格式转换为 MiniMessage <sprite:...> 标签
     * 3. 检测是否包含 MiniMessage 标签，选择对应解析器
     *    - 有 MiniMessage 标签：同时转换 Legacy 代码后使用 MiniMessage 解析
     *    - 无 MiniMessage 标签：使用 Legacy 颜色代码解析
     *
     * @param text 文本内容
     * @return Adventure Component
     */
    fun parseText(text: String?): Component = parseText(text, null)

    /**
     * 使用玩家上下文智能解析文本，使 Oraxen 权限字形能够正确判定当前玩家。
     *
     * @param text 文本内容
     * @param player 当前玩家；null 时使用不带权限上下文的 Oraxen Resolver
     * @return Adventure Component
     */
    fun parseText(text: String?, player: Player?): Component = parseText(text, player, false)

    /**
     * 在完整文本已确认使用 Oraxen 时，强制让拆分后的 shift-only 片段复用 Oraxen Resolver。
     */
    internal fun parseText(text: String?, player: Player?, forceOraxenResolver: Boolean): Component {
        if (text == null) return Component.empty()

        // 1. 将简化的十六进制颜色代码转换为 MiniMessage 标签
        var convertedText = convertHexToMiniMessage(text)

        // 2. 将简化的物品图标语法转换为 MiniMessage sprite 标签
        convertedText = convertItemSpriteToMiniMessage(convertedText)

        // 3. Paper Dialog 不会触发 ItemsAdder 的监听器，需要主动解析内置字形和偏移标签
        convertedText = ItemsAdderTextAdapter.replace(convertedText, player)

        // 4. 检测是否包含 MiniMessage 标签
        val hasMiniMessageTags = convertedText.contains(MINI_MESSAGE_TAG_PATTERN)

        return if (hasMiniMessageTags) {
            // 检测是否包含 Legacy 颜色代码
            val hasLegacyCodes = convertedText.contains(LEGACY_CODE_PATTERN)

            val textToParse = if (hasLegacyCodes) {
                convertLegacyToMiniMessage(convertedText)
            } else {
                convertedText
            }

            // Oraxen 字形需要其自定义 Resolver；其他文本继续使用标准 MiniMessage
            OraxenTextAdapter.parse(textToParse, player, forceOraxenResolver) ?: miniMessage(textToParse)
        } else {
            // 使用 Legacy 颜色代码解析
            color(convertedText)
        }
    }

    // ==================== 内部转换方法 ====================

    /**
     * 将 Legacy 颜色代码转换为 MiniMessage 标签
     */
    private fun convertLegacyToMiniMessage(text: String): String {
        var result = text
        legacyToMiniMessageMap.forEach { (legacy, mini) ->
            result = result.replace(legacy, mini)
        }
        return result
    }

    /**
     * 将简化的十六进制颜色代码转换为 MiniMessage 标签
     * &#FF4444你好 → <color:#FF4444>你好
     */
    private fun convertHexToMiniMessage(text: String): String {
        return text.replace(HEX_PATTERN) { matchResult ->
            val color = matchResult.groupValues[1].uppercase()
            "<color:#$color>"
        }
    }

    /**
     * 将简化的物品图标语法转换为 MiniMessage sprite 标签
     * &item:[diamond] → <sprite:items:item/diamond>
     * &item:[stone]  → <sprite:blocks:block/stone>
     */
    private fun convertItemSpriteToMiniMessage(text: String): String {
        return text.replace(ITEM_SPRITE_PATTERN) { matchResult ->
            val materialName = matchResult.groupValues[1]
            MaterialUtils.getSpriteTag(materialName) ?: matchResult.value
        }
    }
}
