# Settings

容器类菜单使用部分通用 `Settings`，并增加库存界面专用的防点击设置。Dialog 专属的 `can_escape`、`after_action` 和 `lifetime` 不用于容器类菜单，关闭和跳转由动作及 `Events.Close` 处理。

## 字段一览

| 键 | 类型 | 默认行为 | 主要用途 |
|---|---|---|---|
| `need_placeholder` | 字符串列表 | 不检查 | 打开前确认 PAPI 扩展可用 |
| `min_click_delay` | 非负整数 | `0`，单位毫秒 | 防止按钮被快速重复点击 |
| `pass_arguments.enable` | 布尔值 | 关闭 | 是否启用目标菜单参数补位 |
| `pass_arguments.default` | 列表 | 空列表 | 调用参数不足时按索引提供默认值 |
| `pass_arguments.must` | 非负整数 | 不限制 | 补位后至少需要的参数数量 |

`Settings` 只负责菜单会话级规则；它不会定义槽位或物品。槽位写在 `Layout`，物品和动作写在 `Buttons`。

## need_placeholder

在打开菜单前检查 PlaceholderAPI 和指定扩展是否存在。检查失败时会阻止菜单渲染，避免显示或条件判断不完整。

```yaml
Settings:
  need_placeholder:
    - player
    - vault
```

扩展标识符对应 `%player_name%`、`%vault_eco_balance%` 等变量的 PlaceholderAPI 扩展。该配置不会自动扫描变量，使用了扩展时应主动填写。详见[PlaceholderAPI 前置检查](../modern-dialog/setting.md#need_placeholder)。

## min_click_delay

```yaml
Settings:
  min_click_delay: 200
```

单位为绝对毫秒数，按玩家和当前容器类菜单会话记录：

- `0` 或未配置表示不限制。
- 只限制最终存在动作的有效按钮点击。
- 空槽位、隐藏按钮和没有动作的按钮不消耗冷却。
- 重开、`reset` 或切换菜单后重新计时。

商店、领取奖励、扣除经济或点券的菜单通常可设置 `150` 至 `300` 毫秒。通用防点击说明参见[全局设置](../modern-dialog/setting.md#min_click_delay)。

## pass_arguments

容器类菜单与 Dialog 共用菜单参数传递配置：

```yaml
Settings:
  pass_arguments:
    enable: true
    default: ['default', '%player_name%', '{meta:source}']
    must: 2
```

`default` 会在调用方参数不足时按索引补位，并支持 PAPI 和 KaMenu 内置变量；`must` 是完成补位后仍必须存在的最少参数数量，不满足时阻止菜单打开。打开动作中的参数由目标菜单解析；`open`、`force-open` 和 `reset` 的生命周期行为参见[动作](../modern-dialog/actions.md)与[事件](events.md)。

## 使用案例：商店防连点和参数传递

```yaml
Type: CHEST
Title: '&8购买 {arg:0}'
Settings:
  need_placeholder:
    - vault
  min_click_delay: 250
  pass_arguments:
    enable: true
    default: ['DIAMOND', '1']
    must: 1
Layout:
  - '         '
  - '    `buy`    '
  - '         '

Buttons:
  buy:
    display:
      material: DIAMOND
      name: '&b购买 {arg:1} 个 {arg:0}'
    actions:
      left:
        - 'actionbar: &a已提交购买请求'
```

调用 `open: shop DIAMOND 16` 时，菜单收到两个参数；调用 `open: shop` 时使用默认值。`min_click_delay` 只限制真正执行动作的按钮，不会限制空槽位。
