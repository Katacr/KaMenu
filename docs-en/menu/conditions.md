# 🔍 Conditions

KaMenu provides a powerful universal condition system that can be used in **any text field** and **action list** in menus, dynamically displaying different content or executing different actions based on player state.

---

## Supported Locations

Conditions can be used in the following locations:

| Location | Description |
|----------|-------------|
| `Title` | Menu title |
| `Body.*.text` | Body component text |
| `Body.*.name` / `Body.*.lore` | Item component name and Lore |
| `Body.*.type` | Component type (for conditional hiding of components) |
| `Inputs.*.text` | Input component label text |
| `Bottom.*.text` | All button text |
| `Bottom.*.actions` | Button action list (conditional branching) |
| `Events.*` | Menu event action lists ([See event system](events.md)) |

---

## Text Field Conditions

### Syntax

```yaml
field_name:
  - condition: "condition_expression"
    allow: 'value when condition is met'
    deny: 'value when condition is not met'
```

### Examples

**Menu Title:**

```yaml
Title:
  - condition: "%player_is_op% == true"
    allow: '&8» &4&lAdmin Panel &8«'
    deny: '&8» &6&lPlayer Panel &8«'
```

**Input Component Label:**

```yaml
Inputs:
  amount:
    type: 'slider'
    text:
      - condition: "%player_level% >= 10"
        allow: '&6VIP Purchase Quantity (max 64)'
        deny: '&7Purchase Quantity (max 16)'
    min: 1
    max:
      - condition: "%player_level% >= 10"
        allow: '64'
        deny: '16'
```

**Button Text:**

```yaml
Bottom:
  type: 'confirmation'
  confirm:
    text:
      - condition: "%player_level% >= 10"
        allow: '&6[ VIP Confirm ]'
        deny: '&a[ Confirm ]'
```

---

## Action List Conditions

Nesting conditions in `actions` lists enables branching execution:

### Syntax

```yaml
actions:
  - condition: "condition_expression"
    allow:
      - 'action 1 when condition is met'
      - 'action 2 when condition is met'
    deny:
      - 'action 1 when condition is not met'
      - 'action 2 when condition is not met'
```

The `deny` field is optional; if not provided, nothing executes when the condition is not met.

### Mixed Usage

You can mix regular actions and conditional actions in the same `actions` list:

```yaml
actions:
  - 'sound: ui.button.click'           # Always executes
  - condition: "%player_balance% >= 100"
    allow:
      - 'console: eco take %player_name% 100'
      - 'tell: &aDeduction successful!'
    deny:
      - 'tell: &cInsufficient balance!'
  - 'close'                             # Always executes
```

---

## Condition Expression Syntax

### Comparison Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `==` | Equals | `%player_name% == Steve` |
| `!=` | Not equals | `%world_name% != world_nether` |
| `>` | Greater than | `%player_level% > 10` |
| `>=` | Greater than or equal | `%player_health% >= 10` |
| `<` | Less than | `%player_food_level% < 18` |
| `<=` | Less than or equal | `%player_exp% <= 100` |

**Note:**
- String comparisons (`==` and `!=`) are **case-insensitive** by default
- Strings that cannot be converted to numbers are treated as `0` in numeric comparisons

### Logical Operators

| Operator | Description | Priority |
|----------|-------------|----------|
| `&&` | Logical AND | High |
| `\|\|` | Logical OR | Low |
| `()` | Parentheses (change priority) | Highest |

**Short-circuit evaluation is supported:**
- `||`: If the first condition is `true`, subsequent conditions are not evaluated
- `&&`: If the first condition is `false`, subsequent conditions are not evaluated

### Expression Examples

```yaml
# Single condition
- condition: "%player_level% >= 10"

# AND condition
- condition: "%player_level% >= 10 && %player_level% < 20"

# OR condition
- condition: "%player_is_op% == true || %player_level% >= 20"

# Using parentheses
- condition: "(%player_level% >= 5 && %player_level% <= 10) || %player_is_op% == true"
```

---

## Built-in Condition Methods

KaMenu provides some built-in condition methods, called using the `.` symbol.

### Syntax

```
method.value    # Forward check
!method.value   # Reverse check
```

### Supported Methods

