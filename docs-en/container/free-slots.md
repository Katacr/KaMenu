# Free Slots

`Free-Slots` lets players place real owned items into selected Container slots and take them back. It is intended for material submission, custom crafting, item inspection, and live previews.

A free slot is not a button. Do not assign a `Buttons` action to the same slot. KaMenu validates and manually commits placement, pickup, merge, and swap transactions.

## Field Overview

| Key | Default | Purpose |
|---|---|---|
| `Free-Slots.<id>.slots` | Required | One or more 0-based top-inventory slots |
| `place.enabled` | `true` | Allows player placement |
| `place.condition` | Any item | Checks the incoming item before placement |
| `take.enabled` | `true` | Allows manual player pickup |
| `take.condition` | Any item | Checks the stored item before pickup |
| `events.place` | Empty | Runs once after successful placement |
| `events.take` | Empty | Runs once after successful pickup |
| `events.deny_place` | Empty | Runs once when placement is rejected |
| `events.deny_take` | Empty | Runs once when pickup is rejected |
| `return.on_close` | `true` | Tries an immediate inventory return on normal close |
| `return.overflow` | `pending` | Persists leftovers for later recovery; the only first-release value |

The first release supports `CHEST`, `HOPPER`, `DISPENSER`, and `DROPPER`. Furnace-family and anvil free slots are currently rejected.

## Basic Configuration

```yaml
Type: CHEST
Title: '&8Material Submission'

Layout:
  - '#########'
  - '####  C##'
  - '#########'

Free-Slots:
  diamond:
    slots: [13]
    place:
      enabled: true
      condition: '{free:incoming.material} == DIAMOND'
    take:
      enabled: true
    events:
      place:
        - 'actionbar: &aDiamond placed'
      take:
        - 'actionbar: &eDiamond returned'
      deny_place:
        - 'actionbar: &cOnly diamonds are accepted'
    return:
      on_close: true
      overflow: pending

Buttons:
  '#':
    display:
      material: GRAY_STAINED_GLASS_PANE
      name: ' '
  C:
    display:
      material: LIME_CONCRETE
      name: '&aConfirm'
```

Slot indexes start at `0`; slot `13` is the center of the second chest row. A free slot must be a space in `Layout`. Duplicate, out-of-range, or button-overlapping slots prevent loading.

## Interaction Rules

| Input | First-release behavior |
|---|---|
| Left click | Place/pick up a stack or swap different items |
| Right click | Place one or pick up half |
| Similar items | Merge up to the maximum stack size |
| Shift-click player inventory | Place into the first accepting group in declaration order |
| Shift-click free slot | Move into player storage when capacity permits |
| Drag | Commit only when every affected top slot is an allowed free slot |
| Number key, offhand, double-click, drop, creative clone | Rejected |

A rejected operation leaves every item at its source. Different-item swaps require both `take` and `place`; successful swaps run `events.take` before `events.place`, with final session state visible to both callbacks.

## Conditions And Item Properties

```text
{free:incoming.*}  item being placed
{free:stored.*}    item before the operation
{free:result.*}    item after the operation
{free:id}          logical free-slot ID
{free:slot}        physical slot index
```

Supported properties are `empty`, `material`, `amount`, `name`, `plain_name`, `lore`, `enchantments`, `enchantment_count`, `enchantment.<key>`, `custom_model_data`, `item_model`, `max_stack_size`, `provider`, and `id`.

```yaml
place:
  condition: >
    {free:incoming.material} == DIAMOND &&
    {free:incoming.custom_model_data} == 1001
```

Empty items return stable values: `AIR`, `0`, `true`, or an empty string as appropriate. `item_model` is empty on cores without that API.

Other buttons read the current session through `{free:<id>.<property>}`. All buttons refresh after a free-slot change, so the values can drive `variants` directly:

```yaml
Buttons:
  C:
    variants:
      - priority: 0
        condition: '{free:diamond.amount} >= 2'
        display:
          material: LIME_CONCRETE
          name: '&aCraft'
        actions:
          left:
            - 'free-slot: type=consume;id=diamond;amount=2'
            - 'item: type=give;mats=EMERALD;amount=1'
      - priority: 1
        display:
          material: RED_CONCRETE
          name: '&cMore diamonds required'
```

## Full Item Preview

Scalar variables cannot rebuild potions, books, banners, PDC, or modern components. `[FREE:<id>]` clones the actual `ItemStack`:

```yaml
Buttons:
  preview:
    display:
      material: '[FREE:diamond]'
      name: '&eSubmitted Item'
```

Unspecified metadata is preserved; explicit display fields override the clone. An empty free slot renders no preview item.

## Multi-Slot Groups

```yaml
Free-Slots:
  ingredients:
    slots: [10, 11, 12]
```

- Shift placement merges first, then fills empty slots in declaration order.
- `{free:ingredients.amount}` is the total across all physical slots.
- `material` is the common material or `MIXED` when non-empty slots differ.
- Consumption follows declaration order; returns preserve each actual `ItemStack` and its metadata.

## free-slot Actions

`free-slot` applies only to the player's current Container session and has no target-selector support.

```yaml
- 'free-slot: type=consume;id=input;amount=1'
- 'free-slot: type=consume;items=diamond:1,emerald:2'
- 'free-slot: type=return;id=input'
- 'free-slot: type=return;id=*'
- 'free-slot: type=refresh;id=input'
```

Multi-material consumption is atomic. KaMenu validates every ID and amount before changing inventory; any failure consumes nothing and stops the current action chain. Put reward actions after consumption.

An active return also requires enough capacity for every selected item. If storage is insufficient, nothing moves and later actions stop.

## Return And Recovery

Free-slot items are player assets, so KaMenu keeps persistent escrow for every physical slot:

- Normal close, menu replacement, reload, and quit return items or retain a pending record.
- Full inventories use `RETURN_PENDING`; recovery is retried on the next login instead of dropping items.
- `return.on_close: false` disables only the immediate close return. It does not consume the item; the item becomes pending.
- Plugin shutdown retains escrow for login recovery, avoiding Folia player-scheduler/database-close races.

The Bukkit player file and SQLite/MySQL cannot participate in one cross-system ACID transaction. Recovery prioritizes preventing loss. A power failure, `kill -9`, or crash inside the commit window can still cause an extremely rare duplicate recovery; absolute zero-loss and zero-duplication cannot both be guaranteed.

## Custom Crafting Example

```yaml
Type: CHEST
Title: '&8Custom Crafting'
Layout:
  - '#########'
  - '##   C###'
  - '#########'
Free-Slots:
  diamond:
    slots: [11]
    place:
      condition: '{free:incoming.material} == DIAMOND'
  emerald:
    slots: [12]
    place:
      condition: '{free:incoming.material} == EMERALD'
Buttons:
  '#':
    display:
      material: GRAY_STAINED_GLASS_PANE
      name: ' '
  C:
    variants:
      - priority: 0
        condition: '{free:diamond.amount} >= 1 && {free:emerald.amount} >= 1'
        display:
          material: LIME_CONCRETE
          name: '&aCraft'
        actions:
          left:
            - 'free-slot: type=consume;items=diamond:1,emerald:1'
            - 'item: type=give;mats=NETHER_STAR;amount=1'
      - priority: 1
        display:
          material: RED_CONCRETE
          name: '&cMissing materials'
```
