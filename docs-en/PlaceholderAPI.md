# 🔖 PlaceholderAPI

KaMenu includes a built-in PlaceholderAPI (PAPI) extension that exposes plugin data as placeholder variables for use in other plugins (such as scoreboards and chat plugins).

---

## Prerequisites

Requires the [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) plugin. KaMenu will automatically register the extension on startup — no extra steps needed.

---

## 📊 Variable Types Overview

| Variable Type | Prefix | Data Source | Persistent | Description |
|--------------|--------|-------------|------------|-------------|
| **Player Data** | `%kamenu_data_<key>%` | Database | ✅ Yes | Per-player persistent data |
| **Global Data** | `%kamenu_gdata_<key>%` | Database | ✅ Yes | Server-wide shared data |
| **Player List** | `%kamenu_list_<key>%` | Database | ✅ Yes | Per-player persistent string list, returned as a JSON array |
| **Global List** | `%kamenu_glist_<key>%` | Database | ✅ Yes | Server-wide shared string list, returned as a JSON array |
| **Player List Size** | `%kamenu_list_size_<key>%` | Database | ✅ Yes | Number of items in a player's list |
| **Global List Size** | `%kamenu_glist_size_<key>%` | Database | ✅ Yes | Number of items in a global list |
| **Online Player List** | `%kamenu_online_players%` | Online players | ❌ No | Current online player names, returned as a JSON array |
| **Player Metadata** | `%kamenu_meta_<key>%` | Memory | ❌ No | Temporary cached player data |
| **Item Properties** | `%kamenu_checkitem_[source;property]%` | Player Inventory / Memory | ❌ No | Read common properties from held, slotted, or saved items |
| **Inventory Items** | `%kamenu_hasitem_[item attributes]%` | Player Inventory | — | Count of matching items in player's inventory |
| **Stock Items** | `%kamenu_hasstockitem_<itemName>%` | Player Inventory | — | Count of a saved stock item in player's inventory |

