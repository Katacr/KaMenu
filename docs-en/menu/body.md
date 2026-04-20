# 🧩 Body Components (Body)

The `Body` node displays various content in the main area of the menu, such as plain text messages and item displays.

---

## Configuration Structure

```yaml
Body:
  component-name:
    type: 'component-type'
    # Component-specific configuration...
```

- **Component name**: Any string, used as the unique identifier for this component (must be unique within the same menu)
- **Component order**: Displayed top-to-bottom in the order they appear in the YAML file

---

## Component Types

### message — Plain Text Message

Displays one or more lines of plain text in the menu.

**Configuration options:**

| Field | Type | Description |
|-------|------|-------------|
| `type` | `String` | Fixed value `message` |
| `text` | `String`/`List` | Message text; supports multiple formats (see below) |
| `width` | `Int` | Optional; message width (1–1024); defaults to 200 if not set |

---

### Text Format Support

KaMenu supports both the **MiniMessage API** and **Legacy color codes**, which can be used freely or mixed.

**Supported formats:**

1. **Legacy color codes**: `&aGreen text`, `&cRed text`, `&6Gold text`
2. **MiniMessage format**: `<green>Green text</green>`, `<red>Red text</red>`, `<gold>Gold text</gold>`

**Auto-detection mechanism:**
- The system automatically detects whether MiniMessage tags (`<...>`) are present in the text
- If detected, the text is parsed using MiniMessage
- If not detected, Legacy color codes are used
- Both formats can be mixed and the system handles them automatically

**Examples:**

```yaml
# Using Legacy color codes
text: '&aWelcome to the server'

# Using MiniMessage format
text: '<green>Welcome to the server</green>'

# Mixed
text: '&aGreen text <gold>Gold text</gold>'
```

**Advantages of MiniMessage:**
- A more modern text format supporting more styles (bold, italic, underline, etc.)
- Cleaner tag structure that is easier to maintain
- Fully compatible with the Adventure API

---

### Multiple Formats for the `text` Field

The `text` field supports three formats for different scenarios:

#### 1. Single-Line Text (Supports `\n` for line breaks)

The simplest format, using `\n` to break lines.

```yaml
Body:
  welcome_msg:
    type: 'message'
    text: '&7Welcome to the server shop\n&7Click the button below to browse'
```

**Characteristics:**
- Suitable for static multi-line text
- Supports the `\n` line break character
- Automatically resolves variables and color codes

#### 2. List Mode (One Element Per Line)

Uses a YAML list with one string per line.

```yaml
Body:
  welcome_msg:
    type: 'message'
    text:
      - '&7Welcome to the server shop'
      - '&7Click the button below to browse'
      - '&7Happy shopping!'
```

**Characteristics:**
- Cleaner configuration format
- Each line is managed independently
- Good for long text editing
- Variables and color codes are resolved independently per line

#### 3. Condition Check Mode (Supports allow/deny Branches)

Displays different text based on conditions. Both `allow` and `deny` branches support two sub-formats:

**Format A: List mode**

```yaml
Body:
  status_msg:
    type: 'message'
    text:
      - condition: '%player_is_op% == true'
        allow:
          - '&a✔ Current role: Admin'
          - '&7You have all permissions'
        deny:
          - '&7Current role: Regular Player'
          - '&7Contact an admin if you need an upgrade'
```

**Format B: String mode (supports `\n` line breaks)**

```yaml
Body:
  status_msg:
    type: 'message'
    text:
      - condition: '%player_is_op% == true'
        allow: '&a✔ Current role: Admin\n&7You have all permissions'
        deny: '&7Current role: Regular Player\n&7Contact an admin if you need an upgrade'
```

**Format A vs Format B comparison:**

| Feature | List mode (A) | String mode (B) |
|---------|--------------|-----------------|
| Readability | ✅ Cleaner; each line is independent | ⚠️ Requires `\n` separators |
| Variable resolution | ✅ Each line resolved independently | ✅ Full text resolved at once |
| Best for | Multi-line, formatted text | Short text or logically connected content |

