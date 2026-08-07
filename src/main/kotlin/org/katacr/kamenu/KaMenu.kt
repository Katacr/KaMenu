@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import net.byteflux.libby.BukkitLibraryManager
import net.byteflux.libby.Library
import net.milkbowl.vault.economy.Economy
import org.bstats.bukkit.Metrics
import org.bstats.charts.SingleLineChart
import org.bukkit.plugin.RegisteredServiceProvider
import org.bukkit.plugin.java.JavaPlugin
import org.katacr.kamenu.container.ContainerMenuListener
import org.katacr.kamenu.container.ContainerMenuService
import java.io.File

/**
 * KaMenu 插件主类。
 *
 * 负责插件生命周期编排：依赖库加载、配置/语言初始化、菜单与包管理器加载、
 * 动作/条件/变量模块注入，以及 PlaceholderAPI、Vault、bStats 等外部能力挂钩。
 *
 * 业务模块多数通过 `lateinit` 属性从这里取得共享管理器，因此新增初始化步骤时要注意顺序：
 * 语言和 TextResolver 必须早于需要 i18n 的模块，数据库必须早于 data/list 变量解析。
 */
class KaMenu : JavaPlugin() {

    lateinit var menuManager: MenuManager
    lateinit var languageManager: LanguageManager
    lateinit var databaseManager: DatabaseManager
    lateinit var metaDataManager: MetaDataManager
    lateinit var customCommandManager: CustomCommandManager
    lateinit var itemManager: ItemManager
    lateinit var itemPlaceholderService: ItemPlaceholderService
    lateinit var actionPackageManager: ActionPackageManager
    lateinit var javaScriptPackageManager: JavaScriptPackageManager
    lateinit var pauseEntryDatapackManager: PauseEntryDatapackManager
    lateinit var containerMenuService: ContainerMenuService
    var economy: Economy? = null
    var bungeeCordEnabled: Boolean = false

    /** 当前运行核心是否已初始化 ESC 暂停菜单数据包与平台回调。 */
    val pauseEntrySupported: Boolean
        get() = ::pauseEntryDatapackManager.isInitialized && MenuUI.dialogSupported

    /** Container 运行时是否已经完成初始化。 */
    val containerMenusReady: Boolean
        get() = ::containerMenuService.isInitialized

    /**
     * 在 Bukkit 启用插件前下载并挂载运行时依赖。
     *
     * 该方法在 Kotlin stdlib 尚不可用时进入，因此加载 kotlin-stdlib 之前只能调用
     * Java、Bukkit 和已随 JAR 提供的 Libby API。Kotlin 加载完成后才进入常规运行库加载流程。
     */
    override fun onLoad() {
        // 此引导段必须保持 Kotlin-free；修改后需检查 onLoad 字节码中的首次 kotlin/ 引用。
        val librariesDir = File(dataFolder.parentFile.parentFile, "libraries")
        if (!librariesDir.exists()) {
            librariesDir.mkdirs()
        }

        val libraryManager = BukkitLibraryManager(this, librariesDir.absolutePath)

        // 国内镜像优先，Maven Central 仅作为回退源。
        libraryManager.addRepository("https://maven.aliyun.com/repository/public")
        libraryManager.addMavenCentral()

        logger.info("Checking and downloading necessary dependent libraries, please wait...")

        val kotlinStdlib = Library.builder()
            .groupId("org{}jetbrains{}kotlin")
            .artifactId("kotlin-stdlib")
            .version("2.3.20")
            .build()
        libraryManager.loadLibrary(kotlinStdlib)

        loadRuntimeLibraries(libraryManager)
    }

