# ⚙️ Global Settings (Settings)

The `Settings` node configures the menu's global behaviour, including how it can be closed and what happens after a button action is executed.

---

## Configuration Overview

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `can_escape` | `Boolean` | `true` | Whether players can close the menu using the ESC key |
| `after_action` | `String` | `CLOSE` | Client-side behaviour after a button action is executed |
| `lifetime` | `Long` | `300` | Maximum time the menu may remain open, in seconds |
| `need_placeholder` | `List<String>` | `null` | List of PlaceholderAPI expansions required by the menu |

---

## can_escape

### Description

Controls how the menu can be closed, determining whether players can press ESC to close it.

### Values

| Value | Description |
|-------|-------------|
| `true` (default) | Players can close the menu with ESC, triggering the corresponding bottom button actions |
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
- Menus using `Events.Tasks` where close actions and task cancellation timing must be controlled explicitly

### Button Actions Triggered by ESC

When `can_escape: true`, pressing ESC triggers bottom button actions according to the button layout:

| Menu Type | Action triggered by ESC |
|-----------|-------------------------|
| `notice` | The only button's actions |
| `confirmation` | The `deny` (cancel) button's actions |
| `multi` | The `exit` button's actions, when an `exit` button is configured |

If a menu depends on close events or periodic task lifecycle, the button triggered by ESC is already the close path, so you usually do not need to add `close` there. Use `close` or `force-close` only when you want to close the menu manually from other branches.

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
      - 'tell: &cYou chose to cancel'
      - 'close'
```

---

## lifetime

### lifetime - Menu lifetime limit

`lifetime` is measured in seconds and defaults to `300` (5 minutes). It controls both the Paper callback lifetime and KaMenu's server-side menu timeout.

When the limit is reached, KaMenu:

1. Verifies that the player is still in the original menu session, so an old timeout cannot close a newer menu;
2. Actively closes the Dialog;
3. Stops the menu's `Events.Tasks`;
4. Clears repeat pagination state;
5. Runs `Events.Close` for timeout cleanup.

The timeout is a hard limit, so `return` inside `Events.Close` cannot prevent the Dialog from closing. Values less than or equal to `0` are invalid and fall back to 300 seconds.

```yaml
Settings:
  lifetime: 300
```

All server callbacks remain single-use. Resetting or opening another menu creates new callbacks and restarts `lifetime`. The lifetime limit can close menus or waiting overlays that remain open for too long, but it does not replace proper close or refresh actions.

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
| `NONE` | No local action taken | Menus where the server explicitly closes, refreshes, or opens another menu |
| `WAIT_FOR_RESPONSE` | Displays an overlay screen while waiting for the server | Menus where the server explicitly closes, refreshes, or opens another menu after processing |

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
- Useful when closure, refresh, or navigation depends on server-side conditions

**Disadvantages:**
- Requires every button action path to explicitly close, refresh, or open a menu

**Use Cases:**
- Scenarios requiring complete server control over behaviour
- Scenarios where menu closure depends on conditions
- Scenarios where a click refreshes the current menu or opens another menu

{% hint style="warning" %}
**Important:**

When `after_action: NONE` is used, the client does not automatically close the menu, but server callbacks for regular buttons and clickable body text are always single-use. If an action finishes without closing or rebuilding the menu, later clicks no longer reach the server and the client retains an unresponsive cached Dialog.

With `after_action: NONE`, **every button and every conditional branch** must eventually run one of:
- `close` / `force-close`: close the menu
- `reset`: refresh the current menu and rebuild button callbacks
- `open` / `force-open`: open another menu

KaMenu actively closes the menu when `lifetime` expires so an invalid Dialog cannot remain indefinitely, but this is only a fallback.
{% endhint %}

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
          - 'reset'  # Re-render the menu to avoid leaving an old dialog with no callback
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
- Must ensure every button action path eventually closes, refreshes, or opens a menu; otherwise the overlay can remain
- Adds one extra waiting step

**Use Cases:**
- Menus with sub-menus (a new menu will open automatically)
- Critical operations (trades, permission changes)
- Servers with unstable networks or low TPS
- Actions that must wait for server-side validation, refresh, or navigation

{% hint style="warning" %}
**Important:**

When using `WAIT_FOR_RESPONSE`, the client enters a waiting state. As with `NONE`, button callbacks are still one-shot. If the action chain finishes without `close`, `reset`, `open` / `force-open`, or another action that closes or re-renders the dialog, the client can remain on the waiting overlay or old dialog and the player cannot continue interacting.

Therefore, when using `WAIT_FOR_RESPONSE`, make sure **every button and every conditional branch** eventually runs one of:
- `close` / `force-close`: close the menu and end the wait
- `reset`: refresh the current menu and rebuild button callbacks
- `open` / `force-open`: open another menu and end the current wait
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

**Example (no sub-menu — must explicitly finish):**

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

**Example (validation fails but the menu should stay interactive):**

```yaml
Settings:
  after_action: WAIT_FOR_RESPONSE

Bottom:
  type: 'confirmation'
  confirm:
    text: '&a[ Confirm ]'
    actions:
      - condition: 'isPosInt.$(amount)'
        allow:
          - 'tell: &aInput accepted'
          - 'close'
        deny:
          - 'toast: type=task;msg=Invalid;icon=barrier'
          - 'reset'  # Required: re-render the menu, clear the wait state, and rebuild callbacks
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

When using this configuration, every button action path must explicitly `close`, `reset`, or open another menu.

### 2. Critical Operations Configuration

For menus involving critical operations (deletion, payment, permission changes, etc.):

```yaml
Settings:
  can_escape: false
  after_action: WAIT_FOR_RESPONSE
```

**Important:** When using `WAIT_FOR_RESPONSE`, ensure that every button action path eventually `close`s, `reset`s, or opens another menu.

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
          - 'reset'  # Re-render when keeping the menu interactive
```

### 4. Network Considerations

**Servers with good network conditions:**
- Use `after_action: NONE` for most scenarios
- Simple menus can use `after_action: CLOSE`
- With `after_action: NONE`, every button path must eventually `close`, `reset`, or open another menu to avoid leaving a cached dialog with no callback

**Servers with unstable networks or low TPS:**
- Use `after_action: WAIT_FOR_RESPONSE` for critical operations
- Ensure every button path eventually `close`s, `reset`s, or opens another menu

---

## Notes

1. **Choosing after_action**
   - Use `NONE` by default for maximum flexibility
   - Use `WAIT_FOR_RESPONSE` for critical operations to ensure data consistency
   - `NONE` does not close automatically, and button callbacks are one-shot; every branch must explicitly `close`, `reset`, or `open`
   - `WAIT_FOR_RESPONSE` must also explicitly finish or rebuild interaction; every branch must `close`, `reset`, or `open`

2. **Using can_escape**
   - Keep `true` for regular menus to provide a better user experience
   - Set to `false` for important confirmation menus to force a choice
   - When using `Events.Tasks` or relying on `Events.Close`, let the ESC-mapped button own the close path; use `close` / `force-close` only for manual closure in other branches

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
