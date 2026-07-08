# KaMenu v1.6.1 更新报告

## 📋 版本信息
- **版本号**: 1.6.1
- **发布日期**: 2026年7月8日

---

## 🐛 问题修复

### 1. 修复 JavaScript 延迟调度错误

修复 JavaScript 内置辅助方法在调用 Bukkit 延迟任务时可能出现的参数类型不匹配问题：

```text
NoSuchMethodException:
CraftScheduler.runTaskLater(...)
```

该问题可能在 JavaScript 包或菜单内脚本使用延迟执行逻辑时触发。现在延迟 tick 参数会正确转换为 Bukkit 调度器需要的 `Long` 类型，并将 JavaScript 回调包装为 Java `Runnable`，避免 Nashorn 对方法重载解析失败。

同时更新了 JavaScript 文档中的延迟执行示例，避免继续推荐不适合当前独立执行上下文的旧写法。

### 2. 修复自定义指令重载后 TAB 补全不立即刷新

修复新增、删除或修改 `custom-commands` 后，在线玩家需要退出重进服务器才能获得最新 TAB 补全的问题。

现在执行以下重载后，会主动刷新所有在线玩家的客户端命令树：

- `/kamenu reload config`
- `/kamenu reload all`

这样自定义指令重新注册完成后，在线玩家可以立刻使用新的命令补全。

### 3. 支持播放资源包自定义音效

`sound:` 动作现在会在原版声音 Registry 查找失败时，自动使用字符串 sound key 转发给客户端播放。

这意味着可以直接播放资源包 `sounds.json` 中定义的自定义声音，例如：

```yaml
- 'sound: mypack:ui.click;volume=1.0;pitch=1.0;category=ui'
```

同时修复了原版声音 ID 使用下划线风格时的兼容问题，例如 `ENTITY_PLAYER_LEVELUP` 会尝试转换为 `entity.player.levelup`。

---

## 📝 兼容说明

- 菜单语法无需修改
- 配置文件版本无需升级
- 现有 JavaScript 包、菜单内 JavaScript、自定义指令配置、原版 `sound:` 动作保持兼容
- 本次为修复版本，建议已经使用 1.6.0 的服务器升级

---

## 🚀 升级指南

### 从 1.6.0 升级到 1.6.1

1. 替换插件 jar 文件为 `KaMenu-1.6.1.jar`
2. 完整重启服务器
3. 如需测试自定义指令补全，可修改 `custom-commands` 后执行 `/kamenu reload config`

---

# KaMenu v1.6.1 Update Report

## 📋 Version Info
- **Version**: 1.6.1
- **Release Date**: July 8, 2026

---

## 🐛 Bug Fixes

### 1. Fixed JavaScript delayed scheduler errors

Fixed an argument type mismatch that could occur when JavaScript helper methods scheduled delayed Bukkit tasks:

```text
NoSuchMethodException:
CraftScheduler.runTaskLater(...)
```

This could happen when JavaScript packages or menu scripts used delayed execution. The delay tick value is now converted to the `Long` type required by the Bukkit scheduler, and the JavaScript callback is wrapped as a Java `Runnable`, avoiding Nashorn overload resolution failures.

The JavaScript documentation delayed execution example has also been updated to avoid recommending the old pattern that does not fit the current isolated execution context.

### 2. Fixed TAB completion not refreshing after custom command reload

Fixed an issue where online players had to relog before seeing updated TAB completions after adding, removing, or changing `custom-commands`.

The client command tree is now refreshed for all online players after:

- `/kamenu reload config`
- `/kamenu reload all`

This allows newly registered custom commands to appear in TAB completion immediately after reload.

### 3. Added support for resource-pack custom sounds

The `sound:` action now falls back to the string sound-key API when the sound is not found in the vanilla sound registry.

This allows menus to play custom sounds defined in a resource pack `sounds.json`, for example:

```yaml
- 'sound: mypack:ui.click;volume=1.0;pitch=1.0;category=ui'
```

This also improves compatibility for vanilla sound IDs written in underscore style, such as converting `ENTITY_PLAYER_LEVELUP` to `entity.player.levelup`.

---

## 📝 Compatibility Notes

- No menu syntax changes are required
- No config version upgrade is required
- Existing JavaScript packages, menu JavaScript, custom command configurations, and vanilla `sound:` actions remain compatible
- This is a bug fix release recommended for servers already using 1.6.0

---

## 🚀 Upgrade Guide

### Upgrading from 1.6.0 to 1.6.1

1. Replace the plugin jar with `KaMenu-1.6.1.jar`
2. Fully restart the server
3. To test custom command completion, edit `custom-commands` and run `/kamenu reload config`
