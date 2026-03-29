# 📥 输入组件 (Inputs)

`Inputs` 节点用于在菜单中添加交互式输入组件，允许玩家提交数值、文字或做出选择。输入的值可以在动作中通过 `$(键名)` 变量引用。

---

## 配置结构

```yaml
Inputs:
  组件键名:
    type: '组件类型'
    text: '组件标签文字'
    # 组件专属配置...
```

- **组件键名**：唯一标识，在动作中用 `$(键名)` 引用该组件的值
- **组件顺序**：按照 YAML 书写顺序从上到下排列

---

## 引用输入值

在 `Bottom` 的 `actions` 中，可以通过 `$(键名)` 引用对应输入框的当前值：

```yaml
Inputs:
  player_name:  # 输入内容将对应下方$(player_name)
    type: 'input'
    text: '请输入玩家名称'
    default: '请在此输入...'
  volume:  # 输入内容将对应下方$(volume)
    type: 'slider'
    text: '音量'
    min: 0
    max: 100
    step: 1
    default: 50
    format: '音量: %s%s'
  amount:  # 输入内容将对应下方$(amount)
    type: 'input'
    text: '请输入数量'
    default: '请在此输入...'
    
Bottom:
  type: 'notice'
  confirm:
    text: '&a确认'
    actions:
      - 'tell: &f你输入的名字是: &e $(player_name)'
      - 'tell: &f你选择的音量是: &e $(volume)'
      - 'tell: &f你输入的数量是: &e $(amount)'
```

---

## 组件类型

### input - 文本输入框

允许玩家输入任意文字。

**配置项：**

| 字段 | 类型 | 必须 | 默认值 | 说明 |
|------|------|------|--------|------|
| `type` | `String` | ✅ | — | 固定值 `input` |
| `text` | `String` | ✅ | — | 输入框标签文字，支持条件判断 |
| `default` | `String` | ❌ | `""` | 默认填充文字 |
| `max_length` | `Int` | ❌ | `256` | 最大输入字符数 |
| `width` | `Int` | ❌ | `250` | 输入框宽度（像素）|
| `multiline` | 节点 | ❌ | — | 启用多行输入模式 |

**多行输入配置：**

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `multiline.max_lines` | `Int` | `5` | 最大行数 |
| `multiline.height` | `Int` | `100` | 输入框高度（像素）|

**示例：**

```yaml
Inputs:
  player_name:
    type: 'input'
    text: '&a请输入玩家名称'
    default: 'Steve'
    max_length: 16

  feedback:
    type: 'input'
    text: '&7留言内容'
    default: '请在此输入...'
    multiline:
      max_lines: 5
      height: 80
```

---

### slider - 数值滑块

允许玩家通过拖动滑块选择一个范围内的数值。

**配置项：**

| 字段 | 类型 | 必须 | 默认值 | 说明 |
|------|------|------|--------|------|
| `type` | `String` | ✅ | — | 固定值 `slider` |
| `text` | `String` | ✅ | — | 滑块标签文字，支持条件判断 |
| `min` | `Double` | ✅ | `0.0` | 最小值 |
| `max` | `Double` | ✅ | `10.0` | 最大值 |
| `default` | `Double` | ❌ | 等于 `min` | 默认值 |
| `step` | `Double` | ❌ | `1.0` | 每次移动的步长 |
| `format` | `String` | ❌ | `%s: %s` | 显示格式（第一个 `%s` 为标签，第二个为当前值）|

**示例：**

```yaml
Inputs:
  volume:
    type: 'slider'
    text: '&e音量'
    min: 0
    max: 100
    step: 5
    default: 50
    format: '&e音量: &f%s%%'

  purchase_amount:
    type: 'slider'
    text: '&6购买数量'
    min: 1
    max: 64
    step: 1
    default: 1
    format: '数量: %s'
```

---

### dropdown - 下拉选择框

允许玩家从预设选项列表中选择一项。

**配置项：**

| 字段 | 类型 | 必须 | 默认值 | 说明 |
|------|------|------|--------|------|
| `type` | `String` | ✅ | — | 固定值 `dropdown` |
| `text` | `String` | ✅ | — | 下拉框标签文字，支持条件判断 |
| `options` | `List<String>` | ✅ | — | 选项列表，每项支持颜色代码 |
| `default_id` | `String` | ❌ | — | 默认选中的选项（需与 `options` 中某项完全一致）|
| `width` | `Int` | ❌ | `200` | 下拉框宽度（像素）|

**示例：**

```yaml
Inputs:
  color_select:
    type: 'dropdown'
    text: '&b选择颜色'
    options:
      - '&c红色'
      - '&a绿色'
      - '&b蓝色'
      - '&e黄色'
    default_id: '&a绿色'

  server_select:
    type: 'dropdown'
    text: '&7选择服务器'
    options:
      - 'lobby'
      - 'survival'
      - 'creative'
    default_id: 'lobby'
    width: 150
```

---

### checkbox - 复选框

允许玩家切换一个开/关状态。

**配置项：**

| 字段 | 类型 | 必须 | 默认值 | 说明                     |
|------|------|------|--------|------------------------|
| `type` | `String` | ✅ | — | 固定值 `checkbox`         |
| `text` | `String` | ✅ | — | 复选框标签文字，支持条件判断         |
| `default` | `Boolean` | ❌ | `false` | 默认是否勾选                 |
| `on_true` | `String` | ❌ | `true` | 勾选时传递给动作的值  |
| `on_false` | `String` | ❌ | `false` | 未勾选时传递给动作的值 |

**示例：**

```yaml
Inputs:
  enable_notify:
    type: 'checkbox'
    text: '&a开启公告通知'
    default: true

  pvp_mode:
    type: 'checkbox'
    text: '&c开启 PvP 模式'
    default: false
    on_true: 'enabled'    # 勾选时 $(pvp_mode) = "enabled"
    on_false: 'disabled'  # 未勾选时 $(pvp_mode) = "disabled"
```

**在动作中使用：**

```yaml
Bottom:
  confirm:
    actions:
      - 'console: pvp set %player_name% $(pvp_mode)'
      # 勾选时执行: pvp set Steve enabled
      # 未勾选时执行: pvp set Steve disabled
```

---

## 完整示例

```yaml
Title: '&6玩家设置'

Inputs:
  nickname:
    type: 'input'
    text: '&e昵称'
    default: '%player_name%'
    max_length: 16

  chat_volume:
    type: 'slider'
    text: '&b聊天音量'
    min: 0
    max: 10
    default: 5
    format: '聊天音量: %s'

  language:
    type: 'dropdown'
    text: '&a语言'
    options:
      - '简体中文'
      - 'English'
    default_id: '简体中文'

  join_notify:
    type: 'checkbox'
    text: '&7接收玩家上线提醒'
    default: true

Bottom:
  type: 'confirmation'
  confirm:
    text: '&a[ 保存设置 ]'
    actions:
      - 'set-data: nickname $(nickname)'
      - 'set-data: chat_volume $(chat_volume)'
      - 'set-data: language $(language)'
      - 'set-data: join_notify $(join_notify)'
      - 'tell: &a设置已保存！'
      - 'sound: entity.experience_orb.pickup'
  deny:
    text: '&c[ 取消 ]'
    actions:
      - 'tell: &7操作已取消。'
```
