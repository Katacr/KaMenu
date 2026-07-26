# KaMenu v1.7.0 更新日志 / Changelog

## 版本信息 / Release Information

- **版本号 / Version:** 1.7.0
- **发布日期 / Release Date:** 2026-07-26

---

## 中文

### 单 JAR 支持 Spigot Dialog

- 同一个 `KaMenu-1.7.0.jar` 可运行于 Paper、Folia 与 Spigot，无需额外兼容插件。
- 插件启动时自动探测平台，只加载对应的 Paper 或 Spigot Dialog 适配器。
- 将菜单入口、关闭操作和 Folia 调度实现隔离，避免 Spigot 启动时解析 Paper/Folia 专属类。
- Kotlin 与 Adventure 运行时由服务端在插件主类实例化前自动下载，兼顾精简 JAR 与全新 Spigot 启动顺序。
- 在 Spigot 26.2 与 Paper 26.2 上完成同一 JAR 的启动、Body/Inputs/Bottom 原生构造、Dialog 显示调用与关闭调用运行时验证。

### Spigot 完整菜单运行时

- 为每个按钮和正文 `actions=` 文本建立服务端可信、绑定玩家且一次性的回调。
- 支持完整 actions 队列、条件、`wait`、`return`、JavaScript、全局包、存储和外部 action handler。
- 支持 `Events.Open`、`Events.Close`、`Events.Click` 与 `Events.Tasks`，并接通 `open`、`reset`、`close`、`force-close` 生命周期。
- `input`、`slider`、`dropdown`、`checkbox` 的响应会提交给 actions；服务端会校验长度、数值范围、选项 ID 与复选框值。
- 支持文件菜单、自定义指令、`/km open`、`KaMenuAPI.openYaml/openConfig`、内存菜单重置和 `Settings.need_placeholder`。
- 支持 `notice`、`confirmation`、`multi`、无按钮 Dialog 与 `repeat` 动态按钮。
- `Settings.lifetime` 会主动关闭过期 Dialog、停止周期任务、清理分页并执行 `Events.Close`。

### 平台隔离与边界

- 消息、ActionBar、Title、可点击文本、物品名称/Lore/模型读取和 Dialog 关闭统一经过平台适配器。
- Folia 区域线程判断与 `teleportAsync` 移入隔离适配器，Spigot 通用代码不再直接链接 Folia API。
- `toast:` 在 Spigot 下发送本地化的不支持提示；ESC 暂停菜单入口通过平台各自的 custom-click 事件支持 Paper、Folia 与 Spigot。
- Spigot `Body.item` 使用 Bukkit/Spigot 公共 API 映射名称、Lore、附魔、耐久、模型、光效、Tooltip、皮革颜色和玩家头颅，不静态或反射调用 NMS。
- PDC、插件私有组件和 Bukkit 未公开的数据组件不会写入 Spigot Dialog；映射异常时按组件回退到基础物品并限流告警。
- Spigot 可点击文本 `hover_item` 与 `Body.item` 复用同一套 Bukkit 公共属性映射；`message` 中的 sprite 文本组件及 `Body.item` 玩家头颅同样支持。
- Spigot 运行时 Adventure/MiniMessage 统一升级至 `4.26.1`，修复 sprite 标签因旧运行库而原样显示的问题。
- 修复 sprite 材质键读取误用 Paper `NamespacedKey.value()` 导致的 Spigot `NoSuchMethodError`。
- 客户端静态 `url:` / `copy:` 不会向服务端回传关闭状态，相关生命周期由 `Settings.lifetime` 兜底。
- 修复 Spigot 下 `sound:` 使用 Paper 专属注册表字段导致的异常，并保留资源包自定义音效支持。
- 验证内置 `{checkitem:[...]}` 与外部 `%kamenu_checkitem_[...]%` 在 Spigot 玩家背包中的读取链路。
- 修复 Spigot Dialog 正文可点击文本无法执行 `actions=` 的问题。

