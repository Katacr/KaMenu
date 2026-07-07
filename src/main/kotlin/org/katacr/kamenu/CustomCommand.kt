@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.Locale

/**
 * config.yml 中 custom-commands 的运行定义。
 *
 * 同一条自定义指令可以是“打开菜单”，也可以是“执行动作列表”。
 * `argSuggestions` 以参数下标为键，支持静态列表、PAPI、KaMenu 内置变量和 list/glist JSON。
 */
sealed interface CustomCommandDefinition {
    val argSuggestions: Map<Int, Any>

    /**
     * 自定义指令直接打开某个菜单。
     */
    data class OpenMenu(
        val menuId: String,
        override val argSuggestions: Map<Int, Any> = emptyMap()
    ) : CustomCommandDefinition

    /**
     * 自定义指令执行一组 actions。
     */
    data class RunActions(
        val actions: List<Any>,
        override val argSuggestions: Map<Int, Any> = emptyMap()
    ) : CustomCommandDefinition
}

/**
 * 自定义指令处理器
 * 用于处理 config.yml 中配置的自定义指令，可直接打开菜单或执行动作队列。
 *
 * 用法示例：
 * `test: example/main_menu` 直接打开菜单；
 * `test: { actions: ["tell: hello"] }` 执行动作列表。
 */
class CustomCommand(
    private val plugin: KaMenu,
    private val definition: CustomCommandDefinition,
    commandName: String
) : Command(commandName) {

    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        // 自定义指令仅限玩家使用
        if (sender !is Player) {
            sender.sendMessage(plugin.languageManager.getMessage("command.player_only"))
            return true
        }

        when (val commandDefinition = definition) {
            is CustomCommandDefinition.OpenMenu -> {
                MenuUI.openMenu(sender, commandDefinition.menuId, plugin.menuManager, plugin)
            }
            is CustomCommandDefinition.RunActions -> {
                MenuActions.executeStandaloneActions(
                    sender,
                    commandDefinition.actions,
                    buildCommandVariables(commandLabel, args)
                ).whenComplete { _, error ->
                    if (error != null) {
                        plugin.logger.severe(
                            plugin.languageManager.getMessage(
                                "custom_commands.action_execution_failed",
                                commandLabel,
                                error.message ?: error.javaClass.simpleName
                            )
                        )
                        error.printStackTrace()
                    }
                }
            }
        }
        return true
    }

    /**
     * 将命令参数暴露给动作系统。
     *
     * 动作内可读取 `{command}`、`{args}`、`{arg_count}` 和 `{arg:0}`。
     */
    private fun buildCommandVariables(commandLabel: String, args: Array<out String>): Map<String, String> {
        val variables = mutableMapOf<String, String>()
        variables["command"] = commandLabel
        variables["args"] = args.joinToString(" ")
        variables["arg_count"] = args.size.toString()
        args.forEachIndexed { index, value ->
            variables["arg:$index"] = value
        }
        return variables
    }

    /**
     * 为当前正在输入的参数生成补全候选。
     *
     * 候选源会按玩家实时解析，因此可以使用动态 PAPI 或 `{list:*}` / `{glist:*}`。
     */
    override fun tabComplete(
        sender: CommandSender,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (sender !is Player) {
            return emptyList()
        }

        val argumentIndex = (args.size - 1).coerceAtLeast(0)
        val source = definition.argSuggestions[argumentIndex] ?: return emptyList()
        val variables = buildCommandVariables(alias, args)
        val prefix = args.lastOrNull().orEmpty()

        return resolveSuggestions(sender, source, variables)
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .filter { it.lowercase(Locale.ROOT).startsWith(prefix.lowercase(Locale.ROOT)) }
            .toList()
    }

    private fun resolveSuggestions(player: Player, source: Any, variables: Map<String, String>): List<String> {
        return when (source) {
            is Iterable<*> -> source.flatMap { item ->
                if (item == null) {
                    emptyList()
                } else {
                    resolveSuggestions(player, item, variables)
                }
            }
            is Array<*> -> source.flatMap { item ->
                if (item == null) {
                    emptyList()
                } else {
                    resolveSuggestions(player, item, variables)
                }
            }
            else -> resolveSuggestionText(player, source.toString(), variables)
        }
    }

    private fun resolveSuggestionText(player: Player, text: String, variables: Map<String, String>): List<String> {
        val resolved = TextResolver.resolve(player, text, variables).trim()
        if (resolved.isEmpty()) {
            return emptyList()
        }
        return DatabaseManager.decodeStringList(resolved)
    }
}
