# KaMenu v2.0.6 更新日志 / Changelog

## 版本信息 / Release Information

- **版本号 / Version:** 2.0.6
- **发布日期 / Release Date:** 2026年8月23日 / August 23, 2026

---

## 中文

### 运行时物品判断与扣除支持 `name` 与 `item_model`

- `hasItem` 条件与 `item: type=take` 扣除动作现在支持按显示名（`name`）与物品模型（`item_model`，命名空间+ID 字符串格式，非数字 `custom_model_data`）匹配。
- 匹配规则：`name` 为**包含匹配**（忽略大小写、忽略颜色码 `§`），与已有 `lore` 匹配语义一致；`item_model` 为精确匹配（忽略大小写）。
- `mats` 不再是必填：允许仅按 `name` 或仅按 `item_model` 判断/扣除物品。这覆盖了 RPG 类型服务器使用铁砧改名或自定义模型物品、并以显示名/模型作为有效识别依据的场景。
- 说明：按 `name` 判断依赖显示名文本，理论上仍可被铁砧改名绕过；这是 RPG 玩法的常见约定，KaMenu 现提供该能力，由菜单作者自行评估安全性。

### 容器菜单迁移（TrMenu）— `item` 条件与 `take-item` 动作支持 `name` / `item_model`

- TrMenu 的 `item *"amount:1,name:<名称>"` 这类仅按显示名或物品模型的判断/扣除，现可迁移为 `hasItem.[name=<名称>;amount=1]` / `item: type=take;name=<名称>;amount=1`（以及 `item_model` 等价形式），不再发 `TRM_ITEM_MATCHER_UNSUPPORTED` / `TRM_CONDITION_UNSUPPORTED`。
- 修正了迁移器词法切分：TrMenu 原始语法中 `*` 前缀紧跟引号 `*"amount:1,name:..."` 现会被正确拆分为独立 `*` 标记与后续引号内容，避免此前将整段当作单个畸形 trait 而误报 `Unknown ItemMatcher trait`。
- `data`、`head`（材质头像/纹理）仍因无便携等价而不迁移。

### 物品模型展示与容器布局兼容性修正

- 修复 Dialog 物品组件和容器菜单 `display` 组件设置 `item_model` 后未可靠写入 Bukkit `ItemStack` 的问题；同时兼容现代核心中 `ItemMeta` 接口与具体实现类的 API 调用。
- 容器布局行现在始终归一化为容器规定的列数：不足的部分自动补为空槽，超出的部分截断，不再因列数不符禁用菜单。
- Layout 引用未定义的按钮键、Buttons 中未被 Layout 使用的键现在直接允许，不再生成 `layout.button_not_found` 或 `button.unused` 诊断。

### 容器菜单物品小图标（`&item:[...]`）支持

- 在 1.21.9+ 服务器上，容器菜单的按钮显示名、Lore 和菜单标题现在支持 `&item:[coal]` 物品小图标语法，与 Dialog 保持一致。
- 通过反射调用 Paper 的 Adventure Component API（`ItemMeta.displayName(Component)`、`ItemMeta.lore(List<Component>)`、`InventoryView.title(Component)`）实现；旧核心自动回退到 Legacy 文本，不产生错误。

---

## English

### Runtime Item Check and Deduction Support `name` and `item_model`

- The `hasItem` condition and the `item: type=take` deduction action now support matching by display name (`name`) and item model (`item_model`, namespace+ID string format, not the numeric `custom_model_data`).
- Matching rules: `name` uses **contains matching** (case-insensitive, color-code `§` stripped), consistent with the existing `lore` matching; `item_model` uses exact matching (case-insensitive).
- `mats` is no longer required: matching/deducting by `name` alone or `item_model` alone is now allowed. This covers RPG-style servers where players rename items with an anvil or use custom-model items, and the display name / model serves as a valid identity.
- Note: `name`-based matching relies on display-name text and can in theory still be bypassed by renaming via an anvil; this is a common RPG convention and KaMenu now provides the capability, leaving the security trade-off to the menu author.

### Container Menu Migration (TrMenu) — `item` Condition and `take-item` Action Support `name` / `item_model`

- TrMenu conditions like `item *"amount:1,name:<name>"` that identify items solely by display name or item model can now migrate to `hasItem.[name=<name>;amount=1]` / `item: type=take;name=<name>;amount=1` (and the `item_model` equivalent), instead of emitting `TRM_ITEM_MATCHER_UNSUPPORTED` / `TRM_CONDITION_UNSUPPORTED`.
- Fixed the migrator's lexical tokenizer: the original TrMenu syntax where a `*` prefix is immediately followed by a quote (`*"amount:1,name:..."`) is now correctly split into a separate `*` token and the quoted content, instead of being treated as a single malformed trait that previously raised a false `Unknown ItemMatcher trait`.
- `data` and `head` (skull/texture) remain unmigrated due to lack of a portable equivalent.

### Item Model Rendering and Container Layout Compatibility Fixes

- Fixed Dialog item components and container `display` components failing to persist `item_model` to the Bukkit `ItemStack`; modern cores using either the `ItemMeta` API or a concrete implementation are supported.
- Container layout rows are now normalized to the required number of columns: short rows are padded with empty slots and excess columns are discarded, without disabling the menu.
- Layout references to undefined button keys and button definitions unused by Layout are now allowed without emitting `layout.button_not_found` or `button.unused` diagnostics.

### Container Menu Item Sprite (`&item:[...]`) Support

- On 1.21.9+ servers, container menu button display names, lore, and menu titles now support the `&item:[coal]` item sprite syntax, consistent with Dialog.
- Implemented via reflective calls to Paper's Adventure Component API (`ItemMeta.displayName(Component)`, `ItemMeta.lore(List<Component>)`, `InventoryView.title(Component)`); older cores automatically fall back to legacy text without errors.
