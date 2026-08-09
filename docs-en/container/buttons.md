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

### display Fields

| Field | Common value | Purpose |
|---|---|---|
| `material` | `DIAMOND`, `stock:Sword`, `itemsadder:pack:item` | Vanilla, saved, or external item source; required |
| `amount` | `1`, `{meta:amount}` | Display stack size |
| `name` | Color codes, PAPI, built-in variables | Item name |
| `lore` | String list | Item lore |
| `item_model` | `namespace:model` | Modern Item Model key |
| `custom_model_data` | Integer or variable | Custom model data |
| `skull_owner` | Player name | Player-head owner |
| `skull_texture` | Base64 or texture value | Player-head texture |
| `enchantments` | `sharpness: 5` | Enchantments and levels |
| `item_flags` | `HIDE_ATTRIBUTES` | Hide item attributes |
| `glow` | `true` / `false` | Enchantment glint |
| `unbreakable` | `true` / `false` | Unbreakable state |

External items require the provider plugin to be enabled and the ID to exist. Unknown fields produce a warning; do not depend on private NBT.

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
