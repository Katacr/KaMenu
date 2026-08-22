# 菜单迁移总览

KaMenu 可以将 DeluxeMenus 和 TrMenu 的库存菜单转换为 KaMenu V2 标准容器类菜单。迁移器只读取 YAML，不会加载来源插件，也不会执行来源菜单中的指令、JavaScript 或点击动作。

## 支持的来源

| 来源 | 指令 | 默认源目录 | 默认输出目录 |
|---|---|---|---|
| DeluxeMenus | `/km migrate dm` | `plugins/DeluxeMenus/gui_menus` | `plugins/KaMenu/menus/dm_migrated` |
| TrMenu stable-v3 | `/km migrate trmenu` | `plugins/TrMenu/menus` | `plugins/KaMenu/menus/trmenu_migrated` |

两条指令都支持以下完整格式：

```text
/km migrate <dm|trmenu> [源文件或目录] [输出目录] [overwrite]
```

- 源可以是单个 YAML 文件或目录。
- 输出目录必须位于 `plugins/KaMenu/menus` 内。
- 默认保留已有目标文件和冲突配置；只有明确追加 `overwrite` 才会覆盖。
- 迁移完成后会自动重载菜单和相关入口配置。
- 聊天框只显示迁移计数、入口合并、运行时重载和日志路径等摘要。
- 每次迁移的逐文件结果、全部警告/错误和配置冲突会写入 `plugins/KaMenu/logs/migration/` 下的独立 `.log` 文件。
- 生成成功不代表行为完全一致，所有 `WARNING` 都需要人工复核。

## YAML 锚点与重复内容

TrMenu 和 KaMenu 都使用标准 YAML 解析，因此可以使用 `&名称` 定义锚点，使用 `*名称` 复用已经定义的 Map 或列表：

```yaml
Events:
  Click:
    buy: &buy_actions
      - 'tell: &a购买成功'
      - 'close'

Buttons:
  product_a:
    display: &product_display
      material: DIAMOND
      name: '&b商品'
    actions:
      left: *buy_actions
  product_b:
    display: *product_display
    actions:
      left: *buy_actions
```

迁移器处理的是 YAML 解析后的锚点内容，而不是 `*名称` 这段引用文本。锚点中的动作、条件、变量和物品字段仍会按 KaMenu 规则转换；源文件的锚点名称不保证原样保留，生成文件可能会展开内容或由 YAML 序列化器重新命名锚点。迁移后请检查报告和目标菜单，确认复用内容的上下文变量仍然正确。

## DeluxeMenus 迁移教程

### 1. 使用默认目录迁移

确认 DM 菜单位于：

```text
plugins/DeluxeMenus/gui_menus/
```

执行：

```text
/km migrate dm
```

生成文件会保留源目录结构并写入：

```text
plugins/KaMenu/menus/dm_migrated/
```

例如 `gui_menus/shop/main.yml` 对应菜单 ID `dm_migrated/shop/main`：

```text
/km open dm_migrated/shop/main
```

DM 的 `open_command` 会合并到 `custom_commands.yml`。已有同名指令默认保留并报告冲突。

### 2. 指定来源或覆盖旧结果

```text
/km migrate dm /path/to/DeluxeMenus/gui_menus
/km migrate dm /path/to/DeluxeMenus/gui_menus dm_migrated
/km migrate dm /path/to/DeluxeMenus/gui_menus dm_migrated overwrite
```

只在确认需要替换旧菜单和同名自定义指令时使用 `overwrite`。

### 3. DM 可迁移内容

- 9 至 54 槽的箱子菜单、标题、`slot`、`slots` 和槽位范围。
- 物品材质、名称、Lore、数量、附魔、标志、模型 ID、头颅和不可破坏属性。
- 同槽位 `priority` 候选，可转换为 `Buttons.<id>.variants`。
- 权限、余额、物品和基础字符串/数值比较要求。
- 通用、左键、右键、Shift 左/右键和中键动作。
- 消息、玩家/控制台指令、打开菜单、跨服、关闭、刷新、音效和 Vault 经济动作。
- `open_requirement`、`open_commands`、`open_command` 和物品 `update`。

## TrMenu 迁移

使用默认目录迁移：

```text
/km migrate trmenu
```