**Characteristics:**
- Dynamically displays content based on conditions
- Both `allow` and `deny` branches support list and string formats
- In list mode, each line is processed and variable-resolved independently
- Both formats support the `\n` line break character
- Supports nested condition checks
- Both formats can be mixed (one uses a list, the other uses a string)

---

### Advanced Examples

**Multi-line + variables + colors:**

```yaml
Body:
  player_info:
    type: 'message'
    text:
      - '&aPlayer name: &f{player_name}'
      - '&aPlayer balance: &e%vault_eco_balance% &7coins'
      - '&aOnline time: &e%player_time_played%'
      - '&aServer name: &f{gdata:server_name}'
```

**Multi-line + clickable text:**

```yaml
Body:
  welcome_msg:
    type: 'message'
    text:
      - '&7Welcome to the server!'
      - '&7Click below to continue'
      - '&e<text=''View Rules'';hover=''Click to view server rules'';command=''/rules''>'
```

**Conditional multi-line text (list mode):**

```yaml
Body:
  vip_status:
    type: 'message'
    text:
      - condition: '%vault_rank% == VIP'
        allow:
          - '&a✓ You are a valued VIP member'
          - '&7Expiry: &e%player_vip_expiry%'
          - '&7Enjoy all premium services'
        deny:
          - '&7You are not a VIP member yet'
          - '&7Click the button below to upgrade'
          - '&7Only &e10 &7coins/month'
```

**Conditional multi-line text (`\n` line break mode):**

```yaml
Body:
  vip_status:
    type: 'message'
    text:
      - condition: '%vault_rank% == VIP'
        allow: '&a✓ You are a valued VIP member\n&7Expiry: &e%player_vip_expiry%\n&7Enjoy all premium services'
        deny: '&7You are not a VIP member yet\n&7Click the button below to upgrade\n&7Only &e10 &7coins/month'
```

**Mixed mode (allow uses list, deny uses string):**

```yaml
Body:
  mixed_format:
    type: 'message'
    text:
      - condition: '%player_is_op% == true'
        allow:
          - '&6[ Admin Panel ]'
          - '&7You have full access'
          - '&7All features are available to you'
        deny: '&7[ User Panel ]\n&7You can only access basic features\n&7Contact an admin for more permissions'
```

**Nested conditions:**

```yaml
Body:
  player_type:
    type: 'message'
    text:
      - condition: '%player_is_op% == true'
        allow:
          - condition: '%player_name% == AdminPlayer'
            allow: '&6Server Administrator'
            deny: '&6Admin Account'
        deny:
          - condition: '%vault_rank% == VIP'
            allow: '&aVIP Player'
            deny: '&7Regular Player'
```

---

### Complete Examples

**Basic example:**

```yaml
Body:
  welcome_msg:
    type: 'message'
    text: '&7Welcome to the server shop. Click the button below to browse items.'

  separator:
    type: 'message'
    text: '&8————————————————'
```

**Custom width example:**

```yaml
Body:
  wide_message:
    type: 'message'
    text: '&7This is a message with a width of 300'
    width: 300

  normal_message:
    type: 'message'
    text: '&7This is a message with the default width'
```

**Conditional width example:**

```yaml
Body:
  dynamic_width:
    type: 'message'
    text: '&7Message content'
    width:
      - condition: "%player_is_op% == true"
        allow: 400
        deny: 200
```

**Interactive text examples (using hovertext syntax):**

