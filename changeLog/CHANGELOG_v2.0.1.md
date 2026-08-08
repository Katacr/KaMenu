# KaMenu v2.0.1 更新日志 / Changelog

## 版本信息 / Release Information

- **版本号 / Version:** 2.0.1
- **发布日期 / Release Date:** 2026年8月8日 / August 8, 2026

---

## 中文

### Container 按钮状态变体

- 新增 `Buttons.<id>.variants`，允许同一个物理槽位根据权限、冷却、物品、数据或其他条件显示不同的完整按钮状态。
- 每个变体独立配置完整的 `display` 和 `actions`，避免为同一槽位的名称、Lore、材质和点击动作重复编写条件。
- 支持 `priority` 优先级：数值越小越优先；相同优先级保持 YAML 声明顺序。
- 当所有变体都未指定 `priority` 时，严格按照 YAML 从上到下选择第一个满足条件的变体。
- 渲染和点击时都会重新判断当前变体，避免权限或冷却变化后执行已经过期的动作。
- 保留旧版按钮格式兼容；同一个按钮不能混用 `variants` 与顶层 `display` / `actions`。
- 增加变体配置错误诊断，包括缺少 `display.material`、无效 `priority` 和混用旧格式等情况。

### DeluxeMenus 迁移增强

- DeluxeMenus 中同一 `slot` 的多个候选物品现在会自动迁移为 KaMenu 的 `variants`。
- 迁移器按 DM 的 `priority` 升序处理，相同优先级保持源文件顺序。
- DM 未显式配置 `priority` 时，迁移器会写入 DM 的默认有效值，避免迁移后候选顺序发生变化。
- `requirements_menu.yml` 已用于实际 Paper1.16.5 测试服验证，3 个菜单成功生成，运行时菜单重载成功。
- 移除插件资源内的静态 `dm_migrated` 迁移样例，迁移结果仅在运行时输出到 `plugins/KaMenu/menus/dm_migrated`，避免将第三方菜单快照打包进插件。

### TrMenu 迁移系统

- 新增 `/km migrate trmenu`（别名 `trm`），将 TrMenu stable-v3 经典库存菜单编译为 KaMenu V2 Container 标准 YAML；默认读取 `plugins/TrMenu/menus`，输出到 `plugins/KaMenu/menus/trmenu_migrated`。
- 迁移器不依赖 TrMenu、Kether 或 TabooLib，也不会在迁移期间执行源菜单的 JavaScript、指令和动作。
- 支持版本化正则键、单页常用容器布局、显式槽位、反引号多字符按钮 ID、嵌套图标变体、常见物品字段和跨文件菜单跳转。
- 支持常用 Kether 条件、消息/指令/音效/经济/点券/数据等动作、组合动作、延迟与概率，以及 `Events.Open`、`Events.Close` 和自动任务。
- 可静态审计的纯返回值 `Functions` 会转换为菜单 JavaScript；TrMenu 私有对象、任意 Kether 流程和动态私有物品表达式会被拒绝或明确报告。
- 普通 `Bindings.Commands` 会合并到 `custom_commands.yml`；默认保留同名指令和已有目标文件，使用 `overwrite` 才会替换。
- 可兼容的 `Bindings.Items` 会合并到独立的 `item_bindings.yml`，支持 material、lore、name、data 和 model-data，并保留 TrMenu 默认的 2000ms 绑定冷却；不安全的 trait 会跳过并输出诊断。
- 新增 TrMenu 固定 `utils` 映射：在线玩家和装备槽位条件转换为 KaMenu 原生条件，`utils.getEquipment` 转换为 `[HEAD:玩家]` 等标准物品来源；简单参数形式的 `hasMoney` 和 `hasItem` 复用现有条件。
- 新增 `set-args` 与 `del-args` 标准动作，并迁移 TrMenu 同名动作；`set-args` 替换当前菜单参数后刷新 Container 或 Dialog，`del-args` 仅清理当前参数。
- 新增结构化 WARNING/ERROR 诊断、原子写入和 KaMenu Container 二次解析校验；不支持的条件不会被静默删除后继续执行受保护动作。
- 增加中英文 TrMenu 迁移文档，并使用 stable-v3 内置样例进行批量兼容审计。

### Container 刷新动作

- 新增无目标 `refresh` / `refresh:` 语法，原地刷新当前 Container 的全部按钮图标，不重新打开菜单，也不触发 `Events.Open`。
- 保留 `refresh: *` 作为完整刷新，继续同时刷新标题、容器属性和全部按钮；按钮 ID、`title`、`properties` 仍支持局部刷新。
- TrMenu 无参数 `refresh` / `update` 现在转换为 KaMenu 裸 `refresh`；TrMenu `reset` 近似转换为全部按钮刷新，并对无法保留的动画索引输出明确诊断。

### 文档与维护规范

- 将中英文 Container 文档拆分为独立的 `container/` 分组，分别细化 `Type`、`Title`、`Settings`、`Layout`、`Buttons`、`Properties`、刷新机制和 `Events`。
- 原 `menu/` 文档目录更名为 `modern-dialog/`，明确其中内容属于现代原生 Dialog 菜单；通用事件、动作和条件由 Container 文档直接跳转复用。
- 修正原 `layout.md` 实际介绍 `Title` 的命名错误，正式标题文档更名为 `title.md`，并保留旧路径说明页指向真正的 Container Layout 文档。
- 补充项目级 `AGENTS.md`，记录项目兼容目标、四个测试服、构建部署方式以及中英文文档和 Skill 的同步要求。

