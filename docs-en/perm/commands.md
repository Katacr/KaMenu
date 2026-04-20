# ⌨️ Command List

KaMenu provides a concise command structure. The main command is `/km` (aliases: `/kamenu`, `/menu`).

---

## Main Command

```
/km <subcommand> [arguments]
```

**Aliases:** `/kamenu`, `/menu`

---

## Subcommand Details

### /km help

Displays help information for all commands.

**Format:** `/km help`

**Permission:** None (available to all players)

**Example:**

```bash
/km help
```

**Output includes:**
- Plugin version information
- List of all available subcommands
- Brief description of each command

{% hint style="info" %}
Typing `/km` without any arguments also displays the help information.
{% endhint %}

---

### /km open

Opens a specified menu.

**Format:**
- `/km open <menu-id>` — Opens a menu for yourself (players only)
- `/km open <menu-id> <player>` — Opens a menu for a specified player (console-compatible)

**Permission:** `kamenu.admin`

**Tab completion:** After typing `/km open `, press Tab to auto-complete all loaded menu IDs (including subfolder paths).

**Examples:**

```bash
# Player opens a menu for themselves
/km open main_menu

# Player opens a menu for another player
/km open shop/weapons Player1

# Console opens a menu for a specified player (player name required)
/km open example/actions_demo Player1

# Open a menu in a subfolder
/km open shop/weapons
```

**Notes:**
- If the menu ID does not exist, the player receives an error message
- Players can omit the second argument to open the menu for themselves by default
- The console must specify the second argument (player name)
- If the specified player does not exist, an error message is displayed

{% hint style="warning" %}
Because this command can open any menu, it requires the `kamenu.admin` permission. It is recommended to use [Custom Commands](../config/customCommands.md) to provide players with a safer way to access menus.
{% endhint %}

---

### /km list

View all loaded menus on the server.

**Format:** `/km list [page]`

**Permission:** `kamenu.admin`

**Features:**
- Displays all loaded menus (10 per page)
- Click a menu name to open it directly
- Supports pagination
- Uses clickable text for convenient interaction

**Examples:**

```bash
# View the first page of the menu list
/km list

# View a specific page
/km list 2
```

**Display output:**

```

§6§lMenu List
§f1. example/main_menu §e[Click to open]
§f2. example/shop_menu §e[Click to open]
§f3. example/vip_menu §e[Click to open]
...
§7[Previous]  §7Page 1/3  §e[Next]

```

---

### /km reload

Reloads all plugin configuration files and menu files without restarting the server.

**Format:** `/km reload`

**Permission:** `kamenu.admin`

**Reloaded items:**
1. `config.yml` global configuration
2. Language files (`lang/` directory)
3. All menu files in the `menus/` directory (including subfolders)
4. Custom command registrations

**Example:**

```bash
/km reload
# Output: [KaMenu] Menus reloaded. Loaded 12 menus and 3 custom commands.
```

{% hint style="info" %}
After modifying menu files, simply run `/km reload` to apply changes immediately — no server restart required.
{% endhint %}

---

### /km item

Manage saved items, including saving held items, giving saved items, and deleting saved items.

**Format:**
- `/km item save <item-name>` — Save the held item to the database (players only)
- `/km item give <item-name>` — Give yourself 1 item (players only)
- `/km item give <item-name> <amount>` — Give yourself a specified number of items (players only)
- `/km item give <item-name> <player>` — Give 1 item to a specified player (console-compatible)
- `/km item give <item-name> <player> <amount>` — Give a specified number of items to a specified player (console-compatible)
- `/km item delete <item-name>` — Delete a saved item

**Permission:** `kamenu.admin`

**Details:**
- Items are serialized in Base64 format and stored in the database
- Supports all item types (including items with NBT tags)
- Can save complex items such as enchanted items and items with custom textures
- When saving, the item count is automatically set to 1 to avoid storing quantity information
- When giving items, you can specify a quantity (range: 1–64)
- Deleting an item permanently removes it from the database

**Examples:**

```bash
# Save the held item
/km item save diamond_sword
/km item save vip_reward

# Give yourself 1 item
/km item give diamond_sword

# Give yourself 10 items
/km item give diamond_sword 10

# Give 1 item to a specified player
/km item give vip_reward Player1

# Give 10 items to a specified player
/km item give vip_reward Player1 10

# Console gives 1 item to a specified player (player name required)
/km item give diamond_sword Player1

# Console gives multiple items to a specified player
/km item give diamond_sword Player1 5

# Delete a saved item
/km item delete diamond_sword
/km item delete vip_reward
```

**Tab completion:**
- After `/km item `, Tab shows subcommands (save, give, delete)
- After `/km item give `, Tab shows all saved item names
- After `/km item delete `, Tab shows all saved item names
- After an item name, Tab shows all online players (give command only)

{% hint style="warning" %}
- The `save` command can only be used by players; the console cannot use it
- In the `give` command, the player argument is optional for players (defaults to self) but required for the console
- The amount argument is optional and defaults to 1 (range: 1–64)
- Cannot save if no item is held
- If the player's inventory is full, excess items cannot be given
{% endhint %}

---

### /km action

Test and execute a specified action for debugging and verifying action configurations.

**Format:** `/km action <player> <action>`

**Permission:** `kamenu.admin`

**Notes:** This command can be used by both players and the console; a target player must always be specified.

**Supported action types:**
- `tell:message` — Send a chat message
- `actionbar:message` — Send an ActionBar message
- `title:params` — Send a title
- `sound:params` — Play a sound
- `command:command` — Player executes a command
- `console:command` — Console executes a command
- `data:operation` — Player data operation
- `gdata:operation` — Global data operation
- `meta:operation` — Metadata operation


  For a full list of action types, see [🤖 Actions](../menu/actions.md).

---

**Examples:**

```bash
# Send a message
/km action Player1 tell:Hello World

# Play a sound
/km action Player2 sound:block_note_block_harp;volume=1.0;pitch=1.0

# Send a title
/km action Player3 title:title=Test;subtitle=Subtitle

# Operate data
/km action Player4 data:type=set;key=test;var=100

# Supports variables and PAPI
/km action Player5 tell: Your level is %player_level%, score is {data:score}
```

**Tab completion:**
- After `/km action `, Tab shows all online players
- After a player name, Tab shows common action prefixes

{% hint style="info" %}
This command supports all built-in variables (`{data:var}`, `{gdata:var}`, `{meta:var}`) and PlaceholderAPI variables (`%player_name%`, etc.).
{% endhint %}

---

## Custom Quick Commands

In addition to `/km open`, you can register custom quick commands in `config.yml` that map a short command directly to opening a specific menu:

```yaml
custom-commands:
  shop: 'server_shop'   # Players run /shop to open server_shop
  menu: 'main_menu'     # Players run /menu to open main_menu
```

For detailed configuration, see [⌨️ Custom Commands](../home/commands.md).
