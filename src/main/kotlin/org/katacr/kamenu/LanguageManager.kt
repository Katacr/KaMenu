package org.katacr.kamenu

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.InputStream

/**
 * 语言管理器
 * 负责加载和管理多语言文本
 */
class LanguageManager(private val plugin: KaMenu) {

    private val defaultLanguage = "zh_CN"
    private val fallbackLanguage = "en_US"
    private var currentLanguage = defaultLanguage
    private val messages = mutableMapOf<String, String>()

    // 缓存插件内部的默认语言文件
    private val internalMessages = mutableMapOf<String, YamlConfiguration>()

    /**
     * 初始化语言管理器
     */
    fun init() {
        // 从配置文件读取语言设置
        currentLanguage = plugin.config.getString("language") ?: defaultLanguage

        // 保存默认语言文件
        saveDefaultMessages()

        // 加载插件内部的默认语言文件到内存
        loadInternalMessages()

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
     * 加载插件内部的默认语言文件到内存
     */
    private fun loadInternalMessages() {
        val languages = listOf("zh_CN", "en_US")
        languages.forEach { lang ->
            val inputStream: InputStream? = plugin.getResource("lang/${lang}.yml")
            if (inputStream != null) {
                try {
                    val config = YamlConfiguration.loadConfiguration(inputStream.reader())
                    internalMessages[lang] = config
                } catch (e: Exception) {
                    plugin.logger.warning("加载内部语言文件失败: lang/${lang}.yml - ${e.message}")
                } finally {
                    inputStream.close()
                }
            } else {
                plugin.logger.warning("找不到内部语言文件: lang/${lang}.yml")
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
            // 尝试从内部语言文件中获取默认值
            message = getDefaultMessage(key)

            if (message != null) {
                // 写入到用户的语言文件
                addMissingKey(key, message)
            } else {
                plugin.logger.warning("not found lang key: $key")
                return key
            }
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
     * 从内部语言文件中获取默认消息
     * 优先使用当前语言的内部文件，如果没有则使用英语
     * @param key 消息键
     * @return 默认消息，如果找不到则返回 null
     */
    private fun getDefaultMessage(key: String): String? {
        // 1. 尝试从当前语言的内部文件中获取
        val currentInternalConfig = internalMessages[currentLanguage]
        if (currentInternalConfig != null && currentInternalConfig.contains(key)) {
            val value = currentInternalConfig.getString(key)
            if (value != null && currentInternalConfig.isString(key)) {
                return value
            }
        }

        // 2. 如果当前语言的内部文件中没有，尝试从英语文件中获取
        if (currentLanguage != fallbackLanguage) {
            val fallbackInternalConfig = internalMessages[fallbackLanguage]
            if (fallbackInternalConfig != null && fallbackInternalConfig.contains(key)) {
                val value = fallbackInternalConfig.getString(key)
                if (value != null && fallbackInternalConfig.isString(key)) {
                    return value
                }
            }
        }

        return null
    }

    /**
     * 将缺失的键写入到用户的语言文件中
     * @param key 消息键
     * @param value 消息值
     */
    private fun addMissingKey(key: String, value: String) {
        val langFolder = File(plugin.dataFolder, "lang")
        val file = File(langFolder, "${currentLanguage}.yml")

        if (!file.exists()) {
            plugin.logger.warning("用户语言文件不存在: ${file.name}，无法写入缺失的键")
            return
        }

        try {
            val config = YamlConfiguration.loadConfiguration(file)

            // 检查是否已经存在（避免重复写入）
            if (config.contains(key)) {
                return
            }

            // 写入缺失的键
            config.set(key, value)

            // 保存文件
            config.save(file)

            // 更新内存中的消息
            messages[key] = value

        } catch (e: Exception) {
            plugin.logger.warning("写入缺失的键到语言文件失败: ${e.message}")
        }
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
