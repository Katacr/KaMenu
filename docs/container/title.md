# Title

`Title` 定义 Container 顶部显示的库存标题。

**类型：** `String` 或条件结果

**默认值：** `KaMenu`

支持颜色代码、PlaceholderAPI、KaMenu 内置变量、`meta`、`data` 和 JavaScript 输出。标题在打开时解析，配置 `Title-Update` 后会按周期重新解析。

```yaml
Title: '&8玩家商店 - %player_name%'
```

```yaml
Title:
  - condition: 'hasPerm.shop.admin'
    allow: '&4管理员商店'
    deny: '&6普通商店'
```

Container 标题不使用 Dialog 的 `width`、`external_title` 或其他客户端 Dialog 字段。标题刷新规则参见[刷新机制](refresh.md)；通用条件语法参见[条件判断](../modern-dialog/conditions.md)。
