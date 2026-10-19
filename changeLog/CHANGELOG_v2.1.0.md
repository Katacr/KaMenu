# KaMenu v2.1.0 更新日志 / Changelog

## 版本信息 / Release Information

- **版本号 / Version:** 2.1.0
- **发布日期 / Release Date:** 2026年10月17日 / October 17, 2026

---

## 中文

### 物品显示新增 `hide_tooltip` 与 `hide_components`

容器菜单按钮与 Dialog 物品新增两个 tooltip 隐藏属性，均可用于 `display`（容器）或 `item` 主体（Dialog）。

- `hide_tooltip`：隐藏整个物品提示框。
- `hide_components`：只隐藏原版自动生成的组件行（属性、附魔、耐久、可破坏/可放置等），**保留**菜单自定义的名称与 Lore。
- Paper 上通过隐藏数据组件实现；Spigot 等非 Paper 核心退化为隐藏常见组件行的物品旗标。
- `hide_tooltip` 优先于 `hide_components`；需要 1.20.5+ 核心，旧核心会输出配置警告并跳过。

### 物品显示新增 `color` 与 `damage`，并更新 `glow`

- `color`：为皮革护甲、药水、烟花效果染色，支持 `R,G,B` 或 `#RRGGBB`。
- `damage`：设置已损失耐久（仅可损坏物品）。
- `glow` 改为三态：未设置=不处理，`true`=强制发光，`false`=取消光效。1.20.5+ 使用原版附魔光效覆盖组件；旧核心或不兼容时，`true` 回退为附魔模拟方案，`false` 在无法取消时仅记录提示。不再提供单独的 `glint_override` 键。
- 已在 Paper 26.3（build 159 BETA）验证相关属性正常加载和工作。
- 中英文 Container 按钮与 Dialog Body 文档已同步。

### 新增 `post-effect` 动作（屏幕后期效果）

新增 `post-effect` 菜单动作，操作玩家的屏幕后期效果（全屏着色器滤镜），用于打开菜单时背景虚化、
进入场景时施加氛围滤镜等。

- 格式：`post-effect: type=add|remove|set|clear|list;effect=<namespace:name>`
- 原版内置效果：`minecraft:blur`、`minecraft:creeper`、`minecraft:spider`、`minecraft:invert`、`minecraft:entity_outline`。
- 需要 Paper 26.3+；旧核心或不支持时记录警告并跳过，不影响其余动作。
- 自定义效果可由资源包在 `assets/<namespace>/post_effect/<name>.json` 提供。
- 支持目标选择器，可作用于其他在线玩家。

### 文档同步

- 中文 `docs/modern-dialog/actions.md` 新增 `post-effect` 说明。
- 英文 `docs-en/modern-dialog/actions.md` 新增 `post-effect` 说明。

### JavaScript `giveItem` helper

新增 `giveItem(material, amount, targetPlayer?)`，供 JavaScript 复用内置 `item: type=give` 的物品构造、背包写入、满包掉落及提示逻辑。

- 支持原版材质，例如 `minecraft:bread`。
- 支持 CraftEngine 物品，例如 `ce:customfishing:simple_bait`。
- `targetPlayer` 可选，默认使用当前脚本上下文的玩家。
- 中英文 JavaScript 文档与 KaMenu Menu Author Skill 已同步。

### `item: type=take` 支持 `cmd` 匹配

`item: type=take` 新增整数 CustomModelData 匹配维度，与既有 `mats`、`lore`、`model` 并列，可组合使用。

- 新增参数 `cmd`（别名 `custom_model_data`、`custom_model_id`），读取物品的整数 `custom_model_data` 组件。
- `mats` 不再是必需项：可仅用 `cmd`（或 `lore`/`model`）精确定位物品，便于扣除不带 `craftengine:id` 标签的 CraftEngine `client-bound-model` 物品。
- 与 `hasItem`/`%kamenu_hasitem_%` 的 `cmd` 语义保持一致。
- 中文 `docs/modern-dialog/actions.md` 与英文 `docs-en/modern-dialog/actions.md` 的 `item` 动作说明已同步。

### Container repeat 动态列表按钮

Container 按钮新增 `type: repeat`，将 `Layout` 中同 ID 的固定槽位作为动态列表矩阵，并为每个槽位绑定对应数据项。

- 支持 `source`、`item.display`、`item.actions`、JSON/字符串列表和 JavaScript 数据源。
- 每页容量由当前 Layout 中该按钮 ID 的槽位数决定；新增 `repeat-page: <id> next|prev|+N|-N|页码` 独立翻页，不影响 Container Layout 翻页。
- 支持 `{page:<id>}`、`{pages:<id>}`、`{total:<id>}`、`{start:<id>}`、`{end:<id>}` 分页变量。
- 空列表默认显示 AIR 且无点击动作；可用 `empty.display` 配置无动作的占位物。
- 中文/英文 Container 按钮、布局及动作文档与 KaMenu Menu Author Skill 已同步。

### 统一物品绑定配置

物品绑定统一放在 `item_bindings.yml`，用 `item`、`trigger` 和 `actions` 分别配置匹配物品、触发手势及执行动作；打开菜单使用 `open: <菜单ID>`。`trigger.type` 支持左键、右键、F、Q，`trigger.sneaking` 区分潜行，点击可用 `trigger.target` 指定空气或方块。F 优先匹配物品绑定，Q 命中时取消丢弃；未配置冷却时默认 500ms。升级时自动迁移旧格式及 `config.yml` 中的物品监听器。详见 `docs/config/item-bindings.md`。

