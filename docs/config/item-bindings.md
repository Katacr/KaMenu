# 物品绑定：item_bindings.yml

`plugins/KaMenu/item_bindings.yml` 统一管理手持物品的左键、右键、F 和 Q 绑定。文件不存在时按空配置处理；`/km reload config`、`/km reload all` 可重载绑定。

## 配置格式

```yaml
item-bindings:
  recipe_book:
    enabled: true
    item:
      material: 'ce:farmersdelight:iron_knife'
    trigger:
      type: right-click       # left-click / right-click / f / q
      sneaking: true          # 精确匹配；默认 false
      target: air             # 仅左/右键可用：air / block / both；默认 both
      cooldown-ms: 600        # 默认 500ms；显式设为 0 可关闭冷却
    actions:
      - 'command: fd recipe cutting_board'

  guide_book:
    enabled: true
    item:
      material: 'ce:hfrpg:hefeng_guide_book'
    trigger:
      type: right-click
      sneaking: false
      target: both
    actions:
      - 'open: 任务章节/引导书'

  protected_tool:
    enabled: true
    item:
      material: 'ce:example:tool'
    trigger:
      type: q
      sneaking: true
    actions:
      - 'open: example/tool_menu'
```

每条绑定需 `enabled: true`、`item` 中至少一个物品匹配字段、`trigger.type` 和非空 `actions`。按配置顺序执行首个匹配项；`actions` 支持 KaMenu 动作和全局动作包 `actions: <包ID>`。打开菜单直接用 `open: <菜单ID>`，不再使用顶层 `menu`。

## 字段

| 位置 | 字段 | 说明 | 默认值 |
|---|---|---|---|
| 绑定 | `enabled` | 是否启用 | `false` |
| 绑定 | `actions` | 匹配后执行的动作列表 | 必填 |
| `item` | `material` | 原版物品或外部物品 ID | 可选 |
| `item` | `target-name` | 显示名称包含的文本 | 可选 |
| `item` | `target-lore` | 至少一行 Lore 包含的文本；空值/`[]` 表示不检查 | 可选 |
| `item` | `data` | 旧版物品数据值/损伤值 | 可选 |
| `item` | `custom-model-data` | 整数 CustomModelData | 可选 |
| `item` | `ignore-case` | 名称和 Lore 匹配忽略大小写 | `false` |
| `item` | `translate-colors` | 将配置的 `&` 颜色代码转换为 Bukkit 颜色代码 | `false` |
| `trigger` | `type` | `left-click`、`right-click`、`f` 或 `q` | 必填 |
| `trigger` | `sneaking` | 精确匹配潜行状态；`false` 仅匹配不潜行 | `false` |
| `trigger` | `target` | 左/右键点击 `air`、`block` 或 `both`；F/Q 不可配置 | `both` |
| `trigger` | `cooldown-ms` | 同一玩家两次物品绑定触发的间隔，毫秒 | `500` |

外部物品 ID 例：`itemsadder:namespace:item`、`ia:namespace:item`、`oraxen:item_id`、`craftengine:namespace:item`、`ce:namespace:item`。不支持通配符；物品提供方不可用时不会匹配。`target-name` 和 `target-lore` 使用包含匹配，不自动去除物品元数据的颜色。每位玩家共享最后触发时间；再次命中时按当前绑定的 `cooldown-ms` 判断。

F 优先检查物品绑定，未匹配才执行 `config.yml > listeners.swap-hand` 通用监听；命中但尚在冷却也不会回退通用菜单。Q 命中时取消丢弃，物品保留；未命中则正常丢弃。打开服务端容器菜单时不处理 Q 绑定；**个人背包界面仅由客户端管理**，Bukkit 无法可靠区分在背包界面丢弃与在世界按 Q，可能仍触发绑定。

## 自动迁移

升级时插件在主配置更新**之前**自动迁移旧 `item_bindings.yml` 的平铺格式及 `config.yml > listeners.item-lore`：合并到新文件，并从 `config.yml` 移除旧节。迁移前分别备份受影响的文件；重复启动不会产生重复绑定。原先 `menu: 名称` 转为 `actions: ['open: 名称']`；旧版未要求潜行的右键同时接受两种状态，因此迁移成普通和潜行两条绑定，保持原行为及旧主配置优先级。旧版显式冷却保留，未设置则使用新默认 500ms。迁移遇到无法安全转换的配置会停止插件启动并保留原文件，需修正后重试。

`/km migrate trmenu` 生成的新物品绑定也采用此格式；旧 TrMenu `Bindings.Items` 不区分潜行，因此会生成普通/潜行两条绑定，并保留迁移器的 2000ms 默认冷却。
