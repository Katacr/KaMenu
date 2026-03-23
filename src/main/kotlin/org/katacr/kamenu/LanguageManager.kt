package org.katacr.kamenu

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/**
 * 语言管理器
 * 负责加载和管理多语言文本
 */
class LanguageManager(private val plugin: KaMenu) {

    private val defaultLanguage = "zh_CN"
    private var currentLanguage = defaultLanguage
    private val messages = mutableMapOf<String, String>()

    /**
     * 初始化语言管理器
     */
    fun init() {
        // 从配置文件读取语言设置
        currentLanguage = plugin.config.getString("language") ?: defaultLanguage

        // 保存默认语言文件
        saveDefaultMessages()

        // 加载语言文件
        loadMessages(currentLanguage)

    }

    /**
     * 保存默认语言文件到插件文件夹
     */
    private fun saveDefaultMessages() {
        // 确保 lang 文件夹存在
        val langFolder = File(plugin.dataFolder, "lang")
        if (!langFolder.exists()) {
            langFolder.mkdirs()
        }

        val languages = listOf("zh_CN", "en_US")
        languages.forEach { lang ->
            val file = File(langFolder, "${lang}.yml")
            if (!file.exists()) {
                plugin.saveResource("lang/${lang}.yml", false)

            }
        }
    }

    /**
     * 加载指定语言的消息文件
     */
    private fun loadMessages(language: String) {
        val langFolder = File(plugin.dataFolder, "lang")
        val file = File(langFolder, "${language}.yml")
        if (!file.exists()) {
            plugin.logger.warning("语言文件不存在: lang/${language}.yml，使用默认语言")
            if (language != defaultLanguage) {
                loadMessages(defaultLanguage)
            }
            return
        }

        val config = YamlConfiguration.loadConfiguration(file)
        messages.clear()

        // 递归加载所有键值对
        loadKeys(config, "")
    }

    /**
     * 递归加载配置文件中的所有键
     */
    private fun loadKeys(config: YamlConfiguration, prefix: String) {
        for (key in config.getKeys(true)) {
            if (config.isString(key)) {
                val value = config.getString(key) ?: continue
                val fullKey = if (prefix.isEmpty()) key else "$prefix.$key"
                messages[fullKey] = value
            }
        }
    }

    /**
     * 获取本地化消息
     * @param key 消息键（支持点号分隔，如 "plugin.enabled"）
     * @param args 参数替换（使用 {0}, {1}, {2}... 作为占位符）
     * @return 本地化后的消息，如果找不到则返回键本身
     */
    fun getMessage(key: String, vararg args: Any): String {
        var message: String? = messages[key]

        if (message == null) {
            plugin.logger.warning("未找到语言键: $key")
            return key
        }

        // 替换参数占位符
        if (args.isNotEmpty()) {
            args.forEachIndexed { index, arg ->
                if (message != null) {
                    message = message.replace("{$index}", arg.toString())
                }
            }
        }

        return message.toString()
    }

    /**
     * 重新加载语言文件
     */
    fun reload() {
        currentLanguage = plugin.config.getString("language") ?: defaultLanguage
        saveDefaultMessages()
        loadMessages(currentLanguage)
    }

    /**
     * 获取当前语言
     */
    fun getCurrentLanguage(): String = currentLanguage

}