| Method | Description | Forward Example | Reverse Example |
|--------|-------------|----------------|-----------------|
| `isNum` | Check if value is a number (integer or decimal) | `isNum.$(amount)` | `!isNum.$(amount)` |
| `isPosNum` | Check if value is a positive number (>0) | `isPosNum.{data:price}` | `!isPosNum.{data:price}` |
| `isInt` | Check if value is an integer | `isInt.$(count)` | `!isInt.$(count)` |
| `isPosInt` | Check if value is a positive integer (>0) | `isPosInt.$(amount)` | `!isPosInt.$(amount)` |
| `hasPerm` | Check if player has permission | `hasPerm.kamenu.admin` | `!hasPerm.kamenu.admin` |
| `hasMoney` | Check if player has enough coins | `hasMoney.100` | `!hasMoney.100` |
| `hasItem` | Check if player inventory has specified material/quantity | `hasItem.[mats=DIAMOND;amount=10]` | `!hasItem.[mats=DIAMOND;amount=10]` |
| `hasStockItem` | Check if player inventory has stored item | `hasStockItem.MysticFruit;16` | `!hasStockItem.MysticFruit;16` |

**For detailed usage of item checks, see [ hasItem and hasStockItem Condition Methods](conditions_item.md).**

### Usage Examples

**Check if input value is an integer:**

```yaml
actions:
  - condition: "isInt.$(amount)"
    allow:
      - 'tell: &aInput value is an integer: $(amount)'
    deny:
      - 'tell: &cPlease enter a valid integer!'
```

**Check if value is a positive integer:**

```yaml
actions:
  - condition: "isPosInt.$(amount)"
    allow:
      - 'tell: &aValid positive integer: $(amount)'
    deny:
      - 'tell: &cPlease enter an integer greater than 0!'
```

**Check if player has enough coins:**

```yaml
actions:
  - condition: "hasMoney.100"
    allow:
      - 'console: eco take %player_name% 100'
      - 'tell: &aPurchase successful!'
    deny:
      - 'tell: &cInsufficient balance, 100 coins required'
```

**Permission check (forward):**

```yaml
Bottom:
  confirm:
    text: 'Admin Operation'
    actions:
      - condition: "hasPerm.kamenu.admin"
        allow:
          - 'open: admin_panel'
        deny:
          - 'tell: &cYou don''t have permission to perform this action!'
```

**Permission check (reverse - execute when no permission):**

```yaml
Bottom:
  confirm:
    text: 'Admin Operation'
    actions:
      - condition: "!hasPerm.kamenu.admin"
        allow:
          - 'tell: &cYou don''t have permission!'
```

**Check if player has 10 diamonds:**

```yaml
actions:
  - condition: "hasItem.[mats=DIAMOND;amount=10]"
    allow:
      - 'tell: &aYou have enough diamonds!'
    deny:
      - 'tell: &cYou need 10 diamonds!'
```

**Check if player has 16 Mystic Fruits:**

```yaml
actions:
  - condition: "hasStockItem.MysticFruit;16"
    allow:
      - 'tell: &aYou have enough Mystic Fruits!'
    deny:
      - 'tell: &cYou need 16 Mystic Fruits!'
```

---

## Variable Support

The following variable formats are supported in condition expressions:

| Variable Format | Description | Example |
|-----------------|-------------|---------|
| `%papi_var%` | PlaceholderAPI variable | `%player_level%` |
| `{data:key}` | Player personal data (persistent) | `{data:vip_level}` |
| `{gdata:key}` | Global shared data (persistent) | `{gdata:server_status}` |
| `{meta:key}` | Player metadata (memory cache) | `{meta:last_visit}` |
| `$(key)` | Dialog input variable (only supported in Actions and Inputs, **not in Body**) | `$(amount)` |

**Body Area Special Notes:**
- Text in the Body area is rendered before dialog and button inputs, so **does not support** `$(key)` input variables
- Body area supports `{data:key}`, `{gdata:key}`, and `{meta:key}` internal variables
- Inputs and Actions areas support all variable formats

**Metadata Notes:**
- Metadata is only stored in memory, not persisted to database
- Automatically cleaned when player disconnects
- All metadata is cleared on plugin reload or server shutdown
- Suitable for scenarios requiring short-term temporary data storage

---

## Complete Examples

### Example 1: VIP Level Check

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&aClaim Daily Reward'
    actions:
      - condition: "%player_level% >= 10"
        allow:
          - 'console: give %player_name% diamond 5'
          - 'tell: &a&lVIP Reward: &f5 diamonds'
          - 'title: title=&6Claim successful;subtitle=&fVIP exclusive reward;in=5;keep=40;out=10'
        deny:
          - 'console: give %player_name% dirt 1'
          - 'tell: &7Normal reward: 1 dirt'
          - 'tell: &eReach level 10 to claim VIP rewards!'
