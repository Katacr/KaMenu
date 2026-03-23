# KaMenu 条件判断功能说明

## 概述

KaMenu 提供了通用的条件判断系统，可以在菜单的任何文本字段中使用条件判断，根据不同玩家状态动态显示不同的内容。

## 支持的位置

条件判断可以在以下所有文本字段中使用：

### 1. 菜单标题（Title）

```yaml
Title:
  - condition: "%player_is_op% == true"
    allow: '&8» &4&l高级管理面板 &8« &7[管理员]'
    deny: '&8» &6&l玩家面板 &8« &7[玩家]'

# 也可以是简单字符串格式（不使用条件）
Title: '&8» &6&l普通菜单 &8«'
```

### 2. Inputs 组件文本

所有输入组件的 `text` 字段都支持条件判断：

#### 滑块（slider）

```yaml
Inputs:
  volume_setting:
    type: 'slider'
    text:
      - condition: "%player_is_op% == true"
        allow: '&e管理员音量调节'
        deny: '&e玩家音量调节'
    min: 0
    max: 100
    step: 1
    default: 50
    format: '%s: %s%%'
```

#### 文本框（input）

```yaml
Inputs:
  user_feedback:
    type: 'input'
    text:
      - condition: "%player_level% >= 10"
        allow: '&aVIP 留言板'
        deny: '&7普通留言板'
    default: '请在此输入内容...'
```

#### 下拉框（dropdown）

```yaml
Inputs:
  color_select:
    type: 'dropdown'
    text:
      - condition: "%player_is_op% == true"
        allow: '&6管理员颜色选择'
        deny: '&b玩家颜色选择'
    options:
      - '&c红色'
      - '&a绿色'
      - '&b蓝色'
    default_id: '&a绿色'
```

#### 复选框（checkbox）

```yaml
Inputs:
  enable_feature:
    type: 'checkbox'
    text:
      - condition: "%player_level% >= 20"
        allow: '&6启用高级功能'
        deny: '&7启用基础功能'
    default: false
```

### 3. 底部按钮文本

所有底部按钮的文本都支持条件判断：

#### Confirmation 模式

```yaml
Bottom:
  type: confirmation
  confirm:
    text:
      - condition: "%player_level% >= 10"
        allow: '&6[ VIP 提交 ]'
        deny: '&a[ 提交 ]'
    actions: ...
  deny:
    text:
      - condition: "%player_is_op% == true"
        allow: '&c[ 管理员取消 ]'
        deny: '&c[ 以后再说 ]'
    actions: ...
```

#### Multi 模式

```yaml
Bottom:
  type: multi
  columns: 2
  buttons:
    1:
      text:
        - condition: "%player_is_op% == true"
          allow: '&6[ 管理员按钮 ]'
          deny: '&a[ 普通按钮 ]'
      actions: ...
    2:
      text: '普通按钮'  # 也可以是简单字符串
      actions: ...
  exit:
    text:
      - condition: "%player_is_op% == true"
        allow: '&c[ 管理员退出 ]'
        deny: '&7[ 退出 ]'
    actions: ...
```

#### Multi 模式 - 按钮显示条件

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
      show_condition: "%player_level% >= 10"  # 兼容写法：使用下划线
      text: '[ VIP 按钮 ]'
      actions: ...
    3:
      text: '[ 普通按钮 ]'  # 无显示条件，所有玩家可见
      actions: ...
```

**show-condition 说明：**
- 使用 `show-condition` 或 `show_condition` 两种写法均可
- 当条件不满足时，按钮完全不可见（不是禁用状态）
- 与 `condition/allow/deny` 的区别：
  - `show-condition`：控制按钮是否显示（UI层面）
  - `condition/allow/deny`：控制按钮点击后的行为（逻辑层面）

#### Notice 模式

```yaml
Bottom:
  type: notice
  confirm:
    text:
      - condition: "%player_level% >= 10"
        allow: '&6[ VIP 确认 ]'
        deny: '&a[ 确认 ]'
    actions: ...
```

### 4. 操作动作中的条件判断

动作使用 `actions` 键，支持条件判断：

#### 格式

```yaml
Bottom:
  type: multi
  buttons:
    1:
      text: '管理员按钮'
      actions:
        - condition: "%player_is_op% == true"
          allow:
            - 'tell: &a✓ 您是管理员'
            - 'open: manage_menu'
            - 'sound: entity.player.levelup;volume=1.0;pitch=1.0'
          deny:
            - 'tell: &c✗ 该功能仅限管理员可用'
            - 'sound: block.note_block.bass;volume=1.0;pitch=0.5'

    2:
      text: 'VIP 按钮'
      actions:
        - condition: "%player_level% >= 10"
          allow:
            - 'tell: &b✓ 您是 VIP 玩家'
            - 'title: title=欢迎 VIP;subtitle=尊贵的会员'
            - 'console: give %player_name% diamond 1'
          deny:
            - 'tell: &c✗ 该功能仅限 VIP 玩家可用'
            - 'actionbar: &e达到 10 级即可解锁'
