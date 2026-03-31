package org.katacr.kamenu

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.ScriptException

/**
 * JavaScript 支持管理器
 * 内嵌 Nashorn JavaScript 引擎
 */
object JavaScriptManager {
    private var scriptEngine: ScriptEngine? = null
    private var available = false
    private var plugin: JavaPlugin? = null

    /**
     * 初始化 JavaScript 支持
     */
    fun initialize(plugin: JavaPlugin) {
        this.plugin = plugin

        try {
            // 尝试直接创建 Nashorn 引擎（适用于 Nashorn 15.3+）
            val nashornFactoryClass = Class.forName("org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory")
            val factory = nashornFactoryClass.getDeclaredConstructor().newInstance()

            // 获取 getScriptEngine 方法并调用（Nashorn 15.3 使用此方法）
            val getEngineMethod = nashornFactoryClass.getMethod("getScriptEngine")
            scriptEngine = getEngineMethod.invoke(factory) as? ScriptEngine

            if (scriptEngine != null) {
                available = true
                // 绑定一些常用的全局变量
                setupGlobalVariables()
            } else {
                plugin.logger.warning("Nashorn engine not found, JavaScript features will be unavailable")
                plugin.logger.info("Please wait for the Nashorn library to download...")
            }
        } catch (e: ClassNotFoundException) {
            plugin.logger.warning("Nashorn library not loaded yet, JavaScript features will be unavailable")
            plugin.logger.info("Please restart the server after the initial setup completes")
        } catch (e: Exception) {
            plugin.logger.warning("Failed to initialize JavaScript support: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 设置全局变量和辅助函数
     */
    private fun setupGlobalVariables() {
        if (scriptEngine == null) return

        // 绑定 Bukkit 相关对象
        scriptEngine!!.put("server", Bukkit.getServer())

        // 添加一些实用函数
        scriptEngine!!.eval("""
            // 发送消息给玩家
            function tell(player, message) {
                if (player && player.sendMessage) {
                    player.sendMessage(message);
                }
            }
            
            // 打印日志
            function log(message) {
                print("[JS] " + message);
            }
            
            // 延迟执行
            function delay(ticks, callback) {
                var task;
                var runnable = {
                    run: function() {
                        callback();
                    }
                };
                task = Bukkit.getScheduler().runTaskLater(null, runnable, ticks);
                return task;
            }
            
            // 异步延迟执行
            function asyncDelay(ticks, callback) {
                var task;
                var runnable = {
                    run: function() {
                        callback();
                    }
                };
                task = Bukkit.getScheduler().runTaskLaterAsynchronously(null, runnable, ticks);
                return task;
            }
            
            // 获取玩家
            function getPlayer(name) {
                return Bukkit.getPlayer(name);
            }
        """)
    }

    /**
     * 检查 JavaScript 是否可用
     */
    fun isAvailable(): Boolean = available

    /**
     * 执行 JavaScript 代码
     * @return 执行结果，如果失败返回 null
     */
    fun evaluate(script: String): Any? {
        if (!available || scriptEngine == null) {
            return null
        }

        return try {
            scriptEngine!!.eval(script)
        } catch (e: ScriptException) {
            plugin?.logger?.warning("JavaScript execution error: ${e.message}")
            null
        } catch (e: Exception) {
            plugin?.logger?.warning("JavaScript error: ${e.message}")
            null
        }
    }

    /**
     * 绑定变量到 JavaScript 上下文
     */
    fun putVariable(name: String, value: Any?) {
        scriptEngine?.put(name, value)
    }

    /**
     * 从 JavaScript 上下文获取变量
     */
    fun getVariable(name: String): Any? {
        return scriptEngine?.get(name)
    }

    /**
     * 为特定执行绑定玩家上下文
     * @param player 玩家对象
     * @param script 要执行的脚本
     * @return 执行结果
     */
    fun evaluateWithContext(player: org.bukkit.entity.Player, script: String): Any? {
        if (!available || scriptEngine == null) {
            return null
        }

        return try {
            // 绑定玩家相关的变量
            putVariable("player", player)
            putVariable("uuid", player.uniqueId.toString())
            putVariable("name", player.name)
            putVariable("location", player.location)
            putVariable("inventory", player.inventory)
            putVariable("world", player.world)

            val result = scriptEngine!!.eval(script)

            // 清理绑定的变量
            putVariable("player", null)
            putVariable("uuid", null)
            putVariable("name", null)
            putVariable("location", null)
            putVariable("inventory", null)
            putVariable("world", null)

            result
        } catch (e: ScriptException) {
            plugin?.logger?.warning("JavaScript execution error for player ${player.name}: ${e.message}")
            null
        } catch (e: Exception) {
            plugin?.logger?.warning("JavaScript error for player ${player.name}: ${e.message}")
            null
        }
    }

    /**
     * 重新初始化引擎（用于在库加载后）
     */
    fun reinitialize() {
        if (plugin != null) {
            available = false
            scriptEngine = null
            initialize(plugin!!)
        }
    }

    /**
     * 执行预定义的 JavaScript 代码块（带参数）
     * @param player 玩家对象
     * @param functionName 函数名（不带方括号）
     * @param argsString 参数字符串（空格分隔）
     * @param menuConfig 菜单配置（用于读取 JavaScript 部分）
     * @return 执行结果
     */
    fun executePredefinedFunctionWithArgs(
        player: org.bukkit.entity.Player,
        functionName: String,
        argsString: String,
        menuConfig: org.bukkit.configuration.file.YamlConfiguration
    ): Any? {
        if (!available || scriptEngine == null) {
            return null
        }

        // 解析参数（如果有）
        val args = if (argsString.isEmpty()) {
            emptyList()
        } else {
            argsString.trim().split("\\s+".toRegex())
        }

        // 从菜单配置中读取 JavaScript 部分
        val jsSection = menuConfig.getConfigurationSection("JavaScript")
        if (jsSection == null) {
            plugin?.logger?.warning("JavaScript section not found in menu config")
            return null
        }

        val jsCode = jsSection.getString(functionName)
        if (jsCode == null) {
            plugin?.logger?.warning("JavaScript function '$functionName' not found")
            return null
        }

        return try {
            // 绑定玩家相关的变量
            putVariable("player", player)
            putVariable("uuid", player.uniqueId.toString())
            putVariable("name", player.name)
            putVariable("location", player.location)
            putVariable("inventory", player.inventory)
            putVariable("world", player.world)

            // 预处理参数：创建 JavaScript 数组并绑定
            val argsArrayJs = if (args.isEmpty()) {
                "[]"
            } else {
                args.joinToString(", ", "[", "]") { arg ->
                    // 转义 JavaScript 字符串中的特殊字符
                    val escaped = arg
                        .replace("\\", "\\\\")  // 反斜杠
                        .replace("\"", "\\\"")   // 双引号
                        .replace("\n", "\\n")    // 换行
                        .replace("\r", "\\r")    // 回车
                        .replace("\t", "\\t")    // 制表符
                    "\"$escaped\""
                }
            }


            // 将 args 绑定为 JavaScript 数组
            scriptEngine!!.eval("var args = $argsArrayJs;")

            // 只执行指定的 JavaScript 代码块
            val result = scriptEngine!!.eval(jsCode)

            // 清理绑定的变量
            putVariable("player", null)
            putVariable("uuid", null)
            putVariable("name", null)
            putVariable("location", null)
            putVariable("inventory", null)
            putVariable("world", null)

            result
        } catch (e: ScriptException) {
            plugin?.logger?.warning("JavaScript execution error for function '$functionName': ${e.message}")
            null
        } catch (e: Exception) {
            plugin?.logger?.warning("JavaScript error for function '$functionName': ${e.message}")
            null
        }
    }

    /**
     * 执行预定义的 JavaScript 代码块（旧版本，兼容性保留）
     * @param player 玩家对象
     * @param functionName 函数名和参数（格式：[function_name] arg1 arg2 arg3 ...）
     * @param menuConfig 菜单配置（用于读取 JavaScript 部分）
     * @return 执行结果
     */
    fun executePredefinedFunction(
        player: org.bukkit.entity.Player,
        functionName: String,
        menuConfig: org.bukkit.configuration.file.YamlConfiguration
    ): Any? {
        if (!available || scriptEngine == null) {
            return null
        }

        // 解析函数名和参数
        // 格式：[function_name] arg1 arg2 arg3 ...
        val trimmed = functionName.trim()

        // 检查是否是预定义函数格式 [function_name]
        val closeBracketIndex = trimmed.indexOf(']')
        if (!trimmed.startsWith("[") || closeBracketIndex == -1) {
            return null
        }

        // 提取函数名（在 [ 和 ] 之间）
        val cleanName = trimmed.substring(1, closeBracketIndex)

        // 查找第一个空格的位置（在 ] 之后）
        var firstSpaceIndex = -1
        for (i in (closeBracketIndex + 1) until trimmed.length) {
            if (trimmed[i].isWhitespace()) {
                firstSpaceIndex = i
                break
            }
        }

        // 解析参数（如果有）
        val argsString = if (firstSpaceIndex > closeBracketIndex) {
            trimmed.substring(firstSpaceIndex).trim()
        } else {
            ""
        }

        // 调用新方法
        return executePredefinedFunctionWithArgs(player, cleanName, argsString, menuConfig)
    }
}
