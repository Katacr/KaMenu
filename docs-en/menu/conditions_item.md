# 🍉 hasItem and hasStockItem Condition Methods

## hasItem - Regular Item Check

Checks if the player inventory contains items of a specified material, quantity (with optional lore and model).

### Format

```
hasItem.[mats=material;amount=quantity;lore=description;model=model]
```

### Parameters

| Parameter | Description | Required |
|-----------|-------------|----------|
| `mats` | Minecraft material name (supports multiple formats) | ✅ |
| `amount` | Required quantity | ✅ |
| `lore` | Text that must be contained in item description (case-insensitive) | ❌ |
| `model` | Item's item_model (format: namespace:key) | ❌ |

### Matching Rules

- Item material must match
- Total quantity of all matching items must be greater than or equal to the specified quantity
- If `lore` is specified, the item's description must contain that string (case-insensitive)
- If `model` is specified, the item's item_model must match (format: `namespace:key`)

### Material Name Format Support

KaMenu supports multiple material name formats, and the system automatically normalizes and matches the corresponding Material enum:

- Standard format: `DIAMOND_SWORD`
- Lowercase: `diamond_sword`
- Mixed case: `DiAMond swORd`
- Hyphens: `Diamond-Sword`
- Spaces: `diamond sword`

**Examples:**

```yaml
# All formats below match DIAMOND_SWORD
- condition: "hasItem.[mats=DIAMOND_SWORD;amount=1]"
- condition: "hasItem.[mats=diamond_sword;amount=1]"
- condition: "hasItem.[mats=Diamond-Sword;amount=1]"
- condition: "hasItem.[mats=diamond sword;amount=1]"
```

{% hint style="info" %}
The system automatically ignores case, replaces hyphens and spaces with underscores, and merges extra underscores, so all formats above will match correctly.
{% endhint %}

### Examples

**Check if player has 10 diamonds:**

```yaml
- condition: "hasItem.[mats=DIAMOND;amount=10]"
  allow:
    - 'tell: &aYou have enough diamonds!'
  deny:
    - 'tell: &cYou need 10 diamonds!'
```

**Check if player has 1 diamond with "Forging Material" in the description:**

```yaml
- condition: 'hasItem.[mats=DIAMOND;amount=1;lore=Forging Material]'
  allow:
    - 'tell: &aYou have Forging Material diamonds!'
  deny:
    - 'tell: &cYou''re missing Forging Material diamonds!'
```

**Check if player has 16 items with a custom model (e.g., Oraxen items):**

```yaml
- condition: 'hasItem.[mats=PAPER;amount=16;model=oraxen:mana_crystal]'
  allow:
    - 'tell: &aYou have enough Magic Crystals!'
  deny:
    - 'tell: &cYou need 16 Magic Crystals!'
```

**Combine with item action:**

```yaml
actions:
  - condition: "hasItem.[mats=DIAMOND;amount=16]"
    allow:
      - 'item: type=take;mats=DIAMOND;amount=16'
      - 'tell: &aPurchase successful! 16 diamonds deducted'
    deny:
      - 'tell: &cInsufficient items! 16 diamonds required'
```

**Reverse check (condition met when no items):**

```yaml
actions:
  - condition: "!hasItem.[mats=DIAMOND;amount=64]"
    allow:
      - 'tell: &aYou can continue mining diamonds!'
    deny:
      - 'tell: &cYour inventory is full!'
```

### Notes

- Material names are **case-insensitive**, supporting multiple formats (see above)
- `lore` check is an "includes" relationship, any matching string in description is sufficient
- `model` format is `namespace:key`, such as:
  - `minecraft:book` (vanilla item model)
  - `oraxen:mana_crystal` (Oraxen custom items)
  - `itemsadder:test_item` (ItemsAdder custom items)
- Supports reverse checks, e.g., `!hasItem.[...]` means condition is met when the item is absent
- Traverses all inventory slots (main inventory, armor slots, offhand, main hand)

---

## hasStockItem - Saved Item Check

Checks if the player inventory contains items with a specified name and quantity from the saved items.

### Format

```
hasStockItem.item_name;quantity
```

### Parameters

| Parameter | Description | Required |
|-----------|-------------|----------|
| Item name | Saved item name | ✅ |
| Quantity | Required quantity | ✅ |

### Matching Rules

- Item must have been saved using `/km item save` command
- Item comparison uses `ItemStack.isSimilar()` method, ignoring quantity differences
- Total quantity of all matching items must be greater than or equal to the specified quantity

### Examples

**Check if player has 16 Mystic Fruits:**

```yaml
- condition: "hasStockItem.MysticFruit;16"
  allow:
    - 'tell: &aYou have enough Mystic Fruits!'
  deny:
    - 'tell: &cYou need 16 Mystic Fruits!'
```

**Check if player has 1 Sacred Sword:**

```yaml
- condition: "hasStockItem.SacredSword;1"
  allow:
    - 'tell: &aYou can purchase this item!'
  deny:
    - 'tell: &cYou need a Sacred Sword to purchase!'
```

**Combine with stock-item action:**

