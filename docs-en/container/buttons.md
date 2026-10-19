# Buttons

`Buttons` defines the buttons referenced by `Layout`. A legacy button needs at least `display.material`; display values resolve PlaceholderAPI, KaMenu variables, and conditions when the menu opens or refreshes.

## Field Overview

| Key | Location | Purpose |
|---|---|---|
| `view_condition` | `Buttons.<id>` | Controls whether the button is visible; it is not a click guard |
| `update` | `Buttons.<id>` | Refreshes this button on a tick interval |
| `display` | `Buttons.<id>.display` | Defines the ItemStack shown in the slot |
| `actions` | `Buttons.<id>.actions` | Executes actions for each click type |
| `variants` | `Buttons.<id>.variants` | Defines complete states for one slot; cannot be mixed with top-level `display/actions` |

Use `display + actions` for a standard button. Use `variants` when permissions, cooldowns, inventory state, or other conditions must produce several complete versions of the same slot.

## Standard Button

```yaml
Buttons:
  shop:
    view_condition: 'hasPerm.shop.use'
    display:
      material: DIAMOND
      amount: 1
      name: '&bProduct'
      lore:
        - '&7Balance: %vault_eco_balance%'
      item_model: 'example:shop'
      custom_model_data: '{meta:model_id}'
      glow: true
      item_flags:
        - HIDE_ATTRIBUTES
    actions:
      all:
        - 'actionbar: &7Product clicked'
      left:
        - 'refresh: shop'
      right:
        - 'close'
```

Display fields support Bukkit-mappable material, amount, name, lore, model, skull, enchantment, flag, glow, and unbreakable properties. See [Body item display](../modern-dialog/body.md) for the shared ItemStack field reference.

## Dynamic Repeat Lists

`type: repeat` uses every `Layout` slot that references the same button ID as a fixed list matrix. Slots bind to items from left to right, then top to bottom. The number of matching slots in the current layout determines the page capacity; repeat never reallocates another button's slots.

```yaml
JavaScript:
  products: |
    JSON.stringify([
      { id: "sword", name: "Iron Sword", material: "IRON_SWORD", price: 100 },
      { id: "pickaxe", name: "Iron Pickaxe", material: "IRON_PICKAXE", price: 150 }
    ]);

Layout:
  - 'AAAABBCCC'
  - 'AAAABBCCC'
  - 'AAAADDEEE'

Buttons:
  A:
    type: repeat
    source: '[products]'
    item:
      display:
        material: '{item.material}'
        name: '&f{item.name}'
        lore:
          - '&7Price: {item.price}'
      actions:
        left:
          - 'tell: Selected {item.id}, price {item.price}'
    empty:
      display:
        material: BARRIER
        name: '&7No products'
  B:
    view_condition: '{page:A} < {pages:A}'
    display:
      material: ARROW
      name: '&eNext page'
    actions:
      left:
        - 'repeat-page: A next'
  C:
    view_condition: '{page:A} > 1'
    display:
      material: ARROW
      name: '&ePrevious page'
    actions:
      left:
        - 'repeat-page: A prev'
```

- `Buttons.<id>.type: repeat` requires `source` and `item.display.material`; `item.actions` is optional. Do not combine a repeat button with top-level `display`, `actions`, `variants`, or dynamic `slot`.
- `source` supports JavaScript functions (`[functionName]`), KaMenu variables, PAPI, JSON arrays, and plain string lists. Use `split` for delimited strings; `trim` defaults to `true`. JavaScript can prepare a filtered and ordered matrix list.
- Item object fields are available as `{item.xxx}`. String/number entries provide `{item.value}` and `{item.text}`; `{item.index}` is 0-based and `{item.number}` is 1-based. `{item.page_index}` / `{item.page_number}` provide the 0-/1-based position within the current page.
- Pagination variables are available in normal button and repeat-item display, conditions, and actions: `{page:A}`, `{pages:A}`, `{total:A}`, `{start:A}`, and `{end:A}`. Page numbers start at 1; `start`/`end` are a 0-based half-open range in the complete data list. For an empty list: `page=1`, `pages=1`, `total=0`, `start=0`, and `end=0`.
- Use `repeat-page: A next|prev|+N|-N|pageNumber` to navigate a list. Absolute page numbers start at 1. The action immediately refreshes the list and buttons that read its pagination variables; no additional `refresh` action is needed. It is independent of Container multi-layout `page:` navigation.
- An empty list defaults to air in every slot assigned to that ID, with no click binding. Configure `empty.display` for a display-only placeholder; it also has no click action. Unfilled slots on the last page of a non-empty list always remain air.
- Repeat does not filter items with per-item conditions and imposes no plugin-side source size limit. Prepare the desired data and order in JavaScript.

