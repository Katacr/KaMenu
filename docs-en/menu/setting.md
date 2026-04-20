# ⚙️ Global Settings (Settings)

The `Settings` node configures the menu's global behaviour, including how it can be closed and what happens after a button action is executed.

---

## Configuration Overview

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `can_escape` | `Boolean` | `true` | Whether players can close the menu using the ESC key |
| `after_action` | `String` | `CLOSE` | Client-side behaviour after a button action is executed |
| `need_placeholder` | `List<String>` | `null` | List of PlaceholderAPI expansions required by the menu |

---

## can_escape

### Description

Controls how the menu can be closed, determining whether players can press ESC to close it.

### Values

| Value | Description |
|-------|-------------|
| `true` (default) | Players can close the menu with ESC; the corresponding button's actions will be executed on close |
| `false` | Players must click a specific button to close the menu; ESC is disabled |

### Configuration Example

```yaml
Settings:
  can_escape: false  # Force players to click a button to close the menu
```

### Use Cases

**Recommended: `true` (default)**
- Regular menus where players should be able to exit freely
- Scenarios requiring flexible closing behaviour

**Recommended: `false`**
- Important confirmation menus (e.g., confirm deletion, confirm payment)
- Admin operation menus
- Scenarios where a player must choose an option

### Default Action When Closing via ESC

When `can_escape: true`, pressing ESC executes the actions of the corresponding button:

| Menu Type | Button Executed |
|-----------|----------------|
| `notice` | `confirm` / `button1` button |
| `confirmation` | `deny` button |
| `multi` | `exit` button |

**Example configuration:**

```yaml
Settings:
  can_escape: true

Bottom:
  type: 'confirmation'
  confirm:
    text: '&a[ Confirm ]'
    actions:
      - 'tell: &aYou chose to confirm'
  deny:
    text: '&c[ Cancel ]'
    actions:
      - 'tell: &cYou chose to cancel'  # This runs when ESC is pressed
```

---

## after_action

### Description

Defines the client-side behaviour while waiting for the server to respond after a player clicks a button.

### Background: Why Is This Parameter Needed?

There is some latency in the network communication between the server and client:
- **Normal conditions**: Latency is only a few dozen milliseconds
- **Poor network / high server load**: Latency can reach 1 second or more

During this period, if the player performs operations in the game world (such as moving items or dropping items), it may cause the subsequent menu logic to become inconsistent with the actual state, leading to unexpected behaviour.

The `after_action` parameter declares a specific operation to be performed locally on the client, preventing the player from making invalid operations during the server response period and ensuring the integrity of the menu logic.

### Values

| Value | Client Behaviour | Use Case |
|-------|-----------------|---------|
| `CLOSE` (default) | Immediately closes the menu | Menus with no sub-menus or no follow-up behaviour to handle |
| `NONE` | No local action taken | Most scenarios where all actions are controlled by the server |
| `WAIT_FOR_RESPONSE` | Displays an overlay screen while waiting for the server | Menus with sub-menus or involving critical operations |

### Configuration Example

```yaml
Settings:
  after_action: CLOSE  # Default value
  # after_action: NONE
  # after_action: WAIT_FOR_RESPONSE
```

### Detailed Description

#### 1. CLOSE (Default)

After clicking a button, the client immediately closes the menu.

**Advantages:**
- Simple and straightforward
- Smooth user experience

**Disadvantages:**
- Cannot prevent invalid operations during network latency

**Use Cases:**
- Simple menus without sub-menus
- Scenarios with no follow-up behaviour to handle

**Example:**

```yaml
Settings:
  after_action: CLOSE

Bottom:
  type: 'notice'
  confirm:
    text: '&a[ Close ]'
    actions:
      - 'close'
```

#### 2. NONE

After clicking a button, the client takes no local action; all logic is controlled by the server.

**Advantages:**
- Maximum flexibility
- Server has complete control over menu behaviour
- **Recommended for most scenarios**

**Disadvantages:**
- Requires explicitly adding a `close` action in the action list

**Use Cases:**
- Most menus (recommended as default)
- Scenarios requiring complete server control over behaviour
- Scenarios where menu closure depends on conditions

**Example:**

```yaml
Settings:
  after_action: NONE

Bottom:
  type: 'confirmation'
  confirm:
    text: '&a[ Confirm ]'
    actions:
      - condition: '%player_balance% >= 1000'
        allow:
          - 'console: eco take %player_name% 1000'
          - 'tell: &aPayment successful!'
          - 'close'  # Manually close the menu
        deny:
          - 'tell: &cInsufficient balance!'
          # Do not close the menu; let the player choose again
  deny:
    text: '&c[ Cancel ]'
    actions:
      - 'close'
```

