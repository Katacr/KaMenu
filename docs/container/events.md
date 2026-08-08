# Events

Container 复用 KaMenu 的生命周期和动作运行时，不需要维护另一套动作语法。

| 事件 | Container 行为 | 通用文档 |
|---|---|---|
| `Open` | 库存显示前执行；动作链完成后才打开 | [Events.Open](../modern-dialog/events.md#open) |
| `Close` | KaMenu 观察到玩家关闭菜单后执行 | [Events.Close](../modern-dialog/events.md#close) |
| `Click` | 定义可由按钮 `actions` 调用的动作组 | [Events.Click](../modern-dialog/events.md#click) |
| `Tasks` | 菜单打开期间运行周期动作 | [Events.Tasks](../modern-dialog/events.md#tasks) |
| `Progress` | 熔炉进度条件发生边沿变化时执行 | [刷新机制](refresh.md) |

```yaml
Events:
  Open:
    - 'actionbar: &a菜单已打开'
  Close:
    - 'actionbar: &7菜单已关闭'
  Click:
    help:
      - 'tell: &e这是一个可复用动作组'
```

`open`、`close`、`force-open`、`force-close`、`reset` 和 `return` 与 Dialog 使用相同含义。`reset` 重新渲染当前 Container，但不会再次执行 `Events.Open`；`force-open` 和 `force-close` 会跳过生命周期事件。

事件中的条件、变量、PAPI、JavaScript、动作包和 `wait` 参见[条件判断](../modern-dialog/conditions.md)、[动作](../modern-dialog/actions.md)、[JavaScript](../modern-dialog/javascript.md)和[数据存储](../data/storage.md)。
