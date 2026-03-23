@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import org.bukkit.plugin.java.JavaPlugin

class KaMenu : JavaPlugin() {

    lateinit var menuManager: MenuManager
    lateinit var languageManager: LanguageManager

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

        logger.info(languageManager.getMessage("plugin.enabled"))
        val lang = languageManager.getCurrentLanguage()
        logger.info(languageManager.getMessage("plugin.language_set", lang))
    }

    override fun onDisable() {
        if (::menuManager.isInitialized) {
            menuManager.clear()
        }
        server.scheduler.cancelTasks(this)

        logger.info(languageManager.getMessage("plugin.disabled"))
    }
}