#### 3. WAIT_FOR_RESPONSE

After clicking a button, the client displays an overlay screen and waits for the server to respond before continuing.

**Advantages:**
- Completely prevents invalid operations during network latency
- Suitable for critical operations
- Stable user experience

**Disadvantages:**
- Must ensure the menu is closed (otherwise the overlay screen will remain)
- Adds one extra waiting step

**Use Cases:**
- Menus with sub-menus (a new menu will open automatically)
- Critical operations (trades, permission changes)
- Servers with unstable networks or low TPS

{% hint style="warning" %}
**Important:**

When using `WAIT_FOR_RESPONSE`, if a button **does not open a sub-menu**, you must include a `close` action in the action list. Otherwise the client will remain on the overlay screen and the player will be unable to interact.
{% endhint %}

**Example (with sub-menu):**

```yaml
Settings:
  after_action: WAIT_FOR_RESPONSE

Bottom:
  type: 'multi'
  buttons:
    open_sub_menu:
      text: '&a[ Open Sub-Menu ]'
      actions:
        - 'open: shop/weapons'  # Opens a sub-menu, which automatically removes the overlay
    exit:
      text: '&c[ Exit ]'
      actions:
        - 'close'
```

**Example (no sub-menu — must close manually):**

```yaml
Settings:
  after_action: WAIT_FOR_RESPONSE

Bottom:
  type: 'confirmation'
  confirm:
    text: '&a[ Confirm Payment ]'
    actions:
      - 'console: eco take %player_name% 1000'
      - 'tell: &aPayment successful!'
      - 'close'  # Required: close the menu and remove the overlay
  deny:
    text: '&c[ Cancel ]'
    actions:
      - 'close'  # Required: close the menu and remove the overlay
```

## need_placeholder

### Description

Configures the list of PlaceholderAPI expansions required by the menu. Before opening the menu, the plugin checks whether the required expansions are loaded. If an expansion is not loaded:

- **Admin players (with `kamenu.admin` permission)**: A detailed prompt is shown, including the list of missing expansions and a clickable download button
- **Regular players**: A simplified prompt is shown

This feature ensures that placeholder variables in the menu work correctly, preventing display errors caused by missing expansions.

### Configuration Format

```yaml
Settings:
  need_placeholder:
    - 'player'     # Player expansion
    - 'server'     # Server expansion
    - 'vault'      # Vault expansion
```

### Values

`need_placeholder` is a list of strings, where each element represents the identifier of a PlaceholderAPI expansion.

### Configuration Examples

**Basic example:**

```yaml
Title: '&8» &6&lPlayer Info &8«'

Settings:
  need_placeholder:
    - 'player'
    - 'vault'

Body:
  message:
    type: 'message'
    text: |
      &aPlayer name: %player_name%
      &aPlayer balance: %vault_eco_balance%
      &aOnline time: %player_time_played%

Bottom:
  type: 'notice'
  confirm:
    text: '&a[ OK ]'
    actions:
      - 'close'
```

### Admin Prompt

When an admin (with `kamenu.admin` permission) tries to open a menu with missing dependencies, they will see a message similar to:

```
§cThis menu requires the following PlaceholderAPI expansions: §e[player], §e[server]
```

Each expansion name (e.g., `[player]`) is clickable:
- **Click expansion**: Automatically runs `/papi ecloud download <expansion-name>`
- **Hover**: Shows the exact download command that will be executed

### Regular Player Prompt

Regular players see a simplified message:

```
§cThis menu is missing required dependencies. Please contact an administrator.
```

(Can be customised in the language file)

