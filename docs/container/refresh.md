# 刷新机制

Container 刷新周期的单位是 tick。周期小于 5 会产生警告；优先使用局部刷新，避免每 tick 重建整个库存。

| 键 | 作用 |
|---|---|
| `Update` | 刷新标题、按钮显示/可见性和容器属性 |
| `Title-Update` | 只刷新标题 |
| `Progress-Update` | 熔炉类刷新进度属性并检查 `Events.Progress` |
| `Buttons.<id>.update` | 只刷新指定按钮占用的槽位 |

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
