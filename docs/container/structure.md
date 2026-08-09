# 容器类菜单文件结构

## 顶层键

| 键 | 必需 | 作用 | 详细文档 |
|---|---|---|---|
| `Type` | 否 | 选择容器类型，缺省时按 `CHEST` 处理 | [Type](type.md) |
| `Title` | 否 | 设置库存界面标题，缺省为 `KaMenu` | [Title](title.md) |
| `Settings` | 否 | 设置依赖检查、防点击和参数规则 | [Settings](settings.md) |
| `References` | 否 | 定义当前菜单可复用的文本和模板 | [菜单引用](../config/references.md) |
| `Layout` | 是 | 定义物理槽位中的按钮排列 | [Layout](layout.md) |
| `Buttons` | 否 | 定义 Layout 引用的按钮；纯空布局可省略 | [Buttons](buttons.md) |
| `Properties` | 否 | 设置熔炉进度或铁砧输入等专属属性 | [Properties](properties.md) |
| `Update` | 否 | 按周期刷新整个容器 | [刷新机制](refresh.md) |
| `Title-Update` | 否 | 按周期刷新标题 | [刷新机制](refresh.md) |
| `Progress-Update` | 否 | 按周期刷新熔炉进度并触发进度事件 | [刷新机制](refresh.md) |
| `Events` | 否 | 配置打开、关闭、点击动作组和进度事件 | [Events](events.md) |

## 配置层级速查

```yaml
Type: CHEST                    # 容器类型
Title: '&8菜单标题'             # 顶部标题
Settings:                       # 前置、冷却和参数
References:                     # 可选：当前菜单的公共文本和模板
Layout:                         # 槽位布局，必须存在
Update: 20                      # 可选：整体刷新周期，单位 tick
Title-Update: 40                # 可选：标题刷新周期，单位 tick
Progress-Update: 5              # 可选：熔炉进度刷新周期，单位 tick
Properties:                     # 可选：熔炉或铁砧专属字段
Events:                         # 可选：生命周期和动作组
Buttons:                        # 可选：布局中引用的按钮
```

推荐按以下顺序编写：

1. 用 `Type` 选择容器并确定槽位数量。
2. 用 `Layout` 安排每个按钮所在的物理槽位。
3. 为布局引用的 ID 定义 `Buttons.<id>.display.material`。
4. 在 `Buttons.<id>.actions` 中添加点击交互。
5. 最后按需添加 `Settings`、`Properties`、刷新字段和 `Events`。

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

References:
  product_name: '&b商品'

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
      name: '{ref:product_name}'
    actions:
      left:
        - 'close'
```

`References` 可通过 `{ref:path}` 复用公共值；容器按钮还可以通过 `{self:id}` 和 `{self:<字段路径>}` 读取自身节点。完整规则参见[菜单引用](../config/references.md)。

`Body`、`Inputs`、`Bottom` 属于 Dialog 结构，不能添加到容器类菜单文件中。通用 `Events`、动作和条件不需要为容器类菜单重写，分别参见[事件](events.md)、[动作](../modern-dialog/actions.md)和[条件判断](../modern-dialog/conditions.md)。

`Layout` 是容器类菜单唯一必须存在的顶层键；其他键都可以按需求省略。一个可打开的最小菜单至少需要一个合法的布局，以及布局中引用的按钮定义。
