@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.Locale

sealed interface CustomCommandDefinition {
    val argSuggestions: Map<Int, Any>

    data class OpenMenu(
        val menuId: String,
        override val argSuggestions: Map<Int, Any> = emptyMap()
    ) : CustomCommandDefinition

    data class RunActions(
        val actions: List<Any>,
        override val argSuggestions: Map<Int, Any> = emptyMap()
    ) : CustomCommandDefinition
}

/**
 * 自定义指令处理器
 * 用于处理 config.yml 中配置的自定义指令，可直接打开菜单或执行动作队列。
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
