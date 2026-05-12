@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput
import io.papermc.paper.registry.data.dialog.input.TextDialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.katacr.kamenu.ConditionUtils.getBoolean
import org.katacr.kamenu.ConditionUtils.getDouble
import org.katacr.kamenu.ConditionUtils.getFirstConditionString
import org.katacr.kamenu.ConditionUtils.getInt
import org.katacr.kamenu.ConditionUtils.getString
import org.katacr.kamenu.ConditionUtils.getStringList
import org.katacr.kamenu.ConditionUtils.getType
import java.util.concurrent.CompletableFuture

object MenuUI {
    private lateinit var plugin: KaMenu

    /**
     * 初始化插件引用
     */
    fun init(kaMenu: KaMenu) {
        this.plugin = kaMenu
    }

    /**
     * 创建带有点击和悬停事件的消息组件
     * 支持多种模式：
     * 1. 纯文本列表模式：每行一个字符串
     * 2. 条件判断模式：支持 allow/deny 分支（字符串或列表）
     * 3. 单行文本模式：支持 \n 换行符
     * @param player 玩家对象
     * @param section 配置节
     * @param path 配置路径
     * @param defaultText 默认文本
     * @param config 菜单配置（用于加载 actions）
     * @param menuOpener 菜单打开函数
     * @return 带有事件的组件
     */
     fun createMessageComponent(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultText: String,
        config: YamlConfiguration? = null,
        menuOpener: ((Player, String) -> Unit)? = null
    ): Component {
        // 获取文本内容（支持列表和字符串）
        val rawText = getString(player, section, path, defaultText)

        // 先解析变量，再使用 parseClickableText 支持 hovertext 和 MiniMessage 语法
        return MenuActions.parseClickableText(
            ConditionUtils.resolveVariables(player, rawText),
            player,
            config,
            menuOpener
        )
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
            return getFirstConditionString(player, conditions, defaultValue)
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

        // 0. 检查 Events.Open 是否包含 wait 动作
        val openActions = config.getList("Events.Open")
        val hasWait = MenuActions.hasWaitActionInList(openActions ?: emptyList<Any>())

        if (hasWait) {
            // 有 wait 动作，整个打开过程异步执行
            openMenuAsync(player, config, manager, plugin)
        } else {
            // 没有 wait 动作，同步执行 Open 事件
            val shouldStop = MenuActions.executeEventSync(player, config, "Open")
            if (shouldStop) {
                // 如果Open事件中遇到return，停止打开菜单
                return
            }
            // 继续打开菜单
            openMenuInternal(player, config, plugin, menuId)
        }
    }

