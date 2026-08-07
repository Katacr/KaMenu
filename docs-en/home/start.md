# Quick Start

This guide will help you quickly install and configure the KaMenu plugin.

---

## System Requirements

| Item | Details |
|------|---------|
| Minecraft Version | 1.16.5+ |
| Java Version | Java 16+ |
| Server Type | **Paper**, **Folia**, **Spigot**, and compatible forks |
| Database | SQLite (default), MySQL 5.7+ |

{% hint style="info" %}
**Version Feature Support**: 

- ✅ Java 16+: The shared plugin runtime can load on Java 16 and newer
- ✅ Bukkit/Spigot/Paper-compatible core 1.16.5+: Container menus, actions, variables, JavaScript, storage, and custom commands
- ✅ Paper/Folia 1.21.7+: Native Dialogs enabled
- Paper 1.21.8+: Recommended — more stable API
- Minecraft 1.21.9+: Supports sprite and other newer client text components with the same menu syntax on Paper, Folia, and Spigot
- Folia 1.21.7+: Region-threaded scheduling support; use a current build matching the target Minecraft version
- Spigot 1.21.6+: Native Dialogs plus server-side actions, Events, Inputs, Tasks, JavaScript, storage, and external APIs
- Cores below the native Dialog minimum: Dialog menus and the ESC Dialog entry are disabled automatically; shared features remain available
{% endhint %}

{% hint style="info" %}
**Folia compatibility:** KaMenu detects Folia automatically and schedules player menus, `wait`, `Events.Tasks`, JavaScript `delay()`, and menu API calls on the appropriate player or global scheduler. Custom JavaScript, external action handlers, PlaceholderAPI expansions, and commands from other plugins invoked through `console:` must also be Folia-compatible.
{% endhint %}

---

## Installation

### 1. Download the Plugin

Build from source on GitHub:

{% embed url="https://github.com/Katacr/KaMenu/releases" %}

Or download from these plugin distribution platforms:

{% embed url="https://www.spigotmc.org/resources/133736/" %}

{% embed url="https://www.minebbs.com/resources/15814/" %}

### 2. Install Optional Dependencies

All KaMenu features work standalone — no hard dependencies. The following are optional:

**Optional Dependencies:**
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) — Use `%variable%` PAPI placeholders in menus
- [Vault](https://www.spigotmc.org/resources/vault.34315/) — Economy integration (if you need to manipulate player balances in actions)
- ItemsAdder — Resolve `:glyph_id:` and `:offset_pixels:` in Dialog text and use ItemsAdder custom items
- Oraxen — Resolve `<glyph:glyph_id>` and `<shift:pixels>` in Dialog text and use Oraxen custom items
- CraftEngine — Display `<image:namespace:id>` and `<shift:pixels>` through CraftEngine's Dialog packet interceptor and use CraftEngine custom items

{% hint style="info" %}
ItemsAdder, Oraxen, and CraftEngine are soft dependencies; KaMenu still starts when they are absent. Keep `network.intercept-packets.dialog: true` enabled when using CraftEngine glyphs. Fully restart the server after installing or removing any of these plugins so the soft-dependency load order is applied.
{% endhint %}

### 3. Install the Plugin

1. Place the downloaded KaMenu `.jar` file into your server's `plugins` folder
2. Start the server
3. The plugin will automatically:
   - Create the `plugins/KaMenu/` configuration directory
   - Generate the default `config.yml`
   - Initialize the database (SQLite by default)

{% hint style="info" %}
KaMenu bundles only Libby in the plugin JAR. During `onLoad`, Libby hot-loads Kotlin first and then mounts Adventure/MiniMessage, database drivers, the JavaScript engine, and other runtime libraries. The first startup requires Maven repository access; cached dependencies are not downloaded again on later startups.
{% endhint %}

{% hint style="info" %}
For first-time setup, run `/kamenu guide` (or `/km guide`) in game. Platforms with Dialog support open the Dialog guide, while older platforms without Dialog support automatically fall back to the container guide. The guide is loaded directly from inside the plugin jar into memory and is not written to the `menus` directory.
{% endhint %}

### 4. Open the Getting Started Guide

After the server starts, a player with the `kamenu.admin` permission can run:

```bash
/kamenu guide
```

The guide helps you set the plugin language and release sample menus for the selected language. Older platforms without Dialog support release only chest, furnace, and anvil Container examples. Platforms with Dialog support release both Container and Dialog examples. Sample menus are written to:

```text
plugins/KaMenu/menus/example/
```

You can also release sample menus directly with commands:

```bash
# Release examples using the current plugin language
/kamenu examples

# Release Chinese examples
/kamenu examples zh_CN

# Release English examples
/kamenu examples en_US
```

{% hint style="info" %}
When no menus are loaded and an OP player joins the server, KaMenu sends a clickable guide prompt to make first-time setup easier.
{% endhint %}

---

## Verify Installation

After starting the server, the console should show the KaMenu startup banner, including the version, database type, and number of loaded menus.

You can also verify in-game with:

```
/kamenu guide
```

If the guide opens, the installation is working correctly. On older platforms, run `/km open example/container_main` after releasing examples to open the container example. Platforms with Dialog support can also run `/km open example/actions_demo` for the Dialog actions example.

---

## Hot Reload

After modifying configuration or menu files, reload without restarting the server. If no target is provided, all modules are reloaded:

```
/km reload
```

Common targeted reloads:

```bash
/km reload menu      # Reload menus only
/km reload config    # Reload config.yml, custom_commands.yml, language files, and custom commands
/km reload actions   # Reload global action packages only
/km reload js        # Reload global JavaScript packages only
/km reload lang      # Reload the current language file only
```

Requires the `kamenu.admin` permission.
