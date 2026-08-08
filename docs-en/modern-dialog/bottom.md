# Bottom Buttons (Bottom)

The `Bottom` node defines the interactive button area at the bottom of the menu, with three layout modes available: `notice`, `confirmation`, and `multi`. Inside `multi.buttons`, you can also use `type: repeat` to generate dynamic button lists.

---

## Configuration Structure

```yaml
Bottom:
  type: 'mode_type'   # notice | confirmation | multi
  # Mode-specific configuration...
```

{% hint style="info" %}
`repeat` is not a `Bottom.type` layout mode, so do not write `Bottom.type: repeat`. It is a dynamic button template written under `Bottom.type: multi` as `buttons.<buttonId>.type: repeat`.
{% endhint %}

---

## Type Overview

| Type | Name | Purpose | Common Use Cases |
|------|------|---------|------------------|
| `notice` | Single button mode | Displays one confirm button | Information confirmation, reward claims, simple submit actions |
| `confirmation` | Confirm/cancel dual button mode | Displays confirm and cancel buttons | Purchase confirmation, delete confirmation, dangerous-action confirmation |
| `multi` | Multi-button matrix mode | Displays multiple buttons with configurable columns and optional exit button | Main menus, control panels, category entries, complex action menus |

---

## Layout Modes And Dynamic Buttons

### notice - Single Button Mode

Displays only one confirm button, suitable for information display or simple trigger actions.

**Configuration:**

| Field | Description |
|------|-------------|
| `confirm.text` | Button text, supports color codes and conditions |
| `confirm.width` | Optional, button width (1-1024) |
| `confirm.actions` | List of actions to execute on click |

**Example:**

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&a[ Claim Reward ]'
    actions:
      - 'console: give %player_name% diamond 1'
      - 'tell: &aYou claimed a diamond!'
      - 'sound: entity.player.levelup'
```

---

### confirmation - Confirm/Cancel Dual Button Mode

Displays confirm and cancel buttons, suitable for dangerous operations requiring secondary confirmation.

**Configuration:**

| Field | Description |
|------|-------------|
| `confirm.text` | Confirm button text, supports conditions |
| `confirm.width` | Optional, confirm button width (1-1024) |
| `confirm.actions` | List of actions to execute on confirm click |
| `deny.text` | Cancel button text, supports conditions |
| `deny.width` | Optional, cancel button width (1-1024) |
| `deny.actions` | List of actions to execute on cancel click |

**Example:**

```yaml
Bottom:
  type: 'confirmation'
  confirm:
    text: '&a[ Confirm Purchase ]'
    actions:
      - 'console: eco take %player_name% 100'
      - 'console: give %player_name% diamond_sword 1'
      - 'tell: &aPurchase successful!'
      - 'sound: entity.experience_orb.pickup'
  deny:
    text: '&c[ Cancel ]'
    actions:
      - 'tell: &7Purchase cancelled.'
      - 'sound: block.note_block.bass'
```

---

### multi - Multi-Button Matrix Mode

`multi` displays multiple buttons at the bottom of a menu. Buttons under `buttons` follow YAML declaration order, `columns` controls how many columns appear per row, and `exit` can append a separate exit or back button after the matrix.

#### Basic Format

```yaml
Bottom:
  type: multi
  columns: 2

  buttons:
    shop:
      text: '&6[ Shop ]'
      tooltip:
        - '&7Open the server shop'
      actions:
        - 'open: shop/main'

    profile:
      text: '&b[ Profile ]'
      actions:
        - 'open: profile'

    settings:
      text: '&7[ Settings ]'
      actions:
        - 'open: settings'

    admin:
      text: '&c[ Admin Panel ]'
      show-condition: 'hasPerm.kamenu.admin'
      actions:
        - 'open: admin/tools'

  exit:
    text: '&8[ Close ]'
    actions:
      - 'close'
```

This example generates buttons in `shop → profile → settings → admin` order and arranges them in two columns. Button IDs such as `shop` only need to be unique inside the current `buttons` node.

#### multi Configuration

| Field | Type | Default | Description |
|------|------|---------|-------------|
| `columns` | `Int` | `2` | Number of button columns per row |
| `buttons` | Node | — | List of buttons (arranged in YAML order) |
| `exit` | Node | — | Optional exit/return button (displayed at the end of the button list) |

#### Normal Button Configuration

| Field | Type | Description |
|------|------|-------------|
| `show-condition` | String | Optional, button display condition; button is hidden when condition is not met |
| `text` | String/List | Button text, supports color codes, conditions, and MiniMessage tags |
| `width` | Int | Optional, button width (1-1024); uses default width if not set |
| `tooltip` | List | Optional, button hover tooltip (one string per line), supports color codes and MiniMessage |
| `actions` | List | Optional, list of actions to execute on click; if not set, clicking does nothing |

#### Exit Button Configuration

| Field | Type | Description |
|------|------|-------------|
| `text` | String/List | Exit button text, supports color codes, conditions, and MiniMessage tags |
| `width` | Int | Optional exit button width (1-1024) |
| `tooltip` | List | Optional exit button hover tooltip, one string per line |
| `actions` | List | Optional action list; commonly uses `close`, `open`, or `force-open` |

#### Display Conditions

Normal buttons can use `show-condition` to control visibility:

```yaml
Bottom:
  type: multi
  columns: 2
  buttons:
    admin:
      show-condition: 'hasPerm.kamenu.admin'
      text: '&c[ Admin Button ]'
      actions:
        - 'open: admin/tools'

    level_reward:
      show-condition: '%player_level% >= 10'
      text: '&e[ Level 10 Reward ]'
      actions:
        - 'actions: claim_level_reward'

    public:
      text: '&a[ Public Button ]'
      actions:
        - 'tell: &aEvery player can see this button'