```yaml
Body:
  # Single clickable link
  simple_link:
    type: 'message'
    text: '<text=&b[ Click to visit the server website ];hover=&aClick to open our official website;url=https://example.com>'

  # Click text with a command
  command_link:
    type: 'message'
    text: '<text=&e[ Click to claim daily reward ];hover=&6Click to claim your daily reward now;command=dailyreward claim>'

  # Mixed plain text and clickable text
  mixed_text:
    type: 'message'
    text: '&7Welcome to the server! <text=&a[ Claim Reward ];hover=&6Click to claim daily reward;command=daily> Visit <text=&b[ Website ];hover=&7Open website;url=https://example.com> for more info.'

  # Multiple clickable areas
  multi_link:
    type: 'message'
    text: '&7Navigation: <text=&a[ Shop ];hover=&cOpen shop;command=shop> <text=&b[ Bag ];hover=&cOpen bag;command=bag> <text=&e[ Help ];hover=&cView help;command=help>'

  # Condition + clickable text
  conditional_click:
    type: 'message'
    text:
      - condition: '%player_is_op% == true'
        allow: '<text=&4[ Admin Panel ];hover=&aOpen admin panel;command=admin>'
        deny: '<text=&7[ Player Panel ];hover=&aOpen player panel;command=player>'
```

**hovertext syntax format:**

```
<text=display-text;hover=hover-text;command=command;url=url;actions=action-list-name;newline=false>
```

**Parameter descriptions:**

| Parameter | Description | Required |
|-----------|-------------|----------|
| `text` | The clickable display text | ✅ |
| `hover` | Tooltip text shown on hover | ❌ |
| `command` | Command executed by the player on click | ❌ |
| `url` | URL opened on click | ❌ |
| `actions` | Action list to execute on click (key under Events.Click) | ❌ |
| `newline` | Whether to add a line break after the text (`true`/`false`) | ❌ |

**Notes:**
- Commands in `command` are executed as the player (no `/` prefix needed)
- `url` opens a webpage link
- `actions` executes action lists defined under Events.Click
- Clickable areas are wrapped in `< >`
- Parameter values can be wrapped in backticks `` ` ``, single quotes `'`, or double quotes `"`
- Plain text and clickable text can be mixed
- Color codes and PAPI variables are supported
- All fields support condition checks

**Click event priority:**

When multiple click parameters coexist, priority is as follows (highest to lowest):
1. `actions` — Execute action list
2. `url` — Open link
3. `command` — Execute command

**Using the `actions` parameter:**

```yaml
Events:
  Click:
    greet:
      - 'tell: &aHello! Welcome to the server.'
      - 'sound: ENTITY_PLAYER_LEVELUP'

Body:
  welcome_msg:
    type: 'message'
    text: '<text="Click to Greet";actions=greet;hover=Click to run the greet action> or view <text="Website";url=https://example.com;hover=Open website>'
```

**Use cases:**

- **Button action list reuse**: Multiple texts execute the same action sequence
- **Conditional branching**: Execute different actions based on player state
- **Inline actions**: Reference action lists from Events.Click directly
- **Links and actions combined**: Text that includes both links and actions

---

### item — Item Display

Displays an item icon in the menu, optionally with a name, Lore, and description text.

**Configuration options:**

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `type` | `String` | ✅ | — | Fixed value `item` |
| `material` | `String` | ✅ | `PAPER` | Item material (multiple formats supported; see below) |
| `amount` | `Int` | ❌ | `1` | Stack size (1–64); defaults to 1 if not set |
| `name` | `String` | ❌ | Default item name | Display name; supports color codes |
| `lore` | `List<String>` | ❌ | — | Item Lore (list of description lines) |
| `description` | `String` | ❌ | — | Extra description text shown below the item; supports color codes, PAPI variables, condition checks, and clickable text syntax |
| `description_width` | `Int` | ❌ | `0` | Width of the description text box (1–1024 px); defaults to 200 if not set or set to 0 |
| `item_model` | `String` | ❌ | — | Item model identifier (format: `namespace:key`); used to display items with special appearances (e.g., namespaced item models in 1.21.7+) |
| `width` | `Int` | ❌ | `16` | Item icon width (px) |
| `height` | `Int` | ❌ | `16` | Item icon height (px) |
| `show_overlays` | `Boolean` | ❌ | `true` | Whether to show item overlays (durability bar, cooldown, count, etc.) |
| `tooltip` | `Boolean` | ❌ | `true` | Whether to show the item tooltip on hover |

