# ESC Pause Menu: pause_menu.yml

`plugins/KaMenu/pause_menu.yml` defines KaMenu's single Dialog entry on the vanilla ESC pause screen. KaMenu checks this file during startup and releases the default template when it is missing. Existing files are never overwritten.

After editing, run:

```bash
/km pause register
```

KaMenu compiles the file into `world/datapacks/KaMenuPauseEntry`. A full server restart is required before the client ESC pause screen changes.

{% hint style="info" %}
KaMenu registers exactly one ESC entry because vanilla groups multiple entries into a secondary menu. Use a `Bottom.type: multi` button matrix when several actions are needed.
{% endhint %}

## Complete Structure

```yaml
Title: '&bKaMenu'
External-Title: 'Server Menu'

Settings:
  can_escape: false
  pause: false
  after_action: NONE

Body:
  website_link:
    type: message
    width: 340
    text: '<text="&b[ Visit Website ]";hover="&aOpen the website";url=https://example.com>'

  welcome:
    type: message
    width: 300
    text:
      - '&fWelcome to KaMenu!'
      - '&7Select an option below.'

  guide_item:
    type: item
    material: PAPER
    amount: 1
    description: '&7KaMenu menu guide'
    description_width: 200
    show_overlays: true
    show_tooltip: true
    width: 32
    height: 32

Inputs:
  player_name:
    type: input
    text: '&ePlayer Name'
    default: 'Steve'
    max_length: 16
    remove_chars: global

Bottom:
  type: multi
  columns: 3
  buttons:
    main:
      text: '&aOpen Menu'
      tooltip: '&7Open the KaMenu main menu'
      width: 160
      menu: 'example/main_menu'
    website:
      text: '&bWebsite'
      tooltip: '&7Open in your browser'
      width: 160
      url: 'https://example.com'
    address:
      text: '&eCopy Address'
      width: 160
      copy: 'play.example.com'
    greet:
      text: '&dSend Greeting'
      width: 160
      actions:
        - 'tell: &aHello, $(player_name)!'
        - 'close'
  exit:
    text: '&cClose'
    tooltip: '&7Return to the game'
    width: 160
```

## Top-Level Fields

| Field | Description |
|-------|-------------|
| `Title` | Dialog title; supports Legacy colors and MiniMessage |
| `External-Title` | Entry text on the ESC pause screen; falls back to `Title` |
| `Settings` | Vanilla Dialog behavior settings |
| `Body` | Ordered static body components |
| `Inputs` | Static inputs; submitted values are available to `actions` as `$(key)` |
| `Bottom` | `notice`, `confirmation`, or `multi` buttons |

## Settings

```yaml
Settings:
  can_escape: false
  pause: false
  after_action: NONE
```

| Field | Default | Description |
|-------|---------|-------------|
| `can_escape` | `true` | Whether ESC may close the Dialog |
| `pause` | `false` | Whether single-player is paused; normally `false` on servers |
| `after_action` | `CLOSE` | Client behavior after click: `CLOSE`, `NONE`, or `WAIT_FOR_RESPONSE` |

`pause: true` cannot be combined with `after_action: NONE`; the vanilla Dialog codec rejects that combination.

## Body

Pause menus support static `message` and `item` bodies. Body content does not evaluate PAPI, KaMenu variables, conditions, JavaScript, or dynamic lists.

### message

```yaml
Body:
  info:
    type: message
    width: 340
    text:
      - '&fPlain text'
      - '<text="&b[ Website ]";hover="&7Click to open";url=https://example.com>'
```

`text` accepts a string or string list and reuses KaMenu's Legacy, MiniMessage, and `<text=...>` parsing. Static pause-menu `<text>` supports `hover`, `url`, `command`, and `newline`. Player-context features `actions` and `hover_item` are not supported.

### item

```yaml
Body:
  icon:
    type: item
    material: DIAMOND
    amount: 3
    description: '&bReward Item'
    description_width: 200
    show_overlays: true
    show_tooltip: true
    width: 32
    height: 32
```

