# Creating A Container Menu

This tutorial builds a complete three-row chest menu from scratch. You will create the file, select an inventory type, arrange slots, define item buttons and actions, reload the plugin, and diagnose common mistakes.

## Learning Goals

- ✅ Create a Container menu file and identify its menu ID
- ✅ Select the correct `Type`
- ✅ Arrange inventory slots with `Layout`
- ✅ Define item buttons and click actions under `Buttons`
- ✅ Reload and open the menu
- ✅ Extend a button with variables and `variants`

## Step 1: Create The Menu File

Create a file under `plugins/KaMenu/menus/`. Subfolders become part of the menu ID:

```text
plugins/KaMenu/menus/
└── tutorial/
    └── first_container.yml
```

The menu ID is `tutorial/first_container`; omit `.yml` when opening it.

## Step 2: Select A Type And Title

Start with the Container identity:

```yaml
Type: CHEST
Title: '&8My First Chest Menu'
```

A `CHEST` has exactly 9 logical slots per row and supports one to six rows. This tutorial uses three rows. See [Type](type.md) for other dimensions.

## Step 3: Design The Layout

Add a three-row layout:

```yaml
Layout:
  - '#########'
  - '#   D   #'
  - '#H     X#'
```

Each row contains 9 logical slots:

| Character | Button | Purpose |
|---|---|---|
| `#` | `Buttons.#` | Gray border |
| `D` | `Buttons.D` | Diamond button |
| `H` | `Buttons.H` | Help button |
| `X` | `Buttons.X` | Close button |
| Space | None | Empty slot |

Define `Buttons.#` once even though `#` appears in several slots. Wrap a multi-character ID in backticks, for example `` `shop` ``. See [Layout](layout.md).

## Step 4: Define Buttons

Define the four IDs referenced by the layout:

```yaml
Buttons:
  '#':
    display:
      material: GRAY_STAINED_GLASS_PANE
      name: ' '

  D:
    display:
      material: DIAMOND
      name: '&bClaim Diamond'
      lore:
        - '&7Click to receive one diamond'
    actions:
      left:
        - 'item: type=give;mats=DIAMOND;amount=1'
        - 'actionbar: &aYou received one diamond'
        - 'sound: ENTITY_PLAYER_LEVELUP;volume=1.0;pitch=1.0'

  H:
    display:
      material: BOOK
      name: '&eHelp'
      lore:
        - '&7View menu help'
    actions:
      left:
        - 'tell: &eClick the diamond to claim the item.'

  X:
    display:
      material: BARRIER
      name: '&cClose'
    actions:
      left:
        - 'close'
```

Every standard button requires `display.material`. `display` controls the item and `actions.left` controls a left click. See [Buttons](buttons.md) for other click types.

## Step 5: Add Settings And Events

Throttle valid button clicks to one every 250 milliseconds and send action-bar feedback:

```yaml
Settings:
  min_click_delay: 250

Events:
  Open:
    - 'actionbar: &aWelcome, %player_name%'
  Close:
    - 'actionbar: &7Menu closed'
```

`min_click_delay` does not throttle empty slots or an actionless border. `Events.Open` runs before the inventory is shown; `Events.Close` runs after KaMenu observes the close.

## Step 6: Complete Menu

The final `plugins/KaMenu/menus/tutorial/first_container.yml` is:

```yaml
Type: CHEST
Title: '&8My First Chest Menu'

Settings:
  min_click_delay: 250

Layout:
  - '#########'
  - '#   D   #'
  - '#H     X#'

Events:
  Open:
    - 'actionbar: &aWelcome, %player_name%'
  Close:
    - 'actionbar: &7Menu closed'

Buttons:
  '#':
    display:
      material: GRAY_STAINED_GLASS_PANE
      name: ' '

  D:
    display:
      material: DIAMOND
      name: '&bClaim Diamond'
      lore:
        - '&7Click to receive one diamond'
    actions:
      left:
        - 'item: type=give;mats=DIAMOND;amount=1'
        - 'actionbar: &aYou received one diamond'
        - 'sound: ENTITY_PLAYER_LEVELUP;volume=1.0;pitch=1.0'

  H:
    display:
      material: BOOK
      name: '&eHelp'
      lore:
        - '&7View menu help'
    actions:
      left:
        - 'tell: &eClick the diamond to claim the item.'

  X:
    display:
      material: BARRIER
      name: '&cClose'
    actions:
      left:
        - 'close'
```

## Step 7: Reload And Open

Save the file, then run:

```text
/km reload menu
/km open tutorial/first_container
```

If the menu does not appear in `/km open ` Tab completion, inspect the console for YAML or Container diagnostics. KaMenu rejects incomplete menus with invalid slot counts, unknown button references, or mixed Dialog fields.

## Step 8: Add A Dynamic State

Replace a standard `display/actions` button with `variants` when its item and behaviour depend on permission or state:

```yaml
Buttons:
  D:
    variants:
      - priority: 0
        condition: 'hasPerm.tutorial.claim'
        display:
          material: DIAMOND
          name: '&aAvailable'
        actions:
          left:
            - 'item: type=give;mats=DIAMOND;amount=1'
            - 'refresh: D'
      - priority: 1
        display:
          material: COAL
          name: '&cNo permission'
        actions:
          left:
            - 'actionbar: &cYou cannot claim this item'
```

Do not keep top-level `display/actions` beside `variants` on the same button.

## Common Mistakes

| Symptom | Common cause | Fix |
|---|---|---|
| Menu parse failure | A `CHEST` row is not 9 logical slots | Recount characters, spaces, and backtick IDs |
| Unknown button diagnostic | Layout uses an ID missing from `Buttons` | Define `Buttons.<id>` or replace it with a space |
| Multi-character ID splits | `shop` is not wrapped in backticks | Write `` `shop` `` in Layout |
| Wrong menu family | `Body`, `Inputs`, or `Bottom` was added | Containers use `Layout` and `Buttons` |
| PAPI value remains unresolved | Expansion is missing or not required | Install it and review [Settings](settings.md) |
| Display does not change after a click | State changed without a refresh | Use `refresh: <buttonId>` or `refresh` |

Continue with [Settings](settings.md), [Button Variants](buttons.md#variants), [Refresh](refresh.md), [Furnace And Anvil Properties](properties.md), and [Events](events.md).
