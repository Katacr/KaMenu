# 🤖 动作 (Actions)

`actions` 节点定义按钮被点击后执行的操作列表。支持多种动作类型、延迟执行和条件分支。

---

## 配置结构

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&a确认'
    actions:
      - 'tell: &a操作成功！'
      - 'sound: entity.experience_orb.pickup'
      - 'close'
```

动作列表按**顺序**逐一执行（`wait` 动作可插入延迟）。

---

## 动作类型总览

| 动作          | 功能说明               |
|-------------|--------------------|
| `tell`      | 向玩家发送聊天消息          |
| `actionbar` | 向玩家发送动作栏消息（屏幕底部）   |
| `title`     | 向玩家发送屏幕标题和副标题      |
| `toast`     | 在屏幕上显示 Toast 通知    |
| `hovertext` | 发送带有悬停提示和点击功能的聊天消息 |
| `command`   | 让玩家执行一条指令          |
| `chat`      | 让玩家在聊天框中发送消息       |
| `console`   | 以控制台权限执行一条指令       |
| `sound`     | 在玩家位置播放声音          |
| `money`     | 操作玩家金币（需要 Vault）   |
| `stock-item`| 物品给予/扣除           |
| `open`      | 为玩家打开另一个菜单         |
| `close`     | 关闭当前菜单             |
| `url`       | 打开指定链接（仅单动作时生效）    |
| `copy`      | 复制文字到剪贴板（仅单动作时生效）  |
| `data`      | 操作玩家数据（支持 set/add/take/delete）|
| `gdata`     | 操作全局数据（支持 set/add/take/delete）|
| `meta`      | 操作玩家元数据（支持 set/add/take/delete）|
| `set-data`  | 设置玩家数据（旧格式，推荐使用 `data`）    |
| `set-gdata` | 设置全局数据（旧格式，推荐使用 `gdata`）    |
| `set-meta`  | 设置玩家元数据（旧格式，推荐使用 `meta`）    |
| `js`        | 执行 JavaScript 代码（支持预定义函数）   |
| `actions`   | 执行 Events.Click 下定义的动作列表  |
| `wait`      | 插入延迟执行                  |
| `return`    | 中断动作执行列表                |

---

## 动作类型一览

### tell - 聊天消息

向玩家发送一条聊天消息。**完整支持 Adventure MiniMessage 所有功能**，包括颜色、渐变、点击事件、悬停事件等。

**格式：** `tell: <消息>`

**示例（Legacy 颜色代码）：**

```yaml
- 'tell: &a操作成功！'
- 'tell: &c操作失败，请联系管理员'
- 'tell: &7当前余额: &f%player_balance%'
- 'tell: &e你输入的内容: $(input_key)'
```

**示例（MiniMessage 格式）：**

```yaml
# 基础颜色和格式
- 'tell: <red>红色文字</red>'
- 'tell: <bold>粗体</bold> <italic>斜体</italic> <underline>下划线</underline>'
- 'tell: <gradient:red:blue>蓝红渐变文字</gradient>'

# 点击事件
- 'tell: 点击这里执行指令: <click:run_command:/say 你点击了这里！><gold>点我！</gold></click>'
- 'tell: <click:copy_to_clipboard:Hello KaMenu><gold>复制这段文字</gold></click>'
- 'tell: <click:open_url:https://minecraft.wiki><gold>打开Minecraft Wiki</gold></click>'
- 'tell: <click:suggest_command:/gamemode creative><gold>切换创造模式</gold></click>'

# 悬停事件
- 'tell: <hover:show_text:"<red>这是悬停文字<reset>\n<blue>支持多行显示"><gold>把鼠标放上来！</gold></hover>'
- 'tell: <hover:show_item:diamond_sword>显示钻石剑</hover>'
- 'tell: <hover:show_item:diamond><gold>显示钻石</gold></hover>'

# 组合使用（点击+悬停）
- 'tell: <click:run_command:/say 联合事件><hover:show_text:"<green>点击执行指令\n<gray>悬停显示提示"><gold>点击并悬停</gold></hover></click>'

# 物品图标和玩家头像（1.21.9+）
- 'tell: 看看这个 <sprite:block/stone> 石头'
- 'tell: 这是 <sprite:items:item/porkchop> 猪排'
- 'tell: 这是 <head:Notch> Notch的头'
- 'tell: <head:entity/player/wide/steve> Steve的头'

