# 🔧 JavaScript Features

KaMenu has a built-in powerful JavaScript engine (based on OpenJDK Nashorn 15.3), allowing you to use JavaScript code inside menus to perform various complex operations.

---

## ✨ Features

- ✅ **Ready to use**: No extra plugins required
- ✅ **Auto-download**: Required dependencies are downloaded automatically on first startup
- ✅ **Bukkit API integration**: Direct access to the Bukkit API
- ✅ **Variable bindings**: Player-related variables are automatically bound
- ✅ **JavaScript packages**: Supports reusable scripts in menu `JavaScript` and global `plugins/KaMenu/js/`
- ✅ **Parameter passing**: Supports passing multiple parameter types to JavaScript packages

---

## 🔧 Basic Usage

### Executing JavaScript Directly

Use the `js:` prefix inside `actions` to execute JavaScript code directly:

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&aTest JS'
    actions:
      - 'js: player.sendMessage("Hello from JavaScript!");'
      - 'js: var random = Math.floor(Math.random() * 100); player.sendMessage("Random number: " + random);'
```

### Using `{js:...}` as a text placeholder

`{js:...}` can be written anywhere in text, and KaMenu will replace it with the return value of the JavaScript expression:

```yaml
Title: '&6Result: {js:player.getLevel()}'

actions:
  - 'tell: Congratulations, player {js:name} got a bonus package'
  - 'tell: Level check result: {js:player.getLevel() >= 10}'
```

**Notes:**
- `{js:...}` is for value output only, not side effects
- If the script returns nothing or fails, it is replaced with an empty string
- It can be used in titles, text, conditions, and action parameters

---

## 📦 Built-in Variables

The following variables can be used directly in JavaScript code:

| Variable | Type | Description |
|----------|------|-------------|
| `player` | Player | The current player object |
| `uuid` | String | The player's UUID string |
| `name` | String | The player's name |
| `location` | Location | The player's location |
| `inventory` | Inventory | The player's inventory |
| `world` | World | The world the player is in |
| `server` | Server | The server instance |

**Example:**
```yaml
actions:
  - 'js: player.sendMessage("Welcome, " + name + "!");'
  - 'js: player.sendMessage("UUID: " + uuid);'
  - 'js: player.sendMessage("World: " + world.getName());'
```

---

## 🔌 Built-in Helper Functions

The following helper functions are pre-loaded in the JavaScript environment:

### `tell(player, message)`
Sends a message to a player

```javascript
tell(player, "Hello!");
```

### `log(message)`
Prints a message to the console

```javascript
log("Player clicked the button");
```

### `delay(ticks, callback)`
Executes a callback function after a delay (main thread)

```javascript
delay(20, function() {
    player.sendMessage("Executed after 1 second");
});
```

### `asyncDelay(ticks, callback)`
Executes a callback function asynchronously after a delay

```javascript
asyncDelay(20, function() {
    log("Async task completed");
});
```

### `getPlayer(name)`
Gets a player by name

```javascript
var target = getPlayer("Steve");
```

### `papi(placeholder, targetPlayer)`
Resolves a PlaceholderAPI variable. `targetPlayer` is optional; if omitted, the current `player` is used.

Both `%...%` and plain placeholder names are supported:

```javascript
var playerName = papi("player_name");
var level = parseInt(papi("%player_level%"));
var onlinePlayers = JSON.parse(papi("kamenu_online_players"));
var targetLevel = papi("player_level", getPlayer("Steve"));
```

Returns an empty string if PlaceholderAPI is not installed or parsing fails.

### `kvar(variable, targetPlayer)`
Resolves KaMenu internal variables. `targetPlayer` is optional; if omitted, the current `player` is used.

Both `{...}` and plain variable names are supported:

```javascript
var coins = data("coins");
var serverStatus = gdata("server_status");
var tempChoice = meta("temporary_choice");
var friends = JSON.parse(list("friends"));
var servers = JSON.parse(glist("servers"));
var raw = kvar("{gdata:server_status}");
```

Convenience helpers:

- `data(key, targetPlayer)`: same as `kvar("data:" + key, targetPlayer)`
- `gdata(key, targetPlayer)`: same as `kvar("gdata:" + key, targetPlayer)`
- `meta(key, targetPlayer)`: same as `kvar("meta:" + key, targetPlayer)`
- `list(key, targetPlayer)`: same as `kvar("list:" + key, targetPlayer)`, returns a JSON array string
- `glist(key, targetPlayer)`: same as `kvar("glist:" + key, targetPlayer)`, returns a JSON array string

#### Reading Lists in JavaScript

`list()` and `glist()` read KaMenu built-in list data:

- `list("friends")`: reads the current player's `friends` list
- `glist("servers")`: reads the global shared `servers` list
- The second argument can be a target player object, for example `list("friends", getPlayer("Steve"))`
- The return value is a JSON array string, so it is usually parsed with `JSON.parse(...)`

```javascript
var friends = JSON.parse(list("friends"));
if (friends.indexOf("Steve") >= 0) {
    tell(player, "Steve is already in your friend list");
}

