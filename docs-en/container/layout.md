# Layout

`Layout` maps button IDs to physical inventory slots. Each row represents an inventory row; spaces are empty slots, a normal single character is a button ID, and a backtick-wrapped value is one logical slot even when its ID has multiple characters.

## Field Overview

| Syntax | Logical slots | Purpose |
|---|---:|---|
| `Layout` | Whole layout | A required list of strings; each entry is one inventory row |
| Space | 1 | Leaves the physical slot empty |
| Single character such as `A` or `#` | 1 | References `Buttons.A` or `Buttons.#` |
| Backticks such as `` `shop` `` | 1 | References the multi-character ID `Buttons.shop` |

`Layout` answers only where a button appears. Define what it displays and what a click does under `Buttons.<id>.display` and `Buttons.<id>.actions`.

```yaml
Type: CHEST
Layout:
  - '#########'
  - '####`shop`####'
  - '#########'
```

The middle row has 9 logical slots: four empty slots, one `shop` button, and four empty slots. `Buttons.shop` must exist; one button ID may occupy multiple slots and will render the same state in each slot.

Rules:

- A chest has 9 columns and 1-6 rows; hoppers have 5 slots; dispensers and droppers are 3 x 3; furnace-class inventories and anvils have 3 slots.
- Invalid row or slot counts prevent the menu from opening rather than rendering an incomplete inventory.
- Button IDs should use letters, numbers, `_`, `-`, and `/`; multi-character IDs must use backticks.
- Do not put a Dialog `Bottom` matrix into Container `Layout`; Container buttons are arranged by inventory slots.

See [Buttons](buttons.md) for button definitions and [Type](type.md) for inventory limits.

## Examples: Chest And Hopper

```yaml
# CHEST rows contain exactly 9 logical slots
Type: CHEST
Layout:
  - '# # # # #'
  - '    `shop`    '
  - 'A B C D E'
```

```yaml
# A HOPPER contains exactly 5 logical slots
Type: HOPPER
Layout:
  - 'ABCDE'
```

The parser counts logical slots, not the raw character length. A backtick-wrapped `` `shop` `` occupies one slot just like a single character. Invalid row or column counts prevent the menu from opening.
