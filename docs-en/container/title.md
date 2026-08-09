# Title

`Title` defines the inventory title shown at the top of a Container menu.

## Field Overview

| Item | Supported value | Description |
|---|---|---|
| Key | `Title` | The title at the top of the Container |
| Type | String, string list, or conditional result | A string list is used as ordered title frames |
| Default | `KaMenu` | Used when omitted |
| Variables | PAPI, `{meta:*}`, `{data:*}`, `{js:...}`, and others | Resolved for the current player on open and refresh |
| Frame refresh | `Title-Update` | Advances one item per interval and loops after the last item |
| Current-frame refresh | `Update`, `refresh: title`, or `refresh: *` | Re-resolves the current item without advancing the frame index |

**Type:** `String`, `List<String>`, or conditional result

**Default:** `KaMenu`

It supports color codes, PlaceholderAPI, KaMenu variables, `meta`, `data`, and JavaScript output. The title is resolved when the menu opens. A string list starts at its first item; every `Title-Update` interval advances one item and loops.

```yaml
Title: '&8Player Shop - %player_name%'
```

Use a string list for rotation or a simple title animation:

```yaml
Title:
  - '&8Loading.'
  - '&8Loading..'
  - '&8Loading...'
Title-Update: 10
```

This example shows the first item on open, advances every 10 ticks, and returns to the first item after the third. Every frame may contain PAPI and KaMenu variables.

`Update`, `refresh: title`, and `refresh: *` re-resolve the current frame without selecting the next one. Only the periodic `Title-Update` advances the frame index.

Conditional titles continue to use conditional results:

```yaml
Title:
  - condition: 'hasPerm.shop.admin'
    allow: '&4Admin Shop'
    deny: '&6Regular Shop'
```

A list made entirely of conditional Maps is interpreted as a conditional candidate list. KaMenu selects the first meaningful result instead of rotating it as title frames. When string frames and conditional Maps are mixed, they are expanded in YAML order, and one or several strings returned by a branch become title frames at that position.

Container titles do not use Dialog-only fields such as `width` or `external_title`. See [Refresh](refresh.md) for title refresh rules and [Conditions](../modern-dialog/conditions.md) for common condition syntax.

## Example

```yaml
# Select by permission and evaluate again every 40 ticks
Title:
  - condition: 'hasPerm.shop.admin'
    allow: '&4Admin Shop - %player_name%'
    deny: '&6Player Shop - %player_name%'
Title-Update: 40
```

The title controls only the text at the top. Use button `variants` when balances, inventory state, or permissions must change a button item and its actions.