```

#### Button Width

Normal buttons and the `exit` button in `multi` mode can use `width` to set their Java Dialog button width:

- The supported range is 1-1024.
- When omitted, the default width from the Paper Dialog API is used.
- Conditions are supported.
- Width only affects the Java Dialog and does not affect Bedrock forms.

```yaml
Bottom:
  type: multi
  columns: 2
  buttons:
    wide_button:
      text: '&a[ Wide Button ]'
      width: 200
      actions:
        - 'tell: &aThis is a wide button'

    conditional_width:
      text: '&c[ Conditional Width ]'
      width:
        - condition: '%player_is_op% == true'
          allow: 200
          deny: 100
      actions:
        - 'tell: &cButton width changes based on a condition'

  exit:
    text: '&8[ Exit ]'
    width: 80
    actions:
      - 'close'
```

An excessively large width may cause a button to extend beyond the screen. Adjust it for the actual layout.

#### repeat - Dynamic Button Lists

A button under `multi.buttons` can use `type: repeat` to generate real native Dialog buttons from a dynamic data source. This is useful for online player lists, warp lists, friend lists, mail lists, and other content with unknown item counts.

**Basic location:**

```yaml
Bottom:
  type: multi
  buttons:
    list_id:
      type: repeat
      source: "data source"
      item:
        text: "&a{item.value}"
        actions:
          - "tell: You clicked {item.value}"
```

```yaml
JavaScript:
  getWarpList: |
    JSON.stringify([
      { id: "home", name: "Home", world: "world", x: 100, y: 64, z: 200 },
      { id: "mine", name: "Mine", world: "world", x: -30, y: 12, z: 80 }
    ]);

Bottom:
  type: multi
  columns: 2
  buttons:
    warp_list:
      type: repeat
      source: "[getWarpList]"
      page_size: 20
      item:
        text: "&a{item.name}"
        width: 160
        tooltip:
          - "&7World: &f{item.world}"
          - "&7Location: &f{item.x}, {item.y}, {item.z}"
          - "&eClick to teleport"
        actions:
          - "actions: teleport_warp,{item.id}"
      empty:
        text: "&7No warps"
        actions:
          - "toast: type=task;msg=No data;icon=barrier"

    prev:
      text: "&ePrevious"
      show-condition: "{page:warp_list} > 1"
      actions:
        - "page: warp_list prev"
        - "reset"

    next:
      text: "&eNext"
      show-condition: "{page:warp_list} < {pages:warp_list}"
      actions:
        - "page: warp_list next"
        - "reset"
```

**repeat Configuration:**

| Field | Type | Default | Description |
|------|------|---------|-------------|
| `type` | String | — | Must be `repeat` |
| `source` | String | — | Data source. Recommended format is `[functionName]`, calling a `JavaScript` function that returns a JSON array |
| `split` | String | — | Optional separator for non-JSON string lists, such as `","` |
| `trim` | Boolean | `true` | Whether to trim each item when `split` is used |
| `page_size` / `page-size` | Int | `20` | Number of generated buttons per page, range `1-99` |
| `item` | Node | — | Button template for each list item |
| `empty` | Node | — | Optional button shown when the source has no items |

`source` is resolved through KaMenu internal variables, PAPI, `{js:...}`, and other text variables first. The resolved result can be a JSON array, newline text, or a simple string list used with `split`. Items may be objects, strings, or numbers. Object fields become `{item.fieldName}` and can be used in button text, tooltip, show-condition, and actions.

**Matrix alignment:**

On every render, KaMenu counts the repeat buttons actually generated on the current page. If that count is not divisible by `columns`, it appends enough blank buttons to complete the row. For example, with `columns: 3` and 28 generated buttons, KaMenu adds 2 blank buttons so the repeat section contains 30 buttons. Blank buttons have no visible text but execute `reset` when clicked, rebuilding the current menu callback. When `item.width` is configured, padding buttons reuse that width so the repeat grid stays aligned; otherwise they use the default width. Normal buttons such as Previous/Next are excluded from the repeat count and are appended after the padding buttons.

Built-in list variables `{list:key}` and `{glist:key}` return JSON array strings and can be used directly as `source`:

```yaml
Bottom:
  type: multi
  buttons:
    friends:
      type: repeat
      source: "{list:friends}"
      item:
        text: "&a{item.value}"
        actions:
          - "tell: You clicked {item.value}"