```

#### 嵌套条件判断

支持在 `allow` 或 `deny` 中嵌套使用条件判断：

```yaml
Bottom:
  type: multi
  buttons:
    1:
      text: '数据操作按钮'
      actions:
        - condition: "{data:counter} == null"
          allow:
            - 'tell: &e数据不存在，已设置初始值: 1'
            - 'set-data: counter 1'
          deny:
            - 'tell: &e数据已存在，值为: {data:counter}'
            - 'wait: 20'
            - condition: "{data:counter} == 10"
              allow:
                - 'tell: &e数据为10，现已设置为1'
                - 'set-data: counter 1'
              deny:
                - 'tell: &e数据不为10，现已设置为10'
                - 'set-data: counter 10'
```

#### 格式说明

**条件判断动作格式：**

```yaml
actions:
  - condition: "条件表达式"
    allow:
      - '条件满足时执行的动作1'
      - '条件满足时执行的动作2'
    deny:
      - '条件不满足时执行的动作1'
      - '条件不满足时执行的动作2'
```

**参数说明：**

| 参数 | 说明 | 必需 |
|------|------|------|
| condition | 条件表达式，支持 PAPI 变量和复杂逻辑 | 是 |
| allow | 条件满足时执行的动作列表 | 是 |
| deny | 条件不满足时执行的动作列表 | 否（可选，不提供时不执行任何动作） |

**混合使用简单动作和条件判断：**

```yaml
actions:
  - 'tell: 总是执行这个动作'
  - condition: "%player_is_op% == true"
    allow:
      - 'tell: 管理员专属消息'
    deny:
      - 'tell: 普通玩家消息'
  - 'tell: 这个动作也会执行'
```

## 参数说明

| 参数 | 说明 | 必需 |
|------|------|------|
| condition | 条件表达式，支持复杂逻辑 | 是 |
| allow | 条件满足时执行的内容 | 是 |
| deny | 条件不满足时执行的内容 | 是 |
| show-condition | 控制按钮是否显示（仅在 Multi 模式可用） | 否 |

## 条件表达式语法

### 基础比较运算符

| 运算符 | 说明 | 示例 |
|--------|------|------|
| `==` | 等于 | `%player_name% == Steve` |
| `!=` | 不等于 | `%world_name% != world_nether` |
| `>` | 大于 | `%player_level% > 10` |
| `>=` | 大于等于 | `%player_health% >= 10` |
| `<` | 小于 | `%player_food_level% < 18` |
| `<=` | 小于等于 | `%player_exp% <= 100` |

### 逻辑运算符

| 运算符 | 说明 | 优先级 |
|--------|------|--------|
| `&&` | 逻辑与（AND） | 高 |
| `\|\|` | 逻辑或（OR） | 低 |

### 括号分组

使用括号 `()` 来控制运算优先级：

```yaml
# 先计算括号内的条件
'condition=(%player_level% >= 5 && %player_level% <= 10) || %player_is_op% == true'
```

## 变量支持

### PAPI 变量

所有 PlaceholderAPI 变量都可以在条件中使用：

```yaml
# 检查玩家是否在线
'condition=%player_is_online% == true'

# 检查玩家经济余额
'condition=%player_balance% >= 1000'

# 检查玩家等级
'condition=%player_level% >= 20'

# 检查玩家所在世界
'condition=%world_name% == world'

# 检查玩家是否是管理员
'condition=%player_is_op% == true'
```

### 内置变量

KaMenu 提供了内置变量，用于读取插件内的持久化数据：

#### 个人数据变量 `{data:key}`

读取特定玩家的个人持久化数据（由 `set-data` 动作写入）。

**格式：** `{data:<键名>}`

**示例：**

```yaml
# 检查个人数据是否存在
condition: "{data:test_key} == null"

# 检查个人数据的值
condition: "{data:vip_level} == 3"

# 检查个人数据是否为特定值
condition: "{data:nickname} == Steve"
```

**数据存在性判断：**
- 数据不存在时，`{data:key}` 会被替换为字面量字符串 `"null"`
- 可以使用 `== null` 或 `!= null` 来判断数据是否存在
- 示例：`{data:counter} != null` 表示数据存在

#### 全局数据变量 `{gdata:key}`

读取服务器级别的全局共享数据（由 `set-gdata` 动作写入）。

**格式：** `{gdata:<键名>}`

**示例：**

```yaml
# 检查全局数据是否存在
condition: "{gdata:server_status} != null"

