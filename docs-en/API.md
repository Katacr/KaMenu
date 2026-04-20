# KaMenu API Documentation

KaMenu provides a public API that allows other plugins to open KaMenu menus.

## API Class

### `org.katacr.kamenu.api.KaMenuAPI`

#### Methods

##### `openMenu(Player player, String menuId)`

Opens the specified menu.

**Parameters:**
- `player` — The target player
- `menuId` — The menu ID (e.g. `"main_menu"` or `"shop/weapons"`)

**Returns:**
- `boolean` — Whether the menu was successfully opened

**Example:**

```kotlin
import org.katacr.kamenu.api.KaMenuAPI
import org.bukkit.entity.Player

// Open the main menu
val success = KaMenuAPI.openMenu(player, "main_menu")
if (!success) {
    player.sendMessage("§cFailed to open menu!")
}

// Open a menu in a subdirectory
KaMenuAPI.openMenu(player, "shop/weapons")
```

```java
import org.katacr.kamenu.api.KaMenuAPI;
import org.bukkit.entity.Player;

// Open the main menu
boolean success = KaMenuAPI.openMenu(player, "main_menu");
if (!success) {
    player.sendMessage("§cFailed to open menu!");
}

// Open a menu in a subdirectory
KaMenuAPI.openMenu(player, "shop/weapons");
```

##### `isAvailable()`

Checks whether KaMenu is loaded and available.

**Returns:**
- `boolean` — Whether KaMenu is available

**Example:**

```kotlin
if (KaMenuAPI.isAvailable()) {
    KaMenuAPI.openMenu(player, "main_menu")
} else {
    player.sendMessage("§cKaMenu plugin is not installed or not enabled")
}
```

##### `getPlugin()`

Gets the KaMenu plugin instance.

**Returns:**
- `KaMenuPlugin` — The KaMenu plugin instance, or null if not loaded

## Using in Other Plugins

### Method 1: Reflection (No Dependency Required)

If your plugin does not need to depend on KaMenu directly, you can call the API via reflection:

```kotlin
fun openKaMenu(player: Player, menuId: String): Boolean {
    try {
        val kaMenuPlugin = player.server.pluginManager.getPlugin("KaMenu")
        if (kaMenuPlugin == null) {
            player.sendMessage("§cKaMenu plugin is not installed or not enabled")
            return false
        }

        val apiClass = Class.forName("org.katacr.kamenu.api.KaMenuAPI")
        val openMenuMethod = apiClass.getMethod("openMenu", org.bukkit.entity.Player::class.java, String::class.java)
        val result = openMenuMethod.invoke(null, player, menuId)
        return result == true
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
}
```

### Method 2: Adding a Dependency (Recommended)

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

## Using in Actions

The KaGuilds plugin already integrates the KaMenu API, and you can use the `kamenu:` action in menu configurations:

```yaml
# KaGuilds menu config example
guild_manage:
  type: NOTICE
  layout: ...
  buttons:
    kamenu_demo:
      icon: BOOK
      text: "&aOpen KaMenu Demo"
      actions:
        right:
          - 'kamenu: condition_demo'
        left:
          - 'tell: &aOpening KaMenu...'
          - 'kamenu: example/actions_demo'
```

## Notes

1. **Plugin Dependency**: Ensure KaMenu is installed and enabled
2. **Menu Existence**: Ensure the target menu ID exists
3. **Async Calls**: API methods are synchronous, but may need to be called asynchronously in some contexts
4. **Error Handling**: It is recommended to add proper error handling to provide feedback when an API call fails

## Full Example

Below is a complete plugin example demonstrating how to use the KaMenu API:

```kotlin
package com.example.plugin

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.katacr.kamenu.api.KaMenuAPI

class ExamplePlugin : JavaPlugin() {
    override fun onEnable() {
        logger.info("ExamplePlugin enabled")

        // Check if KaMenu is available
        if (!KaMenuAPI.isAvailable()) {
            logger.warning("KaMenu plugin not installed, some features will be unavailable")
        }
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (command.name.equals("openmenu", ignoreCase = true)) {
            if (sender !is Player) {
                sender.sendMessage("§cThis command can only be used by players")
                return true
            }

            if (!KaMenuAPI.isAvailable()) {
                sender.sendMessage("§cKaMenu plugin is not installed or not enabled")
                return true
            }

            val menuId = if (args.isNotEmpty()) args[0] else "main_menu"
            val success = KaMenuAPI.openMenu(sender, menuId)

            if (success) {
                sender.sendMessage("§aSuccessfully opened menu: $menuId")
            } else {
                sender.sendMessage("§cFailed to open menu, please check the menu ID")
            }
            return true
        }
        return false
    }
}
```

Corresponding `plugin.yml`:

```yaml
name: ExamplePlugin
version: 1.0.0
main: com.example.plugin.ExamplePlugin
api-version: "1.21"
depend:
  - KaMenu

commands:
  openmenu:
    description: Open a KaMenu menu
    usage: /openmenu [menuId]
    permission: exampleplugin.openmenu
    permission-message: "§cYou do not have permission to use this command"
```
