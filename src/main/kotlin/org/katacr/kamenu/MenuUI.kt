@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput
import io.papermc.paper.registry.data.dialog.input.TextDialogInput
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.time.Duration

object MenuUI {
    private val serializer = LegacyComponentSerializer.legacyAmpersand()
    lateinit var plugin: KaMenu

    private fun color(text: String?): Component =
        if (text == null) Component.empty() else serializer.deserialize(text)

    fun openMenu(player: Player, menuId: String, manager: MenuManager) {
        val config = manager.getMenuConfig(menuId) ?: return
        val title = color(config.getString("Title", "KaMenu"))

        val bodyList = mutableListOf<DialogBody>()
        val inputList = mutableListOf<DialogInput>()
        val inputKeys = mutableListOf<String>()

        // 0. 解析设置
        val canEscape = config.getBoolean("Settings.can_escape", true)
        val pauseGame = config.getBoolean("Settings.pause", false) // 仅单机有效，一般设为 false
        val afterActionStr = config.getString("Settings.after_action", "CLOSE")?.uppercase() ?: "CLOSE"

        val afterAction = try {
            DialogBase.DialogAfterAction.valueOf(afterActionStr)
        } catch (e: Exception) {
            DialogBase.DialogAfterAction.CLOSE
        }

        // 1. 解析 Body (展示内容) - 保持不变
        config.getConfigurationSection("Body")?.let { section ->
            for (key in section.getKeys(false)) {
                val type = section.getString("$key.type")
                when (type) {
                    "message" -> bodyList.add(DialogBody.plainMessage(color(section.getString("$key.text"))))
                    "item" -> {
                        val material = Material.matchMaterial(section.getString("$key.material") ?: "PAPER") ?: Material.PAPER
                        val item = ItemStack(material)
                        item.editMeta { meta ->
                            meta.displayName(color(section.getString("$key.name")))
                            meta.lore(section.getStringList("$key.lore").map { color(it) })
                        }
                        val descriptionText = section.getString("$key.description")
                        val descriptionBody = descriptionText?.let { DialogBody.plainMessage(color(it)) }

                        val width = section.getInt("$key.width", 16)
                        val height = section.getInt("$key.height", 16)
                        val decorations = section.getBoolean("$key.decorations", true)
                        val tooltip = section.getBoolean("$key.tooltip", true)

                        bodyList.add(DialogBody.item(item, descriptionBody, decorations, tooltip, width, height))
                    }
                }
            }
        }

        // 2. 解析 Inputs (高级交互组件) - 保持不变，收集 inputKeys
        config.getConfigurationSection("Inputs")?.let { section ->
            for (key in section.getKeys(false)) {
                val type = section.getString("$key.type") ?: "text"
                val prompt = color(section.getString("$key.text"))

                when (type) {
                    "checkbox" -> {
                        inputList.add(DialogInput.bool(key, prompt)
                            .initial(section.getBoolean("$key.default", false))
                            .onTrue(section.getString("$key.on_true", "true")!!)
                            .onFalse(section.getString("$key.on_false", "false")!!)
                            .build())
                    }
                    "slider" -> {
                        val start = section.getDouble("$key.min", 0.0).toFloat()
                        val end = section.getDouble("$key.max", 10.0).toFloat()
                        inputList.add(DialogInput.numberRange(
                            key, 250, prompt,
                            section.getString("$key.format") ?: "%s: %s",
                            start, end, section.getDouble("$key.default", start.toDouble()).toFloat(),
                            section.getDouble("$key.step", 1.0).toFloat()
                        ))
                    }
                    "input" -> {
                        val builder = DialogInput.text(key, prompt)
                            .width(section.getInt("$key.width", 250))
                            .initial(section.getString("$key.default", "")!!)
                            .maxLength(section.getInt("$key.max_length", 256))

                        if (section.contains("$key.multiline")) {
                            builder.multiline(TextDialogInput.MultilineOptions.create(
                                section.getInt("$key.multiline.max_lines", 5),
                                section.getInt("$key.multiline.height", 100)
                            ))
                        }
                        inputList.add(builder.build())
                    }
                    "dropdown" -> {
                        val entries = section.getStringList("$key.options").map {
                            SingleOptionDialogInput.OptionEntry.create(it, color(it), it == section.getString("$key.default_id"))
                        }
                        inputList.add(DialogInput.singleOption(key, prompt, entries).width(section.getInt("$key.width", 200)).build())
                    }
                }
                inputKeys.add(key)
            }
        }

        // 3. 处理底部布局 (引入 MultiActionType)
        val bottomSection = config.getConfigurationSection("Bottom")
        val bottomType = bottomSection?.getString("type") ?: "notice"

        val dialogType = when (bottomType) {
            "multi" -> {
                // 矩阵按钮解析
                val actionButtons = mutableListOf<ActionButton>()
                bottomSection?.getConfigurationSection("buttons")?.let { btnSection ->
                    for (btnKey in btnSection.getKeys(false)) {
                        val btnText = btnSection.getString("$btnKey.text") ?: "按钮"
                        actionButtons.add(
                            ActionButton.builder(color(btnText))
                                .action(buildActionFromConfig(player, config, "Bottom.buttons.$btnKey.action", inputKeys))
                                .build()
                        )
                    }
                }

                // 退出/返回按钮
                val exitBtn = bottomSection?.getString("exit.text")?.let { text ->
                    ActionButton.builder(color(text))
                        .action(buildActionFromConfig(player, config, "Bottom.exit.action", inputKeys))
                        .build()
                }

                DialogType.multiAction(actionButtons)
                    .columns(bottomSection?.getInt("columns", 2) ?: 2)
                    .exitAction(exitBtn)
                    .build()
            }

            "confirmation" -> {
                val confirmBtn = ActionButton.builder(color(bottomSection?.getString("confirm.text") ?: "确认"))
                    .action(buildActionFromConfig(player, config, "Bottom.confirm.action", inputKeys))
                    .build()
                val denyBtn = ActionButton.builder(color(bottomSection?.getString("deny.text") ?: "取消"))
                    .action(buildActionFromConfig(player, config, "Bottom.deny.action", inputKeys))
                    .build()
                DialogType.confirmation(confirmBtn, denyBtn)
            }

            else -> { // notice 模式
                val path = if (config.contains("Bottom.confirm.action")) "Bottom.confirm.action" else "Bottom.button1.action"
                val btnText = bottomSection?.getString("confirm.text") ?: bottomSection?.getString("button1.text") ?: "确认"
                val confirmBtn = ActionButton.builder(color(btnText))
                    .action(buildActionFromConfig(player, config, path, inputKeys))
                    .build()
                DialogType.notice(confirmBtn)
            }
        }

        // 4. 显示 Dialog
        val base = DialogBase.builder(title)
            .body(bodyList)
            .inputs(inputList)
            .canCloseWithEscape(canEscape)
            .pause(pauseGame)
            .afterAction(afterAction)
            .build()
        player.showDialog(Dialog.create { it.empty().base(base).type(dialogType) })
    }

