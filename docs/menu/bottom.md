# 🔘 底部按钮 (Bottom)

`Bottom` 节点定义菜单底部的交互按钮区域，共有三种布局模式：`notice`、`confirmation`、`multi`。在 `multi.buttons` 内还可以使用 `type: repeat` 动态生成按钮列表。

---

## 配置结构

```yaml
Bottom:
  type: '模式类型'   # notice | confirmation | multi
  # 模式专属配置...
```

{% hint style="info" %}
`repeat` 不是 `Bottom.type` 的布局模式，不能写成 `Bottom.type: repeat`。它是 `Bottom.type: multi` 下 `buttons.<按钮ID>.type: repeat` 的动态按钮模板。
{% endhint %}

---

## 类型总览

| 类型 | 名称 | 用途 | 常见场景 |
|------|------|------|----------|
| `notice` | 单按钮模式 | 显示一个确认按钮 | 信息确认、领取奖励、简单提交 |
| `confirmation` | 确认/取消双按钮模式 | 显示确认和取消两个按钮 | 购买确认、删除确认、危险操作二次确认 |
| `multi` | 多按钮矩阵模式 | 显示多个按钮，可配置列数和退出按钮 | 主菜单、功能面板、分类入口、复杂操作菜单 |

---

## 布局模式与动态按钮

### notice - 单按钮模式

只显示一个确认按钮，适合信息展示或简单触发操作。

**配置项：**

| 字段 | 说明 |
|------|------|
| `confirm.text` | 按钮文字，支持颜色代码和条件判断 |
| `confirm.width` | 可选，按钮宽度（1-1024）|
| `confirm.actions` | 点击时执行的动作列表 |

**示例：**

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&a[ 领取奖励 ]'
    actions:
      - 'console: give %player_name% diamond 1'
      - 'tell: &a你已领取钻石！'
      - 'sound: entity.player.levelup'
```

---

### confirmation - 确认/取消双按钮模式

显示确认和取消两个按钮，适合需要二次确认的危险操作。

**配置项：**

| 字段 | 说明 |
|------|------|
| `confirm.text` | 确认按钮文字，支持条件判断 |
| `confirm.width` | 可选，确认按钮宽度（1-1024）|
| `confirm.actions` | 点击确认时执行的动作列表 |
| `deny.text` | 取消按钮文字，支持条件判断 |
| `deny.width` | 可选，取消按钮宽度（1-1024）|
| `deny.actions` | 点击取消时执行的动作列表 |

**示例：**

```yaml
Bottom:
  type: 'confirmation'
  confirm:
    text: '&a[ 确认购买 ]'
    actions:
      - 'console: eco take %player_name% 100'
      - 'console: give %player_name% diamond_sword 1'
      - 'tell: &a购买成功！'
      - 'sound: entity.experience_orb.pickup'
  deny:
    text: '&c[ 取消 ]'
    actions:
      - 'tell: &7已取消购买。'
      - 'sound: block.note_block.bass'
```

---

### multi - 多按钮矩阵模式

支持多个自定义按钮，以矩阵方式排列，可额外配置一个退出按钮。

**配置项：**

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `columns` | `Int` | `2` | 每行显示的按钮列数 |
| `buttons` | 节点 | — | 按钮列表（按 YAML 书写顺序排列）|
| `exit` | 节点 | — | 可选的退出/返回按钮（显示在按钮列表末尾）|

**按钮配置项：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `show-condition` | String | 可选，按钮显示条件；条件不满足时该按钮不显示 |
| `type` | String | 可选。普通按钮无需填写；填写 `repeat` 时表示动态按钮列表 |
| `text` | String/List | 按钮文字，支持颜色代码、条件判断和 MiniMessage 标签 |
| `width` | Int | 可选，按钮宽度（1-1024），不设置则使用默认宽度 |
| `tooltip` | List | 可选，按钮悬停提示（每行一个字符串），支持颜色代码和 MiniMessage |
| `actions` | List | 可选，点击时执行的动作列表；如果不设置则点击时无反应 |

### repeat - 动态按钮列表

`multi.buttons` 中的某个按钮可以配置为 `type: repeat`，用于根据动态数据源生成一组真实 Paper Dialog 按钮。适合在线玩家列表、传送点列表、好友列表、邮件列表等数量不固定的内容。

**基本位置：**

```yaml
Bottom:
  type: multi
  buttons:
    列表ID:
      type: repeat
      source: "数据源"
      item:
        text: "&a{item.value}"
        actions:
          - "tell: 你点击了 {item.value}"
