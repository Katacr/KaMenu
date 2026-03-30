# 🚥 事件 (Events)

KaMenu 支持在菜单的特定时刻执行预定义的动作列表，通过 `Events` 键配置。事件系统允许你在菜单打开、关闭等关键时刻执行特定逻辑。

---

## 支持的事件

| 事件名     | 触发时机     | 支持变量 |
|---------|----------|---------|
| `Open`  | 菜单打开前    | `{data:*}`, `{gdata:*}`, `{meta:*}`, `%papi_var%` |
| `Close` | 菜单关闭后    | `{data:*}`, `{gdata:*}`, `{meta:*}`, `%papi_var%`, `$(input_key)` |
| `Click`     | 待触发的动作列表 | `{data:*}`, `{gdata:*}`, `{meta:*}`, `%papi_var%` |

**重要说明：**
- `Open` 事件在菜单打开前触发，因此**不支持** `$(input_key)` 输入变量（因为输入框还未显示）
- `Close` 事件在菜单关闭后触发，支持所有变量格式，包括 `$(input_key)`

---

## 基本语法

### 简单事件（无条件执行）

```yaml
Events:
  Open:
    - 'tell: &a欢迎来到本服务器！'
    - 'sound: entity.player.levelup'
```

### 条件事件（有条件执行）

```yaml
Events:
  Open:
    - condition: "条件表达式"  # 可选
      allow:
        - '条件满足时执行的动作1'
        - '条件满足时执行的动作2'
      deny:
        - '条件不满足时执行的动作1'
        - '条件不满足时执行的动作2'
```

---

## 如何调用待触发的动作列表

**先配置动作列表**

```yaml
Events:
  Click:
    hello:
      - 'tell: &a你好！欢迎来到服务器。'
      - 'tell: &a祝你在这里玩的愉快！'
```

**使用 `actions` 动作激活：**

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&a[ 确定 ]'
    actions:
      - 'actions: hello'  # 执行上面配置的`hello`待触发的动作列表
```

**使用 `可点击文本` 激活：**

```yaml
Body:
  text:
    type: 'message'
    text: '请 <text="点击问候";actions=hello;hover=点击执行 hello 动作> 可以看到欢迎语'
```

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&a[ 确定 ]'
    actions:
      - 'hovertext: 请 <text="[点击问候]";actions=hello;hover=点击执行 hello 动作> 可以看到欢迎语'  
```
---

## 特殊动作

### Return 动作

在事件动作列表中，支持使用 `return` 动作来中断后续动作的执行：

```yaml
Events:
  Open:
    - condition: 'hasPerm.kamenu.admin'
      allow:
        - 'tell: &a欢迎管理员！'
      deny:
        - 'tell: &c你没有权限！'
        - 'return'  # 中断后续动作
        - 'tell: 你永远看不到这条消息'
```

**注意事项：**
- `Open` 事件中的 `return` 会**阻止菜单打开**
- `Close` 事件中的 `return` 只是中断剩余动作（因为菜单已经关闭）
- `return` 在条件判断的 `allow` 和 `deny` 分支中都有效

---

## 使用场景

### 1. 权限检查

**无条件检查：**

```yaml
Events:
  Open:
    - 'tell: &a欢迎来到本服务器！'
    - 'sound: entity.player.levelup'
```

**有条件检查：**

```yaml
Events:
  Open:
    - condition: '%player_is_op% == true'
      allow:
        - 'tell: &a你打开了管理员菜单'
      deny:
        - 'tell: &c你没有权限打开此菜单'
        - 'return'  # 非OP玩家看不到菜单
```

### 2. 复合条件检查

```yaml
Events:
  Open:
    - condition: '%player_level% >= 10 && %player_health% >= 15'
      allow:
        - 'tell: &a满足条件：等级>=10 且 血量>=15'
        - 'title: title=&6欢迎;subtitle=&fVIP 玩家;in=5;keep=40;out=10'
      deny:
        - 'tell: &7未达到要求：需要等级>=10 且 血量>=15'
```

### 3. 数据初始化

**首次访问检测：**

```yaml
Events:
  Open:
    - condition: '{data:first_visit} == null'
      allow:
        - 'tell: &e欢迎首次访问！已初始化数据'
        - 'set-data: first_visit %timestamp%'
        - 'set-data: visit_count 1'
        - 'title: title=&6欢迎新人;subtitle=&f这是你第一次访问;in=10;keep=60;out=20'
      deny:
        - 'tell: &7欢迎回来！这是你第 {data:visit_count} 次访问'
        - 'set-data: visit_count {data:visit_count} + 1'
```

### 4. 状态检查

```yaml
Events:
  Open:
    - condition: '{meta:temp_banned} == true'
      allow:
        - 'tell: &c你已被临时封禁，无法访问此菜单'
        - 'return'
    - condition: '%player_health% <= 5'
      allow:
        - 'tell: &e你的血量很低，请注意安全'
```

### 5. 问候语系统

```yaml
Events:
  Open:
    - condition: '%player_health% >= 20'
      allow:
        - 'tell: &a你的血量很健康！'
      deny:
        - 'tell: &7你需要注意血量了'
    - condition: '%player_level% >= 30'
      allow:
        - 'tell: &6你已经达到30级了！'
```

### 6. 关闭事件处理

```yaml
Events:
  Close:
    - 'tell: &a感谢使用本菜单'
    - 'set-meta: last_visit %timestamp%'
    - 'set-data: total_visits {data:total_visits} + 1'
    - 'sound: entity.experience_orb.pickup'
```

