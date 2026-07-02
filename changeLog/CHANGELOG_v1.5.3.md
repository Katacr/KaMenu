# KaMenu v1.5.3 更新报告

## 📋 版本信息
- **版本号**: 1.5.3
- **发布日期**: 2026年7月3日

---

## ✨ 新增功能

### 1. 新增动态按钮列表 `type: repeat`

`Bottom.multi.buttons` 现在支持 `type: repeat`，可根据动态数据源生成真实 Paper Dialog 按钮，解决玩家列表、传送点列表、好友列表、邮件列表等项目数量不固定时难以预先设计按钮数量的问题。

```yaml
Bottom:
  type: multi
  columns: 2
  buttons:
    warp_list:
      type: repeat
      source: "[getWarpList]"
      page_size: 8
      item:
        text: "&a{item.name}"
        tooltip:
          - "&7世界: &f{item.world}"
          - "&7坐标: &f{item.x}, {item.y}, {item.z}"
        actions:
          - "actions: teleport_warp,{item.id}"
      empty:
        text: "&7暂无传送点"
        actions:
          - "toast: type=task;msg=暂无数据;icon=barrier"
```

动态列表生成的是原生 `ActionButton`，因此保留按钮的宽度、tooltip、点击动作、条件逻辑和矩阵布局能力。

### 2. 支持多种 repeat 数据源格式

`source` 支持以下数据形式：

- `[函数名]`：调用当前菜单 `JavaScript` 中的函数，函数返回 JSON 数组
- JSON 数组字符串：例如 `[{"id":"home","name":"家"}]`
- 变量解析结果：例如 `{data:warp_list}`、`{gdata:server_list}`、PlaceholderAPI 变量
- 多行文本：每行作为一个列表项
- 简单字符串列表：配合 `split` 自定义分隔符拆分

简单字符串列表示例：

```yaml
Bottom:
  type: multi
  buttons:
    player_list:
      type: repeat
      source: "%kamenu_online_players%"
      split: ","
      trim: true
      item:
        text: "&a{item.value}"
        actions:
          - "tell: 你点击了 {item.value}"
```

如果变量返回 `player1, player2, player3`，会自动生成 3 个按钮。

### 3. 新增 repeat item 变量

repeat item 模板中可使用：

- `{item.xxx}`：当前对象字段
- `{item.value}`：字符串或数字项的值
- `{item.index}`：完整列表下标，从 0 开始
- `{item.number}`：完整列表序号，从 1 开始
- `{item.page_index}`：当前页下标，从 0 开始
- `{item.page_number}`：当前页序号，从 1 开始

### 4. 新增分页状态与 `page:` 动作

KaMenu 会按玩家、菜单上下文和列表 ID 维护分页状态，并提供分页变量：

- `{page:listId}`：当前页
- `{pages:listId}`：总页数
- `{total:listId}`：总项目数
- `{start:listId}`：当前页起始下标
- `{end:listId}`：当前页结束下标

分页动作：

```yaml
- "page: warp_list next"
- "page: warp_list prev"
- "page: warp_list 1"
- "page: warp_list +1"
- "page: warp_list -1"
```

`page:` 动作只修改分页状态，不会自动刷新界面。通常需要紧跟 `reset`、`open` 或 `force-open`。

### 5. 新增内置列表数据 `list` / `glist`

KaMenu 现在支持持久化列表数据类型，可用于好友列表、传送点列表、收藏列表、服务器列表等动态数据。

```yaml
- 'list: type=set;key=friends;var=`Steve,Alex`;split=,'
- 'list: type=add;key=friends;var=`Notch`'
- 'list: type=add;key=history;var=`Notch`;unique=false'
- 'list: type=remove;key=friends;var=`Alex`'
- 'glist: type=set;key=servers;var=`survival,skyblock`;split=,'
```

读取变量：

- `{list:key}`：读取当前玩家列表，返回 JSON 数组字符串
- `{glist:key}`：读取全局列表，返回 JSON 数组字符串
- `%kamenu_list_key%` / `%kamenu_glist_key%`：通过 PlaceholderAPI 读取列表 JSON

