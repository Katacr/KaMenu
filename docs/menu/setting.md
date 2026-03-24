# ⚙️ 全局设置 (Settings)

`Settings` 节点用于配置菜单的全局行为参数，包括关闭方式、动作执行后的行为等。

---

## 配置项总览

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `can_escape` | `Boolean` | `true` | 是否允许玩家通过 ESC 键关闭菜单 |
| `after_action` | `String` | `CLOSE` | 点击按钮执行动作后的客户端行为 |

---

## can_escape 参数

### 功能说明

控制菜单的关闭方式，决定玩家是否可以通过 ESC 键关闭菜单。

### 可选值

| 值 | 说明 |
|----|------|
| `true`（默认） | 玩家可通过 ESC 键关闭菜单，关闭时将执行对应按钮的动作 |
| `false` | 玩家必须点击指定按钮才能关闭菜单，禁用 ESC 退出功能 |

### 配置示例

```yaml
Settings:
  can_escape: false  # 强制玩家必须点击按钮关闭菜单
```

### 使用场景

**推荐使用 `true`（默认）：**
- 普通菜单，允许玩家随时退出
- 需要灵活关闭的场景

**推荐使用 `false`：**
- 重要确认菜单（如确认删除、确认支付）
- 管理员操作菜单
- 需要确保用户选择一个选项的场景

### ESC 关闭的默认动作

当 `can_escape: true` 时，按 ESC 键会执行对应按钮的动作：

| 菜单类型 | 执行的按钮 |
|---------|-----------|
| `notice` | `confirm` / `button1` 按钮 |
| `confirmation` | `deny` 按钮 |
| `multi` | `exit` 按钮 |

**示例配置：**

```yaml
Settings:
  can_escape: true

Bottom:
  type: 'confirmation'
  confirm:
    text: '&a[ 确认 ]'
    actions:
      - 'tell: &a你选择了确认'
  deny:
    text: '&c[ 取消 ]'
    actions:
      - 'tell: &c你选择了取消'  # 按 ESC 键时执行此动作
```

---

## after_action 参数

### 功能说明

定义玩家点击按钮后，客户端在等待服务器响应期间的行为。

### 背景：为什么需要此参数？

服务器与客户端之间的网络通信存在一定的延迟：
- **正常情况**：延迟仅为几十毫秒
- **网络差/服务器负载高**：延迟可能达到 1 秒或更长

在此期间，如果玩家对游戏世界进行操作（如移动物品、丢弃物品等），可能导致后续菜单逻辑与实际状态不一致，引发异常行为。

`after_action` 参数用于声明在客户端本地执行指定操作，防止玩家在服务器响应期间进行非法操作，确保菜单逻辑的完整性。

### 可选值

| 值 | 客户端行为 | 适用场景 |
|----|-----------|---------|
| `CLOSE`（默认） | 直接关闭菜单界面 | 没有二级菜单或无需处理后续行为的场景 |
| `NONE` | 不执行任何本地行为 | 大部分场景，点击后的动作均由服务器决定 |
| `WAIT_FOR_RESPONSE` | 显示遮罩界面，等待服务器响应 | 有二级菜单、涉及重要操作的场景 |

### 配置示例

```yaml
Settings:
  after_action: CLOSE  # 默认值
  # after_action: NONE
  # after_action: WAIT_FOR_RESPONSE
```

### 详细说明

#### 1. CLOSE（默认）

点击按钮后，客户端立即关闭菜单界面。

**优点：**
- 简单直接
- 用户体验流畅

**缺点：**
- 无法防止网络延迟期间的非法操作

**适用场景：**
- 没有二级菜单的简单菜单
- 无需处理后续行为的场景

**示例：**

```yaml
Settings:
  after_action: CLOSE

Bottom:
  type: 'notice'
  confirm:
    text: '&a[ 关闭 ]'
    actions:
      - 'close'
```

#### 2. NONE