### display Fields

| Field | Common value | Purpose |
|---|---|---|
| `material` | `DIAMOND`, `stock:Sword`, `itemsadder:pack:item`, `sxitem:item_id` | Vanilla, saved, or external item source; required |
| `amount` | `1`, `{meta:amount}` | Display stack size |
| `name` | Color codes, PAPI, built-in variables | Item name |
| `lore` | String list | Item lore |
| `item_model` | `namespace:model` | Modern Item Model key |
| `tooltip_style` | `namespace:style` | Custom tooltip background/frame style (1.21.4+) |
| `custom_model_data` | Integer or variable | Custom model data |
| `skull_owner` | Player name | Player-head owner |
| `skull_texture` | Base64 or texture value | Player-head texture |
| `enchantments` | `sharpness: 5` | Enchantments and levels |
| `item_flags` | `HIDE_ATTRIBUTES` | Hide item attributes |
| `glow` | `true` / `false` | Enchantment glint: `true` forces it, `false` cancels it; the plugin picks the implementation by core version (1.20.5+ uses the vanilla glint component, older cores fall back to enchant simulation) |
| `unbreakable` | `true` / `false` | Unbreakable state |
| `hide_tooltip` | `true` / `false` | Hide the entire tooltip (1.20.5+) |
| `hide_components` | `true` / `false` | Hide only vanilla auto-generated component lines, keeping custom name and lore |
| `color` | `255,0,0`, `#FF0000` | Dye color: leather armor, potion, firework effect |
| `damage` | Integer | Damage taken (damageable items only) |

### Item Flags and Tooltips

`item_flags` uses Bukkit `ItemFlag` enum names. Names are case-insensitive; uppercase strings are recommended. Flags hide matching tooltip lines only—they do not remove enchantments, attributes, durability, or other item data:

| Flag | Tooltip content hidden |
|---|---|
| `HIDE_ENCHANTS` | Enchantment information |
| `HIDE_ATTRIBUTES` | Attribute modifiers |
| `HIDE_UNBREAKABLE` | Unbreakable information |
| `HIDE_DESTROYS` | Blocks the item can destroy |
| `HIDE_PLACED_ON` | Blocks the item can be placed on |
| `HIDE_POTION_EFFECTS` | Potion-effect information (newer versions may group this under additional tooltip text) |
| `HIDE_DYE` | Dye information |
| `HIDE_ARMOR_TRIM` | Armor trim information |
| `HIDE_ADDITIONAL_TOOLTIP` | Item-specific extra information, such as some food, potion, or special-item text |

The Bukkit/Paper API on the running server determines which enum values exist. A flag unavailable on an older server is skipped and logged as an `item_flags` warning. Multiple flags can be used in one list.

```yaml
display:
  material: DIAMOND_SWORD
  name: '&bPreview weapon'
  lore:
    - '&7Item data is still preserved'
  enchantments:
    sharpness: 5
  item_flags:
    - HIDE_ENCHANTS
    - HIDE_ATTRIBUTES
    - HIDE_UNBREAKABLE
```

These three fields have different effects:

- `item_flags`: selectively hides vanilla tooltip lines. Custom name, Lore, and other unhidden lines remain visible.
- `hide_components: true`: hides vanilla auto-generated component lines while preserving the custom name, Lore, item model, custom model data, and tooltip style. Paper uses `tooltip_display.hidden_components`; non-Paper cores such as Spigot fall back to these common flags: `HIDE_ATTRIBUTES`, `HIDE_UNBREAKABLE`, `HIDE_ENCHANTS`, `HIDE_PLACED_ON`, `HIDE_DESTROYS`, `HIDE_DYE`, `HIDE_ARMOR_TRIM`, and `HIDE_ADDITIONAL_TOOLTIP`. The non-Paper behavior is therefore approximate and may not cover every data component.
- `hide_tooltip: true`: hides the entire item tooltip, including the hover display of the name and Lore. It takes precedence over `hide_components`; when both are `true`, `hide_tooltip` wins.

`hide_tooltip` and `hide_components` require a 1.20.5+ core. If the core cannot apply them, KaMenu logs a warning and skips the property. They affect display only and do not clear item data. Dialog `Body.item`'s `show_tooltip` is a Dialog-component-level switch and differs from `hide_tooltip`, which is written to the ItemStack; see [Body Item Display](../modern-dialog/body.md#item--item-display).

```yaml
Buttons:
  hidden_stats:
    display:
      material: DIAMOND_CHESTPLATE
      name: '&bEquipment preview'
      lore:
        - '&7Custom Lore remains visible'
      item_flags:
        - HIDE_ATTRIBUTES
        - HIDE_UNBREAKABLE
      hide_components: true
```