# 其他 MiniMessage 标签
- 'tell: <blue>按键: </blue><key:key.keyboard.b><red>B键</red>'
- 'tell: <blue>换行测试: <newline><red>这是新的一行</red></blue>'
- 'tell: <blue>NBT数据: </blue><nbt:display.Name></blue>'
```

**MiniMessage 常用标签：**

| 标签 | 说明 | 示例 |
|------|------|------|
| `<color>` | 颜色标签 | `<red>红色</red>` |
| `<gradient>` | 渐变色 | `<gradient:red:blue>渐变</gradient>` |
| `<bold>` | 粗体 | `<bold>粗体</bold>` |
| `<italic>` | 斜体 | `<italic>斜体</italic>` |
| `<underline>` | 下划线 | `<underline>下划线</underline>` |
| `<click:action:value>` | 点击事件 | `<click:run_command:/say hi>点击</click>` |
| `<hover:action:value>` | 悬停事件 | `<hover:show_text:提示>悬停</hover>` |
| `<newline>` | 换行 | `第一行<newline>第二行` |
| `<key:keyname>` | 按键显示 | `<key:key.keyboard.b>B键</key>` |

**注意：**
- 支持 Legacy 颜色代码（`&a`、`&c` 等）和 MiniMessage 标签混合使用
- 当检测到 MiniMessage 标签时，会自动将 Legacy 颜色代码转换为对应的 MiniMessage 标签
  - 例如：`&a` → `<green>`，`&c` → `<red>`，`&l` → `<bold>`
  - 示例：`'<gold>&a这是 &c红色 &l粗体文字</l>'` 会自动转换为 `'<gold><green>这是 <red>红色 <bold>粗体文字</bold>'`
- 支持 PAPI 变量（`%var%`）、内置数据变量（`{data:key}`）和输入组件引用（`$(key)`）
- 建议使用纯 MiniMessage 格式以获得最佳效果和功能完整性

---

### actionbar - 动作栏消息

向玩家发送一条动作栏消息（显示在屏幕底部准星上方）。**完整支持 Adventure MiniMessage 所有功能**。

**格式：** `actionbar: <消息>`

**示例：**

```yaml
# Legacy 颜色代码
- 'actionbar: &a操作成功！'
- 'actionbar: &7余额: &f%player_balance%'

# MiniMessage 格式
- 'actionbar: <green>操作成功！</green>'
- 'actionbar: <gradient:gold:red>余额: 1000</gradient>'
- 'actionbar: <hover:show_text:查看详细><gold>点击查看详情</gold></hover>'
```

**注意：** 消息持续显示约 3 秒后消失。支持与 `tell` 相同的 MiniMessage 功能。

---

### title - 标题消息

向玩家发送屏幕标题和副标题。

**格式：** `title: title=主标题;subtitle=副标题;in=淡入;keep=停留;out=淡出`

**参数说明：**

| 参数 | 说明 | 单位 | 默认值 |
|------|------|------|--------|
| `title` | 主标题文本 | — | 空 |
| `subtitle` | 副标题文本 | — | 空 |
| `in` | 淡入时间 | tick | `0` |
| `keep` | 停留时间 | tick | `60` |
| `out` | 淡出时间 | tick | `20` |

**示例：**

```yaml
- 'title: title=&a操作成功;subtitle=&7已完成'
- 'title: title=&6欢迎！;subtitle=&f你好，%player_name%;in=10;keep=80;out=20'
```

**注意：** 参数用分号 `;` 分隔；支持颜色代码和变量。

---

### hovertext - 可点击聊天文本

发送带有悬停提示和点击功能的聊天消息。

**格式：** `hovertext: 普通文字 <text=显示文字;hover=悬停文字;command=指令;url=链接;actions=动作列表名;newline=false> 继续文字`

**参数说明：**

| 参数 | 说明 | 必需 |
|------|------|------|
| `text` | 可点击的显示文字 | ✅ |
| `hover` | 鼠标悬停时显示的提示文字 | ❌ |
| `command` | 点击时玩家执行的指令 | ❌ |
| `url` | 点击时打开的链接 | ❌ |
| `actions` | 点击时执行的动作列表（Events.Click 下的键名）| ❌ |
| `newline` | 是否在文字后换行（`true`/`false`）| ❌ |

**示例：**

```yaml
- 'hovertext: &7点击这里 <text=&a[领取奖励];hover=&e点击领取今日奖励;command=/daily> 或稍后再来。'
- 'hovertext: 访问 <text=&b[官网];hover=&7打开浏览器访问官网;url=https://example.com> 了解更多。'
- 'hovertext: <text=&a[问候];actions=greet;hover=点击发送问候> 问候玩家'
```

**使用 actions 参数：**

```yaml
Events:
  Click:
    greet:
      - 'tell: &a你好！欢迎来到服务器。'
      - 'sound: ENTITY_PLAYER_LEVELUP'