列表变量可以直接作为动态按钮 `repeat.source` 使用：

```yaml
source: "{list:friends}"
```

---

## 🔧 优化与加固

### 1. 动作变量解析增强

动作包参数和非 item 模板变量仍可使用 `$(key)`；repeat item 字段统一使用 `{item.xxx}`，可直接写入按钮文字、tooltip 和 actions。

### 2. 外部内存菜单分页上下文兼容

repeat 分页状态会沿用当前菜单 contextId。通过 `KaMenuAPI.openYaml` / `openConfig` 打开的内存菜单，也可以正常使用 repeat 列表和 `page:` 动作。

### 3. 周期任务动作上下文继承

`Events.Tasks` 和 `on_end` 动作现在会继承菜单 contextId，避免任务内使用 `page:` 等上下文动作时状态错位。

### 4. 分页状态清理

玩家退出服务器或插件关闭时，会清理该玩家的动态列表分页状态，避免长期运行服务器出现无用状态累积。

### 5. `/km action` 动作测试指令补全

`/km action` 现在使用完整动作链执行入口，并补齐服务端动作前缀 Tab 补全，包括 `chat:`、`server:`、`stock-item:`、`item:`、`js:`、`actions:`、`page:`、`run-task:`、`stop-task:`、`wait:`、`return` 等。

### 6. 新增 `inList` / `inGlist` 条件方法

条件系统新增列表成员判断，可判断某个值是否存在于列表或组中：

```yaml
condition: 'inGlist.%player_name%;{glist:vip_players}'
condition: '!inList.$(name);Steve,Alex,Notch'
```

列表参数支持 `{list:key}` / `{glist:key}` 返回的 JSON 数组、JSON 字符串数组和简易逗号列表。成员匹配为精确匹配，默认不区分大小写。

### 7. 新增在线玩家列表 PAPI 变量

新增 `%kamenu_online_players%`，返回当前在线玩家名称的 JSON 数组，可直接用于动态按钮列表：

```yaml
source: "%kamenu_online_players%"
```

### 8. JavaScript 新增 `papi()` 辅助函数

JavaScript 内现在可以直接解析 PlaceholderAPI 变量：

```javascript
var level = parseInt(papi("player_level") || "0");
var onlinePlayers = JSON.parse(papi("kamenu_online_players"));
var targetLevel = papi("%player_level%", getPlayer("Steve"));
```

支持带 `%` 和不带 `%` 的变量名。未安装 PlaceholderAPI 或解析失败时返回空字符串。

### 9. JavaScript 新增 KaMenu 内部变量辅助函数

JavaScript 内现在可以直接读取 KaMenu 内部变量：

```javascript
var coins = parseInt(data("coins") || "0");
var status = gdata("server_status");
var temp = meta("temporary_choice");
var friends = JSON.parse(list("friends"));
var servers = JSON.parse(glist("servers"));
var raw = kvar("{gdata:server_status}");
```

支持 `kvar(...)` 通用解析，以及 `data/gdata/meta/list/glist` 便捷函数。`list` 和 `glist` 返回 JSON 数组字符串。

### 10. 新增列表长度 PAPI 变量

新增两个用于读取列表项目数量的 PlaceholderAPI 变量：

- `%kamenu_list_size_key%`：当前玩家指定列表的项目数量
- `%kamenu_glist_size_key%`：指定全局列表的项目数量

可用于条件判断：

```yaml
condition: "%kamenu_list_size_friends% > 0"
condition: "%kamenu_glist_size_servers% >= 3"
```

### 11. 新增首次使用入门向导

新增 `/km guide` / `/kamenu guide` 指令，可直接打开内置入门向导菜单。该菜单从插件 jar 内部加载到内存，不会写入 `menus` 目录，适合新服务器首次配置。

入门向导支持：

- 引导选择插件语言
- 按语言释放示例菜单
- 展示示例菜单说明
- 通过可点击文本快捷打开示例菜单

