# KaMenu API Documentation

KaMenu provides a public API for other plugins to open file-based menus, render in-memory YAML menus, and register custom action namespaces.

## API Class

### `org.katacr.kamenu.api.KaMenuAPI`

#### `openMenu(Player player, String menuId)`

Opens a menu loaded by KaMenu's `MenuManager`.

**Parameters:**
- `player` — The target player
- `menuId` — The menu ID, such as `"main_menu"` or `"shop/weapons"`

**Returns:**
- `boolean` — Whether the open request was submitted successfully

```kotlin
import org.katacr.kamenu.api.KaMenuAPI

val success = KaMenuAPI.openMenu(player, "main_menu")
KaMenuAPI.openMenu(player, "shop/weapons")
```

```java
import org.katacr.kamenu.api.KaMenuAPI;

boolean success = KaMenuAPI.openMenu(player, "main_menu");
KaMenuAPI.openMenu(player, "shop/weapons");
```

#### `openYaml(Player player, String yaml, String contextId = "external")`

Parses a YAML string in memory and opens it as a KaMenu menu. The YAML is not written to the `menus` directory and does not require a menu reload.

**Parameters:**
- `player` — The target player
- `yaml` — Full KaMenu menu YAML content
- `contextId` — A log identifier used to locate the source of the external menu

**Returns:**
- `boolean` — Whether the YAML was parsed and the open request was submitted successfully

```kotlin
val yaml = """
Title: "&aGame Center"
Body:
  welcome:
    type: message
    text: "&7Welcome, %player_name%!"
Bottom:
  type: notice
  confirm:
    text: "&aJoin"
    actions:
      - "kgc:join lobby"
""".trimIndent()

KaMenuAPI.openYaml(player, yaml, "kagamecenter:main")
```

If YAML parsing fails, KaMenu writes a warning containing the `contextId`.

#### `openConfig(Player player, YamlConfiguration config, String contextId = "external")`

Opens an in-memory `YamlConfiguration` as a KaMenu menu. The configuration does not need to come from `MenuManager`.

**Parameters:**
- `player` — The target player
- `config` — A complete KaMenu menu configuration
- `contextId` — A log identifier used to locate the source of the external menu

**Returns:**
- `boolean` — Whether the open request was submitted successfully

```kotlin
import org.bukkit.configuration.file.YamlConfiguration
import org.katacr.kamenu.api.KaMenuAPI

val config = YamlConfiguration()
config.loadFromString(yaml)

KaMenuAPI.openConfig(player, config, "myplugin:dynamic-shop")
```

`Events.Open` is executed before rendering. If the `Open` event returns with `return`, the menu is not opened. If the external menu uses `reset` and no file menu ID exists, KaMenu ignores the reset action.

#### `registerActionHandler(String namespace, KaMenuActionHandler handler)`

Registers a custom action namespace. Actions matching `namespace:payload` are offered to the registered handler before KaMenu's built-in action logic continues.

**Parameters:**
- `namespace` — The action namespace without `:`, such as `"kgc"`
- `handler` — The custom action handler

**Returns:**
- `boolean` — Whether the handler was registered successfully

```kotlin
import org.katacr.kamenu.api.KaMenuAPI

KaMenuAPI.registerActionHandler("kgc") { player, action, variables, config ->
    val payload = action.removePrefix("kgc:").trim()

    when {
        payload == "open-main" -> {
            KaMenuAPI.openYaml(player, buildMainMenuYaml(player), "kagamecenter:main")
            true
        }
        payload.startsWith("join ") -> {
            val arenaId = payload.removePrefix("join ").trim()
            joinArena(player, arenaId)
            true
        }
        else -> false
    }
}
```

Menu YAML can then use the namespace anywhere an action list is supported:

```yaml
Bottom:
  type: notice
  confirm:
    text: "&aJoin"
    actions:
      - "kgc:join lobby"
```

The handler receives the resolved action string, input variables such as `$(name)`, and the current menu config when available. Returning `true` means the action was handled. Returning `false` lets KaMenu continue with its normal action handling. Handler exceptions are logged with the namespace and action.

#### `unregisterActionHandler(String namespace)`

Unregisters a custom action namespace.

```kotlin
KaMenuAPI.unregisterActionHandler("kgc")
```

#### `isAvailable()`

Checks whether KaMenu is loaded and available.

```kotlin
if (KaMenuAPI.isAvailable()) {
    KaMenuAPI.openMenu(player, "main_menu")
}
```

#### `getPlugin()`

Gets the KaMenu plugin instance, or `null` if KaMenu is not loaded.

## Action Handler Interface

```kotlin
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
```

Choose a unique namespace for your plugin. Avoid using built-in action names such as `tell`, `open`, `command`, `console`, or `js` unless you intentionally want to intercept that namespace.

## Using in Other Plugins

### Method 1: Reflection

If your plugin does not need a direct compile-time dependency on KaMenu, you can call the API via reflection:

```kotlin
fun openKaMenu(player: Player, menuId: String): Boolean {
    return try {
        val kaMenuPlugin = player.server.pluginManager.getPlugin("KaMenu") ?: return false
        val apiClass = Class.forName("org.katacr.kamenu.api.KaMenuAPI")
        val openMenuMethod = apiClass.getMethod("openMenu", Player::class.java, String::class.java)
        openMenuMethod.invoke(null, player, menuId) == true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
```

### Method 2: Plugin Dependency

For type-safe API access, add KaMenu as a dependency in your `plugin.yml`:

```yaml
name: YourPlugin
version: 1.0.0
main: com.yourname.yourplugin.Main
api-version: "1.21"
depend:
  - KaMenu
```

Then use the API directly:

```kotlin
import org.katacr.kamenu.api.KaMenuAPI

fun openShop(player: Player) {
    KaMenuAPI.openMenu(player, "shop/weapons")
}
```

## Notes

1. **Plugin dependency**: Ensure KaMenu is installed and enabled before calling the API.
2. **Menu source**: Use `openMenu` for file menus and `openYaml` or `openConfig` for dynamic in-memory menus.
3. **Threading**: `openConfig` and `openYaml` can be called from async code; KaMenu schedules the actual dialog rendering back to the main server thread.
4. **Open events**: In-memory menus still execute `Events.Open` before rendering.
5. **Error handling**: Check the boolean return value and provide user feedback when an API call fails.

## Complete Example

```kotlin
package com.example.plugin

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.katacr.kamenu.api.KaMenuAPI

class ExamplePlugin : JavaPlugin() {
    override fun onEnable() {
        if (!KaMenuAPI.isAvailable()) {
            logger.warning("KaMenu plugin not installed, some features will be unavailable")
        }

        KaMenuAPI.registerActionHandler("example") { player, action, variables, config ->
            val payload = action.removePrefix("example:").trim()
            if (payload == "reward") {
                player.sendMessage("§aReward action handled by ExamplePlugin")
                true
            } else {
                false
            }
        }
    }

    override fun onDisable() {
        KaMenuAPI.unregisterActionHandler("example")
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!command.name.equals("openmenu", ignoreCase = true)) {
            return false
        }
        if (sender !is Player) {
            sender.sendMessage("§cThis command can only be used by players")
            return true
        }

        val yaml = """
        Title: "&aExample Menu"
        Body:
          text:
            type: message
            text: "&7This menu is rendered from memory."
        Bottom:
          type: notice
          confirm:
            text: "&aReward"
            actions:
              - "example:reward"
        """.trimIndent()

        val success = KaMenuAPI.openYaml(sender, yaml, "example:command")
        if (!success) {
            sender.sendMessage("§cFailed to open menu")
        }
        return true
    }
}
```
