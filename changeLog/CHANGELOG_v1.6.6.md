# KaMenu v1.6.6 更新报告

## 版本信息
- **版本号**: 1.6.6
- **发布日期**: 2026年7月22日

---

## 资源包插件字形兼容

### ItemsAdder

- 动态 Dialog 文本现在会主动调用 ItemsAdder API 解析 `:font_image:`
- 支持 ItemsAdder `:offset_N:` 像素偏移符
- 解决 ItemsAdder 内部占位符在 Paper Dialog 中原文显示的问题

### Oraxen

- 支持 `<glyph:id>` 与短写 `<g:id>`
- 同一 Oraxen 原始文本行支持 `<shift:N>` 像素偏移，可点击文本拆分后仍保持解析上下文
- 使用当前玩家的 Oraxen Resolver，保留字形字体、颜色、阴影和权限检查
- 只在文本出现 Oraxen glyph 标签时启用 Oraxen shift，避免与 CraftEngine 标签冲突

### CraftEngine

- 兼容 `<image:namespace:id>` 与 `<shift:N>`
- 字形继续由 CraftEngine 的 Dialog 数据包拦截器处理，KaMenu 不重复转换
- 使用时需保持 CraftEngine `network.intercept-packets.dialog: true`

固定 Unicode 字符仍可由客户端直接读取，不需要经过插件占位符解析。

---

## 三插件自定义物品兼容

新增统一的软依赖物品适配层，支持以下显式 ID：

| 插件 | 完整写法 | 短写 |
|------|----------|------|
| ItemsAdder | `itemsadder:namespace:item` | `ia:namespace:item` |
| Oraxen | `oraxen:item_id` | — |
| CraftEngine | `craftengine:namespace:item` | `ce:namespace:item` |

这些 ID 可用于：

- 动态菜单 `Body.item.material` 物品展示组件
- 可点击文本 `hover_item` 物品悬浮信息
- `item: type=give/take` 给予和扣除动作
- `hasItem` 条件与 `%kamenu_hasitem_...%` 数量变量
- `checkitem` 物品模板来源
- `listeners.item-lore.*.material` 右键物品监听

外部物品的扣除和数量检查按插件物品 ID 匹配，不会误匹配使用相同基础材质的其他物品。

`checkitem` 新增属性：

- `custom_id` / `external_id` / `item_id`：返回带提供方前缀的规范 ID
- `plugin_id` / `native_id`：返回插件自身物品 ID
- `plugin` / `provider`：返回 `ItemsAdder`、`Oraxen` 或 `CraftEngine`

---

## 可点击文本点击行为

- `Body.message` 和 `hovertext:` 动作中的 `<text=...>` 新增 `copy=文本`，点击后由客户端复制到剪贴板
- 可点击文本支持四种互斥点击行为：`actions` 动作包 / Click、`copy` 复制文本、`command` 玩家指令、`url` 链接
- 新配置应只选择一种点击行为；KaMenu UI Studio 的可点击文本编辑器使用下拉框强制四选一
- 为兼容旧配置，同时出现多个点击属性时按 `actions > copy > command > url` 选择首个有效行为

示例：

```yaml
Body:
  copy_code:
    type: message
    text: '<text=&e[复制口令];hover=&7点击复制;copy=KAMENU-2026>'
```

---

## 兼容说明

- ItemsAdder、Oraxen、CraftEngine 均为软依赖，缺失时 KaMenu 可正常启动
- 现有原版材质、固定 Unicode、保存物品和自定义模型写法保持兼容
- 三插件 API 通过隔离适配器调用，API 不兼容时降级为未解析，不中断菜单主流程
- 配置文件版本保持 `5`，无需迁移现有 `config.yml`
- 安装、更新或移除资源包插件后建议完整重启服务器，使软依赖加载顺序生效

---

## 升级指南

### 从 1.6.5 升级到 1.6.6