当服务器当前没有加载任何菜单，且 OP 玩家进入服务器时，KaMenu 会发送一条可点击文本，引导用户打开入门向导。

### 12. 新增语言与示例释放指令

新增语言设置指令：

```bash
/km language zh_CN
/km language en_US
```

执行后会写入 `config.yml` 并重载语言、配置和菜单。

新增示例菜单释放指令：

```bash
/km examples
/km examples zh_CN
/km examples en_US
/km examples zh_CN overwrite
```

示例菜单会按指定语言释放到 `plugins/KaMenu/menus/example/`。中文和英文示例使用同一目标目录，不再要求用户手动处理 `exampleEN` 目录；默认跳过已存在文件，添加 `overwrite` 后覆盖。

---

## 📝 文档与示例

- 新增动态按钮列表示例菜单：`example/repeat_buttons_demo.yml`
- 新增玩家互动示例菜单：`example/inspect_player.yml`，用于 Shift+右键玩家查看装备和执行互动动作
- 新增内置入门向导菜单：`internal/guide.yml`
- 新增中文/英文示例释放流程，示例菜单按语言释放到 `menus/example/`
- 更新中文文档：`docs/menu/bottom.md`
- 更新英文文档：`docs-en/menu/bottom.md`
- 更新快速开始和指令文档，补充 `/kamenu guide`、`/km language`、`/km examples`
- 更新 KaMenu 菜单编写 skill 参考

---

## 📝 兼容说明

- 旧菜单语法无需修改
- 未使用 `type: repeat` 的菜单行为不变
- `repeat` 当前仅支持 `Bottom.type: multi` 下的 `buttons` 区域
- 分页变量当前主要用于 `Bottom.multi.buttons` 的普通按钮和 repeat item 模板
- `page:` 不会自动刷新菜单，需要配合 `reset` / `open` / `force-open`
- 入门向导菜单不会释放到 `menus` 目录；需要示例菜单时请使用向导或 `/km examples`
- 默认 `listeners.player-click.menu` 已改为 `example/inspect_player`；旧默认的 `main_menu` / `inspect_player` 监听引用会在配置升级时自动迁移到 `example/...`

---

## 🚀 升级指南

### 从 1.5.2 升级到 1.5.3

1. 替换插件 jar 文件为 `KaMenu-1.5.3.jar`
2. 完整重启服务器
3. 可选：将需要动态列表的菜单改用 `Bottom.multi.buttons.type: repeat`
4. 可选：首次使用可执行 `/kamenu guide` 打开入门向导，或执行 `/km examples [zh_CN|en_US]` 释放示例菜单
5. 可选：参考 `example/repeat_buttons_demo.yml` 编写玩家列表、传送点列表或好友列表

---

# KaMenu v1.5.3 Update Report

## 📋 Version Info
- **Version**: 1.5.3
- **Release Date**: July 3, 2026

---

## ✨ New Features

### 1. Added dynamic button lists: `type: repeat`

`Bottom.multi.buttons` now supports `type: repeat`, allowing KaMenu to generate real Paper Dialog buttons from a dynamic data source. This solves unknown item counts for player lists, warp lists, friend lists, mail lists, and similar menus.

```yaml
Bottom:
  type: multi
  columns: 2
  buttons:
    warp_list:
      type: repeat
      source: "[getWarpList]"
      page_size: 8
      item:
        text: "&a{item.name}"
        tooltip:
          - "&7World: &f{item.world}"
          - "&7Location: &f{item.x}, {item.y}, {item.z}"
        actions:
          - "actions: teleport_warp,{item.id}"
      empty:
        text: "&7No warps"
        actions:
          - "toast: type=task;msg=No data;icon=barrier"
```

The generated entries are native `ActionButton`s, so they keep width, tooltip, actions, conditions, and matrix layout support.

### 2. Multiple repeat source formats

`source` supports:

- `[functionName]`: call a menu `JavaScript` function that returns a JSON array
- JSON array strings, such as `[{"id":"home","name":"Home"}]`
- Resolved variables, such as `{data:warp_list}`, `{gdata:server_list}`, or PlaceholderAPI variables
- Multiline text, where each line becomes one item
- Simple string lists with a custom `split` separator