`trm` 是 `trmenu` 的别名。普通 `Bindings.Commands` 会合并到 `custom_commands.yml`；可安全映射的 `Bindings.Items` 会合并到 `item_bindings.yml`。

主要支持单页标准容器布局、普通或显式槽位图标、嵌套图标状态、常见物品字段、常用 Kether 条件和动作、生命周期事件、自动任务、跨菜单打开，以及可静态分析的纯返回值 `Functions`。

静态 `{node:path}`、`{nodes:path}` 和 `{n:path}` 会转换为 KaMenu `{ref:trmenu.path}`。迁移器只把实际使用的源节点复制到目标菜单的 `References.trmenu`，节点模板中的 `{0}`、`{1}` 会转换为 `{refarg:0}`、`{refarg:1}`。图标内部使用的 `@iconId@` 会在迁移时替换为当前源图标 ID：

```yaml
# TrMenu 来源
lore:
  - '{node:Icons.@iconId@.display.material}'

# KaMenu 迁移结果
lore:
  - '{ref:trmenu.Icons.shop.display.material}'
```

包含运行时变量的动态节点路径无法静态确定，会保留诊断并要求手工改写。

指定目录或覆盖旧结果：

```text
/km migrate trmenu /path/to/TrMenu/menus trmenu_migrated
/km migrate trmenu /path/to/TrMenu/menus trmenu_migrated overwrite
```

## 不可迁移和不兼容内容

### DeluxeMenus

| 内容 | 迁移结果 |
|---|---|
| 不支持的 requirement | 条件按不满足处理并输出警告，不会绕过原限制 |
| 未识别的动作 | 跳过该动作并输出警告 |
| 未支持的点击类型 | 不生成对应点击动作，需要手工补写 |
| 旧版 material `data` | 忽略并输出警告 |
| HeadDatabase `hdb-*` | 临时使用 `PAPER`，需要手工替换 |
| `[json]` | 降级为普通 `tell` 文本，需要检查显示效果 |
| `[broadcastsound]` | 近似为只向当前菜单玩家播放音效 |
| `[commandevent]` | 近似为普通玩家指令 |
| `has item` 的物品名称判断 | 忽略名称，只保留可映射的材质、数量和 Lore |

### TrMenu

以下内容会拒绝文件、跳过对应分支或输出警告：

- `Render-Type: DIALOG`（仍不支持）。
- 多页 `Layout` 现已支持，正常无损迁移为 KaMenu 页数组与 `Settings.default_page`；仅当多页解析本身失败时拒绝文件。
- `PlayerInventory`、`Free-Slots`、`Hide-Player-Inventory` 等发包物品栏功能。
- catcher、动态聊天输入、菜单页动作、拖拽和窗口外点击。
- 任意 Kether 流程、JEXL、NovaScript、内联 TrMenu JavaScript，以及未列入固定映射的私有对象或方法。
- repo/私有物品源、NBT 和无法通过 Bukkit API 显式映射的属性。
- 正则指令绑定、客户端本地化 `Lang` 和不安全的绑定物品 trait。

TrMenu 的图标槽位已支持运行时动态解析：静态多值 `[8, 9, 10]` 写为 KaMenu `slot: [8, 9, 10]`（多副本）；含变量写为 `slot: '<表达式>'`（按玩家变量解析）；动画帧 `[[8], [9], [10]]` 写为 `slot: [[8], [9], [10]]`（每次刷新循环到下一帧）。上述情况输出 `INFO` 级提示，不再报错。

不支持的条件不会被删除后继续执行受保护动作。无法转换打开条件时，迁移器会阻止残缺菜单打开。

## 迁移报告和错误代码解析

每次执行迁移都会生成类似以下文件：

```text
plugins/KaMenu/logs/migration/migration-20260810-153012-123-trmenu.log
```

日志使用 UTF-8 编码，包含迁移类型、来源、输出目录、是否覆盖、逐文件成功/失败状态、完整诊断、指令冲突、物品绑定冲突和运行时重载结果。聊天框不会再逐条输出这些明细，只会给出日志绝对路径。若日志文件无法写入，KaMenu 会在聊天中提示错误，并将完整报告回退到服务器控制台。

### DeluxeMenus 报告

DM 报告没有稳定代码，格式为级别、来源 YAML 路径和消息：

