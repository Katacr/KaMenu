# Container Structure

## Top-Level Keys

| Key | Required | Purpose | Reference |
|---|---|---|---|
| `Type` | No | Selects the inventory type; defaults to `CHEST` when `Layout` is present | [Type](type.md) |
| `Title` | No | Sets the inventory title; defaults to `KaMenu` | [Title](title.md) |
| `Settings` | No | Dependency checks, click throttling, and argument rules | [Settings](settings.md) |
| `References` | No | Shared text and templates for the current menu | [Menu References](../config/references.md) |
| `Layout` | Yes | Maps buttons to physical slots | [Layout](layout.md) |
| `Free-Slots` | No | Named slots that accept or return real player-owned items | [Free Slots](free-slots.md) |
| `Buttons` | No | Defines buttons referenced by Layout; may be omitted for an entirely empty layout | [Buttons](buttons.md) |
| `Properties` | No | Furnace progress, anvil input, and other type-specific values | [Properties](properties.md) |
| `Update` | No | Refreshes the entire container on an interval | [Refresh](refresh.md) |
| `Title-Update` | No | Refreshes only the title | [Refresh](refresh.md) |
| `Progress-Update` | No | Refreshes furnace progress and evaluates progress events | [Refresh](refresh.md) |
| `Events` | No | Open, close, reusable click groups, tasks, and progress events | [Events](events.md) |

## Configuration Hierarchy

```yaml
Type: CHEST                    # Inventory type
Title: '&8Menu title'          # Top inventory title
Settings:                       # Prerequisites, throttle, and arguments
References:                     # Optional shared text and templates
Layout:                         # Slot layout; always required
Free-Slots:                     # Optional real-item interaction slots
Update: 20                      # Optional full refresh interval in ticks
Title-Update: 40                # Optional title refresh interval in ticks
Progress-Update: 5              # Optional furnace progress interval in ticks
Properties:                     # Optional furnace/anvil fields
Events:                         # Optional lifecycle and action groups
Buttons:                        # Optional buttons referenced by Layout
```

Recommended authoring order:

1. Select the inventory and slot dimensions with `Type`.
2. Place each button in a physical slot with `Layout`.
3. Define `Buttons.<id>.display.material` for every ID referenced by the layout.
4. For player-owned inputs, leave the Layout slot empty and bind it under `Free-Slots`.
5. Add click interaction under `Buttons.<id>.actions`.
6. Add `Settings`, `Properties`, refresh fields, and `Events` only when needed.

## Complete Skeleton

```yaml
Type: CHEST
Title: '&8Container Menu'

Settings:
  need_placeholder:
    - player
  min_click_delay: 200

References:
  product_name: '&bProduct'

Update: 20
Title-Update: 40

Layout:
  - '#########'
  - '####`shop`####'
  - '#########'

Events:
  Open:
    - 'actionbar: &aMenu opened'

Buttons:
  '#':
    display:
      material: GRAY_STAINED_GLASS_PANE
      name: ' '
  shop:
    display:
      material: DIAMOND
      name: '{ref:product_name}'
    actions:
      left:
        - 'close'
```

Use `{ref:path}` to read shared values from `References`. Container buttons may also use `{self:id}` and `{self:<field-path>}` to read their own node. See [Menu References](../config/references.md) for the complete rules.

`Body`, `Inputs`, and `Bottom` are Dialog sections and cannot be added to a Container file. Common events, actions, and conditions are documented under [Events](events.md), [Actions](../modern-dialog/actions.md), and [Conditions](../modern-dialog/conditions.md).

`Layout` is the only top-level key that every Container menu must define. All other keys are optional. A useful menu normally includes a valid layout and definitions for every button ID referenced by that layout.
