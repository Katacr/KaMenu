# 🔧 JavaScript 功能

KaMenu 内置了强大的 JavaScript 引擎（基于 OpenJDK Nashorn 15.3），允许你在菜单中使用 JavaScript 代码执行各种复杂操作。

---

## ✨ 功能特性

- ✅ **开箱即用**：无需安装额外插件
- ✅ **自动下载**：首次启动时自动下载所需依赖
- ✅ **Bukkit API 集成**：可直接访问 Bukkit API
- ✅ **变量绑定**：自动绑定玩家相关变量
- ✅ **JavaScript 包**：支持菜单内 `JavaScript` 和全局 `plugins/KaMenu/js/` 复用脚本
- ✅ **参数传递**：支持向 JavaScript 包传递多种类型参数

---

## 🔧 基础使用

### 直接执行 JavaScript 代码

在 `actions` 中使用 `js:` 前缀直接执行 JavaScript 代码：

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&a测试 JS'
    actions:
      - 'js: player.sendMessage("Hello from JavaScript!");'
      - 'js: var random = Math.floor(Math.random() * 100); player.sendMessage("随机数: " + random);'
```

### 作为文本占位符使用

`{js:...}` 可以直接写在任意文本中，系统会把它替换成 JavaScript 表达式的返回值：

```yaml
Title: '&6结果: {js:player.getLevel()}'

actions:
  - 'tell: 恭喜玩家 {js:name} 获得了大礼包'
  - 'tell: 等级是否足够: {js:player.getLevel() >= 10}'
```

**说明：**
- `{js:...}` 只适合输出值，不适合执行副作用动作
- 如果脚本没有返回值，或执行失败，会被替换为空串
- 可用于标题、文本、条件表达式、动作参数等任意文本位置

---

## 📦 内置变量

JavaScript 代码中可以直接使用以下变量：

| 变量名 | 类型 | 说明 |
|--------|------|------|
| `player` | Player | 当前玩家对象 |
| `uuid` | String | 玩家 UUID 字符串 |
| `name` | String | 玩家名称 |
| `location` | Location | 玩家位置 |
| `inventory` | Inventory | 玩家物品栏 |
| `world` | World | 玩家所在世界 |
| `server` | Server | 服务器实例 |

**示例：**
```yaml
actions:
  - 'js: player.sendMessage("欢迎, " + name + "!");'
  - 'js: player.sendMessage("UUID: " + uuid);'
  - 'js: player.sendMessage("世界: " + world.getName());'
