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

| 字段 | 类型 | 说明                              |
|------|------|---------------------------------|
| `type` | `String` | 固定值 `message`                   |
| `text` | `String`/`List` | 消息文字，支持多种格式（见下方说明）              |
| `width` | `Int` | 可选，消息宽度（1-1024），不设置则使用默认宽度（200） |

---

### 文本格式支持

KaMenu 支持 **MiniMessage API** 和 **Legacy 颜色代码** 两种文本格式，可以自由选择或混合使用。

**支持的格式：**

1. **Legacy 颜色代码**：`&a绿色文本`、`&c红色文本`、`&6金色文本`
2. **MiniMessage 格式**：`<green>绿色文本</green>`、`<red>红色文本</red>`、`<gold>金色文本</gold>`

**自动检测机制：**
- 系统会自动检测文本中是否包含 MiniMessage 标签（`<...>`）
- 如果检测到 MiniMessage 标签，会使用 MiniMessage 解析
- 如果没有 MiniMessage 标签，会使用 Legacy 颜色代码解析
- 两种格式可以混合使用，系统会自动处理

**示例：**

```yaml
# 使用 Legacy 颜色代码
text: '&a欢迎来到服务器'

# 使用 MiniMessage 格式
text: '<green>欢迎来到服务器</green>'

# 混合使用
text: '&a绿色文本 <gold>金色文本</gold>'
```

**MiniMessage 优势：**
- 更现代的文本格式，支持更多样式（加粗、斜体、下划线等）
- 更清晰的标签结构，易于维护
- 与 Adventure API 完全兼容

---

### text 字段的多种格式

`text` 字段支持三种格式，分别适用于不同场景：

#### 1. 单行文本（支持 \n 换行）

最简单的格式，使用 `\n` 字符换行。

```yaml
Body:
  welcome_msg:
    type: 'message'
    text: '&7欢迎来到服务器商店\n&7点击下方按钮浏览商品'
```

**特点：**
- 适合静态多行文本
- 支持 `\n` 换行符
- 自动解析变量和颜色代码

#### 2. 列表模式（每行一个元素）

使用 YAML 列表，每行一个字符串。

```yaml
Body:
  welcome_msg:
    type: 'message'
    text:
      - '&7欢迎来到服务器商店'
      - '&7点击下方按钮浏览商品'
      - '&7祝您购物愉快！'
```

**特点：**
- 更清晰的配置格式
- 每行独立管理
- 支持长文本编辑
- 自动解析每行的变量和颜色代码

#### 3. 条件判断模式（支持 allow/deny 分支）

根据条件显示不同的文本内容。allow 和 deny 分支支持两种格式：

**格式 A：列表模式**

```yaml
Body:
  status_msg:
    type: 'message'
    text:
      - condition: '%player_is_op% == true'
        allow:
          - '&a✔ 当前身份：管理员'
          - '&7您拥有所有权限'
        deny:
          - '&7当前身份：普通玩家'
          - '&7如需升级请联系管理员'
```

**格式 B：字符串模式（支持 \n 换行）**

```yaml
Body:
  status_msg:
    type: 'message'
    text:
      - condition: '%player_is_op% == true'
        allow: '&a✔ 当前身份：管理员\n&7您拥有所有权限'
        deny: '&7当前身份：普通玩家\n&7如需升级请联系管理员'
```

**格式 A vs 格式 B 对比：**

| 特性 | 列表模式（A） | 字符串模式（B） |
|------|-------------|----------------|
| 可读性 | ✅ 更清晰，每行独立 | ⚠️ 需要使用 \n 分隔 |
| 变量解析 | ✅ 每行独立解析 | ✅ 完整文本一次性解析 |
| 适用场景 | 多行、格式化文本 | 简短文本或需要逻辑连接 |

**特点：**
- 根据条件动态显示内容
- allow/deny 分支都支持列表和字符串两种格式
- 列表模式中，每一行会独立处理并解析变量
- 列表模式和字符串模式都支持 `\n` 换行符
- 支持嵌套条件判断
- 两种格式可以混合使用（一个用列表，一个用字符串）

---

### 高级示例

**多行 + 变量 + 颜色：**

