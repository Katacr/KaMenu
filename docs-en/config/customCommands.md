# Custom Commands

KaMenu provides a powerful custom command feature that lets you create dedicated, concise open-commands for each menu, greatly improving the player experience.

## Why Use Custom Commands?

### ❌ The Problem with the Traditional Approach

Opening menus via `/kamenu open <menu-id>` has several drawbacks:

- **Verbose commands**: Players must type the full menu path, which is hard to memorise
- **Permission issues**: Players need the `kamenu.admin` permission, giving them access to every menu — a security risk
- **Complex management**: Restricting certain players to certain menus requires a complicated permission setup

### ✅ Advantages of Custom Commands

Using registered commands to open menus instead:

- **Short and simple**: Each menu gets its own short command such as `/main` or `/shop` — easy to remember
- **Flexible access control**: No permission nodes required; all players can see and use the commands
- **Condition-based control**: Use the `Events.Open` event to check conditions and decide who can actually open the menu

## Configuring Custom Commands

Configure custom commands in `config.yml`:

```yaml
custom-commands:
  # Legacy form: command-name: menu-id
  main: example/main_menu
  shop: example/shop_menu
  vip: example/vip_menu
  admin: example/admin_menu

  # Action form: run an actions list
  test:
    args:
      0: "[hello, info]"
      1: "%kamenu_online_players%"
    actions:
      - "tell: Hey, you ran /test"
      - "tell: Arguments: {args}"
      - "sound: entity.experience_orb.pickup;volume=1.0;pitch=1.3"
      - "tell: What would you like to test?"
```

The `actions` form uses the same action-list behavior as button actions. It supports normal actions, conditional branches, nested action lists, `wait`, `return`, target selectors, and complex conditions.

```yaml
custom-commands:
  reward:
    actions:
      - condition: "hasPerm.reward.daily && {data:daily_reward} != true"
        allow:
          - "money: type=add;num=100"
          - "data: type=set;key=daily_reward;var=true"
          - "toast: type=task;msg=Claimed;icon=emerald"
        deny:
          - "toast: type=task;msg=Denied;icon=barrier"
          - "return"
```

Custom command actions can read command arguments:

- `{arg:0}`: first argument
- `{arg:1}`: second argument
- `{args}`: full argument text
- `{arg_count}`: argument count
- `{command}`: the command label that was used

```yaml
custom-commands:
  greet:
    actions:
      - "tell: &aHello {arg:0}, welcome to {arg:1}"
```

## Argument Tab Completion

Object-form custom commands can define `args` to provide Tab completion candidates for command arguments. `args` indexes start at `0`, matching `{arg:0}`, `{arg:1}`, and other action variables.

```yaml
custom-commands:
  test2:
    args:
      0: "[tp, tphere]"
      1: "%kamenu_online_players%"
    actions:
      - condition: "{arg:0} == tp"
        allow:
          - "tell: You will teleport to {arg:1}"
      - condition: "{arg:0} == tphere"
        allow:
          - "tell: You will teleport {arg:1} to you"
```

Candidate sources support these forms:

```yaml
args:
  0: "[tp, tphere]"              # simple list
  1: "Steve, Alex, Katacr"       # comma-separated text
  2:
    - spawn
    - home
    - shop
  3: "%kamenu_online_players%"   # PAPI, resolved when the player presses Tab
  4: "{list:friends}"            # KaMenu player list, resolved when Tab is pressed
  5: "{glist:warps}"             # KaMenu global list, resolved when Tab is pressed
```

Menu-opening object commands can also define argument completion:

```yaml
custom-commands:
  profile:
    menu: example/player_profile
    args:
      0: "%kamenu_online_players%"
```

Tab completion candidates are not cached. PAPI placeholders and KaMenu built-in variables are resolved in real time each time a player presses Tab.

## Example: Restricting Access to Specific Players

Suppose you only want VIP players to open the VIP menu. No permission nodes are needed — just add a condition check in the menu's `Events.Open` event:

```yaml
Events:
  Open:
    - condition: "hasPerm.user.vip"
      actions:
        - 'tell: Welcome to the VIP menu!'
      deny:
        - 'tell: §cYou do not have the user.vip permission to access this menu'
        - 'return'
```

With this configuration:
- ✅ All players can try running `/vip`
- ✅ Only players with the `vip` permission will successfully open the menu
- ✅ Other players receive a message and the menu will not open

## Use Cases

### Scenario 1: Quick Access to Common Menus

Create short commands for frequently used menus:

```yaml
custom-commands:
  menu: example/main_menu      # Main menu
  warp: example/warp_menu      # Warp menu
  shop: example/shop_menu      # Shop menu
  bank: example/bank_menu      # Bank menu
```

Players simply type `/menu`, `/warp`, etc. to access them instantly.

### Scenario 2: Privileged Menus with Conditional Access

Create special menus for VIPs, admins, and so on:

```yaml
custom-commands:
  vip: example/vip_menu
  admin: example/admin_menu
  owner: example/owner_menu
```

Then check the player's permissions in each menu's `Events.Open` event to implement conditional access.

### Scenario 3: Feature-Specific Entry Points

Create dedicated entry points for specific features:

```yaml
custom-commands:
  daily: example/daily_reward     # Daily sign-in
  mail: example/mail_system        # Mail system
  quest: example/quest_system      # Quest system
```

### Scenario 4: Lightweight Commands without Menus

Run a small action list directly without creating a menu file:

```yaml
custom-commands:
  ping:
    actions:
      - "sound: block.note_block.pling;volume=1.0;pitch=1.4"
      - "toast: type=task;msg=Received;icon=bell"
```

## Best Practices

### 1. Naming Conventions

- Use short, memorable English names
- Avoid special characters
- Lowercase is recommended for better compatibility

```yaml
# ✅ Recommended
custom-commands:
  menu: ...
  shop: ...
  vip: ...

# ❌ Not recommended
custom-commands:
  @shop: ...           # Special characters may conflict
  VERYLONGNAME: ...    # Too long to type comfortably
  help: ...         # Try to avoid conflicts with other plugins
```

### 2. Condition Check Recommendations

When using condition checks in `Events.Open`:

- Provide clear, informative messages
- Use the `return` action to prevent ineligible players from opening the menu
- Combine `data` or `meta` for more flexible access control

```yaml
Events:
  Open:
    # Multi-condition check
    - condition: "hasPerm.user.vip && %player_level% >= 10"
      actions:
        - 'tell: Welcome to the VIP menu'
      deny:
        - 'tell: §cYou need the user.vip permission and level 10 or above to access this menu'
        - 'return'
```

### 3. Reloading Commands

After modifying `config.yml`, run the following command to reload:

```
/kamenu reload config
```

The system will automatically re-register all custom commands — no server restart required.

## Technical Details

- Custom commands are registered automatically when the server starts
- Supports hot-reload via `/kamenu reload config` — no restart needed
- Command names are case-insensitive (`/menu` and `/MENU` behave identically)
- Custom commands are independent of the main `/km` command and do not interfere with it
- String values open menus directly; a command section with an `actions` list runs the action queue
- Action commands do not have a current menu context, so actions such as `reset` or `actions: Events.ClickAction` that depend on menu config are not suitable here. Use `open: menuId` when a command should open a menu.

## Summary

The custom commands feature makes menu access simple, secure, and flexible. Used correctly, it allows you to:

1. Simplify the player workflow
2. Avoid complex permission configurations
3. Implement fine-grained access control through condition checks
4. Improve the overall user experience

Start using custom commands to make your plugin more accessible and powerful!
