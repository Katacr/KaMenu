@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import org.bukkit.plugin.java.JavaPlugin

class KaMenu : JavaPlugin() {

    lateinit var menuManager: MenuManager

    override fun onEnable() {
        // 1. 保存并加载配置
        saveDefaultConfig()
        val config = config

        // 2. 初始化管理器
        menuManager = MenuManager(this)
        menuManager.loadMenus()

        // 3. 注册主指令
        getCommand("km")?.let { cmd ->
            val menuCommand = MenuCommand(this)
            cmd.setExecutor(menuCommand)
            cmd.tabCompleter = menuCommand
        }

        // 4. 注册监听器
        server.pluginManager.registerEvents(MenuListener(this), this)

        logger.info("KaMenu 已就绪！")
        val lang = config.getString("language") ?: "zh_CN"
        logger.info("当前语言设置为: $lang")
    }

    override fun onDisable() {
        if (::menuManager.isInitialized) {
            menuManager.clear()
        }
        server.scheduler.cancelTasks(this)

        logger.info("KaMenu 已关闭。")
    }
}