```yaml
Body:
  player_info:
    type: 'message'
    text:
      - '&a玩家名称: &f{player_name}'
      - '&a玩家余额: &e%vault_eco_balance% &7金币'
      - '&a在线时间: &e%player_time_played%'
      - '&a服务器名称: &f{gdata:server_name}'
```

**多行 + 可点击文本：**

```yaml
Body:
  welcome_msg:
    type: 'message'
    text:
      - '&7欢迎来到服务器！'
      - '&7请点击下方按钮继续'
      - '&e<text=''查看规则'';hover=''点击查看服务器规则'';command=''/rules''>'
```

**条件多行文本（列表模式）：**

```yaml
Body:
  vip_status:
    type: 'message'
    text:
      - condition: '%vault_rank% == VIP'
        allow:
          - '&a✓ 您是尊贵的 VIP 会员'
          - '&7到期时间: &e%player_vip_expiry%'
          - '&7享受所有特权服务'
        deny:
          - '&7您还不是 VIP 会员'
          - '&7点击下方按钮升级'
          - '&7仅需 &e10 &7金币/月'
```

**条件多行文本（\n 换行模式）：**

```yaml
Body:
  vip_status:
    type: 'message'
    text:
      - condition: '%vault_rank% == VIP'
        allow: '&a✓ 您是尊贵的 VIP 会员\n&7到期时间: &e%player_vip_expiry%\n&7享受所有特权服务'
        deny: '&7您还不是 VIP 会员\n&7点击下方按钮升级\n&7仅需 &e10 &7金币/月'
```

**混合模式（allow 用列表，deny 用字符串）：**

```yaml
Body:
  mixed_format:
    type: 'message'
    text:
      - condition: '%player_is_op% == true'
        allow:
          - '&6[ 管理员面板 ]'
          - '&7您拥有完全访问权限'
          - '&7可以查看所有功能'
        deny: '&7[ 普通用户面板 ]\n&7您只能访问基础功能\n&7如需更多权限请联系管理员'
```

**嵌套条件：**

```yaml
Body:
  player_type:
    type: 'message'
    text:
      - condition: '%player_is_op% == true'
        allow:
          - condition: '%player_name% == AdminPlayer'
            allow: '&6服务器管理员'
            deny: '&6管理员账号'
        deny:
          - condition: '%vault_rank% == VIP'
            allow: '&aVIP 玩家'
            deny: '&7普通玩家'
```

---

### 完整示例

**基础示例：**

```yaml
Body:
  welcome_msg:
    type: 'message'
    text: '&7欢迎来到服务器商店，点击下方按钮浏览商品。'

  separator:
    type: 'message'
    text: '&8————————————————'
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
<text=显示文字;hover=悬停文字;command=指令;url=链接;actions=动作列表名;newline=false>
```

**参数说明：**

| 参数 | 说明 | 必需 |
|------|------|------|
| `text` | 可点击的显示文字 | ✅ |
| `hover` | 鼠标悬停时显示的提示文字 | ❌ |
| `command` | 点击时玩家执行的指令 | ❌ |
| `url` | 点击时打开的网址链接 | ❌ |
| `actions` | 点击时执行的动作列表（Events.Click 下的键名）| ❌ |
| `newline` | 是否在文字后换行（`true`/`false`）| ❌ |

**注意事项：**
- `command` 中的命令会以玩家身份执行（不需要 `/` 前缀）
- `url` 用于打开网页链接
- `actions` 用于执行 Events.Click 下定义的动作列表
- 可点击区域用 `< >` 包裹
- 参数值可以用反引号 `` ` ``、单引号 `'` 或双引号 `"` 包裹
- 普通文本和可点击文本可以混合使用
- 支持颜色代码和 PAPI 变量
- 所有字段都支持条件判断

**点击事件优先级：**

当同时存在多个点击参数时，优先级如下（从高到低）：
1. `actions` - 执行动作列表
2. `url` - 打开链接
3. `command` - 执行指令

**使用 actions 参数示例：**