```

---

## 🔌 内置辅助函数

JavaScript 环境中已预置以下辅助函数：

### `tell(player, message)`
向玩家发送消息

```javascript
tell(player, "Hello!");
```

### `log(message)`
在控制台打印日志

```javascript
log("玩家点击了按钮");
```

### `delay(ticks, callback)`
延迟执行回调函数（主线程）

```javascript
delay(20, function() {
    player.sendMessage("1秒后执行");
});
```

### `asyncDelay(ticks, callback)`
异步延迟执行回调函数

```javascript
asyncDelay(20, function() {
    log("异步任务完成");
});
```

### `getPlayer(name)`
通过名称获取玩家

```javascript
var target = getPlayer("Steve");
```

### `papi(placeholder, targetPlayer)`
解析 PlaceholderAPI 变量。`targetPlayer` 可选，未填写时使用当前 `player`。

支持带 `%` 和不带 `%` 两种写法：

```javascript
var playerName = papi("player_name");
var level = parseInt(papi("%player_level%"));
var onlinePlayers = JSON.parse(papi("kamenu_online_players"));
var targetLevel = papi("player_level", getPlayer("Steve"));
```

如果未安装 PlaceholderAPI 或解析失败，返回空字符串。

### `kvar(variable, targetPlayer)`
解析 KaMenu 内部变量。`targetPlayer` 可选，未填写时使用当前 `player`。

支持带 `{}` 和不带 `{}` 两种写法：

```javascript
var coins = data("coins");
var serverStatus = gdata("server_status");
var tempChoice = meta("temporary_choice");
var friends = JSON.parse(list("friends"));
var servers = JSON.parse(glist("servers"));
var handType = kvar("checkitem:[hand;type]");
var enchants = JSON.parse(kvar("checkitem:[hand;enchants]"));
var raw = kvar("{gdata:server_status}");
```

便捷函数：

- `data(key, targetPlayer)`：等价于 `kvar("data:" + key, targetPlayer)`
- `gdata(key, targetPlayer)`：等价于 `kvar("gdata:" + key, targetPlayer)`
- `meta(key, targetPlayer)`：等价于 `kvar("meta:" + key, targetPlayer)`
- `list(key, targetPlayer)`：等价于 `kvar("list:" + key, targetPlayer)`，返回 JSON 数组字符串
- `glist(key, targetPlayer)`：等价于 `kvar("glist:" + key, targetPlayer)`，返回 JSON 数组字符串

#### 在 JavaScript 中读取列表

`list()` 和 `glist()` 读取的是 KaMenu 的内置列表数据：

- `list("friends")`：读取当前玩家的 `friends` 列表
- `glist("servers")`：读取全局共享的 `servers` 列表
- 第二个参数可传入目标玩家对象，例如 `list("friends", getPlayer("Steve"))`
- 返回值是 JSON 数组字符串，通常需要用 `JSON.parse(...)` 转成 JavaScript 数组

```javascript
var friends = JSON.parse(list("friends"));
if (friends.indexOf("Steve") >= 0) {
    tell(player, "Steve 已在你的好友列表中");
}

var servers = JSON.parse(glist("servers"));
for (var i = 0; i < servers.length; i++) {
    log("服务器: " + servers[i]);
}
```

也可以在 `{js:...}` 内输出列表数量或判断结果：

```yaml
Body:
  info:
    type: message
    text:
      - '&a好友数量: {js:JSON.parse(list("friends")).length}'
      - '&e是否包含 Steve: {js:JSON.parse(list("friends")).indexOf("Steve") >= 0}'
```

{% hint style="info" %}
`list()` / `glist()` 只负责读取变量，不会修改列表。要写入列表，请在 actions 中使用 `list:` / `glist:` 动作；要在 YAML 条件里判断成员关系，推荐使用 `inList` / `inGlist`。
{% endhint %}

```javascript
var target = getPlayer("Steve");
if (target) {
    tell(target, "你好!");
}
```

---

## 📝 JavaScript 包

### 定义菜单内 JavaScript 包

在菜单配置文件的顶层添加 `JavaScript` 节点，定义可复用的 JavaScript 代码块：

```yaml
Title: '&6JavaScript 示例菜单'

# 菜单内 JavaScript 包区域
JavaScript:
  show_health: |
    var health = player.getHealth();
    var maxHealth = player.getMaxHealth();
    player.sendMessage("§e生命值: §f" + health + "/" + maxHealth);

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
`JavaScript` 节点中的每个键都是一个菜单内包名，值是 JavaScript 代码（使用 `|` 表示多行字符串）。调用时会优先查找当前菜单内包，再查找全局 `plugins/KaMenu/js/` 包。
{% endhint %}

### 调用 JavaScript 包

#### 方式 1：直接调用（无参数）

使用 `[package_name]` 格式调用当前菜单 `JavaScript` 节点中的代码块，或全局 `plugins/KaMenu/js/` 下的 `.js` 文件。

查找优先级：

1. 当前菜单 `JavaScript.<package_name>`
2. 全局 JavaScript 包 `plugins/KaMenu/js/<package_name>.js`

```yaml
Bottom:
  type: 'multi'
  buttons:
    test-functions:
      text: '&a测试 JavaScript 包'
      actions:
        - 'js: [show_health]'
        - 'js: [greet]'
```

#### 方式 2：传递参数

在包名后添加参数，支持英文逗号或空格分隔：

