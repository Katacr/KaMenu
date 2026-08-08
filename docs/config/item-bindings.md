# 物品右键绑定: item_bindings.yml

`item_bindings.yml` 用于把玩家主手物品的右键操作绑定到 KaMenu 菜单。该文件位于 `plugins/KaMenu/item_bindings.yml`，支持手工维护，也用于保存 `/km migrate trmenu` 转换出的 `Bindings.Items`。

文件不存在时 KaMenu 会按空配置处理；只有迁移器确实生成了物品绑定时才会自动创建。`/km reload config` 和 `/km reload all` 会重新加载该文件。

## 基础格式

```yaml
item-bindings:
  server_selector:
    enabled: true
    menu: 'example/main_menu'
    require-sneaking: false
    cooldown-ms: 2000
    ignore-case: true
    translate-colors: true
    material: 'COMPASS'
    target-name: '&e服务器选择器'
    target-lore: '&7右键打开菜单'
    data: 0
    custom-model-data: 1001
```

每个绑定必须启用、指定 `menu`，并至少提供一个物品匹配字段。玩家使用主手右键空气或方块时，KaMenu 按配置顺序选择第一个匹配项，取消原右键事件并打开目标菜单。

## 字段

| 字段 | 说明 | 默认值 |
|---|---|---|
| `enabled` | 是否启用该绑定 | `false` |
| `menu` | 打开的 KaMenu 菜单 ID | 无 |
| `require-sneaking` | 是否要求玩家正在潜行 | `false` |
| `cooldown-ms` | 同一玩家再次触发物品绑定前的绝对时间间隔，单位毫秒 | `0` |
| `ignore-case` | 名称与 Lore 包含匹配时是否忽略大小写 | `false` |
| `translate-colors` | 是否先把配置文本中的 `&` 颜色代码转换为 Bukkit 颜色代码 | `false` |
| `material` | 原版材质名，或带提供方前缀的 ItemsAdder、Oraxen、CraftEngine 物品 ID | 可选 |
| `target-name` | 物品显示名称必须包含的文本 | 可选 |
| `target-lore` | 至少一行 Lore 必须包含的文本；`''`、`[]` 或省略表示不检查 | 可选 |
| `data` | 旧版 Bukkit 物品损伤值/数据值 | 可选 |
| `custom-model-data` | 整数 CustomModelData | 可选 |

外部物品 ID 示例为 `itemsadder:namespace:item`、`ia:namespace:item`、`oraxen:item_id`、`craftengine:namespace:item` 或 `ce:namespace:item`。对应插件未安装或物品无法识别时不会匹配。

`cooldown-ms` 使用每名玩家共享的最后触发时间：再次命中某个绑定时，会用该绑定自己的 `cooldown-ms` 判断间隔。迁移生成的绑定统一为 2000ms，因此行为与 TrMenu 的全局间隔一致。冷却使用系统绝对时间，不受服务器 tick 波动影响。

## 颜色与文本匹配

名称和 Lore 使用“包含”匹配，不要求整行完全相同。物品元数据中的颜色不会被自动删除：

- `translate-colors: true`：配置中的 `&a菜单` 会先转换为实际颜色代码，适合匹配带颜色的物品文本。
- `translate-colors: false`：配置文本按原值匹配；这是旧 `listeners.item-lore` 的兼容行为。

## 与 config.yml 旧监听器的关系

KaMenu 仍会读取 `config.yml > listeners.item-lore`。旧监听器要求填写 `material`，并支持 `target-lore` 留空后只判断材质。加载顺序为旧监听器在前、`item_bindings.yml` 在后，因此同一物品同时命中时优先使用旧监听器。

新增绑定建议写入独立文件，避免主配置过长，也避免迁移器保存配置时改写 `config.yml` 注释。

## TrMenu 迁移

`/km migrate trmenu` 会把可安全映射的 `Bindings.Items` 合并到该文件，并绑定到迁移后的 Container 菜单。支持 `material`、`lore`、`name`、`data` 和 `model-data`；TrMenu 在右键绑定中忽略 `amount`，KaMenu 迁移时也会忽略它。

TrMenu 默认的 `Bound-Item-Interval` 为 `2000` 毫秒，因此迁移结果默认写入 `cooldown-ms: 2000`。迁移器不读取 TrMenu 的全局 `settings.yml`；如果原服务器修改过该值，需要手工同步调整。

反向 trait、头颅 owner/texture、未知 trait、只有 `amount` 的匹配以及互相冲突的多组 matcher 不会自动迁移。命令会输出对应 WARNING，且不会生成可能误匹配任意物品的宽松绑定。
