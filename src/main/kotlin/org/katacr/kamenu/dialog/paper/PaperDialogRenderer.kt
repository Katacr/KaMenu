@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu.dialog.paper

import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.body.PlainMessageDialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput
import io.papermc.paper.registry.data.dialog.input.TextDialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.katacr.kamenu.KaMenu
import org.katacr.kamenu.MenuActions
import org.katacr.kamenu.TextParser
import org.katacr.kamenu.dialog.DialogAfterAction
import org.katacr.kamenu.dialog.DialogBodyDefinition
import org.katacr.kamenu.dialog.DialogBottomType
import org.katacr.kamenu.dialog.DialogButtonDefinition
import org.katacr.kamenu.dialog.DialogDefinition
import org.katacr.kamenu.dialog.DialogInputDefinition

/**
 * 将共享 [DialogDefinition] 映射为 Paper 原生 Dialog。
 *
 * 该类只负责 Paper API 对象构建；菜单 YAML 的解析、变量解析和 repeat 分页由共享编译器完成。
 */
class PaperDialogRenderer(private val plugin: KaMenu) {
    /** 将一份已编译定义渲染并显示给玩家。调用方必须已进入玩家允许的线程。 */
    fun show(player: Player, config: YamlConfiguration, contextId: String, definition: DialogDefinition) {
        val menuOpener: (Player, String) -> Unit = { target, menuId ->
            org.katacr.kamenu.MenuUI.openMenu(target, menuId, plugin.menuManager, plugin)
        }
        val base = DialogBase.builder(text(player, definition.title))
            .body(body(player, config, definition, menuOpener))
            .inputs(inputs(player, definition.inputs))
            .canCloseWithEscape(definition.settings.canEscape)
            .pause(definition.settings.pause)
            .afterAction(DialogBase.DialogAfterAction.valueOf(definition.settings.afterAction.name))
            .build()

        val dialogType = bottom(player, config, contextId, definition, menuOpener)
        player.showDialog(Dialog.create { it.empty().base(base).type(dialogType) })
    }

    /** 将共享 Body 定义映射为 Paper Body，并保留可点击文本和物品描述。 */
    private fun body(
        player: Player,
        config: YamlConfiguration,
        definition: DialogDefinition,
        menuOpener: (Player, String) -> Unit
    ): List<DialogBody> = definition.body.map { body ->
        when (body) {
            is DialogBodyDefinition.Message -> plainMessage(
                clickableText(body.text, player, config, menuOpener),
                body.width
            )

            is DialogBodyDefinition.Item -> {
                val description = body.description?.let {
                    plainMessage(clickableText(it, player, config, menuOpener), body.descriptionWidth)
                }
                DialogBody.item(
                    body.itemStack,
                    description,
                    body.showOverlays,
                    body.showTooltip,
                    body.width,
                    body.height
                )
            }
        }
    }

    /** 将共享输入定义映射为 Paper 输入组件。 */
    private fun inputs(player: Player, definitions: List<DialogInputDefinition>): List<DialogInput> =
        definitions.map { input ->
            when (input) {
                is DialogInputDefinition.Text -> DialogInput.text(input.key, text(player, input.label))
                    .width(input.width)
                    .labelVisible(input.labelVisible)
                    .initial(input.initial)
                    .maxLength(input.maxLength)
                    .apply {
                        input.multiline?.let {
                            multiline(TextDialogInput.MultilineOptions.create(it.maxLines, it.height))
                        }
                    }
                    .build()

                is DialogInputDefinition.Slider -> DialogInput.numberRange(
                    input.key,
                    input.width,
                    text(player, input.label),
                    input.format,
                    input.minimum,
                    input.maximum,
                    input.initial,
                    input.step
                )

                is DialogInputDefinition.Checkbox -> DialogInput.bool(input.key, text(player, input.label))
                    .initial(input.initial)
                    .onTrue(input.onTrue)
                    .onFalse(input.onFalse)
                    .build()

                is DialogInputDefinition.Dropdown -> DialogInput.singleOption(
                    input.key,
                    text(player, input.label),
                    input.options.map { option ->
                        SingleOptionDialogInput.OptionEntry.create(
                            option.id,
                            text(player, option.display),
                            option.initial
                        )
                    }
                )
                    .width(input.width)
                    .labelVisible(input.labelVisible)
                    .build()
            }
        }

    /** 将共享底部定义映射为 Paper notice、confirmation 或 multiAction。 */
    private fun bottom(
        player: Player,
        config: YamlConfiguration,
        contextId: String,
        definition: DialogDefinition,
        menuOpener: (Player, String) -> Unit
    ): DialogType {
        val buttons = definition.bottom.buttons
        return when (definition.bottom.type) {
            DialogBottomType.NOTICE -> DialogType.notice(
                button(player, config, contextId, buttons.firstOrNull() ?: emptyButton(), definition, menuOpener)
            )

            DialogBottomType.CONFIRMATION -> DialogType.confirmation(
                button(player, config, contextId, buttons.getOrNull(0) ?: emptyButton("确认"), definition, menuOpener),
                button(player, config, contextId, buttons.getOrNull(1) ?: emptyButton("取消"), definition, menuOpener)
            )

            DialogBottomType.MULTI -> DialogType.multiAction(
                buttons.map { button(player, config, contextId, it, definition, menuOpener) }
            )
                .columns(definition.bottom.columns)
                .exitAction(definition.bottom.exit?.let {
                    button(player, config, contextId, it, definition, menuOpener)
                })
                .build()
        }
    }

    /** 构造 Paper ActionButton，并把按钮变量和动作路径交给统一动作工厂。 */
    private fun button(
        player: Player,
        config: YamlConfiguration,
        contextId: String,
        definition: DialogButtonDefinition,
        dialog: DialogDefinition,
        menuOpener: (Player, String) -> Unit
    ): ActionButton {
        val builder = ActionButton.builder(text(player, definition.text))
            .action(
                PaperDialogActionFactory.build(
                    player = player,
                    config = config,
                    path = definition.actionPath,
                    inputKeys = dialog.inputSchema.keys,
                    inputTypes = dialog.inputSchema.types,
                    inputRemoveChars = dialog.inputSchema.removeChars,
                    checkboxMappings = dialog.inputSchema.checkboxMappings,
                    menuOpener = menuOpener,
                    closesDialogAfterAction = dialog.settings.afterAction == DialogAfterAction.CLOSE,
                    initialVariables = definition.variables,
                    contextId = contextId,
                    actionOverride = definition.actionOverride
                )
            )
        definition.tooltip?.let { builder.tooltip(clickableText(it, player, config, menuOpener)) }
        definition.width?.let { builder.width(it) }
        return builder.build()
    }

    /** 创建无动作的默认按钮，仅用于非法或不完整配置的兼容回退。 */
    private fun emptyButton(text: String = "确认"): DialogButtonDefinition =
        DialogButtonDefinition(text, null, null, "")

    /** 解析 Paper 文本组件并保留 KaMenu 的可点击文本语法。 */
    private fun clickableText(
        value: String,
        player: Player,
        config: YamlConfiguration,
        menuOpener: (Player, String) -> Unit
    ): Component = MenuActions.parseClickableText(value, player, config, menuOpener)

    /** 按共享定义中的可选宽度创建普通文本 Body。 */
    private fun plainMessage(component: Component, width: Int?): PlainMessageDialogBody =
        width?.let { DialogBody.plainMessage(component, it) } ?: DialogBody.plainMessage(component)

    /** 将已解析字符串转换为普通 Paper 文本组件。 */
    private fun text(player: Player, value: String): Component = TextParser.parseText(value, player)
}
