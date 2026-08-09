# Type

`Type` selects the Bukkit inventory type. When `Type` is omitted but `Layout` exists, the parser defaults to `CHEST`; writing it explicitly is recommended.

## Field Overview

| Value | Logical size | Uses `Properties` | Typical use |
|---|---:|---|---|
| `CHEST` | 9 columns, 1-6 rows | Usually no | Shops, backpacks, main menus |
| `HOPPER` | 5 slots | Usually no | Small shortcut menus |
| `DISPENSER` | 3 x 3 | Usually no | Nine-slot selection grids |
| `DROPPER` | 3 x 3 | Usually no | Nine-slot selection grids |
| `FURNACE` | 3 slots | `burn_progress`, `cook_progress` | Custom processing/progress UI |
| `BLAST_FURNACE` | 3 slots | `burn_progress`, `cook_progress` | Blast-furnace styled progress UI |
| `SMOKER` | 3 slots | `burn_progress`, `cook_progress` | Smoker-styled progress UI |
| `ANVIL` | 3 slots | `input` and anvil fields | Rename and text-confirmation UI |

Each `Layout` row must match the logical slot count for the selected type. Container menus use Bukkit's public inventory API rather than an NMS fake-window layer, so display and click support follow the public Bukkit API boundary.

## Choosing A Type

- Use `CHEST` for one to six rows of buttons.
- Use `HOPPER` for five compact shortcuts instead of reserving three chest rows.
- Use `DISPENSER` or `DROPPER` for a fixed 3 x 3 selector.
- Use a furnace type when the client must display flame and cook-arrow progress. These properties do not create a real furnace.
- Use `ANVIL` when the player must enter a name. KaMenu controls its input and result slots.

See [Properties](properties.md) for furnace and anvil fields. Static chest-like menus usually need only `Type`, `Layout`, and `Buttons`.

```yaml
# Five-slot shortcut menu
Type: HOPPER
Title: '&8Shortcuts'
Layout:
  - 'ABCDE'
```
