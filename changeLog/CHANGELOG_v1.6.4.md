# KaMenu v1.6.4 更新报告

## 📋 版本信息
- **版本号**: 1.6.4
- **发布日期**: 2026年7月19日

---

## ✨ 新增功能

### 1. 可点击文本支持 ItemStack 悬浮

可点击文本新增 `hover_item` 参数，鼠标悬停时可以展示完整 ItemStack，包括名称、Lore、附魔、模型和其他数据组件：

```yaml
text:
  - '当前武器：<text="&6[主手]";hover_item=hand>'
  - '奖励预览：<text="&d[神奇之剑]";hover_item="stock:神奇之剑">'
```

支持以下来源：

- `hand`、`offhand`
- `slot:<槽位>`
- `armor:helmet`、`armor:chestplate`、`armor:leggings`、`armor:boots`
- `stock:<保存物品名>`
- `material:<材质ID>`

有效的 `hover_item` 优先于普通 `hover`；物品不存在时仍可使用 `hover` 文字作为回退。

### 2. 新增 `checkitem` 物品属性变量

新增统一的物品属性查询能力。菜单内部使用大括号变量，其他插件通过 PlaceholderAPI 调用：

```text
{checkitem:[hand;name]}
%kamenu_checkitem_[hand;name]%
```

支持的物品来源：

- `hand`：主手物品
- `offhand`：副手物品
- `slot:<索引>`：玩家背包槽位
- `stock:<名称>`：KaMenu 保存物品

支持的属性：

- 基础属性：`type`、`amt`、`name`
- Lore：`lore`、`lore:<行号>`；行号从 `1` 开始
- 附魔：`enchants`、`ench:<附魔ID>`
- 模型：`model` / `item_model`
- 自定义模型 ID：`cmd` / `custom_model_data` / `custom_model_id`，默认按整数输出
- 耐久：`dmg`、`dura`、`dura_pct`

`name` 和 Lore 默认输出纯文本，也可使用第三个参数 `fmt:legacy` 或 `fmt:mini`。`fmt` 仅影响名称和 Lore 文本。`lore` 返回 JSON 字符串数组，`enchants` 返回包含 `key` 和 `level` 的 JSON 对象数组，可直接作为 repeat 数据源。

原版附魔 ID 可省略 `minecraft:`；自定义附魔必须提供完整命名空间。

保存物品名包含分号时，使用反引号包裹内部名称，例如 ``{checkitem:[stock:`活动;长剑`;name]}``，避免与 YAML 外层引号冲突。

### 3. `hasItem` 支持自定义模型 ID

原有 `hasItem` 条件和 `%kamenu_hasitem_[...]%` 数量变量现在可以分别判断 ItemModel 与整数 CustomModelData：

```yaml
condition: 'hasItem.[mats=PAPER;amount=1;model=custom:magic_scroll]'
condition: 'hasItem.[mats=PAPER;amount=1;custom_model_id=10001]'
```

- `model` / `item_model`：匹配 NamespacedKey 类型的 ItemModel
- `cmd` / `custom_model_data` / `custom_model_id`：匹配整数 CustomModelData

两类模型条件可以同时填写，此时物品必须同时满足。`hasItem` 与 `checkitem` 现已共用模型属性读取逻辑，避免两套功能对同一物品给出不同结果。同时修复主手快捷栏物品可能被重复计数的问题。

### 4. 保存物品改用内存缓存

保存物品在插件启动时集中载入线程安全内存缓存，保存或删除物品后立即同步缓存。

以下高频操作不再同步查询 SQL：

- `hover_item=stock:*`
- `{checkitem:[stock:*;属性]}`
- `%kamenu_checkitem_[stock:*;属性]%`
- 原有保存物品读取操作

玩家背包属性只在 Paper 主线程或 Folia 当前玩家区域线程读取。异步第三方 PAPI 请求不会阻塞切换线程；不安全线程中的玩家物品查询返回对应空值，`stock:` 查询不受此限制。

---

## 📝 文档更新

- 更新中英文 PlaceholderAPI、条件和 JavaScript 文档
- 补充 `hover_item` 完整来源和回退规则
- 补充 `checkitem` 来源、属性、JSON 返回结构和线程说明
- 同步更新 KaMenu 菜单编写 skill

---

## 📝 兼容说明

- 现有 `hasitem` 和 `hasstockitem` 变量保持兼容
- repeat 的 `{item.value}`、`{item.index}` 等项目变量不受影响
- `checkitem` 是 1.6.4 新增语法，不保留开发阶段的 `{item:[...]}` / `%kamenu_item_[...]%` 名称
- Paper 与 Folia 继续使用同一个插件 jar
- 配置文件版本无需升级

---

## 🚀 升级指南

