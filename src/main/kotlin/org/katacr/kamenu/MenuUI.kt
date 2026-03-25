@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput
import io.papermc.paper.registry.data.dialog.input.TextDialogInput
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.katacr.kamenu.ConditionUtils.getConditionalBooleanFromSection
import org.katacr.kamenu.ConditionUtils.getConditionalDoubleFromSection
import org.katacr.kamenu.ConditionUtils.getConditionalIntFromSection
import org.katacr.kamenu.ConditionUtils.getConditionalListFromSection
import org.katacr.kamenu.ConditionUtils.getConditionalTypeFromSection
import org.katacr.kamenu.ConditionUtils.getConditionalValueFromSection

object MenuUI {
    private val serializer = LegacyComponentSerializer.legacyAmpersand()
    private lateinit var plugin: KaMenu

    /**
     * 初始化插件引用
     */
    fun init(kaMenu: KaMenu) {
        this.plugin = kaMenu
    }

    /**
     * 将颜色代码转换为 Adventure Component
     */
    fun color(text: String?): Component =
        if (text == null) Component.empty() else serializer.deserialize(text)

    /**
     * 创建带有点击和悬停事件的消息组件
     * @param player 玩家对象
     * @param section 配置节
     * @param path 配置路径
     * @param defaultText 默认文本
     * @return 带有事件的组件
     */
    private fun createMessageComponent(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultText: String
    ): Component {
        // 获取文本内容：支持 hovertext 语法和普通的 text 字段
        val rawText = if (section.isList(path)) {
            val conditions = section.getList(path)
            ConditionUtils.getConditionalValueFromList(player, conditions ?: emptyList<Any>(), defaultText)
        } else {
            // 直接获取原始值，避免配置系统将 text 误认为是配置节
            section.getString(path, defaultText) ?: defaultText
        }

        // 使用 MenuActions 的 parseClickableText 解析文本，支持 <text=...;hover=...;command=...;url=...> 语法
        return MenuActions.parseClickableText(rawText)
    }

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

