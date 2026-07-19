@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.papermc.paper.dialog.DialogResponseView
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File

/**
 * ESC 暂停菜单数据包管理器。
 *
 * 读取插件根目录的 `pause_menu.yml`，将其中静态的 KaMenu 风格布局编译为原版 Dialog 数据包。
 * 数据包只保存静态界面；`menu` 与 `actions` 按钮通过固定目标 id 回调插件，
 * 分别打开真正的 KaMenu 菜单或执行服务端动作队列。
 */
class PauseEntryDatapackManager(private val plugin: KaMenu) {
    /**
     * `/km pause info` 使用的源文件与数据包状态。
     */
    data class EntryInfo(
        val sourceFile: File,
        val sourceExists: Boolean,
        val datapackFolder: File,
        val datapackExists: Boolean
    )

    /**
     * 暂停菜单底部按钮的静态配置。
     */
    private data class EntryButton(
        val id: String,
        val text: String,
        val tooltip: String?,
        val width: Int,
        val menu: String?,
        val actions: List<*>?,
        val url: String?,
        val copy: String?,
        val command: String?
    )

    /**
     * 客户端按钮 id 对应的可信运行时目标。
     */
    private sealed interface RegisteredTarget {
        data class Menu(val menuId: String) : RegisteredTarget
        data class Actions(val actions: List<*>) : RegisteredTarget
    }

    /**
     * 当前已注册暂停菜单的运行时配置、输入结构与按钮目标。
     */
    private data class RegisteredPauseMenu(
        val config: YamlConfiguration,
        val inputSchema: InputCaptureUtils.Schema,
        val targets: Map<String, RegisteredTarget>
    )

    /**
     * 一次编译产生的 Dialog JSON 与可信运行时状态。
     */
    private data class CompiledPauseMenu(
        val dialog: JsonObject,
        val runtime: RegisteredPauseMenu
    )

    /**
     * 一组编译后的原版输入组件及对应响应结构。
     */
    private data class CompiledInputs(
        val json: JsonArray,
        val schema: InputCaptureUtils.Schema
    )

    /**
     * 单项选择按钮的静态 id 与显示文本。
     */
    private data class DropdownOption(
        val id: String,
        val display: String
    )

    companion object {
        val ACTION_KEY: Key = Key.key("kamenu", "pause_screen_open")
        private val JSON: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
        private val COMPONENT_SERIALIZER = GsonComponentSerializer.gson()
        private const val SOURCE_FILE = "pause_menu.yml"
        private const val DATAPACK_FOLDER = "KaMenuPauseEntry"
        private const val DIALOG_ID = "kamenu:pause_menu"
        private const val DEFAULT_TITLE = "KaMenu"
    }

    @Volatile
    private var registeredPauseMenu: RegisteredPauseMenu? = null

    init {
        ensureSourceFile()
        if (datapackFolder().exists()) {
            reloadMenuTargets()
        }
    }

    /**
     * 解析 `pause_menu.yml` 并生成 ESC 入口数据包。
     */
    fun register(): Boolean {
        return try {
            val compiled = compilePauseMenu(loadSourceConfig())
            writeDatapack(compiled.dialog)
            registeredPauseMenu = compiled.runtime
            true
        } catch (exception: Exception) {
            plugin.logger.warning(
                plugin.languageManager.getMessage(
                    "pause_entry.write_failed",
                    exception.message ?: exception.javaClass.simpleName
                )
            )
            false
        }
    }

    /**
     * 移除生成的数据包并立即停止接受旧入口回调。
     */
    fun unregister(): Boolean {
        registeredPauseMenu = null
        val folder = datapackFolder()
        val removedCurrent = !folder.exists() || folder.deleteRecursively()
        val legacyFolder = legacyDatapackFolder()
        val removedLegacy = legacyFolder == null || !legacyFolder.exists() || legacyFolder.deleteRecursively()
        return removedCurrent && removedLegacy
    }

    /**
     * 返回源文件和数据包目录状态。
     */
    fun info(): EntryInfo {
        val source = sourceFile()
        val datapack = datapackFolder()
        return EntryInfo(source, source.exists(), datapack, datapack.exists())
    }