{% hint style="info" %}
**How to find expansion identifiers:**
1. Use `/papi list` to view all installed expansions
2. Visit [PlaceholderAPI Expansion](https://wiki.placeholderapi.com/users/placeholder-list/minecraft/) to search for expansions
3. Check the expansion's official documentation or source code
   {% endhint %}

---


## Complete Examples

### Example 1: Regular Shop Menu (Recommended Configuration)

```yaml
Title: '&8» &6&lServer Shop &8«'

Settings:
  can_escape: true        # Allow ESC to exit
  after_action: NONE      # Controlled by server
  pause: false

Bottom:
  type: 'multi'
  buttons:
    buy:
      text: '&a[ Buy ]'
      actions:
        - 'console: give %player_name% diamond 1'
        - 'console: eco take %player_name% 100'
        - 'tell: &aPurchase successful!'
        - 'close'
    exit:
      text: '&c[ Exit ]'
      actions:
        - 'tell: &cGoodbye!'
        - 'close'
```

### Example 2: Important Confirmation Menu

```yaml
Title: '&8» &c&lConfirm Deletion &8«'

Settings:
  can_escape: false                # Disable ESC
  after_action: WAIT_FOR_RESPONSE  # Prevent latency issues
  pause: false

Bottom:
  type: 'confirmation'
  confirm:
    text: '&c[ Confirm Delete ]'
    actions:
      - condition: '%player_name% == target_player'
        allow:
          - 'tell: &aTarget item deleted'
          - 'close'  # Required: close the menu
        deny:
          - 'tell: &cYou are not the item owner!'
          - 'close'
  deny:
    text: '&a[ Cancel ]'
    actions:
      - 'tell: &aDeletion cancelled'
      - 'close'  # Required: close the menu
```

### Example 3: Admin Operation Menu

```yaml
Title: '&8» &4&lAdmin Tools &8«'

Settings:
  can_escape: false                # Disable ESC
  after_action: NONE               # Controlled by server
  pause: false

Bottom:
  type: 'multi'
  buttons:
    ban:
      text: '&c[ Ban Player ]'
      actions:
        - 'open: admin/ban_player'
    kick:
      text: '&e[ Kick Player ]'
      actions:
        - 'open: admin/kick_player'
    exit:
      text: '&7[ Back ]'
      actions:
        - 'open: main_menu'
```

### Example 4: Simple Notice Menu

```yaml
Title: '&8» &a&lNotice &8«'

Settings:
  can_escape: true        # Allow ESC
  after_action: CLOSE      # Close directly
  pause: false

Body:
  message:
    type: 'message'
    text: |
      &aWelcome to our server!
      &7Please follow the server rules and help maintain a great gaming environment.

Bottom:
  type: 'notice'
  confirm:
    text: '&a[ OK ]'
    actions:
      - 'tell: &aNotice acknowledged'
```

---

## Best Practices

### 1. Recommended Default Configuration

For most menus, the following configuration is recommended:

```yaml
Settings:
  can_escape: true
  after_action: NONE
```

### 2. Critical Operations Configuration

For menus involving critical operations (deletion, payment, permission changes, etc.):

```yaml
Settings:
  can_escape: false
  after_action: WAIT_FOR_RESPONSE
```

**Important:** When using `WAIT_FOR_RESPONSE`, ensure that all button action lists include a `close` action (unless a sub-menu is opened).

### 3. Conditional Close Configuration

When the decision to close the menu depends on a condition:

```yaml
Settings:
  can_escape: true
  after_action: NONE

Bottom:
  type: 'confirmation'
  confirm:
    text: '&a[ Confirm ]'
    actions:
      - condition: 'checkCondition'
        allow:
          - 'tell: &aOperation successful'
          - 'close'
        deny:
          - 'tell: &cOperation failed, please try again'
          # Do not close the menu
```

### 4. Network Considerations

**Servers with good network conditions:**
- Use `after_action: NONE` for most scenarios
- Simple menus can use `after_action: CLOSE`

**Servers with unstable networks or low TPS:**
- Use `after_action: WAIT_FOR_RESPONSE` for critical operations
- Ensure menu closure is handled correctly

---

## Notes

1. **Choosing after_action**
   - Use `NONE` by default for maximum flexibility
   - Use `WAIT_FOR_RESPONSE` for critical operations to ensure data consistency
   - `WAIT_FOR_RESPONSE` must be used alongside a `close` action (unless a sub-menu is opened)

2. **Using can_escape**
   - Keep `true` for regular menus to provide a better user experience
   - Set to `false` for important confirmation menus to force a choice

3. **pause parameter**
   - Only effective in single-player mode; can be ignored for multiplayer servers
   - Defaults to `false`

4. **Backward compatibility**
   - Old configuration files without a `Settings` node use default values
   - It is recommended that all new configuration files include a `Settings` node

---

## Related Documentation

- [🔘 Bottom Buttons (Bottom)](bottom.md) — Learn about button action configuration
- [🤖 Actions (Actions)](actions.md) — Learn about all available action types
- [⚙️ Events (Events)](events.md) — Learn about the events system
- [📊 PlaceholderAPI](../PlaceholderAPI.md) — Learn about PlaceholderAPI integration
