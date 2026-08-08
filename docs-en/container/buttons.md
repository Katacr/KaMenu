# Buttons

`Buttons` defines the buttons referenced by `Layout`. A legacy button needs at least `display.material`; display values resolve PlaceholderAPI, KaMenu variables, and conditions when the menu opens or refreshes.

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

The DeluxeMenus migrator generates this structure for multiple candidates sharing one `slot` while preserving DM priority and source order. See [DeluxeMenus migration](../perm/commands.md#km-migrate-dm).
