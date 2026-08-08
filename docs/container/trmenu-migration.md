# TrMenu 迁移

KaMenu 可以把 TrMenu stable-v3 的经典库存菜单编译为 KaMenu V2 Container 标准菜单。迁移器只读取 YAML，不依赖 TrMenu，也不会执行源菜单中的 Kether、JavaScript、指令或点击动作。

## 执行迁移

```bash
/km migrate trmenu [源文件或目录] [输出目录] [overwrite]
```

- `trm` 是 `trmenu` 的别名。
- 省略源路径时读取 `plugins/TrMenu/menus`。
- 省略输出目录时写入 `plugins/KaMenu/menus/trmenu_migrated`。
- 输出目录必须位于 `plugins/KaMenu/menus` 内。
- 默认不覆盖已有菜单、同名自定义指令或物品绑定；追加 `overwrite` 才会覆盖。
- `Bindings.Commands` 中的普通根指令会合并到 `custom_commands.yml`。
- 可兼容的 `Bindings.Items` 会合并到 `item_bindings.yml`。
- 完成后自动重载菜单、自定义指令、物品绑定和在线玩家的客户端命令树。

```bash
/km migrate trmenu
/km migrate trm overwrite
/km migrate trmenu /path/to/TrMenu/menus trmenu_migrated overwrite
```

## 迁移流程

迁移器先扫描整个批次并建立 TrMenu 文件名到 KaMenu 目标 ID 的映射，再转换跨菜单 `open:`。不同目录中的 TrMenu 文件若使用相同文件名，会报告 `TRM_DUPLICATE_MENU_ID` 并拒绝生成，因为 TrMenu 本身使用不含扩展名的文件名作为菜单 ID。

每个目标文件都会经过 KaMenu `ContainerMenuParser` 再次校验。存在 ERROR 时不写入目标；成功文件先写入同目录临时文件，再原子替换目标文件。

## 支持范围

主要支持：

- 单页 CHEST、HOPPER、DISPENSER、DROPPER、FURNACE、BLAST_FURNACE、SMOKER 和 ANVIL 布局
- TrMenu 属性键的大小写、单复数和短别名正则
- 原版材质、玩家头颅、玩家装备槽位、ItemsAdder、Oraxen、CraftEngine 和 CustomModelData
- nested `icons` 到 `Buttons.<id>.variants`，包括 priority、inherit 和 append 动作顺序
- `any`、`all`、`not`、`check`、`perm`、`var/vars`、PAPI、money、points 和基础 item 条件
- 可静态识别的 `utils.isPlayerOnline`、`utils.hasEquipment`、`utils.getEquipment`，以及简单参数形式的 `utils.hasMoney` 和 `utils.hasItem`
- 常用消息、命令、音效、标题、菜单跳转、刷新、菜单参数更新/清理、延迟、概率、经济、点券、数据和基础物品动作
- `Events.Open`、`Events.Close` 和自动 `Tasks`
- 纯返回值 `Functions` 到菜单 `JavaScript`，以及 `${function_arg}` 调用
- 普通 `Bindings.Commands` 到 `custom_commands.yml`
- `Bindings.Items` 中的 material、lore、name、data 和 model-data 到 `item_bindings.yml`

TrMenu 无参数 `refresh` / `update` 会转换为 KaMenu 裸 `refresh`，刷新全部按钮图标；指定图标 ID 时转换为对应的局部刷新。TrMenu `reset` 会近似转换为裸 `refresh`，但源菜单的动画索引无法保留，因此仍会产生近似迁移提示。

TrMenu 的绑定物品监听会忽略 `amount`，迁移结果也忽略该 trait。生成的绑定默认使用 `cooldown-ms: 2000`，对应 TrMenu 的默认 `Bound-Item-Interval`；迁移器不会读取全局 `settings.yml`，原服务器修改过间隔时需要手工调整。完整字段见 [物品右键绑定](../config/item-bindings.md)。

## 不能自动迁移

以下内容会拒绝文件、跳过分支或产生 WARNING：

- TrMenu `Render-Type: DIALOG` 和多页 Layout
- PlayerInventory、Free-Slots、Hide-Player-Inventory 等发包物品栏功能
- catcher、动态聊天输入和菜单页动作
- 任意 Kether 流程、JEXL、NovaScript、内联 TrMenu JS，以及未列入固定映射的私有 `session/config/utils` 绑定
- repo/私有物品源、NBT 和不能由 Bukkit API 显式映射的属性
- 正则指令绑定、客户端本地多语言节点，以及无法映射的绑定物品 trait
- TrMenu 拖拽或窗口外点击类型

绑定物品中的反向 trait、头颅 owner/texture、未知 trait、只有 `amount` 的匹配和冲突的多组 matcher 会被跳过并报告，不会降级为更宽松的物品匹配。

不支持条件不会被直接删除后执行 allow 分支。Open 条件无法转换时会保留可转换的 deny 动作并追加 `return`，从而阻止玩家打开功能残缺或权限失效的菜单。

stable-v3 的官方示例把 `Free-Slots` 写在 `Options` 内，但对应运行时源码从菜单根节点读取。迁移器会同时检测根级 `Free-Slots` 和 `Options.Free-Slots`，两者都只报告并跳过，不会把可取物槽位带入 KaMenu 的只读 Container 模型。

## 诊断

每条诊断包含严重程度、兼容性、稳定 code 和源 YAML 路径：

```text
[WARNING/APPROXIMATE/TRM_ICON_ANIMATION_FIRST_FRAME]
Icons.demo.display.name: Animated name was reduced to the first frame.
```

- `WARNING`：文件仍可能生成，但必须人工复核对应行为。
- `ERROR`：该文件不会生成。
- `APPROXIMATE`：目标可运行，但时机、动画或生命周期不完全相同。
- `UNSUPPORTED`：对应字段、动作或分支已过滤。

迁移完成后应在测试服逐项验证第三方物品、Vault、PlayerPoints、代理服跳转、权限和所有按钮动作。