```text
迁移警告 [items.shop.click_commands]: Unsupported DeluxeMenus action [takeexp] was skipped.
```

- `WARNING`：目标文件仍可能生成，但对应字段被跳过或近似处理。
- `ERROR`：目标文件不会生成。先根据方括号中的路径修正源 YAML，再重新迁移。

### TrMenu 报告

TrMenu 报告格式为：

```text
[WARNING/UNSUPPORTED/TRM_ACTION_UNSUPPORTED] Icons.shop.actions: Unknown action was skipped.
```

四部分依次表示：

1. `INFO`、`WARNING` 或 `ERROR`：`ERROR` 会阻止该文件生成。
2. `EXACT`、`APPROXIMATE`、`UNSUPPORTED` 或 `INVALID`：说明转换后的兼容程度。
3. `TRM_*`：稳定诊断代码，可用于搜索同类问题。
4. YAML 路径和消息：指出需要修改的源配置位置与原因。

常见代码：

| 代码 | 含义 | 处理方式 |
|---|---|---|
| `TRM_SOURCE_INVALID` / `TRM_YAML_INVALID` | 来源不存在、不是 YAML 或 YAML 解析失败 | 检查路径、缩进和 YAML 语法 |
| `TRM_TARGET_EXISTS` | 目标文件已存在 | 确认后使用 `overwrite`，或更换输出目录 |
| `TRM_DUPLICATE_MENU_ID` | 多个 TrMenu 文件使用相同文件名 | 重命名冲突文件后整批重试 |
| `TRM_LAYOUT_INVALID` | 布局行数、槽位或图标引用无效 | 修正源 `Layout` 和 `Icons` |
| `TRM_RENDER_TYPE_UNSUPPORTED` | Dialog 菜单无法转换为容器类菜单 | 使用现代 Dialog 或其他方案 |
| `TRM_MULTI_PAGE_UNSUPPORTED` | 多页 Layout 解析本身失败（正常多页已支持） | 修正源 `Layout` 页数与行列数 |
| `TRM_CONDITION_UNSUPPORTED` / `TRM_ACTION_UNSUPPORTED` | 条件或动作不在固定映射范围内 | 使用 KaMenu 条件或动作手工改写 |
| `TRM_ITEM_SOURCE_UNSUPPORTED` / `TRM_ITEM_META_UNSUPPORTED` | 私有物品源或物品属性不能安全映射 | 改用 Bukkit 材质或 KaMenu 支持的物品格式 |
| `TRM_NODE_DYNAMIC_UNSUPPORTED` | node 路径包含运行时变量或嵌套动态内容 | 改为静态路径，或手工改写为 KaMenu 变量/引用 |
| `TRM_NODE_ICON_CONTEXT_MISSING` | `@iconId@` 出现在图标以外，无法确定图标 ID | 使用明确的静态节点路径 |
| `TRM_NODE_PATH_MISSING` | 来源菜单中找不到 node 指向的路径 | 检查源路径、键名和文件内容 |
| `TRM_NODE_STRUCTURE_UNSUPPORTED` | node 指向 Map、嵌套列表等非文本结构 | 只引用具体标量或简单列表字段 |
| `TRM_NODE_REFERENCE_UNSUPPORTED` | 当前字段不能安全保留未转换的 node 引用 | 手工改写该字段后重新迁移 |
| `TRM_TARGET_VALIDATION_FAILED` | 生成结果未通过 KaMenu 二次解析 | 保留完整报告并检查前序警告；仍可复现时提交问题 |

其他 `TRM_*` 代码也应结合同行的 YAML 路径和消息处理。`APPROXIMATE` 表示可运行但行为不完全相同，`UNSUPPORTED` 表示对应内容已过滤，不能只看“迁移成功”就直接投入生产。

## 迁移后检查

至少在测试服检查：

- 每个菜单能否打开，布局和标题是否正确。
- 权限、余额、物品条件及拒绝分支是否生效。
- 所有点击类型、菜单跳转、关闭和刷新动作。
- ItemsAdder、Oraxen、CraftEngine、Vault、PlayerPoints 和代理服相关功能。
- `custom_commands.yml` 与 `item_bindings.yml` 是否出现冲突或遗漏。
