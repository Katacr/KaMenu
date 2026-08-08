# Settings

Container 使用部分通用 `Settings`，并增加库存界面专用的防点击设置。Dialog 专属的 `can_escape`、`after_action` 和 `lifetime` 不用于 Container，关闭和跳转由动作及 `Events.Close` 处理。

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

单位为绝对毫秒数，按玩家和当前 Container 会话记录：

- `0` 或未配置表示不限制。
- 只限制最终存在动作的有效按钮点击。
- 空槽位、隐藏按钮和没有动作的按钮不消耗冷却。
- 重开、`reset` 或切换菜单后重新计时。

商店、领取奖励、扣除经济或点券的菜单通常可设置 `150` 至 `300` 毫秒。通用防点击说明参见[全局设置](../modern-dialog/setting.md#min_click_delay)。

## pass_arguments

Container 与 Dialog 共用菜单参数传递配置：

```yaml
Settings:
  pass_arguments:
    enable: true
    default: ['default', '%player_name%', '{meta:source}']
    must: 2
```

`default` 会在调用方参数不足时按索引补位，并支持 PAPI 和 KaMenu 内置变量；`must` 是完成补位后仍必须存在的最少参数数量，不满足时阻止菜单打开。打开动作中的参数由目标菜单解析；`open`、`force-open` 和 `reset` 的生命周期行为参见[动作](../modern-dialog/actions.md)与[事件](events.md)。