    /**
     * 处理静态 Dialog 的受控目标回调，并将客户端输入传给动作系统。
     */
    fun handleRegisteredTarget(
        sender: CommandSender,
        targetId: String? = null,
        response: DialogResponseView? = null
    ) {
        if (sender !is Player) {
            return
        }
        if (!datapackFolder().exists()) {
            sender.sendMessage(plugin.languageManager.getMessage("pause_entry.disabled"))
            return
        }

        val target = targetId?.trim().takeUnless { it.isNullOrEmpty() }
        val runtime = registeredPauseMenu ?: run {
            sender.sendMessage(plugin.languageManager.getMessage("pause_entry.disabled"))
            return
        }
        val registeredTarget = target?.let { runtime.targets[it] } ?: run {
            sender.sendMessage(plugin.languageManager.getMessage("pause_entry.button_not_found", target ?: ""))
            return
        }

        when (registeredTarget) {
            is RegisteredTarget.Menu -> {
                if (plugin.menuManager.getMenuConfig(registeredTarget.menuId) == null) {
                    sender.sendMessage(
                        plugin.languageManager.getMessage("pause_entry.menu_not_found", registeredTarget.menuId)
                    )
                    return
                }
                MenuUI.openMenu(sender, registeredTarget.menuId, plugin.menuManager, plugin)
            }

            is RegisteredTarget.Actions -> {
                val variables = InputCaptureUtils.captureVariables(plugin, response, runtime.inputSchema)
                MenuActions.executeActionGroup(
                    sender,
                    runtime.config,
                    registeredTarget.actions,
                    variables = variables,
                    contextId = "pause_menu"
                ).whenComplete { _, error ->
                    if (error != null) {
                        plugin.logger.warning(
                            plugin.languageManager.getMessage(
                                "pause_entry.action_execution_failed",
                                target,
                                error.message ?: error.javaClass.simpleName
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * 首次安装时释放可直接修改的 `pause_menu.yml` 模板。
     */
    private fun ensureSourceFile() {
        val source = sourceFile()
        if (!source.exists()) {
            plugin.saveResource(SOURCE_FILE, false)
        }
    }

    /**
     * 从插件根目录加载暂停菜单 YAML，并让语法错误进入统一错误处理。
     */
    private fun loadSourceConfig(): YamlConfiguration {
        val source = sourceFile()
        require(source.isFile) { "Missing $SOURCE_FILE: ${source.absolutePath}" }
        return YamlConfiguration().apply { load(source) }
    }

    /**
     * 插件启动时从源文件恢复可信菜单目标，不改写已有数据包。
     */
    private fun reloadMenuTargets() {
        try {
            registeredPauseMenu = compilePauseMenu(loadSourceConfig()).runtime
        } catch (exception: Exception) {
            registeredPauseMenu = null
            plugin.logger.warning(
                plugin.languageManager.getMessage(
                    "pause_entry.source_invalid",
                    exception.message ?: exception.javaClass.simpleName
                )
            )
        }
    }

    /**
     * 将 KaMenu 风格静态结构编译为一个原版 Dialog。
     */
    private fun compilePauseMenu(config: YamlConfiguration): CompiledPauseMenu {
        val targets = linkedMapOf<String, RegisteredTarget>()
        val inputs = compileInputs(config)
        val dialog = JsonObject().apply {
            addProperty("type", dialogType(config))
            add("title", staticComponent(config.getString("Title", DEFAULT_TITLE) ?: DEFAULT_TITLE))
            config.getString("External-Title")?.takeUnless { it.isBlank() }?.let {
                add("external_title", staticComponent(it))
            }
            addCommonSettings(config)
            add("body", compileBody(config))
            if (inputs.json.size() > 0) {
                add("inputs", inputs.json)
            }
            addBottom(config, targets)
        }
        return CompiledPauseMenu(dialog, RegisteredPauseMenu(config, inputs.schema, targets))
    }

    /**
     * 根据 `Bottom.type` 选择原版 Dialog 类型。
     */
    private fun dialogType(config: YamlConfiguration): String {
        return when (config.getString("Bottom.type", "notice")?.trim()?.lowercase()) {
            "notice" -> "minecraft:notice"
            "confirmation" -> "minecraft:confirmation"
            "multi" -> "minecraft:multi_action"
            else -> throw IllegalArgumentException("Unsupported Bottom.type in $SOURCE_FILE")
        }
    }

    /**
     * 编译 Settings 中可由原版数据包直接表达的属性。
     */
    private fun JsonObject.addCommonSettings(config: YamlConfiguration) {
        val canEscape = config.getBoolean("Settings.can_escape", true)
        val pause = config.getBoolean("Settings.pause", false)
        val afterAction = when (config.getString("Settings.after_action", "CLOSE")?.trim()?.uppercase()) {
            "CLOSE" -> "close"
            "NONE" -> "none"
            "WAIT_FOR_RESPONSE" -> "wait_for_response"
            else -> throw IllegalArgumentException("Unsupported Settings.after_action in $SOURCE_FILE")
        }
        require(!pause || afterAction != "none") {
            "Settings.pause=true cannot be used with Settings.after_action=NONE"
        }

        addProperty("can_close_with_escape", canEscape)
        addProperty("pause", pause)
        addProperty("after_action", afterAction)
    }

    /**
     * 按 YAML 顺序编译 `Body.*.type: message` 组件。
     */
    private fun compileBody(config: YamlConfiguration): JsonArray {
        val body = JsonArray()
        val section = config.getConfigurationSection("Body") ?: return body
        section.getKeys(false).forEach { id ->
            val component = section.getConfigurationSection(id)
                ?: throw IllegalArgumentException("Body.$id must be a section")
            when (component.getString("type", "message")?.trim()?.lowercase()) {
                "none" -> Unit
                "message" -> body.add(compileMessageBody(component))
                "item" -> body.add(compileItemBody(component, "Body.$id"))
                else -> throw IllegalArgumentException("Unsupported Body.$id.type; pause menus support message and item")
            }
        }
        return body
    }

    /**
     * 编译一个静态正文组件，复用 KaMenu 的颜色、MiniMessage 与 `<text=...>` 解析。
     */
    private fun compileMessageBody(section: ConfigurationSection): JsonObject {
        val text = readStaticText(section, "text")
        val width = section.getInt("width", 200).coerceIn(1, 1024)
        return JsonObject().apply {
            addProperty("type", "minecraft:plain_message")
            add("contents", staticComponent(text))
            addProperty("width", width)
        }
    }

    /**
     * 编译静态物品 Body；数据包模式支持材质、数量、描述、尺寸和显示开关。
     */
    private fun compileItemBody(section: ConfigurationSection, path: String): JsonObject {
        val rawMaterial = section.getString("material", "PAPER") ?: "PAPER"
        require(!rawMaterial.startsWith("[")) { "$path.material cannot use a runtime slot reference" }
        val material = MaterialUtils.matchMaterial(rawMaterial)
            ?: throw IllegalArgumentException("Unknown material at $path.material: $rawMaterial")
        val amount = section.getInt("amount", 1).coerceIn(1, 99)
        val item = JsonObject().apply {
            addProperty("id", material.key.toString())
            if (amount != 1) {
                addProperty("count", amount)
            }
        }

        return JsonObject().apply {
            addProperty("type", "minecraft:item")
            add("item", item)
            if (section.contains("description")) {
                add("description", JsonObject().apply {
                    add("contents", staticComponent(readStaticText(section, "description")))
                    addProperty("width", section.getInt("description_width", 200).coerceIn(1, 1024))
                })
            }
            addProperty("show_decorations", section.getBoolean("show_overlays", true))
            addProperty("show_tooltip", section.getBoolean("show_tooltip", true))
            addProperty("width", section.getInt("width", 16).coerceIn(1, 256))
            addProperty("height", section.getInt("height", 16).coerceIn(1, 256))
        }
    }

    /**
     * 读取静态字符串或字符串列表；条件对象和运行期变量不属于暂停菜单编译范围。
     */
    private fun readStaticText(section: ConfigurationSection, path: String): String {
        if (!section.isList(path)) {
            return section.getString(path, "") ?: ""
        }
        val values = section.getList(path).orEmpty()
        require(values.all { it is String }) { "${section.currentPath}.$path must contain strings only" }
        return values.filterIsInstance<String>().joinToString("\n")
    }

    /**
     * 编译标准 Inputs，并记录点击回调读取 `$(key)` 所需的字段类型与清理规则。
     */
    private fun compileInputs(config: YamlConfiguration): CompiledInputs {
        val json = JsonArray()
        val keys = mutableListOf<String>()
        val types = linkedMapOf<String, String>()
        val removeChars = linkedMapOf<String, String>()
        val checkboxMappings = linkedMapOf<String, Pair<String, String>>()
        val inputs = config.getConfigurationSection("Inputs")
            ?: return CompiledInputs(json, InputCaptureUtils.Schema.EMPTY)

        inputs.getKeys(false).forEach { key ->
            require(key.isNotBlank()) { "Inputs keys cannot be empty" }
            val section = inputs.getConfigurationSection(key)
                ?: throw IllegalArgumentException("Inputs.$key must be a section")
            val type = section.getString("type", "input")?.trim()?.lowercase() ?: "input"
            if (type == "none") {
                return@forEach
            }

            val input = when (type) {
                "input", "text" -> compileTextInput(section, key).also {
                    types[key] = "text"
                    InputCaptureUtils.resolveRemoveChars(plugin, section.get("remove_chars"))
                        .takeIf { it.isNotEmpty() }
                        ?.let { chars -> removeChars[key] = chars }
                }

                "slider" -> compileSliderInput(section, key).also {
                    types[key] = "number"
                }

                "dropdown" -> compileDropdownInput(section, key).also {
                    types[key] = "text"
                }

                "checkbox" -> compileCheckboxInput(section, key).also {
                    types[key] = "checkbox"
                    checkboxMappings[key] = Pair(
                        section.getString("on_true", "true") ?: "true",
                        section.getString("on_false", "false") ?: "false"
                    )
                }

                else -> throw IllegalArgumentException("Unsupported Inputs.$key.type: $type")
            }
            keys.add(key)
            json.add(input)
        }

        return CompiledInputs(
            json,
            InputCaptureUtils.Schema(keys, types, removeChars, checkboxMappings)
        )
    }

    /** 编译静态文本输入框。 */
    private fun compileTextInput(section: ConfigurationSection, key: String): JsonObject {
        val maxLength = section.getInt("max_length", 256)
        require(maxLength > 0) { "Inputs.$key.max_length must be greater than 0" }
        return JsonObject().apply {
            addProperty("key", key)
            addProperty("type", "minecraft:text")
            addProperty("width", section.getInt("width", 250).coerceIn(1, 1024))
            add("label", staticComponent(section.getString("text", "") ?: ""))
            addProperty("label_visible", !section.getBoolean("hide_text", false))
            addProperty("initial", section.getString("default", "") ?: "")
            addProperty("max_length", maxLength)
            section.getConfigurationSection("multiline")?.let { multiline ->
                add("multiline", JsonObject().apply {
                    addProperty("max_lines", multiline.getInt("max_lines", 5).coerceAtLeast(1))
                    addProperty("height", multiline.getInt("height", 100).coerceIn(1, 1024))
                })
            }
        }
    }

    /** 编译静态数值滑块。 */
    private fun compileSliderInput(section: ConfigurationSection, key: String): JsonObject {
        val start = section.getDouble("min", 0.0)
        val end = section.getDouble("max", 10.0)
        val initial = section.getDouble("default", start)
        val step = section.getDouble("step", 1.0)
        require(start < end) { "Inputs.$key.min must be lower than max" }
        require(initial in start..end) { "Inputs.$key.default must be between min and max" }
        require(step > 0.0) { "Inputs.$key.step must be greater than 0" }
        return JsonObject().apply {
            addProperty("key", key)
            addProperty("type", "minecraft:number_range")
            addProperty("width", section.getInt("width", 250).coerceIn(1, 1024))
            add("label", staticComponent(section.getString("text", "") ?: ""))
            addProperty("label_format", section.getString("format", "%s: %s") ?: "%s: %s")
            addProperty("start", start)
            addProperty("end", end)
            addProperty("initial", initial)
            addProperty("step", step)
        }
    }

    /** 编译静态单项选择按钮。 */
    private fun compileDropdownInput(section: ConfigurationSection, key: String): JsonObject {
        val options = section.getStringList("options").map(::parseDropdownOption)
        require(options.isNotEmpty()) { "Inputs.$key.options must contain at least one option" }
        require(options.all { it.id.isNotBlank() }) { "Inputs.$key.options cannot contain an empty id" }
        require(options.map { it.id }.distinct().size == options.size) {
            "Inputs.$key.options cannot contain duplicate ids"
        }
        val defaultId = section.getString("default_id")?.takeUnless { it.isBlank() }
        require(defaultId == null || options.any { it.id == defaultId }) {
            "Inputs.$key.default_id does not match any option"
        }
        return JsonObject().apply {
            addProperty("key", key)
            addProperty("type", "minecraft:single_option")
            addProperty("width", section.getInt("width", 200).coerceIn(1, 1024))
            add("label", staticComponent(section.getString("text", "") ?: ""))
            addProperty("label_visible", !section.getBoolean("hide_text", false))
            add("options", JsonArray().apply {
                options.forEachIndexed { index, option ->
                    add(JsonObject().apply {
                        addProperty("id", option.id)
                        add("display", staticComponent(option.display))
                        addProperty("initial", option.id == defaultId || defaultId == null && index == 0)
                    })
                }
            })
        }
    }

    /** 编译静态复选框。 */
    private fun compileCheckboxInput(section: ConfigurationSection, key: String): JsonObject {
        return JsonObject().apply {
            addProperty("key", key)
            addProperty("type", "minecraft:boolean")
            add("label", staticComponent(section.getString("text", "") ?: ""))
            addProperty("initial", section.getBoolean("default", false))
            addProperty("on_true", section.getString("on_true", "true") ?: "true")
            addProperty("on_false", section.getString("on_false", "false") ?: "false")
        }
    }

    /** 解析 `id => 显示文本` 格式的单项选择项。 */
    private fun parseDropdownOption(raw: String): DropdownOption {
        val parts = raw.split("=>", limit = 2)
        return if (parts.size == 2) {
            val id = parts[0].trim()
            DropdownOption(id, parts[1].trim().ifEmpty { id })
        } else {
            DropdownOption(raw.trim(), raw.trim())
        }
    }

    /**
     * 编译 notice、confirmation 或 multi 的底部按钮结构。
     */
    private fun JsonObject.addBottom(
        config: YamlConfiguration,
        targets: MutableMap<String, RegisteredTarget>
    ) {
        when (config.getString("Bottom.type", "notice")?.trim()?.lowercase()) {
            "notice" -> {
                val button = parseButton(config.getConfigurationSection("Bottom.confirm"), "confirm", "OK")
                add("action", dialogButton(button, "confirm", targets))
            }

            "confirmation" -> {
                val confirm = parseButton(config.getConfigurationSection("Bottom.confirm"), "confirm", "Yes")
                val deny = parseButton(config.getConfigurationSection("Bottom.deny"), "deny", "No")
                add("yes", dialogButton(confirm, "confirm", targets))
                add("no", dialogButton(deny, "deny", targets))
            }

            "multi" -> {
                val buttonsSection = config.getConfigurationSection("Bottom.buttons")
                    ?: throw IllegalArgumentException("Bottom.buttons is required when Bottom.type is multi")
                val actions = JsonArray()
                buttonsSection.getKeys(false).forEach { id ->
                    val section = buttonsSection.getConfigurationSection(id)
                        ?: throw IllegalArgumentException("Bottom.buttons.$id must be a section")
                    actions.add(dialogButton(parseButton(section, id, id), "button:$id", targets))
                }
                require(actions.size() > 0) { "Bottom.buttons must contain at least one button" }
                add("actions", actions)
                addProperty("columns", config.getInt("Bottom.columns", 2).coerceIn(1, 8))
                config.getConfigurationSection("Bottom.exit")?.let {
                    add("exit_action", dialogButton(parseButton(it, "exit", "Exit"), "exit", targets))
                }
            }
        }
    }

    /**
     * 读取一个静态按钮并拒绝多个目标同时存在的歧义配置。
     */
    private fun parseButton(section: ConfigurationSection?, id: String, defaultText: String): EntryButton {
        val text = section?.getString("text") ?: defaultText
        val tooltip = section?.getString("tooltip")?.takeUnless { it.isBlank() }
        val width = section?.getInt("width", 200)?.coerceIn(1, 1024) ?: 200
        val menu = section?.getString("menu")?.trim().takeUnless { it.isNullOrEmpty() }
        val actions = when {
            section == null || !section.contains("actions") -> null
            !section.isList("actions") -> throw IllegalArgumentException("Button '$id'.actions must be a list")
            else -> section.getList("actions").orEmpty().toList()
        }
        val url = section?.getString("url")?.trim().takeUnless { it.isNullOrEmpty() }
        val copy = section?.getString("copy")?.takeUnless { it.isBlank() }
        val command = section?.getString("command")?.trim().takeUnless { it.isNullOrEmpty() }
        val targetCount = listOf(menu, actions, url, copy, command).count { it != null }
        require(targetCount <= 1) {
            "Button '$id' may define only one of menu, actions, url, copy, command"
        }
        return EntryButton(id, text, tooltip, width, menu, actions, url, copy, command)
    }

    /**
     * 生成原版 ActionButton，并登记可信的 KaMenu 菜单或动作目标。
     */
    private fun dialogButton(
        button: EntryButton,
        targetId: String,
        targets: MutableMap<String, RegisteredTarget>
    ): JsonObject {
        return JsonObject().apply {
            add("label", staticComponent(button.text))
            button.tooltip?.let { add("tooltip", staticComponent(it)) }
            addProperty("width", button.width)
            buttonAction(button, targetId, targets)?.let { add("action", it) }
        }
    }

    /**
     * 将按钮目标转换为 Dialog 静态动作；`menu` 与 `actions` 使用受控的插件回调。
     */
    private fun buttonAction(
        button: EntryButton,
        targetId: String,
        targets: MutableMap<String, RegisteredTarget>
    ): JsonObject? {
        return when {
            button.menu != null -> JsonObject().apply {
                targets[targetId] = RegisteredTarget.Menu(button.menu)
                addProperty("type", "minecraft:dynamic/custom")
                addProperty("id", ACTION_KEY.asString())
                add("additions", JsonObject().apply { addProperty("target", targetId) })
            }

            button.actions != null -> JsonObject().apply {
                targets[targetId] = RegisteredTarget.Actions(button.actions)
                addProperty("type", "minecraft:dynamic/custom")
                addProperty("id", ACTION_KEY.asString())
                add("additions", JsonObject().apply { addProperty("target", targetId) })
            }

            button.url != null -> JsonObject().apply {
                addProperty("type", "minecraft:open_url")
                addProperty("url", button.url)
            }

            button.copy != null -> JsonObject().apply {
                addProperty("type", "minecraft:copy_to_clipboard")
                addProperty("value", button.copy)
            }

            button.command != null -> JsonObject().apply {
                addProperty("type", "minecraft:run_command")
                addProperty("command", button.command)
            }

            else -> null
        }
    }

    /**
     * 将 KaMenu 文本组件序列化为数据包可读取的原版 Component JSON。
     */
    private fun staticComponent(text: String): JsonElement {
        require(!Regex("""(?i)<text=[^>]*;\s*(actions|hover[_-]item)\s*=""").containsMatchIn(text)) {
            "Static pause menu text does not support actions or hover_item"
        }
        val component = MenuActions.parseClickableText(text)
        return JsonParser.parseString(COMPONENT_SERIALIZER.serialize(component))
    }

    /**
     * 在覆盖旧数据包前生成并校验全部 JSON 文件。
     */
    private fun writeDatapack(dialog: JsonObject) {
        val folder = datapackFolder()
        val dialogFolder = File(folder, "data/kamenu/dialog")
        val tagFolder = File(folder, "data/minecraft/tags/dialog")
        val generatedFiles = linkedMapOf(
            File(folder, "pack.mcmeta") to packMetaJson(),
            File(tagFolder, "pause_screen_additions.json") to tagJson(),
            File(dialogFolder, "pause_menu.json") to JSON.toJson(dialog)
        )
        validateJsonFiles(generatedFiles)

        dialogFolder.mkdirs()
        tagFolder.mkdirs()
        dialogFolder.listFiles { file -> file.isFile && file.extension.equals("json", ignoreCase = true) }
            ?.forEach { it.delete() }
        generatedFiles.forEach { (file, contents) -> file.writeText(contents, Charsets.UTF_8) }
        legacyDatapackFolder()?.takeIf { it.exists() }?.deleteRecursively()
    }

    /**
     * 复用当前服务器 Bukkit 数据包声明的格式范围，自动兼容不同 Minecraft 版本。
     */
    private fun packMetaJson(): String {
        val pack = JsonObject().apply { addProperty("description", "KaMenu ESC pause menu entry") }
        val bukkitMeta = File(datapacksFolder(), "bukkit/pack.mcmeta")
        val sourcePack = runCatching {
            JsonParser.parseString(bukkitMeta.readText(Charsets.UTF_8)).asJsonObject.getAsJsonObject("pack")
        }.getOrNull()

        when {
            sourcePack?.has("min_format") == true && sourcePack.has("max_format") -> {
                pack.add("min_format", sourcePack.get("min_format").deepCopy())
                pack.add("max_format", sourcePack.get("max_format").deepCopy())
            }

            sourcePack?.has("pack_format") == true -> pack.add("pack_format", sourcePack.get("pack_format").deepCopy())
            else -> pack.addProperty("pack_format", 81)
        }
        return JSON.toJson(JsonObject().apply { add("pack", pack) })
    }

    /**
     * 生成只包含一个 ESC Dialog 的原版标签。
     */
    private fun tagJson(): String {
        return JSON.toJson(JsonObject().apply {
            add("values", JsonArray().apply { add(DIALOG_ID) })
        })
    }

    /**
     * 验证生成文件至少满足严格 JSON 语法和对象根节点要求。
     */
    private fun validateJsonFiles(files: Map<File, String>) {
        files.forEach { (file, contents) ->
            require(JsonParser.parseString(contents).isJsonObject) {
                "Generated JSON root must be an object: ${file.name}"
            }
        }
    }

    /** 返回插件根目录中的暂停菜单源文件。 */
    private fun sourceFile(): File = File(plugin.dataFolder, SOURCE_FILE)

    /** 返回 KaMenu 生成的数据包目录。 */
    private fun datapackFolder(): File = File(datapacksFolder(), DATAPACK_FOLDER)

    /**
     * 定位包含 `level.dat` 的世界存档根目录，避免写入新版 Paper 的 dimensions 子目录。
     */
    private fun datapacksFolder(): File {
        val mainWorld = plugin.server.worlds.firstOrNull { it.environment == World.Environment.NORMAL }
            ?: plugin.server.worlds.firstOrNull()
        val worldContainer = plugin.server.worldContainer.absoluteFile.normalize()
        var candidate = mainWorld?.worldFolder?.absoluteFile?.normalize()
        while (candidate != null && candidate.toPath().startsWith(worldContainer.toPath())) {
            if (File(candidate, "level.dat").isFile) {
                return File(candidate, "datapacks")
            }
            candidate = candidate.parentFile
        }
        return File(File(worldContainer, mainWorld?.name ?: "world"), "datapacks")
    }

    /**
     * 返回旧实现可能误写在维度目录内的数据包位置。
     */
    private fun legacyDatapackFolder(): File? {
        val mainWorld = plugin.server.worlds.firstOrNull { it.environment == World.Environment.NORMAL }
            ?: plugin.server.worlds.firstOrNull()
            ?: return null
        val legacy = File(File(mainWorld.worldFolder, "datapacks"), DATAPACK_FOLDER).absoluteFile.normalize()
        val current = datapackFolder().absoluteFile.normalize()
        return legacy.takeUnless { it == current }
    }
}