---

## English

### Container Button State Variants

- Added `Buttons.<id>.variants`, allowing one physical slot to render different complete button states based on permissions, cooldowns, items, data, or other conditions.
- Each variant owns a complete `display` and `actions` definition, avoiding repeated conditions across the material, name, lore, and click action of one slot.
- Added `priority`: lower values are selected first, while equal priorities preserve YAML declaration order.
- When no variant declares `priority`, variants are evaluated strictly from top to bottom and the first matching variant is selected.
- The current variant is resolved again during both rendering and clicking, preventing stale actions after a permission or cooldown change.
- The legacy button format remains supported; a button cannot combine `variants` with top-level `display` or `actions`.
- Added diagnostics for invalid variant definitions, including missing `display.material`, invalid `priority`, and mixed legacy/variant formats.

### DeluxeMenus Migration Improvements

- Multiple DeluxeMenus candidates sharing one `slot` are now migrated into KaMenu `variants` automatically.
- Candidates are ordered by DM `priority`, with source-file order preserved for equal priorities.
- When DM does not explicitly define `priority`, the migrator writes DM's effective default value so candidate ordering remains unchanged after migration.
- `requirements_menu.yml` was verified on the real Paper1.16.5 test server; all 3 menus were generated and the runtime menu reload succeeded.
- Removed static `dm_migrated` migration samples from plugin resources. Migration output is generated at runtime under `plugins/KaMenu/menus/dm_migrated` instead of packaging third-party menu snapshots.

### TrMenu Migration System

- Added `/km migrate trmenu` (alias `trm`) to compile classic TrMenu stable-v3 inventory menus into standard KaMenu V2 Container YAML. It reads `plugins/TrMenu/menus` and writes `plugins/KaMenu/menus/trmenu_migrated` by default.
- The migrator does not depend on TrMenu, Kether, or TabooLib and never executes source JavaScript, commands, or actions during migration.
- Added versioned regex-key handling, common single-page container layouts, explicit slots, backtick multi-character button IDs, nested icon variants, common item fields, and cross-file menu-open mapping.
- Added common Kether conditions, message/command/sound/economy/points/data actions, compound actions, delay and chance modifiers, `Events.Open`, `Events.Close`, and automatic tasks.
- Statically auditable return-value `Functions` are converted into menu JavaScript. TrMenu private objects, arbitrary Kether flows, and dynamic private item expressions are rejected or reported explicitly.
- Plain `Bindings.Commands` entries are merged into `custom_commands.yml`. Existing command bindings and target files are preserved unless `overwrite` is supplied.
- Compatible `Bindings.Items` entries are merged into the separate `item_bindings.yml`, supporting material, lore, name, data, and model-data with TrMenu's default 2000ms binding cooldown. Unsafe traits are skipped with diagnostics.
- Added fixed TrMenu `utils` mappings: online-player and equipment-slot checks become native KaMenu conditions, while `utils.getEquipment` becomes a standard item source such as `[HEAD:player]`. Simple `hasMoney` and `hasItem` calls reuse existing conditions.
- Added standard `set-args` and `del-args` actions and their TrMenu mappings. `set-args` replaces active menu arguments and refreshes the Container or Dialog, while `del-args` only clears them.
- Added structured WARNING/ERROR diagnostics, atomic output, and a second KaMenu Container parse. Unsupported conditions are never silently removed while protected actions remain executable.
- Added matching Chinese and English TrMenu migration guides and audited the converter against the stable-v3 built-in menu samples.

### Container Refresh Action

- Added untargeted `refresh` / `refresh:` syntax to refresh every button icon in the active Container without reopening it or running `Events.Open`.
- Kept `refresh: *` as the full refresh for the title, container properties, and every button; button IDs, `title`, and `properties` remain available as targeted refreshes.
- Untargeted TrMenu `refresh` / `update` actions now become bare KaMenu `refresh`; TrMenu `reset` is approximated as an all-button refresh with an explicit diagnostic for animation indexes that cannot be retained.

### Documentation And Maintenance

- Split the Chinese and English Container documentation into dedicated `container/` groups, with separate references for `Type`, `Title`, `Settings`, `Layout`, `Buttons`, `Properties`, refresh behaviour, and `Events`.
- Renamed the former `menu/` documentation directories to `modern-dialog/` so their scope is explicitly the modern native Dialog UI; Container pages link to the shared event, action, and condition references where behaviour is unchanged.
- Corrected the old `layout.md` naming mismatch: the Dialog title page is now `title.md`, while the compatibility page points readers to the actual Container Layout reference.
- Added a project-level `AGENTS.md` documenting compatibility targets, all four test servers, build/deployment procedures, and the requirement to keep Chinese docs, English docs, and the Skill synchronized.

---

## Upgrade Guide

### Upgrading from 2.0.0 to 2.0.1

1. Replace the plugin JAR with `KaMenu-2.0.1.jar`.
2. Fully restart the server.
3. Existing Container menus using top-level `display` and `actions` require no changes.
4. Use `Buttons.<id>.variants` when one physical slot needs multiple complete states.
5. Run `/km migrate dm overwrite` to regenerate DeluxeMenus migration output with same-slot variants.
6. Run `/km migrate trmenu` to generate standard Container menus from `plugins/TrMenu/menus`, then review all WARNING/ERROR diagnostics before enabling the output in production.