Body:
  text:
    type: 'message'
    text: '<text="点击问候";actions=greet;hover=点击执行 greet 动作>'
```

**点击事件优先级：**

当同时存在多个点击参数时，优先级如下（从高到低）：
1. `actions` - 执行动作列表
2. `url` - 打开链接
3. `command` - 执行指令

**注意：**
- 参数值用反引号 `` ` ``、单引号 `'` 或双引号 `"` 包裹
- 可点击区域用 `< >` 包裹
- `actions` 参数仅在 Body.message 文本组件中有效
- 当使用 `actions` 参数时，文本的点击事件会注册一个 ClickCallback，有效期 5 分钟

---

### command - 玩家指令

让点击按钮的玩家执行一条指令。

**格式：** `command: <指令>`

**示例：**

```yaml
- 'command: spawn'
- 'command: msg %player_name% Hello'
- 'command: warp hub'
```

**注意：** 指令前无需加 `/`；玩家需要有执行该指令的权限。

---

### chat - 聊天消息

让玩家在聊天框中发送一条消息。

**格式：** `chat: <消息>`

**示例：**

```yaml
- 'chat: /spawn'           # 玩家发送 /spawn 指令
- 'chat: 大家好！'          # 玩家发送聊天消息
- 'chat: /msg Admin 帮助我' # 玩家给管理员发送私聊
- 'chat: $(input_message)'  # 发送玩家输入的内容
```

**与 command 的区别：**

| 动作 | 执行方式 | 权限要求 | 适用场景 |
|------|---------|---------|---------|
| `command` | 直接执行指令 | 需要玩家权限 | 执行插件指令（如 `/spawn`）|
| `chat` | 模拟玩家在聊天框输入 | 不需要特殊权限 | 发送聊天消息、执行需要玩家权限的指令 |

**使用场景：**
- 需要让玩家在聊天框中显示消息（如广播、喊话）
- 执行需要玩家在聊天框中输入的指令
- 与其他玩家或插件进行交互

**注意：** 消息会被广播给在线玩家看到；支持颜色代码、PAPI 变量和输入组件引用。

---

### console - 控制台指令

以控制台（OP 权限）执行一条指令。

**格式：** `console: <指令>`

**示例：**

```yaml
- 'console: give %player_name% diamond 64'
- 'console: eco give %player_name% 1000'
- 'console: lp user %player_name% group add vip'
```

**注意：** 不需要玩家权限；指令前无需加 `/`；支持 PAPI 变量。

---

### sound - 播放声音

在玩家位置播放一个声音，支持音量、音调和声音分类。

**格式：** `sound: <声音名称>;volume=音量;pitch=音调;category=分类`

**参数说明：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| 声音名称 | Minecraft 声音 ID（使用 `_` 或 `.` 均可）| — |
| `volume` | 音量（浮点数）| `1.0` |
| `pitch` | 音调（浮点数）| `1.0` |
| `category` | 声音分类 | `master` |

**声音分类可选值：**

| 值 | 说明 |
|----|------|
| `master` | 主音量 |
| `music` | 音乐 |
| `record` | 唱片机 |
| `weather` | 天气 |
| `block` | 方块 |
| `hostile` | 敌对生物 |
| `neutral` | 中性生物 |
| `player` | 玩家 |
| `ambient` | 环境音 |
| `voice` | 语音 |
| `ui` | 界面音效 |

**示例：**

```yaml
- 'sound: entity.experience_orb.pickup'
- 'sound: entity.player.levelup;volume=1.5;pitch=1.2'
- 'sound: block.note_block.pling;volume=1.0;pitch=2.0;category=ui'
```

---

### open - 打开菜单

为玩家打开另一个菜单，当前菜单会自动关闭。

**格式：** `open: <菜单ID>`

**示例：**

```yaml
- 'open: main_menu'
- 'open: shop/weapons'
- 'open: admin/tools'
```

