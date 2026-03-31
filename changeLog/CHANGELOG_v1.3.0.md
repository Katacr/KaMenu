# KaMenu v1.3.0 更新报告

## 📋 版本信息
|- **版本号**: 1.3.0
|- **发布日期**: 2026年4月1日
|

---

## ✨ 新增功能

### 1. JavaScript 功能完整支持

#### 功能概述

KaMenu 现在支持在菜单中使用 JavaScript 代码，基于 OpenJDK Nashorn 15.3 引擎实现。这将极大地扩展插件的功能性和灵活性。

#### 核心特性

**✅ 开箱即用**
- 无需安装额外插件
- 首次启动时自动下载 Nashorn 依赖
- 下载完成后重启即可使用

**✅ Bukkit API 集成**
- 完整访问 Bukkit API
- 支持操作玩家、世界、服务器等对象
- 所有 JavaScript 代码在服务器端执行

**✅ 变量自动绑定**
JavaScript 代码中可以直接使用以下内置变量：

| 变量名 | 类型 | 说明 |
|--------|------|------|
| `player` | Player | 当前玩家对象 |
| `uuid` | String | 玩家 UUID 字符串 |
| `name` | String | 玩家名称 |
| `location` | Location | 玩家位置 |
| `inventory` | Inventory | 玩家物品栏 |
| `world` | World | 玩家所在世界 |
| `server` | Server | 服务器实例 |

**✅ 内置辅助函数**

提供了以下便捷的辅助函数：

- `tell(player, message)` - 向玩家发送消息
- `log(message)` - 在控制台打印日志
- `delay(ticks, callback)` - 延迟执行回调函数（主线程）
- `asyncDelay(ticks, callback)` - 异步延迟执行回调函数
- `getPlayer(name)` - 通过名称获取玩家

#### 预定义函数系统

**功能说明：**

可以在菜单配置中定义可复用的 JavaScript 函数，然后在按钮中调用。

**定义方式：**

在菜单配置的顶层添加 `JavaScript` 节点：

```yaml
JavaScript:
  show_health: |
    var health = player.getHealth();
    var maxHealth = player.getMaxHealth();
    player.sendMessage("§e生命值: §f" + health + "/" + maxHealth);

  greet: |
    player.sendMessage("§aHello, " + name + "!");
```

**调用方式：**

**方式一：直接调用（无参数）**

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&a测试函数'
    actions:
      - 'js: [show_health]'
      - 'js: [greet]'
```

**方式二：传递参数**

```yaml
JavaScript:
  process_data: |
    var playerName = args[0];
    var playerLevel = args[1];
    var money = args[2];

    player.sendMessage("§a玩家: §f" + playerName);
    player.sendMessage("§a等级: §f" + playerLevel);
    player.sendMessage("§a金币: §f" + money);

Bottom:
  type: 'notice'
  confirm:
    text: '&e处理数据'
    actions:
      - 'js: [process_data] %player_name% $(level) {data:money}'
```

**支持的参数类型：**

| 参数类型 | 示例 | 说明 |
|---------|------|------|
| 字符串 | `Hello` | 直接传递的文本 |
| PAPI 变量 | `%player_name%` | PlaceholderAPI 变量 |
| 玩家数据 | `{data:money}` | 玩家存储的数据 |
| 全局数据 | `{gdata:config}` | 全局存储的数据 |
| 输入框变量 | `$(input1)` | 输入组件的值 |
| 数字 | `50`, `3.14` | 数值类型 |

**参数访问：**

在 JavaScript 函数中使用 `args` 数组访问参数：

```javascript
var name = args[0];     // 访问第一个参数
var level = args[1];    // 访问第二个参数
var data = args[2];     // 访问第三个参数
var allArgs = args.join(", ");  // 连接所有参数

// 检查参数是否存在
if (args[0]) {
    player.sendMessage("第一个参数: " + args[0]);
}
```

#### 使用示例

**示例 1：直接执行 JavaScript 代码**

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

**示例 2：数学计算**

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

**示例 3：随机数**

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&e生成随机数'
    actions:
      - 'js: var random = Math.floor(Math.random() * 100);'
      - 'js: player.sendMessage("§e随机数: §f" + random);'
```

**示例 4：条件判断**

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

**示例 5：访问 Bukkit API**

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

**示例 6：延迟执行**

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

#### 技术实现

**Nashorn 引擎集成：**
- 使用 Nashorn 15.3（OpenJDK 独立版本）
- 通过 Libby 库自动下载和管理依赖
- 反射方式初始化引擎（NashornScriptEngineFactory）
- 支持 ASM 9.5 字节码操作

**依赖管理：**
```kotlin
dependencies {
    compileOnly("org.openjdk.nashorn:nashorn-core:15.3")
    library("org.ow2.asm:asm:9.5")
    library("org.ow2.asm:asm-util:9.5")
}
```

