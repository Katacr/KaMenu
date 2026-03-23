@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import net.milkbowl.vault.economy.Economy
import org.bukkit.command.CommandSender
import org.bukkit.plugin.RegisteredServiceProvider
import org.bukkit.plugin.java.JavaPlugin

class KaMenu : JavaPlugin() {

    lateinit var menuManager: MenuManager
    lateinit var languageManager: LanguageManager
    var economy: Economy? = null

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

        // 6. 设置经济系统
        setupEconomy()

        // 7. 打印启动信息
        sendStartupMessage()
    }

    override fun onDisable() {
        if (::menuManager.isInitialized) {
            menuManager.clear()
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
