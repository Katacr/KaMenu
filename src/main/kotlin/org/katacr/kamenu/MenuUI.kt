package org.katacr.kamenu

import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.katacr.kamenu.dialog.DialogPlatformAdapter
import org.katacr.kamenu.dialog.bedrock.BedrockFormAdapter

/**
 * KaMenu 的平台中立 Dialog 入口。
 *
 * 运行时先探测 Paper Dialog API，否则探测 Spigot Bungee Dialog API；具体适配器通过反射加载，
 * 从而保证单个插件 JAR 在另一平台缺少对应 API 类时仍可安全启动。
 */
object MenuUI {
    private lateinit var plugin: KaMenu
    private lateinit var adapter: DialogPlatformAdapter
    private var bedrockAdapter: BedrockFormAdapter? = null

    val platformName: String
        get() = adapter.platformName

    val paperPlatform: Boolean
        get() = adapter.platformName == "Paper"

    /** 探测并初始化当前服务器可用的 Dialog 适配器。 */
    fun init(kaMenu: KaMenu) {
        plugin = kaMenu
        val classLoader = kaMenu.javaClass.classLoader
        val adapterClassName = when {
            classAvailable("io.papermc.paper.dialog.Dialog", classLoader) ->
                "org.katacr.kamenu.dialog.paper.PaperDialogPlatformAdapter"

            classAvailable("net.md_5.bungee.api.dialog.Dialog", classLoader) &&
                classAvailable("org.bukkit.event.player.PlayerCustomClickEvent", classLoader) ->
                "org.katacr.kamenu.dialog.spigot.SpigotDialogPlatformAdapter"

            else -> throw IllegalStateException("No compatible Paper or Spigot Dialog API was found")
        }

        try {
            val adapterClass = Class.forName(adapterClassName, true, classLoader)
            adapter = adapterClass.getDeclaredConstructor().newInstance() as DialogPlatformAdapter
            adapter.initialize(kaMenu)
        } catch (error: ReflectiveOperationException) {
            throw IllegalStateException("Failed to initialize Dialog platform adapter: $adapterClassName", error)
        } catch (error: LinkageError) {
            throw IllegalStateException("Failed to link Dialog platform adapter: $adapterClassName", error)
        }

        initializeBedrockAdapter(kaMenu, classLoader)
    }

    /** 打开菜单管理器中的菜单。 */
    fun openMenu(player: Player, menuId: String, manager: MenuManager, plugin: KaMenu) {
        val config = manager.getMenuConfig(menuId)
        if (config != null && bedrockAdapter?.tryOpen(
                player,
                config,
                menuId,
                true
            ) { adapter.forceOpenConfig(player, config, plugin, menuId) } == true
        ) {
            return
        }
        adapter.openMenu(player, menuId, manager, plugin)
    }

    /** 打开外部插件提供的内存 YAML 菜单。 */
    fun openConfig(player: Player, config: YamlConfiguration, plugin: KaMenu, contextId: String = "external") {
        if (bedrockAdapter?.tryOpen(
                player,
                config,
                contextId,
                true
            ) { adapter.forceOpenConfig(player, config, plugin, contextId) } == true
        ) {
            return
        }
        adapter.openConfig(player, config, plugin, contextId)
    }

    /** 强制重新打开已加载菜单。 */
    fun forceOpenMenu(player: Player, menuId: String, manager: MenuManager, plugin: KaMenu) {
        val config = manager.getMenuConfig(menuId)
        if (config != null && bedrockAdapter?.tryOpen(
                player,
                config,
                menuId,
                false
            ) { adapter.forceOpenConfig(player, config, plugin, menuId) } == true
        ) {
            return
        }
        adapter.forceOpenMenu(player, menuId, manager, plugin)
    }

    /** 强制重新打开内存菜单，不重复执行 Events.Open。 */
    fun forceOpenConfig(player: Player, config: YamlConfiguration, plugin: KaMenu, contextId: String = "external") {
        if (bedrockAdapter?.tryOpen(
                player,
                config,
                contextId,
                false
            ) { adapter.forceOpenConfig(player, config, plugin, contextId) } == true
        ) {
            return
        }
        adapter.forceOpenConfig(player, config, plugin, contextId)
    }

    /** 通过当前平台 API 关闭 Dialog。 */
    fun closeDialog(player: Player) {
        if (bedrockAdapter?.close(player) != true) {
            adapter.close(player)
        }
    }

    /** 玩家离线时丢弃可选基岩表单状态。 */
    fun discardPlayer(player: Player) {
        bedrockAdapter?.discard(player)
    }

