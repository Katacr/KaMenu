# 🔐 Permission Design Philosophy

KaMenu adopts a minimalist permission philosophy, keeping only a single core admin permission and delegating all access control to the configuration system.

---

## 💡 Design Philosophy

### Why Only One Permission Node?

KaMenu believes in the principle of **configuration over permissions**. Rather than using cumbersome permission nodes to control menu access, a flexible configuration system provides more precise and intuitive access control.

**Core principles:**
- **Permission system**: Used only for plugin management operations (e.g., reloading configuration)
- **Configuration system**: Used for all menu access control (commands, items, condition checks)

### Why This Approach?

#### Problems with Traditional Permission Systems

```yaml
# Traditional plugins typically require many permission nodes
menu.main.open
menu.shop.open
menu.teleport.open
menu.admin.open
# ... potentially dozens of permission nodes
```

**Problems:**
- Large number of permission nodes, difficult to manage
- Permissions are separate from menu files, scattered configuration
- Need to memorise many permission node names
- Cannot implement complex conditional access control

#### KaMenu's Configuration-Driven Design

You can add open actions directly inside a menu to check whether a player meets the required conditions:

```yaml
# Implement access control through condition checks
Events:
  open:
    actions:
      - condition: 'hasPerm.user.vip'
        allow:
          - 'tell: You have the user.vip permission. Access granted.'  # Allow menu to open
        deny:
          - 'tell: &cYou do not have the user.vip permission and cannot open this menu'
          - 'cancel'    # Block menu from opening
```

**Advantages:**
- All access control logic is centralised in the menu file
- Supports complex multi-condition checks
- No permission nodes to configure — simpler management
- More intuitive and flexible

---

## 📋 Permission Overview

| Permission Node | Description | Default Holders |
|----------------|-------------|-----------------|
| `kamenu.admin` | Allows administrative operations (e.g., reloading configuration) | OP only |

---

## 🔧 kamenu.admin Permission

### Description

Allows server administrators to perform plugin management operations:

- Grants access to all `/kamenu` subcommands

**Default state:** Only OPs have this permission

---

## 🎯 Flexible Access Control

KaMenu implements menu access control through multiple configuration methods, without relying on permission nodes.

### 1. Custom Commands — Who Can Use a Command to Open a Menu?

Register custom commands via `custom-commands` in `config.yml`. By default, all players can use them.

```yaml
custom-commands:
  menu: 'main_menu'       # /menu -> all players can run this
  shop: 'server_shop'     # /shop -> all players can run this
```

### 2. Listeners — Who Can Open a Menu via an Item or Key?

Configure listeners in `config.yml` to automatically trigger menu opening.

```yaml
listeners:
  # Triggered by the swap-hand key
  swap-hand:
    enabled: true
    menu: 'main_menu'
    require-sneaking: true  # Requires Shift + F

  # Triggered by right-clicking an item
  item-lore:
    main-menu:
      enabled: true
      material: 'CLOCK'
      target-lore: 'Menu'
      menu: 'main_menu'
      require-sneaking: false
```

**Default behaviour:**
- Any player holding the matching item or pressing the matching key will trigger the listener.


### 3. Events.open Conditions — Who Can Open a Menu?

Use the `Events.open` configuration inside a menu file to implement fine-grained access control.

#### Basic Example: Permission Check

```yaml
Events:
  open:
    actions:
      - condition: 'hasPerm.kamenu.vip'
        allow:
          - 'tell: &aYou have the kamenu.vip permission. Access granted.'  # Proceed to open menu
        deny:
          - 'tell: &cYou need the kamenu.vip permission to access this menu'
          - 'return'    # Block menu from opening
```

#### Advanced Example: Multi-Condition Check

```yaml
Events:
  open:
    actions:
      # Check multiple conditions
      - condition: 'hasPerm.kamenu.vip && %vault_eco_balance% >= 100'
        allow:
          - 'tell: &aYou have the kamenu.vip permission and 100 coins. Access granted.'
        deny:
          - 'tell: &cYou need the kamenu.vip permission and 100 coins to access this menu'
          - 'return'
```

---

## 📝 Comparison Summary

### Traditional Permission System vs KaMenu Configuration-Driven

| Feature | Traditional Permission System | KaMenu Configuration-Driven |
|---------|-------------------------------|------------------------------|
| Number of permission nodes | Many (dozens) | Minimal (only 1) |
| Configuration location | Scattered (permission plugin + plugin config) | Centralised (menu files) |
| Condition checks | Does not support complex conditions | Supports arbitrarily complex conditions |
| Flexibility | Low | High |
| Learning curve | Must memorise many permission nodes | Configuration is intuitive and easy to understand |
| Maintenance cost | High (changes require multiple config files) | Low (configuration is centralised) |


---

## 📚 Related Documentation

- [⌨️ Custom Commands](../config/customCommands.md) — Learn how to register custom commands
- [⚙️ Event System](../menu/events.md) — Learn about detailed usage of Events.open
- [🔧 Conditions](../menu/conditions.md) — Learn about all condition check methods
- [📋 Configuration File](../config/config.md) — Full reference for config.yml
