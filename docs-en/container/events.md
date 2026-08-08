# Events

Containers reuse KaMenu's shared lifecycle and action runtime; they do not have a separate action syntax.

| Event | Container behaviour | Shared reference |
|---|---|---|
| `Open` | Runs before the inventory is shown; opening waits for the action chain | [Events.Open](../modern-dialog/events.md#open) |
| `Close` | Runs after KaMenu observes a player-controlled close | [Events.Close](../modern-dialog/events.md#close) |
| `Click` | Defines reusable action groups called by button `actions` | [Events.Click](../modern-dialog/events.md#click) |
| `Tasks` | Runs periodic actions while the menu is open | [Events.Tasks](../modern-dialog/events.md#tasks) |
| `Progress` | Runs when a furnace progress condition changes state | [Refresh](refresh.md) |

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
