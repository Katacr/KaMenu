# Properties

`Properties` 是容器类型专属配置。普通箱子、漏斗、发射器和投掷器通常不需要该节点；熔炉类使用进度属性，铁砧使用输入和重命名属性。

## 字段一览

| 容器类型 | 字段 | 类型 | 作用 |
|---|---|---|---|
| 熔炉、高炉、烟熏炉 | `burn_progress` | 数值/变量 | 火焰进度，0 至 100% |
| 熔炉、高炉、烟熏炉 | `cook_progress` | 数值/变量 | 箭头进度，0 至 100% |
| 铁砧 | `input` | 字符串/变量 | 初始输入文本，并启用输入捕获 |
| 铁砧 | `remove_chars` | 字符串列表或预设名 | 清理玩家提交的文本 |
| 铁砧 | `repair_cost` | 非负整数/变量 | 经验等级费用 |
| 铁砧 | `maximum_repair_cost` | 非负整数/变量 | 铁砧可接受的最大费用 |
| 铁砧 | `repair_item_count` | 非负整数/变量 | 输入物品消耗数量 |

`Properties` 只会应用于对应容器类型。把熔炉字段写入箱子或把铁砧字段写入熔炉会产生警告，并不会改变容器行为。

## 熔炉类容器

适用于 `FURNACE`、`BLAST_FURNACE` 和 `SMOKER`：

```yaml
Type: FURNACE
Properties:
  burn_progress: '{meta:burn}'
  cook_progress: '{meta:cook}'
```

进度接受 `0`、`55.31`、`55.31%` 和 `100%`，并限制在 `0..100`。这里是客户端显示的火焰和加工箭头，不是真实熔炉：KaMenu 不消耗燃料，也不执行配方。

进度周期和完成事件参见[刷新机制](refresh.md)和[Events](events.md)。需要持久化、离线运行的后台熔炉应使用独立的后台插件实现。

## 使用案例：带完成事件的加工界面

```yaml
Type: FURNACE
Title: '&8加工中'
Progress-Update: 5
Properties:
  burn_progress: '{meta:fuel_pct}'
  cook_progress: '{meta:cook_pct}'

Layout:
  - 'ABC'

Events:
  Progress:
    complete:
      source: cook_progress
      condition: '{progress.current} >= 100'
      trigger_initial: false
      actions:
        - 'actionbar: &a加工完成'
        - 'actions: furnace/reward'
        - 'meta: type=set;key=cook_pct;var=`0`'

Buttons:
  A:
    display:
      material: RAW_IRON
      name: '&f加工原料'
  B:
    display:
      material: COAL
      name: '&6燃料'
  C:
    display:
      material: IRON_INGOT
      name: '&a加工结果'
```

进度值可以来自 PAPI、`meta`、`data` 或 JavaScript。KaMenu 只改变客户端看到的进度，不会自动消耗燃料、运行配方或生成产物；需要后台持续运行的熔炉逻辑应由独立插件负责。

## 铁砧

```yaml
Type: ANVIL
Properties:
  input: 'Name_%player_name%'
  remove_chars: ['&', '_']
  repair_cost: 0
  maximum_repair_cost: 40
  repair_item_count: 0
```

`input` 是初始输入文字，`remove_chars` 用于清理玩家输入，`repair_cost`、`maximum_repair_cost` 和 `repair_item_count` 控制支持的铁砧属性。捕获后可在按钮显示、`view_condition`、动作和关闭事件中使用 `$(input)`；它不能在 `Events.Open` 中使用。

把 `$(input)` 用于指令、JavaScript、条件或存储前应清理输入。铁砧结果槽由 KaMenu 控制，不应作为玩家物品存储槽。

输入清理规则参见[输入组件](../modern-dialog/inputs.md)，生命周期参见[Events](events.md)。

## 使用案例：铁砧重命名

```yaml
Type: ANVIL
Title: '&8重命名物品'
Properties:
  input: '新名称'
  remove_chars: ['&', '\n', '\r']
  repair_cost: 0
  maximum_repair_cost: 40
  repair_item_count: 0

Layout:
  - 'ABC'

Buttons:
  A:
    display:
      material: PAPER
      name: '&e当前名称：$(input)'
    actions:
      left:
        - 'tell: &a已捕获名称：$(input)'
        - 'close'
  B:
    display:
      material: NAME_TAG
      name: '&7辅助输入槽'
  C:
    display:
      material: EMERALD
      name: '&a确认：$(input)'
    actions:
      left:
        - 'tell: &a已确认名称：$(input)'
        - 'close'
```

玩家提交名称后，`$(input)` 才可用于按钮显示、条件、动作和 `Events.Close`。不要把它直接拼接到未清理的控制台指令或持久化键中。