var servers = JSON.parse(glist("servers"));
for (var i = 0; i < servers.length; i++) {
    log("Server: " + servers[i]);
}
```

You can also use `{js:...}` to output list counts or boolean checks:

```yaml
Body:
  info:
    type: message
    text:
      - '&aFriend count: {js:JSON.parse(list("friends")).length}'
      - '&eContains Steve: {js:JSON.parse(list("friends")).indexOf("Steve") >= 0}'
```

{% hint style="info" %}
`list()` / `glist()` only read variables and do not modify lists. To write list data, use `list:` / `glist:` actions. For YAML condition membership checks, prefer `inList` / `inGlist`.
{% endhint %}

```javascript
var target = getPlayer("Steve");
if (target) {
    tell(target, "Hello!");
}
```

---

## 📝 JavaScript Packages

### Defining Menu-local JavaScript Packages

Add a `JavaScript` node at the top level of the menu configuration file to define reusable JavaScript code blocks:

```yaml
Title: '&6JavaScript Example Menu'

# Menu-local JavaScript package area
JavaScript:
  show_health: |
    var health = player.getHealth();
    var maxHealth = player.getMaxHealth();
    player.sendMessage("§eHealth: §f" + health + "/" + maxHealth);

  greet: |
    player.sendMessage("§aHello, " + name + "!");

  pass_args: |
    var playerName = args[0];
    var playerLevel = args[1];
    var dataValue = args[2];
    var inputValue = args[3];

    player.sendMessage("§aHello, " + playerName + "!");
    player.sendMessage("§aLevel: §f" + playerLevel);
    player.sendMessage("§aData: §f" + dataValue);
    player.sendMessage("§aInput: §f" + inputValue);

Body:
  ...
```

{% hint style="info" %}
Each key inside the `JavaScript` node is a menu-local package name, and the value is the JavaScript code (use `|` for multi-line strings). Calls resolve the current menu package first, then global packages under `plugins/KaMenu/js/`.
{% endhint %}

### Calling JavaScript Packages

#### Method 1: Direct Call (No Arguments)

Use the `[package_name]` format to call a code block in the current menu's `JavaScript` section, or a global `.js` file under `plugins/KaMenu/js/`.

Lookup priority:

1. Current menu `JavaScript.<package_name>`
2. Global JavaScript package `plugins/KaMenu/js/<package_name>.js`

```yaml
Bottom:
  type: 'multi'
  buttons:
    test-functions:
      text: '&aTest JavaScript Packages'
      actions:
        - 'js: [show_health]'
        - 'js: [greet]'
```

#### Method 2: Passing Arguments

Add arguments after the package name. Both comma-separated and space-separated arguments are supported:

```yaml
Bottom:
  type: 'multi'
  buttons:
    pass-args:
      text: '&cPass Arguments'
      actions:
        - 'js: [pass_args],%player_name%,50,{data:test},default_value'
        - 'js: [pass_args] %player_name% 50 {data:test} default_value'
```

When an argument needs to contain a space or comma, wrap it with single quotes, double quotes, or backticks:

```yaml
- 'js: [reward/message],"100 coins",`Steve,Alex`'
```

#### Global JavaScript Packages

Global JavaScript packages are stored under `plugins/KaMenu/js/`. One `.js` file is one package. The package ID is the relative path without the `.js` suffix, using `/` as the path separator.

For the full folder structure, execution context, and troubleshooting notes, see [js Folder](../config/javascript-packages.md).

```text
plugins/KaMenu/js/reward/message.js -> reward/message
```

When the server starts and `plugins/KaMenu/js/` does not exist yet, KaMenu releases a built-in example package:

```text
plugins/KaMenu/js/example/message.js -> example/message
```

Use `{js:[example/message],Steve}` to quickly test whether global JavaScript packages are loaded correctly.

File content:

```javascript
var amount = args[0] || "0";
var target = args[1] || name;
"Congratulations, " + target + " received " + amount + " coins";
```

Call it with:

```yaml
- 'js: [reward/message],100,Steve'
text: 'Message: {js:[reward/message],100,Steve}'
```

Global package IDs may only use letters, numbers, `_`, `-`, `.`, and `/`. Global packages are loaded on server startup, `/km reload`, or `/km reload js`.

{% hint style="info" %}
Neither `js:` actions nor `{js:...}` require JavaScript to return a value. If there is no return value, `js:` simply executes the code, while `{js:...}` resolves to an empty string.
{% endhint %}

### Supported Parameter Types

| Parameter Type | Example | Description |
|---------------|---------|-------------|
| **String** | `Hello` | Plain text passed directly |
| **PAPI Variable** | `%player_name%` | PlaceholderAPI variable |
| **Player Data** | `{data:money}` | Player-stored data |
| **Global Data** | `{gdata:config}` | Globally-stored data |
| **Input Variable** | `$(input1)` | Value from an input component |
| **Number** | `50`, `3.14` | Numeric value |

**Accessing arguments:**
```javascript
// Access arguments via the args array in JavaScript
var name = args[0];     // First argument
var level = args[1];    // Second argument
var data = args[2];     // Third argument
var input = args[3];    // Fourth argument

