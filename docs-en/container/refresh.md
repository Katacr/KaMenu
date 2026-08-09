# Refresh

Container refresh intervals use ticks. Values below 5 generate a warning; prefer targeted refreshes instead of rebuilding the whole inventory every tick.

## Field Overview

| Key | Refresh scope | Unit | Typical use |
|---|---|---|---|
| `Update` | Title, all buttons, and container properties | Ticks | Whole-page state synchronization |
| `Title-Update` | Title and one title-list frame | Ticks | Rotation, balance, time, or state in the title |
| `Progress-Update` | Furnace properties and `Events.Progress` | Ticks | Flame/arrow progress |
| `Buttons.<id>.update` | One button's slots | Ticks | One dynamic button |
| `refresh` | Every button in the active session | Action | Immediate button update after a click |
| `refresh: *` | Title, properties, and every button | Action | Full update after arguments or global state changes |

```yaml
Update: 20
Title-Update: 40
Progress-Update: 5

Buttons:
  status:
    update: 20
    display:
      material: CLOCK
```

Action refresh targets are:

```yaml
- 'refresh'
- 'refresh: *'
- 'refresh: title'
- 'refresh: properties'
- 'refresh: status'
```

- `refresh` or an empty `refresh:` target refreshes every button icon without refreshing the title or container properties.
- `refresh: *` refreshes the title, container properties, and every button.
- `refresh: title` and `refresh: properties` refresh only that part.
- `refresh: <buttonId>` refreshes only the selected button.

When `Title` is a string list, only the periodic `Title-Update` advances to the next item and loops. `Update`, `refresh: title`, and `refresh: *` only re-resolve the current item.

Refreshing re-resolves PlaceholderAPI, built-in variables, conditions, `view_condition`, and `variants`. Database-backed `data`, `gdata`, `list`, and `glist` operations may be asynchronous; prefer `meta` for high-frequency state and wait a few ticks after persistent writes when the new value must be observed.

## Furnace Progress Events

```yaml
Events:
  Progress:
    cook_complete:
      source: cook_progress
      condition: '{progress.current} >= 100'
      trigger_initial: false
      actions:
        - 'actionbar: &aCooking complete'
```

A progress event runs when its condition changes from false to true. `trigger_initial: true` also permits a match on the first evaluation. See [Events](events.md) for the shared event syntax.

## Choosing A Refresh Method

- Use `Buttons.<id>.update` or `refresh: <id>` when only one balance or inventory button changes.
- Use `Title-Update` or `refresh: title` when the title is dynamic.
- Use `Progress-Update` or `refresh: properties` for furnace properties.
- Use `refresh` after several button variants or menu arguments change; use `refresh: *` only when the title and properties must update too.
- Do not rebuild a Container every few ticks with `reset` inside `Events.Tasks`; use the dedicated Container refresh fields.