### 自定义指令别名

对象式自定义指令（`menu:` 或 `actions:`）新增 `aliases` 列表，可为同一指令注册多个别名。别名共用原指令的菜单/动作定义及参数 Tab 补全；字符串简写 `command: menu/id` 不支持声明别名，需改用对象格式。详见 `docs/config/customCommands.md`。

---

## English

### New `hide_tooltip` and `hide_components` item display properties

Container menu buttons and Dialog items gain two tooltip-hiding properties, usable in `display` (container) or `item` body (Dialog).

- `hide_tooltip` hides the entire item tooltip.
- `hide_components` hides only vanilla-generated component lines (attributes, enchantments, durability, can-destroy/can-place-on, etc.) while **keeping** the custom name and lore.
- Paper hides data components; non-Paper cores such as Spigot fall back to item flags that hide common component lines.
- `hide_tooltip` takes priority. Requires a 1.20.5+ core; older cores log a configuration warning and skip.

### New `color` and `damage` item display properties; updated `glow`

- `color` dyes leather armor, potion and firework effects; accepts `R,G,B` or `#RRGGBB`.
- `damage` sets damage taken (damageable items only).
- `glow` is now tri-state: unset = do nothing, `true` = force glint, `false` = cancel glint. On 1.20.5+ it uses the vanilla enchantment glint override component. On older or incompatible cores, `true` falls back to enchant simulation; `false` only logs a notice when glint cannot be cancelled. The separate `glint_override` key is no longer provided.
- Verified on Paper 26.3 (build 159 BETA).
- Chinese and English Container button and Dialog Body documentation are synchronized.

### New `post-effect` action (screen post effects)

New `post-effect` menu action to manage a player's screen post effects (full-screen shader filters),
useful for blurring the background when opening a menu or applying ambient filters in a scene.

- Format: `post-effect: type=add|remove|set|clear|list;effect=<namespace:name>`
- Built-in vanilla effects: `minecraft:blur`, `minecraft:creeper`, `minecraft:spider`, `minecraft:invert`, `minecraft:entity_outline`.
- Requires Paper 26.3+; older or unsupported cores log a warning and skip without affecting other actions.
- Custom effects can be provided by a resource pack at `assets/<namespace>/post_effect/<name>.json`.
- Supports target selectors, so it can apply to other online players.

### Documentation

- Chinese `docs/modern-dialog/actions.md`: new `post-effect` section.
- English `docs-en/modern-dialog/actions.md`: new `post-effect` section.

### JavaScript `giveItem` helper

Added `giveItem(material, amount, targetPlayer?)` so JavaScript can reuse the built-in `item: type=give` behavior for item construction, inventory insertion, overflow drops, and feedback.

- Supports vanilla material names such as `minecraft:bread`.
- Supports CraftEngine items such as `ce:customfishing:simple_bait`.
- `targetPlayer` is optional and defaults to the player in the current script context.
- Chinese and English JavaScript docs plus the KaMenu Menu Author Skill are synchronized.

### `item: type=take` now supports `cmd` matching

`item: type=take` gains an integer CustomModelData match dimension, alongside the existing `mats`, `lore`, and `model` dimensions; all are combinable.

- New parameter `cmd` (aliases `custom_model_data`, `custom_model_id`) reads the item's integer `custom_model_data` component.
- `mats` is no longer required: `cmd` (or `lore`/`model`) alone can precisely target an item, which helps take CraftEngine `client-bound-model` items that carry no `craftengine:id` tag.
- Semantics now match the `cmd` support already available in `hasItem`/`%kamenu_hasitem_%`.
- Chinese `docs/modern-dialog/actions.md` and English `docs-en/modern-dialog/actions.md` `item` action sections are synchronized.

### Container `repeat` dynamic list buttons

Container buttons now support `type: repeat`, binding a dynamic list to the fixed `Layout` slots using the same button ID. Each slot retains the item data used to render and execute its click actions.

- Supports `source`, `item.display`, `item.actions`, JSON/string lists, and JavaScript data sources.
- Page capacity comes from the number of matching IDs in the current Layout. The new `repeat-page: <id> next|prev|+N|-N|pageNumber` action navigates the list independently from Container Layout pages.
- Adds `{page:<id>}`, `{pages:<id>}`, `{total:<id>}`, `{start:<id>}`, and `{end:<id>}` pagination variables.
- Empty lists render AIR with no click action by default; `empty.display` may configure a display-only placeholder.
- Chinese/English Container button, layout, and action docs plus the KaMenu Menu Author Skill are synchronized.

### Unified item binding configuration

Configure item bindings only in `item_bindings.yml` using `item`, `trigger`, and `actions` for the matched item, gesture, and action list; open menus with `open: <menuId>`. `trigger.type` supports left-click, right-click, F, and Q; `trigger.sneaking` selects the sneak state and `trigger.target` selects air or blocks for clicks. Item bindings take priority over generic F, matching Q cancels the drop, and the default cooldown is 500ms. Upgrades automatically migrate legacy bindings and item listeners in `config.yml`. See `docs-en/config/item-bindings.md`.

### Custom command aliases

Object-form custom commands (`menu:` or `actions:`) now support an `aliases` list to register multiple names for the same command. Aliases share the command's menu/action definition and argument Tab completion. The scalar shorthand `command: menu/id` does not support aliases; use object form instead. See `docs-en/config/customCommands.md`.
