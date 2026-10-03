# KaMenu v2.0.8 更新日志 / Changelog

## 版本信息 / Release Information

- **版本号 / Version:** 2.0.8
- **发布日期 / Release Date:** 2026年10月4日 / October 4, 2026

---

## 中文

### 新增公开事件 API

KaMenu 现在提供两个公开 Bukkit 事件，供其他插件（如 EcoQuests、KaScoreboard 等）监听菜单打开与数据变化，以驱动任务、统计等逻辑。事件类位于 `org.katacr.kamenu.api.event` 包。

#### `KaMenuOpenEvent` — 菜单打开事件

- 玩家打开 KaMenu 菜单时触发，在菜单真正渲染（容器菜单与 Dialog 菜单均可）前同步抛出。
- 携带字段：`menuId`（菜单 ID，如 `"主菜单/主菜单"`）、`source`（打开来源：`COMMAND` / `BUTTON` / `API` / `OTHER`）、`arguments`（传入菜单的参数列表），以及继承自事件的玩家引用。
- 覆盖全部打开入口：`MenuUI.openMenu` / `openConfig` / `forceOpenMenu` / `forceOpenConfig`（含 KaMenuAPI 与自定义指令），每个入口各触发一次，不重复。

#### `KaMenuDataChangeEvent` — 数据变化事件

- KaMenu 数据存储（`data` / `gdata` / `list` / `glist` / `meta` / 玩家临时数据）发生写入、修改、增减或删除时触发。
- 携带字段：`scope`（作用域：`PLAYER_DATA` / `GLOBAL_DATA` / `PLAYER_LIST` / `GLOBAL_LIST` / `META` / `PLAYER_TMP`）、`action`（操作：`SET` / `MODIFY` / `ADD` / `REMOVE` / `CLEAR`）、`key`、`value`（删除类操作可能为 `null`）、`player`（玩家在线时非空，全局作用域为 `null`）。
- 由异步数据库写操作触发，KaMenu 自动切回服务端主线程后再抛出，监听器内可安全调用 Bukkit API。

### `item_bindings.yml` / `listeners.item-lore` 支持动作包

物品右键绑定现在除「打开菜单」外，还可直接执行动作列表。

- 新增字段 `actions`：要执行的动作列表，支持全局动作包调用（如 `actions: voucher/use_plot`）。
- `menu` 与 `actions` 至少配置一个；**两者同时配置时后台输出警告，并以 `menu` 为主，`actions` 被忽略**。
- 文档：`docs/config/item-bindings.md` 与 `docs-en/config/item-bindings.md` 已同步。

### `&item:[...]` 图标强制白色

`&item:[...]` 转换为 MiniMessage `<sprite>` 后会继承周围文本颜色。现生成时强制包裹 `<white>...</white>`，确保物品图标始终为白色，不再被其他行的 `&a`/`&7` 等颜色染色。

### 修复 `item: type=take` 无法扣除原版物品

修复了 `item: type=take;mats=<原版材质>;amount=<数量>` 无法扣除原版物品的问题。
此前扣除动作对原版物品（如骨头、钻石等）始终不生效，`give` 发放不受影响。
现在原版物品可正常扣除。

### 文档同步

- 中文 `docs/API.md` 新增「监听事件」章节。
- 英文 `docs-en/API.md` 新增「Listening to Events」章节。

---

## English

### New Public Event API

KaMenu now exposes two public Bukkit events for other plugins (such as EcoQuests, KaScoreboard, etc.) to listen to menu opens and data changes — e.g. to drive quests or statistics. Both event classes live in the `org.katacr.kamenu.api.event` package.

#### `KaMenuOpenEvent` — Menu Open Event

- Fired when a player opens a KaMenu menu, thrown synchronously just before the menu actually renders (both container and dialog menus).
- Fields: `menuId` (menu ID, e.g. `"main/main_menu"`), `source` (open source: `COMMAND` / `BUTTON` / `API` / `OTHER`), `arguments` (arguments passed to the menu), plus the player reference inherited from the event.
- Covers all open entry points: `MenuUI.openMenu` / `openConfig` / `forceOpenMenu` / `forceOpenConfig` (including KaMenuAPI and custom commands) — fired once per entry, without duplication.

#### `KaMenuDataChangeEvent` — Data Change Event

- Fired when KaMenu storage (`data` / `gdata` / `list` / `glist` / `meta` / player temp data) is written, modified, added, removed, or cleared.
- Fields: `scope` (`PLAYER_DATA` / `GLOBAL_DATA` / `PLAYER_LIST` / `GLOBAL_LIST` / `META` / `PLAYER_TMP`), `action` (`SET` / `MODIFY` / `ADD` / `REMOVE` / `CLEAR`), `key`, `value` (`null` for removal operations), `player` (non-null while the player is online; `null` for global scopes).
- Triggered by async database writes and automatically rescheduled to the server main thread before dispatch; Bukkit APIs are safe inside listeners.

### Action lists in `item_bindings.yml` / `listeners.item-lore`

Item right-click bindings can now run an action list in addition to opening a menu.

- New field `actions`: list of actions to run, supports global action package calls (e.g. `actions: voucher/use_plot`).
- At least one of `menu` / `actions` is required; **when both are set, a warning is logged and `menu` takes priority — `actions` is ignored**.
- Docs updated: `docs/config/item-bindings.md` and `docs-en/config/item-bindings.md`.

### `&item:[...]` icons forced white

`&item:[...]` compiles to a MiniMessage `<sprite>` that inherits the surrounding text color. It is now wrapped in `<white>...</white>` so item icons always render white and are no longer tinted by reward-line colors such as `&a`/`&7`.

### Documentation

- Chinese `docs/API.md`: new "监听事件" section.
- English `docs-en/API.md`: new "Listening to Events" section.

### Fixed `item: type=take` failing to remove vanilla items

Fixed `item: type=take;mats=<vanilla>;amount=<n>` failing to remove vanilla items.
The take action previously never removed vanilla items (e.g. bone, diamond); `give` was unaffected.
Vanilla items can now be removed correctly.