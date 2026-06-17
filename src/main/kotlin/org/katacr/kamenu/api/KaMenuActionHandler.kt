package org.katacr.kamenu.api

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player

fun interface KaMenuActionHandler {
    fun execute(
        player: Player,
        action: String,
        variables: Map<String, String>,
        rawConfig: YamlConfiguration?
    ): Boolean
}
