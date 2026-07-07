# 📦 js 文件夹

`plugins/KaMenu/js/` 用于存放全局 JavaScript 包。JavaScript 包适合放置多个菜单都会复用的脚本逻辑，例如文本格式化、复杂状态计算、奖励消息、游戏逻辑和列表解析。

全局 JavaScript 包可以在 `js:` 动作、`{js:...}` 文本占位和条件表达式中调用。

---

## 文件位置

运行目录：

```text
plugins/KaMenu/js/
```

一个 `.js` 文件就是一个 JavaScript 包。包 ID 为相对路径去掉 `.js` 后缀，并使用 `/` 作为路径分隔符：

```text
plugins/KaMenu/js/reward/message.js -> reward/message
plugins/KaMenu/js/common/format_money.js -> common/format_money
```

首次启动且 `plugins/KaMenu/js/` 不存在时，KaMenu 会释放内置示例：

```text
plugins/KaMenu/js/example/message.js -> example/message
```

如果该文件夹已存在，KaMenu 不会自动写入示例文件，避免影响已有服务器文件。

---

## 命名与大小限制

包 ID 只能使用英文、数字、`_`、`-`、`.`、`/`，不能包含 `..`，不能以 `/` 开头或结尾，也不能包含连续的 `//`。

每个 JavaScript 包文件最大为 `1 MiB`。超过上限的文件会被跳过，并在控制台输出本地化警告。

---

## 文件内容

`.js` 文件内容就是 JavaScript 代码：

```javascript
var amount = args[0] || "0";
var target = args[1] || name;
"恭喜 " + target + " 获得 " + amount + " 金币";
```

最后一个表达式会作为 `{js:...}` 的结果。`js:` 动作不要求返回值，也不会自动把返回值发送给玩家。

---

## 调用方式

动作中调用：

```yaml
actions:
  - 'js: [reward/message],100,Steve'
```

文本中调用：

```yaml
text: '提示：{js:[reward/message],100,Steve}'
```

条件中调用：

```yaml
condition: '{js:[check/vip]} == true'
```

参数可以用英文逗号或空格分隔：

```yaml
- 'js: [reward/message],100,Steve'
- 'js: [reward/message] 100 Steve'
```

参数中包含空格或逗号时，使用单引号、双引号或反引号包裹：

```yaml
- 'js: [reward/message],"100 coins",`Steve,Alex`'
```

JavaScript 内通过 `args[0]`、`args[1]` 读取参数。

---

## 查找优先级

当调用：

```yaml
- 'js: [reward/message],100,Steve'
```

KaMenu 按以下顺序查找：

1. 当前菜单 `JavaScript.reward/message`
2. 全局 JavaScript 包 `plugins/KaMenu/js/reward/message.js`

如果菜单内 `JavaScript` 和全局包同名，优先执行菜单内脚本。

---

## 执行上下文

每次 `js:`、`{js:...}` 和 JavaScript 包调用都会使用独立执行上下文。不要依赖上一条 `js:` 动作中定义的变量或函数。

旧写法：

```yaml
actions:
  - 'js: var random = Math.floor(Math.random() * 100);'
  - 'js: player.sendMessage("随机数: " + random);'
```

推荐写法：

```yaml
actions:
  - 'js: var random = Math.floor(Math.random() * 100); player.sendMessage("随机数: " + random);'
```

更推荐将复杂逻辑放入菜单 `JavaScript` 或全局 JS 包：

```yaml
JavaScript:
  random_message: |
    var random = Math.floor(Math.random() * 100);
    player.sendMessage("随机数: " + random);

Bottom:
  type: notice
  confirm:
    text: '&a[ 随机数 ]'
    actions:
      - 'js: [random_message]'
```

---

## 内置变量

JavaScript 包中可以直接使用：

- `player`：当前玩家对象
- `uuid`：玩家 UUID 字符串
- `name`：玩家名
- `location`：玩家位置
- `inventory`：玩家物品栏
- `world`：玩家所在世界
- `server`：Bukkit 服务器实例
- `args`：包调用参数数组

---

## 辅助函数

- `tell(player, message)`：发送消息
- `log(message)`：输出 `[JS]` 日志
- `delay(ticks, callback)`：主线程延迟执行
- `asyncDelay(ticks, callback)`：异步延迟执行
- `getPlayer(name)`：按名称获取玩家
- `papi(placeholder, targetPlayer?)`：解析 PlaceholderAPI
- `kvar(variable, targetPlayer?)`：解析 KaMenu 内部变量
- `data(key)` / `gdata(key)` / `meta(key)`：快捷读取数据变量
- `list(key)` / `glist(key)`：读取列表变量，返回 JSON 数组字符串

---

## 重载

全局 JavaScript 包会在服务器启动、`/km reload` 或 `/km reload js` 时加载。

仅加载 `.js` 文件。

---

## 常见问题

**脚本没有返回文本**

`js:` 动作不显示返回值，这是正常行为。需要显示结果时，使用 `tell`、`toast`、`title` 等动作，或在文本中使用 `{js:[包名]}`。

**提示找不到 JavaScript 包**

- 检查文件是否位于 `plugins/KaMenu/js/`
- 检查后缀是否为 `.js`
- 检查包 ID 是否只使用英文、数字、`_`、`-`、`.`、`/`
- 检查文件大小是否不超过 `1 MiB`
- 检查调用时是否使用 `[包名]`
- 执行 `/km reload js`

**变量在下一条 js 中不存在**

这是执行上下文隔离的正常行为。将相关代码合并到同一条 `js:`，或放入菜单 `JavaScript` / 全局 JS 包。
