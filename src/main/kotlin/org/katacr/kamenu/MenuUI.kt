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
import org.bukkit.NamespacedKey
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
import org.bukkit.inventory.meta.SkullMeta
import com.destroystokyo.paper.profile.ProfileProperty

object MenuUI {
    private lateinit var plugin: KaMenu
    private const val DEFAULT_REPEAT_PAGE_SIZE = 20
    private const val MAX_REPEAT_PAGE_SIZE = 99

    private data class DropdownOption(
        val id: String,
        val display: String
    )

    private data class RepeatItem(
        val values: Map<String, String>
    )

    /**
     * 解析 dropdown 选项。
     * 支持：
     * 1. 旧格式："warrior" -> id=warrior, display=warrior
     * 2. 新格式："warrior => &c战士" -> id=warrior, display=&c战士
     *
     * 使用字符串分隔而不是 Map 配置，这样可以继续兼容现有 options 的条件判断列表能力。
     */
    private fun parseDropdownOption(raw: String): DropdownOption {
        val parts = raw.split("=>", limit = 2)
        return if (parts.size == 2) {
            val id = parts[0].trim()
            val display = parts[1].trim().ifEmpty { id }
            DropdownOption(id, display)
        } else {
            DropdownOption(raw, raw)
        }
    }

    private fun dynamicVariable(player: Player, contextId: String, key: String): String? {
        val dotIndex = key.indexOf(':')
        if (dotIndex <= 0) {
            return null
        }

        val type = key.substring(0, dotIndex).trim().lowercase()
        val listId = key.substring(dotIndex + 1).trim()
        if (listId.isEmpty()) {
            return null
        }

        val info = MenuListManager.getPageInfo(player, contextId, listId)
        return when (type) {
            "page" -> (info?.page ?: MenuListManager.getPage(player, contextId, listId)).toString()
            "pages" -> (info?.pages ?: 1).toString()
            "total" -> (info?.total ?: 0).toString()
            "start" -> (info?.start ?: 0).toString()
            "end" -> (info?.end ?: 0).toString()
            else -> null
        }
    }

    private fun resolveMenuText(
        player: Player,
        text: String?,
        variables: Map<String, String> = emptyMap(),
        contextId: String? = null
    ): String {
        return if (contextId == null) {
            TextResolver.resolve(player, text, variables)
        } else {
            TextResolver.resolve(player, text, variables) { key ->
                dynamicVariable(player, contextId, key)
            }
        }
    }

    private fun resolveRepeatSource(
        player: Player,
        config: YamlConfiguration,
        rawSource: String,
        split: String?,
        trimItems: Boolean
    ): List<RepeatItem> {
        val source = rawSource.trim()
        val result = when {
            isJavaScriptFunctionSource(source) -> {
                val functionName = source.substring(1, source.length - 1).trim()
                JavaScriptManager.executePredefinedFunctionWithArgs(player, functionName, "", config)
            }
            else -> resolveMenuText(player, source)
        } ?: return emptyList()

        return repeatItemsFromAny(result, split, trimItems)
    }

    private fun isJavaScriptFunctionSource(source: String): Boolean {
        if (!source.startsWith("[") || !source.endsWith("]")) {
            return false
        }
        val functionName = source.substring(1, source.length - 1).trim()
        return functionName.matches(Regex("[A-Za-z_$][A-Za-z0-9_$]*"))
    }

    private fun repeatItemsFromAny(value: Any?, split: String? = null, trimItems: Boolean = true): List<RepeatItem> {
        return when (value) {
            null -> emptyList()
            is Iterable<*> -> value.mapIndexedNotNull { index, item -> repeatItemFromAny(index, item) }
            is Array<*> -> value.mapIndexedNotNull { index, item -> repeatItemFromAny(index, item) }
            is String -> {
                val parsed = JavaScriptManager.parseJsonCompatible(value)
                if (parsed != null && parsed !is String) {
                    repeatItemsFromAny(parsed)
                } else if (!split.isNullOrEmpty()) {
                    splitStringItems(value, split, trimItems)
                } else {
                    value.lines()
                        .filter { it.isNotBlank() }
                        .mapIndexed { index, line ->
                            RepeatItem(mapOf(
                                "item.value" to line.trim(),
                                "item.text" to line.trim(),
                                "item.index" to index.toString(),
                                "item.number" to (index + 1).toString()
                            ))
                        }
                }
            }
            else -> emptyList()
        }
    }