// Join all arguments
var allArgs = args.join(", ");

// Check if an argument exists
if (args[0]) {
    player.sendMessage("First argument: " + args[0]);
}

// Get the number of arguments
var count = args.length;
```

{% hint style="warning" %}
- Arguments may be separated by commas or spaces; wrap an argument with single quotes, double quotes, or backticks when it contains a space or comma
- Use `args[0]`, `args[1]`, etc. to access arguments (zero-indexed)
- `args` is a JavaScript array and supports all array methods
- Accessing a non-existent argument returns `undefined`
- It is recommended to check whether an argument exists before using it
{% endhint %}

---

## 💡 Usage Examples

### Example 1: Sending Messages and Variables

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&aShow Info'
    actions:
      - 'js: player.sendMessage("§aYour name: §f" + name);'
      - 'js: player.sendMessage("§aYour UUID: §f" + uuid);'
      - 'js: player.sendMessage("§aCurrent world: §f" + world.getName());'
```

### Example 2: Math Calculations

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&bCalculate Result'
    actions:
      - 'js: var num1 = 42; var num2 = 17; var sum = num1 + num2; player.sendMessage("§b" + num1 + " + " + num2 + " = §f" + sum);'
```

### Example 3: Random Numbers

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&eGenerate Random Number'
    actions:
      - 'js: var random = Math.floor(Math.random() * 100); player.sendMessage("§eRandom number: §f" + random);'
```

### Example 4: Conditional Logic

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&cCheck Health'
    actions:
      - 'js: var health = player.getHealth(); if (health > 15) { player.sendMessage("§aYou are in great health!"); } else if (health > 5) { player.sendMessage("§eYour health is decent."); } else { player.sendMessage("§cYou need to heal!"); }'
```

### Example 5: Accessing the Bukkit API

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&dServer Info'
    actions:
      - 'js: var onlinePlayers = Bukkit.getOnlinePlayers().size(); var maxPlayers = Bukkit.getMaxPlayers(); player.sendMessage("§dOnline players: §f" + onlinePlayers + "/" + maxPlayers);'
```

### Example 6: Delayed Execution

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&eCountdown'
    actions:
      - |-
        js: for (var i = 5; i >= 1; i--) {
              var delayTicks = (5 - i) * 20;
              (function(num) {
                delay(delayTicks, function() {
                  player.sendMessage("§e" + num + "...");
                });
              })(i);
            }
            delay(100, function() { player.sendMessage("§aGo!"); });
```

### Example 7: Using JavaScript Packages (No Arguments)

```yaml
Title: '&6Health Check Menu'

JavaScript:
  show_health: |
    var health = player.getHealth();
    var maxHealth = player.getMaxHealth();
    player.sendMessage("§eHealth: §f" + health + "/" + maxHealth);

  greet: |
    player.sendMessage("§aHello, " + name + "!");

Bottom:
  type: 'multi'
  buttons:
    health:
      text: '&aView Health'
      actions:
        - 'js: [show_health]'
    
    greet:
      text: '&bSay Hello'
      actions:
        - 'js: [greet]'
```

### Example 8: Using JavaScript Packages (With Arguments)

```yaml
Title: '&6Argument Test'

JavaScript:
  process_data: |
    var playerName = args[0];
    var playerLevel = args[1];
    var money = args[2];
    var note = args[3];

    player.sendMessage("§aPlayer: §f" + playerName);
    player.sendMessage("§aLevel: §f" + playerLevel);
    player.sendMessage("§aBalance: §f" + money);
    player.sendMessage("§aNote: §f" + note);

Inputs:
  level:
    type: 'slider'
    text: '&aSelect Level'
    min: 1
    max: 100
    default: 10

  note_input:
    type: 'input'
    text: '&7Note'
    default: 'None'

Bottom:
  type: 'notice'
  confirm:
    text: '&eProcess Data'
    actions:
      - 'js: [process_data] %player_name% $(level) {data:money} $(note_input)'
