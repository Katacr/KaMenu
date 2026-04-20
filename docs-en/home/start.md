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
   - Generate the default `config.yml` and sample menus
   - Initialize the database (SQLite by default)

{% hint style="info" %}
After the first startup, sample menu files are automatically generated in `plugins/KaMenu/menus/`. You can reference or modify them directly.
{% endhint %}

---

## 🧪 Verify Installation

After starting the server, the console should show the KaMenu startup banner, including the version, database type, and number of loaded menus.

You can also verify in-game with:

```
/km open example/actions_demo
```

If a demo menu pops up, the installation is working correctly.

---

## 🔄 Hot Reload

After modifying configuration or menu files, reload without restarting the server:

```
/km reload
```

Requires the `kamenu.admin` permission.
