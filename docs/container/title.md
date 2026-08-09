# Title

`Title` 定义容器类菜单顶部显示的库存标题。

## 字段一览

| 项目 | 支持内容 | 说明 |
|---|---|---|
| 配置键 | `Title` | 容器类菜单顶部标题 |
| 类型 | 字符串、字符串列表或条件结果 | 字符串列表按顺序作为标题帧 |
| 默认值 | `KaMenu` | 未配置时使用 |
| 变量 | PAPI、`{meta:*}`、`{data:*}`、`{js:...}` 等 | 打开和刷新时按当前玩家重新解析 |
| 帧刷新 | `Title-Update` | 每个周期推进一项，到达末尾后回到第一项 |
| 当前项刷新 | `Update`、`refresh: title` 或 `refresh: *` | 重新解析当前项，但不推进列表索引 |

**类型：** `String`、`List<String>` 或条件结果

**默认值：** `KaMenu`

支持颜色代码、PlaceholderAPI、KaMenu 内置变量、`meta`、`data` 和 JavaScript 输出。标题在打开时解析，字符串列表默认显示第一项；配置 `Title-Update` 后，每次周期刷新都会推进一项并循环。

```yaml
Title: '&8玩家商店 - %player_name%'
```

列表模式可用于轮播或简单标题动画：

```yaml
Title:
  - '&8正在加载.'
  - '&8正在加载..'
  - '&8正在加载...'
Title-Update: 10
```

上例打开时显示第一项，之后每 10 tick 显示下一项，第三项之后回到第一项。每一项都可以使用 PAPI 和 KaMenu 变量。

`Update`、`refresh: title` 和 `refresh: *` 会重新解析当前项，但不会切换到下一项；只有 `Title-Update` 的周期刷新会推进列表索引。

条件标题仍使用条件结果格式：

```yaml
Title:
  - condition: 'hasPerm.shop.admin'
    allow: '&4管理员商店'
    deny: '&6普通商店'
```

列表全部由条件 Map 组成时会被解释为条件候选列表，并选择第一个有效结果，不会作为标题帧轮播。字符串帧与条件 Map 混写时则按 YAML 顺序展开，条件分支返回的一个或多个字符串会成为其所在位置的标题帧。

容器类菜单标题不使用 Dialog 的 `width`、`external_title` 或其他客户端 Dialog 字段。标题刷新规则参见[刷新机制](refresh.md)；通用条件语法参见[条件判断](../modern-dialog/conditions.md)。

## 使用案例

```yaml
# 根据权限显示不同标题，并每 40 tick 重新判断一次
Title:
  - condition: 'hasPerm.shop.admin'
    allow: '&4管理员商店 - %player_name%'
    deny: '&6玩家商店 - %player_name%'
Title-Update: 40
```

标题只影响顶部文字。如果需要根据余额、库存或权限改变按钮物品，应在 `Buttons` 中使用 `variants`，而不是把所有显示逻辑塞进标题。
