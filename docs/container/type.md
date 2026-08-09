# Type

`Type` 选择 Bukkit 库存类型。未配置 `Type` 但存在 `Layout` 时，默认使用 `CHEST`；建议始终显式填写，避免菜单类型发生误判。

## 字段一览

| 值 | 逻辑尺寸 | 是否使用 `Properties` | 适合场景 |
|---|---:|---|---|
| `CHEST` | 9 列，1 至 6 行 | 通常不需要 | 商店、背包、菜单主页 |
| `HOPPER` | 5 槽 | 通常不需要 | 少量快捷按钮 |
| `DISPENSER` | 3 x 3 | 通常不需要 | 九宫格选择 |
| `DROPPER` | 3 x 3 | 通常不需要 | 九宫格选择 |
| `FURNACE` | 3 槽 | `burn_progress`、`cook_progress` | 自定义加工/进度展示 |
| `BLAST_FURNACE` | 3 槽 | `burn_progress`、`cook_progress` | 高炉样式的加工界面 |
| `SMOKER` | 3 槽 | `burn_progress`、`cook_progress` | 烟熏炉样式的加工界面 |
| `ANVIL` | 3 槽 | `input` 和铁砧属性 | 重命名、文本确认 |

每种类型的 `Layout` 行必须符合对应的逻辑槽位数量。容器类菜单使用 Bukkit 公共库存 API，不依赖 NMS 假窗口；因此公共显示和点击功能以 Bukkit API 能力为边界。

## 如何选择

- 需要 1 至 6 行按钮时选择 `CHEST`。
- 只需要 5 个快捷操作时选择 `HOPPER`，不需要为了少量按钮创建 3 行箱子。
- 需要固定 3 x 3 选择盘时选择 `DISPENSER` 或 `DROPPER`。
- 需要客户端显示火焰和箭头时选择熔炉类类型，并配置[熔炉属性](properties.md)。这些进度不会创建真实熔炉。
- 需要玩家输入名称时选择 `ANVIL`，并配置[铁砧属性](properties.md)。结果槽和输入槽由 KaMenu 控制。

熔炉和铁砧的特殊配置参见[Properties](properties.md)，普通箱子和其他静态容器只需要 `Type`、`Layout` 和 `Buttons`。

```yaml
# 5 槽快捷菜单
Type: HOPPER
Title: '&8快捷操作'
Layout:
  - 'ABCDE'
```