    /**
     * 从配置文件中构建一个 DialogAction 对象
     */
    fun buildActionFromConfig(player: Player, config: YamlConfiguration, path: String, inputKeys: List<String>): DialogAction {
        val actionList = config.getStringList(path)
        if (actionList.isEmpty()) return DialogAction.staticAction(ClickEvent.runCommand("/empty"))

        val firstAction = actionList[0]

        // 1. 优先处理不需要服务器参与的静态动作
        if (actionList.size == 1) {
            when {
                firstAction.startsWith("url:") ->
                    return DialogAction.staticAction(ClickEvent.openUrl(firstAction.removePrefix("url:").trim()))
                firstAction.startsWith("copy:") ->
                    return DialogAction.staticAction(ClickEvent.copyToClipboard(firstAction.removePrefix("copy:").trim()))
            }
        }

        // 2. 统一处理所有复杂逻辑 (多行指令、变量、声音等)
        return DialogAction.customClick({ response, _ ->
            val variables = mutableMapOf<String, String>()
            inputKeys.forEach { key ->
                val value = when {
                    response.getFloat(key) != null -> {
                        val f = response.getFloat(key)!!
                        if (f == f.toInt().toFloat()) f.toInt().toString() else f.toString()
                    }
                    response.getText(key) != null -> response.getText(key)
                    response.getBoolean(key) != null -> response.getBoolean(key).toString()
                    else -> ""
                }
                variables[key] = value ?: ""
            }

            // 执行多行动作
            executeActions(player, actionList, variables)
        }, ClickCallback.Options.builder().lifetime(Duration.ofMinutes(5)).build())
    }

    fun executeActions(player: Player, actions: List<String>, variables: Map<String, String>) {
        actions.forEach { action ->
            var finalCmd = action
            variables.forEach { (key, value) ->
                finalCmd = finalCmd.replace("\$($key)", value)
            }

            when {
                finalCmd.startsWith("tell:") ->
                    player.sendMessage(color(finalCmd.removePrefix("tell:").trim()))
                finalCmd.startsWith("cmd:") ->
                    org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), finalCmd.removePrefix("cmd:").trim())
                finalCmd.startsWith("pcmd:") ->
                    player.performCommand(finalCmd.removePrefix("pcmd:").trim())
                finalCmd.startsWith("sound:") -> {
                    val sound = finalCmd.removePrefix("sound:").trim()
                    player.playSound(player.location, sound, 1f, 1f)
                }
            }
        }
    }
}