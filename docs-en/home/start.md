# 🚀 Quick Start

This guide will help you quickly install and configure the KaMenu plugin.

---

## 🎮 System Requirements

| Item | Details |
|------|---------|
| Minecraft Version | 1.21.7+ |
| Java Version | Java 21+ |
| Server Type | **Paper** and its forks (Purpur, etc.) |
| Database | SQLite (default), MySQL 5.7+ |

{% hint style="info" %}
**Version Feature Support**: 

- ✅ Paper 1.21.7+: Full basic functionality
- ✨ Paper 1.21.8+: Recommended — more stable API
- 🎨 Paper 1.21.9+: Full features (sprite item icons, player head avatars, and more)
{% endhint %}

{% hint style="warning" %}
KaMenu depends on the Paper Dialog API and does **not support**: 

- Paper 1.21.6 or below (API not fully implemented)
- Spigot, CraftBukkit, or other non-Paper server software

Please ensure your server runs **Paper 1.21.7 or higher**!
{% endhint %}

---

## 📥 Installation

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

### 3. Install the Plugin

1. Place the downloaded KaMenu `.jar` file into your server's `plugins` folder
2. Start the server
3. The plugin will automatically:
   - Create the `plugins/KaMenu/` configuration directory
   - Generate the default `config.yml`
   - Initialize the database (SQLite by default)

{% hint style="info" %}
For first-time setup, run `/kamenu guide` (or `/km guide`) in game to open the getting started guide. The guide menu is loaded directly from inside the plugin jar into memory and is not written to the `menus` directory.
{% endhint %}

### 4. Open the Getting Started Guide

After the server starts, a player with the `kamenu.admin` permission can run:

```bash
/kamenu guide
```

The guide helps you set the plugin language and release sample menus for the selected language. Sample menus are written to:

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

## 🧪 Verify Installation

After starting the server, the console should show the KaMenu startup banner, including the version, database type, and number of loaded menus.

You can also verify in-game with:

```
/kamenu guide
```

If the guide opens, the installation is working correctly. After releasing the examples, you can also run `/km open example/actions_demo` to open the actions demo menu.

---

## 🔄 Hot Reload

After modifying configuration or menu files, reload without restarting the server:

```
/km reload
```

Requires the `kamenu.admin` permission.
