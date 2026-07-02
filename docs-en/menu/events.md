# 🚥 Events

KaMenu supports executing predefined action lists at specific moments in the menu lifecycle, configured via the `Events` key. The event system allows you to run specific logic when the menu opens, closes, and at other key moments.

---

## Supported Events

| Event Name | Trigger | Supported Variables |
|-----------|---------|---------------------|
| `Open` | Before the menu opens | `{data:*}`, `{gdata:*}`, `{meta:*}`, `%papi_var%` |
| `Close` | After the menu closes | `{data:*}`, `{gdata:*}`, `{meta:*}`, `%papi_var%`, `$(input_key)` |
| `Click` | Pending action list to trigger | `{data:*}`, `{gdata:*}`, `{meta:*}`, `%papi_var%` |
| `Tasks` | Repeating task groups while the menu is open | `{data:*}`, `{gdata:*}`, `{meta:*}`, `%papi_var%`, `{js:*}` |

**Important notes:**
- The `Open` event fires before the menu opens, so it does **not support** `$(input_key)` input variables (the input fields haven't been displayed yet)
- The `Open` event waits for the full action chain to complete before opening the menu; if `return` is encountered, the menu will not open
- The `Close` event fires after the menu closes and supports all variable formats, including `$(input_key)`
- `Tasks` do not receive live input responses, so they do not support real-time `$(input_key)` values; if a task must stop exactly when the menu closes, make sure every menu-closing path executes `close` / `force-close`

---

## Basic Syntax

### Simple Event (Unconditional)

```yaml
Events:
  Open:
    - 'tell: &aWelcome to the server!'
    - 'sound: entity.player.levelup'
```

### Conditional Event

```yaml
Events:
  Open:
    - condition: "condition expression"  # Optional
      allow:
        - 'action when condition is met 1'
        - 'action when condition is met 2'
      deny:
        - 'action when condition is not met 1'
        - 'action when condition is not met 2'
```

---

## Periodic Tasks (Events.Tasks)

`Events.Tasks` runs action lists repeatedly at a fixed tick interval while the menu is open. Each task has its own execution state and can be used for status refreshes, timed checks, timeout handling, sounds, and similar workflows.

```yaml
Events:
  Tasks:
    refresh:
      mode: auto
      interval: 20
      repeat: -1
      run_immediately: true
      skip_if_running: true
      actions:
        - 'tell: &7Periodic menu refresh'
        - condition: '{data:warning} == true'
          allow:
            - 'sound: block.note_block.pling 1 1'
          deny: []
      on_end:
        - 'tell: &7Refresh task stopped'
```

### Options

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `mode` | `String` | `auto` | Trigger mode. `auto` starts when the menu opens, `manual` starts only through the `run-task` action |
| `interval` | `Long` | `20` | Repeat interval in ticks. Minimum value is `1` |
| `repeat` | `Int` | `-1` | Number of rounds. Missing, `0`, or `-1` means keep running until the menu lifecycle ends or the task is stopped |
| `run_immediately` | `Boolean` | `false` | Whether to run once immediately after the menu opens |
| `skip_if_running` | `Boolean` | `true` | Whether to skip a round if the previous action chain is still running |
| `actions` | `List` | Required | The action list to execute. Supports conditions, `wait`, and nested `actions` |
| `on_end` / `end_actions` | `List` | `[]` | Action list executed after the task lifecycle ends. Supports normal actions syntax |

### Execution Rules

- Periodic tasks start only after the menu is successfully shown. If `Events.Open` uses `return` to prevent opening, tasks do not start.
- A player has only one task lifecycle for the same `contextId`; reopening the same menu does not create duplicate running tasks with the same task ID.
- Each task ID under `Events.Tasks` is an independent running key. If that task is already running, another `run-task` call will not create a duplicate.
- `mode: auto` tasks start when the menu opens. `mode: manual` tasks start only when `run-task` is executed.
- The current menu tasks are cancelled when the player opens another menu, uses `open` / `force-open` / `reset` to another menu, uses `close` / `force-close`, leaves the server, or the plugin reloads.
- `return` inside a periodic task only stops the current round. It does not stop the repeating task. Use `stop-current-task` to stop the current task loop and interrupt the rest of the current round.
- If a task contains `wait`, keeping `skip_if_running: true` is recommended to prevent overlapping rounds.
- With `Settings.can_escape: true`, ESC triggers the corresponding bottom button action: `notice` triggers the only button, `confirmation` triggers the `deny` button, and `multi` triggers the `exit` button when it is configured. For strict lifecycle control, make sure those actions include `close` / `force-close`, or set `Settings.can_escape: false` and provide a close button.

### Task Control Actions

- `run-task: <taskId>`: Starts the specified task.
- `run-task: <taskId> <count>`: Starts the specified task and overrides the round count for this run, for example `run-task: refresh 10`.
- `run-task: *`: Starts all defined tasks in the current menu that are not already running.
- `run-task: * <count>`: Starts all tasks in the current menu and overrides the round count for this run.
- `stop-task: <taskId>`: Stops the specified task and runs its `on_end` / `end_actions`.
- `stop-task: *`: Stops all running periodic tasks in the current menu and runs each task's `on_end` / `end_actions`.
- `stop-current-task`: Only valid inside a periodic task. Stops the current task loop and immediately interrupts the rest of the current round.

### Example: Timeout Close

```yaml
Settings:
  can_escape: false

Events:
  Open:
    - 'set-meta: menu_seconds 0'

  Tasks:
    timeout:
      mode: auto
      interval: 20
      repeat: 30
      run_immediately: false
      actions:
        - 'meta: menu_seconds + 1'
        - condition: '{meta:menu_seconds} >= 30'
          allow:
            - 'tell: &cMenu timed out'
            - 'close'
            - 'stop-current-task'
      on_end:
        - 'tell: &7Timeout task ended'
```

### Example: Manually Started Task

```yaml
Events:
  Tasks:
    countdown:
      mode: manual
      interval: 20
      run_immediately: true
      actions:
        - 'tell: &eCountdown running'
      on_end:
        - 'tell: &aCountdown finished'

Bottom:
  type: multi
  buttons:
    start:
      text: '&a[ Start 10 ]'
      actions:
        - 'run-task: countdown 10'
    stop:
      text: '&c[ Stop ]'
      actions:
        - 'stop-task: countdown'
    stop_all:
      text: '&4[ Stop All ]'
      actions:
        - 'stop-task: *'
```

---

## How to Trigger a Click Action List

**First, define the action list:**

```yaml
Events:
  Click:
    hello:
      - 'tell: &aHello! Welcome to the server.'
      - 'tell: &aHope you enjoy your time here!'
```

**Activate with the `actions` action:**

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&a[ OK ]'
    actions:
      - 'actions: hello'  # Executes the `hello` action list defined above
```

### Action List Arguments

`actions:` can append arguments after the action list name. Arguments are separated by commas:

```yaml
Events:
  Click:
    greet:
      - 'tell: &aHello, {arg:0}! Welcome to &e{arg:1}&a.'

Bottom:
  type: 'notice'
  confirm:
    text: '&a[ Greet ]'
    actions:
      - 'actions: greet,player,survival server'
```

Inside the action list, use `{arg:0}`, `{arg:1}` to read arguments. Use single quotes, double quotes, or backticks around an argument when it needs to contain a comma.

**Activate with a clickable text:**

```yaml
Body:
  text:
    type: 'message'
    text: 'Please <text="click to greet";actions=hello,player,survival server;hover=Click to run the hello action> to see the welcome message'
```

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&a[ OK ]'
    actions:
      - 'hovertext: Please <text="[Click to Greet]";actions=hello,player,survival server;hover=Click to run the hello action> to see the welcome message'  
```
---

## Special Actions

### Return Action

The `return` action can be used in an event's action list to interrupt the execution of subsequent actions:

```yaml
Events:
  Open:
    - condition: 'hasPerm.kamenu.admin'
      allow:
        - 'tell: &aWelcome, admin!'
      deny:
        - 'tell: &cYou do not have permission!'
        - 'return'  # Interrupt subsequent actions
        - 'tell: You will never see this message'
```

**Notes:**
- `return` in the `Open` event **prevents the menu from opening**
- `return` in the `Close` event only interrupts the remaining actions (the menu is already closed)
- `return` is effective in both the `allow` and `deny` branches of a condition check

---

## Use Cases

### 1. Permission Checks

**Unconditional:**

```yaml
Events:
  Open:
    - 'tell: &aWelcome to the server!'
    - 'sound: entity.player.levelup'
```

**Conditional:**

```yaml
Events:
  Open:
    - condition: '%player_is_op% == true'
      allow:
        - 'tell: &aYou opened the admin menu'
      deny:
        - 'tell: &cYou do not have permission to open this menu'
        - 'return'  # Non-OP players cannot see the menu
```

### 2. Compound Condition Checks

```yaml
Events:
  Open:
    - condition: '%player_level% >= 10 && %player_health% >= 15'
      allow:
        - 'tell: &aConditions met: level >= 10 and health >= 15'
        - 'title: title=&6Welcome;subtitle=&fVIP Player;in=5;keep=40;out=10'
      deny:
        - 'tell: &7Requirements not met: need level >= 10 and health >= 15'
```

### 3. Data Initialisation

**First-visit detection:**

```yaml
Events:
  Open:
    - condition: '{data:first_visit} == null'
      allow:
        - 'tell: &eWelcome for the first time! Data initialised'
        - 'set-data: first_visit %timestamp%'
        - 'set-data: visit_count 1'
        - 'title: title=&6Welcome;subtitle=&fThis is your first visit;in=10;keep=60;out=20'
      deny:
        - 'tell: &7Welcome back! This is your {data:visit_count} visit'
        - 'set-data: visit_count {data:visit_count} + 1'
```

### 4. Status Checks

```yaml
Events:
  Open:
    - condition: '{meta:temp_banned} == true'
      allow:
        - 'tell: &cYou are temporarily banned and cannot access this menu'
        - 'return'
    - condition: '%player_health% <= 5'
      allow:
        - 'tell: &eYour health is very low. Be careful!'
```

### 5. Greeting System

```yaml
Events:
  Open:
    - condition: '%player_health% >= 20'
      allow:
        - 'tell: &aYour health is great!'
      deny:
        - 'tell: &7You should keep an eye on your health'
    - condition: '%player_level% >= 30'
      allow:
        - 'tell: &6You have reached level 30!'
```

### 6. Close Event Handling

```yaml
Events:
  Close:
    - 'tell: &aThanks for using this menu'
    - 'set-meta: last_visit %timestamp%'
    - 'set-data: total_visits {data:total_visits} + 1'
    - 'sound: entity.experience_orb.pickup'
```

---

## Complete Examples

### Example 1: Admin Panel with Permission Control

```yaml
Title: '&8» &4&lAdmin Panel &8«'

Settings:
  can_escape: false

Events:
  Open:
    - condition: 'hasPerm.kamenu.admin'
      allow:
        - 'tell: &aWelcome to the admin panel'
      deny:
        - 'tell: &cYou do not have permission to access this menu!'
        - 'sound: block.note_block.bass'
        - 'return'

Body:
  welcome:
    type: 'message'
    text: '&7Admin Control Panel - Proceed with caution'

Bottom:
  type: 'notice'
  confirm:
    text: '&c[ Close ]'
    actions:
      - 'close'
```

### Example 2: Shop Menu with Welcome System

```yaml
Title: '&8» &6&lServer Shop &8«'

Settings:
  after_action: CLOSE

Events:
  Open:
    # First-visit detection
    - condition: '{data:shop_first_visit} == null'
      allow:
        - 'tell: &aWelcome to the server shop! This is your first visit'
        - 'set-data: shop_first_visit true'
        - 'title: title=&6Welcome;subtitle=&fFirst Visit Bonus Granted;in=5;keep=60;out=20'
      deny:
        - 'tell: &7Welcome back! Keep browsing'
    
    # VIP player welcome
    - condition: 'hasPerm.vip.gold'
      allow:
        - 'tell: &6Esteemed Gold Member, welcome!'
    
    # Show balance
    - 'tell: &fCurrent balance: &e%player_balance%'

Body:
  welcome:
    type: 'message'
    text: '&7Please select the items you need'

Bottom:
  type: 'notice'
  confirm:
    text: '&a[ Browse Items ]'
    actions:
      - 'tell: &aBrowsing items...'
```

### Example 3: Survey Menu with Data Tracking

```yaml
Title: '&8» &b&lPlayer Survey &8«'

Settings:
  can_escape: false
  after_action: WAIT_FOR_RESPONSE

Events:
  Open:
    # Check if already completed
    - condition: '{data:questionnaire_completed} == true'
      allow:
        - 'tell: &aYou have already completed the survey. Thank you!'
        - 'return'
    
    - 'tell: &ePlease fill out the survey below'
  
  Close:
    # Mark as completed
    - 'set-data: questionnaire_completed true'
    - 'set-data: questionnaire_date %timestamp%'
    - 'tell: &aThank you for completing the survey!'
    - 'title: title=&aCompleted;subtitle=&fThank you for your participation;in=5;keep=40;out=10'

Inputs:
  rating:
    type: 'slider'
    text: 'Server Rating (1-10)'
    min: 1
    max: 10
    default: 5
  
  feedback:
    type: 'input'
    text: 'Comments or Suggestions'
    multiline:
      max_lines: 5
      height: 100
    max_length: 500

Bottom:
  type: 'confirmation'
  confirm:
    text: '&a[ Submit ]'
    actions:
      - 'tell: &aSubmitted successfully!'
      - 'close'
  deny:
    text: '&c[ Cancel ]'
    actions:
      - 'tell: &cSubmission cancelled'
      - 'close'
```

### Example 4: Multi-Condition Check Menu

```yaml
Title: '&8» &5&lVIP Exclusive Features &8«'

Settings:
  can_escape: false

Events:
  Open:
    # Multiple checks
    - condition: '%player_level% < 30'
      allow:
        - 'tell: &cYour level is below 30'
        - 'return'
    
    - condition: '!hasPerm.vip.basic'
      allow:
        - 'tell: &cYou need VIP permission to access this menu'
        - 'return'
    
    - condition: '%player_health% < 10'
      allow:
        - 'tell: &eYour health is low. Stay safe!'
    
    # Passed all checks
    - 'tell: &aWelcome to VIP Exclusive Features'
    - 'sound: entity.player.levelup'

Body:
  welcome:
    type: 'message'
    text: '&6VIP Exclusive Feature Area'

Bottom:
  type: 'notice'
  confirm:
    text: '&a[ OK ]'
    actions:
      - 'tell: &aStarting VIP features'
```

---

## Notes

1. **Execution Order**
   - The `Open` event runs before the menu is parsed
   - The `Close` event runs after the menu is closed
   - Event action lists are executed in order

2. **Scope of Return**
   - `return` in the `Open` event prevents the entire menu from opening
   - `return` in the `Close` event only interrupts the remaining actions
   - `return` is effective inside nested condition checks

3. **Variable Limitations**
   - The `Open` event does not support `$(input_key)` (input fields not yet displayed)
   - The `Close` event supports all variable formats
   - Condition checks support all built-in methods and operators

4. **Performance Considerations**
   - Avoid executing large numbers of time-consuming operations in events
   - Use `return` to short-circuit unnecessary logic
   - Use parentheses to clarify priority for complex condition checks

5. **Error Handling**
   - A failed condition check does not prevent the menu from opening (unless `return` is used)
   - A failed action does not prevent subsequent actions from executing
   - Use the `tell` action to provide feedback messages

---

## Related Documentation

- [🔍 Conditions](conditions.md) — Learn the detailed syntax for condition expressions
- [🤖 Actions (Actions)](actions.md) — Learn about all available action types
- [💾 Data Storage](../data/storage.md) — Learn about data storage and variable usage