```yaml
Bottom:
  type: 'multi'
  buttons:
    pass-args:
      text: '&c传递参数'
      actions:
        - 'js: [pass_args],%player_name%,50,{data:test},default_value'
        - 'js: [pass_args] %player_name% 50 {data:test} default_value'
```

参数中需要包含空格或逗号时，可以使用单引号、双引号或反引号包裹：

```yaml
- 'js: [reward/message],"100 coins",`Steve,Alex`'
```

#### 全局 JavaScript 包

全局 JavaScript 包存放在 `plugins/KaMenu/js/` 下，一个 `.js` 文件就是一个包。包 ID 为相对路径去掉 `.js` 后缀，并使用 `/` 作为路径分隔符。

完整的文件夹结构、执行上下文和排错说明见 [js 文件夹](../config/javascript-packages.md)。

```text
plugins/KaMenu/js/reward/message.js -> reward/message
```

首次启动且 `plugins/KaMenu/js/` 文件夹不存在时，KaMenu 会释放内置示例包：

```text
plugins/KaMenu/js/example/message.js -> example/message
```

可使用 `{js:[example/message],Steve}` 快速测试全局 JavaScript 包是否加载正常。

文件内容：

```javascript
var amount = args[0] || "0";
var target = args[1] || name;
"恭喜 " + target + " 获得 " + amount + " 金币";
```

调用：

```yaml
- 'js: [reward/message],100,Steve'
text: '提示：{js:[reward/message],100,Steve}'
```

全局包 ID 只能使用英文字母、数字、`_`、`-`、`.` 和 `/`。全局包会在服务器启动、`/km reload` 或 `/km reload js` 时加载。

{% hint style="info" %}
`js:` 动作和 `{js:...}` 都不强制要求 JavaScript 返回值。无返回值时，`js:` 只执行代码；`{js:...}` 会解析为空字符串。
{% endhint %}

### 支持的参数类型

| 参数类型 | 示例 | 说明 |
|---------|------|------|
| **字符串** | `Hello` | 直接传递的文本 |
| **PAPI 变量** | `%player_name%` | PlaceholderAPI 变量 |
| **玩家数据** | `{data:money}` | 玩家存储的数据 |
| **全局数据** | `{gdata:config}` | 全局存储的数据 |
| **输入框变量** | `$(input1)` | 输入组件的值 |
| **数字** | `50`, `3.14` | 数值类型 |

**参数访问示例：**
```javascript
// 在 JavaScript 中使用 args 数组访问参数
var name = args[0];     // 访问第一个参数
var level = args[1];    // 访问第二个参数
var data = args[2];     // 访问第三个参数
var input = args[3];    // 访问第四个参数

// 连接所有参数
var allArgs = args.join(", ");

// 检查参数是否存在
if (args[0]) {
    player.sendMessage("第一个参数: " + args[0]);
}

// 获取参数数量
var count = args.length;
```

{% hint style="warning" %}
- 参数支持英文逗号或空格分隔；参数自身包含空格或逗号时，可以使用单引号、双引号或反引号包裹
- 使用 `args[0]`、`args[1]` 等访问参数（从 0 开始）
- `args` 是 JavaScript 数组，支持所有数组方法
- 如果参数不存在，访问时会返回 `undefined`
- 建议在使用前检查参数是否存在
{% endhint %}

---

## 💡 使用示例

### 示例 1：发送消息和变量

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&a显示信息'
    actions:
      - 'js: player.sendMessage("§a你的名字: §f" + name);'
      - 'js: player.sendMessage("§a你的 UUID: §f" + uuid);'
      - 'js: player.sendMessage("§a当前世界: §f" + world.getName());'
```

### 示例 2：数学计算

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&b计算结果'
    actions:
      - 'js: var num1 = 42; var num2 = 17; var sum = num1 + num2; player.sendMessage("§b" + num1 + " + " + num2 + " = §f" + sum);'
```

### 示例 3：随机数

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&e生成随机数'
    actions:
      - 'js: var random = Math.floor(Math.random() * 100); player.sendMessage("§e随机数: §f" + random);'
