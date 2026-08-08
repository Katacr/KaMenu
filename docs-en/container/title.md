# Title

`Title` defines the inventory title shown at the top of a Container menu.

**Type:** `String` or conditional result

**Default:** `KaMenu`

It supports color codes, PlaceholderAPI, KaMenu variables, `meta`, `data`, and JavaScript output. The title is resolved when the menu opens and is resolved again when `Title-Update` is configured.

```yaml
Title: '&8Player Shop - %player_name%'
```

```yaml
Title:
  - condition: 'hasPerm.shop.admin'
    allow: '&4Admin Shop'
    deny: '&6Regular Shop'
```

Container titles do not use Dialog-only fields such as `width` or `external_title`. See [Refresh](refresh.md) for title refresh rules and [Conditions](../modern-dialog/conditions.md) for common condition syntax.