点击按钮后，客户端不执行任何本地行为，所有逻辑由服务器控制。

**优点：**
- 灵活性最高
- 服务器完全控制菜单行为
- **大部分场景的推荐选择**

**缺点：**
- 需要在动作列表中明确关闭菜单

**适用场景：**
- 大部分菜单（推荐默认使用）
- 需要完全由服务器控制行为的场景
- 需要根据条件决定是否关闭的场景

**示例：**

```yaml
Settings:
  after_action: NONE

Bottom:
  type: 'confirmation'
  confirm:
    text: '&a[ 确认删除 ]'
    actions:
      - condition: '%player_balance% >= 1000'
        allow:
          - 'console: eco take %player_name% 1000'
          - 'tell: &a扣款成功！'
          - 'close'  # 手动关闭菜单
        deny:
          - 'tell: &c余额不足！'
          # 不关闭菜单，让玩家重新选择
  deny:
    text: '&c[ 取消 ]'
    actions:
      - 'close'
```

#### 3. WAIT_FOR_RESPONSE

点击按钮后，客户端显示遮罩界面，等待服务器响应后继续操作。

**优点：**
- 完全防止网络延迟期间的非法操作
- 适合重要操作
- 用户体验稳定

**缺点：**
- 需要确保关闭菜单（否则会卡在遮罩界面）
- 增加一步等待时间

**适用场景：**
- 有二级菜单（会自动打开新菜单）
- 涉及重要操作（交易、权限变更）
- 网络环境不稳定或 TPS 较低的服务器

{% hint style="warning" %}
**重要提示：**

使用 `WAIT_FOR_RESPONSE` 时，如果按钮**没有二级菜单功能**，必须在动作列表中添加 `close` 动作。否则客户端将持续显示遮罩界面，玩家无法操作。
{% endhint %}

**示例（带二级菜单）：**

```yaml
Settings:
  after_action: WAIT_FOR_RESPONSE

Bottom:
  type: 'multi'
  buttons:
    open_sub_menu:
      text: '&a[ 打开子菜单 ]'
      actions:
        - 'open: shop/weapons'  # 打开二级菜单，会自动关闭遮罩
    exit:
      text: '&c[ 退出 ]'
      actions:
        - 'close'
```

**示例（无二级菜单，必须手动关闭）：**

```yaml
Settings:
  after_action: WAIT_FOR_RESPONSE

Bottom:
  type: 'confirmation'
  confirm:
    text: '&a[ 确认支付 ]'
    actions:
      - 'console: eco take %player_name% 1000'
      - 'tell: &a支付成功！'
      - 'close'  # 必须：关闭菜单，移除遮罩
  deny:
    text: '&c[ 取消 ]'
    actions:
      - 'close'  # 必须：关闭菜单，移除遮罩
```

---

## 完整示例

### 示例 1：普通商店菜单（推荐配置）

```yaml
Title: '&8» &6&l服务器商店 &8«'

Settings:
  can_escape: true        # 允许 ESC 退出
  after_action: NONE      # 由服务器控制
  pause: false

Bottom:
  type: 'multi'
  buttons:
    buy:
      text: '&a[ 购买 ]'
      actions:
        - 'console: give %player_name% diamond 1'
        - 'console: eco take %player_name% 100'
        - 'tell: &a购买成功！'
        - 'close'
    exit:
      text: '&c[ 退出 ]'
      actions:
        - 'tell: &c再见！'
        - 'close'
```

### 示例 2：重要确认菜单

```yaml
Title: '&8» &c&l确认删除 &8«'

Settings:
  can_escape: false                # 禁止 ESC 退出
  after_action: WAIT_FOR_RESPONSE  # 防止网络延迟操作
  pause: false

Bottom:
  type: 'confirmation'
  confirm:
    text: '&c[ 确认删除 ]'
    actions:
      - condition: '%player_name% == target_player'
        allow:
          - 'tell: &a已删除目标物品'
          - 'close'  # 必须：关闭菜单
        deny:
          - 'tell: &c你不是物品所有者！'
          - 'close'
  deny:
    text: '&a[ 取消 ]'
    actions:
      - 'tell: &a已取消删除'
      - 'close'  # 必须：关闭菜单
```

