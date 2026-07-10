# KaMenu v1.6.3 更新报告

## 📋 版本信息
- **版本号**: 1.6.3
- **发布日期**: 2026年7月11日

---

## ✨ 新增功能

### 1. 新增菜单存在时间上限

`Settings` 新增 `lifetime`，用于限制 Dialog 菜单的最大存在时间，单位为秒，默认值为 `300`（5 分钟）：

```yaml
Settings:
  lifetime: 300
```

该值同时控制 Paper callback 的有效期和 KaMenu 服务端的菜单关闭定时器。

达到时间上限后，KaMenu 会：

- 确认玩家仍处于原菜单会话，避免旧定时器关闭后来打开的新菜单
- 主动关闭已超时的 Dialog
- 停止该菜单的 `Events.Tasks`
- 清理 repeat 分页状态
- 执行 `Events.Close`，用于完成菜单清理逻辑

超时属于硬性上限，`Events.Close` 中的 `return` 不会阻止菜单关闭。`lifetime` 小于或等于 `0` 时会回退到默认的 300 秒。

`reset` 或打开其它菜单会替换原菜单会话并重新计时；旧菜单的超时任务不会误关新菜单。

### 2. callback 固定保持一次性

普通按钮和正文可点击文本的服务端 callback 均明确固定为一次性，不提供 `Settings.uses` 配置。

当使用 `after_action: NONE` 或 `WAIT_FOR_RESPONSE` 时，每个按钮的每条动作分支仍必须最终执行以下动作之一：

- `close` / `force-close`
- `reset`
- `open` / `force-open`

否则该次 callback 执行后，客户端可能保留一个无法继续向服务器发起回调的旧 Dialog。`lifetime` 会在超时后关闭该界面，但它只是一项兜底措施，不能代替正常的关闭或刷新动作。

---

## 🐛 问题修复

### 1. 修复菜单内 JavaScript 包被错误识别为全局包

修复菜单 UI 字段和条件化配置在解析 `{js:[name]}` 时丢失当前菜单配置的问题。

例如菜单已经定义：

```yaml
JavaScript:
  player_name: |
    player.getName();

Body:
  info:
    type: message
    text: '&a{js:[player_name]}'
```

旧版本可能跳过菜单内的 `JavaScript.player_name`，错误地只检查 `plugins/KaMenu/js/player_name.js`，并输出类似警告：

```text
JavaScript package 'player_name' not found (checked global js/player_name.js)
```

现在条件值解析器会保留当前 `YamlConfiguration` 上下文，恢复正确的查找顺序：

1. 当前菜单的 `JavaScript.<name>`
2. 全局 JavaScript 包 `plugins/KaMenu/js/<name>.js`

修复范围包括：

- `Title` 条件值
- `Body` 文本和物品描述
- `Inputs` 文本与默认值
- `Bottom` 按钮文本和 tooltip
- 条件化字符串、字符串列表和类型字段
- 条件值内部使用的菜单 JavaScript 包

现有 `{js:[name]}` 语法无需修改。

### 2. 保持条件值解析 API 二进制兼容

条件值解析接口保留原有三参数 JVM 方法，同时新增携带菜单配置的上下文重载，避免外部已编译调用者因方法签名变化出现 `NoSuchMethodError`。

---

## 📝 文档更新

- 更新中文和英文 `Settings` 文档，新增 `lifetime` 的默认值、单位和超时行为
- 补充 callback 一次性行为说明
- 补充 `after_action: NONE` 与 `WAIT_FOR_RESPONSE` 下必须显式关闭、刷新或打开菜单的要求
- 明确 `lifetime` 仅用于超时兜底

---

## 📝 兼容说明

- 现有菜单 YAML 无需修改
- 未配置 `Settings.lifetime` 的菜单默认使用 300 秒
- 不支持 `Settings.uses`，callback 始终保持一次性
- 菜单内 JavaScript 包继续优先于同名全局 JavaScript 包
- Paper 与 Folia 继续使用同一个插件 jar
- 配置文件版本无需升级

---

## 🚀 升级指南

### 从 1.6.2 升级到 1.6.3