Simple string list example:

```yaml
Bottom:
  type: multi
  buttons:
    player_list:
      type: repeat
      source: "%kamenu_online_players%"
      split: ","
      trim: true
      item:
        text: "&a{item.value}"
        actions:
          - "tell: You clicked {item.value}"
```

If the variable returns `player1, player2, player3`, KaMenu generates 3 buttons.

### 3. Added repeat item variables

Repeat item templates can use:

- `{item.xxx}`: current object field
- `{item.value}`: value for string or number items
- `{item.index}`: full-list index starting at 0
- `{item.number}`: full-list number starting at 1
- `{item.page_index}`: current-page index starting at 0
- `{item.page_number}`: current-page number starting at 1

### 4. Added pagination state and `page:` actions

KaMenu stores pagination state per player, menu context, and list ID. Available pagination variables:

- `{page:listId}`: current page
- `{pages:listId}`: total pages
- `{total:listId}`: total item count
- `{start:listId}`: current page start index
- `{end:listId}`: current page end index

Pagination actions:

```yaml
- "page: warp_list next"
- "page: warp_list prev"
- "page: warp_list 1"
- "page: warp_list +1"
- "page: warp_list -1"
```

The `page:` action only changes pagination state. It does not refresh the dialog by itself. Usually follow it with `reset`, `open`, or `force-open`.

### 5. Added built-in list data: `list` / `glist`

KaMenu now supports persistent list data for friend lists, warp lists, favorites, server lists, and other dynamic data.

```yaml
- 'list: type=set;key=friends;var=`Steve,Alex`;split=,'
- 'list: type=add;key=friends;var=`Notch`'
- 'list: type=add;key=history;var=`Notch`;unique=false'
- 'list: type=remove;key=friends;var=`Alex`'
- 'glist: type=set;key=servers;var=`survival,skyblock`;split=,'
```

Read variables:

- `{list:key}`: read the current player's list as a JSON array string
- `{glist:key}`: read the global list as a JSON array string
- `%kamenu_list_key%` / `%kamenu_glist_key%`: read list JSON through PlaceholderAPI

List variables can be used directly as dynamic button `repeat.source`:

```yaml
source: "{list:friends}"
```

---

## 🔧 Improvements

### 1. Improved action variable resolution

Action-list arguments and non-item template variables can still use `$(key)`. Repeat item fields use `{item.xxx}` and can be used directly in button text, tooltip, and actions.

### 2. External in-memory menu pagination compatibility

Repeat pagination state follows the current menu contextId. In-memory menus opened through `KaMenuAPI.openYaml` / `openConfig` can use repeat lists and `page:` actions correctly.

### 3. Periodic task action context inheritance

`Events.Tasks` and `on_end` actions now inherit the menu contextId, preventing context-sensitive actions such as `page:` from writing to the wrong state.

### 4. Pagination state cleanup

Dynamic list pagination state is cleared when a player leaves the server or when the plugin disables, preventing stale state accumulation on long-running servers.

### 5. `/km action` test command completion

`/km action` now uses the full action-chain execution entry and includes Tab completion for server-side action prefixes such as `chat:`, `server:`, `stock-item:`, `item:`, `js:`, `actions:`, `page:`, `run-task:`, `stop-task:`, `wait:`, and `return`.

### 6. Added `inList` / `inGlist` condition methods

The condition system now supports list membership checks:

```yaml
condition: 'inGlist.%player_name%;{glist:vip_players}'
condition: '!inList.$(name);Steve,Alex,Notch'
```

The list argument supports JSON arrays returned by `{list:key}` / `{glist:key}`, JSON string arrays, and simple comma-separated string lists. Matching is exact and case-insensitive by default.

### 7. Added online player list PAPI variable

Added `%kamenu_online_players%`, which returns current online player names as a JSON array and can be used directly by dynamic button lists:

```yaml
source: "%kamenu_online_players%"
```

### 8. Added JavaScript `papi()` helper

JavaScript can now resolve PlaceholderAPI variables directly:

