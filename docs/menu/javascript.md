# 🔧 JavaScript 功能

KaMenu 内置了强大的 JavaScript 引擎（基于 OpenJDK Nashorn 15.3），允许你在菜单中使用 JavaScript 代码执行各种复杂操作。

---

## ✨ 功能特性

- ✅ **开箱即用**：无需安装额外插件
- ✅ **自动下载**：首次启动时自动下载所需依赖
- ✅ **Bukkit API 集成**：可直接访问 Bukkit API
- ✅ **变量绑定**：自动绑定玩家相关变量
- ✅ **预定义函数**：支持在菜单中定义可复用的 JavaScript 函数
- ✅ **参数传递**：支持向预定义函数传递多种类型参数

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
      - 'js: var random = Math.floor(Math.random() * 100);'
      - 'js: player.sendMessage("随机数: " + random);'
```

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
if (target) {
    tell(target, "你好!");
}
```

---

## 📝 预定义函数

### 定义预定义函数

在菜单配置文件的顶层添加 `JavaScript` 节点，定义可复用的 JavaScript 代码块：

```yaml
Title: '&6JavaScript 示例菜单'

# JavaScript 预定义函数区域
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
`JavaScript` 节点中的每个键都是一个函数名，值是 JavaScript 代码（使用 `|` 表示多行字符串）。
{% endhint %}

### 调用预定义函数

#### 方式 1：直接调用（无参数）

使用 `[function_name]` 格式调用预定义函数：

```yaml
Bottom:
  type: 'multi'
  buttons:
    test-functions:
      text: '&a测试预定义函数'
      actions:
        - 'js: [show_health]'
        - 'js: [greet]'
```

#### 方式 2：传递参数

在函数名后添加空格分隔的参数：

```yaml
Bottom:
  type: 'multi'
  buttons:
    pass-args:
      text: '&c传递参数'
      actions:
        # 格式: [function_name] 参数1 参数2 参数3 ...
        - 'js: [pass_args] %player_name% 50 {data:test} default_value'
```

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
- 参数以空格分隔，参数中不能包含空格
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
      - 'js: var num1 = 42;'
      - 'js: var num2 = 17;'
      - 'js: var sum = num1 + num2;'
      - 'js: player.sendMessage("§b" + num1 + " + " + num2 + " = §f" + sum);'
```

### 示例 3：随机数

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&e生成随机数'
    actions:
      - 'js: var random = Math.floor(Math.random() * 100);'
      - 'js: player.sendMessage("§e随机数: §f" + random);'
```

### 示例 4：条件判断

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&c检查生命值'
    actions:
      - 'js: var health = player.getHealth();'
      - 'js: if (health > 15) { player.sendMessage("§a你很健康！"); }'
      - 'js: else if (health > 5) { player.sendMessage("§e你的生命值还不错。"); }'
      - 'js: else { player.sendMessage("§c你需要治疗！"); }'
```

### 示例 5：访问 Bukkit API

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&d服务器信息'
    actions:
      - 'js: var onlinePlayers = Bukkit.getOnlinePlayers().size();'
      - 'js: var maxPlayers = Bukkit.getMaxPlayers();'
      - 'js: player.sendMessage("§d在线玩家: §f" + onlinePlayers + "/" + maxPlayers);'
```

### 示例 6：延迟执行

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&e倒计时'
    actions:
      - 'js: for (var i = 5; i >= 1; i--) {'
      - 'js:   var delayTicks = (5 - i) * 20;'
      - 'js:   (function(num) {'
      - 'js:     delay(delayTicks, function() {'
      - 'js:       player.sendMessage("§e" + num + "...");'
      - 'js:     });'
      - 'js:   })(i);'
      - 'js: }'
      - 'js: delay(100, function() { player.sendMessage("§a开始！"); });'
```

### 示例 7：使用预定义函数（无参数）

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

### 示例 8：使用预定义函数（带参数）

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

### 创建自定义函数

在事件中定义自定义函数，然后在按钮中调用：

```yaml
Events:
  Open:
    - 'js: function getRandomItem(items) {'
    - 'js:   return items[Math.floor(Math.random() * items.length)];'
    - 'js: }'

Bottom:
  type: 'notice'
  confirm:
    text: '&6随机战利品'
    actions:
      - 'js: var item = getRandomItem(["钻石", "金子", "铁", "煤炭"]);'
      - 'js: player.sendMessage("你发现了: §e" + item);'
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

- 拆分为多个预定义函数
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
- 预定义函数（无参数）
- 预定义函数（带参数）
- 延迟执行

---

## ⚠️ 注意事项

- JavaScript 代码在**服务器端**执行
- 每个玩家点击时的上下文是**独立**的
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
