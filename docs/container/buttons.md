# Buttons

`Buttons` 定义 Layout 中引用的按钮。普通按钮至少需要 `display.material`；显示值会在玩家打开或刷新时解析 PAPI、KaMenu 变量和条件结果。

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

DeluxeMenus 迁移器会把同一 `slot` 的多个候选自动生成这种结构，并保留 DM 的优先级和源文件顺序。详见[DeluxeMenus 迁移](../perm/commands.md#km-migrate-dm)。
