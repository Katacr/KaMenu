# Events

容器类菜单复用 KaMenu 的生命周期和动作运行时，不需要维护另一套动作语法。

## 事件一览

| 事件 | 触发时机 | 常见用途 | 注意事项 |
|---|---|---|---|
| `Open` | 菜单显示前 | 初始化 `meta`、检查条件、发送提示 | `return` 会阻止打开 |
| `Close` | 玩家关闭菜单后 | 清理临时状态、保存结果、返回上一级 | 不能假设玩家一定主动按了按钮 |
| `Click` | 不自动触发 | 定义可复用动作组 | 通过 `actions: <id>` 调用 |
| `Tasks` | 菜单打开期间按周期运行 | 倒计时、状态检查、轻量刷新 | 含 `wait` 时考虑 `skip_if_running` |
| `Progress` | 熔炉进度条件由假变真 | 完成、耗尽、奖励动作 | 只适用于熔炉类容器 |

`Open`、`Close`、`Click` 和 `Tasks` 的完整字段沿用通用 [Events 文档](../modern-dialog/events.md)；熔炉类菜单的 `Progress` 配置参见[刷新机制](refresh.md#熔炉进度事件)。

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

`open`、`close`、`force-open`、`force-close`、`reset` 和 `return` 与 Dialog 使用相同含义。`reset` 重新渲染当前容器类菜单，但不会再次执行 `Events.Open`；`force-open` 和 `force-close` 会跳过生命周期事件。

事件中的条件、变量、PAPI、JavaScript、动作包和 `wait` 参见[条件判断](../modern-dialog/conditions.md)、[动作](../modern-dialog/actions.md)、[JavaScript](../modern-dialog/javascript.md)和[数据存储](../data/storage.md)。

## 使用案例：初始化、复用动作和关闭清理

```yaml
Events:
  Open:
    - 'meta: type=set;key=opened_at;var=`{js:Date.now()}`'
    - 'actionbar: &a菜单已打开'
  Close:
    - 'meta: type=delete;key=opened_at'
    - 'actionbar: &7菜单已关闭'
  Click:
    confirm:
      - 'tell: &a已确认'
      - 'close'
```

按钮中可以这样调用 `Click` 动作组：

```yaml
Buttons:
  confirm:
    display:
      material: LIME_CONCRETE
      name: '&a确认'
    actions:
      left:
        - 'actions: confirm'
```

`Events.Open` 适合初始化；如果只是改变按钮显示，点击动作后使用 `refresh` 或 `reset`，不要重复依赖 `Events.Open` 完成普通刷新。
