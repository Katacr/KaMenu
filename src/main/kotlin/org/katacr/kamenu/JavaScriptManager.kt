package org.katacr.kamenu

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import javax.script.Bindings
import javax.script.Compilable
import javax.script.ScriptEngine
import javax.script.ScriptException

/**
 * JavaScript 支持管理器
 * 内嵌 Nashorn JavaScript 引擎
 */
object JavaScriptManager {
    private var scriptEngine: ScriptEngine? = null
    private var available = false
    private var plugin: JavaPlugin? = null
    private var packageManager: JavaScriptPackageManager? = null
    private val scriptLock = Any()
    private val helperScript = """
        var Bukkit = Java.type("org.bukkit.Bukkit");

        function __kamenu_target_player(targetPlayer) {
            return targetPlayer || (typeof player !== "undefined" ? player : null);
        }

        function tell(targetPlayer, message) {
            if (targetPlayer && targetPlayer.sendMessage) {
                targetPlayer.sendMessage(message);
            }
        }

        function log(message) {
            print("[JS] " + message);
        }

        function delay(ticks, callback) {
            var runnable = {
                run: function() {
                    callback();
                }
            };
            return Bukkit.getScheduler().runTaskLater(__kamenu_plugin, runnable, ticks);
        }

        function asyncDelay(ticks, callback) {
            var runnable = {
                run: function() {
                    callback();
                }
            };
            return Bukkit.getScheduler().runTaskLaterAsynchronously(__kamenu_plugin, runnable, ticks);
        }

        function getPlayer(name) {
            return Bukkit.getPlayer(name);
        }

        function papi(placeholder, targetPlayer) {
            return __kamenu_js_manager.resolvePapi(__kamenu_target_player(targetPlayer), String(placeholder));
        }

        function kvar(variable, targetPlayer) {
            return __kamenu_js_manager.resolveKaMenuVariable(__kamenu_target_player(targetPlayer), String(variable));
        }

        function data(key, targetPlayer) {
            return kvar("data:" + key, targetPlayer);
        }

        function gdata(key, targetPlayer) {
            return kvar("gdata:" + key, targetPlayer);
        }

        function meta(key, targetPlayer) {
            return kvar("meta:" + key, targetPlayer);
        }

        function list(key, targetPlayer) {
            return kvar("list:" + key, targetPlayer);
        }

        function glist(key, targetPlayer) {
            return kvar("glist:" + key, targetPlayer);
        }
    """.trimIndent()

