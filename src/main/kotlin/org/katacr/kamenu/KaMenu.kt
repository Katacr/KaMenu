@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import net.byteflux.libby.BukkitLibraryManager
import net.byteflux.libby.Library
import net.milkbowl.vault.economy.Economy
import org.bstats.bukkit.Metrics
import org.bstats.charts.SingleLineChart
import org.bukkit.plugin.RegisteredServiceProvider
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class KaMenu : JavaPlugin() {

    lateinit var menuManager: MenuManager
    lateinit var languageManager: LanguageManager
    lateinit var databaseManager: DatabaseManager
    lateinit var metaDataManager: MetaDataManager
    var economy: Economy? = null

    /**
     * 在插件加载时优先处理依赖下载
     */
    override fun onLoad() {
        // 创建共享的库目录（服务器根目录下的libraries文件夹）
        val librariesDir = File(dataFolder.parentFile.parentFile, "libraries")
        if (!librariesDir.exists()) {
            librariesDir.mkdirs()
        }

        val libraryManager = BukkitLibraryManager(this, librariesDir.absolutePath)

        // 添加 Maven 中央仓库和阿里云镜像（加速国内下载）
        libraryManager.addMavenCentral()
        libraryManager.addRepository("https://maven.aliyun.com/repository/public")

        // Kotlin 标准库
        val kotlinStd = Library.builder()
            .groupId("org{}jetbrains{}kotlin")
            .artifactId("kotlin-stdlib")
            .version("1.9.22")
            .build()

        // SQLite JDBC 驱动
        val sqlite = Library.builder()
            .groupId("org{}xerial")
            .artifactId("sqlite-jdbc")
            .version("3.46.1.0")
            .build()

        // MySQL Connector/J 驱动
        val mysql = Library.builder()
            .groupId("com{}mysql")
            .artifactId("mysql-connector-j")
            .version("9.1.0")
            .build()

        // HikariCP 连接池
        val hikari = Library.builder()
            .groupId("com{}zaxxer")
            .artifactId("HikariCP")
            .version("5.1.0")
            .build()

        logger.info("Checking and downloading necessary dependent libraries, please wait...")

        libraryManager.loadLibrary(kotlinStd)
        libraryManager.loadLibrary(sqlite)
        libraryManager.loadLibrary(mysql)
        libraryManager.loadLibrary(hikari)
    }

    override fun onEnable() {
        // 1. 保存并加载配置
        saveDefaultConfig()
        val config = config

        // 2. 初始化语言管理器
        languageManager = LanguageManager(this)
        languageManager.init()

        // 设置工具类的语言管理器引用
        ConditionUtils.setLanguageManager(languageManager)
        MenuActions.setLanguageManager(languageManager)

        // 初始化 MenuUI
        MenuUI.init(this)

        // 设置 MenuActions 插件引用
        MenuActions.setPlugin(this)

        // 3. 初始化菜单管理器
        menuManager = MenuManager(this)
        menuManager.loadMenus()

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

        // 6.5 初始化元数据管理器
        metaDataManager = MetaDataManager()
        MenuActions.setMetaDataManager(metaDataManager)

        // 设置 ConditionUtils 插件引用
        ConditionUtils.setPlugin(this)

        // 7. 设置经济系统
        setupEconomy()
        MenuActions.setEconomy(economy)

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

        // 10. 打印启动信息
        sendStartupMessage()
    }

    override fun onDisable() {
        if (::menuManager.isInitialized) {
            menuManager.clear()
        }
        if (::databaseManager.isInitialized) {
            databaseManager.close()
        }
        if (::metaDataManager.isInitialized) {
            metaDataManager.clearAll()
        }
        server.scheduler.cancelTasks(this)

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
            §7${languageManager.getMessage("logo.database", dbType)}
            §7${languageManager.getMessage("logo.language", currentLang)}
            §7${languageManager.getMessage("logo.vault", vaultText)}
            §7${languageManager.getMessage("logo.placeholderapi", papiText)}
            §7${languageManager.getMessage("logo.menu_count", menuCount.toString())}
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
}