**Example:**

```yaml
Body:
  featured_item:
    type: 'item'
    material: 'ENCHANTED_BOOK'
    name: '&6&lSacred Sword'
    lore:
      - '&7A legendary weapon'
      - '&cAttack: &f+20'
      - '&ePrice: &f500 coins'
    description: '&fClick the button below to purchase this weapon'
    width: 16
    height: 16

  # Item with custom description width
  custom_width_item:
    type: 'item'
    material: 'DIAMOND'
    name: '&b&lDiamond'
    description: '&7A rare diamond\n&7Worth 1000 coins'
    description_width: 200  # Custom description width
    width: 16
    height: 16

  # Hide overlays (durability bar, count, cooldown, etc.)
  no_overlays_item:
    type: 'item'
    material: 'DIAMOND_SWORD'
    name: '&6&lLegendary Sword'
    show_overlays: false  # Do not show durability bar, count, etc.
    width: 16
    height: 16

  # Item with custom stack count
  custom_item:
    type: 'item'
    material: 'DIAMOND'
    name: '&b&lDiamond'
    amount: 16  # Set stack count to 16
    width: 16
    height: 16
```

**`amount` property:**

The `amount` property sets the item's stack count; defaults to `1`.

- **Value range**: 1–64 (depending on the item's maximum stack size)
- **Use cases**:
  - Displaying stacked quantities in a shop
  - Showing item counts in inventory previews
  - Displaying multiple items in equipment showcases

- **Examples:**

  **Example 1: Single item**
  ```yaml
  single_item:
    type: 'item'
    material: 'DIAMOND_SWORD'
    name: '&6Diamond Sword'
    amount: 1  # Single item (default)
    width: 16
    height: 16
  ```

  **Example 2: Stacked items**
  ```yaml
  stack_item:
    type: 'item'
    material: 'DIAMOND'
    name: '&bDiamond'
    amount: 64  # Maximum stack
    width: 16
    height: 16
  ```

  **Example 3: Conditional count**
  ```yaml
  vip_item:
    type: 'item'
    material: 'GOLD_INGOT'
    name: '&eGold Ingot'
    amount:
      - condition: '%vault_rank% == VIP'
        allow: 32  # VIP players see 32
        deny: 16  # Regular players see 16
    width: 16
    height: 16
  ```

  **Example 4: Showing count when show_overlays: false**
  ```yaml
  # When show_overlays: false, the stack count will be hidden
  # You can include the count in the name attribute for display
  item_with_amount:
    type: 'item'
    material: 'DIAMOND'
    name: '&bDiamond x64'  # Display count in name
    amount: 64  # Actual count (but not shown on the icon)
    show_overlays: false  # Disable overlays; count will be hidden
    width: 16
    height: 16
  ```


- **Notes**:
  - Values exceeding the item's maximum stack size are automatically capped
  - For tools and weapons that cannot be stacked, the count is capped at 1
  - In slot reference mode, the `amount` property has no effect (the actual slot count is shown)
  - **Important**: When `show_overlays: false`, the item's stack count (e.g., "64") is hidden, as overlays include the durability bar, cooldown, and count. If you need to display the count on screen, include it in the `name` property, e.g., `name: '&bDiamond x64'`


**`name` property is optional:**

  - The `name` property is optional. If not provided, the item's default name will be used.


**`description_width` is optional:**

  - The `description_width` property is optional and sets the width of the description text (in pixels). If not set or set to 0, the default width of 200 is used.

```yaml
Body:
  # Use item's default name
  diamond_sword:
    type: 'item'
    material: 'DIAMOND_SWORD'
    # name not set; uses the default item name "Diamond Sword"
    width: 16
    height: 16

  # Custom item name
  custom_sword:
    type: 'item'
    material: 'DIAMOND_SWORD'
    name: '&6&lLegendary Sword'  # Override default name
    width: 16
    height: 16

  # Using description_width
  wide_description_item:
    type: 'item'
    material: 'ENCHANTED_BOOK'
    name: '&6Magic Book'
    description: '&7A magical book\n&7Can cast powerful spells'
    description_width: 300  # Set description width to 300px
    width: 16
    height: 16

  # description and description_width both support condition checks
  conditional_description_item:
    type: 'item'
    material: 'DIAMOND'
    name: '&bDiamond'
    description:
      - condition: '%player_level% >= 10'
        allow: '&aAdvanced item\n&7Purchase unlocked'
        deny: '&7Requires level &e10&7 to purchase'
    description_width:
      - condition: '%player_level% >= 10'
        allow: 300
        deny: 200
    width: 16
    height: 16
```

{% hint style="info" %}
If the `name` property is not provided, the system uses the item material's default name. Set `name` only when you want a custom display name.
{% endhint %}

**Material name format support:**

KaMenu supports multiple material name formats; the system automatically normalises and matches the corresponding Material enum:

- Standard: `DIAMOND_SWORD`
- Lowercase: `diamond_sword`
- Mixed case: `DiAMond swORd`
- Hyphenated: `Diamond-Sword`
- Spaced: `diamond sword`
- Underscored: `diamond_sword`

**Examples:**

```yaml
# All of the following resolve to DIAMOND_SWORD
material: 'diamond_sword'
material: 'Diamond_Sword'
material: 'Diamond-Sword'
material: 'diamond sword'
material: 'diAMond swORd'
```

{% hint style="info" %}
The system automatically ignores case, replaces hyphens and spaces with underscores, and merges extra underscores — so all the formats above will correctly resolve to `DIAMOND_SWORD`.
{% endhint %}

---

### Item Slot References

The `material` field supports the slot reference format, allowing you to directly display the item in a player's equipment slot.

**Format:**

`[SLOT]` or `[SLOT:PlayerName]` or `[SLOT:{variable}]`

**Supported slots:**

| Slot Name | Description | Bukkit Constant |
|-----------|-------------|----------------|
| `HEAD` | Head (helmet) | `EquipmentSlot.HEAD` |
| `CHEST` | Chest (chestplate) | `EquipmentSlot.CHEST` |
| `LEGGINGS` | Leggings | `EquipmentSlot.LEGS` |
| `BOOTS` | Boots | `EquipmentSlot.FEET` |
| `MAINHAND` | Main hand | `EquipmentSlot.HAND` |
| `OFFHAND` | Off-hand | `EquipmentSlot.OFF_HAND` |

**Basic examples:**

```yaml
Body:
  # Display the current player's helmet
  player_helmet:
    type: 'item'
    material: '[HEAD]'
    width: 16
    height: 16

  # Display a specified player's helmet
  admin_helmet:
    type: 'item'
    material: '[HEAD:AdminPlayer]'
    width: 16
    height: 16
```

**Using with variables:**

Slot references support all variable types (PAPI variables, built-in variables, Meta variables, etc.):

```yaml
Body:
  # Using a Meta variable (combined with the player-click listener)
  target_helmet:
    type: 'item'
    material: '[HEAD:{meta:player}]'
    width: 16
    height: 16

  # Using a data storage variable
  saved_player_helmet:
    type: 'item'
    material: '[CHEST:{data:target_player}]'
    width: 16
    height: 16

  # Using a PAPI variable
  random_player_sword:
    type: 'item'
    material: '[MAINHAND:%random_online_player%]'
    width: 16
    height: 16
```

**Empty slot handling:**

When the referenced slot is empty, a fallback item is shown automatically:

| Slot | Empty Display |
|------|--------------|
| `HEAD` | Player's skin head |
| Other slots | Light grey glass pane with name "Empty" |

```yaml
Body:
  # HEAD slot empty -> show player skull
  helmet_display:
    type: 'item'
    material: '[HEAD:{meta:player}]'
    width: 16
    height: 16

  # Other slots empty -> show light grey glass pane
  chestplate_display:
    type: 'item'
    material: '[CHEST:{meta:player}]'
    width: 16
    height: 16
```

**Complete example — Player inspection menu:**

```yaml
# menus/inspect_player.yml
Title: 'Player Info'
Background: '#1a1a1a'

Body:
  # Show player's helmet
  helmet:
    type: 'item'
    material: '[HEAD:{meta:player}]'
    description: '&7View head equipment'
    width: 16
    height: 16

  # Show player's chestplate
  chestplate:
    type: 'item'
    material: '[CHEST:{meta:player}]'
    description: '&7View chest equipment'
    width: 16
    height: 16

  # Show player's leggings
  leggings:
    type: 'item'
    material: '[LEGGINGS:{meta:player}]'
    description: '&7View leg equipment'
    width: 16
    height: 16

  # Show player's boots
  boots:
    type: 'item'
    material: '[BOOTS:{meta:player}]'
    description: '&7View foot equipment'
    width: 16
    height: 16

Events:
  Click:
    # Use slot references to display target player's equipment
```

{% hint style="info" %}
In slot reference mode, use the `description` property to add descriptive text, not the `name` property, because `name` has no effect when a slot contains an item.
{% endhint %}

**Combined with the player-click listener:**

```yaml
# config.yml
listeners:
  player-click:
    enabled: true
    menu: 'inspect_player'
    require-sneaking: false
```

When a player right-clicks another player:
1. The system automatically sets `{meta:player}` to the clicked player's name
2. The `inspect_player` menu opens
3. The `[HEAD:{meta:player}]` references in the menu display the clicked player's equipment

**Notes:**

1. **In slot reference mode, the following properties have no effect:**
   - `name` (except for empty slots)
   - `amount`
   - `lore`
   - `item_model`

   {% hint style="warning" %}
   **About the `name` property:**
   - When the slot has an item, the item's own name is used; the `name` property has no effect
   - When the slot is empty, the HEAD slot shows a player skull (using the player's name), and other slots show a light grey glass pane (name fixed as "Empty"); the `name` property has no effect
   - To add a custom label, use the `description` property
   {% endhint %}

2. **Properties that are still supported:**
   - `width` — Icon width
   - `height` — Icon height
   - `show_overlays` — Overlays
   - `tooltip` — Hover tooltip
   - `description` — Description text below the item (custom labels can go here)

3. **Variable resolution order:**
   - Variables are resolved first (e.g., `{meta:player}`)
   - Then checked for slot reference format
   - Finally the item in the corresponding slot is fetched

4. **When the referenced player doesn't exist:**
   - If the player doesn't exist or is offline, the default material (PAPER) is shown
   - It is recommended to check whether the player is online before using slot references

**Advanced example — condition check + slot reference:**

```yaml
Body:
  # Only show equipment when the target player is online
  player_equipment:
    type: 'item'
    material:
      - condition: '{meta:player} != null'
        allow: '[HEAD:{meta:player}]'
        deny: 'BARRIER'
    name:
      - condition: '{meta:player} != null'
        allow: '&6{meta:player}''s Helmet'
        deny: '&cPlayer offline'
    width: 16
    height: 16
```

---

**Using a custom item model (1.21.7+):**

```yaml
Body:
  custom_model_item:
    type: 'item'
    material: 'DIAMOND_SWORD'
    name: '&b&lBlade of Light'
    lore:
      - '&7A divine weapon with a unique appearance'
      - '&aAttack: &f+50'
      - '&eRarity: &6Legendary'
    item_model: 'minecraft:custom_sword'  # Use a namespaced item model
    description: '&fThis is a special item using a custom model'
```

**All fields support condition checks:**

```yaml
Body:
  dynamic_item:
    type: 'item'
    material: 'DIAMOND'
    name:
      - condition: "%player_level% >= 10"
        allow: '&bDiamond (VIP Exclusive)'
        deny: '&8Diamond (Locked)'
    lore:
      - condition: "%player_level% >= 10"
        allow:
          - '&7Status: &aUnlocked'
          - '&7Available for purchase at level 10'
        deny:
          - '&7Status: &cLocked'
          - '&7Reach &elevel 10&7 to purchase'
```

**`description` supports clickable text:**

```yaml
Body:
  interactive_item:
    type: 'item'
    material: 'BOOK'
    name: '&a&lOperation Guide'
    description: '&7Click <text=&b[ Buy ];hover=&cBuy this item;command=buy item> or <text=&e[ Preview ];hover=&cView details;command=preview item>'

  multi_click_description:
    type: 'item'
    material: 'ENCHANTED_BOOK'
    name: '&6&lMagic Book'
    description: '&7Features: <text=&a[ Teleport ];hover=&cTeleport to spawn;command=spawn> <text=&b[ Shop ];hover=&cOpen shop;command=shop> <text=&e[ Help ];hover=&cView help;command=help>'
```

**`item_model` notes:**

The `item_model` property specifies the custom model the item should use, in the format `namespace:key`.

- **Applicable version**: 1.21.7 and above
- **Format**: `namespace:key`, e.g., `minecraft:custom_sword` or `your_plugin:special_item`
- **How it works**: Stores model data in the item's PersistentDataContainer; the Minecraft client renders the special appearance based on the model definition
- **Purpose**: Used to display items with unique appearances, such as special weapons and props

---

**`show_overlays` notes:**

The `show_overlays` property controls whether item overlays are shown; defaults to `true`.

- **Overlay elements include**:
  - **Durability bar**: Durability display for tools and weapons
  - **Stack count**: Stack quantity (e.g., 64, 32)
  - **Cooldown**: Item cooldown countdown

- **Use cases**:

  **Case 1: Clean item icon display**
  ```yaml
  Body:
    pure_icon:
      type: 'item'
      material: 'DIAMOND_SWORD'
      name: '&6&lLegendary Sword'
      show_overlays: false  # Hide durability bar; show a clean icon
      width: 16
      height: 16
  ```

  **Case 2: Full item information display**
  ```yaml
  Body:
    full_item:
      type: 'item'
      material: 'DIAMOND_PICKAXE'
      name: '&bDiamond Pickaxe'
      lore:
        - '&7Durability: &e1561/1561'
      show_overlays: true  # Show durability bar (default)
      width: 16
      height: 16
  ```

  **Case 3: Stacked item with count**
  ```yaml
  Body:
    stack_item:
      type: 'item'
      material: 'DIAMOND'
      name: '&bDiamond x64'
      show_overlays: true  # Show count 64
      width: 16
      height: 16
  ```

  **Case 4: Conditional overlay control**
  ```yaml
  Body:
    conditional_overlays:
      type: 'item'
      material: 'DIAMOND_SWORD'
      name: '&6Diamond Sword'
      show_overlays:
        - condition: '%player_is_op% == true'
          allow: false  # OP players: hide overlays
          deny: true    # Regular players: show overlays
      width: 16
      height: 16
  ```

- **Notes**:
  - `show_overlays` is still supported in slot reference mode
  - Setting it to `false` produces a cleaner item icon
  - Suitable for scenarios where a clean item appearance is desired
  - **Important**: When `show_overlays: false`, the stack count is hidden (overlays include durability bar, cooldown, and count)

---

---

## Conditional Component Hiding

To completely hide a Body component based on a condition, set the `type` field to a condition check and return `none` when the condition is not met:

```yaml
Body:
  admin_only_section:
    type:
      - condition: "%player_is_op% == true"
        allow: 'message'
        deny: 'none'          # Non-OP players will not see this component
    text: '&c[Admin Only] Please check the backend logs'
```
