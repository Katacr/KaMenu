# Item Bindings: item_bindings.yml

`plugins/KaMenu/item_bindings.yml` is the single source for main-hand item left/right clicks, F, and Q bindings. A missing file means no bindings; `/km reload config` and `/km reload all` reload it.

## Format

```yaml
item-bindings:
  recipe_book:
    enabled: true
    item:
      material: 'ce:farmersdelight:iron_knife'
    trigger:
      type: right-click       # left-click / right-click / f / q
      sneaking: true          # exact match; defaults to false
      target: air             # clicks only: air / block / both (default)
      cooldown-ms: 600        # defaults to 500ms; set 0 to disable
    actions:
      - 'command: fd recipe cutting_board'

  guide_book:
    enabled: true
    item:
      material: 'ce:hfrpg:hefeng_guide_book'
    trigger:
      type: right-click
      sneaking: false
      target: both
    actions:
      - 'open: 任务章节/引导书'

  protected_tool:
    enabled: true
    item:
      material: 'ce:example:tool'
    trigger:
      type: q
      sneaking: true
    actions:
      - 'open: example/tool_menu'
```

Each binding needs `enabled: true`, at least one item matcher in `item`, `trigger.type`, and a nonempty `actions` list. KaMenu selects the first matching binding in file order. `actions` may call global action packages (`actions: <packageId>`). Use `open: <menuId>` instead of the former top-level `menu` field.

## Fields

| Location | Field | Meaning | Default |
|---|---|---|---|
| Binding | `enabled` | Enable binding | `false` |
| Binding | `actions` | Actions to execute on match | required |
| `item` | `material` | Vanilla material or externally provided item ID | optional |
| `item` | `target-name` | Display-name substring | optional |
| `item` | `target-lore` | Substring of at least one lore line; empty or `[]` disables lore matching | optional |
| `item` | `data` | Legacy item damage/data value | optional |
| `item` | `custom-model-data` | Integer CustomModelData | optional |
| `item` | `ignore-case` | Ignore case in name/lore matching | `false` |
| `item` | `translate-colors` | Translate configured `&` color codes to Bukkit colors | `false` |
| `trigger` | `type` | `left-click`, `right-click`, `f`, or `q` | required |
| `trigger` | `sneaking` | Exact sneak state; `false` matches only non-sneaking | `false` |
| `trigger` | `target` | `air`, `block`, or `both` for clicks only; invalid for F/Q | `both` |
| `trigger` | `cooldown-ms` | Interval between item-binding activations per player, in milliseconds | `500` |

External IDs include `itemsadder:namespace:item`, `ia:namespace:item`, `oraxen:item_id`, `craftengine:namespace:item`, and `ce:namespace:item`. Wildcards are unsupported; missing providers never match. Name/lore fields use substring matching without stripping item colors. A player's last activation time is shared by all bindings; the current binding's cooldown determines whether it may fire.

F checks item bindings before the generic `config.yml > listeners.swap-hand` listener; a matching binding on cooldown does not fall back to the generic menu. Matching Q bindings cancel the drop and retain the item; unmatched Q drops normally. Q bindings do not run with a server-side container menu open. **The personal inventory screen is client-managed**, and Bukkit cannot reliably distinguish a drop from that screen from pressing Q in the world; such drops may still match.

## Automatic Migration

On upgrade, KaMenu migrates flat entries in the old `item_bindings.yml` and `config.yml > listeners.item-lore` into the unified file **before** updating the main config, then removes the old section. It backs up affected files first and is idempotent. Legacy `menu: name` becomes `actions: ['open: name']`. Old right-click bindings without a sneak requirement matched both states, so the migration creates one non-sneaking and one sneaking binding, preserving behavior and the old main-config priority. Explicit legacy cooldowns stay unchanged; absent ones use the new 500ms default. Unsafe migration errors stop startup and retain the original files for correction.

`/km migrate trmenu` now also writes this format. Legacy TrMenu `Bindings.Items` match both sneak states, so it emits two bindings and retains the migrator's 2000ms default cooldown.
