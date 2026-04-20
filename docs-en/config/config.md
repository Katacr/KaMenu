# ⛳ Configuration File: config.yml

`config.yml` is the global configuration file for KaMenu, located at `plugins/KaMenu/config.yml`.

---

## 📋 Full Example

```yaml
# KaMenu global configuration file

# Plugin language (corresponds to a filename in the lang/ folder)
language: 'zh_CN'

# BungeeCord support (for the server: action)
bungeecord: true

# Database configuration
storage:
  # Options: sqlite, mysql
  type: 'sqlite'
  host: 'localhost'
  port: 3306
  db: 'minecraft'
  user: 'root'
  password: ''

# Shortcut / listener configuration
listeners:
  # Swap off-hand (default key: F) trigger
  swap-hand:
    enabled: true
    # Menu to open on trigger
    menu: 'main_menu'
    # Whether sneaking is required to trigger
    require-sneaking: true

  # Right-click player listener
  player-click:
    # Enable this listener
    enabled: true
    # Menu to open on trigger
    menu: 'inspect_player'
    # Requires sneaking to trigger (Shift + right-click)
    require-sneaking: true 
    
  # Right-click item with Lore trigger (supports multiple entries)
  item-lore:
    main-menu:  # Config name (custom)
      enabled: true
      # Item material (must match)
      material: 'CLOCK'
      # Target Lore text (triggers if item Lore contains this text)
      target-lore: 'Menu'
      # Menu to open on trigger
      menu: 'main_menu'
      # Whether sneaking is required to trigger
      require-sneaking: false
    # You can add more entries...
    # shop-menu:
    #   enabled: true
    #   material: 'COMPASS'
    #   target-lore: 'Shop'
    #   menu: 'server_shop'
    #   require-sneaking: false

# Custom command registration
# Format: command_name: menu_id
custom-commands:
  zcd: 'main_menu'
  shop: 'server_shop'
  menu: 'main_menu'
```

---

## 🔧 Configuration Reference

### language — Plugin Language

Sets the display language of the plugin. Corresponds to a filename (without `.yml`) in the `plugins/KaMenu/lang/` directory.

**Type:** `String`

**Built-in Options:**

| Value | Language |
|-------|---------|
| `zh_CN` | Simplified Chinese (default) |
| `en_US` | English |

**Example:**

```yaml
language: 'en_US'
```

---

### bungeecord — BungeeCord Support

Configures whether to enable BungeeCord/Velocity proxy support for the `server:` action to transfer players to other servers.

**Type:** `Boolean`

**Default:** `false`

**Values:**

| Value | Description |
|-------|-------------|
| `true` | Enables BungeeCord support — the `server:` action uses the plugin messaging system and requires no player permissions |
| `false` | Disables BungeeCord support — the `server:` action uses the `/server` command (requires the player to have the corresponding permission) |

**Using BungeeCord mode (recommended):**

```yaml
bungeecord: true
```

This mode communicates with the proxy directly via the BungeeCord plugin messaging channel:

- ✅ **No player permission required**: The player does not need permission for `/server`
- ✅ **More reliable**: Does not depend on the command system, better compatibility
- ✅ **Better performance**: Avoids command parsing and permission-check overhead
- ✅ **Standardized**: Consistent with mainstream plugins like DeluxeMenus

**Using command mode (non-proxy servers):**

```yaml
bungeecord: false
```

Suitable for single-server setups or when using another cross-server solution.

{% hint style="info" %}
**Recommendation:**
- If your server runs behind a BungeeCord/Velocity proxy, set this to `true`
- If you are running a standalone server or using another cross-server solution, `false` is fine
- When BungeeCord mode is enabled, the `server:` action automatically uses the plugin messaging system
{% endhint %}

---

### storage — Database Configuration

Configures the persistent storage backend for player data and global data.

**Type:** The `type` field determines which database to use

| Field | Description | Default |
|-------|-------------|---------|
| `type` | Database type: `sqlite` or `mysql` | `sqlite` |
| `host` | MySQL host address | `localhost` |
| `port` | MySQL port | `3306` |
| `db` | MySQL database name | `minecraft` |
| `user` | MySQL username | `root` |
| `password` | MySQL password | _(empty)_ |

**Using SQLite (recommended for single-server):**

```yaml
storage:
  type: 'sqlite'
```

Data is stored in `plugins/KaMenu/storage.db` — no extra configuration needed.

**Using MySQL (recommended for multi-server / large servers):**

```yaml
storage:
  type: 'mysql'
  host: '127.0.0.1'
  port: 3306
  db: 'kamenu_db'
  user: 'mc_user'
  password: 'your_password'
```

---

### listeners — Shortcut Listeners

Configure shortcuts that automatically open menus when players perform certain actions.

#### swap-hand — Swap Off-Hand Key Trigger

Opens a menu when the player presses the swap-hand key (default: `F`).

| Field | Description | Type | Default |
|-------|-------------|------|---------|
| `enabled` | Whether to enable this listener | `Boolean` | `true` |
| `menu` | Menu ID to open on trigger | `String` | `main_menu` |
| `require-sneaking` | Whether the player must also hold Sneak (Shift) to trigger | `Boolean` | `true` |

**Example:**

```yaml
listeners:
  swap-hand:
    enabled: true
    menu: 'server_menu'
    require-sneaking: true   # Shift + F to trigger — prevents accidental activation
```

{% hint style="info" %}
Enabling `require-sneaking` prevents the menu from opening accidentally during normal gameplay. It is recommended to keep this enabled.
{% endhint %}

#### item-lore — Right-Click Item with Lore Trigger

Opens a menu when the player right-clicks while holding an item of a specified material that contains specific Lore text.