```yaml
actions:
  - condition: "hasStockItem.MysticFruit;16"
    allow:
      - 'stock-item: type=take;name=MysticFruit;amount=16'
      - 'tell: &aPurchase successful!'
    deny:
      - 'tell: &cInsufficient items! 16 Mystic Fruits required'
```

**Reverse check (condition met when no items):**

```yaml
actions:
  - condition: "!hasStockItem.SacredSword;1"
    allow:
      - 'tell: &aYou don''t have this weapon yet, you can purchase it!'
    deny:
      - 'tell: &cYou already have a Sacred Sword, you can''t purchase again!'
```

**Using variables:**

```yaml
actions:
  # Read item name and quantity from data
  - condition: "hasStockItem.{data:purchase_item};{data:required_amount}"
    allow:
      - 'stock-item: type=take;name={data:purchase_item};amount={data:required_amount}'
      - 'tell: &aPurchase successful!'
    deny:
      - 'tell: &cInsufficient items!'
```

### Notes

- Item must be saved using `/km item save` before use
- Supports reverse checks, e.g., `!hasStockItem.item_name;quantity`
- Item comparison ignores differences except NBT tags
- Traverses all inventory slots (main inventory, armor slots, offhand, main hand)

---

## Complete Example: Shopping Menu

### Purchase Regular Items

This demonstrates creating a menu to purchase diamonds at 100 coins each.

```yaml
Title: '§6§lDiamond Shop'

Settings:
  need_placeholder:
    - 'math'
Body:
  diamond:
    type: 'item'
    text: '&a&lDiamond'
    material: DIAMOND
    lore:
      - '&7Price: 100 coins / each'
    description: 'Please use the slider below to select purchase quantity.'

Inputs:
  amount:
    type: 'slider'
    text: '&aPurchase Quantity'
    min: 1
    max: 64
    default: 1
    format: '%s: %seach'

Bottom:
  type: 'confirmation'
  confirm:
    text: '&a[ Confirm Purchase ]'
    actions:
      # Purchase diamonds (use hasMoney to check if player has enough currency)
      - condition: "isIntNum.$(amount)"
        deny:
          - 'tell: &cPlease enter a valid number.'
          - 'return'
      - condition: "hasMoney.%math_0_100*$(amount)%"
        allow:
          - 'money: type=take;num=%math_0_100*$(amount)%'
          - 'item: type=give;mats=DIAMOND;amount=$(amount)'
          - 'tell: &aPurchase successful! Spent %math_0_100*$(amount)% coins, received %$(amount) diamonds'
        deny:
          - 'tell: &cInsufficient currency! Need %math_0_100*$(amount)% coins'
          - 'sound: block.note_block.bass'
  deny:
    text: '&c[ Cancel Purchase ]'
    actions:
      - 'actionbar: &cPurchase cancelled'
      - 'sound: block.note_block.bass'
      - 'close'
```

### Purchase Stored Items

**Prerequisite:** To use this example menu, you should first save an item named `Magic Diamond Sword` using `/km item save Magic Diamond Sword`.

```yaml
Title: '§6§lMagic Diamond Sword Shop'

Settings:
  need_placeholder:
    - 'math'
Body:
  diamond_sword:
    type: 'item'
    text: '&a&lMagic Diamond Sword'
    material: DIAMOND_SWORD
    lore:
      - '&7Price: 10 diamonds'
    description: 'Please use the slider below to select redemption quantity.'

Inputs:
  amount:
    type: 'slider'
    text: '&aPurchase Quantity'
    min: 1
    max: 64
    default: 1
    format: '%s: %seach'

Bottom:
  type: 'confirmation'
  confirm:
    text: '&a[ Confirm Purchase ]'
    actions:
      # Purchase diamond sword (use hasItem to check regular items)
      - condition: "hasItem.[mats=DIAMOND;amount=%math_0_10*$(amount)%]"
        allow:
          - 'item: type=take;mats=DIAMOND;amount=%math_0_10*$(amount)%'
          - 'stock-item: type=give;name=Magic Diamond Sword;amount={data:purchase_amount}'
          - 'tell: &aPurchase successful! Spent %math_0_10*$(amount)% diamonds, received Magic Diamond Sword x$(amount)'
        deny:
          - 'tell: &cInsufficient items! Need diamonds x%math_0_10*$(amount)%'
          - 'sound: block.note_block.bass'
  deny:
    text: '&c[ Cancel Purchase ]'
    actions:
      - 'actionbar: &cPurchase cancelled'
      - 'sound: block.note_block.bass'
      - 'close'
```

---

## Differences Between the Two

| Feature | hasItem | hasStockItem |
|---------|---------|--------------|
| Scope | Regular items | Saved items |
| Parameter Format | `hasItem.[mats=...;amount=...]` | `hasStockItem.item_name;quantity` |
| Material Check | Uses Minecraft native material ID | Uses saved item's complete ItemStack |
| Item Matching | Matches material only, optionally lore and model | Uses `isSimilar()` for full match |
| Pre-save Required | No | Yes, via `/km item save` |
| Use Case | Check basic items (e.g., diamonds, iron ore) | Check custom items (e.g., enchanted gear, special items) |
