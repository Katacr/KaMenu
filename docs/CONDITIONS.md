# KaMenu 条件判断功能说明

## 概述

KaMenu 提供了通用的条件判断系统，可以在菜单的任何文本字段中使用条件判断，根据不同玩家状态动态显示不同的内容。

## 支持的位置

条件判断可以在以下所有文本字段中使用：

### 1. 菜单标题（Title）

```yaml
Title:
  - condition: "%player_is_op% == true"
    meet: '&8» &4&l高级管理面板 &8« &7[管理员]'
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
        meet: '&e管理员音量调节'
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
        meet: '&aVIP 留言板'
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
        meet: '&6管理员颜色选择'
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
        meet: '&6启用高级功能'
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
        meet: '&6[ VIP 提交 ]'
        deny: '&a[ 提交 ]'
    actions: ...
  deny:
    text:
      - condition: "%player_is_op% == true"
        meet: '&c[ 管理员取消 ]'
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
          meet: '&6[ 管理员按钮 ]'
          deny: '&a[ 普通按钮 ]'
      actions: ...
    2:
      text: '普通按钮'  # 也可以是简单字符串
      actions: ...
  exit:
    text:
      - condition: "%player_is_op% == true"
        meet: '&c[ 管理员退出 ]'
        deny: '&7[ 退出 ]'
    actions: ...
```

#### Notice 模式

```yaml
Bottom:
  type: notice
  confirm:
    text:
      - condition: "%player_level% >= 10"
        meet: '&6[ VIP 确认 ]'
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
          meet:
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
          meet:
            - 'tell: &b✓ 您是 VIP 玩家'
            - 'title: title=欢迎 VIP;subtitle=尊贵的会员'
            - 'console: give %player_name% diamond 1'
          deny:
            - 'tell: &c✗ 该功能仅限 VIP 玩家可用'
            - 'actionbar: &e达到 10 级即可解锁'
```

#### 格式说明

**条件判断动作格式：**

```yaml
actions:
  - condition: "条件表达式"
    meet:
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
| meet | 条件满足时执行的动作列表 | 是 |
| deny | 条件不满足时执行的动作列表 | 否（可选，不提供时不执行任何动作） |

**混合使用简单动作和条件判断：**

```yaml
actions:
  - 'tell: 总是执行这个动作'
  - condition: "%player_is_op% == true"
    meet:
      - 'tell: 管理员专属消息'
    deny:
      - 'tell: 普通玩家消息'
  - 'tell: 这个动作也会执行'
```

#### Title 动作（特殊格式）

`Title:` 动作支持字符串格式条件判断：

**基础格式：**

```yaml
actions:
  - 'Title: condition=%player_is_op% == true;meet=你是管理员;deny=你不是管理员'
```

**高级格式（指定标题参数）：**

```yaml
actions:
  - 'Title: condition=%player_level% >= 10;meet=title=恭喜;subtitle=升级成功;in=10;keep=60;out=20;deny=title=等级不足'
```

**注意**：这种格式仅适用于 `Title:` 动作，其他动作建议使用上面的 `actions` 格式。

## 参数说明

| 参数 | 说明 | 必需 |
|------|------|------|
| condition | 条件表达式，支持复杂逻辑 | 是 |
| meet | 条件满足时执行的内容 | 是 |
| deny | 条件不满足时执行的内容 | 是 |

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

## PAPI 变量支持

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

## 使用示例

### 示例 1：OP 判断

```yaml
actions:
  - 'Title: condition=%player_is_op% == true;meet=title=你是管理员;subtitle=拥有所有权限;in=10;keep=60;out=20;deny=title=你不是管理员;subtitle=需要获取OP权限;in=10;keep=40;out=20'
```

### 示例 2：等级判断

```yaml
Bottom:
  type: notice
  confirm:
    text: "领取奖励"
    actions:
      - 'Title: condition=%player_level% >= 10;meet=title=领取成功;subtitle=你已领取VIP奖励;deny=title=等级不足;subtitle=需要达到10级才能领取'
```

### 示例 3：多条件判断（OR）

```yaml
actions:
  - 'Title: condition=%player_is_op% == true || %player_level% >= 10;meet=title=条件满足;subtitle=是管理员或等级>=10;deny=title=条件不满足'
```

### 示例 4：条件组合（AND）

```yaml
actions:
  - 'Title: condition=%player_is_op% == true && %player_health% >= 15;meet=title=你是OP且生命值高;deny=不满足条件'
```

### 示例 5：复杂条件（括号）

```yaml
actions:
  - 'Title: condition=(%player_level% >= 5 && %player_level% <= 10) || %player_is_op% == true;meet=title=条件通过;subtitle=等级5-10或管理员;deny=不符合条件'
```

### 示例 6：数值比较

```yaml
actions:
  - 'Title: condition=%player_health% >= 10;meet=生命值充足;deny=生命值较低'
  - 'Title: condition=%player_exp_to_level% <= 100;meet=即将升级;deny=经验不足'
```

## meet/deny 内容格式

### 简单消息

直接发送聊天消息：

```yaml
'meet=条件满足的消息'
'deny=条件不满足的消息'
```

### 标题参数

使用 `title=` 和 `subtitle=` 来发送标题：

```yaml
'meet=title=恭喜;subtitle=操作成功;in=10;keep=60;out=20'
'deny=title=失败;subtitle=操作失败'
```

### 完整参数列表

| 参数 | 说明 | 默认值 |
|------|------|--------|
| title | 主标题 | 空 |
| subtitle | 副标题 | 空 |
| in | 淡入时长（tick） | 10 |
| keep | 停留时长（tick） | 60 |
| out | 淡出时长（tick） | 20 |

## 注意事项

1. **PAPI 依赖**：使用 PAPI 变量需要安装 PlaceholderAPI 插件
2. **短路求值**：逻辑运算符支持短路求值，提高性能
   - `||`：第一个条件为 true 时，不计算后续条件
   - `&&`：第一个条件为 false 时，不计算后续条件
3. **大小写**：字符串比较默认不区分大小写（`==` 和 `!=`）
4. **数值转换**：无法转换为数值的字符串会被当作 0 处理
5. **条件嵌套**：支持多层括号嵌套
6. **菜单标题**：支持条件判断，可为不同玩家显示不同的菜单顶部标题
7. **动作执行**：`actions` 键支持条件判断，可在 `meet` 和 `deny` 中执行多个动作

## 测试菜单

插件提供了三个条件判断演示菜单：

### 1. 条件动作演示（condition_demo）

```
/km open examples/condition_demo
```

该菜单演示了动作执行时的条件判断（使用 `Title:` 格式），包含以下测试项：
1. OP 判断
2. 等级判断
3. 多条件判断（OR）
4. 数值比较
5. 条件组合（AND）
6. 复杂条件（括号）

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
5. **混合使用**：展示如何在 `meet` 和 `deny` 中执行多个动作

## 未来计划

- 支持更多比较运算符
- 添加自定义变量支持
