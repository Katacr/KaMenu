# 🤖 Actions

The `actions` node defines a list of actions to execute when a button is clicked. Supports multiple action types, delayed execution, and conditional branches.

---

## Configuration Structure

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&aConfirm'
    actions:
      - 'tell: &aOperation successful!'
      - 'sound: entity.experience_orb.pickup'
      - 'close'
```

Actions are executed **sequentially** in order (`wait` action can insert delays).

---

## Action Type Overview

| Action        | Description                                                    | Supports Target Selector |
|---------------|----------------------------------------------------------------|--------------------------|
| `tell`        | Send a chat message to the player                              | ✅                        | 
| `actionbar`   | Send an action bar message (bottom of screen)                  | ✅                        | 
| `title`       | Send a screen title and subtitle                               | ✅                        | 
| `toast`       | Display a Toast notification on screen                         | ✅                        | 
| `hovertext`   | Send a chat message with hover tooltip and click functionality | ✅                        | 
| `command`     | Make the player execute a command                              | ✅                        | 
| `chat`        | Make the player send a message in chat                         | ✅                        | 
| `console`     | Execute a command with console permissions                     | ✅                        | 
| `server`      | Transfer to a specified server (BungeeCord/Velocity)           | ✅                        | 
| `tppos`       | Teleport to specified coordinates                              | ✅                        | 
| `sound`       | Play a sound at the player's location                          | ✅                        | 
| `money`       | Operate player coins (requires Vault)                          | ✅                        | 
| `stock-item`  | Give/take stored items                                         | ✅                        | 
| `item`        | Give/take regular items                                        | ✅                        | 
| `open`        | Open another menu for the player                               | ✅                        | 
| `close`       | Close the current menu                                         | ✅                        | 
| `force-open`  | Force open menu (skips Events.Open)                            | ✅                        | 
| `force-close` | Force close menu (skips Events.Close)                          | ✅                        | 
| `reset`       | Reopen current menu (skips Events.Open)                        | ✅                        | 
| `url`         | Open a specified link (only works with single action)          | ❌                        | 
| `copy`        | Copy text to clipboard (only works with single action)         | ❌                        | 
| `data`        | Operate player data (supports set/add/take/delete)             | ✅                        | 
| `gdata`       | Operate global data (supports set/add/take/delete)             | ✅                        | 
| `list`        | Operate player list data (supports set/add/remove/take/clear/delete) | ✅                        | 
| `glist`       | Operate global list data (supports set/add/remove/take/clear/delete) | ✅                        | 
| `meta`        | Operate player metadata (supports set/add/take/delete)         | ✅                        | 
| `set-data`    | Short form for setting player data                             | ✅                        | 
| `set-gdata`   | Short form for setting global data                             | ✅                        | 
| `set-meta`    | Short form for setting player metadata                         | ✅                        | 
| `js`          | Execute JavaScript code (supports predefined functions)        | ❌                        | 
| `actions`     | Execute an action list defined under Events.Click              | ❌                        | 
| `run-task`    | Start a periodic task defined under Events.Tasks               | ❌                        | 
| `stop-task`   | Stop a specified periodic task                                 | ❌                        | 
| `stop-current-task` | Stop the current periodic task and interrupt the current round | ❌                        | 
| `wait`        | Insert delayed execution                                       | ❌                        | 
| `return`      | Interrupt action execution list                                | ❌                        | 

---

## Target Selector

All actions support a target selector to specify which player(s) the action affects.

**Syntax:** `{player: selector}`

**Usage Examples:**

```yaml
# 1. No target specified, sent to current player (default)
- 'tell: Hello!'

# 2. Send to all online players
- 'tell: Server announcement: Server will restart in 5 minutes! {player: *}'
- 'tell: Hello everyone! {player: *}'

# 3. Use conditional selection (PAPI variables)
- 'tell: Welcome admin! {player: %player_is_op% == true}'
- 'tell: Players at level 10+: Rewards have been sent! {player: %player_level% >= 10}'

# 4. Complex conditions
- 'tell: VIP player exclusive message {player: hasPerm.user.vip}'
- 'tell: Players with balance over 10000 {player: %vault_eco_balance% >= 10000}'
```

**Selector Types:**

| Selector | Description | Example |
|----------|-------------|---------|
| `{player: *}` | All online players | `{player: *}` |
| `{player: all}` | All online players (same as *) | `{player: all}` |
| `{player: condition}` | Online players meeting the condition | `{player: %player_level% >= 10}` |



**Condition Expressions:**

Target selectors support all condition expressions supported by `Condition`:

- **PAPI variables**: `%player_level%`, `%vault_eco_balance%`
- **Comparison operators**: `>`, `>=`, `<`, `<=`, `==`, `!=`, `contains`, `!contains`
- **Logical operators**: `&&` (AND), `||` (OR)
- **Parentheses grouping**: For complex logical expressions

**Condition Examples:**

```yaml
# Single condition
{player: %player_level% >= 10}

# Multiple conditions (AND)
{player: %player_level% >= 10 && hasPerm.user.vip}

# Multiple conditions (OR)
{player: %player_is_op% || hasPerm.user.vip}

