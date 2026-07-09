# KaMenu v1.6.2 更新报告

## 📋 版本信息
- **版本号**: 1.6.2
- **发布日期**: 2026年7月10日

---

## ⚡ Folia 支持

### 1. 支持 Folia 区域线程模型

KaMenu 现已声明并适配 Folia。插件会自动识别当前运行环境，在 Paper 与 Folia 上选择对应的调度方式：

- 玩家菜单与玩家相关动作使用玩家 EntityScheduler
- 控制台和全局任务使用 GlobalRegionScheduler
- 数据库、网络与其他异步任务使用 AsyncScheduler
- Paper 环境继续使用 BukkitScheduler，保持现有行为兼容

`plugin.yml` 已加入：

```yaml
folia-supported: true
```

同一个 KaMenu jar 可同时用于 Paper 与 Folia，无需下载不同版本。

### 2. 菜单生命周期与周期任务适配

统一的 Paper/Folia 调度层已覆盖：

- 菜单打开、强制打开、重置与关闭
- `Events.Open` 和 `Events.Close`
- `wait:` 延迟动作链
- `Events.Tasks`、`run-task`、`stop-task` 和任务结束动作
- 玩家进入后的向导提示与更新通知
- 自定义指令重载后的在线玩家 TAB 补全刷新

`Events.Tasks` 不再直接依赖 `BukkitTask`，而是通过统一任务句柄管理 Paper 与 Folia 的任务取消。

### 3. 动作调度适配

玩家相关动作会调度到目标玩家的执行上下文，包括：

- `title:`、`sound:`、`toast:`
- `command:`、`chat:`
- `money:`、`item:`、`stock-item:`
- `open:`、`force-open:`、`reset`、`close`、`force-close`
- `server:`、`tppos:`

`console:` 动作使用全局调度器执行。`tppos:` 在 Folia 环境中使用异步传送 API。

### 4. JavaScript 延迟辅助方法适配

JavaScript 内置方法保持原有写法：

```javascript
delay(20, function () {
    tell(player, "一秒后执行");
});
```

- `delay()` 在玩家上下文中使用玩家调度器
- `asyncDelay()` 使用异步调度器
- 无玩家上下文的延迟任务使用全局调度器

自定义 JavaScript 若直接访问 Bukkit 玩家、实体、世界或区块 API，仍需遵守 Folia 的区域线程规则。`asyncDelay()` 回调不应直接操作这些 Bukkit 对象。

### 5. 外部菜单 API 适配

以下 API 会将菜单打开逻辑调度到目标玩家线程：

```kotlin
KaMenuAPI.openYaml(player, yaml, "external:main")
KaMenuAPI.openConfig(player, config, "external:main")
```

外部 action handler 会在动作当前执行上下文中调用。外部插件若直接操作玩家、实体或世界，需要自行遵守 Folia 线程规则。

---

## 📝 兼容说明

- 菜单 YAML 语法无需修改
- 配置文件版本无需升级
- Paper 与 Folia 使用同一个插件 jar
- 建议使用与 Minecraft 版本匹配的最新 Paper/Folia 构建
- 第三方 PlaceholderAPI 扩展、外部 action handler、JavaScript 中直接调用的 Bukkit API，以及通过 `console:` 执行的其他插件指令，仍取决于对应扩展或插件自身的 Folia 兼容性

---

## 🚀 升级指南

### 从 1.6.1 升级到 1.6.2

1. 替换插件 jar 文件为 `KaMenu-1.6.2.jar`
2. 完整重启服务器
3. Paper 服务器无需修改配置
4. Folia 服务器建议验证菜单打开、`wait`、`Events.Tasks`、JavaScript 和跨玩家动作

---

# KaMenu v1.6.2 Update Report

## 📋 Version Info
- **Version**: 1.6.2
- **Release Date**: July 10, 2026

---

## ⚡ Folia Support

### 1. Folia region-threaded execution support

KaMenu now declares and supports Folia. It detects the current server environment and selects the appropriate scheduler on Paper and Folia:

- Player menus and player-bound actions use the player EntityScheduler
- Console and global operations use the GlobalRegionScheduler
- Database, network, and other asynchronous work use the AsyncScheduler
- Paper continues to use BukkitScheduler, preserving existing behavior

`plugin.yml` now includes:

```yaml
folia-supported: true
```

The same KaMenu jar supports both Paper and Folia.

### 2. Menu lifecycle and periodic task support

The unified Paper/Folia scheduling layer now covers:

- Menu open, force-open, reset, and close operations
- `Events.Open` and `Events.Close`
- `wait:` action-chain delays
- `Events.Tasks`, `run-task`, `stop-task`, and task end actions
- Join-time guide hints and update notifications
- Online player TAB-completion refresh after custom-command reloads

`Events.Tasks` no longer depends directly on `BukkitTask`; a unified task handle manages cancellation on Paper and Folia.

### 3. Action scheduling support

Player-bound actions run in the target player's scheduling context, including:

- `title:`, `sound:`, and `toast:`
- `command:` and `chat:`
- `money:`, `item:`, and `stock-item:`
- `open:`, `force-open:`, `reset`, `close`, and `force-close`
- `server:` and `tppos:`

`console:` actions use the global scheduler. On Folia, `tppos:` uses the asynchronous teleport API.

### 4. JavaScript delay helper support

Existing JavaScript syntax remains unchanged:

```javascript
delay(20, function () {
    tell(player, "Runs after one second");
});
```

- `delay()` uses the player scheduler when a player context is available
- `asyncDelay()` uses the asynchronous scheduler
- Delayed work without a player context uses the global scheduler

Custom JavaScript that directly accesses Bukkit player, entity, world, or chunk APIs must still follow Folia's region-threading rules. `asyncDelay()` callbacks should not directly manipulate these Bukkit objects.

### 5. External menu API support

The following APIs schedule menu opening on the target player's thread:

```kotlin
KaMenuAPI.openYaml(player, yaml, "external:main")
KaMenuAPI.openConfig(player, config, "external:main")
```

External action handlers run in the current action execution context. External plugins that directly manipulate players, entities, or worlds must follow Folia's threading rules.

---

## 📝 Compatibility Notes

- No menu YAML syntax changes are required
- No config version upgrade is required
- Paper and Folia use the same plugin jar
- Use a current Paper/Folia build matching the Minecraft version
- Third-party PlaceholderAPI expansions, external action handlers, direct Bukkit API calls from JavaScript, and commands from other plugins invoked through `console:` still depend on those components being Folia-compatible

---

## 🚀 Upgrade Guide

### Upgrading from 1.6.1 to 1.6.2

1. Replace the plugin jar with `KaMenu-1.6.2.jar`
2. Fully restart the server
3. No configuration changes are required on Paper
4. On Folia, verify menu opening, `wait`, `Events.Tasks`, JavaScript, and multi-player actions
