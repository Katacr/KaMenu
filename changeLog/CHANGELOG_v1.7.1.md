# KaMenu v1.7.1 更新日志

## 版本信息

- **版本号**: 1.7.1
- **发布日期**: 2026年7月29日

---

## 中文

### Folia 关服修复

- 修复 Folia 核心关闭服务器时调用 Bukkit 全局任务取消接口导致的 `UnsupportedOperationException`。
- 关闭插件时改为使用 KaMenu 自身的调度任务清理逻辑，避免触发 Folia 不支持的全局调度器操作。

### repeat 动态按钮矩阵补齐

- `type: repeat` 会按当前页实际渲染的动态按钮数量和 `columns` 自动补齐矩阵。
- 当按钮数量无法被 `columns` 整除时，追加空白按钮，使动态按钮区域保持完整对齐。
- 空白按钮没有显示文本，不执行 `item.actions` 或其他业务动作，点击后仅执行 `reset`。
- 如果 `item.width` 已配置，空白按钮会复用该宽度，保持整个动态按钮矩阵对齐。
- 上一页、下一页等普通分页按钮不计入动态按钮补齐数量，仍会在补位按钮后按配置顺序追加。
- Paper 与 Spigot 的 Dialog 渲染路径均保持一致。

---

## English

## Version Information

- **Version**: 1.7.1
- **Release Date**: July 29, 2026

---

### Folia Shutdown Fix

- Fixed an `UnsupportedOperationException` during Folia shutdown when Bukkit's global task-cancellation API was called.
- Plugin shutdown now uses KaMenu's own scheduler cleanup path and avoids unsupported global scheduler operations on Folia.

### `repeat` Grid Padding

- `type: repeat` now pads each page according to the number of successfully rendered dynamic buttons and `columns`.
- When the dynamic button count is not divisible by `columns`, empty buttons are appended to keep the repeat area aligned.
- Padding buttons have no visible text and do not execute `item.actions` or other business actions; clicking one only executes `reset`.
- When `item.width` is configured, padding buttons reuse that width to keep the dynamic button grid aligned.
- Previous, next, and other regular pagination buttons are excluded from the repeat count and remain appended after the padding buttons in configuration order.
- Paper and Spigot Dialog rendering paths now use the same behavior.

---

## Upgrade Guide

### Upgrading from 1.7.0 to 1.7.1

1. Replace the plugin JAR with `KaMenu-1.7.1.jar`.
2. Fully restart the server to apply the Folia scheduler shutdown fix.
3. Existing `repeat` menus require no configuration changes.
