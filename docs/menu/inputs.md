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

## 类型总览

| 类型 | 名称 | 返回值 | 常见场景 |
|------|------|--------|----------|
| `input` | 文本输入框 | 玩家输入的文本 | 玩家名、数量、搜索词、留言、指令参数 |
| `slider` | 数值滑块 | 数字，整数值会以整数形式返回 | 数量、音量、等级、范围选择 |
| `dropdown` | 单项选择按钮 | 选中项的 ID | 服务器选择、职业选择、分类筛选 |
| `checkbox` | 复选框 | 默认 `true` / `false`，可通过 `on_true` / `on_false` 自定义 | 同意条款、开关选项、二选一状态 |

输入值只会在玩家点击底部按钮提交后进入动作上下文，因此 `$(键名)` 主要用于 `Bottom` 动作、条件判断、关闭事件等提交后的逻辑；`Events.Open` 中不能读取实时输入值。

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
| `hide_text` | `Boolean` | ❌ | `false` | 是否隐藏输入框自身的标签文字 |
| `default` | `String` | ❌ | `""` | 默认填充文字 |
| `max_length` | `Int` | ❌ | `256` | 最大输入字符数 |
| `width` | `Int` | ❌ | `250` | 输入框宽度（像素）|
| `remove_chars` | `String/List` | ❌ | `""` | 捕获输入后需要删除的字符，例如 `&`、`_` |
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
    remove_chars: '&_'

  feedback:
    type: 'input'
    text: '&7留言内容'
    hide_text: true
    default: '请在此输入...'
    multiline:
      max_lines: 5
      height: 80
```

`remove_chars` 只对 `type: input` 文本输入框生效。玩家点击按钮后，KaMenu 会先捕获输入值，再按配置删除指定字符，之后动作、条件、JavaScript 参数中的 `$(player_name)` 都会读取处理后的值。

也可以使用列表写法：

```yaml
Inputs:
  command_arg:
    type: 'input'
    text: '&a请输入参数'
    remove_chars:
      - '&'
      - '_'
      - '"'
```

也可以引用 `config.yml` 中的全局字符移除列表：

```yaml
Inputs:
  command_arg:
    type: 'input'
    text: '&a请输入参数'
    remove_chars: global
```

如果 `remove_chars` 的字符串值匹配 `input-capture.remove-char-lists` 下的预设名，则会使用该全局列表；否则会按旧规则作为字面字符集合处理。

`remove_chars` 支持特殊转义字符：

| 写法 | 含义 |
|------|------|
| `\s` | 普通空格 |
| `\n` | 换行 |
| `\r` | 回车 |
| `\t` | Tab |
| `\\` | 反斜杠 `\` |

例如移除颜色符号、下划线、空格和换行：

```yaml
Inputs:
  command_arg:
    type: 'input'
    text: '&a请输入参数'
    remove_chars:
      - '&'
      - '_'
      - '\s'
      - '\n'
```

如果需要统一移除所有文本输入内容前后的空格，可在 `config.yml` 中开启 `input-capture.trim-edge-spaces`。

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

{% hint style="warning" %}
**重要：** `min` 值**必须小于** `max` 值。如果 `min` ≥ `max`，插件会：
- 在控制台输出警告日志
- 自动使用默认值（min=0.0, max=10.0）
{% endhint %}

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

### dropdown - 单项选择按钮

显示为一个可点击按钮，玩家每次点击会切换到下一个预设选项。

**配置项：**

| 字段 | 类型 | 必须 | 默认值 | 说明 |
|------|------|------|--------|------|
| `type` | `String` | ✅ | — | 固定值 `dropdown` |
| `text` | `String` | ✅ | — | 单项选择按钮标签文字，支持条件判断 |
| `hide_text` | `Boolean` | ❌ | `false` | 是否隐藏单项选择按钮自身的标签文字 |
| `options` | `List<String>` | ✅ | — | 选项列表，支持普通字符串、`id => display` 格式，以及条件判断列表 |
| `default_id` | `String` | ❌ | — | 默认选中的选项 ID |
| `width` | `Int` | ❌ | `200` | 单项选择按钮宽度（像素）|

**options 支持格式：**

1. **旧格式：显示值与提交值相同**
```yaml
options:
  - 'red'
  - 'green'
  - 'blue'
```

2. **新格式：使用 `id => display` 分离提交值与显示值**
```yaml
options:
  - 'red => &c红色'
  - 'green => &a绿色'
  - 'blue => &b蓝色'
```

3. **条件判断格式：兼容 `allow` / `deny` 返回字符串列表**
```yaml
options:
  - condition: "%player_is_op% == true"
    allow:
      - 'red => &cOP-红色'
      - 'green => &aOP-绿色'
    deny:
      - 'red => &c玩家-红色'
      - 'green => &a玩家-绿色'
```

> 当使用 `id => display` 时：
> - 左侧 `id` 会作为动作中 `$(变量名)` 的真实值
> - 右侧 `display` 是玩家在界面中看到的文本

**示例：**

```yaml
Inputs:
  color_select:
    type: 'dropdown'
    text: '&b选择颜色'
    hide_text: true
    options:
      - 'red => &c红色'
      - 'green => &a绿色'
      - 'blue => &b蓝色'
      - 'yellow => &e黄色'
    default_id: 'green'

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
    format: '%s%s'

  language:
    type: 'dropdown'
    text: '&a语言'
    options:
      - 'zh_cn => 简体中文'
      - 'en_us => English'
    default_id: 'zh_cn'

  color_select:
    type: 'dropdown'
    text: '&b选择颜色'
    hide_text: true
    options:
      - condition: "%player_is_op% == true"
        allow:
          - 'red => &cOP-红色'
          - 'green => &aOP-绿色'
          - 'blue => &bOP-蓝色'
        deny:
          - 'red => &c玩家-红色'
          - 'green => &a玩家-绿色'
          - 'blue => &b玩家-蓝色'
    default_id: 'red'

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