    /**
     * 在 Kotlin stdlib 已挂载后加载 KaMenu 的常规运行库。
     *
     * Libby 1.3.0 不解析 Maven 传递依赖，因此 Adventure、bStats 等依赖按运行顺序显式列出。
     */
    private fun loadRuntimeLibraries(libraryManager: BukkitLibraryManager) {
        val runtimeLibraries = listOf(
            library("org{}jetbrains", "annotations", "13.0"),
            library("net{}kyori", "adventure-api", "4.26.1"),
            library("net{}kyori", "adventure-key", "4.26.1"),
            library("net{}kyori", "adventure-text-minimessage", "4.26.1"),
            library("net{}kyori", "adventure-text-serializer-legacy", "4.26.1"),
            library("net{}kyori", "adventure-text-serializer-plain", "4.26.1"),
            library("net{}kyori", "adventure-text-serializer-gson", "4.26.1"),
            library("net{}kyori", "adventure-text-serializer-json", "4.26.1"),
            library("net{}kyori", "adventure-text-serializer-commons", "4.26.1"),
            library("net{}kyori", "adventure-text-serializer-bungeecord", "4.4.1"),
            library("net{}kyori", "examination-api", "1.3.0"),
            library("net{}kyori", "examination-string", "1.3.0"),
            library("net{}kyori", "option", "1.1.0"),
            relocatedLibrary("org{}bstats", "bstats-base", "3.1.0"),
            relocatedLibrary("org{}bstats", "bstats-bukkit", "3.1.0"),
            library("org{}xerial", "sqlite-jdbc", "3.46.1.0"),
            library("com{}mysql", "mysql-connector-j", "9.1.0"),
            library("com{}zaxxer", "HikariCP", "5.1.0"),
            library("org{}ow2{}asm", "asm", "9.5"),
            library("org{}ow2{}asm", "asm-util", "9.5"),
            library("org{}openjdk{}nashorn", "nashorn-core", "15.3")
        )
        runtimeLibraries.forEach(libraryManager::loadLibrary)
    }

    /** 创建一项由 Libby 从 Maven 仓库下载的运行库描述。 */
    private fun library(groupId: String, artifactId: String, version: String): Library {
        return Library.builder()
            .groupId(groupId)
            .artifactId(artifactId)
            .version(version)
            .build()
    }

    /** 创建一项下载后重定位到 KaMenu 私有命名空间的运行库。 */
    private fun relocatedLibrary(groupId: String, artifactId: String, version: String): Library {
        return Library.builder()
            .groupId(groupId)
            .artifactId(artifactId)
            .version(version)
            .relocate("org{}bstats", "org{}katacr{}kamenu{}libs{}bstats")
            .build()
    }

