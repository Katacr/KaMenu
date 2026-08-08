# Properties

`Properties` contains type-specific values. Static chest-like menus usually do not need it; furnace-class menus use progress properties, while anvils use input and rename properties.

## Furnace-class Containers

Applies to `FURNACE`, `BLAST_FURNACE`, and `SMOKER`:

```yaml
Type: FURNACE
Properties:
  burn_progress: '{meta:burn}'
  cook_progress: '{meta:cook}'
```

Progress accepts `0`, `55.31`, `55.31%`, and `100%`, and is clamped to `0..100`. This is a client-rendered flame and cook arrow, not a real furnace session: KaMenu does not consume fuel or execute recipes.

See [Refresh](refresh.md) and [Events](events.md) for progress intervals and completion events. Persistent offline furnace logic belongs in a separate backend plugin.

## Anvil

```yaml
Type: ANVIL
Properties:
  input: 'Name_%player_name%'
  remove_chars: ['&', '_']
  repair_cost: 0
  maximum_repair_cost: 40
  repair_item_count: 0
```

`input` is the initial text, `remove_chars` cleans captured input, and the repair fields control supported anvil properties. After capture, `$(input)` is available in button display, `view_condition`, actions, and close events; it is not available in `Events.Open`.

Clean `$(input)` before using it in commands, JavaScript, conditions, or persistence. The result slot is controlled by KaMenu and must not be used as player item storage.

See [Input Components](../modern-dialog/inputs.md) for cleanup and [Events](events.md) for lifecycle behaviour.
