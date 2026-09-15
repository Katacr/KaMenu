# KaMenu v2.0.7 更新日志 / Changelog

## 版本信息 / Release Information

- **版本号 / Version:** 2.0.7
- **发布日期 / Release Date:** 2026年9月4日 / September 4, 2026

---

## 中文

### 原生 HMCCosmetics 饰品槽位引用

- `material` 新增四种 HMCCosmetics 槽位引用：`[HMCC_HAT]`、`[HMCC_BACKPACK]`、`[HMCC_OFFHAND]`、`[HMCC_BALLOON]`。与原版装备槽位一致，也可用 `[HMCC_HAT:Steve]` 或 `[HMCC_HAT:{meta:player}]` 读取指定在线玩家。
- KaMenu 通过 HMCCosmetics 公开 API 直接克隆玩家当前槽位的最终 `ItemStack`，不再需要通过 PAPI 返回的基础材质、`item_model` 或 CustomModelData 重组图标。因此会完整保留 CraftEngine 物品组件、PDC、CustomModelData，以及可染色饰品的玩家专属颜色。
- HMCCosmetics 保持软依赖：未安装、目标用户尚未初始化或槽位为空时安全回退为空槽位占位物品，不影响其他菜单。Dialog 与 Container 共用该能力。
- Lobby 的「个人信息」菜单四个 HMCCosmetics 槽位已迁移到原生引用，修复图标退化为 `PAPER` / `LEATHER_HORSE_ARMOR` 的问题。

### 容器菜单物品小图标修正与 SX-Item 支持

- 修复 2.0.6 引入 Component 路径后容器物品名/Lore 默认显示为斜体的问题：原版 Minecraft 中通过 Component 设置的自定义物品名/Lore 默认斜体，而此前的 Legacy 字符串路径不会。现已在 Component 路径显式关闭斜体，使物品名/Lore 外观与旧版一致（Dialog 与容器标题本就走 Component 路径，不受影响）。
- 修复 `&item:[wheat]` 等既有物品态又有方块态的材质显示为方块态异常纹理的问题：Bukkit 默认将这类名称解析为方块态（如 crops 等特殊方块），现改为**优先使用物品态**（`minecraft:item/...`），仅当物品态不可用时回退方块态（`minecraft:block/...`）。
- 新增 **SX-Item** 自定义物品支持：容器菜单物品槽（`material: sxitem:<id>` 或 `sx:<id>`）与 Dialog 物品组件现在可显示 SX-Item 生成的物品，悬浮文本（显示名/Lore）随物品本体展示。同时 `&item:[sxitem:<id>]` 内联小图标也会尝试按物品自身 `item_model` 解析精灵。可点击文本中的 `hover_item=sxitem:<id>`（或 `material:sxitem:<id>`）悬浮物品组件也已支持，Paper 通过 `ItemStack.asHoverEvent()`、Spigot 通过完整组件映射传递真实外观。

### Dialog 按钮 `tooltip` 的 `\n` 换行修正

- 修复 Dialog 按钮 `tooltip` 中字面 `\n`（反斜杠 n）不换行、直接显示为字面字符的问题。
- 原因：`tooltip` 解析路径（`ConditionValueResolver.getInlineConditionalStringList`）此前未将字面 `\n` 转换为真实换行符，而同文件的 `getString` 路径已有该转换。现已统一补上该转换，使单行 `tooltip: 'line1\nline2'` 可正常换行（列表写法 `- 'line1'` / `- 'line2'` 本就支持）。

### 容器与 Dialog 物品支持 `tooltip_style`

- 新增 `tooltip_style` 物品显示属性：容器菜单按钮槽位（`display.tooltip_style`）与 Dialog 物品组件（`item.tooltip_style`）现可设置自定义 tooltip 背景与边框样式（格式 `namespace:key`），指向资源包 `assets/<namespace>/textures/gui/sprites/tooltip/<key>_background` 与 `<key>_frame`（Minecraft 1.21.4+ 的 `minecraft:tooltip_style` 数据组件）。旧核心不支持时输出警告并忽略该字段。该属性也会随物品一并被 Dialog 悬浮（`hover_item`）与 Spigot 组件映射保留。

### `/km item give` 给予时替换 lore 中的 `{player}`

- 通过 `/km item give <物品> <玩家> [数量]` 指令给予物品时，若物品 lore 中包含字面 `{player}`，会自动替换为**被给予玩家**的名称（例如 `灵魂绑定：{player}` → `灵魂绑定：Steve`）。该替换仅发生在 `/km item give` 指令路径，菜单内 `stock-item: type=give` 动作不替换。
- 替换后物品 lore 已与数据库模板不同，因此 `stock-item: type=take`（基于 `isSimilar()` 全量比较）无法再用原始模板选中该物品；如需回收，改用 `item: type=take;lore=灵魂绑定：{player}`，其中 `{player}` 在执行 take 时会被解析为触发玩家自身名称，从而精准匹配其名下物品。