```javascript
var level = parseInt(papi("player_level") || "0");
var onlinePlayers = JSON.parse(papi("kamenu_online_players"));
var targetLevel = papi("%player_level%", getPlayer("Steve"));
```

Both `%...%` and plain placeholder names are supported. Returns an empty string if PlaceholderAPI is not installed or parsing fails.

### 9. Added JavaScript KaMenu variable helpers

JavaScript can now read KaMenu internal variables directly:

```javascript
var coins = parseInt(data("coins") || "0");
var status = gdata("server_status");
var temp = meta("temporary_choice");
var friends = JSON.parse(list("friends"));
var servers = JSON.parse(glist("servers"));
var raw = kvar("{gdata:server_status}");
```

Supports the generic `kvar(...)` resolver and `data/gdata/meta/list/glist` convenience helpers. `list` and `glist` return JSON array strings.

### 10. Added list size PAPI variables

Added two PlaceholderAPI variables for reading list item counts:

- `%kamenu_list_size_key%`: item count of the specified current-player list
- `%kamenu_glist_size_key%`: item count of the specified global list

They can be used in conditions:

```yaml
condition: "%kamenu_list_size_friends% > 0"
condition: "%kamenu_glist_size_servers% >= 3"
```

### 11. Added the first-time getting started guide

Added `/km guide` / `/kamenu guide` to open the built-in getting started guide menu. The guide is loaded from inside the plugin jar into memory and is not written to the `menus` directory, making it suitable for first-time server setup.

The guide supports:

- Language selection
- Releasing examples for the selected language
- Viewing example menu descriptions
- Opening example menus through clickable text

When no menus are loaded and an OP player joins the server, KaMenu sends a clickable prompt that opens the guide.

### 12. Added language and example release commands

Added language setup commands:

```bash
/km language zh_CN
/km language en_US
```

The command writes the selected language to `config.yml` and reloads language files, configuration, and menus.

Added example release commands:

```bash
/km examples
/km examples zh_CN
/km examples en_US
/km examples zh_CN overwrite
```

Examples are released by language to `plugins/KaMenu/menus/example/`. Chinese and English examples use the same target directory, so users no longer need to handle an `exampleEN` runtime directory manually. Existing files are skipped by default; add `overwrite` to replace them.

---

## 📝 Documentation and Examples

- Added example menu: `example/repeat_buttons_demo.yml`
- Added player interaction example menu: `example/inspect_player.yml` for Shift+right-click equipment display and interaction actions
- Added built-in getting started guide menu: `internal/guide.yml`
- Added Chinese/English example release flow, with examples released to `menus/example/` by language
- Updated Chinese docs: `docs/menu/bottom.md`
- Updated English docs: `docs-en/menu/bottom.md`
- Updated quick start and command docs for `/kamenu guide`, `/km language`, and `/km examples`
- Updated the KaMenu menu-authoring skill reference

---

## 📝 Compatibility Notes

- Existing menus do not need syntax changes
- Menus that do not use `type: repeat` behave as before
- `repeat` currently works under `Bottom.type: multi` / `buttons`
- Pagination variables are currently intended for normal `Bottom.multi.buttons` and repeat item templates
- `page:` does not refresh the menu automatically; use it with `reset`, `open`, or `force-open`
- The guide menu is not released to the `menus` directory. Use the guide or `/km examples` when sample menus are needed.
- The default `listeners.player-click.menu` is now `example/inspect_player`; old default `main_menu` / `inspect_player` listener references are migrated to `example/...` during config update.

---

## 🚀 Upgrade Guide

### Upgrading from 1.5.2 to 1.5.3

1. Replace the plugin jar with `KaMenu-1.5.3.jar`
2. Fully restart the server
3. Optionally migrate dynamic-list menus to `Bottom.multi.buttons.type: repeat`
4. Optionally run `/kamenu guide` for first-time setup, or `/km examples [zh_CN|en_US]` to release example menus
5. Optionally use `example/repeat_buttons_demo.yml` as a starting point for player, warp, or friend lists
