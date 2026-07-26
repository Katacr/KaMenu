package org.katacr.kamenu.dialog

import org.bukkit.inventory.ItemStack

/**
 * 与具体服务器核心无关的 Dialog 定义。
 *
 * 所有文本已经完成 KaMenu 变量和条件解析；渲染器仅负责转换为 Paper 或 Spigot 的原生 API 对象。
 */
data class DialogDefinition(
    val title: String,
    val body: List<DialogBodyDefinition>,
    val inputs: List<DialogInputDefinition>,
    val bottom: DialogBottomDefinition,
    val settings: DialogSettings,
    val inputSchema: DialogInputSchema
)

/** Dialog 的基础行为设置。 */
data class DialogSettings(
    val canEscape: Boolean,
    val pause: Boolean,
    val afterAction: DialogAfterAction
)

/** 客户端按钮动作后的 Dialog 行为。 */
enum class DialogAfterAction {
    CLOSE,
    NONE,
    WAIT_FOR_RESPONSE
}

/** Dialog 正文组件。 */
sealed interface DialogBodyDefinition {
    /** 普通富文本消息。 */
    data class Message(val text: String, val width: Int?) : DialogBodyDefinition

    /** 物品展示；平台适配器负责将完整 Bukkit ItemStack 转换为原生 Dialog 数据。 */
    data class Item(
        val componentId: String,
        val itemStack: ItemStack,
        val description: String?,
        val descriptionWidth: Int?,
        val showOverlays: Boolean,
        val showTooltip: Boolean,
        val width: Int,
        val height: Int
    ) : DialogBodyDefinition
}

/** Dialog 输入组件。 */
sealed interface DialogInputDefinition {
    val key: String
    val label: String
    val width: Int?
    val labelVisible: Boolean

    data class Text(
        override val key: String,
        override val label: String,
        override val width: Int,
        override val labelVisible: Boolean,
        val initial: String,
        val maxLength: Int,
        val multiline: DialogMultiline?
    ) : DialogInputDefinition

    data class Slider(
        override val key: String,
        override val label: String,
        override val width: Int,
        override val labelVisible: Boolean = true,
        val format: String,
        val minimum: Float,
        val maximum: Float,
        val initial: Float,
        val step: Float
    ) : DialogInputDefinition

    data class Checkbox(
        override val key: String,
        override val label: String,
        override val width: Int? = null,
        override val labelVisible: Boolean = true,
        val initial: Boolean,
        val onTrue: String,
        val onFalse: String
    ) : DialogInputDefinition

    data class Dropdown(
        override val key: String,
        override val label: String,
        override val width: Int,
        override val labelVisible: Boolean,
        val options: List<DialogOptionDefinition>
    ) : DialogInputDefinition
}

/** 多行输入框的显示参数。 */
data class DialogMultiline(val maxLines: Int, val height: Int)

/** 单项选择按钮的一项。 */
data class DialogOptionDefinition(val id: String, val display: String, val initial: Boolean)

/** 底部布局。 */
data class DialogBottomDefinition(
    val type: DialogBottomType,
    val buttons: List<DialogButtonDefinition>,
    val columns: Int = 2,
    val exit: DialogButtonDefinition? = null
)

/** 底部布局类型。 */
enum class DialogBottomType {
    NOTICE,
    CONFIRMATION,
    MULTI
}

/** 一个底部按钮及其原始 KaMenu actions 路径。 */
data class DialogButtonDefinition(
    val text: String,
    val tooltip: String?,
    val width: Int?,
    val actionPath: String,
    val variables: Map<String, String> = emptyMap()
)

/** 提交输入时需要保留的 KaMenu 输入清理规则。 */
data class DialogInputSchema(
    val keys: List<String>,
    val types: Map<String, String>,
    val removeChars: Map<String, String>,
    val checkboxMappings: Map<String, Pair<String, String>>
)
