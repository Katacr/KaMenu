# 🎬 Menu Title

The `Title` node defines the title text displayed at the top of the menu.

---

## Basic Usage

**Type:** `String`

**Format:** Supports color codes (`&` symbol) and PlaceholderAPI variables

**Examples:**

```yaml
Title: '&6&lServer Main Menu'
```

```yaml
Title: '&aWelcome, &f%player_name%!'
```

```yaml
Title: '&8» &d&lShop System &8«'
```

---

## Conditional Title

`Title` supports the condition check format, allowing different titles to be displayed for players in different states:

```yaml
Title:
  - condition: "%player_is_op% == true"
    allow: '&8» &4&lAdvanced Admin Panel &8« &7[Admin]'
    deny: '&8» &6&lPlayer Panel &8«'
```

```yaml
Title:
  - condition: "%player_level% >= 20"
    allow: '&8» &6&lVIP Zone &8«'
    deny: '&8» &7&lGeneral Area &8«'
```

For the complete conditional syntax, refer to [🔍 Conditions](conditions.md).

---

## Notes

* Title length is limited; overly long titles may be truncated (keep it within 32 characters)
* Supports PlaceholderAPI variables (requires the PlaceholderAPI plugin)
* Supports built-in data variables: `{data:key}` and `{gdata:key}`
