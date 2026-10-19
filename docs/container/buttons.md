# Buttons

`Buttons` 定义 Layout 中引用的按钮。普通按钮至少需要 `display.material`；显示值会在玩家打开或刷新时解析 PAPI、KaMenu 变量和条件结果。

## 字段一览

| 键 | 位置 | 作用 |
|---|---|---|
| `view_condition` | `Buttons.<id>` | 决定按钮是否显示；不等同于点击条件 |
| `update` | `Buttons.<id>` | 按周期刷新该按钮，单位 tick |
| `display` | `Buttons.<id>.display` | 定义槽位中显示的 ItemStack |
| `actions` | `Buttons.<id>.actions` | 按点击类型执行动作 |
| `variants` | `Buttons.<id>.variants` | 为同一槽位定义多个完整状态，不能与顶层 `display/actions` 混用 |

一个普通按钮使用 `display + actions`；一个有多个权限、冷却或库存状态的按钮使用 `variants`。两种写法不要混在同一个按钮下。

## 普通按钮

```yaml
Buttons:
  shop:
    view_condition: 'hasPerm.shop.use'
    display:
      material: DIAMOND
      amount: 1
      name: '&b商品'
      lore:
        - '&7余额：%vault_eco_balance%'
      item_model: 'example:shop'
      custom_model_data: '{meta:model_id}'
      glow: true
      item_flags:
        - HIDE_ATTRIBUTES
    actions:
      all:
        - 'actionbar: &7点击了商品'
      left:
        - 'refresh: shop'
      right:
        - 'close'
```

物品显示字段可使用 Bukkit 可映射的材质、数量、名称、Lore、模型、头颅、附魔、旗标、发光和不可破坏属性。详细 ItemStack 字段参见[Body 物品显示](../modern-dialog/body.md)。

## repeat 动态列表按钮

`type: repeat` 将 `Layout` 中同一按钮 ID 的所有槽位用作固定列表矩阵。槽位按从左到右、从上到下的顺序绑定数据项；每页容量由当前布局中该 ID 的槽位数决定，不会占用其他按钮的槽位。

```yaml
JavaScript:
  products: |
    JSON.stringify([
      { id: "sword", name: "铁剑", material: "IRON_SWORD", price: 100 },
      { id: "pickaxe", name: "铁镐", material: "IRON_PICKAXE", price: 150 }
    ]);

Layout:
  - 'AAAABBCCC'
  - 'AAAABBCCC'
  - 'AAAADDEEE'

Buttons:
  A:
    type: repeat
    source: '[products]'
    item:
      display:
        material: '{item.material}'
        name: '&f{item.name}'
        lore:
          - '&7价格：{item.price}'
      actions:
        left:
          - 'tell: 已选择 {item.id}，价格 {item.price}'
    empty:
      display:
        material: BARRIER
        name: '&7暂无商品'
  B:
    view_condition: '{page:A} < {pages:A}'
    display:
      material: ARROW
      name: '&e下一页'
    actions:
      left:
        - 'repeat-page: A next'
  C:
    view_condition: '{page:A} > 1'
    display:
      material: ARROW
      name: '&e上一页'
    actions:
      left:
        - 'repeat-page: A prev'
```

- `Buttons.<id>.type: repeat` 需要 `source` 和 `item.display.material`；`item.actions` 可省略。Repeat 按钮不能同时配置顶层 `display`、`actions`、`variants` 或动态 `slot`。
- `source` 支持 JavaScript 函数（`[函数名]`）、KaMenu 变量、PAPI、JSON 数组和普通列表字符串；普通字符串可用 `split` 拆分，`trim` 默认为 `true`。JavaScript 可先构建筛选、排序后的矩阵列表。
- 每个数据项字段可用 `{item.xxx}`；字符串/数字项可用 `{item.value}` 和 `{item.text}`；`{item.index}` 从 0 开始，`{item.number}` 从 1 开始。另提供 `{item.page_index}` / `{item.page_number}` 表示当前页内从 0/1 开始的位置。
- 分页变量可用于普通按钮和 repeat 项显示、条件及动作：`{page:A}`、`{pages:A}`、`{total:A}`、`{start:A}`、`{end:A}`。页码从 1 开始；`start/end` 是完整数据列表中的 0-based 半开区间。空列表值为 `page=1`、`pages=1`、`total=0`、`start=0`、`end=0`。
- 使用 `repeat-page: A next|prev|+N|-N|页码` 翻转指定列表。绝对页码从 1 开始；动作会立即刷新列表及读取该列表分页变量的按钮，不需要再写 `refresh`。它与 Container 多布局的 `page:` 翻页互相独立。
- 空列表默认将该 ID 的槽位渲染为空气且不绑定点击动作。可配置 `empty.display` 显示纯占位物；占位物同样没有点击动作。非空列表最后一页未占满的槽位始终为空气。
- 不支持 repeat 条件过滤或插件侧来源数量上限；请在 JavaScript 中准备需要展示的数据及顺序。

### display 字段

