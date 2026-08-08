# Container 文件结构

## 顶层键

| 键 | 必需 | 作用 | 详细文档 |
|---|---|---|---|
| `Type` | 否 | 选择容器类型，缺省时按 `CHEST` 处理 | [Type](type.md) |
| `Title` | 否 | 设置库存界面标题，缺省为 `KaMenu` | [Title](title.md) |
| `Settings` | 否 | 设置依赖检查、防点击和参数规则 | [Settings](settings.md) |
| `Layout` | 是 | 定义物理槽位中的按钮排列 | [Layout](layout.md) |
| `Buttons` | 否 | 定义 Layout 引用的按钮；纯空布局可省略 | [Buttons](buttons.md) |
| `Properties` | 否 | 设置熔炉进度或铁砧输入等专属属性 | [Properties](properties.md) |
| `Update` | 否 | 按周期刷新整个容器 | [刷新机制](refresh.md) |
| `Title-Update` | 否 | 按周期刷新标题 | [刷新机制](refresh.md) |
| `Progress-Update` | 否 | 按周期刷新熔炉进度并触发进度事件 | [刷新机制](refresh.md) |
| `Events` | 否 | 配置打开、关闭、点击动作组和进度事件 | [Events](events.md) |

## 完整骨架

```yaml
Type: CHEST
Title: '&8容器菜单'

Settings:
  need_placeholder:
    - player
  min_click_delay: 200
  pass_arguments:
    enable: true
    default: ['default']

Update: 20
Title-Update: 40

Layout:
  - '#########'
  - '####`shop`####'
  - '#########'

Properties: {}

Events:
  Open:
    - 'actionbar: &a菜单已打开'
  Close:
    - 'actionbar: &7菜单已关闭'

Buttons:
  '#':
    display:
      material: GRAY_STAINED_GLASS_PANE
      name: ' '
  shop:
    display:
      material: DIAMOND
      name: '&b商品'
    actions:
      left:
        - 'close'
```

`Body`、`Inputs`、`Bottom` 属于 Dialog 结构，不能添加到 Container 文件中。通用 `Events`、动作和条件不需要为 Container 重写，分别参见[事件](events.md)、[动作](../modern-dialog/actions.md)和[条件判断](../modern-dialog/conditions.md)。
