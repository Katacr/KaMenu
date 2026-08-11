# KaMenu v2.0.3 更新日志 / Changelog

## 版本信息 / Release Information

- **版本号 / Version:** 2.0.3
- **发布日期 / Release Date:** 2026年8月12日 / August 12, 2026

---

## 中文

### Container 自由槽位

- 新增顶层 `Free-Slots`，可在箱子、漏斗、发射器和投掷器菜单中定义一个或多个具名真实物品槽位。
- 支持左/右键放入和取出、同类物品合并、异种交换、Shift 转移及受控拖拽；数字键、副手、双击、丢弃和创造克隆等高风险操作默认拒绝。
- 新增 `place` / `take` 开关和条件，以及 `place`、`take`、`deny_place`、`deny_take` 事务事件；拒绝操作时物品保持在原位置。
- 新增 `{free:<id>.*}`、`{free:incoming.*}`、`{free:stored.*}` 和 `{free:result.*}` 物品属性变量，支持数量、材质、名称、Lore、附魔、模型、外部物品来源等状态。
- 新增 `[FREE:<id>]` 完整 ItemStack 预览，可保留药水、书本、PDC 及 Bukkit 可访问的其他物品元数据。

### 原子消费与返还

- 新增 `free-slot` 动作，支持单槽消费、多材料原子消费、主动返还一个或全部自由槽，以及刷新引用自由槽的按钮。
- 多材料不足、ID 无效、会话失效或背包容量不足时不执行部分修改，并中断当前动作链，避免材料不足仍发放奖励。
- 自由槽变化后自动刷新全部 Container 按钮，`variants` 可立即根据材料状态切换外观和动作。

### 持久化托管与安全恢复

- 新增自由槽持久化托管账本和 `PREPARED`、`HELD`、`RETURN_PENDING` 等状态，SQLite 与 MySQL 写入按会话串行执行。
- 正常关闭、菜单跳转、重载和玩家退出时返还真实物品；背包不足时保留待返还记录并在玩家下次登录时重试。
- 释放示例菜单触发菜单重载前会先安全关闭活动 Container 会话，避免旧代际窗口遗留自由槽物品。
- 插件停用时保留托管记录并等待已开始的 SQL 操作，避免 Folia 玩家调度与数据库连接池关闭时序导致物品丢失。
- 修复新版 Paper 不再提供 SnakeYAML 私有 `Base64Coder` 时，自由槽首次放入物品抛出 `NoClassDefFoundError` 的问题；旧版换行 Base64 数据仍可读取。
- 优化自由槽放入、取出、交换、Shift 和拖拽的客户端同步，在玩家事件线程直接提交已校验的最终状态，避免取消原版事务后出现回滚再渲染的双重动画。
- 自由槽 SQL 托管失败时会关闭当前窗口并进入统一返还流程，避免已提交到 UI 的真实物品滞留在无可靠账本的会话中。
- Paper、Folia 与 Spigot 统一使用服务端当前槽位和玩家光标重放已取消的左右键事务，不再依赖不同核心的 `InventoryAction` 或事件光标快照，避免普通放入和取出时丢失物品。

### 兼容性与文本图标

- 修复 Spigot Dialog 可点击文本在 `text` 或 `hover` 中包含 ItemsAdder、Oraxen、CraftEngine 字形标签时，被内部 `>` 提前截断并退化为纯文本的问题。
- 扩展 `&item:[...]` 二维图标语法，可自动解析原版材质以及 ItemsAdder、Oraxen、CraftEngine 自定义物品纹理，并支持通过根目录 `item_sprites.yml` 覆盖复杂模型的图标。
- Sprite 图标仅在 Minecraft `1.21.9+` 启用；运行于 `1.21.8` 及以下服务器时会安全隐藏标记，且不调用第三方物品 API 或读取其资源包。

### TrMenu 迁移修复

- 修复 `action` / `actions` 被宽泛匹配为 `actionbar` 的问题；标准 YAML 锚点包装的 `actions` 列表会先展开，再逐项转换内部动作。
- 迁移器现在处理 YAML 解析后的锚点内容，不再把 `*锚点名` 当作动作文本；源锚点名称可能由输出序列化器展开或重新命名。