| 字段 | 常用值 | 作用 |
|---|---|---|
| `material` | `DIAMOND`、`stock:剑`、`itemsadder:pack:item`、`sxitem:物品ID` | 基础材质或外部/保存物品来源，必填 |
| `amount` | `1`、`{meta:amount}` | 显示数量 |
| `name` | 颜色代码、PAPI、内置变量 | 物品名称 |
| `lore` | 字符串列表 | 物品 Lore |
| `item_model` | `namespace:model` | 新版 Item Model 键 |
| `tooltip_style` | `namespace:style` | 自定义 tooltip 背景与边框样式（1.21.4+） |
| `custom_model_data` | 整数或变量 | 自定义模型数据 |
| `skull_owner` | 玩家名 | 玩家头颅名称 |
| `skull_texture` | Base64 或纹理值 | 玩家头颅纹理 |
| `enchantments` | `sharpness: 5` | 附魔和等级 |
| `item_flags` | `HIDE_ATTRIBUTES` | 隐藏物品属性 |
| `glow` | `true` / `false` | 附魔光效：`true` 强制发光，`false` 取消光效；插件按核心版本自动选择实现（1.20.5+ 用原版光效组件，旧核心回退附魔模拟） |
| `unbreakable` | `true` / `false` | 设置不可破坏 |
| `hide_tooltip` | `true` / `false` | 隐藏整个物品提示框（1.20.5+） |
| `hide_components` | `true` / `false` | 仅隐藏原版自动组件行，保留自定义名称与 Lore |
| `color` | `255,0,0`、`#FF0000` | 染色：皮革护甲、药水、烟花效果 |
| `damage` | 整数 | 已损失耐久（仅可损坏物品） |

### 物品旗标与 Tooltip

`item_flags` 使用 Bukkit `ItemFlag` 枚举名，大小写不敏感；通常写成大写字符串列表。旗标只隐藏对应的提示行，不会移除附魔、属性、耐久等物品数据：

| 旗标 | 隐藏的提示内容 |
|---|---|
| `HIDE_ENCHANTS` | 附魔信息 |
| `HIDE_ATTRIBUTES` | 属性修饰符 |
| `HIDE_UNBREAKABLE` | 不可破坏信息 |
| `HIDE_DESTROYS` | 可破坏方块信息 |
| `HIDE_PLACED_ON` | 可放置方块信息 |
| `HIDE_POTION_EFFECTS` | 药水效果信息（新版本的对应显示可能归入额外提示） |
| `HIDE_DYE` | 染色信息 |
| `HIDE_ARMOR_TRIM` | 盔甲纹饰信息 |
| `HIDE_ADDITIONAL_TOOLTIP` | 物品类型额外提示，如部分食物、药水或特殊物品提示 |

旗标枚举由服务端 Bukkit/Paper API 决定：旧版本没有的新旗标会被跳过并记录 `item_flags` 警告。可在同一列表里使用多个旗标。

```yaml
display:
  material: DIAMOND_SWORD
  name: '&b展示用武器'
  lore:
    - '&7物品数据仍然保留'
  enchantments:
    sharpness: 5
  item_flags:
    - HIDE_ENCHANTS
    - HIDE_ATTRIBUTES
    - HIDE_UNBREAKABLE
```

三个相关字段的效果不同：

- `item_flags`：选择性隐藏原版提示行；可以与自定义名称、Lore 和未隐藏的提示行共存。
- `hide_components: true`：一次隐藏原版自动生成的组件提示行，但保留自定义名称、Lore、物品模型、自定义模型数据和 Tooltip 样式。Paper 使用 `tooltip_display.hidden_components`；Spigot 等非 Paper 核心退化为以下常见旗标：`HIDE_ATTRIBUTES`、`HIDE_UNBREAKABLE`、`HIDE_ENCHANTS`、`HIDE_PLACED_ON`、`HIDE_DESTROYS`、`HIDE_DYE`、`HIDE_ARMOR_TRIM`、`HIDE_ADDITIONAL_TOOLTIP`。因此非 Paper 的隐藏范围是近似实现，不保证覆盖所有数据组件。
- `hide_tooltip: true`：隐藏整个物品 Tooltip，包括名称和 Lore 的悬浮提示；优先于 `hide_components`。当两者同时为 `true` 时，以 `hide_tooltip` 为准。