# Complex conditions
{player: (%player_level% >= 10 && %vault_eco_balance% >= 1000) || %player_is_op% == true}
```

**Performance Optimization:**

- ✅ Target player list is dynamically calculated based on conditions
- ✅ Condition expressions are cached for performance
- ✅ A warning is logged when no players match

**Notes:**

- ⚠️ `*` and `all` match all online players, use with caution
- ⚠️ Variables in condition expressions are resolved individually for each target player
- ⚠️ `server`, `actions`, `js`, `wait`, `return`, and task-control actions don't support target selectors and will ignore the target parameter
- ✅ `open`, `close`, `force-open`, `force-close`, and `reset` support target selectors and can refresh or close menus for selected players
- ✅ Selector conditions can use variables such as `{data:*}`, `{gdata:*}`, and `{meta:*}`, for example: `open: xiangqi{player: {meta:xiangqi-viewer} == true}`

---

## Action Type Details

### tell - Chat Message

Send a chat message to the player. **Fully supports all Adventure MiniMessage features**, including colors, gradients, click events, hover events, etc.

**Format:** `tell: <message>`

**Example (Legacy color codes):**

```yaml
- 'tell: &aOperation successful!'
- 'tell: &cOperation failed, please contact admin'
- 'tell: &7Current balance: &f%player_balance%'
- 'tell: &eYou entered: $(input_key)'
```

**Example (MiniMessage format):**

```yaml
# Basic colors and formatting
- 'tell: <red>Red text</red>'
- 'tell: <bold>Bold</bold> <italic>Italic</italic> <underline>Underline</underline>'
- 'tell: <gradient:red:blue>Red-blue gradient text</gradient>'

# Click events
- 'tell: Click here to run command: <click:run_command:/say You clicked!><gold>Click me!</gold></click>'
- 'tell: <click:copy_to_clipboard:Hello KaMenu><gold>Copy this text</gold></click>'
- 'tell: <click:open_url:https://minecraft.wiki><gold>Open Minecraft Wiki</gold></click>'
- 'tell: <click:suggest_command:/gamemode creative><gold>Switch to creative</gold></click>'

# Hover events
- 'tell: <hover:show_text:"<red>This is hover text<reset>\n<blue>Supports multiline"><gold>Hover over me!</gold></hover>'
- 'tell: <hover:show_item:diamond_sword>Show diamond sword</hover>'
- 'tell: <hover:show_item:diamond><gold>Show diamond</gold></hover>'

# Combined usage (click+hover)
- 'tell: <click:run_command:/say Combined event><hover:show_text:"<green>Click to run command\n<gray>Hover for tooltip"><gold>Click and hover</gold></hover></click>'

# Item icons and player heads (1.21.9+)
- 'tell: Check this <sprite:block/stone> stone'
- 'tell: This is <sprite:items:item/porkchop> porkchop'
- 'tell: This is <head:Notch> Notch''s head'
- 'tell: <head:entity/player/wide/steve> Steve''s head'

# Other MiniMessage tags
- 'tell: <blue>Key: </blue><key:key.keyboard.b><red>B key</red>'
- 'tell: <blue>Newline test: <newline><red>This is a new line</red></blue>'
- 'tell: <blue>NBT data: </blue><nbt:display.Name></blue>'
```

**Common MiniMessage Tags:**

| Tag | Description | Example |
|-----|-------------|---------|
| `<color>` | Color tag | `<red>Red</red>` |
| `<gradient>` | Gradient color | `<gradient:red:blue>Gradient</gradient>` |
| `<bold>` | Bold | `<bold>Bold</bold>` |
| `<italic>` | Italic | `<italic>Italic</italic>` |
| `<underline>` | Underline | `<underline>Underline</underline>` |
| `<click:action:value>` | Click event | `<click:run_command:/say hi>Click</click>` |
| `<hover:action:value>` | Hover event | `<hover:show_text:Tip>Hover</hover>` |
| `<newline>` | Newline | `Line 1<newline>Line 2` |
| `<key:keyname>` | Key display | `<key:key.keyboard.b>B key</key>` |

**Note:**
- Supports both Legacy color codes (`&a`, `&c`, etc.) and MiniMessage tags
- When MiniMessage tags are detected, Legacy color codes are automatically converted to corresponding MiniMessage tags
  - Example: `&a` → `<green>`, `&c` → `<red>`, `&l` → `<bold>`
  - Example: `'<gold>&aThis is &cRed &lBold</l>'` is automatically converted to `'<gold><green>This is <red>Red <bold>Bold</bold>'`
- Supports PAPI variables (`%var%`), internal data variables (`{data:key}`), and input component references (`$(key)`)
- Recommended to use pure MiniMessage format for best results and full functionality

---

### actionbar - Action Bar Message

Send an action bar message to the player (displayed above the crosshair at the bottom of the screen). **Fully supports all Adventure MiniMessage features**.

**Format:** `actionbar: <message>`

**Example:**

```yaml
# Legacy color codes
- 'actionbar: &aOperation successful!'
- 'actionbar: &7Balance: &f%player_balance%'

# MiniMessage format
- 'actionbar: <green>Operation successful!</green>'
- 'actionbar: <gradient:gold:red>Balance: 1000</gradient>'
- 'actionbar: <hover:show_text:View details><gold>Click for details</gold></hover>'
```

**Note:** Message persists for about 3 seconds before disappearing. Supports the same MiniMessage features as `tell`.

---

### title - Title Message

Send a screen title and subtitle to the player.

**Format:** `title: title=main_title;subtitle=subtitle;in=fade_in;keep=stay;out=fade_out`

**Parameters:**

| Parameter | Description | Unit | Default |
|-----------|-------------|------|---------|
| `title` | Main title text | — | empty |
| `subtitle` | Subtitle text | — | empty |
| `in` | Fade in time | tick | `0` |
| `keep` | Stay time | tick | `60` |
| `out` | Fade out time | tick | `20` |

**Example:**

```yaml
- 'title: title=&aOperation successful;subtitle=&7Completed'
- 'title: title=&6Welcome!;subtitle=&fHello, %player_name%;in=10;keep=80;out=20'
```

**Note:** Parameters are separated by semicolons `;`. Supports color codes and variables.

---

### hovertext - Clickable Chat Text

Send a chat message with hover tooltip and click functionality.

**Format:** `hovertext: Normal text <text=display_text;hover=hover_text;command=command;url=url;actions=action_list;newline=false> Continue text`

**Parameters:**

| Parameter | Description | Required |
|-----------|-------------|----------|
| `text` | Clickable display text | ✅ |
| `hover` | Tooltip text shown on hover | ❌ |
| `command` | Command executed when clicked | ❌ |
| `url` | URL opened when clicked | ❌ |
| `actions` | Action list executed when clicked (key under Events.Click) | ❌ |
| `newline` | Whether to add newline after text (`true`/`false`) | ❌ |

**Example:**

```yaml
- 'hovertext: &7Click here <text=&a[Claim Reward];hover=&eClick to claim daily reward;command=/daily> or come back later.'
- 'hovertext: Visit <text=&b[Website];hover=&7Open website in browser;url=https://example.com> for more info.'
- 'hovertext: <text=&a[Greeting];actions=greet;hover=Click to send greeting> the player'
```

**Using the actions parameter:**

```yaml
Events:
  Click:
    greet:
      - 'tell: &aHello! Welcome to the server.'
      - 'sound: ENTITY_PLAYER_LEVELUP'

