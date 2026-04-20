# 📥 Input Components (Inputs)

The `Inputs` node adds interactive input components to menus, allowing players to submit values, text, or make selections. Input values can be referenced in actions via the `$(key)` variable.

---

## Configuration Structure

```yaml
Inputs:
  component_key:
    type: 'component_type'
    text: 'component label text'
    # Component-specific configuration...
```

- **Component Key**: A unique identifier; use `$(key)` in actions to reference the component's value
- **Component Order**: Components are arranged top to bottom following the YAML order

---

## Referencing Input Values

In `Bottom` actions, you can reference the current value of an input field using `$(key)`:

```yaml
Inputs:
  player_name:  # Input value corresponds to $(player_name) below
    type: 'input'
    text: 'Please enter player name'
    default: 'Type here...'
  volume:  # Input value corresponds to $(volume) below
    type: 'slider'
    text: 'Volume'
    min: 0
    max: 100
    step: 1
    default: 50
    format: 'Volume: %s%s'
  amount:  # Input value corresponds to $(amount) below
    type: 'input'
    text: 'Please enter quantity'
    default: 'Type here...'
    
Bottom:
  type: 'notice'
  confirm:
    text: '&aConfirm'
    actions:
      - 'tell: &fYou entered name: &e $(player_name)'
      - 'tell: &fYou selected volume: &e $(volume)'
      - 'tell: &fYou entered quantity: &e $(amount)'
```

---

## Component Types

### input - Text Input Field

Allows players to enter arbitrary text.

**Configuration:**

| Field | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| `type` | `String` | ✅ | — | Fixed value `input` |
| `text` | `String` | ✅ | — | Input field label text, supports conditions |
| `default` | `String` | ❌ | `""` | Default placeholder text |
| `max_length` | `Int` | ❌ | `256` | Maximum input character count |
| `width` | `Int` | ❌ | `250` | Input field width (pixels) |
| `multiline` | Node | ❌ | — | Enable multiline input mode |

**Multiline Configuration:**

| Field | Type | Default | Description |
|------|------|---------|-------------|
| `multiline.max_lines` | `Int` | `5` | Maximum number of lines |
| `multiline.height` | `Int` | `100` | Input field height (pixels) |

**Example:**

```yaml
Inputs:
  player_name:
    type: 'input'
    text: '&aPlease enter player name'
    default: 'Steve'
    max_length: 16

  feedback:
    type: 'input'
    text: '&7Feedback content'
    default: 'Type here...'
    multiline:
      max_lines: 5
      height: 80
```

---

### slider - Numeric Slider

Allows players to select a value within a range by dragging a slider.

**Configuration:**

| Field | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| `type` | `String` | ✅ | — | Fixed value `slider` |
| `text` | `String` | ✅ | — | Slider label text, supports conditions |
| `min` | `Double` | ✅ | `0.0` | Minimum value |
| `max` | `Double` | ✅ | `10.0` | Maximum value |
| `default` | `Double` | ❌ | Equals `min` | Default value |
| `step` | `Double` | ❌ | `1.0` | Step increment per movement |
| `format` | `String` | ❌ | `%s: %s` | Display format (first `%s` is label, second is current value) |

**Example:**

{% hint style="warning" %}
**Important:** `min` value **must be less than** `max` value. If `min` >= `max`, the plugin will:
- Output a warning log to the console
- Automatically use default values (min=0.0, max=10.0)
{% endhint %}

```yaml
Inputs:
  volume:
    type: 'slider'
    text: '&eVolume'
    min: 0
    max: 100
    step: 5
    default: 50
    format: '&eVolume: &f%s%%'

  purchase_amount:
    type: 'slider'
    text: '&6Purchase Quantity'
    min: 1
    max: 64
    step: 1
    default: 1
    format: 'Quantity: %s'
```

---

### dropdown - Dropdown Select Box

Allows players to select one option from a predefined list.

**Configuration:**

| Field | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| `type` | `String` | ✅ | — | Fixed value `dropdown` |
| `text` | `String` | ✅ | — | Dropdown label text, supports conditions |
| `options` | `List<String>` | ✅ | — | List of options, each supports color codes |
| `default_id` | `String` | ❌ | — | Default selected option (must exactly match an option in `options`) |
| `width` | `Int` | ❌ | `200` | Dropdown width (pixels) |

**Example:**

```yaml
Inputs:
  color_select:
    type: 'dropdown'
    text: '&bSelect Color'
    options:
      - '&cRed'
      - '&aGreen'
      - '&bBlue'
      - '&eYellow'
    default_id: '&aGreen'

  server_select:
    type: 'dropdown'
    text: '&7Select Server'
    options:
      - 'lobby'
      - 'survival'
      - 'creative'
    default_id: 'lobby'
    width: 150
```

---

### checkbox - Checkbox

Allows players to toggle an on/off state.

**Configuration:**

| Field | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| `type` | `String` | ✅ | — | Fixed value `checkbox` |
| `text` | `String` | ✅ | — | Checkbox label text, supports conditions |
| `default` | `Boolean` | ❌ | `false` | Default checked state |
| `on_true` | `String` | ❌ | `true` | Value passed to actions when checked |
| `on_false` | `String` | ❌ | `false` | Value passed to actions when unchecked |

**Example:**

```yaml
Inputs:
  enable_notify:
    type: 'checkbox'
    text: '&aEnable announcement notifications'
    default: true

  pvp_mode:
    type: 'checkbox'
    text: '&cEnable PvP mode'
    default: false
    on_true: 'enabled'    # When checked, $(pvp_mode) = "enabled"
    on_false: 'disabled'  # When unchecked, $(pvp_mode) = "disabled"
```

**Using in Actions:**

```yaml
Bottom:
  confirm:
    actions:
      - 'console: pvp set %player_name% $(pvp_mode)'
      # When checked: executes pvp set Steve enabled
      # When unchecked: executes pvp set Steve disabled
```

---

## Complete Example

```yaml
Title: '&6Player Settings'

Inputs:
  nickname:
    type: 'input'
    text: '&eNickname'
    default: '%player_name%'
    max_length: 16

  chat_volume:
    type: 'slider'
    text: '&bChat Volume'
    min: 0
    max: 10
    default: 5
    format: '%s%s'

  language:
    type: 'dropdown'
    text: '&aLanguage'
    options:
      - '简体中文'
      - 'English'
    default_id: '简体中文'

  join_notify:
    type: 'checkbox'
    text: '&7Receive player join notifications'
    default: true

Bottom:
  type: 'confirmation'
  confirm:
    text: '&a[ Save Settings ]'
    actions:
      - 'set-data: nickname $(nickname)'
      - 'set-data: chat_volume $(chat_volume)'
      - 'set-data: language $(language)'
      - 'set-data: join_notify $(join_notify)'
      - 'tell: &aSettings saved!'
      - 'sound: entity.experience_orb.pickup'
  deny:
    text: '&c[ Cancel ]'
    actions:
      - 'tell: &7Operation cancelled.'
```
