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

        // 1. 解析 Body (展示内容)
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

                        // 1. 创建描述 (右侧文本)
                        val descriptionText = section.getString("$key.description")
                        val descriptionBody = if (descriptionText != null) {
                            // 创建一个 PlainMessageDialogBody 作为描述
                            DialogBody.plainMessage(color(descriptionText))
                        } else null

                        // 2. 读取尺寸和装饰设置
                        val width = section.getInt("$key.width", 64)   // 物品占据的宽度
                        val height = section.getInt("$key.height", 64) // 物品占据的高度
                        val decorations = section.getBoolean("$key.decorations", true)
                        val tooltip = section.getBoolean("$key.tooltip", true)

                        // 3. 使用源码中的完整构造方法：item(物品, 描述体, 显示装饰, 显示提示, 宽, 高)
                        bodyList.add(DialogBody.item(item, descriptionBody, decorations, tooltip, width, height))
                    }
                }
            }
        }

        // 2. 解析 Inputs (高级交互组件)
        config.getConfigurationSection("Inputs")?.let { section ->
            for (key in section.getKeys(false)) {
                val type = section.getString("$key.type") ?: "text"
                val prompt = color(section.getString("$key.text"))

                when (type) {
                    "checkbox" -> {
                        val onTrue = section.getString("$key.on_true", "true")!!
                        val onFalse = section.getString("$key.on_false", "false")!!
                        val def = section.getBoolean("$key.default", false)

                        val boolInput = DialogInput.bool(key, prompt)
                            .initial(def)
                            .onTrue(onTrue)
                            .onFalse(onFalse)
                            .build()
                        inputList.add(boolInput)
                    }
                    "slider" -> {
                        val start = section.getDouble("$key.min", 0.0).toFloat()
                        val end = section.getDouble("$key.max", 10.0).toFloat()
                        val def = section.getDouble("$key.default", start.toDouble()).toFloat()
                        val step = section.getDouble("$key.step", 1.0).toFloat()

                        // 第一个 %s 是 prompt (label)，第二个 %s 是当前的数字值
                        val format = section.getString("$key.format") ?: "%s: %s"

                        //  numberRange(key, width, label, labelFormat, start, end, initial, step)
                        inputList.add(DialogInput.numberRange(
                            key,
                            250,
                            prompt,
                            format,
                            start,
                            end,
                            def,
                            step
                        ))
                    }
                    "input" -> {
                        val builder = DialogInput.text(key, prompt)
                            .width(section.getInt("$key.width", 250))
                            .initial(section.getString("$key.default", "")!!)
                            .maxLength(section.getInt("$key.max_length", 256))
                            .labelVisible(section.getBoolean("$key.label_visible", true))

                        // 重点：处理多行设置
                        if (section.contains("$key.multiline")) {
                            val maxLines = section.getInt("$key.multiline.max_lines", 5)
                            val height = section.getInt("$key.multiline.height", 100)
                            builder.multiline(TextDialogInput.MultilineOptions.create(maxLines, height))
                        }

                        inputList.add(builder.build())
                    }
                    "dropdown" -> {
                        val entries = section.getStringList("$key.options").map {
                            SingleOptionDialogInput.OptionEntry.create(it, color(it), it == section.getString("$key.default_id"))
                        }
                        val dropdown = DialogInput.singleOption(key, prompt, entries)
                            .width(section.getInt("$key.width", 200))
                            .labelVisible(section.getBoolean("$key.label_visible", true)) // 新增控制
                            .build()
                        inputList.add(dropdown)
                    }
                    else -> {
                        plugin.logger.info("Unsupported input type: $type")
                    }
                }
                inputKeys.add(key)
            }
        }


        // 3. 处理底部按钮布局 (Notice / Confirmation)
        val bottomSection = config.getConfigurationSection("Bottom")
        val bottomType = bottomSection?.getString("type") ?: "notice"

        val dialogType = if (bottomType == "confirmation") {
            // 确认按钮：直接调用 buildActionFromConfig
            val confirmBtn = ActionButton.builder(color(bottomSection?.getString("confirm.text") ?: "确认"))
                .action(buildActionFromConfig(player, config, "Bottom.confirm.action", inputKeys))
                .build()

            // 取消按钮：同样支持自定义动作
            val denyBtn = ActionButton.builder(color(bottomSection?.getString("deny.text") ?: "取消"))
                .action(buildActionFromConfig(player, config, "Bottom.deny.action", inputKeys))
                .build()

            DialogType.confirmation(confirmBtn, denyBtn)
        } else {
            // 单按钮模式 (Notice)
            val path = if (config.contains("Bottom.confirm.action")) "Bottom.confirm.action" else "Bottom.button1.action"
            val btnText = bottomSection?.getString("confirm.text") ?: bottomSection?.getString("button1.text") ?: "确认"

            val confirmBtn = ActionButton.builder(color(btnText))
                .action(buildActionFromConfig(player, config, path, inputKeys))
                .build()
            DialogType.notice(confirmBtn)
        }

        // 4. 显示 Dialog
        val base = DialogBase.builder(title).body(bodyList).inputs(inputList).build()
        player.showDialog(Dialog.create { it.empty().base(base).type(dialogType) })

    }

    /**
     * 从配置文件中构建一个 DialogAction 对象
     */
    fun buildActionFromConfig(player: Player, config: YamlConfiguration, path: String, inputKeys: List<String>): DialogAction {
        val actionList = config.getStringList(path)

        return DialogAction.customClick({ response, _ ->
            val variables = mutableMapOf<String, String>()

            inputKeys.forEach { key ->
                val value = when {
                    // 1. 优先尝试滑块数值
                    response.getFloat(key) != null -> {
                        val f = response.getFloat(key)!!
                        if (f == f.toInt().toFloat()) f.toInt().toString() else f.toString()
                    }
                    // 2. 尝试文本 (文本框、下拉框)
                    response.getText(key) != null -> response.getText(key)
                    // 3. 尝试布尔值 (勾选框)
                    response.getBoolean(key) != null -> response.getBoolean(key).toString()
                    else -> ""
                }
                variables[key] = value ?: ""
            }
            executeActions(player, actionList, variables)
        }, ClickCallback.Options.builder().lifetime(Duration.ofMinutes(5)).build())
    }

    fun executeActions(player: Player, actions: List<String>, variables: Map<String, String>) {
        actions.forEach { action ->
            var finalCmd = action
            variables.forEach { (key, value) ->
                finalCmd = finalCmd.replace("\${$key.input}", value)
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