```

---

## 🎯 Advanced Tips

### Creating Reusable Functions

Put reusable logic in a menu `JavaScript` package or global JS package, then call it from buttons:

```yaml
JavaScript:
  random_loot: |
    function getRandomItem(items) {
      return items[Math.floor(Math.random() * items.length)];
    }
    var item = getRandomItem(["Diamond", "Gold", "Iron", "Coal"]);
    player.sendMessage("You found: §e" + item);

Bottom:
  type: 'notice'
  confirm:
    text: '&6Random Loot'
    actions:
      - 'js: [random_loot]'
```

### Using Delayed Execution

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&eDelayed Messages'
    actions:
      - 'js: delay(20, function() { player.sendMessage("§e1 second later"); });'
      - 'js: delay(40, function() { player.sendMessage("§e2 seconds later"); });'
      - 'js: delay(60, function() { player.sendMessage("§e3 seconds later"); });'
```

---

## 🐛 Troubleshooting

### JavaScript Features Unavailable?

If you see "JavaScript support disabled" or a similar error:

**Issue 1: Nashorn library not loaded**

1. On first startup, the server automatically downloads the Nashorn dependency in the background
2. After the download completes, **restart the server** to load the JavaScript feature
3. Check the console for a `JavaScript support enabled (Nashorn 15.3 engine)` message

**Issue 2: Download failed**

1. Check the network connection
2. Review error messages in the console logs
3. Ensure the server has write permissions

### JavaScript Code Execution Error?

If JavaScript code fails to execute:

1. **Check syntax**: Ensure the JavaScript syntax is correct (watch for semicolons, brackets, etc.)
2. **Review logs**: The console displays detailed error messages, including line numbers and error types
3. **Variable check**: Ensure the variables and functions used are defined
4. **Type check**: Verify that variable types are correct

**Common error examples:**
```
JavaScript execution error: ReferenceError: "undefinedVar" is not defined
JavaScript execution error: SyntaxError: Unexpected token
```

---

## ✅ Best Practices

### 1. Keep Code Concise

Avoid writing overly complex JavaScript code in menus. For complex logic, consider:

- Splitting it into multiple JavaScript packages
- Using clearly named function names
- Adding appropriate comments

### 2. Prefer Built-in Features

For simple condition checks, prefer KaMenu's built-in `condition` feature:

```yaml
# ✅ Recommended: use KaMenu conditions
- condition: "%player_level% >= 10"
  allow:
    - 'tell: Level sufficient'
  deny:
    - 'tell: Level insufficient'

# ⚠️ Works but not recommended: use JavaScript
- 'js: if (player.getLevel() >= 10) { player.sendMessage("Level sufficient"); }'
```

### 3. Error Handling

Use try-catch to handle potential errors:

```javascript
try {
    var result = someFunction();
    player.sendMessage("Result: " + result);
} catch (e) {
    player.sendMessage("An error occurred: " + e.message);
}
```

### 4. Performance Considerations

- Avoid time-consuming loops or calculations on every click
- Use delayed execution to spread the load
- Cache results of frequently reused calculations

### 5. Security

- Avoid performing sensitive operations in JavaScript (e.g., writing to databases)
- Handle user-input data carefully
- Avoid exposing internal server information

---

## 📚 Complete Example Menu

See `menus/example/javascript_demo.yml` for more complete examples!

The example menu includes:
- Basic JavaScript usage
- Random numbers and math calculations
- Conditional logic
- Bukkit API access
- JavaScript packages (no arguments)
- JavaScript packages (with arguments)
- Delayed execution

---

## ⚠️ Notes

- JavaScript code executes **server-side**
- Each player's click has an **independent** execution context
- Each `js:` / `{js:...}` / JavaScript package call uses an isolated execution context; do not rely on variables or functions defined by a previous `js:` action
- Complex JavaScript code may impact server performance
- Thoroughly test in a development environment **before deploying to production**
- The Nashorn engine is based on ECMAScript 5.1 and does not support ES6+ syntax

---

## 🚀 Next Steps

Now that you have mastered JavaScript, you can:

1. **Explore other features:**
   - [🤖 Actions (Actions)](actions.md) — Learn all available actions
   - [🔍 Conditions](conditions.md) — Master condition checks
   - [💾 Data Storage](../data/storage.md) — Use the database to store data

2. **Explore advanced applications:**
   - Combine data storage to create dynamic menus
   - Use JavaScript to implement complex business logic
   - Create reusable JavaScript function libraries

3. **View more examples:**
   - Browse the example menus in the `menus/example/` directory
   - Study community-shared menu configurations

---

## 💬 Need Help?

If you encounter issues or have suggestions:

- Check the `menus/example/javascript_demo.yml` example file
- Review detailed error logs in the console
- Submit an Issue or Discussion on GitHub
- Join the community Discord for help
