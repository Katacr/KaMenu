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
    internal fun color(text: String?): Component =
        if (text == null) Component.empty() else serializer.deserialize(text)

    /**
     * 解析变量（PAPI + 内置变量）
     * @param player 玩家对象
     * @param text 原始文本
     * @return 解析后的文本
     */
    private fun resolveVariables(player: Player, text: String): String {
        var result = text

        // 1. 解析内置变量 {data:key} 和 {gdata:key}
        result = result.replace(Regex("\\{data:([^}]+)}")) { matchResult ->
            val key = matchResult.groupValues[1]
            plugin.databaseManager.getPlayerData(player.uniqueId, key)
                ?: plugin.languageManager.getMessage("papi.data_not_found", key)
        }
        result = result.replace(Regex("\\{gdata:([^}]+)}")) { matchResult ->
            val key = matchResult.groupValues[1]
            plugin.databaseManager.getGlobalData(key)
                ?: plugin.languageManager.getMessage("papi.data_not_found", key)
        }

        // 2. 解析 PAPI 变量
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, result)
            } catch (_: Exception) {
                // PAPI 解析失败，忽略
            }
        }

        return result
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
            val value = ConditionUtils.getConditionalValueFromList(player, conditions, defaultValue)
            return resolveVariables(player, value)
        } else {
            // 简单字符串值
            val value = section.getString(path, defaultValue) ?: defaultValue
            return resolveVariables(player, value)
        }
    }

    /**
     * 从 ConfigurationSection 获取适合当前玩家的整数值（支持条件判断）
     * @param player 玩家对象
     * @param section 配置节
     * @param path 配置路径（相对于 section）
     * @param defaultValue 默认值
     * @return 整数值
     */
    private fun getConditionalIntFromSection(player: Player, section: org.bukkit.configuration.ConfigurationSection, path: String, defaultValue: Int = 0): Int {
        if (section.isList(path)) {
            val conditions = section.getList(path) ?: return defaultValue
            val stringValue = ConditionUtils.getConditionalValueFromList(player, conditions, defaultValue.toString())
            val resolved = resolveVariables(player, stringValue)
            return resolved.toIntOrNull() ?: defaultValue
        } else {
            val value = section.getString(path, defaultValue.toString()) ?: defaultValue.toString()
            val resolved = resolveVariables(player, value)
            return resolved.toIntOrNull() ?: defaultValue
        }
    }

    /**
     * 从 ConfigurationSection 获取适合当前玩家的双精度浮点数值（支持条件判断）
     * @param player 玩家对象
     * @param section 配置节
     * @param path 配置路径（相对于 section）
     * @param defaultValue 默认值
     * @return 双精度浮点数值
     */
    private fun getConditionalDoubleFromSection(player: Player, section: org.bukkit.configuration.ConfigurationSection, path: String, defaultValue: Double = 0.0): Double {
        if (section.isList(path)) {
            val conditions = section.getList(path) ?: return defaultValue
            val stringValue = ConditionUtils.getConditionalValueFromList(player, conditions, defaultValue.toString())
            val resolved = resolveVariables(player, stringValue)
            return resolved.toDoubleOrNull() ?: defaultValue
        } else {
            val value = section.getString(path, defaultValue.toString()) ?: defaultValue.toString()
            val resolved = resolveVariables(player, value)
            return resolved.toDoubleOrNull() ?: defaultValue
        }
    }

    /**
     * 从 ConfigurationSection 获取适合当前玩家的布尔值（支持条件判断）
     * @param player 玩家对象
     * @param section 配置节
     * @param path 配置路径（相对于 section）
     * @param defaultValue 默认值
     * @return 布尔值
     */
    private fun getConditionalBooleanFromSection(player: Player, section: org.bukkit.configuration.ConfigurationSection, path: String, defaultValue: Boolean = false): Boolean {
        if (section.isList(path)) {
            val conditions = section.getList(path) ?: return defaultValue
            val stringValue = ConditionUtils.getConditionalValueFromList(player, conditions, defaultValue.toString())
            val resolved = resolveVariables(player, stringValue)
            return resolved.toBooleanStrictOrNull() ?: defaultValue
        } else {
            val value = section.getString(path, defaultValue.toString()) ?: defaultValue.toString()
            val resolved = resolveVariables(player, value)
            return resolved.toBooleanStrictOrNull() ?: defaultValue
        }
    }

    /**
     * 从 ConfigurationSection 获取适合当前玩家的列表值（支持条件判断）
     * @param player 玩家对象
     * @param section 配置节
     * @param path 配置路径（相对于 section）
     * @param defaultValue 默认列表
     * @return 列表值
     */
    private fun getConditionalListFromSection(player: Player, section: org.bukkit.configuration.ConfigurationSection, path: String, defaultValue: List<String> = emptyList()): List<String> {
        if (section.isList(path)) {
            val firstItem = section.getList(path)?.firstOrNull()
            // 检查是否为条件判断格式（第一个元素是 Map）
            if (firstItem is Map<*, *>) {
                val conditions = section.getList(path) ?: return defaultValue
                val list = ConditionUtils.getConditionalListFromList(player, conditions, defaultValue)
                return list.map { resolveVariables(player, it) }
            } else {
                // 普通字符串列表
                val list = section.getStringList(path)
                return list.map { resolveVariables(player, it) }
            }
        } else {
            return defaultValue
        }
    }

    /**
     * 从 ConfigurationSection 获取适合当前玩家的类型值（支持条件判断和 'none'）
     * @param player 玩家对象
     * @param section 配置节
     * @param path 配置路径（相对于 section）
     * @param defaultValue 默认值
     * @return 类型值，如果类型为 'none' 或为空则返回 'none'
     */
    private fun getConditionalTypeFromSection(player: Player, section: org.bukkit.configuration.ConfigurationSection, path: String, defaultValue: String = ""): String {
        val rawValue = getConditionalValueFromSection(player, section, path, defaultValue)
        if (rawValue.isEmpty()) {
            return "none"
        }

        // 检查是否为列表格式（条件判断）
        if (section.isList(path)) {
            val firstItem = section.getList(path)?.firstOrNull()
            // 检查是否为条件判断格式（第一个元素是 Map）
            if (firstItem is Map<*, *>) {
                val conditions = section.getList(path) ?: return "none"
                val result = ConditionUtils.getConditionalValueFromList(player, conditions, defaultValue)
                return if (result.isEmpty()) "none" else result
            }
        }

        // 返回解析后的值
        return rawValue
    }

    fun openMenu(player: Player, menuId: String, manager: MenuManager, plugin: KaMenu) {
        val config = manager.getMenuConfig(menuId)
        if (config == null) {
            player.sendMessage(plugin.languageManager.getMessage("menu.not_found", menuId))
            return
        }
        val rawTitle = getConditionalValue(player, config, "Title", plugin.languageManager.getMessage("ui.default_title"))
        val title = color(resolveVariables(player, rawTitle))

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
                        val text = getConditionalValueFromSection(player, section, "$key.text", "")
                        bodyList.add(DialogBody.plainMessage(color(text)))
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
                        actionButtons.add(
                            ActionButton.builder(color(btnText))
                                .action(MenuActions.buildActionFromConfig(player, config, "Bottom.buttons.$btnKey.actions", inputKeys, inputTypes, checkboxMappings, menuOpener))
                                .build()
                        )
                    }
                }

                // 退出/返回按钮
                val exitBtn = bottomSection?.let { section ->
                    val exitText = getConditionalValueFromSection(player, section, "exit.text", "")
                    if (exitText.isNotEmpty()) {
                        ActionButton.builder(color(exitText))
                            .action(MenuActions.buildActionFromConfig(player, config, "Bottom.exit.actions", inputKeys, inputTypes, checkboxMappings, menuOpener))
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
                    .action(MenuActions.buildActionFromConfig(player, config, "Bottom.confirm.actions", inputKeys, inputTypes, checkboxMappings, menuOpener))
                    .build()
                val denyBtn = ActionButton.builder(color(denyBtnText))
                    .action(MenuActions.buildActionFromConfig(player, config, "Bottom.deny.actions", inputKeys, inputTypes, checkboxMappings, menuOpener))
                    .build()
                DialogType.confirmation(confirmBtn, denyBtn)
            }

            else -> { // notice 模式
                val path = if (config.contains("Bottom.confirm.actions")) "Bottom.confirm.actions" else "Bottom.button1.actions"
                val btnText = bottomSection?.let { getConditionalValueFromSection(player, it, "confirm.text", "") }?.takeIf { it.isNotEmpty() }
                    ?: bottomSection?.let { getConditionalValueFromSection(player, it, "button1.text", "") }?.takeIf { it.isNotEmpty() }
                    ?: "确认"
                val confirmBtn = ActionButton.builder(color(btnText))
                    .action(MenuActions.buildActionFromConfig(player, config, path, inputKeys, inputTypes, checkboxMappings, menuOpener))
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