```

```yaml
JavaScript:
  getWarpList: |
    JSON.stringify([
      { id: "home", name: "家", world: "world", x: 100, y: 64, z: 200 },
      { id: "mine", name: "矿洞", world: "world", x: -30, y: 12, z: 80 }
    ]);

Bottom:
  type: multi
  columns: 2
  buttons:
    warp_list:
      type: repeat
      source: "[getWarpList]"
      page_size: 20
      item:
        text: "&a{item.name}"
        width: 160
        tooltip:
          - "&7世界: &f{item.world}"
          - "&7坐标: &f{item.x}, {item.y}, {item.z}"
          - "&e点击传送"
        actions:
          - "actions: teleport_warp,{item.id}"
      empty:
        text: "&7暂无传送点"
        actions:
          - "toast: type=task;msg=暂无数据;icon=barrier"

    prev:
      text: "&e上一页"
      show-condition: "{page:warp_list} > 1"
      actions:
        - "page: warp_list prev"
        - "reset"

    next:
      text: "&e下一页"
      show-condition: "{page:warp_list} < {pages:warp_list}"
      actions:
        - "page: warp_list next"
        - "reset"
```

**repeat 配置项：**

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `type` | String | — | 固定为 `repeat` |
| `source` | String | — | 数据源。推荐使用 `[函数名]` 调用 `JavaScript` 中返回 JSON 数组的函数 |
| `split` | String | — | 可选，非 JSON 字符串列表的分隔符，例如 `","` |
| `trim` | Boolean | `true` | 使用 `split` 时是否自动去除每项前后空格 |
| `page_size` / `page-size` | Int | `20` | 每页生成的按钮数量，范围 `1-99` |
| `item` | 节点 | — | 每个列表项生成按钮时使用的模板 |
| `empty` | 节点 | — | 数据源为空时显示的按钮，可选 |

`source` 会先解析 KaMenu 内置变量、PAPI、`{js:...}` 等文本变量。解析结果可以是 JSON 数组、换行文本，或配合 `split` 使用的简单字符串列表。数组元素可以是对象、字符串或数字；对象字段会变为 `{item.字段名}`，并可用于按钮文字、tooltip、show-condition 和 actions。

内置列表变量 `{list:键名}` 和 `{glist:键名}` 会返回 JSON 数组字符串，可直接作为 `source` 使用：

```yaml
Bottom:
  type: multi
  buttons:
    friends:
      type: repeat
      source: "{list:friends}"
      item:
        text: "&a{item.value}"
        actions:
          - "tell: 你点击了 {item.value}"
```

如果数据源返回简单字符串列表，例如 `player1, player2, player3`，可以使用 `split` 拆分：

```yaml
Events:
  Open:
    - "data: type=set;key=recent_players_raw;var=`player1, player2, player3`"

Bottom:
  type: multi
  buttons:
    player_list:
      type: repeat
      source: "{data:recent_players_raw}"
      split: ","
      trim: true
      item:
        text: "&a{item.value}"
        actions:
          - "tell: 你点击了 {item.value}"