    private data class ScriptSource(
        val code: String,
        val label: String
    )

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
                warn("javascript.nashorn_engine_not_found")
                info("javascript.wait_library_download")
            }
        } catch (e: ClassNotFoundException) {
            warn("javascript.nashorn_library_not_loaded")
            info("javascript.restart_after_setup")
        } catch (e: Exception) {
            warn("javascript.initialize_failed", e.message ?: e.javaClass.simpleName)
            e.printStackTrace()
        }
    }

    /**
     * 设置全局变量和辅助函数
     */
    private fun setupGlobalVariables() {
        if (scriptEngine == null) return

        // 仅保留兼容性全局对象。实际脚本执行会使用独立 Bindings 注入上下文和辅助函数。
        scriptEngine!!.put("server", Bukkit.getServer())
        scriptEngine!!.put("__kamenu_js_manager", this)
    }

    /**
     * 检查 JavaScript 是否可用
     */
    fun isAvailable(): Boolean = available

    fun setPackageManager(manager: JavaScriptPackageManager) {
        packageManager = manager
    }

    /**
     * 校验 JavaScript 语法，不执行脚本，避免包加载阶段触发副作用。
     * @return null 表示语法可编译；非 null 为错误原因。
     */
    fun validateSyntax(script: String): String? {
        if (!available || scriptEngine == null) {
            return "JavaScript engine is not available"
        }

        val compilable = scriptEngine as? Compilable
            ?: return "JavaScript engine does not support syntax compilation"

        return synchronized(scriptLock) {
            try {
                compilable.compile(buildScript(script))
                null
            } catch (e: ScriptException) {
                e.message ?: e.javaClass.simpleName
            } catch (e: Exception) {
                e.message ?: e.javaClass.simpleName
            }
        }
    }

    /**
     * 执行 JavaScript 代码
     * @return 执行结果，如果失败返回 null
     */
    fun evaluate(script: String): Any? {
        if (!available || scriptEngine == null) {
            return null
        }

        return synchronized(scriptLock) {
            try {
                scriptEngine!!.eval(buildScript(script), createBindings())
            } catch (e: ScriptException) {
                warn("javascript.execution_error", e.message ?: e.javaClass.simpleName)
                null
            } catch (e: Exception) {
                warn("javascript.error", e.message ?: e.javaClass.simpleName)
                null
            }
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

    fun resolvePapi(player: org.bukkit.entity.Player?, placeholder: String?): String {
        if (player == null || placeholder.isNullOrBlank()) {
            return ""
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return ""
        }

        val normalized = placeholder.trim().let {
            if (it.startsWith("%") && it.endsWith("%")) it else "%$it%"
        }

        return try {
            me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, normalized)
        } catch (_: Exception) {
            ""
        }
    }

    fun resolveKaMenuVariable(player: org.bukkit.entity.Player?, variable: String?): String {
        if (player == null || variable.isNullOrBlank()) {
            return ""
        }

        val trimmed = variable.trim()
        val expression = if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            trimmed
        } else {
            "{$trimmed}"
        }

        return try {
            TextResolver.resolve(player, expression)
        } catch (_: Exception) {
            ""
        }
    }

    fun parseJsonCompatible(json: String): Any? {
        if (!available || scriptEngine == null || json.isBlank()) {
            return null
        }

        return synchronized(scriptLock) {
            try {
                val bindings = createBindings()
                bindings["__kamenu_json_input"] = json
                scriptEngine!!.eval("Java.asJSONCompatible(JSON.parse(__kamenu_json_input))", bindings)
            } catch (e: ScriptException) {
                warn("javascript.json_parse_error", e.message ?: e.javaClass.simpleName)
                null
            } catch (e: Exception) {
                warn("javascript.json_parse_error", e.message ?: e.javaClass.simpleName)
                null
            }
        }
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

        return synchronized(scriptLock) {
            try {
                scriptEngine!!.eval(buildScript(script), createBindings(player))
            } catch (e: ScriptException) {
                warn("javascript.execution_error_player", player.name, e.message ?: e.javaClass.simpleName)
                null
            } catch (e: Exception) {
                warn("javascript.error_player", player.name, e.message ?: e.javaClass.simpleName)
                null
            }
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
     * @param argsString 参数字符串（支持空格或英文逗号分隔）
     * @param menuConfig 菜单配置（用于读取 JavaScript 部分）
     * @return 执行结果
     */
    fun executePredefinedFunctionWithArgs(
        player: org.bukkit.entity.Player,
        functionName: String,
        argsString: String,
        menuConfig: org.bukkit.configuration.file.YamlConfiguration?
    ): Any? {
        return executePredefinedFunctionWithArgs(
            player,
            functionName,
            ActionArgumentParser.splitArguments(argsString),
            menuConfig
        )
    }

    fun executePredefinedFunctionWithArgs(
        player: org.bukkit.entity.Player,
        functionName: String,
        args: List<String>,
        menuConfig: org.bukkit.configuration.file.YamlConfiguration?
    ): Any? {
        if (!available || scriptEngine == null) {
            return null
        }

        val source = findScriptSource(functionName, menuConfig)
        if (source == null) {
            val checked = if (menuConfig == null) {
                "global js/$functionName.js"
            } else {
                "menu JavaScript.$functionName and global js/$functionName.js"
            }
            warn("javascript.package_not_found", functionName, checked)
            return null
        }

        return evaluateWithContext(player, source.code, args, source.label)
    }

    private fun findScriptSource(
        functionName: String,
        menuConfig: org.bukkit.configuration.file.YamlConfiguration?
    ): ScriptSource? {
        val menuScript = menuConfig
            ?.getConfigurationSection("JavaScript")
            ?.getString(functionName)
        if (menuScript != null) {
            return ScriptSource(menuScript, "menu JavaScript.$functionName")
        }

        val globalScript = packageManager?.getScript(functionName) ?: return null
        return ScriptSource(globalScript, "global js/$functionName.js")
    }

    private fun evaluateWithContext(
        player: org.bukkit.entity.Player,
        script: String,
        args: List<String>,
        sourceLabel: String
    ): Any? {
        return synchronized(scriptLock) {
            try {
                scriptEngine!!.eval(buildScript(script, args), createBindings(player))
            } catch (e: ScriptException) {
                warn("javascript.execution_error_source_player", sourceLabel, player.name, e.message ?: e.javaClass.simpleName)
                null
            } catch (e: Exception) {
                warn("javascript.error_source_player", sourceLabel, player.name, e.message ?: e.javaClass.simpleName)
                null
            }
        }
    }

    private fun createBindings(player: org.bukkit.entity.Player? = null): Bindings {
        val bindings = scriptEngine!!.createBindings()
        bindings["server"] = Bukkit.getServer()
        bindings["__kamenu_js_manager"] = this
        bindings["__kamenu_plugin"] = plugin

        if (player != null) {
            bindings["player"] = player
            bindings["uuid"] = player.uniqueId.toString()
            bindings["name"] = player.name
            bindings["location"] = player.location
            bindings["inventory"] = player.inventory
            bindings["world"] = player.world
        }

        return bindings
    }

    private fun buildScript(script: String, args: List<String> = emptyList()): String {
        return "var args = ${toJavaScriptArray(args)};\n$helperScript\n$script"
    }

    private fun toJavaScriptArray(args: List<String>): String {
        if (args.isEmpty()) {
            return "[]"
        }

        return args.joinToString(", ", "[", "]") { arg ->
            val escaped = arg
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
            "\"$escaped\""
        }
    }

    private fun warn(key: String, vararg args: String) {
        val message = (plugin as? KaMenu)?.languageManager?.getMessage(key, *args)
            ?: "$key: ${args.joinToString(", ")}"
        plugin?.logger?.warning(message)
    }

    private fun info(key: String, vararg args: String) {
        val message = (plugin as? KaMenu)?.languageManager?.getMessage(key, *args)
            ?: "$key: ${args.joinToString(", ")}"
        plugin?.logger?.info(message)
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

        val call = ActionArgumentParser.parseBracketCall(functionName) ?: return null
        return executePredefinedFunctionWithArgs(player, call.name, call.arguments, menuConfig)
    }
}
