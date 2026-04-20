# 🔧 JavaScript Features

KaMenu has a built-in powerful JavaScript engine (based on OpenJDK Nashorn 15.3), allowing you to use JavaScript code inside menus to perform various complex operations.

---

## ✨ Features

- ✅ **Ready to use**: No extra plugins required
- ✅ **Auto-download**: Required dependencies are downloaded automatically on first startup
- ✅ **Bukkit API integration**: Direct access to the Bukkit API
- ✅ **Variable bindings**: Player-related variables are automatically bound
- ✅ **Pre-defined functions**: Supports defining reusable JavaScript functions within a menu
- ✅ **Parameter passing**: Supports passing multiple types of parameters to pre-defined functions

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
      - 'js: var random = Math.floor(Math.random() * 100);'
      - 'js: player.sendMessage("Random number: " + random);'
```

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
if (target) {
    tell(target, "Hello!");
}
```

---

## 📝 Pre-defined Functions

### Defining Pre-defined Functions

Add a `JavaScript` node at the top level of the menu configuration file to define reusable JavaScript code blocks:

```yaml
Title: '&6JavaScript Example Menu'

# Pre-defined function area
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
Each key inside the `JavaScript` node is a function name, and the value is the JavaScript code (use `|` for multi-line strings).
{% endhint %}

### Calling Pre-defined Functions

#### Method 1: Direct Call (No Arguments)

Call a pre-defined function using the `[function_name]` format:

```yaml
Bottom:
  type: 'multi'
  buttons:
    test-functions:
      text: '&aTest Pre-defined Functions'
      actions:
        - 'js: [show_health]'
        - 'js: [greet]'
```

#### Method 2: Passing Arguments

Add space-separated arguments after the function name:

```yaml
Bottom:
  type: 'multi'
  buttons:
    pass-args:
      text: '&cPass Arguments'
      actions:
        # Format: [function_name] arg1 arg2 arg3 ...
        - 'js: [pass_args] %player_name% 50 {data:test} default_value'
```

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
- Arguments are separated by spaces; arguments cannot contain spaces
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
      - 'js: var num1 = 42;'
      - 'js: var num2 = 17;'
      - 'js: var sum = num1 + num2;'
      - 'js: player.sendMessage("§b" + num1 + " + " + num2 + " = §f" + sum);'
```

### Example 3: Random Numbers

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&eGenerate Random Number'
    actions:
      - 'js: var random = Math.floor(Math.random() * 100);'
      - 'js: player.sendMessage("§eRandom number: §f" + random);'
```

### Example 4: Conditional Logic

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&cCheck Health'
    actions:
      - 'js: var health = player.getHealth();'
      - 'js: if (health > 15) { player.sendMessage("§aYou are in great health!"); }'
      - 'js: else if (health > 5) { player.sendMessage("§eYour health is decent."); }'
      - 'js: else { player.sendMessage("§cYou need to heal!"); }'
```

### Example 5: Accessing the Bukkit API

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&dServer Info'
    actions:
      - 'js: var onlinePlayers = Bukkit.getOnlinePlayers().size();'
      - 'js: var maxPlayers = Bukkit.getMaxPlayers();'
      - 'js: player.sendMessage("§dOnline players: §f" + onlinePlayers + "/" + maxPlayers);'
```

### Example 6: Delayed Execution

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&eCountdown'
    actions:
      - 'js: for (var i = 5; i >= 1; i--) {'
      - 'js:   var delayTicks = (5 - i) * 20;'
      - 'js:   (function(num) {'
      - 'js:     delay(delayTicks, function() {'
      - 'js:       player.sendMessage("§e" + num + "...");'
      - 'js:     });'
      - 'js:   })(i);'
      - 'js: }'
      - 'js: delay(100, function() { player.sendMessage("§aGo!"); });'
```

### Example 7: Using Pre-defined Functions (No Arguments)

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

### Example 8: Using Pre-defined Functions (With Arguments)

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

### Creating Custom Functions

Define custom functions in events, then call them in buttons:

```yaml
Events:
  Open:
    - 'js: function getRandomItem(items) {'
    - 'js:   return items[Math.floor(Math.random() * items.length)];'
    - 'js: }'

Bottom:
  type: 'notice'
  confirm:
    text: '&6Random Loot'
    actions:
      - 'js: var item = getRandomItem(["Diamond", "Gold", "Iron", "Coal"]);'
      - 'js: player.sendMessage("You found: §e" + item);'
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

- Splitting it into multiple pre-defined functions
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
- Pre-defined functions (no arguments)
- Pre-defined functions (with arguments)
- Delayed execution

---

## ⚠️ Notes

- JavaScript code executes **server-side**
- Each player's click has an **independent** execution context
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
