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
  # command-name: menu-id
  main: example/main_menu
  shop: example/shop_menu
  vip: example/vip_menu
  admin: example/admin_menu
```

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
/kamenu reload
```

The system will automatically re-register all custom commands — no server restart required.

## Technical Details

- Custom commands are registered automatically when the server starts
- Supports hot-reload via `/kamenu reload` — no restart needed
- Command names are case-insensitive (`/menu` and `/MENU` behave identically)
- Custom commands are independent of the main `/km` command and do not interfere with it

## Summary

The custom commands feature makes menu access simple, secure, and flexible. Used correctly, it allows you to:

1. Simplify the player workflow
2. Avoid complex permission configurations
3. Implement fine-grained access control through condition checks
4. Improve the overall user experience

Start using custom commands to make your plugin more accessible and powerful!