```yaml
Events:
  Click:
    greet:
      - 'tell: &a你好！欢迎来到服务器。'
      - 'sound: ENTITY_PLAYER_LEVELUP'

Body:
  welcome_msg:
    type: 'message'
    text: '<text="点击问候";actions=greet;hover=点击执行 greet 动作> 或查看 <text="网站";url=https://example.com;hover=打开官网>'
```

**使用场景：**

- **按钮复用动作列表**：多个文本执行相同的动作序列
- **条件分支**：根据玩家状态执行不同动作
- **内联动作**：无需单独定义，直接引用 Events.Click 下的动作列表
- **链接与动作混合**：同时包含链接和动作的文本

---

### item - 物品展示

在菜单中展示一个物品图标，可附带名称、Lore 和描述文字。

**配置项：**

| 字段 | 类型 | 必须 | 默认值 | 说明                                                         |
|------|------|------|--------|------------------------------------------------------------|
| `type` | `String` | ✅ | — | 固定值 `item`                                                 |
| `material` | `String` | ✅ | `PAPER` | 物品材质名（支持多种格式，见下方说明）                                        |
| `amount` | `Int` | ❌ | `1` | 物品堆叠数量（1-64），不设置则默认为 1                                       |
| `name` | `String` | ❌ | 物品默认名称 | 物品显示名称，支持颜色代码                                              |
| `lore` | `List<String>` | ❌ | — | 物品 Lore（描述文字列表）                                            |
| `description` | `String` | ❌ | — | 物品下方显示的额外说明文字，支持颜色代码、PAPI 变量、条件判断和可点击文本语法                  |
| `description_width` | `Int` | ❌ | `0` | description 文字框的宽度（1-1024，像素），不设置或设置为 0 则使用默认值 200                                  |
| `item_model` | `String` | ❌ | — | 物品模型标识（格式：`namespace:key`），用于显示特殊材质物品（如 1.21.7+ 的命名空间物品模型） |
| `width` | `Int` | ❌ | `16` | 物品图标宽度（像素）                                                 |
| `height` | `Int` | ❌ | `16` | 物品图标高度（像素）                                                 |
| `show_overlays` | `Boolean` | ❌ | `true` | 是否显示物品叠加层（耐久条、冷却、数量等）                               |
| `tooltip` | `Boolean` | ❌ | `true` | 鼠标悬停时是否显示物品 Tooltip                                        |

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
    width: 16
    height: 16

  # 带自定义 description 宽度的物品
  custom_width_item:
    type: 'item'
    material: 'DIAMOND'
    name: '&b&l钻石'
    description: '&7这是一颗稀有的钻石\n&7价值1000金币'
    description_width: 200  # 自定义 description 宽度
    width: 16
    height: 16

  # 隐藏叠加层（耐久条、数量、冷却等）
  no_overlays_item:
    type: 'item'
    material: 'DIAMOND_SWORD'
    name: '&6&l传奇之剑'
    show_overlays: false  # 不显示耐久条、数量等叠加层
    width: 16
    height: 16

  # 带自定义数量的物品
  custom_item:
    type: 'item'
    material: 'DIAMOND'
    name: '&b&l钻石'
    amount: 16  # 设置堆叠数量为 16
    width: 16
    height: 16