```

### Example 2: Admin Operation

```yaml
Bottom:
  type: 'multi'
  columns: 2
  buttons:
    admin_panel:
      text:
        - condition: "%player_is_op% == true"
          allow: '&4[ Admin Panel ]'
          deny: '&8[ Locked ]'
      actions:
        - condition: "%player_is_op% == true"
          allow:
            - 'open: admin/tools'
          deny:
            - 'tell: &cYou don''t have permission to access the admin panel!'
```

### Example 3: Balance Check

```yaml
actions:
  - condition: "hasMoney.1000"
    allow:
      - 'console: eco take %player_name% 1000'
      - 'tell: &aPurchase successful! Spent 1000 coins'
      - 'sound: entity.player.levelup'
    deny:
      - 'tell: &cInsufficient balance! 1000 coins required'
      - 'tell: &7Current balance: &f%player_balance%'
      - 'sound: block.note_block.bass'
```

### Example 4: Complex Multi-Condition

```yaml
actions:
  - condition: "(%player_level% >= 5 && %player_level% <= 10) || %player_is_op% == true"
    allow:
      - 'tell: &aCondition passed: You are a level 5-10 player, or you are an admin'
    deny:
      - 'tell: &cCondition not met'
```

### Example 5: Metadata State Check

```yaml
actions:
  - condition: "{meta:temp_status} != null"
    allow:
      - 'tell: &aTemporary status exists: {meta:temp_status}'
    deny:
      - 'tell: &7No temporary status set'
  - 'set-meta: last_action clicked'
```

### Example 6: Metadata Combined with Conditions

```yaml
actions:
  # Set temporary status
  - 'set-meta: temp_user true'

  - condition: "{meta:temp_user} == true"
    allow:
      - 'tell: &aMarked as temporary user'
      - 'open: temp_menu'
    deny:
      - 'tell: &cNot marked as temporary user'
```

### Example 7: Using Built-in Methods to Validate Input

```yaml
Inputs:
  amount:
    type: 'input'
    text: 'Please enter quantity (positive integer)'

actions:
  - condition: "isPosInt.$(amount)"
    allow:
      - 'tell: &aValid input: $(amount)'
      - 'set-meta: purchase_amount $(amount)'
    deny:
      - 'tell: &cPlease enter an integer greater than 0!'
      - 'close'
```

### Example 8: Reverse Permission Check (prompt when no permission)

```yaml
Bottom:
  confirm:
    text: 'Purchase VIP'
    actions:
      # Prompt when no permission (reverse check)
      - condition: "!hasPerm.vip.purchase"
        allow:
          - 'tell: &cYou need to purchase the vip.purchase permission to perform this action!'
        deny: []

      # Execute purchase logic when has permission
      - condition: "hasPerm.vip.purchase"
        allow:
          - 'console: eco give %player_name% 1000'
          - 'tell: &a1000 coins credited as VIP reward!'
```

---

## Notes

1. **PAPI Dependency**: Using `%papi_var%` format requires PlaceholderAPI plugin to be installed
2. **Numeric Conversion**: Strings that cannot be converted to numbers are treated as `0` in numeric comparisons (`>`, `<`, etc.)
3. **Case Insensitive**: `==` and `!=` operators are case-insensitive in string comparisons
4. **deny is Optional**: The `deny` field in action conditions is optional; for text field conditions, `deny` is recommended to avoid blank display
5. **Nested Parentheses**: Supports multiple levels of nested parentheses to build complex logical expressions
6. **Data Persistence**:
   - `{data:key}` and `{gdata:key}` are stored in database, persisted
   - `{meta:key}` is only stored in memory, automatically cleared after player disconnect or plugin reload
   - Use `{meta:key} != null` to check if metadata exists
7. **Built-in Method Format**:
   - Use `method.value` format, e.g., `isInt.$(amount)`
   - Supports reverse checks using `!` prefix, e.g., `!hasPerm.kamenu.admin`
   - Reverse checks are useful for scenarios requiring operations when conditions are not met

---

## Related Documents

- [🍉 hasItem and hasStockItem Condition Methods](conditions_item.md) - Learn detailed usage of item checks
- [⚙️ Events](events.md) - Learn detailed usage of the event system
- [🤖 Actions](actions.md) - Learn all available action types
- [💾 Data Storage](../data/storage.md) - Learn about data storage and variable usage

---

## Demo Menu

The plugin includes a demo menu for conditions, which can be opened with the following command:

```
/km open example/actions_demo
```