`hide_tooltip` 与 `hide_components` 需要 1.20.5+ 核心；旧核心无法应用时会记录警告并跳过。它们只影响显示，不会清除物品数据。Dialog `Body.item` 的 `show_tooltip` 是 Dialog 组件级开关，与此处写入 ItemStack 的 `hide_tooltip` 不同；见 [Body 物品显示](../modern-dialog/body.md#item---物品展示)。

```yaml
Buttons:
  hidden_stats:
    display:
      material: DIAMOND_CHESTPLATE
      name: '&b装备预览'
      lore:
        - '&7自定义 Lore 保留'
      item_flags:
        - HIDE_ATTRIBUTES
        - HIDE_UNBREAKABLE
      hide_components: true
```

`tooltip_style` 设置 1.21.4+ 客户端资源包支持的背景/边框样式，格式为 `namespace:key`。客户端资源包需提供 `assets/<namespace>/textures/gui/sprites/tooltip/<key>_background` 与 `<key>_frame` 对应贴图；缺少资源时客户端无法显示自定义样式。该字段与隐藏 Tooltip 的字段不要同时使用：隐藏 Tooltip 后没有可见框架可供样式化。

`glow` 是三态：省略表示不改发光状态；`true` 强制显示附魔光效；`false` 取消光效。1.20.5+ 优先使用原版光效组件；旧核心的 `true` 使用隐藏附魔模拟，`false` 无法取消已有光效并会记录警告。

外部物品只有在对应插件已启用且 ID 有效时才能创建；未知字段会产生警告，不应依赖私有 NBT。

支持的外部物品前缀：`itemsadder:`（短别名 `ia:`）、`oraxen:`、`craftengine:`（短别名 `ce:`）、`sxitem:`（短别名 `sx:`，SX-Item 插件）。SX-Item 物品会显示其本体（含显示名、Lore 与自身模型），悬浮文本随物品展示；其 `&item:[sxitem:物品ID]` 内联小图标会尝试按物品自身 `item_model` 解析精灵。

需要完整预览自由槽中的真实物品时，`material` 可写为 `[FREE:<id>]`。该写法保留实际 ItemStack 元数据，详见[自由槽位](free-slots.md#完整物品预览)。

### Lore 条件行

`display.lore` 可以按顺序混写静态字符串和条件 Map。条件分支可返回一行或多行，结果会插入条件所在位置：

```yaml
Buttons:
  status:
    display:
      material: BOOK
      name: '&e状态'
      lore:
        - '&7固定 Lore 1'
        - condition: 'hasPerm.shop.vip'
          allow:
            - '&aVIP 状态'
            - '&7专属折扣已启用'
          deny: '&7普通玩家状态'
        - '&7固定 Lore 2'
    actions:
      left:
        - 'refresh: status'
```

如果 `lore` 全部由条件 Map 组成，则按首个返回非空内容的候选处理；只要存在普通字符串，就按 YAML 顺序逐项展开。PAPI 和 KaMenu 变量会在选中分支后照常解析。

只控制一行 Lore 时可使用快捷写法：

```yaml
lore:
  - '&7公共说明'
  - '&aVIP 专属说明 {condition: hasPerm.shop.vip}'
```

条件不成立时整行会被跳过。快捷条件必须使用 `{condition: 表达式}` 并放在行尾。

`view_condition` 是显示条件，不是点击条件；点击时需要再次检查的条件应放进对应动作列表。点击键支持 `all`、`left`、`right`、`shift_left`、`shift_right`、`middle`、`drop`、`control_drop`、`double_click`、`offhand`、`number_key` 和 `number_key_1` 至 `number_key_9`。通用动作语法参见[动作](../modern-dialog/actions.md)。

## variants 状态变体

一个物理槽位需要多个完整状态时使用 `variants`：

```yaml
Buttons:
  daily:
    variants:
      - priority: 0
        condition: '!hasPerm.shop.daily_cooldown'
        display:
          material: DIAMOND
          name: '&a领取每日钻石'
        actions:
          left:
            - 'console: give %player_name% DIAMOND 1'
            - 'refresh: *'
      - priority: 1
        display:
          material: STONE
          name: '&c今日已领取'
        actions:
          left:
            - 'tell: &c明天再来吧'
```

- `priority` 越小越优先；相同优先级保持 YAML 声明顺序。
- 所有变体未设置 `priority` 时，严格按 YAML 从上到下选择第一个满足 `condition` 的变体。
- 变体必须拥有完整 `display`，并至少指定 `display.material`；动作属于该变体自己的 `actions`。
- 缺少 `condition` 表示始终满足，通常作为最后的兜底状态。
- 渲染和点击都会重新选择变体，避免权限或冷却变化后执行旧状态动作。
- 不能同时配置 `variants` 和按钮顶层的 `display` / `actions`。

DeluxeMenus 迁移器会把同一 `slot` 的多个候选自动生成这种结构，并保留 DM 的优先级和源文件顺序。详见[菜单迁移总览](migration.md#deluxemenus-迁移教程)。

## 使用案例：带库存判断的购买按钮

```yaml
Buttons:
  buy:
    update: 20
    variants:
      - priority: 0
        condition: '{checkitem:[hand;amt]} >= 1 && %vault_eco_balance% >= 100'
        display:
          material: DIAMOND
          name: '&a购买钻石'
          lore:
            - '&7价格：100 金币'
        actions:
          left:
            - 'money: type=take;num=100'
            - 'item: type=give;mats=DIAMOND;amount=1'
            - 'refresh'
      - priority: 1
        display:
          material: GRAY_STAINED_GLASS_PANE
          name: '&c条件不足'
        actions:
          left:
            - 'actionbar: &c你没有足够的物品或金币'
```

`view_condition` 适合隐藏整个按钮；`variants.condition` 适合在同一槽位保留按钮位置但切换显示和动作。点击时 KaMenu 会再次选择当前变体，因此不要只依赖打开时的显示状态。