### Paper/Folia 兼容

- 既有 Paper Dialog、输入回调、actions、Events 和周期任务保持兼容。
- Paper 回调也统一复用平台中立的动作执行入口；外部内存菜单的 `reset` 可重新打开当前配置。
- Folia 调度器继续按玩家、全局与异步线程边界执行，并改为运行时隔离加载。

---

## English

### One JAR for Spigot Dialogs

- The same `KaMenu-1.7.0.jar` runs on Paper, Folia, and Spigot without a separate compatibility plugin.
- KaMenu detects the platform at startup and loads only the matching Paper or Spigot Dialog adapter.
- Menu entry points, close operations, and Folia scheduler code are isolated so Spigot never links Paper/Folia-only classes during startup.
- The server downloads Kotlin and Adventure before instantiating the plugin main class, keeping the JAR smaller while preserving clean Spigot startup order.
- The same JAR has been runtime-tested on Spigot 26.2 and Paper 26.2 for startup, native Body/Inputs/Bottom construction, Dialog show calls, and close calls.

### Complete Menu Runtime on Spigot

- Each button and body `actions=` segment receives a trusted, player-bound, one-shot server-side callback.
- Full action queues, conditions, `wait`, `return`, JavaScript, global packages, storage, and external action handlers are supported.
- `Events.Open`, `Events.Close`, `Events.Click`, and `Events.Tasks` are supported together with `open`, `reset`, `close`, and `force-close` lifecycle handling.
- All four Input types submit values to actions after server-side validation of length, range, option IDs, and checkbox values.
- Loaded menus, custom commands, `/km open`, `KaMenuAPI.openYaml/openConfig`, in-memory reset, and `Settings.need_placeholder` are supported.
- `notice`, `confirmation`, `multi`, buttonless Dialogs, and generated `repeat` buttons are supported.
- `Settings.lifetime` actively closes expired Dialogs, stops Tasks, clears pagination, and runs `Events.Close`.

### Platform Isolation and Boundaries

- Messages, ActionBar, Title, clickable text, item name/lore/model access, and Dialog close operations now use platform adapters.
- Folia ownership checks and `teleportAsync` are isolated from shared Spigot runtime classes.
- `toast:` sends a localized unsupported message on Spigot. The ESC pause-screen entry supports Paper, Folia, and Spigot through each platform's custom-click event.
- Spigot `Body.item` maps names, lore, enchantments, damage, models, glint, tooltip settings, leather colors, and player heads through Bukkit/Spigot public APIs without static or reflective NMS calls.
- PDC, plugin-private components, and data components not exposed by Bukkit are omitted from Spigot Dialogs. Mapping errors fall back per component with rate-limited warnings.
- Spigot clickable-text `hover_item` shares the same Bukkit public-property mapping as `Body.item`; sprite text components in `message` and player heads rendered through `Body.item` are supported as well.
- Upgraded the Spigot Adventure/MiniMessage runtime to `4.26.1`, fixing sprite tags being displayed literally by the older runtime.
- Fixed a Spigot `NoSuchMethodError` caused by using Paper's `NamespacedKey.value()` while resolving sprite material keys.
- Client-side `url:` and `copy:` actions cannot report close state to the server; `Settings.lifetime` provides lifecycle fallback cleanup.
- Fixed `sound:` failures on Spigot caused by a Paper-only registry field while retaining resource-pack custom sound support.
- Verified both internal `{checkitem:[...]}` and external `%kamenu_checkitem_[...]%` inventory lookups on Spigot.
- Fixed clickable body text failing to execute `actions=` in Spigot Dialogs.

### Paper/Folia Compatibility

- Existing Paper Dialogs, input callbacks, actions, Events, and periodic Tasks remain compatible.
- Paper callbacks now reuse the same platform-neutral action entry point, and `reset` can reopen the current in-memory menu.
- Folia continues to use player, global, and async scheduling boundaries through a runtime-isolated adapter.
