# 🔘 底部按钮 (Bottom)

`Bottom` 节点定义菜单底部的交互按钮区域，共有三种布局模式。

---

## 配置结构

```yaml
Bottom:
  type: '模式类型'   # notice | confirmation | multi
  # 模式专属配置...
```

---

## 三种布局模式

### notice - 单按钮模式

只显示一个确认按钮，适合信息展示或简单触发操作。

**配置项：**

| 字段 | 说明 |
|------|------|
| `confirm.text` | 按钮文字，支持颜色代码和条件判断 |
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
| `confirm.actions` | 点击确认时执行的动作列表 |
| `deny.text` | 取消按钮文字，支持条件判断 |
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

| 字段 | 说明 |
|------|------|
| `condition` | 可选，按钮显示条件；条件不满足时该按钮不显示 |
| `text` | 按钮文字，支持颜色代码和条件判断 |
| `actions` | 点击时执行的动作列表 |

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