# 检查全局数据的值
condition: "{gdata:server_status} == running"

# 检查全局数据是否为特定值
condition: "{gdata:event_winner} == Steve"
```

**数据存在性判断：**
- 数据不存在时，`{gdata:key}` 会被替换为字面量字符串 `"null"`
- 可以使用 `== null` 或 `!= null` 来判断数据是否存在

### 内置条件方法

KaMenu 提供了内置的条件判断方法，使用 `@` 符号调用：

#### 支持的方法

| 方法 | 说明 | 示例 |
|------|------|------|
| `isNum` | 判断是否为数字 | `value @ isNum` |
| `isPosNum` | 判断是否为正数 | `value @ isPosNum` |
| `hasPerm` | 判断玩家是否拥有权限 | `permission.node @ hasPerm` |
| `hasMoney` | 判断玩家是否有足够的金币 | `100 @ hasMoney` |

**使用示例：**

```yaml
# 检查输入值是否为数字
condition: "$(input_value) @ isNum"

# 检查输入值是否为正数
condition: "$(input_value) @ isPosNum"

# 检查玩家是否有权限
condition: "kamenu.admin @ hasPerm"

# 检查玩家是否有足够金币
condition: "1000 @ hasMoney"
```

### 输入框变量 $(variable)

在条件中使用输入框的值，使用 `$(变量名)` 格式：

```yaml
Inputs:
  test_input:
    type: 'input'
    text: '输入测试'
    default: ''

Bottom:
  type: multi
  buttons:
    1:
      text: '检查输入'
      actions:
        - condition: "$(test_input) == Hello World"
          allow:
            - 'tell: 你输入的是 Hello World'
          deny:
            - 'tell: 你输入的不是 Hello World'
```

### 双引号包裹

如果值包含空格或特殊字符，使用双引号包裹：

```yaml
# 字符串包含空格
condition: '$(test_input) == "Hello World"'

# 值包含运算符
condition: "$(test_input) == "value >= 10"'
```

## 使用示例

### 示例 1：OP 判断

```yaml
actions:
  - condition: "%player_is_op% == true"
    allow:
      - 'title: title=你是管理员;subtitle=拥有所有权限;in=10;keep=60;out=20'
    deny:
      - 'title: title=你不是管理员;subtitle=需要获取OP权限;in=10;keep=40;out=20'
```

### 示例 2：等级判断

```yaml
Bottom:
  type: notice
  confirm:
    text: "领取奖励"
    actions:
      - condition: "%player_level% >= 10"
        allow:
          - 'title: title=领取成功;subtitle=你已领取VIP奖励'
        deny:
          - 'title: title=等级不足;subtitle=需要达到10级才能领取'
```

### 示例 3：多条件判断（OR）

```yaml
actions:
  - condition: "%player_is_op% == true || %player_level% >= 10"
    allow:
      - 'tell: 条件满足：是管理员或等级>=10'
    deny:
      - 'tell: 条件不满足'
```

### 示例 4：条件组合（AND）

```yaml
actions:
  - condition: "%player_is_op% == true && %player_health% >= 15"
    allow:
      - 'tell: 你是OP且生命值高'
    deny:
      - 'tell: 不满足条件'
```

### 示例 5：复杂条件（括号）

```yaml
actions:
  - condition: "(%player_level% >= 5 && %player_level% <= 10) || %player_is_op% == true"
    allow:
      - 'tell: 条件通过：等级5-10或管理员'
    deny:
      - 'tell: 不符合条件'
```

### 示例 6：数值比较

```yaml
actions:
  - condition: "%player_health% >= 10"
    allow:
      - 'tell: 生命值充足'
    deny:
      - 'tell: 生命值较低'
  - condition: "%player_exp_to_level% <= 100"
    allow:
      - 'tell: 即将升级'
    deny:
      - 'tell: 经验不足'
```

### 示例 7：按钮显示条件

```yaml
Bottom:
  type: multi
  columns: 2
  buttons:
    admin_panel:
      show-condition: "%player_name% == Steve"  # 只有Steve能看到此按钮
      text: '[ 管理面板 ]'
      actions:
        - 'open: admin/tools'
    vip_panel:
      show-condition: "%player_level% >= 10"  # 10级以上能看到
      text: '[ VIP 面板 ]'
      actions:
        - 'open: vip/rewards'
    normal_panel:
      text: '[ 普通面板 ]'  # 所有人可见
      actions:
        - 'open: player/info'
