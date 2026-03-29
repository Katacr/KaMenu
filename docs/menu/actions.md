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
| `wait`      | 插入延迟执行                  |
| `return`    | 中断动作执行列表                |

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