**注意：** 菜单 ID 规则与 `/km open` 指令相同，子文件夹用 `/` 分隔，不包含 `.yml` 扩展名。

---

### close - 关闭菜单

关闭当前打开的菜单。

**格式：** `close`

**示例：**

```yaml
- 'tell: &c再见！'
- 'close'
```

---

### url - 打开链接

打开指定 URL（仅在按钮只有这一个动作时有效）。

**格式：** `url: <链接地址>`

**示例：**

```yaml
actions:
  - 'url: https://github.com/Katacr/KaMenu'
```

{% hint style="info" %}
`url` 和 `copy` 动作为静态动作，**仅当按钮的 actions 列表中只有这一个动作时**才会生效。如需在执行其他动作的同时打开链接，请使用 `hovertext` 动作。
{% endhint %}

---

### copy - 复制到剪贴板

将指定文字复制到玩家的剪贴板（仅在按钮只有这一个动作时有效）。

**格式：** `copy: <文字>`

**示例：**

```yaml
actions:
  - 'copy: play.example.com'
```

---

### set-data - 设置玩家数据（旧格式）

将一个键值对保存到当前玩家的持久化数据中。

**格式：** `set-data: <键名> <值>`

**示例：**

```yaml
- 'set-data: language zh_CN'
- 'set-data: nickname $(player_nickname)'
- 'set-data: score %player_level%'
```

**读取方式：** 在菜单任意文本位置使用 `{data:键名}` 或 PAPI 变量 `%kamenu_data_键名%`。

{% hint style="warning" %}
此为旧格式，**推荐使用新的 `data` 动作**，支持更多操作类型（add/take/delete）。
{% endhint %}

---

### set-gdata - 设置全局数据（旧格式）

将一个键值对保存到全局数据中（所有玩家共享）。

**格式：** `set-gdata: <键名> <值>`

**示例：**

```yaml
- 'set-gdata: server_status open'
- 'set-gdata: event_winner %player_name%'
```

**读取方式：** 在菜单任意文本位置使用 `{gdata:键名}` 或 PAPI 变量 `%kamenu_gdata_键名%`。

{% hint style="warning" %}
此为旧格式，**推荐使用新的 `gdata` 动作**，支持更多操作类型（add/take/delete）。
{% endhint %}

---

### set-meta - 设置玩家元数据（旧格式）

将一个键值对保存到玩家的元数据中（内存缓存，无需持久化）。

**格式：** `set-meta: <键名> <值>`

**示例：**

```yaml
- 'set-meta: time 19:02'
- 'set-meta: nickname $(player_nickname)'
- 'set-meta: last_menu shop/weapons'
```

**读取方式：** 在菜单任意文本位置使用 `{meta:键名}` 或 PAPI 变量 `%kamenu_meta_键名%`。

{% hint style="warning" %}
此为旧格式，**推荐使用新的 `meta` 动作**，支持更多操作类型（add/take/delete）。
{% endhint %}

**注意：**
- 元数据仅存储在内存中，不持久化到数据库
- 玩家退出时自动清理该玩家的元数据
- 插件重载或关服时清理全部元数据
- 适用于需要短时间存储临时数据的场景

---

### data - 玩家数据操作

操作玩家的持久化数据，支持设置、增加、减少和删除数值。

**格式：** `data: type=操作类型;key=键名;var=值`

**参数说明：**

| 参数 | 说明 | 必需 |
|------|------|------|
| `type` | 操作类型 | ✅ |
| `key` | 数据键名 | ✅ |
| `var` | 值（仅 type=set/add/take 时需要）| ❌ |

**type 可选值：**
- `set`：设置值
- `add`：增加数值（仅当值为数字时有效）
- `take`：减少数值（仅当值为数字时有效）
- `delete`：删除该键值对

**示例：**

```yaml
# 设置文本值
- 'data: type=set;key=test;var=`你好，我的世界`'

# 设置数字值
- 'data: type=set;key=num;var=`100`'

# 为数字值增加
- 'data: type=add;key=num;var=`10`'

# 为数字值减少
- 'data: type=take;key=num;var=`10`'

# 删除数据
- 'data: type=delete;key=num'
```

**读取方式：** 在菜单任意文本位置使用 `{data:键名}` 或 PAPI 变量 `%kamenu_data_键名%`。