1. 替换插件 jar 为 `KaMenu-1.6.6.jar`
2. 确认需要使用的资源包插件已经正常加载
3. 使用 CraftEngine 字形时确认 `network.intercept-packets.dialog: true`
4. 完整重启服务器

---

# KaMenu v1.6.6 Update Report

## Version Info
- **Version**: 1.6.6
- **Release Date**: July 22, 2026

---

## Resource-Pack Glyph Compatibility

### ItemsAdder

- Dynamic Dialog text now calls the ItemsAdder API to resolve `:font_image:`
- Supports the ItemsAdder `:offset_N:` pixel-shift syntax
- Fixes internal placeholders appearing literally in Paper Dialogs

### Oraxen

- Supports `<glyph:id>` and its `<g:id>` alias
- Supports `<shift:N>` across the same original Oraxen text line, retaining resolver context after clickable-text splitting
- Uses Oraxen's player-aware resolver to preserve fonts, colors, shadows, and permission checks
- Activates Oraxen shift parsing only when an Oraxen glyph tag is present, avoiding conflicts with CraftEngine tags

### CraftEngine

- Compatible with `<image:namespace:id>` and `<shift:N>`
- Glyphs remain handled by CraftEngine's Dialog packet interceptor; KaMenu does not convert them twice
- Requires CraftEngine `network.intercept-packets.dialog: true`

Fixed Unicode characters still render directly on the client without server-side placeholder parsing.

---

## Custom Items from All Three Plugins

A shared soft-dependency adapter now accepts these explicit IDs:

| Plugin | Full form | Alias |
|--------|-----------|-------|
| ItemsAdder | `itemsadder:namespace:item` | `ia:namespace:item` |
| Oraxen | `oraxen:item_id` | — |
| CraftEngine | `craftengine:namespace:item` | `ce:namespace:item` |

These IDs work in:

- Dynamic-menu `Body.item.material` item components
- Clickable-text `hover_item` item tooltips
- `item: type=give/take` actions
- `hasItem` conditions and `%kamenu_hasitem_...%` counters
- `checkitem` template sources
- `listeners.item-lore.*.material` interaction listeners

External-item removal and counting match the plugin item ID, so unrelated items sharing the same base material are not selected.

New `checkitem` properties:

- `custom_id` / `external_id` / `item_id`: canonical ID with the provider prefix
- `plugin_id` / `native_id`: native plugin item ID
- `plugin` / `provider`: `ItemsAdder`, `Oraxen`, or `CraftEngine`

---

## Clickable-Text Click Behaviors

- `<text=...>` segments in `Body.message` and `hovertext:` actions now support `copy=text`, which copies text to the client clipboard
- Clickable text provides four mutually exclusive click behaviors: `actions` package / Click, `copy` text, player `command`, or `url`
- New configurations should select only one click behavior; the KaMenu UI Studio clickable-text editor enforces this with a dropdown
- For compatibility, legacy tags containing multiple click properties resolve the first valid behavior in this order: `actions > copy > command > url`

Example:

```yaml
Body:
  copy_code:
    type: message
    text: '<text=&e[Copy Code];hover=&7Click to copy;copy=KAMENU-2026>'
```

---

## Compatibility Notes

- ItemsAdder, Oraxen, and CraftEngine are soft dependencies; KaMenu starts without them
- Existing vanilla materials, fixed Unicode, saved items, and custom-model configurations remain compatible
- Plugin APIs are isolated behind adapters; incompatible APIs fall back to unresolved content without interrupting the menu flow
- The config version remains `5`; existing `config.yml` files require no migration
- Fully restart the server after installing, updating, or removing a resource-pack plugin so soft-dependency ordering is applied

---

## Upgrade Guide

### Upgrading from 1.6.5 to 1.6.6

1. Replace the plugin jar with `KaMenu-1.6.6.jar`
2. Confirm that each required resource-pack plugin loads successfully
3. When using CraftEngine glyphs, verify `network.intercept-packets.dialog: true`
4. Fully restart the server