    /**
     * 异步打开菜单（用于包含 wait 动作的 Events.Open）
     */
    private fun openMenuAsync(player: Player, config: YamlConfiguration, manager: MenuManager, plugin: KaMenu) {
        // 异步执行 Open 事件
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val eventFuture: CompletableFuture<Boolean> = MenuActions.executeEvent(player, config, "Open")
            try {
                val shouldStop: Boolean = eventFuture.get()
                if (shouldStop) {
                    // 如果Open事件中遇到return，停止打开菜单
                    return@Runnable
                }
            } catch (e: Exception) {
                plugin.logger.severe("Open 事件执行失败: ${e.message}")
                e.printStackTrace()
                return@Runnable
            }

            // Open 事件执行完成，切换到主线程打开菜单
            Bukkit.getScheduler().runTask(plugin, Runnable {
                openMenuInternal(player, config, plugin, "")
            })
        })
    }

    /**
     * 强制打开菜单（不执行 Events.Open 动作列表）
     */
    fun forceOpenMenu(player: Player, menuId: String, manager: MenuManager, plugin: KaMenu) {
        val config = manager.getMenuConfig(menuId)
        if (config == null) {
            player.sendMessage(plugin.languageManager.getMessage("menu.not_found", menuId))
            return
        }
        openMenuInternal(player, config, plugin, menuId)
    }

    /**
     * 实际打开菜单的内部方法（在主线程执行）
     */
    private fun openMenuInternal(player: Player, config: YamlConfiguration, plugin: KaMenu, menuId: String) {

        // 检查 PlaceholderAPI 扩展依赖
        val needPlaceholderExtensions = config.getList("Settings.need_placeholder")
        if (needPlaceholderExtensions != null && needPlaceholderExtensions.isNotEmpty()) {
            val papiPlugin = Bukkit.getPluginManager().getPlugin("PlaceholderAPI")
            
            if (papiPlugin == null || !papiPlugin.isEnabled) {
                // PlaceholderAPI 未加载，阻止打开菜单
                if (player.hasPermission("kamenu.admin")) {
                    val prefix = plugin.languageManager.getMessage("menu.missing_papi_extensions_prefix")
                    val extensionsClickable = needPlaceholderExtensions.filterIsInstance<String>()
                        .joinToString(", ") { ext ->
                            val command = "/papi ecloud download $ext"
                            "<text=&e[$ext];hover=&7$command;command=$command>"
                        }
                    player.sendMessage(MenuActions.parseClickableText("$prefix $extensionsClickable"))
                } else {
                    player.sendMessage(plugin.languageManager.getMessage("menu.missing_dependencies"))
                }
                return
            } else {
                // 检查所需的扩展是否已加载
                val missingExtensions = mutableListOf<String>()
                for (extension in needPlaceholderExtensions) {
                    if (extension is String) {
                        try {
                            if (!me.clip.placeholderapi.PlaceholderAPI.isRegistered(extension)) {
                                missingExtensions.add(extension)
                            }
                        } catch (e: Exception) {
                            missingExtensions.add(extension)
                        }
                    }
                }
                
                if (missingExtensions.isNotEmpty()) {
                    // 扩展未全部加载，阻止打开菜单
                    if (player.hasPermission("kamenu.admin")) {
                        val prefix = plugin.languageManager.getMessage("menu.missing_papi_extensions_prefix")
                        val extensionsClickable = missingExtensions.joinToString(", ") { ext ->
                            val command = "/papi ecloud download $ext"
                            "<text=&e[$ext];hover=&7$command;command=$command>"
                        }
                        player.sendMessage(MenuActions.parseClickableText("$prefix $extensionsClickable"))
                    } else {
                        player.sendMessage(plugin.languageManager.getMessage("menu.missing_dependencies"))
                    }
                    return
                }
            }
        }

        val rawTitle = getConditionalValue(player, config, "Title", plugin.languageManager.getMessage("ui.default_title"))
        val title = TextParser.parseText(ConditionUtils.resolveVariables(player, rawTitle))

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

        // 定义菜单打开器（需要在解析 Body 之前定义，因为 message 组件可能需要它）
        val menuOpener: (Player, String) -> Unit = { p, menuName ->
            val pluginRef = Bukkit.getPluginManager().getPlugin("KaMenu") as? KaMenu
            if (pluginRef != null) {
                Bukkit.getScheduler().runTask(pluginRef, Runnable {
                    openMenu(p, menuName, pluginRef.menuManager, pluginRef)
                })
            }
        }

        // 1. 解析 Body
        config.getConfigurationSection("Body")?.let { section ->
            for (key in section.getKeys(false)) {
                val type = getType(player, section, "$key.type", "")
                // 如果类型为 'none'，跳过此组件
                if (type == "none") continue

                when (type) {
                    "message" -> {
                        val component = createMessageComponent(player, section, "$key.text", "", config, menuOpener)
                        val width = getInt(player, section, "$key.width", 0)
                        if (width > 0) {
                            bodyList.add(DialogBody.plainMessage(component, width))
                        } else {
                            bodyList.add(DialogBody.plainMessage(component))
                        }
                    }
                    "item" -> {
                        val materialStr = ConditionUtils.resolveVariables(player, getString(player, section, "$key.material", "PAPER"))
                        val item: ItemStack
                        
                        // 检查是否为槽位引用格式 [SLOT] 或 [SLOT:Player]
                        if (materialStr.startsWith("[") && materialStr.endsWith("]")) {
                            val slotRef = materialStr.substring(1, materialStr.length - 1)
                            val parts = slotRef.split(":")
                            val slotName = parts[0].uppercase()
                            val targetPlayer = if (parts.size > 1) {
                                Bukkit.getPlayer(parts[1])
                            } else {
                                player
                            }
                            
                            if (targetPlayer == null || !targetPlayer.isOnline) {
                                plugin.logger.warning("Item 槽位引用失败: 玩家 ${parts.getOrElse(1) { "null" }} 不在线。菜单: ${config.getString("Title", "")}")
                                // 使用默认物品
                                item = ItemStack(Material.PAPER)
                            } else {
                                val slotItem = when (slotName) {
                                    "HEAD" -> targetPlayer.inventory.helmet
                                    "CHEST" -> targetPlayer.inventory.chestplate
                                    "LEGGINGS" -> targetPlayer.inventory.leggings
                                    "BOOTS" -> targetPlayer.inventory.boots
                                    "MAINHAND" -> targetPlayer.inventory.itemInMainHand
                                    "OFFHAND" -> targetPlayer.inventory.itemInOffHand
                                    else -> {
                                        plugin.logger.warning("未知的槽位名称: $slotName。菜单: ${config.getString("Title", "")}")
                                        null
                                    }
                                }
                                
                                item = if (slotItem != null && !slotItem.isEmpty) {
                                    slotItem
                                } else if (slotName == "HEAD") {
                                    // 头部为空，渲染玩家皮肤头颅
                                    val skull = ItemStack(Material.PLAYER_HEAD)
                                    skull.editMeta { meta ->
                                        val skullMeta = meta as org.bukkit.inventory.meta.SkullMeta
                                        skullMeta.owningPlayer = targetPlayer
                                    }
                                    skull
                                } else {
                                    // 其他槽位为空，渲染浅灰色玻璃板，name设为"无"
                                    val glass = ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                                    glass.editMeta { meta ->
                                        meta.displayName(TextParser.parseText("无"))
                                    }
                                    glass
                                }
                                
                                // 槽位引用模式下，跳过其他属性设置（lore、item_model等）
                                val width = getInt(player, section, "$key.width", 16)
                                val height = getInt(player, section, "$key.height", 16)
                                val showOverlays = getBoolean(player, section, "$key.show_overlays", true)
                                val showTooltip = getBoolean(player, section, "$key.show_tooltip", true)
                                
                                bodyList.add(DialogBody.item(item, null, showOverlays, showTooltip, width, height))
                                continue  // 跳过后续处理
                            }
                        } else {
                            // 正常物品创建流程
                            val material = MaterialUtils.matchMaterial(materialStr) ?: Material.PAPER
                            val amount = getInt(player, section, "$key.amount", 1)
                            item = ItemStack(material, amount)

                            item.editMeta { meta ->
                                val name = ConditionUtils.resolveVariables(player, getString(player, section, "$key.name", ""))
                                if (name.isNotEmpty()) {
                                    meta.displayName(TextParser.parseText(name))
                                }
                                val lore = getStringList(player, section, "$key.lore")
                                if (lore.isNotEmpty()) {
                                    meta.lore(lore.map { TextParser.parseText(it) })
                                }
                                
                                // 支持设置 item_model（1.21.7+ 命名空间物品模型）
                                val itemModel = ConditionUtils.resolveVariables(player, getString(player, section, "$key.item_model", ""))
                                if (itemModel.isNotEmpty()) {
                                    val namespacedKey = org.bukkit.NamespacedKey.minecraft("item_model")
                                    val pdc = meta.persistentDataContainer
                                    pdc.set(namespacedKey, org.bukkit.persistence.PersistentDataType.STRING, itemModel)
                                }
                            }
                        }
                        val descriptionText = ConditionUtils.resolveVariables(player, getString(player, section, "$key.description", ""))
                        val descriptionWidth = getInt(player, section, "$key.description_width", 0)
                        val descriptionBody = descriptionText.takeIf { it.isNotEmpty() }?.let {
                            if (descriptionWidth > 0) {
                                DialogBody.plainMessage(MenuActions.parseClickableText(it), descriptionWidth)
                            } else {
                                DialogBody.plainMessage(MenuActions.parseClickableText(it))
                            }
                        }

                        val width = getInt(player, section, "$key.width", 16)
                        val height = getInt(player, section, "$key.height", 16)
                        val showOverlays = getBoolean(player, section, "$key.show_overlays", true)
                        val showTooltip = getBoolean(player, section, "$key.show_tooltip", true)

                        bodyList.add(DialogBody.item(item, descriptionBody, showOverlays, showTooltip, width, height))
                    }
                }
            }
        }

        // 2. 解析 Inputs
        config.getConfigurationSection("Inputs")?.let { section ->
            for (key in section.getKeys(false)) {
                val type = getType(player, section, "$key.type", "text")
                // 如果类型为 'none'，跳过此组件
                if (type == "none") continue

                val prompt = TextParser.parseText(getString(player, section, "$key.text", ""))

                when (type) {
                    "checkbox" -> {
                        inputTypes[key] = "checkbox"  // 记录为布尔类型
                        val onTrue = getString(player, section, "$key.on_true", "true")
                        val onFalse = getString(player, section, "$key.on_false", "false")
                        checkboxMappings[key] = Pair(onTrue, onFalse)  // 记录映射
                        inputList.add(DialogInput.bool(key, prompt)
                            .initial(getBoolean(player, section, "$key.default", false))
                            .onTrue(onTrue)
                            .onFalse(onFalse)
                            .build())
                    }
                    "slider" -> {
                        inputTypes[key] = "number"  // 记录为数值类型
                        val start = getDouble(player, section, "$key.min", 0.0).toFloat()
                        val end = getDouble(player, section, "$key.max", 10.0).toFloat()

                        // 验证 min 必须小于 max
                        val (effectiveMin, effectiveMax) = if (start >= end) {
                            plugin.logger.warning(plugin.languageManager.getMessage("ui.slider_invalid_config", menuId, key, start, end))
                            Pair(0.0f, 10.0f)  // 使用默认值
                        } else {
                            Pair(start, end)
                        }

                        inputList.add(DialogInput.numberRange(
                            key, 250, prompt,
                            getString(player, section, "$key.format", "%s: %s"),
                            effectiveMin, effectiveMax, getDouble(player, section, "$key.default", effectiveMin.toDouble()).toFloat(),
                            getDouble(player, section, "$key.step", 1.0).toFloat()
                        ))
                    }
                    "input" -> {
                        inputTypes[key] = "text"  // 记录为文本类型
                        val builder = DialogInput.text(key, prompt)
                            .width(getInt(player, section, "$key.width", 250))
                            .labelVisible(!getBoolean(player, section, "$key.hide_text", false))
                            .initial(getString(player, section, "$key.default", ""))
                            .maxLength(getInt(player, section, "$key.max_length", 256))

                        if (section.contains("$key.multiline")) {
                            builder.multiline(TextDialogInput.MultilineOptions.create(
                                getInt(player, section, "$key.multiline.max_lines", 5),
                                getInt(player, section, "$key.multiline.height", 100)
                            ))
                        }
                        inputList.add(builder.build())
                    }
                    "dropdown" -> {
                        inputTypes[key] = "text"  // 记录为文本类型
                        val defaultId = getString(player, section, "$key.default_id", "")
                        val options = getStringList(player, section, "$key.options")
                        val entries = options.map {
                            SingleOptionDialogInput.OptionEntry.create(it, TextParser.parseText(it), it == defaultId)
                        }
                        inputList.add(
                            DialogInput.singleOption(key, prompt, entries)
                                .width(getInt(player, section, "$key.width", 200))
                                .labelVisible(!getBoolean(player, section, "$key.hide_text", false))
                                .build()
                        )
                    }
                }
                inputKeys.add(key)
            }
        }

        // 3. 处理底部布局
        val bottomSection = config.getConfigurationSection("Bottom")
        val bottomType = bottomSection?.getString("type") ?: "notice"

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

                        val btnText = getString(player, btnSection, "$btnKey.text", "按钮")
                        val btnWidth = getInt(player, btnSection, "$btnKey.width", 0)

                        val builder = ActionButton.builder(TextParser.parseText(btnText))
                            .action(MenuActions.buildActionFromConfig(player, config, "Bottom.buttons.$btnKey.actions", inputKeys, inputTypes, checkboxMappings, menuOpener))

                        // 读取 tooltip 配置
                        val tooltipList = getStringList(player, btnSection, "$btnKey.tooltip")
                        if (tooltipList.isNotEmpty()) {
                            // 将 tooltip 列表转换为 Component，每个元素作为一行
                            val tooltipComponent = Component.join(Component.newline(), *tooltipList.map { TextParser.parseText(it) }.toTypedArray())
                            builder.tooltip(tooltipComponent)
                        }

                        // 如果设置了宽度（width > 0），则应用宽度设置
                        if (btnWidth > 0) {
                            builder.width(btnWidth)
                        }

                        actionButtons.add(builder.build())
                    }
                }

                // 退出/返回按钮
                val exitBtn = bottomSection?.let { section ->
                    val exitText = getString(player, section, "exit.text", "")
                    if (exitText.isNotEmpty()) {
                        val exitWidth = getInt(player, section, "exit.width", 0)
                        val builder = ActionButton.builder(TextParser.parseText(exitText))
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
                val confirmBtnText = bottomSection?.let { getString(player, it, "confirm.text", "确认") } ?: "确认"
                val denyBtnText = bottomSection?.let { getString(player, it, "deny.text", "取消") } ?: "取消"

                val confirmWidth = bottomSection?.let { getInt(player, it, "confirm.width", 0) } ?: 0
                val denyWidth = bottomSection?.let { getInt(player, it, "deny.width", 0) } ?: 0

                val confirmBuilder = ActionButton.builder(TextParser.parseText(confirmBtnText))
                    .action(MenuActions.buildActionFromConfig(player, config, "Bottom.confirm.actions", inputKeys, inputTypes, checkboxMappings, menuOpener))
                
                // 读取 confirm 按钮的 tooltip
                val confirmTooltipList = bottomSection?.let { getStringList(player, it, "confirm.tooltip") }
                confirmTooltipList?.let {
                    if (it.isNotEmpty()) {
                        val confirmTooltipComponent = Component.join(Component.newline(), *confirmTooltipList.map { TextParser.parseText(it) }.toTypedArray())
                        confirmBuilder.tooltip(confirmTooltipComponent)
                    }
                }
                
                if (confirmWidth > 0) {
                    confirmBuilder.width(confirmWidth)
                }
                val confirmBtn = confirmBuilder.build()

                val denyBuilder = ActionButton.builder(TextParser.parseText(denyBtnText))
                    .action(MenuActions.buildActionFromConfig(player, config, "Bottom.deny.actions", inputKeys, inputTypes, checkboxMappings, menuOpener))
                
                // 读取 deny 按钮的 tooltip
                val denyTooltipList = bottomSection?.let { getStringList(player, it, "deny.tooltip") }
                denyTooltipList?.let {
                    if (it.isNotEmpty()) {
                        val denyTooltipComponent = Component.join(Component.newline(), *denyTooltipList.map { TextParser.parseText(it) }.toTypedArray())
                        denyBuilder.tooltip(denyTooltipComponent)
                    }
                }
                
                if (denyWidth > 0) {
                    denyBuilder.width(denyWidth)
                }
                val denyBtn = denyBuilder.build()

                DialogType.confirmation(confirmBtn, denyBtn)
            }

            else -> { // notice 模式
                val path = if (config.contains("Bottom.confirm.actions")) "Bottom.confirm.actions" else "Bottom.button1.actions"
                val btnText = bottomSection?.let { getString(player, it, "confirm.text", "") }?.takeIf { it.isNotEmpty() }
                    ?: bottomSection?.let { getString(player, it, "button1.text", "") }?.takeIf { it.isNotEmpty() }
                    ?: "确认"

                val widthPath = if (config.contains("Bottom.confirm.width")) "Bottom.confirm.width" else "Bottom.button1.width"
                val btnWidth = getInt(player, config, widthPath, 0)

                val builder = ActionButton.builder(TextParser.parseText(btnText))
                    .action(MenuActions.buildActionFromConfig(player, config, path, inputKeys, inputTypes, checkboxMappings, menuOpener))

                // 读取 tooltip 配置
                val tooltipPath = if (config.contains("Bottom.confirm.tooltip")) "Bottom.confirm.tooltip" else "Bottom.button1.tooltip"
                val tooltipList = getStringList(player, config, tooltipPath)
                if (tooltipList.isNotEmpty()) {
                    val tooltipComponent = Component.join(Component.newline(), *tooltipList.map { TextParser.parseText(it) }.toTypedArray())
                    builder.tooltip(tooltipComponent)
                }

                if (btnWidth > 0) {
                    builder.width(btnWidth)
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