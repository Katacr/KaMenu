@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput
import io.papermc.paper.registry.data.dialog.input.TextDialogInput
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object MenuUI {
    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    /**
     * 将颜色代码转换为 Adventure Component
     */
    internal fun color(text: String?): Component =
        if (text == null) Component.empty() else serializer.deserialize(text)

    /**
     * 获取配置路径下适合当前玩家的值（支持条件判断）
     * @param player 玩家对象
     * @param config 菜单配置
     * @param path 配置路径
     * @param defaultValue 默认值
     * @return 字符串值
     */
    internal fun getConditionalValue(player: Player, config: YamlConfiguration, path: String, defaultValue: String = ""): String {
        // 检查该路径下是否为列表格式（条件判断）
        if (config.isList(path)) {
            val conditions = config.getList(path) ?: return defaultValue
            return ConditionUtils.getConditionalValueFromList(player, conditions, defaultValue)
        } else {
            // 简单字符串值
            return config.getString(path, defaultValue) ?: defaultValue
        }
    }

    /**
     * 从 ConfigurationSection 获取适合当前玩家的值（支持条件判断）
     * @param player 玩家对象
     * @param section 配置节
     * @param path 配置路径（相对于 section）
     * @param defaultValue 默认值
     * @return 字符串值
     */
    private fun getConditionalValueFromSection(player: Player, section: org.bukkit.configuration.ConfigurationSection, path: String, defaultValue: String = ""): String {
        // 检查该路径下是否为列表格式（条件判断）
        if (section.isList(path)) {
            val conditions = section.getList(path) ?: return defaultValue
            return ConditionUtils.getConditionalValueFromList(player, conditions, defaultValue)
        } else {
            // 简单字符串值
            return section.getString(path, defaultValue) ?: defaultValue
        }
    }

    fun openMenu(player: Player, menuId: String, manager: MenuManager) {
        val config = manager.getMenuConfig(menuId)
        if (config == null) {
            player.sendMessage("§c[KaMenu] 菜单 '$menuId' 不存在！")
            return
        }
        val title = color(getConditionalValue(player, config, "Title", "KaMenu"))

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

        // 1. 解析 Body
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

        // 2. 解析 Inputs
        config.getConfigurationSection("Inputs")?.let { section ->
            for (key in section.getKeys(false)) {
                val type = section.getString("$key.type") ?: "text"
                val prompt = color(getConditionalValueFromSection(player, section, "$key.text", ""))

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

        // 3. 处理底部布局
        val bottomSection = config.getConfigurationSection("Bottom")
        val bottomType = bottomSection?.getString("type") ?: "notice"

        // 定义菜单打开器
        val menuOpener: (Player, String) -> Unit = { p, menuName ->
            val plugin = Bukkit.getPluginManager().getPlugin("KaMenu") as? KaMenu
            if (plugin != null) {
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    openMenu(p, menuName, plugin.menuManager)
                })
            }
        }

        val dialogType = when (bottomType) {
            "multi" -> {
                // 矩阵按钮解析
                val actionButtons = mutableListOf<ActionButton>()
                bottomSection?.getConfigurationSection("buttons")?.let { btnSection ->
                    for (btnKey in btnSection.getKeys(false)) {
                        val btnText = getConditionalValueFromSection(player, btnSection, "$btnKey.text", "按钮")
                        actionButtons.add(
                            ActionButton.builder(color(btnText))
                                .action(MenuActions.buildActionFromConfig(player, config, "Bottom.buttons.$btnKey.actions", inputKeys, menuOpener))
                                .build()
                        )
                    }
                }

                // 退出/返回按钮
                val exitBtn = bottomSection?.let { section ->
                    val exitText = getConditionalValueFromSection(player, section, "exit.text", "")
                    if (exitText.isNotEmpty()) {
                        ActionButton.builder(color(exitText))
                            .action(MenuActions.buildActionFromConfig(player, config, "Bottom.exit.actions", inputKeys, menuOpener))
                            .build()
                    } else null
                }

                DialogType.multiAction(actionButtons)
                    .columns(bottomSection?.getInt("columns", 2) ?: 2)
                    .exitAction(exitBtn)
                    .build()
            }

            "confirmation" -> {
                val confirmBtnText = bottomSection?.let { getConditionalValueFromSection(player, it, "confirm.text", "确认") } ?: "确认"
                val denyBtnText = bottomSection?.let { getConditionalValueFromSection(player, it, "deny.text", "取消") } ?: "取消"

                val confirmBtn = ActionButton.builder(color(confirmBtnText))
                    .action(MenuActions.buildActionFromConfig(player, config, "Bottom.confirm.actions", inputKeys, menuOpener))
                    .build()
                val denyBtn = ActionButton.builder(color(denyBtnText))
                    .action(MenuActions.buildActionFromConfig(player, config, "Bottom.deny.actions", inputKeys, menuOpener))
                    .build()
                DialogType.confirmation(confirmBtn, denyBtn)
            }

            else -> { // notice 模式
                val path = if (config.contains("Bottom.confirm.actions")) "Bottom.confirm.actions" else "Bottom.button1.actions"
                val btnText = bottomSection?.let { getConditionalValueFromSection(player, it, "confirm.text", "") }?.takeIf { it.isNotEmpty() }
                    ?: bottomSection?.let { getConditionalValueFromSection(player, it, "button1.text", "") }?.takeIf { it.isNotEmpty() }
                    ?: "确认"
                val confirmBtn = ActionButton.builder(color(btnText))
                    .action(MenuActions.buildActionFromConfig(player, config, path, inputKeys, menuOpener))
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
}