**注意：**
- `add` 和 `take` 操作时，如果当前值或指定值不是数字，操作会失败并在后台输出警告
- `delete` 操作时，如果键不存在，操作会静默失败（不会报错）
- 旧格式 `set-data: <键名> <值>` 仍然可用，但推荐使用新格式

---

### gdata - 全局数据操作

操作全局数据（所有玩家共享），支持设置、增加、减少和删除数值。

**格式：** `gdata: type=操作类型;key=键名;var=值`

**参数说明：**

| 参数 | 说明 | 必需 |
|------|------|------|
| `type` | 操作类型 | ✅ |
| `key` | 数据键名 | ✅ |
| `var` | 值（仅 type=set/add/take 时需要）| ❌ |

**type 可选值：**
- `set`：设置值
- `add`：增加数值（仅当值为数字时有效）
- `take`：减少数值（仅当值为数字时有效）
- `delete`：删除该键值对

**示例：**

```yaml
# 设置全局数据
- 'gdata: type=set;key=total;var=`1000`'

# 增加数值
- 'gdata: type=add;key=total;var=`50`'

# 减少数值
- 'gdata: type=take;key=total;var=`20`'

# 删除数据
- 'gdata: type=delete;key=total'
```

**读取方式：** 在菜单任意文本位置使用 `{gdata:键名}` 或 PAPI 变量 `%kamenu_gdata_键名%`。

**注意：**
- 全局数据在所有玩家之间共享
- `add` 和 `take` 操作时，如果当前值或指定值不是数字，操作会失败并在后台输出警告
- `delete` 操作时，如果键不存在，操作会静默失败（不会报错）
- 旧格式 `set-gdata: <键名> <值>` 仍然可用，但推荐使用新格式

---

### meta - 玩家元数据操作

操作玩家的元数据（内存缓存），支持设置、增加、减少和删除数值。

**格式：** `meta: type=操作类型;key=键名;var=值`

**参数说明：**

| 参数 | 说明 | 必需 |
|------|------|------|
| `type` | 操作类型 | ✅ |
| `key` | 数据键名 | ✅ |
| `var` | 值（仅 type=set/add/take 时需要）| ❌ |

**type 可选值：**
- `set`：设置值
- `add`：增加数值（仅当值为数字时有效）
- `take`：减少数值（仅当值为数字时有效）
- `delete`：删除该键值对

**示例：**

```yaml
# 设置元数据
- 'meta: type=set;key=level;var=`10`'

# 增加数值
- 'meta: type=add;key=level;var=`1`'

# 减少数值
- 'meta: type=take;key=level;var=`1`'

# 删除数据
- 'meta: type=delete;key=level'
```

**读取方式：** 在菜单任意文本位置使用 `{meta:键名}` 或 PAPI 变量 `%kamenu_meta_键名%`。

**注意：**
- 元数据仅存储在内存中，不持久化到数据库
- 玩家退出时自动清理该玩家的元数据
- 插件重载或关服时清理全部元数据
- `add` 和 `take` 操作时，如果当前值或指定值不是数字，操作会失败并在后台输出警告
- `delete` 操作时，如果键不存在，操作会静默失败（不会报错）
- 旧格式 `set-meta: <键名> <值>` 仍然可用，但推荐使用新格式

---

### toast - Toast 通知

在屏幕右上角显示一个 Toast 通知。

**格式：** `toast: type=类型;icon=物品ID;msg=标题`

**参数说明：**

| 参数 | 说明 | 默认值 |
| ------ | ------ |--------|
| `type` | 通知类型 | `task` |
| `icon` | 显示的物品ID | `paper` |
| `msg` | 内容文本 | 空 |

**type 可选值：**
- `task`：标题文本：`进度已达成!`（默认）
- `goal`：标题文本：`目标已达成!`
- `challenge`：标题文本：`挑战已完成!`（会播放音效）

**示例：**

```yaml
- 'toast: msg=&f你获得了一把钻石剑;icon=diamond_sword'
- 'toast: type=challenge;msg=&f恭喜你完成了挑战！;icon=diamond'
- 'toast: type=goal;msg=&f已达成目标;icon=gold_ingot'
```

**注意：** Toast 通知会在屏幕上显示约 3 秒后自动消失。

---

### money - 金币操作

操作玩家的金币（需要安装 Vault 经济插件）。

**格式：** `money: type=操作类型;num=金额`

**参数说明：**

| 参数 | 说明 | 可选值      |
|------|------|----------|
| `type` | 操作类型 | 如下列可选值       |
| `num` | 金额 | 数值（支持小数） |