**Configuration Format:**

```yaml
listeners:
  item-lore:
    config_name:           # Custom name to differentiate entries
      enabled: true        # Whether to enable this entry
      material: 'CLOCK'              # Item material (must match)
      target-lore: 'Menu'            # Target Lore text
      menu: 'main_menu'              # Menu ID to open on trigger
      require-sneaking: false        # Whether sneaking is required
```

**Field Description:**

| Field | Description | Type | Default |
|-------|-------------|------|---------|
| `enabled` | Whether to enable this listener entry | `Boolean` | `true` |
| `material` | Item material (Material enum value, must match) | `String` | — |
| `target-lore` | Text that must be present in item Lore (partial match) | `String` | — |
| `menu` | Menu ID to open on trigger | `String` | — |
| `require-sneaking` | Whether the player must hold Sneak (Shift) to trigger | `Boolean` | `false` |

**Basic Example:**

```yaml
listeners:
  item-lore:
    server-menu:
      enabled: true
      material: 'CLOCK'
      target-lore: 'Server Menu'
      menu: 'server_menu'
      require-sneaking: false
```

**Multi-entry Example:**

```yaml
listeners:
  item-lore:
    # Main menu: clock item containing "Menu"
    main-menu:
      enabled: true
      material: 'CLOCK'
      target-lore: 'Menu'
      menu: 'main_menu'
      require-sneaking: false

    # Shop menu: compass item containing "Shop"
    shop:
      enabled: true
      material: 'COMPASS'
      target-lore: 'Shop'
      menu: 'server_shop'
      require-sneaking: false

    # Teleport menu: player head item, requires sneaking
    teleport:
      enabled: true
      material: 'PLAYER_HEAD'
      target-lore: 'Teleport'
      menu: 'teleport_menu'
      require-sneaking: true
```

**Use Cases:**

1. **Main Menu Item** — Give new players a special item; right-click opens the main menu
2. **Feature Menus** — Clock/compass items; right-click opens the corresponding feature menu
3. **Special Tools** — Items with specific functions; right-click opens the relevant menu

{% hint style="info" %}
- Multiple item-lore listeners are supported, each with different items and menus
- `target-lore` is a partial match — triggers as long as the item Lore contains the text
- Use unique Lore text for functional items to avoid conflicts with other items
{% endhint %}

{% hint style="warning" %}
**Notes:**
- Color codes in item Lore are stripped during matching (raw text match)
- Ensure Lore text is unique enough to prevent unintended triggers
{% endhint %}

#### player-click — Right-Click Player Trigger

Opens a menu when a player right-clicks another player. Supports both plain right-click and Shift+right-click.

**Configuration Format:**

```yaml
listeners:
  player-click:
    enabled: false              # Whether to enable this listener
    menu: 'inspect_player'      # Menu ID to open on trigger
    require-sneaking: false     # Whether sneaking is required to trigger
```

**Field Description:**

| Field | Description | Type | Default |
|-------|-------------|------|---------|
| `enabled` | Whether to enable this listener | `Boolean` | `false` |
| `menu` | Menu ID to open on trigger | `String` | — |
| `require-sneaking` | Whether the player must hold Sneak (Shift) to trigger | `Boolean` | `false` |

**Basic Example:**

```yaml
listeners:
  player-click:
    enabled: true
    menu: 'inspect_player'
    require-sneaking: false
```

**Shift+Right-Click Example:**

```yaml
listeners:
  player-click:
    enabled: true
    menu: 'inspect_player'
    require-sneaking: true   # Only triggers on Shift + right-click
```

**Meta Data Set on Trigger:**

When the `player-click` listener triggers, the system automatically sets a meta entry:

- **Meta Key**: `player`
- **Meta Value**: The name of the clicked player
- **Usage**: Reference the clicked player in the menu via `{meta:player}`

**Example with Slot References:**

```yaml
# config.yml
listeners:
  player-click:
    enabled: true
    menu: 'inspect_player'
    require-sneaking: false

# menus/inspect_player.yml
Body:
  helmet:
    type: 'item'
    material: '[HEAD:{meta:player}]'  # Show the clicked player's helmet
    width: 32
    height: 32

  chestplate:
    type: 'item'
    material: '[CHEST:{meta:player}]'  # Show the clicked player's chestplate
    width: 32
    height: 32
```

**Use Cases:**

1. **Player Interaction Menu** — Right-click a player to view their equipment and interact (private message, teleport, friend request, etc.)
2. **Admin Tools** — Right-click a player to quickly open the admin menu (view info, ban, teleport, etc.)
3. **Roleplay Servers** — Right-click a player to view character info and interact

{% hint style="info" %}
- Disabled by default (`enabled: false`); must be manually enabled
- `{meta:player}` is only available in menus opened via the player-click listener
- Combine with slot reference features to display the clicked player's equipment
{% endhint %}

{% hint style="warning" %}
**Notes:**
- If the target player is offline or does not exist, right-clicking will not trigger the menu
- Slot references (e.g. `[HEAD:{meta:player}]`) are required to display the clicked player's equipment
- The right-click event is cancelled — other plugins' right-click-player events will not fire
{% endhint %}

---

### custom-commands — Custom Commands

Registers short custom commands as shortcuts to open specified menus, without any additional permission configuration.

**Format:** `command_name: menu_id`

**Example:**

```yaml
custom-commands:
  shop: 'server_shop'       # /shop -> opens server_shop menu
  menu: 'main_menu'         # /menu -> opens main_menu menu
  hub: 'hub/main'           # /hub  -> opens hub/main menu (subdirectory)
```

To learn more about custom commands and their advantages, see [⌨️ Custom Commands](customCommands.md)
