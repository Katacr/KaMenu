# Container Structure

| Key | Required | Purpose | Reference |
|---|---|---|---|
| `Type` | No | Selects the inventory type; defaults to `CHEST` when `Layout` is present | [Type](type.md) |
| `Title` | No | Sets the inventory title; defaults to `KaMenu` | [Title](title.md) |
| `Settings` | No | Dependency checks, click throttling, and argument rules | [Settings](settings.md) |
| `Layout` | Yes | Maps buttons to physical slots | [Layout](layout.md) |
| `Buttons` | No | Defines buttons referenced by Layout; may be omitted for an entirely empty layout | [Buttons](buttons.md) |
| `Properties` | No | Furnace progress, anvil input, and other type-specific values | [Properties](properties.md) |
| `Update` | No | Refreshes the entire container on an interval | [Refresh](refresh.md) |
| `Title-Update` | No | Refreshes only the title | [Refresh](refresh.md) |
| `Progress-Update` | No | Refreshes furnace progress and evaluates progress events | [Refresh](refresh.md) |
| `Events` | No | Open, close, reusable click groups, tasks, and progress events | [Events](events.md) |

```yaml
Type: CHEST
Title: '&8Container Menu'

Settings:
  need_placeholder:
    - player
  min_click_delay: 200

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
      name: '&bProduct'
    actions:
      left:
        - 'close'
```

`Body`, `Inputs`, and `Bottom` are Dialog sections and cannot be added to a Container file. Common events, actions, and conditions are documented under [Events](events.md), [Actions](../modern-dialog/actions.md), and [Conditions](../modern-dialog/conditions.md).
