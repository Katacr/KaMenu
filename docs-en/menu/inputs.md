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

## Type Overview

| Type | Name | Returned Value | Common Use Cases |
|------|------|----------------|------------------|
| `input` | Text input field | Text entered by the player | Player names, amounts, search terms, messages, command arguments |
| `slider` | Numeric slider | Number; integer values are returned as integers | Amounts, volume, levels, range choices |
| `dropdown` | Single-option button | Selected option ID | Server selection, class selection, category filtering |
| `checkbox` | Checkbox | Default `true` / `false`, customizable with `on_true` / `on_false` | Agreement toggles, option switches, two-state choices |

Input values enter the action context only after the player submits the dialog by clicking a bottom button. Therefore, `$(key)` is mainly used in `Bottom` actions, conditions, close events, and other post-submit logic; `Events.Open` cannot read live input values.

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
| `hide_text` | `Boolean` | ❌ | `false` | Whether to hide the input field's own label text |
| `default` | `String` | ❌ | `""` | Default placeholder text |
| `max_length` | `Int` | ❌ | `256` | Maximum input character count |
| `width` | `Int` | ❌ | `250` | Input field width (pixels) |
| `remove_chars` | `String/List` | ❌ | `""` | Characters to remove after the input is captured, such as `&` or `_` |
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
    remove_chars: '&_'

  feedback:
    type: 'input'
    text: '&7Feedback content'
    hide_text: true
    default: 'Type here...'
    multiline:
      max_lines: 5
      height: 80
```

`remove_chars` only applies to `type: input` text fields. After the player clicks a button, KaMenu captures the input value, removes the configured characters, and then actions, conditions, and JavaScript parameters read the processed value through `$(player_name)`.

List form is also supported:

```yaml
Inputs:
  command_arg:
    type: 'input'
    text: '&aEnter argument'
    remove_chars:
      - '&'
      - '_'
      - '"'
```

You can also reference a global character removal list from `config.yml`:

```yaml
Inputs:
  command_arg:
    type: 'input'
    text: '&aEnter argument'
    remove_chars: global
```

If the string value of `remove_chars` matches a preset under `input-capture.remove-char-lists`, KaMenu uses that global list. Otherwise, the value keeps the legacy behavior and is treated as the literal set of characters to remove.

`remove_chars` supports special escape sequences:

| Syntax | Meaning |
|--------|---------|
| `\s` | Normal space |
| `\n` | Newline |
| `\r` | Carriage return |
| `\t` | Tab |
| `\\` | Backslash `\` |

Example: remove color symbols, underscores, spaces, and newlines:

```yaml
Inputs:
  command_arg:
    type: 'input'
    text: '&aEnter argument'
    remove_chars:
      - '&'
      - '_'
      - '\s'
      - '\n'
```

To trim leading and trailing spaces for all text inputs, enable `input-capture.trim-edge-spaces` in `config.yml`.

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

### dropdown - Single-Option Button

Displays a clickable button. Each click cycles to the next predefined option.

**Configuration:**

| Field | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| `type` | `String` | ✅ | — | Fixed value `dropdown` |
| `text` | `String` | ✅ | — | Single-option button label text, supports conditions |
| `hide_text` | `Boolean` | ❌ | `false` | Whether to hide the single-option button's own label text |
| `options` | `List<String>` | ✅ | — | Option list. Supports plain strings, `id => display` format, and conditional option lists |
| `default_id` | `String` | ❌ | — | Default selected option ID |
| `width` | `Int` | ❌ | `200` | Single-option button width (pixels) |

**Supported `options` formats:**

1. **Old format: display text and submitted value are the same**
```yaml
options:
  - 'red'
  - 'green'
  - 'blue'
```

2. **New format: use `id => display` to separate submitted value from visible text**
```yaml
options:
  - 'red => &cRed'
  - 'green => &aGreen'
  - 'blue => &bBlue'
```

3. **Conditional format: compatible with `allow` / `deny` returning string lists**
```yaml
options:
  - condition: "%player_is_op% == true"
    allow:
      - 'red => &cOP-Red'
      - 'green => &aOP-Green'
    deny:
      - 'red => &cPlayer-Red'
      - 'green => &aPlayer-Green'
```

> When using `id => display`:
> - The left side `id` becomes the real submitted value used by `$(variable)` in actions
> - The right side `display` is what players actually see in the UI

**Example:**

```yaml
Inputs:
  color_select:
    type: 'dropdown'
    text: '&bSelect Color'
    hide_text: true
    options:
      - 'red => &cRed'
      - 'green => &aGreen'
      - 'blue => &bBlue'
      - 'yellow => &eYellow'
    default_id: 'green'

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
      - 'zh_cn => 简体中文'
      - 'en_us => English'
    default_id: 'zh_cn'

  color_select:
    type: 'dropdown'
    text: '&bSelect Color'
    hide_text: true
    options:
      - condition: "%player_is_op% == true"
        allow:
          - 'red => &cOP-Red'
          - 'green => &aOP-Green'
          - 'blue => &bOP-Blue'
        deny:
          - 'red => &cPlayer-Red'
          - 'green => &aPlayer-Green'
          - 'blue => &bPlayer-Blue'
    default_id: 'red'

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
