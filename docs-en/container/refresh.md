# Refresh

Container refresh intervals use ticks. Values below 5 generate a warning; prefer targeted refreshes instead of rebuilding the whole inventory every tick.

| Key | Effect |
|---|---|
| `Update` | Refreshes title, button display/visibility, and container properties |
| `Title-Update` | Refreshes only the title |
| `Progress-Update` | Refreshes furnace properties and checks `Events.Progress` |
| `Buttons.<id>.update` | Refreshes only the slots occupied by that button |

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