```

**amount 属性说明：**

`amount` 属性用于设置物品的堆叠数量，默认值为 `1`。

- **数值范围**：1-64（根据物品的最大堆叠数而定）
- **适用场景**：
  - 商店中显示商品堆叠数
  - 背包预览中显示物品数量
  - 装备展示中显示多个物品

- **使用示例：**

  **示例 1：单个物品**
  ```yaml
  single_item:
    type: 'item'
    material: 'DIAMOND_SWORD'
    name: '&6钻石剑'
    amount: 1  # 单个物品（默认值）
    width: 16
    height: 16
  ```

  **示例 2：堆叠物品**
  ```yaml
  stack_item:
    type: 'item'
    material: 'DIAMOND'
    name: '&b钻石'
    amount: 64  # 最大堆叠数
    width: 16
    height: 16
  ```

  **示例 3：条件控制数量**
  ```yaml
  vip_item:
    type: 'item'
    material: 'GOLD_INGOT'
    name: '&e金锭'
    amount:
      - condition: '%vault_rank% == VIP'
        allow: 32  # VIP 玩家显示 32 个
        deny: 16  # 普通玩家显示 16 个
    width: 16
    height: 16
  ```

  **示例 4：show_overlays: false 时显示数量**
  ```yaml
  # 当 show_overlays: false 时，堆叠数量会被隐藏
  # 可以将数量写在 name 属性中以便在界面上显示
  item_with_amount:
    type: 'item'
    material: 'DIAMOND'
    name: '&b钻石 x64'  # 在名称中显示数量
    amount: 64  # 设置实际数量（但不显示在图标上）
    show_overlays: false  # 禁用叠加层，数量会隐藏
    width: 16
    height: 16
  ```


- **注意事项**：
  - 如果设置的数值超过物品的最大堆叠数，会自动限制为最大值
  - 对于工具和武器等不可堆叠的物品，数量会被限制为 1
  - 槽位引用模式下，`amount` 属性不生效（显示实际槽位的物品数量）
  - **重要**：当 `show_overlays: false` 时，物品的堆叠数量（如 "64"）会被隐藏，不会显示在物品图标上。如果需要在界面上显示数量，建议将数量写在 `name` 属性中，例如 `name: '&b钻石 x64'`

```

**name 属性可选：**

`name` 属性是可选的。如果不提供，将使用物品的默认名称。

**description_width 可选：**

`description_width` 属性是可选的，用于设置 description 文字的宽度（像素）。如果不设置或设置为 0，则使用默认宽度 200。

```yaml
Body:
  # 使用物品默认名称
  diamond_sword:
    type: 'item'
    material: 'DIAMOND_SWORD'
    # 不设置 name，显示物品默认名称"钻石剑"
    width: 16
    height: 16

  # 自定义物品名称
  custom_sword:
    type: 'item'
    material: 'DIAMOND_SWORD'
    name: '&6&l传奇之剑'  # 覆盖默认名称
    width: 16
    height: 16

  # 使用 description_width 设置宽度
  wide_description_item:
    type: 'item'
    material: 'ENCHANTED_BOOK'
    name: '&6魔法书'
    description: '&7这是一本神奇的魔法书\n&7可以施放强大的法术'
    description_width: 300  # 设置 description 宽度为 300 像素
    width: 16
    height: 16

  # description 和 description_width 都支持条件判断
  conditional_description_item:
    type: 'item'
    material: 'DIAMOND'
    name: '&b钻石'
    description:
      - condition: '%player_level% >= 10'
        allow: '&a高级物品\n&7您已解锁购买权限'
        deny: '&7需要达到 &e10 &7级才能购买'
    description_width:
      - condition: '%player_level% >= 10'
        allow: 300
        deny: 200
    width: 16
    height: 16
```

{% hint style="info" %}
如果不提供 `name` 属性，系统会使用物品材质对应的默认中文名称。如果需要自定义显示名称，再设置 `name` 属性。
{% endhint %}

**物品材质名格式支持：**

KaMenu 支持多种材质名称格式，系统会自动规范化并匹配对应的 Material 枚举：

- 标准格式：`DIAMOND_SWORD`
- 小写：`diamond_sword`
- 混合大小写：`DiAMond swORd`
- 短杠：`Diamond-Sword`
- 空格：`diamond sword`
- 下划线：`diamond_sword`

**示例：**

```yaml
# 以下格式均可匹配到 DIAMOND_SWORD
material: 'diamond_sword'
material: 'Diamond_Sword'
material: 'Diamond-Sword'
material: 'diamond sword'
material: 'diAMond swORd'
```

{% hint style="info" %}
系统会自动忽略大小写、将短杠和空格替换为下划线，并合并多余的下划线，因此所有上述格式都会正确匹配到 `DIAMOND_SWORD`。
{% endhint %}

---

### 物品槽位引用

`material` 字段支持槽位引用格式，可以直接显示玩家装备槽位的物品。

**格式：**

`[SLOT]` 或 `[SLOT:PlayerName]` 或 `[SLOT:{变量}]`

**支持的槽位：**

