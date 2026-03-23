# 🔍 条件判断

KaMenu 提供了强大的通用条件判断系统，可以在菜单的**任意文本字段**和**动作列表**中使用，根据玩家状态动态显示不同内容或执行不同操作。

---

## 支持的位置

条件判断可用于以下所有位置：

| 位置 | 说明 |
|------|------|
| `Title` | 菜单标题 |
| `Body.*.text` | Body 组件文本 |
| `Body.*.name` / `Body.*.lore` | 物品组件名称和 Lore |
| `Body.*.type` | 组件类型（用于条件隐藏组件）|
| `Inputs.*.text` | 输入组件标签文字 |
| `Bottom.*.text` | 所有按钮文字 |
| `Bottom.*.actions` | 按钮动作列表（执行条件分支）|

---

## 文本字段条件判断

### 语法

```yaml
字段名:
  - condition: "条件表达式"
    allow: '条件满足时的值'
    deny: '条件不满足时的值'
```

### 示例

**菜单标题：**

```yaml
Title:
  - condition: "%player_is_op% == true"
    allow: '&8» &4&l管理员面板 &8«'
    deny: '&8» &6&l玩家面板 &8«'
```

**输入组件标签：**

```yaml
Inputs:
  amount:
    type: 'slider'
    text:
      - condition: "%player_level% >= 10"
        allow: '&6VIP 购买数量（最多 64）'
        deny: '&7购买数量（最多 16）'
    min: 1
    max:
      - condition: "%player_level% >= 10"
        allow: '64'
        deny: '16'
```

**按钮文字：**

```yaml
Bottom:
  type: 'confirmation'
  confirm:
    text:
      - condition: "%player_level% >= 10"
        allow: '&6[ VIP 确认 ]'
        deny: '&a[ 确认 ]'
```

---

## 动作列表条件判断

在 `actions` 列表中嵌套条件，可以实现分支执行：

### 语法

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

`deny` 字段为可选项；不提供时，条件不满足则不执行任何操作。

### 混合使用

可以在同一个 `actions` 列表中混合使用普通动作和条件动作：

```yaml
actions:
  - 'sound: ui.button.click'           # 无论如何都执行
  - condition: "%player_balance% >= 100"
    allow:
      - 'console: eco take %player_name% 100'
      - 'tell: &a扣款成功！'
    deny:
      - 'tell: &c余额不足！'
  - 'close'                             # 无论如何都执行
```

---

## 条件表达式语法

### 比较运算符

| 运算符 | 说明 | 示例 |
|--------|------|------|
| `==` | 等于 | `%player_name% == Steve` |
| `!=` | 不等于 | `%world_name% != world_nether` |
| `>` | 大于 | `%player_level% > 10` |
| `>=` | 大于等于 | `%player_health% >= 10` |
| `<` | 小于 | `%player_food_level% < 18` |
| `<=` | 小于等于 | `%player_exp% <= 100` |

**注意：**
- 字符串比较（`==` 和 `!=`）默认**不区分大小写**
- 无法转换为数值的字符串在数值比较中会被当作 `0` 处理

### 逻辑运算符

| 运算符 | 说明 | 优先级 |
|--------|------|--------|
| `&&` | 逻辑与（AND）| 高 |
| `\|\|` | 逻辑或（OR）| 低 |
| `()` | 括号（改变优先级）| 最高 |

**支持短路求值：**
- `\|\|`：第一个条件为 `true` 时，不再计算后续条件
- `&&`：第一个条件为 `false` 时，不再计算后续条件

### 表达式示例

```yaml
# 单个条件
condition: "%player_level% >= 10"

# AND 条件
condition: "%player_level% >= 10 && %player_level% < 20"

# OR 条件
condition: "%player_is_op% == true || %player_level% >= 20"

# 使用括号
condition: "(%player_level% >= 5 && %player_level% <= 10) || %player_is_op% == true"
```

---

## 变量支持

条件表达式中支持以下变量格式：

| 变量格式 | 说明 | 示例 |
|---------|------|------|
| `%papi_var%` | PlaceholderAPI 变量 | `%player_level%` |
| `{data:key}` | 玩家个人数据 | `{data:vip_level}` |
| `{gdata:key}` | 全局共享数据 | `{gdata:server_status}` |

---

## 完整示例

### 示例 1：VIP 等级判断

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&a领取每日奖励'
    actions:
      - condition: "%player_level% >= 10"
        allow:
          - 'console: give %player_name% diamond 5'
          - 'tell: &a&lVIP 奖励: &f5 颗钻石'
          - 'title: title=&6领取成功;subtitle=&fVIP 专属奖励;in=5;keep=40;out=10'
        deny:
          - 'console: give %player_name% dirt 1'
          - 'tell: &7普通奖励: 1 块泥土'
          - 'tell: &e达到 10 级可领取 VIP 奖励！'
```

### 示例 2：管理员操作

```yaml
Bottom:
  type: 'multi'
  columns: 2
  buttons:
    admin_panel:
      condition: '%player_is_op% == true'
      text:
        - condition: "%player_is_op% == true"
          allow: '&4[ 管理面板 ]'
          deny: '&8[ 已锁定 ]'
      actions:
        - condition: "%player_is_op% == true"
          allow:
            - 'open: admin/tools'
          deny:
            - 'tell: &c你没有权限访问管理面板！'
```

### 示例 3：余额检查

```yaml
actions:
  - condition: "%player_balance% >= 1000"
    allow:
      - 'console: eco take %player_name% 1000'
      - 'tell: &a购买成功！已扣除 1000 金币'
      - 'sound: entity.player.levelup'
    deny:
      - 'tell: &c余额不足！需要 1000 金币'
      - 'tell: &7当前余额: &f%player_balance%'
      - 'sound: block.note_block.bass'
```

### 示例 4：复杂多条件

```yaml
actions:
  - condition: "(%player_level% >= 5 && %player_level% <= 10) || %player_is_op% == true"
    allow:
      - 'tell: &a条件通过：你是 5-10 级玩家，或者你是管理员'
    deny:
      - 'tell: &c不符合条件'
```

---

## 注意事项

1. **PAPI 依赖**：使用 `%papi_var%` 格式的变量需要安装 PlaceholderAPI 插件
2. **数值转换**：无法转换为数值的字符串在数值比较（`>`、`<` 等）中被视为 `0`
3. **大小写不敏感**：`==` 和 `!=` 运算符在字符串比较时不区分大小写
4. **deny 可省略**：动作条件中 `deny` 字段为可选；文本字段条件中 `deny` 建议填写以避免空白显示
5. **嵌套括号**：支持多层括号嵌套来构建复杂的逻辑表达式

---

## 演示菜单

插件内置了条件判断的演示菜单，可通过以下指令打开体验：

```
/km open example/actions_demo
```
