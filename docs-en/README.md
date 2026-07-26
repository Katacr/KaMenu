---
description: KaMenu - A modern Dialog menu plugin for Paper, Folia, and Spigot
---

# 🏠 Home

> A next-generation Minecraft GUI plugin powered by the native Dialog API — a fresh interactive experience beyond traditional chest menus

**KaMenu** is a native Dialog menu plugin for modern Minecraft servers. Paper/Folia use the Paper Dialog API and Spigot 1.21.6+ uses the Bungee Dialog API. The same YAML configuration and plugin JAR run on all three server platforms.

> **⚠️ Important:**
> - **Paper/Folia minimum:** 1.21.7
> - **Spigot Dialog minimum:** Spigot 1.21.6
> - **Recommended version:** Paper 1.21.8+
> - **Advanced text components:** Minecraft 1.21.9+ (supports sprite and other newer client text components)
> - **Supported server software:** Paper, Folia, Spigot, and compatible forks
>
> Some features that depend on the Paper API may not be available on Spigot. We still recommend Paper for full functionality and better compatibility.

KaMenu supports Folia's region-threaded execution model. Menu opening, delayed actions, periodic tasks, JavaScript delay helpers, and the external menu API automatically use the appropriate Paper or Folia scheduler. Custom JavaScript, external action handlers, and third-party PlaceholderAPI expansions must still follow Folia's threading rules.

***

## ✨ Core Features

### 🖥️ Modern GUI Based on the Dialog API

Say goodbye to chest menus and embrace native UI:

* A brand-new Paper Dialog menu interface with a more modern look
* Multiple rich interactive components: text inputs, numeric sliders, single-option buttons, checkboxes
* Supports `Item` (item display) and `Message` (plain text) content components
* Three bottom button layout modes: `notice`, `confirmation`, `multi`

### 🔧 Highly Customizable

* Fully YAML-based configuration — no programming knowledge required
* Supports multi-level folder structures for easy management of large numbers of menus
* Hot-reload support — no server restart needed after editing configs

### 🔀 Powerful Action System

Supports a wide range of button click actions:

* `tell` / `actionbar` / `title` — multiple message delivery methods
* `command` / `console` — execute player or console commands
* `sound` — play sounds (with volume, pitch, and category parameters)
* `open` / `close` — menu navigation and dismissal
* `hovertext` — hoverable and clickable chat text
* `actions` — execute predefined action lists (supports reuse and conditional branching)
* `wait` / `return` — delay subsequent actions or stop the current action chain
* `set-data` / `set-gdata` — read/write persistent data
* `url` / `copy` — open links or copy to clipboard

Advanced action and text features:

* `{js:...}` — use JavaScript expression or JavaScript package results in any text position; no return value resolves to empty text
* `Events.Open` — waits for the full action chain before opening the menu
* `Events.Tasks` — run action groups periodically while the menu is open

### 🔍 Universal Condition System

* Use conditions in **any text field** (title, button text, component text)
* Nest conditions within **action lists** for branching execution logic
* Supports PlaceholderAPI variables, comparison operators (`==` `!=` `>` `<` `>=` `<=`), and logical operators (`&&` `||`)
* `{js:...}` can be used directly in conditions for dynamic evaluation

### 💾 Built-in Data Storage

* Supports **SQLite** (default) and **MySQL**
* **Player data** (`{data:key}`): individual key-value pairs stored by player UUID
* **Global data** (`{gdata:key}`): key-value pairs shared across all players
* Data variables are exposed externally via **PlaceholderAPI**

### 🌐 Multiple Ways to Open Menus

* `/kamenu guide` — open the built-in getting started guide for first-time setup, language selection, and example release
* `/km open <menuId>` — standard command
* **Hotkey listener:** configure pressing `F` (swap offhand) to open a specified menu
* **Custom command registration:** map any word to a menu with a single line of config
* **External plugin API:** other plugins can open file menus or render in-memory YAML / `YamlConfiguration` menus without writing to the `menus` directory or reloading

### 📊 PlaceholderAPI Support

* Full PAPI variable parsing (usable in menu titles, component text, and actions)
* Provides `%kamenu_data_<key>%`, `%kamenu_gdata_<key>%`, `%kamenu_list_<key>%`, `%kamenu_glist_<key>%`, and `%kamenu_online_players%` variables

***

## 💰 Support

KaMenu is a free and open-source plugin. You can download the source code from GitHub and build the latest experimental features yourself.

{% embed url="https://github.com/Katacr/KaMenu/" %}


***

## 🤝 Community & Feedback

* **GitHub**: [Katacr/KaMenu](https://github.com/Katacr/KaMenu/)
* **Issue Tracker**: [GitHub Issues](https://github.com/Katacr/KaMenu/issues)

## 📄 License

This project is open-sourced under the [GPL-3.0](https://www.gnu.org/licenses/gpl-3.0.html) license.
