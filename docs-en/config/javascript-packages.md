# 📦 js Folder

`plugins/KaMenu/js/` stores global JavaScript packages. Use JavaScript packages for script logic shared by multiple menus, such as text formatting, complex state calculation, reward messages, game logic, and list parsing.

Global JavaScript packages can be called from `js:` actions, `{js:...}` text placeholders, and condition expressions.

---

## File Location

Runtime folder:

```text
plugins/KaMenu/js/
```

One `.js` file is one JavaScript package. The package ID is the relative path without the `.js` suffix, using `/` as the path separator:

```text
plugins/KaMenu/js/reward/message.js -> reward/message
plugins/KaMenu/js/common/format_money.js -> common/format_money
```

When the server starts and `plugins/KaMenu/js/` does not exist yet, KaMenu releases a built-in example:

```text
plugins/KaMenu/js/example/message.js -> example/message
```

If the folder already exists, KaMenu does not write example files automatically, avoiding changes to existing server files.

---

## Naming And Size Limits

Package IDs may only use letters, numbers, `_`, `-`, `.`, and `/`. They cannot contain `..`, cannot start or end with `/`, and cannot contain consecutive `//`.

Each JavaScript package file is limited to `1 MiB`. Files over the limit are skipped and a localized warning is printed to the console.

---

## File Content

The `.js` file content is JavaScript code:

```javascript
var amount = args[0] || "0";
var target = args[1] || name;
"Congratulations, " + target + " received " + amount + " coins";
```

The last expression is returned to `{js:...}`. `js:` actions do not require a return value and do not automatically display the return value to the player.

---

## Calling Packages

Call from actions:

```yaml
actions:
  - 'js: [reward/message],100,Steve'
```

Call from text:

```yaml
text: 'Message: {js:[reward/message],100,Steve}'
```

Call from conditions:

```yaml
condition: '{js:[check/vip]} == true'
```

Arguments may be separated by commas or spaces:

```yaml
- 'js: [reward/message],100,Steve'
- 'js: [reward/message] 100 Steve'
```

Wrap an argument with single quotes, double quotes, or backticks when it contains a space or comma:

```yaml
- 'js: [reward/message],"100 coins",`Steve,Alex`'
```

Inside JavaScript, read arguments with `args[0]`, `args[1]`, and so on.

---

## Lookup Priority

When calling:

```yaml
- 'js: [reward/message],100,Steve'
```

KaMenu checks:

1. Current menu `JavaScript.reward/message`
2. Global package `plugins/KaMenu/js/reward/message.js`

If a menu `JavaScript` entry and a global package use the same name, the menu script has priority.

---

## Execution Context

Each `js:`, `{js:...}`, and JavaScript package call uses an isolated execution context. Do not rely on variables or functions defined by a previous `js:` action.

Old style:

```yaml
actions:
  - 'js: var random = Math.floor(Math.random() * 100);'
  - 'js: player.sendMessage("Random number: " + random);'
```

Recommended style:

```yaml
actions:
  - 'js: var random = Math.floor(Math.random() * 100); player.sendMessage("Random number: " + random);'
```

For complex logic, prefer a menu `JavaScript` block or a global JS package:

```yaml
JavaScript:
  random_message: |
    var random = Math.floor(Math.random() * 100);
    player.sendMessage("Random number: " + random);

Bottom:
  type: notice
  confirm:
    text: '&a[ Random ]'
    actions:
      - 'js: [random_message]'
```

---

## Built-in Variables

JavaScript packages can use:

- `player`: current player object
- `uuid`: player UUID string
- `name`: player name
- `location`: player location
- `inventory`: player inventory
- `world`: player's world
- `server`: Bukkit server instance
- `args`: package argument array

---

## Helper Functions

- `tell(player, message)`: send a message
- `log(message)`: print a `[JS]` log
- `delay(ticks, callback)`: run later on the main thread
- `asyncDelay(ticks, callback)`: run later asynchronously
- `getPlayer(name)`: get a player by name
- `papi(placeholder, targetPlayer?)`: resolve PlaceholderAPI
- `kvar(variable, targetPlayer?)`: resolve KaMenu internal variables
- `data(key)` / `gdata(key)` / `meta(key)`: read data variables
- `list(key)` / `glist(key)`: read list variables, returning JSON array strings

---

## Reload

Global JavaScript packages are loaded on server startup, `/km reload`, or `/km reload js`.

Only `.js` files are loaded.

---

## Troubleshooting

**The script does not show text**

`js:` actions do not display return values. Use `tell`, `toast`, `title`, or another feedback action, or use `{js:[package]}` in text.

**JavaScript package not found**

- Check that the file is under `plugins/KaMenu/js/`
- Check that the suffix is `.js`
- Check that the package ID only uses letters, numbers, `_`, `-`, `.`, and `/`
- Check that the file size is no more than `1 MiB`
- Check that the script syntax can be compiled by the JavaScript engine; syntax errors count as failures during `/km reload js` and print console warnings
- Check that the call uses `[package_name]`
- Run `/km reload js`

**A variable does not exist in the next js action**

This is normal execution context isolation. Merge related code into one `js:` action, or move it into a menu `JavaScript` block / global JS package.
