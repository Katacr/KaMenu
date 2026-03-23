# 🎬 菜单视图

`Title` 节点定义菜单顶部显示的标题文字。

---

## 基本用法

**类型：** `String`

**格式：** 支持颜色代码（`&` 符号）和 PlaceholderAPI 变量

**示例：**

```yaml
Title: '&6&l服务器主菜单'
```

```yaml
Title: '&a欢迎，&f%player_name%！'
```

```yaml
Title: '&8» &d&l商店系统 &8«'
```

---

## 条件判断标题

`Title` 支持条件判断格式，可以为不同状态的玩家显示不同标题：

```yaml
Title:
  - condition: "%player_is_op% == true"
    allow: '&8» &4&l高级管理面板 &8« &7[管理员]'
    deny: '&8» &6&l玩家面板 &8«'
```

```yaml
Title:
  - condition: "%player_level% >= 20"
    allow: '&8» &6&lVIP 专区 &8«'
    deny: '&8» &7&l普通区域 &8«'
```

关于条件判断的完整语法，请参阅 [🔍 条件判断](conditions.md)。

---

## 注意事项

* 标题长度有限制，过长的标题可能被截断（建议控制在 32 个字符以内）
* 支持 PlaceholderAPI 变量（需要安装 PlaceholderAPI 插件）
* 支持内置数据变量：`{data:key}` 和 `{gdata:key}`
