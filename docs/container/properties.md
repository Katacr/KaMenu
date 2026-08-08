# Properties

`Properties` 是容器类型专属配置。普通箱子、漏斗、发射器和投掷器通常不需要该节点；熔炉类使用进度属性，铁砧使用输入和重命名属性。

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
