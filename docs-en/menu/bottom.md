# 🔘 Bottom Buttons (Bottom)

The `Bottom` node defines the interactive button area at the bottom of the menu, with three layout modes available.

---

## Configuration Structure

```yaml
Bottom:
  type: 'mode_type'   # notice | confirmation | multi
  # Mode-specific configuration...
```

---

## Three Layout Modes

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

Supports multiple custom buttons arranged in a matrix, with an optional exit button.

**Configuration:**

| Field | Type | Default | Description |
|------|------|---------|-------------|
| `columns` | `Int` | `2` | Number of button columns per row |
| `buttons` | Node | — | List of buttons (arranged in YAML order) |
| `exit` | Node | — | Optional exit/return button (displayed at the end of the button list) |

**Button Configuration:**

| Field | Type | Description |
|------|------|-------------|
| `show-condition` | String | Optional, button display condition; button is hidden when condition is not met |
| `text` | String/List | Button text, supports color codes, conditions, and MiniMessage tags |
| `width` | Int | Optional, button width (1-1024); uses default width if not set |
| `tooltip` | List | Optional, button hover tooltip (one string per line), supports color codes and MiniMessage |
| `actions` | List | Optional, list of actions to execute on click; if not set, clicking does nothing |

**Exit Button Configuration:**

| Field | Type | Description |
|------|------|-------------|
| `text` | String/List | Exit button text, supports color codes, conditions, and MiniMessage tags |
| `width` | Int | Optional, exit button width (1-1024) |
| `actions` | List | Optional, list of actions to execute on click; if not set, clicking does nothing |

**Example:**

```yaml
Bottom:
  type: 'multi'
  columns: 3

  buttons:
    btn_shop:
      text: '&6[ Shop ]'
      actions:
        - 'open: shop/main'

    btn_profile:
      text: '&b[ Profile ]'
      actions:
        - 'open: profile'

    btn_settings:
      text: '&7[ Settings ]'
      actions:
        - 'open: settings'

    btn_admin:
      text: '&4[ Admin Panel ]'
      actions:
        - 'open: admin/tools'

  exit:
    text: '&8[ Close ]'
    actions:
      - 'actionbar: &7Menu closed'
      - 'close'
```

You can also use `show-condition` in Multi mode to control button visibility:

```yaml
Bottom:
  type: multi
  columns: 2
  buttons:
    1:
      show-condition: "%player_is_op% == true"  # Only admins can see this button
      text: '[ Admin Button ]'
      actions: ...
    2:
      show-condition: "%player_level% >= 10"  # Players level 10+ can see
      text: '[ VIP Button ]'
      actions: ...
    3:
      text: '[ Normal Button ]'  # No condition, visible to all players
      actions: ...
```

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

For complete condition syntax, see [🔍 Conditions](conditions.md).

---

## Button Width

All buttons support custom width configuration via the `width` field.

**Applicable Scope:**
- Confirm button in `notice` mode (`confirm.width`)
- Confirm and cancel buttons in `confirmation` mode (`confirm.width` and `deny.width`)
- All buttons in `multi` mode (`buttons` and `exit` buttons)

**Width Values:**
- Range: 1 - 1024
- Uses default width (determined by Paper Dialog API) if not set
- Supports conditions

**Example:**

```yaml
# notice mode
Bottom:
  type: 'notice'
  confirm:
    text: '&a[ Confirm ]'
    width: 200
    actions:
      - 'tell: &aOperation confirmed'

# confirmation mode
Bottom:
  type: 'confirmation'
  confirm:
    text: '&a[ Confirm ]'
    width: 200
    actions:
      - 'tell: &aOperation confirmed'
  deny:
    text: '&c[ Cancel ]'
    width: 100
    actions:
      - 'tell: &cOperation cancelled'

# multi mode
Bottom:
  type: 'multi'
  columns: 2

  buttons:
    wide_button:
      text: '&a[ Wide Button ]'
      width: 200
      actions:
        - 'tell: &aThis is a wide button'

    narrow_button:
      text: '&b[ Narrow Button ]'
      width: 50
      actions:
        - 'tell: &bThis is a narrow button'

    conditional_width:
      text: '&c[ Conditional Width ]'
      width:
        - condition: '%player_is_op% == true'
          allow: 200
          deny: 100
      actions:
        - 'tell: &cButton width changes based on permission'

  exit:
    text: '&8[ Exit ]'
    width: 80
    actions:
      - 'close'
```

**Note:**
- Width values affect the actual display size of buttons on screen
- Too large widths may cause buttons to extend beyond the screen
- Adjust width values based on actual display needs