    override fun onEnable() {
        // 1. 保存并加载配置
        saveDefaultConfig()

        // 1.5 在配置升级前迁移旧版 config.yml 中的自定义指令，避免旧值被升级器丢弃
        val configFile = File(dataFolder, "config.yml")
        val customCommandPreparation = CustomCommandFileManager.prepare(this, configFile)
        val configUpdated = if (customCommandPreparation.error == null) {
            ConfigUpdater.checkAndUpdateConfig(this, configFile)
        } else {
            false
        }

        // 重新加载配置（如果已更新）
        if (configUpdated) {
            reloadConfig()
            config
        } else {
            config
        }

        // 2. 初始化语言管理器
        languageManager = LanguageManager(this)
        languageManager.init()
        TextResolver.setPlugin(this)
        TextResolver.setLanguageManager(languageManager)

        // 设置工具类的语言管理器引用
        ConditionUtils.setLanguageManager(languageManager)
        MenuActions.setLanguageManager(languageManager)
        ActionHandlers.setLanguageManager(languageManager)
        ConfigUpdater.setLanguageManager(languageManager)

        if (customCommandPreparation.legacyFound) {
            logger.info(
                languageManager.getMessage(
                    "custom_commands.legacy_migrated",
                    customCommandPreparation.migratedCount.toString(),
                    customCommandPreparation.backupName ?: "none"
                )
            )
        }
        customCommandPreparation.error?.let { error ->
            logger.warning(languageManager.getMessage("custom_commands.legacy_migration_failed", error))
        }

        // 初始化 MenuUI
        KaScheduler.init(this)
        DialogSessionManager.init(this)
        MenuUI.init(this)
        MenuTaskManager.init(this)

        // 设置 MenuActions 插件引用
        MenuActions.setPlugin(this)

        // 初始化 ActionHandlers
        ActionHandlers.init(this)

        // 初始化 JavaScript 支持
        JavaScriptManager.initialize(this)

        // 0.5 读取并应用 BungeeCord 配置
        bungeeCordEnabled = config.getBoolean("bungeecord", false)
        MenuActions.setBungeeCordEnabled(bungeeCordEnabled)
        ActionHandlers.setBungeeCordEnabled(bungeeCordEnabled)
        if (bungeeCordEnabled) {
            server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")
            logger.info("BungeeCord support enabled")
        } else {
            logger.info("BungeeCord support disabled")
        }

        // 3. 初始化菜单管理器
        menuManager = MenuManager(this)
        menuManager.loadMenus()

        // 3.3 初始化全局 actions 包管理器
        actionPackageManager = ActionPackageManager(this)
        actionPackageManager.loadPackages()
        MenuActions.setActionPackageManager(actionPackageManager)

        // 3.4 初始化全局 JavaScript 包管理器
        javaScriptPackageManager = JavaScriptPackageManager(this)
        javaScriptPackageManager.loadPackages()
        JavaScriptManager.setPackageManager(javaScriptPackageManager)

        // 3.45 初始化平台中立的 ESC 暂停菜单数据包管理器。
        pauseEntryDatapackManager = PauseEntryDatapackManager(this)

        // 3.5 初始化自定义指令管理器
        customCommandManager = CustomCommandManager(this)
        customCommandManager.registerCustomCommands()

        // 4. 注册主指令
        getCommand("km")?.let { cmd ->
            val menuCommand = MenuCommand(this)
            cmd.setExecutor(menuCommand)
            cmd.tabCompleter = menuCommand
        }

        // 5. 注册监听器
        server.pluginManager.registerEvents(MenuListener(this), this)

        // 6. 初始化数据库管理器
        databaseManager = DatabaseManager(this)
        databaseManager.setup()
        MenuActions.setDatabaseManager(databaseManager)
        ActionHandlers.setDatabaseManager(databaseManager)

        // 6.5 初始化元数据管理器
        metaDataManager = MetaDataManager()
        MenuActions.setMetaDataManager(metaDataManager)
        ActionHandlers.setMetaDataManager(metaDataManager)

        // 6.6 初始化物品管理器
        itemManager = ItemManager(this)
        itemPlaceholderService = ItemPlaceholderService(itemManager)
        MenuActions.setItemManager(itemManager)
        ActionHandlers.setItemManager(itemManager)

        // 6.7 初始化 V2 Container 菜单运行时和只读库存监听器
        containerMenuService = ContainerMenuService(this)
        server.pluginManager.registerEvents(ContainerMenuListener(containerMenuService), this)
        containerMenuService.cleanupOnlinePlayers()

        // 设置 ConditionUtils 插件引用
        ConditionUtils.setPlugin(this)

        // 7. 设置经济系统
        setupEconomy()
        MenuActions.setEconomy(economy)
        ActionHandlers.setEconomy(economy)
        setupPlayerPoints()

        // 7.5 初始化 API
        org.katacr.kamenu.api.KaMenuAPI.init(this)

        // 8. 统计数据
        val metrics = Metrics(this, 30376)
        metrics.addCustomChart(SingleLineChart("menus_total") {
            menuManager.getAllMenuIds().size
        })

        // 9. 注册 PlaceholderAPI 扩展
        if (server.pluginManager.getPlugin("PlaceholderAPI") != null) {
            KaMenuExpansion(this).register()
        }

        // 9.5 异步检查更新
        if (config.getBoolean("check-update", true)) {
            UpdateChecker.check(this)
        }

        // 10. 打印启动信息
        sendStartupMessage()
    }