| 槽位名 | 说明 | 对应 Bukkit 常量 |
|--------|------|-----------------|
| `HEAD` | 头部（头盔） | `EquipmentSlot.HEAD` |
| `CHEST` | 胸部（胸甲） | `EquipmentSlot.CHEST` |
| `LEGGINGS` | 护腿 | `EquipmentSlot.LEGS` |
| `BOOTS` | 靴子 | `EquipmentSlot.FEET` |
| `MAINHAND` | 主手 | `EquipmentSlot.HAND` |
| `OFFHAND` | 副手 | `EquipmentSlot.OFF_HAND` |

**基础示例：**

```yaml
Body:
  # 显示当前玩家的头盔
  player_helmet:
    type: 'item'
    material: '[HEAD]'
    width: 16
    height: 16

  # 显示指定玩家的头盔
  admin_helmet:
    type: 'item'
    material: '[HEAD:AdminPlayer]'
    width: 16
    height: 16
```

**配合变量使用：**

槽位引用支持所有变量类型（PAPI 变量、内置变量、Meta 变量等）：

```yaml
Body:
  # 使用 Meta 变量（配合 player-click 监听器）
  target_helmet:
    type: 'item'
    material: '[HEAD:{meta:player}]'
    width: 16
    height: 16

  # 使用数据存储变量
  saved_player_helmet:
    type: 'item'
    material: '[CHEST:{data:target_player}]'
    width: 16
    height: 16

  # 使用 PAPI 变量
  random_player_sword:
    type: 'item'
    material: '[MAINHAND:%random_online_player%]'
    width: 16
    height: 16
```

**空槽位处理：**

当引用的槽位为空时，系统会自动显示替代物品：

| 槽位 | 空槽位显示 |
|------|-----------|
| `HEAD` | 玩家皮肤头颅 |
| 其他槽位 | 浅灰色玻璃板，名称为"无" |

```yaml
Body:
  # 头部为空时显示玩家头颅
  helmet_display:
    type: 'item'
    material: '[HEAD:{meta:player}]'
    width: 16
    height: 16

  # 其他槽位为空时显示浅灰色玻璃板
  chestplate_display:
    type: 'item'
    material: '[CHEST:{meta:player}]'
    width: 16
    height: 16
```

**完整示例 - 玩家互动菜单：**

```yaml
# menus/inspect_player.yml
Title: '玩家信息'
Background: '#1a1a1a'

Body:
  # 显示玩家头盔
  helmet:
    type: 'item'
    material: '[HEAD:{meta:player}]'
    description: '&7查看头部装备'
    width: 16
    height: 16

  # 显示玩家胸甲
  chestplate:
    type: 'item'
    material: '[CHEST:{meta:player}]'
    description: '&7查看胸部装备'
    width: 16
    height: 16

  # 显示玩家护腿
  leggings:
    type: 'item'
    material: '[LEGGINGS:{meta:player}]'
    description: '&7查看腿部装备'
    width: 16
    height: 16

  # 显示玩家靴子
  boots:
    type: 'item'
    material: '[BOOTS:{meta:player}]'
    description: '&7查看脚部装备'
    width: 16
    height: 16

Events:
  Click:
    # 使用槽位引用显示目标玩家装备
```

{% hint style="info" %}
在槽位引用模式下，使用 `description` 属性来添加说明文字，而不是使用 `name` 属性，因为 `name` 属性在槽位有物品时不会生效。
{% endhint %}

**配合 player-click 监听器：**

```yaml
# config.yml
listeners:
  player-click:
    enabled: true
    menu: 'inspect_player'
    require-sneaking: false
```

当玩家右键其他玩家时：
1. 系统自动设置 `{meta:player}` 为被点击玩家名称
2. 打开 `inspect_player` 菜单
3. 菜单中的 `[HEAD:{meta:player}]` 等引用会显示被点击玩家的装备

**注意事项：**

1. **槽位引用模式下，以下属性不生效：**
   - `name`（空槽位除外）
   - `amount`
   - `lore`
   - `item_model`

   {% hint style="warning" %}
   **关于 `name` 属性：**
   - 当槽位有物品时，使用物品本身的名称，`name` 属性不生效
   - 当槽位为空时，HEAD 槽位显示玩家头颅（使用玩家名称），其他槽位显示浅灰色玻璃板（名称固定为"无"），`name` 属性不生效
   - 如果需要自定义名称，可以在 `description` 中添加说明文字
   {% endhint %}