**引擎初始化：**
```kotlin
val nashornFactoryClass = Class.forName("org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory")
val factory = nashornFactoryClass.getDeclaredConstructor().newInstance()
val getEngineMethod = nashornFactoryClass.getMethod("getScriptEngine")
scriptEngine = getEngineMethod.invoke(factory) as? ScriptEngine
```

**参数预处理：**
- 参数在传递给 JavaScript 之前会进行预处理
- 自动转义特殊字符（反斜杠、引号、换行符等）
- 构建为 JavaScript 数组：`["arg1", "arg2", "arg3"]`
- 绑定到 `args` 变量供 JavaScript 函数使用

---

### 2. JavaScript 演示菜单

#### 新增文件

创建了完整的 JavaScript 功能演示菜单：

**中文版**：`src/main/resources/menus/example/javascript_demo.yml`
**英文版**：`src/main/resources/menus/exampleEN/javascript_demo.yml`

#### 演示内容

包含 9 个演示按钮，涵盖各种 JavaScript 使用场景：

| 按钮 | 功能演示 |
|------|---------|
| 基础 JavaScript | 直接执行 JavaScript 代码，发送消息 |
| 随机数 | 生成随机数并显示 |
| 数学运算 | 执行数学计算（42 + 17）|
| 条件逻辑 | 根据生命值显示不同消息 |
| Bukkit 对象 | 访问 Bukkit API 获取服务器信息 |
| 自定义函数 | 使用内置辅助函数（tell、log）|
| 变量测试 | 测试内置变量绑定 |
| 高级示例 | 数组操作和字符串方法 |
| 预定义函数 | 调用菜单中定义的函数 |
| 传递参数 | 测试参数传递功能 |

#### 特性

- ✅ 双语支持（中文/英文）
- ✅ 完整的 tooltip 说明
- ✅ 包含输入组件示例
- ✅ 覆盖各种使用场景
- ✅ 代码注释清晰，易于学习

---

## 📚 文档更新

### 1. 新增 JavaScript 功能文档

创建了完整的 JavaScript 功能文档：`docs/menu/javascript.md`

**文档内容：**
- ✨ 功能特性说明
- 🔧 基础使用方法
- 📦 内置变量列表
- 🔌 内置辅助函数说明
- 📝 预定义函数系统详解
- 💡 丰富的使用示例
- 🎯 高级技巧
- 🐛 故障排除
- ✅ 最佳实践
- ⚠️ 注意事项
- 🚀 下一步指引

**文档特点：**
- 使用 emoji 作为章节标题，符合项目风格
- 包含详细的表格和代码示例
- 提供提示框（info/warning）强调重要信息
- 8 个完整的使用示例
- 完整的参数类型说明
- 友好的帮助指引

### 2. actions.md 文档更新

在 `docs/menu/actions.md` 中添加了 `js` 动作说明：

**更新内容：**
- 在动作类型总览表格中添加 `js` 动作
- 新增完整的 `js - 执行 JavaScript 代码` 章节
- 添加三种使用方式的详细说明
  1. 直接执行 JavaScript 代码
  2. 调用预定义函数（无参数）
  3. 调用预定义函数（带参数）
- 列出所有支持的变量
- 详细说明支持的参数类型
- 添加丰富的代码示例
- 包含提示框，引导用户查看详细文档

**文档特点：**
- ✅ 清晰的格式说明
- ✅ 详细的参数说明
- ✅ 丰富的使用示例
- ✅ 变量列表完整
- ✅ 参数类型详细
- ✅ 链接到完整 JavaScript 文档

### 3. SUMMARY.md 目录更新

在 `docs/SUMMARY.md` 中添加了 JavaScript 文档链接：

```markdown
* [🏞️ 菜单系统](menu/README.md)
  * [📁 菜单文件结构](menu/structure.md)
  * [📝 创建菜单教程](menu/creating_menu.md)
  * [🎬 菜单标题 (Title)](menu/layout.md)
  * [⚙️ 全局设置 (Setting)](menu/setting.md)
  * [🔧 JavaScript 功能](menu/javascript.md)
  * ...
```

---

## 🔨 技术改进

### 1. JavaScript 引擎架构

**设计模式：**
- 使用单例模式管理 JavaScript 引擎
- 延迟初始化（首次使用时才初始化）
- 支持动态重新初始化（库下载后）

**变量绑定优化：**
- 玩家上下文变量在执行前绑定
- 执行后自动清理，避免内存泄漏
- 线程安全，支持多玩家并发执行

### 2. 参数处理机制

**字符串转义：**
自动转义以下特殊字符：
- 反斜杠 `\` → `\\`
- 双引号 `"` → `\"`
- 换行符 `\n` → `\\n`
- 回车符 `\r` → `\\r`
- 制表符 `\t` → `\\t`

