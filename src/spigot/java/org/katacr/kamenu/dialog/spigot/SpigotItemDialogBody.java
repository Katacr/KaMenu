package org.katacr.kamenu.dialog.spigot;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import net.md_5.bungee.api.dialog.body.DialogBody;
import net.md_5.bungee.api.dialog.body.PlainMessageBody;

/**
 * Spigot 的公开 Dialog API 未封装原版 item body，此类补齐 Mojang Dialog codec 所需 JSON 字段。
 */
final class SpigotItemDialogBody extends DialogBody {
    private final JsonObject item;
    private final PlainMessageBody description;
    @SerializedName("show_decorations")
    private final boolean showDecorations;
    @SerializedName("show_tooltip")
    private final boolean showTooltip;
    private final int width;
    private final int height;

    SpigotItemDialogBody(
            JsonObject item,
            PlainMessageBody description,
            boolean showDecorations,
            boolean showTooltip,
            int width,
            int height
    ) {
        super("minecraft:item");
        this.item = item;
        this.description = description;
        this.showDecorations = showDecorations;
        this.showTooltip = showTooltip;
        this.width = width;
        this.height = height;
    }

    /** 在完整 ItemStack codec 不可用时生成只含材质与数量的兼容数据。 */
    static JsonObject basicItem(String itemId, int amount) {
        JsonObject item = new JsonObject();
        item.addProperty("id", itemId);
        if (amount != 1) {
            item.addProperty("count", amount);
        }
        return item;
    }
}