### 文档与示例

- 补充中英文自由槽位文档、动作索引、完整自定义合成案例、崩溃一致性边界、YAML 锚点迁移说明和二维物品图标说明，并同步 KaMenu 菜单编写 Skill。

---

## English

### Container Free Slots

- Added top-level `Free-Slots` for one or more named real-item slots in chest, hopper, dispenser, and dropper menus.
- Added controlled left/right placement and pickup, similar-item merging, different-item swaps, Shift transfers, and drag handling. Number keys, offhand swaps, double-click collection, dropping, and creative cloning are rejected by default.
- Added `place` / `take` switches and conditions plus `place`, `take`, `deny_place`, and `deny_take` transaction events. Rejected transactions leave every item at its source.
- Added `{free:<id>.*}`, `{free:incoming.*}`, `{free:stored.*}`, and `{free:result.*}` item properties for amount, material, names, lore, enchantments, models, external providers, and other state.
- Added `[FREE:<id>]` full ItemStack previews that preserve potions, books, PDC, and other Bukkit-accessible metadata.

### Atomic Consumption And Return

- Added the `free-slot` action for single-slot consumption, atomic multi-material consumption, active return of one/all free slots, and free-slot-dependent button refreshes.
- Missing materials, invalid IDs, stale sessions, and insufficient inventory capacity cause no partial mutation and stop the current action chain, preventing rewards after a failed consume.
- Free-slot changes refresh every Container button so `variants` can switch appearance and actions immediately.

### Persistent Escrow And Recovery

- Added persistent free-slot escrow with `PREPARED`, `HELD`, and `RETURN_PENDING` states. SQLite and MySQL writes are serialized per session.
- Real items are returned on normal close, menu replacement, reload, and quit. Full inventories retain pending records for retry on the next login.
- Releasing example menus now safely closes active Container sessions before reloading menus, preventing stale-generation windows from retaining free-slot items.
- Plugin shutdown retains escrow and waits for already-started SQL work, avoiding item loss from Folia player scheduling and connection-pool shutdown ordering.
- Fixed `NoClassDefFoundError` on the first free-slot placement when newer Paper builds no longer expose SnakeYAML's private `Base64Coder`; legacy line-wrapped Base64 data remains readable.
- Improved client synchronization for free-slot placement, pickup, swaps, Shift transfers, and drags by committing the validated final state on the player event thread, avoiding the rollback-then-render double animation caused by delayed cancelled transactions.
- If free-slot SQL escrow fails, KaMenu now closes the current window and enters the unified return flow so real items do not remain in a session without a reliable ledger.
- Paper, Folia, and Spigot now replay cancelled left/right-click transactions from the server's current slot and player cursor state instead of relying on platform-specific `InventoryAction` or event cursor snapshots, preventing item loss during normal placement and pickup.

### Compatibility And Text Icons

- Fixed Spigot Dialog clickable text being truncated at an inner `>` and rendered as plain text when `text` or `hover` contains an ItemsAdder, Oraxen, or CraftEngine glyph tag.
- Extended the `&item:[...]` two-dimensional icon syntax to resolve vanilla materials and ItemsAdder, Oraxen, and CraftEngine custom-item textures, with root-level `item_sprites.yml` overrides for complex models.
- Sprite icons are enabled only on Minecraft `1.21.9+`. Servers running `1.21.8` or older safely remove the marker without querying third-party item APIs or resource packs.

### TrMenu Migration Fixes

- Fixed broad `action` / `actions` matching that incorrectly produced `actionbar`; `actions` lists wrapped by standard YAML anchors are now unwrapped before each nested action is converted.
- The migrator now processes parsed YAML anchor content instead of treating `*anchor` as action text. Source anchor names may be expanded or renamed by the output serializer.

### Documentation And Examples

- Added complete Chinese/English free-slot documentation, action references, a custom crafting example, crash-consistency boundaries, YAML anchor migration guidance, two-dimensional item icon guidance, and synchronized KaMenu authoring Skill references.