**Quick Navigation:**
- 📦 [Player Data Variables](#player-data-variables)
- 🌐 [Global Data Variables](#global-data-variables)
- 📋 [List Variables](#list-variables)
- 👥 [Online Player List Variable](#online-player-list-variable)
- 💾 [Player Metadata Variables](#player-metadata-variables)
- 🧰 [Item Property Variables](#item-property-variables)
- 🎒 [Inventory Item Variables](#inventory-item-variables)
- 📦 [Stock Item Variables](#stock-item-variables)

---

## Provided Variables

### Player Data Variables

Reads persistent per-player data (written via the `set-data` short form or the `data:` parameter action).

**Format:** `%kamenu_data_<key>%`

**Examples:**

| Variable | Description |
|----------|-------------|
| `%kamenu_data_vip_level%` | Reads the player's `vip_level` data |
| `%kamenu_data_nickname%` | Reads the player's `nickname` data |
| `%kamenu_data_sign_count%` | Reads the player's `sign_count` data |

**Usage in other plugins (e.g. CMI scoreboard):**

```yaml
# Scoreboard config example
scoreboard:
  - '&6VIP Level: &f%kamenu_data_vip_level%'
  - '&7Nickname: &f%kamenu_data_nickname%'
```

---

### Global Data Variables

Reads server-wide shared data (written via the `set-gdata` short form or the `gdata:` parameter action).

**Format:** `%kamenu_gdata_<key>%`

**Examples:**

| Variable | Description |
|----------|-------------|
| `%kamenu_gdata_server_event%` | Reads the global `server_event` data |
| `%kamenu_gdata_event_winner%` | Reads the global `event_winner` data |

**Usage in other plugins:**

```yaml
# Announcement config example
announcements:
  - 'Current event status: %kamenu_gdata_server_event%'
  - 'Last event winner: %kamenu_gdata_event_winner%'
```

---

### List Variables

Reads KaMenu built-in list data and returns a JSON string array. This is suitable for dynamic button `repeat.source` and `inList` / `inGlist` condition list parameters.

**Format:**

- `%kamenu_list_<key>%`: read the current player's list
- `%kamenu_glist_<key>%`: read a global list

**Examples:**

| Variable | Description |
|----------|-------------|
| `%kamenu_list_friends%` | Current player's `friends` list |
| `%kamenu_glist_servers%` | Global `servers` list |
| `%kamenu_list_size_friends%` | Item count of the current player's `friends` list |
| `%kamenu_glist_size_servers%` | Item count of the global `servers` list |

```yaml
Bottom:
  type: multi
  buttons:
    friends:
      type: repeat
      source: "%kamenu_list_friends%"
      item:
        text: "&a{item.value}"
```

**Using in conditions:**

```yaml
condition: "%kamenu_list_size_friends% > 0"
condition: "%kamenu_glist_size_servers% >= 3"
```

---

### Online Player List Variable

Reads current online player names and returns a JSON string array.

**Format:** `%kamenu_online_players%`

**Example return value:**

```json
["Steve","Alex","Notch"]
```

**Using with dynamic buttons:**

```yaml
Bottom:
  type: multi
  buttons:
    online_players:
      type: repeat
      source: "%kamenu_online_players%"
      item:
        text: "&a{item.value}"
```

**Using in conditions:**

```yaml
condition: "inGlist.$(target);%kamenu_online_players%"
```

---

### Player Metadata Variables

Reads temporary in-memory metadata for a player (written via the `set-meta` short form or the `meta:` parameter action).

**Format:** `%kamenu_meta_<key>%`

**Examples:**

| Variable | Description |
|----------|-------------|
| `%kamenu_meta_time%` | Reads the player's `time` metadata |
| `%kamenu_meta_last_menu%` | Reads the player's `last_menu` metadata |
| `%kamenu_meta_temp_data%` | Reads the player's `temp_data` metadata |

**Usage in other plugins:**

```yaml
# Scoreboard config example (showing temporary data)
scoreboard:
  - '&6Last Visit: &f%kamenu_meta_time%'
  - '&7Temp Status: &f%kamenu_meta_temp_status%'
```

**Notes:**
- Metadata is stored in memory only and is not persisted to the database
- Player metadata is automatically cleared when the player disconnects
- All metadata is cleared when the plugin is reloaded or the server stops
- Returns `"null"` if the data does not exist

---

### Item Property Variables

KaMenu can read common item properties. Prefer the built-in brace variable in menus because it does not require PlaceholderAPI; use PAPI when another plugin needs the value:

```text
{checkitem:[hand;name]}
%kamenu_checkitem_[hand;name]%
```

Both forms use the same sources, properties, and return values.

**Parameter structure:**

```text
{checkitem:[<item location>;<output property>;<optional format>]}
%kamenu_checkitem_[<item location>;<output property>;<optional format>]%
```

`checkitem` accepts up to three parameters separated by English semicolons (`;`):

| Order | Parameter | Required | Purpose |
|-------|-----------|----------|---------|
| First | Item location/source | ✅ | Locates the item to read, such as `hand`, `offhand`, `slot:0`, or `stock:Magic Sword` |
| Second | Output property | ✅ | Selects the exact value to return, such as `name`, `lore:1`, `ench:sharpness`, or `dura_pct` |
| Third | Text format | ❌ | Uses `fmt:plain`, `fmt:legacy`, or `fmt:mini`; defaults to `fmt:plain` when omitted |

For example, `{checkitem:[slot:0;lore:1;fmt:mini]}` consists of:

1. `slot:0`: locate slot `0` in the player's inventory.
2. `lore:1`: return lore line `1` from that item; lore line numbers start at `1`.
3. `fmt:mini`: convert that line to MiniMessage format.

Semicolons (`;`) separate the three top-level parameters, while colons (`:`) assign values inside a parameter. Therefore `slot:0`, `lore:1`, and `fmt:mini` are each one complete parameter. Inventory slots start at `0`, while lore line numbers start at `1`.

**Item sources:**

| Source | Description |
|--------|-------------|
| `hand` | Current player's main-hand item |
| `offhand` | Current player's off-hand item |
| `slot:<index>` | A Bukkit player inventory slot |
| `stock:<name>` | KaMenu saved-item storage; reads the memory cache without querying SQL |

**Properties:**

| Property | Return value |
|----------|--------------|
| `type` | Full material ID, such as `minecraft:diamond_sword` |
| `amt` | Stack amount |
| `name` | Effective item display name |
| `lore` | Lore as a JSON string array |
| `lore:<line>` | One lore line, starting at `1`; `lore:1` means the first line |
| `enchants` | A JSON array such as `[ {"key":"minecraft:sharpness","level":5} ]` |
| `ench:<enchantment ID>` | Enchantment level; returns `0` when absent |
| `model` / `item_model` | Item-model NamespacedKey |
| `cmd` / `custom_model_data` / `custom_model_id` | Custom model ID, returned as an integer by default; empty when absent |
| `dmg` | Consumed durability |
| `dura` | Remaining durability |
| `dura_pct` | Remaining durability from `0` to `100`, without `%` |

Names and lore use plain text by default. The format option uses `fmt:<format>` as the third semicolon-separated argument:

| Format option | Returned content |
|---------------|------------------|
| `fmt:plain` | Plain text and the default; removes colors and events |
| `fmt:legacy` | Preserves `&` Legacy color formatting |
| `fmt:mini` | Converts to MiniMessage format |

`fmt` only affects `name`, `lore`, and `lore:<line>`. Material, model, enchantment, and numeric properties are unaffected.

Each line below is an independent example for its corresponding menu field. Do not place multiple duplicate `text` keys in the same YAML node:

```yaml
# Return the effective display name of the main-hand item; plain text is used because fmt is omitted
text: '&fMain hand: {checkitem:[hand;name]}'

# Return remaining main-hand durability as a percentage; the placeholder does not include the percent sign
text: '&7Durability: {checkitem:[hand;dura_pct]}%'

# Check whether the main-hand item has Sharpness level 5 or higher
condition: '{checkitem:[hand;ench:sharpness]} >= 5'

# Return all enchantments from the saved “Legendary Sword” as JSON for use as a repeat source
source: '{checkitem:[stock:Legendary Sword;enchants]}'

# Return lore line 1 from the saved “Legendary Sword” and convert it to MiniMessage format
text: '{checkitem:[stock:Legendary Sword;lore:1;fmt:mini]}'

# Return both the main-hand ItemModel NamespacedKey and integer CustomModelData
text: 'Model: {checkitem:[hand;item_model]} / ID: {checkitem:[hand;custom_model_id]}'
```

Vanilla enchantments may omit `minecraft:`, such as `ench:sharpness`. Custom enchantments require their full namespace, such as `ench:myplugin:lifesteal`.

When a saved-item name contains semicolons, wrap the item name in backticks to avoid conflicts with YAML single- or double-quoted strings:

```yaml
text: '{checkitem:[stock:`Event;Sword`;name]}'
```

The outer YAML value may still use single or double quotes. Only backticks delimit the inner item name; `checkitem` does not treat single or double quotes as argument delimiters.

Missing strings return an empty string, missing numbers return `0`, and missing lists return `[]`. Player inventory sources can only be read on the Paper main thread or the player's current Folia region thread. Asynchronous third-party PAPI requests do not block across threads and therefore receive empty player-item values. The `stock:` source is not subject to this restriction.

---

### Inventory Item Variables

Counts matching items (by material, lore, model) in the player's inventory.

**Format:** `%kamenu_hasitem_[mats=material;lore=description;model=item-model;custom_model_id=integer-ID]%`

**Parameter Description:**

| Parameter | Required | Description | Example |
|-----------|----------|-------------|---------|
| `mats` | ✅ Yes | Item material type | `DIAMOND`, `GOLD_INGOT`, `IRON_INGOT` |
| `lore` | ❌ No | Item lore text (fuzzy match) | `artifact`, `crafting material` |
| `model` / `item_model` | ❌ No | ItemModel (namespace:key format) | `minecraft:custom_item` |
| `cmd` / `custom_model_data` / `custom_model_id` | ❌ No | Integer CustomModelData | `10001` |

**Examples:**

| Variable | Description |
|----------|-------------|
| `%kamenu_hasitem_[mats=DIAMOND]%` | Returns the count of diamonds in the player's inventory |
| `%kamenu_hasitem_[mats=GOLD_INGOT]%` | Returns the count of gold ingots in the player's inventory |
| `%kamenu_hasitem_[mats=DIAMOND;lore=artifact]%` | Returns the count of diamonds with "artifact" in their lore |
| `%kamenu_hasitem_[mats=IRON_INGOT;lore=crafting material;model=custom:iron]%` | Returns the count of iron ingots matching material, lore, and model |
| `%kamenu_hasitem_[mats=PAPER;custom_model_id=10001]%` | Returns the number of paper items with custom model ID `10001` |

**Usage in menus:**

```yaml
# Display item count
Body:
  diamond_info:
    type: 'message'
    text: '&aDiamonds in inventory: &f%kamenu_hasitem_[mats=DIAMOND]%'

# Conditional check
Bottom:
  type: 'multi'
  buttons:
    check_diamond:
      text: '&aUse Diamonds'
      actions:
        - condition: '%kamenu_hasitem_[mats=DIAMOND]% >= 10'
          allow:
            - 'tell: &aYou have enough diamonds!'
          deny:
            - 'tell: &cNot enough diamonds, you only have %kamenu_hasitem_[mats=DIAMOND]%'
```

---

### Stock Item Variables

Counts how many of a saved stock item the player has in their inventory.

**Format:** `%kamenu_hasstockitem_<itemName>%`

**Parameter Description:**

| Parameter | Required | Description |
|-----------|----------|-------------|
| `itemName` | ✅ Yes | The name of the saved stock item |

{% hint style="info" %}
**How to save items?**

Stock items must be saved to the database via command before they can be used.

1. Hold the item you want to save
2. Run `/km item save <item_name>`
3. For full details see: [📝 Commands - /km item](perm/commands.md#km-item)
{% endhint %}

**Examples:**

| Variable | Description |
|----------|-------------|
| `%kamenu_hasstockitem_MysticFruit%` | Returns the count of "MysticFruit" in the player's inventory |
| `%kamenu_hasstockitem_TeleportScroll%` | Returns the count of "TeleportScroll" in the player's inventory |
| `%kamenu_hasstockitem_AdvancedExpBook%` | Returns the count of "AdvancedExpBook" in the player's inventory |

**Usage in menus:**

```yaml
# Display stock item count
Body:
  fruit_info:
    type: 'message'
    text: '&aMystic Fruits: &f%kamenu_hasstockitem_MysticFruit%'

# Conditional check
Bottom:
  type: 'multi'
  buttons:
    use_fruit:
      text: '&aUse Mystic Fruit'
      actions:
        - condition: '%kamenu_hasstockitem_MysticFruit% >= 5'
          allow:
            - 'tell: &aYou have enough Mystic Fruits!'
          deny:
            - 'tell: &cNot enough Mystic Fruits, you only have %kamenu_hasstockitem_MysticFruit%'
```

**Notes:**
- The item name must match the filename in the item stock
- Returns `0` if the item does not exist
- Item matching uses the `isSimilar` method, which includes material and NBT
- Returns the total count of all matching items regardless of stack size

---

## Using PAPI Variables Inside KaMenu Menus

KaMenu fully supports **parsing variables from other PAPI extensions** — use them in any text field or condition inside a menu:

### Menu Title

```yaml
Title: '&aWelcome, &f%player_name%!'
```

### Component Text

```yaml
Body:
  stats:
    type: 'message'
    text: '&7Level: &f%player_level% &7| Health: &c%player_health%'
```

### Conditions

```yaml
condition: "%player_level% >= 10 && %player_balance% >= 500"
```

### Actions

```yaml
actions:
  - 'tell: &aHello, %player_name%! You are in %player_world%.'
  - 'console: give %player_name% diamond 1'
```

---

## Handling Missing Data

When the specified key does not exist:

1. **PAPI Variables (`%kamenu_data_key%`)**:
   - Returns the message defined in the language file (key: `papi.data_not_found`)
   - Defaults to an empty string
   - Can be customized in the language file

2. **Built-in Variables (`{data:key}`)**:
   - Returns the literal string `"null"`
   - You can use `{data:key} == null` in conditions to check if data exists
   - Example: `{data:counter} != null` means the data exists

3. **PAPI Metadata Variables (`%kamenu_meta_key%`)**:
   - Returns the literal string `"null"`
   - Metadata is stored in memory only; it is automatically cleared when the player disconnects or the plugin is reloaded
   - You can use `{meta:key} == null` in conditions to check if metadata exists

**Example:**

```yaml
# Check if data exists using built-in variable
actions:
  - condition: "{data:counter} != null"
    allow:
      - 'tell: Data exists, value: {data:counter}'
    deny:
      - 'tell: Data does not exist'

# In scoreboard using PAPI variable (shows default prompt if not found)
scoreboard:
  - '&6Visit Count: &f%kamenu_data_visit_count%'
  # If visit_count does not exist, shows the default prompt or empty string
```

---

## Usage Scenario Examples

### Display Player Data on Scoreboard

```yaml
scoreboard:
  title: '&6Player Info'
  lines:
    - '&aPlayer: &f%player_name%'
    - '&bLevel: &f%player_level%'
    - '&cVIP: &f%kamenu_data_vip_level%'
    - '&dNickname: &f%kamenu_data_nickname%'
    - '&eVisits: &f%kamenu_data_visit_count%'
```

### Display Global Data on Announcement Board

```yaml
announcements:
  - '&6Current Event: &f%kamenu_gdata_server_event%'
  - '&aWinner: &f%kamenu_gdata_event_winner%'
  - '&bServer Status: &f%kamenu_gdata_server_status%'
```

### Use in Chat Format

```yaml
chat_format:
  format: '&7[%kamenu_data_vip_level%&7] &f%player_name%: &7%message%'
  # If the player has no VIP level data, shows [] or a custom prompt
```

### Store Temporary State with Metadata

```yaml
# Set temporary data in a menu
actions:
  - 'set-meta: last_visit %player_time%'
  - 'set-meta: temp_status shopping'

# Read temporary data in another menu
Title: 'Welcome back, %player_name%'
Body:
  status:
    type: 'message'
    text: '&7Last Visit: %kamenu_meta_last_visit% | Status: %kamenu_meta_temp_status%'
```

### Display Item Counts on Scoreboard

```yaml
scoreboard:
  title: '&6Inventory Info'
  lines:
    - '&aPlayer: &f%player_name%'
    - '&6Diamonds: &f%kamenu_hasitem_[mats=DIAMOND]%'
    - '&eGold Ingots: &f%kamenu_hasitem_[mats=GOLD_INGOT]%'
    - '&7Iron Ingots: &f%kamenu_hasitem_[mats=IRON_INGOT]%'
    - '&dMystic Fruits: &f%kamenu_hasstockitem_MysticFruit%'
    - '&bTeleport Scrolls: &f%kamenu_hasstockitem_TeleportScroll%'
```

### Item Condition Check in Menu

```yaml
# Check if the player has enough diamonds
Bottom:
  type: 'multi'
  buttons:
    buy_item:
      text: '&aBuy Item'
      actions:
        - condition: '%kamenu_hasitem_[mats=DIAMOND]% >= 10'
          allow:
            - 'tell: &aPayment successful!'
            - 'command: give %player_name% diamond_sword 1'
            - 'console: clear %player_name% diamond 10'
          deny:
            - 'tell: &cNot enough diamonds! You need 10 diamonds.'

    use_fruit:
      text: '&bUse Mystic Fruit'
      actions:
        - condition: '%kamenu_hasstockitem_MysticFruit% >= 1'
          allow:
            - 'tell: &aUsed a Mystic Fruit!'
            - 'command: effect %player_name% regeneration 600 2'
            - 'data: take MysticFruit 1'
          deny:
            - 'tell: &cYou have no Mystic Fruits!'
```

---

## Notes

1. **PAPI Plugin Dependency**: PlaceholderAPI must be installed to use these variables
2. **Data Persistence**:
   - Data written by `set-data` / `data:` and `set-gdata` / `gdata:` is persisted to the database
   - Data written by `set-meta` / `meta:` is stored in memory only and is not persisted
3. **Key Prefixes**: Player data, global data, and metadata use different prefixes (`data_`, `gdata_`, `meta_`) and have different storage locations and lifetimes
4. **Type Limitation**: All data is stored as strings; type conversion must be handled as needed
5. **Performance**: Reading large amounts of data frequently may impact performance — use wisely
6. **Checking Data Existence**:
   - PAPI variables: returns a prompt or empty string if data does not exist
   - Built-in variables: returns `"null"` if data does not exist, suitable for condition checks
   - Metadata: returns `"null"` if data does not exist; automatically cleared on disconnect or reload
7. **Metadata Lifecycle**:
   - Metadata is stored in memory only
   - Player metadata is cleared on disconnect
   - All metadata is cleared on plugin reload or server shutdown
   - Suitable for storing temporary short-lived data
8. **Item Variable Features**:
   - `hasitem` variables support multi-condition matching (material, lore, ItemModel, integer CustomModelData)
   - `hasstockitem` variables use the saved stock item for exact matching
   - Returns `0` if the item does not exist
   - Item count is calculated in real time as the sum of all matching items
   - Supported in both conditions and text display
9. **Parameter Format**:
   - Item variables use square brackets `[]` to wrap parameters (brackets are optional)
   - Parameters are separated by semicolons `;`
   - Lore matching supports fuzzy matching (case-insensitive)
   - Model format is `namespace:key`