**type 可选值：**
- `add`：给予玩家指定金额
- `take`：扣除玩家指定金额
- `reset`：将玩家余额设置为指定金额

**示例：**

```yaml
# 给予玩家 100 金币
- 'money: type=add;num=100'

# 扣除玩家 50 金币
- 'money: type=take;num=50'

# 将玩家余额设置为 1000 金币
- 'money: type=reset;num=1000'

# 结合条件判断使用
- condition: "%player_balance% >= 500"
  allow:
    - 'money: type=take;num=500'
    - 'tell: &a购买成功！'
  deny:
    - 'tell: &c余额不足！需要 500 金币'
```

**注意：**
- 需要安装 Vault 经济插件才能使用
- **此动作不会向玩家发送任何消息**，玩家需自行判断和提示（如使用 `tell` 或条件判断）
- `take` 操作会检查余额，余额不足时不会执行扣除，仅会在控制台打印警告
- 金额支持小数，如 1.5、0.99 等
- 金额可以使用变量，如 `%player_level%` 或 `{data:price}`

---

### stock-item - 物品给予/扣除

给予玩家或从玩家背包中扣除指定数量的数据库的物品。

**格式：** `stock-item: type=操作类型;name=物品名称;amount=数量`

**参数说明：**

| 参数 | 说明 | 必需 |
|------|------|------|
| `type` | 操作类型 | ✅ |
| `name` | 物品名称（已保存的物品）| ✅ |
| `amount` | 数量 | ❌（默认: 1）|

**type 可选值：**
- `give`：给予玩家物品
- `take`：从玩家背包中扣除物品

**示例：**

```yaml
# 给予玩家 16 个神秘果
- 'stock-item: type=give;name=神秘果;amount=16'

# 从玩家背包扣除 16 个神秘果
- 'stock-item: type=take;name=神秘果;amount=16'

# 结合条件判断使用
- condition: "hasStockItem.神秘果;16"
  allow:
    - 'stock-item: type=take;name=神秘果;amount=16'
    - 'tell: &a购买成功！'
  deny:
    - 'tell: &c物品不足！需要 16 个神秘果'
```

**注意：**
- 物品必须通过 `/km item save` 指令保存后才能使用
- `give` 操作如果玩家背包已满，剩余物品会自动掉落在玩家位置，不会丢失
- `take` 操作会遍历玩家所有背包槽位（包括主背包、盔甲槽、副手槽和主手槽）
- 物品比较使用 `ItemStack.isSimilar()` 方法，忽略物品数量差异
- 支持变量替换，如 `name=$(item_name)` 或 `amount={data:price}`

---

### item - 普通物品给予/扣除

给予玩家或从玩家背包中扣除指定材质的普通物品。

**格式：**
- `item: type=give;mats=材质;amount=数量`
- `item: type=take;mats=材质;amount=数量;lore=描述;model=模型`

**参数说明：**

| 参数 | 说明 | 必需 |
|------|------|------|
| `type` | 操作类型（give/take）| ✅ |
| `mats` | 物品材质（Material ID）| ✅ |
| `amount` | 数量 | ❌（默认: 1）|
| `lore` | 描述（仅用于take操作，可选）| ❌ |
| `model` | 物品模型（仅用于take操作，可选）| ❌ |

**type 可选值：**
- `give`：给予玩家物品（忽略lore和model参数）
- `take`：从玩家背包中扣除物品（支持lore和model判断）

**示例：**

```yaml
# 给予玩家 10 个钻石
- 'item: type=give;mats=DIAMOND;amount=10'

# 从玩家背包扣除 10 个钻石
- 'item: type=take;mats=DIAMOND;amount=10'

# 扣除指定 lore 的物品
- 'item: type=take;mats=DIAMOND;amount=10;lore=锻造材料'

# 扣除指定模型的物品（如 Oraxen 物品）
- 'item: type=take;mats=DIAMOND;amount=10;model=oraxen:mana_crystal'

# 同时指定 lore 和 model
- 'item: type=take;mats=DIAMOND;amount=10;lore=锻造材料;model=oraxen:mana_crystal'

# 结合条件判断使用
- condition: "hasItem.[mats=DIAMOND;amount=10]"
  allow:
    - 'item: type=take;mats=DIAMOND;amount=10'
    - 'tell: &a扣除成功！'
  deny:
    - 'tell: &c物品不足！需要 10 个钻石'
```