    fun openMenu(player: Player, menuId: String, manager: MenuManager, plugin: KaMenu) {
        val config = manager.getMenuConfig(menuId)
        if (config == null) {
            player.sendMessage(plugin.languageManager.getMessage("menu.not_found", menuId))
            return
        }

        // 0. 执行 Open 事件（在菜单打开前执行）
        val shouldStop = MenuActions.executeEvent(player, config, "Open")
        if (shouldStop) {
            // 如果Open事件中遇到return，停止打开菜单
            return
        }

        val rawTitle = getConditionalValue(player, config, "Title", plugin.languageManager.getMessage("ui.default_title"))
        val title = color(ConditionUtils.resolveVariables(player, rawTitle))

        val bodyList = mutableListOf<DialogBody>()
        val inputList = mutableListOf<DialogInput>()
        val inputKeys = mutableListOf<String>()
        val inputTypes = mutableMapOf<String, String>()  // 记录输入类型
        val checkboxMappings = mutableMapOf<String, Pair<String, String>>()  // 记录 checkbox 的 on_true/on_false 映射

        // 0. 解析设置
        val canEscape = config.getBoolean("Settings.can_escape", true)
        val pauseGame = config.getBoolean("Settings.pause", false) // 仅单机有效，一般设为 false
        val afterActionStr = config.getString("Settings.after_action", "CLOSE")?.uppercase() ?: "CLOSE"

        val afterAction = try {
            DialogBase.DialogAfterAction.valueOf(afterActionStr)
        } catch (_: Exception) {
            DialogBase.DialogAfterAction.CLOSE
        }

        // 1. 解析 Body
        config.getConfigurationSection("Body")?.let { section ->
            for (key in section.getKeys(false)) {
                val type = getConditionalTypeFromSection(player, section, "$key.type", "")
                // 如果类型为 'none'，跳过此组件
                if (type == "none") continue

                when (type) {
                    "message" -> {
                        val component = createMessageComponent(player, section, "$key.text", "")
                        val width = getConditionalIntFromSection(player, section, "$key.width", 0)
                        if (width > 0) {
                            bodyList.add(DialogBody.plainMessage(component, width))
                        } else {
                            bodyList.add(DialogBody.plainMessage(component))
                        }
                    }
                    "item" -> {
                        val materialStr = getConditionalValueFromSection(player, section, "$key.material", "PAPER")
                        val material = Material.matchMaterial(materialStr) ?: Material.PAPER
                        val item = ItemStack(material)
                        item.editMeta { meta ->
                            val name = getConditionalValueFromSection(player, section, "$key.name", "")
                            meta.displayName(color(name))
                            val lore = getConditionalListFromSection(player, section, "$key.lore")
                            meta.lore(lore.map { color(it) })
                        }
                        val descriptionText = getConditionalValueFromSection(player, section, "$key.description", "")
                        val descriptionBody = descriptionText.takeIf { it.isNotEmpty() }?.let { DialogBody.plainMessage(color(it)) }

                        val width = getConditionalIntFromSection(player, section, "$key.width", 16)
                        val height = getConditionalIntFromSection(player, section, "$key.height", 16)
                        val decorations = getConditionalBooleanFromSection(player, section, "$key.decorations", true)
                        val tooltip = getConditionalBooleanFromSection(player, section, "$key.tooltip", true)

                        bodyList.add(DialogBody.item(item, descriptionBody, decorations, tooltip, width, height))
                    }
                }
            }
        }

        // 2. 解析 Inputs
        config.getConfigurationSection("Inputs")?.let { section ->
            for (key in section.getKeys(false)) {
                val type = getConditionalTypeFromSection(player, section, "$key.type", "text")
                // 如果类型为 'none'，跳过此组件
                if (type == "none") continue

                val prompt = color(getConditionalValueFromSection(player, section, "$key.text", ""))

                when (type) {
                    "checkbox" -> {
                        inputTypes[key] = "checkbox"  // 记录为布尔类型
                        val onTrue = getConditionalValueFromSection(player, section, "$key.on_true", "true")
                        val onFalse = getConditionalValueFromSection(player, section, "$key.on_false", "false")
                        checkboxMappings[key] = Pair(onTrue, onFalse)  // 记录映射
                        inputList.add(DialogInput.bool(key, prompt)
                            .initial(getConditionalBooleanFromSection(player, section, "$key.default", false))
                            .onTrue(onTrue)
                            .onFalse(onFalse)
                            .build())
                    }
                    "slider" -> {
                        inputTypes[key] = "number"  // 记录为数值类型
                        val start = getConditionalDoubleFromSection(player, section, "$key.min", 0.0).toFloat()
                        val end = getConditionalDoubleFromSection(player, section, "$key.max", 10.0).toFloat()
                        inputList.add(DialogInput.numberRange(
                            key, 250, prompt,
                            getConditionalValueFromSection(player, section, "$key.format", "%s: %s"),
                            start, end, getConditionalDoubleFromSection(player, section, "$key.default", start.toDouble()).toFloat(),
                            getConditionalDoubleFromSection(player, section, "$key.step", 1.0).toFloat()
                        ))
                    }
                    "input" -> {
                        inputTypes[key] = "text"  // 记录为文本类型
                        val builder = DialogInput.text(key, prompt)
                            .width(getConditionalIntFromSection(player, section, "$key.width", 250))
                            .initial(getConditionalValueFromSection(player, section, "$key.default", ""))
                            .maxLength(getConditionalIntFromSection(player, section, "$key.max_length", 256))

                        if (section.contains("$key.multiline")) {
                            builder.multiline(TextDialogInput.MultilineOptions.create(
                                getConditionalIntFromSection(player, section, "$key.multiline.max_lines", 5),
                                getConditionalIntFromSection(player, section, "$key.multiline.height", 100)
                            ))
                        }
                        inputList.add(builder.build())
                    }
                    "dropdown" -> {
                        inputTypes[key] = "text"  // 记录为文本类型
                        val defaultId = getConditionalValueFromSection(player, section, "$key.default_id", "")
                        val options = getConditionalListFromSection(player, section, "$key.options")
                        val entries = options.map {
                            SingleOptionDialogInput.OptionEntry.create(it, color(it), it == defaultId)
                        }
                        inputList.add(DialogInput.singleOption(key, prompt, entries).width(getConditionalIntFromSection(player, section, "$key.width", 200)).build())
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
                    openMenu(p, menuName, plugin.menuManager, plugin)
                })
            }
        }

        val dialogType = when (bottomType) {
            "multi" -> {
                // 矩阵按钮解析
                val actionButtons = mutableListOf<ActionButton>()
                bottomSection?.getConfigurationSection("buttons")?.let { btnSection ->
                    for (btnKey in btnSection.getKeys(false)) {
                        // 检查按钮显示条件（兼容 show-condition 和 show_condition 两种写法）
                        val showCondition = btnSection.getString("$btnKey.show-condition") ?: btnSection.getString("$btnKey.show_condition")

                        if (showCondition != null && !ConditionUtils.checkCondition(player, showCondition)) {
                            // 条件不满足，不显示此按钮
                            continue
                        }

                        val btnText = getConditionalValueFromSection(player, btnSection, "$btnKey.text", "按钮")
                        val btnWidth = getConditionalIntFromSection(player, btnSection, "$btnKey.width", 0)

                        val builder = ActionButton.builder(color(btnText))
                            .action(MenuActions.buildActionFromConfig(player, config, "Bottom.buttons.$btnKey.actions", inputKeys, inputTypes, checkboxMappings, menuOpener))

                        // 如果设置了宽度（width > 0），则应用宽度设置
                        if (btnWidth > 0) {
                            builder.width(btnWidth)
                        }

                        actionButtons.add(builder.build())
                    }
                }

                // 退出/返回按钮
                val exitBtn = bottomSection?.let { section ->
                    val exitText = getConditionalValueFromSection(player, section, "exit.text", "")
                    if (exitText.isNotEmpty()) {
                        val exitWidth = getConditionalIntFromSection(player, section, "exit.width", 0)
                        val builder = ActionButton.builder(color(exitText))
                            .action(MenuActions.buildActionFromConfig(player, config, "Bottom.exit.actions", inputKeys, inputTypes, checkboxMappings, menuOpener))

                        // 如果设置了宽度（width > 0），则应用宽度设置
                        if (exitWidth > 0) {
                            builder.width(exitWidth)
                        }

                        builder.build()
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

                val confirmWidth = bottomSection?.let { getConditionalIntFromSection(player, it, "confirm.width", 0) } ?: 0
                val denyWidth = bottomSection?.let { getConditionalIntFromSection(player, it, "deny.width", 0) } ?: 0

                val confirmBuilder = ActionButton.builder(color(confirmBtnText))
                    .action(MenuActions.buildActionFromConfig(player, config, "Bottom.confirm.actions", inputKeys, inputTypes, checkboxMappings, menuOpener))
                if (confirmWidth > 0) {
                    confirmBuilder.width(confirmWidth)
                }
                val confirmBtn = confirmBuilder.build()

                val denyBuilder = ActionButton.builder(color(denyBtnText))
                    .action(MenuActions.buildActionFromConfig(player, config, "Bottom.deny.actions", inputKeys, inputTypes, checkboxMappings, menuOpener))
                if (denyWidth > 0) {
                    denyBuilder.width(denyWidth)
                }
                val denyBtn = denyBuilder.build()

                DialogType.confirmation(confirmBtn, denyBtn)
            }

            else -> { // notice 模式
                val path = if (config.contains("Bottom.confirm.actions")) "Bottom.confirm.actions" else "Bottom.button1.actions"
                val btnText = bottomSection?.let { getConditionalValueFromSection(player, it, "confirm.text", "") }?.takeIf { it.isNotEmpty() }
                    ?: bottomSection?.let { getConditionalValueFromSection(player, it, "button1.text", "") }?.takeIf { it.isNotEmpty() }
                    ?: "确认"

                val widthPath = if (config.contains("Bottom.confirm.width")) "Bottom.confirm.width" else "Bottom.button1.width"
                val confirmWidth = getConditionalIntFromSection(player, config, widthPath, 0)

                val builder = ActionButton.builder(color(btnText))
                    .action(MenuActions.buildActionFromConfig(player, config, path, inputKeys, inputTypes, checkboxMappings, menuOpener))

                if (confirmWidth > 0) {
                    builder.width(confirmWidth)
                }

                val confirmBtn = builder.build()
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