1. 替换插件 jar 文件为 `KaMenu-1.6.3.jar`
2. 完整重启服务器
3. 如需调整菜单超时时间，在菜单的 `Settings` 中添加 `lifetime`
4. 检查使用 `after_action: NONE` 或 `WAIT_FOR_RESPONSE` 的菜单，确保每条按钮路径最终会关闭、刷新或打开菜单

---

# KaMenu v1.6.3 Update Report

## 📋 Version Info
- **Version**: 1.6.3
- **Release Date**: July 11, 2026

---

## ✨ New Features

### 1. Added a menu lifetime limit

`Settings` now supports `lifetime`, which limits how long a Dialog may remain open. The value is measured in seconds and defaults to `300` (5 minutes):

```yaml
Settings:
  lifetime: 300
```

This value controls both the Paper callback lifetime and KaMenu's server-side menu timeout.

When the limit is reached, KaMenu:

- Confirms that the player is still in the original menu session, preventing an old timer from closing a newer menu
- Actively closes the expired Dialog
- Stops the menu's `Events.Tasks`
- Clears repeat pagination state
- Runs `Events.Close` for menu cleanup

The timeout is a hard limit. A `return` inside `Events.Close` cannot prevent the Dialog from closing. Values less than or equal to `0` fall back to the default 300 seconds.

Resetting or opening another menu replaces the original session and restarts the timer. An old menu timeout cannot close the newer menu.

### 2. Callbacks remain strictly single-use

Server callbacks for regular buttons and clickable body text are explicitly fixed to one use. KaMenu does not provide a `Settings.uses` option.

When using `after_action: NONE` or `WAIT_FOR_RESPONSE`, every action branch must still end with one of:

- `close` / `force-close`
- `reset`
- `open` / `force-open`

Otherwise, after the callback runs, the client may retain an old Dialog that can no longer send callbacks to the server. `lifetime` eventually closes that screen, but it is only a fallback and does not replace proper close or refresh actions.

---

## 🐛 Bug Fixes

### 1. Fixed menu JavaScript packages being mistaken for global packages

Fixed an issue where menu UI fields and conditional values lost the current menu configuration while resolving `{js:[name]}`.

For example:

```yaml
JavaScript:
  player_name: |
    player.getName();

Body:
  info:
    type: message
    text: '&a{js:[player_name]}'
```

Previous versions could skip `JavaScript.player_name`, check only `plugins/KaMenu/js/player_name.js`, and log:

```text
JavaScript package 'player_name' not found (checked global js/player_name.js)
```

The conditional value resolver now retains the current `YamlConfiguration`, restoring the intended lookup order:

1. `JavaScript.<name>` in the current menu
2. Global package `plugins/KaMenu/js/<name>.js`

The fix covers:

- Conditional `Title` values
- `Body` text and item descriptions
- `Inputs` labels and defaults
- `Bottom` button text and tooltips
- Conditional strings, string lists, and type fields
- Menu JavaScript packages used inside conditional values

Existing `{js:[name]}` syntax requires no changes.

### 2. Preserved binary compatibility for conditional value APIs

The original three-argument JVM methods remain available. New context-aware overloads carry the menu configuration without causing `NoSuchMethodError` for already compiled external callers.

---

## 📝 Documentation

- Updated the Chinese and English `Settings` documentation with the `lifetime` default, unit, and timeout behavior
- Documented the single-use callback behavior
- Documented the requirement to explicitly close, reset, or open a menu when using `after_action: NONE` or `WAIT_FOR_RESPONSE`
- Clarified that `lifetime` is only a timeout fallback

---

## 📝 Compatibility Notes

- Existing menu YAML files require no changes
- Menus without `Settings.lifetime` use the default 300 seconds
- `Settings.uses` is not supported; callbacks always remain single-use
- Menu JavaScript packages continue to take priority over global packages with the same name
- Paper and Folia continue to use the same plugin jar
- No config version upgrade is required

---

## 🚀 Upgrade Guide

### Upgrading from 1.6.2 to 1.6.3

1. Replace the plugin jar with `KaMenu-1.6.3.jar`
2. Fully restart the server
3. Add `lifetime` under a menu's `Settings` only when a custom timeout is needed
4. Review menus using `after_action: NONE` or `WAIT_FOR_RESPONSE` and ensure every button path closes, resets, or opens a menu