```

内置 item 变量：

| 变量 | 说明 |
|------|------|
| `{item.xxx}` | 当前项对象中的字段 |
| `{item.value}` | 当前项是字符串或数字时的值 |
| `{item.index}` | 当前项在完整列表中的下标，从 0 开始 |
| `{item.number}` | 当前项在完整列表中的序号，从 1 开始 |
| `{item.page_index}` | 当前项在当前页的下标，从 0 开始 |
| `{item.page_number}` | 当前项在当前页的序号，从 1 开始 |

分页变量可用于 `Bottom.multi.buttons` 的普通按钮和 repeat item 模板：

| 变量 | 说明 |
|------|------|
| `{page:列表ID}` | 当前页码 |
| `{pages:列表ID}` | 总页数 |
| `{total:列表ID}` | 总项目数 |
| `{start:列表ID}` | 当前页起始下标 |
| `{end:列表ID}` | 当前页结束下标 |

分页动作：

```yaml
- "page: warp_list next"
- "page: warp_list prev"
- "page: warp_list 1"
- "page: warp_list +1"
- "page: warp_list -1"
```

`page:` 动作只修改分页状态，不会自动刷新界面。通常需要紧跟 `reset`、`open` 或 `force-open`。

**退出按钮配置项：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `text` | String/List | 退出按钮文字，支持颜色代码、条件判断和 MiniMessage 标签 |
| `width` | Int | 可选，退出按钮宽度（1-1024）|
| `actions` | List | 可选，点击时执行的动作列表；如果不设置则点击时无反应 |

**示例：**

```yaml
Bottom:
  type: 'multi'
  columns: 3

  buttons:
    btn_shop:
      text: '&6[ 商店 ]'
      actions:
        - 'open: shop/main'

    btn_profile:
      text: '&b[ 个人信息 ]'
      actions:
        - 'open: profile'

    btn_settings:
      text: '&7[ 设置 ]'
      actions:
        - 'open: settings'

    btn_admin:
      text: '&4[ 管理面板 ]'
      actions:
        - 'open: admin/tools'

  exit:
    text: '&8[ 关闭 ]'
    actions:
      - 'actionbar: &7菜单已关闭'
      - 'close'
```


在 Multi 模式下，还可以使用 `show-condition` 来控制按钮是否显示：

```yaml
Bottom:
  type: multi
  columns: 2
  buttons:
    1:
      show-condition: "%player_is_op% == true"  # 只有管理员才能看到此按钮
      text: '[ 管理员按钮 ]'
      actions: ...
    2:
      show-condition: "%player_level% >= 10"  # 玩家大于等于 10 级才能看到
      text: '[ VIP 按钮 ]'
      actions: ...
    3:
      text: '[ 普通按钮 ]'  # 无显示条件，所有玩家可见
      actions: ...
```

---

## 条件判断按钮文字

所有按钮的 `text` 字段均支持条件判断：

```yaml
Bottom:
  type: 'confirmation'
  confirm:
    text:
      - condition: "%player_level% >= 10"
        allow: '&6[ VIP 确认 ]'
        deny: '&a[ 确认 ]'
    actions:
      - 'tell: &a已确认'
  deny:
    text: '&c[ 取消 ]'
    actions:
      - 'tell: &7已取消'
```

关于条件判断的完整语法，请参阅 [🔍 条件判断](conditions.md)。

---

## 按钮宽度 (width)

所有按钮都支持自定义宽度配置，通过 `width` 字段可以控制按钮的显示宽度。

**适用范围：**
- `notice` 模式的确认按钮（`confirm.width`）
- `confirmation` 模式的确认和取消按钮（`confirm.width` 和 `deny.width`）
- `multi` 模式的所有按钮（`buttons` 中的每个按钮和 `exit` 按钮）

**宽度值：**
- 范围：1 - 1024
- 不设置则使用默认宽度（由 Paper Dialog API 决定）
- 支持条件判断

**示例：**

```yaml
# notice 模式
Bottom:
  type: 'notice'
  confirm:
    text: '&a[ 确认 ]'
    width: 200
    actions:
      - 'tell: &a确认操作'

# confirmation 模式
Bottom:
  type: 'confirmation'
  confirm:
    text: '&a[ 确认 ]'
    width: 200
    actions:
      - 'tell: &a确认操作'
  deny:
    text: '&c[ 取消 ]'
    width: 100
    actions:
      - 'tell: &c取消操作'

# multi 模式
Bottom:
  type: 'multi'
  columns: 2

  buttons:
    wide_button:
      text: '&a[ 宽按钮 ]'
      width: 200
      actions:
        - 'tell: &a这是一个宽按钮'

    narrow_button:
      text: '&b[ 窄按钮 ]'
      width: 50
      actions:
        - 'tell: &b这是一个窄按钮'

    conditional_width:
      text: '&c[ 条件宽度 ]'
      width:
        - condition: '%player_is_op% == true'
          allow: 200
          deny: 100
      actions:
        - 'tell: &c按钮宽度根据权限变化'

  exit:
    text: '&8[ 退出 ]'
    width: 80
    actions:
      - 'close'
```

**注意：**
- 宽度值会影响按钮在界面上的实际显示尺寸
- 过大的宽度可能导致按钮超出屏幕
- 建议根据实际显示需求调整宽度值