**注意：**
- `mats` 参数使用 Minecraft 原生材质 ID（如 `DIAMOND`、`IRON_INGOT` 等）
- `give` 操作时，`lore` 和 `model` 参数会被忽略，因为这两个参数仅用于判断
- `give` 操作如果玩家背包已满，剩余物品会自动掉落在玩家位置，不会丢失
- `take` 操作时：
  - 如果指定了 `lore`，会只扣除 lore 中包含指定字符串的物品（忽略大小写）
  - 如果指定了 `model`，会只扣除匹配指定物品模型的物品
  - 如果同时指定了 `lore` 和 `model`，物品需要同时满足两个条件才会被扣除
  - `model` 格式为 `namespace:key`（如 `oraxen:mana_crystal`、`minecraft:diamond`）
- `take` 操作会遍历玩家所有背包槽位（包括主背包、盔甲槽、副手槽和主手槽）
- 支持变量替换，如 `mats=$(material)`、`amount={data:price}`、`lore={data:item_desc}`

---

### wait - 延迟执行

在动作列表中插入延迟，后续动作将在等待指定时间后执行。

**格式：** `wait: <tick数>`

**单位：** Minecraft tick（1 tick = 0.05 秒，20 tick = 1 秒）

**示例：**

```yaml
- 'tell: &a开始倒计时...'
- 'wait: 20'            # 等待 1 秒
- 'tell: &e3...'
- 'wait: 20'
- 'tell: &e2...'
- 'wait: 20'
- 'tell: &e1...'
- 'wait: 20'
- 'title: title=&c出发！;in=5;keep=30;out=10'
```

**注意：** `wait` 只影响其**之后**的动作；不会阻塞其他正在执行的任务。

---

### return - 中断执行

在动作列表中插入中断，后续动作将不会被执行。

**格式：** `return`

**示例：**

```yaml
- 'tell: 你点击了这个按钮'
- 'return'            # 执行中断
- 'tell: 你永远无法看到这行消息。'  # 不会执行后续操作
```
```yaml
- 'tell: &a开始倒计时...'
- 'wait: 20'            # 等待 1 秒
- 'tell: &e3...'
- 'wait: 20'
- 'tell: &e2...'
- 'wait: 20'
- 'tell: &e1...'
- 'wait: 20'
- condition: '%player_is_online% == false'
  allow:
    - 'tell: &c检测到玩家离线，操作中断！'
    - 'return'   # 执行中断
- 'tell: &a你完成了操作。' # 若满足条件该动作不会被执行
```

**注意：** `wait` 只影响其**之后**的动作；不会阻塞其他正在执行的任务。

---

### js - 执行 JavaScript 代码

执行 JavaScript 代码，支持直接执行代码或调用预定义函数。

**格式：** `js: <JavaScript代码>`

**使用方式：**

1. **直接执行 JavaScript 代码**

```yaml
actions:
  - 'js: player.sendMessage("Hello from JavaScript!");'
  - 'js: var random = Math.floor(Math.random() * 100);'
  - 'js: player.sendMessage("随机数: " + random);'
```

2. **调用预定义函数（无参数）**

```yaml
JavaScript:
  show_health: |
    var health = player.getHealth();
    var maxHealth = player.getMaxHealth();
    player.sendMessage("§e生命值: §f" + health + "/" + maxHealth);

Bottom:
  type: 'notice'
  confirm:
    text: '&a查看生命值'
    actions:
      - 'js: [show_health]'
```

3. **调用预定义函数（带参数）**

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

**支持的变量：**

- `player` - 当前玩家对象
- `uuid` - 玩家 UUID 字符串
- `name` - 玩家名称
- `location` - 玩家位置
- `inventory` - 玩家物品栏
- `world` - 玩家所在世界
- `server` - 服务器实例
- `args` - 预定义函数的参数数组（仅在调用预定义函数时可用）

**支持的参数类型（用于预定义函数）：**

- 字符串：直接传递
- PAPI 变量：`%player_name%`
- 玩家数据：`{data:money}`
- 全局数据：`{gdata:config}`
- 输入框变量：`$(input1)`
- 数字：`50`, `3.14`

{% hint style="info" %}
JavaScript 功能非常强大，支持访问 Bukkit API、数学计算、条件判断等。详细了解 JavaScript 功能，请查看 [🔧 JavaScript 功能](javascript.md) 文档。
{% endhint %}