`tooltip_style` sets a background/frame style supported by 1.21.4+ clients, using the `namespace:key` format. The client resource pack must provide textures corresponding to `assets/<namespace>/textures/gui/sprites/tooltip/<key>_background` and `<key>_frame`. Without those assets, the client cannot show the custom style. Do not combine it with a hidden tooltip: there is no visible frame to style.

`glow` is tri-state: omitted means do not change the glint; `true` forces an enchantment glint; `false` cancels it. On 1.20.5+ the vanilla glint component is preferred. On older cores, `true` is simulated with a hidden enchantment; `false` cannot cancel an existing glint and logs a warning.

External items require the provider plugin to be enabled and the ID to exist. Unknown fields produce a warning; do not depend on private NBT.

Supported external-item prefixes: `itemsadder:` (alias `ia:`), `oraxen:`, `craftengine:` (alias `ce:`), `sxitem:` (alias `sx:`, the SX-Item plugin). SX-Item items display their own body (display name, lore, and own model); hover text follows the item itself. Its inline `&item:[sxitem:item_id]` icon also tries to resolve the sprite from the item's own `item_model`.

To preview a complete real item from a free slot, set `material` to `[FREE:<id>]`. It preserves the actual ItemStack metadata; see [Free Slots](free-slots.md#full-item-preview).

### Conditional Lore Lines

`display.lore` may mix static strings and conditional maps in order. A selected branch may return one or several lines, which are inserted at the condition's position:

```yaml
Buttons:
  status:
    display:
      material: BOOK
      name: '&eStatus'
      lore:
        - '&7Fixed lore 1'
        - condition: 'hasPerm.shop.vip'
          allow:
            - '&aVIP status'
            - '&7Exclusive discount enabled'
          deny: '&7Regular player status'
        - '&7Fixed lore 2'
    actions:
      left:
        - 'refresh: status'
```

If every `lore` entry is a conditional map, KaMenu selects the first candidate that returns non-empty content. If any plain string is present, entries are expanded in YAML order. PAPI and KaMenu variables are resolved normally after branch selection.

Use the inline form when only one Lore line needs a condition:

```yaml
lore:
  - '&7Public description'
  - '&aVIP-only description {condition: hasPerm.shop.vip}'
```

The line is omitted when the condition fails. Inline conditions must use `{condition: expression}` and appear at the end of the line.

`view_condition` controls visibility rather than clicks. Put click-time checks inside the relevant action list. Supported click keys are `all`, `left`, `right`, `shift_left`, `shift_right`, `middle`, `drop`, `control_drop`, `double_click`, `offhand`, `number_key`, and `number_key_1` through `number_key_9`. See [Actions](../modern-dialog/actions.md) for common action syntax.

## variants

Use `variants` when one physical slot needs several complete states:

```yaml
Buttons:
  daily:
    variants:
      - priority: 0
        condition: '!hasPerm.shop.daily_cooldown'
        display:
          material: DIAMOND
          name: '&aClaim daily diamond'
        actions:
          left:
            - 'console: give %player_name% DIAMOND 1'
            - 'refresh: *'
      - priority: 1
        display:
          material: STONE
          name: '&cAlready claimed'
        actions:
          left:
            - 'tell: &cCome back tomorrow'
```

- Lower `priority` values are selected first; equal priorities preserve YAML order.
- When no variant declares `priority`, selection proceeds strictly from top to bottom.
- A variant owns a complete `display` and must specify `display.material`; actions belong to that variant.
- A missing `condition` is always true and is normally the final fallback.
- Rendering and clicking resolve the variant again, preventing stale actions after permission or cooldown changes.
- Do not combine `variants` with top-level `display` or `actions` on the same button.

The DeluxeMenus migrator generates this structure for multiple candidates sharing one `slot` while preserving DM priority and source order. See [Menu Migration Overview](migration.md#deluxemenus-migration-tutorial).

## Example: Purchase Button With State Selection

```yaml
Buttons:
  buy:
    update: 20
    variants:
      - priority: 0
        condition: '{checkitem:[hand;amt]} >= 1 && %vault_eco_balance% >= 100'
        display:
          material: DIAMOND
          name: '&aBuy diamond'
          lore:
            - '&7Price: 100 coins'
        actions:
          left:
            - 'money: type=take;num=100'
            - 'item: type=give;mats=DIAMOND;amount=1'
            - 'refresh'
      - priority: 1
        display:
          material: GRAY_STAINED_GLASS_PANE
          name: '&cRequirements not met'
        actions:
          left:
            - 'actionbar: &cYou do not have enough items or coins'
```

Use `view_condition` to remove the button entirely. Use `variants.condition` to keep the slot while changing its item and actions. KaMenu selects the current variant again at click time, so the opening display is not treated as permanent authorization.