    /** 使用当前平台支持的文本协议发送富文本消息。 */
    fun sendMessage(sender: CommandSender, message: Component) {
        adapter.sendMessage(sender, message)
    }

    /** 使用当前平台支持的文本协议发送 ActionBar。 */
    fun sendActionBar(player: Player, message: Component) {
        adapter.sendActionBar(player, message)
    }

    /** 使用当前平台支持的标题 API 发送标题。 */
    fun showTitle(player: Player, title: Component, subtitle: Component, fadeIn: Int, stay: Int, fadeOut: Int) {
        adapter.showTitle(player, title, subtitle, fadeIn, stay, fadeOut)
    }

    /** 通过当前平台的静态或服务端回调协议发送 KaMenu 可点击文本。 */
    fun sendClickableText(player: Player, rawText: String, config: YamlConfiguration?, contextId: String?) {
        adapter.sendClickableText(player, rawText, config, contextId)
    }

    /** 读取当前平台的物品名称组件。 */
    fun itemName(item: ItemStack): Component = adapter.itemName(item)

    /** 读取当前平台的物品 Lore 组件。 */
    fun itemLore(meta: ItemMeta): List<Component> = adapter.itemLore(meta)

    /** 读取当前平台的 ItemModel 键。 */
    fun itemModel(meta: ItemMeta): String? = adapter.itemModel(meta)

    /** 释放平台适配器状态。 */
    fun shutdown() {
        bedrockAdapter?.shutdown()
        bedrockAdapter = null
        if (::adapter.isInitialized) {
            adapter.shutdown()
        }
    }

    /**
     * 保留旧的条件取值辅助 API，供现有调用方继续使用。
     *
     * 此方法本身不依赖 Dialog 平台类型。
     */
    internal fun getConditionalValue(
        player: Player,
        config: YamlConfiguration,
        path: String,
        defaultValue: String = ""
    ): String {
        if (config.isList(path)) {
            val conditions = config.getList(path) ?: return defaultValue
            return ConditionUtils.getFirstConditionString(player, conditions, defaultValue, config)
        }
        return config.getString(path, defaultValue) ?: defaultValue
    }

    /**
     * 兼容旧调用点的消息构建入口；仅 Paper 适配器会返回 Adventure Component。
     */
    fun createMessageComponent(
        player: Player,
        section: ConfigurationSection,
        path: String,
        defaultText: String,
        config: YamlConfiguration? = null,
        menuOpener: ((Player, String) -> Unit)? = null
    ): Component {
        if (paperPlatform) {
            return PaperMenuUI.createMessageComponent(player, section, path, defaultText, config, menuOpener)
        }
        return TextParser.parseText(getConditionalValue(player, config ?: YamlConfiguration(), path, defaultText), player)
    }

    private fun classAvailable(name: String, classLoader: ClassLoader): Boolean {
        return try {
            Class.forName(name, false, classLoader)
            true
        } catch (_: ClassNotFoundException) {
            false
        } catch (_: LinkageError) {
            false
        }
    }

    /** Floodgate 可用时通过反射加载基岩表单层，避免形成强制运行时依赖。 */
    private fun initializeBedrockAdapter(kaMenu: KaMenu, classLoader: ClassLoader) {
        val floodgatePlugin = kaMenu.server.pluginManager.getPlugin("floodgate")
        if (floodgatePlugin?.isEnabled != true ||
            !classAvailable("org.geysermc.floodgate.api.FloodgateApi", classLoader) ||
            !classAvailable("org.geysermc.cumulus.form.SimpleForm", classLoader)
        ) {
            return
        }

        val className = "org.katacr.kamenu.dialog.bedrock.FloodgateFormAdapter"
        try {
            val adapterClass = Class.forName(className, true, classLoader)
            val resolved = adapterClass.getDeclaredConstructor().newInstance() as BedrockFormAdapter
            resolved.initialize(kaMenu)
            bedrockAdapter = resolved
        } catch (error: ReflectiveOperationException) {
            kaMenu.logger.warning(
                kaMenu.languageManager.getMessage("bedrock_form.initialize_failed", error.message.toString())
            )
        } catch (error: LinkageError) {
            kaMenu.logger.warning(
                kaMenu.languageManager.getMessage("bedrock_form.initialize_failed", error.message.toString())
            )
        } catch (error: RuntimeException) {
            kaMenu.logger.warning(
                kaMenu.languageManager.getMessage("bedrock_form.initialize_failed", error.message.toString())
            )
        }
    }
}
