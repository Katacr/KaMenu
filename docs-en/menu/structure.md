# 📁 Menu File Structure

## 📂 Folder Layout

All menu files are stored under `plugins/KaMenu/menus/`, supporting any level of subfolder nesting:

```
plugins/KaMenu/menus/
├── main_menu.yml           # Root-level menu
├── server_shop.yml         # Root-level menu
├── example/                # Example folder
│   └── actions_demo.yml    # Demo menu
├── shop/                   # Shop folder
│   ├── main.yml            # Shop main menu
│   ├── weapons.yml         # Weapons shop
│   └── armor.yml           # Armor shop
└── admin/                  # Admin tools folder
    └── tools.yml           # Admin tools
```

---

## 🎯 Menu ID Rules

A menu's ID is determined by its file path:

- **Root-level menus**: Use the file name without the `.yml` extension
  ```
  /km open main_menu
  /km open server_shop
  ```

- **Subfolder menus**: Use the relative path with `/` as the separator
  ```
  /km open example/actions_demo
  /km open shop/weapons
  /km open admin/tools
  ```

---

## ✏️ Adding Custom Menus

1. Create a `.yml` file under `plugins/KaMenu/menus/` (create subfolders as needed)
2. Write the menu configuration following the menu format (see the following sections)
3. Run `/km reload` to reload

**File naming notes:**
- ✅ Supports Unicode file names and folder names
- ⚠️ The file extension must be `.yml` (not `.yaml`)
- ⚠️ Use `/` as the path separator, not `\`

---

## 📝 Tab Completion

After typing `/km open `, press Tab to automatically list all loaded menu IDs, including subfolder paths:

```
demo
server_shop
example/actions_demo
shop/weapons
admin/tools
```

---

## 📄 Basic Menu File Structure

A complete menu YAML file has the following basic structure:

```yaml
# Menu title (supports color codes and condition checks)
Title: '&6Menu Title'

# Optional: global settings
Settings:
  can_escape: true      # Whether pressing ESC closes the menu
  after_action: CLOSE   # Client behavior after a button action is executed
  
# Optional: pre-defined JavaScript functions
JavaScript:
  test: |
    player.sendMessage("§aHello, " + name + "!");
    
# Optional: menu events
Events:
  Open:                # Actions executed when the menu opens
    - 'tell: &aWelcome!'

# Optional: content display area (plain text, item display, etc.)
Body:
  ...

# Optional: input component area (text fields, sliders, dropdowns, checkboxes)
Inputs:
  ...

# Optional: bottom button area (confirm/cancel/multi-button)
Bottom:
  type: 'notice'       # notice | confirmation | multi
  ...
```

{% hint style="info" %}
Only the `Title` node is required; all other nodes are optional. Add whichever features you need.
{% endhint %}

---

## 🎨 Menu Node Descriptions

### Title — Menu Title

The required top-level node that defines the title displayed at the top of the menu.

**Format:**
- Single-line text: `Title: '&6Menu Title'`
- Conditional: Supports showing different titles based on conditions

**Examples:**
```yaml
Title: '&6Shop'

# With condition check
Title:
  - condition: "%player_is_op% == true"
    allow: '&4Admin Shop'
    deny: '&6Regular Shop'
```

### Settings — Global Settings

Configures the menu's global behaviour.

**Options:**

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `can_escape` | `Boolean` | `true` | Whether players can close the menu using the ESC key |
| `after_action` | `String` | `CLOSE` | Client-side behaviour after a button action is executed |

**Detailed description and examples:** See [⚙️ Global Settings (Settings)](setting.md)

### JavaScript — Pre-defined Functions

Define JavaScript code functions that can be called from actions.

**Example:**

```yaml
JavaScript:
  show_health: |
    var health = player.getHealth();
    var maxHealth = player.getMaxHealth();
    player.sendMessage("§eHealth: §f" + health + "/" + maxHealth);
```

**Calling in actions**

```yaml
actions:
  - 'js: [show_health]'
```

**Detailed description and examples:** See [🔧 JavaScript Pre-defined Functions (JavaScript)](javascript.md)


### Events — Menu Events

Defines actions that execute at specific moments during the menu's lifecycle.

**Supported events:**

| Event Name | Trigger |
|-----------|---------|
| `Open` | When a player opens the menu |
| `Close` | When a player closes the menu |

**Example:**
```yaml
Events:
  Open:
    - 'tell: &aWelcome to the menu!'
    - 'sound: entity.experience_orb.pickup'
  Close:
    - 'tell: &7Goodbye!'
```

**Detailed description and examples:** See [🎯 Menu Events (Events)](events.md)

### Body — Content Display Area

Displays various content in the main body of the menu, such as plain text messages and item displays.

**Component types:**
- `message` — Plain text message
- `item` — Item display

**Detailed description and examples:** See [🧩 Body Components (Body)](body.md)

### Inputs — Input Component Area

Provides interactive input components such as text fields, sliders, dropdowns, and more.

**Component types:**
- `input` — Text input field
- `slider` — Slider
- `dropdown` — Dropdown selection box
- `checkbox` — Checkbox

**Detailed description and examples:** See [⌨️ Input Components (Inputs)](inputs.md)

### Bottom — Bottom Button Area

Configures the buttons at the bottom of the menu, supporting multiple layout types.

**Layout types:**
- `notice` — Notice type (single confirm button)
- `confirmation` — Confirmation type (confirm and cancel buttons)
- `multi` — Multi-button type (multiple custom buttons)

**Detailed description and examples:** See [📋 Bottom Buttons (Bottom)](bottom.md)

---

## 🚀 Next Steps

Now that you understand the menu file structure, you can:

1. **Create your first menu**: See [📝 Creating a Menu Tutorial](creating_menu.md)
2. **Dive into each component**: Read the corresponding detailed documentation
3. **Explore advanced features**: Conditions, data storage, actions system, and more

{% hint style="success" %}
It is recommended to start with the [📝 Creating a Menu Tutorial](creating_menu.md) and follow it step by step to create your first menu!
{% endhint %}
