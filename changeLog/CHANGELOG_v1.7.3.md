# KaMenu v1.7.3 更新日志

## 版本信息

- **版本号**: 1.7.3
- **发布日期**: 2026年8月1日

---

## 中文

### 菜单依赖检查顺序修复

- `Settings.need_placeholder` 现在会在 `Events.Open` 之前检查。
- 缺少必需的 PlaceholderAPI 扩展时，菜单不会再提前执行 Open 事件中的数据写入、扣款、物品或指令动作。
- Paper、Spigot 和 Floodgate 表单入口使用一致的检查顺序；强制重开路径仍保留依赖检查作为安全兜底。

### Paper Dialog 解析统一

- Paper Dialog 改为使用与 Spigot、Floodgate 相同的共享菜单编译器，不再由平台入口单独解析 YAML。
- `Title`、`Settings`、`Body`、`Inputs`、`Bottom`、条件、变量、repeat、分页、输入清理和按钮定义先编译为统一模型，再由 Paper 原生渲染器显示。
- 修复 `Body.item.material` 未识别 `stock:<保存物品名>` 的问题；现在会读取 KaMenu 保存物品库中的完整 ItemStack，并按 `amount` 配置覆盖数量。
- Paper 按钮继续使用原生一次性 callback，并保留 actions、输入捕获、客户端 URL/复制动作、`Settings.lifetime`、菜单任务和生命周期行为。
- 非法 Dialog 参数仍会阻止菜单打开并保留底层异常，便于定位错误配置，现有菜单格式无需修改。

---

## English

## Version Information

- **Version**: 1.7.3
- **Release Date**: August 1, 2026

---

### Menu Dependency Check Ordering

- `Settings.need_placeholder` is now checked before `Events.Open` runs.
- When a required PlaceholderAPI expansion is missing, Open-event data writes, charges, item operations, and command actions no longer execute before the menu is rejected.
- Paper, Spigot, and Floodgate form entry points now use the same ordering; force-open paths retain a dependency check as a safety fallback.

### Unified Paper Dialog Compilation

- Paper Dialogs now use the same shared menu compiler as Spigot and Floodgate instead of parsing YAML independently in the platform entry point.
- `Title`, `Settings`, `Body`, `Inputs`, `Bottom`, conditions, variables, repeat lists, pagination, input cleanup, and button definitions are compiled into one platform-neutral model before Paper renders native Dialog objects.
- Fixed `Body.item.material` not recognizing `stock:<saved-item-name>`; it now reads the complete ItemStack from KaMenu's saved-item library and applies the configured `amount`.
- Paper buttons continue to use native one-use callbacks while preserving actions, input capture, client-side URL/copy actions, `Settings.lifetime`, menu tasks, and lifecycle behavior.
- Invalid Dialog parameters still prevent the menu from opening and retain the underlying exception for configuration diagnostics. Existing menu syntax requires no changes.

---

## Upgrade Guide

### Upgrading from 1.7.2 to 1.7.3

1. Replace the plugin JAR with `KaMenu-1.7.3.jar`.
2. Fully restart the server.
3. Existing menus and configuration files require no migration.