    override fun onDisable() {
        // 取消注册 BungeeCord 消息通道
        if (bungeeCordEnabled) {
            server.messenger.unregisterOutgoingPluginChannel(this, "BungeeCord")
        }

        if (::containerMenuService.isInitialized) {
            containerMenuService.shutdown()
        }
        MenuTaskManager.cancelAll()
        DialogSessionManager.clearAll()
        MenuListManager.clearAll()
        MenuArgumentManager.clearAll()
        MenuUI.shutdown()
        if (::menuManager.isInitialized) {
            menuManager.clear()
        }
        if (::databaseManager.isInitialized) {
            databaseManager.close()
        }
        if (::metaDataManager.isInitialized) {
            metaDataManager.clearAll()
        }
        if (::customCommandManager.isInitialized) {
            customCommandManager.clear()
        }
        KaScheduler.cancelPluginTasks()

        logger.info(languageManager.getMessage("plugin.disabled"))
    }

    /**
     * 打印启动 Logo
     */
    private fun sendStartupMessage() {
        val console = server.consoleSender
        val version = description.version
        val gameVersion = server.version.split("MC: ")[1].removeSuffix(")")

        // 统计信息
        val papiStatus = server.pluginManager.getPlugin("PlaceholderAPI") != null
        val vaultStatus = economy != null
        val menuCount = menuManager.getAllMenuIds().size
        val commandCount = customCommandManager.getRegisteredCommandCount()
        val actionPackageCount = actionPackageManager.getPackageIds().size
        val javaScriptPackageCount = javaScriptPackageManager.getPackageIds().size
        val currentLang = languageManager.getCurrentLanguage()
        val dbType = config.getString("storage.type", "SQLite") ?: "SQLite"

        // 准备国际化文本
        val papiText = languageManager.getMessage("logo.hook_true").takeIf { papiStatus }
            ?: languageManager.getMessage("logo.hook_false")
        val vaultText = languageManager.getMessage("logo.hook_true").takeIf { vaultStatus }
            ?: languageManager.getMessage("logo.hook_false")

        // 使用三引号避免转义字符导致的对齐问题
        val logo = """
            §e________________________________________________________
            §b
            §b  _  __      §3 __  __                        §b
            §b | |/ / ____ §3|  \/  | ___ _ __  _   _       §b
            §b | ' / |    |§3| |\/| |/ _ \ '_ \| | | |      §b
            §b | . \ | [] |§3| |  | |  __/ | | | |_| |      §b
            §b |_|\_\|_,\_\§3|_|  |_|\___|_| |_|\__,_|      §b
            §b
            §7${languageManager.getMessage("logo.version", version)}
            §7${languageManager.getMessage("logo.minecraft", gameVersion)}
            §7${languageManager.getMessage("logo.dialog_platform", MenuUI.platformName)}
            §7${languageManager.getMessage("logo.database", dbType)}
            §7${languageManager.getMessage("logo.language", currentLang)}
            §7${languageManager.getMessage("logo.vault", vaultText)}
            §7${languageManager.getMessage("logo.placeholderapi", papiText)}
            §7${languageManager.getMessage("logo.menu_count", menuCount.toString())}
            §7${languageManager.getMessage("logo.command_count", commandCount.toString())}
            §7${languageManager.getMessage("logo.action_package_count", actionPackageCount.toString())}
            §7${languageManager.getMessage("logo.javascript_package_count", javaScriptPackageCount.toString())}
            §e________________________________________________________
        """.trimIndent()

        // 按行拆分发送
        logo.split("\n").forEach { line ->
            console.sendMessage(line)
        }
    }

    /**
     * 设置经济系统
     */
    private fun setupEconomy(): Boolean {
        if (server.pluginManager.getPlugin("Vault") == null) return false
        val rsp: RegisteredServiceProvider<Economy> = server.servicesManager.getRegistration(Economy::class.java) ?: return false
        economy = rsp.provider
        return true
    }

    /**
     * 在 PlayerPoints 已启用时向动作模块注入点券服务。
     *
     * 先按插件名判断再加载 API 适配类，保证未安装 PlayerPoints 的服务器不解析其类引用。
     */
    private fun setupPlayerPoints() {
        val pointsPlugin = server.pluginManager.getPlugin("PlayerPoints")
            ?.takeIf { it.isEnabled }
            ?: return
        ActionHandlers.setPointsService(PlayerPointsService.create(pointsPlugin))
    }
}
