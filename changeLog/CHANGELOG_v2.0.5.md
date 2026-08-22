# KaMenu v2.0.5 更新日志 / Changelog

## 版本信息 / Release Information

- **版本号 / Version:** 2.0.5
- **发布日期 / Release Date:** 2026年8月21日 / August 21, 2026

---

## 中文

### KaProxy 跨服目标选择器

- 新增 `{cross}` 标记。在动作末尾的目标选择器 `{player: 选择器}` 后追加 `{cross}`，即可通过 KaProxy 把动作转发到其他后端执行。
- 新增 KaMenu `kaproxy.enabled` 配置项。启用后自动注册 `kaproxy:main` 插件消息通道；关闭或卸载时自动注销。
- KaProxy 新增 `modules.kamenu` 模块及 `servers` 配置。列表控制可接收跨服动作的后端；设为 `all` 或 `*` 时转发到所有后端，始终排除来源服。
- 目标后端收到动作后在本地求值选择器，因此 PAPI 变量和 KaMenu 内置条件在各服均可正常使用。
- 跨服执行采用后端白名单保护，仅支持 `tell`、`actionbar`、`title`、`sound`、`hovertext`、`money`、`points`、`data`、`gdata`、`tmpdata`、`meta`、`set-data`、`set-gdata`、`set-meta`。非白名单动作会被拒绝并记录警告。
- `/kaproxy status` 现在显示 Kamenu 模块状态。

### 文档

- 新增 KaProxy Kamenu 模块中英文文档，并补充 KaMenu 中英文动作文档与菜单编写 Skill 的跨服选择器说明。

### Container 多页菜单与运行时动态槽位

- **多页菜单**：KaMenu Container 现在支持原生多页布局。`Layout` 接受页数组（如 `[['A...'], ['B...']]`），`page: n` / `page: next` / `page: prev` / `page: +n` / `page: -n` 动作在会话内安全切页，不触发菜单 Open/Close，并保留参数、任务和图标状态。迁移器将 TrMenu 多页 `Layout` 与 `Default-Layout` 无损转换为 `Layout` 页数组与 `Settings.default_page`。
- **运行时动态槽位**：按钮可声明 `slot` 表达式（支持 PAPI 变量与 KaMenu 变量），在每次打开、刷新或翻页时按玩家状态解析为槽位，不再依赖静态 `Layout` 字符。例如 `slot: '%player_empty_slots%'` 让按钮随玩家空槽数量移动。
- **多副本槽位**：`slot: [8, 9, 10]`（扁平列表）表示同一按钮同时渲染在多个槽位。
- **运动帧动画**：`slot: [[8], [9], [10]]`（列表的列表）表示逐帧循环，每次刷新把按钮移动到下一帧位置，实现 TrMenu 风格的运动槽位效果。
- 点击位置优先查运行时动态槽位映射，再回退到静态布局。动态槽位与静态布局槽位冲突时跳过该动态按钮，旧槽位物品在帧切换时被自动清理。
- **运行时标题覆盖（`set-title` 动作）**：新增 `set-title: <标题>` 动作，可在点击或任务中动态覆盖当前容器会话的库存窗口标题，优先级高于配置 `Title`。覆盖值会一直生效直到会话结束，后续 `refresh: title` / `refresh: *` 与周期 `Title-Update` 都以该覆盖值刷新，因此可结合 PAPI 变量做出随状态变化的标题。该动作与 `title:`（向玩家发送淡入淡出大标题特效、不影响库存标题）相互区分。

### 修复

- 修复 Paper 原生 Dialog 的 `Inputs` 中 `type: checkbox` 无论勾选状态如何都返回 `false` 的问题。回调现在按组件声明类型读取响应，checkbox 使用布尔值读取，不再被数值读取的默认 `0` 覆盖。
- 修复 Paper 平台上聊天可点击文本（`hovertext:` 发送、`<text=...;actions=...>`）的自定义回调在执行 `Events.Click` 动作时丢失 `contextId` 的问题。`contextId` 现已沿解析链路（`parseClickableText` → 回调 → `executeActionList`）透传，与 Spigot 路径一致，使点击回调能正确关联原菜单的参数会话与任务上下文（此前 Paper 该路径的 `contextId` 为空）。