**数组构建：**
将参数列表转换为 JavaScript 数组格式：
```
输入: [func_name] arg1 arg2 arg3
输出: ["arg1", "arg2", "arg3"]
```

**空格分隔：**
- 参数以空格分隔
- 连续的多个空格会被合并为一个
- 支持引号包裹的参数（未来扩展）

### 3. 错误处理

**初始化错误处理：**
```kotlin
try {
    // 初始化 JavaScript 引擎
} catch (e: ClassNotFoundException) {
    plugin.logger.warning("Nashorn library not loaded yet")
} catch (e: Exception) {
    plugin.logger.warning("Failed to initialize JavaScript support: ${e.message}")
}
```

**执行错误处理：**
```kotlin
try {
    scriptEngine!!.eval(script)
} catch (e: ScriptException) {
    plugin?.logger?.warning("JavaScript execution error: ${e.message}")
} catch (e: Exception) {
    plugin?.logger?.warning("JavaScript error: ${e.message}")
}
```

---

## 🐛 问题修复

### 1. Nashorn 引擎初始化问题

**问题描述：**
之前的实现使用 `ScriptEngineManager.getEngineByName("nashorn")`，这在 Nashorn 15.3+ 版本中不可用。

**修复方案：**
改用反射方式创建 `NashornScriptEngineFactory`，然后调用 `getScriptEngine()` 方法。

**修复前：**
```kotlin
val manager = ScriptEngineManager()
val engine = manager.getEngineByName("nashorn")  // Nashorn 15.3 不支持
```

**修复后：**
```kotlin
val nashornFactoryClass = Class.forName("org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory")
val factory = nashornFactoryClass.getDeclaredConstructor().newInstance()
val getEngineMethod = nashornFactoryClass.getMethod("getScriptEngine")
scriptEngine = getEngineMethod.invoke(factory) as? ScriptEngine
```

### 2. ASM 依赖缺失问题

**问题描述：**
Nashorn 15.3 需要 ASM 9.5 库，但项目只包含了 `asm`，缺少 `asm-util`。

**修复方案：**
在 `build.gradle.kts` 中添加 `asm-util` 依赖：

```kotlin
library("org.ow2.asm:asm:9.5")
library("org.ow2.asm:asm-util:9.5")
```

### 3. 参数解析错误

**问题描述：**
初始实现将整个字符串 `[function_name] arg1 arg2` 传递给 JavaScript 引擎，导致引擎将其解析为一条 JavaScript 代码。

**修复方案：**
在 `MenuActions.kt` 中预处理参数，分离函数名和参数字符串，然后分别传递给 JavaScriptManager。

**修复前：**
```kotlin
JavaScriptManager.executePredefinedFunction(
    player,
    "[pass_args] Katacr 0 null 你好",  // 整个字符串传递
    config
)
```

**修复后：**
```kotlin
// 预处理
val functionName = "[pass_args]"
val argsString = "Katacr 0 null 你好"

JavaScriptManager.executePredefinedFunctionWithArgs(
    player,
    functionName,
    argsString,  // 分别传递
    config
)
```

---

## 📊 兼容性说明

### 系统要求

|- ✅ **Paper 1.21.6+**：推荐使用 Paper 服务端
|- ✅ **Java 17+**：要求 Java 17 或更高版本
|- ✅ **网络连接**：首次启动时需要网络以下载 Nashorn 依赖

### 向后兼容性

|- ✅ **完全兼容**：所有现有菜单配置无需修改即可正常使用
|- ✅ **可选功能**：JavaScript 功能是可选的，不使用不受影响
|- ✅ **渐进增强**：可以在现有菜单中逐步添加 JavaScript 功能

### 依赖变化

**新增依赖：**
```
org.openjdk.nashorn:nashorn-core:15.3
org.ow2.asm:asm:9.5
org.ow2.asm:asm-util:9.5
```

**注意事项：**
- Nashorn 依赖通过 Libby 自动下载
- 首次启动时会自动下载，无需手动操作
- 下载完成后需要重启服务器以加载 JavaScript 功能

---

## 📝 配置示例

### 示例 1：基础 JavaScript 使用

```yaml
Title: '&6JavaScript 演示'

Bottom:
  type: 'notice'
  confirm:
    text: '&a测试 JS'
    actions:
      - 'js: player.sendMessage("Hello from JavaScript!");'
      - 'js: var random = Math.floor(Math.random() * 100);'
      - 'js: player.sendMessage("随机数: " + random);'
```

### 示例 2：使用预定义函数

```yaml
Title: '&6预定义函数演示'

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

### 示例 3：参数传递

```yaml
Title: '&6参数传递演示'

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

## ⚠️ 注意事项

### 1. JavaScript 语法

