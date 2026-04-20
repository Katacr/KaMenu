# 📝 Creating a Menu Tutorial

This tutorial walks you through creating a complete menu from scratch, helping you quickly master KaMenu's menu-building workflow.

---

## 🎯 Learning Goals

By the end of this tutorial, you will be able to:
- ✅ Create the basic structure of a menu file
- ✅ Configure the menu title and settings
- ✅ Add text content
- ✅ Add item displays
- ✅ Add input components
- ✅ Configure buttons and actions
- ✅ Use condition checks

---

## 📄 Step 1: Create the Menu File

### 1.1 File Location

Create a new file under `plugins/KaMenu/menus/` with a meaningful name:

```
plugins/KaMenu/menus/
└── my_first_menu.yml    # Create this file
```

### 1.2 Basic Structure

To create a menu, you first need to understand its structure. A complete menu file typically contains four sections — `Title`, `Body`, `Inputs`, and `Bottom` — to display content. In addition, `Settings` and `Events` are available to extend the menu's functionality.

#### Parameter Reference

| Parameter | Description | Required | Documentation |
|-----------|-------------|----------|---------------|
| `Title` | Title text displayed at the very top of the menu | ✅ | [🎬 Menu Title (Title)](layout.md) |
| `Body` | Area for displaying information | | [🧩 Body Components (Body)](body.md) |
| `Inputs` | Area for creating input components | | [⌨️ Input Components (Inputs)](inputs.md) |
| `Bottom` | Bottom button area | ✅ | [📋 Bottom Buttons (Bottom)](bottom.md) |
| `Settings` | Options for configuring the menu | | [⚙️ Global Settings (Settings)](setting.md) |
| `Events` | Menu open/close actions | | [🎯 Events (Events)](events.md) |


{% hint style="info" %}
Did you know?
Actually all sections are optional. You can create an empty file and KaMenu will still be able to open it — but that would serve no purpose.
{% endhint %}

---
Creating my first menu:
```yaml
# Menu title
Title: '&6My First Menu'

# Optional: global settings
Settings:
  can_escape: true
  after_action: CLOSE

# Optional: actions executed when the menu opens
Events:
  Open:
    - 'tell: &aWelcome to the menu!'

# Optional: content display area
Body:
  ...

# Optional: input component area
Inputs:
  ...

# Bottom button area
Bottom:
  ...
```

**Current state:** You now have a basic empty menu framework!

---

## 📝 Step 2: Add Text Content

### 2.1 Add a Welcome Message

Add a text component inside the `Body` node:

```yaml
Title: '&6My First Menu'

Body:
  welcome:
    type: 'message'
    text:
      - '&7Welcome to the server shop'
      - '&7Click the button below to browse items'
```

### 2.2 Add a Separator and Tips

```yaml
Body:
  welcome:
    type: 'message'
    text:
      - '&7Welcome to the server shop'
      - '&7Click the button below to browse items'

  separator:
    type: 'message'
    text: '&8————————————————'

  tips:
    type: 'message'
    text:
      - '&eTips:'
      - '&7- Make sure you have enough balance before purchasing'
      - '&7- Contact an admin if you have any issues'
```

**Current state:** Your menu now displays a welcome message and some helpful tips.

---

## 🎨 Step 3: Add Item Displays

### 3.1 Add a Single Item

```yaml
Body:
  # ... previous text content ...

  diamond_item:
    type: 'item'
    material: 'DIAMOND'
    name: '&6&lDiamond'
    lore:
      - '&7A precious gemstone'
    description: '&fClick the button below to obtain'
```

### 3.2 Add Multiple Items

```yaml
Body:
  # ... previous text content ...

  diamond_item:
    type: 'item'
    material: 'DIAMOND'
    name: '&6&lDiamond'
    lore:
      - '&7A precious gemstone'
    description: '&fClick the button below to obtain'

  iron_ingot:
    type: 'item'
    material: 'IRON_INGOT'
    name: '&f&lIron Ingot'
    lore:
      - '&7A common metal'
    description: '&fClick the button below to obtain'

  gold_ingot:
    type: 'item'
    material: 'GOLD_INGOT'
    name: '&e&lGold Ingot'
    lore:
      - '&7A rare metal'
    description: '&fClick the button below to obtain'
```

**Tip:** The material name supports multiple formats, such as `diamond_sword`, `Diamond-Sword`, `diamond sword`, etc.

**Current state:** Your menu now shows three items, each with a name and description.

