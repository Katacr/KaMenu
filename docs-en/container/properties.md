# Properties

`Properties` contains type-specific values. Static chest-like menus usually do not need it; furnace-class menus use progress properties, while anvils use input and rename properties.

## Field Overview

| Container type | Field | Type | Purpose |
|---|---|---|---|
| Furnace, blast furnace, smoker | `burn_progress` | Number/variable | Flame progress from 0 to 100% |
| Furnace, blast furnace, smoker | `cook_progress` | Number/variable | Arrow progress from 0 to 100% |
| Anvil | `input` | String/variable | Initial text and input-capture enablement |
| Anvil | `remove_chars` | String list or preset | Cleans submitted text |
| Anvil | `repair_cost` | Non-negative integer/variable | Level cost |
| Anvil | `maximum_repair_cost` | Non-negative integer/variable | Maximum accepted cost |
| Anvil | `repair_item_count` | Non-negative integer/variable | Input item consumption count |

Properties apply only to the matching Container type. Furnace properties on a chest, or anvil properties on a furnace, produce a warning and do not change the UI.

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

## Example: Processing UI With Completion Event

```yaml
Type: FURNACE
Title: '&8Processing'
Progress-Update: 5
Properties:
  burn_progress: '{meta:fuel_pct}'
  cook_progress: '{meta:cook_pct}'

Layout:
  - 'ABC'

Events:
  Progress:
    complete:
      source: cook_progress
      condition: '{progress.current} >= 100'
      trigger_initial: false
      actions:
        - 'actionbar: &aProcessing complete'
        - 'actions: furnace/reward'
        - 'meta: type=set;key=cook_pct;var=`0`'

Buttons:
  A:
    display:
      material: RAW_IRON
      name: '&fInput'
  B:
    display:
      material: COAL
      name: '&6Fuel'
  C:
    display:
      material: IRON_INGOT
      name: '&aOutput'
```

Progress may come from PAPI, `meta`, `data`, or JavaScript. KaMenu only changes the client-rendered progress; it does not consume fuel, run recipes, or create output items. Persistent background processing belongs in a separate plugin.

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

## Example: Anvil Rename

```yaml
Type: ANVIL
Title: '&8Rename Item'
Properties:
  input: 'New name'
  remove_chars: ['&', '\n', '\r']
  repair_cost: 0
  maximum_repair_cost: 40
  repair_item_count: 0

Layout:
  - 'ABC'

Buttons:
  A:
    display:
      material: PAPER
      name: '&eCurrent name: $(input)'
    actions:
      left:
        - 'tell: &aCaptured name: $(input)'
        - 'close'
  B:
    display:
      material: NAME_TAG
      name: '&7Secondary input slot'
  C:
    display:
      material: EMERALD
      name: '&aConfirm: $(input)'
    actions:
      left:
        - 'tell: &aConfirmed name: $(input)'
        - 'close'
```

`$(input)` becomes available in button display, conditions, actions, and `Events.Close` after the player submits a name. Do not concatenate uncleaned input into console commands or persistent keys.