- Nashorn 引擎基于 ECMAScript 5.1 标准
- 不支持 ES6+ 语法（如 `const`、`let`、箭头函数等）
- 必须使用 `var` 声明变量
- 必须使用分号 `;` 结束语句

**正确示例：**
```javascript
var name = "test";     // ✅ 正确
var sum = a + b;      // ✅ 正确
```

**错误示例：**
```javascript
const name = "test";   // ❌ 不支持
let sum = a + b;       // ❌ 不支持
var sum = () => a + b; // ❌ 不支持箭头函数
```

### 2. 性能考虑

- JavaScript 代码在服务器端执行，复杂逻辑可能影响性能
- 避免在每次点击时执行耗时操作
- 建议使用延迟执行来分散负载
- 预定义函数会被缓存，多次调用效率较高

### 3. 安全性

- JavaScript 代码拥有服务器权限
- 不要在 JavaScript 中执行敏感操作
- 谨慎处理用户输入的数据
- 避免暴露服务器内部信息

### 4. 参数限制

- 参数以空格分隔，参数中不能包含空格
- 如果参数需要包含空格，未来可以考虑使用引号包裹
- `args` 数组索引从 0 开始
- 访问不存在的参数返回 `undefined`

### 5. 依赖下载

- 首次启动时会自动下载 Nashorn 库
- 下载过程在后台进行
- 下载完成后需要**重启服务器**
- 检查日志确认 JavaScript 功能已启用

---

## 💡 使用建议

### 1. 简单场景使用内置动作

对于简单的消息发送、指令执行等操作，优先使用内置动作：
- `tell` - 发送消息
- `command` - 执行指令
- `console` - 控制台指令
- `actionbar` - 动作栏消息

### 2. 复杂逻辑使用 JavaScript

对于需要数学计算、条件判断、循环等复杂逻辑，使用 JavaScript：
- 数学运算（加减乘除、随机数等）
- 条件分支（if-else 语句）
- 循环操作（for 循环）
- 数组处理

### 3. 复用预定义函数

将常用的 JavaScript 代码定义为预定义函数，然后在多个地方调用：
- 避免重复代码
- 便于维护和修改
- 提高配置可读性

### 4. 结合数据存储

将 JavaScript 与数据存储功能结合使用：
- 使用 `{data:key}` 读取玩家数据
- 使用 `{gdata:key}` 读取全局数据
- 在 JavaScript 中处理数据
- 处理结果保存回数据存储

---

## 🙏 致谢

感谢所有使用和测试 KaMenu 的用户！

特别感谢：
- 为 JavaScript 功能提供反馈和测试建议的用户
- 帮助发现和报告 Bug 的社区成员
- 贡献文档和示例的翻译者

您的反馈是我们不断改进的动力！

---

## 📞 反馈与支持

如果您在使用过程中遇到问题或有建议：

- 📝 **提交 Issue**：在 GitHub 上提交 Bug 报告或功能建议
- 💬 **社区讨论**：在 Discussions 中交流使用心得
- 📖 **文档反馈**：如果发现文档问题，欢迎提出改进建议
- ⭐ **支持项目**：如果觉得插件有用，欢迎给个 Star！

---

## 🚀 升级指南

### 从 1.2.x 升级到 1.3.0

**升级步骤：**

1. **备份配置**
   - 备份 `plugins/KaMenu/` 目录
   - 备份 `config.yml` 配置文件

2. **更新插件**
   - 下载最新的 `KaMenu-1.3.0.jar`
   - 替换旧版本的 jar 文件
   - 删除旧版本 jar 文件

3. **首次启动**
   - 启动服务器
   - 等待 Nashorn 库自动下载（首次启动）
   - 查看日志确认下载完成

4. **重启服务器**
   - 完全停止服务器
   - 重新启动服务器
   - 查看日志确认 "JavaScript support enabled"

5. **测试功能**
   - 打开演示菜单：`/km open javascript_demo`
   - 测试 JavaScript 功能是否正常工作
   - 查看更新日志了解新功能

**兼容性：**

- ✅ **完全兼容**：所有现有菜单无需修改
- ✅ **渐进增强**：可以逐步添加 JavaScript 功能
- ✅ **向后兼容**：旧版本创建的菜单继续正常工作

---

## 📈 版本对比

| 功能 | 1.2.7 | 1.3.0 |
|------|---------|---------|
| MiniMessage 支持 | ✅ | ✅ |
| 点击执行动作列表 | ✅ | ✅ |
| JavaScript 支持 | ❌ | ✅ |
| 预定义函数 | ❌ | ✅ |
| 参数传递 | ❌ | ✅ |
| 内置变量 | ❌ | ✅ |
| JavaScript 文档 | ❌ | ✅ |
| JavaScript 演示菜单 | ❌ | ✅ |

---

**v1.3.0 - 释放无限可能 🚀**
