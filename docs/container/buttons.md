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

### display 字段

| 字段 | 常用值 | 作用 |
|---|---|---|
| `material` | `DIAMOND`、`stock:剑`、`itemsadder:pack:item` | 基础材质或外部/保存物品来源，必填 |
| `amount` | `1`、`{meta:amount}` | 显示数量 |
| `name` | 颜色代码、PAPI、内置变量 | 物品名称 |
| `lore` | 字符串列表 | 物品 Lore |
| `item_model` | `namespace:model` | 新版 Item Model 键 |
| `custom_model_data` | 整数或变量 | 自定义模型数据 |
| `skull_owner` | 玩家名 | 玩家头颅名称 |
| `skull_texture` | Base64 或纹理值 | 玩家头颅纹理 |
| `enchantments` | `sharpness: 5` | 附魔和等级 |
| `item_flags` | `HIDE_ATTRIBUTES` | 隐藏物品属性 |
| `glow` | `true` / `false` | 使用附魔光效显示 |
| `unbreakable` | `true` / `false` | 设置不可破坏 |

外部物品只有在对应插件已启用且 ID 有效时才能创建；未知字段会产生警告，不应依赖私有 NBT。

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
