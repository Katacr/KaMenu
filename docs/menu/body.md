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
| `text` | `String` | 消息文字，支持颜色代码、PAPI 变量、条件判断和可点击文本语法 |
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

**交互式文本示例（使用 hovertext 语法）：**

```yaml
Body:
  # 单个可点击链接
  simple_link:
    type: 'message'
    text: '<text=&b[ 点击访问服务器网站 ];hover=&a点击打开我们的官方网站;url=https://example.com>'

  # 带命令的点击文本
  command_link:
    type: 'message'
    text: '<text=&e[ 点击领取每日奖励 ];hover=&6点击立即领取今日奖励;command=dailyreward claim>'

  # 普通文本和可点击文本混合
  mixed_text:
    type: 'message'
    text: '&7欢迎来到服务器！<text=&a[ 领取奖励 ];hover=&6点击领取每日奖励;command=daily> 访问<text=&b[ 官网 ];hover=&7打开官网;url=https://example.com>了解更多信息。'

  # 多个可点击区域
  multi_link:
    type: 'message'
    text: '&7功能导航：<text=&a[ 商店 ];hover=&c打开商店;command=shop> <text=&b[ 背包 ];hover=&c打开背包;command=bag> <text=&e[ 帮助 ];hover=&c查看帮助;command=help>'

  # 条件判断 + 可点击文本
  conditional_click:
    type: 'message'
    text:
      - condition: '%player_is_op% == true'
        allow: '<text=&4[ 管理面板 ];hover=&a打开管理面板;command=admin>'
        deny: '<text=&7[ 玩家面板 ];hover=&a打开玩家面板;command=player>'
```

**hovertext 语法格式：**

```
<text=显示文字;hover=悬停文字;command=指令;url=链接;newline=false>
```

**参数说明：**

| 参数 | 说明 | 必需 |
|------|------|------|
| `text` | 可点击的显示文字 | ✅ |
| `hover` | 鼠标悬停时显示的提示文字 | ❌ |
| `command` | 点击时玩家执行的指令 | ❌ |
| `url` | 点击时打开的网址链接 | ❌ |
| `newline` | 是否在文字后换行（`true`/`false`）| ❌ |

**注意事项：**
- `command` 中的命令会以玩家身份执行（不需要 `/` 前缀）
- `url` 用于打开网页链接
- 可点击区域用 `< >` 包裹
- 参数值可以用反引号 `` ` ``、单引号 `'` 或双引号 `"` 包裹
- 普通文本和可点击文本可以混合使用
- 支持颜色代码和 PAPI 变量
- 所有字段都支持条件判断

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
| `description` | `String` | ❌ | — | 物品下方显示的额外说明文字，支持颜色代码、PAPI 变量、条件判断和可点击文本语法 |
| `item_model` | `String` | ❌ | — | 物品模型标识（格式：`namespace:key`），用于显示特殊材质物品（如 1.21.7+ 的命名空间物品模型）|
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

**使用自定义物品模型示例（1.21.7+）：**

```yaml
Body:
  custom_model_item:
    type: 'item'
    material: 'DIAMOND_SWORD'
    name: '&b&l光之圣剑'
    lore:
      - '&7拥有独特外观的神器'
      - '&a攻击力: &f+50'
      - '&e稀有度: &6传说'
    item_model: 'minecraft:custom_sword'  # 使用命名空间物品模型
    description: '&f这是使用自定义模型的特殊物品'
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

**description 支持可点击文本示例：**

```yaml
Body:
  interactive_item:
    type: 'item'
    material: 'BOOK'
    name: '&a&l操作指南'
    description: '&7点击<text=&b[ 购买 ];hover=&c购买此物品;command=buy item> 或 <text=&e[ 预览 ];hover=&c查看详情;command=preview item>'

  multi_click_description:
    type: 'item'
    material: 'ENCHANTED_BOOK'
    name: '&6&l魔法书'
    description: '&7功能：<text=&a[ 传送 ];hover=&c传送到主城;command=/spawn> <text=&b[ 商店 ];hover=&c打开商店;command=/shop> <text=&e[ 帮助 ];hover=&c查看帮助;command=/help>'
```

**item_model 说明：**

`item_model` 属性用于指定物品使用的自定义模型，格式为 `namespace:key`。

- **适用版本**：1.21.7 及以上版本
- **格式要求**：`命名空间:键名`，例如 `minecraft:custom_sword` 或 `your_plugin:special_item`
- **工作原理**：将模型数据存储在物品的 PersistentDataContainer 中，由 Minecraft 客户端根据模型定义渲染特殊外观
- **用途**：用于显示具有独特外观的物品，如特殊材质的武器、道具等

---

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
