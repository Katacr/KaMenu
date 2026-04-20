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
| **Player Metadata** | `%kamenu_meta_<key>%` | Memory | ❌ No | Temporary cached player data |
| **Inventory Items** | `%kamenu_hasitem_[item attributes]%` | Player Inventory | — | Count of matching items in player's inventory |
| **Stock Items** | `%kamenu_hasstockitem_<itemName>%` | Player Inventory | — | Count of a saved stock item in player's inventory |

**Quick Navigation:**
- 📦 [Player Data Variables](#player-data-variables)
- 🌐 [Global Data Variables](#global-data-variables)
- 💾 [Player Metadata Variables](#player-metadata-variables)
- 🎒 [Inventory Item Variables](#inventory-item-variables)
- 📦 [Stock Item Variables](#stock-item-variables)

---

## Provided Variables

### Player Data Variables

Reads persistent per-player data (written via the `set-data` action).

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

Reads server-wide shared data (written via the `set-gdata` action).

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

### Player Metadata Variables

Reads temporary in-memory metadata for a player (written via the `set-meta` action).

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

### Inventory Item Variables

Counts matching items (by material, lore, model) in the player's inventory.

**Format:** `%kamenu_hasitem_[mats=material;lore=description;model=model]%`

**Parameter Description:**

| Parameter | Required | Description | Example |
|-----------|----------|-------------|---------|
| `mats` | ✅ Yes | Item material type | `DIAMOND`, `GOLD_INGOT`, `IRON_INGOT` |
| `lore` | ❌ No | Item lore text (fuzzy match) | `artifact`, `crafting material` |
| `model` | ❌ No | Item model (namespace:key format) | `minecraft:custom_item` |

**Examples:**

| Variable | Description |
|----------|-------------|
| `%kamenu_hasitem_[mats=DIAMOND]%` | Returns the count of diamonds in the player's inventory |
| `%kamenu_hasitem_[mats=GOLD_INGOT]%` | Returns the count of gold ingots in the player's inventory |
| `%kamenu_hasitem_[mats=DIAMOND;lore=artifact]%` | Returns the count of diamonds with "artifact" in their lore |
| `%kamenu_hasitem_[mats=IRON_INGOT;lore=crafting material;model=custom:iron]%` | Returns the count of iron ingots matching material, lore, and model |

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
   - Data written by `set-data` and `set-gdata` is persisted to the database
   - Data written by `set-meta` is stored in memory only and is not persisted
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
   - `hasitem` variables support multi-condition matching (material, lore, model)
   - `hasstockitem` variables use the saved stock item for exact matching
   - Returns `0` if the item does not exist
   - Item count is calculated in real time as the sum of all matching items
   - Supported in both conditions and text display
9. **Parameter Format**:
   - Item variables use square brackets `[]` to wrap parameters (brackets are optional)
   - Parameters are separated by semicolons `;`
   - Lore matching supports fuzzy matching (case-insensitive)
   - Model format is `namespace:key`