### 修复 SX-Item 物品创建失败回退为 PAPER

- 修复 `ExternalItemAdapter` 中 SX-Item 集成因调用了不存在的 `SXItem.getInstance()` 方法（Lombok `@Getter` 实际生成 `SXItem.getInst()`）导致反射失败、物品回退为 PAPER 的问题。现已改用正确的 `SXItem.getItemManager()` 静态方法获取物品管理器，`getItem(id, player, args)` 与 `getItemKey(item)` 调用保持不变。

## English

### Native HMCCosmetics Cosmetic Slot References

- Added four HMCCosmetics `material` slot references: `[HMCC_HAT]`, `[HMCC_BACKPACK]`, `[HMCC_OFFHAND]`, and `[HMCC_BALLOON]`. Like vanilla equipment references, `[HMCC_HAT:Steve]` or `[HMCC_HAT:{meta:player}]` can target another online player.
- KaMenu clones the player's final slot `ItemStack` directly through the public HMCCosmetics API instead of rebuilding an icon from PAPI base-material, `item_model`, or CustomModelData strings. This preserves CraftEngine item components, PDC, CustomModelData, and the player's selected color for dyeable cosmetics.
- HMCCosmetics remains a soft dependency. A missing plugin, an uninitialized target user, or an empty slot safely falls back to the empty-slot item without affecting other menus. Dialog and Container menus share this support.
- The Lobby profile menu now uses the native references for all four HMCCosmetics slots, fixing icons that degraded to `PAPER` or `LEATHER_HORSE_ARMOR`.

### Dialog Button `tooltip` `\n` Line Break Fix

- Fixed Dialog button `tooltip` where a literal `\n` (backslash-n) was shown as literal characters instead of breaking the line.
- Cause: the `tooltip` parse path (`ConditionValueResolver.getInlineConditionalStringList`) did not convert literal `\n` to a real newline, while the sibling `getString` path already did. The conversion is now unified so a single-line `tooltip: 'line1\nline2'` breaks correctly (the list form `- 'line1'` / `- 'line2'` was already supported).

### Container and Dialog Items Support `tooltip_style`

- Added the `tooltip_style` item display property: container menu button slots (`display.tooltip_style`) and Dialog item components (`item.tooltip_style`) can now set a custom tooltip background and frame style (format `namespace:key`), pointing to the resource pack `assets/<namespace>/textures/gui/sprites/tooltip/<key>_background` and `<key>_frame` (Minecraft 1.21.4+ `minecraft:tooltip_style` data component). On older cores that do not support it, a warning is logged and the field is ignored. The style is also preserved through Dialog `hover_item` hovers and Spigot component mapping.

### `/km item give` Replaces `{player}` in Lore

- When giving an item through `/km item give <item> <player> [amount]`, if any lore line contains the literal `{player}`, it is replaced with the **receiving player's** name (e.g. `Soulbound: {player}` → `Soulbound: Steve`). This replacement only happens on the `/km item give` command path; the in-menu `stock-item: type=give` action does not replace it.
- After replacement the item lore differs from the database template, so `stock-item: type=take` (full-data `isSimilar()` comparison) can no longer target the item with the original template. To reclaim such bound items, use `item: type=take;lore=Soulbound: {player}` instead, where `{player}` is resolved to the triggering player's own name at take time and matches that player's item precisely.

### Container Menu Item Sprite Fixes and SX-Item Support

- Fixed item name/lore rendering in italic by default after 2.0.6 introduced the Component path: vanilla Minecraft renders Component-based custom item names/lore italic by default, while the previous legacy-string path did not. Italic is now explicitly disabled on the Component path so item name/lore appearance matches the previous version (Dialog and container titles already used the Component path and are unaffected).
- Fixed `&item:[wheat]` and similar materials that have both an item form and a block form rendering as the block form's abnormal texture: Bukkit resolves such names to the block form by default (e.g. special crops blocks). The item form (`minecraft:item/...`) is now preferred, falling back to the block form (`minecraft:block/...`) only when the item form is unavailable.
- Added **SX-Item** custom item support: container menu item slots (`material: sxitem:<id>` or `sx:<id>`) and Dialog item components can now display items generated by SX-Item, with hover text (display name/lore) following the item itself. Inline `&item:[sxitem:<id>]` icons also try to resolve the sprite from the item's own `item_model`.

### Dialog Button `tooltip` `\n` Line Break Fix

- Fixed Dialog button `tooltip` where a literal `\n` (backslash-n) was shown as literal characters instead of breaking the line.
- Cause: the `tooltip` parse path (`ConditionValueResolver.getInlineConditionalStringList`) did not convert literal `\n` to a real newline, while the sibling `getString` path already did. The conversion is now unified so a single-line `tooltip: 'line1\nline2'` breaks correctly (the list form `- 'line1'` / `- 'line2'` was already supported).