```

### 示例 8：嵌套条件判断

```yaml
Bottom:
  type: multi
  buttons:
    1:
      text: '执行复杂操作'
      actions:
        - condition: "%player_is_op% == true"
          allow:
            - 'tell: 开始管理员操作...'
            - 'wait: 20'
            - condition: "%player_health% >= 15"
              allow:
                - 'tell: 生命值充足，继续操作'
                - 'console: say 管理员 %player_name% 执行了高级操作'
              deny:
                - 'tell: 生命值不足，跳过操作'
          deny:
            - 'tell: 你不是管理员'
```

### 示例 9：使用内置变量

```yaml
Bottom:
  type: multi
  buttons:
    1:
      text: '检查数据'
      actions:
        - condition: "{data:visited_count} != null"
          allow:
            - 'tell: 你已访问次数: {data:visited_count}'
          deny:
            - 'tell: 这是你的第一次访问！'
            - 'set-data: visited_count 1'

    2:
      text: '检查服务器状态'
      actions:
        - condition: "{gdata:server_event} == running"
          allow:
            - 'tell: 当前活动正在进行中'
          deny:
            - 'tell: 当前没有活动'
```

### 示例 10：使用内置条件方法

```yaml
Bottom:
  type: multi
  buttons:
    1:
      text: '检查数值'
      actions:
        - condition: "$(input_value) @ isPosNum"
          allow:
            - 'tell: 输入的是正数'
          deny:
            - 'tell: 输入的不是正数'

    2:
      text: '检查权限'
      actions:
        - condition: "kamenu.admin @ hasPerm"
          allow:
            - 'tell: 你有管理员权限'
          deny:
            - 'tell: 你没有管理员权限'
```

## 注意事项

1. **PAPI 依赖**：使用 PAPI 变量需要安装 PlaceholderAPI 插件
2. **短路求值**：逻辑运算符支持短路求值，提高性能
   - `||`：第一个条件为 true 时，不计算后续条件
   - `&&`：第一个条件为 false 时，不计算后续条件
3. **大小写**：字符串比较默认不区分大小写（`==` 和 `!=`）
4. **数值转换**：无法转换为数值的字符串会被当作 0 处理
5. **条件嵌套**：支持多层括号嵌套和动作嵌套
6. **菜单标题**：支持条件判断，可为不同玩家显示不同的菜单顶部标题
7. **动作执行**：`actions` 键支持条件判断，可在 `allow` 和 `deny` 中执行多个动作
8. **按钮显示**：Multi 模式支持 `show-condition` 控制按钮是否可见
   - `show-condition`：控制显示（不满足时按钮完全不可见）
   - `condition/allow/deny`：控制行为（点击后的执行逻辑）
9. **内置变量**：
   - `{data:key}`：读取玩家个人数据，不存在时返回 `"null"`
   - `{gdata:key}`：读取全局数据，不存在时返回 `"null"`
   - 使用 `== null` 或 `!= null` 判断数据是否存在
10. **输入框变量**：使用 `$(变量名)` 在条件中引用输入框的值
11. **双引号包裹**：包含空格或特殊字符的值需要用双引号包裹
12. **show-condition 兼容性**：支持 `show-condition`（推荐）和 `show_condition`（兼容）两种写法

## 测试菜单

插件提供了条件判断演示菜单：

### 1. 条件动作演示（condition_demo）

```
/km open examples/condition_demo
```

该菜单演示了条件判断的完整功能，包含以下测试项：
1. 菜单标题条件判断
2. 按钮文本条件判断
3. 动作执行条件判断
4. 嵌套条件判断
5. 内置变量使用
6. 按钮显示条件
7. PAPI 变量使用
8. 逻辑运算符组合

### 2. 通用条件值演示（conditional_value_demo）

```
/km open examples/conditional_value_demo
```

该菜单演示了所有文本字段的条件判断，包括：
- **菜单标题**：根据 OP 状态显示不同标题
- **滑块文本**：管理员和玩家显示不同的提示文本
- **文本框文本**：根据等级显示 VIP/普通留言板
- **下拉框文本**：管理员和玩家显示不同的选择提示
- **底部按钮文本**：确认和取消按钮根据状态显示不同文本

### 3. Actions 条件判断演示（actions_condition_demo）

```
/km open examples/actions_condition_demo
```

该菜单演示了 `actions` 键的条件判断功能，包含以下测试项：
1. **管理员按钮**：根据 OP 权限执行不同的动作
2. **VIP 按钮**：根据等级执行不同的动作，支持发送标题、控制台指令等
3. **组合条件**：使用 OR 逻辑组合多个条件
4. **多个动作**：根据输入框的值执行不同的动作列表
5. **混合使用**：展示如何在 `allow` 和 `deny` 中执行多个动作
6. **嵌套条件**：展示多层条件判断的使用

## 未来计划

- 支持更多比较运算符
- 添加自定义变量支持