---

## 完整示例

### 示例 1：管理员菜单的权限控制

```yaml
Title: '&8» &4&l管理员面板 &8«'

Settings:
  can_escape: false

Events:
  Open:
    - condition: 'hasPerm.kamenu.admin'
      allow:
        - 'tell: &a欢迎访问管理员面板'
      deny:
        - 'tell: &c你没有权限访问此菜单！'
        - 'sound: block.note_block.bass'
        - 'return'

Body:
  welcome:
    type: 'message'
    text: '&7管理员控制面板 - 请谨慎操作'

Bottom:
  type: 'notice'
  confirm:
    text: '&c[ 关闭 ]'
    actions:
      - 'close'
```

### 示例 2：商店菜单的欢迎系统

```yaml
Title: '&8» &6&l服务器商店 &8«'

Settings:
  after_action: CLOSE

Events:
  Open:
    # 首次访问检测
    - condition: '{data:shop_first_visit} == null'
      allow:
        - 'tell: &a欢迎来到服务器商店！这是你的第一次访问'
        - 'set-data: shop_first_visit true'
        - 'title: title=&6欢迎;subtitle=&f初次访问奖励已发放;in=5;keep=60;out=20'
      deny:
        - 'tell: &7欢迎回来！继续选购吧'
    
    # VIP 玩家欢迎
    - condition: 'hasPerm.vip.gold'
      allow:
        - 'tell: &6尊贵的金卡会员，欢迎光临！'
    
    # 显示余额
    - 'tell: &f当前余额: &e%player_balance%'

Body:
  welcome:
    type: 'message'
    text: '&7请选择你需要的商品'

Bottom:
  type: 'notice'
  confirm:
    text: '&a[ 浏览商品 ]'
    actions:
      - 'tell: &a正在浏览商品...'
```

### 示例 3：带数据追踪的问卷菜单

```yaml
Title: '&8» &b&l玩家问卷 &8«'

Settings:
  can_escape: false
  after_action: WAIT_FOR_RESPONSE

Events:
  Open:
    # 检查是否已经填写
    - condition: '{data:questionnaire_completed} == true'
      allow:
        - 'tell: &a你已经完成了问卷，感谢参与！'
        - 'return'
    
    - 'tell: &e请填写以下问卷'
  
  Close:
    # 标记完成时间
    - 'set-data: questionnaire_completed true'
    - 'set-data: questionnaire_date %timestamp%'
    - 'tell: &a感谢你完成问卷！'
    - 'title: title=&a完成;subtitle=&f感谢你的参与;in=5;keep=40;out=10'

Inputs:
  rating:
    type: 'slider'
    text: '服务器评分 (1-10)'
    min: 1
    max: 10
    default: 5
  
  feedback:
    type: 'input'
    text: '意见或建议'
    multiline:
      max_lines: 5
      height: 100
    max_length: 500

Bottom:
  type: 'confirmation'
  confirm:
    text: '&a[ 提交 ]'
    actions:
      - 'tell: &a提交成功！'
      - 'close'
  deny:
    text: '&c[ 取消 ]'
    actions:
      - 'tell: &c已取消提交'
      - 'close'
```

### 示例 4：多条件检查菜单

```yaml
Title: '&8» &5&lVIP 专属功能 &8«'

Settings:
  can_escape: false

Events:
  Open:
    # 多重检查
    - condition: '%player_level% < 30'
      allow:
        - 'tell: &c你的等级不足 30 级'
        - 'return'
    
    - condition: '!hasPerm.vip.basic'
      allow:
        - 'tell: &c你需要 VIP 权限才能访问此菜单'
        - 'return'
    
    - condition: '%player_health% < 10'
      allow:
        - 'tell: &e你的血量较低，请注意安全'
    
    # 通过所有检查
    - 'tell: &a欢迎访问 VIP 专属功能'
    - 'sound: entity.player.levelup'

Body:
  welcome:
    type: 'message'
    text: '&6VIP 专属功能区域'

Bottom:
  type: 'notice'
  confirm:
    text: '&a[ 确定 ]'
    actions:
      - 'tell: &a开始使用 VIP 功能'
```

---

## 注意事项

1. **执行顺序**
   - `Open` 事件在菜单解析前执行
   - `Close` 事件在菜单关闭后执行
   - 事件动作列表按顺序执行

2. **Return 的作用域**
   - `Open` 事件的 `return` 会阻止整个菜单打开
   - `Close` 事件的 `return` 只是中断剩余动作
   - `return` 对嵌套条件判断有效

3. **变量限制**
   - `Open` 事件不支持 `$(input_key)`（输入框未显示）
   - `Close` 事件支持所有变量格式
   - 条件判断支持所有内置方法和运算符

4. **性能考虑**
   - 避免在事件中执行大量耗时操作
   - 使用 `return` 及时中断不需要的逻辑
   - 复杂条件判断建议使用括号明确优先级

5. **错误处理**
   - 条件判断失败时不会阻止菜单打开（除非使用 `return`）
   - 动作执行失败不会影响后续动作执行
   - 建议使用 `tell` 动作提供反馈信息

---

## 相关文档

- [🔍 条件判断](conditions.md) - 了解条件表达式的详细语法
- [🤖 动作 (Actions)](actions.md) - 了解所有可用的动作类型
- [💾 数据存储](../data/storage.md) - 了解数据存储和变量使用
