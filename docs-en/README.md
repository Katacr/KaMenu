---
description: KaMenu - A modern Dialog menu plugin for Minecraft Paper servers
---

# 🏠 Home

> The next-generation Minecraft GUI plugin powered by the Paper Dialog API — a fresh interactive experience beyond traditional chest menus

**KaMenu** is a GUI plugin built for modern Minecraft Paper servers. It abandons the traditional Inventory (chest) menu pattern in favor of the **native Paper 1.21.7+ Dialog API**, providing players with a modern menu interface featuring rich interactive components such as text inputs, sliders, dropdowns, and checkboxes. Configuration is YAML-based — clean, intuitive, and ready to use out of the box.

> **⚠️ Important:**
> - **Minimum version:** Paper 1.21.7
> - **Recommended version:** Paper 1.21.8+
> - **Full feature version:** Paper 1.21.9+ (supports sprite item icons, player head avatars, and other advanced features)
>
> KaMenu **does not support** Paper 1.21.6 or earlier. Make sure your server meets the version requirements!

***

## ✨ Core Features

### 🖥️ Modern GUI Based on the Dialog API

Say goodbye to chest menus and embrace native UI:

* A brand-new Paper Dialog menu interface with a more modern look
* Multiple rich interactive components: text inputs, numeric sliders, dropdown selects, checkboxes
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
* `wait` — delay subsequent actions
* `set-data` / `set-gdata` — read/write persistent data
* `url` / `copy` — open links or copy to clipboard

### 🔍 Universal Condition System

* Use conditions in **any text field** (title, button text, component text)
* Nest conditions within **action lists** for branching execution logic
* Supports PlaceholderAPI variables, comparison operators (`==` `!=` `>` `<` `>=` `<=`), and logical operators (`&&` `||`)

### 💾 Built-in Data Storage

* Supports **SQLite** (default) and **MySQL**
* **Player data** (`{data:key}`): individual key-value pairs stored by player UUID
* **Global data** (`{gdata:key}`): key-value pairs shared across all players
* Data variables are exposed externally via **PlaceholderAPI**

### 🌐 Multiple Ways to Open Menus

* `/km open <menuId>` — standard command
* **Hotkey listener:** configure pressing `F` (swap offhand) to open a specified menu
* **Custom command registration:** map any word to a menu with a single line of config

### 📊 PlaceholderAPI Support

* Full PAPI variable parsing (usable in menu titles, component text, and actions)
* Provides `%kamenu_data_<key>%` and `%kamenu_gdata_<key>%` variables

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