---

## English

### KaProxy Cross-Server Target Selector

- Added the `{cross}` tag. Append it after an action's `{player: selector}` target selector to forward the action to other backends through KaProxy.
- Added the KaMenu `kaproxy.enabled` setting. When enabled, KaMenu registers the `kaproxy:main` plugin-message channel and unregisters it during shutdown.
- Added KaProxy's `modules.kamenu` module and `servers` setting. The list controls which backends receive cross-server actions; setting it to `all` or `*` forwards to every backend while always excluding the source backend.
- Target backends evaluate selectors locally, so PAPI variables and KaMenu built-in conditions work independently on every backend.
- Cross-server execution is protected by a backend whitelist: `tell`, `actionbar`, `title`, `sound`, `hovertext`, `money`, `points`, `data`, `gdata`, `tmpdata`, `meta`, `set-data`, `set-gdata`, and `set-meta`. Non-whitelisted actions are rejected with a warning.
- `/kaproxy status` now shows the Kamenu module state.

### Container Multi-Page Menus and Runtime Dynamic Slots

- **Multi-page menus**: KaMenu Container now supports native multi-page layouts. `Layout` accepts a page array (e.g. `[['A...'], ['B...']]`), and `page: n` / `page: next` / `page: prev` / `page: +n` / `page: -n` actions switch pages safely within the session without triggering menu Open/Close, preserving arguments, tasks, and icon state. The migrator losslessly converts TrMenu multi-page `Layout` and `Default-Layout` into the `Layout` page array and `Settings.default_page`.
- **Runtime dynamic slots**: A button may declare a `slot` expression (supporting PAPI and KaMenu variables) that is resolved per player on each open, refresh, or page switch, instead of relying on a static `Layout` character. For example `slot: '%player_empty_slots%'` moves the button according to the player's empty slot count.
- **Multi-copy slots**: `slot: [8, 9, 10]` (flat list) renders the same button across multiple slots simultaneously.
- **Cycling frame animation**: `slot: [[8], [9], [10]]` (list of lists) cycles on each refresh, moving the button to the next frame position, reproducing TrMenu's moving-slot effect.
- Click positions resolve via the runtime dynamic slot mapping first, then fall back to the static layout. Dynamic slots conflicting with static layout slots are skipped, and stale slot items are cleared automatically on frame change.
- **Runtime title override (`set-title` action)**: Added the `set-title: <title>` action, which dynamically overrides the current Container session's inventory window title at click or task time, taking priority over the configured `Title`. The override persists until the session ends, and subsequent `refresh: title` / `refresh: *` and periodic `Title-Update` all refresh using the overridden value, so the title can vary with state via PAPI variables. This is distinct from `title:` (which sends a fading big-title effect to the player and does not change the inventory title).

### Documentation

- Added Chinese and English KaProxy Kamenu module documentation, and documented cross-server selectors in KaMenu's Chinese/English action docs and menu-authoring Skill.

### Fixes

- Fixed Paper native Dialog `Inputs` with `type: checkbox` always returning `false` regardless of its selected state. Callbacks now read responses by declared component type, so checkbox booleans are no longer masked by the numeric reader's default `0`.
- Fixed the loss of `contextId` when a chat clickable-text custom callback (sent via `hovertext:`, using `<text=...;actions=...>`) executes an `Events.Click` action list on Paper. The `contextId` is now threaded through the parsing chain (`parseClickableText` → callback → `executeActionList`), matching the Spigot path, so click callbacks correctly associate with the originating menu's argument session and task context (previously the Paper path passed a null `contextId`).