```

### 示例 4：条件判断

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&c检查生命值'
    actions:
      - 'js: var health = player.getHealth(); if (health > 15) { player.sendMessage("§a你很健康！"); } else if (health > 5) { player.sendMessage("§e你的生命值还不错。"); } else { player.sendMessage("§c你需要治疗！"); }'
```

### 示例 5：访问 Bukkit API

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&d服务器信息'
    actions:
      - 'js: var onlinePlayers = Bukkit.getOnlinePlayers().size(); var maxPlayers = Bukkit.getMaxPlayers(); player.sendMessage("§d在线玩家: §f" + onlinePlayers + "/" + maxPlayers);'
```

### 示例 6：延迟执行

多行 JavaScript 应放在菜单 `JavaScript` 包或全局 JS 包中，然后用 `js: [包名]` 调用。每次 `js:` 动作都会使用独立执行上下文，不要把同一段逻辑拆成多条 `js:` 动作共享变量。

```yaml
JavaScript:
  countdown: |
    for (var i = 5; i >= 1; i--) {
      var delayTicks = (5 - i) * 20;
      (function(num) {
        delay(delayTicks, function() {
          player.sendMessage("§e" + num + "...");
        });
      })(i);
    }
    delay(100, function() { player.sendMessage("§a开始！"); });

Bottom:
  type: 'notice'
  confirm:
    text: '&e倒计时'
    actions:
      - 'js: [countdown]'
```

### 示例 7：使用 JavaScript 包（无参数）

```yaml
Title: '&6健康检查菜单'

JavaScript:
  show_health: |
    var health = player.getHealth();
    var maxHealth = player.getMaxHealth();
    player.sendMessage("§e生命值: §f" + health + "/" + maxHealth);

  greet: |
    player.sendMessage("§aHello, " + name + "!");

Bottom:
  type: 'multi'
  buttons:
    health:
      text: '&a查看生命值'
      actions:
        - 'js: [show_health]'
    
    greet:
      text: '&b打招呼'
      actions:
        - 'js: [greet]'
```

### 示例 8：使用 JavaScript 包（带参数）

```yaml
Title: '&6传友试验'

JavaScript:
  process_data: |
    var playerName = args[0];
    var playerLevel = args[1];
    var money = args[2];
    var note = args[3];

    player.sendMessage("§a玩家: §f" + playerName);
    player.sendMessage("§a等级: §f" + playerLevel);
    player.sendMessage("§a金币: §f" + money);
    player.sendMessage("§a备注: §f" + note);

Inputs:
  level:
    type: 'slider'
    text: '&a选择等级'
    min: 1
    max: 100
    default: 10

  note_input:
    type: 'input'
    text: '&7备注信息'
    default: '无'

Bottom:
  type: 'notice'
  confirm:
    text: '&e处理数据'
    actions:
      - 'js: [process_data] %player_name% $(level) {data:money} $(note_input)'
```

---

## 🎯 高级技巧

### 创建可复用函数

将可复用逻辑放在菜单 `JavaScript` 包或全局 JS 包中，然后在按钮中调用：

```yaml
JavaScript:
  random_loot: |
    function getRandomItem(items) {
      return items[Math.floor(Math.random() * items.length)];
    }
    var item = getRandomItem(["钻石", "金子", "铁", "煤炭"]);
    player.sendMessage("你发现了: §e" + item);

Bottom:
  type: 'notice'
  confirm:
    text: '&6随机战利品'
    actions:
      - 'js: [random_loot]'
```

### 使用延迟执行

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&e延迟消息'
    actions:
      - 'js: delay(20, function() { player.sendMessage("§e1秒后"); });'
      - 'js: delay(40, function() { player.sendMessage("§e2秒后"); });'
      - 'js: delay(60, function() { player.sendMessage("§e3秒后"); });'
```

---

## 🐛 故障排除

### JavaScript 功能不可用？

如果看到 "JavaScript support disabled" 或类似错误：

**问题 1：Nashorn 库未加载**