### 示例 3：管理员操作菜单

```yaml
Title: '&8» &4&l管理员工具 &8«'

Settings:
  can_escape: false                # 禁止 ESC 退出
  after_action: NONE               # 由服务器控制
  pause: false

Bottom:
  type: 'multi'
  buttons:
    ban:
      text: '&c[ 封禁玩家 ]'
      actions:
        - 'open: admin/ban_player'
    kick:
      text: '&e[ 踢出玩家 ]'
      actions:
        - 'open: admin/kick_player'
    exit:
      text: '&7[ 返回 ]'
      actions:
        - 'open: main_menu'
```

### 示例 4：简单通知菜单

```yaml
Title: '&8» &a&l通知 &8«'

Settings:
  can_escape: true        # 允许 ESC 退出
  after_action: CLOSE      # 直接关闭
  pause: false

Body:
  message:
    type: 'message'
    text: |
      &a欢迎访问我们的服务器！
      &7请遵守服务器规则，共同维护良好的游戏环境。

Bottom:
  type: 'notice'
  confirm:
    text: '&a[ 确定 ]'
    actions:
      - 'tell: &a已阅读通知'
```

---

## 最佳实践

### 1. 默认配置推荐

对于大多数菜单，推荐使用以下默认配置：

```yaml
Settings:
  can_escape: true
  after_action: NONE
  pause: false
```

### 2. 重要操作配置

对于涉及重要操作（删除、支付、权限变更等）的菜单：

```yaml
Settings:
  can_escape: false
  after_action: WAIT_FOR_RESPONSE
  pause: false
```

**重要提示：** 使用 `WAIT_FOR_RESPONSE` 时，确保所有按钮的动作列表中都包含 `close` 动作（除非有二级菜单）。

### 3. 条件关闭配置

当需要根据条件决定是否关闭菜单时：

```yaml
Settings:
  can_escape: true
  after_action: NONE

Bottom:
  type: 'confirmation'
  confirm:
    text: '&a[ 确认 ]'
    actions:
      - condition: 'checkCondition'
        allow:
          - 'tell: &a操作成功'
          - 'close'
        deny:
          - 'tell: &c操作失败，请重试'
          # 不关闭菜单
```

### 4. 网络环境考虑

**网络良好的服务器：**
- 大部分场景使用 `after_action: NONE`
- 简单菜单可以使用 `after_action: CLOSE`

**网络不稳定或 TPS 较低的服务器：**
- 重要操作使用 `after_action: WAIT_FOR_RESPONSE`
- 确保正确处理菜单关闭

---

## 注意事项

1. **after_action 选择**
   - 默认使用 `NONE`，灵活性最高
   - 重要操作使用 `WAIT_FOR_RESPONSE`，确保数据一致性
   - `WAIT_FOR_RESPONSE` 必须配合 `close` 动作使用（除非有二级菜单）

2. **can_escape 使用**
   - 普通菜单保持 `true`，提供更好的用户体验
   - 重要确认菜单设为 `false`，强制用户选择

3. **pause 参数**
   - 仅单机有效，多人服务器无需关注
   - 通常保持默认值 `false`

4. **向后兼容**
   - 旧版本配置文件不包含 `Settings` 节点时，使用默认值
   - 建议所有新配置文件都包含 `Settings` 节点

---

## 相关文档

- [🔘 底部按钮 (Bottom)](bottom.md) - 了解按钮动作配置
- [🤖 动作 (Actions)](actions.md) - 了解所有可用的动作类型
- [⚙️ 事件 (Events)](events.md) - 了解事件系统