2. **仍然支持的属性：**
   - `width` - 图标宽度
   - `height` - 图标高度
   - `show_overlays` - 叠加层
   - `tooltip` - 鼠标悬停显示
   - `description` - 下方说明文字（可以在这里添加自定义说明）

3. **变量解析顺序：**
   - 先解析变量（如 `{meta:player}`）
   - 再检查是否为槽位引用格式
   - 最后获取对应槽位的物品

4. **玩家不存在时：**
   - 如果引用的玩家不存在或已下线，会显示默认材质（PAPER）
   - 建议在使用槽位引用前先判断玩家是否在线

**高级示例 - 条件判断 + 槽位引用：**

```yaml
Body:
  # 只有当目标玩家在线时才显示装备
  player_equipment:
    type: 'item'
    material:
      - condition: '{meta:player} != null'
        allow: '[HEAD:{meta:player}]'
        deny: 'BARRIER'
    name:
      - condition: '{meta:player} != null'
        allow: '&6{meta:player} 的头盔'
        deny: '&c玩家离线'
    width: 16
    height: 16
```

---

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
    description: '&7功能：<text=&a[ 传送 ];hover=&c传送到主城;command=spawn> <text=&b[ 商店 ];hover=&c打开商店;command=shop> <text=&e[ 帮助 ];hover=&c查看帮助;command=help>'
```

**item_model 说明：**

`item_model` 属性用于指定物品使用的自定义模型，格式为 `namespace:key`。

- **适用版本**：1.21.7 及以上版本
- **格式要求**：`命名空间:键名`，例如 `minecraft:custom_sword` 或 `your_plugin:special_item`
- **工作原理**：将模型数据存储在物品的 PersistentDataContainer 中，由 Minecraft 客户端根据模型定义渲染特殊外观
- **用途**：用于显示具有独特外观的物品，如特殊材质的武器、道具等

---

**show_overlays 说明：**

`show_overlays` 属性用于控制是否显示物品的叠加层，默认值为 `true`。

- **包含的叠加层元素**：
  - **耐久条**（Durability Bar）：工具和武器的耐久度显示
  - **物品数量**（Stack Count）：堆叠数量（如 64、32 等）
  - **冷却时间**（Cooldown）：物品冷却倒计时显示

- **使用场景**：

  **场景 1：纯净展示物品图标**
  ```yaml
  Body:
    pure_icon:
      type: 'item'
      material: 'DIAMOND_SWORD'
      name: '&6&l传说之剑'
      show_overlays: false  # 不显示耐久条，只显示纯净图标
      width: 16
      height: 16
  ```

  **场景 2：显示完整物品信息**
  ```yaml
  Body:
    full_item:
      type: 'item'
      material: 'DIAMOND_PICKAXE'
      name: '&b钻石镐'
      lore:
        - '&7耐久度: &e1561/1561'
      show_overlays: true  # 显示耐久条（默认值）
      width: 16
      height: 16
  ```

  **场景 3：堆叠物品显示数量**
  ```yaml
  Body:
    stack_item:
      type: 'item'
      material: 'DIAMOND'
      name: '&b钻石 x64'
      show_overlays: true  # 显示数量 64
      width: 16
      height: 16
  ```

  **场景 4：条件控制叠加层显示**
  ```yaml
  Body:
    conditional_overlays:
      type: 'item'
      material: 'DIAMOND_SWORD'
      name: '&6钻石剑'
      show_overlays:
        - condition: '%player_is_op% == true'
          allow: false  # OP 玩家不显示叠加层
          deny: true    # 普通玩家显示叠加层
      width: 16
      height: 16
  ```

- **注意事项**：
  - 槽位引用模式下仍然支持 `show_overlays` 属性
  - 设置为 `false` 时，物品图标会更加简洁
  - 适合需要纯净展示物品外观的场景
  - **重要**：当 `show_overlays: false` 时，堆叠数量会被隐藏（叠加层包括耐久条、冷却时间和数量）

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
