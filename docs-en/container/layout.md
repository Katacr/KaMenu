# Layout

`Layout` maps button IDs to physical inventory slots. Each row represents an inventory row; spaces are empty slots, a normal single character is a button ID, and a backtick-wrapped value is one logical slot even when its ID has multiple characters.

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