Body:
  text:
    type: 'message'
    text: '<text="Click to greet";actions=greet;hover=Click to execute greet action>'
```

**Click Event Priority:**

When multiple click parameters exist simultaneously, priority order (highest to lowest):
1. `actions` - Execute action list
2. `url` - Open URL
3. `command` - Execute command

**Note:**
- Parameter values should be wrapped in backticks `` ` ``, single quotes `'`, or double quotes `"`
- Clickable areas are wrapped in `< >`
- The `actions` parameter is only valid in Body.message text components
- When using the `actions` parameter, the text's click event registers a ClickCallback, valid for 5 minutes

---

### command - Player Command

Make the clicking player execute a command.

**Format:** `command: <command>`

**Example:**

```yaml
- 'command: spawn'
- 'command: msg %player_name% Hello'
- 'command: warp hub'
```

**Note:** Commands don't need a leading `/`; player needs permission to execute the command.

---

### chat - Chat Message

Make the player send a message in the chat box.

**Format:** `chat: <message>`

**Example:**

```yaml
- 'chat: /spawn'           # Player sends /spawn command
- 'chat: Hello everyone!'  # Player sends chat message
- 'chat: /msg Admin Help me' # Player sends private message to admin
- 'chat: $(input_message)'  # Send player input content
```

**Difference from command:**

| Action | Execution Method | Permission Requirement | Use Case |
|--------|-----------------|----------------------|----------|
| `command` | Directly execute command | Requires player permission | Execute plugin commands (like `/spawn`) |
| `chat` | Simulate player typing in chat | No special permission needed | Send chat messages, execute commands requiring player input |

**Use Cases:**
- Display player messages in chat (e.g., broadcasts, announcements)
- Execute commands that require player input in chat
- Interact with other players or plugins

**Note:** Messages are broadcast to online players. Supports color codes, PAPI variables, and input component references.

---

### console - Console Command

Execute a command with console (OP) permissions.

**Format:** `console: <command>`

**Example:**

```yaml
- 'console: give %player_name% diamond 64'
- 'console: eco give %player_name% 1000'
- 'console: lp user %player_name% group add vip'
```

**Note:** No player permission required; commands don't need a leading `/`; supports PAPI variables.

---

### server - Transfer to Specified Server

Transfer the player to a specified server (supports BungeeCord or Velocity proxy plugins).

**Format:** `server: <server_name>`

**Example:**

```yaml
- 'server: lobby'
- 'server: survival'
- 'server: creative'
```

**How It Works:**

This action automatically selects the transfer method based on the `bungeecord` configuration in `config.yml`:

| Configuration | Transfer Method | Advantages |
|---------------|----------------|------------|
| `bungeecord: true` | BungeeCord plugin message system | ✅ No player permission needed<br>✅ More reliable<br>✅ Better performance |
| `bungeecord: false` | Execute `/server` command | ⚠️ Player needs `/server` command permission |

**Use Cases:**
- BungeeCord/Velocity network servers
- Transfer between multiple servers
- Lobby/main menu for selecting different game modes

**Note:**
- BungeeCord mode requires proxy plugin
- Server name must be defined in proxy plugin configuration
- Player will immediately disconnect from current server and connect to target server
- Supports variables and conditions
- Recommended to enable `bungeecord: true` in BungeeCord networks

**Using with Variables:**

```yaml
# Transfer to different server based on player selection
- 'server: $(server_name)'

# Use server name from data storage
- 'server: {data:favorite_server}'
```

**Advanced Example - Conditional Transfer:**

```yaml
Events:
  Click:
    # Player selects server
    select_server:
      - condition: '{data:last_server} == survival'
        allow:
          - 'server: survival'
          - 'tell: &aConnecting to survival server...'
        deny:
          - 'server: lobby'
          - 'tell: &aConnecting to lobby...'
```

---

### tppos - Teleport to Coordinates

Teleport the player to specified coordinates.

**Format:** `tppos: <world>,<x>,<y>,<z>[,<yaw>,<pitch>]`

**Parameters:**

