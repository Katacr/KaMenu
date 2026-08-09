# 刷新机制

容器类菜单刷新周期的单位是 tick。周期小于 5 会产生警告；优先使用局部刷新，避免每 tick 重建整个库存。

## 字段一览

| 键 | 刷新范围 | 单位 | 适用场景 |
|---|---|---|---|
| `Update` | 标题、全部按钮和容器属性 | tick | 页面整体状态同步 |
| `Title-Update` | 标题并推进一个标题列表项 | tick | 标题轮播、余额、时间或状态 |
| `Progress-Update` | 熔炉属性并检查 `Events.Progress` | tick | 火焰/箭头进度 |
| `Buttons.<id>.update` | 指定按钮槽位 | tick | 只刷新一个动态按钮 |
| `refresh` | 当前会话全部按钮 | 动作 | 点击后立即更新按钮 |
| `refresh: *` | 标题、属性和全部按钮 | 动作 | 参数或全局状态变化后完整更新 |

```yaml
Update: 20
Title-Update: 40
Progress-Update: 5

Buttons:
  status:
    update: 20
    display:
      material: CLOCK
```

动作中的目标刷新：

```yaml
- 'refresh'
- 'refresh: *'
- 'refresh: title'
- 'refresh: properties'
- 'refresh: status'
```

- `refresh` 或空目标 `refresh:`：刷新全部按钮图标，不刷新标题和容器属性。
- `refresh: *`：刷新标题、容器属性和全部按钮。
- `refresh: title`、`refresh: properties`：只刷新对应部分。
- `refresh: <按钮ID>`：只刷新指定按钮。

当 `Title` 是字符串列表时，只有 `Title-Update` 的周期刷新会推进到下一项并循环。`Update`、`refresh: title` 和 `refresh: *` 只重新解析当前项。

刷新时会重新解析 PAPI、内置变量、条件、`view_condition` 和 `variants`。数据库型 `data`、`gdata`、`list`、`glist` 可能异步返回；高频刷新优先使用 `meta`，持久化写入后按需等待数个 tick。

## 熔炉进度事件

```yaml
Events:
  Progress:
    cook_complete:
      source: cook_progress
      condition: '{progress.current} >= 100'
      trigger_initial: false
      actions:
        - 'actionbar: &a加工完成'
```

进度事件在条件从不满足变为满足时触发；`trigger_initial: true` 允许首次检查时触发。详细事件语法参见[Events](events.md)。

## 如何选择刷新方式

- 页面上只有一个余额或库存按钮变化时，使用 `Buttons.<id>.update` 或 `refresh: <id>`。
- 标题也依赖动态数据时，使用 `Title-Update` 或 `refresh: title`。
- 修改了 `Properties` 或熔炉进度来源时，使用 `Progress-Update` 或 `refresh: properties`。
- 修改了多个按钮、参数或变体状态时，使用 `refresh`；只有需要同时更新标题和属性时才使用 `refresh: *`。
- 不要用 `Events.Tasks` 每几 tick 执行 `reset` 重建整个容器类菜单；优先使用容器类菜单专用刷新字段。
