# Type

`Type` 选择 Bukkit 库存类型。未配置 `Type` 但存在 `Layout` 时，默认使用 `CHEST`；建议始终显式填写，避免菜单类型发生误判。

| 值 | 逻辑尺寸 | 说明 |
|---|---:|---|
| `CHEST` | 9 列，1 至 6 行 | 普通箱子菜单 |
| `HOPPER` | 5 槽 | 漏斗菜单 |
| `DISPENSER` | 3 x 3 | 发射器菜单 |
| `DROPPER` | 3 x 3 | 投掷器菜单 |
| `FURNACE` | 3 槽 | 熔炉进度属性 |
| `BLAST_FURNACE` | 3 槽 | 高炉进度属性 |
| `SMOKER` | 3 槽 | 烟熏炉进度属性 |
| `ANVIL` | 3 槽 | 铁砧输入和结果属性 |

每种类型的 `Layout` 行必须符合对应的逻辑槽位数量。Container 使用 Bukkit 公共库存 API，不依赖 NMS 假窗口；因此公共显示和点击功能以 Bukkit API 能力为边界。

```yaml
Type: HOPPER
Title: '&8快捷菜单'
Layout:
  - 'A B C'
```

熔炉和铁砧的特殊配置参见[Properties](properties.md)，普通箱子和其他静态容器只需要 `Type`、`Layout` 和 `Buttons`。
