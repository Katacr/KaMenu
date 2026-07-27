package org.katacr.kamenu.dialog.spigot

import com.google.gson.JsonObject
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.hover.content.Content

/**
 * 使用 Spigot/Bungee 公开组件 API 承载新版 SHOW_ITEM 的 item components。
 *
 * 字段名称与客户端 item stack codec 保持一致，使 hover_item 与 Body.item
 * 复用同一份 [SpigotPublicItemMapper] 映射结果而无需 NMS 或旧式 SNBT。
 */
internal class SpigotHoverItemContent private constructor(
    private val id: String,
    private val count: Int,
    private val components: JsonObject?
) : Content() {
    override fun requiredAction(): HoverEvent.Action = HoverEvent.Action.SHOW_ITEM

    companion object {
        /** 从 Body.item 映射结果创建客户端 SHOW_ITEM 内容。 */
        fun from(mappedItem: JsonObject): SpigotHoverItemContent {
            val id = mappedItem.get("id").asString
            val count = if (mappedItem.has("count")) mappedItem.get("count").asInt else 1
            val components = if (mappedItem.has("components")) {
                mappedItem.getAsJsonObject("components").deepCopy()
            } else {
                null
            }
            return SpigotHoverItemContent(id, count, components)
        }
    }
}