    private fun splitStringItems(value: String, split: String, trimItems: Boolean): List<RepeatItem> {
        return value.split(split)
            .map { if (trimItems) it.trim() else it }
            .filter { it.isNotEmpty() }
            .mapIndexed { index, item ->
                RepeatItem(mapOf(
                    "item.value" to item,
                    "item.text" to item,
                    "item.index" to index.toString(),
                    "item.number" to (index + 1).toString()
                ))
            }
    }

    private fun repeatItemFromAny(index: Int, item: Any?): RepeatItem? {
        return when (item) {
            null -> null
            is Map<*, *> -> {
                val values = mutableMapOf<String, String>()
                item.forEach { (key, value) ->
                    if (key != null) {
                        values["item.${key.toString()}"] = value?.toString() ?: ""
                    }
                }
                values["item.index"] = index.toString()
                values["item.number"] = (index + 1).toString()
                RepeatItem(values)
            }
            else -> RepeatItem(mapOf(
                "item.value" to item.toString(),
                "item.text" to item.toString(),
                "item.index" to index.toString(),
                "item.number" to (index + 1).toString()
            ))
        }
    }

    private fun createActionButton(
        player: Player,
        config: YamlConfiguration,
        path: String,
        btnSection: ConfigurationSection,
        btnKey: String,
        defaultText: String,
        variables: Map<String, String>,
        contextId: String,
        inputKeys: List<String>,
        inputTypes: Map<String, String>,
        checkboxMappings: Map<String, Pair<String, String>>,
        menuOpener: (Player, String) -> Unit,
        closesDialogAfterAction: Boolean
    ): ActionButton {
        val btnText = resolveMenuText(player, getString(player, btnSection, "$btnKey.text", defaultText), variables, contextId)
        val btnWidth = getInt(player, btnSection, "$btnKey.width", 0)

        val builder = ActionButton.builder(TextParser.parseText(btnText))
            .action(MenuActions.buildActionFromConfig(player, config, "$path.actions", inputKeys, inputTypes, checkboxMappings, menuOpener, closesDialogAfterAction, variables, contextId))

        val tooltipList = getStringList(player, btnSection, "$btnKey.tooltip")
            .map { resolveMenuText(player, it, variables, contextId) }
        if (tooltipList.isNotEmpty()) {
            val tooltipComponent = Component.join(Component.newline(), *tooltipList.map { TextParser.parseText(it) }.toTypedArray())
            builder.tooltip(tooltipComponent)
        }

        if (btnWidth > 0) {
            builder.width(btnWidth)
        }

        return builder.build()
    }

    private fun addRepeatButtons(
        player: Player,
        config: YamlConfiguration,
        contextId: String,
        listId: String,
        listSection: ConfigurationSection,
        actionButtons: MutableList<ActionButton>,
        inputKeys: List<String>,
        inputTypes: Map<String, String>,
        checkboxMappings: Map<String, Pair<String, String>>,
        menuOpener: (Player, String) -> Unit,
        closesDialogAfterAction: Boolean
    ) {
        val source = listSection.getString("source", "") ?: ""
        val split = listSection.getString("split")
        val trimItems = getBoolean(player, listSection, "trim", true)
        val pageSize = getInt(
            player,
            listSection,
            "page_size",
            getInt(player, listSection, "page-size", DEFAULT_REPEAT_PAGE_SIZE)
        ).coerceIn(1, MAX_REPEAT_PAGE_SIZE)
        val items = resolveRepeatSource(player, config, source, split, trimItems)
        val pageInfo = MenuListManager.updatePageInfo(player, contextId, listId, pageSize, items.size)

        if (items.isEmpty()) {
            val emptySection = listSection.getConfigurationSection("empty") ?: return
            actionButtons.add(createActionButton(
                player = player,
                config = config,
                path = emptySection.currentPath ?: "Bottom.buttons.$listId.empty",
                btnSection = listSection,
                btnKey = "empty",
                defaultText = "暂无数据",
                variables = emptyMap(),
                contextId = contextId,
                inputKeys = inputKeys,
                inputTypes = inputTypes,
                checkboxMappings = checkboxMappings,
                menuOpener = menuOpener,
                closesDialogAfterAction = closesDialogAfterAction
            ))
            return
        }

        val itemSection = listSection.getConfigurationSection("item") ?: return
        val visibleItems = items.subList(pageInfo.start, pageInfo.end)
        visibleItems.forEachIndexed { pageIndex, item ->
            val variables = item.values.toMutableMap()
            variables["item.page_index"] = pageIndex.toString()
            variables["item.page_number"] = (pageIndex + 1).toString()
            variables["list.id"] = listId
            variables["list.page"] = pageInfo.page.toString()
            variables["list.pages"] = pageInfo.pages.toString()
            variables["list.total"] = pageInfo.total.toString()

            val showCondition = itemSection.getString("show-condition") ?: itemSection.getString("show_condition")
            if (showCondition != null) {
                if (!ConditionUtils.checkCondition(player, showCondition, variables) { key -> dynamicVariable(player, contextId, key) }) {
                    return@forEachIndexed
                }
            }

            actionButtons.add(createActionButton(
                player = player,
                config = config,
                path = "Bottom.buttons.$listId.item",
                btnSection = listSection,
                btnKey = "item",
                defaultText = "{item.text}",
                variables = variables,
                contextId = contextId,
                inputKeys = inputKeys,
                inputTypes = inputTypes,
                checkboxMappings = checkboxMappings,
                menuOpener = menuOpener,
                closesDialogAfterAction = closesDialogAfterAction
            ))
        }
    }

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
            TextResolver.resolve(player, rawText),
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

