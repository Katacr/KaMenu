package org.katacr.kamenu.dialog.spigot

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import net.md_5.bungee.api.dialog.body.DialogBody
import net.md_5.bungee.api.dialog.body.PlainMessageBody

/**
 * 补齐 Spigot 公开 Dialog API 尚未封装的原版 item body JSON 字段。
 */
internal class SpigotItemDialogBody(
    private val item: JsonObject,
    private val description: PlainMessageBody?,
    @field:SerializedName("show_decorations")
    private val showDecorations: Boolean,
    @field:SerializedName("show_tooltip")
    private val showTooltip: Boolean,
    private val width: Int,
    private val height: Int
) : DialogBody("minecraft:item") {
    companion object {
        /** 在完整 ItemStack codec 不可用时生成只含材质与数量的兼容数据。 */
        fun basicItem(itemId: String, amount: Int): JsonObject = JsonObject().apply {
            addProperty("id", itemId)
            if (amount != 1) {
                addProperty("count", amount)
            }
        }
    }
}