### 从 1.6.3 升级到 1.6.4

1. 替换插件 jar 为 `KaMenu-1.6.4.jar`
2. 完整重启服务器
3. 新菜单统一使用 `{checkitem:[...]}` 或 `%kamenu_checkitem_[...]%`

---

# KaMenu v1.6.4 Update Report

## 📋 Version Info
- **Version**: 1.6.4
- **Release Date**: July 19, 2026

---

## ✨ New Features

### 1. ItemStack hovers for clickable text

Clickable text now supports `hover_item`, which displays a complete ItemStack including its name, lore, enchantments, model, and other data components:

```yaml
text:
  - 'Current weapon: <text="&6[Main hand]";hover_item=hand>'
  - 'Reward preview: <text="&d[Magic Sword]";hover_item="stock:Magic Sword">'
```

Supported sources:

- `hand`, `offhand`
- `slot:<index>`
- `armor:helmet`, `armor:chestplate`, `armor:leggings`, `armor:boots`
- `stock:<saved-item name>`
- `material:<material ID>`

A valid `hover_item` takes priority over plain `hover`. When the item is unavailable, the regular hover text remains available as fallback.

### 2. Added `checkitem` item-property variables

Item properties can now be queried through a built-in brace variable or PlaceholderAPI:

```text
{checkitem:[hand;name]}
%kamenu_checkitem_[hand;name]%
```

Supported item sources:

- `hand`: main-hand item
- `offhand`: off-hand item
- `slot:<index>`: player inventory slot
- `stock:<name>`: KaMenu saved item

Supported properties:

- Basics: `type`, `amt`, `name`
- Lore: `lore`, `lore:<line>`; line numbers start at `1`
- Enchantments: `enchants`, `ench:<enchantment ID>`
- Model: `model` / `item_model`
- Custom model ID: `cmd` / `custom_model_data` / `custom_model_id`, returned as an integer by default
- Durability: `dmg`, `dura`, `dura_pct`

Names and lore use plain text by default, with optional third arguments `fmt:legacy` and `fmt:mini`. `fmt` only affects name and lore text. `lore` returns a JSON string array, while `enchants` returns JSON objects containing `key` and `level`; both can be used directly as repeat sources.

Vanilla enchantment IDs may omit `minecraft:`. Custom enchantments require their full namespace.

Saved-item names containing semicolons use backticks around the inner name, such as ``{checkitem:[stock:`Event;Sword`;name]}``, avoiding conflicts with outer YAML quotes.

### 3. Custom model ID support in `hasItem`

The existing `hasItem` condition and `%kamenu_hasitem_[...]%` count placeholder can now check ItemModel and integer CustomModelData separately:

```yaml
condition: 'hasItem.[mats=PAPER;amount=1;model=custom:magic_scroll]'
condition: 'hasItem.[mats=PAPER;amount=1;custom_model_id=10001]'
```

- `model` / `item_model`: matches the NamespacedKey ItemModel
- `cmd` / `custom_model_data` / `custom_model_id`: matches integer CustomModelData

Both model conditions may be combined, in which case both must match. `hasItem` and `checkitem` now share the same model-property reader so they cannot interpret the same item differently. This also fixes main-hand hotbar items potentially being counted twice.

### 4. In-memory saved-item cache

Saved items are loaded into a thread-safe memory cache at startup. Save and delete operations update the cache immediately.

The following high-frequency operations no longer perform synchronous SQL queries:

- `hover_item=stock:*`
- `{checkitem:[stock:*;property]}`
- `%kamenu_checkitem_[stock:*;property]%`
- Existing saved-item read operations

Player inventory properties are read only on the Paper main thread or the player's current Folia region thread. Asynchronous third-party PAPI calls never block to switch threads; unsafe player-item queries return their empty value, while `stock:` queries remain available.

---

## 📝 Documentation

- Updated the Chinese and English PlaceholderAPI, condition, and JavaScript documentation
- Documented all `hover_item` sources and fallback behavior
- Documented `checkitem` sources, properties, JSON formats, and threading behavior
- Updated the KaMenu menu-authoring skill

---

## 📝 Compatibility Notes

- Existing `hasitem` and `hasstockitem` variables remain compatible
- Repeat variables such as `{item.value}` and `{item.index}` are unaffected
- `checkitem` is the final 1.6.4 syntax; the development-only `{item:[...]}` / `%kamenu_item_[...]%` names are not retained
- Paper and Folia continue to use the same plugin jar
- No config version upgrade is required

---

## 🚀 Upgrade Guide

### Upgrading from 1.6.3 to 1.6.4

1. Replace the plugin jar with `KaMenu-1.6.4.jar`
2. Fully restart the server
3. Use `{checkitem:[...]}` or `%kamenu_checkitem_[...]%` in new menus