| Parameter | Description | Required |
|-----------|-------------|----------|
| world | Target world name | ✅ |
| x | X coordinate | ✅ |
| y | Y coordinate | ✅ |
| z | Z coordinate | ✅ |
| yaw | Horizontal rotation | ❌ (defaults to player's current yaw) |
| pitch | Vertical rotation | ❌ (defaults to player's current pitch) |

**Example:**

```yaml
# Coordinates only, keep current facing direction
- 'tppos: world,100,64,200'

# Full coordinates + rotation
- 'tppos: world,100,64,200,90,0'

# With variables
- 'tppos: {data:target_world},{data:target_x},{data:target_y},{data:target_z}'
```

**Note:** The world name must exist; teleport will not execute if it doesn't.

---

### sound - Play Sound

Play a sound at the player's location, supporting volume, pitch, and sound category.

**Format:** `sound: <sound_name>;volume=volume;pitch=pitch;category=category`

**Parameters:**

| Parameter | Description | Default |
|-----------|-------------|---------|
| Sound name | Minecraft sound ID (use `_` or `.`) | — |
| `volume` | Volume (float) | `1.0` |
| `pitch` | Pitch (float) | `1.0` |
| `category` | Sound category | `master` |

**Sound Category Options:**

| Value | Description |
|-------|-------------|
| `master` | Master volume |
| `music` | Music |
| `record` | Jukebox |
| `weather` | Weather |
| `block` | Blocks |
| `hostile` | Hostile mobs |
| `neutral` | Neutral mobs |
| `player` | Players |
| `ambient` | Ambient |
| `voice` | Voice |
| `ui` | UI sounds |

**Example:**

```yaml
- 'sound: entity.experience_orb.pickup'
- 'sound: entity.player.levelup;volume=1.5;pitch=1.2'
- 'sound: block.note_block.pling;volume=1.0;pitch=2.0;category=ui'
```

---

### open - Open Menu

Open another menu for the player, current menu closes automatically.

**Format:** `open: <menu_id>`

**Example:**

```yaml
- 'open: main_menu'
- 'open: shop/weapons'
- 'open: admin/tools'
```

**Note:** Menu ID rules are the same as `/km open` command; subfolders are separated by `/`; no `.yml` extension.

---

### close - Close Menu

Close the currently open menu (**executes Events.Close first**).

**Format:** `close`

**Example:**

```yaml
- 'tell: &cGoodbye!'
- 'close'
```

---

### force-open - Force Open Menu

Force open a specified menu for the player, **skipping the target menu's Events.Open action list**. Unlike `open`, this does not trigger the target menu's Open event.

**Format:** `force-open: <menu_id>`

**Example:**

```yaml
# Normal open (triggers target's Open event)
- 'open: shop'

# Force open (skips Open event)
- 'force-open: shop'
```

**Use Cases:**
- Opening a menu without triggering initialization logic in the Open event
- Nesting menu opens in Events.Click without re-executing the Open event

---

### force-close - Force Close Menu

Force close the current menu **without executing the Events.Close action list**.

**Format:** `force-close`

**Example:**

```yaml
- 'force-close'
```

**Use Cases:**
- Immediately closing the menu without triggering cleanup/recording logic in the Close event

---

### reset - Reopen Current Menu

Reopen the current menu (equivalent to refresh), **without executing the Events.Open action list**.

**Format:** `reset`

**Example:**

```yaml
# Refresh current menu
- 'reset'
```

**Use Cases:**
- Refreshing current menu content (e.g., updated variable displays)
- Resetting button states

---

### url - Open Link

Open a specified URL (only works when the button has only this one action).

**Format:** `url: <url>`

**Example:**

```yaml
actions:
  - 'url: https://github.com/Katacr/KaMenu'
```

{% hint style="info" %}
`url` and `copy` actions are static actions, **they only work when the button's actions list contains only this one action**. If you need to open a URL while executing other actions, use the `hovertext` action instead.
{% endhint %}

---

### copy - Copy to Clipboard

Copy specified text to the player's clipboard (only works when the button has only this one action).

**Format:** `copy: <text>`

**Example:**

```yaml
actions:
  - 'copy: play.example.com'
```

---

### set-data - Set Player Data (Short Form)

Save a key-value pair to the current player's persistent data.

**Format:** `set-data: <key> <value>`

**Example:**

```yaml
- 'set-data: language zh_CN'
- 'set-data: nickname $(player_nickname)'
- 'set-data: score %player_level%'
```

**Reading:** Use `{data:key}` or PAPI variable `%kamenu_data_key%` anywhere in the menu.

{% hint style="info" %}
This is a short form for setting one value. Use the parameter form `data: type=...;key=...;var=...` when you need `add` / `take` / `delete`.
{% endhint %}

---

### set-gdata - Set Global Data (Short Form)

Save a key-value pair to global data (shared by all players).

**Format:** `set-gdata: <key> <value>`

**Example:**

```yaml
- 'set-gdata: server_status open'
- 'set-gdata: event_winner %player_name%'
```

**Reading:** Use `{gdata:key}` or PAPI variable `%kamenu_gdata_key%` anywhere in the menu.

{% hint style="info" %}
This is a short form for setting one global value. Use the parameter form `gdata: type=...;key=...;var=...` when you need `add` / `take` / `delete`.
{% endhint %}

---

### set-meta - Set Player Metadata (Short Form)

Save a key-value pair to the player's metadata (memory cache, not persistent).

**Format:** `set-meta: <key> <value>`

**Example:**

```yaml
- 'set-meta: time 19:02'
- 'set-meta: nickname $(player_nickname)'
- 'set-meta: last_menu shop/weapons'
```

**Reading:** Use `{meta:key}` or PAPI variable `%kamenu_meta_key%` anywhere in the menu.

{% hint style="info" %}
This is a short form for setting one temporary value. Use the parameter form `meta: type=...;key=...;var=...` when you need `add` / `take` / `delete`.
{% endhint %}

**Note:**
- Metadata is only stored in memory, not persisted to database
- Automatically cleaned when player disconnects
- All metadata is cleared on plugin reload or server shutdown
- Suitable for scenarios requiring short-term temporary data storage

---

### data - Player Data Operation

Operate the player's persistent data, supporting set, add, subtract, and delete values.

**Format:** `data: type=operation_type;key=key;var=value`

**Parameters:**

| Parameter | Description | Required |
|-----------|-------------|----------|
| `type` | Operation type | ✅ |
| `key` | Data key | ✅ |
| `var` | Value (required only for type=set/add/take) | ❌ |

**type Options:**
- `set`: Set value
- `add`: Add to value (only works when value is numeric)
- `take`: Subtract from value (only works when value is numeric)
- `delete`: Delete the key-value pair

**Example:**

```yaml
# Set text value
- 'data: type=set;key=test;var=`Hello, Minecraft`'

# Set numeric value
- 'data: type=set;key=num;var=`100`'

# Add to numeric value
- 'data: type=add;key=num;var=`10`'

# Subtract from numeric value
- 'data: type=take;key=num;var=`10`'

# Delete data
- 'data: type=delete;key=num'
```

**Reading:** Use `{data:key}` or PAPI variable `%kamenu_data_key%` anywhere in the menu.

**Note:**
- In `add` and `take` operations, if the current value or specified value is not a number, the operation fails and a warning is output in the console
- In `delete` operation, if the key doesn't exist, the operation silently fails (no error)
- Short form `set-data: <key> <value>` can be used for quick value setting

---

### gdata - Global Data Operation

Operate global data (shared by all players), supporting set, add, subtract, and delete values.

**Format:** `gdata: type=operation_type;key=key;var=value`

**Parameters:**

| Parameter | Description | Required |
|-----------|-------------|----------|
| `type` | Operation type | ✅ |
| `key` | Data key | ✅ |
| `var` | Value (required only for type=set/add/take) | ❌ |

**type Options:**
- `set`: Set value
- `add`: Add to value (only works when value is numeric)
- `take`: Subtract from value (only works when value is numeric)
- `delete`: Delete the key-value pair

**Example:**

```yaml
# Set global data
- 'gdata: type=set;key=total;var=`1000`'

# Add to value
- 'gdata: type=add;key=total;var=`50`'

# Subtract from value
- 'gdata: type=take;key=total;var=`20`'

# Delete data
- 'gdata: type=delete;key=total'
```

**Reading:** Use `{gdata:key}` or PAPI variable `%kamenu_gdata_key%` anywhere in the menu.

**Note:**
- Global data is shared among all players
- In `add` and `take` operations, if the current value or specified value is not a number, the operation fails and a warning is output in the console
- In `delete` operation, if the key doesn't exist, the operation silently fails (no error)
- Short form `set-gdata: <key> <value>` can be used for quick value setting

---

### list / glist - List Data Operation

Operate persistent list data. `list` belongs to the current player, while `glist` is shared globally. A list is stored under one database key as a JSON array, making it suitable for friend lists, warp lists, favorites, and direct `Bottom.multi.buttons.type: repeat` sources.

**Format:**

```yaml
- 'list: type=operation_type;key=key;var=value'
- 'glist: type=operation_type;key=key;var=value'
```

**Parameters:**

| Parameter | Description | Required |
|-----------|-------------|----------|
| `type` | Operation type | ✅ |
| `key` | List key | ✅ |
| `var` / `value` | List item or list text | Required for `set/add/remove/take` |
| `split` / `separator` | Optional separator used to split `var` into multiple items | ❌ |
| `unique` | Whether `add` should skip existing items, supports `true/false`, default `true` | ❌ |

**type Options:**

- `set` / `create`: Create or overwrite the whole list
- `add` / `append`: Append items to the list
- `remove` / `take`: Remove matching items
- `clear`: Clear the list while keeping the key
- `delete`: Delete the key

**Example:**

```yaml
# Set a player friend list
- 'list: type=set;key=friends;var=`Steve,Alex`;split=,'

# Add a friend. Existing items are skipped by default
- 'list: type=add;key=friends;var=`Notch`'

# Allow duplicates, suitable for logs and history records
- 'list: type=add;key=history;var=`Notch`;unique=false'

# Remove a friend
- 'list: type=remove;key=friends;var=`Alex`'

# Clear the player list
- 'list: type=clear;key=friends'

# Set a global server list
- 'glist: type=set;key=servers;var=`survival,skyblock,resource`;split=,'
```

**Reading:**

- `{list:key}`: Read the current player's list as a JSON array string
- `{glist:key}`: Read the global list as a JSON array string
- `%kamenu_list_key%` / `%kamenu_glist_key%`: Read list JSON through PlaceholderAPI
- `%kamenu_list_size_key%` / `%kamenu_glist_size_key%`: Read list item counts through PlaceholderAPI

**Using with dynamic buttons:**

```yaml
Bottom:
  type: multi
  buttons:
    friends:
      type: repeat
      source: "{list:friends}"
      item:
        text: "&a{item.value}"
        actions:
          - "tell: You clicked {item.value}"
```

**Note:**

- `remove/take` removes all exact matches
- `add` skips existing items by default; set `unique=false` when duplicates are required
- `{list:*}` / `{glist:*}` returns a JSON array, so repeat sources do not need `split`
- Lists are still SQL-backed persistent data, so avoid writing them on every render in high-frequency refresh flows

---

### meta - Player Metadata Operation

Operate the player's metadata (memory cache), supporting set, add, subtract, and delete values.

**Format:** `meta: type=operation_type;key=key;var=value`

**Parameters:**

| Parameter | Description | Required |
|-----------|-------------|----------|
| `type` | Operation type | ✅ |
| `key` | Data key | ✅ |
| `var` | Value (required only for type=set/add/take) | ❌ |

**type Options:**
- `set`: Set value
- `add`: Add to value (only works when value is numeric)
- `take`: Subtract from value (only works when value is numeric)
- `delete`: Delete the key-value pair

**Example:**

```yaml
# Set metadata
- 'meta: type=set;key=level;var=`10`'

# Add to value
- 'meta: type=add;key=level;var=`1`'

# Subtract from value
- 'meta: type=take;key=level;var=`1`'

# Delete data
- 'meta: type=delete;key=level'
```

**Reading:** Use `{meta:key}` or PAPI variable `%kamenu_meta_key%` anywhere in the menu.

**Note:**
- Metadata is only stored in memory, not persisted to database
- Automatically cleaned when player disconnects
- All metadata is cleared on plugin reload or server shutdown
- In `add` and `take` operations, if the current value or specified value is not a number, the operation fails and a warning is output in the console
- In `delete` operation, if the key doesn't exist, the operation silently fails (no error)
- Short form `set-meta: <key> <value>` can be used for quick value setting

---

### toast - Toast Notification

Display a Toast notification in the upper right corner of the screen.

**Format:** `toast: type=type;icon=item_id;msg=title`

**Parameters:**

| Parameter | Description | Default |
|-----------|-------------|---------|
| `type` | Notification type | `task` |
| `icon` | Display item ID | `paper` |
| `msg` | Content text | empty |

**type Options:**
- `task`: Title text: `Quest Completed!` (default)
- `goal`: Title text: `Goal Reached!`
- `challenge`: Title text: `Challenge Completed!` (plays sound)

**Example:**

```yaml
- 'toast: msg=&fYou obtained a diamond sword;icon=diamond_sword'
- 'toast: type=challenge;msg=&fCongratulations on completing the challenge!;icon=diamond'
- 'toast: type=goal;msg=&fGoal achieved;icon=gold_ingot'
```

**Note:** Toast notifications automatically disappear after about 3 seconds.

---

### money - Coin Operation

Operate the player's coins (requires Vault economy plugin).

**Format:** `money: type=operation_type;num=amount`

**Parameters:**

| Parameter | Description | Options |
|-----------|-------------|---------|
| `type` | Operation type | See options below |
| `num` | Amount | Number (supports decimals) |

**type Options:**
- `add`: Give the player the specified amount
- `take`: Take the specified amount from the player
- `reset`: Set the player's balance to the specified amount

**Example:**

```yaml
# Give player 100 coins
- 'money: type=add;num=100'

# Take 50 coins from player
- 'money: type=take;num=50'

# Set player balance to 1000 coins
- 'money: type=reset;num=1000'

# Use with conditions
- condition: "%player_balance% >= 500"
  allow:
    - 'money: type=take;num=500'
    - 'tell: &aPurchase successful!'
  deny:
    - 'tell: &cInsufficient balance! 500 coins required'
```

**Note:**
- Requires Vault economy plugin to be installed
- **This action does not send any message to the player**, players need to judge and prompt themselves (e.g., using `tell` or conditions)
- `take` operation checks balance; if insufficient, no deduction occurs, only a warning is printed to console
- Amount supports decimals, e.g., 1.5, 0.99
- Amount can use variables, e.g., `%player_level%` or `{data:price}`

---

### stock-item - Item Give/Take (Storage)

Give the player or take from the player's inventory an item from the database storage.

**Format:** `stock-item: type=operation_type;name=item_name;amount=quantity`

**Parameters:**

| Parameter | Description | Required |
|-----------|-------------|----------|
| `type` | Operation type | ✅ |
| `name` | Item name (saved item) | ✅ |
| `amount` | Quantity | ❌ (default: 1) |

**type Options:**
- `give`: Give item to player
- `take`: Take item from player's inventory

**Example:**

```yaml
# Give player 16 Mystic Fruits
- 'stock-item: type=give;name=MysticFruit;amount=16'

# Take 16 Mystic Fruits from player inventory
- 'stock-item: type=take;name=MysticFruit;amount=16'

# Use with conditions
- condition: "hasStockItem.MysticFruit;16"
  allow:
    - 'stock-item: type=take;name=MysticFruit;amount=16'
    - 'tell: &aPurchase successful!'
  deny:
    - 'tell: &cInsufficient items! 16 Mystic Fruits required'
```

**Note:**
- Item must be saved using `/km item save` command before use
- If player's inventory is full during `give`, excess items will drop at player location, not lost
- `take` operation traverses all player inventory slots (main inventory, armor slots, offhand, main hand)
- Item comparison uses `ItemStack.isSimilar()` method, ignoring quantity differences
- Supports variable replacement, e.g., `name=$(item_name)` or `amount={data:price}`

---

### item - Regular Item Give/Take

Give the player or take from the player's inventory items of a specified material.

**Format:**
- `item: type=give;mats=material;amount=quantity`
- `item: type=take;mats=material;amount=quantity;lore=description;model=model`

**Parameters:**

| Parameter | Description | Required |
|-----------|-------------|----------|
| `type` | Operation type (give/take) | ✅ |
| `mats` | Item material (Material ID) | ✅ |
| `amount` | Quantity | ❌ (default: 1) |
| `lore` | Description (only for take operation, optional) | ❌ |
| `model` | Item model (only for take operation, optional) | ❌ |

**type Options:**
- `give`: Give item to player (ignores lore and model parameters)
- `take`: Take item from player inventory (supports lore and model matching)

**Example:**

```yaml
# Give player 10 diamonds
- 'item: type=give;mats=DIAMOND;amount=10'

# Take 10 diamonds from player inventory
- 'item: type=take;mats=DIAMOND;amount=10'

# Take items with specified lore
- 'item: type=take;mats=DIAMOND;amount=10;lore=Forging Material'

# Take items with specified model (e.g., Oraxen items)
- 'item: type=take;mats=DIAMOND;amount=10;model=oraxen:mana_crystal'

# Specify both lore and model
- 'item: type=take;mats=DIAMOND;amount=10;lore=Forging Material;model=oraxen:mana_crystal'

# Use with conditions
- condition: "hasItem.[mats=DIAMOND;amount=10]"
  allow:
    - 'item: type=take;mats=DIAMOND;amount=10'
    - 'tell: &aDeduction successful!'
  deny:
    - 'tell: &cInsufficient items! 10 diamonds required'
```

**Note:**
- `mats` parameter uses Minecraft native material ID (e.g., `DIAMOND`, `IRON_INGOT`, etc.)
- During `give` operation, `lore` and `model` parameters are ignored because they're only used for matching
- If player's inventory is full during `give`, excess items will drop at player location, not lost
- During `take` operation:
  - If `lore` is specified, only items with lore containing the specified string are taken (case-insensitive)
  - If `model` is specified, only items matching the specified item model are taken
  - If both `lore` and `model` are specified, items must satisfy both conditions to be taken
  - `model` format is `namespace:key` (e.g., `oraxen:mana_crystal`, `minecraft:diamond`)
- `take` operation traverses all player inventory slots (main inventory, armor slots, offhand, main hand)
- Supports variable replacement, e.g., `mats=$(material)`, `amount={data:price}`, `lore={data:item_desc}`

---

### wait - Delayed Execution

Insert a delay in the action list; subsequent actions will execute after the specified wait time.

**Format:** `wait: <ticks>`

**Unit:** Minecraft tick (1 tick = 0.05 seconds, 20 ticks = 1 second)

**Example:**

```yaml
- 'tell: &aStarting countdown...'
- 'wait: 20'            # Wait 1 second
- 'tell: &e3...'
- 'wait: 20'
- 'tell: &e2...'
- 'wait: 20'
- 'tell: &e1...'
- 'wait: 20'
- 'title: title=&cGo!;in=5;keep=30;out=10'
```

**Note:** `wait` only affects actions **after** it; does not block other tasks being executed.

---

### return - Interrupt Execution

Insert an interrupt in the action list; subsequent actions will not be executed.

**Format:** `return`

**Example:**

```yaml
- 'tell: You clicked this button'
- 'return'            # Execute interrupt
- 'tell: You will never see this message.'  # This action won't be executed
```
```yaml
- 'tell: &aStarting countdown...'
- 'wait: 20'            # Wait 1 second
- 'tell: &e3...'
- 'wait: 20'
- 'tell: &e2...'
- 'wait: 20'
- 'tell: &e1...'
- 'wait: 20'
- condition: '%player_is_online% == false'
  allow:
    - 'tell: &cPlayer offline detected, interrupting operation!'
    - 'return'   # Execute interrupt
- 'tell: &aYou completed the operation.' # This action won't be executed if condition is met
```

**Note:** `wait` only affects actions **after** it; does not block other tasks being executed.

---

### js - Execute JavaScript Code

Execute JavaScript code, supporting direct code execution or calling predefined functions.

**Format:** `js: <JavaScript code>`

**Usage:**

1. **Directly execute JavaScript code**

```yaml
actions:
  - 'js: player.sendMessage("Hello from JavaScript!");'
  - 'js: var random = Math.floor(Math.random() * 100);'
  - 'js: player.sendMessage("Random number: " + random);'
```

2. **Call predefined function (no parameters)**

```yaml
JavaScript:
  show_health: |
    var health = player.getHealth();
    var maxHealth = player.getMaxHealth();
    player.sendMessage("§eHealth: §f" + health + "/" + maxHealth);

Bottom:
  type: 'notice'
  confirm:
    text: '&aView Health'
    actions:
      - 'js: [show_health]'
```

3. **Call predefined function (with parameters)**

```yaml
JavaScript:
  process_data: |
    var playerName = args[0];
    var playerLevel = args[1];
    var money = args[2];

    player.sendMessage("§aPlayer: §f" + playerName);
    player.sendMessage("§aLevel: §f" + playerLevel);
    player.sendMessage("§aCoins: §f" + money);

Bottom:
  type: 'notice'
  confirm:
    text: '&eProcess Data'
    actions:
      - 'js: [process_data] %player_name% $(level) {data:money}'
```

**Supported Variables:**

- `player` - Current player object
- `uuid` - Player UUID string
- `name` - Player name
- `location` - Player location
- `inventory` - Player inventory
- `world` - Player's world
- `server` - Server instance
- `args` - Predefined function parameter array (only available when calling predefined functions)

**Supported Parameter Types (for predefined functions):**

- String: passed directly
- PAPI variable: `%player_name%`
- Player data: `{data:money}`
- Global data: `{gdata:config}`
- Input field variable: `$(input1)`
- Number: `50`, `3.14`

{% hint style="info" %}
JavaScript functionality is very powerful, supporting access to Bukkit API, math calculations, condition checks, etc. For more details on JavaScript functionality, see [🔧 JavaScript Features](javascript.md).
{% endhint %}

**Note:**
- JavaScript code is executed on the server side
- Predefined functions must be defined in the menu's `JavaScript` node
- Parameters are separated by spaces; parameters cannot contain spaces
- Nashorn engine is based on ECMAScript 5.1 standard, does not support ES6+ syntax

---

### actions - Execute Action List

Execute an action list defined under `Events.Click`. This allows you to reuse defined action lists in actions, avoiding duplicate code.

**Format:**

```yaml
- 'actions: <action_list_name>'
- 'actions: <action_list_name>,<arg0>,<arg1>'
```

**Parameters:**

| Parameter | Description | Example |
|-----------|-------------|---------|
| Action list name | Action list key under `Events.Click` | `greet`, `vip_check`, `daily_reward` |
| Arguments | Temporary arguments passed into the action list. Read them with `{arg:0}`, `{arg:1}` inside the action list | `player`, `survival server` |

**Example:**

```yaml
Events:
  Click:
    greet:
      - 'tell: &aHello, {arg:0}! Welcome to &e{arg:1}&a.'
      - 'sound: ENTITY_PLAYER_LEVELUP'

    vip_check:
      - condition: 'hasPerm.essentials.vip'
        allow:
          - 'tell: &aVIP exclusive welcome!'
          - 'sound: ENTITY_EXPERIENCE_ORB_PICKUP'
        deny:
          - 'tell: &cYou need VIP permission'

Bottom:
  type: 'multi'
  buttons:
    btn_greet:
      text: 'Greeting'
      actions:
        - 'actions: greet,player,survival server'  # Execute Events.Click.greet with arguments

    btn_vip:
      text: 'VIP Check'
      actions:
        - 'actions: vip_check'  # Execute Events.Click.vip_check
```

**Complex Action Chain Example:**

```yaml
Events:
  Click:
    daily_login:
      - 'tell: &6Daily sign-in successful!'
      - 'sound: ENTITY_PLAYER_LEVELUP'
      - 'set-data: coins +100'
      - 'tell: &eObtained 100 coins'
      - 'sound: ENTITY_EXPERIENCE_ORB_PICKUP'

Bottom:
  type: 'multi'
  buttons:
    daily:
      text: 'Daily Sign-in'
      actions:
        - 'actions: daily_login'
```

**Features:**

1. **Async execution**: `actions` action executes in an async thread, won't block main thread
2. **Condition support**: The referenced action list can use `condition` for conditional branching
3. **Variable support**: All KaMenu variables are supported in action lists (`{data:xxx}`, `{gdata:xxx}`, etc.)
4. **Code reuse**: Avoid redefining the same action sequence in multiple buttons

**Clickable text arguments:**

The `<text>` tag in `Body.message` and the `hovertext:` action can also call an action list with arguments:

```yaml
Body:
  hello_text:
    type: message
    text: '&7Greet player: <text="&a[click greet]";hover="&7Click to run action list";actions=hello,player,survival server>'

Events:
  Click:
    hello:
      - 'tell: &aHello, {arg:0}! Welcome to &e{arg:1}&a.'
```

In this example, `{arg:0}` is `player` and `{arg:1}` is `survival server`. Arguments are separated by commas. Use single quotes, double quotes, or backticks around an argument when it needs to contain a comma.

**Error Handling:**

If the referenced action list doesn't exist, the player receives an error message:
```
&cError: Action list 'xxx' not found
```

**Comparison with Other Methods:**

| Method | Usage Location | Trigger | Example |
|--------|--------------|---------|---------|
| `actions` action | Button actions, commands | Button click/command | `actions: greet` |
| `<text>` tag's `actions` parameter | Text component (Body.message) | Text click | `<text='Click';actions=greet,player,survival server>` |

**Use Cases:**

- **Button reuse action list**: Multiple buttons execute the same action sequence
- **Conditional branching**: Execute different actions based on player state
- **Command shortcuts**: Trigger predefined action lists via commands
- **Action chain reuse**: Avoid redefining complex action sequences

**Notes:**

1. Action list must be defined under `Events.Click`
2. Avoid circular references (e.g., action list A references itself)
3. `actions` action can also be used within conditions

---

### run-task / stop-task - Control Periodic Tasks

Controls periodic tasks defined under `Events.Tasks`.

**Format:**

```yaml
- 'run-task: <taskId>'
- 'run-task: <taskId> <count>'
- 'run-task: *'
- 'run-task: * <count>'
- 'stop-task: <taskId>'
- 'stop-task: *'
- 'stop-current-task'
```

**Description:**

| Action | Description |
|--------|-------------|
| `run-task: test` | Starts `Events.Tasks.test` |
| `run-task: test 10` | Starts `test` and limits this run to 10 rounds |
| `run-task: *` | Starts all non-running tasks in the current menu |
| `run-task: * 10` | Starts all tasks in the current menu and limits this run to 10 rounds |
| `stop-task: test` | Stops the running `test` task and runs its `on_end` / `end_actions` |
| `stop-task: *` | Stops all running periodic tasks in the current menu |
| `stop-current-task` | Only valid inside a periodic task. Stops the current task loop and immediately interrupts the rest of the current round |

If the specified task is already running, `run-task` does not create a duplicate task.

**Example:**

```yaml
Events:
  Tasks:
    countdown:
      mode: manual
      interval: 20
      run_immediately: true
      actions:
        - 'tell: &eCountdown running'
      on_end:
        - 'tell: &aCountdown finished'

Bottom:
  type: multi
  buttons:
    start:
      text: '&a[ Start ]'
      actions:
        - 'run-task: countdown 10'
    stop:
      text: '&c[ Stop ]'
      actions:
        - 'stop-task: countdown'
    stop_all:
      text: '&4[ Stop All ]'
      actions:
        - 'stop-task: *'
```

---

## Complete Example

```yaml
Bottom:
  type: 'multi'
  columns: 2
  buttons:
    purchase:
      text: '&6[ Purchase ]'
      actions:
        - condition: "%player_balance% >= 500"
          allow:
            - 'console: eco take %player_name% 500'
            - 'console: give %player_name% diamond_sword 1'
            - 'tell: &aPurchase successful! Spent 500 coins'
            - 'sound: entity.player.levelup'
            - 'close'
          deny:
            - 'tell: &cInsufficient balance! 500 coins required, current: %player_balance%'
            - 'sound: block.note_block.bass'

    info:
      text: '&7[ View Instructions ]'
      actions:
        - 'tell: &6=== Sacred Sword Instructions ==='
        - 'tell: &f- Attack +20'
        - 'tell: &f- Can be used in advanced dungeons'
        - 'hovertext: &7Learn more <text=&b[Click to view official website];hover=&7Open in browser;url=https://example.com>'
```
