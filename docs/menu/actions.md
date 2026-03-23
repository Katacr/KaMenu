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

| 动作 | 功能说明 |
|------|----------|
| `tell` | 向玩家发送聊天消息 |
| `actionbar` | 向玩家发送动作栏消息（屏幕底部）|
| `title` | 向玩家发送屏幕标题和副标题 |
| `toast` | 在屏幕上显示 Toast 通知 |
| `hovertext` | 发送带有悬停提示和点击功能的聊天消息 |
| `command` | 让玩家执行一条指令 |
| `console` | 以控制台权限执行一条指令 |
| `sound` | 在玩家位置播放声音 |
| `open` | 为玩家打开另一个菜单 |
| `close` | 关闭当前菜单 |
| `url` | 打开指定链接（仅单动作时生效）|
| `copy` | 复制文字到剪贴板（仅单动作时生效）|
| `set-data` | 保存键值对到玩家数据 |
| `set-gdata` | 保存键值对到全局数据 |
| `wait` | 插入延迟执行 |

---

## 动作类型一览

### tell - 聊天消息

向玩家发送一条聊天消息。

**格式：** `tell: <消息>`

**示例：**

```yaml
- 'tell: &a操作成功！'
- 'tell: &c操作失败，请联系管理员'
- 'tell: &7当前余额: &f%player_balance%'
- 'tell: &e你输入的内容: $(input_key)'
```

**注意：** 支持颜色代码、PAPI 变量（`%var%`）、内置数据变量（`{data:key}`）和输入组件引用（`$(key)`）。

---

### actionbar - 动作栏消息

向玩家发送一条动作栏消息（显示在屏幕底部准星上方）。

**格式：** `actionbar: <消息>`

**示例：**

```yaml
- 'actionbar: &a操作成功！'
- 'actionbar: &7余额: &f%player_balance%'
```

**注意：** 消息持续显示约 3 秒后消失。

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

**格式：** `hovertext: 普通文字 <text=显示文字;hover=悬停文字;command=指令;url=链接;newline=false> 继续文字`

**参数说明：**

| 参数 | 说明 | 必需 |
|------|------|------|
| `text` | 可点击的显示文字 | ✅ |
| `hover` | 鼠标悬停时显示的提示文字 | ❌ |
| `command` | 点击时玩家执行的指令 | ❌ |
| `url` | 点击时打开的链接 | ❌ |
| `newline` | 是否在文字后换行（`true`/`false`）| ❌ |

**示例：**

```yaml
- 'hovertext: &7点击这里 <text=&a[领取奖励];hover=&e点击领取今日奖励;command=/daily> 或稍后再来。'
- 'hovertext: 访问 <text=&b[官网];hover=&7打开浏览器访问官网;url=https://example.com> 了解更多。'
```

**注意：** 参数值用反引号 `` ` ``、单引号 `'` 或双引号 `"` 包裹；可点击区域用 `< >` 包裹。

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

### set-data - 设置玩家数据

将一个键值对保存到当前玩家的持久化数据中。

**格式：** `set-data: <键名> <值>`

**示例：**

```yaml
- 'set-data: language zh_CN'
- 'set-data: nickname $(player_nickname)'
- 'set-data: score %player_level%'
```

**读取方式：** 在菜单任意文本位置使用 `{data:键名}` 或 PAPI 变量 `%kamenu_data_键名%`。

---

### set-gdata - 设置全局数据

将一个键值对保存到全局数据中（所有玩家共享）。

**格式：** `set-gdata: <键名> <值>`

**示例：**

```yaml
- 'set-gdata: server_status open'
- 'set-gdata: event_winner %player_name%'
```

**读取方式：** 在菜单任意文本位置使用 `{gdata:键名}` 或 PAPI 变量 `%kamenu_gdata_键名%`。

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
