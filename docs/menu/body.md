# 🧩 内容组件 (Body)

`Body` 节点用于在菜单主体区域显示各种内容，如纯文字消息和物品展示。

---

## 配置结构

```yaml
Body:
  组件名称:
    type: '组件类型'
    # 组件专属配置...
```

- **组件名称**：任意字符串，作为该组件的唯一标识（同一菜单内不可重复）
- **组件顺序**：按照 YAML 文件中的书写顺序从上到下排列展示

---

## 组件类型

### message - 纯文字消息

在菜单中显示一行或多行纯文本消息。

**配置项：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `type` | `String` | 固定值 `message` |
| `text` | `String` | 消息文字，支持颜色代码、PAPI 变量和条件判断 |
| `width` | `Int` | 可选，消息宽度（1-1024），不设置则使用默认宽度 |

**示例：**

```yaml
Body:
  welcome_msg:
    type: 'message'
    text: '&7欢迎来到服务器商店，点击下方按钮浏览商品。'

  separator:
    type: 'message'
    text: '&8————————————————'
```

**条件判断示例：**

```yaml
Body:
  status_msg:
    type: 'message'
    text:
      - condition: "%player_is_op% == true"
        allow: '&a✔ 当前身份：管理员'
        deny: '&7当前身份：普通玩家'
```

**宽度自定义示例：**

```yaml
Body:
  wide_message:
    type: 'message'
    text: '&7这是一条宽度为 300 的消息'
    width: 300

  normal_message:
    type: 'message'
    text: '&7这是一条使用默认宽度的消息'
```

**条件宽度示例：**

```yaml
Body:
  dynamic_width:
    type: 'message'
    text: '&7消息内容'
    width:
      - condition: "%player_is_op% == true"
        allow: 400
        deny: 200
```

---

### item - 物品展示

在菜单中展示一个物品图标，可附带名称、Lore 和描述文字。

**配置项：**

| 字段 | 类型 | 必须 | 默认值 | 说明 |
|------|------|------|--------|------|
| `type` | `String` | ✅ | — | 固定值 `item` |
| `material` | `String` | ✅ | `PAPER` | 物品材质名（Minecraft 英文 ID）|
| `name` | `String` | ✅ | — | 物品显示名称，支持颜色代码 |
| `lore` | `List<String>` | ❌ | — | 物品 Lore（描述文字列表）|
| `description` | `String` | ❌ | — | 物品下方显示的额外说明文字 |
| `width` | `Int` | ❌ | `16` | 物品图标宽度（像素）|
| `height` | `Int` | ❌ | `16` | 物品图标高度（像素）|
| `decorations` | `Boolean` | ❌ | `true` | 是否显示物品装饰效果（附魔光效等）|
| `tooltip` | `Boolean` | ❌ | `true` | 鼠标悬停时是否显示物品 Tooltip |

**示例：**

```yaml
Body:
  featured_item:
    type: 'item'
    material: 'ENCHANTED_BOOK'
    name: '&6&l神圣之剑'
    lore:
      - '&7一把传说中的武器'
      - '&c攻击力: &f+20'
      - '&e价格: &f500金币'
    description: '&f点击下方按钮购买此武器'
    width: 32
    height: 32
```

**所有字段均支持条件判断：**

```yaml
Body:
  dynamic_item:
    type: 'item'
    material: 'DIAMOND'
    name:
      - condition: "%player_level% >= 10"
        allow: '&b钻石（VIP专享）'
        deny: '&8钻石（已锁定）'
    lore:
      - condition: "%player_level% >= 10"
        allow:
          - '&7解锁状态: &a已解锁'
          - '&7达到 10 级后可购买'
        deny:
          - '&7解锁状态: &c未解锁'
          - '&7需要达到 &e10 级&7 才能购买'
```

---

## 条件隐藏组件

如果需要根据条件完全隐藏某个 Body 组件，可以将 `type` 字段设为条件判断，当条件不满足时返回 `none`：

```yaml
Body:
  admin_only_section:
    type:
      - condition: "%player_is_op% == true"
        allow: 'message'
        deny: 'none'          # 非 OP 玩家不显示此组件
    text: '&c[管理员专属] 请注意查看后台日志'
```