```

If the data source returns a simple string list such as `player1, player2, player3`, use `split`:

```yaml
Events:
  Open:
    - "data: type=set;key=recent_players_raw;var=`player1, player2, player3`"

Bottom:
  type: multi
  buttons:
    player_list:
      type: repeat
      source: "{data:recent_players_raw}"
      split: ","
      trim: true
      item:
        text: "&a{item.value}"
        actions:
          - "tell: You clicked {item.value}"
```

Built-in item variables:

| Variable | Description |
|----------|-------------|
| `{item.xxx}` | Field from the current item object |
| `{item.value}` | Value when the current item is a string or number |
| `{item.index}` | Index in the full list, starting at 0 |
| `{item.number}` | Number in the full list, starting at 1 |
| `{item.page_index}` | Index on the current page, starting at 0 |
| `{item.page_number}` | Number on the current page, starting at 1 |

Pagination variables can be used in normal `Bottom.buttons` and repeat item templates:

| Variable | Description |
|----------|-------------|
| `{page:listId}` | Current page |
| `{pages:listId}` | Total pages |
| `{total:listId}` | Total item count |
| `{start:listId}` | Current page start index |
| `{end:listId}` | Current page end index |

Pagination actions:

```yaml
- "page: warp_list next"
- "page: warp_list prev"
- "page: warp_list 1"
- "page: warp_list +1"
- "page: warp_list -1"
```

The `page:` action only changes page state. It does not refresh the dialog by itself. Usually follow it with `reset`, `open`, or `force-open`.

---

## Bedrock Button Icons

When Geyser and Floodgate are installed, KaMenu can provide button images for menus without input components. KaMenu uses an icon-enabled Floodgate `SimpleForm` only when all of these conditions are met:

- The current player joined through Floodgate.
- The menu has no `Inputs` components.
- At least one currently visible button has a valid `icon`.

Other players and menus continue to use the existing Java Dialog. Menus with `Inputs` are still converted automatically by Geyser, so their bottom buttons do not support `icon`.

`icon` can be configured under `Bottom.confirm`, `Bottom.deny`, `Bottom.button1`, `Bottom.buttons.<buttonId>`, `Bottom.exit`, and `Bottom.buttons.<repeatId>.item`.

### URL Images

```yaml
Bottom:
  type: multi
  buttons:
    shop:
      text: '&aOpen Shop'
      icon:
        type: url
        value: 'https://example.com/images/shop.png'
      actions:
        - 'open: shop'
```

Only `http` and `https` URLs are accepted, with a maximum length of 2048 characters. The player's client downloads the image directly, so the image host may receive the player's IP address. Use a trusted image host.

### Bedrock Resource-Pack Paths

```yaml
Bottom:
  type: multi
  buttons:
    reward:
      text: '&eClaim Reward'
      icon:
        type: path
        value: 'textures/items/diamond'
      actions:
        - 'actions: reward'
```

`path` refers to an image inside the Bedrock client resource pack, not a file on the server. Absolute paths and paths containing `..` are rejected.

A URL can also use the scalar shorthand. Other scalar values are treated as resource-pack paths:

```yaml
icon: 'https://example.com/images/shop.png'
```

### Dynamic `repeat` Icons

`Bottom.buttons.<repeatId>.item.icon` supports `{item.xxx}`, PAPI placeholders, and built-in KaMenu variables:

```yaml
item:
  text: '&f{item.name}'
  icon:
    type: url
    value: '{item.icon}'
  actions:
    - 'tell: &aYou selected {item.name}'
```

A Bedrock `SimpleForm` is a vertical button list and has no equivalents for Java Dialog button grids, widths, or tooltips. Therefore, `columns`, button `width`, and `tooltip` do not affect the Bedrock form, and repeat grid-padding buttons are omitted. If a menu button uses a standalone client-side `url:` or `copy:` action, the entire menu falls back to the existing Java Dialog conversion so that action keeps working.

---

## Conditional Button Text

The `text` field of all buttons supports conditions:

```yaml
Bottom:
  type: 'confirmation'
  confirm:
    text:
      - condition: "%player_level% >= 10"
        allow: '&6[ VIP Confirm ]'
        deny: '&a[ Confirm ]'
    actions:
      - 'tell: &aConfirmed'
  deny:
    text: '&c[ Cancel ]'
    actions:
      - 'tell: &7Cancelled'
```

For complete condition syntax, see [Conditions](conditions.md).
