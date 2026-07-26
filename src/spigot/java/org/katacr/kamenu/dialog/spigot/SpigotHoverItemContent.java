package org.katacr.kamenu.dialog.spigot;

import com.google.gson.JsonObject;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Content;

/**
 * 使用 Spigot/Bungee 公开组件 API 承载新版 SHOW_ITEM 的 item components。
 *
 * 字段名称与客户端 item stack codec 保持一致，使 hover_item 与 Body.item
 * 复用同一份 {@link SpigotPublicItemMapper} 映射结果而无需 NMS 或旧式 SNBT。
 */
final class SpigotHoverItemContent extends Content {
    private final String id;
    private final int count;
    private final JsonObject components;

    private SpigotHoverItemContent(String id, int count, JsonObject components) {
        this.id = id;
        this.count = count;
        this.components = components;
    }

    /** 从 Body.item 映射结果创建客户端 SHOW_ITEM 内容。 */
    static SpigotHoverItemContent from(JsonObject mappedItem) {
        String id = mappedItem.get("id").getAsString();
        int count = mappedItem.has("count") ? mappedItem.get("count").getAsInt() : 1;
        JsonObject components = mappedItem.has("components")
                ? mappedItem.getAsJsonObject("components").deepCopy()
                : null;
        return new SpigotHoverItemContent(id, count, components);
    }

    @Override
    public HoverEvent.Action requiredAction() {
        return HoverEvent.Action.SHOW_ITEM;
    }
}
