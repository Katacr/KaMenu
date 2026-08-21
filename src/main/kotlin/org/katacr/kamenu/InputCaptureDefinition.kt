package org.katacr.kamenu

/**
 * 统一输入捕获的不可变定义模型。
 *
 * 一个定义由若干有序层组成，玩家逐层输入；全部层通过校验后执行 complete 动作组。
 * 定义可来自菜单 YAML 的 `Input-Captures.<id>` 段，也可由 `input:` 动作内联参数生成。
 */
data class InputCaptureDefinition(
    val id: String,
    val type: InputCaptureDefinition.CaptureType,
    /** 每层超时秒数，进入新层时重置计时。 */
    val timeoutSeconds: Long,
    /** 取消关键字；null 表示不启用取消文本。 */
    val cancelText: String?,
    /** 单层校验失败重试上限，超限按取消处理。 */
    val maxAttempts: Int,
    /** anvil 类型捕获完成后是否恢复原容器菜单；默认 true。 */
    val reopen: Boolean,
    val layers: List<InputCaptureDefinition.InputLayer>,
    val completeActions: List<Any>,
    val timeoutActions: List<Any>,
    val cancelActions: List<Any>
) {
    enum class CaptureType {
        CHAT, ANVIL, DIALOG;

        companion object {
            /** 从配置文本解析类型，未知值返回 null。 */
            fun fromConfig(raw: String): CaptureType? =
                when (raw.trim().lowercase()) {
                    "chat" -> CHAT
                    "anvil" -> ANVIL
                    "dialog" -> DIALOG
                    else -> null
                }
        }
    }

    /** 单层捕获定义：独立变量 key、提示动作与可选校验。 */
    data class InputLayer(
        val key: String,
        val maxLength: Int,
        /** anvil 类型中铁砧界面的标题文本；null 时使用默认语言提示。 */
        val title: String?,
        val promptActions: List<Any>,
        val validateCondition: String?,
        val invalidActions: List<Any>
    )
}