---

## 🎯 Step 4: Add Input Components

### 4.1 Add a Quantity Slider

```yaml
Inputs:
  diamond_amount: 
    type: 'slider'
    text: '&aSelect the number of diamonds to receive'
    min: 1
    max: 64
    default: 1
```

### 4.2 Add More Input Components

```yaml
Inputs:
  diamond_amount:
    type: 'slider'
    text: '&aSelect the number of diamonds to receive'
    min: 1
    max: 64
    default: 1

  note:
    type: 'input'
    text: '&7Note'
    default: ''
    placeholder: 'Optional...'
```
**Current state:** Players can now select a quantity and add an optional note.


Once you have created `Inputs` components, each component maps to an internal key. Using the menu above as an example: the value of the `diamond_amount` slider will be available in actions as `$(diamond_amount)`, and the value of the `note` text field will be available as `$(note)`.


---

## 🔘 Step 5: Add Bottom Buttons

### 5.1 Simple Confirm Button

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&a[ Click to Receive ]'
    actions:
      - 'item: type=give;mats=DIAMOND;amount=$(diamond_amount)' 
      - 'tell: &aSuccess! Received $(diamond_amount) item(s)'
      # Uses the built-in key $(diamond_amount), which resolves to the slider value.
      - 'sound: entity.player.levelup'
```

### 5.2 Full Button Configuration

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&a[ Click to Receive ]'
    actions:
      - 'item: type=give;mats=DIAMOND;amount=$(diamond_amount)'
      - 'tell: &aSuccess! Received $(diamond_amount) item(s)'
      # Uses the built-in key $(diamond_amount), which resolves to the slider value.
      - 'tell: &aYour note: $(note)'
      # Uses the built-in key $(note), which resolves to the text field value.
      - 'sound: entity.player.levelup'
  cancel:
    text: '&c[ Cancel ]'
    actions:
      - 'tell: &7Purchase cancelled'
      - 'close'
```

**Current state:** Your menu now has confirm and cancel buttons that execute the corresponding actions when clicked.

---

## 🎨 Step 6: Use Condition Checks

### 6.1 Display Different Content Based on Player State
In KaMenu, most areas support condition checks to display customised content.

- Normal mode:
```yaml
Body:
  player_status:
    type: 'message'
    text: 'Hello, welcome to the server!'
```
- Using condition checks
```yaml
Body:
  player_status:
    type: 'message'
    text:
      - condition: '%player_is_op% == true' 
        allow: '&6Hello, Admin! Welcome to the server!'
        deny: '&7Hello, Player! Welcome to the server!'
```

### 6.2 Using Conditions in Actions

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&a[ Confirm ]'
    actions:
      - condition: "%player_level% >= 10"
        allow:
          - 'tell: Your level is 10 or above. Proceeding...'
            ...
        deny:
          - 'tell: Your level is below 10. Cannot proceed...'
            ...
```

**Current state:** Your menu now responds intelligently to the player's state.

---

## 🚀 Test Your Menu

### 7.1 Reload the Plugin

```bash
/km reload
```

### 7.2 Open the Menu

```bash
/km open my_first_menu
```

Or use Tab completion:

```bash
/km open <press Tab>
```

### 7.3 Test the Features

- ✅ Check that the menu opens correctly
- ✅ Check that text and items are displayed correctly
- ✅ Test that the slider can be dragged
- ✅ Test that button clicks work correctly
- ✅ Test that condition checks take effect

---

## 📚 Advanced Topics

Once you have mastered the basics, you can explore more advanced features:

### Layout and Styling
- [🧩 Body Components (Body)](body.md) — Learn about all available component types
- [⌨️ Input Components (Inputs)](inputs.md) — Explore the various input components
- [📋 Bottom Buttons (Bottom)](bottom.md) — Customise button layouts

### Interaction and Actions
- [🤖 Actions (Actions)](actions.md) — Learn all available actions
- [❓ Conditions (Conditions)](conditions.md) — Master condition checks

### Other Features
- [⚙️ Global Settings (Settings)](setting.md) — Configure menu behaviour
- [🎯 Events (Events)](events.md) — Respond to menu events
- [🗄️ Data Storage (Storage)](../data/storage.md) — Use the database to store data

---

## 🎉 Congratulations!

You have successfully created your first KaMenu menu!

Keep exploring more features and possibilities to build richer menu experiences!

If you have any questions, refer to the detailed documentation or reach out to the community for support.