        openConfig(player, config, plugin, menuId)
    }

    fun openConfig(player: Player, config: YamlConfiguration, plugin: KaMenu, contextId: String = "external") {
        val openActions = config.getList("Events.Open")
        if (openActions.isNullOrEmpty()) {
            openMenuInternal(player, config, plugin, contextId)
            return
        }

        MenuActions.executeEvent(player, config, "Open", contextId).whenComplete { shouldStop, error ->
            if (error != null) {
                plugin.logger.severe("Open 事件执行失败: contextId=$contextId, 错误: ${error.message}")
                error.printStackTrace()
                return@whenComplete
            }

            if (shouldStop) {
                return@whenComplete
            }

            Bukkit.getScheduler().runTask(plugin, Runnable {
                openMenuInternal(player, config, plugin, contextId)
            })
        }
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
        val title = TextParser.parseText(TextResolver.resolve(player, rawTitle))

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
        val closesDialogAfterAction = afterAction == DialogBase.DialogAfterAction.CLOSE

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
                        val materialStr = TextResolver.resolve(player, getString(player, section, "$key.material", "PAPER"))
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
                                        val skullMeta = meta as SkullMeta
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
                                val name = TextResolver.resolve(player, getString(player, section, "$key.name", ""))
                                if (name.isNotEmpty()) {
                                    meta.displayName(TextParser.parseText(name))
                                }
                                val lore = getStringList(player, section, "$key.lore")
                                if (lore.isNotEmpty()) {
                                    meta.lore(lore.map { TextParser.parseText(it) })
                                }

                                // 支持设置 custom_model_data（兼容 ItemsAdder 等基于该属性的资源包物品）
                                val customModelDataStr = TextResolver.resolve(player, getString(player, section, "$key.custom_model_data", ""))
                                if (customModelDataStr.isNotEmpty()) {
                                    val customModelData = customModelDataStr.toIntOrNull()
                                    if (customModelData != null) {
                                        meta.setCustomModelData(customModelData)
                                    } else {
                                        plugin.logger.warning("Invalid custom_model_data: $customModelDataStr, menu: $menuId, component: $key")
                                    }
                                }
                                
                                // 支持设置 item_model（Paper 1.21.5+；本插件整体仍要求 1.21.7+）
                                val itemModel = TextResolver.resolve(player, getString(player, section, "$key.item_model", ""))
                                if (itemModel.isNotEmpty()) {
                                    val modelKey = if (itemModel.contains(":")) {
                                        NamespacedKey.fromString(itemModel)
                                    } else {
                                        NamespacedKey.minecraft(itemModel)
                                    }

                                    if (modelKey != null) {
                                        meta.itemModel = modelKey
                                    } else {
                                        plugin.logger.warning("Invalid item_model: $itemModel, menu: $menuId, component: $key")
                                    }
                                }

                                // 支持设置玩家头颅皮肤
                                if (meta is SkullMeta) {
                                    // skull_texture: 自定义 Base64 纹理
                                    val skullTexture = TextResolver.resolve(player, getString(player, section, "$key.skull_texture", ""))
                                    if (skullTexture.isNotEmpty()) {
                                        // 基于纹理值生成固定 UUID，使客户端能缓存已下载的纹理
                                        val textureUUID = java.util.UUID.nameUUIDFromBytes(skullTexture.toByteArray())
                                        val profile = Bukkit.createProfile(textureUUID, "custom_head")
                                        profile.setProperty(ProfileProperty("textures", skullTexture))
                                        meta.playerProfile = profile
                                    } else {
                                        // skull_owner: 通过玩家名设置头颅
                                        val skullOwner = TextResolver.resolve(player, getString(player, section, "$key.skull_owner", ""))
                                        if (skullOwner.isNotEmpty()) {
                                            val ownerPlayer = Bukkit.getOfflinePlayer(skullOwner)
                                            meta.owningPlayer = ownerPlayer
                                        }
                                    }
                                }
                            }
                        }
                        val descriptionText = TextResolver.resolve(player, getString(player, section, "$key.description", ""))
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
                            val option = parseDropdownOption(it)
                            SingleOptionDialogInput.OptionEntry.create(
                                option.id,
                                TextParser.parseText(option.display),
                                option.id == defaultId
                            )
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
                val contextId = menuId
                bottomSection?.getConfigurationSection("buttons")?.let { btnSection ->
                    for (btnKey in btnSection.getKeys(false)) {
                        val buttonType = btnSection.getString("$btnKey.type", "")?.lowercase()
                        if (buttonType == "repeat") {
                            btnSection.getConfigurationSection(btnKey)?.let { repeatSection ->
                                addRepeatButtons(
                                    player = player,
                                    config = config,
                                    contextId = contextId,
                                    listId = btnKey,
                                    listSection = repeatSection,
                                    actionButtons = actionButtons,
                                    inputKeys = inputKeys,
                                    inputTypes = inputTypes,
                                    checkboxMappings = checkboxMappings,
                                    menuOpener = menuOpener,
                                    closesDialogAfterAction = closesDialogAfterAction
                                )
                            }
                            continue
                        }

                        // 检查按钮显示条件（兼容 show-condition 和 show_condition 两种写法）
                        val showCondition = btnSection.getString("$btnKey.show-condition") ?: btnSection.getString("$btnKey.show_condition")

                        if (showCondition != null && !ConditionUtils.checkCondition(player, showCondition, emptyMap()) { key -> dynamicVariable(player, contextId, key) }) {
                            // 条件不满足，不显示此按钮
                            continue
                        }

                        actionButtons.add(createActionButton(
                            player = player,
                            config = config,
                            path = "Bottom.buttons.$btnKey",
                            btnSection = btnSection,
                            btnKey = btnKey,
                            defaultText = "按钮",
                            variables = emptyMap(),
                            contextId = contextId,
                            inputKeys = inputKeys,
                            inputTypes = inputTypes,
                            checkboxMappings = checkboxMappings,
                            menuOpener = menuOpener,
                            closesDialogAfterAction = closesDialogAfterAction
                        ))
                    }
                }

                // 退出/返回按钮
                val exitBtn = bottomSection?.let { section ->
                    val exitText = getString(player, section, "exit.text", "")
                    if (exitText.isNotEmpty()) {
                        val exitWidth = getInt(player, section, "exit.width", 0)
                        val builder = ActionButton.builder(TextParser.parseText(exitText))
                            .action(MenuActions.buildActionFromConfig(player, config, "Bottom.exit.actions", inputKeys, inputTypes, checkboxMappings, menuOpener, closesDialogAfterAction, contextId = menuId))

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
                    .action(MenuActions.buildActionFromConfig(player, config, "Bottom.confirm.actions", inputKeys, inputTypes, checkboxMappings, menuOpener, closesDialogAfterAction, contextId = menuId))
                
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
                    .action(MenuActions.buildActionFromConfig(player, config, "Bottom.deny.actions", inputKeys, inputTypes, checkboxMappings, menuOpener, closesDialogAfterAction, contextId = menuId))
                
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
                    .action(MenuActions.buildActionFromConfig(player, config, path, inputKeys, inputTypes, checkboxMappings, menuOpener, closesDialogAfterAction, contextId = menuId))

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
        MenuTaskManager.attachMenu(player, config, menuId)
    }
}