1. 首次启动时，服务器会在后台自动下载 Nashorn 依赖
2. 下载完成后，需要**重启服务器**以加载 JavaScript 功能
3. 检查控制台是否有 `JavaScript support enabled (Nashorn 15.3 engine)` 消息

**问题 2：下载失败**

1. 检查网络连接
2. 查看控制台日志中的错误信息
3. 确保服务器有写入权限

### JavaScript 代码执行错误？

如果 JavaScript 代码执行失败：

1. **检查语法**：确保 JavaScript 语法正确（注意分号、括号等）
2. **查看日志**：控制台会显示详细的错误信息，包括行号和错误类型
3. **变量检查**：确保使用的变量和函数已定义
4. **类型检查**：确认变量类型是否正确

**常见错误示例：**
```
JavaScript execution error: ReferenceError: "undefinedVar" is not defined
JavaScript execution error: SyntaxError: Unexpected token
```

---

## ✅ 最佳实践

### 1. 保持代码简洁

避免在菜单中编写过于复杂的 JavaScript 代码。复杂逻辑建议：

- 拆分为多个 JavaScript 包
- 使用命名清晰的函数名
- 添加适当的注释

### 2. 优先使用内置功能

对于简单的条件判断，优先使用 KaMenu 的 `condition` 功能：

```yaml
# ✅ 推荐：使用 KaMenu 条件
- condition: "%player_level% >= 10"
  allow:
    - 'tell: 等级足够'
  deny:
    - 'tell: 等级不足'

# ⚠️ 可以但不推荐：使用 JavaScript
- 'js: if (player.getLevel() >= 10) { player.sendMessage("等级足够"); }'
```

### 3. 错误处理

使用 try-catch 处理可能的错误：

```javascript
try {
    var result = someFunction();
    player.sendMessage("结果: " + result);
} catch (e) {
    player.sendMessage("发生错误: " + e.message);
}
```

### 4. 性能考虑

- 避免在每次点击时执行耗时的循环或计算
- 使用延迟执行来分散负载
- 缓存重复使用的计算结果

### 5. 安全性

- 不要在 JavaScript 中执行敏感操作（如数据库写入）
- 谨慎处理用户输入的数据
- 避免暴露服务器内部信息

---

## 📚 完整示例菜单

查看 `menus/example/javascript_demo.yml` 获取更多完整示例！

该示例菜单包含：
- 基础 JavaScript 使用
- 随机数和数学计算
- 条件判断
- Bukkit API 访问
- JavaScript 包（无参数）
- JavaScript 包（带参数）
- 延迟执行

---

## ⚠️ 注意事项

- JavaScript 代码在**服务器端**执行
- 每个玩家点击时的上下文是**独立**的
- 每次 `js:` / `{js:...}` / JavaScript 包调用都会使用独立执行上下文；不要依赖上一条 `js:` 动作中定义的变量或函数
- 复杂的 JavaScript 代码可能会影响服务器性能
- 建议在生产环境**充分测试**后再部署
- Nashorn 引擎基于 ECMAScript 5.1 标准，不支持 ES6+ 语法

---

## 🚀 下一步

掌握了 JavaScript 功能后，你可以：

1. **深入了解其他功能**：
   - [🤖 动作 (Actions)](actions.md) - 学习所有可用的动作
   - [🔍 条件判断](conditions.md) - 掌握条件判断的使用
   - [💾 数据存储](../data/storage.md) - 使用数据库存储数据

2. **探索高级应用**：
   - 结合数据存储创建动态菜单
   - 使用 JavaScript 实现复杂的业务逻辑
   - 创建可复用的 JavaScript 函数库

3. **查看更多示例**：
   - 浏览 `menus/example/` 目录下的示例菜单
   - 学习社区分享的菜单配置

---

## 💬 需要帮助？

如果遇到问题或有建议：

- 查看 `menus/example/javascript_demo.yml` 示例文件
- 在控制台查看详细的错误日志
- 在 GitHub 上提交 Issue 或 Discussion
- 加入社区 Discord 寻求帮助