**注意：**
- JavaScript 代码在服务器端执行
- 预定义函数必须在菜单的 `JavaScript` 节点中定义
- 参数以空格分隔，参数中不能包含空格
- Nashorn 引擎基于 ECMAScript 5.1 标准，不支持 ES6+ 语法

---

### actions - 执行动作列表

执行 `Events.Click` 下定义的动作列表。这允许你在动作中复用已定义的动作列表，避免重复代码。

**格式：** `actions: <动作列表名称>`

**参数说明：**

| 参数 | 说明 | 示例 |
|------|------|------|
| 动作列表名称 | `Events.Click` 下的动作列表键名 | `greet`, `vip_check`, `daily_reward` |

**示例：**

```yaml
Events:
  Click:
    greet:
      - 'tell: &a你好！欢迎来到服务器。'
      - 'sound: ENTITY_PLAYER_LEVELUP'

    vip_check:
      - condition: '{permission:essentials.vip} == true'
        allow:
          - 'tell: &aVIP 专属欢迎！'
          - 'sound: ENTITY_EXPERIENCE_ORB_PICKUP'
        deny:
          - 'tell: &c你需要 VIP 权限'

Bottom:
  type: 'multi'
  buttons:
    btn_greet:
      text: '问候'
      actions:
        - 'actions: greet'  # 执行 Events.Click.greet

    btn_vip:
      text: 'VIP 检查'
      actions:
        - 'actions: vip_check'  # 执行 Events.Click.vip_check
```

**复杂动作链示例：**

```yaml
Events:
  Click:
    daily_login:
      - 'tell: &6每日签到成功！'
      - 'sound: ENTITY_PLAYER_LEVELUP'
      - 'set-data: coins +100'
      - 'tell: &e获得 100 金币'
      - 'sound: ENTITY_EXPERIENCE_ORB_PICKUP'

Bottom:
  type: 'multi'
  buttons:
    daily:
      text: '每日签到'
      actions:
        - 'actions: daily_login'
```

**特性：**

1. **异步执行**：`actions` 动作在异步线程中执行，不会阻塞主线程
2. **支持条件判断**：引用的动作列表中可以使用 `condition` 进行条件分支
3. **变量支持**：动作列表中支持所有 KaMenu 变量（`{data:xxx}`, `{permission:xxx}` 等）
4. **复用代码**：避免在多个按钮中重复定义相同的动作序列

**错误处理：**

如果引用的动作列表不存在，玩家会收到错误消息：
```
&c错误: 找不到动作列表 'xxx'
```

**与其他方式对比：**

| 方式 | 使用位置 | 触发方式 | 示例 |
|------|---------|---------|------|
| `actions` 动作 | 按钮动作、命令 | 点击按钮/执行命令 | `actions: greet` |
| `<text>` 标签的 `actions` 参数 | 文本组件（Body.message） | 点击文本 | `<text='点击';actions=greet>` |

**使用场景：**

- **按钮复用动作列表**：多个按钮执行相同的动作序列
- **条件分支**：根据玩家状态执行不同动作
- **命令快捷方式**：通过命令触发预定义的动作列表
- **动作链复用**：避免重复定义复杂的动作序列

**注意事项：**

1. 动作列表必须在 `Events.Click` 下定义
2. 避免循环引用（如动作列表 A 引用自己）
3. `actions` 动作本身也可以在条件判断中使用

---
## 完整示例

```yaml
Bottom:
  type: 'multi'
  columns: 2
  buttons:
    purchase:
      text: '&6[ 购买 ]'
      actions:
        - condition: "%player_balance% >= 500"
          allow:
            - 'console: eco take %player_name% 500'
            - 'console: give %player_name% diamond_sword 1'
            - 'tell: &a购买成功！消费 500 金币'
            - 'sound: entity.player.levelup'
            - 'close'
          deny:
            - 'tell: &c余额不足！需要 500 金币，当前: %player_balance%'
            - 'sound: block.note_block.bass'

    info:
      text: '&7[ 查看说明 ]'
      actions:
        - 'tell: &6=== 神圣之剑说明 ==='
        - 'tell: &f- 攻击力 +20'
        - 'tell: &f- 可用于高级副本'
        - 'hovertext: &7了解更多 <text=&b[点击查看官网];hover=&7打开浏览器;url=https://example.com>'
```
