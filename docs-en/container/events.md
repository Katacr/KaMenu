# Events

Containers reuse KaMenu's shared lifecycle and action runtime; they do not have a separate action syntax.

## Event Overview

| Event | Trigger | Typical use | Important behaviour |
|---|---|---|---|
| `Open` | Before the inventory is shown | Initialize `meta`, check requirements, notify | `return` prevents opening |
| `Close` | After the player closes the menu | Clear temporary state, save input, return | Do not assume a button caused the close |
| `Click` | Never automatic | Define reusable action groups | Called with `actions: <id>` |
| `Tasks` | Periodically while open | Countdown, state checks, light updates | Consider `skip_if_running` when using `wait` |
| `Progress` | Furnace condition changes false to true | Complete, empty, or reward actions | Furnace-class Containers only |

`Open`, `Close`, `Click`, and `Tasks` use the complete fields documented in the shared [Events reference](../modern-dialog/events.md). See [Refresh](refresh.md#furnace-progress-events) for furnace-class `Progress` configuration.

```yaml
Events:
  Open:
    - 'actionbar: &aMenu opened'
  Close:
    - 'actionbar: &7Menu closed'
  Click:
    help:
      - 'tell: &eThis is a reusable action group'
```

`open`, `close`, `force-open`, `force-close`, `reset`, and `return` have the same meanings as in Dialog menus. `reset` rerenders the current Container without running `Events.Open`; `force-open` and `force-close` skip lifecycle events.

Conditions, variables, PAPI, JavaScript, action packages, and `wait` use the shared references: [Conditions](../modern-dialog/conditions.md), [Actions](../modern-dialog/actions.md), [JavaScript](../modern-dialog/javascript.md), and [Data Storage](../data/storage.md).

## Example: Initialize, Reuse Actions, And Clean Up

```yaml
Events:
  Open:
    - 'meta: type=set;key=opened_at;var=`{js:Date.now()}`'
    - 'actionbar: &aMenu opened'
  Close:
    - 'meta: type=delete;key=opened_at'
    - 'actionbar: &7Menu closed'
  Click:
    confirm:
      - 'tell: &aConfirmed'
      - 'close'
```

Call the reusable `Click` group from a button:

```yaml
Buttons:
  confirm:
    display:
      material: LIME_CONCRETE
      name: '&aConfirm'
    actions:
      left:
        - 'actions: confirm'
```

Use `Events.Open` for initialization. For normal visual updates, use `refresh` or `reset` after a click instead of relying on `Events.Open` to run again.
