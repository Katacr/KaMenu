@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu.dialog.paper

import io.papermc.paper.registry.data.dialog.action.DialogAction
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.katacr.kamenu.DialogSessionManager
import org.katacr.kamenu.InputCaptureUtils
import org.katacr.kamenu.KaMenu
import org.katacr.kamenu.MenuActions
import java.time.Duration

/**
 * 构造 Paper Dialog 按钮 callback，并把输入值交给平台中立的动作执行器。
 *
 * Paper 专属的 [DialogAction] 和响应对象只存在于此适配类，避免 Spigot 加载通用动作代码时解析 Paper 类型。
 */
object PaperDialogActionFactory {
    /** 根据菜单动作路径创建一次性 Paper callback。 */
    fun build(
        player: Player,
        config: YamlConfiguration,
        path: String,
        inputKeys: List<String>,
        inputTypes: Map<String, String>,
        inputRemoveChars: Map<String, String> = emptyMap(),
        checkboxMappings: Map<String, Pair<String, String>>,
        menuOpener: (Player, String) -> Unit,
        closesDialogAfterAction: Boolean = false,
        initialVariables: Map<String, String> = emptyMap(),
        contextId: String? = null,
        actionOverride: List<*>? = null
    ): DialogAction {
        val actionList = actionOverride ?: config.getList(path)
        if (actionList?.size == 1) {
            val action = actionList.first()
            if (action is String) {
                when {
                    action.startsWith("url:") -> return DialogAction.staticAction(
                        ClickEvent.openUrl(action.removePrefix("url:").trim())
                    )

                    action.startsWith("copy:") -> return DialogAction.staticAction(
                        ClickEvent.copyToClipboard(action.removePrefix("copy:").trim())
                    )
                }
            }
        }

        return DialogAction.customClick({ response, _ ->
            val variables = initialVariables.toMutableMap()
            val rawValues = inputKeys.associateWith { key ->
                response?.getFloat(key)?.toString()
                    ?: response?.getText(key)
                    ?: response?.getBoolean(key)?.toString()
            }
            variables.putAll(
                InputCaptureUtils.captureVariables(
                    Bukkit.getPluginManager().getPlugin("KaMenu") as? KaMenu,
                    rawValues,
                    InputCaptureUtils.Schema(inputKeys, inputTypes, inputRemoveChars, checkboxMappings)
                )
            )
            if (actionOverride != null) {
                MenuActions.executeActionGroup(player, config, actionOverride, variables, contextId = contextId)
            } else {
                MenuActions.executeConfigActionPath(
                    player,
                    config,
                    path,
                    variables,
                    menuOpener,
                    closesDialogAfterAction,
                    contextId
                )
            }
        }, callbackOptions(config))
    }

    private fun callbackOptions(config: YamlConfiguration): ClickCallback.Options =
        ClickCallback.Options.builder()
            .uses(1)
            .lifetime(Duration.ofSeconds(DialogSessionManager.lifetimeSeconds(config)))
            .build()
}