| Field | Default | Description |
|-------|---------|-------------|
| `material` | `PAPER` | Vanilla Bukkit material; runtime slot references are unsupported |
| `amount` | `1` | Display amount, range 1-99 |
| `description` | None | Static text shown under the item |
| `description_width` | `200` | Description width, range 1-1024 |
| `show_overlays` | `true` | Shows count, durability, and other decorations |
| `show_tooltip` | `true` | Shows the vanilla item tooltip |
| `width` / `height` | `16` | Item display size, range 1-256 |

Datapack item bodies currently do not support runtime items, `name`, `lore`, player heads, `item_model`, or custom model data.

## Inputs

Pause menus support the same four standard input types as regular menus: `input`, `slider`, `dropdown`, and `checkbox`. Definitions, labels, defaults, and options are compiled statically by `register`; conditions, PAPI, and JavaScript cannot generate them dynamically.

```yaml
Inputs:
  player_name:
    type: input
    text: '&ePlayer Name'
    default: 'Steve'
    max_length: 16
    width: 250
    remove_chars: global

  amount:
    type: slider
    text: '&eAmount'
    min: 1
    max: 10
    step: 1
    default: 2
    format: '%s: %s'

  server:
    type: dropdown
    text: '&bServer'
    options:
      - 'survival => &aSurvival'
      - 'lobby => &eLobby'
    default_id: survival
    width: 200

  notify:
    type: checkbox
    text: '&aEnable Notifications'
    default: true
    on_true: enabled
    on_false: disabled
```

When a button with `actions` is clicked, KaMenu reads the client response. Use `$(player_name)`, `$(amount)`, `$(server)`, and `$(notify)` in its actions. Text input still follows `input-capture.trim-edge-spaces` and `remove_chars` from `config.yml`.

Fields match regular menus: text inputs support `hide_text`, `default`, `max_length`, `width`, `multiline`, and `remove_chars`; sliders support `min`, `max`, `step`, `default`, `format`, and `width`; single-option buttons support `options`, `default_id`, `hide_text`, and `width`; checkboxes support `default`, `on_true`, and `on_false`.

`menu`, `url`, `copy`, and `command` do not submit inputs to the action engine. When input is required, use `actions` and place operations such as `open:` or `command:` in that queue.

## Bottom

Common button fields:

| Field | Description |
|-------|-------------|
| `text` | Button label |
| `tooltip` | Hover tooltip |
| `width` | Button width, range 1-1024 |
| `menu` | Calls KaMenu and opens the specified menu ID |
| `actions` | Calls KaMenu and runs a standard action list with conditions, JS, packages, and `$(key)` |
| `url` | Opens a URL on the client |
| `copy` | Copies text on the client |
| `command` | Runs a command as the player on the client |

Each button may define at most one of `menu`, `actions`, `url`, `copy`, and `command`. The client submits only a fixed button ID; the actual menu ID or action list comes from the server-side `pause_menu.yml` state loaded during registration.

### notice

```yaml
Bottom:
  type: notice
  confirm:
    text: '&aSubmit'
    actions:
      - condition: 'isNull.$(player_name)'
        allow:
          - 'toast: type=task;msg=Enter a name;icon=barrier'
          - 'return'
      - 'tell: &aHello, $(player_name)!'
      - 'close'
```

Pause-menu button `actions` use the complete KaMenu action engine, including nested conditions, `wait`, `return`, JavaScript, global action packages, and local `Events.Click` packages in the same `pause_menu.yml`. `reset` is safely ignored because a pause menu is not owned by `MenuManager`; use `open:` for a regular KaMenu menu, or register the edited file again and restart the server.

### confirmation

```yaml
Bottom:
  type: confirmation
  confirm:
    text: '&aConfirm'
    menu: 'example/main_menu'
  deny:
    text: '&cCancel'
```

### multi

```yaml
Bottom:
  type: multi
  columns: 2
  buttons:
    menu:
      text: '&aMenu'
      menu: 'example/main_menu'
    website:
      text: '&bWebsite'
      url: 'https://example.com'
  exit:
    text: '&cClose'
```

## Commands

```bash
/km pause register
/km pause unregister
/km pause info
```

- `register`: reloads `pause_menu.yml`, validates it, and generates the datapack.
- `unregister`: removes the datapack generated by KaMenu.
- `info`: shows source-file and datapack paths and existence states.

{% hint style="warning" %}
`/km reload` does not re-register vanilla datapack Dialogs. A full server restart is required after every `register` or `unregister` operation.
{% endhint %}
